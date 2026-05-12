package com.roadmate.core.sync

import com.roadmate.core.database.dao.DocumentDao
import com.roadmate.core.database.dao.FuelDao
import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.dao.TripDao
import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.Document
import com.roadmate.core.database.entity.DocumentType
import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelLog
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.database.entity.Vehicle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("DeltaSyncEngine")
class DeltaSyncEngineTest {

    private lateinit var vehicleDao: VehicleDao
    private lateinit var tripDao: TripDao
    private lateinit var maintenanceDao: MaintenanceDao
    private lateinit var fuelDao: FuelDao
    private lateinit var documentDao: DocumentDao
    private lateinit var engine: DeltaSyncEngine

    private val testVehicle = Vehicle(
        id = "v-1", name = "Lancer", make = "Mitsubishi", model = "Lancer EX",
        year = 2015, engineType = EngineType.INLINE_4, engineSize = 1.6,
        fuelType = FuelType.GASOLINE, plateNumber = "ABC-123", vin = null,
        odometerKm = 85000.0, odometerUnit = OdometerUnit.KM,
        cityConsumption = 9.5, highwayConsumption = 6.8, lastModified = 600L,
    )

    private val testTrip = Trip(
        id = "t-1", vehicleId = "v-1", startTime = 1000L, endTime = 2000L,
        distanceKm = 45.5, durationMs = 1800000L, maxSpeedKmh = 120.0,
        avgSpeedKmh = 65.0, estimatedFuelL = 4.2, startOdometerKm = 85000.0,
        endOdometerKm = 85045.5, status = TripStatus.COMPLETED, lastModified = 700L,
    )

    private val testTripPoint = TripPoint(
        id = "tp-1", tripId = "t-1", latitude = 30.0444, longitude = 31.2357,
        speedKmh = 60.0, altitude = 75.0, accuracy = 5.0f,
        timestamp = 1000L, lastModified = 750L,
    )

    private val testSchedule = MaintenanceSchedule(
        id = "ms-1", vehicleId = "v-1", name = "Oil Change",
        intervalKm = 10000, intervalMonths = 6, lastServiceKm = 80000.0,
        lastServiceDate = 1000L, isCustom = false, lastModified = 800L,
    )

    private val testRecord = MaintenanceRecord(
        id = "mr-1", scheduleId = "ms-1", vehicleId = "v-1",
        datePerformed = 1000L, odometerKm = 85000.0,
        cost = 250.0, location = "AutoService", notes = "Good",
        lastModified = 850L,
    )

    private val testFuelLog = FuelLog(
        id = "f-1", vehicleId = "v-1", date = 1000L, odometerKm = 86000.0,
        liters = 45.0, pricePerLiter = 12.75, totalCost = 573.75,
        isFullTank = true, station = "Total", lastModified = 900L,
    )

    private val testDocument = Document(
        id = "d-1", vehicleId = "v-1", type = DocumentType.INSURANCE,
        name = "Insurance 2026", expiryDate = 1700000000000L,
        reminderDaysBefore = 30, notes = null, lastModified = 950L,
    )

    @BeforeEach
    fun setUp() {
        vehicleDao = StubVehicleDao()
        tripDao = StubTripDao()
        maintenanceDao = StubMaintenanceDao()
        fuelDao = StubFuelDao()
        documentDao = StubDocumentDao()
        engine = DeltaSyncEngine(vehicleDao, tripDao, maintenanceDao, fuelDao, documentDao, SyncBatcher())
    }

