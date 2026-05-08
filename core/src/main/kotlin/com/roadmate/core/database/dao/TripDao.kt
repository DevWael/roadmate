package com.roadmate.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [Trip] and [TripPoint] entities.
 *
 * All queries are scoped by vehicleId or tripId — no unscoped queries.
 * Reads return [Flow] for reactive observation.
 * Writes use [@Upsert] for idempotent insert-or-update.
 */
@Dao
interface TripDao {

    // --- Trip queries ---

    @Query("SELECT * FROM trips WHERE vehicle_id = :vehicleId ORDER BY start_time DESC")
    fun getTripsForVehicle(vehicleId: String): Flow<List<Trip>>

    @Query("SELECT * FROM trip_points WHERE trip_id = :tripId ORDER BY timestamp ASC")
    fun getTripPointsForTrip(tripId: String): Flow<List<TripPoint>>

    /**
     * Returns the currently active trip for a vehicle, if any.
     *
     * Note: Room requires literal strings in queries — `'ACTIVE'` is coupled to
     * [TripStatus.ACTIVE].name. If the enum value is renamed, this query must be updated.
     */
    @Query("SELECT * FROM trips WHERE vehicle_id = :vehicleId AND status = 'ACTIVE' LIMIT 1")
    fun getActiveTrip(vehicleId: String): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun getTrip(tripId: String): Flow<Trip?>

    @Upsert
    suspend fun upsertTrip(trip: Trip)

    @Upsert
    suspend fun upsertTrips(trips: List<Trip>)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTripById(tripId: String)

    // --- TripPoint queries ---

    @Upsert
    suspend fun upsertTripPoint(tripPoint: TripPoint)

    @Upsert
    suspend fun upsertTripPoints(tripPoints: List<TripPoint>)

    @Delete
    suspend fun deleteTripPoint(tripPoint: TripPoint)
}
