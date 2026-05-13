package com.roadmate.core.ui.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AlertStripVariant logic")
class AlertStripVariantTest {

    @Nested
    @DisplayName("breakpoint selection")
    inner class BreakpointSelection {

        @Test
        fun `full variant at 960dp`() {
            val variant = alertStripVariantForWidth(960.dpValue)
            assertEquals(AlertStripVariant.Full, variant)
        }

        @Test
        fun `full variant above 960dp`() {
            val variant = alertStripVariantForWidth(1200.dpValue)
            assertEquals(AlertStripVariant.Full, variant)
        }

        @Test
        fun `compact variant at 480dp`() {
            val variant = alertStripVariantForWidth(480.dpValue)
            assertEquals(AlertStripVariant.Compact, variant)
        }

        @Test
        fun `compact variant at 700dp`() {
            val variant = alertStripVariantForWidth(700.dpValue)
            assertEquals(AlertStripVariant.Compact, variant)
        }

        @Test
        fun `compact variant at 959dp`() {
            val variant = alertStripVariantForWidth(959.dpValue)
            assertEquals(AlertStripVariant.Compact, variant)
        }

        @Test
        fun `dot variant below 480dp`() {
            val variant = alertStripVariantForWidth(479.dpValue)
            assertEquals(AlertStripVariant.Dot, variant)
        }

        @Test
        fun `dot variant at 200dp`() {
            val variant = alertStripVariantForWidth(200.dpValue)
            assertEquals(AlertStripVariant.Dot, variant)
        }
    }

    private val Int.dpValue: Float get() = toFloat()
}
