package com.roadmate.phone.ui.documents

import app.cash.turbine.test
import com.roadmate.core.database.dao.DocumentDao
import com.roadmate.core.database.entity.Document
import com.roadmate.core.database.entity.DocumentType
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("DocumentDetailViewModel")
class DocumentDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeDocumentDao: DetailFakeDocumentDao
    private lateinit var documentRepository: DocumentRepository
    private lateinit var viewModel: DocumentDetailViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDocumentDao = DetailFakeDocumentDao()
        documentRepository = DocumentRepository(fakeDocumentDao)
        viewModel = DocumentDetailViewModel(documentRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("loadDocument")
    inner class LoadDocument {

        @Test
        fun `loads document by id`() = runTest {
            val doc = testDocument()
            fakeDocumentDao.documents["doc-1"] = doc
            fakeDocumentDao.updateFlow()

            viewModel.loadDocument("doc-1")

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                assertEquals("doc-1", (state as UiState.Success).data.document?.id)
                assertEquals("Insurance", state.data.document?.name)
            }
        }

        @Test
        fun `shows error when document not found`() = runTest {
            fakeDocumentDao.updateFlow()

            viewModel.loadDocument("nonexistent")

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Error)
            }
        }
    }

    @Nested
    @DisplayName("edit sheet")
    inner class EditSheet {

        @Test
        fun `onEditClick populates form from document`() = runTest {
            val doc = testDocument()
            fakeDocumentDao.documents["doc-1"] = doc
            fakeDocumentDao.updateFlow()
            viewModel.loadDocument("doc-1")

            viewModel.uiState.test {
                awaitItem()
            }

            viewModel.onEditClick()

            assertEquals(doc.type, viewModel.formType.value)
            assertEquals(doc.name, viewModel.formName.value)
            assertEquals(doc.reminderDaysBefore.toString(), viewModel.formReminderDays.value)
        }

        @Test
        fun `isSaveEnabled true when required fields filled`() = runTest {
            viewModel.onNameChange("Insurance")
            viewModel.onReminderDaysChange("30")

            assertTrue(viewModel.isSaveEnabled.first())
        }

        @Test
        fun `isSaveEnabled false when name blank`() = runTest {
            viewModel.onNameChange("")
            viewModel.onReminderDaysChange("30")

            assertEquals(false, viewModel.isSaveEnabled.first())
        }
    }

    private fun testDocument(
        id: String = "doc-1",
        vehicleId: String = "veh-1",
        name: String = "Insurance",
        type: DocumentType = DocumentType.INSURANCE,
    ) = Document(
        id = id,
        vehicleId = vehicleId,
        type = type,
        name = name,
        expiryDate = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
        reminderDaysBefore = 30,
        notes = "Test notes",
    )
}

private class DetailFakeDocumentDao : DocumentDao {
    val documents = mutableMapOf<String, Document>()
    private val flow = MutableStateFlow<List<Document>>(emptyList())

    fun updateFlow() { flow.value = documents.values.toList() }

    override fun getDocumentsForVehicle(vehicleId: String): Flow<List<Document>> =
        flow.map { list -> list.filter { it.vehicleId == vehicleId } }

    override fun getExpiringDocuments(vehicleId: String, threshold: Long): Flow<List<Document>> =
        flow.map { list ->
            list.filter { it.vehicleId == vehicleId && it.expiryDate <= threshold }
        }

    override fun getDocument(documentId: String): Flow<Document?> =
        flow.map { list -> list.find { it.id == documentId } }

    override suspend fun upsertDocument(document: Document) {
        documents[document.id] = document
        updateFlow()
    }

    override suspend fun upsertDocuments(documents: List<Document>) {
        documents.forEach { this.documents[it.id] = it }
        updateFlow()
    }

    override suspend fun deleteDocument(document: Document) {
        documents.remove(document.id)
        updateFlow()
    }

    override suspend fun deleteDocumentById(documentId: String) {
        documents.remove(documentId)
        updateFlow()
    }

    override suspend fun getDocumentsModifiedSince(since: Long): List<Document> =
        documents.values.filter { it.lastModified > since }

    override suspend fun getDocumentById(id: String): Document? = documents[id]
}
