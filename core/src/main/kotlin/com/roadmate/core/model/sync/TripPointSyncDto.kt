package com.roadmate.core.model.sync

import com.roadmate.core.database.entity.TripPoint
import kotlinx.serialization.Serializable

/**
 * Sync DTO for [TripPoint] entity.
 */
@Serializable
data class TripPointSyncDto(
    val id: String,
    val tripId: String,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double,
    val altitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val lastModified: Long,
)

fun TripPoint.toSyncDto() = TripPointSyncDto(
    id = id,
    tripId = tripId,
    latitude = latitude,
    longitude = longitude,
    speedKmh = speedKmh,
    altitude = altitude,
    accuracy = accuracy,
    timestamp = timestamp,
    lastModified = lastModified,
)

fun TripPointSyncDto.toEntity() = TripPoint(
    id = id,
    tripId = tripId,
    latitude = latitude,
    longitude = longitude,
    speedKmh = speedKmh,
    altitude = altitude,
    accuracy = accuracy,
    timestamp = timestamp,
    lastModified = lastModified,
)
