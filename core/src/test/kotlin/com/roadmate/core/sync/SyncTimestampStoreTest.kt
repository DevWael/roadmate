package com.roadmate.core.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SyncTimestampStore")
class SyncTimestampStoreTest {

    private lateinit var store: SyncTimestampStore
    private lateinit var fakePreferences: MutableStateFlow<Preferences>

    @BeforeEach
    fun setUp() {
        fakePreferences = MutableStateFlow(emptyPreferences())
        val dataStore = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = fakePreferences
            override suspend fun updateData(
                transformer: suspend (Preferences) -> Preferences,
            ): Preferences {
                val new = transformer(fakePreferences.value)
                fakePreferences.value = new
                return new
            }
        }
        store = SyncTimestampStore(dataStore)
    }

    @Nested
    @DisplayName("getLastSyncTimestamp")
    inner class GetLastSyncTimestamp {

        @Test
        fun `returns 0 when no timestamp stored`() = runTest {
            val ts = store.getLastSyncTimestamp()
            assertEquals(0L, ts)
        }

        @Test
        fun `returns stored timestamp`() = runTest {
            store.setLastSyncTimestamp(1000L)
            val ts = store.getLastSyncTimestamp()
            assertEquals(1000L, ts)
        }
    }

    @Nested
    @DisplayName("setLastSyncTimestamp")
    inner class SetLastSyncTimestamp {

        @Test
        fun `stores timestamp`() = runTest {
            store.setLastSyncTimestamp(5000L)
            assertEquals(5000L, store.getLastSyncTimestamp())
        }

        @Test
        fun `overwrites previous timestamp`() = runTest {
            store.setLastSyncTimestamp(1000L)
            store.setLastSyncTimestamp(2000L)
            assertEquals(2000L, store.getLastSyncTimestamp())
        }
    }

    @Nested
    @DisplayName("per-entity timestamps")
    inner class PerEntityTimestamps {

        @Test
        fun `stores and retrieves per-entity-type timestamp`() = runTest {
            store.setEntityTimestamp("vehicle", 1000L)
            store.setEntityTimestamp("trip", 2000L)
            assertEquals(1000L, store.getEntityTimestamp("vehicle"))
            assertEquals(2000L, store.getEntityTimestamp("trip"))
        }

        @Test
        fun `returns 0 for unset entity type`() = runTest {
            assertEquals(0L, store.getEntityTimestamp("unknown"))
        }
    }
}
