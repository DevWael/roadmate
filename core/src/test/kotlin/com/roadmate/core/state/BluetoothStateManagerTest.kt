package com.roadmate.core.state

import app.cash.turbine.test
import com.roadmate.core.model.BtConnectionState
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BluetoothStateManager")
class BluetoothStateManagerTest {

    private lateinit var manager: BluetoothStateManager

    @BeforeEach
    fun setUp() {
        manager = BluetoothStateManager()
    }

    @Nested
    @DisplayName("initial state")
    inner class InitialState {
        @Test
        fun `is Disconnected`() = runTest {
            manager.btConnectionState.test {
                assertEquals(BtConnectionState.Disconnected, awaitItem())
            }
        }
    }

    @Nested
    @DisplayName("updateState")
    inner class UpdateState {
        @Test
        fun `emits Connected`() = runTest {
            manager.btConnectionState.test {
                assertEquals(BtConnectionState.Disconnected, awaitItem())
                manager.updateState(BtConnectionState.Connected)
                assertEquals(BtConnectionState.Connected, awaitItem())
            }
        }

        @Test
        fun `emits Connecting`() = runTest {
            manager.btConnectionState.test {
                assertEquals(BtConnectionState.Disconnected, awaitItem())
                manager.updateState(BtConnectionState.Connecting)
                assertEquals(BtConnectionState.Connecting, awaitItem())
            }
        }

        @Test
        fun `emits SyncInProgress`() = runTest {
            manager.btConnectionState.test {
                assertEquals(BtConnectionState.Disconnected, awaitItem())
                manager.updateState(BtConnectionState.SyncInProgress)
                assertEquals(BtConnectionState.SyncInProgress, awaitItem())
            }
        }

        @Test
        fun `emits SyncFailed with reason`() = runTest {
            manager.btConnectionState.test {
                assertEquals(BtConnectionState.Disconnected, awaitItem())
                val failed = BtConnectionState.SyncFailed("timeout")
                manager.updateState(failed)
                assertEquals(failed, awaitItem())
            }
        }

        @Test
        fun `full connection lifecycle`() = runTest {
            manager.btConnectionState.test {
                assertEquals(BtConnectionState.Disconnected, awaitItem())
                manager.updateState(BtConnectionState.Connecting)
                assertEquals(BtConnectionState.Connecting, awaitItem())
                manager.updateState(BtConnectionState.Connected)
                assertEquals(BtConnectionState.Connected, awaitItem())
                manager.updateState(BtConnectionState.SyncInProgress)
                assertEquals(BtConnectionState.SyncInProgress, awaitItem())
                manager.updateState(BtConnectionState.Connected)
                assertEquals(BtConnectionState.Connected, awaitItem())
                manager.updateState(BtConnectionState.Disconnected)
                assertEquals(BtConnectionState.Disconnected, awaitItem())
            }
        }
    }
}
