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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.model.DrivingState
import com.roadmate.core.model.GpsState
import com.roadmate.core.ui.components.AlertStrip
import com.roadmate.core.ui.components.TripLiveIndicator
import com.roadmate.core.ui.theme.RoadMateSecondary
import com.roadmate.core.ui.theme.RoadMateSpacing
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DrivingHUD(
    drivingState: DrivingState.Driving,
    vehicle: Vehicle?,
    gpsState: GpsState,
    alertMessage: String?,
    modifier: Modifier = Modifier,
) {
    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            delay(60_000L - (now % 60_000L))
            currentTime = System.currentTimeMillis()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RoadMateSpacing.xxl),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                TripLiveIndicator(gpsState = gpsState)
            }

            Spacer(modifier = Modifier.height(RoadMateSpacing.xxxl))

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

                Spacer(modifier = Modifier.height(RoadMateSpacing.sm))

                Text(
                    text = "${numberFormatter.format(drivingState.distanceKm)} km",
                    style = MaterialTheme.typography.titleLarge,
                    color = RoadMateSecondary,
                )
            }
        }

        Text(
            text = timeFormat.format(Date(currentTime)),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(RoadMateSpacing.xxl),
        )

        if (alertMessage != null) {
            AlertStrip(
                message = alertMessage,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

internal fun formatOdometer(
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
