package com.guyghost.wakeve.services

import com.guyghost.wakeve.models.Album
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.Photo
import com.guyghost.wakeve.models.PhotoCategory
import com.guyghost.wakeve.models.PhotoTag
import com.guyghost.wakeve.models.TagSource
import com.guyghost.wakeve.repository.AlbumRepository
import com.guyghost.wakeve.repository.PhotoRepository
import com.guyghost.wakeve.ml.PhotoRecognitionService as PlatformPhotoRecognitionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.random.Random

/**
 * Service for photo recognition, auto-tagging, and smart album management.
 * 
 * Features:
 * - Face detection and auto-tagging using platform-specific ML services
 * - Smart album creation based on events and visual content
 * - Photo search with relevance scoring
 * - Visual similarity search
 * - All processing runs locally on-device for privacy (photo-105)
 * 
 * @property androidPhotoRecognition Android-specific photo recognition service
 * @property iosPhotoRecognition iOS-specific photo recognition service
 * @property photoRepository Repository for photo data access
 * @property albumRepository Repository for album management
 */
class PhotoRecognitionService(
    private val androidPhotoRecognition: PlatformPhotoRecognitionService?,
    private val iosPhotoRecognition: PlatformPhotoRecognitionService?,
    private val photoRepository: PhotoRepository,
    private val albumRepository: AlbumRepository
) {
    
    companion object {
        private const val SIMILARITY_THRESHOLD = 0.7
        private const val MAX_SIMILAR_PHOTOS = 10
        private const val MAX_SUGGESTED_ALBUMS = 5
        
        /**
         * Extracts a name from an event title.
         * Example: "Anniversaire de Jean" -> "Jean"
         */
        private fun extractName(title: String): String {
            return title
                .removePrefix("Anniversaire de ")
                .removePrefix("Mariage de ")
                .removePrefix("Soirée chez ")
                .removePrefix("Soirée de ")
                .trim()
                .ifEmpty { "l'événement" }
        }
        
        /**
         * Gets the current timestamp in ISO 8601 format.
         */
        private fun getCurrentTimestamp(): String {
            return Clock.System.now().toString()
        }
        
        /**
         * Gets the current season name in French.
         */
        private fun getCurrentSeason(): String {
            val month = Clock.System.now().toEpochMilliseconds()
            // Approximate month from timestamp (milliseconds since epoch)
            // 30 days per month approx, 86400000 ms per day
            val daysSinceEpoch = month / 86400000
            val monthNumber = ((daysSinceEpoch / 30) % 12).toInt() + 1
            return when (monthNumber) {
                in 3..5 -> "Printemps"
                in 6..8 -> "Été"
                in 9..11 -> "Automne"
                else -> "Hiver"
            }
        }
        
        /**
         * Gets the current year.
         */
        private fun getCurrentYear(): Int {
            val timestamp = Clock.System.now().toEpochMilliseconds()
            // Approximate year from timestamp
            val daysSinceEpoch = timestamp / 86400000
            return 1970 + (daysSinceEpoch / 365).toInt()
        }
        
        /**
         * Gets the current month number (1-12).
         */
        private fun getCurrentMonth(): Int {
            val timestamp = Clock.System.now().toEpochMilliseconds()
            val daysSinceEpoch = timestamp / 86400000
            return ((daysSinceEpoch / 30) % 12).toInt() + 1
        }
        
        /**
         * Generates a unique album ID.
         */
        private fun generateAlbumId(): String {
            val timestamp = Clock.System.now().toEpochMilliseconds()
            val random = Random.nextInt(1000000)
            return "album-$timestamp-$random"
        }
    }
    
    /**
     * Processes a photo: detects faces, generates tags, and suggests albums.
     * All processing is performed locally on-device for privacy.
     *
     * @param photoId The photo ID to process
     * @param imageBytes The image data as bytes
     * @return The recognition result with detected elements and album suggestions
     */
    suspend fun processPhoto(
        photoId: String,
        imageBytes: ByteArray
    ): PhotoRecognitionResult = withContext(Dispatchers.Default) {
        val platform = getPlatform()
        
        // Detect faces based on platform
        val faces = when (platform) {
            Platform.ANDROID -> {
                // On Android, we need to convert bytes to Bitmap
                val bitmap = convertBytesToAndroidBitmap(imageBytes)
                if (bitmap != null) {
                    androidPhotoRecognition?.detectFaces(bitmap) ?: emptyList()
                } else {
                    // For testing/mock scenarios, pass bytes directly
                    androidPhotoRecognition?.detectFaces(imageBytes) ?: emptyList()
                }
            }
            Platform.IOS -> {
                // On iOS, bytes can be passed directly
                iosPhotoRecognition?.detectFaces(imageBytes) ?: emptyList()
            }
            Platform.UNKNOWN -> emptyList()
        }
        
        // Tag photo based on platform
        val tags = when (platform) {
            Platform.ANDROID -> {
                val bitmap = convertBytesToAndroidBitmap(imageBytes)
                if (bitmap != null) {
                    androidPhotoRecognition?.tagPhoto(bitmap) ?: emptyList()
                } else {
                    // For testing/mock scenarios, pass bytes directly
                    androidPhotoRecognition?.tagPhoto(imageBytes) ?: emptyList()
                }
            }
            Platform.IOS -> {
                iosPhotoRecognition?.tagPhoto(imageBytes) ?: emptyList()
            }
            Platform.UNKNOWN -> emptyList()
        }
        
        // Update photo with detections and tags
        photoRepository.updatePhotoWithTags(photoId, faces, tags)
        
        // Generate album suggestions based on tags
        val suggestedAlbums = suggestAlbums(photoId, tags)
        
        PhotoRecognitionResult(
            photoId = photoId,
            facesDetected = faces.size,
            tagsSuggested = tags.size,
            suggestedAlbums = suggestedAlbums
        )
    }
    
    /**
     * Creates an auto-generated album for an event.
     * The album name is derived from the event type and title.
     *
     * @param eventId The event ID
     * @return The created album
     * @throws IllegalStateException if no photos exist for the event
     */
    suspend fun createAutoAlbum(eventId: String): Album = withContext(Dispatchers.Default) {
        val eventPhotos = photoRepository.getPhotosByEvent(eventId)
        
        if (eventPhotos.isEmpty()) {
            throw IllegalStateException("No photos for event $eventId")
        }
        
        // Get event details for album name - simplified, in real implementation
        // would fetch from EventRepository
        val albumName = generateAlbumName(eventId, eventPhotos)
        
        // Select cover photo (user-selected favorite or first photo)
        val coverPhotoId = eventPhotos
            .firstOrNull { it.isFavorite }
            ?.id
            ?: eventPhotos.first().id
        
        val album = Album(
            id = generateAlbumId(),
            eventId = eventId,
            name = albumName,
            coverPhotoId = coverPhotoId,
            photoIds = eventPhotos.map { it.id },
            createdAt = getCurrentTimestamp(),
            isAutoGenerated = true,
            updatedAt = getCurrentTimestamp()
        )
        
        albumRepository.createAlbum(album)
        
        // Also add photos to the album in the photo repository
        eventPhotos.forEach { photo ->
            photoRepository.addPhotoToAlbum(photo.id, album.id)
        }
        
        album
    }
    
    /**
     * Searches photos by text query with relevance scoring.
     *
     * @param query The search query
     * @param filters Optional filters for the search
     * @return List of search results sorted by relevance score
     */
    suspend fun searchPhotos(
        query: String,
        filters: PhotoSearchFilters? = null
    ): List<PhotoSearchResult> = withContext(Dispatchers.Default) {
        if (query.isBlank()) {
            return@withContext emptyList()
        }
        
        // Text search on tags and captions
        val textResults = photoRepository.searchByQuery(query)
        
        // Apply filters if provided
        val filteredResults = applyFilters(textResults, filters ?: PhotoSearchFilters())
        
        // Calculate relevance score for each photo
        val scoredResults = filteredResults.map { photo ->
            val score = calculateRelevanceScore(photo, query)
            val matchedTags = getMatchedTags(photo, query)
            PhotoSearchResult(
                photo = photo,
                relevanceScore = score,
                matchedTags = matchedTags
            )
        }
        
        // Sort by relevance (highest first) and return
        scoredResults.sortedByDescending { it.relevanceScore }
    }
    
    /**
     * Finds photos with similar visual content based on tag overlap.
     *
     * @param photoId The reference photo ID
     * @return List of similar photos sorted by similarity score
     */
    suspend fun findSimilarPhotos(photoId: String): List<Photo> = withContext(Dispatchers.Default) {
        val photo = photoRepository.getPhoto(photoId)
            ?: return@withContext emptyList()
        
        // Get all photos except the reference photo
        val allPhotos = photoRepository.getAllPhotos()
            .filter { it.id != photoId }
        
        // Calculate similarity scores based on tag overlap
        val similarPhotos = allPhotos
            .map { otherPhoto ->
                val similarity = calculateSimilarity(
                    photo.tags.map { it.label },
                    otherPhoto.tags.map { it.label }
                )
                Pair(otherPhoto, similarity)
            }
            .filter { it.second >= SIMILARITY_THRESHOLD }
            .sortedByDescending { it.second }
            .take(MAX_SUGGESTED_ALBUMS)
            .map { it.first }
        
        similarPhotos
    }
    
    /**
     * Creates a custom album with user-specified name and photos.
     *
     * @param name The album name
     * @param photoIds List of photo IDs to include
     * @param eventId Optional event ID to associate with
     * @return The created album
     */
    suspend fun createCustomAlbum(
        name: String,
        photoIds: List<String>,
        eventId: String? = null
    ): Album = withContext(Dispatchers.Default) {
        val firstPhotoId = photoIds.firstOrNull()
        
        val album = Album(
            id = generateAlbumId(),
            eventId = eventId,
            name = name,
            coverPhotoId = firstPhotoId,
            photoIds = photoIds,
            createdAt = getCurrentTimestamp(),
            isAutoGenerated = false,
            updatedAt = getCurrentTimestamp()
        )
        
        albumRepository.createAlbum(album)
        
        // Add photos to the album
        photoIds.forEach { photoId ->
            photoRepository.addPhotoToAlbum(photoId, album.id)
        }
        
        album
    }
    
    // ================ Private Helper Methods ================
    
    /**
     * Gets the current platform.
     */
    private fun getPlatform(): Platform {
        return when {
            androidPhotoRecognition != null -> Platform.ANDROID
            iosPhotoRecognition != null -> Platform.IOS
            else -> Platform.UNKNOWN
        }
    }
    
    /**
     * Converts image bytes to Android Bitmap.
     * This is a placeholder - in real implementation would use Android.graphics.Bitmap
     */
    private fun convertBytesToAndroidBitmap(imageBytes: ByteArray): Any? {
        // In Android implementation, this would decode the byte array to Bitmap
        // For now, return null to indicate no bitmap conversion available
        return null
    }
    
    /**
     * Generates an album name based on event type and photo metadata.
     */
    private suspend fun generateAlbumName(eventId: String, photos: List<Photo>): String {
        // In a full implementation, would fetch event from EventRepository
        // For now, use a default naming scheme
        val season = getCurrentSeason()
        val year = getCurrentYear()
        
        return "Album $season $year"
    }
    
    /**
     * Suggests album names based on photo tags and categories.
     */
    private fun suggestAlbums(photoId: String, tags: List<PhotoTag>): List<String> {
        val suggestions = mutableListOf<String>()
        
        // Suggest albums by tag category
        val categoryGroups = tags.groupBy { it.category }
        
        categoryGroups[PhotoCategory.PEOPLE]?.let {
            if (it.isNotEmpty()) suggestions.add("Photos de personnes")
        }
        categoryGroups[PhotoCategory.FOOD]?.let {
            if (it.isNotEmpty()) suggestions.add("Photos de nourriture")
        }
        categoryGroups[PhotoCategory.DECORATION]?.let {
            if (it.isNotEmpty()) suggestions.add("Photos de décoration")
        }
        categoryGroups[PhotoCategory.LOCATION]?.let {
            if (it.isNotEmpty()) suggestions.add("Photos de lieux")
        }
        
        // Suggest seasonal album
        val season = getCurrentSeason()
        val year = getCurrentYear()
        suggestions.add("$season $year")
        
        return suggestions.distinct().take(MAX_SUGGESTED_ALBUMS)
    }
    
    /**
     * Applies search filters to a list of photos.
     */
    private fun applyFilters(photos: List<Photo>, filters: PhotoSearchFilters): List<Photo> {
        var result = photos
        
        // Filter by event
        filters.eventId?.let { eventId ->
            result = result.filter { it.eventId == eventId }
        }
        
        // Filter by date range
        if (filters.startDate != null || filters.endDate != null) {
            result = result.filter { photo ->
                val photoDate = try {
                    Instant.parse(photo.uploadedAt)
                } catch (e: Exception) {
                    null
                }
                
                val afterStart = filters.startDate?.let { start ->
                    photoDate?.let { it >= Instant.parse(start) } ?: true
                } ?: true
                
                val beforeEnd = filters.endDate?.let { end ->
                    photoDate?.let { it <= Instant.parse(end) } ?: true
                } ?: true
                
                afterStart && beforeEnd
            }
        }
        
        // Filter by face detection
        filters.hasFaces?.let { hasFaces ->
            result = result.filter { photo ->
                if (hasFaces) {
                    photo.faceDetections.isNotEmpty()
                } else {
                    true
                }
            }
        }
        
        // Filter by minimum confidence
        filters.minConfidence?.let { minConf ->
            result = result.filter { photo ->
                photo.tags.any { it.confidence >= minConf }
            }
        }
        
        return result
    }
    
    /**
     * Calculates a relevance score for a photo based on query matching.
     * Score is normalized to 0.0 - 1.0 range.
     */
    private fun calculateRelevanceScore(photo: Photo, query: String): Double {
        val tags = photo.tags.map { it.label.lowercase() }
        val caption = (photo.caption ?: "").lowercase()
        val queryLower = query.lowercase()
        
        var score = 0.0
        
        // Exact tag match (highest weight)
        if (tags.contains(queryLower)) {
            score += 1.0
        }
        
        // Partial tag match (medium weight)
        tags.forEach { tag ->
            if (tag.contains(queryLower) || queryLower.contains(tag)) {
                score += 0.8
            }
        }
        
        // Caption match (lower weight)
        if (caption.contains(queryLower)) {
            score += 0.6
        }
        
        // Boost for high-confidence auto tags
        val highConfidenceTags = photo.tags.count { it.confidence >= 0.9 }
        score += highConfidenceTags * 0.1
        
        // Normalize to 0.0 - 1.0
        return score.coerceIn(0.0, 1.0)
    }
    
    /**
     * Gets the list of tags that match the query.
     */
    private fun getMatchedTags(photo: Photo, query: String): List<String> {
        val queryLower = query.lowercase()
        return photo.tags
            .filter { tag ->
                tag.label.lowercase().contains(queryLower) ||
                (photo.caption ?: "").lowercase().contains(queryLower)
            }
            .map { it.label }
            .distinct()
    }
    
    /**
     * Calculates similarity between two sets of tags using Jaccard similarity.
     */
    private fun calculateSimilarity(tags1: List<String>, tags2: List<String>): Double {
        val set1 = tags1.toSet()
        val set2 = tags2.toSet()
        
        if (set1.isEmpty() && set2.isEmpty()) {
            return 0.0
        }
        
        val intersection = set1.intersect(set2).size
        val union = set1.union(set2).size
        
        return if (union > 0) {
            intersection.toDouble() / union.toDouble()
        } else {
            0.0
        }
    }
}

