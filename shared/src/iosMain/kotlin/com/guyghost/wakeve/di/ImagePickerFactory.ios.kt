package com.guyghost.wakeve.di

import com.guyghost.wakeve.image.ImagePickerService
import com.guyghost.wakeve.image.IosImagePickerService

/**
 * iOS actual implementation of [ImagePickerFactory].
 *
 * Provides the iOS-specific implementation using IosImagePickerService
 * which leverages PHPickerViewController for image selection.
 *
 * @see IosImagePickerService
 * @see ImagePickerService
 */
actual class ImagePickerFactory private constructor() {

    actual companion object {
        private val instance = ImagePickerFactory()

        /**
         * Get the singleton factory instance for iOS.
         *
         * @return The iOS-specific factory instance
         */
        actual fun getInstance(): ImagePickerFactory = instance
    }

    /**
     * Create an image picker service for iOS.
     *
     * The returned service uses PHPickerViewController for image selection
     * and handles all iOS-specific operations.
     *
     * @return A configured ImagePickerService implementation for iOS
     */
    actual fun createPickerService(): ImagePickerService {
        return IosImagePickerService()
    }
}
