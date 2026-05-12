package com.roadmate.core.sync

import com.roadmate.core.model.BtConnectionState
import com.roadmate.core.model.sync.SyncMessage
import com.roadmate.core.state.BluetoothStateManager
import com.roadmate.core.sync.protocol.MessageSerializer
import com.roadmate.core.util.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncSession @Inject constructor(
    private val stateManager: BluetoothStateManager,
    private val deltaEngine: DeltaSyncEngine,
    private val batcher: SyncBatcher,
    private val ackTracker: AckTracker,
    private val messageSerializer: MessageSerializer,
    private val clock: Clock,
) {

    /**
     * In-memory last sync timestamp. Updated after each successful sync.
     * TODO(Story 4-4): Persist to DataStore for survival across process death.
     */
    @Volatile
    var lastSyncTimestamp: Long = 0L
        private set

    suspend fun buildOutgoingMessages(lastSyncTs: Long = lastSyncTimestamp): List<SyncMessage> {
        val messages = mutableListOf<SyncMessage>()
        messages.add(createSyncStatus("local", clock.now(), lastSyncTs))
        val deltas = deltaEngine.queryDeltas(lastSyncTs)
        val pushMessages = createPushMessages(deltas)
        messages.addAll(pushMessages)
        return messages
    }

    fun createSyncStatus(deviceId: String, timestamp: Long, lastSyncTimestamp: Long): SyncMessage.SyncStatus {
        return SyncMessage.SyncStatus(
            deviceId = deviceId,
            timestamp = timestamp,
            lastSyncTimestamp = lastSyncTimestamp,
        )
    }

    suspend fun createPushMessages(deltas: List<SyncPushDto>): List<SyncMessage.SyncPush> {
        ackTracker.reset()
        return deltas.map { delta ->
            val messageId = UUID.randomUUID().toString()
            ackTracker.track(messageId)
            SyncMessage.SyncPush(
                entityType = delta.entityType,
                data = delta.data,
                messageId = messageId,
                timestamp = clock.now(),
            )
        }
    }

    fun handleAck(ack: SyncMessage.SyncAck): Boolean {
        return ackTracker.acknowledge(ack.messageId)
    }

    fun isSyncComplete(): Boolean = ackTracker.isComplete()

    fun beginSync() {
        stateManager.updateState(BtConnectionState.SyncInProgress)
    }

    fun syncComplete() {
        lastSyncTimestamp = clock.now()
        stateManager.updateState(BtConnectionState.Connected)
    }

    fun syncFailed(reason: String) {
        stateManager.updateState(BtConnectionState.SyncFailed(reason))
    }

    fun getMessageSerializer(): MessageSerializer = messageSerializer

    fun reset() {
        ackTracker.reset()
    }
}
