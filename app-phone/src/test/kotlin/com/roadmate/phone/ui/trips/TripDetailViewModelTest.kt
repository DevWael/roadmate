package com.roadmate.phone.ui.trips

import app.cash.turbine.test
import com.roadmate.core.database.dao.TripDao
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.TripRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("TripDetailViewModel")
class TripDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeTripDao: DetailFakeTripDao
    private lateinit var tripRepository: TripRepository
    private lateinit var viewModel: TripDetailViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTripDao = DetailFakeTripDao()
        tripRepository = TripRepository(fakeTripDao)
    }

    private suspend fun createViewModel() {
        viewModel = TripDetailViewModel(
            tripRepository = tripRepository,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("load trip")
    inner class LoadTrip {

        @Test
        fun `loads trip with details`() = runTest {
            val trip = testTrip(id = "trip-1")
            fakeTripDao.trips["trip-1"] = trip
            fakeTripDao.updateFlow()

            createViewModel()
            viewModel.loadTrip("trip-1")

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertEquals("trip-1", data.trip.id)
                assertEquals(25.0, data.trip.distanceKm, 0.01)
            }
        }

        @Test
        fun `shows error when trip not found`() = runTest {
            fakeTripDao.updateFlow()

            createViewModel()
            viewModel.loadTrip("nonexistent")

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Error)
                assertTrue((state as UiState.Error).message.contains("Trip not found"))
            }
        }

        @Test
        fun `builds route summary from trip points`() = runTest {
            val trip = testTrip(id = "trip-1")
            fakeTripDao.trips["trip-1"] = trip
            fakeTripDao.tripPointsForTrip["trip-1"] = mutableListOf(
                testTripPoint(id = "tp-1", tripId = "trip-1", lat = 37.7749, lng = -122.4194),
                testTripPoint(id = "tp-2", tripId = "trip-1", lat = 37.7849, lng = -122.4094),
            )
            fakeTripDao.updateFlow()

            createViewModel()
            viewModel.loadTrip("trip-1")

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertNotNull(data.routeSummary)
                assertEquals(37.7749, data.routeSummary!!.startLat, 0.0001)
                assertEquals(-122.4194, data.routeSummary!!.startLng, 0.0001)
                assertEquals(37.7849, data.routeSummary!!.endLat, 0.0001)
                assertEquals(-122.4094, data.routeSummary!!.endLng, 0.0001)
            }
        }

        @Test
        fun `returns null route summary when fewer than 2 points`() = runTest {
            val trip = testTrip(id = "trip-1")
            fakeTripDao.trips["trip-1"] = trip
            fakeTripDao.tripPointsForTrip["trip-1"] = mutableListOf(
                testTripPoint(id = "tp-1", tripId = "trip-1"),
            )
            fakeTripDao.updateFlow()

            createViewModel()
            viewModel.loadTrip("trip-1")

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertNull(data.routeSummary)
            }
        }

        @Test
        fun `returns null route summary when no trip points`() = runTest {
            val trip = testTrip(id = "trip-1")
            fakeTripDao.trips["trip-1"] = trip
            fakeTripDao.updateFlow()

            createViewModel()
            viewModel.loadTrip("trip-1")

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertNull(data.routeSummary)
            }
        }
    }

    @Nested
    @DisplayName("generateShareText")
    inner class GenerateShareText {

        @Test
        fun `generates share text for trip with points`() = runTest {
            val trip = testTrip(id = "trip-1")
            fakeTripDao.trips["trip-1"] = trip
            fakeTripDao.tripPointsForTrip["trip-1"] = mutableListOf(
                testTripPoint(id = "tp-1", tripId = "trip-1", lat = 37.7749, lng = -122.4194),
                testTripPoint(id = "tp-2", tripId = "trip-1", lat = 34.0522, lng = -118.2437),
            )
            fakeTripDao.updateFlow()

            createViewModel()
            viewModel.loadTrip("trip-1")

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val shareText = viewModel.generateShareText((state as UiState.Success).data)
                assertTrue(shareText != null)
                assertTrue(shareText!!.startsWith("My trip on"))
                assertTrue(shareText.contains("25.0 km"))
                assertTrue(shareText.contains("https://www.openstreetmap.org/directions?route="))
            }
        }

        @Test
        fun `returns null when fewer than 2 points`() = runTest {
            val trip = testTrip(id = "trip-1")
            fakeTripDao.trips["trip-1"] = trip
            fakeTripDao.tripPointsForTrip["trip-1"] = mutableListOf(
                testTripPoint(id = "tp-1", tripId = "trip-1"),
            )
            fakeTripDao.updateFlow()

            createViewModel()
            viewModel.loadTrip("trip-1")

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val shareText = viewModel.generateShareText((state as UiState.Success).data)
                assertNull(shareText)
            }
        }

        @Test
        fun `share text includes sampled waypoints in URL`() = runTest {
            val points = (0..99).map { i ->
                testTripPoint(id = "tp-$i", tripId = "trip-1", lat = 37.0 + i * 0.001, lng = -122.0 + i * 0.001)
            }
            val trip = testTrip(id = "trip-1")
            fakeTripDao.trips["trip-1"] = trip
            fakeTripDao.tripPointsForTrip["trip-1"] = points.toMutableList()
            fakeTripDao.updateFlow()

            createViewModel()
            viewModel.loadTrip("trip-1")

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val shareText = viewModel.generateShareText((state as UiState.Success).data)
                assertTrue(shareText != null)
                val urlPart = shareText!!.substringAfterLast("— ")
                val coords = urlPart.substringAfter("route=").split(";")
                assertTrue(coords.size <= 25)
            }
        }
    }

    private fun testTrip(
        id: String = "trip-1",
        vehicleId: String = "veh-1",
        startTime: Long = System.currentTimeMillis() - 3600000,
    ) = Trip(
        id = id,
        vehicleId = vehicleId,
        startTime = startTime,
        endTime = startTime + 1800000,
        distanceKm = 25.0,
        durationMs = 1800000,
        maxSpeedKmh = 80.0,
        avgSpeedKmh = 50.0,
        estimatedFuelL = 2.0,
        startOdometerKm = 89975.0,
        endOdometerKm = 90000.0,
        status = TripStatus.COMPLETED,
    )

    private fun testTripPoint(
        id: String = "tp-1",
        tripId: String = "trip-1",
        lat: Double = 37.7749,
        lng: Double = -122.4194,
    ) = TripPoint(
        id = id,
        tripId = tripId,
        latitude = lat,
        longitude = lng,
        speedKmh = 50.0,
        altitude = 10.0,
        accuracy = 5f,
        timestamp = System.currentTimeMillis(),
    )
}

