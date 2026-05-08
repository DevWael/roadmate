package com.roadmate.core.database.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [MaintenanceSchedule] entity.
 */
class MaintenanceScheduleTest {

    @Test
    fun `schedule creates with UUID default id`() {
        val schedule = createTestSchedule()
        assertTrue(schedule.id.isNotBlank())
        assertTrue(schedule.id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
    }

    @Test
    fun `two schedules get different default ids`() {
        val s1 = createTestSchedule()
        val s2 = createTestSchedule()
        assertNotEquals(s1.id, s2.id)
    }

    @Test
    fun `schedule preserves all field values`() {
        val schedule = MaintenanceSchedule(
            id = "sched-1",
            vehicleId = "vehicle-1",
            name = "Oil Change",
            intervalKm = 10_000,
            intervalMonths = 6,
            lastServiceKm = 80000.0,
            lastServiceDate = 1700000000000L,
            isCustom = false,
            lastModified = 1000L,
        )

        assertEquals("sched-1", schedule.id)
        assertEquals("vehicle-1", schedule.vehicleId)
        assertEquals("Oil Change", schedule.name)
        assertEquals(10_000, schedule.intervalKm)
        assertEquals(6, schedule.intervalMonths)
        assertEquals(80000.0, schedule.lastServiceKm)
        assertEquals(1700000000000L, schedule.lastServiceDate)
        assertEquals(false, schedule.isCustom)
        assertEquals(1000L, schedule.lastModified)
    }

    @Test
    fun `schedule nullable intervals default to null`() {
        val schedule = createTestSchedule(intervalKm = null, intervalMonths = null)
        assertNull(schedule.intervalKm)
        assertNull(schedule.intervalMonths)
    }

    @Test
    fun `schedule lastModified defaults to current time`() {
        val before = System.currentTimeMillis()
        val schedule = createTestSchedule()
        val after = System.currentTimeMillis()
        assertTrue(schedule.lastModified in before..after)
    }

    private fun createTestSchedule(
        intervalKm: Int? = 10_000,
        intervalMonths: Int? = 6,
    ): MaintenanceSchedule = MaintenanceSchedule(
        vehicleId = "vehicle-1",
        name = "Oil Change",
        intervalKm = intervalKm,
        intervalMonths = intervalMonths,
        lastServiceKm = 80000.0,
        lastServiceDate = System.currentTimeMillis(),
        isCustom = false,
    )
}
