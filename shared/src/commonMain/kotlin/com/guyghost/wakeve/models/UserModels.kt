package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * User domain model for authenticated users
 */
data class User(
    val id: String,
    val providerId: String,  // OAuth provider ID
    val email: String,
    val name: String,
    val avatarUrl: String? = null,
    val provider: OAuthProvider,
    val createdAt: String,  // ISO 8601 UTC
    val updatedAt: String   // ISO 8601 UTC
)

enum class OAuthProvider {
    GOOGLE, APPLE
}

/**
 * User token domain model
 */
data class UserToken(
    val id: String,
    val userId: String,
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String = "Bearer",
    val expiresAt: String,  // ISO 8601 UTC
    val scope: String? = null,
    val createdAt: String,  // ISO 8601 UTC
    val updatedAt: String   // ISO 8601 UTC
)

/**
 * Notification preferences domain model
 */
data class NotificationPreferences(
    val id: String,
    val userId: String,
    val deadlineReminder: Boolean = true,
    val eventUpdate: Boolean = true,
    val voteCloseReminder: Boolean = true,
    val timezone: String = "UTC",
    val createdAt: String,  // ISO 8601 UTC
    val updatedAt: String   // ISO 8601 UTC
)

/**
 * OAuth login request model
 */
@Serializable
data class OAuthLoginRequest(
    val provider: String,  // "google" or "apple"
    val idToken: String,   // JWT token from OAuth provider
    val accessToken: String? = null,
    val authorizationCode: String? = null
)

/**
 * OAuth login response model
 */
@Serializable
data class OAuthLoginResponse(
    val user: UserResponse,
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String = "Bearer",
    val expiresIn: Long,  // seconds
    val scope: String? = null
)

/**
 * User API response model
 */
@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val name: String,
    val avatarUrl: String? = null,
    val provider: String,  // "google" or "apple"
    val createdAt: String
)

/**
 * Token refresh request
 */
@Serializable
data class TokenRefreshRequest(
    val refreshToken: String
)

/**
 * Token refresh response
 */
@Serializable
data class TokenRefreshResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val scope: String? = null
)

/**
 * Authentication middleware context
 */
data class AuthContext(
    val userId: String,
    val user: User? = null,
    val token: UserToken? = null
)

/**
 * Sync operation types
 */
enum class SyncOperation {
    CREATE, UPDATE, DELETE
}

/**
 * Sync change record
 */
@Serializable
data class SyncChange(
    val id: String,
    val table: String,  // "events", "participants", "votes"
    val operation: String,  // "CREATE", "UPDATE", "DELETE"
    val recordId: String,
    val data: String,  // JSON string of the record
    val timestamp: String,  // ISO 8601 UTC
    val userId: String
)

/**
 * Sync request payload
 */
@Serializable
data class SyncRequest(
    val changes: List<SyncChange>,
    val lastSyncTimestamp: String? = null  // Client's last successful sync
)

/**
 * Sync response payload
 */
@Serializable
data class SyncResponse(
    val success: Boolean,
    val appliedChanges: Int,
    val conflicts: List<SyncConflict> = emptyList(),
    val serverTimestamp: String,
    val message: String? = null
)

/**
 * Sync conflict information
 */
@Serializable
data class SyncConflict(
    val changeId: String,
    val table: String,
    val recordId: String,
    val clientData: String,
    val serverData: String,
    val resolution: String  // "CLIENT_WINS", "SERVER_WINS", "MERGE"
)

/**
 * Sync metadata for tracking changes
 */
data class SyncMetadata(
    val id: String,
    val tableName: String,
    val recordId: String,
    val operation: SyncOperation,
    val timestamp: String,
    val userId: String,
    val synced: Boolean = false,
    val retryCount: Int = 0,
    val lastError: String? = null
)

/**
 * Data structures for sync operations
 */
@Serializable
data class SyncEventData(
    val id: String,
    val title: String,
    val description: String,
    val organizerId: String,
    val deadline: String,
    val timezone: String
)

@Serializable
data class SyncParticipantData(
    val eventId: String,
    val userId: String
)

@Serializable
data class SyncVoteData(
    val eventId: String,
    val participantId: String,
    val slotId: String,
    val preference: String  // "YES", "MAYBE", "NO"
)