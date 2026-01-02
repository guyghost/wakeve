package com.guyghost.wakeve.ml

import com.guyghost.wakeve.models.FaceDetection
import com.guyghost.wakeve.models.Photo
import com.guyghost.wakeve.models.PhotoTag
import com.guyghost.wakeve.models.Album
import com.guyghost.wakeve.models.Participant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

/**
 * Default implementation of SharingSuggestionService.
 *
 * Implements multi-factor scoring algorithm:
 * 1. Face Detection (40% weight) - Users detected in photos
 * 2. Event Participants (30% weight) - Users in the same event
 * 3. Tag Interests (20% weight) - Users with matching interests
 * 4. Sharing History (10% weight) - Users you frequently share with
 *
 * All processing runs on-device for privacy compliance.
 */
class DefaultSharingSuggestionService(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val scoringWeights: SharingScoringWeights = SharingScoringWeights.DEFAULT
) : SharingSuggestionService {

    private companion object {
        private const val TAG = "SharingSuggestionService"
        private const val MIN_RELEVANCE_SCORE = 0.3
        private const val MAX_SUGGESTIONS = 5
        private const val FACE_CONFIDENCE_THRESHOLD = 0.7
    }

    /**
     * Suggests sharing targets for a single photo.
     */
    override fun suggestSharingTargetsForPhoto(photo: Photo): Flow<SharingSuggestion> = flow {
        val suggestions = mutableListOf<SharingSuggestion>()

        // 1. Face-based suggestions (40% weight)
        val faceSuggestions = generateFaceBasedSuggestions(photo)
        suggestions.addAll(faceSuggestions)

        // 2. Event-based suggestions (30% weight)
        val eventSuggestions = generateEventBasedSuggestions(photo)
        suggestions.addAll(eventSuggestions)

        // 3. Tag-based suggestions (20% weight)
        val tagSuggestions = generateTagBasedSuggestions(photo)
        suggestions.addAll(tagSuggestions)

        // 4. History-based suggestions (10% weight)
        val historySuggestions = generateHistoryBasedSuggestions(photo)
        suggestions.addAll(historySuggestions)

        // Merge and de-duplicate suggestions
        val mergedSuggestions = mergeAndScoreSuggestions(suggestions)

        // Filter by minimum relevance and limit to max suggestions
        mergedSuggestions
            .filter { it.relevanceScore >= MIN_RELEVANCE_SCORE }
            .sortedByDescending { it.relevanceScore }
            .take(MAX_SUGGESTIONS)
            .forEach { emit(it) }
    }

    /**
     * Suggests sharing targets for an entire album.
     */
    override fun suggestSharingTargetsForAlbum(
        album: Album,
        photos: Map<String, Photo>
    ): Flow<SharingSuggestion> = flow {
        val albumPhotos = album.photoIds.mapNotNull { photos[it] }

        if (albumPhotos.isEmpty()) {
            return@flow
        }

        // Aggregate all suggestions from all photos
        val allSuggestions = mutableListOf<SharingSuggestion>()

        for (photo in albumPhotos) {
            suggestSharingTargetsForPhoto(photo).collect { suggestion ->
                allSuggestions.add(suggestion)
            }
        }

        // De-duplicate and score
        val mergedSuggestions = mergeAlbumSuggestions(allSuggestions)

        mergedSuggestions
            .filter { it.relevanceScore >= MIN_RELEVANCE_SCORE }
            .sortedByDescending { it.relevanceScore }
            .take(MAX_SUGGESTIONS)
            .forEach { emit(it) }
    }

    /**
     * Suggests sharing targets for a specific tag across all photos.
     */
    override fun suggestSharingTargetsForTag(tag: PhotoTag, photos: List<Photo>): Flow<SharingSuggestion> = flow {
        // Find photos with matching tags
        val matchingPhotos = photos.filter { photo ->
            photo.tags.any { it.category == tag.category }
        }

        // Get users interested in this tag from preferences
        val interestedUsers = userPreferencesRepository.getUsersInterestedInTag(tag.category)

        // Generate suggestions
        val suggestions = interestedUsers.map { user ->
            val relevanceScore = calculateTagRelevanceScore(user, matchingPhotos.size)
            SharingSuggestion(
                userId = user.userId,
                name = user.name,
                avatarUrl = user.avatarUrl,
                relevanceScore = relevanceScore,
                reason = SuggestionReason.TAG_INTEREST,
                confidence = user.interestConfidence,
                supportingDetails = listOf(
                    "Interested in ${tag.category.name}",
                    "${matchingPhotos.size} matching photos"
                )
            )
        }

        suggestions
            .sortedByDescending { it.relevanceScore }
            .take(MAX_SUGGESTIONS)
            .forEach { emit(it) }
    }

    /**
     * Records a sharing action to improve future suggestions.
     */
    override suspend fun recordSharingAction(
        userId: String,
        targetUserId: String,
        photoIds: List<String>,
        wasAccepted: Boolean
    ) {
        userPreferencesRepository.recordSharingAction(
            userId = userId,
            targetUserId = targetUserId,
            photoIds = photoIds,
            wasAccepted = wasAccepted
        )

        // Update scoring weights based on feedback
        if (wasAccepted) {
            // Increase weight for the suggestion reason that led to this share
            // (simplified implementation)
        }
    }

    /**
     * Gets personalized sharing insights for a user.
     */
    override suspend fun getSharingInsights(userId: String): SharingInsights {
        return userPreferencesRepository.getSharingInsights(userId)
    }

    // MARK: - Private Helper Methods

    /**
     * Generates face-based suggestions from detected faces in photo.
     */
    private suspend fun generateFaceBasedSuggestions(photo: Photo): List<SharingSuggestion> {
        return photo.faceDetections
            .filter { it.confidence >= FACE_CONFIDENCE_THRESHOLD }
            .mapNotNull { face ->
                // Match face to user (simplified - would use actual face recognition)
                val matchedUser = userPreferencesRepository.matchFaceToUser(face)
                    ?: return@mapNotNull null

                val relevanceScore = scoringWeights.faceDetectionWeight * face.confidence

                SharingSuggestion(
                    userId = matchedUser.userId,
                    name = matchedUser.name,
                    avatarUrl = matchedUser.avatarUrl,
                    relevanceScore = relevanceScore,
                    reason = SuggestionReason.DETECTED_IN_PHOTOS,
                    confidence = face.confidence,
                    supportingDetails = listOf(
                        "Detected in photo with ${(face.confidence * 100).toInt()}% confidence"
                    )
                )
            }
    }

    /**
     * Generates event-based suggestions from event participants.
     */
    private suspend fun generateEventBasedSuggestions(photo: Photo): List<SharingSuggestion> {
        val participants = userPreferencesRepository.getEventParticipants(photo.eventId)

        return participants.map { participant ->
            val relevanceScore = scoringWeights.eventParticipantWeight

            SharingSuggestion(
                userId = participant.userId,
                name = participant.name,
                avatarUrl = participant.avatarUrl,
                relevanceScore = relevanceScore,
                reason = SuggestionReason.EVENT_PARTICIPANT,
                confidence = 0.8, // Medium confidence for event-based
                supportingDetails = listOf(
                    "Event participant",
                    "Event: ${userPreferencesRepository.getEventName(photo.eventId) ?: photo.eventId}"
                )
            )
        }
    }

    /**
     * Generates tag-based suggestions from photo tags.
     */
    private suspend fun generateTagBasedSuggestions(photo: Photo): List<SharingSuggestion> {
        val suggestions = mutableListOf<SharingSuggestion>()

        for (tag in photo.tags) {
            val interestedUsers = userPreferencesRepository.getUsersInterestedInTag(tag.category)

            for (user in interestedUsers) {
                val relevanceScore = scoringWeights.tagInterestWeight * tag.confidence

                suggestions.add(
                    SharingSuggestion(
                        userId = user.userId,
                        name = user.name,
                        avatarUrl = user.avatarUrl,
                        relevanceScore = relevanceScore,
                        reason = SuggestionReason.TAG_INTEREST,
                        confidence = tag.confidence * user.interestConfidence,
                        supportingDetails = listOf(
                            "Interested in ${tag.category.name}",
                            "Photo tagged: ${tag.label}"
                        )
                    )
                )
            }
        }

        return suggestions
    }

    /**
     * Generates history-based suggestions from sharing history.
     */
    private suspend fun generateHistoryBasedSuggestions(photo: Photo): List<SharingSuggestion> {
        val frequentTargets = userPreferencesRepository.getFrequentShareTargets(
            userId = getCurrentUserId()
        )

        return frequentTargets.map { target ->
            val relevanceScore = scoringWeights.historyWeight * (target.shareCount / 10.0).coerceAtMost(1.0)

            SharingSuggestion(
                userId = target.userId,
                name = target.name,
                avatarUrl = target.avatarUrl,
                relevanceScore = relevanceScore,
                reason = SuggestionReason.FREQUENT_SHARE_TARGET,
                confidence = target.acceptanceRate,
                supportingDetails = listOf(
                    "Shared ${target.shareCount} times before",
                    "${(target.acceptanceRate * 100).toInt()}% acceptance rate"
                )
            )
        }
    }

    /**
     * Merges and scores multiple suggestions for the same user.
     */
    private fun mergeAndScoreSuggestions(suggestions: List<SharingSuggestion>): List<SharingSuggestion> {
        val merged = mutableMapOf<String, SharingSuggestion>()

        for (suggestion in suggestions) {
            val existing = merged[suggestion.userId]

            if (existing == null) {
                merged[suggestion.userId] = suggestion
            } else {
                // Merge scores and reasons
                val mergedScore = existing.relevanceScore + suggestion.relevanceScore
                val mergedReason = if (mergedScore > 0.6) {
                    SuggestionReason.MULTI_FACTOR
                } else {
                    existing.reason
                }
                val mergedDetails = existing.supportingDetails + suggestion.supportingDetails

                merged[suggestion.userId] = suggestion.copy(
                    relevanceScore = mergedScore,
                    reason = mergedReason,
                    confidence = (existing.confidence + suggestion.confidence) / 2,
                    supportingDetails = mergedDetails
                )
            }
        }

        return merged.values.toList()
    }

    /**
     * Merges suggestions from multiple photos in an album.
     */
    private fun mergeAlbumSuggestions(suggestions: List<SharingSuggestion>): List<SharingSuggestion> {
        val userSuggestions = suggestions.groupBy { it.userId }

        return userSuggestions.map { (userId, userSuggestionList) ->
            val totalRelevanceScore = userSuggestionList.sumOf { it.relevanceScore }
            val normalizedScore = (totalRelevanceScore / userSuggestionList.size).coerceAtMost(1.0)
            val allReasons = userSuggestionList.map { it.reason }.distinct()
            val mergedReason = if (allReasons.size > 1) {
                SuggestionReason.MULTI_FACTOR
            } else {
                allReasons.firstOrNull() ?: SuggestionReason.TAG_INTEREST
            }
            val allDetails = userSuggestionList.flatMap { it.supportingDetails }.distinct()
            val avgConfidence = userSuggestionList.map { it.confidence }.average()

            userSuggestionList.first().copy(
                relevanceScore = normalizedScore,
                reason = mergedReason,
                confidence = avgConfidence,
                supportingDetails = allDetails
            )
        }
    }

    /**
     * Calculates relevance score for tag-based suggestions.
     */
    private fun calculateTagRelevanceScore(user: UserInterest, photoCount: Int): Double {
        val normalizedPhotoCount = (photoCount / 10.0).coerceAtMost(1.0)
        return scoringWeights.tagInterestWeight * user.interestConfidence * normalizedPhotoCount
    }

    /**
     * Gets the current user ID.
     */
    private fun getCurrentUserId(): String {
        return userPreferencesRepository.getCurrentUserId()
    }
}

// MARK: - Supporting Data Classes

@Serializable
data class UserInterest(
    val userId: String,
    val name: String,
    val avatarUrl: String?,
    val interestConfidence: Double // 0.0 - 1.0
)

@Serializable
data class FrequentShareTarget(
    val userId: String,
    val name: String,
    val avatarUrl: String?,
    val shareCount: Int,
    val acceptanceRate: Double // 0.0 - 1.0
)

/**
 * Result of face matching operation.
 */
@Serializable
data class FaceMatchResult(
    val userId: String,
    val name: String,
    val avatarUrl: String?,
    val confidence: Double // 0.0 - 1.0
)
