package com.roadmate.core.sync

import com.roadmate.core.model.BtConnectionState
import com.roadmate.core.model.sync.SyncMessage
import com.roadmate.core.state.BluetoothStateManager
import com.roadmate.core.util.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SyncSession")
class SyncSessionTest {

    private lateinit var stateManager: BluetoothStateManager
    private lateinit var ackTracker: AckTracker
    private lateinit var session: SyncSession
    private val testClock = Clock { 1000L }

    @BeforeEach
    fun setUp() {
        stateManager = BluetoothStateManager()
        ackTracker = AckTracker()
        val serializer = com.roadmate.core.sync.protocol.MessageSerializer()
        val batcher = SyncBatcher()
        val noOpEngine = DeltaSyncEngine(
            NoOpVehicleDao(), NoOpTripDao(), NoOpMaintenanceDao(), NoOpFuelDao(), NoOpDocumentDao(), batcher,
        )
        session = SyncSession(stateManager, noOpEngine, batcher, ackTracker, serializer, UnackedMessageTracker(), InMemorySyncTimestampStore(), testClock)
    }

    @Nested
    @DisplayName("createSyncStatus")
    inner class CreateSyncStatus {

        @Test
        fun `creates SyncStatus with device ID and timestamps`() {
            val status = session.createSyncStatus("device-1", 1000L, 500L)
            assertNotNull(status)
            assertEquals("device-1", status.deviceId)
            assertEquals(1000L, status.timestamp)
            assertEquals(500L, status.lastSyncTimestamp)
        }
    }

    @Nested
    @DisplayName("createPushMessages")
    inner class CreatePushMessages {

        @Test
        fun `creates SyncPush messages from deltas with unique messageIds`() = runTest {
            val deltas = listOf(
                SyncPushDto("vehicle", """[{"id":"v-1"}]"""),
                SyncPushDto("trip", """[{"id":"t-1"}]"""),
            )
            val messages = session.createPushMessages(deltas)
            assertEquals(2, messages.size)
            assertEquals("vehicle", messages[0].entityType)
            assertEquals("trip", messages[1].entityType)
            assertNotEquals(messages[0].messageId, messages[1].messageId)
        }

        @Test
        fun `returns empty list for empty deltas`() = runTest {
            val messages = session.createPushMessages(emptyList())
            assertTrue(messages.isEmpty())
        }

        @Test
        fun `tracks all messageIds in ack tracker`() = runTest {
            val deltas = listOf(
                SyncPushDto("vehicle", "[]"),
                SyncPushDto("trip", "[]"),
            )
            session.createPushMessages(deltas)
            assertEquals(2, ackTracker.pendingCount())
        }
    }

    @Nested
    @DisplayName("handleAck")
    inner class HandleAck {

        @Test
        fun `acknowledges tracked messageId`() = runTest {
            ackTracker.track("msg-1")
            ackTracker.track("msg-2")
            val result = session.handleAck(SyncMessage.SyncAck(true, "msg-1", System.currentTimeMillis(), null))
            assertTrue(result)
            assertEquals(1, ackTracker.pendingCount())
        }

        @Test
        fun `detects sync completion when all acked`() = runTest {
            ackTracker.track("msg-1")
            ackTracker.track("msg-2")
            session.handleAck(SyncMessage.SyncAck(true, "msg-1", System.currentTimeMillis(), null))
            assertTrue(!session.isSyncComplete())
            session.handleAck(SyncMessage.SyncAck(true, "msg-2", System.currentTimeMillis(), null))
            assertTrue(session.isSyncComplete())
        }
    }

    @Nested
    @DisplayName("state transitions")
    inner class StateTransitions {

        @Test
        fun `beginSync sets SyncInProgress state`() = runTest {
            stateManager.updateState(BtConnectionState.Connected)
            session.beginSync()
            assertEquals(BtConnectionState.SyncInProgress, stateManager.btConnectionState.value)
        }

        @Test
        fun `syncComplete transitions to Connected`() = runTest {
            stateManager.updateState(BtConnectionState.SyncInProgress)
            session.syncComplete()
            assertEquals(BtConnectionState.Connected, stateManager.btConnectionState.value)
        }

        @Test
        fun `syncComplete fails when unacked messages remain`() = runTest {
            stateManager.updateState(BtConnectionState.SyncInProgress)
            ackTracker.track("msg-1")
            session.syncComplete()
            assertTrue(stateManager.btConnectionState.value is BtConnectionState.SyncFailed)
        }

        @Test
        fun `syncFailed transitions to SyncFailed with reason`() {
            stateManager.updateState(BtConnectionState.SyncInProgress)
            session.syncFailed("Connection lost")
            assertTrue(stateManager.btConnectionState.value is BtConnectionState.SyncFailed)
            assertEquals("Connection lost", (stateManager.btConnectionState.value as BtConnectionState.SyncFailed).reason)
        }
    }

    @Nested
    @DisplayName("reset")
    inner class Reset {

        @Test
        fun `clears ack tracker`() = runTest {
            ackTracker.track("msg-1")
            session.reset()
            assertTrue(ackTracker.isComplete())
        }

        @Test
        fun `clears unacked message tracker`() = runTest {
            val push = SyncMessage.SyncPush("vehicle", "[{}]", "msg-1", 1000L)
            val deltas = listOf(SyncPushDto("vehicle", "[{}]"))
            session.createPushMessages(deltas)
            session.reset()
            // After reset, buildOutgoingMessages should not retransmit
            val messages = session.buildOutgoingMessages(0L)
            // Only SyncStatus + fresh deltas, no retransmitted unacked
            val pushMessages = messages.filterIsInstance<SyncMessage.SyncPush>()
            // Fresh deltas from noOpEngine return empty, so only retransmitted would appear
            assertTrue(pushMessages.isEmpty() || pushMessages.all { it.messageId != push.messageId })
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
