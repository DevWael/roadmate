package com.roadmate.core.database.converter

import androidx.room.TypeConverter
import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.OdometerUnit

/**
 * Room TypeConverters for enum types used across entities.
 *
 * Stores enums as their [name] string representation for readability
 * and forward compatibility when new enum values are added.
 */
class Converters {

    @TypeConverter
    fun fromEngineType(value: EngineType): String = value.name

    @TypeConverter
    fun toEngineType(value: String): EngineType = EngineType.valueOf(value)

    @TypeConverter
    fun fromFuelType(value: FuelType): String = value.name

    @TypeConverter
    fun toFuelType(value: String): FuelType = FuelType.valueOf(value)

    @TypeConverter
    fun fromOdometerUnit(value: OdometerUnit): String = value.name

    @TypeConverter
    fun toOdometerUnit(value: String): OdometerUnit = OdometerUnit.valueOf(value)
}
