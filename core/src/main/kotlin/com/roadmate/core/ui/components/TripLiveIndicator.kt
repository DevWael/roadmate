package com.roadmate.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.roadmate.core.model.GpsState

@Composable
fun TripLiveIndicator(
    gpsState: GpsState,
    modifier: Modifier = Modifier,
) {
    val isAcquiring = gpsState is GpsState.Acquiring

    val displayScale = if (!isAcquiring) {
        val infiniteTransition = rememberInfiniteTransition(label = "trip-pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 750, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse-scale",
        )
        scale
    } else {
        1f
    }
    val color = if (isAcquiring) Color.Gray else Color(0xFF4CAF50)

    Box(
        modifier = modifier
            .size(12.dp)
            .scale(displayScale)
            .clip(CircleShape)
            .background(color),
    )
}
