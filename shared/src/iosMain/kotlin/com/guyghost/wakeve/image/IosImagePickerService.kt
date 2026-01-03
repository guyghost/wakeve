package com.guyghost.wakeve.image

import com.guyghost.wakeve.models.ImageBatchResult
import com.guyghost.wakeve.models.ImagePickerConfig
import com.guyghost.wakeve.models.ImagePickerResult
import com.guyghost.wakeve.models.ImageQuality
import com.guyghost.wakeve.models.MediaType
import com.guyghost.wakeve.models.PickedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Photos.PHAsset
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHAuthorizationStatus
import platform.Photos.PHPhotoLibrary
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegate
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIViewController
import platform.dispatching
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS-specific implementation of [ImagePickerService] using PHPickerViewController.
 *
 * This class handles all iOS-specific image picking operations including:
 * - Photo library access via PHPickerViewController (iOS 14+)
 * - Multiple image selection
 * - Image compression using UIImage and ImageIO
 * - Permission handling for photo library access
 *
 * ## FC&IS Architecture
 *
 * This class belongs to the **Imperative Shell** layer as it handles:
 * - Platform-specific I/O operations (photo library access)
 * - UI presentation (PHPickerViewController presentation)
 * - Permission handling
 *
 * It uses models from the **Functional Core** layer:
 * - [PickedImage] - Pure data class for picked image metadata
 * - [ImageQuality] - Compression quality levels
 * - [ImagePickerConfig] - Configuration for picking behavior
 *
 * ## Usage
 *
 * ```kotlin
 * val pickerService: ImagePickerService = IosImagePickerService()
 *
 * // Check authorization
 * if (!pickerService.isAuthorized()) {
 *     // Request permission before picking
 * }
 *
 * // Pick a single image
 * val result = pickerService.pickImage()
 * result.fold(
 *     onSuccess = { image ->
 *         uploadImage(image.uri)
 *     },
 *     onFailure = { error ->
 *         showError("Failed to pick image: ${error.message}")
 *     }
 * )
 * ```
 *
 * @see ImagePickerService
 * @see PHPickerViewController
 */
class IosImagePickerService : ImagePickerService {

    private companion object {
        private const val TAG = "IosImagePickerService"
    }

    private var currentPicker: PHPickerViewController? = null
    private var cachedImage: PickedImage? = null
    private val _authorizationStatus = MutableStateFlow(checkAuthorizationStatus())

