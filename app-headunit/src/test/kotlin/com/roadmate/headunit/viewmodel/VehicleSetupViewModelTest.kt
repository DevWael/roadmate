package com.roadmate.headunit.viewmodel

import app.cash.turbine.test
import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.OdometerUnit
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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("VehicleSetupViewModel")
class VehicleSetupViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeVehicleDao: FakeVehicleDao
    private lateinit var fakeMaintenanceDao: FakeMaintenanceDao
    private lateinit var fakeDataStore: FakeDataStore
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var maintenanceRepository: MaintenanceRepository
    private lateinit var activeVehicleRepository: ActiveVehicleRepository
    private lateinit var viewModel: VehicleSetupViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeVehicleDao = FakeVehicleDao()
        fakeMaintenanceDao = FakeMaintenanceDao()
        fakeDataStore = FakeDataStore()
        vehicleRepository = VehicleRepository(fakeVehicleDao)
        maintenanceRepository = MaintenanceRepository(fakeMaintenanceDao)
        activeVehicleRepository = ActiveVehicleRepository(fakeDataStore)
        viewModel = VehicleSetupViewModel(
            vehicleRepository = vehicleRepository,
            maintenanceRepository = maintenanceRepository,
            activeVehicleRepository = activeVehicleRepository,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("initial state")
    inner class InitialState {

        @Test
        fun `initial uiState is Success with empty form`() {
            val state = viewModel.uiState.value
            assertTrue(state is UiState.Success)
            val form = (state as UiState.Success).data
            assertEquals("", form.name)
            assertEquals("", form.make)
            assertEquals("", form.model)
        }

        @Test
        fun `initial form has no errors`() {
            val state = viewModel.uiState.value
            val form = (state as UiState.Success).data
            assertTrue(form.errors.isEmpty())
        }
    }

    @Nested
    @DisplayName("field updates")
    inner class FieldUpdates {

        @Test
        fun `updateName changes name field`() {
            viewModel.updateName("Test Car")
            assertEquals("Test Car", currentForm().name)
        }

        @Test
        fun `updateMake changes make field`() {
            viewModel.updateMake("Toyota")
            assertEquals("Toyota", currentForm().make)
        }

        @Test
        fun `updateModel changes model field`() {
            viewModel.updateModel("Corolla")
            assertEquals("Corolla", currentForm().model)
        }

        @Test
        fun `updateYear changes year field`() {
            viewModel.updateYear("2020")
            assertEquals("2020", currentForm().year)
        }

        @Test
        fun `updateEngineType changes engine type`() {
            viewModel.updateEngineType(EngineType.V6)
            assertEquals(EngineType.V6, currentForm().engineType)
        }

        @Test
        fun `updateEngineSize changes engine size`() {
            viewModel.updateEngineSize("2.0")
            assertEquals("2.0", currentForm().engineSize)
        }

        @Test
        fun `updateFuelType changes fuel type`() {
            viewModel.updateFuelType(FuelType.DIESEL)
            assertEquals(FuelType.DIESEL, currentForm().fuelType)
        }

        @Test
        fun `updatePlateNumber changes plate number`() {
            viewModel.updatePlateNumber("XYZ-789")
            assertEquals("XYZ-789", currentForm().plateNumber)
        }

        @Test
        fun `updateOdometerKm changes odometer`() {
            viewModel.updateOdometerKm("50000")
            assertEquals("50000", currentForm().odometerKm)
        }

        @Test
        fun `updateOdometerUnit changes unit`() {
            viewModel.updateOdometerUnit(OdometerUnit.MILES)
            assertEquals(OdometerUnit.MILES, currentForm().odometerUnit)
        }

        @Test
        fun `updateCityConsumption changes city consumption`() {
            viewModel.updateCityConsumption("8.5")
            assertEquals("8.5", currentForm().cityConsumption)
        }

        @Test
        fun `updateHighwayConsumption changes highway consumption`() {
            viewModel.updateHighwayConsumption("5.2")
            assertEquals("5.2", currentForm().highwayConsumption)
        }

        @Test
        fun `multiple updates preserve all values`() {
            viewModel.updateName("My Car")
            viewModel.updateMake("Honda")
            viewModel.updateModel("Civic")
            viewModel.updateYear("2022")
            val form = currentForm()
            assertEquals("My Car", form.name)
            assertEquals("Honda", form.make)
            assertEquals("Civic", form.model)
            assertEquals("2022", form.year)
        }
    }

    @Nested
    @DisplayName("template selection")
    inner class TemplateSelectionTests {

        @Test
        fun `selectTemplate changes selection to Mitsubishi`() {
            viewModel.selectTemplate(TemplateSelection.MITSUBISHI_LANCER_EX_2015)
            assertEquals(TemplateSelection.MITSUBISHI_LANCER_EX_2015, currentForm().templateSelection)
        }

        @Test
        fun `selectTemplate changes selection to Custom`() {
            viewModel.selectTemplate(TemplateSelection.CUSTOM)
            assertEquals(TemplateSelection.CUSTOM, currentForm().templateSelection)
        }
    }

    @Nested
    @DisplayName("save")
    inner class Save {

        @Test
        fun `save with invalid form sets errors`() {
            viewModel.save()
            val form = currentForm()
            assertTrue(form.errors.isNotEmpty())
            assertTrue(form.errors.containsKey("name"))
            assertTrue(form.errors.containsKey("make"))
        }

        @Test
        fun `save with valid form saves vehicle to dao`() = runTest {
            fillValidForm()
            viewModel.save()

            assertTrue(fakeVehicleDao.vehicles.isNotEmpty())
        }

        @Test
        fun `save with valid form sets active vehicle`() = runTest {
            fillValidForm()
            viewModel.save()

            val activeId = activeVehicleRepository.activeVehicleId.first()
            assertNotNull(activeId)
        }

        @Test
        fun `save without template does not create schedules`() = runTest {
            fillValidForm()
            viewModel.save()

            assertTrue(fakeMaintenanceDao.schedules.isEmpty())
        }

        @Test
        fun `save with Mitsubishi template creates 9 schedules`() = runTest {
            fillValidForm()
            viewModel.selectTemplate(TemplateSelection.MITSUBISHI_LANCER_EX_2015)
            viewModel.save()

            assertEquals(9, fakeMaintenanceDao.schedules.size)
        }

        @Test
        fun `save with Custom template does not create schedules`() = runTest {
            fillValidForm()
            viewModel.selectTemplate(TemplateSelection.CUSTOM)
            viewModel.save()

            assertTrue(fakeMaintenanceDao.schedules.isEmpty())
        }

        @Test
        fun `save with valid form clears errors`() = runTest {
            viewModel.save()
            assertTrue(currentForm().errors.isNotEmpty())

            fillValidForm()
            viewModel.save()
            assertTrue(currentForm().errors.isEmpty())
        }

        @Test
        fun `save with dao failure results in error state`() = runTest {
            fakeVehicleDao.shouldThrow = true
            fillValidForm()
            viewModel.save()

            val state = viewModel.uiState.value
            assertTrue(state is UiState.Error)
        }
    }

    private fun fillValidForm() {
        viewModel.updateName("Test Car")
        viewModel.updateMake("Mitsubishi")
        viewModel.updateModel("Lancer EX")
        viewModel.updateYear("2015")
        viewModel.updateEngineSize("1.6")
        viewModel.updatePlateNumber("ABC-123")
        viewModel.updateOdometerKm("85000")
        viewModel.updateCityConsumption("9.5")
        viewModel.updateHighwayConsumption("6.8")
    }

    private fun currentForm(): VehicleFormState {
        val state = viewModel.uiState.value
        return (state as? UiState.Success)?.data ?: VehicleFormState()
    }
}

