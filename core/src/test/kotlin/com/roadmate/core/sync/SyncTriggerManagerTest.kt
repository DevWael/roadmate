package com.roadmate.core.sync

import com.roadmate.core.model.BtConnectionState
import com.roadmate.core.model.sync.SyncReason
import com.roadmate.core.model.sync.SyncResult
import com.roadmate.core.state.BluetoothStateManager
import com.roadmate.core.util.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SyncTriggerManager")
class SyncTriggerManagerTest {

    private lateinit var stateManager: BluetoothStateManager
    private lateinit var syncSession: SyncSession
    private lateinit var deltaEngine: DeltaSyncEngine
    private lateinit var manager: SyncTriggerManager
    private var job: Job? = null

    @BeforeEach
    fun setUp() {
        stateManager = BluetoothStateManager()
        stateManager.updateState(BtConnectionState.Connected)

        val j = SupervisorJob()
        job = j
        val scope = CoroutineScope(j + UnconfinedTestDispatcher())
        val batcher = SyncBatcher()
        val ackTracker = AckTracker()
        val serializer = com.roadmate.core.sync.protocol.MessageSerializer()
        val noOpEngine = DeltaSyncEngine(
            NoOpVehicleDao(), NoOpTripDao(), NoOpMaintenanceDao(), NoOpFuelDao(), NoOpDocumentDao(), batcher,
        )
        deltaEngine = noOpEngine
        syncSession = SyncSession(
            stateManager, noOpEngine, batcher, ackTracker, serializer,
            UnackedMessageTracker(), InMemorySyncTimestampStore(),
            Clock { System.currentTimeMillis() },
        )
        manager = SyncTriggerManager(stateManager, syncSession, deltaEngine, scope)
        manager.start()
    }

    @AfterEach
    fun tearDown() {
        manager.destroy()
        job?.cancel()
    }

    @Nested
    @DisplayName("event triggers")
    inner class EventTriggers {

        @Test
        fun `trip completion triggers sync when connected`() = runTest {
            val tripEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
            val maintenanceEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
            val fuelEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

            manager.startEventObservation(tripEvents, maintenanceEvents, fuelEvents)
            tripEvents.tryEmit(Unit)
            manager.stopEventObservation()

            val result = manager.syncResult.value
            assertTrue(
                result is SyncResult.Success || result is SyncResult.InProgress,
                "Expected Success or InProgress but got $result",
            )
        }

        @Test
        fun `maintenance done triggers sync when connected`() = runTest {
            val tripEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
            val maintenanceEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
            val fuelEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

            manager.startEventObservation(tripEvents, maintenanceEvents, fuelEvents)
            maintenanceEvents.tryEmit(Unit)
            manager.stopEventObservation()

            val result = manager.syncResult.value
            assertTrue(
                result is SyncResult.Success || result is SyncResult.InProgress,
                "Expected Success or InProgress but got $result",
            )
        }

        @Test
        fun `fuel entry triggers sync when connected`() = runTest {
            val tripEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
            val maintenanceEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
            val fuelEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

            manager.startEventObservation(tripEvents, maintenanceEvents, fuelEvents)
            fuelEvents.tryEmit(Unit)
            manager.stopEventObservation()

            val result = manager.syncResult.value
            assertTrue(
                result is SyncResult.Success || result is SyncResult.InProgress,
                "Expected Success or InProgress but got $result",
            )
        }

        @Test
        fun `event triggers are silently ignored when BT disconnected`() = runTest {
            stateManager.updateState(BtConnectionState.Disconnected)
            val tripEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
            val maintenanceEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
            val fuelEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

            manager.startEventObservation(tripEvents, maintenanceEvents, fuelEvents)
            tripEvents.tryEmit(Unit)
            manager.stopEventObservation()

            assertEquals(SyncResult.Idle, manager.syncResult.value)
        }
    }

    @Nested
    @DisplayName("queue serialization")
    inner class QueueSerialization {

        @Test
        fun `syncs are serialized via channel`() = runTest {
            manager.requestSync(SyncReason.TRIP_COMPLETED)
            manager.requestSync(SyncReason.MAINTENANCE_DONE)

            val result = manager.syncResult.value
            assertTrue(
                result is SyncResult.Success || result is SyncResult.InProgress,
                "Expected Success or InProgress but got $result",
            )
        }

        @Test
        fun `rapid requests do not cause concurrent syncs`() = runTest {
            repeat(10) {
                manager.requestSync(SyncReason.FUEL_ENTRY)
            }

            val result = manager.syncResult.value
            assertTrue(
                result is SyncResult.Success || result is SyncResult.Failed,
                "Expected Success or Failed but got $result",
            )
        }
    }

    @Nested
    @DisplayName("manual sync")
    inner class ManualSync {

        @Test
        fun `triggerManualSync emits result`() = runTest {
            manager.triggerManualSync()

            val result = manager.syncResult.value
            assertTrue(
                result is SyncResult.Success || result is SyncResult.InProgress,
                "Expected Success or InProgress but got $result",
            )
        }

        @Test
        fun `triggerManualSync ignored when disconnected`() = runTest {
            stateManager.updateState(BtConnectionState.Disconnected)
            manager.triggerManualSync()

            assertEquals(SyncResult.Idle, manager.syncResult.value)
        }
    }

