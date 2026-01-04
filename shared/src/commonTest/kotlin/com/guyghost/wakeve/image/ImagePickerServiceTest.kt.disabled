package com.guyghost.wakeve.image

import com.guyghost.wakeve.models.ImageBatchResult
import com.guyghost.wakeve.models.ImagePickerConfig
import com.guyghost.wakeve.models.ImageQuality
import com.guyghost.wakeve.models.MediaType
import com.guyghost.wakeve.models.PickedImage
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for image picker models and service contract.
 * 
 * These tests validate:
 * - Model creation and properties
 * - Image quality and media type enums
 * - Batch result calculations
 * - PickedImage convenience methods
 * 
 * Note: Platform-specific tests (AndroidImagePickerServiceTest) should be
 * created in androidTest for instrumentation tests that require the Android
 * runtime.
 */
class ImagePickerServiceTest {
    
    // ===== Model Creation Tests =====
    
    @Test
    fun `PickedImage should have correct default values`() {
        val image = PickedImage(
            uri = "content://media/external/images/1",
            mediaType = MediaType.IMAGE,
            width = 1920,
            height = 1080,
            sizeBytes = 2_500_000,
            mimeType = "image/jpeg"
        )
        
        assertNull(image.compressionQuality)
        assertFalse(image.isCompressed)
    }
    
    @Test
    fun `PickedImage with compression quality should be marked as compressed`() {
        val image = PickedImage(
            uri = "content://media/external/images/1",
            mediaType = MediaType.IMAGE,
            width = 1920,
            height = 1080,
            sizeBytes = 1_000_000,
            mimeType = "image/jpeg",
            compressionQuality = ImageQuality.MEDIUM
        )
        
        assertNotNull(image.compressionQuality)
        assertEquals(ImageQuality.MEDIUM, image.compressionQuality)
        assertTrue(image.isCompressed)
    }
    
    @Test
    fun `PickedImage formattedSize should format bytes correctly`() {
        // Test various size formats
        val image1Kb = createTestImage(sizeBytes = 500)
        assertEquals("500 B", image1Kb.formattedSize)
        
        val image1Mb = createTestImage(sizeBytes = 1024 * 500)
        assertEquals("500 KB", image1Mb.formattedSize)
        
        val image10Mb = createTestImage(sizeBytes = 1024 * 1024 * 10)
        assertEquals("10 MB", image10Mb.formattedSize)
        
        val image2Gb = createTestImage(sizeBytes = 1024L * 1024 * 1024 * 2)
        assertTrue(image2Gb.formattedSize.contains("GB"))
    }
    
    @Test
    fun `PickedImage dimensions should return formatted string`() {
        val image = createTestImage(width = 1920, height = 1080)
        
        assertEquals("1920×1080", image.dimensions)
    }
    
    @Test
    fun `PickedImage dimensions should return null when width or height is null`() {
        val imageWidthNull = createTestImage(width = null, height = 1080)
        val imageHeightNull = createTestImage(width = 1920, height = null)
        
        assertNull(imageWidthNull.dimensions)
        assertNull(imageHeightNull.dimensions)
    }
    
    @Test
    fun `PickedImage aspectRatio should calculate correctly`() {
        val image16x9 = createTestImage(width = 1920, height = 1080)
        assertEquals(1.777f, image16x9.aspectRatio, 0.01f)
        
        val image1x1 = createTestImage(width = 1000, height = 1000)
        assertEquals(1.0f, image1x1.aspectRatio, 0.01f)
        
        val imageVertical = createTestImage(width = 1080, height = 1920)
        assertEquals(0.562f, imageVertical.aspectRatio, 0.01f)
    }
    
    @Test
    fun `PickedImage aspectRatio should return null when height is zero or null`() {
        val imageZeroHeight = createTestImage(width = 1920, height = 0)
        val imageNullHeight = createTestImage(width = 1920, height = null)
        
        assertNull(imageZeroHeight.aspectRatio)
        assertNull(imageNullHeight.aspectRatio)
    }
    
    // ===== MediaType Tests =====
    
    @Test
    fun `MediaType should have correct string values`() {
        assertEquals("image", MediaType.IMAGE.type)
        assertEquals("video", MediaType.VIDEO.type)
        assertEquals("document", MediaType.DOCUMENT.type)
    }
    
    // ===== ImageQuality Tests =====
    
    @Test
    fun `ImageQuality should have correct percentage values`() {
        assertEquals(90, ImageQuality.HIGH.quality)
        assertEquals(75, ImageQuality.MEDIUM.quality)
        assertEquals(50, ImageQuality.LOW.quality)
    }
    
