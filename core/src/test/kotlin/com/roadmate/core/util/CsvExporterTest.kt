package com.roadmate.core.util

import com.roadmate.core.database.entity.FuelLog
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CsvExporter")
class CsvExporterTest {

    @Nested
    @DisplayName("exportTrips")
    inner class ExportTrips {

        @Test
        fun `generates CSV with correct headers`() {
            val csv = CsvExporter.exportTrips(emptyList(), "MyCar")
            val lines = csv.lines()
            assertEquals("Date,Start Time,End Time,Distance,Duration,Avg Speed,Max Speed,Est Fuel,Status", lines.first())
        }

        @Test
        fun `generates CSV with trip data rows`() {
            val trip = Trip(
                id = "t1",
                vehicleId = "v1",
                startTime = 1700000000000L,
                endTime = 1700003600000L,
                distanceKm = 45.5,
                durationMs = 3600000L,
                maxSpeedKmh = 120.0,
                avgSpeedKmh = 45.5,
                estimatedFuelL = 3.2,
                startOdometerKm = 85000.0,
                endOdometerKm = 85045.5,
                status = TripStatus.COMPLETED,
                lastModified = 1700000000000L,
            )

            val csv = CsvExporter.exportTrips(listOf(trip), "MyCar")
            val lines = csv.lines().filter { it.isNotBlank() }

            assertEquals(2, lines.size)
            val dataLine = lines[1]
            assertTrue(dataLine.contains("COMPLETED"))
            assertTrue(dataLine.contains("45.5"))
            assertTrue(dataLine.contains("120.0"))
        }

        @Test
        fun `handles active trip with no end time`() {
            val trip = Trip(
                id = "t1",
                vehicleId = "v1",
                startTime = 1700000000000L,
                endTime = null,
                distanceKm = 10.0,
                durationMs = 600000L,
                maxSpeedKmh = 60.0,
                avgSpeedKmh = 60.0,
                estimatedFuelL = 0.8,
                startOdometerKm = 85000.0,
                endOdometerKm = 85010.0,
                status = TripStatus.ACTIVE,
                lastModified = 1700000000000L,
            )

            val csv = CsvExporter.exportTrips(listOf(trip), "MyCar")
            val lines = csv.lines().filter { it.isNotBlank() }

            assertEquals(2, lines.size)
            assertTrue(lines[1].contains("ACTIVE"))
        }

        @Test
        fun `filters trips by date range`() {
            val trips = listOf(
                testTrip(id = "t1", startTime = 1000L),
                testTrip(id = "t2", startTime = 5000L),
                testTrip(id = "t3", startTime = 9000L),
            )

            val csv = CsvExporter.exportTrips(trips, "MyCar", fromMs = 3000L, toMs = 8000L)
            val lines = csv.lines().filter { it.isNotBlank() }

            assertEquals(2, lines.size)
        }
    }

    @Nested
    @DisplayName("exportFuelLogs")
    inner class ExportFuelLogs {

        @Test
        fun `generates CSV with correct headers`() {
            val csv = CsvExporter.exportFuelLogs(emptyList())
            val lines = csv.lines()
            assertEquals("Date,ODO,Liters,Price/L,Total Cost,Full Tank,Station,Consumption", lines.first())
        }

        @Test
        fun `generates CSV with fuel log data`() {
            val fuelLog = FuelLog(
                id = "f1",
                vehicleId = "v1",
                date = 1700000000000L,
                odometerKm = 85500.0,
                liters = 30.0,
                pricePerLiter = 12.5,
                totalCost = 375.0,
                isFullTank = true,
                station = "Shell",
                lastModified = 1700000000000L,
            )

            val csv = CsvExporter.exportFuelLogs(listOf(fuelLog))
            val lines = csv.lines().filter { it.isNotBlank() }

            assertEquals(2, lines.size)
            assertTrue(lines[1].contains("85500.0"))
            assertTrue(lines[1].contains("30.0"))
            assertTrue(lines[1].contains("12.5"))
            assertTrue(lines[1].contains("375.0"))
            assertTrue(lines[1].contains("Yes"))
            assertTrue(lines[1].contains("Shell"))
        }

        @Test
        fun `handles null station`() {
            val fuelLog = FuelLog(
                id = "f1",
                vehicleId = "v1",
                date = 1700000000000L,
                odometerKm = 85500.0,
                liters = 30.0,
                pricePerLiter = 12.5,
                totalCost = 375.0,
                isFullTank = false,
                station = null,
                lastModified = 1700000000000L,
            )

            val csv = CsvExporter.exportFuelLogs(listOf(fuelLog))
            val lines = csv.lines().filter { it.isNotBlank() }

            assertTrue(lines[1].contains("No"))
        }
    }

