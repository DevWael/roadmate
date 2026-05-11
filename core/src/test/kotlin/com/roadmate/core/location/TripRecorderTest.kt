package com.roadmate.core.location

import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.model.DrivingState
import com.roadmate.core.repository.TripRepository
import com.roadmate.core.repository.VehicleRepository
import com.roadmate.core.state.DrivingStateManager
import com.roadmate.core.state.TripEndEvent
import com.roadmate.core.util.Clock
import com.roadmate.core.util.CrashRecoveryJournal
import com.roadmate.core.util.HaversineCalculator
import com.roadmate.core.util.JournalEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@DisplayName("TripRecorder")
class TripRecorderTest {

    private lateinit var drivingStateManager: DrivingStateManager
    private lateinit var fakeTripDao: FakeTripDaoForRecorder
    private lateinit var tripRepository: TripRepository
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var fakeLocations: MutableSharedFlow<LocationUpdate>
    private lateinit var fakeTripEndEvents: MutableSharedFlow<TripEndEvent>
    private lateinit var fakeClock: FakeClock
    private lateinit var fakeJournal: FakeJournalForRecorder
    private lateinit var fakeVehicleDao: FakeVehicleDaoForRecorder
    private var recorderJob: Job? = null

    @BeforeEach
    fun setUp() {
        drivingStateManager = DrivingStateManager()
        fakeTripDao = FakeTripDaoForRecorder()
        tripRepository = TripRepository(fakeTripDao)
        fakeVehicleDao = FakeVehicleDaoForRecorder()
        vehicleRepository = VehicleRepository(fakeVehicleDao)
        fakeLocations = MutableSharedFlow(replay = 0, extraBufferCapacity = 64)
        fakeTripEndEvents = MutableSharedFlow(extraBufferCapacity = 1)
        fakeClock = FakeClock(1000L)
        fakeJournal = FakeJournalForRecorder()
    }

    @AfterEach
    fun tearDown() {
        recorderJob?.cancel()
    }

