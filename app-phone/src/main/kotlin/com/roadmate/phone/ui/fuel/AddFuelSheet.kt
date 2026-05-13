package com.roadmate.phone.ui.fuel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.roadmate.core.ui.components.DatePickerField
import com.roadmate.core.ui.theme.RoadMateSpacing
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFuelSheet(
    date: Long,
    odometerKm: String,
    liters: String,
    pricePerLiter: String,
    totalCost: String,
    isFullTank: Boolean,
    station: String,
    errors: Map<String, String>,
    isSaveEnabled: Boolean,
    onDateChange: (Long) -> Unit,
    onOdometerKmChange: (String) -> Unit,
    onLitersChange: (String) -> Unit,
    onPricePerLiterChange: (String) -> Unit,
    onIsFullTankChange: (Boolean) -> Unit,
    onStationChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = RoadMateSpacing.xxl)
                .padding(bottom = RoadMateSpacing.xxl),
        ) {
            Text(
                text = "Add Fuel Entry",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(RoadMateSpacing.lg))

            DatePickerField(
                label = "Date",
                selectedDateMillis = date,
                onDateSelected = onDateChange,
            )

            Spacer(modifier = Modifier.height(RoadMateSpacing.md))

            val odoError = errors["odometerKm"]
            OutlinedTextField(
                value = odometerKm,
                onValueChange = onOdometerKmChange,
                label = { Text("ODO (km)") },
                isError = odoError != null,
                supportingText = odoError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    cursorColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = if (odoError != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (odoError != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(RoadMateSpacing.md))

            Row(modifier = Modifier.fillMaxWidth()) {
                val litersError = errors["liters"]
                OutlinedTextField(
                    value = liters,
                    onValueChange = onLitersChange,
                    label = { Text("Liters") },
                    isError = litersError != null,
                    supportingText = litersError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.padding(horizontal = RoadMateSpacing.sm))

                val priceError = errors["pricePerLiter"]
                OutlinedTextField(
                    value = pricePerLiter,
                    onValueChange = onPricePerLiterChange,
                    label = { Text("Price/L") },
                    isError = priceError != null,
                    supportingText = priceError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(RoadMateSpacing.md))

            OutlinedTextField(
                value = totalCost,
                onValueChange = {},
                label = { Text("Total cost (auto)") },
                enabled = false,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(RoadMateSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Full tank",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = isFullTank,
                    onCheckedChange = onIsFullTankChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(RoadMateSpacing.md))

            OutlinedTextField(
                value = station,
                onValueChange = onStationChange,
                label = { Text("Station (optional)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    cursorColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(RoadMateSpacing.xl))

            Button(
                onClick = onSave,
                enabled = isSaveEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
            ) {
                Text("Save")
            }
        }
    }
}

internal fun calculateTotalCost(liters: String, pricePerLiter: String): String {
    val l = liters.toDoubleOrNull() ?: return ""
    val p = pricePerLiter.toDoubleOrNull() ?: return ""
    return String.format(Locale.US, "%.2f", l * p)
}
