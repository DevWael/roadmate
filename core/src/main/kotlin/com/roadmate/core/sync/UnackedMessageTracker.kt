package com.roadmate.core.sync

import com.roadmate.core.model.sync.SyncMessage
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnackedMessageTracker @Inject constructor() {

    private val pending = ConcurrentHashMap<String, SyncMessage.SyncPush>()

    fun trackPending(push: SyncMessage.SyncPush) {
        pending[push.messageId] = push
    }

    fun acknowledge(messageId: String): Boolean = pending.remove(messageId) != null

    fun isComplete(): Boolean = pending.isEmpty()

    fun pendingCount(): Int = pending.size

    fun drainUnacked(): List<SyncMessage.SyncPush> {
        val unacked = pending.values.toList()
        pending.clear()
        return unacked
    }
}
