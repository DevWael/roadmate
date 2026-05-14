package com.roadmate.headunit.ui.adaptive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.model.BtConnectionState
import com.roadmate.core.model.DrivingState
import com.roadmate.core.model.GpsState
import com.roadmate.core.ui.components.AlertStrip
import com.roadmate.core.ui.components.AlertStripCompact
import com.roadmate.core.ui.components.AlertStripDot
import com.roadmate.core.ui.components.AlertStripVariant
import com.roadmate.core.ui.components.GaugeArc
import com.roadmate.core.ui.components.GaugeArcVariant
import com.roadmate.core.ui.components.alertStripVariantForWidth
import com.roadmate.core.ui.theme.RoadMateSpacing
import com.roadmate.headunit.ui.driving.CompactDrivingHUD
import com.roadmate.headunit.ui.driving.DrivingHUD
import com.roadmate.headunit.ui.parked.ParkedDashboard
import com.roadmate.headunit.ui.parked.maintenancePercentage
import com.roadmate.headunit.ui.parked.remainingKm
import com.roadmate.headunit.ui.parked.sortSchedulesByUrgency
import java.text.NumberFormat
import java.util.Locale
import com.roadmate.headunit.ui.components.CockpitPanel
import com.roadmate.headunit.ui.components.PanelDivider

@Composable
fun AdaptiveDashboard(
    drivingState: DrivingState,
    gpsState: GpsState,
    vehicle: Vehicle?,
    trips: List<Trip>,
    maintenanceSchedules: List<MaintenanceSchedule>,
    btConnectionState: BtConnectionState,
    lastSyncTimestamp: Long,
    alertMessage: String?,
    onSwitchVehicle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthDp = maxWidth.value
        val breakpoint = DashboardBreakpoint.fromWidthDp(widthDp)

        if (drivingState is DrivingState.Driving) {
            AdaptiveDrivingLayout(
                drivingState = drivingState,
                vehicle = vehicle,
                gpsState = gpsState,
                alertMessage = alertMessage,
                breakpoint = breakpoint,
                widthDp = widthDp,
            )
        } else {
            AdaptiveParkedLayout(
                vehicle = vehicle,
                trips = trips,
                maintenanceSchedules = maintenanceSchedules,
                btConnectionState = btConnectionState,
                lastSyncTimestamp = lastSyncTimestamp,
                breakpoint = breakpoint,
                onSwitchVehicle = onSwitchVehicle,
                drivingState = drivingState,
            )
        }
    }
}

@Composable
private fun AdaptiveDrivingLayout(
    drivingState: DrivingState.Driving,
    vehicle: Vehicle?,
    gpsState: GpsState,
    alertMessage: String?,
    breakpoint: DashboardBreakpoint,
    widthDp: Float,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (breakpoint) {
            DashboardBreakpoint.Full -> {
                DrivingHUD(
                    drivingState = drivingState,
                    vehicle = vehicle,
                    gpsState = gpsState,
                    alertMessage = alertMessage,
                )
            }
            DashboardBreakpoint.Compact, DashboardBreakpoint.Narrow -> {
                CompactDrivingHUD(
                    drivingState = drivingState,
                    vehicle = vehicle,
                    gpsState = gpsState,
                    breakpoint = breakpoint,
                )
            }
        }

        if (alertMessage != null) {
            when (alertStripVariantForWidth(widthDp)) {
                AlertStripVariant.Full -> {
                    AlertStrip(
                        message = alertMessage,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
                AlertStripVariant.Compact -> {
                    AlertStripCompact(
                        message = alertMessage,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
                AlertStripVariant.Dot -> {
                    AlertStripDot(
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .padding(RoadMateSpacing.md),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdaptiveParkedLayout(
    vehicle: Vehicle?,
    trips: List<Trip>,
    maintenanceSchedules: List<MaintenanceSchedule>,
    btConnectionState: BtConnectionState,
    lastSyncTimestamp: Long,
    breakpoint: DashboardBreakpoint,
    onSwitchVehicle: () -> Unit,
    drivingState: DrivingState,
) {
    when (breakpoint) {
        DashboardBreakpoint.Full -> {
            ParkedDashboard(
                vehicle = vehicle,
                trips = trips,
                maintenanceSchedules = maintenanceSchedules,
                btConnectionState = btConnectionState,
                lastSyncTimestamp = lastSyncTimestamp,
                onSwitchVehicle = onSwitchVehicle,
                drivingState = drivingState,
            )
        }
        DashboardBreakpoint.Compact -> {
            ParkedDashboard(
                vehicle = vehicle,
                trips = emptyList(),
                maintenanceSchedules = maintenanceSchedules,
                btConnectionState = btConnectionState,
                lastSyncTimestamp = lastSyncTimestamp,
                onSwitchVehicle = onSwitchVehicle,
                drivingState = drivingState,
            )
        }
        DashboardBreakpoint.Narrow -> {
            NarrowParkedLayout(
                vehicle = vehicle,
                maintenanceSchedules = maintenanceSchedules,
            )
        }
    }
}


@Composable
private fun NarrowParkedLayout(
    vehicle: Vehicle?,
    maintenanceSchedules: List<MaintenanceSchedule>,
    modifier: Modifier = Modifier,
) {
    if (vehicle == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No vehicle set up",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val urgentMaintenance = remember(maintenanceSchedules, vehicle.odometerKm) {
        sortSchedulesByUrgency(maintenanceSchedules, vehicle.odometerKm)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(RoadMateSpacing.lg),
    ) {
        CockpitPanel(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = com.roadmate.headunit.ui.driving.formatOdometer(
                                vehicle.odometerKm,
                                vehicle.odometerUnit,
                                numberFormatter,
                            ),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Spacer(modifier = Modifier.height(RoadMateSpacing.xs))

                        Text(
                            text = "odometer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(RoadMateSpacing.sm))

                        Text(
                            text = vehicle.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                if (urgentMaintenance.isNotEmpty()) {
                    item {
                        PanelDivider()
                    }

                    item {
                        Text(
                            text = "Maintenance",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    items(urgentMaintenance, key = { it.id }) { schedule ->
                        val percentage = maintenancePercentage(schedule, vehicle.odometerKm)
                        val remaining = remainingKm(schedule, vehicle.odometerKm)
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
}

