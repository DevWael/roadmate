package com.roadmate.phone.ui.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadmate.core.database.entity.FuelLog
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.FuelRepository
import com.roadmate.core.repository.MaintenanceRepository
import com.roadmate.core.repository.TripRepository
import com.roadmate.core.repository.VehicleRepository
import com.roadmate.core.util.CsvExporter
import com.roadmate.core.util.PdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

enum class ExportFormat { CSV, PDF }
enum class ExportScope { TRIPS, FUEL, MAINTENANCE, ALL }

sealed interface ExportState {
    data object Idle : ExportState
    data object Loading : ExportState
    data class Success(val file: File, val mimeType: String) : ExportState
    data class Error(val message: String) : ExportState
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val activeVehicleRepository: ActiveVehicleRepository,
    private val tripRepository: TripRepository,
    private val fuelRepository: FuelRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {

    private val _format = MutableStateFlow(ExportFormat.CSV)
    val format: StateFlow<ExportFormat> = _format.asStateFlow()

    private val _scope = MutableStateFlow(ExportScope.ALL)
    val scope: StateFlow<ExportScope> = _scope.asStateFlow()

    private val _fromDate = MutableStateFlow<Long?>(null)
    val fromDate: StateFlow<Long?> = _fromDate.asStateFlow()

    private val _toDate = MutableStateFlow<Long?>(null)
    val toDate: StateFlow<Long?> = _toDate.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    val isExportEnabled: StateFlow<Boolean> = combine(
        _exportState,
        _format,
        _scope,
    ) { state, _, _ ->
        state !is ExportState.Loading
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setFormat(format: ExportFormat) {
        _format.value = format
    }

    fun setScope(scope: ExportScope) {
        _scope.value = scope
    }

    fun setFromDate(date: Long?) {
        _fromDate.value = date
    }

    fun setToDate(date: Long?) {
        _toDate.value = date
    }

    fun resetState() {
        _exportState.value = ExportState.Idle
    }

    fun export(context: Context) {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val vehicleId = activeVehicleRepository.activeVehicleId.first()
                if (vehicleId == null) {
                    _exportState.value = ExportState.Error("No active vehicle")
                    return@launch
                }

                val vehicle = vehicleRepository.getVehicle(vehicleId).first()
                if (vehicle == null) {
                    _exportState.value = ExportState.Error("Vehicle not found")
                    return@launch
                }

                val vehicleName = vehicle.name
                val fromMs = _fromDate.value
                val toMs = _toDate.value
                val cacheDir = context.cacheDir

                val files = mutableListOf<File>()
                val scopeVal = _scope.value
                val formatVal = _format.value

                withContext(Dispatchers.IO) {
                    if (scopeVal == ExportScope.TRIPS || scopeVal == ExportScope.ALL) {
                        val trips = tripRepository.getTripsForVehicle(vehicleId).first()
                        val file = generateExportFile(
                            trips = trips,
                            vehicleName = vehicleName,
                            fromMs = fromMs,
                            toMs = toMs,
                            cacheDir = cacheDir,
                            format = formatVal,
                            scope = "trips",
                            scopeEnum = ExportScope.TRIPS,
                        )
                        if (file != null) files.add(file)
                    }

                    if (scopeVal == ExportScope.FUEL || scopeVal == ExportScope.ALL) {
                        val fuelLogs = fuelRepository.getFuelLogsForVehicle(vehicleId).first()
                        val file = generateExportFile(
                            fuelLogs = fuelLogs,
                            vehicleName = vehicleName,
                            fromMs = fromMs,
                            toMs = toMs,
                            cacheDir = cacheDir,
                            format = formatVal,
                            scope = "fuel",
                            scopeEnum = ExportScope.FUEL,
                        )
                        if (file != null) files.add(file)
                    }

                    if (scopeVal == ExportScope.MAINTENANCE || scopeVal == ExportScope.ALL) {
                        val records = maintenanceRepository.getRecordsForVehicle(vehicleId).first()
                        val schedules = maintenanceRepository.getSchedulesForVehicle(vehicleId).first()
                            .associateBy { it.id }
                        val file = generateExportFile(
                            records = records,
                            schedules = schedules,
                            vehicleName = vehicleName,
                            fromMs = fromMs,
                            toMs = toMs,
                            cacheDir = cacheDir,
                            format = formatVal,
                            scope = "maintenance",
                            scopeEnum = ExportScope.MAINTENANCE,
                        )
                        if (file != null) files.add(file)
                    }
                }

                if (files.isEmpty()) {
                    _exportState.value = ExportState.Error("No data to export")
                } else if (files.size == 1) {
                    val mime = if (formatVal == ExportFormat.PDF) "application/pdf" else "text/csv"
                    _exportState.value = ExportState.Success(files.first(), mime)
                } else {
                    val zipFile = zipFiles(files, cacheDir, vehicleName)
                    _exportState.value = ExportState.Success(zipFile, "application/zip")
                }
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Export failed")
            }
        }
    }

    private fun generateExportFile(
        trips: List<Trip>? = null,
        fuelLogs: List<FuelLog>? = null,
        records: List<MaintenanceRecord>? = null,
        schedules: Map<String, MaintenanceSchedule>? = null,
        vehicleName: String,
        fromMs: Long?,
        toMs: Long?,
        cacheDir: File,
        format: ExportFormat,
        scope: String,
        scopeEnum: ExportScope,
    ): File? {
        return when (format) {
            ExportFormat.CSV -> {
                val content = when (scopeEnum) {
                    ExportScope.TRIPS -> CsvExporter.exportTrips(trips!!, vehicleName, fromMs, toMs)
                    ExportScope.FUEL -> CsvExporter.exportFuelLogs(fuelLogs!!, fromMs, toMs)
                    ExportScope.MAINTENANCE -> CsvExporter.exportMaintenance(records!!, schedules!!, fromMs, toMs)
                    ExportScope.ALL -> throw IllegalStateException("ALL should be handled by caller")
                }
                val fileName = CsvExporter.generateFileName(scope, vehicleName)
                CsvExporter.writeToCacheDir(content, cacheDir, fileName)
            }
            ExportFormat.PDF -> {
                when (scopeEnum) {
                    ExportScope.TRIPS -> PdfExporter.exportTripsPdf(trips!!, vehicleName, cacheDir, fromMs, toMs)
                    ExportScope.FUEL -> PdfExporter.exportFuelLogsPdf(fuelLogs!!, vehicleName, cacheDir, fromMs, toMs)
                    ExportScope.MAINTENANCE -> PdfExporter.exportMaintenancePdf(records!!, schedules!!, vehicleName, cacheDir, fromMs, toMs)
                    ExportScope.ALL -> throw IllegalStateException("ALL should be handled by caller")
                }
            }
        }
    }

    private fun zipFiles(files: List<File>, cacheDir: File, vehicleName: String): File {
        val sanitized = vehicleName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val dateStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        val exportDir = File(cacheDir, "exports")
        val zipFile = File(exportDir, "roadmate_export_${sanitized}_${dateStamp}.zip")

        java.util.zip.ZipOutputStream(zipFile.outputStream()).use { zos ->
            for (file in files) {
                val entry = java.util.zip.ZipEntry(file.name)
                zos.putNextEntry(entry)
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        return zipFile
    }

    fun createShareIntent(context: Context, file: File, mimeType: String): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
