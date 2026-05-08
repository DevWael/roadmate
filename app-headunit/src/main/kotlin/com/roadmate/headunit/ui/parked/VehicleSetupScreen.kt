package com.roadmate.headunit.ui.parked

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.model.UiState
import com.roadmate.core.ui.theme.RoadMateSpacing
import com.roadmate.headunit.viewmodel.TemplateSelection
import com.roadmate.headunit.viewmodel.VehicleFormState
import com.roadmate.headunit.viewmodel.VehicleSetupViewModel

private val MinTouchTarget = 76.dp

@Composable
fun VehicleSetupScreen(viewModel: VehicleSetupViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VehicleSetupContent(
        uiState = uiState,
        onNameChange = viewModel::updateName,
        onMakeChange = viewModel::updateMake,
        onModelChange = viewModel::updateModel,
        onYearChange = viewModel::updateYear,
        onEngineTypeChange = viewModel::updateEngineType,
        onEngineSizeChange = viewModel::updateEngineSize,
        onFuelTypeChange = viewModel::updateFuelType,
        onPlateNumberChange = viewModel::updatePlateNumber,
        onOdometerKmChange = viewModel::updateOdometerKm,
        onOdometerUnitChange = viewModel::updateOdometerUnit,
        onCityConsumptionChange = viewModel::updateCityConsumption,
        onHighwayConsumptionChange = viewModel::updateHighwayConsumption,
        onTemplateSelected = viewModel::selectTemplate,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSetupContent(
    uiState: UiState<VehicleFormState>,
    onNameChange: (String) -> Unit,
    onMakeChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onEngineTypeChange: (EngineType) -> Unit,
    onEngineSizeChange: (String) -> Unit,
    onFuelTypeChange: (FuelType) -> Unit,
    onPlateNumberChange: (String) -> Unit,
    onOdometerKmChange: (String) -> Unit,
    onOdometerUnitChange: (OdometerUnit) -> Unit,
    onCityConsumptionChange: (String) -> Unit,
    onHighwayConsumptionChange: (String) -> Unit,
    onTemplateSelected: (TemplateSelection) -> Unit,
    onSave: () -> Unit,
) {
    when (uiState) {
        is UiState.Loading -> LoadingState()
        is UiState.Success -> {
            val form = uiState.data
            FormContent(
                form = form,
                onNameChange = onNameChange,
                onMakeChange = onMakeChange,
                onModelChange = onModelChange,
                onYearChange = onYearChange,
                onEngineTypeChange = onEngineTypeChange,
                onEngineSizeChange = onEngineSizeChange,
                onFuelTypeChange = onFuelTypeChange,
                onPlateNumberChange = onPlateNumberChange,
                onOdometerKmChange = onOdometerKmChange,
                onOdometerUnitChange = onOdometerUnitChange,
                onCityConsumptionChange = onCityConsumptionChange,
                onHighwayConsumptionChange = onHighwayConsumptionChange,
                onTemplateSelected = onTemplateSelected,
                onSave = onSave,
            )
        }
        is UiState.Error -> ErrorState(message = uiState.message)
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormContent(
    form: VehicleFormState,
    onNameChange: (String) -> Unit,
    onMakeChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onEngineTypeChange: (EngineType) -> Unit,
    onEngineSizeChange: (String) -> Unit,
    onFuelTypeChange: (FuelType) -> Unit,
    onPlateNumberChange: (String) -> Unit,
    onOdometerKmChange: (String) -> Unit,
    onOdometerUnitChange: (OdometerUnit) -> Unit,
    onCityConsumptionChange: (String) -> Unit,
    onHighwayConsumptionChange: (String) -> Unit,
    onTemplateSelected: (TemplateSelection) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(RoadMateSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.lg),
    ) {
        Text(
            text = "Vehicle Setup",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RoadMateSpacing.xl),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
            ) {
                FormTextField(
                    label = "Vehicle Name",
                    value = form.name,
                    onValueChange = onNameChange,
                    error = form.errors["name"],
                    modifier = Modifier.fillMaxWidth(),
                )
                FormTextField(
                    label = "Make",
                    value = form.make,
                    onValueChange = onMakeChange,
                    error = form.errors["make"],
                    modifier = Modifier.fillMaxWidth(),
                )
                FormTextField(
                    label = "Model",
                    value = form.model,
                    onValueChange = onModelChange,
                    error = form.errors["model"],
                    modifier = Modifier.fillMaxWidth(),
                )
                FormTextField(
                    label = "Year",
                    value = form.year,
                    onValueChange = onYearChange,
                    error = form.errors["year"],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownField(
                    label = "Engine Type",
                    options = EngineType.entries,
                    selected = form.engineType,
                    displayText = { it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() } },
                    onSelect = onEngineTypeChange,
                    modifier = Modifier.fillMaxWidth(),
                )
                FormTextField(
                    label = "Engine Size (L)",
                    value = form.engineSize,
                    onValueChange = onEngineSizeChange,
                    error = form.errors["engineSize"],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.md),
            ) {
                DropdownField(
                    label = "Fuel Type",
                    options = FuelType.entries,
                    selected = form.fuelType,
                    displayText = { it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() } },
                    onSelect = onFuelTypeChange,
                    modifier = Modifier.fillMaxWidth(),
                )
                FormTextField(
                    label = "Plate Number",
                    value = form.plateNumber,
                    onValueChange = onPlateNumberChange,
                    error = form.errors["plateNumber"],
                    modifier = Modifier.fillMaxWidth(),
                )
                FormTextField(
                    label = "Odometer",
                    value = form.odometerKm,
                    onValueChange = onOdometerKmChange,
                    error = form.errors["odometerKm"],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OdometerUnitToggle(
                    selected = form.odometerUnit,
                    onSelect = onOdometerUnitChange,
                    modifier = Modifier.fillMaxWidth(),
                )
                FormTextField(
                    label = "City Consumption (L/100km)",
                    value = form.cityConsumption,
                    onValueChange = onCityConsumptionChange,
                    error = form.errors["cityConsumption"],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                FormTextField(
                    label = "Highway Consumption (L/100km)",
                    value = form.highwayConsumption,
                    onValueChange = onHighwayConsumptionChange,
                    error = form.errors["highwayConsumption"],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(RoadMateSpacing.lg))

        TemplateSelectionSection(
            selected = form.templateSelection,
            onSelect = onTemplateSelected,
        )

        Spacer(modifier = Modifier.height(RoadMateSpacing.md))

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(MinTouchTarget),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                text = "Save Vehicle",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        isError = error != null,
        supportingText = error?.let { { Text(text = it, color = MaterialTheme.colorScheme.error) } },
        modifier = modifier.height(MinTouchTarget + 24.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = keyboardOptions,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T,
    displayText: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = displayText(selected),
            onValueChange = {},
            label = { Text(text = label) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .height(MinTouchTarget + 24.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = displayText(option),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    modifier = Modifier.height(MinTouchTarget),
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun OdometerUnitToggle(
    selected: OdometerUnit,
    onSelect: (OdometerUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RoadMateSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Unit:",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(RoadMateSpacing.sm))
        FilterChip(
            selected = selected == OdometerUnit.KM,
            onClick = { onSelect(OdometerUnit.KM) },
            label = { Text(text = "km", style = MaterialTheme.typography.bodyLarge) },
            modifier = Modifier.height(MinTouchTarget),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
        FilterChip(
            selected = selected == OdometerUnit.MILES,
            onClick = { onSelect(OdometerUnit.MILES) },
            label = { Text(text = "miles", style = MaterialTheme.typography.bodyLarge) },
            modifier = Modifier.height(MinTouchTarget),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }
}

@Composable
private fun TemplateSelectionSection(
    selected: TemplateSelection,
    onSelect: (TemplateSelection) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.sm)) {
        Text(
            text = "Maintenance Template",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(RoadMateSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButtonWithLabel(
                label = "Mitsubishi Lancer EX 2015",
                selected = selected == TemplateSelection.MITSUBISHI_LANCER_EX_2015,
                onClick = { onSelect(TemplateSelection.MITSUBISHI_LANCER_EX_2015) },
            )
            RadioButtonWithLabel(
                label = "Custom (no template)",
                selected = selected == TemplateSelection.CUSTOM || selected == TemplateSelection.NONE,
                onClick = { onSelect(TemplateSelection.CUSTOM) },
            )
        }
    }
}

@Composable
private fun RadioButtonWithLabel(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.height(MinTouchTarget),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Spacer(modifier = Modifier.width(RoadMateSpacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