    @Nested
    @DisplayName("queryDeltas")
    inner class QueryDeltas {

        @Test
        fun `returns empty list when no entities modified since threshold`() = runTest {
            val emptyEngine = DeltaSyncEngine(
                StubVehicleDao(),
                StubTripDao(),
                StubMaintenanceDao(),
                StubFuelDao(),
                StubDocumentDao(),
                SyncBatcher(),
            )
            val deltas = emptyEngine.queryDeltas(Long.MAX_VALUE)
            assertTrue(deltas.isEmpty())
        }

        @Test
        fun `returns SyncPushDto for modified vehicles`() = runTest {
            val deltas = engine.queryDeltas(500L)
            val vehicleDelta = deltas.find { it.entityType == "vehicle" }
            assert(vehicleDelta != null)
            assertTrue(vehicleDelta!!.data.contains("\"id\":\"v-1\""))
        }

        @Test
        fun `returns all seven entity types when all modified`() = runTest {
            val deltas = engine.queryDeltas(0L)
            val types = deltas.map { it.entityType }.toSet()
            assertTrue(types.contains("vehicle"))
            assertTrue(types.contains("trip"))
            assertTrue(types.contains("trip_point"))
            assertTrue(types.contains("maintenance_schedule"))
            assertTrue(types.contains("maintenance_record"))
            assertTrue(types.contains("fuel_log"))
            assertTrue(types.contains("document"))
            assertEquals(7, deltas.size)
        }

        @Test
        fun `only includes entity types with actual modifications`() = runTest {
            val partialEngine = DeltaSyncEngine(
                StubVehicleDao(),
                StubTripDao(),
                StubMaintenanceDao(),
                StubFuelDao(),
                StubDocumentDao(),
                SyncBatcher(),
            )
            val deltas = partialEngine.queryDeltas(875L)
            val types = deltas.map { it.entityType }
            assertTrue(types.contains("fuel_log"))
            assertTrue(types.contains("document"))
            assertEquals(2, deltas.size)
        }
    }

    private class StubVehicleDao : VehicleDao {
        override fun getVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf<Vehicle?>(null)
        override fun getAllVehicles() = kotlinx.coroutines.flow.flowOf(emptyList<Vehicle>())
        override suspend fun upsert(vehicle: Vehicle) {}
        override suspend fun upsertAll(vehicles: List<Vehicle>) {}
        override suspend fun delete(vehicle: Vehicle) {}
        override suspend fun deleteById(vehicleId: String) {}
        override fun getVehicleCount() = kotlinx.coroutines.flow.flowOf(0)
        override suspend fun addToOdometer(vehicleId: String, distanceKm: Double, lastModified: Long) {}
        override suspend fun getModifiedSince(since: Long): List<Vehicle> =
            if (since < 600L) listOf(
                Vehicle("v-1", "Lancer", "Mitsubishi", "Lancer EX", 2015,
                    EngineType.INLINE_4, 1.6, FuelType.GASOLINE, "ABC-123", null,
                    85000.0, OdometerUnit.KM, 9.5, 6.8, 600L)
            ) else emptyList()
        override suspend fun getVehicleById(id: String): Vehicle? = null
    }

