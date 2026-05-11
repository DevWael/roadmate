package com.roadmate.core.ui.components

import com.roadmate.core.database.entity.Document
import com.roadmate.core.database.entity.DocumentType

data class AddDocumentFormState(
    val vehicleId: String = "",
    val type: DocumentType = DocumentType.INSURANCE,
    val name: String = "",
    val expiryDate: Long = System.currentTimeMillis(),
    val reminderDaysBefore: String = "30",
    val notes: String = "",
    val errors: Map<String, String> = emptyMap(),
) {
    val isSaveEnabled: Boolean
        get() = name.isNotBlank() &&
                (reminderDaysBefore.isBlank() || (reminderDaysBefore.toIntOrNull()?.let { it >= 0 } == true))

    fun validate(): AddDocumentFormState {
        val errors = mutableMapOf<String, String>()

        if (name.isBlank()) {
            errors["name"] = "Name is required"
        }

        val reminderValue = reminderDaysBefore.toIntOrNull()
        if (reminderDaysBefore.isNotBlank() && (reminderValue == null || reminderValue < 0)) {
            errors["reminderDays"] = "Must be 0 or greater"
        }

        return copy(errors = errors)
    }

    fun toDocument(existingId: String? = null): Document {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Document name must not be blank" }
        val reminderValue = reminderDaysBefore.toIntOrNull() ?: 30

        return Document(
            id = existingId ?: java.util.UUID.randomUUID().toString(),
            vehicleId = vehicleId,
            type = type,
            name = trimmedName,
            expiryDate = expiryDate,
            reminderDaysBefore = reminderValue,
            notes = notes.ifBlank { null },
        )
    }

    companion object {
        fun fromDocument(document: Document): AddDocumentFormState {
            return AddDocumentFormState(
                vehicleId = document.vehicleId,
                type = document.type,
                name = document.name,
                expiryDate = document.expiryDate,
                reminderDaysBefore = document.reminderDaysBefore.toString(),
                notes = document.notes ?: "",
            )
        }
    }
}
