package com.roadmate.core.database.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [Trip] entity.
 * Validates default values, field assignments, enum behavior, and data class contract.
 */
class TripTest {

    @Test
    fun `trip creates with UUID default id`() {
        val trip = createTestTrip()
        assertTrue(trip.id.isNotBlank())
        assertTrue(trip.id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
    }

    @Test
    fun `two trips get different default ids`() {
        val t1 = createTestTrip()
        val t2 = createTestTrip()
        assertNotEquals(t1.id, t2.id)
    }

    @Test
    fun `trip preserves all field values`() {
        val trip = Trip(
            id = "trip-1",
            vehicleId = "v-1",
            startTime = 1000L,
            endTime = 2000L,
            distanceKm = 45.5,
            durationMs = 1800000L,
            maxSpeedKmh = 120.0,
            avgSpeedKmh = 65.0,
            estimatedFuelL = 4.2,
            startOdometerKm = 85000.0,
            endOdometerKm = 85045.5,
            status = TripStatus.COMPLETED,
            lastModified = 3000L,
        )

        assertEquals("trip-1", trip.id)
        assertEquals("v-1", trip.vehicleId)
        assertEquals(1000L, trip.startTime)
        assertEquals(2000L, trip.endTime)
        assertEquals(45.5, trip.distanceKm)
        assertEquals(1800000L, trip.durationMs)
        assertEquals(120.0, trip.maxSpeedKmh)
        assertEquals(65.0, trip.avgSpeedKmh)
        assertEquals(4.2, trip.estimatedFuelL)
        assertEquals(85000.0, trip.startOdometerKm)
        assertEquals(85045.5, trip.endOdometerKm)
        assertEquals(TripStatus.COMPLETED, trip.status)
        assertEquals(3000L, trip.lastModified)
    }

    @Test
    fun `trip endTime defaults to null`() {
        val trip = createTestTrip()
        assertNull(trip.endTime)
    }

    @Test
    fun `trip lastModified defaults to current time`() {
        val before = System.currentTimeMillis()
        val trip = createTestTrip()
        val after = System.currentTimeMillis()

        assertTrue(trip.lastModified in before..after)
    }

    @Test
    fun `trip status enum has expected values`() {
        val values = TripStatus.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(TripStatus.ACTIVE))
        assertTrue(values.contains(TripStatus.COMPLETED))
        assertTrue(values.contains(TripStatus.INTERRUPTED))
    }

    @Test
    fun `trip copy creates modified instance`() {
        val original = createTestTrip()
        val modified = original.copy(distanceKm = 100.0, status = TripStatus.COMPLETED)

        assertEquals(original.id, modified.id)
        assertEquals(100.0, modified.distanceKm)
        assertEquals(TripStatus.COMPLETED, modified.status)
    }

    @Test
    fun `trip equality based on all fields`() {
        val t1 = Trip(
            id = "same-id",
            vehicleId = "v-1",
            startTime = 1000L,
            endTime = null,
            distanceKm = 0.0,
            durationMs = 0L,
            maxSpeedKmh = 0.0,
            avgSpeedKmh = 0.0,
            estimatedFuelL = 0.0,
            startOdometerKm = 85000.0,
            endOdometerKm = 85000.0,
            status = TripStatus.ACTIVE,
            lastModified = 1000L,
        )
        val t2 = t1.copy()

        assertEquals(t1, t2)
        assertEquals(t1.hashCode(), t2.hashCode())
    }

    private fun createTestTrip(): Trip = Trip(
        vehicleId = "v-1",
        startTime = System.currentTimeMillis(),
        endTime = null,
        distanceKm = 0.0,
        durationMs = 0L,
        maxSpeedKmh = 0.0,
        avgSpeedKmh = 0.0,
        estimatedFuelL = 0.0,
        startOdometerKm = 85000.0,
        endOdometerKm = 85000.0,
        status = TripStatus.ACTIVE,
    )
}
