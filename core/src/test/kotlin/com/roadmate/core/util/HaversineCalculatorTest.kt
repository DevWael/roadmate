package com.roadmate.core.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.math.abs

@DisplayName("HaversineCalculator")
class HaversineCalculatorTest {

    @Nested
    @DisplayName("haversineDistanceKm")
    inner class HaversineDistanceKm {

        @Test
        @DisplayName("returns 0 for identical points")
        fun identicalPoints() {
            val distance = HaversineCalculator.haversineDistanceKm(37.7749, -122.4194, 37.7749, -122.4194)
            assertEquals(0.0, distance, 0.001)
        }

        @Test
        @DisplayName("calculates distance between San Francisco and Los Angeles")
        fun sanFranciscoToLA() {
            val sf = 37.7749 to -122.4194
            val la = 34.0522 to -118.2437
            val distance = HaversineCalculator.haversineDistanceKm(sf.first, sf.second, la.first, la.second)
            assertTrue(abs(distance - 559.0) < 10.0, "Expected ~559km, got $distance km")
        }

        @Test
        @DisplayName("calculates short distance between nearby points")
        fun shortDistance() {
            val distance = HaversineCalculator.haversineDistanceKm(37.7749, -122.4194, 37.7750, -122.4195)
            assertTrue(distance < 0.02, "Expected very small distance, got $distance km")
            assertTrue(distance > 0.0, "Expected non-zero distance")
        }

        @Test
        @DisplayName("calculates equator to pole distance")
        fun equatorToPole() {
            val distance = HaversineCalculator.haversineDistanceKm(0.0, 0.0, 90.0, 0.0)
            assertTrue(abs(distance - 10007.5) < 10.0, "Expected ~10008km, got $distance km")
        }

        @Test
        @DisplayName("handles east-west distance at equator")
        fun eastWestAtEquator() {
            val distance = HaversineCalculator.haversineDistanceKm(0.0, 0.0, 0.0, 1.0)
            assertTrue(abs(distance - 111.2) < 1.0, "Expected ~111km, got $distance km")
        }

        @Test
        @DisplayName("handles crossing the prime meridian")
        fun crossPrimeMeridian() {
            val distance = HaversineCalculator.haversineDistanceKm(51.5074, -0.1278, 48.8566, 2.3522)
            assertTrue(abs(distance - 343.0) < 5.0, "Expected ~343km (London-Paris), got $distance km")
        }
    }

    @Nested
    @DisplayName("LOW_ACCURACY_THRESHOLD")
    inner class LowAccuracyThreshold {

        @Test
        @DisplayName("threshold is 50 meters")
        fun thresholdValue() {
            assertEquals(50f, HaversineCalculator.LOW_ACCURACY_THRESHOLD)
        }
    }
}
