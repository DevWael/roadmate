package com.roadmate.core.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RoadMate Typography")
class TypeTest {

    @Nested
    @DisplayName("Head unit typography scale")
    inner class HeadUnitTypography {

        @Test
        fun `displayLarge is 48sp Bold`() {
            val style = HeadUnitTypographyTokens.displayLarge
            assertEquals(48.sp, style.fontSize)
            assertEquals(FontWeight.Bold, style.fontWeight)
        }

        @Test
        fun `headlineMedium is 28sp SemiBold`() {
            val style = HeadUnitTypographyTokens.headlineMedium
            assertEquals(28.sp, style.fontSize)
            assertEquals(FontWeight.SemiBold, style.fontWeight)
        }

        @Test
        fun `titleLarge is 22sp Medium`() {
            val style = HeadUnitTypographyTokens.titleLarge
            assertEquals(22.sp, style.fontSize)
            assertEquals(FontWeight.Medium, style.fontWeight)
        }

        @Test
        fun `bodyLarge is 18sp Regular`() {
            val style = HeadUnitTypographyTokens.bodyLarge
            assertEquals(18.sp, style.fontSize)
            assertEquals(FontWeight.Normal, style.fontWeight)
        }

        @Test
        fun `labelLarge is 16sp Medium`() {
            val style = HeadUnitTypographyTokens.labelLarge
            assertEquals(16.sp, style.fontSize)
            assertEquals(FontWeight.Medium, style.fontWeight)
        }
    }

    @Nested
    @DisplayName("Phone typography scale")
    inner class PhoneTypography {

        @Test
        fun `displayLarge is 36sp Bold`() {
            val style = PhoneTypographyTokens.displayLarge
            assertEquals(36.sp, style.fontSize)
            assertEquals(FontWeight.Bold, style.fontWeight)
        }

        @Test
        fun `headlineMedium is 24sp SemiBold`() {
            val style = PhoneTypographyTokens.headlineMedium
            assertEquals(24.sp, style.fontSize)
            assertEquals(FontWeight.SemiBold, style.fontWeight)
        }

        @Test
        fun `titleMedium is 16sp Medium`() {
            val style = PhoneTypographyTokens.titleMedium
            assertEquals(16.sp, style.fontSize)
            assertEquals(FontWeight.Medium, style.fontWeight)
        }

        @Test
        fun `bodyLarge is 16sp Regular`() {
            val style = PhoneTypographyTokens.bodyLarge
            assertEquals(16.sp, style.fontSize)
            assertEquals(FontWeight.Normal, style.fontWeight)
        }

        @Test
        fun `bodyMedium is 14sp Regular`() {
            val style = PhoneTypographyTokens.bodyMedium
            assertEquals(14.sp, style.fontSize)
            assertEquals(FontWeight.Normal, style.fontWeight)
        }

        @Test
        fun `labelSmall is 11sp Medium`() {
            val style = PhoneTypographyTokens.labelSmall
            assertEquals(11.sp, style.fontSize)
            assertEquals(FontWeight.Medium, style.fontWeight)
        }
    }
}
