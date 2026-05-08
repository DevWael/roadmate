package com.roadmate.headunit.receiver

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("BootReceiver")
class BootReceiverTest {

    @Test
    @DisplayName("BootReceiver can be instantiated")
    fun canBeInstantiated() {
        val receiver = BootReceiver()
        assertNotNull(receiver)
    }
}
