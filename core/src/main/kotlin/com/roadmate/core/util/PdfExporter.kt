package com.roadmate.core.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.roadmate.core.database.entity.FuelLog
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.Trip
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private val dateFormat = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    private val timeFormat = ThreadLocal.withInitial { SimpleDateFormat("HH:mm", Locale.US) }
    private val fileDateFormat = ThreadLocal.withInitial { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US) }

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val TITLE_SIZE = 18f
    private const val HEADER_SIZE = 12f
    private const val BODY_SIZE = 10f
    private const val LINE_HEIGHT = 16f
    private const val TABLE_HEADER_BG = "#1A1A2E"

    fun exportTripsPdf(
        trips: List<Trip>,
        vehicleName: String,
        cacheDir: File,
        fromMs: Long? = null,
        toMs: Long? = null,
    ): File {
        val filtered = trips.filter { trip ->
            (fromMs == null || trip.startTime >= fromMs) &&
                (toMs == null || trip.startTime <= toMs)
        }

        val document = PdfDocument()
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = MARGIN

        val titlePaint = Paint().apply { textSize = TITLE_SIZE; isFakeBoldText = true; color = Color.BLACK }
        val headerPaint = Paint().apply { textSize = HEADER_SIZE; isFakeBoldText = true; color = Color.WHITE }
        val bodyPaint = Paint().apply { textSize = BODY_SIZE; color = Color.DKGRAY }
        val headerBgPaint = Paint().apply { color = Color.parseColor(TABLE_HEADER_BG); style = Paint.Style.FILL }

        y = drawTitle(canvas, titlePaint, vehicleName, y)
        y = drawExportDate(canvas, bodyPaint, y)
        y += 10f

        val columns = floatArrayOf(MARGIN, MARGIN + 70, MARGIN + 130, MARGIN + 195, MARGIN + 265, MARGIN + 335, MARGIN + 410, MARGIN + 470)
        val headers = arrayOf("Date", "Start", "End", "Dist(km)", "Dur(min)", "Avg(km/h)", "Max(km/h)", "Fuel(L)")

        y = drawTableHeader(canvas, headerBgPaint, headerPaint, headers, columns, y)

        for (trip in filtered) {
            if (y > PAGE_HEIGHT - 40) {
                document.finishPage(page)
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN
                y = drawTableHeader(canvas, headerBgPaint, headerPaint, headers, columns, y)
            }

            val date = dateFormat.get()!!.format(Date(trip.startTime))
            val startTime = timeFormat.get()!!.format(Date(trip.startTime))
            val endTime = trip.endTime?.let { timeFormat.get()!!.format(Date(it)) } ?: "-"
            val durationMin = trip.durationMs / 60_000
            val values = arrayOf(
                date, startTime, endTime,
                String.format(Locale.US, "%.1f", trip.distanceKm),
                durationMin.toString(),
                String.format(Locale.US, "%.0f", trip.avgSpeedKmh),
                String.format(Locale.US, "%.0f", trip.maxSpeedKmh),
                String.format(Locale.US, "%.1f", trip.estimatedFuelL),
            )
            y = drawTableRow(canvas, bodyPaint, values, columns, y)
        }

        document.finishPage(page)
        return writeToFile(document, cacheDir, "trips", vehicleName)
    }

    fun exportFuelLogsPdf(
        fuelLogs: List<FuelLog>,
        vehicleName: String,
        cacheDir: File,
        fromMs: Long? = null,
        toMs: Long? = null,
    ): File {
        val filtered = fuelLogs.filter { log ->
            (fromMs == null || log.date >= fromMs) &&
                (toMs == null || log.date <= toMs)
        }

        val document = PdfDocument()
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = MARGIN

        val titlePaint = Paint().apply { textSize = TITLE_SIZE; isFakeBoldText = true; color = Color.BLACK }
        val headerPaint = Paint().apply { textSize = HEADER_SIZE; isFakeBoldText = true; color = Color.WHITE }
        val bodyPaint = Paint().apply { textSize = BODY_SIZE; color = Color.DKGRAY }
        val headerBgPaint = Paint().apply { color = Color.parseColor(TABLE_HEADER_BG); style = Paint.Style.FILL }

        y = drawTitle(canvas, titlePaint, vehicleName, y)
        y = drawExportDate(canvas, bodyPaint, y)
        y += 10f

        val columns = floatArrayOf(MARGIN, MARGIN + 75, MARGIN + 140, MARGIN + 200, MARGIN + 270, MARGIN + 355, MARGIN + 430)
        val headers = arrayOf("Date", "ODO", "Liters", "Price/L", "Total", "Full", "Station")

        y = drawTableHeader(canvas, headerBgPaint, headerPaint, headers, columns, y)

        for (log in filtered) {
            if (y > PAGE_HEIGHT - 40) {
                document.finishPage(page)
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN
                y = drawTableHeader(canvas, headerBgPaint, headerPaint, headers, columns, y)
            }

            val values = arrayOf(
                dateFormat.get()!!.format(Date(log.date)),
                String.format(Locale.US, "%.0f", log.odometerKm),
                String.format(Locale.US, "%.1f", log.liters),
                String.format(Locale.US, "%.2f", log.pricePerLiter),
                String.format(Locale.US, "%.0f", log.totalCost),
                if (log.isFullTank) "Yes" else "No",
                log.station ?: "-",
            )
            y = drawTableRow(canvas, bodyPaint, values, columns, y)
        }

        document.finishPage(page)
        return writeToFile(document, cacheDir, "fuel", vehicleName)
    }

    fun exportMaintenancePdf(
        records: List<MaintenanceRecord>,
        schedules: Map<String, MaintenanceSchedule>,
        vehicleName: String,
        cacheDir: File,
        fromMs: Long? = null,
        toMs: Long? = null,
    ): File {
        val filtered = records.filter { record ->
            (fromMs == null || record.datePerformed >= fromMs) &&
                (toMs == null || record.datePerformed <= toMs)
        }

        val document = PdfDocument()
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = MARGIN

        val titlePaint = Paint().apply { textSize = TITLE_SIZE; isFakeBoldText = true; color = Color.BLACK }
        val headerPaint = Paint().apply { textSize = HEADER_SIZE; isFakeBoldText = true; color = Color.WHITE }
        val bodyPaint = Paint().apply { textSize = BODY_SIZE; color = Color.DKGRAY }
        val headerBgPaint = Paint().apply { color = Color.parseColor(TABLE_HEADER_BG); style = Paint.Style.FILL }

        y = drawTitle(canvas, titlePaint, vehicleName, y)
        y = drawExportDate(canvas, bodyPaint, y)
        y += 10f

        val columns = floatArrayOf(MARGIN, MARGIN + 100, MARGIN + 165, MARGIN + 230, MARGIN + 310, MARGIN + 400)
        val headers = arrayOf("Item", "Date", "ODO", "Cost", "Location", "Notes")

        y = drawTableHeader(canvas, headerBgPaint, headerPaint, headers, columns, y)

        for (record in filtered) {
            if (y > PAGE_HEIGHT - 40) {
                document.finishPage(page)
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN
                y = drawTableHeader(canvas, headerBgPaint, headerPaint, headers, columns, y)
            }

            val itemName = schedules[record.scheduleId]?.name ?: "Unknown"
            val notes = record.notes ?: "-"
            val truncatedNotes = if (notes.length > 20) notes.substring(0, 17) + "..." else notes
            val values = arrayOf(
                itemName,
                dateFormat.get()!!.format(Date(record.datePerformed)),
                String.format(Locale.US, "%.0f", record.odometerKm),
                record.cost?.let { String.format(Locale.US, "%.0f", it) } ?: "-",
                record.location ?: "-",
                truncatedNotes,
            )
            y = drawTableRow(canvas, bodyPaint, values, columns, y)
        }

        document.finishPage(page)
        return writeToFile(document, cacheDir, "maintenance", vehicleName)
    }

    private fun drawTitle(canvas: android.graphics.Canvas, paint: Paint, vehicleName: String, y: Float): Float {
        canvas.drawText("RoadMate Export — $vehicleName", MARGIN, y + TITLE_SIZE, paint)
        return y + TITLE_SIZE + 10f
    }

    private fun drawExportDate(canvas: android.graphics.Canvas, paint: Paint, y: Float): Float {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        canvas.drawText("Exported: $now", MARGIN, y + BODY_SIZE, paint)
        return y + BODY_SIZE + 10f
    }

    private fun drawTableHeader(
        canvas: android.graphics.Canvas,
        bgPaint: Paint,
        textPaint: Paint,
        headers: Array<String>,
        columns: FloatArray,
        y: Float,
    ): Float {
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN.toFloat(), y + LINE_HEIGHT + 4f, bgPaint)
        for (i in headers.indices) {
            canvas.drawText(headers[i], columns[i], y + HEADER_SIZE, textPaint)
        }
        return y + LINE_HEIGHT + 8f
    }

    private fun drawTableRow(
        canvas: android.graphics.Canvas,
        paint: Paint,
        values: Array<String>,
        columns: FloatArray,
        y: Float,
    ): Float {
        for (i in values.indices) {
            canvas.drawText(values[i], columns[i], y + BODY_SIZE, paint)
        }
        return y + LINE_HEIGHT
    }

    private fun writeToFile(
        document: PdfDocument,
        cacheDir: File,
        scope: String,
        vehicleName: String,
    ): File {
        val sanitized = vehicleName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val dateStamp = fileDateFormat.get()!!.format(Date())
        val fileName = "roadmate_${scope}_${sanitized}_${dateStamp}.pdf"

        val exportDir = File(cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)
        try {
            document.writeTo(FileOutputStream(file))
        } finally {
            document.close()
        }
        return file
    }
}
