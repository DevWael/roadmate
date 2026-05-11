package com.roadmate.headunit.viewmodel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AddMaintenanceFormState")
class AddMaintenanceFormStateTest {

    @Nested
    @DisplayName("validation")
    inner class Validation {

        @Test
        @DisplayName("valid form has no errors")
        fun validFormNoErrors() {
            val state = validForm().validate()
            assertTrue(state.errors.isEmpty())
        }

        @Test
        @DisplayName("blank name produces required error")
        fun blankNameRequired() {
            val state = validForm().copy(name = "").validate()
            assertEquals("Name is required", state.errors["name"])
        }

        @Test
        @DisplayName("whitespace-only name produces required error")
        fun whitespaceNameRequired() {
            val state = validForm().copy(name = "   ").validate()
            assertEquals("Name is required", state.errors["name"])
        }

        @Test
        @DisplayName("no intervals produces error")
        fun noIntervalsError() {
            val state = validForm().copy(intervalKm = "", intervalMonths = "").validate()
            assertEquals("At least one interval is required", state.errors["interval"])
        }

        @Test
        @DisplayName("km interval only is valid")
        fun kmIntervalOnlyValid() {
            val state = validForm().copy(intervalKm = "5000", intervalMonths = "").validate()
            assertTrue(state.errors.isEmpty())
        }

        @Test
        @DisplayName("month interval only is valid")
        fun monthIntervalOnlyValid() {
            val state = validForm().copy(intervalKm = "", intervalMonths = "6").validate()
            assertTrue(state.errors.isEmpty())
        }

        @Test
        @DisplayName("zero km interval produces error")
        fun zeroKmIntervalError() {
            val state = validForm().copy(intervalKm = "0", intervalMonths = "").validate()
            assertEquals("Must be a positive number", state.errors["intervalKm"])
        }

        @Test
        @DisplayName("negative km interval produces error")
        fun negativeKmIntervalError() {
            val state = validForm().copy(intervalKm = "-1000", intervalMonths = "").validate()
            assertEquals("Must be a positive number", state.errors["intervalKm"])
        }

        @Test
        @DisplayName("zero months interval produces error")
        fun zeroMonthsIntervalError() {
            val state = validForm().copy(intervalKm = "", intervalMonths = "0").validate()
            assertEquals("Must be a positive number", state.errors["intervalMonths"])
        }

        @Test
        @DisplayName("non-numeric km produces error")
        fun nonNumericKmError() {
            val state = validForm().copy(intervalKm = "abc", intervalMonths = "").validate()
            assertEquals("Must be a positive number", state.errors["intervalKm"])
        }

        @Test
        @DisplayName("non-numeric last service km produces error")
        fun nonNumericLastServiceKmError() {
            val state = validForm().copy(lastServiceKm = "xyz").validate()
            assertEquals("Invalid number", state.errors["lastServiceKm"])
        }

        @Test
        @DisplayName("both intervals valid")
        fun bothIntervalsValid() {
            val state = validForm().copy(intervalKm = "5000", intervalMonths = "6").validate()
            assertTrue(state.errors.isEmpty())
        }
    }

    @Nested
    @DisplayName("isSaveEnabled")
    inner class IsSaveEnabled {

        @Test
        @DisplayName("enabled when form is valid")
        fun enabledWhenValid() {
            val state = validForm().validate()
            assertTrue(state.isSaveEnabled)
        }

        @Test
        @DisplayName("disabled when name is blank")
        fun disabledWhenNameBlank() {
            val state = validForm().copy(name = "").validate()
            assertFalse(state.isSaveEnabled)
        }

        @Test
        @DisplayName("disabled when no intervals")
        fun disabledWhenNoIntervals() {
            val state = validForm().copy(intervalKm = "", intervalMonths = "").validate()
            assertFalse(state.isSaveEnabled)
        }

        @Test
        @DisplayName("disabled when validation errors exist")
        fun disabledWhenErrors() {
            val state = validForm().copy(name = "").validate()
            assertFalse(state.isSaveEnabled)
        }
    }

