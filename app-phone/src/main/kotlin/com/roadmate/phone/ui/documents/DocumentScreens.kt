package com.roadmate.phone.ui.documents

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roadmate.core.model.UiState
import com.roadmate.core.ui.components.AddDocumentSheet
import com.roadmate.core.ui.components.DocumentCard
import com.roadmate.core.ui.components.DocumentEmptyState
import com.roadmate.core.ui.components.formatDateForDisplay
import com.roadmate.core.ui.theme.RoadMateSpacing
import com.roadmate.phone.ui.components.RoadMateScaffold

@Composable
fun DocumentListScreen(
    onDocumentClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: DocumentListViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showSheet by viewModel.showAddSheet.collectAsStateWithLifecycle()
    val formType by viewModel.formType.collectAsStateWithLifecycle()
    val formName by viewModel.formName.collectAsStateWithLifecycle()
    val formExpiryDate by viewModel.formExpiryDate.collectAsStateWithLifecycle()
    val formReminderDays by viewModel.formReminderDays.collectAsStateWithLifecycle()
    val formNotes by viewModel.formNotes.collectAsStateWithLifecycle()
    val formErrors by viewModel.formErrors.collectAsStateWithLifecycle()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsStateWithLifecycle()

    if (showSheet) {
        AddDocumentSheet(
            title = "Add Document",
            documentType = formType,
            name = formName,
            expiryDate = formExpiryDate,
            reminderDaysBefore = formReminderDays,
            notes = formNotes,
            errors = formErrors,
            isSaveEnabled = isSaveEnabled,
            onTypeChange = viewModel::onTypeChange,
            onNameChange = viewModel::onNameChange,
            onExpiryDateChange = viewModel::onExpiryDateChange,
            onReminderDaysChange = viewModel::onReminderDaysChange,
            onNotesChange = viewModel::onNotesChange,
            onSave = viewModel::saveDocument,
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
                val documents = state.data.documents
                if (documents.isEmpty()) {
                    DocumentEmptyState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = RoadMateSpacing.lg),
                        contentPadding = PaddingValues(vertical = RoadMateSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.sm),
                    ) {
                        items(documents, key = { it.id }) { document ->
                            DocumentCard(
                                document = document,
                                onClick = { onDocumentClick(document.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentDetailScreen(
    documentId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: DocumentDetailViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showEditSheet by viewModel.showEditSheet.collectAsStateWithLifecycle()
    val formType by viewModel.formType.collectAsStateWithLifecycle()
    val formName by viewModel.formName.collectAsStateWithLifecycle()
    val formExpiryDate by viewModel.formExpiryDate.collectAsStateWithLifecycle()
    val formReminderDays by viewModel.formReminderDays.collectAsStateWithLifecycle()
    val formNotes by viewModel.formNotes.collectAsStateWithLifecycle()
    val formErrors by viewModel.formErrors.collectAsStateWithLifecycle()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    if (showEditSheet) {
        AddDocumentSheet(
            title = "Edit Document",
            documentType = formType,
            name = formName,
            expiryDate = formExpiryDate,
            reminderDaysBefore = formReminderDays,
            notes = formNotes,
            errors = formErrors,
            isSaveEnabled = isSaveEnabled,
            onTypeChange = viewModel::onTypeChange,
            onNameChange = viewModel::onNameChange,
            onExpiryDateChange = viewModel::onExpiryDateChange,
            onReminderDaysChange = viewModel::onReminderDaysChange,
            onNotesChange = viewModel::onNotesChange,
            onSave = viewModel::saveDocument,
            onDismiss = viewModel::onDismissSheet,
        )
    }

    when (val state = uiState) {
        is UiState.Loading -> {
            RoadMateScaffold(title = "Document", onBack = onBack) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        is UiState.Error -> {
            RoadMateScaffold(title = "Document", onBack = onBack) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        is UiState.Success -> {
            val doc = state.data.document
            if (doc != null) {
                DocumentDetailContent(
                    document = doc,
                    onBack = onBack,
                    onEditClick = viewModel::onEditClick,
                    modifier = modifier,
                )
            } else {
                RoadMateScaffold(title = "Document", onBack = onBack) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Document not found",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentDetailContent(
    document: com.roadmate.core.database.entity.Document,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RoadMateScaffold(
        title = document.name,
        onBack = onBack,
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = RoadMateSpacing.lg)
                .padding(vertical = RoadMateSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.lg),
        ) {
            DetailRow(label = "Type", value = document.type.name.lowercase().replaceFirstChar { it.uppercase() })
            DetailRow(label = "Name", value = document.name)
            DetailRow(label = "Expiry", value = formatDateForDisplay(document.expiryDate))
            DetailRow(label = "Reminder", value = "${document.reminderDaysBefore} days before")
            if (document.notes != null) {
                DetailRow(label = "Notes", value = document.notes!!)
            }

            OutlinedButton(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Edit")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(RoadMateSpacing.xs))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
