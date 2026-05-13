package com.roadmate.phone.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripListViewModel @Inject constructor(
    private val activeVehicleRepository: ActiveVehicleRepository,
    private val tripRepository: TripRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Trip>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Trip>>> = _uiState.asStateFlow()

    init {
        loadTrips()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadTrips() {
        viewModelScope.launch {
            activeVehicleRepository.activeVehicleId
                .flatMapLatest { vehicleId ->
                    if (vehicleId == null) {
                        flowOf(UiState.Error("No active vehicle") as UiState<List<Trip>>)
                    } else {
                        tripRepository.getTripsForVehicle(vehicleId)
                            .map { trips -> UiState.Success(trips) as UiState<List<Trip>> }
                    }
                }
                .collect { state -> _uiState.value = state }
        }
    }
}
