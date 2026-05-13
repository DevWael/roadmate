package com.roadmate.phone.ui.hub

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import com.roadmate.core.database.dao.DocumentDao
import com.roadmate.core.database.dao.FuelDao
import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.dao.TripDao
import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.Document
import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelLog
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.FuelRepository
import com.roadmate.core.repository.MaintenanceRepository
import com.roadmate.core.repository.TripRepository
import com.roadmate.core.repository.VehicleRepository
import com.roadmate.core.sync.SyncTimestampStore
import com.roadmate.core.sync.SyncTriggerManager
import com.roadmate.core.util.AttentionLevel
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
@DisplayName("VehicleHubViewModel")
class VehicleHubViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeVehicleDao: HubFakeVehicleDao
    private lateinit var fakeMaintenanceDao: HubFakeMaintenanceDao
    private lateinit var fakeTripDao: HubFakeTripDao
    private lateinit var fakeFuelDao: HubFakeFuelDao
    private lateinit var fakeDocumentDao: HubFakeDocumentDao
    private lateinit var fakeDataStore: FakePreferencesDataStore
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var activeVehicleRepository: ActiveVehicleRepository
    private lateinit var maintenanceRepository: MaintenanceRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var fuelRepository: FuelRepository
    private lateinit var syncTimestampStore: SyncTimestampStore
    private lateinit var viewModel: VehicleHubViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeVehicleDao = HubFakeVehicleDao()
        fakeMaintenanceDao = HubFakeMaintenanceDao()
        fakeTripDao = HubFakeTripDao()
        fakeFuelDao = HubFakeFuelDao()
        fakeDocumentDao = HubFakeDocumentDao()
        fakeDataStore = FakePreferencesDataStore()
        vehicleRepository = VehicleRepository(fakeVehicleDao)
        activeVehicleRepository = ActiveVehicleRepository(fakeDataStore)
        maintenanceRepository = MaintenanceRepository(fakeMaintenanceDao)
        tripRepository = TripRepository(fakeTripDao)
        fuelRepository = FuelRepository(fakeFuelDao)
        syncTimestampStore = SyncTimestampStore(fakeDataStore)
    }

    private suspend fun createViewModel() {
        val btStateManager = com.roadmate.core.state.BluetoothStateManager()
        val deltaEngine = com.roadmate.core.sync.DeltaSyncEngine(
            fakeVehicleDao, fakeTripDao, fakeMaintenanceDao, fakeFuelDao,
            fakeDocumentDao, com.roadmate.core.sync.SyncBatcher(),
        )
        val syncSession = com.roadmate.core.sync.SyncSession(
            btStateManager,
            deltaEngine,
            com.roadmate.core.sync.SyncBatcher(),
            com.roadmate.core.sync.AckTracker(),
            com.roadmate.core.sync.protocol.MessageSerializer(),
            com.roadmate.core.sync.UnackedMessageTracker(),
            syncTimestampStore,
            com.roadmate.core.util.Clock.SYSTEM,
        )
        val syncTriggerManager = SyncTriggerManager(btStateManager, syncSession, deltaEngine)

        viewModel = VehicleHubViewModel(
            vehicleRepository = vehicleRepository,
            activeVehicleRepository = activeVehicleRepository,
            maintenanceRepository = maintenanceRepository,
            tripRepository = tripRepository,
            fuelRepository = fuelRepository,
            syncTriggerManager = syncTriggerManager,
            syncTimestampStore = syncTimestampStore,
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
        fun `loads vehicle and dashboard data`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            val vehicle = testVehicle()
            val schedule = testSchedule()

            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeMaintenanceDao.schedules["sched-1"] = schedule
            fakeVehicleDao.updateFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertEquals("veh-1", data.vehicle.id)
                assertEquals("My Car", data.vehicle.name)
                assertEquals(90000.0, data.vehicle.odometerKm)
            }
        }

        @Test
        fun `shows error when no active vehicle`() = runTest {
            fakeVehicleDao.updateFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Error)
            }
        }

        @Test
        fun `shows error when vehicle not found`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeVehicleDao.updateFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Error)
                assertTrue((state as UiState.Error).message.contains("Vehicle not found"))
            }
        }
    }

    @Nested
    @DisplayName("attention items")
    inner class AttentionItems {

        @Test
        fun `classifies overdue maintenance as attention`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            val vehicle = testVehicle(odometerKm = 95000.0)
            val schedule = testSchedule(lastServiceKm = 80000.0, intervalKm = 10000)

            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeMaintenanceDao.schedules["sched-1"] = schedule
            fakeVehicleDao.updateFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertEquals(1, data.attentionItems.size)
                assertEquals(AttentionLevel.OVERDUE, data.attentionItems[0].level)
                assertEquals("sched-1", data.attentionItems[0].scheduleId)
            }
        }

        @Test
        fun `excludes normal items from attention list`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            val vehicle = testVehicle(odometerKm = 81000.0)
            val schedule = testSchedule(lastServiceKm = 80000.0, intervalKm = 10000)

            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeMaintenanceDao.schedules["sched-1"] = schedule
            fakeVehicleDao.updateFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertTrue(data.attentionItems.isEmpty())
            }
        }
    }

    @Nested
    @DisplayName("maintenance summaries")
    inner class MaintenanceSummaries {

        @Test
        fun `builds summaries sorted by percentage descending`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            val vehicle = testVehicle(odometerKm = 87000.0)
            val schedule1 = testSchedule(id = "s1", lastServiceKm = 82000.0, intervalKm = 10000)
            val schedule2 = testSchedule(id = "s2", lastServiceKm = 80000.0, intervalKm = 50000)

            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeMaintenanceDao.schedules["s1"] = schedule1
            fakeMaintenanceDao.schedules["s2"] = schedule2
            fakeVehicleDao.updateFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertEquals(2, data.maintenanceSummaries.size)
                assertTrue(data.maintenanceSummaries[0].percentage > data.maintenanceSummaries[1].percentage)
            }
        }

        @Test
        fun `limits summaries to top 3`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            val vehicle = testVehicle(odometerKm = 87000.0)
            for (i in 1..5) {
                fakeMaintenanceDao.schedules["s$i"] = testSchedule(
                    id = "s$i",
                    lastServiceKm = 80000.0 + (i * 1000), intervalKm = 10000,
                )
            }
            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeVehicleDao.updateFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertEquals(3, data.maintenanceSummaries.size)
            }
        }

        @Test
        fun `calculates percentage correctly`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            val vehicle = testVehicle(odometerKm = 85000.0)
            val schedule = testSchedule(lastServiceKm = 80000.0, intervalKm = 10000)

            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeMaintenanceDao.schedules["sched-1"] = schedule
            fakeVehicleDao.updateFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertEquals(1, data.maintenanceSummaries.size)
                assertEquals(50f, data.maintenanceSummaries[0].percentage, 0.1f)
                assertEquals(5000.0, data.maintenanceSummaries[0].remainingKm, 0.01)
            }
        }
    }

    @Nested
    @DisplayName("recent trips")
    inner class RecentTrips {

        @Test
        fun `shows last 3 completed trips sorted by start time`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            val vehicle = testVehicle()
            for (i in 1..5) {
                fakeTripDao.trips["trip-$i"] = testTrip(
                    id = "trip-$i",
                    startTime = 1000L * i,
                    status = TripStatus.COMPLETED,
                )
            }
            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeVehicleDao.updateFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertEquals(3, data.recentTrips.size)
                assertEquals("trip-5", data.recentTrips[0].id)
                assertEquals("trip-4", data.recentTrips[1].id)
                assertEquals("trip-3", data.recentTrips[2].id)
            }
        }

        @Test
        fun `excludes active trips from recent list`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            val vehicle = testVehicle()
            fakeTripDao.trips["trip-1"] = testTrip(id = "trip-1", status = TripStatus.ACTIVE)
            fakeTripDao.trips["trip-2"] = testTrip(id = "trip-2", status = TripStatus.COMPLETED)

            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeVehicleDao.updateFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertEquals(1, data.recentTrips.size)
                assertEquals("trip-2", data.recentTrips[0].id)
            }
        }
    }

    @Nested
    @DisplayName("fuel summary")
    inner class FuelSummary {

        @Test
        fun `computes this months fuel summary`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            val vehicle = testVehicle()
            val now = System.currentTimeMillis()
            fakeFuelDao.fuelLogs["f1"] = testFuelLog(id = "f1", date = now, liters = 40.0, totalCost = 60.0)
            fakeFuelDao.fuelLogs["f2"] = testFuelLog(id = "f2", date = now, liters = 35.0, totalCost = 50.0)

            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeVehicleDao.updateFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertNotNull(data.fuelSummary)
                assertEquals(75.0, data.fuelSummary!!.totalLiters, 0.01)
                assertEquals(110.0, data.fuelSummary!!.totalCost, 0.01)
                assertEquals(2, data.fuelSummary!!.entryCount)
            }
        }

        @Test
        fun `returns null fuel summary when no fuel logs this month`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            val vehicle = testVehicle()
            val lastMonth = System.currentTimeMillis() - 45L * 24 * 60 * 60 * 1000
            fakeFuelDao.fuelLogs["f1"] = testFuelLog(id = "f1", date = lastMonth, liters = 40.0, totalCost = 60.0)

            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeVehicleDao.updateFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertNull(data.fuelSummary)
            }
        }
    }

    private fun testVehicle(
        id: String = "veh-1",
        odometerKm: Double = 90000.0,
    ) = Vehicle(
        id = id,
        name = "My Car",
        make = "Toyota",
        model = "Corolla",
        year = 2023,
        engineType = EngineType.INLINE_4,
        engineSize = 2.0,
        fuelType = FuelType.GASOLINE,
        plateNumber = "ABC-123",
        odometerKm = odometerKm,
        odometerUnit = OdometerUnit.KM,
        cityConsumption = 8.0,
        highwayConsumption = 6.0,
    )

    private fun testSchedule(
        id: String = "sched-1",
        vehicleId: String = "veh-1",
        lastServiceKm: Double = 80000.0,
        intervalKm: Int? = 10000,
    ) = MaintenanceSchedule(
        id = id,
        vehicleId = vehicleId,
        name = "Oil Change",
        intervalKm = intervalKm,
        intervalMonths = 6,
        lastServiceKm = lastServiceKm,
        lastServiceDate = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000,
        isCustom = false,
    )

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

    private fun testFuelLog(
        id: String = "f1",
        vehicleId: String = "veh-1",
        date: Long = System.currentTimeMillis(),
        liters: Double = 40.0,
        totalCost: Double = 60.0,
    ) = FuelLog(
        id = id,
        vehicleId = vehicleId,
        date = date,
        odometerKm = 90000.0,
        liters = liters,
        pricePerLiter = 1.5,
        totalCost = totalCost,
        isFullTank = true,
    )
}

