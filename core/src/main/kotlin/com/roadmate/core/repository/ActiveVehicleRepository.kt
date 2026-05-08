package com.roadmate.core.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveVehicleRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        val ACTIVE_VEHICLE_ID_KEY = stringPreferencesKey("active_vehicle_id")
    }

    val activeVehicleId: Flow<String?> =
        dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[ACTIVE_VEHICLE_ID_KEY] }

    suspend fun setActiveVehicle(vehicleId: String) {
        dataStore.edit { it[ACTIVE_VEHICLE_ID_KEY] = vehicleId }
    }

    suspend fun clearActiveVehicle() {
        dataStore.edit { it.remove(ACTIVE_VEHICLE_ID_KEY) }
    }
}
