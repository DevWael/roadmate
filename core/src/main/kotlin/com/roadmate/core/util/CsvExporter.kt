package com.roadmate.core.util

import com.roadmate.core.database.entity.FuelLog
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.Trip
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private const val SEP = ","
    private val dateFormat = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    private val timeFormat = ThreadLocal.withInitial { SimpleDateFormat("HH:mm", Locale.US) }
    private val fileDateFormat = ThreadLocal.withInitial { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US) }

    fun exportTrips(
        trips: List<Trip>,
        vehicleName: String,
        fromMs: Long? = null,
        toMs: Long? = null,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Start Time,End Time,Distance,Duration,Avg Speed,Max Speed,Est Fuel,Status")

        val filtered = trips.filter { trip ->
            (fromMs == null || trip.startTime >= fromMs) &&
                (toMs == null || trip.startTime < toMs)
        }

        for (trip in filtered) {
            val date = dateFormat.get()!!.format(Date(trip.startTime))
            val startTime = timeFormat.get()!!.format(Date(trip.startTime))
            val endTime = trip.endTime?.let { timeFormat.get()!!.format(Date(it)) } ?: ""
            val durationMin = trip.durationMs / 60_000
            sb.appendLine(
                "$date$SEP$startTime$SEP$endTime$SEP" +
                    "${formatDec(trip.distanceKm)} km$SEP" +
                    "${durationMin} min$SEP" +
                    "${formatDec(trip.avgSpeedKmh)} km/h$SEP" +
                    "${formatDec(trip.maxSpeedKmh)} km/h$SEP" +
                    "${formatDec(trip.estimatedFuelL)} L$SEP" +
                    trip.status.name
            )
        }
        return sb.toString()
    }

    fun exportFuelLogs(
        fuelLogs: List<FuelLog>,
        fromMs: Long? = null,
        toMs: Long? = null,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Date,ODO,Liters,Price/L,Total Cost,Full Tank,Station,Consumption")

        val filtered = fuelLogs.filter { log ->
            (fromMs == null || log.date >= fromMs) &&
                (toMs == null || log.date < toMs)
        }

        for (log in filtered) {
            val date = dateFormat.get()!!.format(Date(log.date))
            val fullTank = if (log.isFullTank) "Yes" else "No"
            val station = escapeCsv(log.station ?: "")
            val consumption = "" // Consumption requires full-tank pair calculation, not available per-row
            sb.appendLine(
                "$date$SEP${formatDec(log.odometerKm)}$SEP" +
                    "${formatDec(log.liters)}$SEP" +
                    "${formatDec(log.pricePerLiter)}$SEP" +
                    "${formatDec(log.totalCost)}$SEP" +
                    "$fullTank$SEP$station$SEP$consumption"
            )
        }
        return sb.toString()
    }

    fun exportMaintenance(
        records: List<MaintenanceRecord>,
        schedules: Map<String, MaintenanceSchedule>,
        fromMs: Long? = null,
        toMs: Long? = null,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Item Name,Date,ODO,Cost,Location,Notes")

        val filtered = records.filter { record ->
            (fromMs == null || record.datePerformed >= fromMs) &&
                (toMs == null || record.datePerformed < toMs)
        }

        for (record in filtered) {
            val itemName = escapeCsv(schedules[record.scheduleId]?.name ?: "Unknown")
            val date = dateFormat.get()!!.format(Date(record.datePerformed))
            val cost = record.cost?.let { formatDec(it) } ?: ""
            val location = escapeCsv(record.location ?: "")
            val notes = escapeCsv(record.notes ?: "")
            sb.appendLine(
                "$itemName$SEP$date$SEP${formatDec(record.odometerKm)}$SEP" +
                    "$cost$SEP$location$SEP$notes"
            )
        }
        return sb.toString()
    }

    fun generateFileName(scope: String, vehicleName: String): String {
        val sanitized = vehicleName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val dateStamp = fileDateFormat.get()!!.format(Date())
        return "roadmate_${scope}_${sanitized}_${dateStamp}.csv"
    }

    fun writeToCacheDir(
        content: String,
        cacheDir: File,
        fileName: String,
    ): File {
        val exportDir = File(cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)
        file.writeText(content)
        return file
    }

    private fun formatDec(value: Double): String {
        return String.format(Locale.US, "%.1f", value)
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
