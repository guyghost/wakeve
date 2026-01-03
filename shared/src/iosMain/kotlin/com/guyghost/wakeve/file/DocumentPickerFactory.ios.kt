package com.guyghost.wakeve.file

/**
 * iOS actual implementation of [DocumentPickerFactory].
 *
 * Provides the iOS-specific implementation using IosDocumentPickerService
 * which leverages UIDocumentPickerViewController for document selection.
 *
 * @see IosDocumentPickerService
 * @see DocumentPickerService
 */
actual class DocumentPickerFactory private constructor() {

    actual companion object {
        private val instance = DocumentPickerFactory()

        /**
         * Get the singleton factory instance for iOS.
         *
         * @return The iOS-specific factory instance
         */
        actual fun getInstance(): DocumentPickerFactory = instance
    }

    /**
     * Create a document picker service for iOS.
     *
     * The returned service uses UIDocumentPickerViewController for document selection
     * and handles all iOS-specific operations.
     *
     * @return A configured DocumentPickerService implementation for iOS
     */
    actual fun createPickerService(): DocumentPickerService {
        return IosDocumentPickerService()
    }
}
