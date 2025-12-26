package com.guyghost.wakeve.comment

import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.EventRepositoryInterface
import kotlinx.datetime.*
import kotlin.random.Random

/**
 * Comment Repository - Manages comments and discussion threads persistence.
 * 
 * Responsibilities:
 * - CRUD operations for comments
 * - Thread building (parent comment + all replies)
 * - Auto-increment/decrement reply counts
 * - Comment queries and filtering
 * - Statistics and aggregations
 * - Map between SQLDelight entities and Kotlin models
 */
class CommentRepository(
    private val db: WakevDb,
    private val commentNotificationService: CommentNotificationService? = null,
    private val eventRepository: EventRepositoryInterface? = null
) {
    
    private val commentQueries = db.commentQueries
    
    // ==================== Comment Operations ====================
    
    /**
     * Create a new comment.
     * 
     * @param eventId Event ID
     * @param authorId Author participant ID
     * @param authorName Author display name
     * @param request Comment creation request
     * @return Created Comment
     */
    suspend fun createComment(
        eventId: String,
        authorId: String,
        authorName: String,
        request: CommentRequest
    ): Comment {
        val now = getCurrentUtcIsoString()
        val commentId = generateId()
        
        // Validate parent comment exists if specified
        if (request.parentCommentId != null) {
            val parentCount = commentQueries.commentExists(request.parentCommentId).executeAsOne()
            require(parentCount > 0) { "Parent comment not found: ${request.parentCommentId}" }
        }
        
        val comment = Comment(
            id = commentId,
            eventId = eventId,
            section = request.section,
            sectionItemId = request.sectionItemId,
            authorId = authorId,
            authorName = authorName,
            content = request.content,
            parentCommentId = request.parentCommentId,
            createdAt = now,
            updatedAt = null,
            isEdited = false,
            replyCount = 0
        )
        
        commentQueries.insertComment(
            id = comment.id,
            event_id = comment.eventId,
            section = comment.section.name,
            section_item_id = comment.sectionItemId,
            author_id = comment.authorId,
            author_name = comment.authorName,
            content = comment.content,
            parent_comment_id = comment.parentCommentId,
            created_at = comment.createdAt,
            updated_at = comment.updatedAt,
            is_edited = if (comment.isEdited) 1L else 0L,
            reply_count = comment.replyCount.toLong()
        )
        
        // Increment parent's reply count if this is a reply
        if (request.parentCommentId != null) {
            commentQueries.incrementReplyCount(request.parentCommentId)
        }
        
        // Send notifications if services are available
        if (commentNotificationService != null && eventRepository != null) {
            if (request.parentCommentId == null) {
                // New top-level comment
                commentNotificationService.notifyCommentPosted(
                    eventId = eventId,
                    section = request.section,
                    sectionItemId = request.sectionItemId,
                    authorId = authorId,
                    authorName = authorName,
                    content = request.content,
                    commentId = commentId,
                    excludeRecipient = authorId
                )
            } else {
                // Reply to a comment
                val parentComment = getCommentById(request.parentCommentId!!)
                if (parentComment != null) {
                    commentNotificationService.notifyCommentReply(
                        eventId = eventId,
                        parentComment = parentComment,
                        replyComment = comment,
                        excludeRecipient = authorId
                    )
                }
            }
        }
        
        return comment
    }
    
    /**
     * Get comment by ID.
     */
    fun getCommentById(commentId: String): Comment? {
        return commentQueries.selectCommentById(commentId).executeAsOneOrNull()?.toModel()
    }
    
    /**
     * Get all comments for an event.
     */
    fun getCommentsByEvent(eventId: String): List<Comment> {
        return commentQueries.selectCommentsByEvent(eventId)
            .executeAsList()
            .map { it.toModel() }
    }
    
    /**
     * Get comments by section.
     * 
     * @param eventId Event ID
     * @param section Section to filter by
     * @param sectionItemId Optional item ID within section
     * @return List of comments
     */
    fun getCommentsBySection(
        eventId: String,
        section: CommentSection,
        sectionItemId: String? = null
    ): List<Comment> {
        return if (sectionItemId != null) {
            commentQueries.selectCommentsByEventSectionAndItem(eventId, section.name, sectionItemId)
                .executeAsList()
                .map { it.toModel() }
        } else {
            commentQueries.selectCommentsByEventAndSection(eventId, section.name)
                .executeAsList()
                .map { it.toModel() }
        }
    }
    
    /**
     * Get top-level comments (no parent) for a section.
     * 
     * @param eventId Event ID
     * @param section Section to filter by
     * @param sectionItemId Optional item ID within section
     * @return List of top-level comments
     */
    fun getTopLevelComments(
        eventId: String,
        section: CommentSection? = null,
        sectionItemId: String? = null
    ): List<Comment> {
        return if (section == null) {
            commentQueries.selectTopLevelCommentsByEvent(eventId)
                .executeAsList()
                .map { it.toModel() }
        } else if (sectionItemId != null) {
            commentQueries.selectTopLevelCommentsBySectionAndItem(eventId, section.name, sectionItemId)
                .executeAsList()
                .map { it.toModel() }
        } else {
            commentQueries.selectTopLevelCommentsBySection(eventId, section.name)
                .executeAsList()
                .map { it.toModel() }
        }
    }
    
    /**
     * Get comment thread (parent comment + all replies recursively).
     * 
     * Builds thread by recursively fetching replies.
     * 
     * @param commentId Root comment ID
     * @return CommentThread with all nested replies
     */
    fun getCommentThread(commentId: String): CommentThread? {
        val rootComment = getCommentById(commentId) ?: return null
        val allReplies = getAllRepliesRecursive(commentId)
        
        return CommentThread(
            comment = rootComment,
            replies = allReplies,
            hasMoreReplies = false
        )
    }
    
    /**
     * Recursively fetch all replies for a comment.
     * 
     * @param commentId Parent comment ID
     * @return List of all replies (direct and nested)
     */
    private fun getAllRepliesRecursive(commentId: String): List<Comment> {
        val directReplies = getReplies(commentId)
        val allReplies = mutableListOf<Comment>()
        
        for (reply in directReplies) {
            allReplies.add(reply)
            allReplies.addAll(getAllRepliesRecursive(reply.id))
        }
        
        return allReplies
    }
    
    /**
     * Get direct replies to a comment (non-recursive).
     * 
     * @param parentCommentId Parent comment ID
     * @return List of direct reply comments
     */
    fun getReplies(parentCommentId: String): List<Comment> {
        return commentQueries.selectRepliesByParentComment(parentCommentId)
            .executeAsList()
            .map { it.toModel() }
    }
    
    /**
     * Get comments by author.
     * 
     * @param eventId Event ID
     * @param authorId Author participant ID
     * @return List of comments by this author
     */
    fun getCommentsByAuthor(eventId: String, authorId: String): List<Comment> {
        return commentQueries.selectCommentsByAuthor(eventId, authorId)
            .executeAsList()
            .map { it.toModel() }
    }
    
    /**
     * Get recent comments (after a specific timestamp).
     * 
     * @param eventId Event ID
     * @param since ISO 8601 timestamp
     * @param limit Maximum number of comments
     * @return List of recent comments
     */
    fun getRecentComments(eventId: String, since: String, limit: Int = 50): List<Comment> {
        return commentQueries.selectRecentComments(eventId, since, limit.toLong())
            .executeAsList()
            .map { it.toModel() }
    }
    
    /**
     * Update an existing comment.
     * 
     * Only content can be updated. Sets is_edited flag and updated_at timestamp.
     * 
     * @param commentId Comment ID
     * @param content New content
     * @return Updated Comment
     */
    fun updateComment(commentId: String, content: String): Comment {
        require(content.isNotBlank()) { "Comment content cannot be blank" }
        require(content.length <= 2000) { "Comment content cannot exceed 2000 characters" }
        
        val existing = getCommentById(commentId)
            ?: throw IllegalArgumentException("Comment not found: $commentId")
        
        val now = getCurrentUtcIsoString()
        
        commentQueries.updateComment(
            content = content,
            updated_at = now,
            id = commentId
        )
        
        return existing.copy(
            content = content,
            updatedAt = now,
            isEdited = true
        )
    }
    
    /**
     * Delete a comment.
     * 
     * CASCADE deletes all replies due to foreign key constraint.
     * Decrements parent's reply count if this is a reply.
     * 
     * @param commentId Comment ID
     */
    fun deleteComment(commentId: String) {
        val comment = getCommentById(commentId)
        
        // Decrement parent's reply count if this is a reply
        if (comment?.parentCommentId != null) {
            commentQueries.decrementReplyCount(comment.parentCommentId)
        }
        
        // Delete comment (CASCADE will delete all replies)
        commentQueries.deleteComment(commentId)
    }
    
    /**
     * Delete all comments for an event.
     * 
     * @param eventId Event ID
     */
    fun deleteCommentsByEvent(eventId: String) {
        commentQueries.deleteCommentsByEvent(eventId)
    }
    
    /**
     * Delete all comments for a section.
     * 
     * @param eventId Event ID
     * @param section Section to delete comments from
     */
    fun deleteCommentsBySection(eventId: String, section: CommentSection) {
        commentQueries.deleteCommentsBySection(eventId, section.name)
    }
    
    /**
     * Delete all comments for a specific section item.
     * 
     * @param eventId Event ID
     * @param section Section
     * @param sectionItemId Item ID within section
     */
    fun deleteCommentsBySectionItem(eventId: String, section: CommentSection, sectionItemId: String) {
        commentQueries.deleteCommentsBySectionItem(eventId, section.name, sectionItemId)
    }
    
    // ==================== Thread Building ====================
    
    /**
     * Get comments organized by threads for a section.
     * 
     * @param eventId Event ID
     * @param section Section to filter by
     * @param sectionItemId Optional item ID within section
     * @return CommentsBySection with threaded comments
     */
    fun getCommentsWithThreads(
        eventId: String,
        section: CommentSection,
        sectionItemId: String? = null
    ): CommentsBySection {
        val topLevelComments = getTopLevelComments(eventId, section, sectionItemId)
        
        val threads = topLevelComments.map { comment ->
            val replies = getReplies(comment.id)
            CommentThread(
                comment = comment,
                replies = replies,
                hasMoreReplies = false
            )
        }
        
        val totalComments = countCommentsBySection(eventId, section, sectionItemId)
        
        return CommentsBySection(
            section = section,
            sectionItemId = sectionItemId,
            comments = threads,
            totalComments = totalComments.toInt()
        )
    }
    
    // ==================== Aggregations & Statistics ====================
    
    /**
     * Count all comments for an event.
     */
    fun countCommentsByEvent(eventId: String): Long {
        return commentQueries.countCommentsByEvent(eventId).executeAsOne()
    }
    
    /**
     * Count comments for a section.
     */
    fun countCommentsBySection(
        eventId: String,
        section: CommentSection,
        sectionItemId: String? = null
    ): Long {
        return if (sectionItemId != null) {
            commentQueries.countCommentsBySectionItem(eventId, section.name, sectionItemId).executeAsOne()
        } else {
            commentQueries.countCommentsBySection(eventId, section.name).executeAsOne()
        }
    }
    
    /**
     * Count comments by author.
     */
    fun countCommentsByAuthor(eventId: String, authorId: String): Long {
        return commentQueries.countCommentsByAuthor(eventId, authorId).executeAsOne()
    }
    
    /**
     * Count replies to a comment.
     */
    fun countRepliesByParent(parentCommentId: String): Long {
        return commentQueries.countRepliesByParent(parentCommentId).executeAsOne()
    }
    
    /**
     * Get comment statistics grouped by section.
     * 
     * @param eventId Event ID
     * @return Map of section to comment count
     */
    fun getCommentStatsBySection(eventId: String): Map<CommentSection, Int> {
        return commentQueries.selectCommentStatsBySection(eventId)
            .executeAsList()
            .associate { 
                CommentSection.valueOf(it.section) to it.commentCount.toInt()
            }
    }
    
    /**
     * Get top contributors for an event.
     * 
     * @param eventId Event ID
     * @param limit Maximum number of contributors
     * @return List of (authorId, authorName, commentCount)
     */
    fun getTopContributors(eventId: String, limit: Int = 10): List<Triple<String, String, Int>> {
        return commentQueries.selectTopContributors(eventId, limit.toLong())
            .executeAsList()
            .map { Triple(it.author_id, it.author_name, it.commentCount.toInt()) }
    }
    
    /**
     * Count recent activity (last 24 hours).
     * 
     * @param eventId Event ID
     * @param since ISO 8601 timestamp
     * @return Number of comments since timestamp
     */
    fun countRecentActivity(eventId: String, since: String): Long {
        return commentQueries.countRecentActivity(eventId, since).executeAsOne()
    }
    
    /**
     * Get participant activity statistics.
     * 
     * @param eventId Event ID
     * @param participantId Participant ID
     * @return ParticipantCommentActivity
     */
    fun getParticipantActivity(eventId: String, participantId: String): ParticipantCommentActivity? {
        val activity = commentQueries.selectParticipantActivity(eventId, participantId)
            .executeAsOneOrNull() ?: return null
        
        return ParticipantCommentActivity(
            participantId = activity.author_id,
            participantName = activity.author_name,
            commentCount = activity.totalComments.toInt(),
            replyCount = activity.replyCount?.toInt() ?: 0,
            lastCommentAt = activity.lastCommentAt
        )
    }
    
    /**
     * Get comprehensive comment statistics for an event.
     * 
     * @param eventId Event ID
     * @return CommentStatistics
     */
    fun getCommentStatistics(eventId: String): CommentStatistics {
        val totalComments = countCommentsByEvent(eventId).toInt()
        val commentsBySection = getCommentStatsBySection(eventId)
        val topContributors = getTopContributors(eventId, 5)
            .map { (authorId, _, count) -> authorId to count }
        
        // Calculate "24 hours ago" timestamp
        val twentyFourHoursAgo = Clock.System.now()
            .minus(24, DateTimeUnit.HOUR)
            .toString()
        val recentActivity = countRecentActivity(eventId, twentyFourHoursAgo).toInt()
        
        return CommentStatistics(
            eventId = eventId,
            totalComments = totalComments,
            commentsBySection = commentsBySection,
            topContributors = topContributors,
            recentActivity = recentActivity
        )
    }
    
    /**
     * Check if a comment exists.
     */
    fun commentExists(commentId: String): Boolean {
        val count = commentQueries.commentExists(commentId).executeAsOne()
        return count > 0
    }
    
    // ==================== Helper Methods ====================
    
    private fun getCurrentUtcIsoString(): String {
        return Clock.System.now().toString()
    }
    
    private fun generateId(): String {
        val chars = "0123456789abcdef"
        return buildString(36) {
            repeat(36) { i ->
                when (i) {
                    8, 13, 18, 23 -> append('-')
                    14 -> append('4') // UUID version 4
                    19 -> append(chars[Random.nextInt(4) + 8]) // 8, 9, a, or b
                    else -> append(chars[Random.nextInt(16)])
                }
            }
        }
    }
    
    /**
     * Convert SQL Comment entity to Kotlin model.
     */
    private fun com.guyghost.wakeve.Comment.toModel(): Comment {
        return Comment(
            id = this.id,
            eventId = this.event_id,
            section = CommentSection.valueOf(this.section),
            sectionItemId = this.section_item_id,
            authorId = this.author_id,
            authorName = this.author_name,
            content = this.content,
            parentCommentId = this.parent_comment_id,
            createdAt = this.created_at,
            updatedAt = this.updated_at,
            isEdited = this.is_edited == 1L,
            replyCount = this.reply_count.toInt()
        )
    }
}
