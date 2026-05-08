package com.roadmate.core.repository

import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.database.entity.Vehicle
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
 * Unit tests for [VehicleRepository].
 * Uses a fake DAO to verify repository delegates correctly and wraps errors in Result.
 */
class VehicleRepositoryTest {

    private lateinit var fakeDao: FakeVehicleDao
    private lateinit var repository: VehicleRepository

    @BeforeEach
    fun setup() {
        fakeDao = FakeVehicleDao()
        repository = VehicleRepository(fakeDao)
    }

    @Test
    fun `saveVehicle delegates to dao upsert and returns success`() = runTest {
        val vehicle = createTestVehicle()
        val result = repository.saveVehicle(vehicle)

        assertTrue(result.isSuccess)
        assertEquals(vehicle, fakeDao.vehicles[vehicle.id])
    }

    @Test
    fun `saveVehicle returns failure when dao throws`() = runTest {
        fakeDao.shouldThrow = true
        val result = repository.saveVehicle(createTestVehicle())

        assertTrue(result.isFailure)
    }

    @Test
    fun `getVehicle returns Flow from dao`() = runTest {
        val vehicle = createTestVehicle(id = "v-1")
        fakeDao.vehicles["v-1"] = vehicle
        fakeDao.updateFlow()

        val result = repository.getVehicle("v-1").first()
        assertEquals(vehicle, result)
    }

    @Test
    fun `getVehicle returns null for non-existent id`() = runTest {
        val result = repository.getVehicle("non-existent").first()
        assertNull(result)
    }

    @Test
    fun `getAllVehicles returns Flow from dao`() = runTest {
        val v1 = createTestVehicle(id = "v-1")
        val v2 = createTestVehicle(id = "v-2")
        fakeDao.vehicles["v-1"] = v1
        fakeDao.vehicles["v-2"] = v2
        fakeDao.updateFlow()

        val result = repository.getAllVehicles().first()
        assertEquals(2, result.size)
    }

    @Test
    fun `deleteVehicle delegates to dao and returns success`() = runTest {
        val vehicle = createTestVehicle(id = "v-1")
        fakeDao.vehicles["v-1"] = vehicle

        val result = repository.deleteVehicle(vehicle)
        assertTrue(result.isSuccess)
        assertNull(fakeDao.vehicles["v-1"])
    }

    @Test
    fun `deleteVehicleById delegates to dao and returns success`() = runTest {
        val vehicle = createTestVehicle(id = "v-1")
        fakeDao.vehicles["v-1"] = vehicle

        val result = repository.deleteVehicleById("v-1")
        assertTrue(result.isSuccess)
        assertNull(fakeDao.vehicles["v-1"])
    }

    @Test
    fun `getVehicleCount returns count from dao`() = runTest {
        fakeDao.vehicles["v-1"] = createTestVehicle(id = "v-1")
        fakeDao.vehicles["v-2"] = createTestVehicle(id = "v-2")
        fakeDao.updateFlow()

        val count = repository.getVehicleCount().first()
        assertEquals(2, count)
    }

    @Test
    fun `saveVehicles delegates batch to dao`() = runTest {
        val vehicles = listOf(
            createTestVehicle(id = "v-1"),
            createTestVehicle(id = "v-2"),
        )
        val result = repository.saveVehicles(vehicles)

        assertTrue(result.isSuccess)
        assertEquals(2, fakeDao.vehicles.size)
    }

    private fun createTestVehicle(id: String = "test-id"): Vehicle = Vehicle(
        id = id,
        name = "Test Car",
        make = "Mitsubishi",
        model = "Lancer EX",
        year = 2015,
        engineType = EngineType.INLINE_4,
        engineSize = 1.6,
        fuelType = FuelType.GASOLINE,
        plateNumber = "TEST-001",
        odometerKm = 85000.0,
        odometerUnit = OdometerUnit.KM,
        cityConsumption = 9.5,
        highwayConsumption = 6.8,
    )
}

/**
 * Fake implementation of [VehicleDao] for unit testing.
 */
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
        if (shouldThrow) throw RuntimeException("Test error")
        vehicles.remove(vehicle.id)
        updateFlow()
    }

    override suspend fun deleteById(vehicleId: String) {
        if (shouldThrow) throw RuntimeException("Test error")
        vehicles.remove(vehicleId)
        updateFlow()
    }

    override fun getVehicleCount(): Flow<Int> =
        flow.map { it.size }
}
