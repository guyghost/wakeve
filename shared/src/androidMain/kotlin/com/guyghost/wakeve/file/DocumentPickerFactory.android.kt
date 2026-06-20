package com.guyghost.wakeve.file

/**
 * Android implementation of DocumentPickerFactory.
 *
 * Returns an explicit not-configured service until an Activity-backed picker
 * is wired from the composeApp module.
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
