package com.roadmate.phone.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roadmate.core.ui.components.DatePickerField
import com.roadmate.core.ui.theme.RoadMateSpacing
import com.roadmate.phone.ui.components.RoadMateScaffold
import java.io.File

@Composable
fun ExportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ExportViewModel = hiltViewModel()
    val context = LocalContext.current
    val format by viewModel.format.collectAsStateWithLifecycle()
    val scope by viewModel.scope.collectAsStateWithLifecycle()
    val fromDate by viewModel.fromDate.collectAsStateWithLifecycle()
    val toDate by viewModel.toDate.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val isExportEnabled by viewModel.isExportEnabled.collectAsStateWithLifecycle()

    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.resetState() }

    RoadMateScaffold(
        title = "Export Data",
        onBack = onBack,
    ) {
        when (exportState) {
            is ExportState.Loading -> {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(RoadMateSpacing.md))
                        Text(
                            text = "Exporting...",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            is ExportState.Success -> {
                val file = (exportState as ExportState.Success).file
                val mimeType = (exportState as ExportState.Success).mimeType
                val shareIntent = viewModel.createShareIntent(context, file, mimeType)

                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(RoadMateSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(RoadMateSpacing.lg))
                    Text(
                        text = "Export Complete",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(modifier = Modifier.height(RoadMateSpacing.sm))
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(RoadMateSpacing.xxl))
                    Button(
                        onClick = {
                            shareLauncher.launch(Intent.createChooser(shareIntent, "Share export"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Share File")
                    }
                    Spacer(modifier = Modifier.height(RoadMateSpacing.md))
                    OutlinedButton(
                        onClick = { viewModel.resetState() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Export Again")
                    }
                }
            }
            is ExportState.Error -> {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(RoadMateSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = (exportState as ExportState.Error).message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(RoadMateSpacing.lg))
                    OutlinedButton(onClick = { viewModel.resetState() }) {
                        Text("Try Again")
                    }
                }
            }
            else -> {
                ExportFormContent(
                    format = format,
                    scope = scope,
                    fromDate = fromDate,
                    toDate = toDate,
                    isExportEnabled = isExportEnabled,
                    onFormatChange = viewModel::setFormat,
                    onScopeChange = viewModel::setScope,
                    onFromDateChange = viewModel::setFromDate,
                    onToDateChange = viewModel::setToDate,
                    onExport = { viewModel.export(context) },
                    modifier = modifier
                        .fillMaxSize()
                        .padding(RoadMateSpacing.lg),
                )
            }
        }
    }
}

@Composable
private fun ExportFormContent(
    format: ExportFormat,
    scope: ExportScope,
    fromDate: Long?,
    toDate: Long?,
    isExportEnabled: Boolean,
    onFormatChange: (ExportFormat) -> Unit,
    onScopeChange: (ExportScope) -> Unit,
    onFromDateChange: (Long?) -> Unit,
    onToDateChange: (Long?) -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.lg),
    ) {
        Text(
            text = "Format",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
        ) {
            SelectableChip(
                label = "CSV",
                selected = format == ExportFormat.CSV,
                onClick = { onFormatChange(ExportFormat.CSV) },
                modifier = Modifier.weight(1f),
            )
            SelectableChip(
                label = "PDF",
                selected = format == ExportFormat.PDF,
                onClick = { onFormatChange(ExportFormat.PDF) },
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = "Scope",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RoadMateSpacing.sm),
        ) {
            SelectableChip(
                label = "Trips",
                selected = scope == ExportScope.TRIPS,
                onClick = { onScopeChange(ExportScope.TRIPS) },
                modifier = Modifier.weight(1f),
            )
            SelectableChip(
                label = "Fuel",
                selected = scope == ExportScope.FUEL,
                onClick = { onScopeChange(ExportScope.FUEL) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RoadMateSpacing.sm),
        ) {
            SelectableChip(
                label = "Maintenance",
                selected = scope == ExportScope.MAINTENANCE,
                onClick = { onScopeChange(ExportScope.MAINTENANCE) },
                modifier = Modifier.weight(1f),
            )
            SelectableChip(
                label = "All",
                selected = scope == ExportScope.ALL,
                onClick = { onScopeChange(ExportScope.ALL) },
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = "Date Range (optional)",
            style = MaterialTheme.typography.titleMedium,
        )
        if (fromDate != null) {
            DatePickerField(
                label = "From",
                selectedDateMillis = fromDate,
                onDateSelected = { onFromDateChange(it) },
            )
            Text(
                text = "Clear",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onFromDateChange(null) },
            )
        } else {
            OutlinedButton(
                onClick = { onFromDateChange(System.currentTimeMillis()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Set start date")
            }
        }
        Spacer(modifier = Modifier.height(RoadMateSpacing.xs))
        if (toDate != null) {
            DatePickerField(
                label = "To",
                selectedDateMillis = toDate,
                onDateSelected = { onToDateChange(it) },
            )
            Text(
                text = "Clear",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onToDateChange(null) },
            )
        } else {
            OutlinedButton(
                onClick = { onToDateChange(System.currentTimeMillis()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Set end date")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onExport,
            enabled = isExportEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                text = "Export",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = RoadMateSpacing.md, horizontal = RoadMateSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = contentColor,
        )
    }
}
