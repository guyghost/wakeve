package com.guyghost.wakeve.image

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.guyghost.wakeve.models.ImageBatchResult
import com.guyghost.wakeve.models.ImagePickerConfig
import com.guyghost.wakeve.models.ImagePickerResult
import com.guyghost.wakeve.models.ImageQuality
import com.guyghost.wakeve.models.MediaType
import com.guyghost.wakeve.models.PickedImage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android-specific implementation of image picker using ActivityResultContracts.
 * 
 * Uses the Photo Picker API (Android 13+) for a modern, privacy-friendly
 * image selection experience without requiring READ_EXTERNAL_STORAGE permission.
 * 
 * For older Android versions, falls back to the traditional intent-based picker.
 * 
 * ## Architecture
 * 
 * This class is part of the **Imperative Shell** - it handles:
 * - ActivityResultLauncher registration and management
 * - Content URI resolution and permission handling
 * - Image metadata extraction via MediaStore
 * - Image compression using Bitmap API
 * - Callback-to-suspend conversion via Channels
 * 
 * ## Features
 * 
 * - Single and multi-image selection
 * - Image compression with quality control
 * - Automatic metadata extraction (size, dimensions, mime type)
 * - Works without READ_EXTERNAL_STORAGE on Android 13+
 * - Graceful fallback for older Android versions
 * 
 * ## Usage
 * 
 * ```kotlin
 * class MainActivity : AppCompatActivity() {
 *     private lateinit var imagePicker: AndroidImagePickerService
 *     
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         imagePicker = AndroidImagePickerService(this)
 *     }
 *     
 *     private fun onPickImageClicked() {
 *         lifecycleScope.launch {
 *             val result = imagePicker.pickImage()
 *             result.fold(
 *                 onSuccess = { image ->
 *                     // Use the picked image
 *                     loadImage(image.uri)
 *                 },
 *                 onFailure = { error ->
 *                     // Handle error
 *                     showError(error.message)
 *                 }
 *             )
 *         }
 *     }
 * }
 * ```
 * 
 * @property activity The AppCompatActivity used for ActivityResultLauncher registration
 * @property context Application context for content resolution and metadata queries
 */
