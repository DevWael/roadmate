package com.roadmate.core.util

import com.roadmate.core.database.entity.Trip
import java.time.LocalDate

enum class AttentionLevel {
    NORMAL,
    WARNING,
    CRITICAL,
    OVERDUE,
}

object MaintenancePredictionEngine {

    const val DEFAULT_DAILY_AVG_KM = 50.0
    const val DEFAULT_ATTENTION_THRESHOLD_KM = 500.0

    fun dailyAverage(
        trips: List<Trip>,
        fallbackIfFewerDays: Int = 7,
        fallbackKmPerDay: Double = DEFAULT_DAILY_AVG_KM,
    ): Double {
        if (trips.isEmpty()) return fallbackKmPerDay
        val earliestMs = trips.minOf { it.startTime }
        val latestMs = trips.maxOf { it.startTime }
        val daySpan = ((latestMs - earliestMs) / (24.0 * 60 * 60 * 1000)).coerceAtLeast(1.0)
        if (daySpan < fallbackIfFewerDays) return fallbackKmPerDay
        val totalDistance = trips.sumOf { it.distanceKm }
        return totalDistance / daySpan
    }

    fun predictNextServiceDate(
        remainingKm: Double,
        dailyAvgKm: Double,
        lastServiceDate: Long,
        intervalMonths: Int?,
        today: LocalDate = LocalDate.now(),
    ): LocalDate {
        val daysUntilKmDue = if (dailyAvgKm > 0) {
            (remainingKm / dailyAvgKm).toLong().coerceAtLeast(0)
        } else {
            Long.MAX_VALUE
        }

        val daysUntilMonthDue = intervalMonths?.let { months ->
            val lastServiceLocalDate = java.time.Instant.ofEpochMilli(lastServiceDate)
                .atZone(java.time.ZoneOffset.UTC)
                .toLocalDate()
            val dueDate = lastServiceLocalDate.plusMonths(months.toLong())
            java.time.temporal.ChronoUnit.DAYS.between(today, dueDate).coerceAtLeast(0)
        } ?: Long.MAX_VALUE

        val daysUntil = minOf(daysUntilKmDue, daysUntilMonthDue)
        return today.plusDays(daysUntil)
    }

    fun remainingKm(
        currentOdometerKm: Double,
        lastServiceKm: Double,
        intervalKm: Int?,
    ): Double {
        if (intervalKm == null) return 0.0
        val driven = currentOdometerKm - lastServiceKm
        return (intervalKm - driven).coerceAtLeast(0.0)
    }

    fun overdueKm(
        currentOdometerKm: Double,
        lastServiceKm: Double,
        intervalKm: Int?,
    ): Double {
        if (intervalKm == null) return 0.0
        val driven = currentOdometerKm - lastServiceKm
        return (driven - intervalKm).coerceAtLeast(0.0)
    }

    fun classifyBand(
        remainingKm: Double,
        attentionThresholdKm: Double = DEFAULT_ATTENTION_THRESHOLD_KM,
        intervalKm: Int?,
    ): AttentionLevel {
        if (intervalKm == null) return AttentionLevel.NORMAL
        if (remainingKm <= 0.0) return AttentionLevel.OVERDUE
        if (remainingKm <= attentionThresholdKm / 2) return AttentionLevel.CRITICAL
        if (remainingKm <= attentionThresholdKm) return AttentionLevel.WARNING
        return AttentionLevel.NORMAL
    }
}
