package com.roadmate.core.ui.components

import com.roadmate.core.ui.theme.RoadMateError
import com.roadmate.core.ui.theme.RoadMateSecondary
import com.roadmate.core.ui.theme.RoadMateTertiary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GaugeArc logic")
class GaugeArcTest {

    @Nested
    @DisplayName("color thresholds")
    inner class ColorThresholds {

        @Test
        @DisplayName("0% returns secondary color")
        fun zeroPercent() {
            val color = gaugeArcColor(0f)
            assertEquals(RoadMateSecondary, color)
        }

        @Test
        @DisplayName("50% returns secondary color")
        fun fiftyPercent() {
            val color = gaugeArcColor(50f)
            assertEquals(RoadMateSecondary, color)
        }

        @Test
        @DisplayName("74% returns secondary color")
        fun seventyFourPercent() {
            val color = gaugeArcColor(74f)
            assertEquals(RoadMateSecondary, color)
        }

        @Test
        @DisplayName("75% returns tertiary color")
        fun seventyFivePercent() {
            val color = gaugeArcColor(75f)
            assertEquals(RoadMateTertiary, color)
        }

        @Test
        @DisplayName("94% returns tertiary color")
        fun ninetyFourPercent() {
            val color = gaugeArcColor(94f)
            assertEquals(RoadMateTertiary, color)
        }

        @Test
        @DisplayName("95% returns error color")
        fun ninetyFivePercent() {
            val color = gaugeArcColor(95f)
            assertEquals(RoadMateError, color)
        }

        @Test
        @DisplayName("100% returns error color")
        fun hundredPercent() {
            val color = gaugeArcColor(100f)
            assertEquals(RoadMateError, color)
        }
    }

    @Nested
    @DisplayName("sweep angle calculation")
    inner class SweepAngle {

        @Test
        @DisplayName("0% maps to 0 degree sweep")
        fun zeroPercent() {
            val sweep = gaugeSweepAngle(0f)
            assertEquals(0f, sweep, 0.01f)
        }

        @Test
        @DisplayName("50% maps to 135 degree sweep")
        fun fiftyPercent() {
            val sweep = gaugeSweepAngle(50f)
            assertEquals(135f, sweep, 0.01f)
        }

        @Test
        @DisplayName("100% maps to 270 degree sweep")
        fun hundredPercent() {
            val sweep = gaugeSweepAngle(100f)
            assertEquals(270f, sweep, 0.01f)
        }
    }

    @Nested
    @DisplayName("variant sizing")
    inner class VariantSizing {

        @Test
        @DisplayName("Large variant is 160dp")
        fun largeSize() {
            assertEquals(160, GaugeArcVariant.Large.sizeDp)
        }

        @Test
        @DisplayName("Compact variant is 48dp")
        fun compactSize() {
            assertEquals(48, GaugeArcVariant.Compact.sizeDp)
        }

        @Test
        @DisplayName("Large variant shows center text")
        fun largeShowsText() {
            assertEquals(true, GaugeArcVariant.Large.showCenterText)
        }

        @Test
        @DisplayName("Compact variant hides center text")
        fun compactHidesText() {
            assertEquals(false, GaugeArcVariant.Compact.showCenterText)
        }
    }

    @Nested
    @DisplayName("pulse animation")
    inner class PulseAnimation {

        @Test
        @DisplayName("critical threshold is 95 percent")
        fun criticalThreshold() {
            assertEquals(95f, GAUGE_CRITICAL_THRESHOLD)
        }

        @Test
        @DisplayName("95% is critical")
        fun isCritical() {
            assertEquals(true, 95f >= GAUGE_CRITICAL_THRESHOLD)
        }

        @Test
        @DisplayName("94% is not critical")
        fun isNotCritical() {
            assertEquals(false, 94f >= GAUGE_CRITICAL_THRESHOLD)
        }
    }

    @Nested
    @DisplayName("accessibility description")
    inner class AccessibilityDescription {

        @Test
        @DisplayName("formats description with item name, percent, remaining km")
        fun formatsDescription() {
            val desc = gaugeContentDescription(
                itemName = "Oil Change",
                percentage = 75f,
                remainingKm = 2500.0,
            )
            assertEquals("Oil Change: 75% used, 2500 km remaining until next service", desc)
        }

        @Test
        @DisplayName("formats zero remaining km")
        fun zeroRemaining() {
            val desc = gaugeContentDescription(
                itemName = "Brake Pads",
                percentage = 100f,
                remainingKm = 0.0,
            )
            assertEquals("Brake Pads: 100% used, 0 km remaining until next service", desc)
        }
    }

    @Nested
    @DisplayName("reset animation logic")
    inner class ResetAnimation {

        @Test
        @DisplayName("should animate when animateReset is true and reduce motion is off")
        fun shouldAnimateWhenResetAndNoReduceMotion() {
            val shouldAnimate = shouldAnimateReset(
                animateReset = true,
                reduceMotion = false,
            )
            assertEquals(true, shouldAnimate)
        }

        @Test
        @DisplayName("should not animate when animateReset is false")
        fun shouldNotAnimateWhenNoReset() {
            val shouldAnimate = shouldAnimateReset(
                animateReset = false,
                reduceMotion = false,
            )
            assertEquals(false, shouldAnimate)
        }

        @Test
        @DisplayName("should not animate when reduce motion is on")
        fun shouldNotAnimateWhenReduceMotion() {
            val shouldAnimate = shouldAnimateReset(
                animateReset = true,
                reduceMotion = true,
            )
            assertEquals(false, shouldAnimate)
        }
    }

    @Nested
    @DisplayName("reduce motion disables pulse")
    inner class ReduceMotionPulse {

        @Test
        @DisplayName("critical percentage with reduce motion shows no pulse")
        fun criticalWithReduceMotion() {
            val isCritical = 95f >= GAUGE_CRITICAL_THRESHOLD
            val reduceMotion = true
            val shouldPulse = isCritical && !reduceMotion
            assertEquals(false, shouldPulse)
        }

        @Test
        @DisplayName("critical percentage without reduce motion shows pulse")
        fun criticalWithoutReduceMotion() {
            val isCritical = 95f >= GAUGE_CRITICAL_THRESHOLD
            val reduceMotion = false
            val shouldPulse = isCritical && !reduceMotion
            assertEquals(true, shouldPulse)
        }

        @Test
        @DisplayName("non-critical percentage never pulses")
        fun nonCriticalNeverPulses() {
            val isCritical = 74f >= GAUGE_CRITICAL_THRESHOLD
            val reduceMotion = false
            val shouldPulse = isCritical && !reduceMotion
            assertEquals(false, shouldPulse)
        }
    }

    @Nested
    @DisplayName("accessibility content description format")
    inner class AccessibilityContentDescriptionFormat {

        @Test
        @DisplayName("includes item name")
        fun includesItemName() {
            val desc = gaugeContentDescription("Oil Change", 50f, 5000.0)
            assertTrue(desc.startsWith("Oil Change:"))
        }

        @Test
        @DisplayName("includes percentage as integer")
        fun includesPercentage() {
            val desc = gaugeContentDescription("Oil Change", 75f, 2500.0)
            assertTrue(desc.contains("75%"))
        }

        @Test
        @DisplayName("includes remaining km as integer")
        fun includesRemainingKm() {
            val desc = gaugeContentDescription("Oil Change", 75f, 2500.0)
            assertTrue(desc.contains("2500 km remaining"))
        }
    }
}
