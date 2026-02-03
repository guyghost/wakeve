package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Section of the event where a comment is posted
 */
@Serializable
enum class CommentSection {
    GENERAL,        // General event comments
    SCENARIO,       // Scenario comparison section
    POLL,           // Date polling section
    TRANSPORT,      // Transport planning section
    ACCOMMODATION,  // Accommodation section
    MEAL,           // Meal planning section
    EQUIPMENT,      // Equipment checklist section
    ACTIVITY,       // Activity planning section
    BUDGET          // Budget section
}

/**
 * Comment on an event section
 *
 * Represents a comment or discussion thread on any section of event.
 * Supports nested comments (replies) via parentCommentId.
 * Supports @mentions, pinning, and soft deletion.
 *
 * @property id Unique identifier
 * @property eventId Event this comment belongs to
 * @property section Section of event
 * @property sectionItemId Optional ID of specific item within section (e.g., scenario ID, meal ID)
 * @property authorId Participant ID who wrote comment
 * @property authorName Name of author (denormalized for display)
 * @property content Comment text content
 * @property parentCommentId ID of parent comment (null if top-level comment)
 * @property mentions List of user IDs mentioned in this comment
 * @property isDeleted Whether comment has been soft deleted
 * @property isPinned Whether comment is pinned by organizer
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC, null if never edited)
 * @property isEdited Whether comment has been edited
 * @property replyCount Number of direct replies to this comment
 */
@Serializable
data class Comment(
    val id: String,
    val eventId: String,
    val section: CommentSection,
    val sectionItemId: String? = null,
    val authorId: String,
    val authorName: String,
    val content: String,
    val parentCommentId: String? = null,
    val mentions: List<String> = emptyList(),  // List of mentioned user IDs
    val isDeleted: Boolean = false,  // Soft delete flag
    val isPinned: Boolean = false,  // Pinned by organizer
    val createdAt: String,  // ISO 8601 UTC timestamp
    val updatedAt: String? = null,  // ISO 8601 UTC timestamp, null if never edited
    val isEdited: Boolean = false,
    val replyCount: Int = 0
) {
    init {
        require(content.isNotBlank()) { "Comment content cannot be blank" }
        require(content.length <= 2000) { "Comment content cannot exceed 2000 characters" }
        require(authorName.isNotBlank()) { "Author name cannot be blank" }
    }

    /**
     * Check if user can edit this comment.
     * User can edit if they are the author and comment is not deleted.
     */
    fun canEdit(userId: String): Boolean =
        authorId == userId && !isDeleted

    /**
     * Check if user can delete this comment.
     * User can delete if they are the author or if they are the organizer.
     */
    fun canDelete(userId: String, isOrganizer: Boolean): Boolean =
        !isDeleted && (authorId == userId || isOrganizer)

    /**
     * Check if user can pin/unpin this comment.
     * Only organizers can pin comments.
     */
    fun canPin(userId: String, isOrganizer: Boolean): Boolean =
        !isDeleted && isOrganizer
}

/**
 * Comment with nested replies
 * 
 * Used for displaying comment threads with their replies.
 */
@Serializable
data class CommentThread(
    val comment: Comment,
    val replies: List<Comment> = emptyList(),
    val hasMoreReplies: Boolean = false
)

/**
 * Comments grouped by section
 * 
 * @property section Section of the event
 * @property sectionItemId Optional ID of specific item
 * @property comments List of top-level comments
 * @property totalComments Total number of comments (including replies)
 */
@Serializable
data class CommentsBySection(
    val section: CommentSection,
    val sectionItemId: String? = null,
    val comments: List<CommentThread>,
    val totalComments: Int
)

/**
 * Comment statistics for an event
 * 
 * @property eventId Event ID
 * @property totalComments Total number of comments across all sections
 * @property commentsBySection Number of comments per section
 * @property topContributors List of top comment contributors (authorId to count)
 * @property recentActivity Recent comment activity (last 24h)
 */
@Serializable
data class CommentStatistics(
    val eventId: String,
    val totalComments: Int,
    val commentsBySection: Map<CommentSection, Int>,
    val topContributors: List<Pair<String, Int>>, // List of (authorId, commentCount)
    val recentActivity: Int  // Comments in last 24 hours
)

