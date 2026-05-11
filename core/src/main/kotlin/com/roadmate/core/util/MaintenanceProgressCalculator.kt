package com.roadmate.core.util

object MaintenanceProgressCalculator {

    fun calculate(
        currentOdometerKm: Double,
        lastServiceKm: Double,
        intervalKm: Int?,
        lastServiceDate: Long,
        intervalMonths: Int?,
        currentTimeMillis: Long,
    ): Float {
        val kmProgress = if (intervalKm != null && intervalKm > 0) {
            ((currentOdometerKm - lastServiceKm) / intervalKm * 100).toFloat().coerceIn(0f, 100f)
        } else {
            0f
        }

        val monthProgress = if (intervalMonths != null && intervalMonths > 0) {
            val elapsedMillis = currentTimeMillis - lastServiceDate
            val elapsedMonths = elapsedMillis.toDouble() / (30.0 * 24 * 60 * 60 * 1000)
            (elapsedMonths.toFloat() / intervalMonths * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }

        return maxOf(kmProgress, monthProgress)
    }

    fun remainingKm(
        currentOdometerKm: Double,
        lastServiceKm: Double,
        intervalKm: Int?,
    ): Double {
        if (intervalKm == null) return 0.0
        val driven = currentOdometerKm - lastServiceKm
        val remaining = intervalKm - driven
        return remaining.coerceAtLeast(0.0)
    }
}
