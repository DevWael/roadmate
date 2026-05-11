package com.roadmate.headunit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.TripRepository
import com.roadmate.core.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
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
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    val activeVehicleId: StateFlow<String?> = activeVehicleRepository.activeVehicleId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val vehicles: StateFlow<List<Vehicle>> = vehicleRepository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentVehicle: StateFlow<Vehicle?> = activeVehicleId
        .flatMapLatest { id ->
            if (id != null) vehicleRepository.getVehicle(id) else kotlinx.coroutines.flow.flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trips: StateFlow<List<Trip>> = activeVehicleId
        .flatMapLatest { id ->
            if (id != null) tripRepository.getTripsForVehicle(id) else kotlinx.coroutines.flow.flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
