package com.guyghost.wakeve.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectionOptions
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabelingOptions
import com.google.mlkit.vision.label.ImageLabel
import com.guyghost.wakeve.models.BoundingBox
import com.guyghost.wakeve.models.FaceDetection as ModelFaceDetection
import com.guyghost.wakeve.models.PhotoCategory
import com.guyghost.wakeve.models.PhotoTag
import com.guyghost.wakeve.models.TagSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android implementation of photo recognition using Google ML Kit Vision.
 * 
 * Features:
 * - Local on-device processing for privacy (no cloud uploads)
 * - Face detection with bounding boxes
 * - Automatic image labeling (tags) with confidence scores
 * 
 * All processing runs locally on device per privacy requirements (photo-105).
 * 
 * @see PhotoRecognitionService
 */
actual class AndroidPhotoRecognitionService(
    private val context: Context
) : PhotoRecognitionService {
    
    private companion object {
        private const val TAG = "PhotoRecognition"
        
        // Confidence threshold for image labeling
        private const val IMAGE_LABELING_CONFIDENCE_THRESHOLD = 0.7f
        
        // Result limits
        private const val MAX_TAGS = 5
    }
    
    private val imageLabeler: ImageLabeler
    private val faceDetector: FaceDetector
    
    init {
        // Initialize ML Kit Image Labeling for general content tags
        val labelOptions = ImageLabelingOptions.Builder()
            .setConfidenceThreshold(IMAGE_LABELING_CONFIDENCE_THRESHOLD)
            .build()
        imageLabeler = ImageLabeling.getClient(labelOptions)
        
        // Initialize ML Kit Face Detection for privacy-preserving face detection
        // No facial recognition - only anonymous bounding boxes
        val faceOptions = FaceDetectionOptions.Builder()
            .setPerformanceMode(FaceDetectionOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetection.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetection.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.15f)
            .build()
        faceDetector = FaceDetection.getClient(faceOptions)
    }
    
    /**
     * Detects faces in the given image using ML Kit Face Detection.
     * All processing is performed locally on-device for privacy.
     * 
     * @param image The image to analyze (accepts Bitmap or null)
     * @return List of detected faces with bounding boxes and confidence scores
     */
    override suspend fun detectFaces(image: Any?): List<ModelFaceDetection> {
        val bitmap = image as? Bitmap ?: return emptyList()
        return detectFacesFromBitmap(bitmap)
    }
    
    /**
     * Android-specific overload for face detection with Bitmap.
     */
    suspend fun detectFacesFromBitmap(imageBitmap: Bitmap): List<ModelFaceDetection> = withContext(Dispatchers.Default) {
        try {
            val inputImage = InputImage.fromBitmap(imageBitmap, 0)
            val task: Task<List<Face>> = faceDetector.process(inputImage)
            val result: List<Face> = awaitTask(task)
            
            result.map { face ->
                val boundingBox = BoundingBox(
                    x = face.boundingBox.left,
                    y = face.boundingBox.top,
                    width = face.boundingBox.width(),
                    height = face.boundingBox.height()
                )
                
                ModelFaceDetection(
                    boundingBox = boundingBox,
                    confidence = DEFAULT_FACE_CONFIDENCE
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Face detection failed", e)
            emptyList()
        }
    }
    
    /**
     * Automatically tags the given image based on visual content analysis.
     * Uses Image Labeling for general tags (people, food, decoration, location).
     * 
     * Tags are created from:
     * - People (faces, groups)
     * - Food (meals, drinks, food items)
     * - Decoration (balloons, flowers, banners)
     * - Location type (indoor/outdoor, venue type)
     * 
     * Top tags with highest confidence are suggested.
     * All processing is performed locally on-device for privacy.
     * 
     * @param image The image to analyze (accepts Bitmap or null)
     * @return List of suggested tags sorted by confidence (highest first), max 5 tags
     */
    override suspend fun tagPhoto(image: Any?): List<PhotoTag> {
        val bitmap = image as? Bitmap ?: return emptyList()
        return tagPhotoFromBitmap(bitmap)
    }
    
    /**
     * Android-specific overload for photo tagging with Bitmap.
     */
    suspend fun tagPhotoFromBitmap(imageBitmap: Bitmap): List<PhotoTag> = withContext(Dispatchers.Default) {
        try {
            val inputImage = InputImage.fromBitmap(imageBitmap, 0)
            val task: Task<List<ImageLabel>> = imageLabeler.process(inputImage)
            val labels: List<ImageLabel> = awaitTask(task)
            
            labels
                .filter { it.confidence >= IMAGE_LABELING_CONFIDENCE_THRESHOLD }
                .sortedByDescending { it.confidence }
                .take(MAX_TAGS)
                .map { label ->
                    val category = mapLabelToCategory(label.text)
                    PhotoTag(
                        tagId = generateTagId(),
                        label = label.text,
                        confidence = label.confidence.toDouble(),
                        category = category,
                        source = TagSource.AUTO,
                        suggestedAt = getCurrentTimestamp()
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Photo tagging failed", e)
            emptyList()
        }
    }
    
    /**
     * Maps an ML Kit image label to a PhotoCategory.
     * 
     * @param label The label text from ML Kit
     * @return The corresponding PhotoCategory
     */
    private fun mapLabelToCategory(label: String): PhotoCategory {
        val lowerLabel = label.lowercase()
        return when {
            // People detection
            lowerLabel.contains("person") || lowerLabel.contains("people") || 
            lowerLabel.contains("human") || lowerLabel.contains("face") ||
            lowerLabel.contains("group") || lowerLabel.contains("crowd") -> PhotoCategory.PEOPLE
            
            // Food detection
            lowerLabel.contains("food") || lowerLabel.contains("meal") || 
            lowerLabel.contains("dinner") || lowerLabel.contains("lunch") ||
            lowerLabel.contains("breakfast") || lowerLabel.contains("cuisine") ||
            lowerLabel.contains("restaurant") || lowerLabel.contains("drink") -> PhotoCategory.FOOD
            
            // Decoration detection
            lowerLabel.contains("decoration") || lowerLabel.contains("balloon") || 
            lowerLabel.contains("flower") || lowerLabel.contains("banner") ||
            lowerLabel.contains("lights") || lowerLabel.contains("party") ||
            lowerLabel.contains("celebration") -> PhotoCategory.DECORATION
            
            // Location detection
            lowerLabel.contains("park") || lowerLabel.contains("beach") || 
            lowerLabel.contains("restaurant") || lowerLabel.contains("venue") ||
            lowerLabel.contains("indoor") || lowerLabel.contains("outdoor") ||
            lowerLabel.contains("building") || lowerLabel.contains("hall") ||
            lowerLabel.contains("garden") -> PhotoCategory.LOCATION
            
            // Default fallback based on common patterns
            else -> PhotoCategory.FOOD
        }
    }
    
    /**
     * Generates a unique tag ID.
     */
    private fun generateTagId(): String {
        return "tag-${java.util.UUID.randomUUID()}"
    }
    
    /**
     * Gets the current timestamp in ISO 8601 format.
     */
    private fun getCurrentTimestamp(): String {
        return java.time.Instant.now().toString()
    }
    
    /**
     * Default confidence for face detection when ML Kit doesn't provide one.
     * ML Kit Face Detection doesn't return confidence scores, so we use a standard value.
     */
    private val DEFAULT_FACE_CONFIDENCE = 0.95
    
    /**
     * Extension function to convert a GMS Task to a suspend function.
     */
    private suspend fun <T> awaitTask(task: Task<T>): T = suspendCancellableCoroutine { continuation ->
        task.addOnSuccessListener { result ->
            continuation.resume(result)
        }.addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
    }
}
