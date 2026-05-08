package com.roadmate.core.repository

import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.Vehicle
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for [Vehicle] domain operations.
 *
 * Reads delegate directly to [VehicleDao] and return [Flow] for reactive observation.
 * Writes return [Result] via [runCatching] to encapsulate exceptions.
 */
@Singleton
class VehicleRepository @Inject constructor(
    private val vehicleDao: VehicleDao,
) {

    fun getVehicle(vehicleId: String): Flow<Vehicle?> =
        vehicleDao.getVehicle(vehicleId)

    fun getAllVehicles(): Flow<List<Vehicle>> =
        vehicleDao.getAllVehicles()

    fun getVehicleCount(): Flow<Int> =
        vehicleDao.getVehicleCount()

    suspend fun saveVehicle(vehicle: Vehicle): Result<Unit> =
        runCatching { vehicleDao.upsert(vehicle) }

    suspend fun saveVehicles(vehicles: List<Vehicle>): Result<Unit> =
        runCatching { vehicleDao.upsertAll(vehicles) }

    suspend fun deleteVehicle(vehicle: Vehicle): Result<Unit> =
        runCatching { vehicleDao.delete(vehicle) }

    suspend fun deleteVehicleById(vehicleId: String): Result<Unit> =
        runCatching { vehicleDao.deleteById(vehicleId) }
}
