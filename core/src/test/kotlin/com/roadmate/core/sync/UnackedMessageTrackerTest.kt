package com.roadmate.core.sync

import com.roadmate.core.model.sync.SyncMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("UnackedMessageTracker")
class UnackedMessageTrackerTest {

    private lateinit var tracker: UnackedMessageTracker

    @BeforeEach
    fun setUp() {
        tracker = UnackedMessageTracker()
    }

    @Nested
    @DisplayName("trackPending")
    inner class TrackPending {

        @Test
        fun `stores push message with messageId`() {
            val push = SyncMessage.SyncPush("vehicle", "[{}]", "msg-1", 1000L)
            tracker.trackPending(push)
            assertEquals(1, tracker.pendingCount())
        }

        @Test
        fun `stores multiple push messages`() {
            tracker.trackPending(SyncMessage.SyncPush("vehicle", "[{}]", "msg-1", 1000L))
            tracker.trackPending(SyncMessage.SyncPush("trip", "[{}]", "msg-2", 1001L))
            assertEquals(2, tracker.pendingCount())
        }
    }

    @Nested
    @DisplayName("acknowledge")
    inner class Acknowledge {

        @Test
        fun `removes pending message on ack`() {
            tracker.trackPending(SyncMessage.SyncPush("vehicle", "[{}]", "msg-1", 1000L))
            val result = tracker.acknowledge("msg-1")
            assertTrue(result)
            assertEquals(0, tracker.pendingCount())
        }

        @Test
        fun `returns false for unknown messageId`() {
            val result = tracker.acknowledge("unknown")
            assertEquals(false, result)
        }
    }

    @Nested
    @DisplayName("drainUnacked")
    inner class DrainUnacked {

        @Test
        fun `returns unacked messages and clears tracker`() {
            val push1 = SyncMessage.SyncPush("vehicle", """[{"id":"v-1"}]""", "msg-1", 1000L)
            val push2 = SyncMessage.SyncPush("trip", """[{"id":"t-1"}]""", "msg-2", 1001L)
            tracker.trackPending(push1)
            tracker.trackPending(push2)
            tracker.acknowledge("msg-1")

            val unacked = tracker.drainUnacked()
            assertEquals(1, unacked.size)
            assertEquals("msg-2", unacked[0].messageId)
            assertEquals(0, tracker.pendingCount())
        }

        @Test
        fun `returns empty list when all acked`() {
            tracker.trackPending(SyncMessage.SyncPush("vehicle", "[{}]", "msg-1", 1000L))
            tracker.acknowledge("msg-1")
            val unacked = tracker.drainUnacked()
            assertTrue(unacked.isEmpty())
        }

        @Test
        fun `returns all when none acked`() {
            tracker.trackPending(SyncMessage.SyncPush("vehicle", "[{}]", "msg-1", 1000L))
            tracker.trackPending(SyncMessage.SyncPush("trip", "[{}]", "msg-2", 1001L))
            val unacked = tracker.drainUnacked()
            assertEquals(2, unacked.size)
        }
    }

    @Nested
    @DisplayName("isComplete")
    inner class IsComplete {

        @Test
        fun `returns true when no pending messages`() {
            assertTrue(tracker.isComplete())
        }

        @Test
        fun `returns false when pending messages exist`() {
            tracker.trackPending(SyncMessage.SyncPush("vehicle", "[{}]", "msg-1", 1000L))
            assertEquals(false, tracker.isComplete())
        }
    }

    @Nested
    @DisplayName("idempotency")
    inner class Idempotency {

        @Test
        fun `re-processing same push produces identical result`() = runTest {
            val push = SyncMessage.SyncPush("vehicle", """[{"id":"v-1"}]""", "msg-1", 1000L)
            tracker.trackPending(push)
            tracker.acknowledge("msg-1")
            val drained = tracker.drainUnacked()
            assertTrue(drained.isEmpty())

            tracker.trackPending(push)
            assertEquals(1, tracker.pendingCount())
            tracker.acknowledge("msg-1")
            assertTrue(tracker.isComplete())
        }
    }
}
