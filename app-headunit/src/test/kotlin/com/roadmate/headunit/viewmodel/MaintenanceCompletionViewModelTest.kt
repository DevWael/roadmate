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
import kotlinx.coroutines.test.TestDispatcher
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
@DisplayName("MaintenanceCompletionViewModel")
class MaintenanceCompletionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeMaintenanceDao: CompletionFakeMaintenanceDao
    private lateinit var maintenanceRepository: MaintenanceRepository
    private lateinit var viewModel: MaintenanceCompletionViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeMaintenanceDao = CompletionFakeMaintenanceDao()
        maintenanceRepository = MaintenanceRepository(fakeMaintenanceDao)
        viewModel = MaintenanceCompletionViewModel(maintenanceRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("initialize")
    inner class Initialize {

        @Test
        fun `sets form fields from schedule and vehicle`() {
            val schedule = testSchedule()
            viewModel.initialize(schedule, 90000.0, 1700000000000L)

            val form = currentForm()
            assertEquals("sched-1", form.scheduleId)
            assertEquals("veh-1", form.vehicleId)
            assertEquals(90000.0, form.vehicleOdometerKm, 0.01)
            assertEquals("90000", form.odometerKm)
            assertEquals(1700000000000L, form.datePerformed)
        }

        @Test
        fun `stores previous schedule values for undo`() {
            val schedule = testSchedule()
            viewModel.initialize(schedule, 90000.0, 1700000000000L)

            assertNotNull(viewModel.previousScheduleValues)
            assertEquals(80000.0, viewModel.previousScheduleValues!!.lastServiceKm, 0.01)
            assertEquals(1600000000000L, viewModel.previousScheduleValues!!.lastServiceDate)
        }
    }

    @Nested
    @DisplayName("field updates")
    inner class FieldUpdates {

        @BeforeEach
        fun initForm() {
            viewModel.initialize(testSchedule(), 90000.0, 1700000000000L)
        }

        @Test
        fun `updateOdometerKm changes odometer`() {
            viewModel.updateOdometerKm("95000")
            assertEquals("95000", currentForm().odometerKm)
        }

        @Test
        fun `updateDate changes date`() {
            viewModel.updateDate(1800000000000L)
            assertEquals(1800000000000L, currentForm().datePerformed)
        }

        @Test
        fun `updateCost changes cost`() {
            viewModel.updateCost("75.50")
            assertEquals("75.50", currentForm().cost)
        }

        @Test
        fun `updateLocation changes location`() {
            viewModel.updateLocation("Dealer")
            assertEquals("Dealer", currentForm().location)
        }

        @Test
        fun `updateNotes changes notes`() {
            viewModel.updateNotes("Full synthetic")
            assertEquals("Full synthetic", currentForm().notes)
        }
    }

    @Nested
    @DisplayName("save")
    inner class Save {

        @BeforeEach
        fun initForm() {
            viewModel.initialize(testSchedule(), 90000.0, 1700000000000L)
        }

        @Test
        fun `save with valid form creates record and updates schedule`() = runTest {
            viewModel.updateOdometerKm("95000")
            var saveResult: SaveResult? = null
            viewModel.save { saveResult = it }

            assertNotNull(saveResult)
            assertEquals(1, fakeMaintenanceDao.records.size)
            val record = fakeMaintenanceDao.records.values.first()
            assertEquals("sched-1", record.scheduleId)
            assertEquals("veh-1", record.vehicleId)
            assertEquals(95000.0, record.odometerKm, 0.01)
        }

        @Test
        fun `save updates schedule lastServiceKm and lastServiceDate`() = runTest {
            viewModel.updateOdometerKm("95000")
            viewModel.save {}

            val schedule = fakeMaintenanceDao.schedules["sched-1"]
            assertNotNull(schedule)
            assertEquals(95000.0, schedule!!.lastServiceKm, 0.01)
        }

        @Test
        fun `save with invalid form returns null`() = runTest {
            viewModel.updateOdometerKm("abc")
            var saveResult: SaveResult? = SaveResult("x", PreviousScheduleValues(0.0, 0L))
            viewModel.save { saveResult = it }
            assertNull(saveResult)
        }

        @Test
        fun `save with odometer less than vehicle returns null`() = runTest {
            viewModel.updateOdometerKm("80000")
            var saveResult: SaveResult? = SaveResult("x", PreviousScheduleValues(0.0, 0L))
            viewModel.save { saveResult = it }
            assertNull(saveResult)
        }

        @Test
        fun `save result contains record id`() = runTest {
            viewModel.updateOdometerKm("95000")
            var saveResult: SaveResult? = null
            viewModel.save { saveResult = it }
            assertNotNull(saveResult)
            assertNotNull(saveResult!!.recordId)
        }
    }

    @Nested
    @DisplayName("undo")
    inner class Undo {

        @Test
        fun `undo deletes record and reverts schedule`() = runTest {
            viewModel.initialize(testSchedule(), 90000.0, 1700000000000L)
            viewModel.updateOdometerKm("95000")
            var saveResult: SaveResult? = null
            viewModel.save { saveResult = it }

            viewModel.undo(saveResult!!)

            assertTrue(fakeMaintenanceDao.records.isEmpty())
            val schedule = fakeMaintenanceDao.schedules["sched-1"]
            assertEquals(80000.0, schedule!!.lastServiceKm, 0.01)
            assertEquals(1600000000000L, schedule.lastServiceDate)
        }
    }

    private fun currentForm(): MaintenanceCompletionFormState {
        val state = viewModel.uiState.value
        return (state as? UiState.Success)?.data ?: MaintenanceCompletionFormState()
    }

    private fun testSchedule() = MaintenanceSchedule(
        id = "sched-1",
        vehicleId = "veh-1",
        name = "Oil Change",
        intervalKm = 10000,
        intervalMonths = 6,
        lastServiceKm = 80000.0,
        lastServiceDate = 1600000000000L,
        isCustom = false,
    )
}

private class CompletionFakeMaintenanceDao : MaintenanceDao() {
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

    override suspend fun getSchedulesModifiedSince(since: Long): List<MaintenanceSchedule> =
        schedules.values.filter { it.lastModified > since }

    override suspend fun getRecordsModifiedSince(since: Long): List<MaintenanceRecord> =
        records.values.filter { it.lastModified > since }

    override suspend fun getScheduleById(id: String): MaintenanceSchedule? = schedules[id]

    override suspend fun getRecordById(id: String): MaintenanceRecord? = records[id]
}
