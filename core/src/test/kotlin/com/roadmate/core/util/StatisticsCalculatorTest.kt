package com.roadmate.core.util

import com.roadmate.core.database.entity.FuelLog
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

@DisplayName("StatisticsCalculator")
class StatisticsCalculatorTest {

    @Nested
    @DisplayName("calculateStatistics")
    inner class CalculateStatistics {

        @Test
        fun `calculates statistics for date range`() {
            val baseTime = 1_700_000_000_000L
            val trips = listOf(
                testTrip(startTime = baseTime, distanceKm = 50.0, durationMs = 3600_000, status = TripStatus.COMPLETED),
                testTrip(startTime = baseTime + 1000, distanceKm = 30.0, durationMs = 1800_000, status = TripStatus.COMPLETED),
            )
            val fuelLogs = listOf(
                testFuelLog(date = baseTime, totalCost = 500.0),
                testFuelLog(date = baseTime + 1000, totalCost = 300.0),
            )
            val records = listOf(
                testMaintenanceRecord(datePerformed = baseTime, cost = 200.0),
            )

            val result = StatisticsCalculator.calculateStatistics(
                trips, fuelLogs, records, baseTime, baseTime + 2000,
            )

            assertEquals(80.0, result.totalDistanceKm, 0.01)
            assertEquals(2, result.totalTrips)
            assertEquals(40.0, result.avgTripDistanceKm, 0.01)
            assertEquals(5_400_000L, result.totalDrivingTimeMs)
            assertEquals(800.0, result.totalFuelCost, 0.01)
            assertEquals(200.0, result.totalMaintenanceCost, 0.01)
            assertEquals(12.5, result.costPerKm, 0.01)
        }

        @Test
        fun `excludes trips outside date range`() {
            val baseTime = 1_700_000_000_000L
            val trips = listOf(
                testTrip(startTime = baseTime - 1000, distanceKm = 50.0, status = TripStatus.COMPLETED),
                testTrip(startTime = baseTime + 1000, distanceKm = 30.0, status = TripStatus.COMPLETED),
            )

            val result = StatisticsCalculator.calculateStatistics(
                trips, emptyList(), emptyList(), baseTime, baseTime + 2000,
            )

            assertEquals(30.0, result.totalDistanceKm, 0.01)
            assertEquals(1, result.totalTrips)
        }

        @Test
        fun `excludes active and interrupted trips`() {
            val baseTime = 1_700_000_000_000L
            val trips = listOf(
                testTrip(startTime = baseTime, distanceKm = 50.0, status = TripStatus.ACTIVE),
                testTrip(startTime = baseTime, distanceKm = 30.0, status = TripStatus.INTERRUPTED),
                testTrip(startTime = baseTime, distanceKm = 20.0, status = TripStatus.COMPLETED),
            )

            val result = StatisticsCalculator.calculateStatistics(
                trips, emptyList(), emptyList(), baseTime, baseTime + 2000,
            )

            assertEquals(20.0, result.totalDistanceKm, 0.01)
            assertEquals(1, result.totalTrips)
        }

        @Test
        fun `returns zeros for empty period`() {
            val result = StatisticsCalculator.calculateStatistics(
                emptyList(), emptyList(), emptyList(), 0, Long.MAX_VALUE,
            )

            assertEquals(0.0, result.totalDistanceKm, 0.01)
            assertEquals(0, result.totalTrips)
            assertEquals(0.0, result.avgTripDistanceKm, 0.01)
            assertEquals(0L, result.totalDrivingTimeMs)
            assertEquals(0.0, result.totalFuelCost, 0.01)
            assertEquals(0.0, result.totalMaintenanceCost, 0.01)
            assertEquals(0.0, result.costPerKm, 0.01)
        }

        @Test
        fun `cost per km is zero when distance is zero`() {
            val baseTime = 1_700_000_000_000L
            val fuelLogs = listOf(testFuelLog(date = baseTime, totalCost = 500.0))

            val result = StatisticsCalculator.calculateStatistics(
                emptyList(), fuelLogs, emptyList(), baseTime, baseTime + 2000,
            )

            assertEquals(0.0, result.costPerKm, 0.01)
        }

        @Test
        fun `handles null maintenance cost`() {
            val baseTime = 1_700_000_000_000L
            val records = listOf(
                testMaintenanceRecord(datePerformed = baseTime, cost = null),
            )

            val result = StatisticsCalculator.calculateStatistics(
                emptyList(), emptyList(), records, baseTime, baseTime + 2000,
            )

            assertEquals(0.0, result.totalMaintenanceCost, 0.01)
        }

        @Test
        fun `avg trip distance is zero when no trips`() {
            val result = StatisticsCalculator.calculateStatistics(
                emptyList(), emptyList(), emptyList(), 0, Long.MAX_VALUE,
            )

            assertEquals(0.0, result.avgTripDistanceKm, 0.01)
        }
    }

