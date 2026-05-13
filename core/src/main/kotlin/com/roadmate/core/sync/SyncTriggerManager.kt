package com.roadmate.core.sync

import com.roadmate.core.model.BtConnectionState
import com.roadmate.core.model.sync.SyncReason
import com.roadmate.core.model.sync.SyncRequest
import com.roadmate.core.model.sync.SyncResult
import com.roadmate.core.state.BluetoothStateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncTriggerManager(
    private val stateManager: BluetoothStateManager,
    private val syncSession: SyncSession,
    private val deltaEngine: DeltaSyncEngine,
    private val scope: CoroutineScope,
) {
    @Inject constructor(
        stateManager: BluetoothStateManager,
        syncSession: SyncSession,
        deltaEngine: DeltaSyncEngine,
    ) : this(
        stateManager = stateManager,
        syncSession = syncSession,
        deltaEngine = deltaEngine,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    private val syncChannel = Channel<SyncRequest>(Channel.UNLIMITED)

    private val _syncResult = MutableStateFlow<SyncResult>(SyncResult.Idle)
    val syncResult: StateFlow<SyncResult> = _syncResult.asStateFlow()

    private var periodicJob: Job? = null
    private var channelConsumerJob: Job? = null
    private var eventObservationJobs = mutableListOf<Job>()

    companion object {
        private const val PERIODIC_INTERVAL_MS = 15L * 60L * 1000L
    }

    fun start() {
        channelConsumerJob = scope.launch {
            syncSession.init()
            for (request in syncChannel) {
                executeSyncSession(request.reason)
            }
        }
    }

    fun startEventObservation(
        tripEvents: Flow<Unit>,
        maintenanceEvents: Flow<Unit>,
        fuelEvents: Flow<Unit>,
    ) {
        stopEventObservation()

        eventObservationJobs.add(scope.launch {
            tripEvents.collect {
                requestSync(SyncReason.TRIP_COMPLETED)
            }
        })

        eventObservationJobs.add(scope.launch {
            maintenanceEvents.collect {
                requestSync(SyncReason.MAINTENANCE_DONE)
            }
        })

        eventObservationJobs.add(scope.launch {
            fuelEvents.collect {
                requestSync(SyncReason.FUEL_ENTRY)
            }
        })
    }

    fun stopEventObservation() {
        eventObservationJobs.forEach { it.cancel() }
        eventObservationJobs.clear()
    }

    fun requestSync(reason: SyncReason) {
        val btState = stateManager.btConnectionState.value
        if (btState !is BtConnectionState.Connected) {
            Timber.d("SyncTriggerManager: ignoring ${reason.name} sync, BT disconnected")
            return
        }
        syncChannel.trySend(SyncRequest(reason))
    }

    fun startPeriodicSync() {
        stopPeriodicSync()
        periodicJob = scope.launch {
            stateManager.btConnectionState.collectLatest { state ->
                if (state is BtConnectionState.Connected) {
                    while (isActive) {
                        delay(PERIODIC_INTERVAL_MS)
                        requestSync(SyncReason.PERIODIC)
                    }
                }
            }
        }
    }

    fun stopPeriodicSync() {
        periodicJob?.cancel()
        periodicJob = null
    }

    fun triggerManualSync(): Flow<SyncResult> {
        requestSync(SyncReason.MANUAL)
        return _syncResult
    }

    fun destroy() {
        stopEventObservation()
        stopPeriodicSync()
        channelConsumerJob?.cancel()
        channelConsumerJob = null
        syncChannel.close()
    }

    private suspend fun executeSyncSession(reason: SyncReason) {
        val btState = stateManager.btConnectionState.value
        if (btState !is BtConnectionState.Connected) {
            Timber.d("SyncTriggerManager: skipping ${reason.name} sync, BT disconnected")
            return
        }

        _syncResult.value = SyncResult.InProgress
        syncSession.beginSync()
        try {
            val deltas = deltaEngine.queryDeltas(syncSession.lastSyncTimestamp)
            if (deltas.isEmpty()) {
                Timber.d("SyncTriggerManager: ${reason.name} sync, no deltas to send")
                syncSession.reset()
                syncSession.syncComplete()
                _syncResult.value = SyncResult.Success(reason)
                return
            }

            Timber.i("SyncTriggerManager: ${reason.name} sync processing ${deltas.size} delta groups")
            // TODO: Wire actual RFCOMM transport — deltas queried but not transmitted yet
            syncSession.reset()
            syncSession.syncComplete()
            _syncResult.value = SyncResult.Success(reason)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "SyncTriggerManager: ${reason.name} sync failed")
            syncSession.syncFailed(e.message ?: "Unknown error")
            _syncResult.value = SyncResult.Failed(reason, e.message ?: "Unknown error")
        }
    }
}
