package com.roadmate.core.sync

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothSyncServer(
    private val adapter: BluetoothAdapter?,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
) {
    @Inject constructor() : this(
        adapter = BluetoothAdapter.getDefaultAdapter(),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ioDispatcher = Dispatchers.IO,
    )

    private var serverSocket: BluetoothServerSocket? = null
    private var acceptJob: Job? = null

    private val _connectedSocket = MutableSharedFlow<BluetoothSocket>(extraBufferCapacity = 1)
    val connectedSocket: SharedFlow<BluetoothSocket> = _connectedSocket.asSharedFlow()

    companion object {
        val ROADMATE_UUID: UUID = UUID.nameUUIDFromBytes("com.roadmate.sync".toByteArray())
        const val SERVICE_NAME = "RoadMateSync"
    }

    fun start() {
        if (adapter == null || !adapter.isEnabled) {
            Timber.w("BluetoothSyncServer: adapter unavailable or disabled")
            return
        }
        stop()
        acceptJob = scope.launch {
            try {
                val socket = withContext(ioDispatcher) {
                    adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, ROADMATE_UUID)
                }
                serverSocket = socket
                Timber.i("BluetoothSyncServer: listening on RFCOMM SPP")
                while (isActive) {
                    try {
                        val client = withContext(ioDispatcher) { socket.accept() }
                        if (client != null) {
                            Timber.i("BluetoothSyncServer: accepted from ${client.remoteDevice?.address}")
                            _connectedSocket.emit(client)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: IOException) {
                        if (isActive) Timber.e(e, "BluetoothSyncServer: accept error")
                    }
                }
            } catch (e: CancellationException) {
                // normal shutdown
            } catch (e: IOException) {
                Timber.e(e, "BluetoothSyncServer: server socket error")
            } catch (e: SecurityException) {
                Timber.e(e, "BluetoothSyncServer: permission denied")
            }
        }
    }

    fun stop() {
        acceptJob?.cancel()
        acceptJob = null
        try {
            serverSocket?.close()
        } catch (_: IOException) {}
        serverSocket = null
    }

    fun destroy() {
        stop()
        scope.cancel()
    }
}