    @Nested
    @DisplayName("toSchedule")
    inner class ToSchedule {

        @Test
        @DisplayName("creates custom schedule with correct fields")
        fun createsCustomSchedule() {
            val state = validForm()
            val schedule = state.toSchedule()

            assertEquals("veh-1", schedule.vehicleId)
            assertEquals("Brake Fluid", schedule.name)
            assertEquals(10000, schedule.intervalKm)
            assertEquals(12, schedule.intervalMonths)
            assertEquals(90000.0, schedule.lastServiceKm, 0.01)
            assertTrue(schedule.isCustom)
        }

        @Test
        @DisplayName("uses provided ID for edit")
        fun usesProvidedId() {
            val state = validForm()
            val schedule = state.toSchedule(existingId = "existing-id")
            assertEquals("existing-id", schedule.id)
        }

        @Test
        @DisplayName("generates new ID when none provided")
        fun generatesNewId() {
            val state = validForm()
            val schedule = state.toSchedule()
            assertNotNull(schedule.id)
            assertTrue(schedule.id.isNotEmpty())
        }

        @Test
        @DisplayName("km only sets month to null")
        fun kmOnlySetsMonthNull() {
            val state = validForm().copy(intervalKm = "5000", intervalMonths = "")
            val schedule = state.toSchedule()
            assertEquals(5000, schedule.intervalKm)
            assertNull(schedule.intervalMonths)
        }

        @Test
        @DisplayName("month only sets km to null")
        fun monthOnlySetsKmNull() {
            val state = validForm().copy(intervalKm = "", intervalMonths = "6")
            val schedule = state.toSchedule()
            assertNull(schedule.intervalKm)
            assertEquals(6, schedule.intervalMonths)
        }

        @Test
        @DisplayName("blank lastServiceKm defaults to vehicleOdometerKm")
        fun blankLastServiceKmDefaults() {
            val state = validForm().copy(lastServiceKm = "", vehicleOdometerKm = 75000.0)
            val schedule = state.toSchedule()
            assertEquals(75000.0, schedule.lastServiceKm, 0.01)
        }

        @Test
        @DisplayName("preserves isCustom from form state")
        fun preservesIsCustom() {
            val customState = validForm().copy(isCustom = true)
            assertTrue(customState.toSchedule().isCustom)

            val templateState = validForm().copy(isCustom = false)
            assertFalse(templateState.toSchedule().isCustom)
        }
    }

    @Nested
    @DisplayName("fromSchedule")
    inner class FromSchedule {

        @Test
        @DisplayName("populates form from existing schedule")
        fun populatesFromSchedule() {
            val schedule = createTestSchedule()
            val state = AddMaintenanceFormState.fromSchedule(schedule, 95000.0)

            assertEquals(schedule.vehicleId, state.vehicleId)
            assertEquals(schedule.name, state.name)
            assertEquals(schedule.intervalKm.toString(), state.intervalKm)
            assertEquals(schedule.intervalMonths.toString(), state.intervalMonths)
            assertEquals(schedule.lastServiceDate, state.lastServiceDate)
        }

        @Test
        @DisplayName("handles null intervals")
        fun handlesNullIntervals() {
            val schedule = createTestSchedule().copy(intervalKm = null, intervalMonths = null)
            val state = AddMaintenanceFormState.fromSchedule(schedule, 95000.0)

            assertEquals("", state.intervalKm)
            assertEquals("", state.intervalMonths)
        }

        @Test
        @DisplayName("preserves fractional km")
        fun preservesFractionalKm() {
            val schedule = createTestSchedule().copy(lastServiceKm = 90000.5)
            val state = AddMaintenanceFormState.fromSchedule(schedule, 95000.0)
            assertEquals("90000.5", state.lastServiceKm)
        }

        @Test
        @DisplayName("formats whole km without decimal")
        fun formatsWholeKm() {
            val schedule = createTestSchedule().copy(lastServiceKm = 90000.0)
            val state = AddMaintenanceFormState.fromSchedule(schedule, 95000.0)
            assertEquals("90000", state.lastServiceKm)
        }

        @Test
        @DisplayName("preserves isCustom from schedule")
        fun preservesIsCustom() {
            val templateSchedule = createTestSchedule().copy(isCustom = false)
            val state = AddMaintenanceFormState.fromSchedule(templateSchedule, 95000.0)
            assertFalse(state.isCustom)

            val customSchedule = createTestSchedule().copy(isCustom = true)
            val customState = AddMaintenanceFormState.fromSchedule(customSchedule, 95000.0)
            assertTrue(customState.isCustom)
        }
    }

    private fun validForm() = AddMaintenanceFormState(
        vehicleId = "veh-1",
        vehicleOdometerKm = 90000.0,
        name = "Brake Fluid",
        intervalKm = "10000",
        intervalMonths = "12",
        lastServiceDate = 1700000000000L,
        lastServiceKm = "90000",
    )

    private fun createTestSchedule() = com.roadmate.core.database.entity.MaintenanceSchedule(
        id = "sched-1",
        vehicleId = "veh-1",
        name = "Oil Change",
        intervalKm = 10000,
        intervalMonths = 6,
        lastServiceKm = 80000.0,
        lastServiceDate = 1600000000000L,
        isCustom = true,
    )
}
