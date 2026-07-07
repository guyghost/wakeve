package com.guyghost.wakeve.image

import com.guyghost.wakeve.models.ImagePickerConfig
import com.guyghost.wakeve.models.ImageQuality
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class NoConfiguredImagePickerServiceTest {

    @Test
    fun `pickImage fails when image picker is not configured`() = runTest {
        val result = NoConfiguredImagePickerService.pickImage()

        val error = assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals("Image picker service is not configured", error.message)
    }

    @Test
    fun `pickImageWithCompression fails when image picker is not configured`() = runTest {
        val result = NoConfiguredImagePickerService.pickImageWithCompression(ImageQuality.HIGH)

        val error = assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals("Image picker service is not configured", error.message)
    }

    @Test
    fun `pickImagesWithConfig fails when image picker is not configured`() = runTest {
        val result = NoConfiguredImagePickerService.pickImagesWithConfig(ImagePickerConfig.multipleImages)

        val error = assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals("Image picker service is not configured", error.message)
    }

    @Test
    fun `pickVisualMedia fails when image picker is not configured`() = runTest {
        val result = NoConfiguredImagePickerService.pickVisualMedia(maxItems = 2)

        val error = assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals("Image picker service is not configured", error.message)
    }

    @Test
    fun `availability is false when image picker is not configured`() {
        assertFalse(NoConfiguredImagePickerService.isPhotoPickerAvailable())
    }

    @Test
    fun `cache methods fail when image picker is not configured`() {
        val getLastError = assertFailsWith<IllegalStateException> {
            NoConfiguredImagePickerService.getLastPickedImage()
        }
        val clearError = assertFailsWith<IllegalStateException> {
            NoConfiguredImagePickerService.clearCache()
        }

        assertEquals("Image picker service is not configured", getLastError.message)
        assertEquals("Image picker service is not configured", clearError.message)
    }
}