/**
 * Participant comment activity
 * 
 * @property participantId Participant ID
 * @property participantName Participant name
 * @property commentCount Total comments posted
 * @property replyCount Total replies posted
 * @property lastCommentAt Timestamp of last comment (ISO 8601 UTC)
 */
@Serializable
data class ParticipantCommentActivity(
    val participantId: String,
    val participantName: String,
    val commentCount: Int,
    val replyCount: Int,
    val lastCommentAt: String?  // ISO 8601 UTC timestamp
)

/**
 * Request to create or update a comment
 * 
 * @property section Section of the event
 * @property sectionItemId Optional ID of specific item
 * @property content Comment text content
 * @property parentCommentId ID of parent comment (for replies)
 */
@Serializable
data class CommentRequest(
    val section: CommentSection,
    val sectionItemId: String? = null,
    val content: String,
    val parentCommentId: String? = null
) {
    init {
        require(content.isNotBlank()) { "Comment content cannot be blank" }
        require(content.length <= 2000) { "Comment content cannot exceed 2000 characters" }
    }
}

/**
 * Request to update a comment
 * 
 * @property content New comment text content
 */
@Serializable
data class CommentUpdateRequest(
    val content: String
) {
    init {
        require(content.isNotBlank()) { "Comment content cannot be blank" }
        require(content.length <= 2000) { "Comment content cannot exceed 2000 characters" }
    }
}

/**
 * Query filters for fetching comments
 * 
 * @property section Filter by section (optional)
 * @property sectionItemId Filter by section item ID (optional)
 * @property authorId Filter by author ID (optional)
 * @property parentCommentId Filter by parent comment (null for top-level, "none" for no parent)
 * @property limit Maximum number of comments to return
 * @property offset Offset for pagination
 */
@Serializable
data class CommentQueryFilters(
    val section: CommentSection? = null,
    val sectionItemId: String? = null,
    val authorId: String? = null,
    val parentCommentId: String? = null,
    val limit: Int = 50,
    val offset: Int = 0
) {
    init {
        require(limit > 0 && limit <= 100) { "Limit must be between 1 and 100" }
        require(offset >= 0) { "Offset must be non-negative" }
    }
}

/**
 * Comment notification
 * 
 * Represents a notification for a new comment or reply.
 * 
 * @property id Notification ID
 * @property recipientId Participant ID to notify
 * @property commentId Comment ID that triggered the notification
 * @property eventId Event ID
 * @property type Type of notification
 * @property message Notification message
 * @property isRead Whether the notification has been read
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 */
@Serializable
data class CommentNotification(
    val id: String,
    val recipientId: String,
    val commentId: String,
    val eventId: String,
    val type: CommentNotificationType,
    val message: String,
    val isRead: Boolean = false,
    val createdAt: String  // ISO 8601 UTC timestamp
)

/**
 * Type of comment notification
 */
@Serializable
enum class CommentNotificationType {
    NEW_COMMENT,     // New comment on a section you're following
    REPLY,           // Someone replied to your comment
    MENTION          // You were mentioned in a comment
}

/**
 * Mention of a user in a comment
 *
 * Represents a @username mention in a comment content with position information.
 *
 * @property id Unique identifier
 * @property commentId Comment ID containing the mention
 * @property mentionedUserId User ID who was mentioned
 * @property startIndex Start index of @username in content
 * @property endIndex End index of @username in content
 */
@Serializable
data class Mention(
    val id: String,
    val commentId: String,
    val mentionedUserId: String,
    val startIndex: Int,
    val endIndex: Int
)

/**
 * Comment with author information
 *
 * Combines comment with author details for display in UI.
 *
 * @property comment The comment
 * @property author Summary of the author
 * @property replyCount Number of replies to this comment
 * @property isLiked Whether current user liked this comment (future feature)
 */
@Serializable
data class CommentWithAuthor(
    val comment: Comment,
    val author: UserSummary,
    val replyCount: Int = 0,
    val isLiked: Boolean = false
)

/**
 * User summary for comment display
 *
 * Lightweight user info for comment author display.
 *
 * @property id User ID
 * @property name Display name
 * @property avatarUrl Optional avatar URL
 * @property username Username for @mentions
 */
@Serializable
data class UserSummary(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val username: String? = null
)
