package com.roadmate.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Corner radius tokens for RoadMate.
 */
object RoadMateCorners {
    val card: Dp = 4.dp
    val panel: Dp = 8.dp
}

/**
 * M3 [Shapes] theme for RoadMate — angular automotive aesthetic.
 *
 * All shapes default to 4dp rounded corners.
 */
val RoadMateShapes = Shapes(
    extraSmall = RoundedCornerShape(RoadMateCorners.card),
    small = RoundedCornerShape(RoadMateCorners.card),
    medium = RoundedCornerShape(RoadMateCorners.card),
    large = RoundedCornerShape(RoadMateCorners.card),
    extraLarge = RoundedCornerShape(RoadMateCorners.card),
)
