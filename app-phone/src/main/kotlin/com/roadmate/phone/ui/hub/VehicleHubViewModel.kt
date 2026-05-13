package com.roadmate.phone.ui.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadmate.core.database.entity.FuelLog
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.model.UiState
import com.roadmate.core.model.sync.SyncResult
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.FuelRepository
import com.roadmate.core.repository.MaintenanceRepository
import com.roadmate.core.repository.TripRepository
import com.roadmate.core.repository.VehicleRepository
import com.roadmate.core.sync.SyncTimestampStore
import com.roadmate.core.sync.SyncTriggerManager
import com.roadmate.core.util.AttentionLevel
import com.roadmate.core.util.MaintenancePredictionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate

import javax.inject.Inject

data class AttentionItem(
    val scheduleId: String,
    val name: String,
    val level: AttentionLevel,
    val remainingKm: Double,
)

data class MaintenanceSummaryItem(
    val scheduleId: String,
    val name: String,
    val percentage: Float,
    val remainingKm: Double,
)

data class FuelSummary(
    val totalLiters: Double,
    val totalCost: Double,
    val entryCount: Int,
)

data class VehicleHubUiState(
    val vehicle: Vehicle,
    val lastSyncTimestamp: Long,
    val attentionItems: List<AttentionItem>,
    val maintenanceSummaries: List<MaintenanceSummaryItem>,
    val recentTrips: List<Trip>,
    val fuelSummary: FuelSummary?,
)

