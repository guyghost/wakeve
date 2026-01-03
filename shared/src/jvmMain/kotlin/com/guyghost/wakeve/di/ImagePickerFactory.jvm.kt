package com.guyghost.wakeve.di

import com.guyghost.wakeve.image.ImagePickerService
import com.guyghost.wakeve.image.StubImagePickerService

/**
 * JVM implementation of ImagePickerFactory.
 * 
 * Provides a stub implementation for desktop/server environments.
 */
actual class ImagePickerFactory {
    
    actual companion object {
        private val instance = ImagePickerFactory()
        
        actual fun getInstance(): ImagePickerFactory = instance
    }
    
    actual fun createPickerService(): ImagePickerService {
        return StubImagePickerService()
    }
}
