package com.roadmate.core.database.template

import com.roadmate.core.database.entity.MaintenanceSchedule

/**
 * Pre-built maintenance schedule templates for common vehicle models.
 *
 * Templates provide default maintenance items with manufacturer-recommended
 * intervals. Users can customize or add to these after initial setup.
 */
object MaintenanceTemplates {

    /**
     * Creates the maintenance schedule template for a Mitsubishi Lancer EX 2015.
     *
     * Contains 9 standard maintenance items with km and/or month intervals
     * based on manufacturer recommendations.
     *
     * @param vehicleId The ID of the vehicle to associate schedules with.
     * @param currentOdometerKm The vehicle's current odometer reading for initial lastServiceKm.
     * @param currentDate The current date as epoch millis for initial lastServiceDate.
     * @return List of [MaintenanceSchedule] items for the template.
     */
    fun mitsubishiLancerEx2015(
        vehicleId: String,
        currentOdometerKm: Double = 0.0,
        currentDate: Long = System.currentTimeMillis(),
    ): List<MaintenanceSchedule> = listOf(
        MaintenanceSchedule(
            vehicleId = vehicleId,
            name = "Oil Change",
            intervalKm = 10_000,
            intervalMonths = 6,
            lastServiceKm = currentOdometerKm,
            lastServiceDate = currentDate,
            isCustom = false,
        ),
        MaintenanceSchedule(
            vehicleId = vehicleId,
            name = "Oil Filter",
            intervalKm = 10_000,
            intervalMonths = 6,
            lastServiceKm = currentOdometerKm,
            lastServiceDate = currentDate,
            isCustom = false,
        ),
        MaintenanceSchedule(
            vehicleId = vehicleId,
            name = "Air Filter",
            intervalKm = 20_000,
            intervalMonths = 12,
            lastServiceKm = currentOdometerKm,
            lastServiceDate = currentDate,
            isCustom = false,
        ),
        MaintenanceSchedule(
            vehicleId = vehicleId,
            name = "Brake Pads",
            intervalKm = 40_000,
            intervalMonths = 24,
            lastServiceKm = currentOdometerKm,
            lastServiceDate = currentDate,
            isCustom = false,
        ),
        MaintenanceSchedule(
            vehicleId = vehicleId,
            name = "Tire Rotation",
            intervalKm = 10_000,
            intervalMonths = 6,
            lastServiceKm = currentOdometerKm,
            lastServiceDate = currentDate,
            isCustom = false,
        ),
        MaintenanceSchedule(
            vehicleId = vehicleId,
            name = "Coolant",
            intervalKm = 40_000,
            intervalMonths = 24,
            lastServiceKm = currentOdometerKm,
            lastServiceDate = currentDate,
            isCustom = false,
        ),
        MaintenanceSchedule(
            vehicleId = vehicleId,
            name = "Spark Plugs",
            intervalKm = 30_000,
            intervalMonths = null,
            lastServiceKm = currentOdometerKm,
            lastServiceDate = currentDate,
            isCustom = false,
        ),
        MaintenanceSchedule(
            vehicleId = vehicleId,
            name = "Transmission Fluid",
            intervalKm = 60_000,
            intervalMonths = null,
            lastServiceKm = currentOdometerKm,
            lastServiceDate = currentDate,
            isCustom = false,
        ),
        MaintenanceSchedule(
            vehicleId = vehicleId,
            name = "Brake Fluid",
            intervalKm = 40_000,
            intervalMonths = 24,
            lastServiceKm = currentOdometerKm,
            lastServiceDate = currentDate,
            isCustom = false,
        ),
    )
}
