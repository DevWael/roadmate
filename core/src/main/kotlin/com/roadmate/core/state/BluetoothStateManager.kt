package com.roadmate.core.state

import com.roadmate.core.model.BtConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothStateManager @Inject constructor() {
    private val _btConnectionState = MutableStateFlow<BtConnectionState>(BtConnectionState.Disconnected)
    val btConnectionState: StateFlow<BtConnectionState> = _btConnectionState.asStateFlow()

    fun updateState(state: BtConnectionState) {
        _btConnectionState.value = state
    }
}
