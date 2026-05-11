package com.roadmate.core.state

data class TripDetectionConfig(
    val startSpeedKmh: Float = 8.0f,
    val stopSpeedKmh: Float = 3.0f,
    val consecutiveReadingsForStart: Int = 3,
    val stopTimeoutMs: Long = 120_000,
    val driftAccuracyThreshold: Float = 30f,
    val driftSpeedThreshold: Float = 5.0f,
    val gapThresholdMs: Long = 30_000,
    val gapTimeoutMs: Long = 300_000,
    val teleportSpeedKmh: Double = 200.0,
    val accuracyRampMaxMeters: Float = 50f,
)
