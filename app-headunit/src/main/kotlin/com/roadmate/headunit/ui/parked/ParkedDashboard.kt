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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.database.entity.Vehicle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import com.roadmate.core.model.BtConnectionState
import com.roadmate.core.ui.components.GaugeArc
import com.roadmate.core.ui.components.GaugeArcVariant
import com.roadmate.core.ui.components.StatusChip
import com.roadmate.core.ui.components.SyncStatusChip
import com.roadmate.core.ui.theme.RoadMateSpacing
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val OuterMargin = 24.dp
private val ColumnGap = 16.dp

internal fun maintenancePercentage(schedule: MaintenanceSchedule, odometerKm: Double): Float {
    val interval = schedule.intervalKm ?: return 0f
    if (interval <= 0) return 0f
    val consumed = odometerKm - schedule.lastServiceKm
    if (consumed <= 0) return 0f
    return ((consumed / interval) * 100.0).coerceAtMost(100.0).toFloat()
}

internal fun remainingKm(schedule: MaintenanceSchedule, odometerKm: Double): Double {
    val interval = schedule.intervalKm ?: return 0.0
    if (interval <= 0) return 0.0
    val nextService = schedule.lastServiceKm + interval
    val remaining = nextService - odometerKm
    return remaining.coerceAtLeast(0.0)
}

internal fun sortSchedulesByUrgency(
    schedules: List<MaintenanceSchedule>,
    odometerKm: Double,
): List<MaintenanceSchedule> {
    return schedules
        .filter { (it.intervalKm ?: 0) > 0 }
        .sortedByDescending { maintenancePercentage(it, odometerKm) }
        .take(3)
}

@Composable
fun ParkedDashboard(
    vehicle: Vehicle?,
    trips: List<Trip>,
    maintenanceSchedules: List<MaintenanceSchedule>,
    btConnectionState: BtConnectionState,
    lastSyncTimestamp: Long,
    modifier: Modifier = Modifier,
) {
    if (vehicle == null) {
        NoVehiclePlaceholder()
        return
    }

    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val recentTrips = remember(trips) { trips.sortedByDescending { it.startTime }.take(5) }
    val urgentMaintenance = remember(maintenanceSchedules, vehicle.odometerKm) {
        sortSchedulesByUrgency(maintenanceSchedules, vehicle.odometerKm)
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(OuterMargin),
        horizontalArrangement = Arrangement.spacedBy(ColumnGap),
    ) {
        LeftPanel(
            vehicle = vehicle,
            btConnectionState = btConnectionState,
            lastSyncTimestamp = lastSyncTimestamp,
            numberFormatter = numberFormatter,
            modifier = Modifier.weight(1f),
        )

        CenterPanel(
            trips = recentTrips,
            numberFormatter = numberFormatter,
            dateFormatter = dateFormatter,
            modifier = Modifier.weight(1f),
        )

        RightPanel(
            schedules = urgentMaintenance,
            odometerKm = vehicle.odometerKm,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LeftPanel(
    vehicle: Vehicle,
    btConnectionState: BtConnectionState,
    lastSyncTimestamp: Long,
    numberFormatter: NumberFormat,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatOdometer(vehicle.odometerKm, vehicle.odometerUnit, numberFormatter),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(RoadMateSpacing.sm))

        Text(
            text = "odometer",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(RoadMateSpacing.lg))

        TrackingStatusChip(btConnectionState = btConnectionState)

        Spacer(modifier = Modifier.height(RoadMateSpacing.sm))

        SyncStatusChipFromBt(
            btConnectionState = btConnectionState,
            lastSyncTimestamp = lastSyncTimestamp,
        )

        Spacer(modifier = Modifier.height(RoadMateSpacing.xl))

        Text(
            text = vehicle.name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TrackingStatusChip(btConnectionState: BtConnectionState) {
    val (label, color) = when (btConnectionState) {
        is BtConnectionState.Connected -> "Tracking: Active" to MaterialTheme.colorScheme.primary
        is BtConnectionState.SyncInProgress -> "Tracking: Active" to MaterialTheme.colorScheme.primary
        is BtConnectionState.Connecting -> "Tracking: Connecting" to MaterialTheme.colorScheme.tertiary
        is BtConnectionState.SyncFailed -> "Tracking: Error" to MaterialTheme.colorScheme.error
        BtConnectionState.Disconnected -> "Tracking: Off" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    StatusChip(
        icon = Icons.Filled.DirectionsCar,
        label = label,
        color = color,
        contentDescription = label,
    )
}

@Composable
private fun SyncStatusChipFromBt(
    btConnectionState: BtConnectionState,
    lastSyncTimestamp: Long,
) {
    val isConnected = btConnectionState is BtConnectionState.Connected ||
        btConnectionState is BtConnectionState.SyncInProgress
    val isConnecting = btConnectionState is BtConnectionState.Connecting
    val isSyncing = btConnectionState is BtConnectionState.SyncInProgress
    val isFailed = btConnectionState is BtConnectionState.SyncFailed
    val failedAtMs = remember(btConnectionState) {
        if (btConnectionState is BtConnectionState.SyncFailed) {
            System.currentTimeMillis()
        } else {
            0L
        }
    }

    SyncStatusChip(
        isConnected = isConnected,
        isConnecting = isConnecting,
        isSyncing = isSyncing,
        isFailed = isFailed,
        failedAtMs = failedAtMs,
        lastSyncTimestamp = lastSyncTimestamp,
        icon = Icons.Filled.Refresh,
    )
}

@Composable
private fun CenterPanel(
    trips: List<Trip>,
    numberFormatter: NumberFormat,
    dateFormatter: SimpleDateFormat,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Recent Trips",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(RoadMateSpacing.md))

        if (trips.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No trips yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
    }
}

@Composable
private fun TripCard(
    trip: Trip,
    numberFormatter: NumberFormat,
    dateFormatter: SimpleDateFormat,
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
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
                TripMetric(label = "Distance", value = "${numberFormatter.format(trip.distanceKm)} km")
                TripMetric(label = "Duration", value = formatDuration(trip.durationMs))
            }
        }
    }
}

@Composable
private fun TripMetric(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun RightPanel(
    schedules: List<MaintenanceSchedule>,
    odometerKm: Double,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Maintenance",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(RoadMateSpacing.md))

        if (schedules.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No maintenance items configured",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.lg),
            ) {
                schedules.forEach { schedule ->
                    val percentage = maintenancePercentage(schedule, odometerKm)
                    val remaining = remainingKm(schedule, odometerKm)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        GaugeArc(
                            percentage = percentage,
                            variant = GaugeArcVariant.Compact,
                            itemName = schedule.name,
                            remainingKm = remaining,
                        )
                        Spacer(modifier = Modifier.height(RoadMateSpacing.xs))
                        Text(
                            text = schedule.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

private fun formatOdometer(
    odometerKm: Double,
    unit: OdometerUnit,
    formatter: NumberFormat,
): String {
    val displayValue = when (unit) {
        OdometerUnit.KM -> odometerKm
        OdometerUnit.MILES -> odometerKm * 0.621371
    }
    val suffix = when (unit) {
        OdometerUnit.KM -> " km"
        OdometerUnit.MILES -> " mi"
    }
    return "${formatter.format(Math.round(displayValue))}$suffix"
}

@Composable
private fun NoVehiclePlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No vehicle set up",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
