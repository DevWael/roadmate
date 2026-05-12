package com.roadmate.core.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class SyncTimestampStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_LAST_SYNC = longPreferencesKey("last_sync_timestamp")
        private fun entityKey(entityType: String) = longPreferencesKey("sync_ts_$entityType")
    }

    suspend fun getLastSyncTimestamp(): Long {
        return try {
            dataStore.data.first()[KEY_LAST_SYNC] ?: 0L
        } catch (e: IOException) {
            Timber.e(e, "SyncTimestampStore: IOException reading last sync timestamp, defaulting to 0")
            0L
        }
    }

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { it[KEY_LAST_SYNC] = timestamp }
    }

    suspend fun getEntityTimestamp(entityType: String): Long {
        return dataStore.data
            .first()[entityKey(entityType)] ?: 0L
    }

    open suspend fun setEntityTimestamp(entityType: String, timestamp: Long) {
        dataStore.edit { it[entityKey(entityType)] = timestamp }
    }
}
