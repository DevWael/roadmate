package com.roadmate.headunit.ui

import com.roadmate.core.model.DrivingState
import com.roadmate.core.model.GpsState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ContextAwareLayout logic")
class ContextAwareLayoutTest {

    @Nested
    @DisplayName("driving state detection")
    inner class DrivingStateDetection {

        @Test
        fun `Driving state shows HUD`() {
            val state: DrivingState = DrivingState.Driving("t1", 15.0, 60000L)
            assertTrue(state is DrivingState.Driving)
        }

        @Test
        fun `Idle state shows parked dashboard`() {
            val state: DrivingState = DrivingState.Idle
            assertFalse(state is DrivingState.Driving)
        }

        @Test
        fun `Stopping state shows parked dashboard`() {
            val state: DrivingState = DrivingState.Stopping(3000L)
            assertFalse(state is DrivingState.Driving)
        }

        @Test
        fun `GapCheck state shows parked dashboard`() {
            val state: DrivingState = DrivingState.GapCheck(5000L)
            assertFalse(state is DrivingState.Driving)
        }
    }

    @Nested
    @DisplayName("transition duration")
    inner class TransitionDuration {

        @Test
        fun `normal transition is 400ms`() {
            assertEquals(400, TRANSITION_DURATION_MS)
        }

        @Test
        fun `reduced motion uses 0ms`() {
            val reduceMotion = true
            val duration = if (reduceMotion) 0 else TRANSITION_DURATION_MS
            assertEquals(0, duration)
        }
    }
}
