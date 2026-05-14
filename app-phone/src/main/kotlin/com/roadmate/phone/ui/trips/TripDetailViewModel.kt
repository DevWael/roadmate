package com.roadmate.phone.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.TripRepository
import com.roadmate.core.util.RouteShareGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TripDetailUiState(
    val trip: Trip,
    val routeSummary: RouteSummary?,
    val tripPoints: List<TripPoint> = emptyList(),
)

data class RouteSummary(
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
)

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    private val tripRepository: TripRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<TripDetailUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<TripDetailUiState>> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadTrip(tripId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                tripRepository.getTrip(tripId),
                tripRepository.getTripPointsForTrip(tripId),
            ) { trip, points ->
                if (trip == null) {
                    UiState.Error("Trip not found") as UiState<TripDetailUiState>
                } else {
                    UiState.Success(
                        TripDetailUiState(
                            trip = trip,
                            routeSummary = buildRouteSummary(points),
                            tripPoints = points,
                        )
                    ) as UiState<TripDetailUiState>
                }
            }.collect { state -> _uiState.value = state }
        }
    }

    private fun buildRouteSummary(points: List<TripPoint>): RouteSummary? {
        if (points.size < 2) return null
        val first = points.first()
        val last = points.last()
        return RouteSummary(
            startLat = first.latitude,
            startLng = first.longitude,
            endLat = last.latitude,
            endLng = last.longitude,
        )
    }

    fun generateShareText(state: TripDetailUiState): String? {
        if (state.tripPoints.size < 2) return null
        return RouteShareGenerator.generateRouteShare(
            points = state.tripPoints,
            startTimeMs = state.trip.startTime,
            distanceKm = state.trip.distanceKm,
            durationMs = state.trip.durationMs,
        )
    }
}
