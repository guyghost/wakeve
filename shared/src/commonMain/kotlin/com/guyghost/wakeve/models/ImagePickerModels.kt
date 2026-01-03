package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Media types supported by image picker.
 *
 * @property type String representation of the media type
 */
@Serializable
enum class MediaType(val type: String) {
    /** Image files (JPEG, PNG, GIF, WebP, etc.) */
    IMAGE("image"),
    
    /** Video files (MP4, MOV, AVI, etc.) */
    VIDEO("video"),
    
    /** Document files (PDF, DOC, etc.) */
    DOCUMENT("document")
}

/**
 * Image quality options for compression.
 *
 * @property quality Compression quality percentage (0-100)
 */
@Serializable
enum class ImageQuality(val quality: Int) {
    /** High quality (90% compression) */
    HIGH(90),
    
    /** Medium quality (75% compression) */
    MEDIUM(75),
    
    /** Low quality (50% compression) */
    LOW(50)
}

/**
 * Result of image picker operation.
 *
 * Contains metadata about the selected media item.
 *
 * @property uri Content URI of the selected media
 * @property mediaType Type of media (image, video, document)
 * @property sizeBytes Size of the file in bytes
 * @property width Width in pixels (null for documents)
 * @property height Height in pixels (null for documents)
 * @property mimeType MIME type of the file (e.g., "image/jpeg")
 */
@Serializable
data class ImagePickerResult(
    val uri: String,
    val mediaType: MediaType,
    val sizeBytes: Long,
    val width: Int?,
    val height: Int?,
    val mimeType: String?
)

/**
 * Picked image with metadata and optional compression settings.
 *
 * This is the primary model used when an image has been picked
 * and is ready for use in the application.
 *
 * @property uri Content URI of the picked image
 * @property mediaType Type of media (always IMAGE for this model)
 * @property width Width in pixels
 * @property height Height in pixels
 * @property sizeBytes Size of the file in bytes
 * @property mimeType MIME type of the file
 * @property compressionQuality Applied compression quality (null if uncompressed)
 */
@Serializable
data class PickedImage(
    val uri: String,
    val mediaType: MediaType,
    val width: Int?,
    val height: Int?,
    val sizeBytes: Long,
    val mimeType: String?,
    val compressionQuality: ImageQuality? = null
) {
    /**
     * Check if the image is compressed.
     */
    val isCompressed: Boolean
        get() = compressionQuality != null
    
    /**
     * Get human-readable file size.
     */
    val formattedSize: String
        get() = formatFileSize(sizeBytes)
    
    /**
     * Get image dimensions as a formatted string.
     */
    val dimensions: String?
        get() = if (width != null && height != null) "${width}×${height}" else null
    
    /**
     * Calculate the aspect ratio of the image.
     */
    val aspectRatio: Float?
        get() = if (width != null && height != null && height > 0) {
            width.toFloat() / height.toFloat()
        } else null
    
    companion object {
        /**
         * Format file size in bytes to human-readable string.
         */
        internal fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> {
                    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
                    val rounded = (gb * 100).toLong() / 100.0
                    "$rounded GB"
                }
            }
        }
        
        /**
         * Create a PickedImage from an ImagePickerResult.
         */
        fun fromResult(result: ImagePickerResult): PickedImage {
            return PickedImage(
                uri = result.uri,
                mediaType = result.mediaType,
                width = result.width,
                height = result.height,
                sizeBytes = result.sizeBytes,
                mimeType = result.mimeType
            )
        }
    }
}

/**
 * Configuration for image picking behavior.
 *
 * @property maxSelectionLimit Maximum number of images that can be selected
 * @property allowedMediaTypes List of allowed media types
 * @property enableCompression Whether to enable image compression
 * @property defaultCompressionQuality Default compression quality if enabled
 */
@Serializable
data class ImagePickerConfig(
    val maxSelectionLimit: Int = 5,
    val allowedMediaTypes: List<MediaType> = listOf(MediaType.IMAGE),
    val enableCompression: Boolean = false,
    val defaultCompressionQuality: ImageQuality = ImageQuality.MEDIUM
) {
    companion object {
        /** Default configuration for single image selection */
        val singleImage = ImagePickerConfig(
            maxSelectionLimit = 1,
            allowedMediaTypes = listOf(MediaType.IMAGE),
            enableCompression = false
        )
        
        /** Default configuration for multiple image selection */
        val multipleImages = ImagePickerConfig(
            maxSelectionLimit = 5,
            allowedMediaTypes = listOf(MediaType.IMAGE),
            enableCompression = false
        )
        
        /** Configuration for compressed image selection */
        val compressedImage = ImagePickerConfig(
            maxSelectionLimit = 1,
            allowedMediaTypes = listOf(MediaType.IMAGE),
            enableCompression = true,
            defaultCompressionQuality = ImageQuality.MEDIUM
        )
    }
}

/**
 * Result of a batch image picking operation.
 *
 * @property images List of picked images
 * @property config Configuration used for picking
 * @property totalSizeBytes Combined size of all images in bytes
 */
@Serializable
data class ImageBatchResult(
    val images: List<PickedImage>,
    val config: ImagePickerConfig,
    val totalSizeBytes: Long = images.sumOf { it.sizeBytes }
) {
    /**
     * Check if the batch is empty.
     */
    val isEmpty: Boolean
        get() = images.isEmpty()
    
    /**
     * Get count of images in the batch.
     */
    val count: Int
        get() = images.size
    
    /**
     * Get human-readable total size.
     */
    val formattedTotalSize: String
        get() = PickedImage.formatFileSize(totalSizeBytes)
}
