package com.roadmate.headunit.ui

import com.roadmate.headunit.ui.adaptive.DashboardBreakpoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AdaptiveDashboard logic")
class AdaptiveDashboardTest {

    @Nested
    @DisplayName("breakpoint classification")
    inner class BreakpointClassification {

        @Test
        fun `960dp is Full`() {
            assertEquals(DashboardBreakpoint.Full, DashboardBreakpoint.fromWidthDp(960f))
        }

        @Test
        fun `1200dp is Full`() {
            assertEquals(DashboardBreakpoint.Full, DashboardBreakpoint.fromWidthDp(1200f))
        }

        @Test
        fun `959dp is Compact`() {
            assertEquals(DashboardBreakpoint.Compact, DashboardBreakpoint.fromWidthDp(959f))
        }

        @Test
        fun `480dp is Compact`() {
            assertEquals(DashboardBreakpoint.Compact, DashboardBreakpoint.fromWidthDp(480f))
        }

        @Test
        fun `479dp is Narrow`() {
            assertEquals(DashboardBreakpoint.Narrow, DashboardBreakpoint.fromWidthDp(479f))
        }

        @Test
        fun `200dp is Narrow`() {
            assertEquals(DashboardBreakpoint.Narrow, DashboardBreakpoint.fromWidthDp(200f))
        }
    }

    @Nested
    @DisplayName("breakpoint properties")
    inner class BreakpointProperties {

        @Test
        fun `Full shows all panels`() {
            val bp = DashboardBreakpoint.Full
            assertEquals(true, bp.showTrips)
            assertEquals(true, bp.showFullAlert)
            assertEquals(true, bp.showTimeInDriving)
        }

        @Test
        fun `Compact hides trips`() {
            val bp = DashboardBreakpoint.Compact
            assertEquals(false, bp.showTrips)
            assertEquals(false, bp.showFullAlert)
            assertEquals(false, bp.showTimeInDriving)
        }

        @Test
        fun `Narrow is most minimal`() {
            val bp = DashboardBreakpoint.Narrow
            assertEquals(false, bp.showTrips)
            assertEquals(false, bp.showFullAlert)
            assertEquals(false, bp.showTimeInDriving)
            assertEquals(false, bp.showTripDistance)
        }
    }
}
