package com.guyghost.wakeve.image

import com.guyghost.wakeve.models.ImageBatchResult
import com.guyghost.wakeve.models.ImagePickerConfig
import com.guyghost.wakeve.models.ImagePickerResult as ModelsImagePickerResult
import com.guyghost.wakeve.models.ImageQuality
import com.guyghost.wakeve.models.PickedImage

/**
 * Service for picking images and media from device gallery.
 * 
 * This interface defines the contract for image picking functionality
 * across all platforms. Platform-specific implementations handle
 * actual gallery access, permissions, and media handling.
 * 
 * ## Architecture
 * 
 * This interface is part of the **Functional Core** - it defines the contract
 * without any I/O or side effects. The actual platform-specific implementations
 * (AndroidImagePickerService, etc.) are in the **Imperative Shell** and handle:
 * - Gallery access via platform APIs
 * - Runtime permissions
 * - Content URI resolution
 * - Image compression
 * 
 * ## Usage
 * 
 * ```kotlin
 * // Get the service from DI/factory
 * val imagePicker = getImagePickerService()
 * 
 * // Pick a single image
 * val result = imagePicker.pickImage()
 * result.fold(
 *     onSuccess = { image ->
 *         // Use the picked image
 *         uploadImage(image.uri)
 *     },
 *     onFailure = { error ->
 *         // Handle error (permission denied, cancellation, etc.)
 *         showError("Failed to pick image: ${error.message}")
 *     }
 * )
 * 
 * // Pick multiple images with limit
 * val batchResult = imagePicker.pickMultipleImages(limit = 3)
 * batchResult.fold(
 *     onSuccess = { batch ->
 *         batch.images.forEach { image ->
 *             uploadImage(image.uri)
 *         }
 *     },
 *     onFailure = { error ->
 *         showError("Failed to pick images: ${error.message}")
 *     }
 * )
 * 
 * // Pick image with compression
 * val compressedResult = imagePicker.pickImageWithCompression(ImageQuality.HIGH)
 * compressedResult.fold(
 *     onSuccess = { image ->
 *         uploadCompressedImage(image)
 *     },
 *     onFailure = { error ->
 *         showError("Failed to pick compressed image: ${error.message}")
 *     }
 * )
 * ```
 * 
 * ## Error Handling
 * 
 * All methods return a [Result] type to handle:
 * - Permission denial ([SecurityException])
 * - User cancellation
 * - Invalid URI or corrupted file
 * - I/O errors during compression
 * - Storage permission issues
 * 
 * ## Thread Safety
 * 
 * Implementations must be thread-safe as image picking operations
 * may be called from multiple coroutines simultaneously.
 */
interface ImagePickerService {
    
    /**
     * Pick a single image from the gallery.
     * 
     * Launches the platform's image picker and waits for the user
     * to select an image. Returns the selected image metadata.
     * 
     * @return [Result] containing the picked image on success,
     *         or an exception on failure (cancellation, permission denied, etc.)
     */
    suspend fun pickImage(): Result<PickedImage>
    
    /**
     * Pick multiple images from the gallery.
     * 
     * Launches the platform's image picker in multi-select mode
     * and waits for the user to select multiple images.
     * 
     * @param limit Maximum number of images that can be selected.
     *              If the picker returns more, they'll be truncated to this limit.
     * @return [Result] containing the list of picked images on success,
     *         or an exception on failure
     */
    suspend fun pickMultipleImages(limit: Int = 5): Result<List<PickedImage>>
    
    /**
     * Pick an image with specific compression quality.
     * 
     * Launches the image picker and applies the specified compression
     * to the selected image before returning.
     * 
     * @param quality The compression quality to apply to the picked image
     * @return [Result] containing the compressed picked image on success,
     *         or an exception on failure
     */
    suspend fun pickImageWithCompression(quality: ImageQuality): Result<PickedImage>
    
    /**
     * Pick images with a custom configuration.
     * 
     * Provides more control over the image picking behavior through
     * an [ImagePickerConfig] object.
     * 
     * @param config Configuration for image picking behavior
     * @return [Result] containing the batch result on success,
     *         or an exception on failure
     */
    suspend fun pickImagesWithConfig(config: ImagePickerConfig): Result<ImageBatchResult>
    
    /**
     * Pick visual media (image or video) from the gallery.
     * 
     * Launches the platform's visual media picker that allows
     * selection of both images and videos.
     * 
     * @param maxItems Maximum number of items to select
     * @return [Result] containing the list of picked media items on success,
     *         or an exception on failure
     */
    suspend fun pickVisualMedia(maxItems: Int = 1): Result<List<ModelsImagePickerResult>>
    
    /**
     * Check if photo picker is available on this platform.
     * 
     * Some features may not be available on older OS versions.
     * 
     * @return true if the photo picker is available, false otherwise
     */
    fun isPhotoPickerAvailable(): Boolean
    
    /**
     * Get the last picked image from cache.
     * 
     * Useful for retrieving the result after the picker closes.
     * 
     * @return The last picked image, or null if no image has been picked
     */
    fun getLastPickedImage(): PickedImage?
    
    /**
     * Clear the cached picked image.
     * 
     * Should be called after processing the picked image to free memory.
     */
    fun clearCache()
}

/**
 * Exception thrown when image picking is cancelled by the user.
 */
class ImagePickerCancelledException : Exception("Image picking was cancelled by the user")

/**
 * Exception thrown when required permissions are not granted.
 */
class ImagePickerPermissionDeniedException(
    val permission: String
) : Exception("Permission denied: $permission")

/**
 * Exception thrown when the picked image is invalid or corrupted.
 */
class ImagePickerInvalidImageException(
    val reason: String
) : Exception("Invalid image: $reason")
