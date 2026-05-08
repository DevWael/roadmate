package com.roadmate.core.state

import com.roadmate.core.model.DrivingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrivingStateManager @Inject constructor() {
    private val _drivingState = MutableStateFlow<DrivingState>(DrivingState.Idle)
    val drivingState: StateFlow<DrivingState> = _drivingState.asStateFlow()

    fun updateState(state: DrivingState) {
        _drivingState.value = state
    }
}
