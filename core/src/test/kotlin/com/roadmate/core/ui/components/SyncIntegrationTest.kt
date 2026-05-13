package com.roadmate.core.ui.components

import com.roadmate.core.model.DrivingState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Sync UI integration logic")
class SyncIntegrationTest {

    @Nested
    @DisplayName("driving mode behavior")
    inner class DrivingMode {

        @Test
        fun `driving state suppresses snackbar`() {
            val drivingState: DrivingState = DrivingState.Driving("trip-1", 10.0, 5000L)
            assertTrue(isDriving(drivingState))
        }

        @Test
        fun `idle state allows snackbar`() {
            val drivingState: DrivingState = DrivingState.Idle
            assertFalse(isDriving(drivingState))
        }

        @Test
        fun `stopping state suppresses snackbar`() {
            val drivingState: DrivingState = DrivingState.Stopping(timeSinceStopMs = 3000L)
            assertTrue(isDriving(drivingState))
        }

        @Test
        fun `gap check state suppresses snackbar`() {
            val drivingState: DrivingState = DrivingState.GapCheck(gapDurationMs = 5000L)
            assertTrue(isDriving(drivingState))
        }

        @Test
        fun `sync feedback type is chip only while driving`() {
            val feedback = syncFeedbackType(isDriving = true, hasNewData = true)
            assertEquals(SyncFeedbackType.ChipOnly, feedback)
        }

        @Test
        fun `sync feedback type is chip with shimmer while parked with new data`() {
            val feedback = syncFeedbackType(isDriving = false, hasNewData = true)
            assertEquals(SyncFeedbackType.ChipAndShimmer, feedback)
        }

        @Test
        fun `sync feedback type is chip only while parked without new data`() {
            val feedback = syncFeedbackType(isDriving = false, hasNewData = false)
            assertEquals(SyncFeedbackType.ChipOnly, feedback)
        }
    }

    @Nested
    @DisplayName("StatusChip updates in all modes")
    inner class ChipUpdates {

        @Test
        fun `chip always updates regardless of driving state`() {
            assertTrue(chipShouldUpdate(isDriving = true))
            assertTrue(chipShouldUpdate(isDriving = false))
        }
    }
}
