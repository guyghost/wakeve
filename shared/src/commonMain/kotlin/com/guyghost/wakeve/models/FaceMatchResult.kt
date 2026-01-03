package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Core model representing the result of a face matching operation.
 * 
 * This is a pure data model - Functional Core.
 * Contains no side effects, no I/O.
 * 
 * @param userId The matched user ID
 * @param name Display name of the user
 * @param avatarUrl Profile picture URL (nullable)
 * @param confidence Confidence level of the match 0.0 - 1.0
 */
@Serializable
data class FaceMatchResult(
    val userId: String,
    val name: String,
    val avatarUrl: String?,
    val confidence: Double // 0.0 - 1.0
)
