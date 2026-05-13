package com.roadmate.phone.ui.maintenance

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.MaintenanceRepository
import com.roadmate.core.repository.VehicleRepository
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
@DisplayName("MaintenanceListViewModel")
class MaintenanceListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeMaintenanceDao: ListFakeMaintenanceDao
    private lateinit var fakeVehicleDao: ListFakeVehicleDao
    private lateinit var fakeDataStore: ListFakePreferencesDataStore
    private lateinit var maintenanceRepository: MaintenanceRepository
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var activeVehicleRepository: ActiveVehicleRepository
    private lateinit var viewModel: MaintenanceListViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeMaintenanceDao = ListFakeMaintenanceDao()
        fakeVehicleDao = ListFakeVehicleDao()
        fakeDataStore = ListFakePreferencesDataStore()
        maintenanceRepository = MaintenanceRepository(fakeMaintenanceDao)
        vehicleRepository = VehicleRepository(fakeVehicleDao)
        activeVehicleRepository = ActiveVehicleRepository(fakeDataStore)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("loadData")
    inner class LoadData {

        @Test
        fun `loads maintenance items for active vehicle`() = runTest {
            val vehicle = testVehicle()
            val schedule1 = testSchedule(id = "s1", name = "Oil Change", lastServiceKm = 80000.0, intervalKm = 10000)
            val schedule2 = testSchedule(id = "s2", name = "Tire Rotation", lastServiceKm = 85000.0, intervalKm = 20000)

            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeMaintenanceDao.schedules["s1"] = schedule1
            fakeMaintenanceDao.schedules["s2"] = schedule2
            fakeVehicleDao.updateVehicleFlow()
            fakeMaintenanceDao.updateScheduleFlow()

            viewModel = MaintenanceListViewModel(activeVehicleRepository, maintenanceRepository, vehicleRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertEquals(2, data.items.size)
                assertEquals("veh-1", data.vehicle.id)
            }
        }

        @Test
        fun `sorts items by urgency (percentage descending)`() = runTest {
            val vehicle = testVehicle()
            val scheduleLow = testSchedule(id = "s1", name = "Low", lastServiceKm = 80000.0, intervalKm = 10000)
            val scheduleHigh = testSchedule(id = "s2", name = "High", lastServiceKm = 88000.0, intervalKm = 10000)

            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeMaintenanceDao.schedules["s1"] = scheduleLow
            fakeMaintenanceDao.schedules["s2"] = scheduleHigh
            fakeVehicleDao.updateVehicleFlow()
            fakeMaintenanceDao.updateScheduleFlow()

            viewModel = MaintenanceListViewModel(activeVehicleRepository, maintenanceRepository, vehicleRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val items = (state as UiState.Success).data.items
                assertTrue(items[0].percentage >= items[1].percentage)
            }
        }

        @Test
        fun `shows error when no active vehicle`() = runTest {
            fakeVehicleDao.updateVehicleFlow()
            fakeMaintenanceDao.updateScheduleFlow()

            viewModel = MaintenanceListViewModel(activeVehicleRepository, maintenanceRepository, vehicleRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Error)
            }
        }

        @Test
        fun `calculates percentage from interval correctly`() = runTest {
            val vehicle = testVehicle(odometerKm = 85000.0)
            val schedule = testSchedule(lastServiceKm = 80000.0, intervalKm = 10000)

            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeMaintenanceDao.schedules["sched-1"] = schedule
            fakeVehicleDao.updateVehicleFlow()
            fakeMaintenanceDao.updateScheduleFlow()

            viewModel = MaintenanceListViewModel(activeVehicleRepository, maintenanceRepository, vehicleRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val item = (state as UiState.Success).data.items.first()
                assertEquals(50f, item.percentage, 0.1f)
                assertEquals(5000.0, item.remainingKm, 0.01)
            }
        }
    }

    @Nested
    @DisplayName("form validation")
    inner class FormValidation {

        @BeforeEach
        fun initViewModel() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeVehicleDao.vehicles["veh-1"] = testVehicle()
            fakeVehicleDao.updateVehicleFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            viewModel = MaintenanceListViewModel(activeVehicleRepository, maintenanceRepository, vehicleRepository)
        }

        @Test
        fun `isSaveEnabled true when all required fields filled`() = runTest {
            viewModel.onNameChange("Oil Change")
            viewModel.onIntervalKmChange("10000")
            viewModel.onLastServiceKmChange("80000")

            assertTrue(viewModel.isSaveEnabled.first())
        }

        @Test
        fun `isSaveEnabled false when name blank`() = runTest {
            viewModel.onNameChange("")
            viewModel.onIntervalKmChange("10000")
            viewModel.onLastServiceKmChange("80000")

            assertEquals(false, viewModel.isSaveEnabled.first())
        }

        @Test
        fun `isSaveEnabled true with interval months instead of km`() = runTest {
            viewModel.onNameChange("Oil Change")
            viewModel.onIntervalMonthsChange("6")
            viewModel.onLastServiceKmChange("80000")

            assertTrue(viewModel.isSaveEnabled.first())
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
        engineType = com.roadmate.core.database.entity.EngineType.INLINE_4,
        engineSize = 2.0,
        fuelType = com.roadmate.core.database.entity.FuelType.GASOLINE,
        plateNumber = "ABC-123",
        odometerKm = odometerKm,
        odometerUnit = com.roadmate.core.database.entity.OdometerUnit.KM,
        cityConsumption = 8.0,
        highwayConsumption = 6.0,
    )

    private fun testSchedule(
        id: String = "sched-1",
        vehicleId: String = "veh-1",
        name: String = "Oil Change",
        lastServiceKm: Double = 80000.0,
        intervalKm: Int? = 10000,
    ) = MaintenanceSchedule(
        id = id,
        vehicleId = vehicleId,
        name = name,
        intervalKm = intervalKm,
        intervalMonths = 6,
        lastServiceKm = lastServiceKm,
        lastServiceDate = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000,
        isCustom = false,
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

private class ListFakeMaintenanceDao : MaintenanceDao() {
    val schedules = mutableMapOf<String, MaintenanceSchedule>()
    val records = mutableMapOf<String, MaintenanceRecord>()

    private val scheduleFlow = MutableStateFlow<List<MaintenanceSchedule>>(emptyList())
    private val recordFlow = MutableStateFlow<List<MaintenanceRecord>>(emptyList())

    fun updateScheduleFlow() { scheduleFlow.value = schedules.values.toList() }
    fun updateRecordFlow() { recordFlow.value = records.values.toList() }

    override fun getSchedulesForVehicle(vehicleId: String): Flow<List<MaintenanceSchedule>> =
        scheduleFlow.map { list -> list.filter { it.vehicleId == vehicleId } }

    override fun getSchedule(scheduleId: String): Flow<MaintenanceSchedule?> =
        scheduleFlow.map { list -> list.find { it.id == scheduleId } }

    override suspend fun upsertSchedule(schedule: MaintenanceSchedule) {
        schedules[schedule.id] = schedule
        updateScheduleFlow()
    }

    override suspend fun upsertSchedules(schedules: List<MaintenanceSchedule>) {
        schedules.forEach { this.schedules[it.id] = it }
        updateScheduleFlow()
    }

    override suspend fun deleteSchedule(schedule: MaintenanceSchedule) {
        schedules.remove(schedule.id)
        updateScheduleFlow()
    }

    override suspend fun deleteScheduleById(scheduleId: String) {
        schedules.remove(scheduleId)
        updateScheduleFlow()
    }

    override fun getRecordsForSchedule(scheduleId: String): Flow<List<MaintenanceRecord>> =
        recordFlow.map { list -> list.filter { it.scheduleId == scheduleId } }

    override fun getRecordsForVehicle(vehicleId: String): Flow<List<MaintenanceRecord>> =
        recordFlow.map { list -> list.filter { it.vehicleId == vehicleId } }

    override fun getRecord(recordId: String): Flow<MaintenanceRecord?> =
        recordFlow.map { list -> list.find { it.id == recordId } }

    override suspend fun upsertRecord(record: MaintenanceRecord) {
        records[record.id] = record
        updateRecordFlow()
    }

    override suspend fun upsertRecords(records: List<MaintenanceRecord>) {
        records.forEach { this.records[it.id] = it }
        updateRecordFlow()
    }

    override suspend fun deleteRecord(record: MaintenanceRecord) {
        records.remove(record.id)
        updateRecordFlow()
    }

    override suspend fun deleteRecordById(recordId: String) {
        records.remove(recordId)
        updateRecordFlow()
    }

    override suspend fun deleteRecordsByScheduleId(scheduleId: String) {
        val toRemove = records.values.filter { it.scheduleId == scheduleId }.map { it.id }
        toRemove.forEach { records.remove(it) }
        updateRecordFlow()
    }

    override suspend fun getSchedulesModifiedSince(since: Long): List<MaintenanceSchedule> =
        schedules.values.filter { it.lastServiceDate > since }

    override suspend fun getRecordsModifiedSince(since: Long): List<MaintenanceRecord> =
        records.values.filter { it.datePerformed > since }

    override suspend fun getScheduleById(id: String): MaintenanceSchedule? = schedules[id]
    override suspend fun getRecordById(id: String): MaintenanceRecord? = records[id]
}

private class ListFakeVehicleDao : VehicleDao {
    val vehicles = mutableMapOf<String, Vehicle>()
    private val vehicleFlow = MutableStateFlow<List<Vehicle>>(emptyList())

    fun updateVehicleFlow() { vehicleFlow.value = vehicles.values.toList() }

    override fun getVehicle(vehicleId: String): Flow<Vehicle?> =
        vehicleFlow.map { list -> list.find { it.id == vehicleId } }

    override fun getAllVehicles(): Flow<List<Vehicle>> = vehicleFlow

    override suspend fun upsert(vehicle: Vehicle) {
        vehicles[vehicle.id] = vehicle
        updateVehicleFlow()
    }

    override suspend fun upsertAll(vehicles: List<Vehicle>) {
        vehicles.forEach { this.vehicles[it.id] = it }
        updateVehicleFlow()
    }

    override suspend fun delete(vehicle: Vehicle) {
        vehicles.remove(vehicle.id)
        updateVehicleFlow()
    }

    override suspend fun deleteById(vehicleId: String) {
        vehicles.remove(vehicleId)
        updateVehicleFlow()
    }

    override fun getVehicleCount(): Flow<Int> = vehicleFlow.map { it.size }

    override suspend fun addToOdometer(vehicleId: String, distanceKm: Double, lastModified: Long) {
        vehicles[vehicleId]?.let {
            vehicles[vehicleId] = it.copy(odometerKm = it.odometerKm + distanceKm)
            updateVehicleFlow()
        }
    }

    override suspend fun getModifiedSince(since: Long): List<Vehicle> =
        vehicles.values.filter { it.lastModified > since }

    override suspend fun getVehicleById(id: String): Vehicle? = vehicles[id]
}
