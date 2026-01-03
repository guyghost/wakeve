package com.guyghost.wakeve.image

import com.guyghost.wakeve.models.ImageBatchResult
import com.guyghost.wakeve.models.ImagePickerConfig
import com.guyghost.wakeve.models.ImagePickerResult
import com.guyghost.wakeve.models.ImageQuality
import com.guyghost.wakeve.models.PickedImage

/**
 * Stub implementation of ImagePickerService for platforms without native picker support.
 * 
 * Returns failure results for all operations. This is used as a fallback
 * when the platform-specific implementation is not available.
 */
class StubImagePickerService : ImagePickerService {
    
    override suspend fun pickImage(): Result<PickedImage> {
        return Result.failure(UnsupportedOperationException("Image picker not available on this platform"))
    }
    
    override suspend fun pickMultipleImages(limit: Int): Result<List<PickedImage>> {
        return Result.failure(UnsupportedOperationException("Image picker not available on this platform"))
    }
    
    override suspend fun pickImageWithCompression(quality: ImageQuality): Result<PickedImage> {
        return Result.failure(UnsupportedOperationException("Image picker not available on this platform"))
    }
    
    override suspend fun pickImagesWithConfig(config: ImagePickerConfig): Result<ImageBatchResult> {
        return Result.failure(UnsupportedOperationException("Image picker not available on this platform"))
    }
    
    override suspend fun pickVisualMedia(maxItems: Int): Result<List<ImagePickerResult>> {
        return Result.failure(UnsupportedOperationException("Image picker not available on this platform"))
    }
    
    override fun isPhotoPickerAvailable(): Boolean = false
    
    override fun getLastPickedImage(): PickedImage? = null
    
    override fun clearCache() {
        // No-op
    }
}
