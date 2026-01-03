package com.guyghost.wakeve.ml

import com.guyghost.wakeve.models.FaceDetection
import com.guyghost.wakeve.models.PhotoTag

/**
 * iOS implementation of photo recognition using Vision Framework.
 * Simplified stub implementation due to Kotlin/Native iOS interop limitations.
 */
class IosPhotoRecognitionService : PhotoRecognitionService {
    override suspend fun detectFaces(image: Any?): List<FaceDetection> = emptyList()
    override suspend fun tagPhoto(image: Any?): List<PhotoTag> = emptyList()
}
