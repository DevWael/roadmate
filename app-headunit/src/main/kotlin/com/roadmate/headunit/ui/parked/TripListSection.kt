package com.roadmate.headunit.ui.parked

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.ui.theme.RoadMateSpacing
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripListSection(
    trips: List<Trip>,
    modifier: Modifier = Modifier,
) {
    if (trips.isEmpty()) {
        EmptyTripState(modifier = modifier)
        return
    }

    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.sm),
    ) {
        items(trips, key = { it.id }) { trip ->
            TripCard(
                trip = trip,
                numberFormatter = numberFormatter,
                dateFormatter = dateFormatter,
            )
        }
    }
}

@Composable
private fun TripCard(
    trip: Trip,
    numberFormatter: NumberFormat,
    dateFormatter: SimpleDateFormat,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RoadMateSpacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = dateFormatter.format(Date(trip.startTime)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (trip.status == TripStatus.INTERRUPTED) {
                    Text(
                        text = "\u26A1 Interrupted",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(RoadMateSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TripMetric(
                    label = "Distance",
                    value = "${numberFormatter.format(trip.distanceKm)} km",
                )
                TripMetric(
                    label = "Duration",
                    value = formatDuration(trip.durationMs),
                )
                TripMetric(
                    label = "Avg Speed",
                    value = "${numberFormatter.format(trip.avgSpeedKmh)} km/h",
                )
            }
        }
    }
}

@Composable
private fun TripMetric(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EmptyTripState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "\uD83D\uDE97",
            style = MaterialTheme.typography.displayMedium,
        )
        Spacer(modifier = Modifier.height(RoadMateSpacing.lg))
        Text(
            text = "No trips recorded yet.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Drive to start tracking.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun formatDuration(durationMs: Long): String {
    val totalMinutes = durationMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        String.format("%d:%02d", hours, minutes)
    } else {
        String.format("0:%02d", minutes)
    }
}