    /**
     * Picks a single image from the photo library.
     *
     * Launches PHPickerViewController in single selection mode and waits
     * for the user to select an image.
     *
     * @return [Result] containing the picked image on success,
     *         or an exception on failure (cancellation, permission denied, etc.)
     */
    override suspend fun pickImage(): Result<PickedImage> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            try {
                // Check authorization
                if (!isAuthorized()) {
                    continuation.resume(
                        Result.failure(ImagePickerPermissionDeniedException("NSPhotoLibraryUsageDescription"))
                    )
                    return@suspendCancellableCoroutine
                }

                // Create configuration for single image selection
                val configuration = PHPickerConfiguration(photoLibrary = PHPhotoLibrary.shared()).apply {
                    selectionLimit = 1
                    filter = PHPickerConfiguration.Filter.images
                }

                // Create picker
                val picker = PHPickerViewController(configuration = configuration)
                currentPicker = picker

                // Store continuation for callback
                val pickerContinuation = continuation

                // Set delegate
                picker.delegate = object : PHPickerViewControllerDelegate {
                    override fun picker(
                        picker: PHPickerViewController,
                        didFinishPicking: List<PHPickerResult>
                    ) {
                        currentPicker = null
                        picker.dismissViewControllerAnimated(true, completion = null)

                        if (didFinishPicking.isEmpty()) {
                            // User cancelled
                            pickerContinuation.resume(Result.failure(ImagePickerCancelledException()))
                            return
                        }

                        // Process the selected image
                        val result = didFinishPicking.first()
                        processPickerResult(result) { pickedImage ->
                            pickedImage?.let {
                                cachedImage = it
                                pickerContinuation.resume(Result.success(it))
                            } ?: run {
                                pickerContinuation.resume(Result.failure(ImagePickerInvalidImageException("Failed to process selected image")))
                            }
                        }
                    }
                }

                // Present picker
                val topViewController = getTopViewController()
                if (topViewController != null) {
                    topViewController.presentViewController(picker, animated = true, completion = null)
                } else {
                    continuation.resume(Result.failure(Exception("No view controller to present picker")))
                }

                // Handle cancellation
                continuation.invokeOnCancellation {
                    currentPicker?.dismissViewControllerAnimated(true, completion = null)
                    currentPicker = null
                }
            } catch (e: Exception) {
                currentPicker = null
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Picks multiple images from the photo library.
     *
     * Launches PHPickerViewController in multi-selection mode and waits
     * for the user to select multiple images.
     *
     * @param limit Maximum number of images that can be selected (default: 10)
     * @return [Result] containing the list of picked images on success,
     *         or an exception on failure
     */
    override suspend fun pickMultipleImages(limit: Int): Result<List<PickedImage>> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            try {
                // Check authorization
                if (!isAuthorized()) {
                    continuation.resume(
                        Result.failure(ImagePickerPermissionDeniedException("NSPhotoLibraryUsageDescription"))
                    )
                    return@suspendCancellableCoroutine
                }

                // Create configuration for multiple image selection
                val configuration = PHPickerConfiguration(photoLibrary = PHPhotoLibrary.shared()).apply {
                    selectionLimit = limit.coerceIn(1, 100)
                    filter = PHPickerConfiguration.Filter.images
                }

                // Create picker
                val picker = PHPickerViewController(configuration = configuration)
                currentPicker = picker

                // Store continuation for callback
                val pickerContinuation = continuation

                // Set delegate
                picker.delegate = object : PHPickerViewControllerDelegate {
                    override fun picker(
                        picker: PHPickerViewController,
                        didFinishPicking: List<PHPickerResult>
                    ) {
                        currentPicker = null
                        picker.dismissViewControllerAnimated(true, completion = null)

                        if (didFinishPicking.isEmpty()) {
                            // User cancelled
                            pickerContinuation.resume(Result.failure(ImagePickerCancelledException()))
                            return
                        }

                        // Process all selected images
                        val pickedImages = mutableListOf<PickedImage>()
                        var hasError = false
                        var error: Exception? = null

                        didFinishPicking.forEach { result ->
                            processPickerResult(result) { pickedImage ->
                                if (pickedImage != null) {
                                    pickedImages.add(pickedImage)
                                } else {
                                    hasError = true
                                    error = ImagePickerInvalidImageException("Failed to process one or more images")
                                }
                            }
                        }

                        if (hasError && pickedImages.isEmpty()) {
                            pickerContinuation.resume(Result.failure(error ?: Exception("Failed to process images")))
                        } else {
                            cachedImage = pickedImages.firstOrNull()
                            pickerContinuation.resume(Result.success(pickedImages))
                        }
                    }
                }

                // Present picker
                val topViewController = getTopViewController()
                if (topViewController != null) {
                    topViewController.presentViewController(picker, animated = true, completion = null)
                } else {
                    continuation.resume(Result.failure(Exception("No view controller to present picker")))
                }

                // Handle cancellation
                continuation.invokeOnCancellation {
                    currentPicker?.dismissViewControllerAnimated(true, completion = null)
                    currentPicker = null
                }
            } catch (e: Exception) {
                currentPicker = null
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Picks an image and applies compression based on the specified quality.
     *
     * This method first picks an image, then compresses it according to
     * the provided quality setting.
     *
     * @param quality The compression quality to apply to the picked image
     * @return [Result] containing the compressed picked image on success,
     *         or an exception on failure
     */
    override suspend fun pickImageWithCompression(quality: ImageQuality): Result<PickedImage> {
        // First, pick the image
        val pickResult = pickImage()
        if (pickResult.isFailure) {
            return Result.failure(pickResult.exceptionOrNull() ?: Exception("Unknown error"))
        }

        val pickedImage = pickResult.getOrNull()
            ?: return Result.failure(Exception("No image picked"))

        // Then, compress it
        return try {
            val compressedImage = compressImage(pickedImage, quality)
            Result.success(compressedImage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Picks images with a custom configuration.
     *
     * Provides more control over the image picking behavior through
     * an [ImagePickerConfig] object.
     *
     * @param config Configuration for image picking behavior
     * @return [Result] containing the batch result on success,
     *         or an exception on failure
     */
    override suspend fun pickImagesWithConfig(config: ImagePickerConfig): Result<ImageBatchResult> {
        // Check authorization
        if (!isAuthorized()) {
            return Result.failure(ImagePickerPermissionDeniedException("NSPhotoLibraryUsageDescription"))
        }

        val maxItems = config.maxSelectionLimit.coerceIn(1, 100)

        return if (maxItems == 1) {
            // Single image selection
            val result = pickImage()
            result.map { image ->
                val processedImage = if (config.enableCompression) {
                    compressImage(image, config.defaultCompressionQuality)
                } else {
                    image
                }
                ImageBatchResult(
                    images = listOf(processedImage),
                    config = config
                )
            }
        } else {
            // Multiple image selection
            val result = pickMultipleImages(maxItems)
            result.map { images ->
                val processedImages = if (config.enableCompression) {
                    images.map { image -> compressImage(image, config.defaultCompressionQuality) }
                } else {
                    images
                }
                ImageBatchResult(
                    images = processedImages,
                    config = config
                )
            }
        }
    }

    /**
     * Picks visual media (image or video) from the gallery.
     *
     * Launches the platform's visual media picker that allows
     * selection of both images and videos.
     *
     * @param maxItems Maximum number of items to select
     * @return [Result] containing the list of picked media items on success,
     *         or an exception on failure
     */
    override suspend fun pickVisualMedia(maxItems: Int): Result<List<ImagePickerResult>> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            try {
                // Check authorization
                if (!isAuthorized()) {
                    continuation.resume(
                        Result.failure(ImagePickerPermissionDeniedException("NSPhotoLibraryUsageDescription"))
                    )
                    return@suspendCancellableCoroutine
                }

                // Create configuration for visual media selection
                val configuration = PHPickerConfiguration(photoLibrary = PHPhotoLibrary.shared()).apply {
                    selectionLimit = maxItems.coerceIn(1, 100)
                    // Allow both images and videos
                    filter = PHPickerConfiguration.Filter.anyImage
                }

                // Create picker
                val picker = PHPickerViewController(configuration = configuration)
                currentPicker = picker

                // Store continuation for callback
                val pickerContinuation = continuation

                // Set delegate
                picker.delegate = object : PHPickerViewControllerDelegate {
                    override fun picker(
                        picker: PHPickerViewController,
                        didFinishPicking: List<PHPickerResult>
                    ) {
                        currentPicker = null
                        picker.dismissViewControllerAnimated(true, completion = null)

                        if (didFinishPicking.isEmpty()) {
                            // User cancelled
                            pickerContinuation.resume(Result.failure(ImagePickerCancelledException()))
                            return
                        }

                        // Process all selected media
                        val results = mutableListOf<ImagePickerResult>()
                        var hasError = false

                        didFinishPicking.forEachIndexed { index, result ->
                            processMediaResult(result) { mediaResult ->
                                if (mediaResult != null) {
                                    results.add(mediaResult)
                                } else {
                                    hasError = true
                                }
                            }
                        }

                        if (hasError && results.isEmpty()) {
                            pickerContinuation.resume(Result.failure(ImagePickerInvalidImageException("Failed to process one or more media items")))
                        } else {
                            pickerContinuation.resume(Result.success(results))
                        }
                    }
                }

                // Present picker
                val topViewController = getTopViewController()
                if (topViewController != null) {
                    topViewController.presentViewController(picker, animated = true, completion = null)
                } else {
                    continuation.resume(Result.failure(Exception("No view controller to present picker")))
                }

                // Handle cancellation
                continuation.invokeOnCancellation {
                    currentPicker?.dismissViewControllerAnimated(true, completion = null)
                    currentPicker = null
                }
            } catch (e: Exception) {
                currentPicker = null
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Checks if photo library access is authorized.
     *
     * @return true if access is authorized, false otherwise
     */
    override fun isAuthorized(): Boolean {
        val status = checkAuthorizationStatus()
        return status == PHAuthorizationStatus.Authorized ||
               status == PHAuthorizationStatus.Limited
    }

    /**
     * Returns a flow of authorization status changes.
     *
     * Emits values when the user grants or revokes photo library access.
     */
    override fun authorizationStatusFlow(): Flow<Boolean> = _authorizationStatus.asStateFlow()

    /**
     * Check if the photo picker is available on this platform.
     *
     * PHPickerViewController is available on iOS 14+.
     *
     * @return true if the photo picker is available, false otherwise
     */
    override fun isPhotoPickerAvailable(): Boolean = true

    /**
     * Get the last picked image from cache.
     *
     * Useful for retrieving the result after the picker closes.
     *
     * @return The last picked image, or null if no image has been picked
     */
    override fun getLastPickedImage(): PickedImage? = cachedImage

    /**
     * Clear the cached picked image.
     *
     * Should be called after processing the picked image to free memory.
     */
    override fun clearCache() {
        cachedImage = null
    }

    // MARK: - Private Helper Methods

    /**
     * Gets the top view controller for presenting the picker.
     */
    private fun getTopViewController(): UIViewController? {
        var rootViewController = UIApplication.sharedApplication?.keyWindow?.rootViewController

        while (rootViewController?.presentedViewController != null) {
            rootViewController = rootViewController.presentedViewController
        }

        return rootViewController
    }

    /**
     * Checks the current photo library authorization status.
     */
    private fun checkAuthorizationStatus(): PHAuthorizationStatus {
        return PHPhotoLibrary.authorizationStatus()
    }

    /**
     * Processes a picker result and converts it to a PickedImage.
     */
    private fun processPickerResult(
        result: PHPickerResult,
        completion: (PickedImage?) -> Unit
    ) {
        // Get the asset identifier
        val assetIdentifier = result.assetIdentifier
        if (assetIdentifier == null) {
            completion(null)
            return
        }

        // Fetch the PHAsset
        val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(
            identifiers = listOf(assetIdentifier),
            options = null
        )

        if (fetchResult.count == 0) {
            completion(null)
            return
        }

        val asset = fetchResult.firstObject as? PHAsset
        if (asset == null) {
            completion(null)
            return
        }

        // Request the image
        val options = PHImageRequestOptions().apply {
            deliveryMode = PHImageRequestOptions.DeliveryMode.HighQualityFormat
            resizeMode = PHImageRequestOptions.ResizeMode.None
            isNetworkAccessAllowed = true
        }

        PHImageManager.defaultManager().requestImageForAsset(
            targetSize = platform.UIKit.UIScreen.mainScreen.bounds.size.toCGSize(),
            contentMode = platform.Photos.PHImageContentMode.AspectFit,
            options = options
        ) { image, info ->
            if (image == null) {
                completion(null)
                return@requestImageForAsset
            }

            // Create PickedImage from UIImage
            val pickedImage = createPickedImageFromUIImage(image, asset)
            completion(pickedImage)
        }
    }

    /**
     * Processes a picker result and converts it to an ImagePickerResult.
     */
    private fun processMediaResult(
        result: PHPickerResult,
        completion: (ImagePickerResult?) -> Unit
    ) {
        // Get the asset identifier
        val assetIdentifier = result.assetIdentifier
        if (assetIdentifier == null) {
            completion(null)
            return
        }

        // Fetch the PHAsset
        val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(
            identifiers = listOf(assetIdentifier),
            options = null
        )

        if (fetchResult.count == 0) {
            completion(null)
            return
        }

        val asset = fetchResult.firstObject as? PHAsset
        if (asset == null) {
            completion(null)
            return
        }

        // Determine media type
        val mediaType = when (asset.mediaType) {
            PHAsset.PHAssetMediaType.Image -> MediaType.IMAGE
            PHAsset.PHAssetMediaType.Video -> MediaType.VIDEO
            else -> MediaType.IMAGE
        }

        // Request the image (for both images and videos, we get the thumbnail)
        val options = PHImageRequestOptions().apply {
            deliveryMode = PHImageRequestOptions.DeliveryMode.HighQualityFormat
            resizeMode = PHImageRequestOptions.ResizeMode.None
            isNetworkAccessAllowed = true
        }

        PHImageManager.defaultManager().requestImageForAsset(
            targetSize = platform.UIKit.UIScreen.mainScreen.bounds.size.toCGSize(),
            contentMode = platform.Photos.PHImageContentMode.AspectFit,
            options = options
        ) { image, info ->
            val width = image?.size?.width?.toInt() ?: 0
            val height = image?.size?.height?.toInt() ?: 0
            val fileSize = asset.pixelWidth * asset.pixelHeight * 4L // Approximate

            val imageResult = ImagePickerResult(
                uri = "ph://$assetIdentifier",
                mediaType = mediaType,
                sizeBytes = fileSize,
                width = width,
                height = height,
                mimeType = if (mediaType == MediaType.IMAGE) "image/jpeg" else "video/mp4"
            )

            completion(imageResult)
        }
    }

    /**
     * Creates a PickedImage from a UIImage and PHAsset.
     */
    private fun createPickedImageFromUIImage(
        image: UIImage,
        asset: PHAsset
    ): PickedImage? {
        val width = image.size.width.toInt()
        val height = image.size.height.toInt()
        val fileSize = asset.pixelWidth.toLong() * asset.pixelHeight.toLong() * 4 // Approximate bytes

        return PickedImage(
            uri = "ph://${asset.localIdentifier}",
            mediaType = MediaType.IMAGE,
            width = width,
            height = height,
            sizeBytes = fileSize,
            mimeType = "image/jpeg"
        )
    }

    /**
     * Compresses an image according to the specified quality.
     *
     * Uses UIImageJPEGRepresentation or UIImagePNGRepresentation
     * depending on the target quality level.
     *
     * @param image The image to compress
     * @param quality The compression quality level
     * @return A new PickedImage with compression applied
     */
    private fun compressImage(
        image: PickedImage,
        quality: ImageQuality
    ): PickedImage = withContext(Dispatchers.Default) {
        try {
            // Convert URI to UIImage
            val assetIdentifier = image.uri.removePrefix("ph://")
            val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(
                identifiers = listOf(assetIdentifier),
                options = null
            )

            if (fetchResult.count == 0) {
                return@withContext image
            }

            val asset = fetchResult.firstObject as? PHAsset ?: return@withContext image

            val options = PHImageRequestOptions().apply {
                deliveryMode = PHImageRequestOptions.DeliveryMode.HighQualityFormat
                resizeMode = PHImageRequestOptions.ResizeMode.None
                isNetworkAccessAllowed = true
            }

            var uiImage: UIImage? = null

            val semaphore = kotlinx.coroutines.sync.Semaphore(0)

            PHImageManager.defaultManager().requestImageForAsset(
                targetSize = platform.UIKit.UIScreen.mainScreen.bounds.size.toCGSize(),
                contentMode = platform.Photos.PHImageContentMode.AspectFit,
                options = options
            ) { image, info ->
                uiImage = image
                semaphore.release()
            }

            semaphore.acquire()

            val originalImage = uiImage ?: return@withContext image

            // Determine compression quality (0.0 - 1.0)
            val compressionQuality = when (quality) {
                ImageQuality.HIGH -> 0.9f
                ImageQuality.MEDIUM -> 0.75f
                ImageQuality.LOW -> 0.5f
            }

            // Compress the image
            val imageData = UIImageJPEGRepresentation(originalImage, compressionQuality)
                ?: UIImagePNGRepresentation(originalImage)

            if (imageData == null) {
                return@withContext image
            }

            val compressedSize = imageData.length.toLong()

            // Return a new PickedImage with compression
            image.copy(
                sizeBytes = compressedSize,
                compressionQuality = quality
            )
        } catch (e: Exception) {
            println("$TAG: Error compressing image: ${e.message}")
            image
        }
    }
}

/**
 * Extension function to convert CGSize to platform types.
 */
private fun platform.UIKit.CGSize.Companion.toCGSize(): platform.UIKit.CGSize {
    return platform.UIKit.CGSizeMake(platform.UIKit.UIScreen.mainScreen.bounds.size.width,
        platform.UIKit.UIScreen.mainScreen.bounds.size.height)
}
