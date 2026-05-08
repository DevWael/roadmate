package com.roadmate.headunit.viewmodel

import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.database.entity.Vehicle

data class VehicleFormState(
    val name: String = "",
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val engineType: EngineType = EngineType.INLINE_4,
    val engineSize: String = "",
    val fuelType: FuelType = FuelType.GASOLINE,
    val plateNumber: String = "",
    val odometerKm: String = "",
    val odometerUnit: OdometerUnit = OdometerUnit.KM,
    val cityConsumption: String = "",
    val highwayConsumption: String = "",
    val templateSelection: TemplateSelection = TemplateSelection.NONE,
    val errors: Map<String, String> = emptyMap(),
)

enum class TemplateSelection {
    NONE,
    MITSUBISHI_LANCER_EX_2015,
    CUSTOM,
}

fun VehicleFormState.validate(): VehicleFormState {
    val errors = mutableMapOf<String, String>()
    if (name.isBlank()) errors["name"] = "Required"
    if (make.isBlank()) errors["make"] = "Required"
    if (model.isBlank()) errors["model"] = "Required"
    val yearNum = year.toIntOrNull()
    if (year.isBlank()) errors["year"] = "Required"
    else if (yearNum == null || yearNum < 1900 || yearNum > 2100) errors["year"] = "Invalid year"
    if (engineSize.isBlank()) errors["engineSize"] = "Required"
    else engineSize.toDoubleOrNull()
        ?.takeIf { it > 0 }
        ?: run { errors["engineSize"] = "Must be positive" }
    if (plateNumber.isBlank()) errors["plateNumber"] = "Required"
    if (odometerKm.isBlank()) errors["odometerKm"] = "Required"
    else odometerKm.toDoubleOrNull()
        ?.takeIf { it >= 0 }
        ?: run { errors["odometerKm"] = "Invalid" }
    if (cityConsumption.isBlank()) errors["cityConsumption"] = "Required"
    else cityConsumption.toDoubleOrNull()
        ?.takeIf { it > 0 }
        ?: run { errors["cityConsumption"] = "Must be positive" }
    if (highwayConsumption.isBlank()) errors["highwayConsumption"] = "Required"
    else highwayConsumption.toDoubleOrNull()
        ?.takeIf { it > 0 }
        ?: run { errors["highwayConsumption"] = "Must be positive" }
    return copy(errors = errors)
}

fun VehicleFormState.isValid(): Boolean = validate().errors.isEmpty()

fun VehicleFormState.toVehicle(): Vehicle? {
    if (!isValid()) return null
    return Vehicle(
        name = name.trim(),
        make = make.trim(),
        model = model.trim(),
        year = year.toInt(),
        engineType = engineType,
        engineSize = engineSize.toDouble(),
        fuelType = fuelType,
        plateNumber = plateNumber.trim(),
        odometerKm = odometerKm.toDouble(),
        odometerUnit = odometerUnit,
        cityConsumption = cityConsumption.toDouble(),
        highwayConsumption = highwayConsumption.toDouble(),
    )
}
