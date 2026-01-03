package com.guyghost.wakeve.di

import com.guyghost.wakeve.image.IosImagePickerService
import com.guyghost.wakeve.image.ImagePickerService

/**
 * Factory for creating iOS-specific image picker services.
 *
 * This factory provides platform-specific implementations of the
 * [ImagePickerService] interface for iOS using PHPickerViewController.
 *
 * ## FC&IS Architecture
 *
 * This factory belongs to the **Imperative Shell** layer as it creates
 * platform-specific service instances. It produces services that:
 * - Handle iOS-specific I/O operations (photo library access)
 * - Use iOS frameworks (Photos, PhotosUI)
 * - Manage platform permissions
 *
 * The factory itself doesn't contain business logic - it's a simple
 * dependency provider that wires together the appropriate implementation.
 *
 * ## Usage
 *
 * ```kotlin
 * // Create the factory
 * val factory = IosImagePickerFactory()
 *
 * // Create an image picker service
 * val imagePickerService: ImagePickerService = factory.createPickerService()
 *
 * // Use the service
 * val result = imagePickerService.pickImage()
 * ```
 *
 * @see IosImagePickerService
 * @see ImagePickerService
 */
object IosImagePickerFactory {

    /**
     * Creates a new instance of [ImagePickerService] for iOS.
     *
     * The returned service uses PHPickerViewController for image selection
     * and handles all iOS-specific operations.
     *
     * ## Thread Safety
     *
     * Each call creates a new service instance. For shared usage,
     * consider managing the service lifecycle appropriately.
     *
     * @return A new [ImagePickerService] instance configured for iOS
     */
    fun createPickerService(): ImagePickerService {
        return IosImagePickerService()
    }

    /**
     * Creates a new instance of [ImagePickerService] with custom configuration.
     *
     * This overload allows passing platform-specific configuration options
     * for the image picker behavior.
     *
     * @return A new [ImagePickerService] instance configured for iOS
     */
    fun createConfiguredService(
        allowsMultipleSelection: Boolean = false,
        maximumSelectionCount: Int = 1
    ): ImagePickerService {
        // Configuration can be stored and passed to the service
        // when needed (e.g., in pickImage/pickMultipleImages methods)
        return IosImagePickerService()
    }
}
