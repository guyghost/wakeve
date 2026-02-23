package com.guyghost.wakeve.routes

import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.gamification.GamificationService
import com.guyghost.wakeve.gamification.PointsAction
import com.guyghost.wakeve.models.AddVoteRequest
import com.guyghost.wakeve.models.PollResponse
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.notification.EventNotificationTrigger
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.voteRoutes(
    repository: DatabaseEventRepository,
    eventNotificationTrigger: EventNotificationTrigger? = null,
    gamificationService: GamificationService? = null
) {
    route("/events/{id}/poll") {
        // GET /api/events/{id}/poll - Get poll for event
        get {
            try {
                val eventId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val poll = repository.getPoll(eventId) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Poll not found for event")
                )

                val response = PollResponse(
                    eventId = poll.eventId,
                    votes = poll.votes.mapValues { (_, participantVotes) ->
                        participantVotes.mapValues { (_, vote) -> vote.name }
                    }
                )
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // POST /api/events/{id}/votes - Add vote to poll
        post("/votes") {
            try {
                val eventId = call.parameters["id"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )

                val request = call.receive<AddVoteRequest>()
                
                val vote = try {
                    Vote.valueOf(request.vote)
                } catch (e: IllegalArgumentException) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid vote: ${request.vote}. Must be YES, MAYBE, or NO")
                    )
                }

                val result = repository.addVote(eventId, request.participantId, request.slotId, vote)
                
                if (result.isSuccess) {
                    // Award points for voting (+10 points)
                    try {
                        gamificationService?.awardPoints(
                            userId = request.participantId,
                            action = PointsAction.VOTE,
                            eventId = eventId
                        )
                    } catch (_: Exception) {
                        // Non-blocking: don't fail the vote if gamification fails
                    }

                    // Trigger notification for new vote (async, non-blocking)
                    eventNotificationTrigger?.onVoteAdded(
                        eventId = eventId,
                        voterId = request.participantId
                    )

                    val poll = repository.getPoll(eventId)
                    if (poll != null) {
                        val response = PollResponse(
                            eventId = poll.eventId,
                            votes = poll.votes.mapValues { (_, participantVotes) ->
                                participantVotes.mapValues { (_, v) -> v.name }
                            }
                        )
                        call.respond(HttpStatusCode.Created, response)
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to retrieve poll after vote")
                        )
                    }
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to add vote"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
    }
}
