package com.roadmate.core.repository

import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [MaintenanceRepository].
 * Uses a fake DAO to verify repository delegates correctly and wraps errors in Result.
 */
class MaintenanceRepositoryTest {

    private lateinit var fakeDao: FakeMaintenanceDao
    private lateinit var repository: MaintenanceRepository

    @BeforeEach
    fun setup() {
        fakeDao = FakeMaintenanceDao()
        repository = MaintenanceRepository(fakeDao)
    }

    // --- Schedule tests ---

    @Test
    fun `saveSchedule delegates and returns success`() = runTest {
        val schedule = createTestSchedule()
        val result = repository.saveSchedule(schedule)

        assertTrue(result.isSuccess)
        assertEquals(schedule, fakeDao.schedules[schedule.id])
    }

    @Test
    fun `saveSchedule returns failure when dao throws`() = runTest {
        fakeDao.shouldThrow = true
        val result = repository.saveSchedule(createTestSchedule())
        assertTrue(result.isFailure)
    }

    @Test
    fun `getSchedulesForVehicle returns Flow from dao`() = runTest {
        val s1 = createTestSchedule(id = "s-1", vehicleId = "v-1")
        val s2 = createTestSchedule(id = "s-2", vehicleId = "v-1")
        val s3 = createTestSchedule(id = "s-3", vehicleId = "v-2")
        fakeDao.schedules["s-1"] = s1
        fakeDao.schedules["s-2"] = s2
        fakeDao.schedules["s-3"] = s3
        fakeDao.updateScheduleFlow()

        val result = repository.getSchedulesForVehicle("v-1").first()
        assertEquals(2, result.size)
    }

    @Test
    fun `getSchedule returns single schedule`() = runTest {
        val schedule = createTestSchedule(id = "s-1")
        fakeDao.schedules["s-1"] = schedule
        fakeDao.updateScheduleFlow()

        val result = repository.getSchedule("s-1").first()
        assertEquals(schedule, result)
    }

    @Test
    fun `getSchedule returns null for non-existent`() = runTest {
        val result = repository.getSchedule("non-existent").first()
        assertNull(result)
    }

    @Test
    fun `deleteSchedule delegates and returns success`() = runTest {
        val schedule = createTestSchedule(id = "s-1")
        fakeDao.schedules["s-1"] = schedule

        val result = repository.deleteSchedule(schedule)
        assertTrue(result.isSuccess)
        assertNull(fakeDao.schedules["s-1"])
    }

    @Test
    fun `saveSchedules delegates batch and returns success`() = runTest {
        val schedules = listOf(
            createTestSchedule(id = "s-1"),
            createTestSchedule(id = "s-2"),
        )
        val result = repository.saveSchedules(schedules)

        assertTrue(result.isSuccess)
        assertEquals(2, fakeDao.schedules.size)
    }

    // --- Record tests ---

    @Test
    fun `saveRecord delegates and returns success`() = runTest {
        val record = createTestRecord()
        val result = repository.saveRecord(record)

        assertTrue(result.isSuccess)
        assertEquals(record, fakeDao.records[record.id])
    }

    @Test
    fun `saveRecord returns failure when dao throws`() = runTest {
        fakeDao.shouldThrow = true
        val result = repository.saveRecord(createTestRecord())
        assertTrue(result.isFailure)
    }

    @Test
    fun `getRecordsForVehicle returns Flow from dao`() = runTest {
        val r1 = createTestRecord(id = "r-1", vehicleId = "v-1")
        val r2 = createTestRecord(id = "r-2", vehicleId = "v-1")
        fakeDao.records["r-1"] = r1
        fakeDao.records["r-2"] = r2
        fakeDao.updateRecordFlow()

        val result = repository.getRecordsForVehicle("v-1").first()
        assertEquals(2, result.size)
    }

    @Test
    fun `getRecordsForSchedule returns scoped results`() = runTest {
        val r1 = createTestRecord(id = "r-1", scheduleId = "s-1")
        val r2 = createTestRecord(id = "r-2", scheduleId = "s-2")
        fakeDao.records["r-1"] = r1
        fakeDao.records["r-2"] = r2
        fakeDao.updateRecordFlow()

        val result = repository.getRecordsForSchedule("s-1").first()
        assertEquals(1, result.size)
        assertEquals("r-1", result.first().id)
    }

