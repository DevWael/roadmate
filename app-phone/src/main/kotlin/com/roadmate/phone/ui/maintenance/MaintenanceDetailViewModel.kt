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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import timber.log.Timber
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
}
