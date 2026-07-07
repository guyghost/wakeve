package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.userId
import com.guyghost.wakeve.comment.CommentRepository
import com.guyghost.wakeve.models.CommentRequest
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.models.Comment
import com.guyghost.wakeve.models.CommentStatistics
import com.guyghost.wakeve.models.CommentThread
import com.guyghost.wakeve.models.CommentsBySection
import com.guyghost.wakeve.moderation.ModerationRejectedException
import com.guyghost.wakeve.moderation.ModerationRepository
import com.guyghost.wakeve.moderation.ModerationStatus
import com.guyghost.wakeve.notification.EventNotificationTrigger
import com.guyghost.wakeve.repository.DatabaseEventRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.serialization.Serializable

/**
 * Comment API Routes
 *
 * Provides RESTful endpoints for comment management including:
 * - Creating comments and replies
 * - Retrieving comments with filtering and pagination
 * - Threaded comment display
 * - Comment statistics and analytics
 * - Top contributor tracking
 */
fun io.ktor.server.routing.Route.commentRoutes(
    repository: CommentRepository,
    eventNotificationTrigger: EventNotificationTrigger? = null,
    eventRepository: DatabaseEventRepository? = null,
    moderationRepository: ModerationRepository? = null
) {
    route("/events/{eventId}/comments") {

        // POST /api/events/{eventId}/comments - Create a new comment
        post {
            try {
                val eventId = call.parameters["eventId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val request = call.receive<CreateCommentRequest>()
                val principal = call.principal<JWTPrincipal>()
                val authorId = bindCommentAuthorToAuthenticatedUser(
                    requestedAuthorId = request.authorId,
                    authenticatedUserId = principal?.userId
                ).getOrElse { error ->
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to commentAuthorForbiddenMessage())
                    )
                }
                val authorName = resolveAuthenticatedCommentAuthorName(
                    authenticatedUserName = principal?.payload?.getClaim("userName")?.asString(),
                    authenticatedEmail = principal?.payload?.getClaim("email")?.asString(),
                    authenticatedUserId = authorId
                )

                val commentRequest = CommentRequest(
                    section = request.section,
                    sectionItemId = request.sectionItemId,
                    content = request.content,
                    parentCommentId = request.parentCommentId
                )

                val comment = repository.createComment(
                    eventId = eventId,
                    authorId = authorId,
                    authorName = authorName,
                    request = commentRequest
                )

                val status = if (comment.moderationStatus == ModerationStatus.PENDING_REVIEW) {
                    HttpStatusCode.Accepted
                } else {
                    // Trigger notification only for comments that are visible immediately.
                    eventNotificationTrigger?.onNewComment(
                        eventId = eventId,
                        authorId = authorId,
                        authorName = authorName,
                        commentPreview = request.content
                    )
                    HttpStatusCode.Created
                }
                call.respond(status, comment)
            } catch (e: ModerationRejectedException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.result.userMessage, "reasonCode" to e.result.reasonCode)
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to commentCreateFailureMessage())
                )
            }
        }

        // GET /api/events/{eventId}/comments - Get comments with optional filters
        get {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val section = call.request.queryParameters["section"]?.let { CommentSection.valueOf(it) }
                val sectionItemId = call.request.queryParameters["sectionItemId"]
                val threaded = call.request.queryParameters["threaded"]?.toBoolean() ?: true
                val limit = parseCommentListLimit(call.request.queryParameters["limit"])
                val offset = parseCommentOffset(call.request.queryParameters["offset"])
                val viewerUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

                if (section != null) {
                    if (threaded) {
                        val comments = repository.getCommentsWithThreads(eventId, section, sectionItemId)
                            .filterBlockedCommentsBySection(eventId, viewerUserId, moderationRepository)
                            .paginate(offset, limit)
                        call.respond(HttpStatusCode.OK, comments)
                    } else {
                        val comments = repository.getCommentsBySection(eventId, section, sectionItemId)
                            .filterBlockedComments(eventId, viewerUserId, moderationRepository)
                            .paginate(offset, limit)
                        call.respond(HttpStatusCode.OK, comments)
                    }
                } else {
                    val comments = repository.getCommentsByEvent(eventId)
                        .filterBlockedComments(eventId, viewerUserId, moderationRepository)
                        .paginate(offset, limit)
                    call.respond(HttpStatusCode.OK, comments)
                }
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to commentListInvalidRequestMessage())
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to commentListFailureMessage())
                )
            }
        }

        // GET /api/events/{eventId}/comments/{commentId} - Get specific comment
        get("/{commentId}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val commentId = call.parameters["commentId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Comment ID required")
                )

                val comment = repository.getCommentById(commentId)

                if (comment == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Comment not found")
                    )
                } else if (comment.eventId != eventId) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Comment not found in this event")
                    )
                } else if (comment.isBlockedForViewer(
                        viewerUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString(),
                        eventId = eventId,
                        moderationRepository = moderationRepository
                    )
                ) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Comment not found")
                    )
                } else {
                    call.respond(HttpStatusCode.OK, comment)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to commentDetailFailureMessage())
                )
            }
        }

        // PUT /api/events/{eventId}/comments/{commentId} - Update comment
        put("/{commentId}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val commentId = call.parameters["commentId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Comment ID required")
                )

                val request = call.receive<UpdateCommentRequest>()

                val existingComment = repository.getCommentById(commentId)

                if (existingComment == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Comment not found")
                    )
                    return@put
                }

                if (existingComment.eventId != eventId) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Comment not found in this event")
                    )
                    return@put
                }

                val currentUserId = call.principal<JWTPrincipal>()?.userId
                if (!isCommentAuthor(existingComment, currentUserId)) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the comment author can update comments")
                    )
                    return@put
                }

                val updatedComment = repository.updateComment(commentId, request.content)
                if (updatedComment != null) {
                    call.respond(HttpStatusCode.OK, updatedComment)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comment not found"))
                }
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to commentUpdateInvalidRequestMessage())
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to commentUpdateFailureMessage())
                )
            }
        }

        // DELETE /api/events/{eventId}/comments/{commentId} - Delete comment
        delete("/{commentId}") {
            try {
                val eventId = call.parameters["eventId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val commentId = call.parameters["commentId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Comment ID required")
                )

                val existingComment = repository.getCommentById(commentId)

                if (existingComment == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Comment not found")
                    )
                    return@delete
                }

                if (existingComment.eventId != eventId) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Comment not found in this event")
                    )
                    return@delete
                }

                val currentUserId = call.principal<JWTPrincipal>()?.userId
                val event = eventRepository?.getEvent(eventId)
                val canDelete = isCommentAuthor(existingComment, currentUserId) ||
                    isEventOrganizer(event?.organizerId, currentUserId)

                if (!canDelete) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the organizer or the comment author can delete comments")
                    )
                    return@delete
                }

                repository.deleteComment(commentId)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to commentDeleteFailureMessage())
                )
            }
        }

        // GET /api/events/{eventId}/comments/statistics - Get comment statistics
        get("/statistics") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val viewerUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                val statistics = repository.getCommentStatistics(eventId)
                    .filterBlockedStatistics(
                        comments = repository.getCommentsByEvent(eventId),
                        eventId = eventId,
                        viewerUserId = viewerUserId,
                        moderationRepository = moderationRepository
                    )
                call.respond(HttpStatusCode.OK, statistics)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to commentStatisticsFailureMessage())
                )
            }
        }

        // GET /api/events/{eventId}/comments/top-contributors - Get top contributors
        get("/top-contributors") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val limit = parseCommentContributorLimit(call.request.queryParameters["limit"])
                val viewerUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                val contributors = repository.getTopContributors(eventId, limit)
                    .filterBlockedContributors(eventId, viewerUserId, moderationRepository)

                // Convert to response format
                val response = contributors.map { (authorId, authorName, count) ->
                    mapOf(
                        "authorId" to authorId,
                        "authorName" to authorName,
                        "commentCount" to count
                    )
                }

                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to commentTopContributorsFailureMessage())
                )
            }
        }

        // GET /api/events/{eventId}/comments/recent - Get recent comments
        get("/recent") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val since = call.request.queryParameters["since"]
                val limit = parseRecentCommentLimit(call.request.queryParameters["limit"])

                if (since == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Parameter 'since' (ISO 8601 timestamp) is required")
                    )
                    return@get
                }

                val viewerUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                val comments = repository.getRecentComments(eventId, since, limit)
                    .filterBlockedComments(eventId, viewerUserId, moderationRepository)
                call.respond(HttpStatusCode.OK, comments)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to recentCommentsFailureMessage())
                )
            }
        }

        // POST /api/events/{eventId}/comments/{commentId}/pin - Pin comment (organizer only)
        post("/{commentId}/pin") {
            try {
                val eventId = call.parameters["eventId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val commentId = call.parameters["commentId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Comment ID required")
                )

                val existingComment = repository.getCommentById(commentId)

                if (existingComment == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Comment not found")
                    )
                    return@post
                }

                if (existingComment.eventId != eventId) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Comment not found in this event")
                    )
                    return@post
                }

                // Check if caller is the event organizer
                val currentUserId = call.principal<JWTPrincipal>()?.userId
                val event = eventRepository?.getEvent(eventId)
                val isOrganizer = currentUserId != null && event?.organizerId == currentUserId

                if (!isOrganizer) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the event organizer can pin comments")
                    )
                    return@post
                }

                val pinnedComment = repository.pinComment(commentId)
                if (pinnedComment != null) {
                    call.respond(HttpStatusCode.OK, pinnedComment)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comment not found"))
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to commentPinFailureMessage())
                )
            }
        }

        // DELETE /api/events/{eventId}/comments/{commentId}/pin - Unpin comment (organizer only)
        delete("/{commentId}/pin") {
            try {
                val eventId = call.parameters["eventId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val commentId = call.parameters["commentId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Comment ID required")
                )

                val existingComment = repository.getCommentById(commentId)

                if (existingComment == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Comment not found")
                    )
                    return@delete
                }

                if (existingComment.eventId != eventId) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Comment not found in this event")
                    )
                    return@delete
                }

                // Check if caller is the event organizer
                val currentUserId = call.principal<JWTPrincipal>()?.userId
                val event = eventRepository?.getEvent(eventId)
                val isOrganizer = currentUserId != null && event?.organizerId == currentUserId

                if (!isOrganizer) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the event organizer can unpin comments")
                    )
                    return@delete
                }

                val unpinnedComment = repository.unpinComment(commentId)
                if (unpinnedComment != null) {
                    call.respond(HttpStatusCode.OK, unpinnedComment)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comment not found"))
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to commentUnpinFailureMessage())
                )
            }
        }

        // POST /api/events/{eventId}/comments/{commentId}/restore - Restore soft-deleted comment
        post("/{commentId}/restore") {
            try {
                val eventId = call.parameters["eventId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val commentId = call.parameters["commentId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Comment ID required")
                )

                val existingComment = repository.getCommentById(commentId)

                if (existingComment == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Comment not found")
                    )
                    return@post
                }

                if (existingComment.eventId != eventId) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Comment not found in this event")
                    )
                    return@post
                }

                // Check if caller is the organizer or the comment author
                val currentUserId = call.principal<JWTPrincipal>()?.userId
                val event = eventRepository?.getEvent(eventId)
                val isOrganizer = currentUserId != null && event?.organizerId == currentUserId
                val isAuthor = currentUserId != null && existingComment.authorId == currentUserId

                if (!isOrganizer && !isAuthor) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the organizer or the comment author can restore comments")
                    )
                    return@post
                }

                val restoredComment = repository.restoreComment(commentId)
                if (restoredComment != null) {
                    call.respond(HttpStatusCode.OK, restoredComment)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comment not found"))
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to commentRestoreFailureMessage())
                )
            }
        }

        // GET /api/events/{eventId}/comments/sections - Get statistics by section
        get("/sections") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val viewerUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                val statsBySection = repository.getCommentsByEvent(eventId)
                    .filterBlockedComments(eventId, viewerUserId, moderationRepository)
                    .groupingBy(Comment::section)
                    .eachCount()
                call.respond(HttpStatusCode.OK, statsBySection)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to commentSectionsFailureMessage())
                )
            }
        }
    }
}

