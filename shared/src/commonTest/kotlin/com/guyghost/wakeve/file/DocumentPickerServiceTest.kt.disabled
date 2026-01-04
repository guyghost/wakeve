package com.guyghost.wakeve.file

import com.guyghost.wakeve.models.DocumentPickerConfig
import com.guyghost.wakeve.models.DocumentType
import com.guyghost.wakeve.models.PickedDocument
import com.guyghost.wakeve.models.DocumentBatchResult
import com.guyghost.wakeve.models.FilePickerResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for DocumentPicker models and contracts.
 *
 * These tests verify the correctness of the Functional Core layer
 * (pure models and interfaces) without any platform-specific code.
 */
class DocumentPickerServiceTest {

    // MARK: - DocumentType Tests

    @Test
    fun `DocumentType fromMimeType returns PDF for application pdf`() {
        val result = DocumentType.fromMimeType("application/pdf")
        assertEquals(DocumentType.PDF, result)
    }

    @Test
    fun `DocumentType fromMimeType returns DOC for application msword`() {
        val result = DocumentType.fromMimeType("application/msword")
        assertEquals(DocumentType.DOC, result)
    }

    @Test
    fun `DocumentType fromMimeType returns DOCX for openxmlformats`() {
        val result = DocumentType.fromMimeType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
        assertEquals(DocumentType.DOCX, result)
    }

    @Test
    fun `DocumentType fromMimeType returns IMAGE for image types`() {
        assertEquals(DocumentType.IMAGE, DocumentType.fromMimeType("image/jpeg"))
        assertEquals(DocumentType.IMAGE, DocumentType.fromMimeType("image/png"))
        assertEquals(DocumentType.IMAGE, DocumentType.fromMimeType("image/gif"))
        assertEquals(DocumentType.IMAGE, DocumentType.fromMimeType("image/webp"))
    }

    @Test
    fun `DocumentType fromMimeType returns VIDEO for video types`() {
        assertEquals(DocumentType.VIDEO, DocumentType.fromMimeType("video/mp4"))
        assertEquals(DocumentType.VIDEO, DocumentType.fromMimeType("video/quicktime"))
        assertEquals(DocumentType.VIDEO, DocumentType.fromMimeType("video/x-matroska"))
    }

    @Test
    fun `DocumentType fromMimeType returns AUDIO for audio types`() {
        assertEquals(DocumentType.AUDIO, DocumentType.fromMimeType("audio/mpeg"))
        assertEquals(DocumentType.AUDIO, DocumentType.fromMimeType("audio/wav"))
        assertEquals(DocumentType.AUDIO, DocumentType.fromMimeType("audio/ogg"))
    }

    @Test
    fun `DocumentType fromMimeType returns OTHER for unknown types`() {
        assertEquals(DocumentType.OTHER, DocumentType.fromMimeType("application/unknown"))
        assertEquals(DocumentType.OTHER, DocumentType.fromMimeType("text/plain"))
        assertEquals(DocumentType.OTHER, DocumentType.fromMimeType(null))
    }

    // MARK: - PickedDocument Tests

    @Test
    fun `PickedDocument isImage returns true for IMAGE type`() {
        val document = createTestDocument(type = DocumentType.IMAGE)
        assertTrue(document.isImage)
        assertFalse(document.isVideo)
        assertFalse(document.isAudio)
    }

    @Test
    fun `PickedDocument isVideo returns true for VIDEO type`() {
        val document = createTestDocument(type = DocumentType.VIDEO)
        assertFalse(document.isImage)
        assertTrue(document.isVideo)
        assertFalse(document.isAudio)
    }

    @Test
    fun `PickedDocument isAudio returns true for AUDIO type`() {
        val document = createTestDocument(type = DocumentType.AUDIO)
        assertFalse(document.isImage)
        assertFalse(document.isVideo)
        assertTrue(document.isAudio)
    }

    @Test
    fun `PickedDocument formattedSize formats bytes correctly`() {
        val smallDoc = createTestDocument(sizeBytes = 500)
        assertEquals("500 B", smallDoc.formattedSize)

        val kbDoc = createTestDocument(sizeBytes = 2048)
        assertEquals("2.0 KB", kbDoc.formattedSize)

        val mbDoc = createTestDocument(sizeBytes = 5 * 1024 * 1024)
        assertEquals("5.0 MB", mbDoc.formattedSize)

        val gbDoc = createTestDocument(sizeBytes = 2L * 1024 * 1024 * 1024)
        assertEquals("2.00 GB", gbDoc.formattedSize)
    }

