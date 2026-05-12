package com.roadmate.core.sync

import android.bluetooth.BluetoothSocket
import com.roadmate.core.model.BtConnectionState
import com.roadmate.core.state.BluetoothStateManager
import com.roadmate.core.model.sync.SyncMessage
import com.roadmate.core.sync.protocol.MessageSerializer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothConnectionManager(
    private val server: BluetoothSyncServer,
    private val client: BluetoothSyncClient,
    private val stateManager: BluetoothStateManager,
    private val syncSession: SyncSession,
    private val scope: CoroutineScope,
) {
    @Inject constructor(
        server: BluetoothSyncServer,
        client: BluetoothSyncClient,
        stateManager: BluetoothStateManager,
        syncSession: SyncSession,
    ) : this(
        server = server,
        client = client,
        stateManager = stateManager,
        syncSession = syncSession,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    enum class Mode { IDLE, SERVER, CLIENT }

    private var mode = Mode.IDLE
    private var connectionJob: Job? = null
    private var monitorJob: Job? = null

    @Volatile private var activeSocket: BluetoothSocket? = null
    @Volatile private var reconnectAttempt = 0
    @Volatile private var isRunning = false

    val currentMode: Mode get() = mode

    companion object {
        private const val INITIAL_BACKOFF_MS = 2000L
        private const val MAX_BACKOFF_MS = 30_000L
        private const val SYNC_TIMEOUT_MS = 30_000L
    }

    fun startServer() {
        stop()
        mode = Mode.SERVER
        isRunning = true
        stateManager.updateState(BtConnectionState.Disconnected)
        server.start()
        connectionJob = scope.launch {
            server.connectedSocket.collect { socket ->
                monitorJob?.cancel()
                try { activeSocket?.close() } catch (_: IOException) {}
                activeSocket = socket
                stateManager.updateState(BtConnectionState.Connected)
                Timber.i("ConnectionManager: server accepted connection")
                monitorJob = launch { awaitProtocolCompletion() }
            }
        }
    }

    fun startClient() {
        stop()
        mode = Mode.CLIENT
        isRunning = true
        stateManager.updateState(BtConnectionState.Disconnected)
        connectionJob = scope.launch { clientReconnectionLoop() }
    }

    fun stop() {
        isRunning = false
        connectionJob?.cancel()
        connectionJob = null
        monitorJob?.cancel()
        monitorJob = null
        when (mode) {
            Mode.SERVER -> server.stop()
            Mode.CLIENT -> client.disconnect()
            Mode.IDLE -> {}
        }
        try { activeSocket?.close() } catch (_: IOException) {}
        activeSocket = null
        stateManager.updateState(BtConnectionState.Disconnected)
        mode = Mode.IDLE
    }

    fun destroy() {
        stop()
        server.destroy()
        client.destroy()
        scope.cancel()
    }

    private suspend fun clientReconnectionLoop() {
        while (scope.isActive && isRunning) {
            stateManager.updateState(BtConnectionState.Connecting)
            val socket = try {
                client.connect()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Timber.e(e, "ConnectionManager: client connection failed")
                null
            }
            if (socket != null) {
                reconnectAttempt = 0
                activeSocket = socket
                stateManager.updateState(BtConnectionState.Connected)
                Timber.i("ConnectionManager: client connected")
                awaitProtocolCompletion()
                stateManager.updateState(BtConnectionState.Disconnected)
            } else {
                stateManager.updateState(BtConnectionState.Disconnected)
            }
            if (!isRunning) break
            val backoff = calculateBackoff(reconnectAttempt)
            Timber.i("ConnectionManager: reconnecting in ${backoff}ms (attempt $reconnectAttempt)")
            reconnectAttempt++
            try {
                delay(backoff)
            } catch (e: CancellationException) {
                break
            }
        }
    }

    private suspend fun awaitProtocolCompletion() {
        val socket = activeSocket ?: return
        try {
            val syncMessages = syncSession.buildOutgoingMessages()
            val serializer = syncSession.getMessageSerializer()
            val output = socket.outputStream
            val input = socket.inputStream

            syncSession.beginSync()

            for (msg in syncMessages) {
                val json = kotlinx.serialization.json.Json.encodeToString(SyncMessage.serializer(), msg)
                serializer.writeMessage(output, json)
            }

            withTimeout(SYNC_TIMEOUT_MS) {
                while (!syncSession.isSyncComplete()) {
                    val ackJson = serializer.readMessage(input)
                    val ack = kotlinx.serialization.json.Json.decodeFromString(SyncMessage.serializer(), ackJson)
                    if (ack is SyncMessage.SyncAck) {
                        syncSession.handleAck(ack)
                    }
                }
            }

            syncSession.syncComplete()
            Timber.i("ConnectionManager: sync completed successfully")
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Timber.e(e, "ConnectionManager: sync failed")
            syncSession.syncFailed(e.message ?: "IO error")
        } catch (e: Exception) {
            Timber.e(e, "ConnectionManager: sync error")
            syncSession.syncFailed(e.message ?: "Unknown error")
        }
    }

    internal fun calculateBackoff(attempt: Int): Long {
        val safeAttempt = maxOf(0, attempt)
        val delay = INITIAL_BACKOFF_MS * (1L shl minOf(safeAttempt, 4))
        return minOf(delay, MAX_BACKOFF_MS)
    }
}
