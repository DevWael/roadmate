package com.roadmate.core.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("AckTracker")
class AckTrackerTest {

    private lateinit var tracker: AckTracker

    @BeforeEach
    fun setUp() {
        tracker = AckTracker()
    }

    @Nested
    @DisplayName("track")
    inner class Track {

        @Test
        fun `adds messageId to pending set`() {
            val id = UUID.randomUUID().toString()
            tracker.track(id)
            assertFalse(tracker.isComplete())
            assertEquals(1, tracker.pendingCount())
        }

        @Test
        fun `tracks multiple messageIds`() {
            tracker.track("id-1")
            tracker.track("id-2")
            tracker.track("id-3")
            assertEquals(3, tracker.pendingCount())
            assertFalse(tracker.isComplete())
        }
    }

    @Nested
    @DisplayName("acknowledge")
    inner class Acknowledge {

        @Test
        fun `removes messageId from pending set`() = runTest {
            tracker.track("id-1")
            tracker.track("id-2")
            val result = tracker.acknowledge("id-1")
            assertTrue(result)
            assertEquals(1, tracker.pendingCount())
        }

        @Test
        fun `returns false for unknown messageId`() = runTest {
            tracker.track("id-1")
            val result = tracker.acknowledge("unknown")
            assertFalse(result)
            assertEquals(1, tracker.pendingCount())
        }

        @Test
        fun `isComplete returns true when all acked`() = runTest {
            tracker.track("id-1")
            tracker.track("id-2")
            assertFalse(tracker.isComplete())
            tracker.acknowledge("id-1")
            assertFalse(tracker.isComplete())
            tracker.acknowledge("id-2")
            assertTrue(tracker.isComplete())
        }

        @Test
        fun `isComplete returns true immediately when no messages tracked`() {
            assertTrue(tracker.isComplete())
        }
    }

    @Nested
    @DisplayName("reset")
    inner class Reset {

        @Test
        fun `clears all tracked messages`() {
            tracker.track("id-1")
            tracker.track("id-2")
            tracker.reset()
            assertTrue(tracker.isComplete())
            assertEquals(0, tracker.pendingCount())
        }
    }

    @Nested
    @DisplayName("pendingMessageIds")
    inner class PendingMessageIds {

        @Test
        fun `returns all unacked messageIds`() {
            tracker.track("id-1")
            tracker.track("id-2")
            tracker.track("id-3")
            val pending = tracker.pendingMessageIds()
            assertEquals(3, pending.size)
            assertTrue(pending.contains("id-1"))
            assertTrue(pending.contains("id-2"))
            assertTrue(pending.contains("id-3"))
        }
    }
}
