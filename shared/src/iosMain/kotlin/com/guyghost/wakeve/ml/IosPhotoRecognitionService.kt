package com.guyghost.wakeve.ml

import com.guyghost.wakeve.models.FaceDetection
import com.guyghost.wakeve.models.BoundingBox
import com.guyghost.wakeve.models.PhotoTag
import com.guyghost.wakeve.models.TagSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRect
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSUUID
import platform.Vision.*
import platform.UIKit.UIImage
import platform.Foundation.NSDate

/**
 * iOS implementation of photo recognition using Vision Framework.
 *
 * Uses:
 * - VNDetectFaceRectanglesRequest for face detection
 * - VNClassifyImageRequest for general image classification
 * - VNRecognizeAnimalsRequest for animal detection
 *
 * All processing runs locally on-device for privacy compliance.
 */
actual class IosPhotoRecognitionService : PhotoRecognitionService {

    private companion object {
        private const val TAG = "IosPhotoRecognition"
        private const val CONFIDENCE_THRESHOLD = 0.7
        private const val MAX_TAGS = 5
        private const val MAX_FACES = 10
    }

    // Vision request handlers
    private val faceDetectionRequest: VNDetectFaceRectanglesRequest = VNDetectFaceRectanglesRequest()
    private val classificationRequest: VNClassifyImageRequest = VNClassifyImageRequest()
    private val animalDetectionRequest: VNRecognizeAnimalsRequest = VNRecognizeAnimalsRequest()

    // Cache for image orientation to avoid repeated CGImage conversions
    private val imageOrientationCache = mutableMapOf<String, Int>()

    /**
     * Detects faces in the given image using Vision Framework.
     *
     * Uses VNDetectFaceRectanglesRequest with configurable confidence threshold.
     *
     * @param image The UIImage to analyze for faces
     * @return List of detected faces with bounding boxes
     */
    override suspend fun detectFaces(image: Any?): List<FaceDetection> = withContext(Dispatchers.Default) {
        val uiImage = image as? UIImage ?: return@withContext emptyList()

        return@withContext try {
            // Convert UIImage to VNImageRequestHandler
            val requestHandler = VNImageRequestHandler(uiImage, uiImage.imageOrientation)

            // Configure the face detection request
            val request = VNDetectFaceRectanglesRequest { request, error in
                // Results will be processed in the completion handler
            }

            request.maximumObservations = MAX_FACES.toLong()
            request.revision = if (isVisionFrameworkAvailable()) VNDetectFaceRectanglesRequestRevision3 else 2

            // Perform the request synchronously
            var error: NSError? = null
            val success = requestHandler.perform(listOf(request), error)

            if (!success) {
                println("$TAG: Face detection failed: ${error?.localizedDescription}")
                return@withContext emptyList()
            }

            // Extract results
            request.results?.mapNotNull { observation ->
                val faceObservation = observation as? VNFaceObservation ?: return@mapNotNull null

                // Check confidence threshold
                if (faceObservation.confidence < CONFIDENCE_THRESHOLD) {
                    return@mapNotNull null
                }

                // Convert bounding box from normalized coordinates (0-1) to pixel coordinates
                val boundingBox = faceObservation.boundingBox

                // Create BoundingBox model
                val box = BoundingBox(
                    x = boundingBox.origin.x * uiImage.size.width.toDouble(),
                    y = boundingBox.origin.y * uiImage.size.height.toDouble(),
                    width = boundingBox.size.width * uiImage.size.width.toDouble(),
                    height = boundingBox.size.height * uiImage.size.height.toDouble()
                )

                FaceDetection(
                    boundingBox = box,
                    confidence = faceObservation.confidence.toDouble()
                )
            } ?: emptyList()

        } catch (e: Exception) {
            println("$TAG: Error detecting faces: ${e.message}")
            emptyList()
        }
    }

    /**
     * Automatically tags the given image based on visual content analysis.
     *
     * Uses multiple Vision requests:
     * - VNClassifyImageRequest for general image classification
     * - VNRecognizeAnimalsRequest for animal detection
     *
     * @param image The UIImage to analyze and tag
     * @return List of suggested tags sorted by confidence
     */
    override suspend fun tagPhoto(image: Any?): List<PhotoTag> = withContext(Dispatchers.Default) {
        val uiImage = image as? UIImage ?: return@withContext emptyList()

        return@withContext try {
            // Convert UIImage to VNImageRequestHandler
            val requestHandler = VNImageRequestHandler(uiImage, uiImage.imageOrientation)

            // Prepare multiple requests
            val classificationRequest = VNClassifyImageRequest()
            classificationRequest.maximumResultsCount = MAX_TAGS.toLong()
            classificationRequest.revision = if (isVisionFrameworkAvailable()) VNClassifyImageRequestRevision3 else 1

            val animalRequest = VNRecognizeAnimalsRequest()
            animalRequest.maximumObservations = 3.toLong()
            animalRequest.revision = if (isVisionFrameworkAvailable()) VNRecognizeAnimalsRequestRevision3 else 1

            // Perform requests
            var error: NSError? = null
            val success = requestHandler.perform(listOf(classificationRequest, animalRequest), error)

            if (!success) {
                println("$TAG: Photo tagging failed: ${error?.localizedDescription}")
                return@withContext emptyList()
            }

            // Collect and combine tags from both requests
            val tags = mutableListOf<PhotoTag>()

            // Get current timestamp for suggestedAt
            val suggestedAt = NSDate().description

            // Process classification results
            classificationRequest.results?.forEach { classification in
                val label = classification.identifier.split(",").firstOrNull()?.trim() ?: return@forEach
                val confidence = classification.confidence.toDouble()

                if (confidence >= CONFIDENCE_THRESHOLD) {
                    tags.add(PhotoTag(
                        tagId = NSUUID().UUIDString,
                        label = label,
                        confidence = confidence,
                        category = mapClassificationToCategory(label),
                        source = TagSource.AUTO,
                        suggestedAt = suggestedAt,
                        createdBy = null
                    ))
                }
            }

            // Process animal detection results
            animalRequest.results?.forEach { animalObservation ->
                animalObservation.labels?.forEach { label in
                    val confidence = label.confidence.toDouble()

                    if (confidence >= CONFIDENCE_THRESHOLD) {
                        tags.add(PhotoTag(
                            tagId = NSUUID().UUIDString,
                            label = label.identifier,
                            confidence = confidence,
                            category = PhotoTag.PhotoCategory.PEOPLE,  // Group animals with people
                            source = TagSource.AUTO,
                            suggestedAt = suggestedAt,
                            createdBy = null
                        ))
                    }
                }
            }

            // Sort by confidence and limit to MAX_TAGS
            tags.sortedByDescending { it.confidence }.take(MAX_TAGS)

        } catch (e: Exception) {
            println("$TAG: Error tagging photo: ${e.message}")
            emptyList()
        }
    }

    /**
     * Maps Vision Framework classification labels to PhotoTag categories.
     */
    private fun mapClassificationToCategory(label: String): PhotoTag.PhotoCategory {
        val lowerLabel = label.lowercase()

        return when {
            lowerLabel.contains("person") || lowerLabel.contains("people") || lowerLabel.contains("human") || lowerLabel.contains("face") -> PhotoTag.PhotoCategory.PEOPLE
            lowerLabel.contains("food") || lowerLabel.contains("meal") || lowerLabel.contains("drink") || lowerLabel.contains("beverage") || lowerLabel.contains("pizza") || lowerLabel.contains("cake") -> PhotoTag.PhotoCategory.FOOD
            lowerLabel.contains("decoration") || lowerLabel.contains("party") || lowerLabel.contains("celebration") || lowerLabel.contains("balloon") || lowerLabel.contains("flower") -> PhotoTag.PhotoCategory.DECORATION
            lowerLabel.contains("outdoor") || lowerLabel.contains("nature") || lowerLabel.contains("landscape") || lowerLabel.contains("mountain") || lowerLabel.contains("beach") || lowerLabel.contains("park") -> PhotoTag.PhotoCategory.LOCATION
            lowerLabel.contains("animal") || lowerLabel.contains("pet") || lowerLabel.contains("dog") || lowerLabel.contains("cat") -> PhotoTag.PhotoCategory.PEOPLE  // Animals fall under PEOPLE category for grouping with people
            else -> PhotoTag.PhotoCategory.LOCATION  // Default to location for other items
        }
    }

    /**
     * Checks if Vision Framework with required features is available.
     */
    private fun isVisionFrameworkAvailable(): Boolean {
        return isVisionFrameworkAvailable_v3()
    }
}

/**
 * Platform function to check Vision Framework availability.
 */
@Suppress("EXTERNAL_API")
private fun isVisionFrameworkAvailable_v3(): Boolean {
    return platform.Version.SDK_INT >= 13 // iOS 13+ for Vision Framework v3 features
}

/**
 * Extension function to get image orientation from cache or compute it.
 */
private fun UIImage.getImageOrientation(image: UIImage): Int {
    val imageKey = "${image.size.width}_${image.size.height}"
    return imageOrientationCache.getOrPut(imageKey) {
        image.imageOrientation.rawValue
    }
}
