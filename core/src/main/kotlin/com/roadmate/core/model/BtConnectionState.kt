package com.roadmate.core.model

/**
 * Bluetooth connection lifecycle states for the sync subsystem.
 */
sealed interface BtConnectionState {
    data object Connected : BtConnectionState
    data object Disconnected : BtConnectionState
    data object Connecting : BtConnectionState
    data object SyncInProgress : BtConnectionState
    data class SyncFailed(val reason: String = "") : BtConnectionState
}
