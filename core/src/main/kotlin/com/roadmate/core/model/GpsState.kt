package com.roadmate.core.model

/**
 * GPS receiver states used by the trip-tracking pipeline.
 */
sealed interface GpsState {
    data object Acquired : GpsState
    data object Acquiring : GpsState
    data object Unavailable : GpsState
}
