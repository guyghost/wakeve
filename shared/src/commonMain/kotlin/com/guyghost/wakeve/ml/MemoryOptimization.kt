package com.guyghost.wakeve.ml

/**
 * Memory optimization configuration for ML photo processing.
 *
 * Controls memory usage when processing high-resolution photos.
 * All processing runs on-device, so memory optimization is critical.
 *
 * Key Features:
 * - Downsampling: Reduces image size before ML processing
 * - Memory pooling: Reuses memory buffers
 * - Batch processing: Processes multiple photos with controlled memory
 * - GC hints: Suggests garbage collection after processing
 */
data class MemoryOptimizationConfig(
    /**
     * Maximum image size (width or height) for ML processing.
     * Images larger than this are downsampled.
     *
     * Recommended: 1024px for face detection, 800px for classification
     */
    val maxImageSize: Int = 1024,

    /**
     * Target memory usage in megabytes for ML processing.
     * The system will throttle processing if memory exceeds this.
     *
     * Recommended: 128MB for phones, 256MB for tablets
     */
    val targetMemoryMB: Int = 128,

    /**
     * Number of photos to process in a single batch.
     * Batching improves efficiency but increases memory usage.
     *
     * Recommended: 3-5 photos per batch
     */
    val batchSize: Int = 5,

    /**
     * Downsample quality factor (0.0 - 1.0).
     * 1.0 = original quality (memory intensive)
     * 0.5 = 50% quality
     *
     * Recommended: 0.75 for face detection, 0.85 for classification
     */
    val downsampleQuality: Double = 0.75,

    /**
     * Enable memory pooling to reuse image buffers.
     * Significant memory savings for multiple photo processing.
     */
    val enableMemoryPooling: Boolean = true,

    /**
     * Enable aggressive garbage collection after each batch.
     * Helps prevent OOM errors but may cause slight pauses.
     */
    val enableAggressiveGC: Boolean = false,

    /**
     * Timeout in milliseconds for ML processing.
     * Prevents hangs on very large images.
     *
     * Recommended: 5000ms (5 seconds)
     */
    val processingTimeoutMs: Int = 5000
) {
    companion object {
        /**
         * Conservative config for low-memory devices.
         */
        val CONSERVATIVE = MemoryOptimizationConfig(
            maxImageSize = 512,
            targetMemoryMB = 64,
            batchSize = 2,
            downsampleQuality = 0.5,
            enableMemoryPooling = true,
            enableAggressiveGC = true,
            processingTimeoutMs = 10000
        )

        /**
         * Balanced config for typical devices.
         */
        val BALANCED = MemoryOptimizationConfig(
            maxImageSize = 1024,
            targetMemoryMB = 128,
            batchSize = 5,
            downsampleQuality = 0.75,
            enableMemoryPooling = true,
            enableAggressiveGC = false,
            processingTimeoutMs = 5000
        )

        /**
         * Aggressive config for high-end devices.
         */
        val AGGRESSIVE = MemoryOptimizationConfig(
            maxImageSize = 2048,
            targetMemoryMB = 256,
            batchSize = 10,
            downsampleQuality = 0.9,
            enableMemoryPooling = true,
            enableAggressiveGC = false,
            processingTimeoutMs = 3000
        )
    }

    /**
     * Validates configuration values.
     */
    init {
        require(maxImageSize in 256..4096) {
            "maxImageSize must be between 256 and 4096"
        }
        require(targetMemoryMB in 32..1024) {
            "targetMemoryMB must be between 32 and 1024"
        }
        require(batchSize in 1..20) {
            "batchSize must be between 1 and 20"
        }
        require(downsampleQuality in 0.25..1.0) {
            "downsampleQuality must be between 0.25 and 1.0"
        }
        require(processingTimeoutMs in 1000..30000) {
            "processingTimeoutMs must be between 1000 and 30000"
        }
    }
}

/**
 * Result of memory-optimized image processing.
 *
 * @param success Whether processing completed successfully
 * @param originalSize Original image dimensions (width, height)
 * @param processedSize Downsampled image dimensions (width, height)
 * @param memoryUsedMB Memory used during processing
 * @param processingTimeMs Time taken to process image
 * @param downsampleFactor Factor by which image was downsampled (1.0 = no downsampling)
 */
data class MemoryOptimizedResult(
    val success: Boolean,
    val originalSize: ImageSize,
    val processedSize: ImageSize,
    val memoryUsedMB: Double,
    val processingTimeMs: Long,
    val downsampleFactor: Double
) {
    /**
     * Calculates memory savings percentage.
     */
    fun memorySavingsPercentage(): Double {
        val originalPixels = originalSize.width.toLong() * originalSize.height.toLong()
        val processedPixels = processedSize.width.toLong() * processedSize.height.toLong()
        return ((originalPixels - processedPixels).toDouble() / originalPixels) * 100
    }
}

