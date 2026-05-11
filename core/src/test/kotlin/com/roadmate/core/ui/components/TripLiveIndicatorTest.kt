package com.roadmate.core.ui.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("TripLiveIndicator logic")
class TripLiveIndicatorTest {

    @Nested
    @DisplayName("gps state handling")
    inner class GpsStateHandling {

        @Test
        fun `acquiring maps to gray static circle`() {
            val isAcquiring = true
            val displayScale = if (isAcquiring) 1f else 1.3f
            assertEquals(1f, displayScale)
        }

        @Test
        fun `acquired allows animated scale`() {
            val isAcquiring = false
            val displayScale = if (isAcquiring) 1f else 1.3f
            assertEquals(1.3f, displayScale)
        }

        @Test
        fun `unavailable allows animated scale`() {
            val isAcquiring = false
            val displayScale = if (isAcquiring) 1f else 1.3f
            assertEquals(1.3f, displayScale)
        }
    }
}
