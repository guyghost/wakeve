package com.guyghost.wakeve.image

import com.guyghost.wakeve.models.ImageBatchResult
import com.guyghost.wakeve.models.ImagePickerConfig
import com.guyghost.wakeve.models.ImageQuality
import com.guyghost.wakeve.models.MediaType
import com.guyghost.wakeve.models.PickedImage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for iOS Image Picker Service.
 *
 * These tests verify the behavior of the image picker service interface
 * and its implementation. Note that some tests may require running on
 * an actual iOS simulator or device for full functionality.
 *
 * ## FC&IS Architecture Compliance
 *
 * - Tests focus on the **Functional Core** types (models)
 * - Platform-specific implementation tests verify the **Imperative Shell**
 * - No tests include actual I/O or side effects
 *
 * ## Test Categories
 *
 * 1. **Model Tests** - Pure data class behavior
 * 2. **Service Interface Tests** - Interface contract verification
 * 3. **Configuration Tests** - Picker configuration validation
 */
class IosImagePickerServiceTest {

    // MARK: - Model Tests

    /**
     * Tests that PickedImage model properties are correctly accessible.
     */
    @Test
    fun `PickedImage properties are accessible`() {
        val image = PickedImage(
            uri = "ph://test-identifier",
            mediaType = MediaType.IMAGE,
            width = 1920,
            height = 1080,
            sizeBytes = 2048000,
            mimeType = "image/jpeg"
        )

        assertTrue(image.uri == "ph://test-identifier")
        assertTrue(image.mediaType == MediaType.IMAGE)
        assertTrue(image.width == 1920)
        assertTrue(image.height == 1080)
        assertTrue(image.sizeBytes == 2048000L)
        assertTrue(image.mimeType == "image/jpeg")
    }

    /**
     * Tests that PickedImage can be created with optional compression quality.
     */
    @Test
    fun `PickedImage supports optional compression quality`() {
        val uncompressed = PickedImage(
            uri = "ph://test",
            mediaType = MediaType.IMAGE,
            width = 100,
            height = 100,
            sizeBytes = 1000,
            mimeType = "image/jpeg"
        )

        val compressed = uncompressed.copy(
            compressionQuality = ImageQuality.MEDIUM
        )

        assertNull(uncompressed.compressionQuality)
        assertNotNull(compressed.compressionQuality)
        assertTrue(compressed.compressionQuality == ImageQuality.MEDIUM)
    }

    /**
     * Tests ImageQuality enum values.
     */
    @Test
    fun `ImageQuality enum has expected values`() {
        assertTrue(ImageQuality.HIGH.quality == 90)
        assertTrue(ImageQuality.MEDIUM.quality == 75)
        assertTrue(ImageQuality.LOW.quality == 50)
    }

    /**
     * Tests PickedImage convenience methods.
     */
    @Test
    fun `PickedImage has correct convenience properties`() {
        val jpegImage = PickedImage(
            uri = "ph://test",
            mediaType = MediaType.IMAGE,
            width = 1920,
            height = 1080,
            sizeBytes = 2048000,
            mimeType = "image/jpeg"
        )

        val pngImage = jpegImage.copy(mimeType = "image/png")

        assertTrue(jpegImage.isCompressed == false)
        assertTrue(jpegImage.formattedSize == "1 MB")
        assertTrue(jpegImage.dimensions == "1920×1080")
        assertTrue(jpegImage.aspectRatio == 1920f / 1080f)
        assertTrue(pngImage.aspectRatio == 1920f / 1080f)
    }

    // MARK: - Configuration Tests

    /**
     * Tests ImagePickerConfig default values.
     */
    @Test
    fun `ImagePickerConfig has correct defaults`() {
        val config = ImagePickerConfig()

        assertTrue(config.maxSelectionLimit == 5)
        assertTrue(config.allowedMediaTypes == listOf(MediaType.IMAGE))
        assertFalse(config.enableCompression)
        assertTrue(config.defaultCompressionQuality == ImageQuality.MEDIUM)
    }

    /**
     * Tests ImagePickerConfig preset configurations.
     */
    @Test
    fun `ImagePickerConfig presets are correct`() {
        // Single image preset
        val singleConfig = ImagePickerConfig.singleImage
        assertTrue(singleConfig.maxSelectionLimit == 1)
        assertTrue(singleConfig.enableCompression == false)

        // Multiple images preset
        val multipleConfig = ImagePickerConfig.multipleImages
        assertTrue(multipleConfig.maxSelectionLimit == 5)
        assertTrue(multipleConfig.enableCompression == false)

        // Compressed image preset
        val compressedConfig = ImagePickerConfig.compressedImage
        assertTrue(compressedConfig.maxSelectionLimit == 1)
        assertTrue(compressedConfig.enableCompression == true)
        assertTrue(compressedConfig.defaultCompressionQuality == ImageQuality.MEDIUM)
    }

    /**
     * Tests ImageBatchResult calculated properties.
     */
    @Test
    fun `ImageBatchResult calculates correct totals`() {
        val images = listOf(
            PickedImage(
                uri = "ph://test1",
                mediaType = MediaType.IMAGE,
                width = 100,
                height = 100,
                sizeBytes = 1000,
                mimeType = "image/jpeg"
            ),
            PickedImage(
                uri = "ph://test2",
                mediaType = MediaType.IMAGE,
                width = 200,
                height = 200,
                sizeBytes = 2000,
                mimeType = "image/jpeg"
            )
        )

        val config = ImagePickerConfig.multipleImages
        val batch = ImageBatchResult(images = images, config = config)

        assertTrue(batch.count == 2)
        assertFalse(batch.isEmpty)
        assertTrue(batch.totalSizeBytes == 3000L)
        assertTrue(batch.formattedTotalSize == "2 KB")
    }

    /**
     * Tests ImageBatchResult empty state.
     */
    @Test
    fun `ImageBatchResult handles empty state`() {
        val emptyBatch = ImageBatchResult(
            images = emptyList(),
            config = ImagePickerConfig()
        )

        assertTrue(emptyBatch.isEmpty)
        assertTrue(emptyBatch.count == 0)
        assertTrue(emptyBatch.totalSizeBytes == 0L)
    }

    // MARK: - Service Factory Tests

    /**
     * Tests that IosImagePickerFactory creates service instances.
     */
    @Test
    fun `IosImagePickerFactory creates service`() {
        val factory = com.guyghost.wakeve.di.IosImagePickerFactory
        val service = factory.createPickerService()

        assertNotNull(service)
        assertTrue(service is ImagePickerService)
    }

    /**
     * Tests that the service interface methods are callable (signatures only).
     */
    @Test
    fun `ImagePickerService has correct method signatures`() = runTest {
        val service = IosImagePickerService()

        // These are signature tests - actual behavior requires iOS environment
        assertNotNull(service)
        assertTrue(service.isPhotoPickerAvailable())
    }

    // MARK: - MediaType Tests

    /**
     * Tests MediaType enum values.
     */
    @Test
    fun `MediaType enum has correct values`() {
        assertTrue(MediaType.IMAGE.type == "image")
        assertTrue(MediaType.VIDEO.type == "video")
        assertTrue(MediaType.DOCUMENT.type == "document")
    }
}
