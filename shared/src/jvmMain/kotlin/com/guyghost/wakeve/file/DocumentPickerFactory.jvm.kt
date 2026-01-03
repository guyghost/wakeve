package com.guyghost.wakeve.file

/**
 * JVM implementation of DocumentPickerFactory.
 * 
 * Provides a stub implementation for desktop/server environments.
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
