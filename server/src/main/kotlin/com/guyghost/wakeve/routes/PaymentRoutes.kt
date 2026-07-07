package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.userId
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.payment.PaymentPotRecord
import com.guyghost.wakeve.payment.PaymentPotRepository
import com.guyghost.wakeve.payment.TricountHandoffRepository
import com.guyghost.wakeve.repository.EventRepositoryInterface
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

fun Route.paymentRoutes(
    repository: TricountHandoffRepository,
    eventRepository: EventRepositoryInterface,
    database: WakeveDb
) {
    val paymentPotRepository = PaymentPotRepository(database)

    authenticate("auth-jwt") {
        route("/events/{eventId}/payment") {
            get("/readiness") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                val eventId = call.parameters["eventId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Event ID required"))
                val userId = principal.userId

                if (!hasPaymentReadAccess(eventRepository, database, eventId, userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        paymentAuditDenial(eventId, userId, "payment_readiness")
                    )
                }

                call.respond(HttpStatusCode.OK, repository.getPaymentReadiness(eventId))
            }

            post("/pot") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                val eventId = call.parameters["eventId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Event ID required"))
                val userId = principal.userId
                val request = call.receive<CreatePaymentPotRequest>()
                val validatedRequest = validateCreatePaymentPotRequest(request).getOrElse { error ->
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to paymentPotCreateValidationFailureMessage())
                    )
                }

                if (validatedRequest.eventId != eventId) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        paymentAuditDenial(eventId, userId, "create_payment_pot_scope")
                    )
                }
                if (validatedRequest.status != "ACTIVE") {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Payment pot creation requires ACTIVE status")
                    )
                }

                val event = eventRepository.getEvent(eventId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Event not found"))
                if (event.organizerId != userId) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        paymentAuditDenial(eventId, userId, "create_payment_pot")
                    )
                }
                if (event.status != EventStatus.ORGANIZING) {
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Payment pots can only be created while event is ORGANIZING")
                    )
                }
                if (validatedRequest.tricountLink != null &&
                    !repository.isTrustedProviderUrl(validatedRequest.paymentProvider, validatedRequest.tricountLink)
                ) {
                    return@post call.respond(
                        HttpStatusCode.UnprocessableEntity,
                        mapOf(
                            "error" to "suspicious_provider_url",
                            "trusted" to "false"
                        )
                    )
                }

                val pot = paymentPotRepository.createPot(
                    eventId = eventId,
                    organizerId = userId,
                    goalAmount = validatedRequest.goalAmount,
                    title = validatedRequest.title ?: "${event.title} payment pot",
                    currency = validatedRequest.currency,
                    paymentProvider = validatedRequest.paymentProvider,
                    tricountGroupUrl = validatedRequest.tricountLink
                )
                call.respond(HttpStatusCode.Created, pot.toResponse())
            }

            get("/pot") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                val eventId = call.parameters["eventId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Event ID required"))
                val userId = principal.userId

                if (!hasPaymentReadAccess(eventRepository, database, eventId, userId)) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        paymentAuditDenial(eventId, userId, "read_payment_pot")
                    )
                }

                val pot = paymentPotRepository.getActivePotForEvent(eventId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Payment pot not found for event"))
                if (pot.eventId != eventId) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        paymentAuditDenial(eventId, userId, "read_payment_pot_scope")
                    )
                }

                call.respond(HttpStatusCode.OK, pot.toResponse())
            }

            post("/pot/close") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                val eventId = call.parameters["eventId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Event ID required"))
                val userId = principal.userId
                val request = call.receive<ClosePaymentPotRequest>()

                if (request.eventId != eventId) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        paymentAuditDenial(eventId, userId, "close_payment_pot_scope")
                    )
                }

                val event = eventRepository.getEvent(eventId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Event not found"))
                if (event.organizerId != userId) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        paymentAuditDenial(eventId, userId, "close_payment_pot")
                    )
                }
                if (event.status != EventStatus.ORGANIZING) {
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Payment pots can only be closed while event is ORGANIZING")
                    )
                }

                val pot = paymentPotRepository.getActivePotForEvent(eventId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Active payment pot not found for event"))
                if (pot.eventId != eventId) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        paymentAuditDenial(eventId, userId, "close_payment_pot_scope")
                    )
                }

                val closed = paymentPotRepository.closePot(pot.id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Payment pot not found"))
                call.respond(HttpStatusCode.OK, closed.toResponse())
            }

            post("/tricount/link") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                val eventId = call.parameters["eventId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Event ID required"))
                val userId = principal.userId

                val event = eventRepository.getEvent(eventId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Event not found"))
                if (event.organizerId != userId) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        paymentAuditDenial(eventId, userId, "tricount_link")
                    )
                }
                if (event.status != EventStatus.ORGANIZING) {
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Tricount handoff can only be linked while event is ORGANIZING")
                    )
                }

                @Serializable
                data class LinkTricountRequest(
                    val provider: String,
                    val providerId: String,
                    val providerUrl: String,
                    val syncStatus: String
                )

                val request = call.receive<LinkTricountRequest>()
                val provider = request.provider.trim().uppercase()
                val providerId = request.providerId.trim()
                val providerUrl = request.providerUrl.trim()
                val syncStatus = request.syncStatus.trim().uppercase()

                if (provider != "TRICOUNT") {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Unsupported provider: $provider")
                    )
                }
                if (providerId.isBlank()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "providerId is required")
                    )
                }
                if (providerId.length > 200) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "providerId must not exceed 200 characters")
                    )
                }
                if (syncStatus !in setOf("LINKED", "SYNCED")) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "syncStatus must be LINKED or SYNCED")
                    )
                }
                if (!repository.isTrustedProviderUrl(provider, providerUrl)) {
                    return@post call.respond(
                        HttpStatusCode.UnprocessableEntity,
                        mapOf(
                            "error" to "suspicious_provider_url",
                            "trusted" to "false"
                        )
                    )
                }

                val handoff = repository.linkHandoff(
                    eventId = eventId,
                    provider = provider,
                    providerId = providerId,
                    providerUrl = providerUrl,
                    syncStatus = syncStatus
                )
                call.respond(HttpStatusCode.Created, handoff)
            }
        }
    }
}

