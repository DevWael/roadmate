package com.roadmate.headunit

import app.cash.turbine.test
import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.dao.TripDao
import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.MaintenanceRepository
import com.roadmate.core.repository.TripRepository
import com.roadmate.core.repository.VehicleRepository
import com.roadmate.core.state.DrivingStateManager
import com.roadmate.core.state.LocationStateManager
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("MainViewModel")
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeVehicleDao: FakeMainVehicleDao
    private lateinit var fakeTripDao: FakeMainTripDao
    private lateinit var fakeDataStore: FakeMainDataStore
    private lateinit var fakeMaintenanceDao: FakeMainMaintenanceDao
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var activeVehicleRepository: ActiveVehicleRepository
    private lateinit var maintenanceRepository: MaintenanceRepository
    private lateinit var drivingStateManager: DrivingStateManager
    private lateinit var locationStateManager: LocationStateManager

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeVehicleDao = FakeMainVehicleDao()
        fakeTripDao = FakeMainTripDao()
        fakeDataStore = FakeMainDataStore()
        fakeMaintenanceDao = FakeMainMaintenanceDao()
        vehicleRepository = VehicleRepository(fakeVehicleDao)
        tripRepository = TripRepository(fakeTripDao)
        activeVehicleRepository = ActiveVehicleRepository(fakeDataStore)
        maintenanceRepository = MaintenanceRepository(fakeMaintenanceDao)
        drivingStateManager = DrivingStateManager()
        locationStateManager = LocationStateManager()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MainViewModel = MainViewModel(
        activeVehicleRepository = activeVehicleRepository,
        vehicleRepository = vehicleRepository,
        tripRepository = tripRepository,
        maintenanceRepository = maintenanceRepository,
        drivingStateManager = drivingStateManager,
        locationStateManager = locationStateManager,
    )

    private fun testVehicle(
        id: String = "v-1",
        odometerKm: Double = 85000.0,
    ) = Vehicle(
        id = id,
        name = "Test Car",
        make = "Toyota",
        model = "Camry",
        year = 2020,
        engineType = EngineType.INLINE_4,
        engineSize = 2.5,
        fuelType = FuelType.GASOLINE,
        plateNumber = "ABC123",
        odometerKm = odometerKm,
        odometerUnit = OdometerUnit.KM,
        cityConsumption = 8.0,
        highwayConsumption = 6.0,
    )

    private fun testTrip(
        id: String = "t-1",
        vehicleId: String = "v-1",
        status: TripStatus = TripStatus.COMPLETED,
        distanceKm: Double = 15.0,
    ) = Trip(
        id = id,
        vehicleId = vehicleId,
        startTime = System.currentTimeMillis(),
        distanceKm = distanceKm,
        durationMs = 1800_000L,
        maxSpeedKmh = 80.0,
        avgSpeedKmh = 50.0,
        estimatedFuelL = 1.2,
        startOdometerKm = 85000.0,
        endOdometerKm = 85015.0,
        status = status,
    )

    @Nested
    @DisplayName("currentVehicle Flow")
    inner class CurrentVehicleFlow {

        @Test
        @DisplayName("emits vehicle when active vehicle ID is set")
        fun emitsVehicle() = runTest {
            val vehicle = testVehicle()
            fakeVehicleDao.vehicles["v-1"] = vehicle
            fakeVehicleDao.updateFlow()
            activeVehicleRepository.setActiveVehicle("v-1")

            val vm = createViewModel()

            vm.currentVehicle.test {
                val emitted = awaitItem()
                assertNotNull(emitted)
                assertEquals("v-1", emitted!!.id)
                assertEquals(85000.0, emitted.odometerKm, 0.001)
            }
        }

        @Test
        @DisplayName("reactively updates when vehicle odometer changes")
        fun reactivelyUpdates() = runTest {
            val vehicle = testVehicle()
            fakeVehicleDao.vehicles["v-1"] = vehicle
            fakeVehicleDao.updateFlow()
            activeVehicleRepository.setActiveVehicle("v-1")

            val vm = createViewModel()

            vm.currentVehicle.test {
                val first = awaitItem()
                assertNotNull(first)
                assertEquals(85000.0, first!!.odometerKm, 0.001)

                fakeVehicleDao.vehicles["v-1"] = vehicle.copy(odometerKm = 85015.0)
                fakeVehicleDao.updateFlow()

                val updated = awaitItem()
                assertNotNull(updated)
                assertEquals(85015.0, updated!!.odometerKm, 0.001)
            }
        }
    }

    @Nested
    @DisplayName("trips Flow")
    inner class TripsFlow {

        @Test
        @DisplayName("emits empty list when no active vehicle")
        fun emitsEmptyWhenNoVehicle() = runTest {
            val vm = createViewModel()

            vm.trips.test {
                assertEquals(emptyList<Trip>(), awaitItem())
            }
        }

        @Test
        @DisplayName("emits trips for active vehicle")
        fun emitsTripsForVehicle() = runTest {
            val vehicle = testVehicle()
            fakeVehicleDao.vehicles["v-1"] = vehicle
            fakeVehicleDao.updateFlow()

            val trip = testTrip()
            fakeTripDao.trips["t-1"] = trip
            fakeTripDao.updateFlow()

            activeVehicleRepository.setActiveVehicle("v-1")

            val vm = createViewModel()

            vm.trips.test {
                val emitted = awaitItem()
                assertEquals(1, emitted.size)
                assertEquals("t-1", emitted[0].id)
            }
        }

        @Test
        @DisplayName("reactively updates when new trip is added")
        fun reactivelyUpdatesOnNewTrip() = runTest {
            val vehicle = testVehicle()
            fakeVehicleDao.vehicles["v-1"] = vehicle
            fakeVehicleDao.updateFlow()

            val trip = testTrip()
            fakeTripDao.trips["t-1"] = trip
            fakeTripDao.updateFlow()

            activeVehicleRepository.setActiveVehicle("v-1")

            val vm = createViewModel()

            vm.trips.test {
                assertEquals(1, awaitItem().size)

                val newTrip = testTrip(id = "t-2", distanceKm = 20.0)
                fakeTripDao.trips["t-2"] = newTrip
                fakeTripDao.updateFlow()

                val updated = awaitItem()
                assertEquals(2, updated.size)
            }
        }
    }
    @Nested
    @DisplayName("maintenanceAlertMessage Flow")
    inner class MaintenanceAlertMessageFlow {

        @Test
        @DisplayName("is null when no active vehicle")
        fun nullWhenNoVehicle() = runTest {
            val vm = createViewModel()

            vm.maintenanceAlertMessage.test {
                assertNull(awaitItem())
            }
        }

        @Test
        @DisplayName("is null when no schedules are overdue")
        fun nullWhenNoOverdue() = runTest {
            val vehicle = testVehicle()
            fakeVehicleDao.vehicles["v-1"] = vehicle
            fakeVehicleDao.updateFlow()
            activeVehicleRepository.setActiveVehicle("v-1")

            val schedule = MaintenanceSchedule(
                id = "s1",
                vehicleId = "v-1",
                name = "Oil Change",
                intervalKm = 10000,
                lastServiceKm = 80000.0,
                lastServiceDate = 0L,
                isCustom = false,
            )
            fakeMaintenanceDao.schedules["s1"] = schedule
            fakeMaintenanceDao.updateFlow()

            val vm = createViewModel()

            vm.maintenanceAlertMessage.test {
                assertNull(awaitItem())
            }
        }

        @Test
        @DisplayName("shows alert when schedule at 95 percent or more")
        fun alertWhenOverdue() = runTest {
            val vehicle = testVehicle(odometerKm = 89500.0)
            fakeVehicleDao.vehicles["v-1"] = vehicle
            fakeVehicleDao.updateFlow()
            activeVehicleRepository.setActiveVehicle("v-1")

            val schedule = MaintenanceSchedule(
                id = "s1",
                vehicleId = "v-1",
                name = "Oil Change",
                intervalKm = 10000,
                lastServiceKm = 80000.0,
                lastServiceDate = 0L,
                isCustom = false,
            )
            fakeMaintenanceDao.schedules["s1"] = schedule
            fakeMaintenanceDao.updateFlow()

            val vm = createViewModel()

            vm.maintenanceAlertMessage.test {
                val message = awaitItem()
                assertNotNull(message)
                assertEquals("Oil Change", message)
            }
        }
    }
}

