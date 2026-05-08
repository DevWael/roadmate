package com.roadmate.core.database.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [Document] entity and [DocumentType] enum.
 * Validates default values, field assignments, enum completeness, and data class contract.
 */
class DocumentTest {

    @Test
    fun `document creates with UUID default id`() {
        val doc = createTestDocument()
        assertTrue(doc.id.isNotBlank())
        assertTrue(doc.id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
    }

    @Test
    fun `two documents get different default ids`() {
        val d1 = createTestDocument()
        val d2 = createTestDocument()
        assertNotEquals(d1.id, d2.id)
    }

    @Test
    fun `document preserves all field values`() {
        val doc = Document(
            id = "doc-1",
            vehicleId = "v-1",
            type = DocumentType.INSURANCE,
            name = "Car Insurance 2026",
            expiryDate = 1700000000000L,
            reminderDaysBefore = 14,
            notes = "Renewal at AXA",
            lastModified = 2000L,
        )

        assertEquals("doc-1", doc.id)
        assertEquals("v-1", doc.vehicleId)
        assertEquals(DocumentType.INSURANCE, doc.type)
        assertEquals("Car Insurance 2026", doc.name)
        assertEquals(1700000000000L, doc.expiryDate)
        assertEquals(14, doc.reminderDaysBefore)
        assertEquals("Renewal at AXA", doc.notes)
        assertEquals(2000L, doc.lastModified)
    }

    @Test
    fun `document reminderDaysBefore defaults to 30`() {
        val doc = createTestDocument()
        assertEquals(30, doc.reminderDaysBefore)
    }

    @Test
    fun `document notes defaults to null`() {
        val doc = createTestDocument()
        assertNull(doc.notes)
    }

    @Test
    fun `document lastModified defaults to current time`() {
        val before = System.currentTimeMillis()
        val doc = createTestDocument()
        val after = System.currentTimeMillis()

        assertTrue(doc.lastModified in before..after)
    }

    @Test
    fun `documentType enum has expected values`() {
        val values = DocumentType.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(DocumentType.INSURANCE))
        assertTrue(values.contains(DocumentType.LICENSE))
        assertTrue(values.contains(DocumentType.REGISTRATION))
        assertTrue(values.contains(DocumentType.OTHER))
    }

    @Test
    fun `document copy creates modified instance`() {
        val original = createTestDocument()
        val modified = original.copy(name = "Updated Doc", reminderDaysBefore = 7)

        assertEquals(original.id, modified.id)
        assertEquals("Updated Doc", modified.name)
        assertEquals(7, modified.reminderDaysBefore)
    }

    @Test
    fun `document equality based on all fields`() {
        val d1 = Document(
            id = "same-id",
            vehicleId = "v-1",
            type = DocumentType.LICENSE,
            name = "Driver License",
            expiryDate = 1700000000000L,
            reminderDaysBefore = 30,
            notes = null,
            lastModified = 1000L,
        )
        val d2 = d1.copy()

        assertEquals(d1, d2)
        assertEquals(d1.hashCode(), d2.hashCode())
    }

    private fun createTestDocument(): Document = Document(
        vehicleId = "v-1",
        type = DocumentType.INSURANCE,
        name = "Test Document",
        expiryDate = System.currentTimeMillis() + 86400000L * 365,
    )
}
