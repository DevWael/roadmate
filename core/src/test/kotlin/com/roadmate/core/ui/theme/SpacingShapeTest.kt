package com.roadmate.core.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RoadMate Spacing & Shape tokens")
class SpacingShapeTest {

    @Test
    fun `xs is 4dp`() {
        assertEquals(4.dp, RoadMateSpacing.xs)
    }

    @Test
    fun `sm is 8dp`() {
        assertEquals(8.dp, RoadMateSpacing.sm)
    }

    @Test
    fun `md is 12dp`() {
        assertEquals(12.dp, RoadMateSpacing.md)
    }

    @Test
    fun `lg is 16dp`() {
        assertEquals(16.dp, RoadMateSpacing.lg)
    }

    @Test
    fun `xl is 20dp`() {
        assertEquals(20.dp, RoadMateSpacing.xl)
    }

    @Test
    fun `xxl is 24dp`() {
        assertEquals(24.dp, RoadMateSpacing.xxl)
    }

    @Test
    fun `xxxl is 32dp`() {
        assertEquals(32.dp, RoadMateSpacing.xxxl)
    }

    @Test
    fun `card corner radius is 4dp`() {
        assertEquals(4.dp, RoadMateCorners.card)
    }
}
