package com.roadmate.phone.ui.fuel

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import com.roadmate.core.database.dao.FuelDao
import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.FuelLog
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.FuelRepository
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("FuelLogViewModel")
class FuelLogViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeFuelDao: FuelTestFakeDao
    private lateinit var fakeVehicleDao: VehicleTestFakeDao
    private lateinit var fakeDataStore: FuelTestFakePreferencesDataStore
    private lateinit var fuelRepository: FuelRepository
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var activeVehicleRepository: ActiveVehicleRepository
    private lateinit var viewModel: FuelLogViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeFuelDao = FuelTestFakeDao()
        fakeVehicleDao = VehicleTestFakeDao()
        fakeDataStore = FuelTestFakePreferencesDataStore()
        fuelRepository = FuelRepository(fakeFuelDao)
        vehicleRepository = VehicleRepository(fakeVehicleDao)
        activeVehicleRepository = ActiveVehicleRepository(fakeDataStore)
    }

    private suspend fun createViewModel() {
        viewModel = FuelLogViewModel(
            activeVehicleRepository = activeVehicleRepository,
            fuelRepository = fuelRepository,
            vehicleRepository = vehicleRepository,
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
        fun `loads fuel entries for active vehicle`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeVehicleDao.vehicle = testVehicle()
            fakeFuelDao.fuelLogs["f-1"] = testFuelLog(id = "f-1", vehicleId = "veh-1")
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertEquals(1, data.entries.size)
                assertEquals("f-1", data.entries[0].fuelLog.id)
            }
        }

        @Test
        fun `shows error when no active vehicle`() = runTest {
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Error)
            }
        }

        @Test
        fun `returns empty entries when no fuel logs exist`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeVehicleDao.vehicle = testVehicle()
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                assertTrue((state as UiState.Success).data.entries.isEmpty())
            }
        }
    }

    @Nested
    @DisplayName("consumption calculation")
    inner class ConsumptionCalculation {

        @Test
        fun `calculates consumption for full tank pairs`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeVehicleDao.vehicle = testVehicle()
            fakeFuelDao.fuelLogs["f-1"] = testFuelLog(
                id = "f-1", vehicleId = "veh-1",
                odometerKm = 85000.0, liters = 40.0, isFullTank = true,
                date = 1000,
            )
            fakeFuelDao.fuelLogs["f-2"] = testFuelLog(
                id = "f-2", vehicleId = "veh-1",
                odometerKm = 85500.0, liters = 30.0, isFullTank = true,
                date = 2000,
            )
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                val entryWithConsumption = data.entries.find { it.fuelLog.id == "f-2" }
                assertNotNull(entryWithConsumption)
                assertNotNull(entryWithConsumption!!.consumptionLPer100km)
                assertEquals(6.0, entryWithConsumption.consumptionLPer100km!!, 0.01)
            }
        }

        @Test
        fun `null consumption when not full tank pair`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeVehicleDao.vehicle = testVehicle()
            fakeFuelDao.fuelLogs["f-1"] = testFuelLog(
                id = "f-1", vehicleId = "veh-1",
                isFullTank = false,
            )
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertEquals(1, data.entries.size)
                assertEquals(null, data.entries[0].consumptionLPer100km)
            }
        }

        @Test
        fun `marks over consumption when actual exceeds 20 percent`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeVehicleDao.vehicle = testVehicle(cityConsumption = 5.0, highwayConsumption = 5.0)
            fakeFuelDao.fuelLogs["f-1"] = testFuelLog(
                id = "f-1", vehicleId = "veh-1",
                odometerKm = 85000.0, liters = 40.0, isFullTank = true,
                date = 1000,
            )
            fakeFuelDao.fuelLogs["f-2"] = testFuelLog(
                id = "f-2", vehicleId = "veh-1",
                odometerKm = 85200.0, liters = 30.0, isFullTank = true,
                date = 2000,
            )
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                val entry = data.entries.find { it.fuelLog.id == "f-2" }
                assertNotNull(entry)
                assertTrue(entry!!.isOverConsumption)
            }
        }
    }

    @Nested
    @DisplayName("ODO validation")
    inner class OdoValidation {

        @Test
        fun `ODO validation error when value less than latest`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeVehicleDao.vehicle = testVehicle()
            fakeFuelDao.fuelLogs["f-1"] = testFuelLog(
                id = "f-1", vehicleId = "veh-1", odometerKm = 86000.0,
            )
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.onOdometerKmChange("85000")

            val errors = viewModel.formErrors.value
            assertTrue(errors.containsKey("odometerKm"))
        }

        @Test
        fun `no ODO error when value greater than latest`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeVehicleDao.vehicle = testVehicle()
            fakeFuelDao.fuelLogs["f-1"] = testFuelLog(
                id = "f-1", vehicleId = "veh-1", odometerKm = 86000.0,
            )
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.onOdometerKmChange("87000")

            val errors = viewModel.formErrors.value
            assertTrue(!errors.containsKey("odometerKm"))
        }

        @Test
        fun `no ODO error when no previous entries`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeVehicleDao.vehicle = testVehicle()
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.onOdometerKmChange("85000")

            val errors = viewModel.formErrors.value
            assertTrue(!errors.containsKey("odometerKm"))
        }
    }

    @Nested
    @DisplayName("summary calculation")
    inner class SummaryCalculation {

        @Test
        fun `calculates total cost this month`() = runTest {
            val now = System.currentTimeMillis()
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeVehicleDao.vehicle = testVehicle()
            fakeFuelDao.fuelLogs["f-1"] = testFuelLog(
                id = "f-1", vehicleId = "veh-1", totalCost = 500.0, date = now,
            )
            fakeFuelDao.fuelLogs["f-2"] = testFuelLog(
                id = "f-2", vehicleId = "veh-1", totalCost = 300.0, date = now - 1000,
            )
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val summary = (state as UiState.Success).data.summary
                assertEquals(800.0, summary.totalCostThisMonth, 0.01)
            }
        }

        @Test
        fun `calculates average cost per km`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeVehicleDao.vehicle = testVehicle()
            fakeFuelDao.fuelLogs["f-1"] = testFuelLog(
                id = "f-1", vehicleId = "veh-1",
                odometerKm = 85000.0, totalCost = 500.0, date = 1000,
            )
            fakeFuelDao.fuelLogs["f-2"] = testFuelLog(
                id = "f-2", vehicleId = "veh-1",
                odometerKm = 86000.0, totalCost = 600.0, date = 2000,
            )
            fakeFuelDao.updateFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val summary = (state as UiState.Success).data.summary
                assertNotNull(summary.avgCostPerKm)
                assertEquals(0.6, summary.avgCostPerKm!!, 0.01)
            }
        }
    }

    private fun testVehicle(
        id: String = "veh-1",
        cityConsumption: Double = 8.0,
        highwayConsumption: Double = 6.0,
    ) = Vehicle(
        id = id,
        name = "Test Car",
        make = "Test",
        model = "Car",
        year = 2020,
        engineType = com.roadmate.core.database.entity.EngineType.INLINE_4,
        engineSize = 2.0,
        fuelType = com.roadmate.core.database.entity.FuelType.GASOLINE,
        plateNumber = "ABC 123",
        odometerKm = 86000.0,
        odometerUnit = com.roadmate.core.database.entity.OdometerUnit.KM,
        cityConsumption = cityConsumption,
        highwayConsumption = highwayConsumption,
    )

    private fun testFuelLog(
        id: String = "test-id",
        vehicleId: String = "veh-1",
        odometerKm: Double = 86000.0,
        liters: Double = 45.0,
        pricePerLiter: Double = 12.75,
        totalCost: Double = 573.75,
        isFullTank: Boolean = true,
        date: Long = System.currentTimeMillis(),
    ) = FuelLog(
        id = id,
        vehicleId = vehicleId,
        date = date,
        odometerKm = odometerKm,
        liters = liters,
        pricePerLiter = pricePerLiter,
        totalCost = totalCost,
        isFullTank = isFullTank,
    )
}

