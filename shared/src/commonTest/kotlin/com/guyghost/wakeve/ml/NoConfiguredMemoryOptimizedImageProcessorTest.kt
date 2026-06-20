package com.guyghost.wakeve.ml

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NoConfiguredMemoryOptimizedImageProcessorTest {

    @Test
    fun `processImage fails when memory optimized image processor is not configured`() = runTest {
        val error = assertFailsWith<IllegalStateException> {
            NoConfiguredMemoryOptimizedImageProcessor.processImage(
                image = ByteArray(8),
                config = MemoryOptimizationConfig.BALANCED
            )
        }

        assertEquals("Memory-optimized image processor is not configured", error.message)
    }

    @Test
    fun `processImagesInBatches fails when memory optimized image processor is not configured`() = runTest {
        val error = assertFailsWith<IllegalStateException> {
            NoConfiguredMemoryOptimizedImageProcessor.processImagesInBatches(
                images = listOf(ByteArray(8)),
                config = MemoryOptimizationConfig.BALANCED
            )
        }

        assertEquals("Memory-optimized image processor is not configured", error.message)
    }

    @Test
    fun `memory probes fail when memory optimized image processor is not configured`() {
        val usageError = assertFailsWith<IllegalStateException> {
            NoConfiguredMemoryOptimizedImageProcessor.getCurrentMemoryUsageMB()
        }
        val throttleError = assertFailsWith<IllegalStateException> {
            NoConfiguredMemoryOptimizedImageProcessor.shouldThrottleProcessing(MemoryOptimizationConfig.BALANCED)
        }

        assertEquals("Memory-optimized image processor is not configured", usageError.message)
        assertEquals("Memory-optimized image processor is not configured", throttleError.message)
    }

    @Test
    fun `suggestGarbageCollection fails when memory optimized image processor is not configured`() = runTest {
        val error = assertFailsWith<IllegalStateException> {
            NoConfiguredMemoryOptimizedImageProcessor.suggestGarbageCollection()
        }

        assertEquals("Memory-optimized image processor is not configured", error.message)
    }

    @Test
    fun `image memory pool methods fail when pool is not configured`() {
        val acquireError = assertFailsWith<IllegalStateException> {
            NoConfiguredImageMemoryPool.acquireBuffer(16)
        }
        val releaseError = assertFailsWith<IllegalStateException> {
            NoConfiguredImageMemoryPool.releaseBuffer(ByteArray(16))
        }
        val clearError = assertFailsWith<IllegalStateException> {
            NoConfiguredImageMemoryPool.clear()
        }
        val statisticsError = assertFailsWith<IllegalStateException> {
            NoConfiguredImageMemoryPool.getStatistics()
        }

        assertEquals("Image memory pool is not configured", acquireError.message)
        assertEquals("Image memory pool is not configured", releaseError.message)
        assertEquals("Image memory pool is not configured", clearError.message)
        assertEquals("Image memory pool is not configured", statisticsError.message)
    }
}
