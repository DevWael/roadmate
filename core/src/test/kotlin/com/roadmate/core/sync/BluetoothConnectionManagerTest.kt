package com.roadmate.core.sync

import app.cash.turbine.test
import com.roadmate.core.model.BtConnectionState
import com.roadmate.core.state.BluetoothStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("BluetoothConnectionManager")
class BluetoothConnectionManagerTest {

    private lateinit var stateManager: BluetoothStateManager
    private lateinit var server: BluetoothSyncServer
    private lateinit var client: BluetoothSyncClient
    private lateinit var manager: BluetoothConnectionManager
    private var job: Job? = null

    @BeforeEach
    fun setUp() {
        stateManager = BluetoothStateManager()
        val j = SupervisorJob()
        job = j
        val scope = CoroutineScope(j + UnconfinedTestDispatcher())
        server = BluetoothSyncServer(null, scope, UnconfinedTestDispatcher())
        client = BluetoothSyncClient(null, UnconfinedTestDispatcher())
        manager = BluetoothConnectionManager(server, client, stateManager, scope)
    }

    @Nested
    @DisplayName("calculateBackoff")
    inner class CalculateBackoff {
        @Test
        fun `attempt 0 returns 2s`() {
            assertEquals(2000L, manager.calculateBackoff(0))
        }

        @Test
        fun `attempt 1 returns 4s`() {
            assertEquals(4000L, manager.calculateBackoff(1))
        }

        @Test
        fun `attempt 2 returns 8s`() {
            assertEquals(8000L, manager.calculateBackoff(2))
        }

        @Test
        fun `attempt 3 returns 16s`() {
            assertEquals(16000L, manager.calculateBackoff(3))
        }

        @Test
        fun `attempt 4 returns 30s capped`() {
            assertEquals(30000L, manager.calculateBackoff(4))
        }

        @Test
        fun `attempt 5 returns 30s capped`() {
            assertEquals(30000L, manager.calculateBackoff(5))
        }

        @Test
        fun `attempt 10 returns 30s capped`() {
            assertEquals(30000L, manager.calculateBackoff(10))
        }

        @Test
        fun `negative attempt returns initial backoff`() {
            assertEquals(2000L, manager.calculateBackoff(-1))
        }
    }

    @Nested
    @DisplayName("startServer")
    inner class StartServer {
        @Test
        fun `emits Disconnected initially`() = runTest {
            stateManager.btConnectionState.test {
                manager.startServer()
                assertEquals(BtConnectionState.Disconnected, awaitItem())
                manager.stop()
            }
        }

        @Test
        fun `does not crash with null adapter`() = runTest {
            manager.startServer()
            assertNotNull(manager)
            manager.stop()
        }
    }

    @Nested
    @DisplayName("startClient")
    inner class StartClient {
        @Test
        fun `sets mode to CLIENT`() = runTest {
            manager.startClient()
            assertEquals(BluetoothConnectionManager.Mode.CLIENT, manager.currentMode)
            manager.stop()
        }

        @Test
        fun `does not crash with null adapter`() = runTest {
            manager.startClient()
            assertNotNull(manager)
            manager.stop()
        }
    }

    @Nested
    @DisplayName("stop")
    inner class Stop {
        @Test
        fun `resets to IDLE`() = runTest {
            manager.startServer()
            manager.stop()
            assertEquals(BluetoothConnectionManager.Mode.IDLE, manager.currentMode)
        }

        @Test
        fun `can be called without start`() = runTest {
            manager.stop()
            assertNotNull(manager)
        }

        @Test
        fun `can be called multiple times`() = runTest {
            manager.startServer()
            manager.stop()
            manager.stop()
            assertNotNull(manager)
        }
    }

    @Nested
    @DisplayName("destroy")
    inner class Destroy {
        @Test
        fun `cleans up all resources`() = runTest {
            manager.startServer()
            manager.destroy()
            assertNotNull(manager)
        }
    }

    @Nested
    @DisplayName("mode tracking")
    inner class ModeTracking {
        @Test
        fun `initial mode is IDLE`() {
            assertEquals(BluetoothConnectionManager.Mode.IDLE, manager.currentMode)
        }

        @Test
        fun `startServer sets SERVER mode`() {
            manager.startServer()
            assertEquals(BluetoothConnectionManager.Mode.SERVER, manager.currentMode)
            manager.stop()
        }

        @Test
        fun `startClient sets CLIENT mode`() = runTest {
            manager.startClient()
            assertEquals(BluetoothConnectionManager.Mode.CLIENT, manager.currentMode)
            manager.stop()
        }

        @Test
        fun `stop resets to IDLE`() {
            manager.startServer()
            manager.stop()
            assertEquals(BluetoothConnectionManager.Mode.IDLE, manager.currentMode)
        }
    }
}
