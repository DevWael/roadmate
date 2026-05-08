package com.roadmate.core.model

import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GpsState sealed interface")
class GpsStateTest {

    @Test
    fun `Acquired is a GpsState`() {
        val state: GpsState = GpsState.Acquired
        assertInstanceOf(GpsState::class.java, state)
    }

    @Test
    fun `Acquiring is a GpsState`() {
        val state: GpsState = GpsState.Acquiring
        assertInstanceOf(GpsState::class.java, state)
    }

    @Test
    fun `Unavailable is a GpsState`() {
        val state: GpsState = GpsState.Unavailable
        assertInstanceOf(GpsState::class.java, state)
    }

    @Test
    fun `when exhaustive over all subtypes compiles`() {
        val state: GpsState = GpsState.Acquired
        val result = when (state) {
            GpsState.Acquired -> "acquired"
            GpsState.Acquiring -> "acquiring"
            GpsState.Unavailable -> "unavailable"
        }
        assertTrue(result == "acquired")
    }
}
