package com.guyghost.wakeve.di

import com.guyghost.wakeve.image.ImagePickerService

/**
 * Factory interface for creating platform-specific image picker instances.
 * 
 * Each platform (Android, iOS) provides its own implementation through
 * the expect/actual mechanism in Kotlin Multiplatform.
 * 
 * ## Usage
 * 
 * ```kotlin
 * // Get the factory for the current platform
 * val factory = ImagePickerFactory.getInstance()
 * 
 // Create the image picker service
 * val imagePicker = factory.createPickerService()
 * 
 * // Use the image picker
 * lifecycleScope.launch {
 *     val result = imagePicker.pickImage()
 *     // Handle result
 * }
 * ```
 */
expect class ImagePickerFactory {
    
    /**
     * Get the singleton factory instance for the current platform.
     * 
     * @return The platform-specific factory instance
     */
    companion object {
        fun getInstance(): ImagePickerFactory
    }
    
    /**
     * Create an image picker service for the current platform.
     * 
     * The returned service is ready to use for picking images from the gallery.
     * 
     * @return A configured ImagePickerService implementation
     */
    fun createPickerService(): ImagePickerService
}

/**
 * Convenience function to get an image picker service.
 * 
 * This function provides a simple way to obtain an image picker service
 * without directly using the factory.
 * 
 * @return An ImagePickerService instance for the current platform
 */
fun getImagePickerService(): ImagePickerService {
    return ImagePickerFactory.getInstance().createPickerService()
}
