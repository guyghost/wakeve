package com.guyghost.wakeve.file

/**
 * JVM implementation of DocumentPickerFactory.
 *
 * Provides an explicit not-configured implementation for desktop/server
 * environments without a native document picker.
 */
actual class DocumentPickerFactory {
    
    actual companion object {
        private val instance = DocumentPickerFactory()
        
        actual fun getInstance(): DocumentPickerFactory = instance
    }
    
    actual fun createPickerService(): DocumentPickerService {
        return NoConfiguredDocumentPickerService
    }
}
