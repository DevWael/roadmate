package com.roadmate.core.obd

/**
 * Interface for accessing OBD-II vehicle diagnostics data.
 *
 * V1 uses [MockOBDProvider] which returns null for all methods,
 * allowing GPS-based fallback for trip tracking.
 * V2 will implement this with a real ELM327 Bluetooth adapter.
 *
 * All methods return nullable types — callers must handle null
 * gracefully (e.g., fall back to GPS-based estimates).
 */
interface OBDProvider {

    /** Current odometer reading in km, or null if unavailable. */
    fun getOdometer(): Double?

    /** Current engine RPM, or null if unavailable. */
    fun getRpm(): Int?

    /** Current fuel consumption rate in L/h, or null if unavailable. */
    fun getFuelRate(): Double?

    /** Current coolant temperature in °C, or null if unavailable. */
    fun getCoolantTemp(): Int?

    /** Current battery voltage in V, or null if unavailable. */
    fun getBatteryVoltage(): Double?

    /** Active DTC (Diagnostic Trouble Code) list, or null if unavailable. */
    fun getDtcCodes(): List<String>?
}
