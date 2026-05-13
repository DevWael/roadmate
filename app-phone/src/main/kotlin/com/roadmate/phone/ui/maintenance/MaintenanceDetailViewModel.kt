package com.roadmate.phone.ui.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.MaintenanceRepository
import com.roadmate.core.repository.VehicleRepository
import com.roadmate.core.util.MaintenancePredictionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MaintenanceDetailUiState(
    val schedule: MaintenanceSchedule? = null,
    val vehicle: Vehicle? = null,
    val records: List<MaintenanceRecord> = emptyList(),
    val totalSpent: Double = 0.0,
    val predictedNextServiceDate: LocalDate? = null,
    val remainingKm: Double = 0.0,
    val percentage: Float = 0f,
)

@HiltViewModel
class MaintenanceDetailViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<MaintenanceDetailUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<MaintenanceDetailUiState>> = _uiState.asStateFlow()

    private val _showCompletionSheet = MutableStateFlow(false)
    val showCompletionSheet: StateFlow<Boolean> = _showCompletionSheet.asStateFlow()

    private val _completionDate = MutableStateFlow(System.currentTimeMillis())
    val completionDate: StateFlow<Long> = _completionDate.asStateFlow()

    private val _completionOdometerKm = MutableStateFlow("")
    val completionOdometerKm: StateFlow<String> = _completionOdometerKm.asStateFlow()

    private val _completionCost = MutableStateFlow("")
    val completionCost: StateFlow<String> = _completionCost.asStateFlow()

    private val _completionLocation = MutableStateFlow("")
    val completionLocation: StateFlow<String> = _completionLocation.asStateFlow()

    private val _completionNotes = MutableStateFlow("")
    val completionNotes: StateFlow<String> = _completionNotes.asStateFlow()

    private val _completionErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val completionErrors: StateFlow<Map<String, String>> = _completionErrors.asStateFlow()

    private val _isSaving = MutableStateFlow(false)

    val isSaveEnabled: StateFlow<Boolean> = combine(
        _completionOdometerKm,
        _isSaving,
        _completionErrors,
    ) { odo, saving, errors ->
        !saving && odo.isNotBlank() && errors.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var loadJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadSchedule(scheduleId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            maintenanceRepository.getSchedule(scheduleId)
                .flatMapLatest { schedule ->
                    if (schedule == null) {
                        flowOf(UiState.Error("Schedule not found") as UiState<MaintenanceDetailUiState>)
                    } else {
                        combine(
                            vehicleRepository.getVehicle(schedule.vehicleId),
                            maintenanceRepository.getRecordsForSchedule(scheduleId),
                        ) { vehicle, records ->
                            if (vehicle == null) {
                                UiState.Error("Vehicle not found")
                            } else {
                                val remaining = MaintenancePredictionEngine.remainingKm(
                                    currentOdometerKm = vehicle.odometerKm,
                                    lastServiceKm = schedule.lastServiceKm,
                                    intervalKm = schedule.intervalKm,
                                )
                                val intervalKm = schedule.intervalKm
                                val percentage = if (intervalKm != null && intervalKm > 0) {
                                    val driven = vehicle.odometerKm - schedule.lastServiceKm
                                    (driven / intervalKm * 100.0).coerceIn(0.0, 100.0).toFloat()
                                } else {
                                    0f
                                }
                                val sortedRecords = records.sortedByDescending { it.datePerformed }
                                val totalSpent = sortedRecords.mapNotNull { it.cost }.sum()
                                val predictedDate = MaintenancePredictionEngine.predictNextServiceDate(
                                    remainingKm = remaining,
                                    dailyAvgKm = MaintenancePredictionEngine.DEFAULT_DAILY_AVG_KM,
                                    lastServiceDate = schedule.lastServiceDate,
                                    intervalMonths = schedule.intervalMonths,
                                )
                                UiState.Success(
                                    MaintenanceDetailUiState(
                                        schedule = schedule,
                                        vehicle = vehicle,
                                        records = sortedRecords,
                                        totalSpent = totalSpent,
                                        predictedNextServiceDate = predictedDate,
                                        remainingKm = remaining,
                                        percentage = percentage,
                                    )
                                )
                            }
                        }
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun onShowCompletionSheet() {
        val currentState = _uiState.value
        val vehicleOdo = (currentState as? UiState.Success)?.data?.vehicle?.odometerKm ?: 0.0
        _completionDate.value = System.currentTimeMillis()
        _completionOdometerKm.value = vehicleOdo.toInt().toString()
        _completionCost.value = ""
        _completionLocation.value = ""
        _completionNotes.value = ""
        _completionErrors.value = emptyMap()
        _showCompletionSheet.value = true
    }

    fun onDismissCompletionSheet() {
        _showCompletionSheet.value = false
    }

    fun onCompletionDateChange(date: Long) {
        _completionDate.value = date
    }

    fun onCompletionOdometerKmChange(value: String) {
        _completionOdometerKm.value = value
        if (_completionErrors.value.containsKey("odometer")) {
            _completionErrors.value = _completionErrors.value.toMutableMap().apply {
                remove("odometer")
            }
        }
    }

    fun onCompletionCostChange(value: String) {
        _completionCost.value = value
    }

    fun onCompletionLocationChange(value: String) {
        _completionLocation.value = value
    }

    fun onCompletionNotesChange(value: String) {
        _completionNotes.value = value
    }

    fun completeMaintenance() {
        val schedule = (_uiState.value as? UiState.Success)?.data?.schedule ?: return
        val vehicle = (_uiState.value as? UiState.Success)?.data?.vehicle ?: return
        val odoVal = _completionOdometerKm.value.toDoubleOrNull() ?: return
        if (odoVal < schedule.lastServiceKm) {
            _completionErrors.value = _completionErrors.value.toMutableMap().apply {
                this["odometer"] = "Odometer cannot be less than last service (${schedule.lastServiceKm.toInt()} km)"
            }
            return
        }
        val costVal = _completionCost.value.toDoubleOrNull()
        val record = MaintenanceRecord(
            scheduleId = schedule.id,
            vehicleId = schedule.vehicleId,
            datePerformed = _completionDate.value,
            odometerKm = odoVal,
            cost = costVal,
            location = _completionLocation.value.ifBlank { null },
            notes = _completionNotes.value.ifBlank { null },
        )
        val updatedSchedule = schedule.copy(
            lastServiceKm = odoVal,
            lastServiceDate = _completionDate.value,
        )

        viewModelScope.launch {
            _isSaving.value = true
            val result = maintenanceRepository.completeMaintenance(record, updatedSchedule)
            _isSaving.value = false
            if (result.isSuccess) {
                _showCompletionSheet.value = false
            } else {
                val errors = _completionErrors.value.toMutableMap()
                errors["save"] = "Failed to save. Please try again."
                _completionErrors.value = errors
            }
        }
    }

    fun getVehicleOdometerKm(): Double {
        return (_uiState.value as? UiState.Success)?.data?.vehicle?.odometerKm ?: 0.0
    }
}
