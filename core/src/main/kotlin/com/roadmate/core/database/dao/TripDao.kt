package com.roadmate.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TripDao {

    @Query("SELECT * FROM trips WHERE vehicle_id = :vehicleId ORDER BY start_time DESC")
    abstract fun getTripsForVehicle(vehicleId: String): Flow<List<Trip>>

    @Query("SELECT * FROM trip_points WHERE trip_id = :tripId ORDER BY timestamp ASC")
    abstract fun getTripPointsForTrip(tripId: String): Flow<List<TripPoint>>

    @Query("SELECT * FROM trips WHERE vehicle_id = :vehicleId AND status = 'ACTIVE' LIMIT 1")
    abstract fun getActiveTrip(vehicleId: String): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    abstract fun getTrip(tripId: String): Flow<Trip?>

    @Upsert
    abstract suspend fun upsertTrip(trip: Trip)

    @Upsert
    abstract suspend fun upsertTrips(trips: List<Trip>)

    @Delete
    abstract suspend fun deleteTrip(trip: Trip)

    @Query("DELETE FROM trips WHERE id = :tripId")
    abstract suspend fun deleteTripById(tripId: String)

    @Upsert
    abstract suspend fun upsertTripPoint(tripPoint: TripPoint)

    @Upsert
    abstract suspend fun upsertTripPoints(tripPoints: List<TripPoint>)

    @Delete
    abstract suspend fun deleteTripPoint(tripPoint: TripPoint)

    @Transaction
    open suspend fun flushTripPointsAndTrip(tripPoints: List<TripPoint>, trip: Trip) {
        upsertTripPoints(tripPoints)
        upsertTrip(trip)
    }

    @Query("SELECT * FROM trips WHERE last_modified > :since")
    abstract suspend fun getTripsModifiedSince(since: Long): List<Trip>

    @Query("SELECT * FROM trip_points WHERE last_modified > :since")
    abstract suspend fun getTripPointsModifiedSince(since: Long): List<TripPoint>

    @Query("SELECT * FROM trips WHERE id = :id")
    abstract suspend fun getTripById(id: String): Trip?

    @Query("SELECT * FROM trip_points WHERE id = :id")
    abstract suspend fun getTripPointById(id: String): TripPoint?
}