    private fun createRecorder(testScope: TestScope): TripRecorder {
        val job = SupervisorJob()
        recorderJob = job
        val scope = CoroutineScope(job + UnconfinedTestDispatcher())

        return TripRecorder(
            locationUpdates = fakeLocations,
            drivingStateFlow = drivingStateManager.drivingState,
            tripEndEventFlow = fakeTripEndEvents,
            tripRepository = tripRepository,
            vehicleRepository = vehicleRepository,
            journal = fakeJournal,
            clock = fakeClock,
            scope = scope,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    private fun locationUpdate(
        lat: Double = 37.7749,
        lng: Double = -122.4194,
        speedKmh: Float = 0f,
        accuracy: Float = 10f,
        altitude: Double = 50.0,
        timestamp: Long = fakeClock.now(),
    ) = LocationUpdate(
        lat = lat,
        lng = lng,
        speedKmh = speedKmh,
        altitude = altitude,
        accuracy = accuracy,
        timestamp = timestamp,
        isLowAccuracy = accuracy > HaversineCalculator.LOW_ACCURACY_THRESHOLD,
    )

    private fun createActiveTrip(tripId: String = "trip-1"): Trip {
        val trip = Trip(
            id = tripId,
            vehicleId = "vehicle-1",
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
        fakeTripDao.trips[tripId] = trip
        fakeTripDao.updateFlow()
        return trip
    }

    private fun setupVehicle() {
        val vehicle = Vehicle(
            id = "vehicle-1",
            name = "Test Car",
            make = "Toyota",
            model = "Camry",
            year = 2020,
            engineType = com.roadmate.core.database.entity.EngineType.INLINE_4,
            engineSize = 2.5,
            fuelType = com.roadmate.core.database.entity.FuelType.GASOLINE,
            plateNumber = "ABC123",
            odometerKm = 85000.0,
            odometerUnit = com.roadmate.core.database.entity.OdometerUnit.KM,
            cityConsumption = 8.0,
            highwayConsumption = 6.0,
        )
        fakeVehicleDao.vehicles["vehicle-1"] = vehicle
        fakeVehicleDao.updateFlow()
    }

    @Nested
    @DisplayName("TripPoint creation (AC #1)")
    inner class TripPointCreation {

        @Test
        @DisplayName("creates TripPoint from location update during active trip")
        fun createsTripPointFromLocation() = runTest {
            setupVehicle()
            createActiveTrip()
            createRecorder(testScope = this)

            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))
            fakeLocations.emit(locationUpdate(speedKmh = 60f, timestamp = 2000L))

            fakeTripEndEvents.emit(TripEndEvent("trip-1", 3000L))

            val points = fakeTripDao.tripPoints.values.filter { it.tripId == "trip-1" }
            assertEquals(1, points.size)
            assertEquals(37.7749, points[0].latitude, 0.001)
            assertEquals(-122.4194, points[0].longitude, 0.001)
            assertEquals(60.0, points[0].speedKmh, 0.1)
            assertEquals(50.0, points[0].altitude, 0.1)
            assertEquals(10.0f, points[0].accuracy, 0.1f)
            assertEquals(2000L, points[0].timestamp)
        }

        @Test
        @DisplayName("ignores location updates when no active trip")
        fun ignoresWhenNoTrip() = runTest {
            setupVehicle()
            createRecorder(testScope = this)

            fakeLocations.emit(locationUpdate(speedKmh = 60f))

            assertTrue(fakeTripDao.tripPoints.isEmpty())
        }

        @Test
        @DisplayName("persists all points including low accuracy")
        fun persistsAllPoints() = runTest {
            setupVehicle()
            createActiveTrip()
            createRecorder(testScope = this)

            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))
            fakeLocations.emit(locationUpdate(accuracy = 10f, timestamp = 2000L))
            fakeLocations.emit(locationUpdate(accuracy = 60f, timestamp = 3000L))
            fakeLocations.emit(locationUpdate(accuracy = 10f, timestamp = 4000L))

            fakeTripEndEvents.emit(TripEndEvent("trip-1", 5000L))

            val points = fakeTripDao.tripPoints.values.filter { it.tripId == "trip-1" }
            assertEquals(3, points.size)
        }
    }

    @Nested
    @DisplayName("Distance calculation (AC #3)")
    inner class DistanceCalculation {

        @Test
        @DisplayName("calculates distance between consecutive valid points")
        fun calculatesDistance() = runTest {
            setupVehicle()
            createActiveTrip()
            createRecorder(testScope = this)

            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))
            fakeLocations.emit(locationUpdate(lat = 37.7749, lng = -122.4194, speedKmh = 60f, timestamp = 2000L))
            fakeLocations.emit(locationUpdate(lat = 37.7849, lng = -122.4194, speedKmh = 60f, timestamp = 3000L))

            fakeClock.setTime(4000L)
            fakeTripEndEvents.emit(TripEndEvent("trip-1", 4000L))

            val trip = fakeTripDao.trips["trip-1"]
            assertNotNull(trip)
            assertTrue(trip!!.distanceKm > 0.0, "Expected positive distance, got ${trip.distanceKm}")
        }

        @Test
        @DisplayName("excludes low accuracy points from distance calculation")
        fun excludesLowAccuracyFromDistance() = runTest {
            setupVehicle()
            createActiveTrip()
            createRecorder(testScope = this)

            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))

            fakeLocations.emit(locationUpdate(lat = 37.7749, lng = -122.4194, accuracy = 10f, timestamp = 2000L))
            fakeLocations.emit(locationUpdate(lat = 37.7750, lng = -122.4195, accuracy = 60f, timestamp = 3000L))
            fakeLocations.emit(locationUpdate(lat = 37.7751, lng = -122.4196, accuracy = 10f, timestamp = 4000L))

            fakeClock.setTime(5000L)
            fakeTripEndEvents.emit(TripEndEvent("trip-1", 5000L))

            val allPoints = fakeTripDao.tripPoints.values.filter { it.tripId == "trip-1" }
            assertEquals(3, allPoints.size, "All points should be persisted")

            val trip = fakeTripDao.trips["trip-1"]
            assertNotNull(trip)
            val expectedDistance = HaversineCalculator.haversineDistanceKm(
                37.7749, -122.4194, 37.7751, -122.4196
            )
            assertEquals(expectedDistance, trip!!.distanceKm, 0.0001,
                "Distance should skip low accuracy point")
        }

        @Test
        @DisplayName("first valid point contributes zero distance")
        fun firstPointZeroDistance() = runTest {
            setupVehicle()
            createActiveTrip()
            createRecorder(testScope = this)

            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))
            fakeLocations.emit(locationUpdate(speedKmh = 60f, timestamp = 2000L))

            fakeClock.setTime(3000L)
            fakeTripEndEvents.emit(TripEndEvent("trip-1", 3000L))

            val trip = fakeTripDao.trips["trip-1"]
            assertNotNull(trip)
            assertEquals(0.0, trip!!.distanceKm, 0.001)
        }
    }

    @Nested
    @DisplayName("Trip summary updates (AC #4)")
    inner class TripSummary {

        @Test
        @DisplayName("updates maxSpeedKmh from location updates")
        fun updatesMaxSpeed() = runTest {
            setupVehicle()
            createActiveTrip()
            createRecorder(testScope = this)

            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))
            fakeLocations.emit(locationUpdate(speedKmh = 60f, timestamp = 2000L))
            fakeLocations.emit(locationUpdate(speedKmh = 80f, timestamp = 3000L))
            fakeLocations.emit(locationUpdate(speedKmh = 50f, timestamp = 4000L))

            fakeClock.setTime(5000L)
            fakeTripEndEvents.emit(TripEndEvent("trip-1", 5000L))

            val trip = fakeTripDao.trips["trip-1"]
            assertNotNull(trip)
            assertEquals(80.0, trip!!.maxSpeedKmh, 0.1)
        }

        @Test
        @DisplayName("calculates avgSpeedKmh as distance over time")
        fun calculatesAvgSpeed() = runTest {
            setupVehicle()
            createActiveTrip()
            createRecorder(testScope = this)

            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))
            fakeLocations.emit(locationUpdate(lat = 37.7749, lng = -122.4194, speedKmh = 60f, timestamp = 2000L))
            fakeLocations.emit(locationUpdate(lat = 37.7849, lng = -122.4194, speedKmh = 60f, timestamp = 3601000L))

            fakeClock.setTime(3602000L)
            fakeTripEndEvents.emit(TripEndEvent("trip-1", 3602000L))

            val trip = fakeTripDao.trips["trip-1"]
            assertNotNull(trip)
            assertTrue(trip!!.avgSpeedKmh > 0.0, "avgSpeed should be positive")
            val expectedDuration = (3602000L - 1000L).toDouble()
            val expectedAvg = trip.distanceKm / (expectedDuration / 3_600_000.0)
            assertEquals(expectedAvg, trip.avgSpeedKmh, 0.1)
        }

        @Test
        @DisplayName("calculates estimatedFuelL from distance and cityConsumption")
        fun calculatesFuelEstimate() = runTest {
            setupVehicle()
            createActiveTrip()
            createRecorder(testScope = this)

            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))
            fakeLocations.emit(locationUpdate(lat = 37.7749, lng = -122.4194, speedKmh = 60f, timestamp = 2000L))
            fakeLocations.emit(locationUpdate(lat = 37.7849, lng = -122.4194, speedKmh = 60f, timestamp = 3000L))

            fakeClock.setTime(4000L)
            fakeTripEndEvents.emit(TripEndEvent("trip-1", 4000L))

            val trip = fakeTripDao.trips["trip-1"]
            assertNotNull(trip)
            val expectedFuel = trip!!.distanceKm * (8.0 / 100.0)
            assertEquals(expectedFuel, trip.estimatedFuelL, 0.001)
        }
    }

    @Nested
    @DisplayName("Trip finalization (AC #6)")
    inner class TripFinalization {

        @Test
        @DisplayName("sets endTime, status=COMPLETED, endOdometerKm on finalization")
        fun finalizesTripCorrectly() = runTest {
            setupVehicle()
            createActiveTrip()
            createRecorder(testScope = this)

            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))
            fakeLocations.emit(locationUpdate(lat = 37.7749, lng = -122.4194, speedKmh = 60f, timestamp = 2000L))
            fakeLocations.emit(locationUpdate(lat = 37.7849, lng = -122.4194, speedKmh = 60f, timestamp = 3000L))

            fakeClock.setTime(5000L)
            fakeTripEndEvents.emit(TripEndEvent("trip-1", 5000L))

            val trip = fakeTripDao.trips["trip-1"]
            assertNotNull(trip)
            assertEquals(TripStatus.COMPLETED, trip!!.status)
            assertEquals(5000L, trip.endTime)
            assertEquals(85000.0 + trip.distanceKm, trip.endOdometerKm, 0.001)
        }

        @Test
        @DisplayName("flushes remaining buffer on finalization")
        fun flushesRemainingBuffer() = runTest {
            setupVehicle()
            createActiveTrip()
            createRecorder(testScope = this)

            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))
            fakeLocations.emit(locationUpdate(speedKmh = 60f, timestamp = 2000L))
            fakeLocations.emit(locationUpdate(speedKmh = 60f, timestamp = 3000L))
            fakeLocations.emit(locationUpdate(speedKmh = 60f, timestamp = 4000L))

            fakeClock.setTime(5000L)
            fakeTripEndEvents.emit(TripEndEvent("trip-1", 5000L))

            val points = fakeTripDao.tripPoints.values.filter { it.tripId == "trip-1" }
            assertEquals(3, points.size)
        }

        @Test
        @DisplayName("does not finalize trip with wrong tripId")
        fun ignoresWrongTripId() = runTest {
            setupVehicle()
            createActiveTrip()
            createRecorder(testScope = this)

            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))
            fakeLocations.emit(locationUpdate(speedKmh = 60f, timestamp = 2000L))

            fakeTripEndEvents.emit(TripEndEvent("trip-2", 3000L))

            val trip = fakeTripDao.trips["trip-1"]
            assertNotNull(trip)
            assertEquals(TripStatus.ACTIVE, trip!!.status)
        }

        @Test
        @DisplayName("saves trip without points when buffer is empty")
        fun savesTripWithoutPoints() = runTest {
            setupVehicle()
            createActiveTrip()
            createRecorder(testScope = this)

            drivingStateManager.updateState(DrivingState.Driving("trip-1", 0.0, 0L))

            fakeClock.setTime(2000L)
            fakeTripEndEvents.emit(TripEndEvent("trip-1", 2000L))

            val trip = fakeTripDao.trips["trip-1"]
            assertNotNull(trip)
            assertEquals(TripStatus.COMPLETED, trip!!.status)
            assertEquals(0, fakeTripDao.tripPoints.values.count { it.tripId == "trip-1" })
        }
    }
}