    @Nested
    @DisplayName("calculateYearBreakdown")
    inner class CalculateYearBreakdown {

        @Test
        fun `produces 12 month breakdowns`() {
            val (months, _) = StatisticsCalculator.calculateYearBreakdown(
                emptyList(), emptyList(), emptyList(), 2026,
            )

            assertEquals(12, months.size)
            assertEquals("January", months[0].monthName)
            assertEquals("December", months[11].monthName)
        }

        @Test
        fun `aggregates data per month`() {
            val zone = ZoneId.systemDefault()
            val janStart = LocalDate.of(2026, 1, 15)
                .atStartOfDay(zone).toInstant().toEpochMilli()
            val febStart = LocalDate.of(2026, 2, 15)
                .atStartOfDay(zone).toInstant().toEpochMilli()

            val trips = listOf(
                testTrip(startTime = janStart, distanceKm = 100.0, status = TripStatus.COMPLETED),
                testTrip(startTime = febStart, distanceKm = 50.0, status = TripStatus.COMPLETED),
            )
            val fuelLogs = listOf(
                testFuelLog(date = janStart, totalCost = 200.0),
            )
            val records = listOf(
                testMaintenanceRecord(datePerformed = febStart, cost = 100.0),
            )

            val (months, runningTotal) = StatisticsCalculator.calculateYearBreakdown(
                trips, fuelLogs, records, 2026,
            )

            assertEquals(100.0, months[0].distanceKm, 0.01)
            assertEquals(200.0, months[0].fuelCost, 0.01)
            assertEquals(50.0, months[1].distanceKm, 0.01)
            assertEquals(100.0, months[1].maintenanceCost, 0.01)
            assertEquals(0.0, months[2].distanceKm, 0.01)

            assertEquals(150.0, runningTotal.totalDistanceKm, 0.01)
            assertEquals(200.0, runningTotal.totalFuelCost, 0.01)
            assertEquals(100.0, runningTotal.totalMaintenanceCost, 0.01)
        }
    }

    @Nested
    @DisplayName("calculateWeekComparison")
    inner class CalculateWeekComparison {

        @Test
        fun `compares current and previous week`() {
            val currentStart = 1_700_000_000_000L
            val currentEnd = currentStart + 604_800_000L
            val previousStart = currentStart - 604_800_000L
            val previousEnd = currentStart

            val trips = listOf(
                testTrip(startTime = currentStart + 1000, distanceKm = 100.0, status = TripStatus.COMPLETED),
                testTrip(startTime = previousStart + 1000, distanceKm = 50.0, status = TripStatus.COMPLETED),
            )
            val fuelLogs = listOf(
                testFuelLog(date = currentStart + 1000, totalCost = 600.0),
                testFuelLog(date = previousStart + 1000, totalCost = 300.0),
            )

            val result = StatisticsCalculator.calculateWeekComparison(
                trips, fuelLogs, currentStart, currentEnd, previousStart, previousEnd,
            )

            assertEquals(100.0, result.currentDistanceKm, 0.01)
            assertEquals(50.0, result.previousDistanceKm, 0.01)
            assertNotNull(result.distanceChangePercent)
            assertEquals(100.0, result.distanceChangePercent!!, 0.01)
            assertEquals(600.0, result.currentFuelCost, 0.01)
            assertEquals(300.0, result.previousFuelCost, 0.01)
            assertNotNull(result.fuelCostChangePercent)
            assertEquals(100.0, result.fuelCostChangePercent!!, 0.01)
        }

        @Test
        fun `returns null percent when previous week is zero`() {
            val currentStart = 1_700_000_000_000L
            val currentEnd = currentStart + 604_800_000L
            val previousStart = currentStart - 604_800_000L
            val previousEnd = currentStart

            val trips = listOf(
                testTrip(startTime = currentStart + 1000, distanceKm = 100.0, status = TripStatus.COMPLETED),
            )

            val result = StatisticsCalculator.calculateWeekComparison(
                trips, emptyList(), currentStart, currentEnd, previousStart, previousEnd,
            )

            assertNull(result.distanceChangePercent)
            assertNull(result.fuelCostChangePercent)
        }

        @Test
        fun `shows negative change when current week is lower`() {
            val currentStart = 1_700_000_000_000L
            val currentEnd = currentStart + 604_800_000L
            val previousStart = currentStart - 604_800_000L
            val previousEnd = currentStart

            val trips = listOf(
                testTrip(startTime = currentStart + 1000, distanceKm = 40.0, status = TripStatus.COMPLETED),
                testTrip(startTime = previousStart + 1000, distanceKm = 80.0, status = TripStatus.COMPLETED),
            )

            val result = StatisticsCalculator.calculateWeekComparison(
                trips, emptyList(), currentStart, currentEnd, previousStart, previousEnd,
            )

            assertNotNull(result.distanceChangePercent)
            assertEquals(-50.0, result.distanceChangePercent!!, 0.01)
        }
    }

