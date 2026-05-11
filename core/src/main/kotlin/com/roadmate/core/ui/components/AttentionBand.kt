package com.roadmate.core.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.roadmate.core.ui.theme.RoadMateError
import com.roadmate.core.ui.theme.RoadMateSpacing
import com.roadmate.core.ui.theme.RoadMateTertiary

data class AttentionBandItem(
    val itemName: String,
    val remainingKm: Double,
    val overdueKm: Double,
    val isOverdue: Boolean,
)

data class StackedBands(
    val visible: List<AttentionBandItem>,
    val overflowCount: Int,
)

fun formatAttentionBandText(
    itemName: String,
    remainingKm: Double,
    overdueKm: Double,
    isOverdue: Boolean,
): String {
    val kmInt = kotlin.math.round(if (isOverdue) overdueKm else remainingKm).toInt()
    return if (isOverdue) {
        "$itemName overdue by $kmInt km"
    } else {
        "$itemName due in $kmInt km"
    }
}

fun stackAttentionBands(
    bands: List<AttentionBandItem>,
    maxVisible: Int = 2,
): StackedBands {
    val sorted = bands.sortedWith(
        compareByDescending<AttentionBandItem> { it.isOverdue }
            .thenByDescending { it.overdueKm }
            .thenBy { it.remainingKm }
    )
    val visible = sorted.take(maxVisible)
    val overflowCount = (bands.size - maxVisible).coerceAtLeast(0)
    return StackedBands(visible, overflowCount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttentionBand(
    item: AttentionBandItem,
    onDismiss: () -> Unit = {},
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDismiss()
                true
            } else {
                false
            }
        },
    )

    val backgroundColor = if (item.isOverdue) RoadMateError else RoadMateTertiary
    val text = formatAttentionBandText(
        itemName = item.itemName,
        remainingKm = item.remainingKm,
        overdueKm = item.overdueKm,
        isOverdue = item.isOverdue,
    )

    val pulseScale = if (item.isOverdue) {
        val infiniteTransition = rememberInfiniteTransition(label = "band-pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500),
            ),
            label = "band-pulse-scale",
        )
        scale
    } else {
        1f
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            )
        },
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .background(backgroundColor)
                .clickable(onClick = onTap)
                .padding(horizontal = RoadMateSpacing.lg, vertical = RoadMateSpacing.md),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun AttentionBandList(
    bands: List<AttentionBandItem>,
    deferredItemNames: Set<String> = emptySet(),
    onDismiss: (String) -> Unit = {},
    onTap: (String) -> Unit = {},
    maxVisible: Int = 2,
    modifier: Modifier = Modifier,
) {
    val active = bands.filter { it.itemName !in deferredItemNames }
    val stacked = stackAttentionBands(active, maxVisible)

    if (stacked.visible.isEmpty() && stacked.overflowCount == 0) return

    Column(modifier = modifier.fillMaxWidth()) {
        stacked.visible.forEach { item ->
            AttentionBand(
                item = item,
                onDismiss = { onDismiss(item.itemName) },
                onTap = { onTap(item.itemName) },
            )
        }
        if (stacked.overflowCount > 0) {
            Text(
                text = "+${stacked.overflowCount} more",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = RoadMateSpacing.lg,
                    vertical = RoadMateSpacing.xs,
                ),
            )
        }
    }
}
