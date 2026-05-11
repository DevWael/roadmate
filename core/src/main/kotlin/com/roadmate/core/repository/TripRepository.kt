package com.roadmate.core.repository

import com.roadmate.core.database.dao.TripDao
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for trip domain operations (trips and trip points).
 *
 * Reads delegate directly to [TripDao] and return [Flow] for reactive observation.
 * Writes return [Result] via [runCatching] to encapsulate exceptions.
 */
@Singleton
class TripRepository @Inject constructor(
    private val tripDao: TripDao,
) {

    fun getTripsForVehicle(vehicleId: String): Flow<List<Trip>> =
        tripDao.getTripsForVehicle(vehicleId)

    fun getTripPointsForTrip(tripId: String): Flow<List<TripPoint>> =
        tripDao.getTripPointsForTrip(tripId)

    fun getActiveTrip(vehicleId: String): Flow<Trip?> =
        tripDao.getActiveTrip(vehicleId)

    fun getTrip(tripId: String): Flow<Trip?> =
        tripDao.getTrip(tripId)

    suspend fun saveTrip(trip: Trip): Result<Unit> =
        runCatching { tripDao.upsertTrip(trip) }

    suspend fun saveTrips(trips: List<Trip>): Result<Unit> =
        runCatching { tripDao.upsertTrips(trips) }

    suspend fun deleteTrip(trip: Trip): Result<Unit> =
        runCatching { tripDao.deleteTrip(trip) }

    suspend fun deleteTripById(tripId: String): Result<Unit> =
        runCatching { tripDao.deleteTripById(tripId) }

    // --- TripPoint operations ---

    suspend fun saveTripPoint(tripPoint: TripPoint): Result<Unit> =
        runCatching { tripDao.upsertTripPoint(tripPoint) }

    suspend fun saveTripPoints(tripPoints: List<TripPoint>): Result<Unit> =
        runCatching { tripDao.upsertTripPoints(tripPoints) }

    suspend fun deleteTripPoint(tripPoint: TripPoint): Result<Unit> =
        runCatching { tripDao.deleteTripPoint(tripPoint) }

    suspend fun flushTripPointsAndTrip(tripPoints: List<TripPoint>, trip: Trip): Result<Unit> =
        runCatching { tripDao.flushTripPointsAndTrip(tripPoints, trip) }
}
