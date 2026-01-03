package com.guyghost.wakeve.image

import com.guyghost.wakeve.models.ImageQuality
import com.guyghost.wakeve.models.PickedImage

/**
 * Re-exports the image picker models from the models package.
 *
 * This module provides the Functional Core types for image picking operations.
 * All types are pure data classes with no side effects or I/O dependencies.
 *
 * ## Models Provided
 *
 * - [ImageQuality] - Compression quality levels
 * - [PickedImage] - Result of a picked image with metadata
 *
 * ## See Also
 *
 * - [ImagePickerService] - Platform-specific service interface
 * - [IosImagePickerService] - iOS implementation using PHPickerViewController
 */
