package com.roadmate.core.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CrashRecoveryJournal")
class CrashRecoveryJournalTest {

    private lateinit var fakeDataStore: FakePreferencesDataStore
    private lateinit var journal: CrashRecoveryJournal

    @BeforeEach
    fun setup() {
        fakeDataStore = FakePreferencesDataStore()
        journal = CrashRecoveryJournal(fakeDataStore.dataStore)
    }

    @Nested
    @DisplayName("write")
    inner class Write {

        @Test
        @DisplayName("writes all journal fields correctly")
        fun writesAllFields() = runTest {
            journal.write(
                tripId = "trip-1",
                vehicleId = "veh-1",
                distanceKm = 12.5,
                durationMs = 30000L,
                odometerKm = 85012.5,
                lastFlushTimestamp = 1000L,
            )

            val state = fakeDataStore.dataStore.data.first()
            assertEquals("trip-1", state[stringPreferencesKey("journal_trip_id")])
            assertEquals("veh-1", state[stringPreferencesKey("journal_vehicle_id")])
            assertEquals(12.5, state[doublePreferencesKey("journal_distance_km")])
            assertEquals(30000L, state[longPreferencesKey("journal_duration_ms")])
            assertEquals(85012.5, state[doublePreferencesKey("journal_odometer_km")])
            assertEquals(1000L, state[longPreferencesKey("journal_last_flush")])
            assertEquals("ACTIVE", state[stringPreferencesKey("journal_status")])
        }

        @Test
        @DisplayName("overwrites previous journal entry")
        fun overwritesPrevious() = runTest {
            journal.write("trip-1", "veh-1", 10.0, 1000L, 85010.0, 100L)
            journal.write("trip-2", "veh-2", 20.0, 2000L, 85020.0, 200L)

            val state = fakeDataStore.dataStore.data.first()
            assertEquals("trip-2", state[stringPreferencesKey("journal_trip_id")])
        }
    }

    @Nested
    @DisplayName("read")
    inner class Read {

        @Test
        @DisplayName("returns null when journal is empty")
        fun returnsNullWhenEmpty() = runTest {
            val entry = journal.read()
            assertNull(entry)
        }

        @Test
        @DisplayName("returns journal entry when present")
        fun returnsEntryWhenPresent() = runTest {
            journal.write("trip-1", "veh-1", 5.0, 500L, 85005.0, 50L)

            val entry = journal.read()
            assertEquals("trip-1", entry!!.tripId)
            assertEquals("veh-1", entry.vehicleId)
        }

        @Test
        @DisplayName("reads all fields correctly")
        fun readsAllFields() = runTest {
            journal.write("trip-1", "veh-1", 5.0, 500L, 85005.0, 50L)

            val entry = journal.read()
            assertEquals("trip-1", entry!!.tripId)
            assertEquals("veh-1", entry.vehicleId)
            assertEquals(5.0, entry.distanceKm, 0.001)
            assertEquals(500L, entry.durationMs)
            assertEquals(85005.0, entry.odometerKm, 0.001)
            assertEquals(50L, entry.lastFlushTimestamp)
            assertEquals("ACTIVE", entry.status)
        }
    }

    @Nested
    @DisplayName("clear")
    inner class Clear {

        @Test
        @DisplayName("clears all journal fields")
        fun clearsAllFields() = runTest {
            journal.write("trip-1", "veh-1", 5.0, 500L, 85005.0, 50L)
            journal.clear()

            val entry = journal.read()
            assertNull(entry)
        }
    }

    @Nested
    @DisplayName("hasActiveTrip")
    inner class HasActiveTrip {

        @Test
        @DisplayName("returns false when journal is empty")
        fun falseWhenEmpty() = runTest {
            val result = journal.hasActiveTrip()
            assertEquals(false, result)
        }

        @Test
        @DisplayName("returns true when journal has ACTIVE status")
        fun trueWhenActive() = runTest {
            journal.write("trip-1", "veh-1", 5.0, 500L, 85005.0, 50L)
            val result = journal.hasActiveTrip()
            assertEquals(true, result)
        }

        @Test
        @DisplayName("returns false after clear")
        fun falseAfterClear() = runTest {
            journal.write("trip-1", "veh-1", 5.0, 500L, 85005.0, 50L)
            journal.clear()
            val result = journal.hasActiveTrip()
            assertEquals(false, result)
        }
    }
}

private class FakePreferencesDataStore {
    private val _prefs = MutableStateFlow<Preferences>(
        androidx.datastore.preferences.core.emptyPreferences()
    )

    val dataStore: DataStore<Preferences> = object : DataStore<Preferences> {
        override val data = _prefs

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences {
            val new = transform(_prefs.value)
            _prefs.value = new
            return new
        }
    }
}