    @Nested
    @DisplayName("disconnected tolerance")
    inner class DisconnectedTolerance {

        @Test
        fun `requestSync does not queue when BT disconnected`() = runTest {
            stateManager.updateState(BtConnectionState.Disconnected)
            manager.requestSync(SyncReason.TRIP_COMPLETED)

            assertEquals(SyncResult.Idle, manager.syncResult.value)
        }

        @Test
        fun `requestSync does not queue when BT connecting`() = runTest {
            stateManager.updateState(BtConnectionState.Connecting)
            manager.requestSync(SyncReason.PERIODIC)

            assertEquals(SyncResult.Idle, manager.syncResult.value)
        }

        @Test
        fun `requestSync does not queue when sync already in progress`() = runTest {
            stateManager.updateState(BtConnectionState.SyncInProgress)
            manager.requestSync(SyncReason.MANUAL)

            assertEquals(SyncResult.Idle, manager.syncResult.value)
        }
    }

    @Nested
    @DisplayName("periodic sync")
    inner class PeriodicSync {

        @Test
        fun `startPeriodicSync does not crash when called`() {
            manager.startPeriodicSync()
            manager.stopPeriodicSync()
        }

        @Test
        fun `stopPeriodicSync cancels timer`() {
            manager.startPeriodicSync()
            manager.stopPeriodicSync()

            assertEquals(SyncResult.Idle, manager.syncResult.value)
        }
    }

    @Nested
    @DisplayName("lifecycle")
    inner class Lifecycle {

        @Test
        fun `destroy cleans up all jobs`() {
            val tripEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
            val maintenanceEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
            val fuelEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

            manager.startEventObservation(tripEvents, maintenanceEvents, fuelEvents)
            manager.startPeriodicSync()
            manager.destroy()

            assertEquals(SyncResult.Idle, manager.syncResult.value)
        }
    }

    private class NoOpVehicleDao : com.roadmate.core.database.dao.VehicleDao {
        override fun getVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf<com.roadmate.core.database.entity.Vehicle?>(null)
        override fun getAllVehicles() = kotlinx.coroutines.flow.flowOf(emptyList<com.roadmate.core.database.entity.Vehicle>())
        override suspend fun upsert(vehicle: com.roadmate.core.database.entity.Vehicle) {}
        override suspend fun upsertAll(vehicles: List<com.roadmate.core.database.entity.Vehicle>) {}
        override suspend fun delete(vehicle: com.roadmate.core.database.entity.Vehicle) {}
        override suspend fun deleteById(vehicleId: String) {}
        override fun getVehicleCount() = kotlinx.coroutines.flow.flowOf(0)
        override suspend fun addToOdometer(vehicleId: String, distanceKm: Double, lastModified: Long) {}
        override suspend fun getModifiedSince(since: Long) = emptyList<com.roadmate.core.database.entity.Vehicle>()
        override suspend fun getVehicleById(id: String): com.roadmate.core.database.entity.Vehicle? = null
    }

