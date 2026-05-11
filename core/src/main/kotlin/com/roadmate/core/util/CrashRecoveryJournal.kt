package com.roadmate.core.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class JournalEntry(
    val tripId: String,
    val vehicleId: String,
    val distanceKm: Double,
    val durationMs: Long,
    val odometerKm: Double,
    val lastFlushTimestamp: Long,
    val status: String,
)

object JournalKeys {
    val TRIP_ID = stringPreferencesKey("journal_trip_id")
    val VEHICLE_ID = stringPreferencesKey("journal_vehicle_id")
    val DISTANCE_KM = doublePreferencesKey("journal_distance_km")
    val DURATION_MS = longPreferencesKey("journal_duration_ms")
    val ODOMETER_KM = doublePreferencesKey("journal_odometer_km")
    val LAST_FLUSH = longPreferencesKey("journal_last_flush")
    val STATUS = stringPreferencesKey("journal_status")
}

@Singleton
open class CrashRecoveryJournal @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    open suspend fun write(
        tripId: String,
        vehicleId: String,
        distanceKm: Double,
        durationMs: Long,
        odometerKm: Double,
        lastFlushTimestamp: Long,
    ) {
        dataStore.edit { prefs ->
            prefs[JournalKeys.TRIP_ID] = tripId
            prefs[JournalKeys.VEHICLE_ID] = vehicleId
            prefs[JournalKeys.DISTANCE_KM] = distanceKm
            prefs[JournalKeys.DURATION_MS] = durationMs
            prefs[JournalKeys.ODOMETER_KM] = odometerKm
            prefs[JournalKeys.LAST_FLUSH] = lastFlushTimestamp
            prefs[JournalKeys.STATUS] = "ACTIVE"
        }
    }

    open suspend fun read(): JournalEntry? {
        val prefs = dataStore.data.first()
        val tripId = prefs[JournalKeys.TRIP_ID] ?: return null
        return JournalEntry(
            tripId = tripId,
            vehicleId = prefs[JournalKeys.VEHICLE_ID] ?: return null,
            distanceKm = prefs[JournalKeys.DISTANCE_KM] ?: 0.0,
            durationMs = prefs[JournalKeys.DURATION_MS] ?: 0L,
            odometerKm = prefs[JournalKeys.ODOMETER_KM] ?: 0.0,
            lastFlushTimestamp = prefs[JournalKeys.LAST_FLUSH] ?: 0L,
            status = prefs[JournalKeys.STATUS] ?: "ACTIVE",
        )
    }

    open suspend fun hasActiveTrip(): Boolean {
        return dataStore.data.map { it[JournalKeys.TRIP_ID] != null }.first()
    }

    open suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(JournalKeys.TRIP_ID)
            prefs.remove(JournalKeys.VEHICLE_ID)
            prefs.remove(JournalKeys.DISTANCE_KM)
            prefs.remove(JournalKeys.DURATION_MS)
            prefs.remove(JournalKeys.ODOMETER_KM)
            prefs.remove(JournalKeys.LAST_FLUSH)
            prefs.remove(JournalKeys.STATUS)
        }
    }
}