    @Test
    fun `deleteRecord delegates and returns success`() = runTest {
        val record = createTestRecord(id = "r-1")
        fakeDao.records["r-1"] = record

        val result = repository.deleteRecord(record)
        assertTrue(result.isSuccess)
        assertNull(fakeDao.records["r-1"])
    }

    @Test
    fun `deleteScheduleById delegates and returns success`() = runTest {
        val schedule = createTestSchedule(id = "s-1")
        fakeDao.schedules["s-1"] = schedule

        val result = repository.deleteScheduleById("s-1")
        assertTrue(result.isSuccess)
        assertNull(fakeDao.schedules["s-1"])
    }

    @Test
    fun `deleteRecordById delegates and returns success`() = runTest {
        val record = createTestRecord(id = "r-1")
        fakeDao.records["r-1"] = record

        val result = repository.deleteRecordById("r-1")
        assertTrue(result.isSuccess)
        assertNull(fakeDao.records["r-1"])
    }

    // --- Transactional operation tests ---

    @Test
    fun `completeMaintenance creates record and updates schedule`() = runTest {
        val schedule = createTestSchedule(id = "s-1", vehicleId = "v-1")
        fakeDao.schedules["s-1"] = schedule
        fakeDao.updateScheduleFlow()

        val record = createTestRecord(id = "r-1", scheduleId = "s-1", vehicleId = "v-1")
        val updatedSchedule = schedule.copy(lastServiceKm = record.odometerKm, lastServiceDate = record.datePerformed)

        val result = repository.completeMaintenance(record, updatedSchedule)
        assertTrue(result.isSuccess)
        assertEquals(record, fakeDao.records["r-1"])
        assertEquals(updatedSchedule, fakeDao.schedules["s-1"])
    }

    @Test
    fun `completeMaintenance returns failure when dao throws`() = runTest {
        fakeDao.shouldThrow = true
        val record = createTestRecord()
        val schedule = createTestSchedule()

        val result = repository.completeMaintenance(record, schedule)
        assertTrue(result.isFailure)
    }

    @Test
    fun `undoCompletion deletes record and reverts schedule`() = runTest {
        val schedule = createTestSchedule(id = "s-1")
        val originalSchedule = schedule.copy(lastServiceKm = 80000.0)
        fakeDao.schedules["s-1"] = schedule.copy(lastServiceKm = 90000.0)
        fakeDao.updateScheduleFlow()

        val record = createTestRecord(id = "r-1", scheduleId = "s-1")
        fakeDao.records["r-1"] = record
        fakeDao.updateRecordFlow()

        val result = repository.undoCompletion("r-1", originalSchedule)
        assertTrue(result.isSuccess)
        assertNull(fakeDao.records["r-1"])
        assertEquals(80000.0, fakeDao.schedules["s-1"]!!.lastServiceKm, 0.01)
    }

    @Test
    fun `undoCompletion returns failure when dao throws`() = runTest {
        fakeDao.shouldThrow = true
        val schedule = createTestSchedule()

        val result = repository.undoCompletion("r-1", schedule)
        assertTrue(result.isFailure)
    }

    private fun createTestSchedule(
        id: String = "sched-1",
        vehicleId: String = "vehicle-1",
    ): MaintenanceSchedule = MaintenanceSchedule(
        id = id,
        vehicleId = vehicleId,
        name = "Oil Change",
        intervalKm = 10_000,
        intervalMonths = 6,
        lastServiceKm = 80000.0,
        lastServiceDate = System.currentTimeMillis(),
        isCustom = false,
    )

    private fun createTestRecord(
        id: String = "rec-1",
        scheduleId: String = "sched-1",
        vehicleId: String = "vehicle-1",
    ): MaintenanceRecord = MaintenanceRecord(
        id = id,
        scheduleId = scheduleId,
        vehicleId = vehicleId,
        datePerformed = System.currentTimeMillis(),
        odometerKm = 90000.0,
    )
}

/**
 * Fake implementation of [MaintenanceDao] for unit testing.
 */
private class FakeMaintenanceDao : MaintenanceDao() {
    val schedules = mutableMapOf<String, MaintenanceSchedule>()
    val records = mutableMapOf<String, MaintenanceRecord>()
    var shouldThrow = false

    private val scheduleFlow = MutableStateFlow<List<MaintenanceSchedule>>(emptyList())
    private val recordFlow = MutableStateFlow<List<MaintenanceRecord>>(emptyList())

    fun updateScheduleFlow() {
        scheduleFlow.value = schedules.values.toList()
    }

    fun updateRecordFlow() {
        recordFlow.value = records.values.toList()
    }

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
}