@Serializable
private data class CreatePaymentPotRequest(
    val eventId: String,
    val goalAmount: Double,
    val currency: String = "EUR",
    val paymentProvider: String = "TRICOUNT",
    val status: String = "ACTIVE",
    val title: String? = null,
    val tricountLink: String? = null
)

@Serializable
private data class ClosePaymentPotRequest(
    val eventId: String
)

@Serializable
private data class PaymentPotResponse(
    val id: String,
    val eventId: String,
    val organizerId: String,
    val goalAmount: Double,
    val currentAmount: Double,
    val currency: String,
    val title: String,
    val status: String,
    val paymentProvider: String,
    val tricountGroupId: String?,
    val tricountGroupUrl: String?,
    val tricountLink: String?,
    val createdAt: String,
    val closedAt: String?
)

private data class ValidatedCreatePaymentPotRequest(
    val eventId: String,
    val goalAmount: Double,
    val currency: String,
    val paymentProvider: String,
    val status: String,
    val title: String?,
    val tricountLink: String?
)

private fun validateCreatePaymentPotRequest(request: CreatePaymentPotRequest): Result<ValidatedCreatePaymentPotRequest> =
    runCatching {
        val eventId = request.eventId.trim()
        require(eventId.isNotEmpty()) { "eventId is required" }
        require(request.goalAmount.isFinite() && request.goalAmount > 0.0) {
            "goalAmount must be a positive finite amount"
        }

        val currency = request.currency.trim().uppercase()
        require(Regex("^[A-Z]{3}$").matches(currency)) { "currency must be a 3-letter ISO currency code" }

        val paymentProvider = request.paymentProvider.trim().uppercase()
        require(paymentProvider == "TRICOUNT") { "Unsupported paymentProvider: $paymentProvider" }

        val status = request.status.trim().uppercase()
        require(status == "ACTIVE") { "Payment pot creation requires ACTIVE status" }

        val title = request.title?.trim()?.takeIf { it.isNotEmpty() }
        require(title == null || title.length <= 120) { "title must not exceed 120 characters" }

        val tricountLink = request.tricountLink?.trim()?.takeIf { it.isNotEmpty() }

        ValidatedCreatePaymentPotRequest(
            eventId = eventId,
            goalAmount = request.goalAmount,
            currency = currency,
            paymentProvider = paymentProvider,
            status = status,
            title = title,
            tricountLink = tricountLink
        )
    }

private fun PaymentPotRecord.toResponse(): PaymentPotResponse =
    PaymentPotResponse(
        id = id,
        eventId = eventId,
        organizerId = organizerId,
        goalAmount = goalAmount,
        currentAmount = currentAmount,
        currency = currency,
        title = title,
        status = status,
        paymentProvider = paymentProvider,
        tricountGroupId = tricountGroupId,
        tricountGroupUrl = tricountGroupUrl,
        tricountLink = tricountGroupUrl,
        createdAt = createdAt,
        closedAt = closedAt
    )

private fun hasPaymentReadAccess(
    eventRepository: EventRepositoryInterface,
    database: WakeveDb,
    eventId: String,
    userId: String
): Boolean {
    val event = eventRepository.getEvent(eventId) ?: return false
    if (event.organizerId == userId) return true
    return database.participantQueries
        .selectByEventIdAndUserId(eventId, userId)
        .executeAsOneOrNull()
        ?.hasValidatedDate == 1L
}

private fun paymentAuditDenial(eventId: String, userId: String, action: String): Map<String, String> =
    mapOf(
        "error" to "You do not have access to this event",
        "auditReference" to "audit-${eventId.take(12)}-${userId.take(12)}-${System.currentTimeMillis()}",
        "auditAction" to action
    )

internal fun paymentPotCreateValidationFailureMessage(): String =
    "Invalid payment pot request. Please review the payment details and try again."
