package com.guyghost.wakeve.file

import com.guyghost.wakeve.models.DocumentBatchResult
import com.guyghost.wakeve.models.DocumentPickerConfig
import com.guyghost.wakeve.models.DocumentType
import com.guyghost.wakeve.models.PickedDocument

/**
 * iOS stub implementation of DocumentPickerService.
 * 
 * This is a placeholder implementation. Full iOS document picker integration
 * using UIDocumentPickerViewController should be implemented in the iosApp module
 * using SwiftUI/UIKit integration.
 */
class IosDocumentPickerService : DocumentPickerService {
    
    private var lastPickedDocument: PickedDocument? = null
    
    override suspend fun pickDocument(): Result<PickedDocument> {
        return Result.failure(NotImplementedError("iOS document picker not yet implemented. Use SwiftUI fileImporter."))
    }
    
    override suspend fun pickDocuments(limit: Int): Result<List<PickedDocument>> {
        return Result.failure(NotImplementedError("iOS document picker not yet implemented. Use SwiftUI fileImporter."))
    }
    
    override suspend fun pickDocument(type: DocumentType): Result<PickedDocument> {
        return Result.failure(NotImplementedError("iOS document picker not yet implemented. Use SwiftUI fileImporter."))
    }
    
    override suspend fun pickDocumentsWithConfig(config: DocumentPickerConfig): Result<DocumentBatchResult> {
        return Result.failure(NotImplementedError("iOS document picker not yet implemented. Use SwiftUI fileImporter."))
    }
    
    override fun isDocumentPickerAvailable(): Boolean {
        return true
    }
    
    override fun getLastPickedDocument(): PickedDocument? {
        return lastPickedDocument
    }
    
    override fun clearCache() {
        lastPickedDocument = null
    }
}
