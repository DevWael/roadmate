package com.roadmate.core.util

import com.roadmate.core.database.dao.TripDao
import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.repository.TripRepository
import com.roadmate.core.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CrashRecoveryManager")
class CrashRecoveryManagerTest {

    private lateinit var fakeJournal: FakeCrashRecoveryJournal
    private lateinit var fakeTripDao: FakeTripDaoForRecovery
    private lateinit var fakeVehicleDao: FakeVehicleDaoForRecovery
    private lateinit var tripRepository: TripRepository
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var manager: CrashRecoveryManager

    @BeforeEach
    fun setup() {
        fakeJournal = FakeCrashRecoveryJournal()
        fakeTripDao = FakeTripDaoForRecovery()
        fakeVehicleDao = FakeVehicleDaoForRecovery()
        tripRepository = TripRepository(fakeTripDao)
        vehicleRepository = VehicleRepository(fakeVehicleDao)
        manager = CrashRecoveryManager(
            journal = fakeJournal,
            tripRepository = tripRepository,
            vehicleRepository = vehicleRepository,
        )
    }

    @Nested
    @DisplayName("recover")
    inner class Recover {

        @Test
        @DisplayName("does nothing when journal is empty")
        fun noJournalEntry() = runTest {
            manager.recover()
            assertFalse(fakeJournal.wasCleared())
        }

        @Test
        @DisplayName("clears stale journal when trip is not found in Room")
        fun tripNotInRoom() = runTest {
            fakeJournal.writeEntry(JournalEntry(
                tripId = "trip-1",
                vehicleId = "veh-1",
                distanceKm = 10.0,
                durationMs = 1000L,
                odometerKm = 85010.0,
                lastFlushTimestamp = 100L,
                status = "ACTIVE",
            ))
            fakeTripDao.trips.clear()
            fakeTripDao.updateFlow()

            manager.recover()

            assertEquals(0, fakeTripDao.trips.size)
            assertTrue(fakeJournal.wasCleared())
        }

        @Test
        @DisplayName("finalizes active trip as INTERRUPTED")
        fun finalizesAsInterrupted() = runTest {
            val trip = createActiveTrip()
            fakeTripDao.trips["trip-1"] = trip
            fakeTripDao.updateFlow()
            fakeJournal.writeEntry(JournalEntry(
                tripId = "trip-1",
                vehicleId = "veh-1",
                distanceKm = 10.0,
                durationMs = 1000L,
                odometerKm = 85010.0,
                lastFlushTimestamp = 2000L,
                status = "ACTIVE",
            ))

            manager.recover()

            val saved = fakeTripDao.trips["trip-1"]!!
            assertEquals(TripStatus.INTERRUPTED, saved.status)
            assertEquals(2000L, saved.endTime)
        }

        @Test
        @DisplayName("updates vehicle odometer from journal")
        fun updatesVehicleOdometer() = runTest {
            val trip = createActiveTrip()
            fakeTripDao.trips["trip-1"] = trip
            fakeTripDao.updateFlow()
            fakeVehicleDao.vehicle = Vehicle(
                id = "veh-1",
                name = "Test Car",
                make = "Toyota",
                model = "Corolla",
                year = 2020,
                engineType = EngineType.INLINE_4,
                engineSize = 1.8,
                fuelType = FuelType.GASOLINE,
                plateNumber = "ABC123",
                odometerKm = 85000.0,
                odometerUnit = OdometerUnit.KM,
                cityConsumption = 8.0,
                highwayConsumption = 6.0,
            )
            fakeVehicleDao.updateFlow()
            fakeJournal.writeEntry(JournalEntry(
                tripId = "trip-1",
                vehicleId = "veh-1",
                distanceKm = 10.0,
                durationMs = 1000L,
                odometerKm = 85010.0,
                lastFlushTimestamp = 2000L,
                status = "ACTIVE",
            ))

            manager.recover()

            val updatedVehicle = fakeVehicleDao.vehicle
            assertEquals(85010.0, updatedVehicle!!.odometerKm, 0.001)
        }

        @Test
        @DisplayName("clears journal after successful recovery")
        fun clearsJournal() = runTest {
            val trip = createActiveTrip()
            fakeTripDao.trips["trip-1"] = trip
            fakeTripDao.updateFlow()
            fakeJournal.writeEntry(JournalEntry(
                tripId = "trip-1",
                vehicleId = "veh-1",
                distanceKm = 10.0,
                durationMs = 1000L,
                odometerKm = 85010.0,
                lastFlushTimestamp = 2000L,
                status = "ACTIVE",
            ))

            manager.recover()

            assertTrue(fakeJournal.wasCleared())
        }

        @Test
        @DisplayName("uses journal distance and duration for finalized trip")
        fun usesJournalValues() = runTest {
            val trip = createActiveTrip()
            fakeTripDao.trips["trip-1"] = trip
            fakeTripDao.updateFlow()
            fakeJournal.writeEntry(JournalEntry(
                tripId = "trip-1",
                vehicleId = "veh-1",
                distanceKm = 15.5,
                durationMs = 2000L,
                odometerKm = 85015.5,
                lastFlushTimestamp = 3000L,
                status = "ACTIVE",
            ))

            manager.recover()

            val saved = fakeTripDao.trips["trip-1"]!!
            assertEquals(15.5, saved.distanceKm, 0.001)
            assertEquals(2000L, saved.durationMs)
            assertEquals(85015.5, saved.endOdometerKm, 0.001)
        }

        @Test
        @DisplayName("clears journal but does not recover trip that is already COMPLETED")
        fun doesNotRecoverCompletedTrip() = runTest {
            val trip = createActiveTrip().copy(status = TripStatus.COMPLETED)
            fakeTripDao.trips["trip-1"] = trip
            fakeTripDao.updateFlow()
            fakeJournal.writeEntry(JournalEntry(
                tripId = "trip-1",
                vehicleId = "veh-1",
                distanceKm = 10.0,
                durationMs = 1000L,
                odometerKm = 85010.0,
                lastFlushTimestamp = 2000L,
                status = "ACTIVE",
            ))

            manager.recover()

            val saved = fakeTripDao.trips["trip-1"]!!
            assertEquals(TripStatus.COMPLETED, saved.status)
            assertTrue(fakeJournal.wasCleared())
        }
    }