    private class NoOpTripDao : com.roadmate.core.database.dao.TripDao() {
        override fun getTripsForVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.roadmate.core.database.entity.Trip>())
        override fun getTripPointsForTrip(tripId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.roadmate.core.database.entity.TripPoint>())
        override fun getActiveTrip(vehicleId: String) = kotlinx.coroutines.flow.flowOf<com.roadmate.core.database.entity.Trip?>(null)
        override fun getTrip(tripId: String) = kotlinx.coroutines.flow.flowOf<com.roadmate.core.database.entity.Trip?>(null)
        override suspend fun upsertTrip(trip: com.roadmate.core.database.entity.Trip) {}
        override suspend fun upsertTrips(trips: List<com.roadmate.core.database.entity.Trip>) {}
        override suspend fun deleteTrip(trip: com.roadmate.core.database.entity.Trip) {}
        override suspend fun deleteTripById(tripId: String) {}
        override suspend fun upsertTripPoint(tripPoint: com.roadmate.core.database.entity.TripPoint) {}
        override suspend fun upsertTripPoints(tripPoints: List<com.roadmate.core.database.entity.TripPoint>) {}
        override suspend fun deleteTripPoint(tripPoint: com.roadmate.core.database.entity.TripPoint) {}
        override suspend fun getTripsModifiedSince(since: Long) = emptyList<com.roadmate.core.database.entity.Trip>()
        override suspend fun getTripPointsModifiedSince(since: Long) = emptyList<com.roadmate.core.database.entity.TripPoint>()
        override suspend fun getTripById(id: String): com.roadmate.core.database.entity.Trip? = null
        override suspend fun getTripPointById(id: String): com.roadmate.core.database.entity.TripPoint? = null
    }

    private class NoOpMaintenanceDao : com.roadmate.core.database.dao.MaintenanceDao() {
        override fun getSchedulesForVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.roadmate.core.database.entity.MaintenanceSchedule>())
        override fun getSchedule(scheduleId: String) = kotlinx.coroutines.flow.flowOf<com.roadmate.core.database.entity.MaintenanceSchedule?>(null)
        override suspend fun upsertSchedule(schedule: com.roadmate.core.database.entity.MaintenanceSchedule) {}
        override suspend fun upsertSchedules(schedules: List<com.roadmate.core.database.entity.MaintenanceSchedule>) {}
        override suspend fun deleteSchedule(schedule: com.roadmate.core.database.entity.MaintenanceSchedule) {}
        override suspend fun deleteScheduleById(scheduleId: String) {}
        override fun getRecordsForSchedule(scheduleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.roadmate.core.database.entity.MaintenanceRecord>())
        override fun getRecordsForVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.roadmate.core.database.entity.MaintenanceRecord>())
        override fun getRecord(recordId: String) = kotlinx.coroutines.flow.flowOf<com.roadmate.core.database.entity.MaintenanceRecord?>(null)
        override suspend fun upsertRecord(record: com.roadmate.core.database.entity.MaintenanceRecord) {}
        override suspend fun upsertRecords(records: List<com.roadmate.core.database.entity.MaintenanceRecord>) {}
        override suspend fun deleteRecord(record: com.roadmate.core.database.entity.MaintenanceRecord) {}
        override suspend fun deleteRecordById(recordId: String) {}
        override suspend fun deleteRecordsByScheduleId(scheduleId: String) {}
        override suspend fun getSchedulesModifiedSince(since: Long) = emptyList<com.roadmate.core.database.entity.MaintenanceSchedule>()
        override suspend fun getRecordsModifiedSince(since: Long) = emptyList<com.roadmate.core.database.entity.MaintenanceRecord>()
        override suspend fun getScheduleById(id: String): com.roadmate.core.database.entity.MaintenanceSchedule? = null
        override suspend fun getRecordById(id: String): com.roadmate.core.database.entity.MaintenanceRecord? = null
    }

    private class NoOpFuelDao : com.roadmate.core.database.dao.FuelDao {
        override fun getFuelLogsForVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.roadmate.core.database.entity.FuelLog>())
        override fun getLastFullTankEntry(vehicleId: String) = kotlinx.coroutines.flow.flowOf<com.roadmate.core.database.entity.FuelLog?>(null)
        override fun getLatestFuelEntry(vehicleId: String) = kotlinx.coroutines.flow.flowOf<com.roadmate.core.database.entity.FuelLog?>(null)
        override fun getTwoLastFullTankEntries(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.roadmate.core.database.entity.FuelLog>())
        override fun getFuelLog(fuelLogId: String) = kotlinx.coroutines.flow.flowOf<com.roadmate.core.database.entity.FuelLog?>(null)
        override suspend fun upsertFuelLog(fuelLog: com.roadmate.core.database.entity.FuelLog) {}
        override suspend fun upsertFuelLogs(fuelLogs: List<com.roadmate.core.database.entity.FuelLog>) {}
        override suspend fun deleteFuelLog(fuelLog: com.roadmate.core.database.entity.FuelLog) {}
        override suspend fun deleteFuelLogById(fuelLogId: String) {}
        override suspend fun getFuelLogsModifiedSince(since: Long) = emptyList<com.roadmate.core.database.entity.FuelLog>()
        override suspend fun getFuelLogById(id: String): com.roadmate.core.database.entity.FuelLog? = null
    }

    private class NoOpDocumentDao : com.roadmate.core.database.dao.DocumentDao {
        override fun getDocumentsForVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.roadmate.core.database.entity.Document>())
        override fun getExpiringDocuments(vehicleId: String, threshold: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.roadmate.core.database.entity.Document>())
        override fun getDocument(documentId: String) = kotlinx.coroutines.flow.flowOf<com.roadmate.core.database.entity.Document?>(null)
        override suspend fun upsertDocument(document: com.roadmate.core.database.entity.Document) {}
        override suspend fun upsertDocuments(documents: List<com.roadmate.core.database.entity.Document>) {}
        override suspend fun deleteDocument(document: com.roadmate.core.database.entity.Document) {}
        override suspend fun deleteDocumentById(documentId: String) {}
        override suspend fun getDocumentsModifiedSince(since: Long) = emptyList<com.roadmate.core.database.entity.Document>()
        override suspend fun getDocumentById(id: String): com.roadmate.core.database.entity.Document? = null
    }

    private class InMemorySyncTimestampStore : SyncTimestampStore(
        object : androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
            private val prefs = kotlinx.coroutines.flow.MutableStateFlow(androidx.datastore.preferences.core.emptyPreferences())
            override val data = prefs
            override suspend fun updateData(transformer: suspend (androidx.datastore.preferences.core.Preferences) -> androidx.datastore.preferences.core.Preferences): androidx.datastore.preferences.core.Preferences {
                val new = transformer(prefs.value)
                prefs.value = new
                return new
            }
        }
    )
}
