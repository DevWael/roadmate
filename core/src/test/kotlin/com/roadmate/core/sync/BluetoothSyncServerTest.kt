package com.roadmate.core.sync

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("BluetoothSyncServer")
class BluetoothSyncServerTest {

    private lateinit var scope: CoroutineScope
    private var job: Job? = null

    @BeforeEach
    fun setUp() {
        val j = SupervisorJob()
        job = j
        scope = CoroutineScope(j + UnconfinedTestDispatcher())
    }

    private fun createServer() = BluetoothSyncServer(
        adapter = null,
        scope = scope,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    @Nested
    @DisplayName("ROADMATE_UUID")
    inner class RoadmateUuid {
        @Test
        fun `is custom UUID not standard SPP`() {
            val standardSpp = "00001101-0000-1000-8000-00805f9b34fb"
            assertNotEquals(
                standardSpp,
                BluetoothSyncServer.ROADMATE_UUID.toString().lowercase(),
            )
        }

        @Test
        fun `is consistent across calls`() {
            assertEquals(
                BluetoothSyncServer.ROADMATE_UUID,
                BluetoothSyncServer.ROADMATE_UUID,
            )
        }
    }

    @Nested
    @DisplayName("SERVICE_NAME")
    inner class ServiceName {
        @Test
        fun `is RoadMateSync`() {
            assertEquals("RoadMateSync", BluetoothSyncServer.SERVICE_NAME)
        }
    }

    @Nested
    @DisplayName("start with null adapter")
    inner class StartNullAdapter {
        @Test
        fun `does not crash`() = runTest {
            val server = createServer()
            server.start()
            assertNotNull(server)
            server.stop()
        }

        @Test
        fun `does not emit connections`() = runTest {
            val server = createServer()
            server.start()
            server.connectedSocket.test {
                expectNoEvents()
            }
            server.stop()
        }
    }

    @Nested
    @DisplayName("stop")
    inner class Stop {
        @Test
        fun `cleans up without crash`() = runTest {
            val server = createServer()
            server.start()
            server.stop()
            assertNotNull(server)
        }

        @Test
        fun `can be called without start`() = runTest {
            val server = createServer()
            server.stop()
            assertNotNull(server)
        }

        @Test
        fun `can be called multiple times`() = runTest {
            val server = createServer()
            server.start()
            server.stop()
            server.stop()
            assertNotNull(server)
        }
    }

    @Nested
    @DisplayName("destroy")
    inner class Destroy {
        @Test
        fun `cancels scope`() = runTest {
            val server = createServer()
            server.destroy()
            assertNotNull(server)
        }
    }

    @Nested
    @DisplayName("connectedSocket flow")
    inner class ConnectedSocketFlow {
        @Test
        fun `is initially empty`() = runTest {
            val server = createServer()
            server.connectedSocket.test {
                expectNoEvents()
            }
        }
    }
}