@HiltViewModel
class VehicleHubViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val activeVehicleRepository: ActiveVehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val tripRepository: TripRepository,
    private val fuelRepository: FuelRepository,
    private val syncTriggerManager: SyncTriggerManager,
    private val syncTimestampStore: SyncTimestampStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<VehicleHubUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<VehicleHubUiState>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _allVehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val allVehicles: StateFlow<List<Vehicle>> = _allVehicles.asStateFlow()

    val activeVehicleId: StateFlow<String?> = activeVehicleRepository.activeVehicleId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var dataJob: Job? = null
    private var syncJob: Job? = null
    private val _lastSyncTimestamp = MutableStateFlow(0L)

    init {
        loadAllVehicles()
        loadData()
        observeSyncResult()
    }

    private fun loadAllVehicles() {
        viewModelScope.launch {
            vehicleRepository.getAllVehicles().collect { vehicles ->
                _allVehicles.value = vehicles
            }
        }
    }

    fun switchVehicle(vehicleId: String) {
        viewModelScope.launch {
            activeVehicleRepository.setActiveVehicle(vehicleId)
        }
    }

    private fun observeSyncResult() {
        syncJob = viewModelScope.launch {
            syncTriggerManager.syncResult.collect { result ->
                when (result) {
                    is SyncResult.InProgress -> _isRefreshing.value = true
                    is SyncResult.Success -> {
                        _isRefreshing.value = false
                        viewModelScope.launch {
                            _lastSyncTimestamp.value = syncTimestampStore.getLastSyncTimestamp()
                        }
                    }
                    is SyncResult.Failed -> _isRefreshing.value = false
                    is SyncResult.Idle -> Unit
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadData() {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            _lastSyncTimestamp.value = syncTimestampStore.getLastSyncTimestamp()
            activeVehicleRepository.activeVehicleId
                .flatMapLatest { vehicleId ->
                    if (vehicleId == null) {
                        handleMissingActiveVehicle()
                    } else {
                        combine(
                            vehicleRepository.getVehicle(vehicleId),
                            maintenanceRepository.getSchedulesForVehicle(vehicleId),
                            tripRepository.getTripsForVehicle(vehicleId),
                            fuelRepository.getFuelLogsForVehicle(vehicleId),
                            _lastSyncTimestamp,
                        ) { vehicle, schedules, trips, fuelLogs, lastSync ->
                            if (vehicle == null) {
                                null
                            } else {
                                UiState.Success(
                                    buildUiState(vehicle, schedules, trips, fuelLogs, lastSync)
                                ) as UiState<VehicleHubUiState>
                            }
                        }
                    }
                }
                .collect { state ->
                    if (state == null) {
                        handleDeletedActiveVehicle()
                    } else {
                        _uiState.value = state
                    }
                }
        }
    }

    private suspend fun handleDeletedActiveVehicle() {
        val vehicles = vehicleRepository.getAllVehicles().first()
        if (vehicles.isNotEmpty()) {
            activeVehicleRepository.setActiveVehicle(vehicles.first().id)
        } else {
            _uiState.value = UiState.Error("No vehicles available")
        }
    }

    private suspend fun handleMissingActiveVehicle(): Flow<UiState<VehicleHubUiState>> {
        val vehicles = vehicleRepository.getAllVehicles().first()
        return if (vehicles.isNotEmpty()) {
            activeVehicleRepository.setActiveVehicle(vehicles.first().id)
            flowOf(UiState.Loading as UiState<VehicleHubUiState>)
        } else {
            flowOf(UiState.Error("No active vehicle") as UiState<VehicleHubUiState>)
        }
    }

    private fun buildUiState(
        vehicle: Vehicle,
        schedules: List<MaintenanceSchedule>,
        trips: List<Trip>,
        fuelLogs: List<FuelLog>,
        lastSync: Long,
    ): VehicleHubUiState {
        val scheduleRemaining = schedules.associateWith { schedule ->
            MaintenancePredictionEngine.remainingKm(
                currentOdometerKm = vehicle.odometerKm,
                lastServiceKm = schedule.lastServiceKm,
                intervalKm = schedule.intervalKm,
            )
        }

        val attentionItems = schedules.mapNotNull { schedule ->
            val remaining = scheduleRemaining.getValue(schedule)
            val level = MaintenancePredictionEngine.classifyBand(
                remainingKm = remaining,
                intervalKm = schedule.intervalKm,
            )
            if (level != AttentionLevel.NORMAL) {
                AttentionItem(
                    scheduleId = schedule.id,
                    name = schedule.name,
                    level = level,
                    remainingKm = remaining,
                )
            } else null
        }.sortedBy { it.level.ordinal }

        val maintenanceSummaries = schedules.map { schedule ->
            val remaining = scheduleRemaining.getValue(schedule)
            val intervalKm = schedule.intervalKm
            val percentage = if (intervalKm != null && intervalKm > 0) {
                val driven = vehicle.odometerKm - schedule.lastServiceKm
                (driven / intervalKm * 100.0).coerceIn(0.0, 100.0).toFloat()
            } else 0f
            MaintenanceSummaryItem(
                scheduleId = schedule.id,
                name = schedule.name,
                percentage = percentage,
                remainingKm = remaining,
            )
        }.sortedByDescending { it.percentage }.take(3)

        val recentTrips = trips
            .filter { it.status == TripStatus.COMPLETED }
            .sortedByDescending { it.startTime }
            .take(3)

        val startOfMonth = LocalDate.now().withDayOfMonth(1)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val thisMonthFuelLogs = fuelLogs.filter { it.date >= startOfMonth }

        val fuelSummary = if (thisMonthFuelLogs.isNotEmpty()) {
            FuelSummary(
                totalLiters = thisMonthFuelLogs.sumOf { it.liters },
                totalCost = thisMonthFuelLogs.sumOf { it.totalCost },
                entryCount = thisMonthFuelLogs.size,
            )
        } else null

        return VehicleHubUiState(
            vehicle = vehicle,
            lastSyncTimestamp = lastSync,
            attentionItems = attentionItems,
            maintenanceSummaries = maintenanceSummaries,
            recentTrips = recentTrips,
            fuelSummary = fuelSummary,
        )
    }

    fun triggerManualSync() {
        syncTriggerManager.triggerManualSync()
    }
}
