package com.roadmate.phone.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.roadmate.core.ui.theme.RoadMateSpacing

@Composable
fun VehicleHubScreen(
    onTripListClick: () -> Unit,
    onMaintenanceListClick: () -> Unit,
    onFuelLogClick: () -> Unit,
    onDocumentListClick: () -> Unit,
    onVehicleManagementClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(RoadMateSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Vehicle Hub",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(RoadMateSpacing.xl))
        TextButton(onClick = onTripListClick) { Text("Trips") }
        TextButton(onClick = onMaintenanceListClick) { Text("Maintenance") }
        TextButton(onClick = onFuelLogClick) { Text("Fuel Log") }
        TextButton(onClick = onDocumentListClick) { Text("Documents") }
        TextButton(onClick = onVehicleManagementClick) { Text("Vehicle Management") }
    }
}
