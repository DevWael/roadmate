package com.roadmate.core.repository

import com.roadmate.core.database.dao.FuelDao
import com.roadmate.core.database.entity.FuelLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [FuelRepository].
 * Uses a fake DAO to verify repository delegates correctly and wraps errors in Result.
 */
class FuelRepositoryTest {

    private lateinit var fakeDao: FakeFuelDao
    private lateinit var repository: FuelRepository

    @BeforeEach
    fun setup() {
        fakeDao = FakeFuelDao()
        repository = FuelRepository(fakeDao)
    }

    @Test
    fun `saveFuelLog delegates to dao upsert and returns success`() = runTest {
        val log = createTestFuelLog()
        val result = repository.saveFuelLog(log)

        assertTrue(result.isSuccess)
        assertEquals(log, fakeDao.fuelLogs[log.id])
    }

    @Test
    fun `saveFuelLog returns failure when dao throws`() = runTest {
        fakeDao.shouldThrow = true
        val result = repository.saveFuelLog(createTestFuelLog())

        assertTrue(result.isFailure)
    }

    @Test
    fun `getFuelLogsForVehicle returns Flow from dao`() = runTest {
        val log = createTestFuelLog(id = "f-1", vehicleId = "v-1")
        fakeDao.fuelLogs["f-1"] = log
        fakeDao.updateFlow()

        val result = repository.getFuelLogsForVehicle("v-1").first()
        assertEquals(1, result.size)
        assertEquals(log, result[0])
    }

    @Test
    fun `getLastFullTankEntry returns last full tank`() = runTest {
        val fullTank = createTestFuelLog(id = "f-1", vehicleId = "v-1", isFullTank = true)
        fakeDao.fuelLogs["f-1"] = fullTank
        fakeDao.updateFlow()

        val result = repository.getLastFullTankEntry("v-1").first()
        assertNotNull(result)
        assertEquals(fullTank, result)
    }

    @Test
    fun `getLastFullTankEntry returns null when no full tank entries`() = runTest {
        val partial = createTestFuelLog(id = "f-1", vehicleId = "v-1", isFullTank = false)
        fakeDao.fuelLogs["f-1"] = partial
        fakeDao.updateFlow()

        val result = repository.getLastFullTankEntry("v-1").first()
        assertNull(result)
    }

    @Test
    fun `deleteFuelLog delegates to dao and returns success`() = runTest {
        val log = createTestFuelLog(id = "f-1")
        fakeDao.fuelLogs["f-1"] = log

        val result = repository.deleteFuelLog(log)
        assertTrue(result.isSuccess)
        assertNull(fakeDao.fuelLogs["f-1"])
    }

    @Test
    fun `deleteFuelLogById delegates to dao and returns success`() = runTest {
        val log = createTestFuelLog(id = "f-1")
        fakeDao.fuelLogs["f-1"] = log

        val result = repository.deleteFuelLogById("f-1")
        assertTrue(result.isSuccess)
        assertNull(fakeDao.fuelLogs["f-1"])
    }

    private fun createTestFuelLog(
        id: String = "test-id",
        vehicleId: String = "v-1",
        isFullTank: Boolean = true,
    ): FuelLog = FuelLog(
        id = id,
        vehicleId = vehicleId,
        date = System.currentTimeMillis(),
        odometerKm = 86000.0,
        liters = 45.0,
        pricePerLiter = 12.75,
        totalCost = 573.75,
        isFullTank = isFullTank,
    )
}

/**
 * Fake implementation of [FuelDao] for unit testing.
 */
private class FakeFuelDao : FuelDao {
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
