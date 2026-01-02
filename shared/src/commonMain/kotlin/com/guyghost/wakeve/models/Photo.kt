package com.guyghost.wakeve.models

/**
 * Represents a photo in the event context with metadata for organization and search.
 */
data class Photo(
    val id: String,
    val eventId: String,
    val url: String,                 // Cloud storage URL (Google Photos/iCloud)
    val localPath: String?,          // Local file path
    val thumbnailUrl: String?,
    val caption: String?,
    val uploadedAt: String,          // ISO 8601
    val tags: List<PhotoTag>,        // Auto and manual tags
    val faceDetections: List<FaceDetection>, // Face bounding boxes
    val albums: List<String>,        // Album IDs
    val isFavorite: Boolean = false
)

/**
 * Represents a detected face in a photo with bounding box and confidence.
 */
data class FaceDetection(
    val boundingBox: BoundingBox,
    val confidence: Double           // 0.0 - 1.0
)

/**
 * Represents a rectangular region of interest in an image.
 */
data class BoundingBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

/**
 * Represents an auto-suggested or manually added tag for a photo.
 */
data class PhotoTag(
    val tagId: String,
    val label: String,               // "Pizza Party", "Wedding Couple"
    val confidence: Double,          // 0.0 - 1.0
    val category: PhotoCategory,     // PEOPLE, FOOD, DECORATION, LOCATION
    val source: TagSource,           // AUTO or MANUAL
    val suggestedAt: String?,        // ISO timestamp
    val createdBy: String? = null    // User ID for manual tags
)

/**
 * Categories for photo tags based on visual content analysis.
 */
enum class PhotoCategory {
    PEOPLE,
    FOOD,
    DECORATION,
    LOCATION
}

/**
 * Source of the tag - either automatically generated or manually added.
 */
enum class TagSource {
    AUTO,
    MANUAL
}
