package com.roadmate.headunit.ui.parked

import com.roadmate.core.database.entity.OdometerUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.text.NumberFormat
import java.util.Locale

@DisplayName("VehicleSwitcherDialog")
class VehicleSwitcherDialogTest {

    @Nested
    @DisplayName("formatOdometerDisplay")
    inner class FormatOdometerDisplay {

        @Test
        @DisplayName("formats 5000km in KM with US locale")
        fun formatsKm5000() {
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            assertEquals("5,000 km", formatOdometerDisplay(5000.0, OdometerUnit.KM, formatter))
        }

        @Test
        @DisplayName("formats 0km in KM")
        fun formatsKm0() {
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            assertEquals("0 km", formatOdometerDisplay(0.0, OdometerUnit.KM, formatter))
        }

        @Test
        @DisplayName("formats 1000km in MILES")
        fun formatsMiles() {
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            assertEquals("621 mi", formatOdometerDisplay(1000.0, OdometerUnit.MILES, formatter))
        }

        @Test
        @DisplayName("formats 12345km in KM")
        fun formatsKm12345() {
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            assertEquals("12,345 km", formatOdometerDisplay(12345.0, OdometerUnit.KM, formatter))
        }

        @Test
        @DisplayName("formats 999km in KM")
        fun formatsKm999() {
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            assertEquals("999 km", formatOdometerDisplay(999.0, OdometerUnit.KM, formatter))
        }

        @Test
        @DisplayName("formats 100000km in KM")
        fun formatsKm100000() {
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            assertEquals("100,000 km", formatOdometerDisplay(100000.0, OdometerUnit.KM, formatter))
        }

        @Test
        @DisplayName("formats 1609km in MILES")
        fun formatsMiles1609() {
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            assertEquals("1,000 mi", formatOdometerDisplay(1609.344, OdometerUnit.MILES, formatter))
        }
    }
}
