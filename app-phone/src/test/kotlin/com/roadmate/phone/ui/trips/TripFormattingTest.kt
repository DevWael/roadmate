package com.roadmate.phone.ui.trips

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

@DisplayName("Trip formatting utilities")
class TripFormattingTest {

    @Nested
    @DisplayName("formatDuration")
    inner class FormatDuration {

        @Test
        fun `formats hours and minutes`() {
            val result = formatDuration(TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(30))
            assertEquals("2:30", result)
        }

        @Test
        fun `formats zero duration`() {
            assertEquals("0:00", formatDuration(0))
        }

        @Test
        fun `formats minutes only`() {
            assertEquals("0:45", formatDuration(TimeUnit.MINUTES.toMillis(45)))
        }

        @Test
        fun `pads single digit minutes`() {
            assertEquals("1:05", formatDuration(TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(5)))
        }

        @Test
        fun `formats large durations`() {
            val ms = TimeUnit.HOURS.toMillis(10) + TimeUnit.MINUTES.toMillis(59)
            assertEquals("10:59", formatDuration(ms))
        }
    }

    @Nested
    @DisplayName("formatCoordinate")
    inner class FormatCoordinate {

        @Test
        fun `formats positive lat and lng`() {
            val result = formatCoordinate(37.7749, 122.4194)
            assertEquals("37.7749\u00b0N, 122.4194\u00b0E", result)
        }

        @Test
        fun `formats negative lat as S`() {
            val result = formatCoordinate(-33.8688, 151.2093)
            assertEquals("33.8688\u00b0S, 151.2093\u00b0E", result)
        }

        @Test
        fun `formats negative lng as W`() {
            val result = formatCoordinate(40.7128, -74.0060)
            assertEquals("40.7128\u00b0N, 74.0060\u00b0W", result)
        }

        @Test
        fun `formats both negative`() {
            val result = formatCoordinate(-33.8688, -151.2093)
            assertEquals("33.8688\u00b0S, 151.2093\u00b0W", result)
        }

        @Test
        fun `formats zero coordinates`() {
            val result = formatCoordinate(0.0, 0.0)
            assertEquals("0.0000\u00b0N, 0.0000\u00b0E", result)
        }
    }

    @Nested
    @DisplayName("formatDate")
    inner class FormatDate {

        @Test
        fun `formats epoch millis to date string`() {
            val result = formatDate(0)
            assert(result.isNotEmpty())
        }
    }

    @Nested
    @DisplayName("formatDateTimeRange")
    inner class FormatDateTimeRange {

        @Test
        fun `formats start and end time range`() {
            val startMs = 1700000000000L
            val endMs = startMs + TimeUnit.HOURS.toMillis(1)
            val result = formatDateTimeRange(startMs, endMs)
            assert(result.contains("–"))
        }

        @Test
        fun `handles null end time`() {
            val startMs = 1700000000000L
            val result = formatDateTimeRange(startMs, null)
            assert(result.contains("—"))
        }
    }
}
