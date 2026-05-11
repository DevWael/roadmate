package com.roadmate.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.roadmate.core.ui.theme.RoadMateSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceSheet(
    title: String,
    name: String,
    intervalKm: String,
    intervalMonths: String,
    lastServiceDate: Long,
    lastServiceKm: String,
    errors: Map<String, String>,
    isSaveEnabled: Boolean,
    onNameChange: (String) -> Unit,
    onIntervalKmChange: (String) -> Unit,
    onIntervalMonthsChange: (String) -> Unit,
    onLastServiceDateChange: (Long) -> Unit,
    onLastServiceKmChange: (String) -> Unit,
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(RoadMateSpacing.lg))

            val nameError = errors["name"]
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Service name") },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    cursorColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = if (nameError != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (nameError != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(RoadMateSpacing.md))

            val intervalError = errors["interval"]
            if (intervalError != null) {
                Text(
                    text = intervalError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(RoadMateSpacing.sm))
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                val kmError = errors["intervalKm"]
                OutlinedTextField(
                    value = intervalKm,
                    onValueChange = onIntervalKmChange,
                    label = { Text("Interval (km)") },
                    isError = kmError != null,
                    supportingText = kmError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.padding(horizontal = RoadMateSpacing.sm))

                val monthError = errors["intervalMonths"]
                OutlinedTextField(
                    value = intervalMonths,
                    onValueChange = onIntervalMonthsChange,
                    label = { Text("Interval (months)") },
                    isError = monthError != null,
                    supportingText = monthError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(RoadMateSpacing.md))

            DatePickerField(
                label = "Last service date",
                selectedDateMillis = lastServiceDate,
                onDateSelected = onLastServiceDateChange,
            )

            Spacer(modifier = Modifier.height(RoadMateSpacing.md))

            val odoError = errors["lastServiceKm"]
            OutlinedTextField(
                value = lastServiceKm,
                onValueChange = onLastServiceKmChange,
                label = { Text("Last service km") },
                isError = odoError != null,
                supportingText = odoError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
