package com.roadmate.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a vehicle profile stored in the local database.
 *
 * This entity serves as both the Room entity and the domain model.
 * Sync operations use separate DTOs (Story 1.3).
 */
@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val name: String,

    val make: String,

    val model: String,

    val year: Int,

    @ColumnInfo(name = "engine_type")
    val engineType: EngineType,

    @ColumnInfo(name = "engine_size")
    val engineSize: Double,

    @ColumnInfo(name = "fuel_type")
    val fuelType: FuelType,

    @ColumnInfo(name = "plate_number")
    val plateNumber: String,

    val vin: String? = null,

    @ColumnInfo(name = "odometer_km")
    val odometerKm: Double,

    @ColumnInfo(name = "odometer_unit")
    val odometerUnit: OdometerUnit,

    @ColumnInfo(name = "city_consumption")
    val cityConsumption: Double,

    @ColumnInfo(name = "highway_consumption")
    val highwayConsumption: Double,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),
)

enum class EngineType {
    INLINE_4,
    INLINE_6,
    V6,
    V8,
    V12,
    FLAT_4,
    FLAT_6,
    ROTARY,
    ELECTRIC,
    HYBRID,
    OTHER,
}

enum class FuelType {
    GASOLINE,
    DIESEL,
    ELECTRIC,
    HYBRID,
    LPG,
    CNG,
    OTHER,
}

enum class OdometerUnit {
    KM,
    MILES,
}