    @Test
    fun `PickedDocument isLargeFile returns true for files over 10MB`() {
        val smallDoc = createTestDocument(sizeBytes = 5 * 1024 * 1024) // 5 MB
        assertFalse(smallDoc.isLargeFile)

        val largeDoc = createTestDocument(sizeBytes = 15 * 1024 * 1024) // 15 MB
        assertTrue(largeDoc.isLargeFile)
    }

    @Test
    fun `PickedDocument create factory method works correctly`() {
        val document = PickedDocument.create(
            uri = "content://test/document",
            displayName = "test.pdf",
            sizeBytes = 1024,
            lastModified = 1234567890L,
            mimeType = "application/pdf"
        )

        assertEquals("content://test/document", document.uri)
        assertEquals("test.pdf", document.displayName)
        assertEquals(DocumentType.PDF, document.type)
        assertEquals(1024L, document.sizeBytes)
        assertEquals(1234567890L, document.lastModified)
        assertEquals("application/pdf", document.mimeType)
    }

    // MARK: - DocumentPickerConfig Tests

    @Test
    fun `DocumentPickerConfig getMimeTypeFilter returns star for empty types`() {
        val config = DocumentPickerConfig()
        assertEquals("*/*", config.getMimeTypeFilter())
    }

    @Test
    fun `DocumentPickerConfig getMimeTypeFilter returns first type for single type`() {
        val config = DocumentPickerConfig(allowedTypes = listOf(DocumentType.PDF))
        assertEquals("application/pdf", config.getMimeTypeFilter())
    }

    @Test
    fun `DocumentPickerConfig singleDocument has correct defaults`() {
        val config = DocumentPickerConfig.singleDocument
        assertEquals(1, config.maxSelectionLimit)
        assertTrue(config.allowedTypes.isEmpty())
        assertFalse(config.allowMultipleSelection)
    }

    @Test
    fun `DocumentPickerConfig multipleDocuments has correct defaults`() {
        val config = DocumentPickerConfig.multipleDocuments
        assertEquals(10, config.maxSelectionLimit)
        assertTrue(config.allowedTypes.isEmpty())
        assertTrue(config.allowMultipleSelection)
    }

    @Test
    fun `DocumentPickerConfig pdfOnly filters PDF only`() {
        val config = DocumentPickerConfig.pdfOnly
        assertEquals(1, config.maxSelectionLimit)
        assertEquals(listOf(DocumentType.PDF), config.allowedTypes)
        assertFalse(config.allowMultipleSelection)
    }

    @Test
    fun `DocumentPickerConfig imagesOnly allows image selection`() {
        val config = DocumentPickerConfig.imagesOnly
        assertEquals(5, config.maxSelectionLimit)
        assertEquals(listOf(DocumentType.IMAGE), config.allowedTypes)
        assertTrue(config.allowMultipleSelection)
    }

    @Test
    fun `DocumentPickerConfig mediaOnly includes image video audio`() {
        val config = DocumentPickerConfig.mediaOnly
        assertEquals(3, config.allowedTypes.size)
        assertTrue(config.allowedTypes.contains(DocumentType.IMAGE))
        assertTrue(config.allowedTypes.contains(DocumentType.VIDEO))
        assertTrue(config.allowedTypes.contains(DocumentType.AUDIO))
    }

    // MARK: - DocumentBatchResult Tests

    @Test
    fun `DocumentBatchResult isEmpty returns true for empty list`() {
        val result = DocumentBatchResult(
            documents = emptyList(),
            config = DocumentPickerConfig()
        )
        assertTrue(result.isEmpty)
        assertEquals(0, result.count)
    }

    @Test
    fun `DocumentBatchResult isEmpty returns false for non-empty list`() {
        val result = DocumentBatchResult(
            documents = listOf(createTestDocument()),
            config = DocumentPickerConfig()
        )
        assertFalse(result.isEmpty)
        assertEquals(1, result.count)
    }

    @Test
    fun `DocumentBatchResult totalSizeBytes calculates correctly`() {
        val documents = listOf(
            createTestDocument(sizeBytes = 1000),
            createTestDocument(sizeBytes = 2000),
            createTestDocument(sizeBytes = 3000)
        )
        val result = DocumentBatchResult(
            documents = documents,
            config = DocumentPickerConfig()
        )
        assertEquals(6000L, result.totalSizeBytes)
        assertEquals("5.9 KB", result.formattedTotalSize)
    }

