package com.roadmate.core.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("MaintenancePredictionEngine")
class MaintenancePredictionEngineTest {

    @Nested
    @DisplayName("daily average calculation")
    inner class DailyAverage {

        @Test
        @DisplayName("calculates average from trips spanning 30 days")
        fun calculatesFromTrips() {
            val now = System.currentTimeMillis()
            val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
            val trips = (0..6).map { i ->
                createTrip(
                    distanceKm = if (i < 4) 75.0 else 0.0,
                    startTime = now - thirtyDaysMs + (i * (thirtyDaysMs / 6)),
                )
            }
            val avg = MaintenancePredictionEngine.dailyAverage(trips)
            assertEquals(300.0 / 30.0, avg, 0.01)
        }

        @Test
        @DisplayName("returns fallback 50 km/day when less than 7 days of data")
        fun fallbackWhenLessThan7Days() {
            val now = System.currentTimeMillis()
            val oneDayMs = 24L * 60 * 60 * 1000
            val trips = listOf(
                createTrip(distanceKm = 100.0, startTime = now - 2 * oneDayMs),
                createTrip(distanceKm = 100.0, startTime = now),
            )
            val avg = MaintenancePredictionEngine.dailyAverage(trips, fallbackIfFewerDays = 7)
            assertEquals(50.0, avg, 0.01)
        }

        @Test
        @DisplayName("returns fallback 50 km/day when no trips")
        fun fallbackWhenNoTrips() {
            val avg = MaintenancePredictionEngine.dailyAverage(emptyList())
            assertEquals(50.0, avg, 0.01)
        }

        @Test
        @DisplayName("uses actual average when span is 7 or more days")
        fun usesActualWhen7Days() {
            val now = System.currentTimeMillis()
            val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
            val trips = (0..6).map { i ->
                createTrip(
                    distanceKm = 100.0,
                    startTime = now - sevenDaysMs + (i * (sevenDaysMs / 6)),
                )
            }
            val avg = MaintenancePredictionEngine.dailyAverage(trips, fallbackIfFewerDays = 7)
            assertEquals(700.0 / 7.0, avg, 0.01)
        }

        @Test
        @DisplayName("single trip with span < 1 day uses coerced 1-day span")
        fun singleTripUsesOneDaySpan() {
            val now = System.currentTimeMillis()
            // Single trip → daySpan = 0 → coerced to 1.0 → 1.0 < 7 → fallback
            val trips = listOf(createTrip(distanceKm = 100.0, startTime = now))
            val avg = MaintenancePredictionEngine.dailyAverage(trips, fallbackIfFewerDays = 7)
            assertEquals(50.0, avg, 0.01)
        }
    }

    @Nested
    @DisplayName("predicted next service date")
    inner class PredictedDate {

        @Test
        @DisplayName("predicts date based on km interval and daily average")
        fun predictsFromDateAndKm() {
            val today = LocalDate.of(2026, 5, 12)
            val result = MaintenancePredictionEngine.predictNextServiceDate(
                remainingKm = 1000.0,
                dailyAvgKm = 50.0,
                lastServiceDate = today.minusMonths(3).toEpochDay() * 24 * 60 * 60 * 1000,
                intervalMonths = null,
                today = today,
            )
            assertEquals(today.plusDays(20), result)
        }

        @Test
        @DisplayName("predicts date based on month interval when earlier")
        fun predictsFromMonthInterval() {
            val today = LocalDate.of(2026, 5, 12)
            val threeMonthsAgo = today.minusMonths(3)
            val result = MaintenancePredictionEngine.predictNextServiceDate(
                remainingKm = 5000.0,
                dailyAvgKm = 50.0,
                lastServiceDate = threeMonthsAgo.toEpochDay() * 24 * 60 * 60 * 1000,
                intervalMonths = 6,
                today = today,
            )
            assertEquals(today.plusMonths(3), result)
        }

        @Test
        @DisplayName("km-based date wins when earlier than month-based")
        fun kmDateWinsWhenEarlier() {
            val today = LocalDate.of(2026, 5, 12)
            val sixMonthsAgo = today.minusMonths(6)
            val result = MaintenancePredictionEngine.predictNextServiceDate(
                remainingKm = 500.0,
                dailyAvgKm = 50.0,
                lastServiceDate = sixMonthsAgo.toEpochDay() * 24 * 60 * 60 * 1000,
                intervalMonths = 12,
                today = today,
            )
            assertEquals(today.plusDays(10), result)
        }

        @Test
        @DisplayName("returns today when remaining km is 0")
        fun returnsTodayWhenNoRemaining() {
            val today = LocalDate.of(2026, 5, 12)
            val result = MaintenancePredictionEngine.predictNextServiceDate(
                remainingKm = 0.0,
                dailyAvgKm = 50.0,
                lastServiceDate = today.minusMonths(6).toEpochDay() * 24 * 60 * 60 * 1000,
                intervalMonths = null,
                today = today,
            )
            assertEquals(today, result)
        }

        @Test
        @DisplayName("returns today when remaining km is negative (overdue)")
        fun returnsTodayWhenNegativeRemaining() {
            val today = LocalDate.of(2026, 5, 12)
            val result = MaintenancePredictionEngine.predictNextServiceDate(
                remainingKm = -500.0,
                dailyAvgKm = 50.0,
                lastServiceDate = today.minusMonths(6).toEpochDay() * 24 * 60 * 60 * 1000,
                intervalMonths = null,
                today = today,
            )
            assertEquals(today, result)
        }
    }

