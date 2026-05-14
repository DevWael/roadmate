package com.roadmate.core.util

import com.roadmate.core.database.entity.FuelLog
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object StatisticsCalculator {

    data class DrivingStatistics(
        val totalDistanceKm: Double,
        val totalTrips: Int,
        val avgTripDistanceKm: Double,
        val totalDrivingTimeMs: Long,
        val totalFuelCost: Double,
        val totalMaintenanceCost: Double,
        val costPerKm: Double,
    )

    data class MonthBreakdown(
        val month: Int,
        val monthName: String,
        val distanceKm: Double,
        val fuelCost: Double,
        val maintenanceCost: Double,
    )

    data class WeekComparison(
        val currentDistanceKm: Double,
        val previousDistanceKm: Double,
        val distanceChangePercent: Double?,
        val currentFuelCost: Double,
        val previousFuelCost: Double,
        val fuelCostChangePercent: Double?,
    )

    private val MONTH_NAMES = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )

    fun calculateStatistics(
        trips: List<Trip>,
        fuelLogs: List<FuelLog>,
        maintenanceRecords: List<MaintenanceRecord>,
        fromEpochMs: Long,
        toEpochMs: Long,
    ): DrivingStatistics {
        val filteredTrips = trips.filter {
            it.status == TripStatus.COMPLETED &&
                it.startTime >= fromEpochMs &&
                it.startTime < toEpochMs
        }
        val filteredFuelLogs = fuelLogs.filter { it.date >= fromEpochMs && it.date < toEpochMs }
        val filteredMaintenanceRecords = maintenanceRecords.filter {
            it.datePerformed >= fromEpochMs && it.datePerformed < toEpochMs
        }

        val totalDistance = filteredTrips.sumOf { it.distanceKm }
        val totalTrips = filteredTrips.size
        val avgTripDistance = if (totalTrips > 0) totalDistance / totalTrips else 0.0
        val totalDrivingTime = filteredTrips.sumOf { it.durationMs }
        val totalFuelCost = filteredFuelLogs.sumOf { it.totalCost }
        val totalMaintenanceCost = filteredMaintenanceRecords.sumOf { it.cost ?: 0.0 }
        val costPerKm = if (totalDistance > 0) {
            (totalFuelCost + totalMaintenanceCost) / totalDistance
        } else 0.0

        return DrivingStatistics(
            totalDistanceKm = totalDistance,
            totalTrips = totalTrips,
            avgTripDistanceKm = avgTripDistance,
            totalDrivingTimeMs = totalDrivingTime,
            totalFuelCost = totalFuelCost,
            totalMaintenanceCost = totalMaintenanceCost,
            costPerKm = costPerKm,
        )
    }

    fun calculateYearBreakdown(
        trips: List<Trip>,
        fuelLogs: List<FuelLog>,
        maintenanceRecords: List<MaintenanceRecord>,
        year: Int,
    ): Pair<List<MonthBreakdown>, DrivingStatistics> {
        val zone = ZoneId.systemDefault()
        val months = (1..12).map { month ->
            val start = LocalDate.of(year, month, 1)
                .atStartOfDay(zone).toInstant().toEpochMilli()
            val end = LocalDate.of(year, month, 1)
                .plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()

            val stats = calculateStatistics(trips, fuelLogs, maintenanceRecords, start, end)
            MonthBreakdown(
                month = month,
                monthName = MONTH_NAMES[month - 1],
                distanceKm = stats.totalDistanceKm,
                fuelCost = stats.totalFuelCost,
                maintenanceCost = stats.totalMaintenanceCost,
            )
        }

        val yearStart = LocalDate.of(year, 1, 1)
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val yearEnd = LocalDate.of(year + 1, 1, 1)
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val runningTotal = calculateStatistics(
            trips, fuelLogs, maintenanceRecords, yearStart, yearEnd,
        )

        return months to runningTotal
    }

    fun calculateWeekComparison(
        trips: List<Trip>,
        fuelLogs: List<FuelLog>,
        currentWeekStart: Long,
        currentWeekEnd: Long,
        previousWeekStart: Long,
        previousWeekEnd: Long,
    ): WeekComparison {
        val currentTrips = trips.filter {
            it.status == TripStatus.COMPLETED &&
                it.startTime >= currentWeekStart &&
                it.startTime < currentWeekEnd
        }
        val previousTrips = trips.filter {
            it.status == TripStatus.COMPLETED &&
                it.startTime >= previousWeekStart &&
                it.startTime < previousWeekEnd
        }

        val currentDistance = currentTrips.sumOf { it.distanceKm }
        val previousDistance = previousTrips.sumOf { it.distanceKm }

        val currentFuelCost = fuelLogs
            .filter { it.date >= currentWeekStart && it.date < currentWeekEnd }
            .sumOf { it.totalCost }
        val previousFuelCost = fuelLogs
            .filter { it.date >= previousWeekStart && it.date < previousWeekEnd }
            .sumOf { it.totalCost }

        return WeekComparison(
            currentDistanceKm = currentDistance,
            previousDistanceKm = previousDistance,
            distanceChangePercent = percentChange(previousDistance, currentDistance),
            currentFuelCost = currentFuelCost,
            previousFuelCost = previousFuelCost,
            fuelCostChangePercent = percentChange(previousFuelCost, currentFuelCost),
        )
    }

    fun percentChange(oldValue: Double, newValue: Double): Double? {
        if (oldValue <= 0.0) return null
        return ((newValue - oldValue) / oldValue) * 100.0
    }

    fun dayRange(date: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    fun weekRange(date: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val start = startOfWeek.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = startOfWeek.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    fun monthRange(date: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = date.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    fun yearRange(date: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = date.withDayOfYear(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.withDayOfYear(1).plusYears(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }
}
