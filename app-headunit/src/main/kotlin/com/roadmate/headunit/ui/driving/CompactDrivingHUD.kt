package com.roadmate.headunit.ui.driving

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.model.DrivingState
import com.roadmate.core.model.GpsState
import com.roadmate.core.ui.components.TripLiveIndicator
import com.roadmate.core.ui.theme.RoadMateSecondary
import com.roadmate.core.ui.theme.RoadMateSpacing
import com.roadmate.headunit.ui.adaptive.DashboardBreakpoint
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CompactDrivingHUD(
    drivingState: DrivingState.Driving,
    vehicle: Vehicle?,
    gpsState: GpsState,
    breakpoint: DashboardBreakpoint,
    modifier: Modifier = Modifier,
) {
    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RoadMateSpacing.lg),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                TripLiveIndicator(gpsState = gpsState)
            }

            Spacer(modifier = Modifier.height(RoadMateSpacing.xl))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                if (vehicle != null) {
                    Text(
                        text = formatOdometer(vehicle.odometerKm, vehicle.odometerUnit, numberFormatter),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (breakpoint == DashboardBreakpoint.Compact) {
                    Spacer(modifier = Modifier.height(RoadMateSpacing.sm))

                    Text(
                        text = "${numberFormatter.format(drivingState.distanceKm)} km",
                        style = MaterialTheme.typography.titleLarge,
                        color = RoadMateSecondary,
                    )
                }
            }
        }
    }
}
