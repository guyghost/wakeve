package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.userId
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.repository.ScenarioRepository
import com.guyghost.wakeve.models.CreateScenarioRequest
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioResponse
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVote
import com.guyghost.wakeve.models.ScenarioVoteRequest
import com.guyghost.wakeve.models.ScenarioVoteResponse
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.models.ScenarioVotingResultResponse
import com.guyghost.wakeve.models.ScenarioWithVotesResponse
import com.guyghost.wakeve.models.UpdateScenarioRequest
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

fun io.ktor.server.routing.Route.scenarioRoutes(repository: ScenarioRepository, database: WakeveDb) {
    route("/scenarios") {
        
        // GET /api/scenarios/{id} - Get specific scenario
        get("/{id}") {
            try {
                val scenarioId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Scenario ID required")
                )

                val scenario = repository.getScenarioById(scenarioId) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Scenario not found")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!hasConfirmedScenarioAccess(database, scenario.eventId, principal.userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to scenario details")
                    )
                }

                val response = ScenarioResponse(
                    id = scenario.id,
                    eventId = scenario.eventId,
                    name = scenario.name,
                    dateOrPeriod = scenario.dateOrPeriod,
                    location = scenario.location,
                    duration = scenario.duration,
                    estimatedParticipants = scenario.estimatedParticipants,
                    estimatedBudgetPerPerson = scenario.estimatedBudgetPerPerson,
                    description = scenario.description,
                    status = scenario.status.name,
                    createdAt = scenario.createdAt,
                    updatedAt = scenario.updatedAt
                )
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // PUT /api/scenarios/{id} - Update scenario
        put("/{id}") {
            try {
                val scenarioId = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Scenario ID required")
                )

                val request = call.receive<UpdateScenarioRequest>()
                val existing = repository.getScenarioById(scenarioId) ?: return@put call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Scenario not found")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!isScenarioOrganizer(database, existing.eventId, principal.userId)) {
                    return@put call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the organizer can update scenarios")
                    )
                }

                if (isScenarioFinalized(database, existing.eventId)) {
                    return@put call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Finalized events are read-only")
                    )
                }

                val now = java.time.Instant.now().toString()
                val updated = existing.copy(
                    name = request.name ?: existing.name,
                    dateOrPeriod = request.dateOrPeriod ?: existing.dateOrPeriod,
                    location = request.location ?: existing.location,
                    duration = request.duration ?: existing.duration,
                    estimatedParticipants = request.estimatedParticipants ?: existing.estimatedParticipants,
                    estimatedBudgetPerPerson = request.estimatedBudgetPerPerson ?: existing.estimatedBudgetPerPerson,
                    description = request.description ?: existing.description,
                    status = request.status?.let { ScenarioStatus.valueOf(it) } ?: existing.status,
                    updatedAt = now
                )

                val result = repository.updateScenario(updated)
                if (result.isSuccess) {
                    val response = ScenarioResponse(
                        id = updated.id,
                        eventId = updated.eventId,
                        name = updated.name,
                        dateOrPeriod = updated.dateOrPeriod,
                        location = updated.location,
                        duration = updated.duration,
                        estimatedParticipants = updated.estimatedParticipants,
                        estimatedBudgetPerPerson = updated.estimatedBudgetPerPerson,
                        description = updated.description,
                        status = updated.status.name,
                        createdAt = updated.createdAt,
                        updatedAt = updated.updatedAt
                    )
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to update scenario"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // DELETE /api/scenarios/{id} - Delete scenario
        delete("/{id}") {
            try {
                val scenarioId = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Scenario ID required")
                )
                val existing = repository.getScenarioById(scenarioId) ?: return@delete call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Scenario not found")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!isScenarioOrganizer(database, existing.eventId, principal.userId)) {
                    return@delete call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the organizer can delete scenarios")
                    )
                }

                if (isScenarioFinalized(database, existing.eventId)) {
                    return@delete call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Finalized events are read-only")
                    )
                }

                val result = repository.deleteScenario(scenarioId)
                if (result.isSuccess) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to delete scenario"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // POST /api/scenarios/{id}/vote - Vote on scenario
        post("/{id}/vote") {
            try {
                val scenarioId = call.parameters["id"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Scenario ID required")
                )
                val scenario = repository.getScenarioById(scenarioId) ?: return@post call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Scenario not found")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                val request = call.receive<ScenarioVoteRequest>()
                if (principal.userId != request.participantId ||
                    !isConfirmedScenarioParticipant(database, scenario.eventId, request.participantId)
                ) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to vote on this scenario")
                    )
                }
                val voteType = try {
                    ScenarioVoteType.valueOf(request.vote)
                } catch (e: IllegalArgumentException) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid vote type: ${request.vote}. Must be PREFER, NEUTRAL, or AGAINST")
                    )
                }

                val vote = ScenarioVote(
                    id = "vote_${System.currentTimeMillis()}_${Math.random()}",
                    scenarioId = scenarioId,
                    participantId = request.participantId,
                    vote = voteType,
                    createdAt = java.time.Instant.now().toString()
                )

                val result = repository.addVote(vote)
                if (result.isSuccess) {
                    val response = ScenarioVoteResponse(
                        id = vote.id,
                        scenarioId = vote.scenarioId,
                        participantId = vote.participantId,
                        vote = vote.vote.name,
                        createdAt = vote.createdAt
                    )
                    call.respond(HttpStatusCode.Created, response)
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

        // GET /api/scenarios/{id}/votes - Get voting results for scenario
        get("/{id}/votes") {
            try {
                val scenarioId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Scenario ID required")
                )
                val scenario = repository.getScenarioById(scenarioId) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Scenario not found or no votes yet")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!hasConfirmedScenarioAccess(database, scenario.eventId, principal.userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to scenario votes")
                    )
                }

                val result = repository.getVotingResult(scenarioId)
                if (result != null) {
                    val response = ScenarioVotingResultResponse(
                        scenarioId = scenarioId,
                        preferCount = result.preferCount,
                        neutralCount = result.neutralCount,
                        againstCount = result.againstCount,
                        totalVotes = result.totalVotes,
                        score = result.score,
                        preferPercentage = result.preferPercentage,
                        neutralPercentage = result.neutralPercentage,
                        againstPercentage = result.againstPercentage
                    )
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Scenario not found or no votes yet")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
    }

    // Event-specific scenario routes
    route("/events/{eventId}/scenarios") {
        
        // GET /api/events/{eventId}/scenarios - Get all scenarios for an event
        get {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!hasConfirmedScenarioAccess(database, eventId, principal.userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to scenario details")
                    )
                }

                val scenarios = repository.getScenariosByEventId(eventId)
                val responses = scenarios.map { scenario ->
                    ScenarioResponse(
                        id = scenario.id,
                        eventId = scenario.eventId,
                        name = scenario.name,
                        dateOrPeriod = scenario.dateOrPeriod,
                        location = scenario.location,
                        duration = scenario.duration,
                        estimatedParticipants = scenario.estimatedParticipants,
                        estimatedBudgetPerPerson = scenario.estimatedBudgetPerPerson,
                        description = scenario.description,
                        status = scenario.status.name,
                        createdAt = scenario.createdAt,
                        updatedAt = scenario.updatedAt
                    )
                }
                call.respond(HttpStatusCode.OK, mapOf("scenarios" to responses))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // POST /api/events/{eventId}/scenarios - Create new scenario
        post {
            try {
                val eventId = call.parameters["eventId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!isScenarioOrganizer(database, eventId, principal.userId)) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the organizer can create scenarios")
                    )
                }

                if (!isScenarioCreationAllowed(database, eventId)) {
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Scenarios can only be created for CONFIRMED or COMPARING events")
                    )
                }

                val request = call.receive<CreateScenarioRequest>()
                val now = java.time.Instant.now().toString()

                val scenario = Scenario(
                    id = "scenario_${System.currentTimeMillis()}_${Math.random()}",
                    eventId = eventId,
                    name = request.name,
                    dateOrPeriod = request.dateOrPeriod,
                    location = request.location,
                    duration = request.duration,
                    estimatedParticipants = request.estimatedParticipants,
                    estimatedBudgetPerPerson = request.estimatedBudgetPerPerson,
                    description = request.description,
                    status = ScenarioStatus.PROPOSED,
                    createdAt = now,
                    updatedAt = now
                )

                val result = repository.createScenario(scenario)
                if (result.isSuccess) {
                    val response = ScenarioResponse(
                        id = scenario.id,
                        eventId = scenario.eventId,
                        name = scenario.name,
                        dateOrPeriod = scenario.dateOrPeriod,
                        location = scenario.location,
                        duration = scenario.duration,
                        estimatedParticipants = scenario.estimatedParticipants,
                        estimatedBudgetPerPerson = scenario.estimatedBudgetPerPerson,
                        description = scenario.description,
                        status = scenario.status.name,
                        createdAt = scenario.createdAt,
                        updatedAt = scenario.updatedAt
                    )
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to create scenario"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }

        // GET /api/events/{eventId}/scenarios/with-votes - Get all scenarios with votes
        get("/with-votes") {
            try {
                val eventId = call.parameters["eventId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!hasConfirmedScenarioAccess(database, eventId, principal.userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to scenario votes")
                    )
                }

                val scenariosWithVotes = repository.getScenariosWithVotes(eventId)
                val responses = scenariosWithVotes.map { swv ->
                    ScenarioWithVotesResponse(
                        scenario = ScenarioResponse(
                            id = swv.scenario.id,
                            eventId = swv.scenario.eventId,
                            name = swv.scenario.name,
                            dateOrPeriod = swv.scenario.dateOrPeriod,
                            location = swv.scenario.location,
                            duration = swv.scenario.duration,
                            estimatedParticipants = swv.scenario.estimatedParticipants,
                            estimatedBudgetPerPerson = swv.scenario.estimatedBudgetPerPerson,
                            description = swv.scenario.description,
                            status = swv.scenario.status.name,
                            createdAt = swv.scenario.createdAt,
                            updatedAt = swv.scenario.updatedAt
                        ),
                        votes = swv.votes.map { vote ->
                            ScenarioVoteResponse(
                                id = vote.id,
                                scenarioId = vote.scenarioId,
                                participantId = vote.participantId,
                                vote = vote.vote.name,
                                createdAt = vote.createdAt
                            )
                        },
                        result = ScenarioVotingResultResponse(
                            scenarioId = swv.scenario.id,
                            preferCount = swv.votingResult.preferCount,
                            neutralCount = swv.votingResult.neutralCount,
                            againstCount = swv.votingResult.againstCount,
                            totalVotes = swv.votingResult.totalVotes,
                            score = swv.votingResult.score,
                            preferPercentage = swv.votingResult.preferPercentage,
                            neutralPercentage = swv.votingResult.neutralPercentage,
                            againstPercentage = swv.votingResult.againstPercentage
                        )
                    )
                }
                call.respond(HttpStatusCode.OK, mapOf("scenarios" to responses))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message.orEmpty())
                )
            }
        }
    }
}

private fun hasConfirmedScenarioAccess(database: WakeveDb, eventId: String, userId: String): Boolean {
    return isScenarioOrganizer(database, eventId, userId) ||
        isConfirmedScenarioParticipant(database, eventId, userId)
}

private fun isScenarioOrganizer(database: WakeveDb, eventId: String, userId: String): Boolean {
    return database.eventQueries
        .selectById(eventId)
        .executeAsOneOrNull()
        ?.organizerId == userId
}

private fun isConfirmedScenarioParticipant(database: WakeveDb, eventId: String, userId: String): Boolean {
    val participant = database.participantQueries
        .selectByEventIdAndUserId(eventId, userId)
        .executeAsOneOrNull()

    return participant?.hasValidatedDate == 1L
}

private fun isScenarioCreationAllowed(database: WakeveDb, eventId: String): Boolean {
    val status = database.eventQueries
        .selectById(eventId)
        .executeAsOneOrNull()
        ?.status

    return status == EventStatus.CONFIRMED.name || status == EventStatus.COMPARING.name
}

private fun isScenarioFinalized(database: WakeveDb, eventId: String): Boolean {
    return database.eventQueries
        .selectById(eventId)
        .executeAsOneOrNull()
        ?.status == EventStatus.FINALIZED.name
}
