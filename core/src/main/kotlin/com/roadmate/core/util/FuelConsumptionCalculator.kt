package com.roadmate.core.util

import com.roadmate.core.database.entity.FuelLog

object FuelConsumptionCalculator {

    fun calculateActualConsumption(current: FuelLog, previous: FuelLog): Double? {
        if (!current.isFullTank || !previous.isFullTank) return null
        val distance = current.odometerKm - previous.odometerKm
        if (distance <= 0) return null
        return current.liters / distance * 100.0
    }

    fun isOverConsumption(actualLPer100km: Double, estimatedLPer100km: Double): Boolean {
        if (estimatedLPer100km <= 0) return false
        return actualLPer100km > estimatedLPer100km * 1.20
    }

    fun calculateEstimatedConsumption(cityConsumption: Double, highwayConsumption: Double): Double {
        return (cityConsumption + highwayConsumption) / 2.0
    }

    fun calculateAvgLPer100km(fullTankPairs: List<Pair<FuelLog, FuelLog>>): Double? {
        if (fullTankPairs.isEmpty()) return null
        var totalLiters = 0.0
        var totalDistance = 0.0
        for ((current, previous) in fullTankPairs) {
            val distance = current.odometerKm - previous.odometerKm
            if (distance > 0) {
                totalLiters += current.liters
                totalDistance += distance
            }
        }
        if (totalDistance <= 0) return null
        return totalLiters / totalDistance * 100.0
    }

    fun calculateAvgCostPerKm(entries: List<FuelLog>): Double? {
        if (entries.isEmpty()) return null
        val sorted = entries.sortedBy { it.odometerKm }
        if (sorted.size < 2) return null
        val minOdo = sorted.first().odometerKm
        val maxOdo = sorted.last().odometerKm
        val totalDistance = maxOdo - minOdo
        if (totalDistance <= 0) return null
        // Exclude first entry's cost — it represents fuel consumed before the tracking window
        val totalCost = sorted.drop(1).sumOf { it.totalCost }
        return totalCost / totalDistance
    }

    fun calculateTotalCostThisMonth(entries: List<FuelLog>, currentMonthStart: Long): Double {
        return entries.filter { it.date >= currentMonthStart }.sumOf { it.totalCost }
    }

    fun findFullTankPairs(entries: List<FuelLog>): List<Pair<FuelLog, FuelLog>> {
        val fullTanks = entries
            .filter { it.isFullTank }
            .sortedBy { it.date }
        val pairs = mutableListOf<Pair<FuelLog, FuelLog>>()
        for (i in 1 until fullTanks.size) {
            pairs.add(fullTanks[i] to fullTanks[i - 1])
        }
        return pairs
    }
}