    @Nested
    @DisplayName("exportMaintenance")
    inner class ExportMaintenance {

        @Test
        fun `generates CSV with correct headers`() {
            val csv = CsvExporter.exportMaintenance(emptyList(), emptyMap())
            val lines = csv.lines()
            assertEquals("Item Name,Date,ODO,Cost,Location,Notes", lines.first())
        }

        @Test
        fun `generates CSV with maintenance record data`() {
            val schedule = MaintenanceSchedule(
                id = "s1",
                vehicleId = "v1",
                name = "Oil Change",
                intervalKm = 10000,
                intervalMonths = 12,
                lastServiceKm = 85000.0,
                lastServiceDate = 1700000000000L,
                isCustom = false,
                lastModified = 1700000000000L,
            )

            val record = MaintenanceRecord(
                id = "r1",
                scheduleId = "s1",
                vehicleId = "v1",
                datePerformed = 1700000000000L,
                odometerKm = 85500.0,
                cost = 250.0,
                location = "Dealer",
                notes = "Synthetic oil",
                lastModified = 1700000000000L,
            )

            val csv = CsvExporter.exportMaintenance(listOf(record), mapOf("s1" to schedule))
            val lines = csv.lines().filter { it.isNotBlank() }

            assertEquals(2, lines.size)
            assertTrue(lines[1].contains("Oil Change"))
            assertTrue(lines[1].contains("85500.0"))
            assertTrue(lines[1].contains("250.0"))
            assertTrue(lines[1].contains("Dealer"))
            assertTrue(lines[1].contains("Synthetic oil"))
        }

        @Test
        fun `handles record with missing schedule`() {
            val record = MaintenanceRecord(
                id = "r1",
                scheduleId = "s-unknown",
                vehicleId = "v1",
                datePerformed = 1700000000000L,
                odometerKm = 85500.0,
                cost = null,
                location = null,
                notes = null,
                lastModified = 1700000000000L,
            )

            val csv = CsvExporter.exportMaintenance(listOf(record), emptyMap())
            val lines = csv.lines().filter { it.isNotBlank() }

            assertEquals(2, lines.size)
            assertTrue(lines[1].contains("Unknown"))
        }

        @Test
        fun `handles null cost location and notes`() {
            val schedule = MaintenanceSchedule(
                id = "s1",
                vehicleId = "v1",
                name = "Oil Change",
                intervalKm = 10000,
                intervalMonths = 12,
                lastServiceKm = 85000.0,
                lastServiceDate = 1700000000000L,
                isCustom = false,
                lastModified = 1700000000000L,
            )

            val record = MaintenanceRecord(
                id = "r1",
                scheduleId = "s1",
                vehicleId = "v1",
                datePerformed = 1700000000000L,
                odometerKm = 85500.0,
                cost = null,
                location = null,
                notes = null,
                lastModified = 1700000000000L,
            )

            val csv = CsvExporter.exportMaintenance(listOf(record), mapOf("s1" to schedule))
            val lines = csv.lines().filter { it.isNotBlank() }

            assertEquals(2, lines.size)
        }
    }

    @Nested
    @DisplayName("generateFileName")
    inner class GenerateFileName {

        @Test
        fun `generates trips file name`() {
            val name = CsvExporter.generateFileName("trips", "MyCar")
            assertTrue(name.startsWith("roadmate_trips_MyCar_"))
            assertTrue(name.endsWith(".csv"))
        }

        @Test
        fun `generates fuel file name`() {
            val name = CsvExporter.generateFileName("fuel", "MyCar")
            assertTrue(name.startsWith("roadmate_fuel_MyCar_"))
            assertTrue(name.endsWith(".csv"))
        }

        @Test
        fun `generates maintenance file name`() {
            val name = CsvExporter.generateFileName("maintenance", "MyCar")
            assertTrue(name.startsWith("roadmate_maintenance_MyCar_"))
            assertTrue(name.endsWith(".csv"))
        }
    }

    private fun testTrip(
        id: String = "t1",
        vehicleId: String = "v1",
        startTime: Long = 1700000000000L,
        endTime: Long? = 1700003600000L,
        distanceKm: Double = 45.5,
        durationMs: Long = 3600000L,
        maxSpeedKmh: Double = 120.0,
        avgSpeedKmh: Double = 45.5,
        estimatedFuelL: Double = 3.2,
        status: TripStatus = TripStatus.COMPLETED,
    ) = Trip(
        id = id,
        vehicleId = vehicleId,
        startTime = startTime,
        endTime = endTime,
        distanceKm = distanceKm,
        durationMs = durationMs,
        maxSpeedKmh = maxSpeedKmh,
        avgSpeedKmh = avgSpeedKmh,
        estimatedFuelL = estimatedFuelL,
        startOdometerKm = 85000.0,
        endOdometerKm = 85000.0 + distanceKm,
        status = status,
        lastModified = startTime,
    )
}
