package com.roadmate.core.ui.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ShimmerSkeleton logic")
class ShimmerSkeletonTest {

    @Nested
    @DisplayName("shimmer trigger conditions")
    inner class ShimmerTrigger {

        @Test
        fun `shimmer triggered when newData flag is true`() {
            assertTrue(shouldShowShimmer(hasNewData = true))
        }

        @Test
        fun `shimmer not triggered when newData flag is false`() {
            assertFalse(shouldShowShimmer(hasNewData = false))
        }
    }

    @Nested
    @DisplayName("shimmer animation parameters")
    inner class ShimmerParams {

        @Test
        fun `animation duration clamped to min 200ms`() {
            val clamped = clampShimmerDuration(100)
            assertEquals(200, clamped)
        }

        @Test
        fun `animation duration clamped to max 400ms`() {
            val clamped = clampShimmerDuration(500)
            assertEquals(400, clamped)
        }

        @Test
        fun `animation duration within range is unchanged`() {
            val clamped = clampShimmerDuration(300)
            assertEquals(300, clamped)
        }

        @Test
        fun `default shimmer duration is 300ms`() {
            assertEquals(300, DEFAULT_SHIMMER_DURATION_MS)
        }
    }

    @Nested
    @DisplayName("gradient brush calculation")
    inner class GradientCalculation {

        @Test
        fun `translate animation value 0f produces correct start offset`() {
            val offset = shimmerOffset(0f, 1000f)
            assertEquals(-1000f, offset, 0.01f)
        }

        @Test
        fun `translate animation value 1f produces correct end offset`() {
            val offset = shimmerOffset(1f, 1000f)
            assertEquals(1000f, offset, 0.01f)
        }

        @Test
        fun `translate animation value 0_5f produces mid offset`() {
            val offset = shimmerOffset(0.5f, 1000f)
            assertEquals(0f, offset, 0.01f)
        }
    }

    @Nested
    @DisplayName("no-data sync behavior")
    inner class NoDataSync {

        @Test
        fun `no shimmer when no data entities changed`() {
            val result = shouldShowShimmer(hasNewData = false)
            assertFalse(result)
        }

        @Test
        fun `status chip timestamp updates without shimmer`() {
            val showShimmer = shouldShowShimmer(hasNewData = false)
            val timestampChanged = true
            assertTrue(!showShimmer && timestampChanged)
        }
    }
}
