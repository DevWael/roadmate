package com.roadmate.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [MaintenanceSchedule] and [MaintenanceRecord] entities.
 *
 * All queries are scoped by vehicleId or scheduleId — no unscoped queries.
 * Reads return [Flow] for reactive observation.
 * Writes use [@Upsert] for idempotent insert-or-update.
 */
@Dao
interface MaintenanceDao {

    // --- MaintenanceSchedule queries ---

    @Query("SELECT * FROM maintenance_schedules WHERE vehicle_id = :vehicleId ORDER BY name ASC")
    fun getSchedulesForVehicle(vehicleId: String): Flow<List<MaintenanceSchedule>>

    @Query("SELECT * FROM maintenance_schedules WHERE id = :scheduleId")
    fun getSchedule(scheduleId: String): Flow<MaintenanceSchedule?>

    @Upsert
    suspend fun upsertSchedule(schedule: MaintenanceSchedule)

    @Upsert
    suspend fun upsertSchedules(schedules: List<MaintenanceSchedule>)

    @Delete
    suspend fun deleteSchedule(schedule: MaintenanceSchedule)

    @Query("DELETE FROM maintenance_schedules WHERE id = :scheduleId")
    suspend fun deleteScheduleById(scheduleId: String)

    // --- MaintenanceRecord queries ---

    @Query("SELECT * FROM maintenance_records WHERE schedule_id = :scheduleId ORDER BY date_performed DESC")
    fun getRecordsForSchedule(scheduleId: String): Flow<List<MaintenanceRecord>>

    @Query("SELECT * FROM maintenance_records WHERE vehicle_id = :vehicleId ORDER BY date_performed DESC")
    fun getRecordsForVehicle(vehicleId: String): Flow<List<MaintenanceRecord>>

    @Query("SELECT * FROM maintenance_records WHERE id = :recordId")
    fun getRecord(recordId: String): Flow<MaintenanceRecord?>

    @Upsert
    suspend fun upsertRecord(record: MaintenanceRecord)

    @Upsert
    suspend fun upsertRecords(records: List<MaintenanceRecord>)

    @Delete
    suspend fun deleteRecord(record: MaintenanceRecord)

    @Query("DELETE FROM maintenance_records WHERE id = :recordId")
    suspend fun deleteRecordById(recordId: String)
}
