package com.roadmate.phone.ui.hub

import androidx.compose.animation.animateColorAsState
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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.model.UiState
import com.roadmate.core.ui.components.GaugeArc
import com.roadmate.core.ui.components.GaugeArcVariant
import com.roadmate.core.ui.components.gaugeArcColor
import com.roadmate.core.ui.theme.RoadMateError
import com.roadmate.core.ui.theme.RoadMateSpacing
import com.roadmate.core.ui.theme.RoadMateTertiary
import com.roadmate.core.util.AttentionLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleHubScreen(
    onTripListClick: () -> Unit,
    onTripClick: (String) -> Unit,
    onMaintenanceListClick: () -> Unit,
    onMaintenanceClick: (String) -> Unit,
    onFuelLogClick: () -> Unit,
    onDocumentListClick: () -> Unit,
    onVehicleManagementClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: VehicleHubViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val allVehicles by viewModel.allVehicles.collectAsStateWithLifecycle()
    val activeVehicleId by viewModel.activeVehicleId.collectAsStateWithLifecycle()

    var showSelector by remember { mutableStateOf(false) }

    if (showSelector && allVehicles.isNotEmpty()) {
        VehicleSelectorSheet(
            vehicles = allVehicles,
            activeVehicleId = activeVehicleId ?: "",
            onVehicleSelected = { viewModel.switchVehicle(it) },
            onDismiss = { showSelector = false },
        )
    }

    val vehicleName = when (val state = uiState) {
        is UiState.Success -> state.data.vehicle.name
        else -> ""
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable { showSelector = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = vehicleName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (allVehicles.size > 1) {
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Select vehicle",
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            is UiState.Success -> {
                VehicleHubContent(
                    uiState = state.data,
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.triggerManualSync() },
                    onTripListClick = onTripListClick,
                    onTripClick = onTripClick,
                    onMaintenanceListClick = onMaintenanceListClick,
                    onMaintenanceClick = onMaintenanceClick,
                    onFuelLogClick = onFuelLogClick,
                    modifier = modifier.padding(padding),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleHubContent(
    uiState: VehicleHubUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onTripListClick: () -> Unit,
    onTripClick: (String) -> Unit,
    onMaintenanceListClick: () -> Unit,
    onMaintenanceClick: (String) -> Unit,
    onFuelLogClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deferredIds = remember { mutableStateSetOf<String>() }

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(RoadMateSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.lg),
        ) {
            item {
                VehicleHeroCard(
                    vehicleName = uiState.vehicle.name,
                    odometerKm = uiState.vehicle.odometerKm,
                    odometerUnit = formatOdometerUnit(uiState.vehicle.odometerUnit),
                    lastSyncTimestamp = uiState.lastSyncTimestamp,
                )
            }

            val visibleAttention = uiState.attentionItems
                .filter { it.scheduleId !in deferredIds }
            if (visibleAttention.isNotEmpty()) {
                item {
                    AttentionBand(
                        items = visibleAttention,
                        onTap = onMaintenanceClick,
                        onDefer = { id -> deferredIds.add(id) },
                    )
                }
            }

            if (uiState.maintenanceSummaries.isNotEmpty()) {
                item {
                    MaintenanceSummarySection(
                        summaries = uiState.maintenanceSummaries,
                        onItemClick = onMaintenanceClick,
                        onSeeAll = onMaintenanceListClick,
                    )
                }
            }

            if (uiState.recentTrips.isNotEmpty()) {
                item {
                    RecentTripsSection(
                        trips = uiState.recentTrips,
                        onTripClick = onTripClick,
                        onSeeAll = onTripListClick,
                    )
                }
            }

            if (uiState.fuelSummary != null) {
                item {
                    FuelSummarySection(
                        fuelSummary = uiState.fuelSummary,
                        onSeeAll = onFuelLogClick,
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(RoadMateSpacing.lg)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttentionBand(
    items: List<AttentionItem>,
    onTap: (String) -> Unit,
    onDefer: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.sm)) {
        val displayItems = items.take(2)
        val extraCount = items.size - 2

        displayItems.forEach { item ->
            val dismissState = rememberSwipeToDismissBoxState()

            LaunchedEffect(dismissState.currentValue) {
                if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                    onDefer(item.scheduleId)
                }
            }

            val stripColor = when (item.level) {
                AttentionLevel.OVERDUE -> RoadMateError
                AttentionLevel.CRITICAL -> RoadMateError.copy(alpha = 0.7f)
                AttentionLevel.WARNING -> RoadMateTertiary
                AttentionLevel.NORMAL -> MaterialTheme.colorScheme.outline
            }

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val color by animateColorAsState(
                        when (dismissState.targetValue) {
                            SwipeToDismissBoxValue.EndToStart -> RoadMateError.copy(alpha = 0.3f)
                            else -> Color.Transparent
                        },
                        label = "dismiss-bg",
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color, RoundedCornerShape(4.dp))
                            .padding(end = RoadMateSpacing.lg),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text("Defer", color = RoadMateError)
                    }
                },
                enableDismissFromStartToEnd = false,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(stripColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .clickable { onTap(item.scheduleId) }
                        .padding(horizontal = RoadMateSpacing.lg, vertical = RoadMateSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = null,
                        tint = stripColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(RoadMateSpacing.md))
                    Text(
                        text = attentionLabel(item),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = stripColor,
                    )
                }
            }
        }

        if (extraCount > 0) {
            Text(
                text = "+$extraCount more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = RoadMateSpacing.sm),
            )
        }
    }
}

private fun attentionLabel(item: AttentionItem): String {
    val remaining = String.format("%,.0f", item.remainingKm)
    return when (item.level) {
        AttentionLevel.OVERDUE -> "${item.name} — overdue"
        AttentionLevel.CRITICAL -> "${item.name} — $remaining km remaining"
        AttentionLevel.WARNING -> "${item.name} — $remaining km remaining"
        AttentionLevel.NORMAL -> item.name
    }
}

@Composable
private fun SectionCard(
    title: String,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(RoadMateSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.clickable(onClick = onSeeAll),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        content()
    }
}

@Composable
private fun MaintenanceSummarySection(
    summaries: List<MaintenanceSummaryItem>,
    onItemClick: (String) -> Unit,
    onSeeAll: () -> Unit,
) {
    SectionCard(title = "Maintenance", onSeeAll = onSeeAll) {
        summaries.forEach { summary ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(summary.scheduleId) }
                    .padding(vertical = RoadMateSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GaugeArc(
                    percentage = summary.percentage,
                    variant = GaugeArcVariant.Compact,
                    itemName = summary.name,
                    remainingKm = summary.remainingKm,
                )
                Spacer(modifier = Modifier.width(RoadMateSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.name,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = String.format("%,.0f km remaining", summary.remainingKm),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${summary.percentage.toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = gaugeArcColor(summary.percentage),
                )
            }
        }
    }
}

@Composable
private fun RecentTripsSection(
    trips: List<com.roadmate.core.database.entity.Trip>,
    onTripClick: (String) -> Unit,
    onSeeAll: () -> Unit,
) {
    SectionCard(title = "Recent Trips", onSeeAll = onSeeAll) {
        trips.forEach { trip ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTripClick(trip.id) }
                    .padding(vertical = RoadMateSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Route,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(RoadMateSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatDate(trip.startTime),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = String.format("%.1f km  •  %s", trip.distanceKm, formatDuration(trip.durationMs)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FuelSummarySection(
    fuelSummary: FuelSummary,
    onSeeAll: () -> Unit,
) {
    SectionCard(title = "Fuel Log", onSeeAll = onSeeAll) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RoadMateSpacing.xxl),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Filled.LocalGasStation,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(RoadMateSpacing.xs))
                Text(
                    text = String.format("%.1f L", fuelSummary.totalLiters),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "this month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Spacer(modifier = Modifier.height(RoadMateSpacing.xxl + RoadMateSpacing.xs))
                Text(
                    text = String.format("$%,.0f", fuelSummary.totalCost),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "${fuelSummary.entryCount} fill-up${if (fuelSummary.entryCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatDate(epochMs: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(epochMs))
}

private fun formatDuration(durationMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
