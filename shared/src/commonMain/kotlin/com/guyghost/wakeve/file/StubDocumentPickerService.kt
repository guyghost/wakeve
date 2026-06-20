package com.guyghost.wakeve.file

import com.guyghost.wakeve.models.DocumentBatchResult
import com.guyghost.wakeve.models.DocumentPickerConfig
import com.guyghost.wakeve.models.DocumentType
import com.guyghost.wakeve.models.PickedDocument

/**
 * Document picker service for builds where no native picker support has been wired.
 */
object NoConfiguredDocumentPickerService : DocumentPickerService {
    private fun notConfiguredError(): IllegalStateException =
        IllegalStateException("Document picker service is not configured")

    override suspend fun pickDocument(): Result<PickedDocument> {
        return Result.failure(notConfiguredError())
    }

    override suspend fun pickDocuments(limit: Int): Result<List<PickedDocument>> {
        return Result.failure(notConfiguredError())
    }

    override suspend fun pickDocument(type: DocumentType): Result<PickedDocument> {
        return Result.failure(notConfiguredError())
    }

    override suspend fun pickDocumentsWithConfig(config: DocumentPickerConfig): Result<DocumentBatchResult> {
        return Result.failure(notConfiguredError())
    }

    override fun isDocumentPickerAvailable(): Boolean = false

    override fun getLastPickedDocument(): PickedDocument? {
        throw notConfiguredError()
    }

    override fun clearCache() {
        throw notConfiguredError()
    }
}

/**
 * @deprecated Use [NoConfiguredDocumentPickerService] for production fallbacks
 * and deterministic fake implementations in tests.
 */
@Deprecated(
    message = "Use NoConfiguredDocumentPickerService instead of a stub that can hide missing platform wiring.",
    replaceWith = ReplaceWith("NoConfiguredDocumentPickerService")
)
class StubDocumentPickerService : DocumentPickerService by NoConfiguredDocumentPickerService
