package com.guyghost.wakeve.ml

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of memory-optimized image processing.
 *
 * Uses Android Bitmap with efficient sampling and scaling.
 *
 * Features:
 * - BitmapFactory.Options for inSampleSize (power of 2)
 * - Bitmap.Config.RGB_565 for memory savings
 * - Bitmap.createScaledBitmap for precise sizing
 * - Bitmap recycling for memory cleanup
 */
class AndroidMemoryOptimizedImageProcessor : MemoryOptimizedImageProcessor {

    private companion object {
        private const val TAG = "AndroidImageProcessor"
    }

    private val memoryPool = AndroidImageMemoryPool(maxPoolSize = 10)

    /**
     * Processes a single photo with memory optimization.
     */
    override suspend fun processImage(
        image: Any,
        config: MemoryOptimizationConfig
    ): MemoryOptimizedResult = withContext(Dispatchers.Default) {
        val bitmap = image as? Bitmap ?: return@withContext MemoryOptimizedResult(
            success = false,
            originalSize = ImageSize(0, 0),
            processedSize = ImageSize(0, 0),
            memoryUsedMB = 0.0,
            processingTimeMs = 0,
            downsampleFactor = 1.0
        )

        val startTime = System.currentTimeMillis()
        val originalSize = ImageSize(bitmap.width, bitmap.height)

        try {
            // Calculate optimal downsample factor
            val downsampleFactor = calculateOptimalDownsampleFactor(
                imageWidth = bitmap.width,
                imageHeight = bitmap.height,
                maxImageSize = config.maxImageSize,
                qualityFactor = config.downsampleQuality
            )

            // Estimate memory usage before processing
            val originalMemoryMB = estimateImageMemoryUsageMB(
                width = bitmap.width,
                height = bitmap.height,
                colorDepth = 32 // ARGB_8888
            )

            // Check if we should throttle
            if (shouldThrottleProcessing(config)) {
                System.gc()
            }

            // Downsample if needed
            val processedBitmap = if (downsampleFactor < 1.0) {
                downsampleBitmap(bitmap, downsampleFactor, config)
            } else {
                bitmap
            }

            val processedSize = ImageSize(processedBitmap.width, processedBitmap.height)

            // Estimate memory usage after processing
            val processedMemoryMB = estimateImageMemoryUsageMB(
                width = processedBitmap.width,
                height = processedBitmap.height,
                colorDepth = if (processedBitmap.config == Bitmap.Config.RGB_565) 16 else 32
            )

            val processingTimeMs = System.currentTimeMillis() - startTime

            // Cleanup
            if (processedBitmap != bitmap) {
                processedBitmap.recycle()
            }

            if (config.enableAggressiveGC) {
                suggestGarbageCollection()
            }

            MemoryOptimizedResult(
                success = true,
                originalSize = originalSize,
                processedSize = processedSize,
                memoryUsedMB = processedMemoryMB,
                processingTimeMs = processingTimeMs,
                downsampleFactor = downsampleFactor
            )

        } catch (e: Exception) {
            println("$TAG: Error processing image: ${e.message}")
            MemoryOptimizedResult(
                success = false,
                originalSize = originalSize,
                processedSize = ImageSize(0, 0),
                memoryUsedMB = 0.0,
                processingTimeMs = 0,
                downsampleFactor = 1.0
            )
        }
    }

    /**
     * Processes multiple photos in batches.
     */
    override suspend fun processImagesInBatches(
        images: List<Any>,
        config: MemoryOptimizationConfig
    ): List<MemoryOptimizedResult> = withContext(Dispatchers.Default) {
        val results = mutableListOf<MemoryOptimizedResult>()

        // Split into batches
        val batches = images.chunked(config.batchSize)

        for (batch in batches) {
            // Check memory pressure before processing batch
            if (shouldThrottleProcessing(config)) {
                suggestGarbageCollection()
            }

            // Process batch
            for (image in batch) {
                val result = processImage(image, config)
                results.add(result)

                // Break if processing fails
                if (!result.success) {
                    break
                }
            }

            // Suggest GC after each batch
            if (config.enableAggressiveGC) {
                suggestGarbageCollection()
            }
        }

        results
    }