private fun List<Comment>.filterBlockedComments(
    eventId: String,
    viewerUserId: String?,
    moderationRepository: ModerationRepository?
): List<Comment> {
    if (viewerUserId == null || moderationRepository == null) return this
    return filterNot { comment -> moderationRepository.isBlockedForEvent(viewerUserId, comment.authorId, eventId) }
}

private fun Comment.isBlockedForViewer(
    eventId: String,
    viewerUserId: String?,
    moderationRepository: ModerationRepository?
): Boolean =
    viewerUserId != null &&
        moderationRepository != null &&
        moderationRepository.isBlockedForEvent(viewerUserId, authorId, eventId)

private fun List<Triple<String, String, Int>>.filterBlockedContributors(
    eventId: String,
    viewerUserId: String?,
    moderationRepository: ModerationRepository?
): List<Triple<String, String, Int>> {
    if (viewerUserId == null || moderationRepository == null) return this
    return filterNot { (authorId, _, _) -> moderationRepository.isBlockedForEvent(viewerUserId, authorId, eventId) }
}

private fun CommentStatistics.filterBlockedStatistics(
    comments: List<Comment>,
    eventId: String,
    viewerUserId: String?,
    moderationRepository: ModerationRepository?
): CommentStatistics {
    val visibleComments = comments.filterBlockedComments(eventId, viewerUserId, moderationRepository)
    if (visibleComments.size == comments.size) return this

    val recentThreshold = Clock.System.now()
        .minus(24, DateTimeUnit.HOUR)
        .toString()
    val visibleTopContributors = visibleComments
        .groupingBy(Comment::authorId)
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(5)
        .map { it.key to it.value }

    return copy(
        totalComments = visibleComments.size,
        commentsBySection = visibleComments.groupingBy(Comment::section).eachCount(),
        topContributors = visibleTopContributors,
        recentActivity = visibleComments.count { it.createdAt > recentThreshold }
    )
}

