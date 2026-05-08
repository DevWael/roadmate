package com.roadmate.core.model.sync

import com.roadmate.core.database.entity.FuelLog
import kotlinx.serialization.Serializable

/**
 * Sync DTO for [FuelLog] entity.
 */
@Serializable
data class FuelLogSyncDto(
    val id: String,
    val vehicleId: String,
    val date: Long,
    val odometerKm: Double,
    val liters: Double,
    val pricePerLiter: Double,
    val totalCost: Double,
    val isFullTank: Boolean,
    val station: String?,
    val lastModified: Long,
)

fun FuelLog.toSyncDto() = FuelLogSyncDto(
    id = id,
    vehicleId = vehicleId,
    date = date,
    odometerKm = odometerKm,
    liters = liters,
    pricePerLiter = pricePerLiter,
    totalCost = totalCost,
    isFullTank = isFullTank,
    station = station,
    lastModified = lastModified,
)

fun FuelLogSyncDto.toEntity() = FuelLog(
    id = id,
    vehicleId = vehicleId,
    date = date,
    odometerKm = odometerKm,
    liters = liters,
    pricePerLiter = pricePerLiter,
    totalCost = totalCost,
    isFullTank = isFullTank,
    station = station,
    lastModified = lastModified,
)
