package com.guyghost.wakeve.file

/**
 * Android implementation of DocumentPickerFactory.
 * 
 * Note: This is a stub implementation. For full functionality,
 * the document picker should be implemented in the composeApp module
 * where Activity context is available.
 */
actual class DocumentPickerFactory {
    
    actual companion object {
        private val instance = DocumentPickerFactory()
        
        actual fun getInstance(): DocumentPickerFactory = instance
    }
    
    actual fun createPickerService(): DocumentPickerService {
        return StubDocumentPickerService()
    }
}
