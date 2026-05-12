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
@DisplayName("ConflictResolver")
class ConflictResolverTest {

    private lateinit var vehicleDao: RecordingVehicleDao
    private lateinit var tripDao: RecordingTripDao
    private lateinit var maintenanceDao: RecordingMaintenanceDao
    private lateinit var fuelDao: RecordingFuelDao
    private lateinit var documentDao: RecordingDocumentDao
    private lateinit var resolver: ConflictResolver

    @BeforeEach
    fun setUp() {
        vehicleDao = RecordingVehicleDao()
        tripDao = RecordingTripDao()
        maintenanceDao = RecordingMaintenanceDao()
        fuelDao = RecordingFuelDao()
        documentDao = RecordingDocumentDao()
        resolver = ConflictResolver(vehicleDao, tripDao, maintenanceDao, fuelDao, documentDao)
    }

    @Nested
    @DisplayName("resolvePush")
    inner class ResolvePush {

        @Test
        fun `upserts new vehicle when no local exists`() = runTest {
            val data = """[{"id":"v-1","name":"Lancer","make":"Mitsubishi","model":"Lancer EX","year":2015,"engineType":"INLINE_4","engineSize":1.6,"fuelType":"GASOLINE","plateNumber":"ABC","vin":null,"odometerKm":85000.0,"odometerUnit":"KM","cityConsumption":9.5,"highwayConsumption":6.8,"lastModified":1000}]"""
            val result = resolver.resolvePush("vehicle", data)
            assertTrue(result.applied > 0)
            assertTrue(result.skipped == 0)
            assertEquals(1, vehicleDao.upserted.size)
        }

        @Test
        fun `skips incoming vehicle when local is newer (local wins)`() = runTest {
            val localVehicle = Vehicle(
                id = "v-1", name = "Lancer", make = "Mitsubishi", model = "Lancer EX",
                year = 2015, engineType = EngineType.INLINE_4, engineSize = 1.6,
                fuelType = FuelType.GASOLINE, plateNumber = "ABC", vin = null,
                odometerKm = 85000.0, odometerUnit = OdometerUnit.KM,
                cityConsumption = 9.5, highwayConsumption = 6.8, lastModified = 2000L,
            )
            vehicleDao.localVehicle = localVehicle

            val data = """[{"id":"v-1","name":"Lancer Old","make":"Mitsubishi","model":"Lancer EX","year":2015,"engineType":"INLINE_4","engineSize":1.6,"fuelType":"GASOLINE","plateNumber":"ABC","vin":null,"odometerKm":85000.0,"odometerUnit":"KM","cityConsumption":9.5,"highwayConsumption":6.8,"lastModified":1000}]"""
            val result = resolver.resolvePush("vehicle", data)
            assertTrue(result.skipped > 0)
            assertTrue(result.applied == 0)
            assertTrue(vehicleDao.upserted.isEmpty())
        }

        @Test
        fun `overwrites local vehicle when incoming is newer (remote wins)`() = runTest {
            val localVehicle = Vehicle(
                id = "v-1", name = "Lancer Old", make = "Mitsubishi", model = "Lancer EX",
                year = 2015, engineType = EngineType.INLINE_4, engineSize = 1.6,
                fuelType = FuelType.GASOLINE, plateNumber = "ABC", vin = null,
                odometerKm = 85000.0, odometerUnit = OdometerUnit.KM,
                cityConsumption = 9.5, highwayConsumption = 6.8, lastModified = 500L,
            )
            vehicleDao.localVehicle = localVehicle

            val data = """[{"id":"v-1","name":"Lancer Updated","make":"Mitsubishi","model":"Lancer EX","year":2015,"engineType":"INLINE_4","engineSize":1.6,"fuelType":"GASOLINE","plateNumber":"ABC","vin":null,"odometerKm":86000.0,"odometerUnit":"KM","cityConsumption":9.5,"highwayConsumption":6.8,"lastModified":1000}]"""
            val result = resolver.resolvePush("vehicle", data)
            assertTrue(result.applied > 0)
            assertTrue(result.skipped == 0)
            assertEquals(1, vehicleDao.upserted.size)
            assertEquals("Lancer Updated", vehicleDao.upserted.first().name)
        }

        @Test
        fun `handles multiple entities in single push`() = runTest {
            val data = """[{"id":"v-1","name":"A","make":"M","model":"X","year":2020,"engineType":"INLINE_4","engineSize":2.0,"fuelType":"GASOLINE","plateNumber":"P","vin":null,"odometerKm":1000.0,"odometerUnit":"KM","cityConsumption":8.0,"highwayConsumption":5.0,"lastModified":100},{"id":"v-2","name":"B","make":"N","model":"Y","year":2021,"engineType":"INLINE_4","engineSize":1.5,"fuelType":"DIESEL","plateNumber":"Q","vin":null,"odometerKm":2000.0,"odometerUnit":"KM","cityConsumption":7.0,"highwayConsumption":4.5,"lastModified":200}]"""
            val result = resolver.resolvePush("vehicle", data)
            assertEquals(2, result.applied)
            assertEquals(0, result.skipped)
            assertEquals(2, vehicleDao.upserted.size)
        }

        @Test
        fun `skips local-equal timestamp (local wins tie)`() = runTest {
            val localVehicle = Vehicle(
                id = "v-1", name = "Lancer Local", make = "Mitsubishi", model = "Lancer EX",
                year = 2015, engineType = EngineType.INLINE_4, engineSize = 1.6,
                fuelType = FuelType.GASOLINE, plateNumber = "ABC", vin = null,
                odometerKm = 85000.0, odometerUnit = OdometerUnit.KM,
                cityConsumption = 9.5, highwayConsumption = 6.8, lastModified = 1000L,
            )
            vehicleDao.localVehicle = localVehicle

            val data = """[{"id":"v-1","name":"Lancer Remote","make":"Mitsubishi","model":"Lancer EX","year":2015,"engineType":"INLINE_4","engineSize":1.6,"fuelType":"GASOLINE","plateNumber":"ABC","vin":null,"odometerKm":85000.0,"odometerUnit":"KM","cityConsumption":9.5,"highwayConsumption":6.8,"lastModified":1000}]"""
            val result = resolver.resolvePush("vehicle", data)
            assertTrue(result.skipped > 0)
            assertTrue(vehicleDao.upserted.isEmpty())
        }
    }

