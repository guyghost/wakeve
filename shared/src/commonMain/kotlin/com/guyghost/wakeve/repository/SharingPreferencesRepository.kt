package com.guyghost.wakeve.repository

import com.guyghost.wakeve.ml.SharingInsights
import com.guyghost.wakeve.models.FaceDetection
import com.guyghost.wakeve.models.FaceMatchResult
import com.guyghost.wakeve.models.FrequentShareTarget
import com.guyghost.wakeve.models.ParticipantInfo
import com.guyghost.wakeve.models.PhotoCategory
import com.guyghost.wakeve.models.UserInterest

/**
 * Repository for sharing-specific user preferences and data.
 * This is part of the Imperative Shell - handles I/O and data access.
 * 
 * Architecture: Functional Core & Imperative Shell
 * - This is in the Shell layer (can have side effects, I/O, async/await)
 * - Core models (ParticipantInfo, UserInterest, etc.) are imported from models/
 */
interface SharingPreferencesRepository {
    
    /**
     * Get users interested in a specific photo category tag.
     */
    suspend fun getUsersInterestedInTag(category: PhotoCategory): List<UserInterest>
    
    /**
     * Match a detected face to a user profile.
     */
    suspend fun matchFaceToUser(face: FaceDetection): FaceMatchResult?
    
    /**
     * Get participants for a specific event.
     */
    suspend fun getEventParticipants(eventId: String): List<ParticipantInfo>
    
    /**
     * Get event name by ID.
     */
    suspend fun getEventName(eventId: String): String?
    
    /**
     * Get frequent share targets for a user.
     */
    suspend fun getFrequentShareTargets(userId: String): List<FrequentShareTarget>
    
    /**
     * Record a sharing action for learning.
     */
    suspend fun recordSharingAction(
        userId: String,
        targetUserId: String,
        photoIds: List<String>,
        wasAccepted: Boolean
    )
    
    /**
     * Get sharing insights for a user.
     */
    suspend fun getSharingInsights(userId: String): SharingInsights
    
    /**
     * Get current user ID.
     */
    fun getCurrentUserId(): String
}
