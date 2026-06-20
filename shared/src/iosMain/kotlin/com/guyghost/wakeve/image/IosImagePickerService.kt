package com.guyghost.wakeve.image

import com.guyghost.wakeve.models.ImageBatchResult
import com.guyghost.wakeve.models.ImagePickerConfig
import com.guyghost.wakeve.models.ImagePickerResult as ModelsImagePickerResult
import com.guyghost.wakeve.models.ImageQuality
import com.guyghost.wakeve.models.PickedImage

/**
 * iOS image picker placeholder.
 *
 * Fails explicitly until PHPickerViewController or SwiftUI PhotosPicker is
 * bridged into this service.
 */
class IosImagePickerService : ImagePickerService {

    override suspend fun pickImage(): Result<PickedImage> {
        return NoConfiguredImagePickerService.pickImage()
    }
    
    override suspend fun pickMultipleImages(limit: Int): Result<List<PickedImage>> {
        return NoConfiguredImagePickerService.pickMultipleImages(limit)
    }
    
    override suspend fun pickImageWithCompression(quality: ImageQuality): Result<PickedImage> {
        return NoConfiguredImagePickerService.pickImageWithCompression(quality)
    }
    
    override suspend fun pickImagesWithConfig(config: ImagePickerConfig): Result<ImageBatchResult> {
        return NoConfiguredImagePickerService.pickImagesWithConfig(config)
    }
    
    override suspend fun pickVisualMedia(maxItems: Int): Result<List<ModelsImagePickerResult>> {
        return NoConfiguredImagePickerService.pickVisualMedia(maxItems)
    }
    
    override fun isPhotoPickerAvailable(): Boolean {
        return NoConfiguredImagePickerService.isPhotoPickerAvailable()
    }
    
    override fun getLastPickedImage(): PickedImage? {
        return NoConfiguredImagePickerService.getLastPickedImage()
    }
    
    override fun clearCache() {
        NoConfiguredImagePickerService.clearCache()
    }
}
