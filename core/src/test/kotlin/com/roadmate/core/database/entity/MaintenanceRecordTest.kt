package com.roadmate.core.database.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [MaintenanceRecord] entity.
 */
class MaintenanceRecordTest {

    @Test
    fun `record creates with UUID default id`() {
        val record = createTestRecord()
        assertTrue(record.id.isNotBlank())
        assertTrue(record.id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
    }

    @Test
    fun `record preserves all field values`() {
        val record = MaintenanceRecord(
            id = "rec-1",
            scheduleId = "sched-1",
            vehicleId = "vehicle-1",
            datePerformed = 1700000000000L,
            odometerKm = 90000.0,
            cost = 150.50,
            location = "AutoService Center",
            notes = "Used synthetic oil",
            lastModified = 1000L,
        )

        assertEquals("rec-1", record.id)
        assertEquals("sched-1", record.scheduleId)
        assertEquals("vehicle-1", record.vehicleId)
        assertEquals(1700000000000L, record.datePerformed)
        assertEquals(90000.0, record.odometerKm)
        assertEquals(150.50, record.cost)
        assertEquals("AutoService Center", record.location)
        assertEquals("Used synthetic oil", record.notes)
        assertEquals(1000L, record.lastModified)
    }

    @Test
    fun `record nullable fields default to null`() {
        val record = createTestRecord()
        assertNull(record.cost)
        assertNull(record.location)
        assertNull(record.notes)
    }

    @Test
    fun `record lastModified defaults to current time`() {
        val before = System.currentTimeMillis()
        val record = createTestRecord()
        val after = System.currentTimeMillis()
        assertTrue(record.lastModified in before..after)
    }

    private fun createTestRecord(): MaintenanceRecord = MaintenanceRecord(
        scheduleId = "sched-1",
        vehicleId = "vehicle-1",
        datePerformed = System.currentTimeMillis(),
        odometerKm = 90000.0,
    )
}
