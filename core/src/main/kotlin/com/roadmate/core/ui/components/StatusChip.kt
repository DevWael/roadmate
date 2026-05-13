package com.roadmate.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

fun formatTimeAgo(timestampMs: Long, nowMs: Long): String {
    if (timestampMs <= 0L) return "never"
    val elapsed = nowMs - timestampMs
    if (elapsed < 0L) return "just now"
    val minutes = elapsed / 60_000
    val hours = elapsed / 3_600_000
    return when {
        minutes < 1L -> "just now"
        minutes < 60L -> "${minutes}m ago"
        hours < 24L -> "${hours}h ago"
        else -> "${hours / 24}d ago"
    }
}

fun syncStatusLabel(
    isConnected: Boolean,
    isConnecting: Boolean,
    isSyncing: Boolean,
    isFailed: Boolean,
    lastSyncTimestamp: Long,
    currentTimeMs: Long,
): String {
    if (!isConnected && !isConnecting) return "Not connected"
    if (isConnecting) return "Connecting..."
    if (isSyncing) return "Syncing..."
    if (isFailed) return "Sync failed"
    return "Synced ${formatTimeAgo(lastSyncTimestamp, currentTimeMs)}"
}

fun shouldRevertFromFailed(
    failedAtMs: Long,
    nowMs: Long,
    revertDelayMs: Long = 10_000,
): Boolean {
    if (failedAtMs <= 0L) return false
    return (nowMs - failedAtMs) >= revertDelayMs
}

fun shouldPulse(isSyncing: Boolean): Boolean = isSyncing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusChip(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    outlined: Boolean = false,
    pulsing: Boolean = false,
    showBadge: Boolean = false,
) {
    val pulseScale = if (pulsing) {
        val infiniteTransition = rememberInfiniteTransition(label = "status-pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "status-pulse-scale",
        )
        scale
    } else {
        1f
    }

    val containerColor = if (outlined) {
        Color.Transparent
    } else {
        color.copy(alpha = 0.15f)
    }

    AssistChip(
        onClick = {},
        enabled = false,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
        },
        leadingIcon = {
            BadgedBox(
                badge = {
                    if (showBadge) {
                        Badge { Text("!") }
                    }
                },
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .size(24.dp)
                        .scale(pulseScale),
                    tint = color,
                )
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor,
            disabledLabelColor = color,
            disabledLeadingIconContentColor = color,
        ),
        border = AssistChipDefaults.assistChipBorder(
            borderColor = if (outlined) color else Color.Transparent,
            disabledBorderColor = if (outlined) color else Color.Transparent,
            enabled = false,
        ),
        modifier = modifier,
    )
}

@Composable
fun SyncStatusChip(
    isConnected: Boolean,
    isConnecting: Boolean,
    isSyncing: Boolean,
    isFailed: Boolean,
    failedAtMs: Long,
    lastSyncTimestamp: Long,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    timeProvider: () -> Long = { System.currentTimeMillis() },
) {
    var currentTime by remember { mutableLongStateOf(timeProvider()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            currentTime = timeProvider()
        }
    }

    val effectiveFailed = isFailed && !shouldRevertFromFailed(failedAtMs, currentTime)
    val color = when {
        !isConnected && !isConnecting -> MaterialTheme.colorScheme.onSurfaceVariant
        isConnecting -> MaterialTheme.colorScheme.primary
        isSyncing -> MaterialTheme.colorScheme.primary
        effectiveFailed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val label = syncStatusLabel(
        isConnected = isConnected,
        isConnecting = isConnecting,
        isSyncing = isSyncing,
        isFailed = effectiveFailed,
        lastSyncTimestamp = lastSyncTimestamp,
        currentTimeMs = currentTime,
    )
    val outlined = !isConnected && !isConnecting
    val pulsing = shouldPulse(isSyncing)
    val showBadge = effectiveFailed

    StatusChip(
        icon = icon,
        label = label,
        color = color,
        modifier = modifier,
        contentDescription = label,
        outlined = outlined,
        pulsing = pulsing,
        showBadge = showBadge,
    )
}