private fun List<CommentThread>.filterBlockedThreads(
    eventId: String,
    viewerUserId: String?,
    moderationRepository: ModerationRepository?
): List<CommentThread> {
    if (viewerUserId == null || moderationRepository == null) return this
    return mapNotNull { thread ->
        if (moderationRepository.isBlockedForEvent(viewerUserId, thread.comment.authorId, eventId)) {
            null
        } else {
            thread.copy(replies = thread.replies.filterBlockedComments(eventId, viewerUserId, moderationRepository))
        }
    }
}

private fun CommentsBySection.filterBlockedCommentsBySection(
    eventId: String,
    viewerUserId: String?,
    moderationRepository: ModerationRepository?
): CommentsBySection {
    val visibleThreads = comments.filterBlockedThreads(eventId, viewerUserId, moderationRepository)
    return copy(
        comments = visibleThreads,
        totalComments = visibleThreads.sumOf { 1 + it.replies.size }
    )
}

private fun <T> List<T>.paginate(offset: Int, limit: Int): List<T> =
    drop(offset).take(limit)

private fun CommentsBySection.paginate(offset: Int, limit: Int): CommentsBySection {
    val visibleThreads = comments.paginate(offset, limit)
    return copy(
        comments = visibleThreads,
        totalComments = visibleThreads.sumOf { 1 + it.replies.size }
    )
}