    @Nested
    @DisplayName("idempotency")
    inner class Idempotency {

        @Test
        fun `duplicate push produces identical result - no duplicates`() = runTest {
            val data = """[{"id":"v-1","name":"Lancer","make":"Mitsubishi","model":"Lancer EX","year":2015,"engineType":"INLINE_4","engineSize":1.6,"fuelType":"GASOLINE","plateNumber":"ABC","vin":null,"odometerKm":85000.0,"odometerUnit":"KM","cityConsumption":9.5,"highwayConsumption":6.8,"lastModified":1000}]"""

            val result1 = resolver.resolvePush("vehicle", data)
            val result2 = resolver.resolvePush("vehicle", data)

            assertEquals(result1, result2)
        }
    }

    @Nested
    @DisplayName("all entity types")
    inner class AllEntityTypes {

        @Test
        fun `resolves trip entities`() = runTest {
            val data = """[{"id":"t-1","vehicleId":"v-1","startTime":1000,"endTime":2000,"distanceKm":45.5,"durationMs":1800000,"maxSpeedKmh":120.0,"avgSpeedKmh":65.0,"estimatedFuelL":4.2,"startOdometerKm":85000.0,"endOdometerKm":85045.5,"status":"COMPLETED","lastModified":700}]"""
            val result = resolver.resolvePush("trip", data)
            assertTrue(result.applied > 0)
        }

        @Test
        fun `resolves trip_point entities`() = runTest {
            val data = """[{"id":"tp-1","tripId":"t-1","latitude":30.0,"longitude":31.0,"speedKmh":60.0,"altitude":75.0,"accuracy":5.0,"timestamp":1000,"isGapBoundary":false,"lastModified":750}]"""
            val result = resolver.resolvePush("trip_point", data)
            assertTrue(result.applied > 0)
        }

        @Test
        fun `resolves maintenance_schedule entities`() = runTest {
            val data = """[{"id":"ms-1","vehicleId":"v-1","name":"Oil Change","intervalKm":10000,"intervalMonths":6,"lastServiceKm":80000.0,"lastServiceDate":1000,"isCustom":false,"lastModified":800}]"""
            val result = resolver.resolvePush("maintenance_schedule", data)
            assertTrue(result.applied > 0)
        }

        @Test
        fun `resolves maintenance_record entities`() = runTest {
            val data = """[{"id":"mr-1","scheduleId":"ms-1","vehicleId":"v-1","datePerformed":1000,"odometerKm":85000.0,"cost":250.0,"location":null,"notes":null,"lastModified":850}]"""
            val result = resolver.resolvePush("maintenance_record", data)
            assertTrue(result.applied > 0)
        }

        @Test
        fun `resolves fuel_log entities`() = runTest {
            val data = """[{"id":"f-1","vehicleId":"v-1","date":1000,"odometerKm":86000.0,"liters":45.0,"pricePerLiter":12.75,"totalCost":573.75,"isFullTank":true,"station":null,"lastModified":900}]"""
            val result = resolver.resolvePush("fuel_log", data)
            assertTrue(result.applied > 0)
        }

        @Test
        fun `resolves document entities`() = runTest {
            val data = """[{"id":"d-1","vehicleId":"v-1","type":"INSURANCE","name":"Insurance","expiryDate":1700000000000,"reminderDaysBefore":30,"notes":null,"lastModified":950}]"""
            val result = resolver.resolvePush("document", data)
            assertTrue(result.applied > 0)
        }

        @Test
        fun `returns empty result for unknown entity type`() = runTest {
            val result = resolver.resolvePush("unknown_type", "[{}]")
            assertEquals(0, result.applied)
            assertEquals(0, result.skipped)
        }
    }

