package com.roadmate.phone.ui.maintenance

import app.cash.turbine.test
import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.MaintenanceRepository
import com.roadmate.core.repository.VehicleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("MaintenanceDetailViewModel")
class MaintenanceDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeMaintenanceDao: DetailFakeMaintenanceDao
    private lateinit var fakeVehicleDao: DetailFakeVehicleDao
    private lateinit var maintenanceRepository: MaintenanceRepository
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var viewModel: MaintenanceDetailViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeMaintenanceDao = DetailFakeMaintenanceDao()
        fakeVehicleDao = DetailFakeVehicleDao()
        maintenanceRepository = MaintenanceRepository(fakeMaintenanceDao)
        vehicleRepository = VehicleRepository(fakeVehicleDao)
        viewModel = MaintenanceDetailViewModel(maintenanceRepository, vehicleRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("loadSchedule")
    inner class LoadSchedule {

        @Test
        fun `loads schedule with vehicle and records`() = runTest {
            val schedule = testSchedule()
            val vehicle = testVehicle()
            val record = testRecord()

            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeMaintenanceDao.schedules["sched-1"] = schedule
            fakeMaintenanceDao.records["rec-1"] = record
            fakeVehicleDao.updateVehicleFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeMaintenanceDao.updateRecordFlow()

            viewModel.loadSchedule("sched-1")

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertEquals("sched-1", data.schedule?.id)
                assertEquals("veh-1", data.vehicle?.id)
                assertEquals(1, data.records.size)
                assertEquals(75.0, data.totalSpent, 0.01)
            }
        }

        @Test
        fun `shows error when schedule not found`() = runTest {
            fakeVehicleDao.updateVehicleFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeMaintenanceDao.updateRecordFlow()

            viewModel.loadSchedule("nonexistent")

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Error)
            }
        }

        @Test
        fun `calculates total spent from records`() = runTest {
            val schedule = testSchedule()
            val vehicle = testVehicle()
            val record1 = testRecord(id = "rec-1", cost = 50.0)
            val record2 = testRecord(id = "rec-2", cost = 30.0)
            val record3 = testRecord(id = "rec-3", cost = null)

            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeMaintenanceDao.schedules["sched-1"] = schedule
            fakeMaintenanceDao.records["rec-1"] = record1
            fakeMaintenanceDao.records["rec-2"] = record2
            fakeMaintenanceDao.records["rec-3"] = record3
            fakeVehicleDao.updateVehicleFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeMaintenanceDao.updateRecordFlow()

            viewModel.loadSchedule("sched-1")

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertEquals(80.0, data.totalSpent, 0.01)
            }
        }

        @Test
        fun `calculates percentage from interval`() = runTest {
            val schedule = testSchedule(lastServiceKm = 80000.0, intervalKm = 10000)
            val vehicle = testVehicle(odometerKm = 85000.0)

            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeMaintenanceDao.schedules["sched-1"] = schedule
            fakeVehicleDao.updateVehicleFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeMaintenanceDao.updateRecordFlow()

            viewModel.loadSchedule("sched-1")

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertEquals(50f, data.percentage, 0.1f)
                assertEquals(5000.0, data.remainingKm, 0.01)
            }
        }

        @Test
        fun `predicted next service date is set`() = runTest {
            val schedule = testSchedule()
            val vehicle = testVehicle()

            fakeVehicleDao.vehicles["veh-1"] = vehicle
            fakeMaintenanceDao.schedules["sched-1"] = schedule
            fakeVehicleDao.updateVehicleFlow()
            fakeMaintenanceDao.updateScheduleFlow()
            fakeMaintenanceDao.updateRecordFlow()

            viewModel.loadSchedule("sched-1")

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertNotNull(data.predictedNextServiceDate)
            }
        }
    }

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

    private fun testRecord(
        id: String = "rec-1",
        cost: Double? = 75.0,
    ) = MaintenanceRecord(
        id = id,
        scheduleId = "sched-1",
        vehicleId = "veh-1",
        datePerformed = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
        odometerKm = 85000.0,
        cost = cost,
        location = "Dealer",
        notes = "Full synthetic",
    )
}

private class DetailFakeMaintenanceDao : MaintenanceDao() {
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
}

private class DetailFakeVehicleDao : VehicleDao {
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
}
