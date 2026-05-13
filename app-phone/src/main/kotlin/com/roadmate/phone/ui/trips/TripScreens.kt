package com.roadmate.phone.ui.trips

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.roadmate.phone.ui.components.RoadMateScaffold

@Composable
fun TripListScreen(
    onTripClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Trip List",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Composable
fun TripDetailScreen(
    tripId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RoadMateScaffold(
        title = "Trip Detail",
        onBack = onBack,
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Trip Detail: $tripId",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
