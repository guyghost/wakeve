package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.userId
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.repository.ScenarioRepository
import com.guyghost.wakeve.models.CreateScenarioRequest
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioGenerationType
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

                val response = scenario.toResponse()
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to scenarioDetailFailureMessage())
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

                val requestedStatus = request.status?.let { value ->
                    parseScenarioRouteEnum<ScenarioStatus>(value, "status").getOrElse { error ->
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to invalidScenarioStatusMessage())
                        )
                    }
                }
                val requestedGenerationType = request.generationType?.let { value ->
                    parseScenarioRouteEnum<ScenarioGenerationType>(value, "generationType").getOrElse { error ->
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to invalidScenarioGenerationTypeMessage())
                        )
                    }
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
                    status = requestedStatus ?: existing.status,
                    updatedAt = now,
                    sourceTimeSlotId = request.sourceTimeSlotId ?: existing.sourceTimeSlotId,
                    sourcePotentialLocationId = request.sourcePotentialLocationId ?: existing.sourcePotentialLocationId,
                    generationType = requestedGenerationType ?: existing.generationType
                )

                val result = repository.updateScenario(updated)
                if (result.isSuccess) {
                    val response = result.getOrThrow().toResponse()
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to scenarioUpdateFailureMessage())
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to scenarioUpdateFailureMessage())
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
                        mapOf("error" to scenarioDeleteFailureMessage())
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to scenarioDeleteFailureMessage())
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
                    !hasConfirmedScenarioAccess(database, scenario.eventId, request.participantId)
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
                        mapOf("error" to scenarioVoteCreateFailureMessage())
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to scenarioVoteCreateFailureMessage())
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
                    mapOf("error" to scenarioVotesFailureMessage())
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

                if (!hasScenarioListAccess(database, eventId, principal.userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You do not have access to scenario details")
                    )
                }

                val scenarios = repository.getScenariosByEventId(eventId)
                val responses = scenarios.map { it.toResponse() }
                call.respond(HttpStatusCode.OK, mapOf("scenarios" to responses))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to scenarioListFailureMessage())
                )
            }
        }

        // POST /api/events/{eventId}/scenarios/matrix/generate - Generate draft scenario matrix
        post("/matrix/generate") {
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
                        mapOf("error" to "Only the organizer can generate scenario matrix")
                    )
                }

                repository.generateScenarioMatrix(eventId).fold(
                    onSuccess = { generated ->
                        call.respond(
                            HttpStatusCode.Created,
                            mapOf("scenarios" to generated.map { it.toResponse() })
                        )
                    },
                    onFailure = { error ->
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to scenarioMatrixGenerateFailureMessage())
                        )
                    }
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to scenarioMatrixGenerateFailureMessage())
                )
            }
        }

        // POST /api/events/{eventId}/scenarios/matrix/publish - Publish draft matrix scenarios
        post("/matrix/publish") {
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
                        mapOf("error" to "Only the organizer can publish scenario matrix")
                    )
                }

                repository.publishScenarioMatrix(eventId).fold(
                    onSuccess = {
                        call.respond(HttpStatusCode.OK, mapOf("status" to "published"))
                    },
                    onFailure = { error ->
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to scenarioMatrixPublishFailureMessage())
                        )
                    }
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to scenarioMatrixPublishFailureMessage())
                )
            }
        }

        // POST /api/events/{eventId}/scenarios/{scenarioId}/select-final - Select final matrix scenario
        post("/{scenarioId}/select-final") {
            try {
                val eventId = call.parameters["eventId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Event ID required")
                )
                val scenarioId = call.parameters["scenarioId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Scenario ID required")
                )
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

                if (!isScenarioOrganizer(database, eventId, principal.userId)) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only the organizer can select final scenario")
                    )
                }

                repository.selectFinalMatrixScenario(eventId, scenarioId).fold(
                    onSuccess = {
                        call.respond(HttpStatusCode.OK, mapOf("status" to "selected"))
                    },
                    onFailure = { error ->
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to scenarioFinalSelectionFailureMessage())
                        )
                    }
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to scenarioFinalSelectionFailureMessage())
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
                if (request.eventId.trim() != eventId) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Request eventId must match path event ID")
                    )
                }
                val now = java.time.Instant.now().toString()
                val generationType = request.generationType?.let { value ->
                    parseScenarioRouteEnum<ScenarioGenerationType>(value, "generationType").getOrElse { error ->
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to invalidScenarioGenerationTypeMessage())
                        )
                    }
                } ?: ScenarioGenerationType.MANUAL

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
                    updatedAt = now,
                    sourceTimeSlotId = request.sourceTimeSlotId,
                    sourcePotentialLocationId = request.sourcePotentialLocationId,
                    generationType = generationType
                )

                val result = repository.createScenario(scenario)
                if (result.isSuccess) {
                    val response = result.getOrThrow().toResponse()
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to scenarioCreateFailureMessage())
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to scenarioCreateFailureMessage())
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
                        scenario = swv.scenario.toResponse(),
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
                    mapOf("error" to scenariosWithVotesFailureMessage())
                )
            }
        }
    }
}

