package com.roadmate.headunit.viewmodel

import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule

data class MaintenanceCompletionFormState(
    val scheduleId: String = "",
    val vehicleId: String = "",
    val vehicleOdometerKm: Double = 0.0,
    val datePerformed: Long = System.currentTimeMillis(),
    val odometerKm: String = "",
    val cost: String = "",
    val location: String = "",
    val notes: String = "",
    val errors: Map<String, String> = emptyMap(),
) {
    val isSaveEnabled: Boolean
        get() = errors.isEmpty() && odometerKm.isNotBlank()

    fun validate(): MaintenanceCompletionFormState {
        val errors = mutableMapOf<String, String>()
        val odoValue = odometerKm.toDoubleOrNull()
        when {
            odometerKm.isBlank() -> errors["odometerKm"] = "Odometer reading is required"
            odoValue == null -> errors["odometerKm"] = "Invalid number"
            odoValue < vehicleOdometerKm -> errors["odometerKm"] =
                "Must be ≥ current odometer (${vehicleOdometerKm.toLong()} km)"
        }
        return copy(errors = errors)
    }

    fun toRecord(): MaintenanceRecord {
        val odoValue = requireNotNull(odometerKm.toDoubleOrNull()) {
            "toRecord() called with invalid odometerKm='$odometerKm'. Call validate() first."
        }
        return MaintenanceRecord(
            scheduleId = scheduleId,
            vehicleId = vehicleId,
            datePerformed = datePerformed,
            odometerKm = odoValue,
            cost = cost.toDoubleOrNull(),
            location = location.ifBlank { null },
            notes = notes.ifBlank { null },
        )
    }

    fun updatedSchedule(schedule: MaintenanceSchedule): MaintenanceSchedule {
        val odoValue = requireNotNull(odometerKm.toDoubleOrNull()) {
            "updatedSchedule() called with invalid odometerKm='$odometerKm'. Call validate() first."
        }
        return schedule.copy(
            lastServiceKm = odoValue,
            lastServiceDate = datePerformed,
        )
    }
}
