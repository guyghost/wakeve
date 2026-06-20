package com.guyghost.wakeve.ml

/**
 * iOS implementation of memory-optimized image processing.
 *
 * Fails explicitly until a native image processing bridge is wired.
 */
class IosMemoryOptimizedImageProcessor : MemoryOptimizedImageProcessor {
    override suspend fun processImage(
        image: Any,
        config: MemoryOptimizationConfig
    ): MemoryOptimizedResult =
        NoConfiguredMemoryOptimizedImageProcessor.processImage(image, config)

    override suspend fun processImagesInBatches(
        images: List<Any>,
        config: MemoryOptimizationConfig
    ): List<MemoryOptimizedResult> =
        NoConfiguredMemoryOptimizedImageProcessor.processImagesInBatches(images, config)

    override fun getCurrentMemoryUsageMB(): Double =
        NoConfiguredMemoryOptimizedImageProcessor.getCurrentMemoryUsageMB()

    override fun shouldThrottleProcessing(config: MemoryOptimizationConfig): Boolean =
        NoConfiguredMemoryOptimizedImageProcessor.shouldThrottleProcessing(config)

    override suspend fun suggestGarbageCollection() {
        NoConfiguredMemoryOptimizedImageProcessor.suggestGarbageCollection()
    }
}

class IosImageMemoryPool(private val maxPoolSize: Int) : ImageMemoryPool {
    override fun acquireBuffer(size: Int): ByteArray? =
        NoConfiguredImageMemoryPool.acquireBuffer(size)

    override fun releaseBuffer(buffer: ByteArray) {
        NoConfiguredImageMemoryPool.releaseBuffer(buffer)
    }

    override fun clear() {
        NoConfiguredImageMemoryPool.clear()
    }

    override fun getStatistics(): MemoryPoolStatistics =
        NoConfiguredImageMemoryPool.getStatistics()
}