    @Nested
    @DisplayName("remaining km calculation")
    inner class RemainingKm {

        @Test
        @DisplayName("calculates remaining km until next service")
        fun calculatesRemaining() {
            val remaining = MaintenancePredictionEngine.remainingKm(
                currentOdometerKm = 15_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = 10_000,
            )
            assertEquals(5_000.0, remaining, 0.01)
        }

        @Test
        @DisplayName("returns 0 when past interval")
        fun pastInterval() {
            val remaining = MaintenancePredictionEngine.remainingKm(
                currentOdometerKm = 21_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = 10_000,
            )
            assertEquals(0.0, remaining, 0.01)
        }

        @Test
        @DisplayName("returns 0 when intervalKm is null")
        fun nullInterval() {
            val remaining = MaintenancePredictionEngine.remainingKm(
                currentOdometerKm = 15_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = null,
            )
            assertEquals(0.0, remaining, 0.01)
        }
    }

    @Nested
    @DisplayName("overdue km calculation")
    inner class OverdueKm {

        @Test
        @DisplayName("calculates overdue km when past interval")
        fun calculatesOverdue() {
            val overdue = MaintenancePredictionEngine.overdueKm(
                currentOdometerKm = 21_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = 10_000,
            )
            assertEquals(1_000.0, overdue, 0.01)
        }

        @Test
        @DisplayName("returns 0 when not overdue")
        fun notOverdue() {
            val overdue = MaintenancePredictionEngine.overdueKm(
                currentOdometerKm = 15_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = 10_000,
            )
            assertEquals(0.0, overdue, 0.01)
        }

        @Test
        @DisplayName("returns 0 when intervalKm is null")
        fun nullInterval() {
            val overdue = MaintenancePredictionEngine.overdueKm(
                currentOdometerKm = 21_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = null,
            )
            assertEquals(0.0, overdue, 0.01)
        }
    }

    @Nested
    @DisplayName("attention band classification")
    inner class AttentionBandClassification {

        @Test
        @DisplayName("classifies as critical when within critical threshold")
        fun criticalBand() {
            val result = MaintenancePredictionEngine.classifyBand(
                remainingKm = 200.0,
                attentionThresholdKm = 500.0,
                intervalKm = 10_000,
            )
            assertEquals(AttentionLevel.CRITICAL, result)
        }

        @Test
        @DisplayName("classifies as warning when within attention threshold")
        fun warningBand() {
            val result = MaintenancePredictionEngine.classifyBand(
                remainingKm = 400.0,
                attentionThresholdKm = 500.0,
                intervalKm = 10_000,
            )
            assertEquals(AttentionLevel.WARNING, result)
        }

        @Test
        @DisplayName("classifies as normal when outside threshold")
        fun normalBand() {
            val result = MaintenancePredictionEngine.classifyBand(
                remainingKm = 5_000.0,
                attentionThresholdKm = 500.0,
                intervalKm = 10_000,
            )
            assertEquals(AttentionLevel.NORMAL, result)
        }

        @Test
        @DisplayName("classifies as overdue when remaining is 0 and interval exists")
        fun overdueBand() {
            val result = MaintenancePredictionEngine.classifyBand(
                remainingKm = 0.0,
                attentionThresholdKm = 500.0,
                intervalKm = 10_000,
            )
            assertEquals(AttentionLevel.OVERDUE, result)
        }

        @Test
        @DisplayName("classifies as normal when intervalKm is null")
        fun nullIntervalKm() {
            val result = MaintenancePredictionEngine.classifyBand(
                remainingKm = 0.0,
                attentionThresholdKm = 500.0,
                intervalKm = null,
            )
            assertEquals(AttentionLevel.NORMAL, result)
        }

        @Test
        @DisplayName("boundary: exactly at half threshold is critical")
        fun boundaryHalfThreshold() {
            val result = MaintenancePredictionEngine.classifyBand(
                remainingKm = 250.0,
                attentionThresholdKm = 500.0,
                intervalKm = 10_000,
            )
            assertEquals(AttentionLevel.CRITICAL, result)
        }

        @Test
        @DisplayName("boundary: exactly at threshold is warning")
        fun boundaryAtThreshold() {
            val result = MaintenancePredictionEngine.classifyBand(
                remainingKm = 500.0,
                attentionThresholdKm = 500.0,
                intervalKm = 10_000,
            )
            assertEquals(AttentionLevel.WARNING, result)
        }
    }

    private fun createTrip(
        distanceKm: Double,
        startTime: Long = System.currentTimeMillis(),
    ): com.roadmate.core.database.entity.Trip {
        return com.roadmate.core.database.entity.Trip(
            vehicleId = "test",
            startTime = startTime,
            endTime = startTime + 3600_000,
            distanceKm = distanceKm,
            durationMs = 3600_000,
            maxSpeedKmh = 100.0,
            avgSpeedKmh = 50.0,
            estimatedFuelL = 5.0,
            startOdometerKm = 0.0,
            endOdometerKm = distanceKm,
            status = com.roadmate.core.database.entity.TripStatus.COMPLETED,
        )
    }
}
