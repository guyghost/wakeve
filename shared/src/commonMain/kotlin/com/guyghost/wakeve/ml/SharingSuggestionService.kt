package com.guyghost.wakeve.ml

import com.guyghost.wakeve.models.FaceDetection
import com.guyghost.wakeve.models.Photo
import com.guyghost.wakeve.models.PhotoTag
import com.guyghost.wakeve.models.Album
import kotlinx.coroutines.flow.Flow

/**
 * Service for generating intelligent photo sharing suggestions.
 *
 * Analyzes photos, albums, and user behavior to suggest optimal sharing targets.
 *
 * Suggestion Types:
 * - **People-based**: Share with detected faces in photos
 * - **Event-based**: Share with event participants
 * - **Tag-based**: Share with users interested in specific topics (food, decoration, etc.)
 * - **History-based**: Share with users you frequently share with
 * - **Combined**: Multi-factor scoring algorithm
 */
interface SharingSuggestionService {

    /**
     * Suggests sharing targets for a single photo.
     *
     * Factors:
     * - Detected faces in photo
     * - Photo tags (food, decoration, etc.)
     * - Event participants
     * - User sharing history
     *
     * @param photo The photo to analyze for sharing suggestions
     * @return Flow of sharing suggestions sorted by relevance score
     */
    fun suggestSharingTargetsForPhoto(photo: Photo): Flow<SharingSuggestion>

    /**
     * Suggests sharing targets for an entire album.
     *
     * Aggregates suggestions from all photos in the album,
     * applies de-duplication and relevance scoring.
     *
     * @param album The album to analyze for sharing suggestions
     * @param photos Map of photo IDs to Photo objects in the album
     * @return Flow of sharing suggestions sorted by relevance score
     */
    fun suggestSharingTargetsForAlbum(album: Album, photos: Map<String, Photo>): Flow<SharingSuggestion>

    /**
     * Suggests sharing targets for a specific tag across all photos.
     *
     * Useful for bulk sharing (e.g., "Share all food photos with Sarah").
     *
     * @param tag The tag to search for
     * @param photos All photos to search through
     * @return Flow of sharing suggestions sorted by relevance score
     */
    fun suggestSharingTargetsForTag(tag: PhotoTag, photos: List<Photo>): Flow<SharingSuggestion>

    /**
     * Records a sharing action to improve future suggestions.
     *
     * Implements feedback loop for machine learning.
     *
     * @param userId The user who shared
     * @param targetUserId The target user who received the share
     * @param photoIds List of photo IDs that were shared
     * @param wasAccepted Whether the target accepted/viewed the share
     */
    suspend fun recordSharingAction(
        userId: String,
        targetUserId: String,
        photoIds: List<String>,
        wasAccepted: Boolean
    )

    /**
     * Gets personalized sharing insights for a user.
     *
     * Returns statistics and trends about the user's sharing behavior.
     *
     * @param userId The user ID to analyze
     * @return SharingInsights containing user-specific statistics
     */
    suspend fun getSharingInsights(userId: String): SharingInsights
}

/**
 * Represents a sharing suggestion with relevance scoring.
 *
 * @param userId The suggested user ID to share with
 * @param name Display name of the user
 * @param avatarUrl Profile picture URL
 * @param relevanceScore 0.0 - 1.0, higher = more relevant
 * @param reason The primary reason for this suggestion
 * @param confidence Confidence level in this suggestion (0.0 - 1.0)
 * @param supportingDetails Additional context for the suggestion
 */
data class SharingSuggestion(
    val userId: String,
    val name: String,
    val avatarUrl: String?,
    val relevanceScore: Double, // 0.0 - 1.0
    val reason: SuggestionReason,
    val confidence: Double, // 0.0 - 1.0
    val supportingDetails: List<String>
) {
    /**
     * Checks if this suggestion is high confidence.
     */
    fun isHighConfidence(): Boolean = confidence >= 0.8

    /**
     * Checks if this suggestion has high relevance.
     */
    fun isHighRelevance(): Boolean = relevanceScore >= 0.7
}

