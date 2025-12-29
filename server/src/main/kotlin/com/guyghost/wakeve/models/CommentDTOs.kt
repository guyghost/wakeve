package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateCommentRequest(
    val participantId: String,
    val content: String,
    val section: CommentSection = CommentSection.GENERAL, 
    val parentId: String? = null
) {
    fun toComment(eventId: String): Comment {
         return Comment(
            id = java.util.UUID.randomUUID().toString(),
            eventId = eventId,
            authorId = participantId,
            content = content,
            parentCommentId = parentId, // Correct parameter name
            section = section,
            authorName = "Unknown",
            createdAt = java.time.Instant.now().toString(),
            updatedAt = java.time.Instant.now().toString()
        )
    }
}

@Serializable
data class UpdateCommentRequest(
    val content: String
)
