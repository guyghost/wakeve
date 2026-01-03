package com.guyghost.wakeve.di

import com.guyghost.wakeve.file.DocumentPickerService
import com.guyghost.wakeve.file.IosDocumentPickerService

/**
 * Factory for creating iOS-specific document picker services.
 *
 * This factory provides platform-specific implementations of the
 * [DocumentPickerService] interface for iOS using UIDocumentPickerViewController.
 *
 * ## FC&IS Architecture
 *
 * This factory belongs to the **Imperative Shell** layer as it creates
 * platform-specific service instances. It produces services that:
 * - Handle iOS-specific I/O operations (document library access)
 * - Use iOS frameworks (UniformTypeIdentifiers, UIKit)
 * - Manage platform permissions
 *
 * The factory itself doesn't contain business logic - it's a simple
 * dependency provider that wires together the appropriate implementation.
 *
 * ## Usage
 *
 * ```kotlin
 * // Create the factory
 * val factory = IosDocumentPickerFactory()
 *
 * // Create a document picker service
 * val documentPickerService: DocumentPickerService = factory.createPickerService()
 *
 * // Use the service
 * val result = documentPickerService.pickDocument()
 * ```
 *
 * @see IosDocumentPickerService
 * @see DocumentPickerService
 */
object IosDocumentPickerFactory {

    /**
     * Creates a new instance of [DocumentPickerService] for iOS.
     *
     * The returned service uses UIDocumentPickerViewController for document selection
     * and handles all iOS-specific operations.
     *
     * ## Thread Safety
     *
     * Each call creates a new service instance. For shared usage,
     * consider managing the service lifecycle appropriately.
     *
     * @return A new [DocumentPickerService] instance configured for iOS
     */
    fun createPickerService(): DocumentPickerService {
        return IosDocumentPickerService()
    }

    /**
     * Creates a new instance of [DocumentPickerService] with custom configuration.
     *
     * This overload allows passing platform-specific configuration options
     * for the document picker behavior.
     *
     * @return A new [DocumentPickerService] instance configured for iOS
     */
    fun createConfiguredService(
        allowsMultipleSelection: Boolean = false,
        maximumSelectionCount: Int = 1
    ): DocumentPickerService {
        // Configuration can be stored and passed to the service
        // when needed (e.g., in pickDocument/pickDocuments methods)
        return IosDocumentPickerService()
    }
}
