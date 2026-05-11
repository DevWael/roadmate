package com.roadmate.headunit.viewmodel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("MaintenanceCompletionFormState")
class MaintenanceCompletionFormStateTest {

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
        @DisplayName("blank odometer produces required error")
        fun blankOdometerRequired() {
            val state = validForm().copy(odometerKm = "").validate()
            assertEquals("Odometer reading is required", state.errors["odometerKm"])
        }

        @Test
        @DisplayName("non-numeric odometer produces invalid error")
        fun nonNumericOdometer() {
            val state = validForm().copy(odometerKm = "abc").validate()
            assertEquals("Invalid number", state.errors["odometerKm"])
        }

        @Test
        @DisplayName("odometer less than vehicle ODO produces error")
        fun odometerLessThanVehicle() {
            val state = validForm().copy(
                vehicleOdometerKm = 90000.0,
                odometerKm = "85000",
            ).validate()
            assertTrue(state.errors["odometerKm"]!!.contains("current odometer"))
        }

        @Test
        @DisplayName("odometer equal to vehicle ODO is valid")
        fun odometerEqualToVehicle() {
            val state = validForm().copy(
                vehicleOdometerKm = 90000.0,
                odometerKm = "90000",
            ).validate()
            assertTrue(state.errors.isEmpty())
        }

        @Test
        @DisplayName("odometer greater than vehicle ODO is valid")
        fun odometerGreaterThanVehicle() {
            val state = validForm().copy(
                vehicleOdometerKm = 90000.0,
                odometerKm = "95000",
            ).validate()
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
        @DisplayName("disabled when odometer is blank")
        fun disabledWhenBlank() {
            val state = validForm().copy(odometerKm = "")
            assertTrue(!state.isSaveEnabled)
        }

        @Test
        @DisplayName("disabled when validation errors exist")
        fun disabledWhenErrors() {
            val state = validForm().copy(odometerKm = "abc").validate()
            assertTrue(!state.isSaveEnabled)
        }
    }

    @Nested
    @DisplayName("toRecord")
    inner class ToRecord {

        @Test
        @DisplayName("maps all fields correctly")
        fun mapsFields() {
            val state = validForm()
            val record = state.toRecord()
            assertEquals("sched-1", record.scheduleId)
            assertEquals("veh-1", record.vehicleId)
            assertEquals(95000.0, record.odometerKm, 0.01)
            assertEquals(50.0, record.cost!!, 0.01)
            assertEquals("Garage A", record.location)
            assertEquals("Synthetic oil", record.notes)
        }

        @Test
        @DisplayName("blank cost maps to null")
        fun blankCostToNull() {
            val state = validForm().copy(cost = "")
            val record = state.toRecord()
            assertEquals(null, record.cost)
        }

        @Test
        @DisplayName("blank location maps to null")
        fun blankLocationToNull() {
            val state = validForm().copy(location = "")
            val record = state.toRecord()
            assertEquals(null, record.location)
        }

        @Test
        @DisplayName("blank notes maps to null")
        fun blankNotesToNull() {
            val state = validForm().copy(notes = "")
            val record = state.toRecord()
            assertEquals(null, record.notes)
        }
    }

    @Nested
    @DisplayName("updatedSchedule")
    inner class UpdatedSchedule {

        @Test
        @DisplayName("updates lastServiceKm and lastServiceDate")
        fun updatesScheduleFields() {
            val schedule = createTestSchedule()
            val state = validForm()
            val updated = state.updatedSchedule(schedule)
            assertEquals(95000.0, updated.lastServiceKm, 0.01)
            assertEquals(state.datePerformed, updated.lastServiceDate)
        }

        @Test
        @DisplayName("preserves other schedule fields")
        fun preservesOtherFields() {
            val schedule = createTestSchedule()
            val state = validForm()
            val updated = state.updatedSchedule(schedule)
            assertEquals(schedule.id, updated.id)
            assertEquals(schedule.vehicleId, updated.vehicleId)
            assertEquals(schedule.name, updated.name)
            assertEquals(schedule.intervalKm, updated.intervalKm)
            assertEquals(schedule.intervalMonths, updated.intervalMonths)
            assertEquals(schedule.isCustom, updated.isCustom)
        }
    }

    private fun validForm() = MaintenanceCompletionFormState(
        scheduleId = "sched-1",
        vehicleId = "veh-1",
        vehicleOdometerKm = 90000.0,
        datePerformed = 1700000000000L,
        odometerKm = "95000",
        cost = "50",
        location = "Garage A",
        notes = "Synthetic oil",
    )

    private fun createTestSchedule() = com.roadmate.core.database.entity.MaintenanceSchedule(
        id = "sched-1",
        vehicleId = "veh-1",
        name = "Oil Change",
        intervalKm = 10000,
        intervalMonths = 6,
        lastServiceKm = 80000.0,
        lastServiceDate = 1600000000000L,
        isCustom = false,
    )
}
