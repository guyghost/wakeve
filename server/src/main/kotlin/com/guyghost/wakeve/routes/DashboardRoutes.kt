package com.guyghost.wakeve.routes

import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.database.WakeveDb
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlin.math.min

/**
 * Dashboard API Routes for Organizer Analytics
 *
 * Provides endpoints for:
 * - GET /api/dashboard/overview — organizer's summary stats
 * - GET /api/dashboard/events — list of organizer's events with analytics
 * - GET /api/dashboard/events/{id}/analytics — detailed analytics for one event
 */
fun io.ktor.server.routing.Route.dashboardRoutes(
    database: WakeveDb,
    repository: DatabaseEventRepository
) {
    route("/dashboard") {

        // GET /api/dashboard/overview — Organizer summary
        get("/overview") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Authentication required")
                    )

                val dashboardQueries = database.dashboardQueries

                val totalEvents = dashboardQueries.countEventsByOrganizer(userId)
                    .executeAsOne()
                val totalParticipants = dashboardQueries.countParticipantsByOrganizer(userId)
                    .executeAsOne()
                val totalVotes = dashboardQueries.countVotesByOrganizer(userId)
                    .executeAsOne()
                val totalComments = dashboardQueries.countCommentsByOrganizer(userId)
                    .executeAsOne()

                val statusBreakdown = dashboardQueries.countEventsByStatusForOrganizer(userId)
                    .executeAsList()
                    .associate { it.status to it.count }

                val avgParticipants = if (totalEvents > 0) {
                    totalParticipants.toDouble() / totalEvents.toDouble()
                } else {
                    0.0
                }

                call.respond(
                    HttpStatusCode.OK,
                    DashboardOverviewResponse(
                        totalEvents = totalEvents.toSafeResponseInt(),
                        totalParticipants = totalParticipants.toSafeResponseInt(),
                        averageParticipants = avgParticipants,
                        totalVotes = totalVotes.toSafeResponseInt(),
                        totalComments = totalComments.toSafeResponseInt(),
                        eventsByStatus = statusBreakdown.mapValues { it.value.toSafeResponseInt() }
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to dashboardOverviewFailureMessage())
                )
            }
        }

        // GET /api/dashboard/events — List organizer's events with analytics
        get("/events") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Authentication required")
                    )

                val dashboardQueries = database.dashboardQueries
                val limit = call.parseBoundedDashboardInt(
                    name = "limit",
                    default = DEFAULT_DASHBOARD_EVENTS_LIMIT,
                    min = 1,
                    max = MAX_DASHBOARD_EVENTS_LIMIT
                ) ?: return@get
                val offset = call.parseBoundedDashboardInt(
                    name = "offset",
                    default = 0,
                    min = 0,
                    max = MAX_DASHBOARD_EVENTS_OFFSET
                ) ?: return@get

                val totalCount = dashboardQueries.countEventsByOrganizer(userId)
                    .executeAsOne()
                    .toSafeResponseInt()

                val events = dashboardQueries.selectEventAnalyticsPaged(
                    organizerId = userId,
                    limit = limit.toLong(),
                    offset = offset.toLong()
                )
                    .executeAsList()
                    .map { row ->
                        val responseRate = if (row.participantCount > 0 && row.timeSlotCount > 0) {
                            val expectedVotes = row.participantCount * row.timeSlotCount
                            if (expectedVotes > 0) {
                                (row.voteCount.toDouble() / expectedVotes.toDouble() * 100.0)
                                    .coerceAtMost(100.0)
                            } else 0.0
                        } else 0.0

                        DashboardEventItem(
                            eventId = row.eventId,
                            title = row.title,
                            status = row.status,
                            eventType = row.eventType,
                            createdAt = row.createdAt,
                            deadline = row.deadline,
                            participantCount = row.participantCount.toSafeResponseInt(),
                            voteCount = row.voteCount.toSafeResponseInt(),
                            commentCount = row.commentCount.toSafeResponseInt(),
                            responseRate = responseRate
                        )
                    }

                call.respond(
                    HttpStatusCode.OK,
                    DashboardEventsResponse(
                        events = events,
                        totalCount = totalCount,
                        limit = limit,
                        offset = offset
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to dashboardEventsFailureMessage())
                )
            }
        }

        // GET /api/dashboard/events/{id}/analytics — Detailed analytics for one event
        get("/events/{id}/analytics") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Authentication required")
                    )

                val eventId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                // Verify the user is the organizer
                val event = repository.getEvent(eventId) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Event not found")
                )

                if (event.organizerId != userId) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the organizer can view event analytics")
                    )
                }

                val dashboardQueries = database.dashboardQueries

                // Vote timeline
                val voteTimeline = dashboardQueries.selectVoteTimeline(eventId)
                    .executeAsList()
                    .map { TimelineEntry(date = it.voteDate ?: "", count = it.voteCount.toSafeResponseInt()) }

                // Participant timeline
                val participantTimeline = dashboardQueries.selectParticipantTimeline(eventId)
                    .executeAsList()
                    .map { TimelineEntry(date = it.joinDate ?: "", count = it.joinCount.toSafeResponseInt()) }

                // Popular time slots
                val popularTimeSlots = dashboardQueries.selectPopularTimeSlots(eventId)
                    .executeAsList()
                    .map { row ->
                        PopularTimeSlot(
                            slotId = row.slotId,
                            startTime = row.startTime,
                            endTime = row.endTime,
                            timeOfDay = row.timeOfDay,
                            yesVotes = row.yesVotes.toSafeResponseInt(),
                            maybeVotes = row.maybeVotes.toSafeResponseInt(),
                            noVotes = row.noVotes.toSafeResponseInt(),
                            totalVotes = row.totalVotes.toSafeResponseInt()
                        )
                    }

                // Poll completion rate
                val pollCompletion = dashboardQueries.selectPollCompletionRate(eventId)
                    .executeAsOne()
                val completionRate = if (pollCompletion.totalParticipants > 0) {
                    pollCompletion.votedParticipants.toDouble() / pollCompletion.totalParticipants.toDouble() * 100.0
                } else 0.0

                call.respond(
                    HttpStatusCode.OK,
                    EventDetailedAnalyticsResponse(
                        eventId = eventId,
                        title = event.title,
                        status = event.status.name,
                        voteTimeline = voteTimeline,
                        participantTimeline = participantTimeline,
                        popularTimeSlots = popularTimeSlots,
                        pollCompletionRate = completionRate,
                        totalParticipants = pollCompletion.totalParticipants.toSafeResponseInt(),
                        votedParticipants = pollCompletion.votedParticipants.toSafeResponseInt()
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to dashboardEventAnalyticsFailureMessage())
                )
            }
        }
    }
}