private class FakeMainMaintenanceDao : MaintenanceDao {
    val schedules = mutableMapOf<String, MaintenanceSchedule>()
    private val scheduleFlow = MutableStateFlow<List<MaintenanceSchedule>>(emptyList())

    fun updateFlow() {
        scheduleFlow.value = schedules.values.toList()
    }

    override fun getSchedulesForVehicle(vehicleId: String): Flow<List<MaintenanceSchedule>> =
        scheduleFlow.map { list -> list.filter { it.vehicleId == vehicleId } }

    override fun getSchedule(scheduleId: String): Flow<MaintenanceSchedule?> =
        scheduleFlow.map { list -> list.find { it.id == scheduleId } }

    override suspend fun upsertSchedule(schedule: MaintenanceSchedule) {
        schedules[schedule.id] = schedule
        updateFlow()
    }

    override suspend fun upsertSchedules(schedules: List<MaintenanceSchedule>) {
        schedules.forEach { this.schedules[it.id] = it }
        updateFlow()
    }

    override suspend fun deleteSchedule(schedule: MaintenanceSchedule) {
        schedules.remove(schedule.id)
        updateFlow()
    }

    override suspend fun deleteScheduleById(scheduleId: String) {
        schedules.remove(scheduleId)
        updateFlow()
    }

