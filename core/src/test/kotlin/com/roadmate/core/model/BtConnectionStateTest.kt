package com.roadmate.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("BtConnectionState sealed interface")
class BtConnectionStateTest {

    @Test
    fun `Connected is a BtConnectionState`() {
        val state: BtConnectionState = BtConnectionState.Connected
        assertInstanceOf(BtConnectionState::class.java, state)
    }

    @Test
    fun `Disconnected is a BtConnectionState`() {
        val state: BtConnectionState = BtConnectionState.Disconnected
        assertInstanceOf(BtConnectionState::class.java, state)
    }

    @Test
    fun `Connecting is a BtConnectionState`() {
        val state: BtConnectionState = BtConnectionState.Connecting
        assertInstanceOf(BtConnectionState::class.java, state)
    }

    @Test
    fun `SyncInProgress is a BtConnectionState`() {
        val state: BtConnectionState = BtConnectionState.SyncInProgress
        assertInstanceOf(BtConnectionState::class.java, state)
    }

    @Test
    fun `SyncFailed is a BtConnectionState`() {
        val state: BtConnectionState = BtConnectionState.SyncFailed()
        assertInstanceOf(BtConnectionState::class.java, state)
    }

    @Test
    fun `SyncFailed reason defaults to empty`() {
        val state = BtConnectionState.SyncFailed()
        assertEquals("", state.reason)
    }

    @Test
    fun `SyncFailed carries reason`() {
        val state = BtConnectionState.SyncFailed(reason = "timeout")
        assertEquals("timeout", state.reason)
    }

    @Test
    fun `when exhaustive over all subtypes compiles`() {
        val state: BtConnectionState = BtConnectionState.Connected
        val result = when (state) {
            BtConnectionState.Connected -> "connected"
            BtConnectionState.Disconnected -> "disconnected"
            BtConnectionState.Connecting -> "connecting"
            BtConnectionState.SyncInProgress -> "syncing"
            is BtConnectionState.SyncFailed -> "failed"
        }
        assertTrue(result == "connected")
    }
}
