package com.roadmate.phone.ui.trips

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import com.roadmate.core.database.dao.TripDao
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.ActiveVehicleRepository
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("TripListViewModel")
class TripListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeTripDao: ListFakeTripDao
    private lateinit var fakeDataStore: ListFakePreferencesDataStore
    private lateinit var tripRepository: TripRepository
    private lateinit var activeVehicleRepository: ActiveVehicleRepository
    private lateinit var viewModel: TripListViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTripDao = ListFakeTripDao()
        fakeDataStore = ListFakePreferencesDataStore()
        tripRepository = TripRepository(fakeTripDao)
        activeVehicleRepository = ActiveVehicleRepository(fakeDataStore)
    }

    private suspend fun createViewModel() {
        viewModel = TripListViewModel(
            activeVehicleRepository = activeVehicleRepository,
            tripRepository = tripRepository,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("initial load")
    inner class InitialLoad {

        @Test
        fun `loads trips for active vehicle`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeTripDao.trips["trip-1"] = testTrip(id = "trip-1", vehicleId = "veh-1")
            fakeTripDao.trips["trip-2"] = testTrip(id = "trip-2", vehicleId = "veh-1")
            fakeTripDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val trips = (state as UiState.Success).data
                assertEquals(2, trips.size)
            }
        }

        @Test
        fun `shows error when no active vehicle`() = runTest {
            fakeTripDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Error)
                assertTrue((state as UiState.Error).message.contains("No active vehicle"))
            }
        }

        @Test
        fun `returns empty list when no trips exist`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeTripDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val trips = (state as UiState.Success).data
                assertTrue(trips.isEmpty())
            }
        }
    }

    @Nested
    @DisplayName("trip filtering")
    inner class TripFiltering {

        @Test
        fun `only returns trips for active vehicle`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeTripDao.trips["trip-1"] = testTrip(id = "trip-1", vehicleId = "veh-1")
            fakeTripDao.trips["trip-2"] = testTrip(id = "trip-2", vehicleId = "veh-2")
            fakeTripDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val trips = (state as UiState.Success).data
                assertEquals(1, trips.size)
                assertEquals("trip-1", trips[0].id)
            }
        }

        @Test
        fun `includes all trip statuses in list`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeTripDao.trips["trip-1"] = testTrip(id = "trip-1", status = TripStatus.COMPLETED)
            fakeTripDao.trips["trip-2"] = testTrip(id = "trip-2", status = TripStatus.INTERRUPTED)
            fakeTripDao.trips["trip-3"] = testTrip(id = "trip-3", status = TripStatus.ACTIVE)
            fakeTripDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val trips = (state as UiState.Success).data
                assertEquals(3, trips.size)
            }
        }
    }

    private fun testTrip(
        id: String = "trip-1",
        vehicleId: String = "veh-1",
        startTime: Long = System.currentTimeMillis() - 3600000,
        status: TripStatus = TripStatus.COMPLETED,
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
        status = status,
    )
}

private class ListFakePreferencesDataStore : androidx.datastore.core.DataStore<Preferences> {
    private val _data = MutableStateFlow<Preferences>(emptyPreferences())
    override val data: Flow<Preferences> = _data

    override suspend fun updateData(
        transformer: suspend (Preferences) -> Preferences,
    ): Preferences {
        val newValue = transformer(_data.value)
        _data.value = newValue
        return newValue
    }
}

private class ListFakeTripDao : TripDao() {
    val trips = mutableMapOf<String, Trip>()
    private val tripPoints = mutableMapOf<String, MutableList<TripPoint>>()
    private val tripFlow = MutableStateFlow<List<Trip>>(emptyList())
    private val tripPointFlow = MutableStateFlow<Map<String, List<TripPoint>>>(emptyMap())

    fun updateFlow() {
        tripFlow.value = trips.values.toList()
        tripPointFlow.value = tripPoints.toMap()
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
