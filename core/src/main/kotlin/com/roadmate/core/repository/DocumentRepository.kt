package com.roadmate.core.repository

import com.roadmate.core.database.dao.DocumentDao
import com.roadmate.core.database.entity.Document
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for document domain operations.
 *
 * Reads delegate directly to [DocumentDao] and return [Flow] for reactive observation.
 * Writes return [Result] via [runCatching] to encapsulate exceptions.
 */
@Singleton
class DocumentRepository @Inject constructor(
    private val documentDao: DocumentDao,
) {

    fun getDocumentsForVehicle(vehicleId: String): Flow<List<Document>> =
        documentDao.getDocumentsForVehicle(vehicleId)

    fun getExpiringDocuments(vehicleId: String, threshold: Long): Flow<List<Document>> =
        documentDao.getExpiringDocuments(vehicleId, threshold)

    fun getDocument(documentId: String): Flow<Document?> =
        documentDao.getDocument(documentId)

    suspend fun saveDocument(document: Document): Result<Unit> =
        runCatching { documentDao.upsertDocument(document) }

    suspend fun saveDocuments(documents: List<Document>): Result<Unit> =
        runCatching { documentDao.upsertDocuments(documents) }

    suspend fun deleteDocument(document: Document): Result<Unit> =
        runCatching { documentDao.deleteDocument(document) }

    suspend fun deleteDocumentById(documentId: String): Result<Unit> =
        runCatching { documentDao.deleteDocumentById(documentId) }
}
