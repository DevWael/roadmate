package com.roadmate.core.util

import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.repository.TripRepository
import com.roadmate.core.repository.VehicleRepository
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashRecoveryManager @Inject constructor(
    private val journal: CrashRecoveryJournal,
    private val tripRepository: TripRepository,
    private val vehicleRepository: VehicleRepository,
) {
    suspend fun recover() {
        val entry = journal.read()
        if (entry == null) {
            Timber.d("CrashRecovery: no journal entry found, nothing to recover")
            return
        }

        Timber.i("CrashRecovery: found journal entry for trip ${entry.tripId}")

        val trip = tripRepository.getTrip(entry.tripId).firstOrNull()
        if (trip == null || trip.status != TripStatus.ACTIVE) {
            Timber.w("CrashRecovery: trip ${entry.tripId} not active in Room, clearing stale journal")
            journal.clear()
            return
        }

        val finalizedTrip = trip.copy(
            endTime = entry.lastFlushTimestamp,
            distanceKm = entry.distanceKm,
            durationMs = entry.durationMs,
            endOdometerKm = entry.odometerKm,
            status = TripStatus.INTERRUPTED,
        )

        val saveResult = tripRepository.saveTrip(finalizedTrip)
        saveResult
            .onSuccess { Timber.i("CrashRecovery: finalized trip ${entry.tripId} as INTERRUPTED") }
            .onFailure {
                Timber.e(it, "CrashRecovery: failed to finalize trip ${entry.tripId}, keeping journal for retry")
                return
            }

        updateVehicleOdometer(entry.vehicleId, entry.odometerKm)

        journal.clear()
        Timber.i("CrashRecovery: journal cleared after recovery")
    }

    private suspend fun updateVehicleOdometer(vehicleId: String, odometerKm: Double) {
        val vehicle = vehicleRepository.getVehicle(vehicleId).firstOrNull()
        if (vehicle == null) {
            Timber.w("CrashRecovery: vehicle $vehicleId not found, cannot update odometer")
            return
        }

        val updated = vehicle.copy(odometerKm = odometerKm)
        vehicleRepository.saveVehicle(updated)
            .onSuccess { Timber.i("CrashRecovery: updated vehicle odometer to $odometerKm km") }
            .onFailure { Timber.e(it, "CrashRecovery: failed to update vehicle odometer") }
    }
}
