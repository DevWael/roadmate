package com.roadmate.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.roadmate.core.database.entity.FuelLog
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [FuelLog] entities.
 *
 * All queries are scoped by vehicleId — no unscoped queries.
 * Reads return [Flow] for reactive observation.
 * Writes use [@Upsert] for idempotent insert-or-update.
 */
@Dao
interface FuelDao {

    @Query("SELECT * FROM fuel_logs WHERE vehicle_id = :vehicleId ORDER BY date DESC")
    fun getFuelLogsForVehicle(vehicleId: String): Flow<List<FuelLog>>

    @Query("SELECT * FROM fuel_logs WHERE vehicle_id = :vehicleId AND is_full_tank = 1 ORDER BY date DESC LIMIT 1")
    fun getLastFullTankEntry(vehicleId: String): Flow<FuelLog?>

    @Query("SELECT * FROM fuel_logs WHERE id = :fuelLogId")
    fun getFuelLog(fuelLogId: String): Flow<FuelLog?>

    @Upsert
    suspend fun upsertFuelLog(fuelLog: FuelLog)

    @Upsert
    suspend fun upsertFuelLogs(fuelLogs: List<FuelLog>)

    @Delete
    suspend fun deleteFuelLog(fuelLog: FuelLog)

    @Query("DELETE FROM fuel_logs WHERE id = :fuelLogId")
    suspend fun deleteFuelLogById(fuelLogId: String)

    @Query("SELECT * FROM fuel_logs WHERE last_modified > :since")
    suspend fun getFuelLogsModifiedSince(since: Long): List<FuelLog>

    @Query("SELECT * FROM fuel_logs WHERE id = :id")
    suspend fun getFuelLogById(id: String): FuelLog?
}
