package com.guyghost.wakeve.di

import com.guyghost.wakeve.image.ImagePickerService
import com.guyghost.wakeve.image.NoConfiguredImagePickerService

/**
 * JVM implementation of ImagePickerFactory.
 *
 * Provides an explicit not-configured implementation for desktop/server
 * environments without a native image picker.
 */
actual class ImagePickerFactory {
    
    actual companion object {
        private val instance = ImagePickerFactory()
        
        actual fun getInstance(): ImagePickerFactory = instance
    }
    
    actual fun createPickerService(): ImagePickerService {
        return NoConfiguredImagePickerService
    }
}