private fun isCommentAuthor(comment: Comment, currentUserId: String?): Boolean =
    currentUserId != null && comment.authorId == currentUserId

private fun isEventOrganizer(organizerId: String?, currentUserId: String?): Boolean =
    currentUserId != null && organizerId == currentUserId

internal fun parseCommentListLimit(rawLimit: String?): Int =
    parseBoundedCommentLimit(
        rawLimit = rawLimit,
        defaultLimit = DEFAULT_COMMENT_LIST_LIMIT,
        maxLimit = MAX_COMMENT_LIST_LIMIT
    )

internal fun parseCommentContributorLimit(rawLimit: String?): Int =
    parseBoundedCommentLimit(
        rawLimit = rawLimit,
        defaultLimit = DEFAULT_COMMENT_CONTRIBUTOR_LIMIT,
        maxLimit = MAX_COMMENT_CONTRIBUTOR_LIMIT
    )

internal fun parseRecentCommentLimit(rawLimit: String?): Int =
    parseBoundedCommentLimit(
        rawLimit = rawLimit,
        defaultLimit = DEFAULT_RECENT_COMMENT_LIMIT,
        maxLimit = MAX_RECENT_COMMENT_LIMIT
    )

internal fun parseCommentOffset(rawOffset: String?): Int {
    val parsedOffset = rawOffset?.trim()?.toIntOrNull() ?: DEFAULT_COMMENT_OFFSET
    return parsedOffset.coerceIn(MIN_COMMENT_OFFSET, MAX_COMMENT_OFFSET)
}