    class RecordingVehicleDao : VehicleDao {
        var localVehicle: Vehicle? = null
        val upserted = mutableListOf<Vehicle>()

        override fun getVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(localVehicle)
        override fun getAllVehicles() = kotlinx.coroutines.flow.flowOf(emptyList<Vehicle>())
        override suspend fun upsert(vehicle: Vehicle) { upserted.add(vehicle) }
        override suspend fun upsertAll(vehicles: List<Vehicle>) { upserted.addAll(vehicles) }
        override suspend fun delete(vehicle: Vehicle) {}
        override suspend fun deleteById(vehicleId: String) {}
        override fun getVehicleCount() = kotlinx.coroutines.flow.flowOf(0)
        override suspend fun addToOdometer(vehicleId: String, distanceKm: Double, lastModified: Long) {}
        override suspend fun getModifiedSince(since: Long) = emptyList<Vehicle>()
        override suspend fun getVehicleById(id: String): Vehicle? = localVehicle
    }

    class RecordingTripDao : TripDao() {
        var localTrip: Trip? = null
        var localTripPoint: TripPoint? = null
        val upsertedTrips = mutableListOf<Trip>()
        val upsertedTripPoints = mutableListOf<TripPoint>()

        override fun getTripsForVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<Trip>())
        override fun getTripPointsForTrip(tripId: String) = kotlinx.coroutines.flow.flowOf(emptyList<TripPoint>())
        override fun getActiveTrip(vehicleId: String) = kotlinx.coroutines.flow.flowOf<Trip?>(null)
        override fun getTrip(tripId: String) = kotlinx.coroutines.flow.flowOf<Trip?>(null)
        override suspend fun upsertTrip(trip: Trip) { upsertedTrips.add(trip) }
        override suspend fun upsertTrips(trips: List<Trip>) { upsertedTrips.addAll(trips) }
        override suspend fun deleteTrip(trip: Trip) {}
        override suspend fun deleteTripById(tripId: String) {}
        override suspend fun upsertTripPoint(tripPoint: TripPoint) { upsertedTripPoints.add(tripPoint) }
        override suspend fun upsertTripPoints(tripPoints: List<TripPoint>) { upsertedTripPoints.addAll(tripPoints) }
        override suspend fun deleteTripPoint(tripPoint: TripPoint) {}
        override suspend fun getTripsModifiedSince(since: Long) = emptyList<Trip>()
        override suspend fun getTripPointsModifiedSince(since: Long) = emptyList<TripPoint>()
        override suspend fun getTripById(id: String): Trip? = localTrip
        override suspend fun getTripPointById(id: String): TripPoint? = localTripPoint
    }

    class RecordingMaintenanceDao : MaintenanceDao() {
        var localSchedule: MaintenanceSchedule? = null
        var localRecord: MaintenanceRecord? = null
        val upsertedSchedules = mutableListOf<MaintenanceSchedule>()
        val upsertedRecords = mutableListOf<MaintenanceRecord>()

        override fun getSchedulesForVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<MaintenanceSchedule>())
        override fun getSchedule(scheduleId: String) = kotlinx.coroutines.flow.flowOf<MaintenanceSchedule?>(null)
        override suspend fun upsertSchedule(schedule: MaintenanceSchedule) { upsertedSchedules.add(schedule) }
        override suspend fun upsertSchedules(schedules: List<MaintenanceSchedule>) { upsertedSchedules.addAll(schedules) }
        override suspend fun deleteSchedule(schedule: MaintenanceSchedule) {}
        override suspend fun deleteScheduleById(scheduleId: String) {}
        override fun getRecordsForSchedule(scheduleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<MaintenanceRecord>())
        override fun getRecordsForVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<MaintenanceRecord>())
        override fun getRecord(recordId: String) = kotlinx.coroutines.flow.flowOf<MaintenanceRecord?>(null)
        override suspend fun upsertRecord(record: MaintenanceRecord) { upsertedRecords.add(record) }
        override suspend fun upsertRecords(records: List<MaintenanceRecord>) { upsertedRecords.addAll(records) }
        override suspend fun deleteRecord(record: MaintenanceRecord) {}
        override suspend fun deleteRecordById(recordId: String) {}
        override suspend fun deleteRecordsByScheduleId(scheduleId: String) {}
        override suspend fun getSchedulesModifiedSince(since: Long) = emptyList<MaintenanceSchedule>()
        override suspend fun getRecordsModifiedSince(since: Long) = emptyList<MaintenanceRecord>()
        override suspend fun getScheduleById(id: String): MaintenanceSchedule? = localSchedule
        override suspend fun getRecordById(id: String): MaintenanceRecord? = localRecord
    }

    class RecordingFuelDao : FuelDao {
        var localFuelLog: FuelLog? = null
        val upsertedFuelLogs = mutableListOf<FuelLog>()

        override fun getFuelLogsForVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<FuelLog>())
        override fun getLastFullTankEntry(vehicleId: String) = kotlinx.coroutines.flow.flowOf<FuelLog?>(null)
        override fun getFuelLog(fuelLogId: String) = kotlinx.coroutines.flow.flowOf<FuelLog?>(null)
        override suspend fun upsertFuelLog(fuelLog: FuelLog) { upsertedFuelLogs.add(fuelLog) }
        override suspend fun upsertFuelLogs(fuelLogs: List<FuelLog>) { upsertedFuelLogs.addAll(fuelLogs) }
        override suspend fun deleteFuelLog(fuelLog: FuelLog) {}
        override suspend fun deleteFuelLogById(fuelLogId: String) {}
        override suspend fun getFuelLogsModifiedSince(since: Long) = emptyList<FuelLog>()
        override suspend fun getFuelLogById(id: String): FuelLog? = localFuelLog
    }

    class RecordingDocumentDao : DocumentDao {
        var localDocument: Document? = null
        val upsertedDocuments = mutableListOf<Document>()

        override fun getDocumentsForVehicle(vehicleId: String) = kotlinx.coroutines.flow.flowOf(emptyList<Document>())
        override fun getExpiringDocuments(vehicleId: String, threshold: Long) = kotlinx.coroutines.flow.flowOf(emptyList<Document>())
        override fun getDocument(documentId: String) = kotlinx.coroutines.flow.flowOf<Document?>(null)
        override suspend fun upsertDocument(document: Document) { upsertedDocuments.add(document) }
        override suspend fun upsertDocuments(documents: List<Document>) { upsertedDocuments.addAll(documents) }
        override suspend fun deleteDocument(document: Document) {}
        override suspend fun deleteDocumentById(documentId: String) {}
        override suspend fun getDocumentsModifiedSince(since: Long) = emptyList<Document>()
        override suspend fun getDocumentById(id: String): Document? = localDocument
    }
}
