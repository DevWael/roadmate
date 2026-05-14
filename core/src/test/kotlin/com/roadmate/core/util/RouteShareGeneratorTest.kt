package com.roadmate.core.util

import com.roadmate.core.database.entity.TripPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RouteShareGenerator")
class RouteShareGeneratorTest {

    @Nested
    @DisplayName("sampleWaypoints")
    inner class SampleWaypoints {

        @Test
        @DisplayName("returns all points when count <= maxPoints")
        fun returnsAllWhenSmall() {
            val points = listOf(
                tp(lat = 0.0, lng = 0.0),
                tp(lat = 1.0, lng = 1.0),
                tp(lat = 2.0, lng = 2.0),
            )
            val result = RouteShareGenerator.sampleWaypoints(points)
            assertEquals(3, result.size)
            assertEquals(points, result)
        }

        @Test
        @DisplayName("returns single point unchanged")
        fun singlePoint() {
            val points = listOf(tp(lat = 5.0, lng = 5.0))
            val result = RouteShareGenerator.sampleWaypoints(points)
            assertEquals(1, result.size)
            assertEquals(points.first(), result.first())
        }

        @Test
        @DisplayName("returns empty list for empty input")
        fun emptyInput() {
            val result = RouteShareGenerator.sampleWaypoints(emptyList())
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("returns exactly 25 points for 500+ input")
        fun samplesTo25() {
            val points = (0..599).map { i -> tp(lat = i * 0.001, lng = i * 0.001) }
            val result = RouteShareGenerator.sampleWaypoints(points)
            assertEquals(25, result.size)
        }

        @Test
        @DisplayName("first and last points are preserved")
        fun firstAndLastPreserved() {
            val points = (0..499).map { i -> tp(lat = i * 0.01, lng = i * 0.01) }
            val result = RouteShareGenerator.sampleWaypoints(points)
            assertEquals(points.first(), result.first())
            assertEquals(points.last(), result.last())
        }

        @Test
        @DisplayName("intermediate points are evenly distributed")
        fun intermediateEvenlyDistributed() {
            val points = (0..299).map { i -> tp(lat = i * 0.01, lng = i * 0.01) }
            val result = RouteShareGenerator.sampleWaypoints(points)
            assertEquals(25, result.size)
            for (i in 1 until result.size - 1) {
                assertTrue(result[i].latitude > result[i - 1].latitude)
            }
        }

        @Test
        @DisplayName("25 points returns all without sampling")
        fun exactly25NoSampling() {
            val points = (0..24).map { i -> tp(lat = i * 1.0, lng = i * 1.0) }
            val result = RouteShareGenerator.sampleWaypoints(points)
            assertEquals(25, result.size)
            assertEquals(points, result)
        }

        @Test
        @DisplayName("26 points triggers sampling to 25")
        fun twentySixTriggersSampling() {
            val points = (0..25).map { i -> tp(lat = i * 1.0, lng = i * 1.0) }
            val result = RouteShareGenerator.sampleWaypoints(points)
            assertEquals(25, result.size)
            assertEquals(points.first(), result.first())
            assertEquals(points.last(), result.last())
        }
    }

    @Nested
    @DisplayName("generateOsmUrl")
    inner class GenerateOsmUrl {

        @Test
        @DisplayName("builds correct OSM URL with waypoints")
        fun buildsOsmUrl() {
            val points = listOf(
                tp(lat = 37.7749, lng = -122.4194),
                tp(lat = 34.0522, lng = -118.2437),
            )
            val url = RouteShareGenerator.generateOsmUrl(points)
            assertTrue(url.startsWith("https://www.openstreetmap.org/directions?route="))
            assertTrue(url.contains("37.7749,-122.4194"))
            assertTrue(url.contains("34.0522,-118.2437"))
        }

        @Test
        @DisplayName("separates waypoints with semicolons")
        fun semicolonSeparation() {
            val points = listOf(
                tp(lat = 1.0, lng = 2.0),
                tp(lat = 3.0, lng = 4.0),
                tp(lat = 5.0, lng = 6.0),
            )
            val url = RouteShareGenerator.generateOsmUrl(points)
            val routePart = url.substringAfter("route=")
            val coords = routePart.split(";")
            assertEquals(3, coords.size)
        }

        @Test
        @DisplayName("formats lat,lng pairs correctly")
        fun latLngFormat() {
            val points = listOf(tp(lat = 37.5, lng = -122.3))
            val url = RouteShareGenerator.generateOsmUrl(points)
            assertTrue(url.contains("37.5,-122.3"))
        }
    }

    @Nested
    @DisplayName("formatShareText")
    inner class FormatShareText {

        @Test
        @DisplayName("formats text with date, distance, duration, and URL")
        fun formatsShareText() {
            val text = RouteShareGenerator.formatShareText(
                startTimeMs = 1700000000000L,
                distanceKm = 45.5,
                durationMs = 3600000L,
                url = "https://www.openstreetmap.org/directions?route=37.77,-122.41",
            )
            assertTrue(text.startsWith("My trip on"))
            assertTrue(text.contains("45.5 km"))
            assertTrue(text.contains("1:00"))
            assertTrue(text.contains("https://www.openstreetmap.org/directions?route=37.77,-122.41"))
        }

        @Test
        @DisplayName("formats short duration as 0:mm")
        fun shortDuration() {
            val text = RouteShareGenerator.formatShareText(
                startTimeMs = 1700000000000L,
                distanceKm = 5.0,
                durationMs = 900000L,
                url = "https://example.com",
            )
            assertTrue(text.contains("0:15"))
        }

        @Test
        @DisplayName("formats long duration with hours")
        fun longDuration() {
            val text = RouteShareGenerator.formatShareText(
                startTimeMs = 1700000000000L,
                distanceKm = 200.0,
                durationMs = 7200000L,
                url = "https://example.com",
            )
            assertTrue(text.contains("2:00"))
        }
    }

    @Nested
    @DisplayName("generateRouteShare")
    inner class GenerateRouteShare {

        @Test
        @DisplayName("generates full share text from trip points")
        fun fullShareGeneration() {
            val points = listOf(
                tp(lat = 37.7749, lng = -122.4194),
                tp(lat = 37.7849, lng = -122.4094),
            )
            val text = RouteShareGenerator.generateRouteShare(
                points = points,
                startTimeMs = 1700000000000L,
                distanceKm = 10.0,
                durationMs = 1800000L,
            )
            assertTrue(text.startsWith("My trip on"))
            assertTrue(text.contains("10.0 km"))
            assertTrue(text.contains("https://www.openstreetmap.org/directions?route="))
        }

        @Test
        @DisplayName("samples waypoints before generating URL")
        fun samplesBeforeUrl() {
            val points = (0..499).map { i -> tp(lat = i * 0.01, lng = i * 0.01) }
            val text = RouteShareGenerator.generateRouteShare(
                points = points,
                startTimeMs = 1700000000000L,
                distanceKm = 100.0,
                durationMs = 3600000L,
            )
            val urlPart = text.substringAfterLast("— ")
            val coords = urlPart.substringAfter("route=").split(";")
            assertEquals(25, coords.size)
        }
    }

    private fun tp(
        lat: Double = 0.0,
        lng: Double = 0.0,
        tripId: String = "trip-1",
    ) = TripPoint(
        id = "tp-${lat}_$lng",
        tripId = tripId,
        latitude = lat,
        longitude = lng,
        speedKmh = 50.0,
        altitude = 10.0,
        accuracy = 5f,
        timestamp = System.currentTimeMillis(),
    )
}
