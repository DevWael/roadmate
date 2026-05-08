package com.roadmate.core.state

import com.roadmate.core.model.GpsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationStateManager @Inject constructor() {
    private val _gpsState = MutableStateFlow<GpsState>(GpsState.Acquiring)
    val gpsState: StateFlow<GpsState> = _gpsState.asStateFlow()

    fun updateState(state: GpsState) {
        _gpsState.value = state
    }
}
