package com.roadmate.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.roadmate.core.database.entity.Vehicle
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [Vehicle] entities.
 *
 * All queries are scoped by vehicle ID — no unscoped "get all" queries.
 * Reads return [Flow] for reactive observation.
 * Writes use [@Upsert] (Room 2.8+) for idempotent insert-or-update.
 */
@Dao
interface VehicleDao {

    @Query("SELECT * FROM vehicles WHERE id = :vehicleId")
    fun getVehicle(vehicleId: String): Flow<Vehicle?>

    @Query("SELECT * FROM vehicles ORDER BY last_modified DESC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Upsert
    suspend fun upsert(vehicle: Vehicle)

    @Upsert
    suspend fun upsertAll(vehicles: List<Vehicle>)

    @Delete
    suspend fun delete(vehicle: Vehicle)

    @Query("DELETE FROM vehicles WHERE id = :vehicleId")
    suspend fun deleteById(vehicleId: String)

    @Query("SELECT COUNT(*) FROM vehicles")
    fun getVehicleCount(): Flow<Int>

    @Query("UPDATE vehicles SET odometer_km = odometer_km + :distanceKm, last_modified = :lastModified WHERE id = :vehicleId")
    suspend fun addToOdometer(vehicleId: String, distanceKm: Double, lastModified: Long)
}
