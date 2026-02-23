package com.guyghost.wakeve.routes

import com.guyghost.wakeve.comment.CommentRepository
import com.guyghost.wakeve.models.CommentRequest
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.notification.EventNotificationTrigger
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
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
    eventNotificationTrigger: EventNotificationTrigger? = null
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

                val commentRequest = CommentRequest(
                    section = request.section,
                    sectionItemId = request.sectionItemId,
                    content = request.content,
                    parentCommentId = request.parentCommentId
                )

                val comment = repository.createComment(
                    eventId = eventId,
                    authorId = request.authorId,
                    authorName = request.authorName,
                    request = commentRequest
                )

                // Trigger notification for new comment (async, non-blocking)
                eventNotificationTrigger?.onNewComment(
                    eventId = eventId,
                    authorId = request.authorId,
                    authorName = request.authorName,
                    commentPreview = request.content
                )

                call.respond(HttpStatusCode.Created, comment)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                val comments = if (section != null) {
                    if (threaded) {
                        repository.getCommentsWithThreads(eventId, section, sectionItemId)
                    } else {
                        repository.getCommentsBySection(eventId, section, sectionItemId)
                    }
                } else {
                    repository.getCommentsByEvent(eventId)
                }

                call.respond(HttpStatusCode.OK, comments)
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.message.orEmpty())
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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
                } else {
                    call.respond(HttpStatusCode.OK, comment)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                val updatedComment = repository.updateComment(commentId, request.content)
                if (updatedComment != null) {
                    call.respond(HttpStatusCode.OK, updatedComment)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comment not found"))
                }
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.message.orEmpty())
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                repository.deleteComment(commentId)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                val statistics = repository.getCommentStatistics(eventId)
                call.respond(HttpStatusCode.OK, statistics)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val contributors = repository.getTopContributors(eventId, limit)

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
                    mapOf("error" to e.message.orEmpty())
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
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

                if (since == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Parameter 'since' (ISO 8601 timestamp) is required")
                    )
                    return@get
                }

                val comments = repository.getRecentComments(eventId, since, limit)
                call.respond(HttpStatusCode.OK, comments)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                // TODO: Check if user is organizer (requires auth context)
                // For now, allow pinning for demo purposes

                val pinnedComment = repository.pinComment(commentId)
                if (pinnedComment != null) {
                    call.respond(HttpStatusCode.OK, pinnedComment)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comment not found"))
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                // TODO: Check if user is organizer (requires auth context)
                // For now, allow unpinning for demo purposes

                val unpinnedComment = repository.unpinComment(commentId)
                if (unpinnedComment != null) {
                    call.respond(HttpStatusCode.OK, unpinnedComment)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comment not found"))
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                // TODO: Check if user is organizer or comment author (requires auth context)
                // For now, allow restoring for demo purposes

                val restoredComment = repository.restoreComment(commentId)
                if (restoredComment != null) {
                    call.respond(HttpStatusCode.OK, restoredComment)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comment not found"))
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
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

                val statsBySection = repository.getCommentStatsBySection(eventId)
                call.respond(HttpStatusCode.OK, statsBySection)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
    }
}

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