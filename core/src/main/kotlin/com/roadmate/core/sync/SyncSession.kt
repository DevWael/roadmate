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
    private val unackedTracker: UnackedMessageTracker,
    private val timestampStore: SyncTimestampStore,
    private val clock: Clock,
) {

    @Volatile
    var lastSyncTimestamp: Long = 0L
        private set

    suspend fun init() {
        lastSyncTimestamp = timestampStore.getLastSyncTimestamp()
    }

    suspend fun buildOutgoingMessages(lastSyncTs: Long = lastSyncTimestamp): List<SyncMessage> {
        val messages = mutableListOf<SyncMessage>()
        messages.add(createSyncStatus("local", clock.now(), lastSyncTs))

        val unacked = unackedTracker.drainUnacked()
        if (unacked.isNotEmpty()) {
            messages.addAll(unacked)
            for (push in unacked) {
                ackTracker.track(push.messageId)
            }
        }

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
        return deltas.map { delta ->
            val messageId = UUID.randomUUID().toString()
            ackTracker.track(messageId)
            val push = SyncMessage.SyncPush(
                entityType = delta.entityType,
                data = delta.data,
                messageId = messageId,
                timestamp = clock.now(),
            )
            unackedTracker.trackPending(push)
            push
        }
    }

    fun handleAck(ack: SyncMessage.SyncAck): Boolean {
        val ackTracked = ackTracker.acknowledge(ack.messageId)
        if (ackTracked) {
            unackedTracker.acknowledge(ack.messageId)
        }
        return ackTracked
    }

    fun isSyncComplete(): Boolean = ackTracker.isComplete()

    fun beginSync() {
        stateManager.updateState(BtConnectionState.SyncInProgress)
    }

    suspend fun syncComplete() {
        if (!ackTracker.isComplete() || !unackedTracker.isComplete()) {
            syncFailed("Cannot complete sync: unacked messages remain")
            return
        }
        val now = clock.now()
        lastSyncTimestamp = now
        timestampStore.setLastSyncTimestamp(now)
        stateManager.updateState(BtConnectionState.Connected)
    }

    fun syncFailed(reason: String) {
        stateManager.updateState(BtConnectionState.SyncFailed(reason))
    }

    fun getMessageSerializer(): MessageSerializer = messageSerializer

    fun reset() {
        ackTracker.reset()
        unackedTracker.drainUnacked()
    }
}
