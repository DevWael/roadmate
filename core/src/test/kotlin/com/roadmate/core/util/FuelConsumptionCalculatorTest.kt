package com.roadmate.core.util

import com.roadmate.core.database.entity.FuelLog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("FuelConsumptionCalculator")
class FuelConsumptionCalculatorTest {

    @Nested
    @DisplayName("calculateActualConsumption")
    inner class CalculateActualConsumption {

        @Test
        fun `calculates consumption between two full tanks`() {
            val previous = testFuelLog(odometerKm = 85000.0, liters = 40.0, isFullTank = true)
            val current = testFuelLog(odometerKm = 85500.0, liters = 30.0, isFullTank = true)

            val result = FuelConsumptionCalculator.calculateActualConsumption(current, previous)

            assertNotNull(result)
            assertEquals(6.0, result!!, 0.01)
        }

        @Test
        fun `returns null if current is not full tank`() {
            val previous = testFuelLog(odometerKm = 85000.0, liters = 40.0, isFullTank = true)
            val current = testFuelLog(odometerKm = 85500.0, liters = 30.0, isFullTank = false)

            assertNull(FuelConsumptionCalculator.calculateActualConsumption(current, previous))
        }

        @Test
        fun `returns null if previous is not full tank`() {
            val previous = testFuelLog(odometerKm = 85000.0, liters = 40.0, isFullTank = false)
            val current = testFuelLog(odometerKm = 85500.0, liters = 30.0, isFullTank = true)

            assertNull(FuelConsumptionCalculator.calculateActualConsumption(current, previous))
        }

        @Test
        fun `returns null if distance is zero`() {
            val previous = testFuelLog(odometerKm = 85000.0, liters = 40.0, isFullTank = true)
            val current = testFuelLog(odometerKm = 85000.0, liters = 30.0, isFullTank = true)

            assertNull(FuelConsumptionCalculator.calculateActualConsumption(current, previous))
        }

        @Test
        fun `returns null if distance is negative`() {
            val previous = testFuelLog(odometerKm = 85500.0, liters = 40.0, isFullTank = true)
            val current = testFuelLog(odometerKm = 85000.0, liters = 30.0, isFullTank = true)

            assertNull(FuelConsumptionCalculator.calculateActualConsumption(current, previous))
        }
    }

    @Nested
    @DisplayName("isOverConsumption")
    inner class IsOverConsumption {

        @Test
        fun `true when actual exceeds 20 percent over estimated`() {
            assertTrue(FuelConsumptionCalculator.isOverConsumption(12.1, 10.0))
        }

        @Test
        fun `false when actual is exactly 20 percent over`() {
            assertFalse(FuelConsumptionCalculator.isOverConsumption(12.0, 10.0))
        }

        @Test
        fun `false when actual is below estimated`() {
            assertFalse(FuelConsumptionCalculator.isOverConsumption(8.0, 10.0))
        }

        @Test
        fun `false when estimated is zero`() {
            assertFalse(FuelConsumptionCalculator.isOverConsumption(12.0, 0.0))
        }
    }

    @Nested
    @DisplayName("calculateEstimatedConsumption")
    inner class CalculateEstimatedConsumption {

        @Test
        fun `averages city and highway consumption`() {
            val result = FuelConsumptionCalculator.calculateEstimatedConsumption(8.0, 6.0)
            assertEquals(7.0, result, 0.01)
        }
    }

    @Nested
    @DisplayName("findFullTankPairs")
    inner class FindFullTankPairs {

        @Test
        fun `finds consecutive full tank pairs`() {
            val baseTime = 1_700_000_000_000L
            val entries = listOf(
                testFuelLog(id = "1", odometerKm = 84000.0, isFullTank = true, date = baseTime),
                testFuelLog(id = "2", odometerKm = 84500.0, isFullTank = false, date = baseTime + 1000),
                testFuelLog(id = "3", odometerKm = 85000.0, isFullTank = true, date = baseTime + 2000),
                testFuelLog(id = "4", odometerKm = 85500.0, isFullTank = true, date = baseTime + 3000),
            )

            val pairs = FuelConsumptionCalculator.findFullTankPairs(entries)

            assertEquals(2, pairs.size)
            assertEquals("3", pairs[0].first.id)
            assertEquals("1", pairs[0].second.id)
            assertEquals("4", pairs[1].first.id)
            assertEquals("3", pairs[1].second.id)
        }

        @Test
        fun `returns empty when no full tanks`() {
            val entries = listOf(
                testFuelLog(id = "1", isFullTank = false),
                testFuelLog(id = "2", isFullTank = false),
            )

            assertTrue(FuelConsumptionCalculator.findFullTankPairs(entries).isEmpty())
        }

        @Test
        fun `returns empty when only one full tank`() {
            val entries = listOf(
                testFuelLog(id = "1", isFullTank = true),
                testFuelLog(id = "2", isFullTank = false),
            )

            assertTrue(FuelConsumptionCalculator.findFullTankPairs(entries).isEmpty())
        }
    }

