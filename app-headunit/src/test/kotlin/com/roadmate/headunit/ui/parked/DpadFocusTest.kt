package com.roadmate.headunit.ui.parked

import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.model.DrivingState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.text.NumberFormat
import java.util.Locale

@DisplayName("DpadFocus")
class DpadFocusTest {

    @Nested
    @DisplayName("driving state focus gating")
    inner class DrivingStateFocusGating {

        private fun isFocusEnabled(state: DrivingState): Boolean = state is DrivingState.Idle

        @Test
        @DisplayName("focus enabled when Idle")
        fun focusEnabledWhenIdle() {
            assertTrue(isFocusEnabled(DrivingState.Idle))
        }

        @Test
        @DisplayName("focus disabled when Driving")
        fun focusDisabledWhenDriving() {
            assertFalse(isFocusEnabled(DrivingState.Driving("trip1", 10.0, 60000L)))
        }

        @Test
        @DisplayName("focus disabled when Stopping")
        fun focusDisabledWhenStopping() {
            assertFalse(isFocusEnabled(DrivingState.Stopping(1000L)))
        }

        @Test
        @DisplayName("focus disabled when GapCheck")
        fun focusDisabledWhenGapCheck() {
            assertFalse(isFocusEnabled(DrivingState.GapCheck(5000L)))
        }
    }

    @Nested
    @DisplayName("maintenance gauge data for focus expand")
    inner class GaugeFocusData {

        @Test
        @DisplayName("percentage at 75% when 7500 of 10000 km consumed")
        fun percentageAt75() {
            val schedule = buildSchedule(intervalKm = 10000, lastServiceKm = 0.0)
            assertEquals(75f, maintenancePercentage(schedule, 7500.0))
        }

        @Test
        @DisplayName("percentage clamped to 100 when overdue")
        fun percentageClampedAt100() {
            val schedule = buildSchedule(intervalKm = 10000, lastServiceKm = 0.0)
            assertEquals(100f, maintenancePercentage(schedule, 12000.0))
        }

        @Test
        @DisplayName("percentage is 0 when no interval set")
        fun percentageZeroNoInterval() {
            val schedule = buildSchedule(intervalKm = null, lastServiceKm = 0.0)
            assertEquals(0f, maintenancePercentage(schedule, 5000.0))
        }

        @Test
        @DisplayName("remaining km is positive before threshold")
        fun remainingKmPositive() {
            val schedule = buildSchedule(intervalKm = 10000, lastServiceKm = 0.0)
            assertEquals(2500.0, remainingKm(schedule, 7500.0))
        }

        @Test
        @DisplayName("remaining km clamped to 0 when overdue")
        fun remainingKmZeroWhenOverdue() {
            val schedule = buildSchedule(intervalKm = 10000, lastServiceKm = 0.0)
            assertEquals(0.0, remainingKm(schedule, 12000.0))
        }

        @Test
        @DisplayName("urgency sort returns most consumed first")
        fun urgencySortOrder() {
            val oil = buildSchedule(name = "Oil", intervalKm = 10000, lastServiceKm = 0.0)
            val tires = buildSchedule(name = "Tires", intervalKm = 50000, lastServiceKm = 0.0)
            val brakes = buildSchedule(name = "Brakes", intervalKm = 20000, lastServiceKm = 0.0)
            val sorted = sortSchedulesByUrgency(listOf(oil, tires, brakes), 8000.0)
            assertEquals("Oil", sorted[0].name) // 80%
            assertEquals("Brakes", sorted[1].name) // 40%
            assertEquals("Tires", sorted[2].name) // 16%
        }

        @Test
        @DisplayName("urgency sort caps at 3 items")
        fun urgencySortMaxThree() {
            val items = (1..5).map { buildSchedule(name = "Item$it", intervalKm = 10000, lastServiceKm = 0.0) }
            val sorted = sortSchedulesByUrgency(items, 5000.0)
            assertEquals(3, sorted.size)
        }
    }

    @Nested
    @DisplayName("odometer display formatting")
    inner class OdometerDisplay {

        private val formatter = NumberFormat.getNumberInstance(Locale.US)

        @Test
        @DisplayName("formats km value with locale grouping")
        fun formatsKm() {
            assertEquals("12,345 km", formatOdometerDisplay(12345.0, OdometerUnit.KM, formatter))
        }

        @Test
        @DisplayName("converts and rounds miles correctly")
        fun formatsMiles() {
            assertEquals("621 mi", formatOdometerDisplay(1000.0, OdometerUnit.MILES, formatter))
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
