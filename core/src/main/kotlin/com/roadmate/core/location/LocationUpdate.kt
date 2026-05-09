package com.roadmate.core.location

data class LocationUpdate(
    val lat: Double,
    val lng: Double,
    val speedKmh: Float,
    val altitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val isLowAccuracy: Boolean,
)
