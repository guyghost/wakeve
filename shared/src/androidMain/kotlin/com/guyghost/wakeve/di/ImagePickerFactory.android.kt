package com.guyghost.wakeve.di

import com.guyghost.wakeve.image.ImagePickerService
import com.guyghost.wakeve.image.StubImagePickerService

/**
 * Android implementation of ImagePickerFactory.
 * 
 * Note: This is a stub implementation. For full functionality,
 * the image picker should be implemented in the composeApp module
 * where Activity context is available.
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