// ==================== Response DTOs ====================

@Serializable
data class DashboardOverviewResponse(
    val totalEvents: Int,
    val totalParticipants: Int,
    val averageParticipants: Double,
    val totalVotes: Int,
    val totalComments: Int,
    val eventsByStatus: Map<String, Int>
)

@Serializable
data class DashboardEventsResponse(
    val events: List<DashboardEventItem>,
    val totalCount: Int,
    val limit: Int,
    val offset: Int
)

@Serializable
data class DashboardEventItem(
    val eventId: String,
    val title: String,
    val status: String,
    val eventType: String? = null,
    val createdAt: String,
    val deadline: String,
    val participantCount: Int,
    val voteCount: Int,
    val commentCount: Int,
    val responseRate: Double
)

@Serializable
data class EventDetailedAnalyticsResponse(
    val eventId: String,
    val title: String,
    val status: String,
    val voteTimeline: List<TimelineEntry>,
    val participantTimeline: List<TimelineEntry>,
    val popularTimeSlots: List<PopularTimeSlot>,
    val pollCompletionRate: Double,
    val totalParticipants: Int,
    val votedParticipants: Int
)

@Serializable
data class TimelineEntry(
    val date: String,
    val count: Int
)

@Serializable
data class PopularTimeSlot(
    val slotId: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val timeOfDay: String? = null,
    val yesVotes: Int,
    val maybeVotes: Int,
    val noVotes: Int,
    val totalVotes: Int
)

private const val DEFAULT_DASHBOARD_EVENTS_LIMIT = 50
private const val MAX_DASHBOARD_EVENTS_LIMIT = 100
private const val MAX_DASHBOARD_EVENTS_OFFSET = 10_000

private fun Long.toSafeResponseInt(): Int = min(this, Int.MAX_VALUE.toLong()).toInt()

private suspend fun ApplicationCall.parseBoundedDashboardInt(
    name: String,
    default: Int,
    min: Int,
    max: Int
): Int? {
    val rawValue = request.queryParameters[name]?.trim()
    if (rawValue.isNullOrEmpty()) return default
    val parsed = rawValue.toIntOrNull()
        ?: return respondDashboardQueryError("$name must be an integer")
    if (parsed < min || parsed > max) {
        return respondDashboardQueryError("$name must be between $min and $max")
    }
    return parsed
}

private suspend fun ApplicationCall.respondDashboardQueryError(message: String): Int? {
    respond(HttpStatusCode.BadRequest, mapOf("error" to message))
    return null
}

internal fun dashboardOverviewFailureMessage(): String =
    "Failed to fetch dashboard overview. Please try again."

internal fun dashboardEventsFailureMessage(): String =
    "Failed to fetch dashboard events. Please try again."

internal fun dashboardEventAnalyticsFailureMessage(): String =
    "Failed to fetch event analytics. Please try again."
