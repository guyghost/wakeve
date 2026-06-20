package com.guyghost.wakeve.ml

import com.guyghost.wakeve.models.FaceDetection
import com.guyghost.wakeve.models.PhotoTag

/**
 * iOS implementation of photo recognition using Vision Framework.
 * Fails explicitly until the native Vision bridge is wired.
 */
class IosPhotoRecognitionService : PhotoRecognitionService {
    override suspend fun detectFaces(image: Any?): List<FaceDetection> {
        throw IllegalStateException("iOS photo face detection is not configured")
    }

    override suspend fun tagPhoto(image: Any?): List<PhotoTag> {
        throw IllegalStateException("iOS photo tagging is not configured")
    }
}
