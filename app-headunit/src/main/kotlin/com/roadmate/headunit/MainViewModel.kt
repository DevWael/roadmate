package com.roadmate.headunit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.model.DrivingState
import com.roadmate.core.model.GpsState
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.MaintenanceRepository
import com.roadmate.core.repository.TripRepository
import com.roadmate.core.repository.VehicleRepository
import com.roadmate.core.state.DrivingStateManager
import com.roadmate.core.state.LocationStateManager
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val activeVehicleRepository: ActiveVehicleRepository,
    private val vehicleRepository: VehicleRepository,
    private val tripRepository: TripRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val drivingStateManager: DrivingStateManager,
    private val locationStateManager: LocationStateManager,
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    val drivingState: StateFlow<DrivingState> = drivingStateManager.drivingState
    val gpsState: StateFlow<GpsState> = locationStateManager.gpsState

    val activeVehicleId: StateFlow<String?> = activeVehicleRepository.activeVehicleId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val vehicles: StateFlow<List<Vehicle>> = vehicleRepository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentVehicle: StateFlow<Vehicle?> = activeVehicleId
        .flatMapLatest { id ->
            if (id != null) vehicleRepository.getVehicle(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trips: StateFlow<List<Trip>> = activeVehicleId
        .flatMapLatest { id ->
            if (id != null) tripRepository.getTripsForVehicle(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val maintenanceAlertMessage: StateFlow<String?> = activeVehicleId
        .flatMapLatest { id ->
            if (id != null) {
                combine(
                    maintenanceRepository.getSchedulesForVehicle(id),
                    currentVehicle,
                ) { schedules: List<MaintenanceSchedule>, vehicle: Vehicle? ->
                    if (vehicle == null) return@combine null
                    val overdue = schedules.filter { schedule ->
                        val interval = schedule.intervalKm ?: return@filter false
                        if (interval <= 0) return@filter false
                        val progress = ((vehicle.odometerKm - schedule.lastServiceKm) / interval)
                            .coerceAtLeast(0.0)
                        progress >= 0.95
                    }
                    if (overdue.isEmpty()) null
                    else overdue.joinToString(", ") { it.name }
                }
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            vehicleRepository.getAllVehicles().first()
            _isReady.value = true
        }
    }

    fun switchVehicle(vehicleId: String) {
        viewModelScope.launch {
            activeVehicleRepository.setActiveVehicle(vehicleId)
        }
    }
}
