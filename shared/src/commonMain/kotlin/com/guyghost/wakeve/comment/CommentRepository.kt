package com.guyghost.wakeve.comment

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.collaboration.MentionParser
import com.guyghost.wakeve.models.Comment
import com.guyghost.wakeve.models.CommentRequest
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.models.CommentStatistics
import com.guyghost.wakeve.models.CommentThread
import com.guyghost.wakeve.models.CommentsBySection
import com.guyghost.wakeve.models.ParticipantCommentActivity
import com.guyghost.wakeve.models.Mention
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Data class for paginated results with metadata
 */
data class PagingData<T>(
    val items: List<T>,
    val hasMore: Boolean,
    val nextOffset: Int? = null
)

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
    private val db: WakeveDb,
    private val commentNotificationService: CommentNotificationService? = null,
    private val eventRepository: EventRepositoryInterface? = null,
    private val cache: CommentCache = CommentCache()
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
     * @param usernameToUserIdMap Map of username to user ID for mention resolution
     * @return Created Comment
     */
    suspend fun createComment(
        eventId: String,
        authorId: String,
        authorName: String,
        request: CommentRequest,
        usernameToUserIdMap: Map<String, String> = emptyMap()
    ): Comment {
        val now = getCurrentUtcIsoString()
        val commentId = generateId()

        // Validate parent comment exists if specified
        if (request.parentCommentId != null) {
            val parentCount = commentQueries.commentExists(request.parentCommentId).executeAsOne()
            require(parentCount > 0) { "Parent comment not found: ${request.parentCommentId}" }
        }

        // Parse mentions from content
        val mentionParseResult = MentionParser.parseMentions(
            commentId = commentId,
            content = request.content,
            usernameToUserIdMap = usernameToUserIdMap
        )

        val mentionsJson = if (mentionParseResult.mentionedUserIds.isNotEmpty()) {
            Json.encodeToString(
                ListSerializer(String.serializer()),
                mentionParseResult.mentionedUserIds
            )
        } else {
            null
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
            mentions = mentionParseResult.mentionedUserIds,
            isDeleted = false,
            isPinned = false,
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
            mentions = mentionsJson,
            is_deleted = 0L,
            is_pinned = 0L,
            created_at = comment.createdAt,
            updated_at = comment.updatedAt,
            is_edited = if (comment.isEdited) 1L else 0L,
            reply_count = comment.replyCount.toLong()
        )

        // Store mentions in separate table for efficient lookup
        mentionParseResult.mentions.forEach { mention ->
            commentQueries.insertMention(
                id = mention.id,
                comment_id = mention.commentId,
                mentioned_user_id = mention.mentionedUserId,
                start_index = mention.startIndex.toLong(),
                end_index = mention.endIndex.toLong()
            )
        }

        // Increment parent's reply count if this is a reply
        if (request.parentCommentId != null) {
            commentQueries.incrementReplyCount(request.parentCommentId)
        }

        // Send notifications if services are available
        if (commentNotificationService != null && eventRepository != null) {
            // Send mention notifications
            mentionParseResult.mentionedUserIds.forEach { mentionedUserId ->
                commentNotificationService.notifyMention(
                    eventId = eventId,
                    mentionedUserId = mentionedUserId,
                    authorId = authorId,
                    authorName = authorName,
                    content = request.content,
                    commentId = commentId,
                    excludeRecipient = authorId
                )
            }

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

        // Invalidate cache for the event
        invalidateEventCache(eventId)

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
    
    // ==================== PAGINATION METHODS ====================
    
    /**
     * Get paginated top-level comments for an event.
     * 
     * @param eventId Event ID
     * @param limit Maximum number of comments per page
     * @param offset Pagination offset
     * @return Paginated result
     */
    fun getTopLevelCommentsByEventPaginated(
        eventId: String,
        limit: Int = 20,
        offset: Int = 0
    ): PagingData<Comment> {
        val comments = commentQueries.selectTopLevelCommentsByEventPaginated(
            eventId,
            limit.toLong(),
            offset.toLong()
        ).executeAsList().map { it.toModel() }
        
        // Check if there are more results
        val totalCount = countCommentsByEvent(eventId).toInt()
        val hasMore = offset + limit < totalCount
        
        return PagingData(
            items = comments,
            hasMore = hasMore,
            nextOffset = if (hasMore) offset + limit else null
        )
    }
    
    /**
     * Get paginated top-level comments for a section.
     * 
     * @param eventId Event ID
     * @param section Section to filter by
     * @param limit Maximum number of comments per page
     * @param offset Pagination offset
     * @return Paginated result
     */
    fun getTopLevelCommentsBySectionPaginated(
        eventId: String,
        section: CommentSection,
        limit: Int = 20,
        offset: Int = 0
    ): PagingData<Comment> {
        val comments = commentQueries.selectTopLevelCommentsBySectionPaginated(
            eventId,
            section.name,
            limit.toLong(),
            offset.toLong()
        ).executeAsList().map { it.toModel() }
        
        // Check if there are more results
        val totalCount = countCommentsBySection(eventId, section).toInt()
        val hasMore = offset + limit < totalCount
        
        return PagingData(
            items = comments,
            hasMore = hasMore,
            nextOffset = if (hasMore) offset + limit else null
        )
    }
    
    /**
     * Get paginated top-level comments for a section and item.
     * 
     * @param eventId Event ID
     * @param section Section to filter by
     * @param sectionItemId Item ID within section
     * @param limit Maximum number of comments per page
     * @param offset Pagination offset
     * @return Paginated result
     */
    fun getTopLevelCommentsBySectionAndItemPaginated(
        eventId: String,
        section: CommentSection,
        sectionItemId: String,
        limit: Int = 20,
        offset: Int = 0
    ): PagingData<Comment> {
        val comments = commentQueries.selectTopLevelCommentsBySectionAndItemPaginated(
            eventId,
            section.name,
            sectionItemId,
            limit.toLong(),
            offset.toLong()
        ).executeAsList().map { it.toModel() }
        
        // Check if there are more results
        val totalCount = countCommentsBySection(eventId, section, sectionItemId).toInt()
        val hasMore = offset + limit < totalCount
        
        return PagingData(
            items = comments,
            hasMore = hasMore,
            nextOffset = if (hasMore) offset + limit else null
        )
    }
    
    // ==================== CACHE-AWARE METHODS ====================
    
    /**
     * Get comments by event with optional caching.
     * 
     * @param eventId Event ID
     * @param useCache Whether to use cache (default: true)
     * @return List of comments
     */
    fun getCommentsByEventCached(eventId: String, useCache: Boolean = true): List<Comment> {
        val cacheKey = "event:$eventId:comments"
        
        if (useCache) {
            cache.get(cacheKey)?.let { return it.comments }
        }
        
        val comments = commentQueries.selectCommentsByEvent(eventId)
            .executeAsList()
            .map { it.toModel() }
        val totalCount = countCommentsByEvent(eventId).toInt()
        
        cache.put(cacheKey, CommentCache.CommentListResult(comments, totalCount))
        return comments
    }
    
    /**
     * Get comments by section with optional caching.
     * 
     * @param eventId Event ID
     * @param section Section to filter by
     * @param sectionItemId Optional item ID within section
     * @param useCache Whether to use cache (default: true)
     * @return List of comments
     */
    fun getCommentsBySectionCached(
        eventId: String,
        section: CommentSection,
        sectionItemId: String? = null,
        useCache: Boolean = true
    ): List<Comment> {
        val cacheKey = "event:$eventId:section:${section.name}:${sectionItemId ?: "all"}"
        
        if (useCache) {
            cache.get(cacheKey)?.let { return it.comments }
        }
        
        val comments = getCommentsBySection(eventId, section, sectionItemId)
        val totalCount = countCommentsBySection(eventId, section, sectionItemId).toInt()
        
        cache.put(cacheKey, CommentCache.CommentListResult(comments, totalCount))
        return comments
    }
    
    // ==================== LAZY LOADING METHODS ====================
    
    /**
     * Get comments with lazy-loaded threads.
     * 
     * @param eventId Event ID
     * @param section Section to filter by
     * @param sectionItemId Optional item ID within section
     * @param loadReplies Whether to load replies immediately (default: true for backward compatibility)
     * @param useCache Whether to use cache (default: true)
     * @return CommentsBySection with potentially lazy-loaded replies
     */
    fun getCommentsWithThreadsLazy(
        eventId: String,
        section: CommentSection,
        sectionItemId: String? = null,
        loadReplies: Boolean = true,
        useCache: Boolean = true
    ): CommentsBySection {
        val topLevelComments = getTopLevelComments(eventId, section, sectionItemId)
        
        val threads = topLevelComments.map { comment ->
            CommentThread(
                comment = comment,
                replies = if (loadReplies) {
                    getReplies(comment.id)
                } else {
                    emptyList()
                },
                hasMoreReplies = if (!loadReplies && comment.replyCount > 0) {
                    true // Indicate there are replies to load
                } else {
                    false
                }
            )
        }
        
        val totalComments = countCommentsBySection(eventId, section, sectionItemId).toInt()
        
        return CommentsBySection(
            section = section,
            sectionItemId = sectionItemId,
            comments = threads,
            totalComments = totalComments
        )
    }
    
    /**
     * Load replies for a specific comment thread.
     * 
     * Useful for lazy loading replies in UI.
     * 
     * @param commentId Comment ID to load replies for
     * @return List of reply comments
     */
    fun loadRepliesForComment(commentId: String): List<Comment> {
        return getReplies(commentId)
    }
    
    // ==================== CACHE MANAGEMENT ====================
    
    /**
     * Invalidate cache for an event.
     * 
     * Call this after creating, updating, or deleting comments.
     * 
     * @param eventId Event ID
     */
    fun invalidateEventCache(eventId: String) {
        cache.invalidate(eventId)
    }
    
    /**
     * Invalidate cache for a specific comment.
     * 
     * @param commentId Comment ID
     */
    fun invalidateCommentCache(commentId: String) {
        cache.invalidateComment(commentId)
    }
    
    /**
     * Clear all cache.
     */
    fun clearCache() {
        cache.clear()
    }
    
    /**
     * Get cache statistics for monitoring.
     */
    fun getCacheStats(): CommentCache.CacheStats {
        return cache.getStats()
    }
    
    // ==================== PRE-CALCULATED STATISTICS ====================
    
    /**
     * Get comment section statistics from pre-calculated view.
     * 
     * Much faster than calculating on-the-fly for large datasets.
     * 
     * @param eventId Event ID
     * @return Map of section to statistics
     */
    fun getCommentSectionStats(eventId: String): Map<CommentSection, CommentSectionStats> {
        return commentQueries.selectCommentSectionStats(eventId)
            .executeAsList()
            .associate { 
                val section = CommentSection.valueOf(it.section)
                section to CommentSectionStats(
                    commentCount = it.comment_count.toInt(),
                    uniqueAuthors = it.unique_authors.toInt(),
                    lastCommentAt = it.last_comment_at
                )
            }
    }
    
    /**
     * Data class for section statistics
     */
    data class CommentSectionStats(
        val commentCount: Int,
        val uniqueAuthors: Int,
        val lastCommentAt: String?
    )
    
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
     * Update a comment.
     */
    fun updateComment(commentId: String, content: String): Comment? {
        val now = getCurrentUtcIsoString()
        commentQueries.updateComment(content, now, commentId)
        
        val updatedComment = getCommentById(commentId)
        if (updatedComment != null) {
            invalidateEventCache(updatedComment.eventId)
            invalidateCommentCache(commentId)
        }
        
        return updatedComment
    }

    /**
     * Delete a comment.
     */
    fun deleteComment(commentId: String) {
        val comment = getCommentById(commentId)
        if (comment != null) {
            // Update parent reply count if this was a reply
            if (comment.parentCommentId != null) {
                commentQueries.decrementReplyCount(comment.parentCommentId)
            }

            commentQueries.deleteComment(commentId)
            invalidateEventCache(comment.eventId)
            invalidateCommentCache(commentId)
        }
    }

    /**
     * Soft delete a comment.
     *
     * Marks comment as deleted without actually removing it from database.
     * Display shows "[Deleted]" placeholder.
     *
     * @param commentId Comment ID to soft delete
     * @return Updated comment or null if not found
     */
    fun softDeleteComment(commentId: String): Comment? {
        val comment = getCommentById(commentId) ?: return null
        val now = getCurrentUtcIsoString()

        commentQueries.softDeleteComment(now, commentId)

        // Invalidate caches
        invalidateEventCache(comment.eventId)
        invalidateCommentCache(commentId)

        // Return updated comment
        return comment.copy(
            isDeleted = true,
            content = "[Deleted]",
            updatedAt = now
        )
    }

    /**
     * Restore a soft-deleted comment.
     *
     * @param commentId Comment ID to restore
     * @return Restored comment or null if not found
     */
    fun restoreComment(commentId: String): Comment? {
        val comment = getCommentById(commentId) ?: return null
        val now = getCurrentUtcIsoString()

        commentQueries.restoreComment(now, commentId)

        // Invalidate caches
        invalidateEventCache(comment.eventId)
        invalidateCommentCache(commentId)

        // Return restored comment (original content would need to be stored separately)
        return comment.copy(
            isDeleted = false,
            updatedAt = now
        )
    }

    /**
     * Pin a comment.
     *
     * Only organizers can pin comments.
     * Pinned comments appear first in the list.
     *
     * @param commentId Comment ID to pin
     * @return Updated comment or null if not found
     */
    fun pinComment(commentId: String): Comment? {
        val comment = getCommentById(commentId) ?: return null

        commentQueries.pinComment(commentId)

        // Invalidate caches
        invalidateEventCache(comment.eventId)
        invalidateCommentCache(commentId)

        return comment.copy(isPinned = true)
    }

    /**
     * Unpin a comment.
     *
     * @param commentId Comment ID to unpin
     * @return Updated comment or null if not found
     */
    fun unpinComment(commentId: String): Comment? {
        val comment = getCommentById(commentId) ?: return null

        commentQueries.unpinComment(commentId)

        // Invalidate caches
        invalidateEventCache(comment.eventId)
        invalidateCommentCache(commentId)

        return comment.copy(isPinned = false)
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
        // Parse mentions JSON if present
        val mentions = this.mentions?.let { mentionsJson ->
            try {
                Json.decodeFromString<List<String>>(mentionsJson)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        return Comment(
            id = this.id,
            eventId = this.event_id,
            section = CommentSection.valueOf(this.section),
            sectionItemId = this.section_item_id,
            authorId = this.author_id,
            authorName = this.author_name,
            content = this.content,
            parentCommentId = this.parent_comment_id,
            mentions = mentions,
            isDeleted = (this.is_deleted ?: 0L) == 1L,
            isPinned = (this.is_pinned ?: 0L) == 1L,
            createdAt = this.created_at,
            updatedAt = this.updated_at,
            isEdited = this.is_edited == 1L,
            replyCount = this.reply_count.toInt()
        )
    }
}
