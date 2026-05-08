package com.roadmate.core.database.converter

import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.OdometerUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for Room [Converters].
 * Validates round-trip conversion for all enum types.
 */
class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `engineType round-trip conversion`() {
        EngineType.entries.forEach { type ->
            val stored = converters.fromEngineType(type)
            val restored = converters.toEngineType(stored)
            assertEquals(type, restored, "Round-trip failed for EngineType.$type")
        }
    }

    @Test
    fun `fuelType round-trip conversion`() {
        FuelType.entries.forEach { type ->
            val stored = converters.fromFuelType(type)
            val restored = converters.toFuelType(stored)
            assertEquals(type, restored, "Round-trip failed for FuelType.$type")
        }
    }

    @Test
    fun `odometerUnit round-trip conversion`() {
        OdometerUnit.entries.forEach { unit ->
            val stored = converters.fromOdometerUnit(unit)
            val restored = converters.toOdometerUnit(stored)
            assertEquals(unit, restored, "Round-trip failed for OdometerUnit.$unit")
        }
    }

    @Test
    fun `engineType stores name string`() {
        assertEquals("INLINE_4", converters.fromEngineType(EngineType.INLINE_4))
        assertEquals("ELECTRIC", converters.fromEngineType(EngineType.ELECTRIC))
    }

    @Test
    fun `fuelType stores name string`() {
        assertEquals("GASOLINE", converters.fromFuelType(FuelType.GASOLINE))
        assertEquals("DIESEL", converters.fromFuelType(FuelType.DIESEL))
    }

    @Test
    fun `odometerUnit stores name string`() {
        assertEquals("KM", converters.fromOdometerUnit(OdometerUnit.KM))
        assertEquals("MILES", converters.fromOdometerUnit(OdometerUnit.MILES))
    }

    @Test
    fun `invalid engineType string throws exception`() {
        assertThrows<IllegalArgumentException> {
            converters.toEngineType("INVALID")
        }
    }

    @Test
    fun `invalid fuelType string throws exception`() {
        assertThrows<IllegalArgumentException> {
            converters.toFuelType("INVALID")
        }
    }

    @Test
    fun `invalid odometerUnit string throws exception`() {
        assertThrows<IllegalArgumentException> {
            converters.toOdometerUnit("INVALID")
        }
    }
}
