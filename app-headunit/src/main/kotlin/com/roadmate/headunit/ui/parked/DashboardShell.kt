package com.roadmate.headunit.ui.parked

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.ui.theme.RoadMateSpacing
import java.text.NumberFormat
import java.util.Locale

private val MinTouchTarget = 76.dp

@Composable
fun DashboardShell(
    vehicle: Vehicle?,
    onSwitchVehicle: () -> Unit,
) {
    if (vehicle == null) {
        NoVehiclePlaceholder(onSwitchVehicle = onSwitchVehicle)
        return
    }

    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(RoadMateSpacing.xxl),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = vehicle.name,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(
                onClick = onSwitchVehicle,
                modifier = Modifier.height(MinTouchTarget),
            ) {
                Text(
                    text = "Switch",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
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
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RoadMateSpacing.lg),
        ) {
            StatusChip(
                label = "Status",
                value = "Parked",
                modifier = Modifier.weight(1f),
            )
            StatusChip(
                label = "Fuel",
                value = if (vehicle.cityConsumption > 0) {
                    "${numberFormatter.format(vehicle.cityConsumption)} L/100km"
                } else {
                    "—"
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(RoadMateSpacing.lg)
                .height(MinTouchTarget),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun NoVehiclePlaceholder(onSwitchVehicle: () -> Unit) {
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
        Spacer(modifier = Modifier.height(RoadMateSpacing.lg))
        TextButton(
            onClick = onSwitchVehicle,
            modifier = Modifier.height(MinTouchTarget),
        ) {
            Text(
                text = "Set up a vehicle",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun formatOdometer(
    odometerKm: Double,
    unit: com.roadmate.core.database.entity.OdometerUnit,
    formatter: NumberFormat,
): String {
    val displayValue = when (unit) {
        com.roadmate.core.database.entity.OdometerUnit.KM -> odometerKm
        com.roadmate.core.database.entity.OdometerUnit.MILES -> odometerKm * 0.621371
    }
    val suffix = when (unit) {
        com.roadmate.core.database.entity.OdometerUnit.KM -> " km"
        com.roadmate.core.database.entity.OdometerUnit.MILES -> " mi"
    }
    return "${formatter.format(Math.round(displayValue))}$suffix"
}
