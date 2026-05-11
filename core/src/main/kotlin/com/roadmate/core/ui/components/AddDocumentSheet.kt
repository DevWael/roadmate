package com.roadmate.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.roadmate.core.database.entity.DocumentType
import com.roadmate.core.ui.theme.RoadMateSpacing

private val MinTouchTarget = 76.dp

private val documentTypeLabels = mapOf(
    DocumentType.INSURANCE to "Insurance",
    DocumentType.LICENSE to "License",
    DocumentType.REGISTRATION to "Registration",
    DocumentType.OTHER to "Other",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentSheet(
    title: String,
    documentType: DocumentType,
    name: String,
    expiryDate: Long,
    reminderDaysBefore: String,
    notes: String,
    errors: Map<String, String>,
    isSaveEnabled: Boolean,
    onTypeChange: (DocumentType) -> Unit,
    onNameChange: (String) -> Unit,
    onExpiryDateChange: (Long) -> Unit,
    onReminderDaysChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
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

            DocumentTypeDropdown(
                selected = documentType,
                onSelect = onTypeChange,
            )

            Spacer(modifier = Modifier.height(RoadMateSpacing.md))

            val nameError = errors["name"]
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Document name") },
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

            DatePickerField(
                label = "Expiry date",
                selectedDateMillis = expiryDate,
                onDateSelected = onExpiryDateChange,
            )

            Spacer(modifier = Modifier.height(RoadMateSpacing.md))

            val reminderError = errors["reminderDays"]
            OutlinedTextField(
                value = reminderDaysBefore,
                onValueChange = onReminderDaysChange,
                label = { Text("Reminder days before") },
                isError = reminderError != null,
                supportingText = reminderError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    cursorColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(RoadMateSpacing.md))

            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Notes (optional)") },
                singleLine = false,
                maxLines = 3,
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
                    .height(MinTouchTarget),
            ) {
                Text("Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentTypeDropdown(
    selected: DocumentType,
    onSelect: (DocumentType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = documentTypeLabels[selected] ?: selected.name,
            onValueChange = {},
            label = { Text("Document type") },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DocumentType.entries.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = documentTypeLabels[type] ?: type.name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    },
                    modifier = Modifier.height(MinTouchTarget),
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
