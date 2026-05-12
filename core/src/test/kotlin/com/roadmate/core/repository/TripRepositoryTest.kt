package com.roadmate.core.repository

import com.roadmate.core.database.dao.TripDao
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
import com.roadmate.core.database.entity.TripStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [TripRepository].
 * Uses a fake DAO to verify repository delegates correctly and wraps errors in Result.
 */
class TripRepositoryTest {

    private lateinit var fakeDao: FakeTripDao
    private lateinit var repository: TripRepository

    @BeforeEach
    fun setup() {
        fakeDao = FakeTripDao()
        repository = TripRepository(fakeDao)
    }

    @Test
    fun `saveTrip delegates to dao upsert and returns success`() = runTest {
        val trip = createTestTrip()
        val result = repository.saveTrip(trip)

        assertTrue(result.isSuccess)
        assertEquals(trip, fakeDao.trips[trip.id])
    }

    @Test
    fun `saveTrip returns failure when dao throws`() = runTest {
        fakeDao.shouldThrow = true
        val result = repository.saveTrip(createTestTrip())

        assertTrue(result.isFailure)
    }

    @Test
    fun `getTripsForVehicle returns Flow from dao`() = runTest {
        val trip = createTestTrip(id = "t-1", vehicleId = "v-1")
        fakeDao.trips["t-1"] = trip
        fakeDao.updateFlow()

        val result = repository.getTripsForVehicle("v-1").first()
        assertEquals(1, result.size)
        assertEquals(trip, result[0])
    }

    @Test
    fun `getActiveTrip returns active trip`() = runTest {
        val active = createTestTrip(id = "t-1", vehicleId = "v-1", status = TripStatus.ACTIVE)
        fakeDao.trips["t-1"] = active
        fakeDao.updateFlow()

        val result = repository.getActiveTrip("v-1").first()
        assertEquals(active, result)
    }

    @Test
    fun `getActiveTrip returns null when no active trip`() = runTest {
        val completed = createTestTrip(id = "t-1", vehicleId = "v-1", status = TripStatus.COMPLETED)
        fakeDao.trips["t-1"] = completed
        fakeDao.updateFlow()

        val result = repository.getActiveTrip("v-1").first()
        assertNull(result)
    }

    @Test
    fun `saveTripPoint delegates to dao and returns success`() = runTest {
        val point = createTestTripPoint()
        val result = repository.saveTripPoint(point)

        assertTrue(result.isSuccess)
        assertEquals(point, fakeDao.tripPoints[point.id])
    }

    @Test
    fun `getTripPointsForTrip returns Flow from dao`() = runTest {
        val point = createTestTripPoint(id = "tp-1", tripId = "t-1")
        fakeDao.tripPoints["tp-1"] = point
        fakeDao.updateFlow()

        val result = repository.getTripPointsForTrip("t-1").first()
        assertEquals(1, result.size)
    }

    @Test
    fun `deleteTrip delegates to dao and returns success`() = runTest {
        val trip = createTestTrip(id = "t-1")
        fakeDao.trips["t-1"] = trip

        val result = repository.deleteTrip(trip)
        assertTrue(result.isSuccess)
        assertNull(fakeDao.trips["t-1"])
    }

    @Test
    fun `deleteTripById delegates to dao and returns success`() = runTest {
        val trip = createTestTrip(id = "t-1")
        fakeDao.trips["t-1"] = trip

        val result = repository.deleteTripById("t-1")
        assertTrue(result.isSuccess)
        assertNull(fakeDao.trips["t-1"])
    }

    @Test
    fun `flushTripPointsAndTrip saves points and trip atomically`() = runTest {
        val trip = createTestTrip(id = "t-1")
        val points = listOf(
            createTestTripPoint(id = "tp-1", tripId = "t-1"),
            createTestTripPoint(id = "tp-2", tripId = "t-1"),
        )

        val result = repository.flushTripPointsAndTrip(points, trip)

        assertTrue(result.isSuccess)
        assertEquals(trip, fakeDao.trips["t-1"])
        assertEquals(2, fakeDao.tripPoints.size)
    }

