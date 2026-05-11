package com.roadmate.headunit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.MaintenanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class PreviousScheduleValues(
    val lastServiceKm: Double,
    val lastServiceDate: Long,
)

data class SaveResult(val recordId: String, val previousValues: PreviousScheduleValues)

@HiltViewModel
class MaintenanceCompletionViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<MaintenanceCompletionFormState>>(
        UiState.Success(MaintenanceCompletionFormState())
    )
    val uiState: StateFlow<UiState<MaintenanceCompletionFormState>> = _uiState.asStateFlow()

    private var _previousScheduleValues: PreviousScheduleValues? = null
    val previousScheduleValues: PreviousScheduleValues? get() = _previousScheduleValues

    private var cachedSchedule: MaintenanceSchedule? = null
    private var isSaving = false

    fun initialize(schedule: MaintenanceSchedule, vehicleOdometerKm: Double, currentTimeMillis: Long) {
        _previousScheduleValues = PreviousScheduleValues(
            lastServiceKm = schedule.lastServiceKm,
            lastServiceDate = schedule.lastServiceDate,
        )
        cachedSchedule = schedule
        _uiState.update {
            UiState.Success(
                MaintenanceCompletionFormState(
                    scheduleId = schedule.id,
                    vehicleId = schedule.vehicleId,
                    vehicleOdometerKm = vehicleOdometerKm,
                    datePerformed = currentTimeMillis,
                    odometerKm = vehicleOdometerKm.toLong().toString(),
                ).validate()
            )
        }
    }

    fun updateDate(date: Long) = updateForm { it.copy(datePerformed = date) }
    fun updateOdometerKm(km: String) = updateForm { it.copy(odometerKm = km) }
    fun updateCost(cost: String) = updateForm { it.copy(cost = cost) }
    fun updateLocation(location: String) = updateForm { it.copy(location = location) }
    fun updateNotes(notes: String) = updateForm { it.copy(notes = notes) }

    private fun updateForm(transform: (MaintenanceCompletionFormState) -> MaintenanceCompletionFormState) {
        _uiState.update { current ->
            val state = (current as? UiState.Success)?.data ?: return@update current
            UiState.Success(transform(state).validate())
        }
    }

    fun save(onResult: (SaveResult?) -> Unit) {
        if (isSaving) return
        val form = currentForm.validate()
        if (form.errors.isNotEmpty()) {
            _uiState.update { UiState.Success(form) }
            onResult(null)
            return
        }

        val prev = _previousScheduleValues ?: run { onResult(null); return }
        val schedule = cachedSchedule ?: run { onResult(null); return }
        val record = form.toRecord()
        val updatedSchedule = form.updatedSchedule(schedule)

        isSaving = true
        viewModelScope.launch {
            try {
                val result = maintenanceRepository.completeMaintenance(record, updatedSchedule)
                if (result.isFailure) {
                    Timber.w(result.exceptionOrNull(), "Failed to save maintenance completion")
                    _uiState.update { UiState.Error(result.exceptionOrNull()?.message ?: "Save failed") }
                    onResult(null)
                } else {
                    onResult(SaveResult(record.id, prev))
                }
            } finally {
                isSaving = false
            }
        }
    }

    fun undo(saveResult: SaveResult, onUndoComplete: () -> Unit = {}) {
        val form = currentForm
        val schedule = cachedSchedule ?: return
        val previousSchedule = schedule.copy(
            lastServiceKm = saveResult.previousValues.lastServiceKm,
            lastServiceDate = saveResult.previousValues.lastServiceDate,
        )

        viewModelScope.launch {
            val result = maintenanceRepository.undoCompletion(saveResult.recordId, previousSchedule)
            if (result.isFailure) {
                Timber.w(result.exceptionOrNull(), "Failed to undo maintenance completion")
            }
            onUndoComplete()
        }
    }

    private val currentForm: MaintenanceCompletionFormState
        get() = (_uiState.value as? UiState.Success)?.data ?: MaintenanceCompletionFormState()
}