internal fun bindCommentAuthorToAuthenticatedUser(
    requestedAuthorId: String,
    authenticatedUserId: String?
): Result<String> {
    val normalizedAuthenticatedUserId = authenticatedUserId?.trim().orEmpty()
    val normalizedRequestedAuthorId = requestedAuthorId.trim()

    return when {
        normalizedAuthenticatedUserId.isBlank() -> Result.failure(
            IllegalArgumentException("Missing userId in token")
        )
        normalizedRequestedAuthorId.isBlank() -> Result.success(normalizedAuthenticatedUserId)
        normalizedRequestedAuthorId == normalizedAuthenticatedUserId -> Result.success(normalizedAuthenticatedUserId)
        else -> Result.failure(
            IllegalArgumentException("Cannot create comments for another user")
        )
    }
}

internal fun resolveAuthenticatedCommentAuthorName(
    authenticatedUserName: String?,
    authenticatedEmail: String?,
    authenticatedUserId: String
): String {
    val normalizedUserName = authenticatedUserName?.trim().orEmpty()
    if (normalizedUserName.isNotBlank()) {
        return normalizedUserName
    }

    val normalizedEmail = authenticatedEmail?.trim().orEmpty()
    val emailPrefix = normalizedEmail.substringBefore("@").takeIf { it.isNotBlank() }
    return emailPrefix ?: authenticatedUserId
}

internal fun commentAuthorForbiddenMessage(): String =
    "You are not allowed to create this comment."

internal fun commentCreateFailureMessage(): String =
    "Failed to create the comment. Please try again."

internal fun commentListInvalidRequestMessage(): String =
    "Invalid comment list request."

internal fun commentListFailureMessage(): String =
    "Failed to fetch comments. Please try again."

internal fun commentDetailFailureMessage(): String =
    "Failed to fetch comment details. Please try again."

internal fun commentUpdateInvalidRequestMessage(): String =
    "Invalid comment update request."

internal fun commentUpdateFailureMessage(): String =
    "Failed to update the comment. Please try again."

internal fun commentDeleteFailureMessage(): String =
    "Failed to delete the comment. Please try again."

internal fun commentStatisticsFailureMessage(): String =
    "Failed to fetch comment statistics. Please try again."

internal fun commentTopContributorsFailureMessage(): String =
    "Failed to fetch comment contributors. Please try again."

internal fun recentCommentsFailureMessage(): String =
    "Failed to fetch recent comments. Please try again."

internal fun commentPinFailureMessage(): String =
    "Failed to pin the comment. Please try again."

internal fun commentUnpinFailureMessage(): String =
    "Failed to unpin the comment. Please try again."

internal fun commentRestoreFailureMessage(): String =
    "Failed to restore the comment. Please try again."

internal fun commentSectionsFailureMessage(): String =
    "Failed to fetch comment sections. Please try again."

private fun parseBoundedCommentLimit(
    rawLimit: String?,
    defaultLimit: Int,
    maxLimit: Int
): Int {
    val parsedLimit = rawLimit?.trim()?.toIntOrNull() ?: defaultLimit
    return parsedLimit.coerceIn(MIN_COMMENT_LIMIT, maxLimit)
}

private const val DEFAULT_COMMENT_LIST_LIMIT = 50
private const val DEFAULT_COMMENT_CONTRIBUTOR_LIMIT = 10
private const val DEFAULT_RECENT_COMMENT_LIMIT = 20
private const val MIN_COMMENT_LIMIT = 1
private const val MAX_COMMENT_LIST_LIMIT = 100
private const val MAX_COMMENT_CONTRIBUTOR_LIMIT = 50
private const val MAX_RECENT_COMMENT_LIMIT = 100
private const val DEFAULT_COMMENT_OFFSET = 0
private const val MIN_COMMENT_OFFSET = 0
private const val MAX_COMMENT_OFFSET = 10_000

// Request/Response DTOs

@Serializable
data class CreateCommentRequest(
    val section: CommentSection,
    val sectionItemId: String? = null,
    val content: String,
    val parentCommentId: String? = null,
    val authorId: String,
    val authorName: String
)

@Serializable
data class UpdateCommentRequest(
    val content: String
)