private fun hasConfirmedScenarioAccess(database: WakeveDb, eventId: String, userId: String): Boolean {
    return isScenarioOrganizer(database, eventId, userId) ||
        isConfirmedScenarioParticipant(database, eventId, userId) ||
        isComparingParticipant(database, eventId, userId)
}

private fun hasScenarioListAccess(database: WakeveDb, eventId: String, userId: String): Boolean {
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

private fun isComparingParticipant(database: WakeveDb, eventId: String, userId: String): Boolean {
    val event = database.eventQueries.selectById(eventId).executeAsOneOrNull() ?: return false
    if (event.status != EventStatus.COMPARING.name) {
        return false
    }
    return database.participantQueries
        .selectByEventIdAndUserId(eventId, userId)
        .executeAsOneOrNull() != null
}

private fun isScenarioCreationAllowed(database: WakeveDb, eventId: String): Boolean {
    val status = database.eventQueries
        .selectById(eventId)
        .executeAsOneOrNull()
        ?.status

    return status == EventStatus.CONFIRMED.name || status == EventStatus.COMPARING.name
}

private inline fun <reified T : Enum<T>> parseScenarioRouteEnum(value: String, fieldName: String): Result<T> = runCatching {
    val normalized = value.trim().uppercase().replace('-', '_')
    enumValues<T>().firstOrNull { it.name == normalized }
        ?: throw IllegalArgumentException("Invalid scenario $fieldName: $value")
}

internal fun scenarioDetailFailureMessage(): String =
    "Failed to fetch scenario details. Please try again."

internal fun invalidScenarioStatusMessage(): String =
    "Invalid scenario status."

internal fun invalidScenarioGenerationTypeMessage(): String =
    "Invalid scenario generation type."

internal fun scenarioUpdateFailureMessage(): String =
    "Failed to update the scenario. Please try again."

internal fun scenarioDeleteFailureMessage(): String =
    "Failed to delete the scenario. Please try again."

internal fun scenarioVoteCreateFailureMessage(): String =
    "Failed to save your scenario vote. Please try again."

internal fun scenarioVotesFailureMessage(): String =
    "Failed to fetch scenario votes. Please try again."

internal fun scenarioListFailureMessage(): String =
    "Failed to fetch scenarios. Please try again."

internal fun scenarioMatrixGenerateFailureMessage(): String =
    "Failed to generate scenario matrix. Please try again."

internal fun scenarioMatrixPublishFailureMessage(): String =
    "Failed to publish scenario matrix. Please try again."

internal fun scenarioFinalSelectionFailureMessage(): String =
    "Failed to select the final scenario. Please try again."

internal fun scenarioCreateFailureMessage(): String =
    "Failed to create the scenario. Please try again."

internal fun scenariosWithVotesFailureMessage(): String =
    "Failed to fetch scenarios with votes. Please try again."

private fun Scenario.toResponse(): ScenarioResponse {
    return ScenarioResponse(
        id = id,
        eventId = eventId,
        name = name,
        dateOrPeriod = dateOrPeriod,
        location = location,
        duration = duration,
        estimatedParticipants = estimatedParticipants,
        estimatedBudgetPerPerson = estimatedBudgetPerPerson,
        description = description,
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sourceTimeSlotId = sourceTimeSlotId,
        sourcePotentialLocationId = sourcePotentialLocationId,
        generationType = generationType.name
    )
}

private fun isScenarioFinalized(database: WakeveDb, eventId: String): Boolean {
    return database.eventQueries
        .selectById(eventId)
        .executeAsOneOrNull()
        ?.status == EventStatus.FINALIZED.name
}
