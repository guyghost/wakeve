package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Core model for participant information.
 * 
 * This is a pure data model - Functional Core.
 * Contains no side effects, no I/O.
 */
@Serializable
data class ParticipantInfo(
    val userId: String,
    val name: String,
    val avatarUrl: String?
)