/**
 * The primary reason for a sharing suggestion.
 */
enum class SuggestionReason {
    /**
     * Suggested because the user's face appears in photos.
     */
    DETECTED_IN_PHOTOS,

    /**
     * Suggested because the user is an event participant.
     */
    EVENT_PARTICIPANT,

    /**
     * Suggested because the user has interest in photo tags (e.g., foodie).
     */
    TAG_INTEREST,

    /**
     * Suggested because of previous sharing history.
     */
    FREQUENT_SHARE_TARGET,

    /**
     * Suggested because of multiple factors (faces, tags, history).
     */
    MULTI_FACTOR
}

/**
 * Insights about a user's sharing behavior.
 *
 * @param totalShares Total number of shares made by this user
 * @param acceptedShares Number of shares that were accepted/viewed
 * @param topShareTargets Users this user shares with most frequently
 * @param topTags Categories of photos this user shares most
 * @param averagePhotosPerShare Average number of photos per share action
 * @param preferredSharingMethod Preferred method (email, app notification, etc.)
 */
data class SharingInsights(
    val totalShares: Int,
    val acceptedShares: Int,
    val topShareTargets: List<ShareTargetStats>,
    val topTags: List<TagShareStats>,
    val averagePhotosPerShare: Double,
    val preferredSharingMethod: SharingMethod
) {
    /**
     * Calculates the acceptance rate of this user's shares.
     */
    fun acceptanceRate(): Double = if (totalShares == 0) 0.0 else acceptedShares.toDouble() / totalShares
}

/**
 * Statistics about sharing with a specific user.
 *
 * @param userId The target user ID
 * @param name Display name
 * @param shareCount Number of times shared with this user
 * @param acceptanceRate Rate at which shares were accepted (0.0 - 1.0)
 * @param lastShareDate ISO timestamp of most recent share
 */
data class ShareTargetStats(
    val userId: String,
    val name: String,
    val shareCount: Int,
    val acceptanceRate: Double,
    val lastShareDate: String
)

/**
 * Statistics about sharing photos with specific tags.
 *
 * @param tag The photo tag category
 * @param shareCount Number of times photos with this tag were shared
 * @param averageRelevance Average relevance score of shares with this tag
 */
data class TagShareStats(
    val tag: PhotoTag.PhotoCategory,
    val shareCount: Int,
    val averageRelevance: Double
)

/**
 * Preferred method for sharing photos.
 */
enum class SharingMethod {
    APP_NOTIFICATION,
    EMAIL,
    SMS,
    COPY_LINK
}

/**
 * Scoring weights for different factors in sharing suggestions.
 *
 * Higher weight = more influence on final score.
 */
data class SharingScoringWeights(
    val faceDetectionWeight: Double = 0.4,
    val eventParticipantWeight: Double = 0.3,
    val tagInterestWeight: Double = 0.2,
    val historyWeight: Double = 0.1
) {
    init {
        require(faceDetectionWeight + eventParticipantWeight + tagInterestWeight + historyWeight == 1.0) {
            "Weights must sum to 1.0"
        }
    }

    companion object {
        /**
         * Default balanced weights.
         */
        val DEFAULT = SharingScoringWeights(
            faceDetectionWeight = 0.4,
            eventParticipantWeight = 0.3,
            val tagInterestWeight = 0.2,
            val historyWeight = 0.1
        )

        /**
         * Conservative weights favoring high-confidence suggestions.
         */
        val CONSERVATIVE = SharingScoringWeights(
            faceDetectionWeight = 0.5,
            eventParticipantWeight = 0.4,
            val tagInterestWeight = 0.1,
            val historyWeight = 0.0
        )

        /**
         * Aggressive weights exploring more suggestions.
         */
        val AGGRESSIVE = SharingScoringWeights(
            faceDetectionWeight = 0.2,
            eventParticipantWeight = 0.2,
            val tagInterestWeight = 0.4,
            val historyWeight = 0.2
        )
    }
}
