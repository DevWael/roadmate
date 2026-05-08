package com.roadmate.core.model.sync

import com.roadmate.core.database.entity.MaintenanceRecord
import kotlinx.serialization.Serializable

/**
 * Sync DTO for [MaintenanceRecord] entity.
 */
@Serializable
data class MaintenanceRecordSyncDto(
    val id: String,
    val scheduleId: String,
    val vehicleId: String,
    val datePerformed: Long,
    val odometerKm: Double,
    val cost: Double?,
    val location: String?,
    val notes: String?,
    val lastModified: Long,
)

fun MaintenanceRecord.toSyncDto() = MaintenanceRecordSyncDto(
    id = id,
    scheduleId = scheduleId,
    vehicleId = vehicleId,
    datePerformed = datePerformed,
    odometerKm = odometerKm,
    cost = cost,
    location = location,
    notes = notes,
    lastModified = lastModified,
)

fun MaintenanceRecordSyncDto.toEntity() = MaintenanceRecord(
    id = id,
    scheduleId = scheduleId,
    vehicleId = vehicleId,
    datePerformed = datePerformed,
    odometerKm = odometerKm,
    cost = cost,
    location = location,
    notes = notes,
    lastModified = lastModified,
)