    // ===== ImageBatchResult Tests =====
    
    @Test
    fun `ImageBatchResult should calculate total size correctly`() {
        val images = listOf(
            createTestImage(sizeBytes = 1000),
            createTestImage(sizeBytes = 2000),
            createTestImage(sizeBytes = 3000)
        )
        val config = ImagePickerConfig(maxSelectionLimit = 5)
        
        val batch = ImageBatchResult(images = images, config = config)
        
        assertEquals(6000, batch.totalSizeBytes)
        assertEquals("6 KB", batch.formattedTotalSize)
    }
    
    @Test
    fun `ImageBatchResult isEmpty should return true for empty list`() {
        val emptyBatch = ImageBatchResult(
            images = emptyList(),
            config = ImagePickerConfig()
        )
        
        assertTrue(emptyBatch.isEmpty)
        assertEquals(0, emptyBatch.count)
    }
    
    @Test
    fun `ImageBatchResult should return correct count`() {
        val batch = ImageBatchResult(
            images = listOf(
                createTestImage(),
                createTestImage()
            ),
            config = ImagePickerConfig()
        )
        
        assertEquals(2, batch.count)
        assertFalse(batch.isEmpty)
    }
    
    // ===== ImagePickerConfig Tests =====
    
    @Test
    fun `ImagePickerConfig defaults should be correct`() {
        val defaultConfig = ImagePickerConfig()
        
        assertEquals(5, defaultConfig.maxSelectionLimit)
        assertEquals(listOf(MediaType.IMAGE), defaultConfig.allowedMediaTypes)
        assertFalse(defaultConfig.enableCompression)
        assertEquals(ImageQuality.MEDIUM, defaultConfig.defaultCompressionQuality)
    }
    
    @Test
    fun `ImagePickerConfig singleImage should have limit of 1`() {
        val singleImageConfig = ImagePickerConfig.singleImage
        
        assertEquals(1, singleImageConfig.maxSelectionLimit)
        assertEquals(listOf(MediaType.IMAGE), singleImageConfig.allowedMediaTypes)
    }
    
    @Test
    fun `ImagePickerConfig compressedImage should have compression enabled`() {
        val compressedConfig = ImagePickerConfig.compressedImage
        
        assertTrue(compressedConfig.enableCompression)
        assertEquals(ImageQuality.MEDIUM, compressedConfig.defaultCompressionQuality)
    }
    
    // ===== Exception Tests =====
    
    @Test
    fun `ImagePickerCancelledException should have correct message`() {
        val exception = ImagePickerCancelledException()
        assertEquals("Image picking was cancelled by the user", exception.message)
    }
    
    @Test
    fun `ImagePickerPermissionDeniedException should include permission name`() {
        val exception = ImagePickerPermissionDeniedException("READ_EXTERNAL_STORAGE")
        assertEquals("Permission denied: READ_EXTERNAL_STORAGE", exception.message)
    }
    
    @Test
    fun `ImagePickerInvalidImageException should include reason`() {
        val exception = ImagePickerInvalidImageException("Failed to decode image")
        assertEquals("Invalid image: Failed to decode image", exception.message)
    }
    
    // ===== PickedImage Companion Tests =====
    
    @Test
    fun `PickedImage fromResult should create correctly`() {
        val result = com.guyghost.wakeve.models.ImagePickerResult(
            uri = "content://test/image/1",
            mediaType = MediaType.IMAGE,
            sizeBytes = 5000,
            width = 100,
            height = 200,
            mimeType = "image/png"
        )
        
        val pickedImage = PickedImage.fromResult(result)
        
        assertEquals(result.uri, pickedImage.uri)
        assertEquals(result.mediaType, pickedImage.mediaType)
        assertEquals(result.sizeBytes, pickedImage.sizeBytes)
        assertEquals(result.width, pickedImage.width)
        assertEquals(result.height, pickedImage.height)
        assertEquals(result.mimeType, pickedImage.mimeType)
    }
    
    // ===== Helper Functions =====
    
    private fun createTestImage(
        uri: String = "content://test/image/1",
        mediaType: MediaType = MediaType.IMAGE,
        width: Int? = 1920,
        height: Int? = 1080,
        sizeBytes: Long = 1_000_000,
        mimeType: String = "image/jpeg",
        compressionQuality: ImageQuality? = null
    ): PickedImage {
        return PickedImage(
            uri = uri,
            mediaType = mediaType,
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            mimeType = mimeType,
            compressionQuality = compressionQuality
        )
    }
}