    @Nested
    @DisplayName("percentChange")
    inner class PercentChange {

        @Test
        fun `calculates positive change`() {
            val result = StatisticsCalculator.percentChange(100.0, 150.0)
            assertNotNull(result)
            assertEquals(50.0, result!!, 0.01)
        }

        @Test
        fun `calculates negative change`() {
            val result = StatisticsCalculator.percentChange(100.0, 75.0)
            assertNotNull(result)
            assertEquals(-25.0, result!!, 0.01)
        }

        @Test
        fun `returns null when old value is zero`() {
            assertNull(StatisticsCalculator.percentChange(0.0, 100.0))
        }

        @Test
        fun `returns zero when values are equal`() {
            val result = StatisticsCalculator.percentChange(100.0, 100.0)
            assertNotNull(result)
            assertEquals(0.0, result!!, 0.01)
        }
    }

    @Nested
    @DisplayName("dayRange")
    inner class DayRange {

        @Test
        fun `returns midnight to midnight range`() {
            val date = LocalDate.of(2026, 5, 14)
            val (start, end) = StatisticsCalculator.dayRange(date)

            val zone = ZoneId.systemDefault()
            val expectedStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val expectedEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            assertEquals(expectedStart, start)
            assertEquals(expectedEnd, end)
            assertTrue(end > start)
        }
    }

    @Nested
    @DisplayName("weekRange")
    inner class WeekRange {

        @Test
        fun `starts on monday`() {
            val wednesday = LocalDate.of(2026, 5, 13)
            val (start, end) = StatisticsCalculator.weekRange(wednesday)

            val zone = ZoneId.systemDefault()
            val expectedMonday = wednesday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val expectedStart = expectedMonday.atStartOfDay(zone).toInstant().toEpochMilli()
            val expectedEnd = expectedMonday.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()

            assertEquals(expectedStart, start)
            assertEquals(expectedEnd, end)
        }
    }

    @Nested
    @DisplayName("monthRange")
    inner class MonthRange {

        @Test
        fun `returns first to first of next month`() {
            val date = LocalDate.of(2026, 5, 14)
            val (start, end) = StatisticsCalculator.monthRange(date)

            val zone = ZoneId.systemDefault()
            val expectedStart = LocalDate.of(2026, 5, 1)
                .atStartOfDay(zone).toInstant().toEpochMilli()
            val expectedEnd = LocalDate.of(2026, 6, 1)
                .atStartOfDay(zone).toInstant().toEpochMilli()

            assertEquals(expectedStart, start)
            assertEquals(expectedEnd, end)
        }
    }

    @Nested
    @DisplayName("yearRange")
    inner class YearRange {

        @Test
        fun `returns jan 1 to jan 1 next year`() {
            val date = LocalDate.of(2026, 5, 14)
            val (start, end) = StatisticsCalculator.yearRange(date)

            val zone = ZoneId.systemDefault()
            val expectedStart = LocalDate.of(2026, 1, 1)
                .atStartOfDay(zone).toInstant().toEpochMilli()
            val expectedEnd = LocalDate.of(2027, 1, 1)
                .atStartOfDay(zone).toInstant().toEpochMilli()

            assertEquals(expectedStart, start)
            assertEquals(expectedEnd, end)
        }
    }

    private fun testTrip(
        id: String = "t-1",
        vehicleId: String = "v-1",
        startTime: Long = System.currentTimeMillis(),
        distanceKm: Double = 10.0,
        durationMs: Long = 600_000L,
        status: TripStatus = TripStatus.COMPLETED,
    ) = Trip(
        id = id,
        vehicleId = vehicleId,
        startTime = startTime,
        endTime = startTime + durationMs,
        distanceKm = distanceKm,
        durationMs = durationMs,
        maxSpeedKmh = 80.0,
        avgSpeedKmh = 40.0,
        estimatedFuelL = 1.0,
        startOdometerKm = 85000.0,
        endOdometerKm = 85000.0 + distanceKm,
        status = status,
    )

    private fun testFuelLog(
        id: String = "f-1",
        vehicleId: String = "v-1",
        date: Long = System.currentTimeMillis(),
        totalCost: Double = 480.0,
    ) = FuelLog(
        id = id,
        vehicleId = vehicleId,
        date = date,
        odometerKm = 85000.0,
        liters = 40.0,
        pricePerLiter = 12.0,
        totalCost = totalCost,
        isFullTank = true,
    )

    private fun testMaintenanceRecord(
        id: String = "mr-1",
        vehicleId: String = "v-1",
        scheduleId: String = "s-1",
        datePerformed: Long = System.currentTimeMillis(),
        cost: Double? = 100.0,
    ) = MaintenanceRecord(
        id = id,
        scheduleId = scheduleId,
        vehicleId = vehicleId,
        datePerformed = datePerformed,
        odometerKm = 85000.0,
        cost = cost,
    )
}
