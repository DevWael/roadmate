package com.roadmate.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MaintenanceDao {

    // --- MaintenanceSchedule queries ---

    @Query("SELECT * FROM maintenance_schedules WHERE vehicle_id = :vehicleId ORDER BY name ASC")
    abstract fun getSchedulesForVehicle(vehicleId: String): Flow<List<MaintenanceSchedule>>

    @Query("SELECT * FROM maintenance_schedules WHERE id = :scheduleId")
    abstract fun getSchedule(scheduleId: String): Flow<MaintenanceSchedule?>

    @Upsert
    abstract suspend fun upsertSchedule(schedule: MaintenanceSchedule)

    @Upsert
    abstract suspend fun upsertSchedules(schedules: List<MaintenanceSchedule>)

    @Delete
    abstract suspend fun deleteSchedule(schedule: MaintenanceSchedule)

    @Query("DELETE FROM maintenance_schedules WHERE id = :scheduleId")
    abstract suspend fun deleteScheduleById(scheduleId: String)

    // --- MaintenanceRecord queries ---

    @Query("SELECT * FROM maintenance_records WHERE schedule_id = :scheduleId ORDER BY date_performed DESC")
    abstract fun getRecordsForSchedule(scheduleId: String): Flow<List<MaintenanceRecord>>

    @Query("SELECT * FROM maintenance_records WHERE vehicle_id = :vehicleId ORDER BY date_performed DESC")
    abstract fun getRecordsForVehicle(vehicleId: String): Flow<List<MaintenanceRecord>>

    @Query("SELECT * FROM maintenance_records WHERE id = :recordId")
    abstract fun getRecord(recordId: String): Flow<MaintenanceRecord?>

    @Upsert
    abstract suspend fun upsertRecord(record: MaintenanceRecord)

    @Upsert
    abstract suspend fun upsertRecords(records: List<MaintenanceRecord>)

    @Delete
    abstract suspend fun deleteRecord(record: MaintenanceRecord)

    @Query("DELETE FROM maintenance_records WHERE id = :recordId")
    abstract suspend fun deleteRecordById(recordId: String)

    @Query("DELETE FROM maintenance_records WHERE schedule_id = :scheduleId")
    abstract suspend fun deleteRecordsByScheduleId(scheduleId: String)

    // --- Transactional operations ---

    @Transaction
    open suspend fun completeMaintenance(record: MaintenanceRecord, schedule: MaintenanceSchedule) {
        upsertRecord(record)
        upsertSchedule(schedule)
    }

    @Transaction
    open suspend fun undoCompletion(recordId: String, previousSchedule: MaintenanceSchedule) {
        deleteRecordById(recordId)
        upsertSchedule(previousSchedule)
    }

    @Transaction
    open suspend fun deleteScheduleWithRecords(scheduleId: String) {
        deleteRecordsByScheduleId(scheduleId)
        deleteScheduleById(scheduleId)
    }

    @Query("SELECT * FROM maintenance_schedules WHERE last_modified > :since")
    abstract suspend fun getSchedulesModifiedSince(since: Long): List<MaintenanceSchedule>

    @Query("SELECT * FROM maintenance_records WHERE last_modified > :since")
    abstract suspend fun getRecordsModifiedSince(since: Long): List<MaintenanceRecord>

    @Query("SELECT * FROM maintenance_schedules WHERE id = :id")
    abstract suspend fun getScheduleById(id: String): MaintenanceSchedule?

    @Query("SELECT * FROM maintenance_records WHERE id = :id")
    abstract suspend fun getRecordById(id: String): MaintenanceRecord?
}
