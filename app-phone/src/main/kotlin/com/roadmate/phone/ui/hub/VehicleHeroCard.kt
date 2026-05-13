package com.roadmate.phone.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadmate.core.ui.theme.RoadMateSpacing
import com.roadmate.core.ui.theme.RoadMateTertiary
import java.util.concurrent.TimeUnit

@Composable
fun VehicleHeroCard(
    vehicleName: String,
    odometerKm: Double,
    odometerUnit: String,
    lastSyncTimestamp: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(RoadMateSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(RoadMateSpacing.lg))
            Text(
                text = vehicleName,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        Spacer(modifier = Modifier.height(RoadMateSpacing.xs))
        Text(
            text = formatOdometer(odometerKm, odometerUnit),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = formatSyncStatus(lastSyncTimestamp),
            style = MaterialTheme.typography.labelSmall,
            color = syncStatusColor(lastSyncTimestamp),
        )
    }
}

internal fun formatOdometer(odometerKm: Double, unit: String): String {
    val formatted = String.format("%,.0f", odometerKm)
    return "$formatted $unit"
}

internal fun formatSyncStatus(lastSyncTimestamp: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (lastSyncTimestamp == 0L) return "Not yet synced"
    val diffMs = nowMs - lastSyncTimestamp
    if (diffMs < 0) return "Last synced: just now"
    val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    return when {
        diffMinutes < 5 -> "Last synced: just now"
        diffMinutes < 60 -> "Last synced: $diffMinutes min ago"
        else -> {
            val hours = TimeUnit.MINUTES.toHours(diffMinutes)
            "Last synced: $hours hour${if (hours != 1L) "s" else ""} ago"
        }
    }
}

internal fun syncStatusColor(lastSyncTimestamp: Long): Color {
    return if (lastSyncTimestamp == 0L) RoadMateTertiary else Color.Unspecified
}
