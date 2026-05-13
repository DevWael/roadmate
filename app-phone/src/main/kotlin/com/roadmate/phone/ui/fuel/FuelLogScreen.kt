package com.roadmate.phone.ui.fuel

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roadmate.core.model.UiState
import com.roadmate.core.ui.theme.RoadMateSpacing
import com.roadmate.core.ui.theme.RoadMateTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FuelLogScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel: FuelLogViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showSheet by viewModel.showAddSheet.collectAsStateWithLifecycle()
    val formDate by viewModel.formDate.collectAsStateWithLifecycle()
    val formOdo by viewModel.formOdometerKm.collectAsStateWithLifecycle()
    val formLiters by viewModel.formLiters.collectAsStateWithLifecycle()
    val formPrice by viewModel.formPricePerLiter.collectAsStateWithLifecycle()
    val totalCost by viewModel.totalCost.collectAsStateWithLifecycle()
    val formFullTank by viewModel.formIsFullTank.collectAsStateWithLifecycle()
    val formStation by viewModel.formStation.collectAsStateWithLifecycle()
    val formErrors by viewModel.formErrors.collectAsStateWithLifecycle()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsStateWithLifecycle()

    if (showSheet) {
        AddFuelSheet(
            date = formDate,
            odometerKm = formOdo,
            liters = formLiters,
            pricePerLiter = formPrice,
            totalCost = totalCost,
            isFullTank = formFullTank,
            station = formStation,
            errors = formErrors,
            isSaveEnabled = isSaveEnabled,
            onDateChange = viewModel::onDateChange,
            onOdometerKmChange = viewModel::onOdometerKmChange,
            onLitersChange = viewModel::onLitersChange,
            onPricePerLiterChange = viewModel::onPricePerLiterChange,
            onIsFullTankChange = viewModel::onIsFullTankChange,
            onStationChange = viewModel::onStationChange,
            onSave = viewModel::saveFuelEntry,
            onDismiss = viewModel::onDismissSheet,
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        },
    ) { padding ->
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            is UiState.Success -> {
                val data = state.data
                if (data.entries.isEmpty()) {
                    FuelEmptyState(
                        onAddClick = viewModel::onAddClick,
                        modifier = modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                } else {
                    FuelLogContent(
                        uiState = data,
                        estimatedLPer100km = data.estimatedLPer100km,
                        modifier = modifier.padding(padding),
                    )
                }
            }
        }
    }
}

@Composable
private fun FuelEmptyState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.LocalGasStation,
            contentDescription = "Fuel",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(RoadMateSpacing.lg))
        Text(
            text = "No fuel entries yet.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(RoadMateSpacing.xs))
        Text(
            text = "Log your first fill-up.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(RoadMateSpacing.xl))
        Button(
            onClick = onAddClick,
            modifier = Modifier.height(76.dp),
        ) {
            Text(
                text = "Add",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun FuelLogContent(
    uiState: FuelLogUiState,
    estimatedLPer100km: Double,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(RoadMateSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
    ) {
        item {
            FuelSummaryCard(summary = uiState.summary)
        }

        items(
            items = uiState.entries,
            key = { it.fuelLog.id },
        ) { entry ->
            FuelEntryCard(entry = entry, estimatedLPer100km = estimatedLPer100km)
        }
    }
}

@Composable
private fun FuelSummaryCard(
    summary: FuelSummary,
    modifier: Modifier = Modifier,
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
        Text(
            text = "This Month",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SummaryRow(
            label = "Total fuel cost",
            value = String.format(Locale.US, "%.2f", summary.totalCostThisMonth),
        )
        SummaryRow(
            label = "Avg L/100km",
            value = summary.avgLPer100km?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
        )
        SummaryRow(
            label = "Avg cost/km",
            value = summary.avgCostPerKm?.let { String.format(Locale.US, "%.2f", it) } ?: "—",
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
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
private fun FuelEntryCard(
    entry: FuelLogEntryUi,
    estimatedLPer100km: Double,
    modifier: Modifier = Modifier,
) {
    val log = entry.fuelLog
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(RoadMateSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDateFuel(log.date),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = String.format(Locale.US, "%.1f L", log.liters),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = String.format(Locale.US, "%.2f cost", log.totalCost),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.consumptionLPer100km != null) {
                val consumptionColor = if (entry.isOverConsumption) RoadMateTertiary
                else MaterialTheme.colorScheme.onSurfaceVariant
                Text(
                    text = String.format(
                        Locale.US,
                        "Actual: %.1f vs Estimated: %.1f L/100km",
                        entry.consumptionLPer100km,
                        estimatedLPer100km,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = consumptionColor,
                )
            }
        }
        val stationName = log.station
        if (!stationName.isNullOrBlank()) {
            Text(
                text = stationName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDateFuel(timestampMs: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestampMs))
}
