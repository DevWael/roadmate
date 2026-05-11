package com.roadmate.headunit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.MaintenanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * One-shot UI events emitted by [MaintenanceItemViewModel].
 *
 * Collected via [MaintenanceItemViewModel.events] to trigger transient UI
 * such as snackbars and navigation.
 */
sealed interface MaintenanceItemEvent {
    data object ItemDeleted : MaintenanceItemEvent
    data class Error(val message: String) : MaintenanceItemEvent
}

@HiltViewModel
class MaintenanceItemViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository,
) : ViewModel() {

    private val _formState = MutableStateFlow<UiState<AddMaintenanceFormState>>(
        UiState.Success(AddMaintenanceFormState())
    )
    val formState: StateFlow<UiState<AddMaintenanceFormState>> = _formState.asStateFlow()

    private val _events = Channel<MaintenanceItemEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var editingScheduleId: String? = null
    private var isSaving = false
    private var isDeleting = false

    fun initializeForAdd(vehicleId: String, vehicleOdometerKm: Double) {
        editingScheduleId = null
        _formState.update {
            UiState.Success(
                AddMaintenanceFormState(
                    vehicleId = vehicleId,
                    vehicleOdometerKm = vehicleOdometerKm,
                    lastServiceKm = vehicleOdometerKm.toLong().toString(),
                ).validate()
            )
        }
    }

    fun initializeForEdit(schedule: MaintenanceSchedule, vehicleOdometerKm: Double) {
        editingScheduleId = schedule.id
        _formState.update {
            UiState.Success(
                AddMaintenanceFormState.fromSchedule(schedule, vehicleOdometerKm).validate()
            )
        }
    }

    fun updateName(name: String) = updateForm { it.copy(name = name) }
    fun updateIntervalKm(km: String) = updateForm { it.copy(intervalKm = km) }
    fun updateIntervalMonths(months: String) = updateForm { it.copy(intervalMonths = months) }
    fun updateLastServiceDate(date: Long) = updateForm { it.copy(lastServiceDate = date) }
    fun updateLastServiceKm(km: String) = updateForm { it.copy(lastServiceKm = km) }

    private fun updateForm(transform: (AddMaintenanceFormState) -> AddMaintenanceFormState) {
        _formState.update { current ->
            val state = (current as? UiState.Success)?.data ?: return@update current
            UiState.Success(transform(state).validate())
        }
    }

    fun save(onResult: (Boolean) -> Unit) {
        if (isSaving) return
        val form = currentForm.validate()
        if (!form.isSaveEnabled) {
            _formState.update { UiState.Success(form) }
            onResult(false)
            return
        }

        isSaving = true
        val schedule = form.toSchedule(editingScheduleId)
        viewModelScope.launch {
            try {
                val result = maintenanceRepository.saveSchedule(schedule)
                if (result.isFailure) {
                    Timber.w(result.exceptionOrNull(), "Failed to save maintenance item")
                    _formState.update {
                        UiState.Error(result.exceptionOrNull()?.message ?: "Save failed")
                    }
                    onResult(false)
                } else {
                    onResult(true)
                }
            } finally {
                isSaving = false
            }
        }
    }

    fun deleteSchedule(scheduleId: String, onResult: (Boolean) -> Unit) {
        if (isDeleting) return
        isDeleting = true
        viewModelScope.launch {
            try {
                val result = maintenanceRepository.deleteScheduleWithRecords(scheduleId)
                if (result.isFailure) {
                    Timber.w(result.exceptionOrNull(), "Failed to delete maintenance item")
                    _events.send(
                        MaintenanceItemEvent.Error(
                            result.exceptionOrNull()?.message ?: "Delete failed"
                        )
                    )
                    onResult(false)
                } else {
                    _events.send(MaintenanceItemEvent.ItemDeleted)
                    onResult(true)
                }
            } finally {
                isDeleting = false
            }
        }
    }

    private val currentForm: AddMaintenanceFormState
        get() = (_formState.value as? UiState.Success)?.data ?: AddMaintenanceFormState()
}
