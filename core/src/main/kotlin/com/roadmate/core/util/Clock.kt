package com.roadmate.core.util

/**
 * Provides wall-clock timestamps. Inject for testability.
 */
fun interface Clock {
    fun now(): Long

    companion object {
        /** System clock backed by [System.currentTimeMillis]. */
        val SYSTEM: Clock = Clock { System.currentTimeMillis() }
    }
}
