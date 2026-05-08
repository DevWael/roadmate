package com.roadmate.core.model

/**
 * Generic UI state wrapper used across all ViewModels.
 *
 * Provides exhaustive `when` matching for Loading / Success / Error.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : UiState<Nothing>
}
