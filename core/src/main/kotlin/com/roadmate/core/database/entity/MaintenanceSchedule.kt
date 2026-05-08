package com.roadmate.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a maintenance schedule item for a vehicle.
 *
 * Each schedule defines an interval-based maintenance task (e.g., oil change every 10,000 km
 * or every 6 months). Custom schedules can be created by users.
 */
@Entity(
    tableName = "maintenance_schedules",
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
data class MaintenanceSchedule(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "vehicle_id")
    val vehicleId: String,

    val name: String,

    @ColumnInfo(name = "interval_km")
    val intervalKm: Int? = null,

    @ColumnInfo(name = "interval_months")
    val intervalMonths: Int? = null,

    @ColumnInfo(name = "last_service_km")
    val lastServiceKm: Double,

    @ColumnInfo(name = "last_service_date")
    val lastServiceDate: Long,

    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),
)
