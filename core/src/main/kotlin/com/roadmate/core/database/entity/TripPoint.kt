package com.roadmate.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a single GPS data point recorded during a trip.
 *
 * At 3-second GPS intervals, a 1-hour trip produces ~1200 points.
 * Index on [tripId] is critical for query performance given volume.
 */
@Entity(
    tableName = "trip_points",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["trip_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["trip_id"])],
)
data class TripPoint(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "trip_id")
    val tripId: String,

    val latitude: Double,

    val longitude: Double,

    @ColumnInfo(name = "speed_kmh")
    val speedKmh: Double,

    val altitude: Double,

    val accuracy: Float,

    val timestamp: Long,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),
)
