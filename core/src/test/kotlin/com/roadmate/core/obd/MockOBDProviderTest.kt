package com.roadmate.core.obd

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [MockOBDProvider].
 * Validates that all methods return null as specified for V1 GPS-based fallback.
 */
class MockOBDProviderTest {

    private lateinit var provider: MockOBDProvider

    @BeforeEach
    fun setup() {
        provider = MockOBDProvider()
    }

    @Test
    fun `getOdometer returns null`() {
        assertNull(provider.getOdometer())
    }

    @Test
    fun `getRpm returns null`() {
        assertNull(provider.getRpm())
    }

    @Test
    fun `getFuelRate returns null`() {
        assertNull(provider.getFuelRate())
    }

    @Test
    fun `getCoolantTemp returns null`() {
        assertNull(provider.getCoolantTemp())
    }

    @Test
    fun `getBatteryVoltage returns null`() {
        assertNull(provider.getBatteryVoltage())
    }

    @Test
    fun `getDtcCodes returns null`() {
        assertNull(provider.getDtcCodes())
    }

    @Test
    fun `provider implements OBDProvider interface`() {
        val obd: OBDProvider = provider
        assertNull(obd.getOdometer())
    }
}
