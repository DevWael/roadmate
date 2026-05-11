package com.roadmate.core.ui.components

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.util.Locale

@DisplayName("MaintenanceCompletionSheet logic")
class MaintenanceCompletionSheetTest {

    @Nested
    @DisplayName("formatDateForDisplay")
    inner class FormatDateForDisplay {

        @Test
        @DisplayName("formats epoch millis to readable date")
        fun formatsEpochMillis() {
            val result = formatDateForDisplay(1700000000000L)
            assertTrue(result.isNotEmpty())
        }

        @Test
        @DisplayName("formats a known date correctly")
        fun formatsKnownDate() {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val timestamp = sdf.parse("15 Nov 2023")?.time ?: 0L
            val result = formatDateForDisplay(timestamp)
            assertTrue(result.contains("2023"))
        }
    }
}
