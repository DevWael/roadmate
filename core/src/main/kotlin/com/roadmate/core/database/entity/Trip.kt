package com.roadmate.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a recorded trip for a vehicle.
 *
 * Tracks distance, duration, speed metrics, and fuel estimates.
 * Trips transition through ACTIVE → COMPLETED or ACTIVE → INTERRUPTED.
 */
@Entity(
    tableName = "trips",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["vehicle_id"])],
)
data class Trip(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "vehicle_id")
    val vehicleId: String,

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,

    @ColumnInfo(name = "distance_km")
    val distanceKm: Double,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,

    @ColumnInfo(name = "max_speed_kmh")
    val maxSpeedKmh: Double,

    @ColumnInfo(name = "avg_speed_kmh")
    val avgSpeedKmh: Double,

    @ColumnInfo(name = "estimated_fuel_l")
    val estimatedFuelL: Double,

    @ColumnInfo(name = "start_odometer_km")
    val startOdometerKm: Double,

    @ColumnInfo(name = "end_odometer_km")
    val endOdometerKm: Double,

    val status: TripStatus,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),
)

enum class TripStatus {
    ACTIVE,
    COMPLETED,
    INTERRUPTED,
}
