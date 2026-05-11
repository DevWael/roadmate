package com.roadmate.core.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadmate.core.ui.theme.RoadMateError
import com.roadmate.core.ui.theme.RoadMateOutline
import com.roadmate.core.ui.theme.RoadMateSecondary
import com.roadmate.core.ui.theme.RoadMateSuccess
import com.roadmate.core.ui.theme.RoadMateTertiary
import kotlin.math.roundToInt

enum class GaugeArcVariant(val sizeDp: Int, val showCenterText: Boolean) {
    Large(sizeDp = 160, showCenterText = true),
    Compact(sizeDp = 48, showCenterText = false),
}

const val GAUGE_CRITICAL_THRESHOLD = 95f

fun gaugeArcColor(percentage: Float): Color {
    return when {
        percentage >= GAUGE_CRITICAL_THRESHOLD -> RoadMateError
        percentage >= 75f -> RoadMateTertiary
        else -> RoadMateSecondary
    }
}

fun gaugeSweepAngle(percentage: Float): Float {
    return percentage / 100f * 270f
}

fun gaugeContentDescription(
    itemName: String,
    percentage: Float,
    remainingKm: Double,
): String {
    val remainingKmInt = kotlin.math.round(remainingKm).toInt()
    val percentInt = percentage.toInt()
    return "$itemName: $percentInt% used, $remainingKmInt km remaining until next service"
}

fun shouldAnimateReset(animateReset: Boolean, reduceMotion: Boolean): Boolean {
    return animateReset && !reduceMotion
}

@Composable
fun GaugeArc(
    percentage: Float,
    variant: GaugeArcVariant,
    modifier: Modifier = Modifier,
    itemName: String = "",
    remainingKm: Double = 0.0,
    animateReset: Boolean = false,
) {
    val context = LocalContext.current
    val reduceMotion = remember {
        try {
            android.provider.Settings.Global.getInt(
                context.contentResolver,
                android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
                1,
            ) == 0
        } catch (_: Exception) {
            false
        }
    }

    val displayPercentage = if (animateReset && !reduceMotion) {
        val animated by animateFloatAsState(
            targetValue = percentage,
            animationSpec = tween(
                durationMillis = 600,
                easing = LinearOutSlowInEasing,
            ),
            label = "gauge-reset",
        )
        animated
    } else {
        percentage
    }

    val arcColor = if (animateReset && !reduceMotion && displayPercentage > 0f) {
        RoadMateSuccess
    } else {
        gaugeArcColor(displayPercentage)
    }
    val sweepAngle = gaugeSweepAngle(displayPercentage)
    val isCritical = displayPercentage >= GAUGE_CRITICAL_THRESHOLD

    val pulseAlpha = if (isCritical && !reduceMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "gauge-pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500),
            ),
            label = "gauge-pulse-alpha",
        )
        alpha
    } else {
        1f
    }

    val sizeDp = variant.sizeDp.dp
    val semanticsDesc = if (itemName.isNotEmpty()) {
        gaugeContentDescription(itemName, displayPercentage, remainingKm)
    } else ""

    Box(
        modifier = modifier
            .size(sizeDp)
            .then(
                if (itemName.isNotEmpty()) {
                    Modifier.semantics { contentDescription = semanticsDesc }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val strokeWidth = if (variant == GaugeArcVariant.Large) 8.dp else 4.dp

        Canvas(
            modifier = Modifier.size(sizeDp),
        ) {
            val canvasSize = size
            val stroke = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
            )
            drawArc(
                color = RoadMateOutline,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                size = Size(
                    canvasSize.width - strokeWidth.toPx(),
                    canvasSize.height - strokeWidth.toPx(),
                ),
                topLeft = androidx.compose.ui.geometry.Offset(
                    strokeWidth.toPx() / 2,
                    strokeWidth.toPx() / 2,
                ),
                style = stroke,
            )
            if (sweepAngle > 0f) {
                drawArc(
                    color = arcColor.copy(alpha = pulseAlpha),
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    size = Size(
                        canvasSize.width - strokeWidth.toPx(),
                        canvasSize.height - strokeWidth.toPx(),
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(
                        strokeWidth.toPx() / 2,
                        strokeWidth.toPx() / 2,
                    ),
                    style = stroke,
                )
            }
        }

        if (variant.showCenterText) {
            Text(
                text = "${displayPercentage.roundToInt()}%",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = arcColor,
                ),
            )
        }
    }
}
