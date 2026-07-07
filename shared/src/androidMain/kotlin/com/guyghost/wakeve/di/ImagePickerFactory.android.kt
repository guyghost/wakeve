package com.guyghost.wakeve.di

import com.guyghost.wakeve.image.ImagePickerService
import com.guyghost.wakeve.image.NoConfiguredImagePickerService

/**
 * Android implementation of ImagePickerFactory.
 *
 * Returns an explicit not-configured service until an Activity-backed picker
 * is wired from the composeApp module.
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