    @Nested
    @DisplayName("calculateAvgLPer100km")
    inner class CalculateAvgLPer100km {

        @Test
        fun `calculates average from multiple pairs`() {
            val pairs = listOf(
                testFuelLog(odometerKm = 85500.0, liters = 30.0, isFullTank = true) to
                    testFuelLog(odometerKm = 85000.0, liters = 40.0, isFullTank = true),
                testFuelLog(odometerKm = 86000.0, liters = 25.0, isFullTank = true) to
                    testFuelLog(odometerKm = 85500.0, liters = 30.0, isFullTank = true),
            )

            val result = FuelConsumptionCalculator.calculateAvgLPer100km(pairs)

            assertNotNull(result)
            assertEquals(5.5, result!!, 0.01)
        }

        @Test
        fun `returns null for empty pairs`() {
            assertNull(FuelConsumptionCalculator.calculateAvgLPer100km(emptyList()))
        }
    }

    @Nested
    @DisplayName("calculateAvgCostPerKm")
    inner class CalculateAvgCostPerKm {

        @Test
        fun `calculates average cost per km excluding first entry`() {
            val entries = listOf(
                testFuelLog(odometerKm = 85000.0, totalCost = 500.0),
                testFuelLog(odometerKm = 86000.0, totalCost = 600.0),
            )

            val result = FuelConsumptionCalculator.calculateAvgCostPerKm(entries)

            assertNotNull(result)
            // Only the second entry's cost (600) over distance (1000km) = 0.6
            assertEquals(0.6, result!!, 0.01)
        }

        @Test
        fun `returns null for single entry`() {
            val entries = listOf(testFuelLog(odometerKm = 85000.0, totalCost = 500.0))

            assertNull(FuelConsumptionCalculator.calculateAvgCostPerKm(entries))
        }

        @Test
        fun `returns null for empty list`() {
            assertNull(FuelConsumptionCalculator.calculateAvgCostPerKm(emptyList()))
        }
    }

    @Nested
    @DisplayName("calculateTotalCostThisMonth")
    inner class CalculateTotalCostThisMonth {

        @Test
        fun `sums costs for entries within month`() {
            val now = System.currentTimeMillis()
            val entries = listOf(
                testFuelLog(date = now, totalCost = 500.0),
                testFuelLog(date = now - 1000, totalCost = 300.0),
            )

            val result = FuelConsumptionCalculator.calculateTotalCostThisMonth(entries, 0)

            assertEquals(800.0, result, 0.01)
        }

        @Test
        fun `excludes entries before month start`() {
            val now = System.currentTimeMillis()
            val entries = listOf(
                testFuelLog(date = now, totalCost = 500.0),
                testFuelLog(date = 100, totalCost = 300.0),
            )

            val result = FuelConsumptionCalculator.calculateTotalCostThisMonth(entries, now - 500)

            assertEquals(500.0, result, 0.01)
        }
    }

    private fun testFuelLog(
        id: String = "test",
        vehicleId: String = "v-1",
        date: Long = System.currentTimeMillis(),
        odometerKm: Double = 85000.0,
        liters: Double = 40.0,
        pricePerLiter: Double = 12.0,
        totalCost: Double = 480.0,
        isFullTank: Boolean = true,
    ) = FuelLog(
        id = id,
        vehicleId = vehicleId,
        date = date,
        odometerKm = odometerKm,
        liters = liters,
        pricePerLiter = pricePerLiter,
        totalCost = totalCost,
        isFullTank = isFullTank,
    )
}