private class FuelTestFakePreferencesDataStore : androidx.datastore.core.DataStore<Preferences> {
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

private class FuelTestFakeDao : FuelDao {
    val fuelLogs = mutableMapOf<String, FuelLog>()
    var shouldThrow = false

    private val flow = MutableStateFlow<List<FuelLog>>(emptyList())

    fun updateFlow() {
        flow.value = fuelLogs.values.toList()
    }

    override fun getFuelLogsForVehicle(vehicleId: String): Flow<List<FuelLog>> =
        flow.map { list -> list.filter { it.vehicleId == vehicleId }.sortedByDescending { it.date } }

    override fun getLastFullTankEntry(vehicleId: String): Flow<FuelLog?> =
        flow.map { list ->
            list.filter { it.vehicleId == vehicleId && it.isFullTank }
                .sortedByDescending { it.date }
                .firstOrNull()
        }

    override fun getLatestFuelEntry(vehicleId: String): Flow<FuelLog?> =
        flow.map { list ->
            list.filter { it.vehicleId == vehicleId }
                .sortedByDescending { it.date }
                .firstOrNull()
        }

    override fun getTwoLastFullTankEntries(vehicleId: String): Flow<List<FuelLog>> =
        flow.map { list ->
            list.filter { it.vehicleId == vehicleId && it.isFullTank }
                .sortedByDescending { it.date }
                .take(2)
        }

