package com.roadmate.core.model.sync

import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.database.entity.Vehicle
import kotlinx.serialization.Serializable

/**
 * Sync DTO for [Vehicle] entity.
 */
@Serializable
data class VehicleSyncDto(
    val id: String,
    val name: String,
    val make: String,
    val model: String,
    val year: Int,
    val engineType: String,
    val engineSize: Double,
    val fuelType: String,
    val plateNumber: String,
    val vin: String?,
    val odometerKm: Double,
    val odometerUnit: String,
    val cityConsumption: Double,
    val highwayConsumption: Double,
    val lastModified: Long,
)

fun Vehicle.toSyncDto() = VehicleSyncDto(
    id = id,
    name = name,
    make = make,
    model = model,
    year = year,
    engineType = engineType.name,
    engineSize = engineSize,
    fuelType = fuelType.name,
    plateNumber = plateNumber,
    vin = vin,
    odometerKm = odometerKm,
    odometerUnit = odometerUnit.name,
    cityConsumption = cityConsumption,
    highwayConsumption = highwayConsumption,
    lastModified = lastModified,
)

fun VehicleSyncDto.toEntity() = Vehicle(
    id = id,
    name = name,
    make = make,
    model = model,
    year = year,
    engineType = safeEnumValueOf(engineType, EngineType.OTHER),
    engineSize = engineSize,
    fuelType = safeEnumValueOf(fuelType, FuelType.OTHER),
    plateNumber = plateNumber,
    vin = vin,
    odometerKm = odometerKm,
    odometerUnit = safeEnumValueOf(odometerUnit, OdometerUnit.KM),
    cityConsumption = cityConsumption,
    highwayConsumption = highwayConsumption,
    lastModified = lastModified,
)
