package com.roadmate.phone.ui.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadmate.core.database.entity.Document
import com.roadmate.core.database.entity.DocumentType
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DocumentDetailUiState(
    val document: Document? = null,
)

@HiltViewModel
class DocumentDetailViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<DocumentDetailUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<DocumentDetailUiState>> = _uiState.asStateFlow()

    private val _showEditSheet = MutableStateFlow(false)
    val showEditSheet: StateFlow<Boolean> = _showEditSheet.asStateFlow()

    private val _formType = MutableStateFlow(DocumentType.OTHER)
    val formType: StateFlow<DocumentType> = _formType.asStateFlow()

    private val _formName = MutableStateFlow("")
    val formName: StateFlow<String> = _formName.asStateFlow()

    private val _formExpiryDate = MutableStateFlow(System.currentTimeMillis())
    val formExpiryDate: StateFlow<Long> = _formExpiryDate.asStateFlow()

    private val _formReminderDays = MutableStateFlow("30")
    val formReminderDays: StateFlow<String> = _formReminderDays.asStateFlow()

    private val _formNotes = MutableStateFlow("")
    val formNotes: StateFlow<String> = _formNotes.asStateFlow()

    private val _formErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val formErrors: StateFlow<Map<String, String>> = _formErrors.asStateFlow()

    private val _isSaving = MutableStateFlow(false)

    val isSaveEnabled: StateFlow<Boolean> = combine(
        _formName,
        _formReminderDays,
        _isSaving,
        _formErrors,
    ) { name, reminder, saving, errors ->
        !saving && name.isNotBlank() && reminder.isNotBlank() && errors.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var loadJob: Job? = null

    fun loadDocument(documentId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            documentRepository.getDocument(documentId)
                .map { document ->
                    if (document == null) {
                        UiState.Error("Document not found") as UiState<DocumentDetailUiState>
                    } else {
                        UiState.Success(
                            DocumentDetailUiState(document = document)
                        ) as UiState<DocumentDetailUiState>
                    }
                }
                .collect { state -> _uiState.value = state }
        }
    }

    fun onEditClick() {
        val doc = (_uiState.value as? UiState.Success)?.data?.document ?: return
        _formType.value = doc.type
        _formName.value = doc.name
        _formExpiryDate.value = doc.expiryDate
        _formReminderDays.value = doc.reminderDaysBefore.toString()
        _formNotes.value = doc.notes ?: ""
        _formErrors.value = emptyMap()
        _showEditSheet.value = true
    }

    fun onDismissSheet() {
        _showEditSheet.value = false
    }

    fun onTypeChange(type: DocumentType) {
        _formType.value = type
    }

    fun onNameChange(value: String) {
        _formName.value = value
    }

    fun onExpiryDateChange(date: Long) {
        _formExpiryDate.value = date
    }

    fun onReminderDaysChange(value: String) {
        _formReminderDays.value = value
    }

    fun onNotesChange(value: String) {
        _formNotes.value = value
    }

    fun saveDocument() {
        val doc = (_uiState.value as? UiState.Success)?.data?.document ?: return
        val name = _formName.value.trim()
        val reminderDays = _formReminderDays.value.trim().toIntOrNull() ?: return

        if (name.isBlank() || _formErrors.value.isNotEmpty()) return

        viewModelScope.launch {
            _isSaving.value = true
            val updated = doc.copy(
                type = _formType.value,
                name = name,
                expiryDate = _formExpiryDate.value,
                reminderDaysBefore = reminderDays,
                notes = _formNotes.value.ifBlank { null },
            )
            val result = documentRepository.saveDocument(updated)
            _isSaving.value = false
            if (result.isSuccess) {
                _showEditSheet.value = false
            } else {
                val errors = _formErrors.value.toMutableMap()
                errors["save"] = "Failed to save. Please try again."
                _formErrors.value = errors
            }
        }
    }
}