private class FakePreferencesDataStore : androidx.datastore.core.DataStore<Preferences> {
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

private class HubFakeDocumentDao : DocumentDao {
    override fun getDocumentsForVehicle(vehicleId: String): Flow<List<Document>> = MutableStateFlow(emptyList())
    override fun getExpiringDocuments(vehicleId: String, threshold: Long): Flow<List<Document>> = MutableStateFlow(emptyList())
    override fun getDocument(documentId: String): Flow<Document?> = MutableStateFlow(null)
    override suspend fun upsertDocument(document: Document) {}
    override suspend fun upsertDocuments(documents: List<Document>) {}
    override suspend fun deleteDocument(document: Document) {}
    override suspend fun deleteDocumentById(documentId: String) {}
    override suspend fun getDocumentsModifiedSince(since: Long): List<Document> = emptyList()
    override suspend fun getDocumentById(id: String): Document? = null
}

private class HubFakeVehicleDao : VehicleDao {
    val vehicles = mutableMapOf<String, Vehicle>()
    private val vehicleFlow = MutableStateFlow<List<Vehicle>>(emptyList())
    fun updateFlow() { vehicleFlow.value = vehicles.values.toList() }
    override fun getVehicle(vehicleId: String): Flow<Vehicle?> = vehicleFlow.map { it.find { v -> v.id == vehicleId } }
    override fun getAllVehicles(): Flow<List<Vehicle>> = vehicleFlow
    override suspend fun upsert(vehicle: Vehicle) { vehicles[vehicle.id] = vehicle; updateFlow() }
    override suspend fun upsertAll(vehicles: List<Vehicle>) { vehicles.forEach { this.vehicles[it.id] = it }; updateFlow() }
    override suspend fun delete(vehicle: Vehicle) { vehicles.remove(vehicle.id); updateFlow() }
    override suspend fun deleteById(vehicleId: String) { vehicles.remove(vehicleId); updateFlow() }
    override fun getVehicleCount(): Flow<Int> = vehicleFlow.map { it.size }
    override suspend fun addToOdometer(vehicleId: String, distanceKm: Double, lastModified: Long) {
        vehicles[vehicleId]?.let { vehicles[vehicleId] = it.copy(odometerKm = it.odometerKm + distanceKm); updateFlow() }
    }
    override suspend fun getModifiedSince(since: Long): List<Vehicle> = vehicles.values.filter { it.lastModified > since }
    override suspend fun getVehicleById(id: String): Vehicle? = vehicles[id]
}

private class HubFakeMaintenanceDao : MaintenanceDao() {
    val schedules = mutableMapOf<String, MaintenanceSchedule>()
    private val scheduleFlow = MutableStateFlow<List<MaintenanceSchedule>>(emptyList())
    fun updateScheduleFlow() { scheduleFlow.value = schedules.values.toList() }
    override fun getSchedulesForVehicle(vehicleId: String): Flow<List<MaintenanceSchedule>> =
        scheduleFlow.map { list -> list.filter { it.vehicleId == vehicleId } }
    override fun getSchedule(scheduleId: String): Flow<MaintenanceSchedule?> =
        scheduleFlow.map { it.find { s -> s.id == scheduleId } }
    override suspend fun upsertSchedule(schedule: MaintenanceSchedule) { schedules[schedule.id] = schedule; updateScheduleFlow() }
    override suspend fun upsertSchedules(schedules: List<MaintenanceSchedule>) { schedules.forEach { this.schedules[it.id] = it }; updateScheduleFlow() }
    override suspend fun deleteSchedule(schedule: MaintenanceSchedule) { schedules.remove(schedule.id); updateScheduleFlow() }
    override suspend fun deleteScheduleById(scheduleId: String) { schedules.remove(scheduleId); updateScheduleFlow() }
    override fun getRecordsForSchedule(scheduleId: String): Flow<List<com.roadmate.core.database.entity.MaintenanceRecord>> = MutableStateFlow(emptyList())
    override fun getRecordsForVehicle(vehicleId: String): Flow<List<com.roadmate.core.database.entity.MaintenanceRecord>> = MutableStateFlow(emptyList())
    override fun getRecord(recordId: String): Flow<com.roadmate.core.database.entity.MaintenanceRecord?> = MutableStateFlow(null)
    override suspend fun upsertRecord(record: com.roadmate.core.database.entity.MaintenanceRecord) {}
    override suspend fun upsertRecords(records: List<com.roadmate.core.database.entity.MaintenanceRecord>) {}
    override suspend fun deleteRecord(record: com.roadmate.core.database.entity.MaintenanceRecord) {}
    override suspend fun deleteRecordById(recordId: String) {}
    override suspend fun deleteRecordsByScheduleId(scheduleId: String) {}
    override suspend fun getSchedulesModifiedSince(since: Long): List<MaintenanceSchedule> = schedules.values.filter { it.lastServiceDate > since }
    override suspend fun getRecordsModifiedSince(since: Long): List<com.roadmate.core.database.entity.MaintenanceRecord> = emptyList()
    override suspend fun getScheduleById(id: String): MaintenanceSchedule? = schedules[id]
    override suspend fun getRecordById(id: String): com.roadmate.core.database.entity.MaintenanceRecord? = null
}

private class HubFakeTripDao : TripDao() {
    val trips = mutableMapOf<String, Trip>()
    private val tripFlow = MutableStateFlow<List<Trip>>(emptyList())
    fun updateFlow() { tripFlow.value = trips.values.toList() }
    override fun getTripsForVehicle(vehicleId: String): Flow<List<Trip>> = tripFlow.map { it.filter { t -> t.vehicleId == vehicleId } }
    override fun getTripPointsForTrip(tripId: String): Flow<List<TripPoint>> = MutableStateFlow(emptyList())
    override fun getActiveTrip(vehicleId: String): Flow<Trip?> = tripFlow.map { it.find { t -> t.vehicleId == vehicleId && t.status == TripStatus.ACTIVE } }
    override fun getTrip(tripId: String): Flow<Trip?> = tripFlow.map { it.find { t -> t.id == tripId } }
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

private class HubFakeFuelDao : FuelDao {
    val fuelLogs = mutableMapOf<String, FuelLog>()
    private val fuelFlow = MutableStateFlow<List<FuelLog>>(emptyList())
    fun updateFlow() { fuelFlow.value = fuelLogs.values.toList() }
    override fun getFuelLogsForVehicle(vehicleId: String): Flow<List<FuelLog>> = fuelFlow.map { it.filter { f -> f.vehicleId == vehicleId } }
    override fun getLastFullTankEntry(vehicleId: String): Flow<FuelLog?> = fuelFlow.map { it.filter { f -> f.vehicleId == vehicleId && f.isFullTank }.maxByOrNull { f -> f.date } }
    override fun getLatestFuelEntry(vehicleId: String): Flow<FuelLog?> = fuelFlow.map { it.filter { f -> f.vehicleId == vehicleId }.maxByOrNull { f -> f.date } }
    override fun getTwoLastFullTankEntries(vehicleId: String): Flow<List<FuelLog>> = fuelFlow.map { it.filter { f -> f.vehicleId == vehicleId && f.isFullTank }.sortedByDescending { f -> f.date }.take(2) }
    override fun getFuelLog(fuelLogId: String): Flow<FuelLog?> = fuelFlow.map { it.find { f -> f.id == fuelLogId } }
    override suspend fun upsertFuelLog(fuelLog: FuelLog) { fuelLogs[fuelLog.id] = fuelLog; updateFlow() }
    override suspend fun upsertFuelLogs(fuelLogs: List<FuelLog>) { fuelLogs.forEach { this.fuelLogs[it.id] = it }; updateFlow() }
    override suspend fun deleteFuelLog(fuelLog: FuelLog) { fuelLogs.remove(fuelLog.id); updateFlow() }
    override suspend fun deleteFuelLogById(fuelLogId: String) { fuelLogs.remove(fuelLogId); updateFlow() }
    override suspend fun getFuelLogsModifiedSince(since: Long): List<FuelLog> = emptyList()
    override suspend fun getFuelLogById(id: String): FuelLog? = fuelLogs[id]
}
