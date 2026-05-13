package com.roadmate.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.roadmate.core.ui.theme.RoadMateCorners

const val DEFAULT_SHIMMER_DURATION_MS = 300
const val SHIMMER_DURATION_MIN_MS = 200
const val SHIMMER_DURATION_MAX_MS = 400

fun shouldShowShimmer(hasNewData: Boolean): Boolean = hasNewData

fun clampShimmerDuration(durationMs: Int): Int {
    return durationMs.coerceIn(SHIMMER_DURATION_MIN_MS, SHIMMER_DURATION_MAX_MS)
}

fun shimmerOffset(progress: Float, width: Float): Float {
    return (progress * 2f - 1f) * width
}

@Composable
fun ShimmerSkeleton(
    modifier: Modifier = Modifier,
    durationMs: Int = DEFAULT_SHIMMER_DURATION_MS,
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val surfaceBright = MaterialTheme.colorScheme.surfaceBright

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = clampShimmerDuration(durationMs),
                easing = LinearEasing,
            ),
        ),
        label = "shimmer-progress",
    )

    val brush = Brush.linearGradient(
        colors = listOf(surfaceVariant, surfaceBright, surfaceVariant),
        start = Offset(shimmerOffset(progress, 1000f), 0f),
        end = Offset(shimmerOffset(progress, 1000f) + 1000f, 0f),
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RoadMateCorners.card))
            .background(brush),
    )
}

@Composable
fun ShimmerContentWrapper(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (isLoading) {
        ShimmerSkeleton(modifier = modifier)
    } else {
        content()
    }
}
