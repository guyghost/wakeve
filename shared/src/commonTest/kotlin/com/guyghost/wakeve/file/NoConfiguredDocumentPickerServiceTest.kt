package com.guyghost.wakeve.file

import com.guyghost.wakeve.models.DocumentPickerConfig
import com.guyghost.wakeve.models.DocumentType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class NoConfiguredDocumentPickerServiceTest {

    @Test
    fun `pickDocument fails when document picker is not configured`() = runTest {
        val result = NoConfiguredDocumentPickerService.pickDocument()

        val error = assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals("Document picker service is not configured", error.message)
    }

    @Test
    fun `pickDocument with type fails when document picker is not configured`() = runTest {
        val result = NoConfiguredDocumentPickerService.pickDocument(DocumentType.PDF)

        val error = assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals("Document picker service is not configured", error.message)
    }

    @Test
    fun `pickDocumentsWithConfig fails when document picker is not configured`() = runTest {
        val result = NoConfiguredDocumentPickerService.pickDocumentsWithConfig(DocumentPickerConfig.pdfOnly)

        val error = assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals("Document picker service is not configured", error.message)
    }

    @Test
    fun `availability is false when document picker is not configured`() {
        assertFalse(NoConfiguredDocumentPickerService.isDocumentPickerAvailable())
    }

    @Test
    fun `cache methods fail when document picker is not configured`() {
        val getLastError = assertFailsWith<IllegalStateException> {
            NoConfiguredDocumentPickerService.getLastPickedDocument()
        }
        val clearError = assertFailsWith<IllegalStateException> {
            NoConfiguredDocumentPickerService.clearCache()
        }

        assertEquals("Document picker service is not configured", getLastError.message)
        assertEquals("Document picker service is not configured", clearError.message)
    }
}
