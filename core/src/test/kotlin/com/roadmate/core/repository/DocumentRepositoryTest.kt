package com.roadmate.core.repository

import com.roadmate.core.database.dao.DocumentDao
import com.roadmate.core.database.entity.Document
import com.roadmate.core.database.entity.DocumentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DocumentRepository].
 * Uses a fake DAO to verify repository delegates correctly and wraps errors in Result.
 */
class DocumentRepositoryTest {

    private lateinit var fakeDao: FakeDocumentDao
    private lateinit var repository: DocumentRepository

    @BeforeEach
    fun setup() {
        fakeDao = FakeDocumentDao()
        repository = DocumentRepository(fakeDao)
    }

    @Test
    fun `saveDocument delegates to dao upsert and returns success`() = runTest {
        val doc = createTestDocument()
        val result = repository.saveDocument(doc)

        assertTrue(result.isSuccess)
        assertEquals(doc, fakeDao.documents[doc.id])
    }

    @Test
    fun `saveDocument returns failure when dao throws`() = runTest {
        fakeDao.shouldThrow = true
        val result = repository.saveDocument(createTestDocument())

        assertTrue(result.isFailure)
    }

    @Test
    fun `getDocumentsForVehicle returns Flow from dao`() = runTest {
        val doc = createTestDocument(id = "d-1", vehicleId = "v-1")
        fakeDao.documents["d-1"] = doc
        fakeDao.updateFlow()

        val result = repository.getDocumentsForVehicle("v-1").first()
        assertEquals(1, result.size)
        assertEquals(doc, result[0])
    }

    @Test
    fun `getExpiringDocuments returns documents before threshold`() = runTest {
        val expiring = createTestDocument(
            id = "d-1",
            vehicleId = "v-1",
            expiryDate = 1000L,
        )
        val future = createTestDocument(
            id = "d-2",
            vehicleId = "v-1",
            expiryDate = 9999999L,
        )
        fakeDao.documents["d-1"] = expiring
        fakeDao.documents["d-2"] = future
        fakeDao.updateFlow()

        val result = repository.getExpiringDocuments("v-1", 5000L).first()
        assertEquals(1, result.size)
        assertEquals("d-1", result[0].id)
    }

    @Test
    fun `deleteDocument delegates to dao and returns success`() = runTest {
        val doc = createTestDocument(id = "d-1")
        fakeDao.documents["d-1"] = doc

        val result = repository.deleteDocument(doc)
        assertTrue(result.isSuccess)
        assertNull(fakeDao.documents["d-1"])
    }

    @Test
    fun `deleteDocumentById delegates to dao and returns success`() = runTest {
        val doc = createTestDocument(id = "d-1")
        fakeDao.documents["d-1"] = doc

        val result = repository.deleteDocumentById("d-1")
        assertTrue(result.isSuccess)
        assertNull(fakeDao.documents["d-1"])
    }

    private fun createTestDocument(
        id: String = "test-id",
        vehicleId: String = "v-1",
        expiryDate: Long = System.currentTimeMillis() + 86400000L * 365,
    ): Document = Document(
        id = id,
        vehicleId = vehicleId,
        type = DocumentType.INSURANCE,
        name = "Test Document",
        expiryDate = expiryDate,
    )
}

/**
 * Fake implementation of [DocumentDao] for unit testing.
 */
private class FakeDocumentDao : DocumentDao {
    val documents = mutableMapOf<String, Document>()
    var shouldThrow = false

    private val flow = MutableStateFlow<List<Document>>(emptyList())

    fun updateFlow() {
        flow.value = documents.values.toList()
    }

    override fun getDocumentsForVehicle(vehicleId: String): Flow<List<Document>> =
        flow.map { list -> list.filter { it.vehicleId == vehicleId }.sortedBy { it.expiryDate } }

    override fun getExpiringDocuments(vehicleId: String, threshold: Long): Flow<List<Document>> =
        flow.map { list ->
            list.filter { it.vehicleId == vehicleId && it.expiryDate <= threshold }
                .sortedBy { it.expiryDate }
        }

    override fun getDocument(documentId: String): Flow<Document?> =
        flow.map { list -> list.find { it.id == documentId } }

    override suspend fun upsertDocument(document: Document) {
        if (shouldThrow) throw RuntimeException("Test error")
        documents[document.id] = document
        updateFlow()
    }

    override suspend fun upsertDocuments(documents: List<Document>) {
        if (shouldThrow) throw RuntimeException("Test error")
        documents.forEach { this.documents[it.id] = it }
        updateFlow()
    }

    override suspend fun deleteDocument(document: Document) {
        if (shouldThrow) throw RuntimeException("Test error")
        documents.remove(document.id)
        updateFlow()
    }

    override suspend fun deleteDocumentById(documentId: String) {
        if (shouldThrow) throw RuntimeException("Test error")
        documents.remove(documentId)
        updateFlow()
    }

    override suspend fun getDocumentsModifiedSince(since: Long): List<Document> =
        documents.values.filter { it.lastModified > since }

    override suspend fun getDocumentById(id: String): Document? = null
}
