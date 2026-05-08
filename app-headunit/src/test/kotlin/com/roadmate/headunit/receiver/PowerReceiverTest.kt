package com.roadmate.headunit.receiver

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PowerReceiver")
class PowerReceiverTest {

    @Test
    @DisplayName("PowerReceiver can be instantiated")
    fun canBeInstantiated() {
        val receiver = PowerReceiver()
        assertNotNull(receiver)
    }

    @Test
    @DisplayName("onShutdown hook exists for data flush")
    fun onShutdownHookExists() {
        val receiver = PowerReceiver()
        receiver.onShutdown()
    }
}
