package com.roadmate.core.ui.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AttentionBand logic")
class AttentionBandTest {

    @Nested
    @DisplayName("band text formatting")
    inner class BandText {

        @Test
        @DisplayName("warning band shows item due in X km")
        fun warningBandText() {
            val text = formatAttentionBandText(
                itemName = "Oil Change",
                remainingKm = 300.0,
                overdueKm = 0.0,
                isOverdue = false,
            )
            assertEquals("Oil Change due in 300 km", text)
        }

        @Test
        @DisplayName("overdue band shows overdue by X km")
        fun overdueBandText() {
            val text = formatAttentionBandText(
                itemName = "Brake Pads",
                remainingKm = 0.0,
                overdueKm = 500.0,
                isOverdue = true,
            )
            assertEquals("Brake Pads overdue by 500 km", text)
        }
    }

    @Nested
    @DisplayName("stacking logic")
    inner class StackingLogic {

        @Test
        @DisplayName("max 2 visible bands with overflow count")
        fun maxTwoBandsWithOverflow() {
            val bands = listOf(
                AttentionBandItem("Oil Change", 100.0, 0.0, true),
                AttentionBandItem("Brake Pads", 200.0, 0.0, false),
                AttentionBandItem("Tire Rotation", 300.0, 0.0, false),
                AttentionBandItem("Air Filter", 400.0, 0.0, false),
            )
            val result = stackAttentionBands(bands, maxVisible = 2)
            assertEquals(2, result.visible.size)
            assertEquals(2, result.overflowCount)
        }

        @Test
        @DisplayName("no overflow when bands <= max")
        fun noOverflow() {
            val bands = listOf(
                AttentionBandItem("Oil Change", 100.0, 0.0, true),
                AttentionBandItem("Brake Pads", 200.0, 0.0, false),
            )
            val result = stackAttentionBands(bands, maxVisible = 2)
            assertEquals(2, result.visible.size)
            assertEquals(0, result.overflowCount)
        }

        @Test
        @DisplayName("overdue items sorted first, then by remaining km ascending")
        fun sortingOrder() {
            val bands = listOf(
                AttentionBandItem("Tire Rotation", 300.0, 0.0, false),
                AttentionBandItem("Oil Change", 0.0, 500.0, true),
                AttentionBandItem("Brake Pads", 100.0, 0.0, false),
            )
            val result = stackAttentionBands(bands, maxVisible = 3)
            assertEquals("Oil Change", result.visible[0].itemName)
            assertEquals("Brake Pads", result.visible[1].itemName)
            assertEquals("Tire Rotation", result.visible[2].itemName)
        }

        @Test
        @DisplayName("multiple overdue items sorted by overdueKm descending")
        fun multipleOverdueSortedByOverdueKm() {
            val bands = listOf(
                AttentionBandItem("Oil Change", 0.0, 200.0, true),
                AttentionBandItem("Brake Pads", 0.0, 800.0, true),
                AttentionBandItem("Tire Rotation", 0.0, 500.0, true),
            )
            val result = stackAttentionBands(bands, maxVisible = 3)
            assertEquals("Brake Pads", result.visible[0].itemName)
            assertEquals("Tire Rotation", result.visible[1].itemName)
            assertEquals("Oil Change", result.visible[2].itemName)
        }
    }

    @Nested
    @DisplayName("dismiss/defer logic")
    inner class DismissDefer {

        @Test
        @DisplayName("dismissed items are filtered out")
        fun dismissedItemsFiltered() {
            val bands = listOf(
                AttentionBandItem("Oil Change", 100.0, 0.0, false),
                AttentionBandItem("Brake Pads", 200.0, 0.0, false),
            )
            val deferred = setOf("Oil Change")
            val result = bands.filter { it.itemName !in deferred }
            assertEquals(1, result.size)
            assertEquals("Brake Pads", result[0].itemName)
        }

        @Test
        @DisplayName("all items dismissed yields empty list")
        fun allDismissed() {
            val bands = listOf(
                AttentionBandItem("Oil Change", 100.0, 0.0, false),
            )
            val deferred = setOf("Oil Change")
            val result = bands.filter { it.itemName !in deferred }
            assertTrue(result.isEmpty())
        }
    }
}