private class FakeTripDaoForRecorder : com.roadmate.core.database.dao.TripDao() {
    val trips = mutableMapOf<String, Trip>()
    val tripPoints = mutableMapOf<String, TripPoint>()
    var shouldThrow = false

    private val tripFlow = MutableStateFlow<List<Trip>>(emptyList())
    private val tripPointFlow = MutableStateFlow<List<TripPoint>>(emptyList())

    fun updateFlow() {
        tripFlow.value = trips.values.toList()
        tripPointFlow.value = tripPoints.values.toList()
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
        if (shouldThrow) throw RuntimeException("Test error")
        trips.remove(trip.id)
        updateFlow()
    }

    override suspend fun deleteTripById(tripId: String) {
        if (shouldThrow) throw RuntimeException("Test error")
        trips.remove(tripId)
        updateFlow()
    }

    override suspend fun upsertTripPoint(tripPoint: TripPoint) {
        if (shouldThrow) throw RuntimeException("Test error")
        tripPoints[tripPoint.id] = tripPoint
        updateFlow()
    }

    override suspend fun upsertTripPoints(tripPoints: List<TripPoint>) {
        if (shouldThrow) throw RuntimeException("Test error")
        tripPoints.forEach { this.tripPoints[it.id] = it }
        updateFlow()
    }

