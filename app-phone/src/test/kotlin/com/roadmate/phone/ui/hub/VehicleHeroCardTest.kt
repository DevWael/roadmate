package com.roadmate.phone.ui.hub

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

@DisplayName("VehicleHeroCard utilities")
class VehicleHeroCardTest {

    @Nested
    @DisplayName("formatOdometer")
    inner class FormatOdometer {

        @Test
        fun `formats odometer with km unit`() {
            assertEquals("90,000 km", formatOdometer(90000.0, "km"))
        }

        @Test
        fun `formats small odometer value`() {
            assertEquals("500 km", formatOdometer(500.0, "km"))
        }

        @Test
        fun `formats odometer with miles unit`() {
            assertEquals("55,923 mi", formatOdometer(55923.0, "mi"))
        }
    }

    @Nested
    @DisplayName("formatSyncStatus")
    inner class FormatSyncStatus {

        @Test
        fun `shows not yet synced when timestamp is zero`() {
            assertEquals("Not yet synced", formatSyncStatus(0L))
        }

        @Test
        fun `shows just now for recent sync`() {
            val now = System.currentTimeMillis()
            val twoMinAgo = now - TimeUnit.MINUTES.toMillis(2)
            assertEquals("Last synced: just now", formatSyncStatus(twoMinAgo))
        }

        @Test
        fun `shows minutes ago for recent sync`() {
            val now = System.currentTimeMillis()
            val thirtyMinAgo = now - TimeUnit.MINUTES.toMillis(30)
            assertEquals("Last synced: 30 min ago", formatSyncStatus(thirtyMinAgo))
        }

        @Test
        fun `shows hours ago for older sync`() {
            val now = System.currentTimeMillis()
            val threeHoursAgo = now - TimeUnit.HOURS.toMillis(3)
            assertEquals("Last synced: 3 hours ago", formatSyncStatus(threeHoursAgo))
        }

        @Test
        fun `shows singular hour for exactly 1 hour`() {
            val now = System.currentTimeMillis()
            val oneHourAgo = now - TimeUnit.HOURS.toMillis(1)
            assertEquals("Last synced: 1 hour ago", formatSyncStatus(oneHourAgo))
        }
    }

    @Nested
    @DisplayName("syncStatusColor")
    inner class SyncStatusColor {

        @Test
        fun `returns tertiary color for never synced`() {
            val color = syncStatusColor(0L)
            assertFalse(color.equals(androidx.compose.ui.graphics.Color.Unspecified))
        }

        @Test
        fun `returns unspecified color for synced`() {
            val now = System.currentTimeMillis()
            val color = syncStatusColor(now)
            assertTrue(color.equals(androidx.compose.ui.graphics.Color.Unspecified))
        }
    }
}
