package com.guyghost.wakeve.image

import com.guyghost.wakeve.models.ImageBatchResult
import com.guyghost.wakeve.models.ImagePickerConfig
import com.guyghost.wakeve.models.ImagePickerResult
import com.guyghost.wakeve.models.ImageQuality
import com.guyghost.wakeve.models.PickedImage

/**
 * Image picker service for builds where no native picker support has been wired.
 */
object NoConfiguredImagePickerService : ImagePickerService {
    private fun notConfiguredError(): IllegalStateException =
        IllegalStateException("Image picker service is not configured")

    override suspend fun pickImage(): Result<PickedImage> {
        return Result.failure(notConfiguredError())
    }

    override suspend fun pickMultipleImages(limit: Int): Result<List<PickedImage>> {
        return Result.failure(notConfiguredError())
    }

    override suspend fun pickImageWithCompression(quality: ImageQuality): Result<PickedImage> {
        return Result.failure(notConfiguredError())
    }

    override suspend fun pickImagesWithConfig(config: ImagePickerConfig): Result<ImageBatchResult> {
        return Result.failure(notConfiguredError())
    }

    override suspend fun pickVisualMedia(maxItems: Int): Result<List<ImagePickerResult>> {
        return Result.failure(notConfiguredError())
    }

    override fun isPhotoPickerAvailable(): Boolean = false

    override fun getLastPickedImage(): PickedImage? {
        throw notConfiguredError()
    }

    override fun clearCache() {
        throw notConfiguredError()
    }
}

/**
 * @deprecated Use [NoConfiguredImagePickerService] for production fallbacks
 * and deterministic fake implementations in tests.
 */
@Deprecated(
    message = "Use NoConfiguredImagePickerService instead of a stub that can hide missing platform wiring.",
    replaceWith = ReplaceWith("NoConfiguredImagePickerService")
)
class StubImagePickerService : ImagePickerService by NoConfiguredImagePickerService
