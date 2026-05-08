package com.roadmate.core.database.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [FuelLog] entity.
 * Validates default values, field assignments, and data class contract.
 */
class FuelLogTest {

    @Test
    fun `fuelLog creates with UUID default id`() {
        val log = createTestFuelLog()
        assertTrue(log.id.isNotBlank())
        assertTrue(log.id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
    }

    @Test
    fun `two fuelLogs get different default ids`() {
        val l1 = createTestFuelLog()
        val l2 = createTestFuelLog()
        assertNotEquals(l1.id, l2.id)
    }

    @Test
    fun `fuelLog preserves all field values`() {
        val log = FuelLog(
            id = "fuel-1",
            vehicleId = "v-1",
            date = 1000L,
            odometerKm = 86000.0,
            liters = 45.0,
            pricePerLiter = 12.75,
            totalCost = 573.75,
            isFullTank = true,
            station = "Total - Maadi",
            lastModified = 2000L,
        )

        assertEquals("fuel-1", log.id)
        assertEquals("v-1", log.vehicleId)
        assertEquals(1000L, log.date)
        assertEquals(86000.0, log.odometerKm)
        assertEquals(45.0, log.liters)
        assertEquals(12.75, log.pricePerLiter)
        assertEquals(573.75, log.totalCost)
        assertTrue(log.isFullTank)
        assertEquals("Total - Maadi", log.station)
        assertEquals(2000L, log.lastModified)
    }

    @Test
    fun `fuelLog station defaults to null`() {
        val log = createTestFuelLog()
        assertNull(log.station)
    }

    @Test
    fun `fuelLog lastModified defaults to current time`() {
        val before = System.currentTimeMillis()
        val log = createTestFuelLog()
        val after = System.currentTimeMillis()

        assertTrue(log.lastModified in before..after)
    }

    @Test
    fun `fuelLog copy creates modified instance`() {
        val original = createTestFuelLog()
        val modified = original.copy(liters = 50.0, totalCost = 637.50)

        assertEquals(original.id, modified.id)
        assertEquals(50.0, modified.liters)
        assertEquals(637.50, modified.totalCost)
    }

    @Test
    fun `fuelLog equality based on all fields`() {
        val f1 = FuelLog(
            id = "same-id",
            vehicleId = "v-1",
            date = 1000L,
            odometerKm = 86000.0,
            liters = 45.0,
            pricePerLiter = 12.75,
            totalCost = 573.75,
            isFullTank = true,
            station = null,
            lastModified = 1000L,
        )
        val f2 = f1.copy()

        assertEquals(f1, f2)
        assertEquals(f1.hashCode(), f2.hashCode())
    }

    private fun createTestFuelLog(): FuelLog = FuelLog(
        vehicleId = "v-1",
        date = System.currentTimeMillis(),
        odometerKm = 86000.0,
        liters = 45.0,
        pricePerLiter = 12.75,
        totalCost = 573.75,
        isFullTank = true,
    )
}
