package com.roadmate.core.sync

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AckTracker @Inject constructor() {

    private val pending = ConcurrentHashMap.newKeySet<String>()

    fun track(messageId: String) {
        pending.add(messageId)
    }

    fun acknowledge(messageId: String): Boolean {
        return pending.remove(messageId)
    }

    fun isComplete(): Boolean = pending.isEmpty()

    fun pendingCount(): Int = pending.size

    fun pendingMessageIds(): Set<String> = pending.toSet()

    fun reset() {
        pending.clear()
    }
}
