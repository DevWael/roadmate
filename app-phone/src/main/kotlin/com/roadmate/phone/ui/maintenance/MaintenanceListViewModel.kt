package com.roadmate.phone.ui.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.MaintenanceRepository
import com.roadmate.core.repository.VehicleRepository
import com.roadmate.core.util.MaintenancePredictionEngine
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MaintenanceItemUi(
    val scheduleId: String,
    val name: String,
    val percentage: Float,
    val remainingKm: Double,
    val predictedNextServiceDate: LocalDate?,
    val intervalKm: Int?,
    val intervalMonths: Int?,
)

data class MaintenanceListUiState(
    val items: List<MaintenanceItemUi>,
    val vehicle: Vehicle,
)

@HiltViewModel
class MaintenanceListViewModel @Inject constructor(
    private val activeVehicleRepository: ActiveVehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<MaintenanceListUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<MaintenanceListUiState>> = _uiState.asStateFlow()

    private val _showAddSheet = MutableStateFlow(false)
    val showAddSheet: StateFlow<Boolean> = _showAddSheet.asStateFlow()

    private val _formName = MutableStateFlow("")
    val formName: StateFlow<String> = _formName.asStateFlow()

    private val _formIntervalKm = MutableStateFlow("")
    val formIntervalKm: StateFlow<String> = _formIntervalKm.asStateFlow()

    private val _formIntervalMonths = MutableStateFlow("")
    val formIntervalMonths: StateFlow<String> = _formIntervalMonths.asStateFlow()

    private val _formLastServiceDate = MutableStateFlow(System.currentTimeMillis())
    val formLastServiceDate: StateFlow<Long> = _formLastServiceDate.asStateFlow()

    private val _formLastServiceKm = MutableStateFlow("")
    val formLastServiceKm: StateFlow<String> = _formLastServiceKm.asStateFlow()

    private val _formErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val formErrors: StateFlow<Map<String, String>> = _formErrors.asStateFlow()

    private val _isSaving = MutableStateFlow(false)

    val isSaveEnabled: StateFlow<Boolean> = combine(
        combine(_formName, _formIntervalKm, _formIntervalMonths) { name, km, months ->
            Triple(name, km, months)
        },
        combine(_formLastServiceKm, _isSaving, _formErrors) { lastKm, saving, errors ->
            Triple(lastKm, saving, errors)
        },
    ) { (name, km, months), (lastKm, saving, errors) ->
        !saving && name.isNotBlank() &&
            (km.isNotBlank() || months.isNotBlank()) &&
            lastKm.isNotBlank() && errors.isEmpty()
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
                        flowOf(UiState.Error("No active vehicle") as UiState<MaintenanceListUiState>)
                    } else {
                        combine(
                            vehicleRepository.getVehicle(vehicleId),
                            maintenanceRepository.getSchedulesForVehicle(vehicleId),
                        ) { vehicle, schedules ->
                            if (vehicle == null) {
                                UiState.Error("Vehicle not found")
                            } else {
                                val items = schedules.map { schedule ->
                                    val remaining = MaintenancePredictionEngine.remainingKm(
                                        currentOdometerKm = vehicle.odometerKm,
                                        lastServiceKm = schedule.lastServiceKm,
                                        intervalKm = schedule.intervalKm,
                                    )
                                    val intervalKm = schedule.intervalKm
                                    val percentage = if (intervalKm != null && intervalKm > 0) {
                                        val driven = vehicle.odometerKm - schedule.lastServiceKm
                                        (driven / intervalKm * 100.0).coerceIn(0.0, 100.0).toFloat()
                                    } else 0f
                                    val predictedDate = MaintenancePredictionEngine.predictNextServiceDate(
                                        remainingKm = remaining,
                                        dailyAvgKm = MaintenancePredictionEngine.DEFAULT_DAILY_AVG_KM,
                                        lastServiceDate = schedule.lastServiceDate,
                                        intervalMonths = schedule.intervalMonths,
                                    )
                                    MaintenanceItemUi(
                                        scheduleId = schedule.id,
                                        name = schedule.name,
                                        percentage = percentage,
                                        remainingKm = remaining,
                                        predictedNextServiceDate = predictedDate,
                                        intervalKm = schedule.intervalKm,
                                        intervalMonths = schedule.intervalMonths,
                                    )
                                }.sortedByDescending { it.percentage }

                                UiState.Success(
                                    MaintenanceListUiState(
                                        items = items,
                                        vehicle = vehicle,
                                    )
                                )
                            }
                        }
                    }
                }
                .collect { state -> _uiState.value = state }
        }
    }

    fun onAddClick() {
        val currentState = _uiState.value
        val vehicleOdo = (currentState as? UiState.Success)?.data?.vehicle?.odometerKm
        _formName.value = ""
        _formIntervalKm.value = ""
        _formIntervalMonths.value = ""
        _formLastServiceDate.value = System.currentTimeMillis()
        _formLastServiceKm.value = vehicleOdo?.toInt()?.toString() ?: ""
        _formErrors.value = emptyMap()
        _showAddSheet.value = true
    }

    fun onDismissSheet() {
        _showAddSheet.value = false
    }

    fun onNameChange(value: String) {
        _formName.value = value
    }

    fun onIntervalKmChange(value: String) {
        _formIntervalKm.value = value
        validateIntervals()
    }

    fun onIntervalMonthsChange(value: String) {
        _formIntervalMonths.value = value
        validateIntervals()
    }

    fun onLastServiceDateChange(date: Long) {
        _formLastServiceDate.value = date
    }

    fun onLastServiceKmChange(value: String) {
        _formLastServiceKm.value = value
    }

    private fun validateIntervals() {
        val errors = _formErrors.value.toMutableMap()
        val km = _formIntervalKm.value.trim()
        val months = _formIntervalMonths.value.trim()
        if (km.isBlank() && months.isBlank()) {
            errors["interval"] = "At least one interval is required"
        } else {
            errors.remove("interval")
        }
        _formErrors.value = errors
    }

    fun saveSchedule() {
        val name = _formName.value.trim()
        val kmVal = _formIntervalKm.value.trim().toIntOrNull()
        val monthsVal = _formIntervalMonths.value.trim().toIntOrNull()
        val lastKm = _formLastServiceKm.value.trim().toDoubleOrNull()

        if (name.isBlank() || lastKm == null) return
        if (_formErrors.value.isNotEmpty()) return

        viewModelScope.launch {
            _isSaving.value = true
            val vehicleId = activeVehicleRepository.activeVehicleId.first() ?: run {
                _isSaving.value = false
                return@launch
            }

            val schedule = MaintenanceSchedule(
                vehicleId = vehicleId,
                name = name,
                intervalKm = kmVal,
                intervalMonths = monthsVal,
                lastServiceKm = lastKm,
                lastServiceDate = _formLastServiceDate.value,
                isCustom = true,
            )

            val result = maintenanceRepository.saveSchedule(schedule)
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
