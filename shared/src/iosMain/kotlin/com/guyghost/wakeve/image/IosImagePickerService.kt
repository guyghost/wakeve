package com.guyghost.wakeve.image

import com.guyghost.wakeve.models.ImageBatchResult
import com.guyghost.wakeve.models.ImagePickerConfig
import com.guyghost.wakeve.models.ImagePickerResult as ModelsImagePickerResult
import com.guyghost.wakeve.models.ImageQuality
import com.guyghost.wakeve.models.PickedImage

/**
 * iOS stub implementation of ImagePickerService.
 * 
 * This is a placeholder implementation. Full iOS photo picker integration
 * using PHPickerViewController should be implemented in the iosApp module
 * using SwiftUI/UIKit integration.
 */
class IosImagePickerService : ImagePickerService {
    
    private var lastPickedImage: PickedImage? = null
    
    override suspend fun pickImage(): Result<PickedImage> {
        return Result.failure(NotImplementedError("iOS image picker not yet implemented. Use SwiftUI PhotosPicker."))
    }
    
    override suspend fun pickMultipleImages(limit: Int): Result<List<PickedImage>> {
        return Result.failure(NotImplementedError("iOS image picker not yet implemented. Use SwiftUI PhotosPicker."))
    }
    
    override suspend fun pickImageWithCompression(quality: ImageQuality): Result<PickedImage> {
        return Result.failure(NotImplementedError("iOS image picker not yet implemented. Use SwiftUI PhotosPicker."))
    }
    
    override suspend fun pickImagesWithConfig(config: ImagePickerConfig): Result<ImageBatchResult> {
        return Result.failure(NotImplementedError("iOS image picker not yet implemented. Use SwiftUI PhotosPicker."))
    }
    
    override suspend fun pickVisualMedia(maxItems: Int): Result<List<ModelsImagePickerResult>> {
        return Result.failure(NotImplementedError("iOS visual media picker not yet implemented. Use SwiftUI PhotosPicker."))
    }
    
    override fun isPhotoPickerAvailable(): Boolean {
        // iOS 14+ has PHPickerViewController available
        return true
    }
    
    override fun getLastPickedImage(): PickedImage? {
        return lastPickedImage
    }
    
    override fun clearCache() {
        lastPickedImage = null
    }
}
