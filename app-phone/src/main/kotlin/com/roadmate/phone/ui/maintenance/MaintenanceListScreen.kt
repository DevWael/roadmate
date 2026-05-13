package com.roadmate.phone.ui.maintenance

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.roadmate.core.ui.components.AddMaintenanceSheet
import com.roadmate.core.ui.components.GaugeArc
import com.roadmate.core.ui.components.GaugeArcVariant
import com.roadmate.core.ui.components.gaugeArcColor
import com.roadmate.core.ui.theme.RoadMateSpacing
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MaintenanceListScreen(
    onMaintenanceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MaintenanceListViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showSheet by viewModel.showAddSheet.collectAsStateWithLifecycle()
    val formName by viewModel.formName.collectAsStateWithLifecycle()
    val formIntervalKm by viewModel.formIntervalKm.collectAsStateWithLifecycle()
    val formIntervalMonths by viewModel.formIntervalMonths.collectAsStateWithLifecycle()
    val formLastServiceDate by viewModel.formLastServiceDate.collectAsStateWithLifecycle()
    val formLastServiceKm by viewModel.formLastServiceKm.collectAsStateWithLifecycle()
    val formErrors by viewModel.formErrors.collectAsStateWithLifecycle()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsStateWithLifecycle()

    if (showSheet) {
        AddMaintenanceSheet(
            title = "Add Maintenance",
            name = formName,
            intervalKm = formIntervalKm,
            intervalMonths = formIntervalMonths,
            lastServiceDate = formLastServiceDate,
            lastServiceKm = formLastServiceKm,
            errors = formErrors,
            isSaveEnabled = isSaveEnabled,
            onNameChange = viewModel::onNameChange,
            onIntervalKmChange = viewModel::onIntervalKmChange,
            onIntervalMonthsChange = viewModel::onIntervalMonthsChange,
            onLastServiceDateChange = viewModel::onLastServiceDateChange,
            onLastServiceKmChange = viewModel::onLastServiceKmChange,
            onSave = viewModel::saveSchedule,
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
        modifier = modifier,
    ) { padding ->
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
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
                if (data.items.isEmpty()) {
                    MaintenanceEmptyState(
                        onAddClick = viewModel::onAddClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                } else {
                    MaintenanceListContent(
                        items = data.items,
                        onItemClick = onMaintenanceClick,
                        contentPadding = padding,
                    )
                }
            }
        }
    }
}

@Composable
private fun MaintenanceEmptyState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Build,
            contentDescription = "Maintenance",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(RoadMateSpacing.lg))
        Text(
            text = "No maintenance items.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(RoadMateSpacing.xs))
        Text(
            text = "Add your first maintenance schedule.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MaintenanceListContent(
    items: List<MaintenanceItemUi>,
    onItemClick: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = RoadMateSpacing.lg),
        contentPadding = PaddingValues(vertical = RoadMateSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
    ) {
        items(items, key = { it.scheduleId }) { item ->
            MaintenanceListItem(
                item = item,
                onClick = { onItemClick(item.scheduleId) },
            )
        }
    }
}

@Composable
private fun MaintenanceListItem(
    item: MaintenanceItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RoadMateSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GaugeArc(
                percentage = item.percentage,
                variant = GaugeArcVariant.Compact,
                itemName = item.name,
                remainingKm = item.remainingKm,
            )
            Spacer(modifier = Modifier.width(RoadMateSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (item.predictedNextServiceDate != null) {
                    Spacer(modifier = Modifier.height(RoadMateSpacing.xs))
                    Text(
                        text = "Next: ${formatDate(item.predictedNextServiceDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.width(RoadMateSpacing.md))
            Text(
                text = "${item.percentage.toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = gaugeArcColor(item.percentage),
            )
        }
    }
}

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

private fun formatDate(date: java.time.LocalDate): String {
    return date.format(DATE_FORMATTER)
}
