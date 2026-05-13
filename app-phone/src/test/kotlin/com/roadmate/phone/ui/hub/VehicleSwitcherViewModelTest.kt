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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
@DisplayName("VehicleSwitcher - VehicleHubViewModel")
class VehicleSwitcherViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeVehicleDao: SwitcherFakeVehicleDao
    private lateinit var fakeMaintenanceDao: SwitcherFakeMaintenanceDao
    private lateinit var fakeTripDao: SwitcherFakeTripDao
    private lateinit var fakeFuelDao: SwitcherFakeFuelDao
    private lateinit var fakeDocumentDao: SwitcherFakeDocumentDao
    private lateinit var fakeDataStore: SwitcherFakePreferencesDataStore
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
        fakeVehicleDao = SwitcherFakeVehicleDao()
        fakeMaintenanceDao = SwitcherFakeMaintenanceDao()
        fakeTripDao = SwitcherFakeTripDao()
        fakeFuelDao = SwitcherFakeFuelDao()
        fakeDocumentDao = SwitcherFakeDocumentDao()
        fakeDataStore = SwitcherFakePreferencesDataStore()
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

    private fun seedVehicle(
        id: String = "veh-1",
        name: String = "My Car",
        odometerKm: Double = 90000.0,
    ) = Vehicle(
        id = id,
        name = name,
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

    private fun refreshAllFlows() {
        fakeVehicleDao.updateFlow()
        fakeMaintenanceDao.updateScheduleFlow()
        fakeTripDao.updateFlow()
        fakeFuelDao.updateFlow()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("allVehicles")
    inner class AllVehicles {

        @Test
        fun `exposes all vehicles list`() = runTest {
            val v1 = seedVehicle("v1", "Car A")
            val v2 = seedVehicle("v2", "Car B")
            fakeVehicleDao.vehicles["v1"] = v1
            fakeVehicleDao.vehicles["v2"] = v2
            refreshAllFlows()

            createViewModel()

            viewModel.allVehicles.test {
                val vehicles = awaitItem()
                assertEquals(2, vehicles.size)
            }
        }

        @Test
        fun `allVehicles updates when vehicles change`() = runTest {
            val v1 = seedVehicle("v1", "Car A")
            fakeVehicleDao.vehicles["v1"] = v1
            refreshAllFlows()

            createViewModel()

            viewModel.allVehicles.test {
                assertEquals(1, awaitItem().size)
                val v2 = seedVehicle("v2", "Car B")
                fakeVehicleDao.vehicles["v2"] = v2
                fakeVehicleDao.updateFlow()
                assertEquals(2, awaitItem().size)
            }
        }
    }

    @Nested
    @DisplayName("switchVehicle")
    inner class SwitchVehicle {

        @Test
        fun `persists new active vehicle to DataStore`() = runTest {
            val v1 = seedVehicle("v1", "Car A")
            val v2 = seedVehicle("v2", "Car B", odometerKm = 50000.0)
            fakeVehicleDao.vehicles["v1"] = v1
            fakeVehicleDao.vehicles["v2"] = v2
            activeVehicleRepository.setActiveVehicle("v1")
            refreshAllFlows()

            createViewModel()

            viewModel.switchVehicle("v2")

            val activeId = activeVehicleRepository.activeVehicleId.first()
            assertEquals("v2", activeId)
        }

        @Test
        fun `ui state updates to show switched vehicle`() = runTest {
            val v1 = seedVehicle("v1", "Car A")
            val v2 = seedVehicle("v2", "Car B", odometerKm = 50000.0)
            fakeVehicleDao.vehicles["v1"] = v1
            fakeVehicleDao.vehicles["v2"] = v2
            activeVehicleRepository.setActiveVehicle("v1")
            refreshAllFlows()

            createViewModel()

            viewModel.uiState.test {
                val initial = awaitItem()
                assertTrue(initial is UiState.Success)
                assertEquals("Car A", (initial as UiState.Success).data.vehicle.name)

                viewModel.switchVehicle("v2")

                val updated = awaitItem()
                assertTrue(updated is UiState.Success)
                assertEquals("Car B", (updated as UiState.Success).data.vehicle.name)
                assertEquals(50000.0, updated.data.vehicle.odometerKm)
            }
        }
    }

    @Nested
    @DisplayName("deleted vehicle fallback")
    inner class DeletedVehicleFallback {

        @Test
        fun `falls back to first available when active vehicle deleted`() = runTest {
            val v1 = seedVehicle("v1", "Car A")
            val v2 = seedVehicle("v2", "Car B")
            fakeVehicleDao.vehicles["v1"] = v1
            fakeVehicleDao.vehicles["v2"] = v2
            activeVehicleRepository.setActiveVehicle("v1")
            refreshAllFlows()

            createViewModel()

            viewModel.uiState.test {
                val initial = awaitItem()
                assertTrue(initial is UiState.Success)
                assertEquals("Car A", (initial as UiState.Success).data.vehicle.name)

                fakeVehicleDao.vehicles.remove("v1")
                fakeVehicleDao.updateFlow()

                // Collect events until we get a Success showing the fallback vehicle
                var finalState = awaitItem()
                // May see transient Loading state during fallback
                if (finalState is UiState.Loading) {
                    finalState = awaitItem()
                }
                assertTrue(finalState is UiState.Success, "Expected Success state after fallback, got $finalState")
                assertEquals("v2", (finalState as UiState.Success).data.vehicle.id)
            }
        }

        @Test
        fun `shows error when no vehicles remain`() = runTest {
            val v1 = seedVehicle("v1", "Car A")
            fakeVehicleDao.vehicles["v1"] = v1
            activeVehicleRepository.setActiveVehicle("v1")
            refreshAllFlows()

            createViewModel()

            viewModel.uiState.test {
                val initial = awaitItem()
                assertTrue(initial is UiState.Success)

                fakeVehicleDao.vehicles.clear()
                fakeVehicleDao.updateFlow()

                val remaining = cancelAndConsumeRemainingEvents()
                val hasError = remaining.any { event ->
                    (event as? app.cash.turbine.Event.Item)?.value is UiState.Error
                }
                assertTrue(hasError, "Expected error state when all vehicles deleted")
            }
        }
    }
}

private class SwitcherFakePreferencesDataStore : androidx.datastore.core.DataStore<Preferences> {
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

private class SwitcherFakeDocumentDao : DocumentDao {
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

private class SwitcherFakeVehicleDao : com.roadmate.core.database.dao.VehicleDao {
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

private class SwitcherFakeMaintenanceDao : com.roadmate.core.database.dao.MaintenanceDao() {
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

private class SwitcherFakeTripDao : com.roadmate.core.database.dao.TripDao() {
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

private class SwitcherFakeFuelDao : com.roadmate.core.database.dao.FuelDao {
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
