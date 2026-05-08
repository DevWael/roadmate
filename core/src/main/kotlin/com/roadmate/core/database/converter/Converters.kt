package com.roadmate.core.database.converter

import androidx.room.TypeConverter
import com.roadmate.core.database.entity.DocumentType
import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.database.entity.TripStatus

/**
 * Room TypeConverters for enum types used across entities.
 *
 * Stores enums as their [name] string representation for readability
 * and forward compatibility when new enum values are added.
 *
 * Uses [safeValueOf] to gracefully handle unknown enum values from
 * newer app versions or corrupted data, falling back to a sensible default.
 */
class Converters {

    @TypeConverter
    fun fromEngineType(value: EngineType): String = value.name

    @TypeConverter
    fun toEngineType(value: String): EngineType =
        safeValueOf(value, EngineType.OTHER)

    @TypeConverter
    fun fromFuelType(value: FuelType): String = value.name

    @TypeConverter
    fun toFuelType(value: String): FuelType =
        safeValueOf(value, FuelType.OTHER)

    @TypeConverter
    fun fromOdometerUnit(value: OdometerUnit): String = value.name

    @TypeConverter
    fun toOdometerUnit(value: String): OdometerUnit =
        safeValueOf(value, OdometerUnit.KM)

    @TypeConverter
    fun fromTripStatus(value: TripStatus): String = value.name

    @TypeConverter
    fun toTripStatus(value: String): TripStatus =
        safeValueOf(value, TripStatus.INTERRUPTED)

    @TypeConverter
    fun fromDocumentType(value: DocumentType): String = value.name

    @TypeConverter
    fun toDocumentType(value: String): DocumentType =
        safeValueOf(value, DocumentType.OTHER)
}

/**
 * Safely converts a string to an enum value, returning [default] if the
 * value doesn't match any enum constant. Prevents crashes from unknown
 * values introduced by newer app versions or data corruption during sync.
 */
private inline fun <reified T : Enum<T>> safeValueOf(value: String, default: T): T =
    try {
        enumValueOf<T>(value)
    } catch (_: IllegalArgumentException) {
        default
    }
