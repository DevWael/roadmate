package com.roadmate.core.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("MaintenanceProgressCalculator")
class MaintenanceProgressCalculatorTest {

    @Nested
    @DisplayName("km-based progress")
    inner class KmBasedProgress {

        @Test
        @DisplayName("calculates progress from km interval")
        fun calculatesKmProgress() {
            val result = MaintenanceProgressCalculator.calculate(
                currentOdometerKm = 15_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = 10_000,
                lastServiceDate = 0L,
                intervalMonths = null,
                currentTimeMillis = 0L,
            )
            assertEquals(50.0f, result, 0.01f)
        }

        @Test
        @DisplayName("returns 0 when no km driven since service")
        fun noKmDriven() {
            val result = MaintenanceProgressCalculator.calculate(
                currentOdometerKm = 10_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = 10_000,
                lastServiceDate = 0L,
                intervalMonths = null,
                currentTimeMillis = 0L,
            )
            assertEquals(0.0f, result, 0.01f)
        }

        @Test
        @DisplayName("returns 100 when interval fully consumed")
        fun intervalFullyConsumed() {
            val result = MaintenanceProgressCalculator.calculate(
                currentOdometerKm = 20_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = 10_000,
                lastServiceDate = 0L,
                intervalMonths = null,
                currentTimeMillis = 0L,
            )
            assertEquals(100.0f, result, 0.01f)
        }

        @Test
        @DisplayName("returns 100 when driven beyond interval")
        fun drivenBeyondInterval() {
            val result = MaintenanceProgressCalculator.calculate(
                currentOdometerKm = 25_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = 10_000,
                lastServiceDate = 0L,
                intervalMonths = null,
                currentTimeMillis = 0L,
            )
            assertEquals(100.0f, result, 0.01f)
        }

        @Test
        @DisplayName("returns 0 when intervalKm is null and no month interval")
        fun nullIntervalKmNoMonths() {
            val result = MaintenanceProgressCalculator.calculate(
                currentOdometerKm = 15_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = null,
                lastServiceDate = 0L,
                intervalMonths = null,
                currentTimeMillis = 0L,
            )
            assertEquals(0.0f, result, 0.01f)
        }
    }

    @Nested
    @DisplayName("month-based progress")
    inner class MonthBasedProgress {

        @Test
        @DisplayName("calculates progress from month interval")
        fun calculatesMonthProgress() {
            val now = 1_700_000_000_000L
            val threeMonthsAgo = now - (90L * 24 * 60 * 60 * 1000)
            val result = MaintenanceProgressCalculator.calculate(
                currentOdometerKm = 10_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = null,
                lastServiceDate = threeMonthsAgo,
                intervalMonths = 12,
                currentTimeMillis = now,
            )
            assertEquals(25.0f, result, 2.0f)
        }

        @Test
        @DisplayName("returns 0 when no time elapsed")
        fun noTimeElapsed() {
            val now = 1_700_000_000_000L
            val result = MaintenanceProgressCalculator.calculate(
                currentOdometerKm = 10_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = null,
                lastServiceDate = now,
                intervalMonths = 6,
                currentTimeMillis = now,
            )
            assertEquals(0.0f, result, 0.01f)
        }

        @Test
        @DisplayName("returns 100 when month interval fully elapsed")
        fun monthIntervalFullyElapsed() {
            val now = 1_700_000_000_000L
            val twelveMonthsAgo = now - (365L * 24 * 60 * 60 * 1000)
            val result = MaintenanceProgressCalculator.calculate(
                currentOdometerKm = 10_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = null,
                lastServiceDate = twelveMonthsAgo,
                intervalMonths = 6,
                currentTimeMillis = now,
            )
            assertEquals(100.0f, result, 0.01f)
        }

        @Test
        @DisplayName("returns 0 when intervalMonths is null")
        fun nullIntervalMonths() {
            val now = 1_700_000_000_000L
            val result = MaintenanceProgressCalculator.calculate(
                currentOdometerKm = 10_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = null,
                lastServiceDate = now - (180L * 24 * 60 * 60 * 1000),
                intervalMonths = null,
                currentTimeMillis = now,
            )
            assertEquals(0.0f, result, 0.01f)
        }
    }

    @Nested
    @DisplayName("combined progress (max of km and month)")
    inner class CombinedProgress {

        @Test
        @DisplayName("returns higher of km and month progress")
        fun returnsHigher() {
            val now = 1_700_000_000_000L
            val nineMonthsAgo = now - (270L * 24 * 60 * 60 * 1000)
            val result = MaintenanceProgressCalculator.calculate(
                currentOdometerKm = 12_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = 10_000,
                lastServiceDate = nineMonthsAgo,
                intervalMonths = 12,
                currentTimeMillis = now,
            )
            val kmProgress = 20.0f
            val monthProgress = 75.0f
            assertEquals(maxOf(kmProgress, monthProgress), result, 3.0f)
        }

        @Test
        @DisplayName("km progress wins when higher")
        fun kmProgressWins() {
            val now = 1_700_000_000_000L
            val oneMonthAgo = now - (30L * 24 * 60 * 60 * 1000)
            val result = MaintenanceProgressCalculator.calculate(
                currentOdometerKm = 18_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = 10_000,
                lastServiceDate = oneMonthAgo,
                intervalMonths = 12,
                currentTimeMillis = now,
            )
            assertEquals(80.0f, result, 1.0f)
        }
    }

    @Nested
    @DisplayName("remaining km calculation")
    inner class RemainingKm {

        @Test
        @DisplayName("calculates remaining km until service")
        fun remainingKmCalculation() {
            val remaining = MaintenanceProgressCalculator.remainingKm(
                currentOdometerKm = 15_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = 10_000,
            )
            assertEquals(5_000.0, remaining, 0.01)
        }

        @Test
        @DisplayName("returns 0 when past interval")
        fun pastInterval() {
            val remaining = MaintenanceProgressCalculator.remainingKm(
                currentOdometerKm = 25_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = 10_000,
            )
            assertEquals(0.0, remaining, 0.01)
        }

        @Test
        @DisplayName("returns interval km when no km driven")
        fun noKmDriven() {
            val remaining = MaintenanceProgressCalculator.remainingKm(
                currentOdometerKm = 10_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = 10_000,
            )
            assertEquals(10_000.0, remaining, 0.01)
        }

        @Test
        @DisplayName("returns 0 when intervalKm is null")
        fun nullInterval() {
            val remaining = MaintenanceProgressCalculator.remainingKm(
                currentOdometerKm = 15_000.0,
                lastServiceKm = 10_000.0,
                intervalKm = null,
            )
            assertEquals(0.0, remaining, 0.01)
        }
    }
}