/**
 * Image dimensions.
 */
data class ImageSize(
    val width: Int,
    val height: Int
) {
    /**
     * Calculates total number of pixels.
     */
    fun pixelCount(): Long = width.toLong() * height.toLong()

    /**
     * Calculates aspect ratio (width / height).
     */
    fun aspectRatio(): Double = width.toDouble() / height.toDouble()
}

/**
 * Memory pool for reusing image buffers.
 *
 * Maintains a pool of pre-allocated memory buffers to avoid
 * repeated allocations and garbage collection overhead.
 */
interface ImageMemoryPool {
    /**
     * Acquires a buffer from the pool.
     *
     * @param size Size of buffer to acquire in bytes
     * @return Buffer or null if pool is exhausted
     */
    fun acquireBuffer(size: Int): ByteArray?

    /**
     * Returns a buffer to the pool.
     *
     * @param buffer Buffer to return to pool
     */
    fun releaseBuffer(buffer: ByteArray)

    /**
     * Clears all buffers from the pool.
     */
    fun clear()

    /**
     * Gets current pool statistics.
     */
    fun getStatistics(): MemoryPoolStatistics
}

/**
 * Statistics about the memory pool.
 */
data class MemoryPoolStatistics(
    val totalBuffers: Int,
    val usedBuffers: Int,
    val availableBuffers: Int,
    val totalMemoryBytes: Long,
    val usedMemoryBytes: Long,
    val availableMemoryBytes: Long
)

/**
 * Memory-optimized image processor.
 *
 * Handles downsampling, memory pooling, and batch processing
 * to efficiently process high-resolution photos with ML models.
 */
interface MemoryOptimizedImageProcessor {

    /**
     * Processes a single photo with memory optimization.
     *
     * @param image The image to process (platform-specific type)
     * @param config Memory optimization configuration
     * @return MemoryOptimizedResult with processing details
     */
    suspend fun processImage(
        image: Any,
        config: MemoryOptimizationConfig = MemoryOptimizationConfig.BALANCED
    ): MemoryOptimizedResult

    /**
     * Processes multiple photos in batches.
     *
     * @param images List of images to process
     * @param config Memory optimization configuration
     * @return Flow of MemoryOptimizedResult for each image
     */
    suspend fun processImagesInBatches(
        images: List<Any>,
        config: MemoryOptimizationConfig = MemoryOptimizationConfig.BALANCED
    ): List<MemoryOptimizedResult>

    /**
     * Gets current memory usage.
     */
    fun getCurrentMemoryUsageMB(): Double

    /**
     * Checks if processing should be throttled due to memory pressure.
     */
    fun shouldThrottleProcessing(config: MemoryOptimizationConfig): Boolean

    /**
     * Suggests garbage collection (platform-specific).
     */
    suspend fun suggestGarbageCollection()
}

/**
 * Memory usage threshold for warnings.
 */
enum class MemoryPressureThreshold {
    LOW,      // < 50% of target
    MEDIUM,   // 50-75% of target
    HIGH,     // 75-90% of target
    CRITICAL  // > 90% of target
}

/**
 * Gets the current memory pressure based on usage.
 */
fun getMemoryPressureThreshold(usedMB: Double, targetMB: Int): MemoryPressureThreshold {
    val ratio = usedMB / targetMB
    return when {
        ratio < 0.5 -> MemoryPressureThreshold.LOW
        ratio < 0.75 -> MemoryPressureThreshold.MEDIUM
        ratio < 0.9 -> MemoryPressureThreshold.HIGH
        else -> MemoryPressureThreshold.CRITICAL
    }
}

/**
 * Calculates the optimal downsample factor based on image size and config.
 */
fun calculateOptimalDownsampleFactor(
    imageWidth: Int,
    imageHeight: Int,
    maxImageSize: Int,
    qualityFactor: Double
): Double {
    val maxDimension = maxOf(imageWidth, imageHeight)

    // If already within limits, apply quality factor
    if (maxDimension <= maxImageSize) {
        return qualityFactor
    }

    // Calculate required downsample factor to fit within maxImageSize
    val requiredFactor = maxImageSize.toDouble() / maxDimension

    // Apply quality factor, but don't go below required factor
    return maxOf(requiredFactor, qualityFactor)
}

/**
 * Estimates memory usage for image processing.
 *
 * @param width Image width in pixels
 * @param height Image height in pixels
 * @param colorDepth Bits per pixel (typically 32 for RGBA)
 * @return Estimated memory usage in megabytes
 */
fun estimateImageMemoryUsageMB(width: Int, height: Int, colorDepth: Int = 32): Double {
    val bits = width.toLong() * height * colorDepth
    val bytes = bits / 8
    return bytes / (1024.0 * 1024.0)
}
