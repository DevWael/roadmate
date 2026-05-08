package com.roadmate.core.database.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [TripPoint] entity.
 * Validates default values, field assignments, and data class contract.
 */
class TripPointTest {

    @Test
    fun `tripPoint creates with UUID default id`() {
        val point = createTestTripPoint()
        assertTrue(point.id.isNotBlank())
        assertTrue(point.id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
    }

    @Test
    fun `two tripPoints get different default ids`() {
        val p1 = createTestTripPoint()
        val p2 = createTestTripPoint()
        assertNotEquals(p1.id, p2.id)
    }

    @Test
    fun `tripPoint preserves all field values`() {
        val point = TripPoint(
            id = "tp-1",
            tripId = "trip-1",
            latitude = 30.0444,
            longitude = 31.2357,
            speedKmh = 60.0,
            altitude = 75.0,
            accuracy = 5.0f,
            timestamp = 1000L,
            lastModified = 2000L,
        )

        assertEquals("tp-1", point.id)
        assertEquals("trip-1", point.tripId)
        assertEquals(30.0444, point.latitude)
        assertEquals(31.2357, point.longitude)
        assertEquals(60.0, point.speedKmh)
        assertEquals(75.0, point.altitude)
        assertEquals(5.0f, point.accuracy)
        assertEquals(1000L, point.timestamp)
        assertEquals(2000L, point.lastModified)
    }

    @Test
    fun `tripPoint lastModified defaults to current time`() {
        val before = System.currentTimeMillis()
        val point = createTestTripPoint()
        val after = System.currentTimeMillis()

        assertTrue(point.lastModified in before..after)
    }

    @Test
    fun `tripPoint copy creates modified instance`() {
        val original = createTestTripPoint()
        val modified = original.copy(speedKmh = 120.0, altitude = 150.0)

        assertEquals(original.id, modified.id)
        assertEquals(120.0, modified.speedKmh)
        assertEquals(150.0, modified.altitude)
    }

    @Test
    fun `tripPoint equality based on all fields`() {
        val p1 = TripPoint(
            id = "same-id",
            tripId = "trip-1",
            latitude = 30.0,
            longitude = 31.0,
            speedKmh = 60.0,
            altitude = 75.0,
            accuracy = 5.0f,
            timestamp = 1000L,
            lastModified = 1000L,
        )
        val p2 = p1.copy()

        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
    }

    private fun createTestTripPoint(): TripPoint = TripPoint(
        tripId = "trip-1",
        latitude = 30.0444,
        longitude = 31.2357,
        speedKmh = 60.0,
        altitude = 75.0,
        accuracy = 5.0f,
        timestamp = System.currentTimeMillis(),
    )
}