    private fun createActiveTrip(
        id: String = "trip-1",
        vehicleId: String = "veh-1",
    ): Trip = Trip(
        id = id,
        vehicleId = vehicleId,
        startTime = 1000L,
        distanceKm = 0.0,
        durationMs = 0L,
        maxSpeedKmh = 0.0,
        avgSpeedKmh = 0.0,
        estimatedFuelL = 0.0,
        startOdometerKm = 85000.0,
        endOdometerKm = 85000.0,
        status = TripStatus.ACTIVE,
    )
}

private class FakeCrashRecoveryJournal : CrashRecoveryJournal(
    FakeDataStoreHelper().dataStore
) {
    private var entry: JournalEntry? = null
    private var cleared = false

    fun writeEntry(e: JournalEntry) {
        entry = e
    }

    fun wasCleared(): Boolean = cleared

    override suspend fun read(): JournalEntry? = entry

    override suspend fun hasActiveTrip(): Boolean = entry != null

    override suspend fun clear() {
        cleared = true
        entry = null
    }
}

private class FakeDataStoreHelper {
    private val _prefs = MutableStateFlow<androidx.datastore.preferences.core.Preferences>(
        androidx.datastore.preferences.core.emptyPreferences()
    )
    val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> =
        object : androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
            override val data = _prefs
            override suspend fun updateData(
                transform: suspend (t: androidx.datastore.preferences.core.Preferences) ->
                androidx.datastore.preferences.core.Preferences,
            ): androidx.datastore.preferences.core.Preferences {
                val new = transform(_prefs.value)
                _prefs.value = new
                return new
            }
        }
}

private class FakeTripDaoForRecovery : TripDao() {
    val trips = mutableMapOf<String, Trip>()
    var shouldThrow = false

    private val tripFlow = MutableStateFlow<List<Trip>>(emptyList())
    private val tripPointFlow = MutableStateFlow<List<TripPoint>>(emptyList())

    fun updateFlow() {
        tripFlow.value = trips.values.toList()
        tripPointFlow.value = emptyList()
    }

    override fun getTripsForVehicle(vehicleId: String): Flow<List<Trip>> =
        tripFlow.map { list -> list.filter { it.vehicleId == vehicleId }.sortedByDescending { it.startTime } }

    override fun getTripPointsForTrip(tripId: String): Flow<List<TripPoint>> =
        tripPointFlow.map { list -> list.filter { it.tripId == tripId }.sortedBy { it.timestamp } }

    override fun getActiveTrip(vehicleId: String): Flow<Trip?> =
        tripFlow.map { list -> list.find { it.vehicleId == vehicleId && it.status == TripStatus.ACTIVE } }

    override fun getTrip(tripId: String): Flow<Trip?> =
        tripFlow.map { list -> list.find { it.id == tripId } }

    override suspend fun upsertTrip(trip: Trip) {
        if (shouldThrow) throw RuntimeException("Test error")
        trips[trip.id] = trip
        updateFlow()
    }

    override suspend fun upsertTrips(trips: List<Trip>) {
        if (shouldThrow) throw RuntimeException("Test error")
        trips.forEach { this.trips[it.id] = it }
        updateFlow()
    }

    override suspend fun deleteTrip(trip: Trip) {
        trips.remove(trip.id)
        updateFlow()
    }

    override suspend fun deleteTripById(tripId: String) {
        trips.remove(tripId)
        updateFlow()
    }

    override suspend fun upsertTripPoint(tripPoint: TripPoint) {}
    override suspend fun upsertTripPoints(tripPoints: List<TripPoint>) {}
    override suspend fun deleteTripPoint(tripPoint: TripPoint) {}
}

private class FakeVehicleDaoForRecovery : VehicleDao {
    var vehicle: Vehicle? = null
    var savedVehicle: Vehicle? = null
    private val vehicleFlow = MutableStateFlow<List<Vehicle>>(emptyList())

    fun updateFlow() {
        vehicleFlow.value = listOfNotNull(vehicle)
    }

    override fun getVehicle(vehicleId: String): Flow<Vehicle?> =
        vehicleFlow.map { list -> list.find { it.id == vehicleId } }

    override fun getAllVehicles(): Flow<List<Vehicle>> = vehicleFlow

    override suspend fun upsert(vehicle: Vehicle) {
        savedVehicle = vehicle
        this.vehicle = vehicle
        updateFlow()
    }

    override suspend fun upsertAll(vehicles: List<Vehicle>) {
        vehicles.forEach { upsert(it) }
    }

    override suspend fun delete(vehicle: Vehicle) {
        if (this.vehicle?.id == vehicle.id) this.vehicle = null
        updateFlow()
    }

    override suspend fun deleteById(vehicleId: String) {
        if (this.vehicle?.id == vehicleId) this.vehicle = null
        updateFlow()
    }

    override fun getVehicleCount(): Flow<Int> = vehicleFlow.map { it.size }

    override suspend fun addToOdometer(vehicleId: String, distanceKm: Double, lastModified: Long) {
        val v = vehicle ?: return
        vehicle = v.copy(odometerKm = v.odometerKm + distanceKm, lastModified = lastModified)
        updateFlow()
    }
}
