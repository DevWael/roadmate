package com.roadmate.core.sync

import com.roadmate.core.database.dao.DocumentDao
import com.roadmate.core.database.dao.FuelDao
import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.dao.TripDao
import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.model.sync.DocumentSyncDto
import com.roadmate.core.model.sync.FuelLogSyncDto
import com.roadmate.core.model.sync.MaintenanceRecordSyncDto
import com.roadmate.core.model.sync.MaintenanceScheduleSyncDto
import com.roadmate.core.model.sync.TripPointSyncDto
import com.roadmate.core.model.sync.TripSyncDto
import com.roadmate.core.model.sync.VehicleSyncDto
import com.roadmate.core.model.sync.toEntity
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class ConflictResult(val applied: Int, val skipped: Int)

@Singleton
class ConflictResolver @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val tripDao: TripDao,
    private val maintenanceDao: MaintenanceDao,
    private val fuelDao: FuelDao,
    private val documentDao: DocumentDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun resolvePush(entityType: String, data: String): ConflictResult {
        var applied = 0
        var skipped = 0

        try {
            when (entityType) {
                "vehicle" -> {
                    val dtos = json.decodeFromString<List<VehicleSyncDto>>(data)
                    for (dto in dtos) {
                        val local = vehicleDao.getVehicleById(dto.id)
                        if (local == null || dto.lastModified > local.lastModified) {
                            vehicleDao.upsert(dto.toEntity())
                            applied++
                        } else {
                            skipped++
                        }
                    }
                }
                "trip" -> {
                    val dtos = json.decodeFromString<List<TripSyncDto>>(data)
                    for (dto in dtos) {
                        val local = tripDao.getTripById(dto.id)
                        if (local == null || dto.lastModified > local.lastModified) {
                            tripDao.upsertTrip(dto.toEntity())
                            applied++
                        } else {
                            skipped++
                        }
                    }
                }
                "trip_point" -> {
                    val dtos = json.decodeFromString<List<TripPointSyncDto>>(data)
                    for (dto in dtos) {
                        val local = tripDao.getTripPointById(dto.id)
                        if (local == null || dto.lastModified > local.lastModified) {
                            tripDao.upsertTripPoint(dto.toEntity())
                            applied++
                        } else {
                            skipped++
                        }
                    }
                }
                "maintenance_schedule" -> {
                    val dtos = json.decodeFromString<List<MaintenanceScheduleSyncDto>>(data)
                    for (dto in dtos) {
                        val local = maintenanceDao.getScheduleById(dto.id)
                        if (local == null || dto.lastModified > local.lastModified) {
                            maintenanceDao.upsertSchedule(dto.toEntity())
                            applied++
                        } else {
                            skipped++
                        }
                    }
                }
                "maintenance_record" -> {
                    val dtos = json.decodeFromString<List<MaintenanceRecordSyncDto>>(data)
                    for (dto in dtos) {
                        val local = maintenanceDao.getRecordById(dto.id)
                        if (local == null || dto.lastModified > local.lastModified) {
                            maintenanceDao.upsertRecord(dto.toEntity())
                            applied++
                        } else {
                            skipped++
                        }
                    }
                }
                "fuel_log" -> {
                    val dtos = json.decodeFromString<List<FuelLogSyncDto>>(data)
                    for (dto in dtos) {
                        val local = fuelDao.getFuelLogById(dto.id)
                        if (local == null || dto.lastModified > local.lastModified) {
                            fuelDao.upsertFuelLog(dto.toEntity())
                            applied++
                        } else {
                            skipped++
                        }
                    }
                }
                "document" -> {
                    val dtos = json.decodeFromString<List<DocumentSyncDto>>(data)
                    for (dto in dtos) {
                        val local = documentDao.getDocumentById(dto.id)
                        if (local == null || dto.lastModified > local.lastModified) {
                            documentDao.upsertDocument(dto.toEntity())
                            applied++
                        } else {
                            skipped++
                        }
                    }
                }
                else -> {
                    Timber.w("ConflictResolver: unknown entity type '$entityType'")
                }
            }
        } catch (e: SerializationException) {
            Timber.e(e, "ConflictResolver: malformed JSON for entity type '$entityType'")
            return ConflictResult(applied, skipped)
        }

        return ConflictResult(applied, skipped)
    }
}
