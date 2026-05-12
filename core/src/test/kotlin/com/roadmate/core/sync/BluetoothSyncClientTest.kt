package com.roadmate.core.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("BluetoothSyncClient")
class BluetoothSyncClientTest {

    private lateinit var client: BluetoothSyncClient

    @BeforeEach
    fun setUp() {
        client = BluetoothSyncClient(
            adapter = null,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Nested
    @DisplayName("connect with null adapter")
    inner class ConnectNullAdapter {
        @Test
        fun `returns null`() = runTest {
            val result = client.connect()
            assertNull(result)
        }

        @Test
        fun `does not crash`() = runTest {
            client.connect()
            assertNotNull(client)
        }
    }

    @Nested
    @DisplayName("disconnect")
    inner class Disconnect {
        @Test
        fun `can be called without connect`() = runTest {
            client.disconnect()
            assertNotNull(client)
        }

        @Test
        fun `can be called multiple times`() = runTest {
            client.disconnect()
            client.disconnect()
            assertNotNull(client)
        }
    }

    @Nested
    @DisplayName("destroy")
    inner class Destroy {
        @Test
        fun `cleans up without crash`() = runTest {
            client.destroy()
            assertNotNull(client)
        }
    }
}
