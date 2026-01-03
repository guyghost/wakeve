package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Core model representing a frequent share target.
 * 
 * This is a pure data model - Functional Core.
 * Contains no side effects, no I/O.
 * 
 * @param userId The target user ID
 * @param name Display name of the user
 * @param avatarUrl Profile picture URL (nullable)
 * @param shareCount Number of times shared with this user
 * @param acceptanceRate Rate at which shares were accepted 0.0 - 1.0
 */
@Serializable
data class FrequentShareTarget(
    val userId: String,
    val name: String,
    val avatarUrl: String?,
    val shareCount: Int,
    val acceptanceRate: Double // 0.0 - 1.0
)
