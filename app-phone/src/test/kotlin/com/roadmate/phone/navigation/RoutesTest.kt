package com.roadmate.phone.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Routes")
class RoutesTest {

    @Nested
    @DisplayName("VehicleHub")
    inner class VehicleHubRoute {
        @Test
        fun `is a serializable object`() {
            val route = VehicleHub
            assertNotNull(route)
        }
    }

    @Nested
    @DisplayName("TripDetail")
    inner class TripDetailRoute {
        @Test
        fun `holds tripId`() {
            val route = TripDetail(tripId = "trip-123")
            assertEquals("trip-123", route.tripId)
        }

        @Test
        fun `different tripIds are not equal`() {
            val a = TripDetail(tripId = "a")
            val b = TripDetail(tripId = "b")
            assertFalse(a == b)
        }
    }

    @Nested
    @DisplayName("MaintenanceDetail")
    inner class MaintenanceDetailRoute {
        @Test
        fun `holds scheduleId`() {
            val route = MaintenanceDetail(scheduleId = "sched-42")
            assertEquals("sched-42", route.scheduleId)
        }
    }

    @Nested
    @DisplayName("DocumentDetail")
    inner class DocumentDetailRoute {
        @Test
        fun `holds documentId`() {
            val route = DocumentDetail(documentId = "doc-7")
            assertEquals("doc-7", route.documentId)
        }
    }

    @Nested
    @DisplayName("Simple routes")
    inner class SimpleRoutes {
        @Test
        fun `all object routes are instantiable`() {
            assertNotNull(VehicleHub)
            assertNotNull(TripList)
            assertNotNull(MaintenanceList)
            assertNotNull(FuelLog)
            assertNotNull(DocumentList)
            assertNotNull(VehicleManagement)
        }
    }
}
