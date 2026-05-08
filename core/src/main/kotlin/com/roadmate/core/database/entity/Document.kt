package com.roadmate.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a vehicle-related document with expiry tracking.
 *
 * Supports insurance, license, registration, and custom document types.
 * [reminderDaysBefore] configures how early to alert before expiry (default: 30 days).
 */
@Entity(
    tableName = "documents",
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
data class Document(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "vehicle_id")
    val vehicleId: String,

    val type: DocumentType,

    val name: String,

    @ColumnInfo(name = "expiry_date")
    val expiryDate: Long,

    @ColumnInfo(name = "reminder_days_before")
    val reminderDaysBefore: Int = 30,

    val notes: String? = null,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),
)

enum class DocumentType {
    INSURANCE,
    LICENSE,
    REGISTRATION,
    OTHER,
}