    @Test
    fun `flushTripPointsAndTrip returns failure when dao throws`() = runTest {
        fakeDao.shouldThrow = true
        val trip = createTestTrip()
        val points = listOf(createTestTripPoint())

        val result = repository.flushTripPointsAndTrip(points, trip)

        assertTrue(result.isFailure)
    }

    private fun createTestTrip(
        id: String = "test-id",
        vehicleId: String = "v-1",
        status: TripStatus = TripStatus.ACTIVE,
    ): Trip = Trip(
        id = id,
        vehicleId = vehicleId,
        startTime = System.currentTimeMillis(),
        distanceKm = 0.0,
        durationMs = 0L,
        maxSpeedKmh = 0.0,
        avgSpeedKmh = 0.0,
        estimatedFuelL = 0.0,
        startOdometerKm = 85000.0,
        endOdometerKm = 85000.0,
        status = status,
    )

    private fun createTestTripPoint(
        id: String = "tp-test",
        tripId: String = "t-1",
    ): TripPoint = TripPoint(
        id = id,
        tripId = tripId,
        latitude = 30.0444,
        longitude = 31.2357,
        speedKmh = 60.0,
        altitude = 75.0,
        accuracy = 5.0f,
        timestamp = System.currentTimeMillis(),
    )
}

/**
 * Fake implementation of [TripDao] for unit testing.
 */
private class FakeTripDao : TripDao() {
    val trips = mutableMapOf<String, Trip>()
    val tripPoints = mutableMapOf<String, TripPoint>()
    var shouldThrow = false

    private val tripFlow = MutableStateFlow<List<Trip>>(emptyList())
    private val tripPointFlow = MutableStateFlow<List<TripPoint>>(emptyList())

    fun updateFlow() {
        tripFlow.value = trips.values.toList()
        tripPointFlow.value = tripPoints.values.toList()
    }

    override fun getTripsForVehicle(vehicleId: String): Flow<List<Trip>> =
        tripFlow.map { list -> list.filter { it.vehicleId == vehicleId }.sortedByDescending { it.startTime } }

    override fun getTripPointsForTrip(tripId: String): Flow<List<TripPoint>> =
        tripPointFlow.map { list -> list.filter { it.tripId == tripId }.sortedBy { it.timestamp } }

    override fun getActiveTrip(vehicleId: String): Flow<Trip?> =
        tripFlow.map { list -> list.find { it.vehicleId == vehicleId && it.status == TripStatus.ACTIVE } }

    override fun getTrip(tripId: String): Flow<Trip?> =
        tripFlow.map { list -> list.find { it.id == tripId } }

    override suspend fun upsertTrip(trip: Trip) {
        if (shouldThrow) throw RuntimeException("Test error")
        trips[trip.id] = trip
        updateFlow()
    }

    override suspend fun upsertTrips(trips: List<Trip>) {
        if (shouldThrow) throw RuntimeException("Test error")
        trips.forEach { this.trips[it.id] = it }
        updateFlow()
    }

    override suspend fun deleteTrip(trip: Trip) {
        if (shouldThrow) throw RuntimeException("Test error")
        trips.remove(trip.id)
        updateFlow()
    }

    override suspend fun deleteTripById(tripId: String) {
        if (shouldThrow) throw RuntimeException("Test error")
        trips.remove(tripId)
        updateFlow()
    }

    override suspend fun upsertTripPoint(tripPoint: TripPoint) {
        if (shouldThrow) throw RuntimeException("Test error")
        tripPoints[tripPoint.id] = tripPoint
        updateFlow()
    }

    override suspend fun upsertTripPoints(tripPoints: List<TripPoint>) {
        if (shouldThrow) throw RuntimeException("Test error")
        tripPoints.forEach { this.tripPoints[it.id] = it }
        updateFlow()
    }

    override suspend fun deleteTripPoint(tripPoint: TripPoint) {
        if (shouldThrow) throw RuntimeException("Test error")
        tripPoints.remove(tripPoint.id)
        updateFlow()
    }

    override suspend fun getTripsModifiedSince(since: Long): List<Trip> =
        trips.values.filter { it.lastModified > since }

    override suspend fun getTripPointsModifiedSince(since: Long): List<TripPoint> =
        tripPoints.values.filter { it.lastModified > since }

    override suspend fun getTripById(id: String): Trip? = null

    override suspend fun getTripPointById(id: String): TripPoint? = null
}