private class DetailFakeTripDao : TripDao() {
    val trips = mutableMapOf<String, Trip>()
    val tripPointsForTrip = mutableMapOf<String, MutableList<TripPoint>>()
    private val tripFlow = MutableStateFlow<List<Trip>>(emptyList())
    private val tripPointFlow = MutableStateFlow<Map<String, List<TripPoint>>>(emptyMap())

    fun updateFlow() {
        tripFlow.value = trips.values.toList()
        tripPointFlow.value = tripPointsForTrip.toMap()
    }

    override fun getTripsForVehicle(vehicleId: String): Flow<List<Trip>> =
        tripFlow.map { it.filter { t -> t.vehicleId == vehicleId } }

    override fun getTripPointsForTrip(tripId: String): Flow<List<TripPoint>> =
        tripPointFlow.map { it[tripId] ?: emptyList() }

    override fun getActiveTrip(vehicleId: String): Flow<Trip?> =
        tripFlow.map { it.find { t -> t.vehicleId == vehicleId && t.status == TripStatus.ACTIVE } }

    override fun getTrip(tripId: String): Flow<Trip?> =
        tripFlow.map { it.find { t -> t.id == tripId } }

    override suspend fun upsertTrip(trip: Trip) { trips[trip.id] = trip; updateFlow() }
    override suspend fun upsertTrips(trips: List<Trip>) { trips.forEach { this.trips[it.id] = it }; updateFlow() }
    override suspend fun deleteTrip(trip: Trip) { trips.remove(trip.id); updateFlow() }
    override suspend fun deleteTripById(tripId: String) { trips.remove(tripId); updateFlow() }
    override suspend fun upsertTripPoint(tripPoint: TripPoint) {}
    override suspend fun upsertTripPoints(tripPoints: List<TripPoint>) {}
    override suspend fun deleteTripPoint(tripPoint: TripPoint) {}
    override suspend fun getTripsModifiedSince(since: Long): List<Trip> = emptyList()
    override suspend fun getTripPointsModifiedSince(since: Long): List<TripPoint> = emptyList()
    override suspend fun getTripById(id: String): Trip? = trips[id]
    override suspend fun getTripPointById(id: String): TripPoint? = null
}
