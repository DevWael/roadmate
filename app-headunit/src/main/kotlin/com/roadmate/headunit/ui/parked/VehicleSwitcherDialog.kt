package com.roadmate.headunit.ui.parked

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.ui.theme.RoadMateSpacing
import com.roadmate.core.ui.theme.RoadMatePrimary
import java.text.NumberFormat
import java.util.Locale

private val MinTouchTarget = 76.dp

@Composable
fun VehicleSwitcherDialog(
    vehicles: List<Vehicle>,
    activeVehicleId: String?,
    onVehicleSelected: (String) -> Unit,
    onAddVehicle: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(RoadMateSpacing.xxl),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

        Column(
            modifier = Modifier.padding(RoadMateSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
        ) {
            Text(
                text = "Switch Vehicle",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.xs),
            ) {
                items(vehicles, key = { it.id }) { vehicle ->
                    var isItemFocused by remember { mutableStateOf(false) }
                    VehicleItem(
                        vehicle = vehicle,
                        isActive = vehicle.id == activeVehicleId,
                        isFocused = isItemFocused,
                        numberFormatter = numberFormatter,
                        onSelect = {
                            onVehicleSelected(vehicle.id)
                            onDismiss()
                        },
                        onFocusChange = { isItemFocused = it },
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            TextButton(
                onClick = onAddVehicle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MinTouchTarget),
            ) {
                Text(
                    text = "+ Add New Vehicle",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MinTouchTarget),
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VehicleItem(
    vehicle: Vehicle,
    isActive: Boolean,
    isFocused: Boolean,
    numberFormatter: NumberFormat,
    onSelect: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }



    Card(
        modifier = Modifier
            .fillMaxWidth()
            .focusable()
            .onFocusChanged { onFocusChange(it.isFocused) }
            .then(
                if (isFocused) {
                    Modifier.border(
                        BorderStroke(1.dp, RoadMatePrimary),
                        shape = CardDefaults.shape,
                    )
                } else Modifier
            )
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MinTouchTarget)
                .padding(horizontal = RoadMateSpacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = vehicle.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatOdometerDisplay(vehicle.odometerKm, vehicle.odometerUnit, numberFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isActive) {
                Text(
                    text = "Active",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

internal fun formatOdometerDisplay(odometerKm: Double, unit: OdometerUnit, formatter: NumberFormat): String {
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
