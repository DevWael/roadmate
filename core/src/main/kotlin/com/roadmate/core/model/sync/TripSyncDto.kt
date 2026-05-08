package com.roadmate.core.model.sync

import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripStatus
import kotlinx.serialization.Serializable

/**
 * Sync DTO for [Trip] entity.
 * Uses camelCase field names for Bluetooth sync protocol.
 */
@Serializable
data class TripSyncDto(
    val id: String,
    val vehicleId: String,
    val startTime: Long,
    val endTime: Long?,
    val distanceKm: Double,
    val durationMs: Long,
    val maxSpeedKmh: Double,
    val avgSpeedKmh: Double,
    val estimatedFuelL: Double,
    val startOdometerKm: Double,
    val endOdometerKm: Double,
    val status: String,
    val lastModified: Long,
)

fun Trip.toSyncDto() = TripSyncDto(
    id = id,
    vehicleId = vehicleId,
    startTime = startTime,
    endTime = endTime,
    distanceKm = distanceKm,
    durationMs = durationMs,
    maxSpeedKmh = maxSpeedKmh,
    avgSpeedKmh = avgSpeedKmh,
    estimatedFuelL = estimatedFuelL,
    startOdometerKm = startOdometerKm,
    endOdometerKm = endOdometerKm,
    status = status.name,
    lastModified = lastModified,
)

fun TripSyncDto.toEntity() = Trip(
    id = id,
    vehicleId = vehicleId,
    startTime = startTime,
    endTime = endTime,
    distanceKm = distanceKm,
    durationMs = durationMs,
    maxSpeedKmh = maxSpeedKmh,
    avgSpeedKmh = avgSpeedKmh,
    estimatedFuelL = estimatedFuelL,
    startOdometerKm = startOdometerKm,
    endOdometerKm = endOdometerKm,
    status = safeEnumValueOf(status, TripStatus.INTERRUPTED),
    lastModified = lastModified,
)
