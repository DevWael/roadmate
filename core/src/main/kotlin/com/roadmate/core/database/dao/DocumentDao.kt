package com.roadmate.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.roadmate.core.database.entity.Document
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [Document] entities.
 *
 * All queries are scoped by vehicleId — no unscoped queries.
 * Reads return [Flow] for reactive observation.
 * Writes use [@Upsert] for idempotent insert-or-update.
 */
@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents WHERE vehicle_id = :vehicleId ORDER BY expiry_date ASC")
    fun getDocumentsForVehicle(vehicleId: String): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE vehicle_id = :vehicleId AND expiry_date <= :threshold ORDER BY expiry_date ASC")
    fun getExpiringDocuments(vehicleId: String, threshold: Long): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE id = :documentId")
    fun getDocument(documentId: String): Flow<Document?>

    @Upsert
    suspend fun upsertDocument(document: Document)

    @Upsert
    suspend fun upsertDocuments(documents: List<Document>)

    @Delete
    suspend fun deleteDocument(document: Document)

    @Query("DELETE FROM documents WHERE id = :documentId")
    suspend fun deleteDocumentById(documentId: String)

    @Query("SELECT * FROM documents WHERE last_modified > :since")
    suspend fun getDocumentsModifiedSince(since: Long): List<Document>
}
