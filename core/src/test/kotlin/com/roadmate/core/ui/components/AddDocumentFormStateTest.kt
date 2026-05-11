package com.roadmate.core.ui.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import com.roadmate.core.database.entity.DocumentType

@DisplayName("AddDocumentFormState")
class AddDocumentFormStateTest {

    @Nested
    @DisplayName("validation")
    inner class Validation {

        @Test
        @DisplayName("valid form has no errors")
        fun validFormNoErrors() {
            val state = validForm().validate()
            assertTrue(state.errors.isEmpty())
        }

        @Test
        @DisplayName("blank name produces required error")
        fun blankNameRequired() {
            val state = validForm().copy(name = "").validate()
            assertEquals("Name is required", state.errors["name"])
        }

        @Test
        @DisplayName("whitespace-only name produces required error")
        fun whitespaceNameRequired() {
            val state = validForm().copy(name = "   ").validate()
            assertEquals("Name is required", state.errors["name"])
        }

        @Test
        @DisplayName("zero reminder days is valid (disables reminders)")
        fun zeroReminderDaysValid() {
            val state = validForm().copy(reminderDaysBefore = "0").validate()
            assertTrue(state.errors.isEmpty())
        }

        @Test
        @DisplayName("negative reminder days produces error")
        fun negativeReminderDaysError() {
            val state = validForm().copy(reminderDaysBefore = "-5").validate()
            assertEquals("Must be 0 or greater", state.errors["reminderDays"])
        }

        @Test
        @DisplayName("non-numeric reminder days produces error")
        fun nonNumericReminderDaysError() {
            val state = validForm().copy(reminderDaysBefore = "abc").validate()
            assertEquals("Must be 0 or greater", state.errors["reminderDays"])
        }

        @Test
        @DisplayName("blank reminder days is valid (uses default)")
        fun blankReminderDaysValid() {
            val state = validForm().copy(reminderDaysBefore = "").validate()
            assertTrue(state.errors.isEmpty())
        }

        @Test
        @DisplayName("custom reminder days value is valid")
        fun customReminderDaysValid() {
            val state = validForm().copy(reminderDaysBefore = "14").validate()
            assertTrue(state.errors.isEmpty())
        }
    }

    @Nested
    @DisplayName("isSaveEnabled")
    inner class IsSaveEnabled {

        @Test
        @DisplayName("enabled when form is valid")
        fun enabledWhenValid() {
            val state = validForm().validate()
            assertTrue(state.isSaveEnabled)
        }

        @Test
        @DisplayName("disabled when name is blank")
        fun disabledWhenNameBlank() {
            val state = validForm().copy(name = "").validate()
            assertFalse(state.isSaveEnabled)
        }

        @Test
        @DisplayName("disabled when validation errors exist")
        fun disabledWhenErrors() {
            val state = validForm().copy(name = "").validate()
            assertFalse(state.isSaveEnabled)
        }

        @Test
        @DisplayName("disabled when reminder days is non-numeric without validate")
        fun disabledWhenReminderInvalid() {
            val state = validForm().copy(reminderDaysBefore = "abc")
            assertFalse(state.isSaveEnabled)
        }
    }

    @Nested
    @DisplayName("toDocument")
    inner class ToDocument {

        @Test
        @DisplayName("creates document with correct fields")
        fun createsDocument() {
            val state = validForm()
            val document = state.toDocument()

            assertEquals("veh-1", document.vehicleId)
            assertEquals(DocumentType.INSURANCE, document.type)
            assertEquals("Car Insurance", document.name)
            assertEquals(1700000000000L, document.expiryDate)
            assertEquals(30, document.reminderDaysBefore)
            assertNull(document.notes)
        }

        @Test
        @DisplayName("uses provided ID for edit")
        fun usesProvidedId() {
            val state = validForm()
            val document = state.toDocument(existingId = "existing-id")
            assertEquals("existing-id", document.id)
        }

        @Test
        @DisplayName("generates new ID when none provided")
        fun generatesNewId() {
            val state = validForm()
            val document = state.toDocument()
            assertNotNull(document.id)
            assertTrue(document.id.isNotEmpty())
        }

        @Test
        @DisplayName("blank notes maps to null")
        fun blankNotesToNull() {
            val state = validForm().copy(notes = "")
            val document = state.toDocument()
            assertNull(document.notes)
        }

        @Test
        @DisplayName("non-blank notes preserved")
        fun notesPreserved() {
            val state = validForm().copy(notes = "Renew online")
            val document = state.toDocument()
            assertEquals("Renew online", document.notes)
        }

        @Test
        @DisplayName("blank reminder days defaults to 30")
        fun blankReminderDefaults() {
            val state = validForm().copy(reminderDaysBefore = "")
            val document = state.toDocument()
            assertEquals(30, document.reminderDaysBefore)
        }

        @Test
        @DisplayName("zero reminder days produces zero")
        fun zeroReminderDaysProducesZero() {
            val state = validForm().copy(reminderDaysBefore = "0")
            val document = state.toDocument()
            assertEquals(0, document.reminderDaysBefore)
        }

        @Test
        @DisplayName("throws on whitespace-only name")
        fun throwsOnBlankName() {
            val state = validForm().copy(name = "   ")
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                state.toDocument()
            }
        }
    }

    @Nested
    @DisplayName("fromDocument")
    inner class FromDocument {

        @Test
        @DisplayName("populates form from existing document")
        fun populatesFromDocument() {
            val document = createTestDocument()
            val state = AddDocumentFormState.fromDocument(document)

            assertEquals(document.vehicleId, state.vehicleId)
            assertEquals(document.type, state.type)
            assertEquals(document.name, state.name)
            assertEquals(document.expiryDate, state.expiryDate)
            assertEquals(document.reminderDaysBefore.toString(), state.reminderDaysBefore)
            assertEquals(document.notes ?: "", state.notes)
        }

        @Test
        @DisplayName("null notes maps to empty string")
        fun nullNotesToEmpty() {
            val document = createTestDocument().copy(notes = null)
            val state = AddDocumentFormState.fromDocument(document)
            assertEquals("", state.notes)
        }
    }

    private fun validForm() = AddDocumentFormState(
        vehicleId = "veh-1",
        type = DocumentType.INSURANCE,
        name = "Car Insurance",
        expiryDate = 1700000000000L,
        reminderDaysBefore = "30",
        notes = "",
    )

    private fun createTestDocument() = com.roadmate.core.database.entity.Document(
        id = "doc-1",
        vehicleId = "veh-1",
        type = DocumentType.LICENSE,
        name = "Driver License",
        expiryDate = 1700000000000L,
        reminderDaysBefore = 14,
        notes = "Renew at DMV",
    )
}
