package com.roadmate.headunit.ui.parked

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("TripListSection")
class TripListSectionTest {

    @Nested
    @DisplayName("formatDuration")
    inner class FormatDuration {

        @ParameterizedTest(name = "{0}ms formats to \"{1}\"")
        @CsvSource(
            "0, 0:00",
            "30_000, 0:00",
            "60_000, 0:01",
            "90_000, 0:01",
            "120_000, 0:02",
            "3_600_000, 1:00",
            "3_660_000, 1:01",
            "7_326_000, 2:02",
            "36_000_000, 10:00",
        )
        fun formatsCorrectly(durationMs: Long, expected: String) {
            assertEquals(expected, formatDuration(durationMs))
        }

        @Test
        @DisplayName("handles zero duration")
        fun zeroDuration() {
            assertEquals("0:00", formatDuration(0L))
        }
    }
}
