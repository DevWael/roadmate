package com.roadmate.core.sync

import app.cash.turbine.test
import com.roadmate.core.model.BtConnectionState
import com.roadmate.core.state.BluetoothStateManager
import com.roadmate.core.sync.protocol.MessageSerializer
import com.roadmate.core.util.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("BluetoothConnectionManager")
class BluetoothConnectionManagerTest {

    private lateinit var stateManager: BluetoothStateManager
    private lateinit var server: BluetoothSyncServer
    private lateinit var client: BluetoothSyncClient
    private lateinit var syncSession: SyncSession
    private lateinit var manager: BluetoothConnectionManager
    private var job: Job? = null

    @BeforeEach
    fun setUp() {
        stateManager = BluetoothStateManager()
        val j = SupervisorJob()
        job = j
        val scope = CoroutineScope(j + UnconfinedTestDispatcher())
        server = BluetoothSyncServer(null, scope, UnconfinedTestDispatcher())
        client = BluetoothSyncClient(null, UnconfinedTestDispatcher())
        syncSession = SyncSession(
            stateManager,
            DeltaSyncEngine(
                object : com.roadmate.core.database.dao.VehicleDao {
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
                },
                object : com.roadmate.core.database.dao.TripDao() {
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
                },
                object : com.roadmate.core.database.dao.MaintenanceDao() {
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
                },
                object : com.roadmate.core.database.dao.FuelDao {
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
                },
                object : com.roadmate.core.database.dao.DocumentDao {
                    override fun getDocumentsForVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.roadmate.core.database.entity.Document>())
                    override fun getExpiringDocuments(vehicleId: String, threshold: Long) = kotlinx.coroutines.flow.flowOf(emptyList<com.roadmate.core.database.entity.Document>())
                    override fun getDocument(documentId: String) = kotlinx.coroutines.flow.flowOf<com.roadmate.core.database.entity.Document?>(null)
                    override suspend fun upsertDocument(document: com.roadmate.core.database.entity.Document) {}
                    override suspend fun upsertDocuments(documents: List<com.roadmate.core.database.entity.Document>) {}
                    override suspend fun deleteDocument(document: com.roadmate.core.database.entity.Document) {}
                    override suspend fun deleteDocumentById(documentId: String) {}
                    override suspend fun getDocumentsModifiedSince(since: Long) = emptyList<com.roadmate.core.database.entity.Document>()
                    override suspend fun getDocumentById(id: String): com.roadmate.core.database.entity.Document? = null
                },
                SyncBatcher(),
            ),
            SyncBatcher(),
            AckTracker(),
            MessageSerializer(),
            UnackedMessageTracker(),
            InMemorySyncTimestampStore(),
            Clock.SYSTEM,
        )
        manager = BluetoothConnectionManager(server, client, stateManager, syncSession, scope)
    }

    @Nested
    @DisplayName("calculateBackoff")
    inner class CalculateBackoff {
        @Test
        fun `attempt 0 returns 2s`() {
            assertEquals(2000L, manager.calculateBackoff(0))
        }

        @Test
        fun `attempt 1 returns 4s`() {
            assertEquals(4000L, manager.calculateBackoff(1))
        }

        @Test
        fun `attempt 2 returns 8s`() {
            assertEquals(8000L, manager.calculateBackoff(2))
        }

        @Test
        fun `attempt 3 returns 16s`() {
            assertEquals(16000L, manager.calculateBackoff(3))
        }

        @Test
        fun `attempt 4 returns 30s capped`() {
            assertEquals(30000L, manager.calculateBackoff(4))
        }

        @Test
        fun `attempt 5 returns 30s capped`() {
            assertEquals(30000L, manager.calculateBackoff(5))
        }

        @Test
        fun `attempt 10 returns 30s capped`() {
            assertEquals(30000L, manager.calculateBackoff(10))
        }

        @Test
        fun `negative attempt returns initial backoff`() {
            assertEquals(2000L, manager.calculateBackoff(-1))
        }
    }

    @Nested
    @DisplayName("startServer")
    inner class StartServer {
        @Test
        fun `emits Disconnected initially`() = runTest {
            stateManager.btConnectionState.test {
                manager.startServer()
                assertEquals(BtConnectionState.Disconnected, awaitItem())
                manager.stop()
            }
        }

        @Test
        fun `does not crash with null adapter`() = runTest {
            manager.startServer()
            assertNotNull(manager)
            manager.stop()
        }
    }

    @Nested
    @DisplayName("startClient")
    inner class StartClient {
        @Test
        fun `sets mode to CLIENT`() = runTest {
            manager.startClient()
            assertEquals(BluetoothConnectionManager.Mode.CLIENT, manager.currentMode)
            manager.stop()
        }

        @Test
        fun `does not crash with null adapter`() = runTest {
            manager.startClient()
            assertNotNull(manager)
            manager.stop()
        }
    }

    @Nested
    @DisplayName("stop")
    inner class Stop {
        @Test
        fun `resets to IDLE`() = runTest {
            manager.startServer()
            manager.stop()
            assertEquals(BluetoothConnectionManager.Mode.IDLE, manager.currentMode)
        }

        @Test
        fun `can be called without start`() = runTest {
            manager.stop()
            assertNotNull(manager)
        }

        @Test
        fun `can be called multiple times`() = runTest {
            manager.startServer()
            manager.stop()
            manager.stop()
            assertNotNull(manager)
        }
    }

    @Nested
    @DisplayName("destroy")
    inner class Destroy {
        @Test
        fun `cleans up all resources`() = runTest {
            manager.startServer()
            manager.destroy()
            assertNotNull(manager)
        }
    }

    @Nested
    @DisplayName("mode tracking")
    inner class ModeTracking {
        @Test
        fun `initial mode is IDLE`() {
            assertEquals(BluetoothConnectionManager.Mode.IDLE, manager.currentMode)
        }

        @Test
        fun `startServer sets SERVER mode`() {
            manager.startServer()
            assertEquals(BluetoothConnectionManager.Mode.SERVER, manager.currentMode)
            manager.stop()
        }

        @Test
        fun `startClient sets CLIENT mode`() = runTest {
            manager.startClient()
            assertEquals(BluetoothConnectionManager.Mode.CLIENT, manager.currentMode)
            manager.stop()
        }

        @Test
        fun `stop resets to IDLE`() {
            manager.startServer()
            manager.stop()
            assertEquals(BluetoothConnectionManager.Mode.IDLE, manager.currentMode)
        }
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
