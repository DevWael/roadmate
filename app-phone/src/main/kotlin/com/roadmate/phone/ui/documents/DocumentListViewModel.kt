package com.roadmate.phone.ui.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadmate.core.database.entity.Document
import com.roadmate.core.database.entity.DocumentType
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DocumentListUiState(
    val documents: List<Document>,
)

@HiltViewModel
class DocumentListViewModel @Inject constructor(
    private val activeVehicleRepository: ActiveVehicleRepository,
    private val documentRepository: DocumentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<DocumentListUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<DocumentListUiState>> = _uiState.asStateFlow()

    private val _showAddSheet = MutableStateFlow(false)
    val showAddSheet: StateFlow<Boolean> = _showAddSheet.asStateFlow()

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

    init {
        loadData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadData() {
        viewModelScope.launch {
            activeVehicleRepository.activeVehicleId
                .flatMapLatest { vehicleId ->
                    if (vehicleId == null) {
                        flowOf(UiState.Error("No active vehicle") as UiState<DocumentListUiState>)
                    } else {
                        documentRepository.getDocumentsForVehicle(vehicleId)
                            .map { documents ->
                                val sorted = documents.sortedBy { it.expiryDate }
                                UiState.Success(
                                    DocumentListUiState(documents = sorted)
                                ) as UiState<DocumentListUiState>
                            }
                    }
                }
                .collect { state -> _uiState.value = state }
        }
    }

    fun onAddClick() {
        _formType.value = DocumentType.OTHER
        _formName.value = ""
        _formExpiryDate.value = System.currentTimeMillis()
        _formReminderDays.value = "30"
        _formNotes.value = ""
        _formErrors.value = emptyMap()
        _showAddSheet.value = true
    }

    fun onDismissSheet() {
        _showAddSheet.value = false
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
        val name = _formName.value.trim()
        val reminderDays = _formReminderDays.value.trim().toIntOrNull() ?: return

        if (name.isBlank() || _formErrors.value.isNotEmpty()) return

        viewModelScope.launch {
            _isSaving.value = true
            val vehicleId = activeVehicleRepository.activeVehicleId.first() ?: run {
                _isSaving.value = false
                return@launch
            }

            val document = Document(
                vehicleId = vehicleId,
                type = _formType.value,
                name = name,
                expiryDate = _formExpiryDate.value,
                reminderDaysBefore = reminderDays,
                notes = _formNotes.value.ifBlank { null },
            )

            val result = documentRepository.saveDocument(document)
            _isSaving.value = false
            if (result.isSuccess) {
                _showAddSheet.value = false
            } else {
                val errors = _formErrors.value.toMutableMap()
                errors["save"] = "Failed to save. Please try again."
                _formErrors.value = errors
            }
        }
    }
}
