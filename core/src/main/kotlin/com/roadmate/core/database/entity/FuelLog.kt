package com.roadmate.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a fuel fill-up log entry for a vehicle.
 *
 * Tracks fuel volume, cost, odometer reading, and whether it was a full tank
 * (needed for accurate consumption calculations).
 */
@Entity(
    tableName = "fuel_logs",
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
data class FuelLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "vehicle_id")
    val vehicleId: String,

    val date: Long,

    @ColumnInfo(name = "odometer_km")
    val odometerKm: Double,

    val liters: Double,

    @ColumnInfo(name = "price_per_liter")
    val pricePerLiter: Double,

    @ColumnInfo(name = "total_cost")
    val totalCost: Double,

    @ColumnInfo(name = "is_full_tank")
    val isFullTank: Boolean,

    val station: String? = null,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),
)
