package com.roadmate.phone.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.roadmate.phone.ui.components.RoadMateScaffold

@Composable
fun VehicleManagementScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RoadMateScaffold(
        title = "Vehicle Management",
        onBack = onBack,
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Vehicle Management",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
