package com.roadmate.core.model

/**
 * Represents the current driving lifecycle state.
 *
 * Subtypes carry contextual data needed by the trip recorder and HUD.
 */
sealed interface DrivingState {
    data object Idle : DrivingState
    data class Driving(
        val tripId: String,
        val distanceKm: Double,
        val durationMs: Long,
    ) : DrivingState

    data class Stopping(
        val timeSinceStopMs: Long,
    ) : DrivingState

    data class GapCheck(
        val gapDurationMs: Long,
    ) : DrivingState
}
