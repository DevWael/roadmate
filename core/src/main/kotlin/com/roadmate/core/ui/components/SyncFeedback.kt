package com.roadmate.core.ui.components

import com.roadmate.core.model.DrivingState

/**
 * Determines the type of UI feedback to show after a sync event.
 */
enum class SyncFeedbackType {
    ChipOnly,
    ChipAndShimmer,
}

/**
 * Returns true when the user is actively driving (includes stopping and gap-check states).
 * Used to suppress intrusive UI elements during driving.
 */
fun isDriving(drivingState: DrivingState): Boolean {
    return drivingState is DrivingState.Driving
            || drivingState is DrivingState.Stopping
            || drivingState is DrivingState.GapCheck
}

/**
 * Determines the appropriate sync feedback type based on driving state and data availability.
 * While driving or when no new data arrived, only the StatusChip updates.
 * When parked with new data, shimmer animation is also triggered.
 */
fun syncFeedbackType(isDriving: Boolean, hasNewData: Boolean): SyncFeedbackType {
    return if (isDriving || !hasNewData) {
        SyncFeedbackType.ChipOnly
    } else {
        SyncFeedbackType.ChipAndShimmer
    }
}

/**
 * StatusChip always updates regardless of driving state — only shimmer is suppressed.
 */
fun chipShouldUpdate(isDriving: Boolean): Boolean = true
