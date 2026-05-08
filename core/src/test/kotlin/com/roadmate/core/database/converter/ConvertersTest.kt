package com.roadmate.core.database.converter

import com.roadmate.core.database.entity.DocumentType
import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.database.entity.TripStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for Room [Converters].
 * Validates round-trip conversion for all enum types and safe fallback for unknown values.
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
    fun `tripStatus round-trip conversion`() {
        TripStatus.entries.forEach { status ->
            val stored = converters.fromTripStatus(status)
            val restored = converters.toTripStatus(stored)
            assertEquals(status, restored, "Round-trip failed for TripStatus.$status")
        }
    }

    @Test
    fun `documentType round-trip conversion`() {
        DocumentType.entries.forEach { type ->
            val stored = converters.fromDocumentType(type)
            val restored = converters.toDocumentType(stored)
            assertEquals(type, restored, "Round-trip failed for DocumentType.$type")
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
    fun `invalid engineType string falls back to OTHER`() {
        assertEquals(EngineType.OTHER, converters.toEngineType("INVALID"))
    }

    @Test
    fun `invalid fuelType string falls back to OTHER`() {
        assertEquals(FuelType.OTHER, converters.toFuelType("INVALID"))
    }

    @Test
    fun `invalid odometerUnit string falls back to KM`() {
        assertEquals(OdometerUnit.KM, converters.toOdometerUnit("INVALID"))
    }

    @Test
    fun `invalid tripStatus string falls back to INTERRUPTED`() {
        assertEquals(TripStatus.INTERRUPTED, converters.toTripStatus("INVALID"))
    }

    @Test
    fun `invalid documentType string falls back to OTHER`() {
        assertEquals(DocumentType.OTHER, converters.toDocumentType("INVALID"))
    }
}
