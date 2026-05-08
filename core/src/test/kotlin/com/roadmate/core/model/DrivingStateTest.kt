package com.roadmate.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DrivingState sealed interface")
class DrivingStateTest {

    @Test
    fun `Idle is a DrivingState`() {
        val state: DrivingState = DrivingState.Idle
        assertInstanceOf(DrivingState::class.java, state)
    }

    @Test
    fun `Driving carries tripId, distanceKm, durationMs`() {
        val state = DrivingState.Driving(
            tripId = "trip-1",
            distanceKm = 12.5,
            durationMs = 3600_000L,
        )
        assertEquals("trip-1", state.tripId)
        assertEquals(12.5, state.distanceKm)
        assertEquals(3600_000L, state.durationMs)
    }

    @Test
    fun `Stopping carries timeSinceStopMs`() {
        val state = DrivingState.Stopping(timeSinceStopMs = 5_000L)
        assertEquals(5_000L, state.timeSinceStopMs)
    }

    @Test
    fun `GapCheck carries gapDurationMs`() {
        val state = DrivingState.GapCheck(gapDurationMs = 120_000L)
        assertEquals(120_000L, state.gapDurationMs)
    }

    @Test
    fun `when exhaustive over all subtypes compiles`() {
        val state: DrivingState = DrivingState.Idle
        val result = when (state) {
            is DrivingState.Idle -> "idle"
            is DrivingState.Driving -> "driving"
            is DrivingState.Stopping -> "stopping"
            is DrivingState.GapCheck -> "gap"
        }
        assertEquals("idle", result)
    }
}