    /**
     * Gets current memory usage.
     */
    override fun getCurrentMemoryUsageMB(): Double {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024.0 * 1024.0)
    }

    /**
     * Checks if processing should be throttled due to memory pressure.
     */
    override fun shouldThrottleProcessing(config: MemoryOptimizationConfig): Boolean {
        val currentUsageMB = getCurrentMemoryUsageMB()
        val threshold = getMemoryPressureThreshold(currentUsageMB, config.targetMemoryMB)
        return threshold in listOf(MemoryPressureThreshold.HIGH, MemoryPressureThreshold.CRITICAL)
    }

    /**
     * Suggests garbage collection.
     */
    override suspend fun suggestGarbageCollection() {
        System.gc()
        // Give GC a moment to run
        kotlinx.coroutines.delay(10)
    }

    // MARK: - Private Helper Methods

    /**
     * Downsamples a bitmap by the given factor.
     */
    private fun downsampleBitmap(
        bitmap: Bitmap,
        factor: Double,
        config: MemoryOptimizationConfig
    ): Bitmap {
        val newWidth = (bitmap.width * factor).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * factor).toInt().coerceAtLeast(1)

        // Use RGB_565 for memory savings (16 bits per pixel vs 32)
        val bitmapConfig = if (config.downsampleQuality < 0.7) {
            Bitmap.Config.RGB_565
        } else {
            Bitmap.Config.ARGB_8888
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true).let { scaledBitmap ->
            scaledBitmap.copy(bitmapConfig, scaledBitmap.isMutable)
        }
    }

    /**
     * Decodes a bitmap with efficient sampling options.
     */
    private fun decodeBitmapWithSampling(
        data: ByteArray,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        // First pass: get dimensions
        BitmapFactory.decodeByteArray(data, 0, data.size, options)

        // Calculate inSampleSize (power of 2)
        options.inSampleSize = calculateInSampleSize(
            options.outWidth,
            options.outHeight,
            targetWidth,
            targetHeight
        )

        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565

        return BitmapFactory.decodeByteArray(data, 0, data.size, options)
    }

    /**
     * Calculates the inSampleSize (power of 2) for efficient decoding.
     */
    private fun calculateInSampleSize(
        srcWidth: Int,
        srcHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1

        if (srcHeight > reqHeight || srcWidth > reqWidth) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2

            while (halfHeight / inSampleSize >= reqHeight &&
                   halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}

/**
 * Android implementation of image memory pool.
 */
class AndroidImageMemoryPool(private val maxPoolSize: Int) : ImageMemoryPool {

    private val pool = mutableListOf<ByteArray>()

    override fun acquireBuffer(size: Int): ByteArray? {
        synchronized(pool) {
            // Try to find an existing buffer of sufficient size
            val index = pool.indexOfFirst { it.size >= size }
            if (index >= 0) {
                return pool.removeAt(index)
            }

            // If pool not full, allocate new buffer
            if (pool.size < maxPoolSize) {
                return ByteArray(size)
            }

            return null
        }
    }

    override fun releaseBuffer(buffer: ByteArray) {
        synchronized(pool) {
            if (pool.size < maxPoolSize) {
                pool.add(buffer)
            }
        }
    }

    override fun clear() {
        synchronized(pool) {
            pool.clear()
        }
    }

    override fun getStatistics(): MemoryPoolStatistics {
        synchronized(pool) {
            val totalBuffers = pool.size
            val usedBuffers = 0 // All buffers are available in this implementation
            val availableBuffers = totalBuffers
            val totalMemoryBytes = pool.sumOf { it.size.toLong() }
            val usedMemoryBytes = 0L
            val availableMemoryBytes = totalMemoryBytes

            return MemoryPoolStatistics(
                totalBuffers = totalBuffers,
                usedBuffers = usedBuffers,
                availableBuffers = availableBuffers,
                totalMemoryBytes = totalMemoryBytes,
                usedMemoryBytes = usedMemoryBytes,
                availableMemoryBytes = availableMemoryBytes
            )
        }
    }
}
