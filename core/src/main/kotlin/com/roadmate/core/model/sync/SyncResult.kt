package com.roadmate.core.model.sync

sealed interface SyncResult {
    data object Idle : SyncResult
    data object InProgress : SyncResult
    data class Success(val reason: SyncReason) : SyncResult
    data class Failed(val reason: SyncReason, val message: String) : SyncResult
}