    private class StubTripDao : TripDao() {
        override fun getTripsForVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<Trip>())
        override fun getTripPointsForTrip(tripId: String) = kotlinx.coroutines.flow.flowOf(emptyList<TripPoint>())
        override fun getActiveTrip(vehicleId: String) = kotlinx.coroutines.flow.flowOf<Trip?>(null)
        override fun getTrip(tripId: String) = kotlinx.coroutines.flow.flowOf<Trip?>(null)
        override suspend fun upsertTrip(trip: Trip) {}
        override suspend fun upsertTrips(trips: List<Trip>) {}
        override suspend fun deleteTrip(trip: Trip) {}
        override suspend fun deleteTripById(tripId: String) {}
        override suspend fun upsertTripPoint(tripPoint: TripPoint) {}
        override suspend fun upsertTripPoints(tripPoints: List<TripPoint>) {}
        override suspend fun deleteTripPoint(tripPoint: TripPoint) {}
        override suspend fun getTripsModifiedSince(since: Long): List<Trip> =
            if (since < 700L) listOf(
                Trip("t-1", "v-1", 1000L, 2000L, 45.5, 1800000L, 120.0, 65.0, 4.2,
                    85000.0, 85045.5, TripStatus.COMPLETED, 700L)
            ) else emptyList()
        override suspend fun getTripPointsModifiedSince(since: Long): List<TripPoint> =
            if (since < 750L) listOf(
                TripPoint("tp-1", "t-1", 30.0444, 31.2357, 60.0, 75.0, 5.0f, 1000L, false, 750L)
            ) else emptyList()
        override suspend fun getTripById(id: String): Trip? = null
        override suspend fun getTripPointById(id: String): TripPoint? = null
    }

    private class StubMaintenanceDao : MaintenanceDao() {
        override fun getSchedulesForVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<MaintenanceSchedule>())
        override fun getSchedule(scheduleId: String) = kotlinx.coroutines.flow.flowOf<MaintenanceSchedule?>(null)
        override suspend fun upsertSchedule(schedule: MaintenanceSchedule) {}
        override suspend fun upsertSchedules(schedules: List<MaintenanceSchedule>) {}
        override suspend fun deleteSchedule(schedule: MaintenanceSchedule) {}
        override suspend fun deleteScheduleById(scheduleId: String) {}
        override fun getRecordsForSchedule(scheduleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<MaintenanceRecord>())
        override fun getRecordsForVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<MaintenanceRecord>())
        override fun getRecord(recordId: String) = kotlinx.coroutines.flow.flowOf<MaintenanceRecord?>(null)
        override suspend fun upsertRecord(record: MaintenanceRecord) {}
        override suspend fun upsertRecords(records: List<MaintenanceRecord>) {}
        override suspend fun deleteRecord(record: MaintenanceRecord) {}
        override suspend fun deleteRecordById(recordId: String) {}
        override suspend fun deleteRecordsByScheduleId(scheduleId: String) {}
        override suspend fun getSchedulesModifiedSince(since: Long): List<MaintenanceSchedule> =
            if (since < 800L) listOf(
                MaintenanceSchedule("ms-1", "v-1", "Oil Change", 10000, 6, 80000.0, 1000L, false, 800L)
            ) else emptyList()
        override suspend fun getRecordsModifiedSince(since: Long): List<MaintenanceRecord> =
            if (since < 850L) listOf(
                MaintenanceRecord("mr-1", "ms-1", "v-1", 1000L, 85000.0, 250.0, "AutoService", "Good", 850L)
            ) else emptyList()
        override suspend fun getScheduleById(id: String): MaintenanceSchedule? = null
        override suspend fun getRecordById(id: String): MaintenanceRecord? = null
    }

    private class StubFuelDao : FuelDao {
        override fun getFuelLogsForVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<FuelLog>())
        override fun getLastFullTankEntry(vehicleId: String) = kotlinx.coroutines.flow.flowOf<FuelLog?>(null)
        override fun getFuelLog(fuelLogId: String) = kotlinx.coroutines.flow.flowOf<FuelLog?>(null)
        override suspend fun upsertFuelLog(fuelLog: FuelLog) {}
        override suspend fun upsertFuelLogs(fuelLogs: List<FuelLog>) {}
        override suspend fun deleteFuelLog(fuelLog: FuelLog) {}
        override suspend fun deleteFuelLogById(fuelLogId: String) {}
        override suspend fun getFuelLogsModifiedSince(since: Long): List<FuelLog> =
            if (since < 900L) listOf(
                FuelLog("f-1", "v-1", 1000L, 86000.0, 45.0, 12.75, 573.75, true, "Total", 900L)
            ) else emptyList()
        override suspend fun getFuelLogById(id: String): FuelLog? = null
    }

    private class StubDocumentDao : DocumentDao {
        override fun getDocumentsForVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<Document>())
        override fun getExpiringDocuments(vehicleId: String, threshold: Long) = kotlinx.coroutines.flow.flowOf(emptyList<Document>())
        override fun getDocument(documentId: String) = kotlinx.coroutines.flow.flowOf<Document?>(null)
        override suspend fun upsertDocument(document: Document) {}
        override suspend fun upsertDocuments(documents: List<Document>) {}
        override suspend fun deleteDocument(document: Document) {}
        override suspend fun deleteDocumentById(documentId: String) {}
            override suspend fun getDocumentsModifiedSince(since: Long): List<Document> =
                if (since < 950L) listOf(
                    Document("d-1", "v-1", DocumentType.INSURANCE, "Insurance 2026", 1700000000000L, 30, null, 950L)
                ) else emptyList()
            override suspend fun getDocumentById(id: String): Document? = null
        }
}
