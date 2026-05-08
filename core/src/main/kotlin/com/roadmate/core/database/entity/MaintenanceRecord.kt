package com.roadmate.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a completed maintenance service record.
 *
 * Tracks when a specific maintenance schedule item was serviced,
 * including odometer reading, cost, and optional notes.
 */
@Entity(
    tableName = "maintenance_records",
    foreignKeys = [
        ForeignKey(
            entity = MaintenanceSchedule::class,
            parentColumns = ["id"],
            childColumns = ["schedule_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["schedule_id"]),
        Index(value = ["vehicle_id"]),
    ],
)
data class MaintenanceRecord(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "schedule_id")
    val scheduleId: String,

    @ColumnInfo(name = "vehicle_id")
    val vehicleId: String,

    @ColumnInfo(name = "date_performed")
    val datePerformed: Long,

    @ColumnInfo(name = "odometer_km")
    val odometerKm: Double,

    val cost: Double? = null,

    val location: String? = null,

    val notes: String? = null,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),
)
