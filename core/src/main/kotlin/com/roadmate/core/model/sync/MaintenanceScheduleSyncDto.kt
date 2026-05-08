package com.roadmate.core.model.sync

import com.roadmate.core.database.entity.MaintenanceSchedule
import kotlinx.serialization.Serializable

/**
 * Sync DTO for [MaintenanceSchedule] entity.
 */
@Serializable
data class MaintenanceScheduleSyncDto(
    val id: String,
    val vehicleId: String,
    val name: String,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val lastServiceKm: Double,
    val lastServiceDate: Long,
    val isCustom: Boolean,
    val lastModified: Long,
)

fun MaintenanceSchedule.toSyncDto() = MaintenanceScheduleSyncDto(
    id = id,
    vehicleId = vehicleId,
    name = name,
    intervalKm = intervalKm,
    intervalMonths = intervalMonths,
    lastServiceKm = lastServiceKm,
    lastServiceDate = lastServiceDate,
    isCustom = isCustom,
    lastModified = lastModified,
)

fun MaintenanceScheduleSyncDto.toEntity() = MaintenanceSchedule(
    id = id,
    vehicleId = vehicleId,
    name = name,
    intervalKm = intervalKm,
    intervalMonths = intervalMonths,
    lastServiceKm = lastServiceKm,
    lastServiceDate = lastServiceDate,
    isCustom = isCustom,
    lastModified = lastModified,
)
