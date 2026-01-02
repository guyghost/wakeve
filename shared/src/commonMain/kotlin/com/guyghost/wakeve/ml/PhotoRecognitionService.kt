package com.guyghost.wakeve.ml

import com.guyghost.wakeve.models.FaceDetection
import com.guyghost.wakeve.models.PhotoTag

/**
 * Platform-agnostic interface for photo recognition and auto-tagging.
 * Implementations should process images locally on-device for privacy.
 */
interface PhotoRecognitionService {
    
    /**
     * Detects faces in the given image.
     * 
     * @param image The image to analyze (platform-specific type: Bitmap for Android, UIImage for iOS)
     * @return List of detected faces with bounding boxes and confidence scores
     */
    suspend fun detectFaces(image: Any?): List<FaceDetection>
    
    /**
     * Automatically tags the given image based on visual content analysis.
     * Tags are created from people, food, decoration, and location type.
     * 
     * @param image The image to analyze (platform-specific type: Bitmap for Android, UIImage for iOS)
     * @return List of suggested tags sorted by confidence (highest first)
     */
    suspend fun tagPhoto(image: Any?): List<PhotoTag>
    
    /**
     * Processes a photo completely: detects faces and generates auto-tags.
     * 
     * @param image The image to process (platform-specific type)
     * @return Complete recognition result including faces and tags
     */
    suspend fun processPhoto(image: Any?): PhotoRecognitionResult {
        val faces = detectFaces(image)
        val tags = tagPhoto(image)
        return PhotoRecognitionResult(
            faceDetections = faces,
            suggestedTags = tags
        )
    }
}

/**
 * Result of photo recognition processing containing all detected elements.
 */
data class PhotoRecognitionResult(
    val faceDetections: List<FaceDetection>,
    val suggestedTags: List<PhotoTag>
)
