package com.guyghost.wakeve.file

import com.guyghost.wakeve.models.DocumentPickerConfig
import com.guyghost.wakeve.models.DocumentType
import com.guyghost.wakeve.models.DocumentBatchResult
import com.guyghost.wakeve.models.PickedDocument

/**
 * Factory interface for creating platform-specific document picker instances.
 *
 * Each platform (Android, iOS) provides its own implementation through
 * the expect/actual mechanism in Kotlin Multiplatform.
 *
 * ## Usage
 *
 * ```kotlin
 * // Get the factory for the current platform
 * val factory = DocumentPickerFactory.getInstance()
 *
 * // Create the document picker service
 * val documentPicker = factory.createPickerService()
 *
 * // Use the document picker
 * lifecycleScope.launch {
 *     val result = documentPicker.pickDocument()
 *     // Handle result
 * }
 * ```
 */
expect class DocumentPickerFactory {

    /**
     * Get the singleton factory instance for the current platform.
     *
     * @return The platform-specific factory instance
     */
    companion object {
        fun getInstance(): DocumentPickerFactory
    }

    /**
     * Create a document picker service for the current platform.
     *
     * The returned service is ready to use for picking documents from storage.
     *
     * @return A configured DocumentPickerService implementation
     */
    fun createPickerService(): DocumentPickerService
}

/**
 * Convenience function to get a document picker service.
 *
 * This function provides a simple way to obtain a document picker service
 * without directly using the factory.
 *
 * @return A DocumentPickerService instance for the current platform
 */
fun getDocumentPickerService(): DocumentPickerService {
    return DocumentPickerFactory.getInstance().createPickerService()
}
