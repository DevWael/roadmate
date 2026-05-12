package com.roadmate.core.sync

import com.roadmate.core.database.dao.DocumentDao
import com.roadmate.core.database.dao.FuelDao
import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.dao.TripDao
import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.model.sync.toSyncDto
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeltaSyncEngine @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val tripDao: TripDao,
    private val maintenanceDao: MaintenanceDao,
    private val fuelDao: FuelDao,
    private val documentDao: DocumentDao,
    private val batcher: SyncBatcher,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun queryDeltas(since: Long): List<SyncPushDto> {
        val deltas = mutableListOf<SyncPushDto>()

        vehicleDao.getModifiedSince(since).takeIf { it.isNotEmpty() }?.let {
            deltas.add(SyncPushDto("vehicle", json.encodeToString(it.map { v -> v.toSyncDto() })))
        }

        tripDao.getTripsModifiedSince(since).takeIf { it.isNotEmpty() }?.let {
            deltas.add(SyncPushDto("trip", json.encodeToString(it.map { t -> t.toSyncDto() })))
        }

        tripDao.getTripPointsModifiedSince(since).takeIf { it.isNotEmpty() }?.let { points ->
            val batches = batcher.batchTripPoints(points)
            for (batch in batches) {
                deltas.add(SyncPushDto("trip_point", json.encodeToString(batch.map { tp -> tp.toSyncDto() })))
            }
        }

        maintenanceDao.getSchedulesModifiedSince(since).takeIf { it.isNotEmpty() }?.let {
            deltas.add(SyncPushDto("maintenance_schedule", json.encodeToString(it.map { s -> s.toSyncDto() })))
        }

        maintenanceDao.getRecordsModifiedSince(since).takeIf { it.isNotEmpty() }?.let {
            deltas.add(SyncPushDto("maintenance_record", json.encodeToString(it.map { r -> r.toSyncDto() })))
        }

        fuelDao.getFuelLogsModifiedSince(since).takeIf { it.isNotEmpty() }?.let {
            deltas.add(SyncPushDto("fuel_log", json.encodeToString(it.map { f -> f.toSyncDto() })))
        }

        documentDao.getDocumentsModifiedSince(since).takeIf { it.isNotEmpty() }?.let {
            deltas.add(SyncPushDto("document", json.encodeToString(it.map { d -> d.toSyncDto() })))
        }

        return deltas
    }
}

