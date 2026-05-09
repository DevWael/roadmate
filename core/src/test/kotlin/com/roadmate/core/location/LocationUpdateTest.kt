package com.roadmate.core.location

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("LocationUpdate")
class LocationUpdateTest {

    @Nested
    @DisplayName("data class")
    inner class DataClass {

        @Test
        @DisplayName("creates update with all fields")
        fun createsWithAllFields() {
            val update = LocationUpdate(
                lat = 37.7749,
                lng = -122.4194,
                speedKmh = 100.0f,
                altitude = 50.0,
                accuracy = 10f,
                timestamp = 1000L,
                isLowAccuracy = false,
            )
            assertEquals(37.7749, update.lat)
            assertEquals(-122.4194, update.lng)
            assertEquals(100.0f, update.speedKmh)
            assertEquals(50.0, update.altitude)
            assertEquals(10f, update.accuracy)
            assertEquals(1000L, update.timestamp)
            assertFalse(update.isLowAccuracy)
        }

        @Test
        @DisplayName("supports low accuracy flag")
        fun supportsLowAccuracyFlag() {
            val update = LocationUpdate(
                lat = 0.0,
                lng = 0.0,
                speedKmh = 0f,
                altitude = 0.0,
                accuracy = 55f,
                timestamp = 0L,
                isLowAccuracy = true,
            )
            assertTrue(update.isLowAccuracy)
        }

        @Test
        @DisplayName("data class equality works")
        fun dataClassEquality() {
            val update1 = LocationUpdate(1.0, 2.0, 3f, 4.0, 5f, 6L, false)
            val update2 = LocationUpdate(1.0, 2.0, 3f, 4.0, 5f, 6L, false)
            assertEquals(update1, update2)
        }
    }
}