private class FakeVehicleDao : VehicleDao {
    val vehicles = mutableMapOf<String, Vehicle>()
    var shouldThrow = false

    private val flow = MutableStateFlow<List<Vehicle>>(emptyList())

    fun updateFlow() {
        flow.value = vehicles.values.toList()
    }

    override fun getVehicle(vehicleId: String): Flow<Vehicle?> =
        flow.map { list -> list.find { it.id == vehicleId } }

    override fun getAllVehicles(): Flow<List<Vehicle>> =
        flow.map { list -> list.sortedByDescending { it.lastModified } }

    override suspend fun upsert(vehicle: Vehicle) {
        if (shouldThrow) throw RuntimeException("Test error")
        vehicles[vehicle.id] = vehicle
        updateFlow()
    }

    override suspend fun upsertAll(vehicles: List<Vehicle>) {
        if (shouldThrow) throw RuntimeException("Test error")
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

    override fun getVehicleCount(): Flow<Int> =
        flow.map { it.size }

    override suspend fun addToOdometer(vehicleId: String, distanceKm: Double, lastModified: Long) {
        val vehicle = vehicles[vehicleId] ?: return
        vehicles[vehicleId] = vehicle.copy(odometerKm = vehicle.odometerKm + distanceKm, lastModified = lastModified)
        updateFlow()
    }
}

private class FakeMaintenanceDao : MaintenanceDao {
    val schedules = mutableListOf<MaintenanceSchedule>()
    val records = mutableListOf<MaintenanceRecord>()
    var shouldThrow = false