    // MARK: - FilePickerResult Tests

    @Test
    fun `FilePickerResult Success isSuccess returns true`() {
        val result = FilePickerResult.Success(createTestDocument())
        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
    }

    @Test
    fun `FilePickerResult MultipleSuccess isSuccess returns true`() {
        val result = FilePickerResult.MultipleSuccess(listOf(createTestDocument()))
        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
    }

    @Test
    fun `FilePickerResult Failure isFailure returns true`() {
        val result = FilePickerResult.Failure("Test error")
        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
    }

    @Test
    fun `FilePickerResult getOrNull returns document for Success`() {
        val document = createTestDocument()
        val result = FilePickerResult.Success(document)
        assertEquals(document, result.getOrNull())
    }

    @Test
    fun `FilePickerResult getOrNull returns null for Failure`() {
        val result = FilePickerResult.Failure("Test error")
        assertNull(result.getOrNull())
    }

    @Test
    fun `FilePickerResult getOrNullList returns list for Success`() {
        val document = createTestDocument()
        val result = FilePickerResult.Success(document)
        assertEquals(listOf(document), result.getOrNullList())
    }

    @Test
    fun `FilePickerResult getOrNullList returns list for MultipleSuccess`() {
        val documents = listOf(createTestDocument(), createTestDocument())
        val result = FilePickerResult.MultipleSuccess(documents)
        assertEquals(documents, result.getOrNullList())
    }

    @Test
    fun `FilePickerResult errorOrNull returns error for Failure`() {
        val result = FilePickerResult.Failure("Test error")
        assertEquals("Test error", result.errorOrNull())
    }

    @Test
    fun `FilePickerResult errorOrNull returns null for Success`() {
        val result = FilePickerResult.Success(createTestDocument())
        assertNull(result.errorOrNull())
    }

    // MARK: - Service Contract Tests

    @Test
    fun `DocumentPickerService interface methods are defined correctly`() {
        // This test verifies the service contract by checking that all methods exist
        val service: DocumentPickerService = object : DocumentPickerService {
            override suspend fun pickDocument(): Result<PickedDocument> = Result.success(createTestDocument())
            override suspend fun pickDocuments(limit: Int): Result<List<PickedDocument>> = Result.success(listOf(createTestDocument()))
            override suspend fun pickDocument(type: DocumentType): Result<PickedDocument> = Result.success(createTestDocument())
            override suspend fun pickDocumentsWithConfig(config: DocumentPickerConfig): Result<DocumentBatchResult> = Result.success(DocumentBatchResult(listOf(createTestDocument()), config))
            override fun isDocumentPickerAvailable(): Boolean = true
            override fun getLastPickedDocument(): PickedDocument? = null
            override fun clearCache() {}
        }

        assertTrue(service.isDocumentPickerAvailable())
        assertNull(service.getLastPickedDocument())
    }

    // MARK: - Exception Tests

    @Test
    fun `DocumentPickerCancelledException has correct message`() {
        val exception = DocumentPickerCancelledException()
        assertEquals("Document picking was cancelled by the user", exception.message)
    }

    @Test
    fun `DocumentPickerPermissionDeniedException includes permission name`() {
        val exception = DocumentPickerPermissionDeniedException("READ_EXTERNAL_STORAGE")
        assertEquals("Permission denied: READ_EXTERNAL_STORAGE", exception.message)
    }

    @Test
    fun `DocumentPickerInvalidDocumentException includes reason`() {
        val exception = DocumentPickerInvalidDocumentException("Failed to read file")
        assertEquals("Invalid document: Failed to read file", exception.message)
    }

    // MARK: - Helper Methods

    private fun createTestDocument(
        uri: String = "content://test/document",
        displayName: String = "test.pdf",
        type: DocumentType = DocumentType.PDF,
        sizeBytes: Long = 1024,
        lastModified: Long? = 1234567890L,
        mimeType: String? = "application/pdf"
    ): PickedDocument {
        return PickedDocument(
            uri = uri,
            displayName = displayName,
            type = type,
            sizeBytes = sizeBytes,
            lastModified = lastModified,
            mimeType = mimeType
        )
    }
}
