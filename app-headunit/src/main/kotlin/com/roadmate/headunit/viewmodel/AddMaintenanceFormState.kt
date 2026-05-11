package com.roadmate.headunit.viewmodel

import com.roadmate.core.database.entity.MaintenanceSchedule

data class AddMaintenanceFormState(
    val vehicleId: String = "",
    val vehicleOdometerKm: Double = 0.0,
    val name: String = "",
    val intervalKm: String = "",
    val intervalMonths: String = "",
    val lastServiceDate: Long = System.currentTimeMillis(),
    val lastServiceKm: String = "",
    val isCustom: Boolean = true,
    val errors: Map<String, String> = emptyMap(),
) {
    val isSaveEnabled: Boolean
        get() = errors.isEmpty() && name.isNotBlank() &&
            (intervalKm.isNotBlank() || intervalMonths.isNotBlank())

    fun validate(): AddMaintenanceFormState {
        val errors = mutableMapOf<String, String>()

        if (name.isBlank()) {
            errors["name"] = "Name is required"
        }

        val kmValue = intervalKm.toIntOrNull()
        val monthValue = intervalMonths.toIntOrNull()

        val hasKm = intervalKm.isNotBlank()
        val hasMonths = intervalMonths.isNotBlank()

        if (!hasKm && !hasMonths) {
            errors["interval"] = "At least one interval is required"
        }

        if (hasKm && (kmValue == null || kmValue <= 0)) {
            errors["intervalKm"] = "Must be a positive number"
        }

        if (hasMonths && (monthValue == null || monthValue <= 0)) {
            errors["intervalMonths"] = "Must be a positive number"
        }

        if (lastServiceKm.isNotBlank()) {
            val odoValue = lastServiceKm.toDoubleOrNull()
            if (odoValue == null) {
                errors["lastServiceKm"] = "Invalid number"
            }
        }

        return copy(errors = errors)
    }

    fun toSchedule(existingId: String? = null): MaintenanceSchedule {
        val kmValue = intervalKm.toIntOrNull()
        val monthValue = intervalMonths.toIntOrNull()
        val odoValue = lastServiceKm.toDoubleOrNull() ?: vehicleOdometerKm

        return MaintenanceSchedule(
            id = existingId ?: java.util.UUID.randomUUID().toString(),
            vehicleId = vehicleId,
            name = name.trim(),
            intervalKm = if (intervalKm.isNotBlank()) kmValue else null,
            intervalMonths = if (intervalMonths.isNotBlank()) monthValue else null,
            lastServiceKm = odoValue,
            lastServiceDate = lastServiceDate,
            isCustom = isCustom,
        )
    }

    companion object {
        fun fromSchedule(schedule: MaintenanceSchedule, vehicleOdometerKm: Double): AddMaintenanceFormState {
            return AddMaintenanceFormState(
                vehicleId = schedule.vehicleId,
                vehicleOdometerKm = vehicleOdometerKm,
                name = schedule.name,
                intervalKm = schedule.intervalKm?.toString() ?: "",
                intervalMonths = schedule.intervalMonths?.toString() ?: "",
                lastServiceDate = schedule.lastServiceDate,
                lastServiceKm = schedule.lastServiceKm.let { km ->
                    if (km == km.toLong().toDouble()) km.toLong().toString()
                    else km.toString()
                },
                isCustom = schedule.isCustom,
            )
        }
    }
}