/**
 * Represents the current platform.
 */
private enum class Platform {
    ANDROID,
    IOS,
    UNKNOWN
}

/**
 * Result of photo recognition processing.
 *
 * @property photoId The processed photo ID
 * @property facesDetected Number of faces detected
 * @property tagsSuggested Number of tags suggested
 * @property suggestedAlbums List of suggested album names
 */
data class PhotoRecognitionResult(
    val photoId: String,
    val facesDetected: Int,
    val tagsSuggested: Int,
    val suggestedAlbums: List<String>
)

/**
 * Result of a photo search with relevance scoring.
 *
 * @property photo The matching photo
 * @property relevanceScore Relevance score (0.0 - 1.0)
 * @property matchedTags List of tags that matched the query
 */
data class PhotoSearchResult(
    val photo: Photo,
    val relevanceScore: Double,
    val matchedTags: List<String>
)

/**
 * Filters for photo search.
 *
 * @property eventId Optional event ID filter
 * @property startDate Optional start date filter (ISO 8601)
 * @property endDate Optional end date filter (ISO 8601)
 * @property hasFaces Optional filter for photos with face detections
 * @property minConfidence Optional minimum confidence threshold for tags
 */
data class PhotoSearchFilters(
    val eventId: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val hasFaces: Boolean? = null,
    val minConfidence: Double? = null
)
