package com.roadmate.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RoadMate Color System")
class ColorTest {

    @Nested
    @DisplayName("Color tokens")
    inner class ColorTokens {

        @Test
        fun `surface color is 0A0A0A`() {
            assertEquals(Color(0xFF0A0A0A), RoadMateSurface)
        }

        @Test
        fun `surfaceVariant color is 1A1A1A`() {
            assertEquals(Color(0xFF1A1A1A), RoadMateSurfaceVariant)
        }

        @Test
        fun `surfaceContainer color is 121212`() {
            assertEquals(Color(0xFF121212), RoadMateSurfaceContainer)
        }

        @Test
        fun `primary color is 4FC3F7`() {
            assertEquals(Color(0xFF4FC3F7), RoadMatePrimary)
        }

        @Test
        fun `secondary color is 80CBC4`() {
            assertEquals(Color(0xFF80CBC4), RoadMateSecondary)
        }

        @Test
        fun `tertiary color is FFB74D`() {
            assertEquals(Color(0xFFFFB74D), RoadMateTertiary)
        }

        @Test
        fun `error color is EF5350`() {
            assertEquals(Color(0xFFEF5350), RoadMateError)
        }

        @Test
        fun `onSurface color is E8E8E8`() {
            assertEquals(Color(0xFFE8E8E8), RoadMateOnSurface)
        }

        @Test
        fun `onSurfaceVariant color is 9E9E9E`() {
            assertEquals(Color(0xFF9E9E9E), RoadMateOnSurfaceVariant)
        }

        @Test
        fun `outline color is 4A4A4A`() {
            assertEquals(Color(0xFF4A4A4A), RoadMateOutline)
        }
    }

    @Nested
    @DisplayName("Dark color scheme")
    inner class DarkColorScheme {

        @Test
        fun `roadMateDarkColorScheme returns correct surface`() {
            val scheme = roadMateDarkColorScheme()
            assertEquals(RoadMateSurface, scheme.surface)
        }

        @Test
        fun `roadMateDarkColorScheme returns correct surfaceVariant`() {
            val scheme = roadMateDarkColorScheme()
            assertEquals(RoadMateSurfaceVariant, scheme.surfaceVariant)
        }

        @Test
        fun `roadMateDarkColorScheme returns correct surfaceContainer`() {
            val scheme = roadMateDarkColorScheme()
            assertEquals(RoadMateSurfaceContainer, scheme.surfaceContainer)
        }

        @Test
        fun `roadMateDarkColorScheme returns correct primary`() {
            val scheme = roadMateDarkColorScheme()
            assertEquals(RoadMatePrimary, scheme.primary)
        }

        @Test
        fun `roadMateDarkColorScheme returns correct secondary`() {
            val scheme = roadMateDarkColorScheme()
            assertEquals(RoadMateSecondary, scheme.secondary)
        }

        @Test
        fun `roadMateDarkColorScheme returns correct tertiary`() {
            val scheme = roadMateDarkColorScheme()
            assertEquals(RoadMateTertiary, scheme.tertiary)
        }

        @Test
        fun `roadMateDarkColorScheme returns correct error`() {
            val scheme = roadMateDarkColorScheme()
            assertEquals(RoadMateError, scheme.error)
        }

        @Test
        fun `roadMateDarkColorScheme returns correct onSurface`() {
            val scheme = roadMateDarkColorScheme()
            assertEquals(RoadMateOnSurface, scheme.onSurface)
        }

        @Test
        fun `roadMateDarkColorScheme returns correct onSurfaceVariant`() {
            val scheme = roadMateDarkColorScheme()
            assertEquals(RoadMateOnSurfaceVariant, scheme.onSurfaceVariant)
        }

        @Test
        fun `roadMateDarkColorScheme returns correct outline`() {
            val scheme = roadMateDarkColorScheme()
            assertEquals(RoadMateOutline, scheme.outline)
        }
    }
}
