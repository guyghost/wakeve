package com.guyghost.wakeve.file

import com.guyghost.wakeve.models.DocumentBatchResult
import com.guyghost.wakeve.models.DocumentPickerConfig
import com.guyghost.wakeve.models.DocumentType
import com.guyghost.wakeve.models.PickedDocument

/**
 * Stub implementation of DocumentPickerService for platforms without native picker support.
 * 
 * Returns failure results for all operations. This is used as a fallback
 * when the platform-specific implementation is not available.
 */
class StubDocumentPickerService : DocumentPickerService {
    
    override suspend fun pickDocument(): Result<PickedDocument> {
        return Result.failure(UnsupportedOperationException("Document picker not available on this platform"))
    }
    
    override suspend fun pickDocuments(limit: Int): Result<List<PickedDocument>> {
        return Result.failure(UnsupportedOperationException("Document picker not available on this platform"))
    }
    
    override suspend fun pickDocument(type: DocumentType): Result<PickedDocument> {
        return Result.failure(UnsupportedOperationException("Document picker not available on this platform"))
    }
    
    override suspend fun pickDocumentsWithConfig(config: DocumentPickerConfig): Result<DocumentBatchResult> {
        return Result.failure(UnsupportedOperationException("Document picker not available on this platform"))
    }
    
    override fun isDocumentPickerAvailable(): Boolean = false
    
    override fun getLastPickedDocument(): PickedDocument? = null
    
    override fun clearCache() {
        // No-op
    }
}
