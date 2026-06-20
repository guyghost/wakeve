package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.userId
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.OptimizationType
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.TransportLocation
import com.guyghost.wakeve.models.TransportPlan
import com.guyghost.wakeve.transport.TransportRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

fun Route.transportRoutes(repository: TransportRepository, database: WakeveDb) {
    route("/events/{eventId}/transport") {
        get("/readiness") {
            val eventId = call.eventIdOrBadRequest() ?: return@get
            val principal = call.principal<JWTPrincipal>()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

            if (!hasConfirmedTransportAccess(database, eventId, principal.userId)) {
                return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "You do not have access to transport details"))
            }
            if (!call.ensureTransportPhaseAllowed(database, eventId, mutation = false)) return@get

            val destination = resolveDestination(database, eventId)
                ?: return@get call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("error" to "A selected destination is required for transport planning")
                )
            call.respond(HttpStatusCode.OK, repository.getReadiness(eventId, destination))
        }

        put("/departures/{participantId}") {
            val eventId = call.eventIdOrBadRequest() ?: return@put
            val participantId = call.parameters["participantId"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Participant ID required"))
            val principal = call.principal<JWTPrincipal>()
                ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

            if (!isOrganizer(database, eventId, principal.userId)) {
                if (participantId != principal.userId || !isConfirmedParticipant(database, eventId, participantId)) {
                    return@put call.respond(HttpStatusCode.Forbidden, mapOf("error" to "You can only update your own confirmed departure"))
                }
            } else if (!isConfirmedParticipant(database, eventId, participantId)) {
                return@put call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Departure can only be set for confirmed participants"))
            }
            if (!call.ensureTransportPhaseAllowed(database, eventId, mutation = true)) return@put

            val request = call.receive<DepartureLocationRequest>()
            if (request.participantId != null && request.participantId != participantId) {
                return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Participant ID mismatch"))
            }

            repository.saveDepartureLocation(
                eventId = eventId,
                participantId = participantId,
                location = request.location,
                updatedByUserId = principal.userId
            ).fold(
                onSuccess = { record -> call.respond(HttpStatusCode.OK, record) },
                onFailure = {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to transportDepartureSaveFailureMessage()))
                }
            )
        }

        get("/plans") {
            val eventId = call.eventIdOrBadRequest() ?: return@get
            val principal = call.principal<JWTPrincipal>()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

            if (!hasConfirmedTransportAccess(database, eventId, principal.userId)) {
                return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "You do not have access to transport plans"))
            }
            if (!call.ensureTransportPhaseAllowed(database, eventId, mutation = false)) return@get

            val plans = database.transportQueries.selectPlansByEvent(eventId)
                .executeAsList()
                .map { row -> row.toTransportPlan(repository, database) }
            call.respond(HttpStatusCode.OK, mapOf("plans" to plans))
        }

        get("/plans/{planId}") {
            val eventId = call.eventIdOrBadRequest() ?: return@get
            val planId = call.parameters["planId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Transport plan ID required"))
            val principal = call.principal<JWTPrincipal>()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

            if (!hasConfirmedTransportAccess(database, eventId, principal.userId)) {
                return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "You do not have access to transport plans"))
            }
            if (!call.ensureTransportPhaseAllowed(database, eventId, mutation = false)) return@get

            val planRow = database.transportQueries.selectPlanById(planId).executeAsOneOrNull()
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Transport plan not found"))
            if (planRow.event_id != eventId) {
                return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Transport plan not found"))
            }

            call.respond(HttpStatusCode.OK, planRow.toTransportPlan(repository, database))
        }

        post("/plans/generate") {
            val eventId = call.eventIdOrBadRequest() ?: return@post
            val principal = call.principal<JWTPrincipal>()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

            if (!isOrganizer(database, eventId, principal.userId)) {
                return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only the organizer can generate transport plans"))
            }
            if (!call.ensureTransportPhaseAllowed(database, eventId, mutation = true)) return@post

            val request = call.receive<GenerateTransportPlanRequest>()
            if (request.destination != null) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Transport destination must come from the selected event scenario")
                )
            }
            val destination = resolveDestination(database, eventId)
                ?: return@post call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("error" to "A selected destination is required before generating transport plans")
                )
            val readiness = repository.getReadiness(eventId, destination)
            if (!readiness.canGeneratePlan) {
                return@post call.respond(HttpStatusCode.Conflict, readiness)
            }

            repository.generatePlan(
                eventId = eventId,
                destination = destination,
                optimizationType = request.optimizationType,
                generatedByUserId = principal.userId
            ).fold(
                onSuccess = { plan -> call.respond(HttpStatusCode.Created, plan) },
                onFailure = {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to transportPlanGenerateFailureMessage()))
                }
            )
        }

        post("/plans/{planId}/select") {
            val eventId = call.eventIdOrBadRequest() ?: return@post
            val planId = call.parameters["planId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Transport plan ID required"))
            val principal = call.principal<JWTPrincipal>()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

            if (!isOrganizer(database, eventId, principal.userId)) {
                return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only the organizer can select transport plans"))
            }
            if (!call.ensureTransportPhaseAllowed(database, eventId, mutation = true)) return@post
            val planRow = database.transportQueries.selectPlanById(planId).executeAsOneOrNull()
                ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Transport plan not found"))
            if (planRow.event_id != eventId) {
                return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Transport plan not found"))
            }

            repository.selectFinalPlan(eventId, planId, principal.userId).fold(
                onSuccess = {
                    val selected = repository.getSelectedPlanSummary(eventId)
                    call.respond(HttpStatusCode.OK, selected ?: mapOf("planId" to planId))
                },
                onFailure = {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to transportPlanSelectFailureMessage()))
                }
            )
        }

        delete("/plans/{planId}") {
            val eventId = call.eventIdOrBadRequest() ?: return@delete
            val planId = call.parameters["planId"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Transport plan ID required"))
            val principal = call.principal<JWTPrincipal>()
                ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

            if (!isOrganizer(database, eventId, principal.userId)) {
                return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only the organizer can delete transport plans"))
            }
            if (!call.ensureTransportPhaseAllowed(database, eventId, mutation = true)) return@delete
            val planRow = database.transportQueries.selectPlanById(planId).executeAsOneOrNull()
                ?: return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Transport plan not found"))
            if (planRow.event_id != eventId) {
                return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Transport plan not found"))
            }

            repository.deletePlan(eventId, planId, principal.userId).fold(
                onSuccess = { call.respond(HttpStatusCode.NoContent) },
                onFailure = {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to transportPlanDeleteFailureMessage()))
                }
            )
        }

        post("/not-needed") {
            val eventId = call.eventIdOrBadRequest() ?: return@post
            val principal = call.principal<JWTPrincipal>()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))

            if (!isOrganizer(database, eventId, principal.userId)) {
                return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only the organizer can mark transport as not needed"))
            }
            if (!call.ensureTransportPhaseAllowed(database, eventId, mutation = true)) return@post

            repository.markTransportNotNeeded(eventId, principal.userId).fold(
                onSuccess = { call.respond(HttpStatusCode.OK, mapOf("transportNotNeeded" to true)) },
                onFailure = {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to transportNotNeededFailureMessage()))
                }
            )
        }
    }
}

