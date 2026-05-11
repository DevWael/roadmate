package com.roadmate.headunit.viewmodel

import app.cash.turbine.test
import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.MaintenanceRepository
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("MaintenanceItemViewModel")
class MaintenanceItemViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeDao: FakeItemMaintenanceDao
    private lateinit var repository: MaintenanceRepository
    private lateinit var viewModel: MaintenanceItemViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeItemMaintenanceDao()
        repository = MaintenanceRepository(fakeDao)
        viewModel = MaintenanceItemViewModel(repository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("initializeForAdd")
    inner class InitializeForAdd {

        @Test
        @DisplayName("sets vehicle defaults")
        fun setsVehicleDefaults() {
            viewModel.initializeForAdd("veh-1", 90000.0)

            val state = (viewModel.formState.value as UiState.Success).data
            assertEquals("veh-1", state.vehicleId)
            assertEquals(90000.0, state.vehicleOdometerKm)
            assertEquals("90000", state.lastServiceKm)
        }
    }

    @Nested
    @DisplayName("initializeForEdit")
    inner class InitializeForEdit {

        @Test
        @DisplayName("populates from existing schedule")
        fun populatesFromSchedule() {
            val schedule = createTestSchedule()
            viewModel.initializeForEdit(schedule, 95000.0)

            val state = (viewModel.formState.value as UiState.Success).data
            assertEquals("Oil Change", state.name)
            assertEquals("10000", state.intervalKm)
            assertEquals("6", state.intervalMonths)
        }

        @Test
        @DisplayName("preserves isCustom from original schedule")
        fun preservesIsCustom() {
            val templateSchedule = createTestSchedule().copy(isCustom = false)
            viewModel.initializeForEdit(templateSchedule, 95000.0)

            val state = (viewModel.formState.value as UiState.Success).data
            assertFalse(state.isCustom)
        }
    }

    @Nested
    @DisplayName("update methods")
    inner class UpdateMethods {

        @Test
        @DisplayName("updateName updates form")
        fun updateName() {
            viewModel.initializeForAdd("veh-1", 90000.0)
            viewModel.updateName("Brake Fluid")

            val state = (viewModel.formState.value as UiState.Success).data
            assertEquals("Brake Fluid", state.name)
        }

        @Test
        @DisplayName("updateIntervalKm updates form")
        fun updateIntervalKm() {
            viewModel.initializeForAdd("veh-1", 90000.0)
            viewModel.updateIntervalKm("5000")

            val state = (viewModel.formState.value as UiState.Success).data
            assertEquals("5000", state.intervalKm)
        }

        @Test
        @DisplayName("updateIntervalMonths updates form")
        fun updateIntervalMonths() {
            viewModel.initializeForAdd("veh-1", 90000.0)
            viewModel.updateIntervalMonths("12")

            val state = (viewModel.formState.value as UiState.Success).data
            assertEquals("12", state.intervalMonths)
        }

        @Test
        @DisplayName("updateLastServiceKm updates form")
        fun updateLastServiceKm() {
            viewModel.initializeForAdd("veh-1", 90000.0)
            viewModel.updateLastServiceKm("85000")

            val state = (viewModel.formState.value as UiState.Success).data
            assertEquals("85000", state.lastServiceKm)
        }
    }

    @Nested
    @DisplayName("save")
    inner class Save {

        @BeforeEach
        fun initForm() {
            viewModel.initializeForAdd("veh-1", 90000.0)
            viewModel.updateName("Brake Fluid")
            viewModel.updateIntervalKm("10000")
        }

        @Test
        @DisplayName("save with valid form persists schedule and returns success")
        fun saveValidForm() = runTest {
            var result = false
            viewModel.save { result = it }

            assertTrue(result)
            assertEquals(1, fakeDao.schedules.size)
            val saved = fakeDao.schedules.values.first()
            assertEquals("Brake Fluid", saved.name)
            assertEquals(10000, saved.intervalKm)
            assertTrue(saved.isCustom)
        }

        @Test
        @DisplayName("save with invalid form returns false and does not persist")
        fun saveInvalidForm() = runTest {
            viewModel.updateName("")
            var result = true
            viewModel.save { result = it }

            assertFalse(result)
            assertTrue(fakeDao.schedules.isEmpty())
        }

        @Test
        @DisplayName("save returns false and emits error state when dao throws")
        fun saveDaoFailure() = runTest {
            fakeDao.shouldThrow = true
            var result = true
            viewModel.save { result = it }

            assertFalse(result)
            assertTrue(viewModel.formState.value is UiState.Error)
        }

        @Test
        @DisplayName("save uses existing ID in edit mode")
        fun saveEditMode() = runTest {
            val schedule = createTestSchedule()
            viewModel.initializeForEdit(schedule, 95000.0)
            viewModel.updateIntervalKm("15000")

            var result = false
            viewModel.save { result = it }

            assertTrue(result)
            val saved = fakeDao.schedules["sched-1"]
            assertEquals(15000, saved?.intervalKm)
        }
    }

    @Nested
    @DisplayName("deleteSchedule")
    inner class DeleteSchedule {

        @Test
        @DisplayName("delete removes schedule and records, emits ItemDeleted event")
        fun deleteSuccess() = runTest {
            val schedule = createTestSchedule()
            fakeDao.schedules[schedule.id] = schedule
            val record = MaintenanceRecord(
                id = "rec-1",
                scheduleId = schedule.id,
                vehicleId = schedule.vehicleId,
                datePerformed = System.currentTimeMillis(),
                odometerKm = 85000.0,
            )
            fakeDao.records[record.id] = record
            fakeDao.updateScheduleFlow()
            fakeDao.updateRecordFlow()

            viewModel.events.test {
                var result = false
                viewModel.deleteSchedule(schedule.id) { result = it }

                assertTrue(result)
                assertTrue(fakeDao.schedules.isEmpty())
                assertTrue(fakeDao.records.isEmpty())

                val event = awaitItem()
                assertTrue(event is MaintenanceItemEvent.ItemDeleted)
            }
        }

        @Test
        @DisplayName("delete returns false and emits Error event when dao throws")
        fun deleteDaoFailure() = runTest {
            fakeDao.shouldThrow = true

            viewModel.events.test {
                var result = true
                viewModel.deleteSchedule("sched-1") { result = it }

                assertFalse(result)

                val event = awaitItem()
                assertTrue(event is MaintenanceItemEvent.Error)
            }
        }
    }

    private fun createTestSchedule() = MaintenanceSchedule(
        id = "sched-1",
        vehicleId = "veh-1",
        name = "Oil Change",
        intervalKm = 10000,
        intervalMonths = 6,
        lastServiceKm = 80000.0,
        lastServiceDate = 1600000000000L,
        isCustom = true,
    )
}

