package com.roadmate.headunit.ui.parked

import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.database.entity.Vehicle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ParkedDashboard")
class ParkedDashboardTest {

    @Nested
    @DisplayName("maintenancePercentage")
    inner class MaintenancePercentage {

        @Test
        @DisplayName("returns 0 when interval is null")
        fun intervalNull() {
            val schedule = buildSchedule(intervalKm = null, lastServiceKm = 1000.0)
            assertEquals(0f, maintenancePercentage(schedule, 5000.0))
        }

        @Test
        @DisplayName("returns 0 when interval is zero")
        fun intervalZero() {
            val schedule = buildSchedule(intervalKm = 0, lastServiceKm = 1000.0)
            assertEquals(0f, maintenancePercentage(schedule, 5000.0))
        }

        @Test
        @DisplayName("calculates percentage correctly")
        fun calculatesCorrectly() {
            val schedule = buildSchedule(intervalKm = 10000, lastServiceKm = 0.0)
            assertEquals(50f, maintenancePercentage(schedule, 5000.0))
        }

        @Test
        @DisplayName("clamps to 100 when overdue")
        fun clampsTo100() {
            val schedule = buildSchedule(intervalKm = 5000, lastServiceKm = 0.0)
            assertEquals(100f, maintenancePercentage(schedule, 6000.0))
        }

        @Test
        @DisplayName("returns 0 when odometer below lastServiceKm")
        fun belowLastService() {
            val schedule = buildSchedule(intervalKm = 10000, lastServiceKm = 5000.0)
            assertEquals(0f, maintenancePercentage(schedule, 3000.0))
        }
    }

    @Nested
    @DisplayName("sortSchedulesByUrgency")
    inner class SortSchedulesByUrgency {

        @Test
        @DisplayName("sorts by percentage descending")
        fun sortsByPercentage() {
            val low = buildSchedule(name = "Oil", intervalKm = 10000, lastServiceKm = 0.0)
            val high = buildSchedule(name = "Brake", intervalKm = 5000, lastServiceKm = 0.0)
            val result = sortSchedulesByUrgency(listOf(low, high), 3000.0)
            assertEquals("Brake", result[0].name)
            assertEquals("Oil", result[1].name)
        }

        @Test
        @DisplayName("limits to 3 items")
        fun limitsTo3() {
            val schedules = (1..5).map { i ->
                buildSchedule(
                    name = "Item$i",
                    intervalKm = 10000,
                    lastServiceKm = 0.0,
                )
            }
            val result = sortSchedulesByUrgency(schedules, 5000.0)
            assertEquals(3, result.size)
        }

        @Test
        @DisplayName("returns empty for empty input")
        fun emptyInput() {
            val result = sortSchedulesByUrgency(emptyList(), 5000.0)
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("filters out schedules with null or zero interval")
        fun filtersNullInterval() {
            val nullInterval = buildSchedule(name = "Null", intervalKm = null, lastServiceKm = 0.0)
            val zeroInterval = buildSchedule(name = "Zero", intervalKm = 0, lastServiceKm = 0.0)
            val valid = buildSchedule(name = "Valid", intervalKm = 10000, lastServiceKm = 0.0)
            val result = sortSchedulesByUrgency(listOf(nullInterval, zeroInterval, valid), 5000.0)
            assertEquals(1, result.size)
            assertEquals("Valid", result[0].name)
        }
    }

    @Nested
    @DisplayName("remainingKm")
    inner class RemainingKm {

        @Test
        @DisplayName("calculates remaining km correctly")
        fun calculatesRemaining() {
            val schedule = buildSchedule(intervalKm = 10000, lastServiceKm = 0.0)
            assertEquals(5000.0, remainingKm(schedule, 5000.0))
        }

        @Test
        @DisplayName("returns 0 when overdue")
        fun overdue() {
            val schedule = buildSchedule(intervalKm = 5000, lastServiceKm = 0.0)
            assertEquals(0.0, remainingKm(schedule, 6000.0))
        }

        @Test
        @DisplayName("returns 0 for null interval")
        fun nullInterval() {
            val schedule = buildSchedule(intervalKm = null, lastServiceKm = 0.0)
            assertEquals(0.0, remainingKm(schedule, 5000.0))
        }
    }

    private fun buildSchedule(
        name: String = "Test",
        intervalKm: Int? = 10000,
        lastServiceKm: Double = 0.0,
    ) = MaintenanceSchedule(
        vehicleId = "v1",
        name = name,
        intervalKm = intervalKm,
        lastServiceKm = lastServiceKm,
        lastServiceDate = 0L,
        isCustom = false,
    )
}