@Serializable
private data class DepartureLocationRequest(
    val participantId: String? = null,
    val location: TransportLocation
)

@Serializable
private data class GenerateTransportPlanRequest(
    val optimizationType: OptimizationType = OptimizationType.BALANCED,
    val destination: TransportLocation? = null
)

private val transportRouteJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private suspend fun io.ktor.server.application.ApplicationCall.eventIdOrBadRequest(): String? {
    return parameters["eventId"] ?: run {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Event ID required"))
        null
    }
}

private fun hasConfirmedTransportAccess(database: WakeveDb, eventId: String, userId: String): Boolean {
    return isOrganizer(database, eventId, userId) || isConfirmedParticipant(database, eventId, userId)
}

private fun isOrganizer(database: WakeveDb, eventId: String, userId: String): Boolean {
    return database.eventQueries
        .selectById(eventId)
        .executeAsOneOrNull()
        ?.organizerId == userId
}

private fun isConfirmedParticipant(database: WakeveDb, eventId: String, userId: String): Boolean {
    return database.participantQueries
        .selectByEventIdAndUserId(eventId, userId)
        .executeAsOneOrNull()
        ?.hasValidatedDate == 1L
}

private suspend fun io.ktor.server.application.ApplicationCall.ensureTransportPhaseAllowed(
    database: WakeveDb,
    eventId: String,
    mutation: Boolean
): Boolean {
    return when (database.eventQueries.selectById(eventId).executeAsOneOrNull()?.status) {
        EventStatus.CONFIRMED.name,
        EventStatus.COMPARING.name,
        EventStatus.ORGANIZING.name -> true
        EventStatus.FINALIZED.name -> {
            if (mutation) {
                respond(HttpStatusCode.Conflict, mapOf("error" to "Finalized events are read-only"))
                false
            } else {
                true
            }
        }
        else -> {
            respond(HttpStatusCode.Conflict, mapOf("error" to "Transport logistics require a confirmed event date"))
            false
        }
    }
}

private fun resolveDestination(database: WakeveDb, eventId: String): TransportLocation? {
    val selectedScenario = database.scenarioQueries
        .selectByEventIdAndStatus(eventId, ScenarioStatus.SELECTED.name)
        .executeAsList()
        .firstOrNull()
        ?: return null

    return TransportLocation(
        name = selectedScenario.location,
        address = selectedScenario.location
    )
}

private fun com.guyghost.wakeve.Transport_plan.toTransportPlan(
    repository: TransportRepository,
    database: WakeveDb
): TransportPlan {
    val routes = repository.getRoutesByPlan(id)
    val participantIds = database.transportQueries.selectRoutesByPlan(id)
        .executeAsList()
        .map { it.participant_id }
    return TransportPlan(
        id = id,
        eventId = event_id,
        participantRoutes = participantIds.zip(routes).toMap(),
        groupArrivals = transportRouteJson.decodeFromString(group_arrivals_json),
        totalGroupCost = total_group_cost,
        optimizationType = OptimizationType.valueOf(optimization_type),
        createdAt = created_at
    )
}

internal fun transportDepartureSaveFailureMessage(): String =
    "Failed to save the departure location. Please review the details and try again."

internal fun transportPlanGenerateFailureMessage(): String =
    "Transport option provider is not configured. Please try again later."

internal fun transportPlanSelectFailureMessage(): String =
    "Failed to select the transport plan. Please try again."

internal fun transportPlanDeleteFailureMessage(): String =
    "Failed to delete the transport plan. Please try again."

internal fun transportNotNeededFailureMessage(): String =
    "Failed to update the transport planning status. Please try again."
