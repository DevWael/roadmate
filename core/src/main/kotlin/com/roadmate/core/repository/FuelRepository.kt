package com.roadmate.core.repository

import com.roadmate.core.database.dao.FuelDao
import com.roadmate.core.database.entity.FuelLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fuel log domain operations.
 *
 * Reads delegate directly to [FuelDao] and return [Flow] for reactive observation.
 * Writes return [Result] via [runCatching] to encapsulate exceptions.
 */
@Singleton
class FuelRepository @Inject constructor(
    private val fuelDao: FuelDao,
) {

    fun getFuelLogsForVehicle(vehicleId: String): Flow<List<FuelLog>> =
        fuelDao.getFuelLogsForVehicle(vehicleId)

    fun getLastFullTankEntry(vehicleId: String): Flow<FuelLog?> =
        fuelDao.getLastFullTankEntry(vehicleId)

    fun getFuelLog(fuelLogId: String): Flow<FuelLog?> =
        fuelDao.getFuelLog(fuelLogId)

    suspend fun saveFuelLog(fuelLog: FuelLog): Result<Unit> =
        runCatching { fuelDao.upsertFuelLog(fuelLog) }

    suspend fun saveFuelLogs(fuelLogs: List<FuelLog>): Result<Unit> =
        runCatching { fuelDao.upsertFuelLogs(fuelLogs) }

    suspend fun deleteFuelLog(fuelLog: FuelLog): Result<Unit> =
        runCatching { fuelDao.deleteFuelLog(fuelLog) }

    suspend fun deleteFuelLogById(fuelLogId: String): Result<Unit> =
        runCatching { fuelDao.deleteFuelLogById(fuelLogId) }
}
