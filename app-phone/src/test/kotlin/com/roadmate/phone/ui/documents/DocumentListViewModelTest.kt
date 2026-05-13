package com.roadmate.phone.ui.documents

import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import com.roadmate.core.database.dao.DocumentDao
import com.roadmate.core.database.entity.Document
import com.roadmate.core.database.entity.DocumentType
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.ActiveVehicleRepository
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
@DisplayName("DocumentListViewModel")
class DocumentListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeDocumentDao: DocListFakeDocumentDao
    private lateinit var fakeDataStore: DocListFakePreferencesDataStore
    private lateinit var documentRepository: DocumentRepository
    private lateinit var activeVehicleRepository: ActiveVehicleRepository
    private lateinit var viewModel: DocumentListViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDocumentDao = DocListFakeDocumentDao()
        fakeDataStore = DocListFakePreferencesDataStore()
        documentRepository = DocumentRepository(fakeDocumentDao)
        activeVehicleRepository = ActiveVehicleRepository(fakeDataStore)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("loadData")
    inner class LoadData {

        @Test
        fun `loads documents sorted by expiry soontest`() = runTest {
            val now = System.currentTimeMillis()
            val doc1 = testDocument(id = "d1", name = "Insurance", expiryDate = now + 30L * 24 * 60 * 60 * 1000)
            val doc2 = testDocument(id = "d2", name = "License", expiryDate = now + 7L * 24 * 60 * 60 * 1000)

            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeDocumentDao.documents["d1"] = doc1
            fakeDocumentDao.documents["d2"] = doc2
            fakeDocumentDao.updateFlow()

            viewModel = DocumentListViewModel(activeVehicleRepository, documentRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val docs = (state as UiState.Success).data.documents
                assertEquals(2, docs.size)
                assertEquals("d2", docs[0].id)
                assertEquals("d1", docs[1].id)
            }
        }

        @Test
        fun `shows error when no active vehicle`() = runTest {
            fakeDocumentDao.updateFlow()
            viewModel = DocumentListViewModel(activeVehicleRepository, documentRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Error)
            }
        }

        @Test
        fun `isSaveEnabled true when all required fields filled`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeDocumentDao.updateFlow()
            viewModel = DocumentListViewModel(activeVehicleRepository, documentRepository)

            viewModel.onNameChange("Insurance")
            viewModel.onReminderDaysChange("30")

            assertTrue(viewModel.isSaveEnabled.first())
        }

        @Test
        fun `isSaveEnabled false when name blank`() = runTest {
            activeVehicleRepository.setActiveVehicle("veh-1")
            fakeDocumentDao.updateFlow()
            viewModel = DocumentListViewModel(activeVehicleRepository, documentRepository)

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
        expiryDate: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
    ) = Document(
        id = id,
        vehicleId = vehicleId,
        type = type,
        name = name,
        expiryDate = expiryDate,
        reminderDaysBefore = 30,
    )
}

private class DocListFakePreferencesDataStore : androidx.datastore.core.DataStore<Preferences> {
    private val _data = MutableStateFlow<Preferences>(androidx.datastore.preferences.core.emptyPreferences())
    override val data: Flow<Preferences> = _data

    override suspend fun updateData(
        transformer: suspend (Preferences) -> Preferences,
    ): Preferences {
        val newValue = transformer(_data.value)
        _data.value = newValue
        return newValue
    }
}

private class DocListFakeDocumentDao : DocumentDao {
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
