package com.roadmate.phone.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roadmate.core.model.UiState
import com.roadmate.core.ui.theme.RoadMateSpacing
import com.roadmate.core.util.StatisticsCalculator
import com.roadmate.phone.ui.components.RoadMateScaffold
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: StatisticsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RoadMateScaffold(
        title = "Statistics",
        onBack = onBack,
    ) {
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            is UiState.Success -> {
                StatisticsContent(
                    uiState = state.data,
                    onPeriodChange = viewModel::setPeriod,
                    modifier = modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun StatisticsContent(
    uiState: StatisticsUiState,
    onPeriodChange: (StatisticsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        PeriodSelectorTabs(
            selectedPeriod = uiState.period,
            onPeriodChange = onPeriodChange,
        )

        val stats = uiState.statistics
        val isEmpty = stats.totalTrips == 0 &&
            stats.totalFuelCost == 0.0 &&
            stats.totalMaintenanceCost == 0.0

        if (isEmpty) {
            EmptyStatisticsState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(RoadMateSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
            ) {
                item {
                    StatisticsGrid(statistics = stats)
                }

                if (uiState.weekComparison != null) {
                    item {
                        WeekComparisonCard(comparison = uiState.weekComparison)
                    }
                }

                if (uiState.yearBreakdown != null && uiState.yearRunningTotal != null) {
                    item {
                        YearRunningTotalCard(total = uiState.yearRunningTotal)
                    }

                    items(
                        items = uiState.yearBreakdown,
                        key = { it.month },
                    ) { month ->
                        MonthBreakdownRow(breakdown = month)
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelectorTabs(
    selectedPeriod: StatisticsPeriod,
    onPeriodChange: (StatisticsPeriod) -> Unit,
) {
    val tabs = StatisticsPeriod.entries
    val selectedIndex = tabs.indexOf(selectedPeriod)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = RoadMateSpacing.lg,
        divider = {},
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        tabs.forEach { period ->
            Tab(
                selected = period == selectedPeriod,
                onClick = { onPeriodChange(period) },
                text = {
                    Text(
                        text = period.label(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}

@Composable
private fun StatisticsGrid(
    statistics: StatisticsCalculator.DrivingStatistics,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(RoadMateSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
    ) {
        Text(
            text = "Overview",
            style = MaterialTheme.typography.titleMedium,
        )

        StatRow(label = "Total Distance", value = formatDistance(statistics.totalDistanceKm))
        StatRow(label = "Total Trips", value = statistics.totalTrips.toString())
        StatRow(label = "Avg Trip Distance", value = formatDistance(statistics.avgTripDistanceKm))
        StatRow(label = "Driving Time", value = formatDuration(statistics.totalDrivingTimeMs))
        StatRow(label = "Fuel Cost", value = formatCost(statistics.totalFuelCost))
        StatRow(label = "Maintenance Cost", value = formatCost(statistics.totalMaintenanceCost))
        StatRow(label = "Cost / km", value = formatCostPerKm(statistics.costPerKm))
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun WeekComparisonCard(
    comparison: StatisticsCalculator.WeekComparison,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(RoadMateSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
    ) {
        Text(
            text = "vs Previous Week",
            style = MaterialTheme.typography.titleMedium,
        )

        ComparisonRow(
            label = "Distance",
            currentValue = formatDistance(comparison.currentDistanceKm),
            changePercent = comparison.distanceChangePercent,
        )
        ComparisonRow(
            label = "Fuel Cost",
            currentValue = formatCost(comparison.currentFuelCost),
            changePercent = comparison.fuelCostChangePercent,
            invertColor = true,
        )
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    currentValue: String,
    changePercent: Double?,
    invertColor: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = currentValue,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatChangePercent(changePercent),
            style = MaterialTheme.typography.labelLarge,
            color = changeColor(changePercent, invertColor),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun YearRunningTotalCard(
    total: StatisticsCalculator.DrivingStatistics,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(RoadMateSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
    ) {
        Text(
            text = "Year Total",
            style = MaterialTheme.typography.titleMedium,
        )
        StatRow(label = "Distance", value = formatDistance(total.totalDistanceKm))
        StatRow(label = "Fuel Cost", value = formatCost(total.totalFuelCost))
        StatRow(label = "Maintenance Cost", value = formatCost(total.totalMaintenanceCost))
    }
}

@Composable
private fun MonthBreakdownRow(
    breakdown: StatisticsCalculator.MonthBreakdown,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(RoadMateSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.sm),
    ) {
        Text(
            text = breakdown.monthName,
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDistance(breakdown.distanceKm),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatCost(breakdown.fuelCost),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatCost(breakdown.maintenanceCost),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyStatisticsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(RoadMateSpacing.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Assessment,
                contentDescription = null,
                modifier = Modifier.padding(bottom = RoadMateSpacing.md),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "No data for this period",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun StatisticsPeriod.label(): String = when (this) {
    StatisticsPeriod.DAY -> "Day"
    StatisticsPeriod.WEEK -> "Week"
    StatisticsPeriod.MONTH -> "Month"
    StatisticsPeriod.YEAR -> "Year"
}

private fun formatDistance(km: Double): String {
    return String.format(Locale.US, "%,.1f km", km)
}

private fun formatCost(cost: Double): String {
    return String.format(Locale.US, "$%,.0f", cost)
}

private fun formatCostPerKm(cost: Double): String {
    if (cost == 0.0) return "—"
    return String.format(Locale.US, "$%.2f", cost)
}

private fun formatDuration(durationMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

private fun formatChangePercent(percent: Double?): String {
    if (percent == null) return "—"
    val prefix = if (percent >= 0) "↑" else "↓"
    return "$prefix${String.format("%.0f", kotlin.math.abs(percent))}%"
}

@Composable
private fun changeColor(percent: Double?, invertColor: Boolean = false) = when {
    percent == null -> MaterialTheme.colorScheme.onSurfaceVariant
    invertColor && percent >= 0 -> MaterialTheme.colorScheme.error
    invertColor && percent < 0 -> MaterialTheme.colorScheme.primary
    percent >= 0 -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.error
}
