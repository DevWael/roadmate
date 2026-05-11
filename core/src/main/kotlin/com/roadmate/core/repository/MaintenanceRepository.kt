package com.roadmate.core.repository

import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for maintenance domain operations (schedules and records).
 *
 * Reads delegate directly to [MaintenanceDao] and return [Flow] for reactive observation.
 * Writes return [Result] via [runCatching] to encapsulate exceptions.
 */
@Singleton
class MaintenanceRepository @Inject constructor(
    private val maintenanceDao: MaintenanceDao,
) {

    // --- Schedule operations ---

    fun getSchedulesForVehicle(vehicleId: String): Flow<List<MaintenanceSchedule>> =
        maintenanceDao.getSchedulesForVehicle(vehicleId)

    fun getSchedule(scheduleId: String): Flow<MaintenanceSchedule?> =
        maintenanceDao.getSchedule(scheduleId)

    suspend fun saveSchedule(schedule: MaintenanceSchedule): Result<Unit> =
        runCatching { maintenanceDao.upsertSchedule(schedule) }

    suspend fun saveSchedules(schedules: List<MaintenanceSchedule>): Result<Unit> =
        runCatching { maintenanceDao.upsertSchedules(schedules) }

    suspend fun deleteSchedule(schedule: MaintenanceSchedule): Result<Unit> =
        runCatching { maintenanceDao.deleteSchedule(schedule) }

    suspend fun deleteScheduleById(scheduleId: String): Result<Unit> =
        runCatching { maintenanceDao.deleteScheduleById(scheduleId) }

    // --- Record operations ---

    fun getRecordsForSchedule(scheduleId: String): Flow<List<MaintenanceRecord>> =
        maintenanceDao.getRecordsForSchedule(scheduleId)

    fun getRecordsForVehicle(vehicleId: String): Flow<List<MaintenanceRecord>> =
        maintenanceDao.getRecordsForVehicle(vehicleId)

    fun getRecord(recordId: String): Flow<MaintenanceRecord?> =
        maintenanceDao.getRecord(recordId)

    suspend fun saveRecord(record: MaintenanceRecord): Result<Unit> =
        runCatching { maintenanceDao.upsertRecord(record) }

    suspend fun saveRecords(records: List<MaintenanceRecord>): Result<Unit> =
        runCatching { maintenanceDao.upsertRecords(records) }

    suspend fun deleteRecord(record: MaintenanceRecord): Result<Unit> =
        runCatching { maintenanceDao.deleteRecord(record) }

    suspend fun deleteRecordById(recordId: String): Result<Unit> =
        runCatching { maintenanceDao.deleteRecordById(recordId) }

    suspend fun deleteScheduleWithRecords(scheduleId: String): Result<Unit> =
        runCatching { maintenanceDao.deleteScheduleWithRecords(scheduleId) }

    // --- Transactional operations ---

    suspend fun completeMaintenance(record: MaintenanceRecord, schedule: MaintenanceSchedule): Result<Unit> =
        runCatching { maintenanceDao.completeMaintenance(record, schedule) }

    suspend fun undoCompletion(recordId: String, previousSchedule: MaintenanceSchedule): Result<Unit> =
        runCatching { maintenanceDao.undoCompletion(recordId, previousSchedule) }
}
