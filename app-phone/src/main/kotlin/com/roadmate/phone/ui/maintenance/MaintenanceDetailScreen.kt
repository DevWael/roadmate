package com.roadmate.phone.ui.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.model.UiState
import com.roadmate.core.ui.components.GaugeArc
import com.roadmate.core.ui.components.GaugeArcVariant
import com.roadmate.core.ui.components.MaintenanceCompletionSheet
import com.roadmate.core.ui.components.formatDateForDisplay
import com.roadmate.core.ui.theme.RoadMateSpacing
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceDetailScreen(
    uiState: UiState<MaintenanceDetailUiState>,
    onBack: () -> Unit,
    completionSheetState: MaintenanceCompletionSheetState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (uiState is UiState.Success) uiState.data.schedule?.name ?: "Maintenance" else "Maintenance",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = modifier,
    ) { padding ->
        when (uiState) {
            is UiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            is UiState.Success -> {
                val data = uiState.data
                val schedule = data.schedule ?: return@Scaffold

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = RoadMateSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
                ) {
                    item {
                        GaugeArcSection(data)
                    }

                    item {
                        IntervalInfoSection(schedule.intervalKm, schedule.intervalMonths)
                    }

                    if (data.predictedNextServiceDate != null) {
                        item {
                            PredictionSection(data.predictedNextServiceDate)
                        }
                    }

                    if (data.records.isNotEmpty()) {
                        item {
                            TotalSpentSection(data.totalSpent)
                        }
                    }

                    if (data.records.isEmpty()) {
                        item {
                            EmptyHistorySection()
                        }
                    } else {
                        items(data.records, key = { it.id }) { record ->
                            RecordCard(record)
                        }
                    }

                    item {
                        Button(
                            onClick = completionSheetState.onShowSheet,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp),
                        ) {
                            Text(
                                text = "Mark as Done",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Spacer(modifier = Modifier.height(RoadMateSpacing.lg))
                    }
                }

                if (completionSheetState.isVisible) {
                    MaintenanceCompletionSheet(
                        scheduleName = schedule.name,
                        datePerformed = completionSheetState.datePerformed,
                        odometerKm = completionSheetState.odometerKm,
                        vehicleOdometerKm = completionSheetState.vehicleOdometerKm,
                        cost = completionSheetState.cost,
                        location = completionSheetState.location,
                        notes = completionSheetState.notes,
                        errors = completionSheetState.errors,
                        isSaveEnabled = completionSheetState.isSaveEnabled,
                        onDateChange = completionSheetState.onDateChange,
                        onOdometerKmChange = completionSheetState.onOdometerKmChange,
                        onCostChange = completionSheetState.onCostChange,
                        onLocationChange = completionSheetState.onLocationChange,
                        onNotesChange = completionSheetState.onNotesChange,
                        onSave = completionSheetState.onSave,
                        onDismiss = completionSheetState.onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun GaugeArcSection(data: MaintenanceDetailUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GaugeArc(
            percentage = data.percentage,
            variant = GaugeArcVariant.Large,
            itemName = data.schedule?.name ?: "",
            remainingKm = data.remainingKm,
        )
        if (data.remainingKm > 0 && data.schedule?.intervalKm != null) {
            Spacer(modifier = Modifier.height(RoadMateSpacing.sm))
            Text(
                text = "${data.remainingKm.toLong()} km remaining",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (data.remainingKm <= 0 && data.schedule?.intervalKm != null) {
            Spacer(modifier = Modifier.height(RoadMateSpacing.sm))
            Text(
                text = "Overdue",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun IntervalInfoSection(intervalKm: Int?, intervalMonths: Int?) {
    if (intervalKm == null && intervalMonths == null) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (intervalKm != null) {
            Text(
                text = "Every $intervalKm km",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (intervalKm != null && intervalMonths != null) {
            Spacer(modifier = Modifier.width(RoadMateSpacing.md))
            Text(
                text = "/",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(RoadMateSpacing.md))
        }
        if (intervalMonths != null) {
            Text(
                text = "Every $intervalMonths months",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PredictionSection(predictedDate: java.time.LocalDate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Icon(
                imageVector = Icons.Filled.CalendarToday,
                contentDescription = "Predicted next service",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(RoadMateSpacing.md))
            Column {
                Text(
                    text = "Predicted next service",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatLocalDate(predictedDate),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun TotalSpentSection(totalSpent: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Icon(
                imageVector = Icons.Filled.AttachMoney,
                contentDescription = "Total spent",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(RoadMateSpacing.md))
            Text(
                text = "Total spent: ",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatCurrency(totalSpent),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EmptyHistorySection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = RoadMateSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Build,
            contentDescription = "No records",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(RoadMateSpacing.md))
        Text(
            text = "No service records yet.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Mark as done after your next service.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecordCard(record: MaintenanceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RoadMateSpacing.lg),
        ) {
            Text(
                text = formatDateForDisplay(record.datePerformed),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(RoadMateSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
            ) {
                InfoChip(
                    icon = Icons.Filled.Speed,
                    label = "${record.odometerKm.toLong()} km",
                )
                if (record.cost != null) {
                    InfoChip(
                        icon = Icons.Filled.AttachMoney,
                        label = formatCurrency(record.cost!!),
                    )
                }
            }

            if (record.location != null) {
                Spacer(modifier = Modifier.height(RoadMateSpacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(RoadMateSpacing.xs))
                    Text(
                        text = record.location!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (record.notes != null) {
                Spacer(modifier = Modifier.height(RoadMateSpacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Note,
                        contentDescription = "Notes",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(RoadMateSpacing.xs))
                    Text(
                        text = record.notes!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(RoadMateSpacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return format.format(amount)
}

private fun formatLocalDate(date: java.time.LocalDate): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    return date.format(formatter)
}

data class MaintenanceCompletionSheetState(
    val isVisible: Boolean = false,
    val datePerformed: Long = System.currentTimeMillis(),
    val odometerKm: String = "",
    val vehicleOdometerKm: Double = 0.0,
    val cost: String = "",
    val location: String = "",
    val notes: String = "",
    val errors: Map<String, String> = emptyMap(),
    val isSaveEnabled: Boolean = false,
    val onShowSheet: () -> Unit = {},
    val onDateChange: (Long) -> Unit = {},
    val onOdometerKmChange: (String) -> Unit = {},
    val onCostChange: (String) -> Unit = {},
    val onLocationChange: (String) -> Unit = {},
    val onNotesChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDismiss: () -> Unit = {},
)
