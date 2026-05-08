package com.roadmate.core.model.sync

import com.roadmate.core.database.entity.Document
import com.roadmate.core.database.entity.DocumentType
import kotlinx.serialization.Serializable

/**
 * Sync DTO for [Document] entity.
 */
@Serializable
data class DocumentSyncDto(
    val id: String,
    val vehicleId: String,
    val type: String,
    val name: String,
    val expiryDate: Long,
    val reminderDaysBefore: Int,
    val notes: String?,
    val lastModified: Long,
)

fun Document.toSyncDto() = DocumentSyncDto(
    id = id,
    vehicleId = vehicleId,
    type = type.name,
    name = name,
    expiryDate = expiryDate,
    reminderDaysBefore = reminderDaysBefore,
    notes = notes,
    lastModified = lastModified,
)

fun DocumentSyncDto.toEntity() = Document(
    id = id,
    vehicleId = vehicleId,
    type = safeEnumValueOf(type, DocumentType.OTHER),
    name = name,
    expiryDate = expiryDate,
    reminderDaysBefore = reminderDaysBefore,
    notes = notes,
    lastModified = lastModified,
)
