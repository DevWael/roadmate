package com.roadmate.headunit.ui.driving

import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.headunit.ui.adaptive.DashboardBreakpoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.text.NumberFormat
import java.util.Locale

@DisplayName("CompactDrivingHUD logic")
class CompactDrivingHUDTest {

    @Nested
    @DisplayName("breakpoint-driven visibility")
    inner class BreakpointDrivenVisibility {

        @Test
        fun `Compact breakpoint shows trip distance`() {
            val breakpoint = DashboardBreakpoint.Compact
            assertTrue(breakpoint.showTripDistance, "Compact should show trip distance stacked with ODO")
        }

        @Test
        fun `Narrow breakpoint hides trip distance`() {
            val breakpoint = DashboardBreakpoint.Narrow
            assertFalse(breakpoint.showTripDistance, "Narrow should hide trip distance, showing ODO only")
        }

        @Test
        fun `Compact breakpoint hides time in driving`() {
            val breakpoint = DashboardBreakpoint.Compact
            assertFalse(breakpoint.showTimeInDriving, "Compact HUD should not show time — Full only")
        }

        @Test
        fun `Narrow breakpoint hides time in driving`() {
            val breakpoint = DashboardBreakpoint.Narrow
            assertFalse(breakpoint.showTimeInDriving, "Narrow HUD should not show time")
        }
    }

    @Nested
    @DisplayName("formatOdometer")
    inner class FormatOdometerTests {

        private val formatter = NumberFormat.getNumberInstance(Locale.US)

        @Test
        fun `formats km correctly`() {
            val result = formatOdometer(85500.0, OdometerUnit.KM, formatter)
            assertEquals("85,500 km", result)
        }

        @Test
        fun `formats miles correctly`() {
            val result = formatOdometer(85500.0, OdometerUnit.MILES, formatter)
            assertEquals("53,127 mi", result)
        }

        @Test
        fun `formats zero odometer`() {
            val result = formatOdometer(0.0, OdometerUnit.KM, formatter)
            assertEquals("0 km", result)
        }

        @Test
        fun `formats large odometer`() {
            val result = formatOdometer(500000.0, OdometerUnit.KM, formatter)
            assertEquals("500,000 km", result)
        }
    }
}
