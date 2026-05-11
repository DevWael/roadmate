package com.roadmate.headunit.ui.driving

import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.database.entity.Vehicle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.text.NumberFormat
import java.util.Locale

@DisplayName("DrivingHUD logic")
class DrivingHUDTest {

    private val formatter = NumberFormat.getNumberInstance(Locale.US)

    @Nested
    @DisplayName("odometer formatting")
    inner class OdometerFormatting {

        @Test
        fun `formats km correctly`() {
            val result = formatOdometer(85500.0, OdometerUnit.KM, formatter)
            assertEquals("85,500 km", result)
        }

        @Test
        fun `formats miles correctly`() {
            val result = formatOdometer(85500.0, OdometerUnit.MILES, formatter)
            assertTrue(result.endsWith(" mi"))
            assertTrue(result.startsWith("53"))
        }

        @Test
        fun `rounds to whole number`() {
            val result = formatOdometer(85500.7, OdometerUnit.KM, formatter)
            assertEquals("85,501 km", result)
        }
    }

    @Nested
    @DisplayName("maintenance alert threshold")
    inner class MaintenanceAlertThreshold {

        @Test
        fun `schedule at 95 percent triggers alert`() {
            val schedule = MaintenanceSchedule(
                id = "s1",
                vehicleId = "v1",
                name = "Oil Change",
                intervalKm = 10000,
                lastServiceKm = 76000.0,
                lastServiceDate = 0L,
                isCustom = false,
            )
            val odometerKm = 85500.0
            val progress = (odometerKm - schedule.lastServiceKm) / (schedule.intervalKm ?: 1)
            assertTrue(progress >= 0.95)
        }

        @Test
        fun `schedule below 95 percent does not trigger`() {
            val schedule = MaintenanceSchedule(
                id = "s1",
                vehicleId = "v1",
                name = "Oil Change",
                intervalKm = 10000,
                lastServiceKm = 76000.0,
                lastServiceDate = 0L,
                isCustom = false,
            )
            val odometerKm = 85000.0
            val progress = (odometerKm - schedule.lastServiceKm) / (schedule.intervalKm ?: 1)
            assertFalse(progress >= 0.95)
        }

        @Test
        fun `schedule with null intervalKm is excluded`() {
            val schedule = MaintenanceSchedule(
                id = "s1",
                vehicleId = "v1",
                name = "Battery Check",
                intervalKm = null,
                lastServiceKm = 0.0,
                lastServiceDate = 0L,
                isCustom = true,
            )
            val odometerKm = 85000.0
            val interval = schedule.intervalKm
            val triggered = interval != null &&
                ((odometerKm - schedule.lastServiceKm) / interval >= 0.95)
            assertFalse(triggered)
        }
    }
}