    override fun getSchedulesForVehicle(vehicleId: String): Flow<List<MaintenanceSchedule>> =
        MutableStateFlow(schedules.filter { it.vehicleId == vehicleId })

    override fun getSchedule(scheduleId: String): Flow<MaintenanceSchedule?> =
        MutableStateFlow(schedules.find { it.id == scheduleId })

    override suspend fun upsertSchedule(schedule: MaintenanceSchedule) {
        if (shouldThrow) throw RuntimeException("Test error")
        schedules.add(schedule)
    }

    override suspend fun upsertSchedules(schedules: List<MaintenanceSchedule>) {
        if (shouldThrow) throw RuntimeException("Test error")
        this.schedules.addAll(schedules)
    }

    override suspend fun deleteSchedule(schedule: MaintenanceSchedule) {
        schedules.removeIf { it.id == schedule.id }
    }

    override suspend fun deleteScheduleById(scheduleId: String) {
        schedules.removeIf { it.id == scheduleId }
    }

    override fun getRecordsForSchedule(scheduleId: String): Flow<List<MaintenanceRecord>> =
        MutableStateFlow(records.filter { it.scheduleId == scheduleId })

    override fun getRecordsForVehicle(vehicleId: String): Flow<List<MaintenanceRecord>> =
        MutableStateFlow(records.filter { it.vehicleId == vehicleId })

    override fun getRecord(recordId: String): Flow<MaintenanceRecord?> =
        MutableStateFlow(records.find { it.id == recordId })

    override suspend fun upsertRecord(record: MaintenanceRecord) {
        records.add(record)
    }

    override suspend fun upsertRecords(records: List<MaintenanceRecord>) {
        this.records.addAll(records)
    }

    override suspend fun deleteRecord(record: MaintenanceRecord) {
        records.removeIf { it.id == record.id }
    }

    override suspend fun deleteRecordById(recordId: String) {
        records.removeIf { it.id == recordId }
    }
}

private class FakeDataStore : DataStore<Preferences> {
    private val _data = MutableStateFlow<Preferences>(emptyPreferences())

    override val data: Flow<Preferences> = _data

    override suspend fun updateData(
        transform: suspend (Preferences) -> Preferences,
    ): Preferences {
        val new = transform(_data.value)
        _data.value = new
        return new
    }
}
