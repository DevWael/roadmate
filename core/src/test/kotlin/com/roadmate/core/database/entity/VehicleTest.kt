package com.roadmate.core.database.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [Vehicle] entity.
 * Validates default values, field assignments, and data class behavior.
 */
class VehicleTest {

    @Test
    fun `vehicle creates with UUID default id`() {
        val vehicle = createTestVehicle()
        assertTrue(vehicle.id.isNotBlank())
        // UUID format: 8-4-4-4-12 hex characters
        assertTrue(vehicle.id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
    }

    @Test
    fun `two vehicles get different default ids`() {
        val v1 = createTestVehicle()
        val v2 = createTestVehicle()
        assertNotEquals(v1.id, v2.id)
    }

    @Test
    fun `vehicle preserves all field values`() {
        val vehicle = Vehicle(
            id = "test-id",
            name = "My Lancer",
            make = "Mitsubishi",
            model = "Lancer EX",
            year = 2015,
            engineType = EngineType.INLINE_4,
            engineSize = 1.6,
            fuelType = FuelType.GASOLINE,
            plateNumber = "ABC-1234",
            vin = "JA32U8FU5FU000001",
            odometerKm = 85000.0,
            odometerUnit = OdometerUnit.KM,
            cityConsumption = 9.5,
            highwayConsumption = 6.8,
            lastModified = 1000L,
        )

        assertEquals("test-id", vehicle.id)
        assertEquals("My Lancer", vehicle.name)
        assertEquals("Mitsubishi", vehicle.make)
        assertEquals("Lancer EX", vehicle.model)
        assertEquals(2015, vehicle.year)
        assertEquals(EngineType.INLINE_4, vehicle.engineType)
        assertEquals(1.6, vehicle.engineSize)
        assertEquals(FuelType.GASOLINE, vehicle.fuelType)
        assertEquals("ABC-1234", vehicle.plateNumber)
        assertEquals("JA32U8FU5FU000001", vehicle.vin)
        assertEquals(85000.0, vehicle.odometerKm)
        assertEquals(OdometerUnit.KM, vehicle.odometerUnit)
        assertEquals(9.5, vehicle.cityConsumption)
        assertEquals(6.8, vehicle.highwayConsumption)
        assertEquals(1000L, vehicle.lastModified)
    }

    @Test
    fun `vehicle vin defaults to null`() {
        val vehicle = createTestVehicle()
        assertNull(vehicle.vin)
    }

    @Test
    fun `vehicle lastModified defaults to current time`() {
        val before = System.currentTimeMillis()
        val vehicle = createTestVehicle()
        val after = System.currentTimeMillis()

        assertTrue(vehicle.lastModified in before..after)
    }

    @Test
    fun `vehicle copy creates modified instance`() {
        val original = createTestVehicle(name = "Original")
        val modified = original.copy(name = "Modified", odometerKm = 90000.0)

        assertEquals("Original", original.name)
        assertEquals("Modified", modified.name)
        assertEquals(90000.0, modified.odometerKm)
        assertEquals(original.id, modified.id)
    }

    @Test
    fun `vehicle equality based on all fields`() {
        val v1 = createTestVehicle(id = "same-id")
        val v2 = v1.copy()

        assertEquals(v1, v2)
        assertEquals(v1.hashCode(), v2.hashCode())
    }

    private fun createTestVehicle(
        id: String? = null,
        name: String = "Test Car",
    ): Vehicle = Vehicle(
        name = name,
        make = "Mitsubishi",
        model = "Lancer EX",
        year = 2015,
        engineType = EngineType.INLINE_4,
        engineSize = 1.6,
        fuelType = FuelType.GASOLINE,
        plateNumber = "TEST-001",
        odometerKm = 85000.0,
        odometerUnit = OdometerUnit.KM,
        cityConsumption = 9.5,
        highwayConsumption = 6.8,
    ).let { if (id != null) it.copy(id = id) else it }
}
