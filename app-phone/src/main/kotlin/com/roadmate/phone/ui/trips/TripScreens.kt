package com.roadmate.phone.ui.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.model.UiState
import com.roadmate.core.ui.theme.RoadMateSpacing
import com.roadmate.core.ui.theme.RoadMateTertiary
import com.roadmate.phone.ui.components.RoadMateScaffold
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun TripListScreen(
    onTripClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TripListViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is UiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        is UiState.Success -> {
            val trips = state.data
            if (trips.isEmpty()) {
                EmptyTripState(modifier = modifier)
            } else {
                TripListContent(
                    trips = trips,
                    onTripClick = onTripClick,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun EmptyTripState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(RoadMateSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(RoadMateSpacing.lg))
        Text(
            text = "No trips recorded yet. Drive to start tracking.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TripListContent(
    trips: List<Trip>,
    onTripClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(RoadMateSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
    ) {
        items(
            items = trips,
            key = { it.id },
        ) { trip ->
            TripCard(
                trip = trip,
                onClick = { onTripClick(trip.id) },
            )
        }
    }
}

@Composable
private fun TripCard(
    trip: Trip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp),
            )
            .clickable(onClick = onClick)
            .padding(RoadMateSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatDate(trip.startTime),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.height(RoadMateSpacing.xs))
            Row(
                horizontalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f km", trip.distanceKm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatDuration(trip.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = String.format(Locale.US, "%.0f km/h", trip.avgSpeedKmh),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (trip.status == TripStatus.INTERRUPTED) {
                Spacer(modifier = Modifier.height(RoadMateSpacing.xs))
                Text(
                    text = "\u26A1 Interrupted",
                    style = MaterialTheme.typography.labelSmall,
                    color = RoadMateTertiary,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun TripDetailScreen(
    tripId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TripDetailViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(tripId) {
        viewModel.loadTrip(tripId)
    }

    when (val state = uiState) {
        is UiState.Loading -> {
            RoadMateScaffold(
                title = "Trip Detail",
                onBack = onBack,
            ) {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        is UiState.Error -> {
            RoadMateScaffold(
                title = "Trip Detail",
                onBack = onBack,
            ) {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        is UiState.Success -> {
            TripDetailContent(
                uiState = state.data,
                onBack = onBack,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun TripDetailContent(
    uiState: TripDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trip = uiState.trip
    RoadMateScaffold(
        title = "Trip Detail",
        onBack = onBack,
    ) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(RoadMateSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp),
                        )
                        .padding(RoadMateSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
                ) {
                    Text(
                        text = "Date / Time",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatDateTimeRange(trip.startTime, trip.endTime),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp),
                        )
                        .padding(RoadMateSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
                ) {
                    StatRow(label = "Distance", value = String.format(Locale.US, "%.1f km", trip.distanceKm))
                    StatRow(label = "Duration", value = formatDuration(trip.durationMs))
                    StatRow(label = "Avg Speed", value = String.format(Locale.US, "%.0f km/h", trip.avgSpeedKmh))
                    StatRow(label = "Max Speed", value = String.format(Locale.US, "%.0f km/h", trip.maxSpeedKmh))
                    StatRow(label = "Est. Fuel", value = String.format(Locale.US, "%.1f L", trip.estimatedFuelL))
                }
            }

            if (uiState.routeSummary != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp),
                            )
                            .padding(RoadMateSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
                    ) {
                        Text(
                            text = "Route Summary",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatCoordinate(uiState.routeSummary.startLat, uiState.routeSummary.startLng),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatCoordinate(uiState.routeSummary.endLat, uiState.routeSummary.endLng),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

internal fun formatDate(epochMs: Long): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    val dateTime = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
    return formatter.format(dateTime)
}

internal fun formatDateTimeRange(startMs: Long, endMs: Long?): String {
    val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    val startDateTime = Instant.ofEpochMilli(startMs).atZone(ZoneId.systemDefault())
    val startDate = dateFormat.format(startDateTime)
    val startTime = timeFormat.format(startDateTime)
    val endTime = endMs?.let {
        timeFormat.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
    } ?: "—"
    return "$startDate\n$startTime – $endTime"
}

internal fun formatDuration(durationMs: Long): String {
    if (durationMs < 0) return "—"
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    return String.format(Locale.US, "%d:%02d", hours, minutes)
}

internal fun formatCoordinate(lat: Double, lng: Double): String {
    val latDir = if (lat >= 0) "N" else "S"
    val lngDir = if (lng >= 0) "E" else "W"
    return String.format(Locale.US, "%.4f°%s, %.4f°%s", kotlin.math.abs(lat), latDir, kotlin.math.abs(lng), lngDir)
}