    override fun getRecordsForSchedule(scheduleId: String): Flow<List<MaintenanceRecord>> =
        scheduleFlow.map { emptyList() }

    override fun getRecordsForVehicle(vehicleId: String): Flow<List<MaintenanceRecord>> =
        scheduleFlow.map { emptyList() }

    override fun getRecord(recordId: String): Flow<MaintenanceRecord?> =
        scheduleFlow.map { null }

    override suspend fun upsertRecord(record: MaintenanceRecord) {}

    override suspend fun upsertRecords(records: List<MaintenanceRecord>) {}

    override suspend fun deleteRecord(record: MaintenanceRecord) {}

    override suspend fun deleteRecordById(recordId: String) {}
}

private class FakeMainVehicleDao : VehicleDao {
    val vehicles = mutableMapOf<String, Vehicle>()
    private val flow = MutableStateFlow<List<Vehicle>>(emptyList())

    fun updateFlow() {
        flow.value = vehicles.values.toList()
    }

    override fun getVehicle(vehicleId: String): Flow<Vehicle?> =
        flow.map { list -> list.find { it.id == vehicleId } }

    override fun getAllVehicles(): Flow<List<Vehicle>> =
        flow.map { list -> list.sortedByDescending { it.lastModified } }

    override suspend fun upsert(vehicle: Vehicle) {
        vehicles[vehicle.id] = vehicle
        updateFlow()
    }

    override suspend fun upsertAll(vehicles: List<Vehicle>) {
        vehicles.forEach { this.vehicles[it.id] = it }
        updateFlow()
    }

    override suspend fun delete(vehicle: Vehicle) {
        vehicles.remove(vehicle.id)
        updateFlow()
    }

    override suspend fun deleteById(vehicleId: String) {
        vehicles.remove(vehicleId)
        updateFlow()
    }

    override fun getVehicleCount(): Flow<Int> = flow.map { it.size }

    override suspend fun addToOdometer(vehicleId: String, distanceKm: Double, lastModified: Long) {
        val vehicle = vehicles[vehicleId] ?: return
        vehicles[vehicleId] = vehicle.copy(
            odometerKm = vehicle.odometerKm + distanceKm,
            lastModified = lastModified,
        )
        updateFlow()
    }
}

private class FakeMainTripDao : TripDao() {
    val trips = mutableMapOf<String, Trip>()
    private val tripFlow = MutableStateFlow<List<Trip>>(emptyList())

    fun updateFlow() {
        tripFlow.value = trips.values.toList()
    }

    override fun getTripsForVehicle(vehicleId: String): Flow<List<Trip>> =
        tripFlow.map { list -> list.filter { it.vehicleId == vehicleId }.sortedByDescending { it.startTime } }

    override fun getTripPointsForTrip(tripId: String): Flow<List<TripPoint>> =
        tripFlow.map { emptyList() }

    override fun getActiveTrip(vehicleId: String): Flow<Trip?> =
        tripFlow.map { list -> list.find { it.vehicleId == vehicleId && it.status == TripStatus.ACTIVE } }

    override fun getTrip(tripId: String): Flow<Trip?> =
        tripFlow.map { list -> list.find { it.id == tripId } }

    override suspend fun upsertTrip(trip: Trip) {
        trips[trip.id] = trip
        updateFlow()
    }

    override suspend fun upsertTrips(trips: List<Trip>) {
        trips.forEach { this.trips[it.id] = it }
        updateFlow()
    }

    override suspend fun deleteTrip(trip: Trip) {
        trips.remove(trip.id)
        updateFlow()
    }

    override suspend fun deleteTripById(tripId: String) {
        trips.remove(tripId)
        updateFlow()
    }

    override suspend fun upsertTripPoint(tripPoint: TripPoint) {}

    override suspend fun upsertTripPoints(tripPoints: List<TripPoint>) {}

    override suspend fun deleteTripPoint(tripPoint: TripPoint) {}
}

private class FakeMainDataStore : DataStore<Preferences> {
    private val prefs = MutableStateFlow<Preferences>(emptyPreferences())
    override val data = prefs
    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences {
        val new = transform(prefs.value)
        prefs.value = new
        return new
    }
}