    override fun getFuelLog(fuelLogId: String): Flow<FuelLog?> =
        flow.map { list -> list.find { it.id == fuelLogId } }

    override suspend fun upsertFuelLog(fuelLog: FuelLog) {
        if (shouldThrow) throw RuntimeException("Test error")
        fuelLogs[fuelLog.id] = fuelLog
        updateFlow()
    }

    override suspend fun upsertFuelLogs(fuelLogs: List<FuelLog>) {
        if (shouldThrow) throw RuntimeException("Test error")
        fuelLogs.forEach { this.fuelLogs[it.id] = it }
        updateFlow()
    }

    override suspend fun deleteFuelLog(fuelLog: FuelLog) {
        if (shouldThrow) throw RuntimeException("Test error")
        fuelLogs.remove(fuelLog.id)
        updateFlow()
    }

    override suspend fun deleteFuelLogById(fuelLogId: String) {
        if (shouldThrow) throw RuntimeException("Test error")
        fuelLogs.remove(fuelLogId)
        updateFlow()
    }

    override suspend fun getFuelLogsModifiedSince(since: Long): List<FuelLog> =
        fuelLogs.values.filter { it.lastModified > since }

    override suspend fun getFuelLogById(id: String): FuelLog? = null
}

private class VehicleTestFakeDao : VehicleDao {
    var vehicle: Vehicle? = null

    override fun getVehicle(vehicleId: String): Flow<Vehicle?> = MutableStateFlow(vehicle)

    override fun getAllVehicles(): Flow<List<Vehicle>> =
        MutableStateFlow(vehicle?.let { listOf(it) } ?: emptyList())

    override fun getVehicleCount(): Flow<Int> = MutableStateFlow(if (vehicle != null) 1 else 0)

    override suspend fun upsert(vehicle: Vehicle) {}
    override suspend fun upsertAll(vehicles: List<Vehicle>) {}
    override suspend fun delete(vehicle: Vehicle) {}
    override suspend fun deleteById(vehicleId: String) {}
    override suspend fun addToOdometer(vehicleId: String, distanceKm: Double, lastModified: Long) {}
    override suspend fun getModifiedSince(since: Long): List<Vehicle> = emptyList()
    override suspend fun getVehicleById(id: String): Vehicle? = vehicle
}
