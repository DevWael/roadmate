package com.roadmate.core.obd

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock OBD-II provider for V1 — all methods return null.
 *
 * This allows the application to function using GPS-based estimates
 * while the interface is ready for real ELM327 integration in V2.
 */
@Singleton
class MockOBDProvider @Inject constructor() : OBDProvider {

    override fun getOdometer(): Double? = null

    override fun getRpm(): Int? = null

    override fun getFuelRate(): Double? = null

    override fun getCoolantTemp(): Int? = null

    override fun getBatteryVoltage(): Double? = null

    override fun getDtcCodes(): List<String>? = null
}
