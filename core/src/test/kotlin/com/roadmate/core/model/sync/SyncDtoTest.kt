package com.roadmate.core.model.sync

import com.roadmate.core.database.entity.Document
import com.roadmate.core.database.entity.DocumentType
import com.roadmate.core.database.entity.FuelLog
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.OdometerUnit
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Unit tests for Sync DTOs and their mapper extension functions.
 * Validates @Serializable correctness and round-trip entity ↔ DTO conversion.
 */
class SyncDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    // --- TripSyncDto ---

    @Test
    fun `TripSyncDto serializes and deserializes`() {
        val dto = TripSyncDto(
            id = "t-1", vehicleId = "v-1", startTime = 1000L,
            endTime = 2000L, distanceKm = 45.5, durationMs = 1800000L,
            maxSpeedKmh = 120.0, avgSpeedKmh = 65.0, estimatedFuelL = 4.2,
            startOdometerKm = 85000.0, endOdometerKm = 85045.5,
            status = "COMPLETED", lastModified = 3000L,
        )
        val serialized = json.encodeToString(TripSyncDto.serializer(), dto)
        val deserialized = json.decodeFromString(TripSyncDto.serializer(), serialized)
        assertEquals(dto, deserialized)
    }

    @Test
    fun `Trip toSyncDto round-trips correctly`() {
        val entity = Trip(
            id = "t-1", vehicleId = "v-1", startTime = 1000L, endTime = 2000L,
            distanceKm = 45.5, durationMs = 1800000L, maxSpeedKmh = 120.0,
            avgSpeedKmh = 65.0, estimatedFuelL = 4.2, startOdometerKm = 85000.0,
            endOdometerKm = 85045.5, status = TripStatus.COMPLETED, lastModified = 3000L,
        )
        val dto = entity.toSyncDto()
        val backToEntity = dto.toEntity()

        assertEquals(entity.id, backToEntity.id)
        assertEquals(entity.vehicleId, backToEntity.vehicleId)
        assertEquals(entity.distanceKm, backToEntity.distanceKm)
        assertEquals(entity.status, backToEntity.status)
        assertEquals(entity.lastModified, backToEntity.lastModified)
    }

    // --- TripPointSyncDto ---

    @Test
    fun `TripPointSyncDto serializes and deserializes`() {
        val dto = TripPointSyncDto(
            id = "tp-1", tripId = "t-1", latitude = 30.0444, longitude = 31.2357,
            speedKmh = 60.0, altitude = 75.0, accuracy = 5.0f,
            timestamp = 1000L, lastModified = 2000L,
        )
        val serialized = json.encodeToString(TripPointSyncDto.serializer(), dto)
        val deserialized = json.decodeFromString(TripPointSyncDto.serializer(), serialized)
        assertEquals(dto, deserialized)
    }

    @Test
    fun `TripPoint toSyncDto round-trips correctly`() {
        val entity = TripPoint(
            id = "tp-1", tripId = "t-1", latitude = 30.0444, longitude = 31.2357,
            speedKmh = 60.0, altitude = 75.0, accuracy = 5.0f,
            timestamp = 1000L, lastModified = 2000L,
        )
        val dto = entity.toSyncDto()
        val backToEntity = dto.toEntity()

        assertEquals(entity.id, backToEntity.id)
        assertEquals(entity.tripId, backToEntity.tripId)
        assertEquals(entity.latitude, backToEntity.latitude)
        assertEquals(entity.lastModified, backToEntity.lastModified)
    }

    // --- FuelLogSyncDto ---

    @Test
    fun `FuelLogSyncDto serializes and deserializes`() {
        val dto = FuelLogSyncDto(
            id = "f-1", vehicleId = "v-1", date = 1000L, odometerKm = 86000.0,
            liters = 45.0, pricePerLiter = 12.75, totalCost = 573.75,
            isFullTank = true, station = "Total", lastModified = 2000L,
        )
        val serialized = json.encodeToString(FuelLogSyncDto.serializer(), dto)
        val deserialized = json.decodeFromString(FuelLogSyncDto.serializer(), serialized)
        assertEquals(dto, deserialized)
    }

    @Test
    fun `FuelLog toSyncDto round-trips correctly`() {
        val entity = FuelLog(
            id = "f-1", vehicleId = "v-1", date = 1000L, odometerKm = 86000.0,
            liters = 45.0, pricePerLiter = 12.75, totalCost = 573.75,
            isFullTank = true, station = "Total", lastModified = 2000L,
        )
        val dto = entity.toSyncDto()
        val backToEntity = dto.toEntity()

        assertEquals(entity.id, backToEntity.id)
        assertEquals(entity.vehicleId, backToEntity.vehicleId)
        assertEquals(entity.liters, backToEntity.liters)
        assertEquals(entity.station, backToEntity.station)
    }

    // --- DocumentSyncDto ---

    @Test
    fun `DocumentSyncDto serializes and deserializes`() {
        val dto = DocumentSyncDto(
            id = "d-1", vehicleId = "v-1", type = "INSURANCE",
            name = "Insurance 2026", expiryDate = 1700000000000L,
            reminderDaysBefore = 30, notes = null, lastModified = 2000L,
        )
        val serialized = json.encodeToString(DocumentSyncDto.serializer(), dto)
        val deserialized = json.decodeFromString(DocumentSyncDto.serializer(), serialized)
        assertEquals(dto, deserialized)
    }

    @Test
    fun `Document toSyncDto round-trips correctly`() {
        val entity = Document(
            id = "d-1", vehicleId = "v-1", type = DocumentType.INSURANCE,
            name = "Insurance 2026", expiryDate = 1700000000000L,
            reminderDaysBefore = 30, notes = "Renewal notes", lastModified = 2000L,
        )
        val dto = entity.toSyncDto()
        val backToEntity = dto.toEntity()

        assertEquals(entity.id, backToEntity.id)
        assertEquals(entity.type, backToEntity.type)
        assertEquals(entity.notes, backToEntity.notes)
    }

    // --- MaintenanceScheduleSyncDto ---

    @Test
    fun `MaintenanceScheduleSyncDto serializes and deserializes`() {
        val dto = MaintenanceScheduleSyncDto(
            id = "ms-1", vehicleId = "v-1", name = "Oil Change",
            intervalKm = 10000, intervalMonths = 6, lastServiceKm = 80000.0,
            lastServiceDate = 1000L, isCustom = false, lastModified = 2000L,
        )
        val serialized = json.encodeToString(MaintenanceScheduleSyncDto.serializer(), dto)
        val deserialized = json.decodeFromString(MaintenanceScheduleSyncDto.serializer(), serialized)
        assertEquals(dto, deserialized)
    }

    @Test
    fun `MaintenanceSchedule toSyncDto round-trips correctly`() {
        val entity = MaintenanceSchedule(
            id = "ms-1", vehicleId = "v-1", name = "Oil Change",
            intervalKm = 10000, intervalMonths = 6, lastServiceKm = 80000.0,
            lastServiceDate = 1000L, isCustom = false, lastModified = 2000L,
        )
        val dto = entity.toSyncDto()
        val backToEntity = dto.toEntity()

        assertEquals(entity.id, backToEntity.id)
        assertEquals(entity.name, backToEntity.name)
        assertEquals(entity.intervalKm, backToEntity.intervalKm)
    }

    // --- MaintenanceRecordSyncDto ---

    @Test
    fun `MaintenanceRecordSyncDto serializes and deserializes`() {
        val dto = MaintenanceRecordSyncDto(
            id = "mr-1", scheduleId = "ms-1", vehicleId = "v-1",
            datePerformed = 1000L, odometerKm = 85000.0,
            cost = 250.0, location = "AutoService", notes = "Good",
            lastModified = 2000L,
        )
        val serialized = json.encodeToString(MaintenanceRecordSyncDto.serializer(), dto)
        val deserialized = json.decodeFromString(MaintenanceRecordSyncDto.serializer(), serialized)
        assertEquals(dto, deserialized)
    }

    @Test
    fun `MaintenanceRecord toSyncDto round-trips correctly`() {
        val entity = MaintenanceRecord(
            id = "mr-1", scheduleId = "ms-1", vehicleId = "v-1",
            datePerformed = 1000L, odometerKm = 85000.0,
            cost = 250.0, location = "AutoService", notes = "Good",
            lastModified = 2000L,
        )
        val dto = entity.toSyncDto()
        val backToEntity = dto.toEntity()

        assertEquals(entity.id, backToEntity.id)
        assertEquals(entity.scheduleId, backToEntity.scheduleId)
        assertEquals(entity.cost, backToEntity.cost)
    }

    // --- VehicleSyncDto ---

    @Test
    fun `VehicleSyncDto serializes and deserializes`() {
        val dto = VehicleSyncDto(
            id = "v-1", name = "Lancer", make = "Mitsubishi", model = "Lancer EX",
            year = 2015, engineType = "INLINE_4", engineSize = 1.6,
            fuelType = "GASOLINE", plateNumber = "ABC-123", vin = null,
            odometerKm = 85000.0, odometerUnit = "KM",
            cityConsumption = 9.5, highwayConsumption = 6.8, lastModified = 2000L,
        )
        val serialized = json.encodeToString(VehicleSyncDto.serializer(), dto)
        val deserialized = json.decodeFromString(VehicleSyncDto.serializer(), serialized)
        assertEquals(dto, deserialized)
    }

    @Test
    fun `Vehicle toSyncDto round-trips correctly`() {
        val entity = Vehicle(
            id = "v-1", name = "Lancer", make = "Mitsubishi", model = "Lancer EX",
            year = 2015, engineType = EngineType.INLINE_4, engineSize = 1.6,
            fuelType = FuelType.GASOLINE, plateNumber = "ABC-123", vin = null,
            odometerKm = 85000.0, odometerUnit = OdometerUnit.KM,
            cityConsumption = 9.5, highwayConsumption = 6.8, lastModified = 2000L,
        )
        val dto = entity.toSyncDto()
        val backToEntity = dto.toEntity()

        assertEquals(entity.id, backToEntity.id)
        assertEquals(entity.name, backToEntity.name)
        assertEquals(entity.engineType, backToEntity.engineType)
        assertEquals(entity.vin, backToEntity.vin)
    }

    // --- SyncMessage ---

    @Test
    fun `SyncMessage sealed interface has all subtypes`() {
        val status = SyncMessage.SyncStatus(deviceId = "dev-1", timestamp = 1000L, lastSyncTimestamp = 500L)
        val pull = SyncMessage.SyncPull(since = 500L, entityTypes = listOf("trip", "vehicle"))
        val push = SyncMessage.SyncPush(entityType = "trip", data = "data", messageId = "uuid-1", timestamp = 1000L)
        val ack = SyncMessage.SyncAck(success = true, messageId = "uuid-1", timestamp = 1000L, message = null)

        assertNotNull(status)
        assertNotNull(pull)
        assertNotNull(push)
        assertNotNull(ack)
    }
}
