package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Core model representing a user's interest in a photo category.
 * 
 * This is a pure data model - Functional Core.
 * Contains no side effects, no I/O.
 * 
 * @param userId The user ID
 * @param name Display name of the user
 * @param avatarUrl Profile picture URL (nullable)
 * @param interestConfidence Confidence level 0.0 - 1.0
 */
@Serializable
data class UserInterest(
    val userId: String,
    val name: String,
    val avatarUrl: String?,
    val interestConfidence: Double // 0.0 - 1.0
)
