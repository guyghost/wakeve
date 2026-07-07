package com.guyghost.wakeve.file

import com.guyghost.wakeve.models.DocumentBatchResult
import com.guyghost.wakeve.models.DocumentPickerConfig
import com.guyghost.wakeve.models.DocumentType
import com.guyghost.wakeve.models.PickedDocument

/**
 * iOS document picker placeholder.
 *
 * Fails explicitly until UIDocumentPickerViewController or SwiftUI fileImporter
 * is bridged into this service.
 */
class IosDocumentPickerService : DocumentPickerService {

    override suspend fun pickDocument(): Result<PickedDocument> {
        return NoConfiguredDocumentPickerService.pickDocument()
    }
    
    override suspend fun pickDocuments(limit: Int): Result<List<PickedDocument>> {
        return NoConfiguredDocumentPickerService.pickDocuments(limit)
    }
    
    override suspend fun pickDocument(type: DocumentType): Result<PickedDocument> {
        return NoConfiguredDocumentPickerService.pickDocument(type)
    }
    
    override suspend fun pickDocumentsWithConfig(config: DocumentPickerConfig): Result<DocumentBatchResult> {
        return NoConfiguredDocumentPickerService.pickDocumentsWithConfig(config)
    }
    
    override fun isDocumentPickerAvailable(): Boolean {
        return NoConfiguredDocumentPickerService.isDocumentPickerAvailable()
    }
    
    override fun getLastPickedDocument(): PickedDocument? {
        return NoConfiguredDocumentPickerService.getLastPickedDocument()
    }
    
    override fun clearCache() {
        NoConfiguredDocumentPickerService.clearCache()
    }
}
