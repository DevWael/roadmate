package com.roadmate.headunit.viewmodel

import android.app.Application
import app.cash.turbine.test
import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.ActiveVehicleRepository
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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("WelcomeViewModel")
class WelcomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeVehicleDao: FakeWelcomeVehicleDao
    private lateinit var fakeDataStore: FakeWelcomeDataStore
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var activeVehicleRepository: ActiveVehicleRepository
    private lateinit var viewModel: WelcomeViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeVehicleDao = FakeWelcomeVehicleDao()
        fakeDataStore = FakeWelcomeDataStore()
        vehicleRepository = VehicleRepository(fakeVehicleDao)
        activeVehicleRepository = ActiveVehicleRepository(fakeDataStore)
        viewModel = WelcomeViewModel(
            application = Application(),
            vehicleRepository = vehicleRepository,
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
            assertEquals("", form.odometer)
        }

        @Test
        fun `initial form has no errors`() {
            val form = currentForm()
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
        fun `updateOdometer changes odometer field`() {
            viewModel.updateOdometer("50000")
            assertEquals("50000", currentForm().odometer)
        }

        @Test
        fun `multiple updates preserve all values`() {
            viewModel.updateName("My Car")
            viewModel.updateOdometer("75000")
            val form = currentForm()
            assertEquals("My Car", form.name)
            assertEquals("75000", form.odometer)
        }
    }

    @Nested
    @DisplayName("startTracking")
    inner class StartTracking {

        @Test
        fun `startTracking with empty form sets errors`() {
            viewModel.startTracking()
            val form = currentForm()
            assertTrue(form.errors.isNotEmpty())
            assertTrue(form.errors.containsKey("name"))
            assertTrue(form.errors.containsKey("odometer"))
        }

        @Test
        fun `startTracking with blank name sets name error`() {
            viewModel.updateOdometer("50000")
            viewModel.startTracking()
            assertTrue(currentForm().errors.containsKey("name"))
        }

        @Test
        fun `startTracking with blank odometer sets odometer error`() {
            viewModel.updateName("Test Car")
            viewModel.startTracking()
            assertTrue(currentForm().errors.containsKey("odometer"))
        }

        @Test
        fun `startTracking with valid form saves vehicle to dao`() = runTest {
            fillValidForm()
            viewModel.startTracking()

            assertTrue(fakeVehicleDao.vehicles.isNotEmpty())
        }

        @Test
        fun `startTracking with valid form sets active vehicle`() = runTest {
            fillValidForm()
            viewModel.startTracking()

            val activeId = activeVehicleRepository.activeVehicleId.first()
            assertNotNull(activeId)
        }

        @Test
        fun `startTracking creates vehicle with provided name`() = runTest {
            viewModel.updateName("My Ride")
            viewModel.updateOdometer("30000")
            viewModel.startTracking()

            val vehicle = fakeVehicleDao.vehicles.values.first()
            assertEquals("My Ride", vehicle.name)
        }

        @Test
        fun `startTracking creates vehicle with provided odometer`() = runTest {
            viewModel.updateName("Test")
            viewModel.updateOdometer("12345")
            viewModel.startTracking()

            val vehicle = fakeVehicleDao.vehicles.values.first()
            assertEquals(12345.0, vehicle.odometerKm, 0.001)
        }

        @Test
        fun `startTracking creates vehicle with zero maintenance schedules`() = runTest {
            fillValidForm()
            viewModel.startTracking()

            val vehicle = fakeVehicleDao.vehicles.values.first()
            assertEquals(0.0, vehicle.engineSize, 0.001)
            assertEquals(0.0, vehicle.cityConsumption, 0.001)
            assertEquals(0.0, vehicle.highwayConsumption, 0.001)
        }

        @Test
        fun `startTracking creates vehicle with empty optional fields`() = runTest {
            fillValidForm()
            viewModel.startTracking()

            val vehicle = fakeVehicleDao.vehicles.values.first()
            assertEquals("", vehicle.make)
            assertEquals("", vehicle.model)
            assertEquals("", vehicle.plateNumber)
            assertEquals(0, vehicle.year)
        }

        @Test
        fun `startTracking with dao failure results in error state`() = runTest {
            fakeVehicleDao.shouldThrow = true
            fillValidForm()
            viewModel.startTracking()

            val state = viewModel.uiState.value
            assertTrue(state is UiState.Error)
        }

        @Test
        fun `startTracking stays on loading after successful save`() = runTest {
            fillValidForm()
            viewModel.startTracking()
            val state = viewModel.uiState.value
            assertTrue(
                state is UiState.Loading,
                "Expected Loading after startTracking (reactive nav), got ${state::class.simpleName}"
            )
        }

        @Test
        fun `startTracking trims name whitespace`() = runTest {
            viewModel.updateName("  My Car  ")
            viewModel.updateOdometer("50000")
            viewModel.startTracking()

            val vehicle = fakeVehicleDao.vehicles.values.first()
            assertEquals("My Car", vehicle.name)
        }

        @Test
        fun `double-tap startTracking does not create duplicate vehicles`() = runTest {
            fillValidForm()
            viewModel.startTracking()
            // State is now Loading
            viewModel.startTracking()

            assertEquals(1, fakeVehicleDao.vehicles.size)
        }
    }

    @Nested
    @DisplayName("resetToForm")
    inner class ResetToForm {

        @Test
        fun `resetToForm from error state returns to success`() = runTest {
            fakeVehicleDao.shouldThrow = true
            fillValidForm()
            viewModel.startTracking()
            assertTrue(viewModel.uiState.value is UiState.Error)

            viewModel.resetToForm()
            assertTrue(viewModel.uiState.value is UiState.Success)
        }
    }

    private fun fillValidForm() {
        viewModel.updateName("Test Car")
        viewModel.updateOdometer("50000")
    }

    private fun currentForm(): WelcomeFormState {
        val state = viewModel.uiState.value
        return (state as? UiState.Success)?.data ?: WelcomeFormState()
    }
}

private class FakeWelcomeVehicleDao : VehicleDao {
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

private class FakeWelcomeDataStore : DataStore<Preferences> {
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