    override suspend fun deleteTripPoint(tripPoint: TripPoint) {
        if (shouldThrow) throw RuntimeException("Test error")
        tripPoints.remove(tripPoint.id)
        updateFlow()
    }
}

private class FakeVehicleDaoForRecorder : VehicleDao {
    val vehicles = mutableMapOf<String, Vehicle>()
    var shouldThrow = false

    private val vehicleFlow = MutableStateFlow<List<Vehicle>>(emptyList())

    fun updateFlow() {
        vehicleFlow.value = vehicles.values.toList()
    }

    override fun getVehicle(id: String): Flow<Vehicle?> =
        vehicleFlow.map { list -> list.find { it.id == id } }

    override fun getAllVehicles(): Flow<List<Vehicle>> = vehicleFlow

    override fun getVehicleCount(): Flow<Int> = vehicleFlow.map { it.size }

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

    override suspend fun deleteById(id: String) {
        if (shouldThrow) throw RuntimeException("Test error")
        vehicles.remove(id)
        updateFlow()
    }
}

private class FakeClock(private var time: Long) : Clock {
    override fun now(): Long = time
    fun setTime(t: Long) { time = t }
}

private class FakeJournalForRecorder : CrashRecoveryJournal(
    object : androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
        private val prefs = MutableStateFlow<androidx.datastore.preferences.core.Preferences>(
            androidx.datastore.preferences.core.emptyPreferences()
        )
        override val data = prefs
        override suspend fun updateData(
            transform: suspend (t: androidx.datastore.preferences.core.Preferences) ->
            androidx.datastore.preferences.core.Preferences,
        ): androidx.datastore.preferences.core.Preferences {
            val new = transform(prefs.value)
            prefs.value = new
            return new
        }
    }
) {
    private var entry: JournalEntry? = null

    override suspend fun write(
        tripId: String,
        vehicleId: String,
        distanceKm: Double,
        durationMs: Long,
        odometerKm: Double,
        lastFlushTimestamp: Long,
    ) {
        entry = JournalEntry(tripId, vehicleId, distanceKm, durationMs, odometerKm, lastFlushTimestamp, "ACTIVE")
    }

    override suspend fun read(): JournalEntry? = entry

    override suspend fun hasActiveTrip(): Boolean = entry != null

    override suspend fun clear() {
        entry = null
    }
}