private class FakeItemMaintenanceDao : MaintenanceDao() {
    val schedules = mutableMapOf<String, MaintenanceSchedule>()
    val records = mutableMapOf<String, MaintenanceRecord>()
    var shouldThrow = false

    private val scheduleFlow = MutableStateFlow<List<MaintenanceSchedule>>(emptyList())
    private val recordFlow = MutableStateFlow<List<MaintenanceRecord>>(emptyList())

    fun updateScheduleFlow() { scheduleFlow.value = schedules.values.toList() }
    fun updateRecordFlow() { recordFlow.value = records.values.toList() }

    override fun getSchedulesForVehicle(vehicleId: String): Flow<List<MaintenanceSchedule>> =
        scheduleFlow.map { list -> list.filter { it.vehicleId == vehicleId } }

    override fun getSchedule(scheduleId: String): Flow<MaintenanceSchedule?> =
        scheduleFlow.map { list -> list.find { it.id == scheduleId } }

    override suspend fun upsertSchedule(schedule: MaintenanceSchedule) {
        if (shouldThrow) throw RuntimeException("Test error")
        schedules[schedule.id] = schedule
        updateScheduleFlow()
    }

    override suspend fun upsertSchedules(schedules: List<MaintenanceSchedule>) {
        if (shouldThrow) throw RuntimeException("Test error")
        schedules.forEach { this.schedules[it.id] = it }
        updateScheduleFlow()
    }

    override suspend fun deleteSchedule(schedule: MaintenanceSchedule) {
        if (shouldThrow) throw RuntimeException("Test error")
        schedules.remove(schedule.id)
        updateScheduleFlow()
    }

    override suspend fun deleteScheduleById(scheduleId: String) {
        if (shouldThrow) throw RuntimeException("Test error")
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
        if (shouldThrow) throw RuntimeException("Test error")
        records[record.id] = record
        updateRecordFlow()
    }

    override suspend fun upsertRecords(records: List<MaintenanceRecord>) {
        if (shouldThrow) throw RuntimeException("Test error")
        records.forEach { this.records[it.id] = it }
        updateRecordFlow()
    }

    override suspend fun deleteRecord(record: MaintenanceRecord) {
        if (shouldThrow) throw RuntimeException("Test error")
        records.remove(record.id)
        updateRecordFlow()
    }

    override suspend fun deleteRecordById(recordId: String) {
        if (shouldThrow) throw RuntimeException("Test error")
        records.remove(recordId)
        updateRecordFlow()
    }

    override suspend fun deleteRecordsByScheduleId(scheduleId: String) {
        if (shouldThrow) throw RuntimeException("Test error")
        val toRemove = records.values.filter { it.scheduleId == scheduleId }.map { it.id }
        toRemove.forEach { records.remove(it) }
        updateRecordFlow()
    }
}
