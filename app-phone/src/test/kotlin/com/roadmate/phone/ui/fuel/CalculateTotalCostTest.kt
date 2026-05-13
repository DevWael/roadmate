package com.roadmate.phone.ui.fuel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("calculateTotalCost")
class CalculateTotalCostTest {

    @Nested
    @DisplayName("auto-calculation")
    inner class AutoCalculation {

        @Test
        fun `multiplies liters by price per liter`() {
            val result = calculateTotalCost("45.0", "12.75")
            assertEquals("573.75", result)
        }

        @Test
        fun `returns empty when liters is empty`() {
            assertEquals("", calculateTotalCost("", "12.75"))
        }

        @Test
        fun `returns empty when price is empty`() {
            assertEquals("", calculateTotalCost("45.0", ""))
        }

        @Test
        fun `returns empty when liters is not a number`() {
            assertEquals("", calculateTotalCost("abc", "12.75"))
        }

        @Test
        fun `handles decimal precision`() {
            val result = calculateTotalCost("33.33", "5.55")
            assertEquals("184.98", result)
        }

        @Test
        fun `handles zero values`() {
            assertEquals("0.00", calculateTotalCost("0", "12.75"))
        }
    }
}
