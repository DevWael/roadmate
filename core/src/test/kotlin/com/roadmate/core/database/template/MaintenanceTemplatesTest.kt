package com.roadmate.core.database.template

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [MaintenanceTemplates].
 * Validates the Mitsubishi Lancer EX 2015 template against AC #7.
 */
class MaintenanceTemplatesTest {

    private val vehicleId = "test-vehicle-id"
    private val odometerKm = 85000.0
    private val date = 1700000000000L

    private val template = MaintenanceTemplates.mitsubishiLancerEx2015(
        vehicleId = vehicleId,
        currentOdometerKm = odometerKm,
        currentDate = date,
    )

    @Test
    fun `template contains exactly 9 items`() {
        assertEquals(9, template.size)
    }

    @Test
    fun `all items reference correct vehicleId`() {
        template.forEach { schedule ->
            assertEquals(vehicleId, schedule.vehicleId, "Wrong vehicleId for ${schedule.name}")
        }
    }

    @Test
    fun `all items are not custom`() {
        template.forEach { schedule ->
            assertFalse(schedule.isCustom, "${schedule.name} should not be custom")
        }
    }

    @Test
    fun `all items have unique ids`() {
        val ids = template.map { it.id }.toSet()
        assertEquals(9, ids.size, "IDs should be unique")
    }

    @Test
    fun `all items have correct initial service values`() {
        template.forEach { schedule ->
            assertEquals(odometerKm, schedule.lastServiceKm, "${schedule.name} lastServiceKm")
            assertEquals(date, schedule.lastServiceDate, "${schedule.name} lastServiceDate")
        }
    }

    @Test
    fun `oil change has correct intervals`() {
        val item = template.first { it.name == "Oil Change" }
        assertEquals(10_000, item.intervalKm)
        assertEquals(6, item.intervalMonths)
    }

    @Test
    fun `oil filter has correct intervals`() {
        val item = template.first { it.name == "Oil Filter" }
        assertEquals(10_000, item.intervalKm)
        assertEquals(6, item.intervalMonths)
    }

    @Test
    fun `air filter has correct intervals`() {
        val item = template.first { it.name == "Air Filter" }
        assertEquals(20_000, item.intervalKm)
        assertEquals(12, item.intervalMonths)
    }

    @Test
    fun `brake pads have correct intervals`() {
        val item = template.first { it.name == "Brake Pads" }
        assertEquals(40_000, item.intervalKm)
        assertEquals(24, item.intervalMonths)
    }

    @Test
    fun `tire rotation has correct intervals`() {
        val item = template.first { it.name == "Tire Rotation" }
        assertEquals(10_000, item.intervalKm)
        assertEquals(6, item.intervalMonths)
    }

    @Test
    fun `coolant has correct intervals`() {
        val item = template.first { it.name == "Coolant" }
        assertEquals(40_000, item.intervalKm)
        assertEquals(24, item.intervalMonths)
    }

    @Test
    fun `spark plugs have km interval only`() {
        val item = template.first { it.name == "Spark Plugs" }
        assertEquals(30_000, item.intervalKm)
        assertNull(item.intervalMonths)
    }

    @Test
    fun `transmission fluid has km interval only`() {
        val item = template.first { it.name == "Transmission Fluid" }
        assertEquals(60_000, item.intervalKm)
        assertNull(item.intervalMonths)
    }

    @Test
    fun `brake fluid has correct intervals`() {
        val item = template.first { it.name == "Brake Fluid" }
        assertEquals(40_000, item.intervalKm)
        assertEquals(24, item.intervalMonths)
    }

    @Test
    fun `template names match specification`() {
        val expectedNames = listOf(
            "Oil Change",
            "Oil Filter",
            "Air Filter",
            "Brake Pads",
            "Tire Rotation",
            "Coolant",
            "Spark Plugs",
            "Transmission Fluid",
            "Brake Fluid",
        )
        val actualNames = template.map { it.name }
        assertTrue(actualNames.containsAll(expectedNames), "Missing template items")
        assertTrue(expectedNames.containsAll(actualNames), "Extra template items")
    }
}