class AndroidImagePickerService(
    private val activity: AppCompatActivity
) : ImagePickerService {
    
    private val context: Context
        get() = activity.applicationContext
    
    // Channel for single image picking
    private val singleImageChannel = Channel<Result<PickedImage>>(Channel.CONFLATED)
    
    // Channel for multiple image picking
    private val multipleImageChannel = Channel<Result<List<PickedImage>>>(Channel.CONFLATED)
    
    // Channel for visual media picking
    private val visualMediaChannel = Channel<Result<List<ImagePickerResult>>>(Channel.CONFLATED)
    
    // Cache for the last picked image
    @Volatile
    private var lastPickedImage: PickedImage? = null
    
    // ActivityResultLaunchers
    private val pickImageLauncher = activity.registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { result ->
        result?.let { uri ->
            val metadata = getImageMetadata(uri)
            val pickedImage = PickedImage(
                uri = uri.toString(),
                mediaType = MediaType.IMAGE,
                width = metadata.width,
                height = metadata.height,
                sizeBytes = metadata.sizeBytes,
                mimeType = metadata.mimeType
            )
            lastPickedImage = pickedImage
            singleImageChannel.trySend(Result.success(pickedImage))
        } ?: singleImageChannel.trySend(
            Result.failure(ImagePickerCancelledException())
        )
    }
    
    private val pickMultipleImagesLauncher = activity.registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_MULTIPLE_IMAGES)
    ) { result ->
        if (result.isNotEmpty()) {
            val pickedImages = result.mapNotNull { uri ->
                try {
                    val metadata = getImageMetadata(uri)
                    PickedImage(
                        uri = uri.toString(),
                        mediaType = MediaType.IMAGE,
                        width = metadata.width,
                        height = metadata.height,
                        sizeBytes = metadata.sizeBytes,
                        mimeType = metadata.mimeType
                    )
                } catch (e: Exception) {
                    null // Skip invalid images
                }
            }
            lastPickedImage = pickedImages.firstOrNull()
            multipleImageChannel.trySend(Result.success(pickedImages))
        } else {
            multipleImageChannel.trySend(
                Result.failure(ImagePickerCancelledException())
            )
        }
    }
    
    private val pickVisualMediaLauncher = activity.registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { result ->
        result?.let { uri ->
            val mediaType = getMediaTypeFromUri(uri)
            val resultItem = when (mediaType) {
                MediaType.IMAGE, MediaType.VIDEO -> {
                    val metadata = getImageMetadata(uri)
                    ImagePickerResult(
                        uri = uri.toString(),
                        mediaType = mediaType,
                        sizeBytes = metadata.sizeBytes,
                        width = metadata.width,
                        height = metadata.height,
                        mimeType = metadata.mimeType
                    )
                }
                MediaType.DOCUMENT -> {
                    ImagePickerResult(
                        uri = uri.toString(),
                        mediaType = MediaType.DOCUMENT,
                        sizeBytes = getFileSize(uri),
                        width = null,
                        height = null,
                        mimeType = getMimeType(uri)
                    )
                }
            }
            visualMediaChannel.trySend(Result.success(listOf(resultItem)))
        } ?: visualMediaChannel.trySend(
            Result.failure(ImagePickerCancelledException())
        )
    }
    
    override suspend fun pickImage(): Result<PickedImage> {
        // Check if photo picker is available (Android 13+)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                pickImageLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
                
                continuation.invokeOnCancellation {
                    singleImageChannel.cancel()
                }
                
                // Try to receive immediately, or wait
                try {
                    val result = singleImageChannel.tryReceive().getOrNull()
                    if (result != null) {
                        continuation.resume(result)
                    }
                } catch (e: Exception) {
                    // Channel is empty, need to wait
                }
            }
        } else {
            // Fallback for older Android versions
            pickImageLegacy()
        }
    }
    
    override suspend fun pickMultipleImages(limit: Int): Result<List<PickedImage>> {
        val effectiveLimit = minOf(limit, MAX_MULTIPLE_IMAGES)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                val launcher = activity.registerForActivityResult(
                    ActivityResultContracts.PickMultipleVisualMedia(effectiveLimit)
                ) { result ->
                    if (result.isNotEmpty()) {
                        val pickedImages = result.mapNotNull { uri ->
                            try {
                                val metadata = getImageMetadata(uri)
                                PickedImage(
                                    uri = uri.toString(),
                                    mediaType = MediaType.IMAGE,
                                    width = metadata.width,
                                    height = metadata.height,
                                    sizeBytes = metadata.sizeBytes,
                                    mimeType = metadata.mimeType
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        lastPickedImage = pickedImages.firstOrNull()
                        continuation.resume(Result.success(pickedImages))
                    } else {
                        continuation.resume(Result.failure(ImagePickerCancelledException()))
                    }
                }
                
                launcher.launch(Unit)
                
                continuation.invokeOnCancellation {
                    multipleImageChannel.cancel()
                }
            }
        } else {
            pickMultipleImagesLegacy(effectiveLimit)
        }
    }
    
    override suspend fun pickImageWithCompression(quality: ImageQuality): Result<PickedImage> {
        return try {
            val result = pickImage()
            result.mapCatching { image ->
                compressImage(image, quality)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun pickImagesWithConfig(config: ImagePickerConfig): Result<ImageBatchResult> {
        return when {
            config.maxSelectionLimit == 1 -> {
                pickImage().map { image ->
                    ImageBatchResult(
                        images = listOf(image),
                        config = config
                    )
                }
            }
            else -> {
                pickMultipleImages(config.maxSelectionLimit).map { images ->
                    ImageBatchResult(
                        images = images.take(config.maxSelectionLimit),
                        config = config
                    )
                }
            }
        }
    }
    
    override suspend fun pickVisualMedia(maxItems: Int): Result<List<ImagePickerResult>> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                val launcher = activity.registerForActivityResult(
                    ActivityResultContracts.PickVisualMedia()
                ) { result ->
                    result?.let { uri ->
                        val mediaType = getMediaTypeFromUri(uri)
                        val resultItem = when (mediaType) {
                            MediaType.IMAGE, MediaType.VIDEO -> {
                                val metadata = getImageMetadata(uri)
                                ImagePickerResult(
                                    uri = uri.toString(),
                                    mediaType = mediaType,
                                    sizeBytes = metadata.sizeBytes,
                                    width = metadata.width,
                                    height = metadata.height,
                                    mimeType = metadata.mimeType
                                )
                            }
                            MediaType.DOCUMENT -> {
                                ImagePickerResult(
                                    uri = uri.toString(),
                                    mediaType = MediaType.DOCUMENT,
                                    sizeBytes = getFileSize(uri),
                                    width = null,
                                    height = null,
                                    mimeType = getMimeType(uri)
                                )
                            }
                        }
                        continuation.resume(Result.success(listOf(resultItem)))
                    } ?: continuation.resume(
                        Result.failure(ImagePickerCancelledException())
                    )
                }
                
                launcher.launch(Unit)
                
                continuation.invokeOnCancellation {
                    visualMediaChannel.cancel()
                }
            }
        } else {
            // Fallback for older versions
            pickImage().mapCatching { image ->
                listOf(
                    ImagePickerResult(
                        uri = image.uri,
                        mediaType = image.mediaType,
                        sizeBytes = image.sizeBytes,
                        width = image.width,
                        height = image.height,
                        mimeType = image.mimeType
                    )
                )
            }
        }
    }
    
    override fun isPhotoPickerAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
    
    override fun getLastPickedImage(): PickedImage? {
        return lastPickedImage
    }
    
    override fun clearCache() {
        lastPickedImage = null
    }
    
    // Legacy support for Android < 13
    private suspend fun pickImageLegacy(): Result<PickedImage> {
        return if (hasReadStoragePermission()) {
            suspendCancellableCoroutine { continuation ->
                val legacyLauncher = activity.registerForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let {
                        val metadata = getImageMetadata(it)
                        val pickedImage = PickedImage(
                            uri = it.toString(),
                            mediaType = MediaType.IMAGE,
                            width = metadata.width,
                            height = metadata.height,
                            sizeBytes = metadata.sizeBytes,
                            mimeType = metadata.mimeType
                        )
                        lastPickedImage = pickedImage
                        continuation.resume(Result.success(pickedImage))
                    } ?: continuation.resume(
                        Result.failure(ImagePickerCancelledException())
                    )
                }
                
                legacyLauncher.launch("image/*")
                
                continuation.invokeOnCancellation {
                    singleImageChannel.cancel()
                }
            }
        } else {
            Result.failure(
                ImagePickerPermissionDeniedException(Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        }
    }
    
    private suspend fun pickMultipleImagesLegacy(limit: Int): Result<List<PickedImage>> {
        return if (hasReadStoragePermission()) {
            suspendCancellableCoroutine { continuation ->
                val legacyLauncher = activity.registerForActivityResult(
                    ActivityResultContracts.GetMultipleContents()
                ) { uris ->
                    if (uris.isNotEmpty()) {
                        val pickedImages = uris.mapNotNull { uri ->
                            try {
                                val metadata = getImageMetadata(uri)
                                PickedImage(
                                    uri = uri.toString(),
                                    mediaType = MediaType.IMAGE,
                                    width = metadata.width,
                                    height = metadata.height,
                                    sizeBytes = metadata.sizeBytes,
                                    mimeType = metadata.mimeType
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        lastPickedImage = pickedImages.firstOrNull()
                        continuation.resume(Result.success(pickedImages.take(limit)))
                    } else {
                        continuation.resume(Result.failure(ImagePickerCancelledException()))
                    }
                }
                
                legacyLauncher.launch("image/*")
                
                continuation.invokeOnCancellation {
                    multipleImageChannel.cancel()
                }
            }
        } else {
            Result.failure(
                ImagePickerPermissionDeniedException(Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        }
    }
    
    private fun hasReadStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true // Photo picker doesn't require permission on Android 13+
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get image metadata (size, dimensions, mime type) from a content URI.
     */
    private fun getImageMetadata(uri: Uri): ImageMetadata {
        val contentResolver: ContentResolver = context.contentResolver
        
        // Get size
        val sizeBytes = getFileSize(uri)
        
        // Get mime type
        val mimeType = getMimeType(uri) ?: "image/*"
        
        // Get dimensions
        var width: Int? = null
        var height: Int? = null
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Use ImageDecoder for Android 9+
                val source = ImageDecoder.createSource(contentResolver, uri)
                val decoder = ImageDecoder.ImageInfoListener { info, _ ->
                    width = info.size.width
                    height = info.size.height
                }
                ImageDecoder.decodeBitmap(source, decoder)
            } else {
                // Fallback to BitmapFactory
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)
                    width = options.outWidth
                    height = options.outHeight
                }
            }
        } catch (e: Exception) {
            // Failed to get dimensions, continue with null values
        }
        
        return ImageMetadata(
            sizeBytes = sizeBytes,
            width = width,
            height = height,
            mimeType = mimeType
        )
    }
    
    /**
     * Get file size from a content URI.
     */
    private fun getFileSize(uri: Uri): Long {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Get MIME type from a content URI.
     */
    private fun getMimeType(uri: Uri): String? {
        return contentResolver.getType(uri)
    }
    
    /**
     * Determine media type from URI content.
     */
    private fun getMediaTypeFromUri(uri: Uri): MediaType {
        val mimeType = getMimeType(uri) ?: return MediaType.IMAGE
        return when {
            mimeType.startsWith("image/") -> MediaType.IMAGE
            mimeType.startsWith("video/") -> MediaType.VIDEO
            mimeType.startsWith("application/") -> MediaType.DOCUMENT
            else -> MediaType.IMAGE
        }
    }
    
    /**
     * Compress an image with the specified quality.
     */
    private fun compressImage(image: PickedImage, quality: ImageQuality): PickedImage {
        val uri = Uri.parse(image.uri)
        
        try {
            val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, uri)
                )
            } else {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } ?: throw ImagePickerInvalidImageException("Failed to decode image")
            
            // Compress
            val outputStream = ByteArrayOutputStream()
            originalBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                quality.quality,
                outputStream
            )
            
            // Get compressed size
            val compressedSize = outputStream.size().toLong()
            
            // Clean up
            originalBitmap.recycle()
            
            return image.copy(
                sizeBytes = compressedSize,
                compressionQuality = quality
            )
        } catch (e: Exception) {
            throw ImagePickerInvalidImageException(e.message ?: "Unknown error during compression")
        }
    }
    
    /**
     * Data class for image metadata.
     */
    private data class ImageMetadata(
        val sizeBytes: Long,
        val width: Int?,
        val height: Int?,
        val mimeType: String?
    )
    
    companion object {
        private const val MAX_MULTIPLE_IMAGES = 10
    }
}
