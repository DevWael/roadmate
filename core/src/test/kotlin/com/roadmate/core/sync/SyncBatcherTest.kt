package com.roadmate.core.sync

import com.roadmate.core.database.dao.DocumentDao
import com.roadmate.core.database.dao.FuelDao
import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.dao.TripDao
import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.TripPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SyncBatcher")
class SyncBatcherTest {

    private lateinit var batcher: SyncBatcher

    @BeforeEach
    fun setUp() {
        batcher = SyncBatcher()
    }

    @Nested
    @DisplayName("batchTripPoints")
    inner class BatchTripPoints {

        @Test
        fun `returns single batch when less than 100 points`() {
            val points = (1..50).map { i ->
                TripPoint(
                    id = "tp-$i", tripId = "t-1",
                    latitude = 30.0 + i * 0.001, longitude = 31.0,
                    speedKmh = 60.0, altitude = 75.0, accuracy = 5.0f,
                    timestamp = 1000L + i, lastModified = 2000L + i,
                )
            }
            val batches = batcher.batchTripPoints(points)
            assertEquals(1, batches.size)
            assertEquals(50, batches[0].size)
        }

        @Test
        fun `returns exactly one batch for exactly 100 points`() {
            val points = (1..100).map { i ->
                TripPoint(
                    id = "tp-$i", tripId = "t-1",
                    latitude = 30.0, longitude = 31.0,
                    speedKmh = 60.0, altitude = 75.0, accuracy = 5.0f,
                    timestamp = 1000L + i, lastModified = 2000L + i,
                )
            }
            val batches = batcher.batchTripPoints(points)
            assertEquals(1, batches.size)
            assertEquals(100, batches[0].size)
        }

        @Test
        fun `splits 250 points into 3 batches of 100 100 and 50`() {
            val points = (1..250).map { i ->
                TripPoint(
                    id = "tp-$i", tripId = "t-1",
                    latitude = 30.0, longitude = 31.0,
                    speedKmh = 60.0, altitude = 75.0, accuracy = 5.0f,
                    timestamp = 1000L + i, lastModified = 2000L + i,
                )
            }
            val batches = batcher.batchTripPoints(points)
            assertEquals(3, batches.size)
            assertEquals(100, batches[0].size)
            assertEquals(100, batches[1].size)
            assertEquals(50, batches[2].size)
        }

        @Test
        fun `returns empty list for empty input`() {
            val batches = batcher.batchTripPoints(emptyList())
            assertTrue(batches.isEmpty())
        }

        @Test
        fun `splits 500 points into 5 batches of 100`() {
            val points = (1..500).map { i ->
                TripPoint(
                    id = "tp-$i", tripId = "t-1",
                    latitude = 30.0, longitude = 31.0,
                    speedKmh = 60.0, altitude = 75.0, accuracy = 5.0f,
                    timestamp = 1000L + i, lastModified = 2000L + i,
                )
            }
            val batches = batcher.batchTripPoints(points)
            assertEquals(5, batches.size)
            batches.forEach { assertEquals(100, it.size) }
        }
    }
}
