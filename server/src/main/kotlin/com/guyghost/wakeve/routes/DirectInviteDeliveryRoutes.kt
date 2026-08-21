package com.guyghost.wakeve.routes

import com.guyghost.wakeve.auth.userId
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.invitationexperience.DirectInviteDeliveryBinding
import com.guyghost.wakeve.invitationexperience.DirectInviteDeliveryEnvelope
import com.guyghost.wakeve.invitationexperience.DirectInviteDeliveryRequest
import com.guyghost.wakeve.invitationexperience.DirectInviteDeliveryResult
import com.guyghost.wakeve.invitationexperience.DirectInviteRecipientOutcome
import com.guyghost.wakeve.invitationexperience.EventTemporalClassifier
import com.guyghost.wakeve.invitationexperience.RecipientKey
import com.guyghost.wakeve.invitationexperience.TemporalClass
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.repository.DatabaseEventRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Server-side delivery boundary. The route persists the encrypted queue before
 * invoking this owner; injected providers can then admit the exact opaque
 * request into their delivery system and return a typed acknowledgement.
 */
fun interface DirectInviteBackendDeliveryOwner {
    suspend fun dispatch(request: DirectInviteDeliveryRequest): DirectInviteDeliveryResult
}

/**
 * Optional availability signal used only by the production composition root.
 * Injected test/provider owners are considered available by default.
 */
interface DirectInviteBackendDeliveryAvailability {
    val isAvailable: Boolean
    val sealingPublicKey: String?
    val keyVersion: Int?
}

private class UnavailableDirectInviteBackendDeliveryOwner :
    DirectInviteBackendDeliveryOwner,
    DirectInviteBackendDeliveryAvailability {
    override val isAvailable: Boolean = false
    override val sealingPublicKey: String? = null
    override val keyVersion: Int? = null

    override suspend fun dispatch(
        request: DirectInviteDeliveryRequest
    ): DirectInviteDeliveryResult = DirectInviteDeliveryResult.Rejected(
        com.guyghost.wakeve.invitationexperience.InvitationExperienceError.PROVIDER_UNAVAILABLE
    )
}

/**
 * Production provider owner. It sends only protected recipient keys and AEAD
 * ciphertext to the configured delivery service. A local DB admission is never
 * promoted to SERVER_ACCEPTED: that state requires PROVIDER_ACCEPTED here.
 */
class HttpDirectInviteBackendDeliveryOwner private constructor(
    private val endpoint: String,
    private val credential: String,
    override val sealingPublicKey: String,
    override val keyVersion: Int,
    private val client: HttpClient
) : DirectInviteBackendDeliveryOwner, DirectInviteBackendDeliveryAvailability {
    override val isAvailable: Boolean = true

    override suspend fun dispatch(
        request: DirectInviteDeliveryRequest
    ): DirectInviteDeliveryResult {
        return try {
            val response = client.post(endpoint) {
                bearerAuth(credential)
                contentType(ContentType.Application.Json)
                setBody(request.toProviderRequest())
            }
            if (response.status != HttpStatusCode.OK) {
                return DirectInviteDeliveryResult.Deferred(
                    com.guyghost.wakeve.invitationexperience.InvitationExperienceError.SERVER_UNAVAILABLE
                )
            }
            response.body<ProviderDeliveryResponse>().toDomain(request)
        } catch (_: Exception) {
            DirectInviteDeliveryResult.Deferred(
                com.guyghost.wakeve.invitationexperience.InvitationExperienceError.NETWORK_UNAVAILABLE
            )
        }
    }

    companion object {
        fun fromEnvironment(): DirectInviteBackendDeliveryOwner {
            val endpoint = System.getenv("DIRECT_INVITE_DELIVERY_URL")?.trim().orEmpty()
            val credential = System.getenv("DIRECT_INVITE_DELIVERY_TOKEN")?.trim().orEmpty()
            val publicKey = System.getenv("DIRECT_INVITE_DELIVERY_PUBLIC_KEY")?.trim().orEmpty()
            val keyVersion = System.getenv("DIRECT_INVITE_DELIVERY_KEY_VERSION")
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
            if (
                endpoint.isBlank() || credential.isBlank() || publicKey.isBlank() ||
                keyVersion == null || runCatching {
                    java.util.Base64.getDecoder().decode(publicKey)
                }.getOrNull()?.size != 32 ||
                !endpoint.startsWith("https://") || endpoint.contains('@') ||
                endpoint.contains('?') || endpoint.contains('#')
            ) return UnavailableDirectInviteBackendDeliveryOwner()

            val client = HttpClient(CIO) {
                install(ClientContentNegotiation) {
                    json(Json { ignoreUnknownKeys = false })
                }
            }
            return HttpDirectInviteBackendDeliveryOwner(
                endpoint,
                credential,
                publicKey,
                keyVersion,
                client
            )
        }
    }
}

@Serializable
private data class ProviderDeliveryRequest(
    val eventId: String,
    val actorId: String,
    val accessRevision: Long,
    val batchId: String,
    val operationId: String,
    val envelopes: List<ProviderDeliveryEnvelope>
)

@Serializable
private data class ProviderDeliveryEnvelope(
    val recipientKey: String,
    val ciphertext: String,
    val keyVersion: Int,
    val expiresAt: String
)

@Serializable
private data class ProviderDeliveryResponse(
    val batchId: String,
    val operationId: String,
    val outcomes: List<ProviderDeliveryOutcome>
)

@Serializable
private data class ProviderDeliveryOutcome(
    val recipientKey: String,
    val status: String,
    val invitationId: String? = null,
    val reasonCode: String? = null
)

private fun DirectInviteDeliveryRequest.toProviderRequest() = ProviderDeliveryRequest(
    eventId = binding.eventId,
    actorId = binding.actorId,
    accessRevision = binding.accessRevision,
    batchId = binding.batchId,
    operationId = binding.operationId,
    envelopes = envelopes.sortedBy { it.recipientKey.value }.map {
        ProviderDeliveryEnvelope(
            recipientKey = it.recipientKey.value,
            ciphertext = it.ciphertext,
            keyVersion = it.keyVersion,
            expiresAt = it.expiresAt
        )
    }
)

private fun ProviderDeliveryResponse.toDomain(
    request: DirectInviteDeliveryRequest
): DirectInviteDeliveryResult {
    val expectedKeys = request.envelopes.mapTo(linkedSetOf()) { it.recipientKey }
    val mapped = outcomes.mapNotNull { outcome ->
        val key = runCatching { RecipientKey(outcome.recipientKey) }.getOrNull()
            ?: return DirectInviteDeliveryResult.Rejected(
                com.guyghost.wakeve.invitationexperience.InvitationExperienceError.PERMANENT_FAILURE
            )
        val value = when (outcome.status) {
            "PROVIDER_ACCEPTED" -> outcome.invitationId
                ?.takeIf(String::isNotBlank)
                ?.let(DirectInviteRecipientOutcome::ServerAccepted)
            "INVALID" -> outcome.reasonCode
                ?.takeIf(String::isNotBlank)
                ?.let(DirectInviteRecipientOutcome::Invalid)
            "RETRYABLE_FAILURE" -> DirectInviteRecipientOutcome.Failed(
                com.guyghost.wakeve.invitationexperience.InvitationExperienceError.NETWORK_UNAVAILABLE
            )
            else -> null
        } ?: return DirectInviteDeliveryResult.Rejected(
            com.guyghost.wakeve.invitationexperience.InvitationExperienceError.PERMANENT_FAILURE
        )
        key to value
    }.toMap()
    if (
        batchId != request.binding.batchId ||
        operationId != request.binding.operationId ||
        mapped.keys != expectedKeys
    ) return DirectInviteDeliveryResult.Rejected(
        com.guyghost.wakeve.invitationexperience.InvitationExperienceError.CONFLICT
    )

    return DirectInviteDeliveryResult.Acknowledged(batchId, operationId, mapped)
}

@Serializable
private data class DirectInviteCapabilityResponse(
    val state: String,
    val eventId: String,
    val actorId: String,
    val accessRevision: Long,
    val sealingPublicKey: String? = null,
    val keyVersion: Int? = null
)

@Serializable
private data class DirectInviteBatchRequest(
    val accessRevision: Long,
    val batchId: String,
    val operationId: String,
    val envelopes: List<DirectInviteEnvelopeRequest>
)

@Serializable
private data class DirectInviteEnvelopeRequest(
    val recipientKey: String,
    val ciphertext: String,
    val keyVersion: Int,
    val expiresAt: String
)

@Serializable
private data class DirectInviteBatchResponse(
    val batchId: String,
    val operationId: String,
    val status: String,
    val outcomes: List<DirectInviteOutcomeResponse>
)

@Serializable
private data class DirectInviteOutcomeResponse(
    val recipientKey: String,
    val status: String,
    val invitationId: String? = null,
    val reasonCode: String? = null
)

private val strictDirectInviteJson = Json {
    ignoreUnknownKeys = false
    explicitNulls = false
}

fun Route.directInviteDeliveryRoutes(
    database: WakeveDb,
    eventRepository: DatabaseEventRepository,
    deliveryOwner: DirectInviteBackendDeliveryOwner
) {
    route("/events/{id}/direct-invites") {
        get("/capability") {
            val eventId = call.parameters["id"]?.trim().orEmpty()
            val actorId = call.principal<JWTPrincipal>()?.userId
                ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "authentication_required")
                )
            val event = eventRepository.getEvent(eventId)
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "event_not_found")
                )
            if (!deliveryOwner.isConfiguredForProduction()) {
                return@get call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    mapOf("error" to "direct_invite_delivery_unavailable")
                )
            }
            if (!event.allowsDirectInvite(actorId, Clock.System.now())) {
                return@get call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to "direct_invite_unavailable")
                )
            }
            call.respond(
                HttpStatusCode.OK,
                DirectInviteCapabilityResponse(
                    state = "READY",
                    eventId = event.id,
                    actorId = actorId,
                    accessRevision = event.aggregateRevision,
                    sealingPublicKey = deliveryOwner.sealingConfiguration()?.first,
                    keyVersion = deliveryOwner.sealingConfiguration()?.second
                )
            )
        }

        post("/batches") {
            val eventId = call.parameters["id"]?.trim().orEmpty()
            val actorId = call.principal<JWTPrincipal>()?.userId
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "authentication_required")
                )
            val request = runCatching {
                strictDirectInviteJson.decodeFromString<DirectInviteBatchRequest>(call.receiveText())
            }.getOrNull() ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "invalid_direct_invite_request")
            )
            val event = eventRepository.getEvent(eventId)
                ?: return@post call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "event_not_found")
                )
            if (!deliveryOwner.isConfiguredForProduction()) {
                return@post call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    mapOf("error" to "direct_invite_delivery_unavailable")
                )
            }
            if (!event.allowsDirectInvite(actorId, Clock.System.now())) {
                return@post call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to "direct_invite_unavailable")
                )
            }
            if (request.accessRevision != event.aggregateRevision) {
                return@post call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("error" to "stale_access_revision")
                )
            }

            val binding = runCatching {
                DirectInviteDeliveryBinding(
                    eventId = eventId,
                    actorId = actorId,
                    accessRevision = request.accessRevision,
                    batchId = request.batchId,
                    operationId = request.operationId
                )
            }.getOrNull() ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "invalid_direct_invite_request")
            )
            val envelopes = request.toDomainEnvelopes(binding)
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "invalid_direct_invite_request")
                )

            val existingResponse = existingDirectInviteResponse(database, binding, envelopes)
            if (existingResponse != null) {
                when (existingResponse) {
                    is ExistingDirectInviteResponse.Exact -> return@post call.respond(
                        HttpStatusCode.OK,
                        existingResponse.response
                    )
                    ExistingDirectInviteResponse.Conflict -> return@post call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "direct_invite_operation_conflict")
                    )
                    ExistingDirectInviteResponse.Pending -> Unit
                }
            }

            if (existingResponse == null && !persistDirectInvitePending(database, binding, envelopes)) {
                return@post call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("error" to "direct_invite_operation_conflict")
                )
            }
            val delivery = runCatching {
                deliveryOwner.dispatch(DirectInviteDeliveryRequest(binding, envelopes))
            }.getOrElse {
                DirectInviteDeliveryResult.Deferred(
                    com.guyghost.wakeve.invitationexperience.InvitationExperienceError.SERVER_UNAVAILABLE
                )
            }
            val response = acknowledgeDirectInvite(database, binding, envelopes, delivery)
                ?: return@post call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    mapOf("error" to "direct_invite_delivery_unavailable")
                )
            call.respond(HttpStatusCode.OK, response)
        }
    }
}

private fun com.guyghost.wakeve.models.Event.allowsDirectInvite(
    actorId: String,
    now: Instant
): Boolean =
    organizerId == actorId &&
        status == EventStatus.DRAFT &&
        aggregateSchemaVersion == 1L &&
        EventTemporalClassifier.classify(this, now) != TemporalClass.PAST

private fun DirectInviteBatchRequest.toDomainEnvelopes(
    binding: DirectInviteDeliveryBinding
): Set<DirectInviteDeliveryEnvelope>? {
    if (batchId.isBlank() || operationId.isBlank() || envelopes.isEmpty()) return null
    val now = Clock.System.now()
    val mapped = envelopes.map { request ->
        val recipientKey = runCatching { RecipientKey(request.recipientKey) }.getOrNull()
            ?: return null
        val expiry = runCatching { Instant.parse(request.expiresAt) }.getOrNull()
            ?: return null
        if (
            request.ciphertext.isBlank() || request.ciphertext == request.recipientKey ||
            request.keyVersion < 1 || expiry <= now
        ) return null
        runCatching {
            DirectInviteDeliveryEnvelope(
                binding = binding,
                recipientKey = recipientKey,
                ciphertext = request.ciphertext,
                keyVersion = request.keyVersion,
                expiresAt = request.expiresAt
            )
        }.getOrNull() ?: return null
    }
    if (mapped.map { it.recipientKey }.toSet().size != mapped.size) return null
    return mapped.toSet()
}

private sealed interface ExistingDirectInviteResponse {
    data class Exact(val response: DirectInviteBatchResponse) : ExistingDirectInviteResponse
    data object Pending : ExistingDirectInviteResponse
    data object Conflict : ExistingDirectInviteResponse
}

private fun existingDirectInviteResponse(
    database: WakeveDb,
    binding: DirectInviteDeliveryBinding,
    envelopes: Set<DirectInviteDeliveryEnvelope>
): ExistingDirectInviteResponse? {
    val byOperation = database.invitationExperienceQueries
        .selectDirectInviteBatchByOperationId(binding.operationId)
        .executeAsOneOrNull()
    val byBatch = database.invitationExperienceQueries
        .selectDirectInviteBatch(binding.batchId)
        .executeAsOneOrNull()
    val existing = byOperation ?: byBatch ?: return null
    if (
        existing.batch_id != binding.batchId ||
        existing.operation_id != binding.operationId ||
        existing.event_id != binding.eventId ||
        existing.actor_id != binding.actorId ||
        existing.access_revision != binding.accessRevision
    ) return ExistingDirectInviteResponse.Conflict

    val persistedEnvelopes = database.invitationExperienceQueries
        .selectDirectInviteDeliveryEnvelopes(binding.batchId)
        .executeAsList()
    val expected = envelopes.sortedBy { it.recipientKey.value }
    if (
        persistedEnvelopes.size != expected.size ||
        persistedEnvelopes.zip(expected).any { (row, envelope) ->
            row.recipient_key != envelope.recipientKey.value ||
                row.ciphertext != envelope.ciphertext ||
                row.key_version != envelope.keyVersion.toLong() ||
                row.expires_at != envelope.expiresAt
        }
    ) return ExistingDirectInviteResponse.Conflict
    if (existing.status == "PENDING_SYNC") return ExistingDirectInviteResponse.Pending
    if (existing.status != "COMPLETED") return ExistingDirectInviteResponse.Conflict
    val outcomes = database.invitationExperienceQueries
        .selectDirectInviteRecipientOutcomes(binding.batchId)
        .executeAsList()
        .map {
            DirectInviteOutcomeResponse(
                recipientKey = it.recipient_key,
                status = it.status,
                invitationId = it.invitation_id,
                reasonCode = it.reason_code
            )
        }
    return ExistingDirectInviteResponse.Exact(
        DirectInviteBatchResponse(
            batchId = binding.batchId,
            operationId = binding.operationId,
            status = "ACKNOWLEDGED",
            outcomes = outcomes
        )
    )
}

private fun persistDirectInvitePending(
    database: WakeveDb,
    binding: DirectInviteDeliveryBinding,
    envelopes: Set<DirectInviteDeliveryEnvelope>
): Boolean = runCatching {
    database.transaction {
        val now = Clock.System.now().toString()
        database.invitationExperienceQueries.insertDirectInviteBatch(
            batch_id = binding.batchId,
            event_id = binding.eventId,
            actor_id = binding.actorId,
            operation_id = binding.operationId,
            access_revision = binding.accessRevision,
            status = "PENDING_SYNC",
            created_at = now,
            updated_at = now,
            expires_at = envelopes.minOf { it.expiresAt }
        )
        envelopes.sortedBy { it.recipientKey.value }.forEach { envelope ->
            database.invitationExperienceQueries.insertDirectInviteRecipientOutcome(
                batch_id = binding.batchId,
                recipient_key = envelope.recipientKey.value,
                key_version = envelope.keyVersion.toLong(),
                status = "QUEUED_LOCAL",
                invitation_id = null,
                reason_code = null,
                expires_at = envelope.expiresAt,
                updated_at = now
            )
            database.invitationExperienceQueries.insertDirectInviteDeliveryEnvelope(
                batch_id = binding.batchId,
                recipient_key = envelope.recipientKey.value,
                ciphertext = envelope.ciphertext,
                key_version = envelope.keyVersion.toLong(),
                expires_at = envelope.expiresAt,
                transport_state = "QUEUED_LOCAL"
            )
        }
    }
}.isSuccess

private fun acknowledgeDirectInvite(
    database: WakeveDb,
    binding: DirectInviteDeliveryBinding,
    envelopes: Set<DirectInviteDeliveryEnvelope>,
    delivery: DirectInviteDeliveryResult
): DirectInviteBatchResponse? {
    val acknowledged = delivery as? DirectInviteDeliveryResult.Acknowledged ?: return null
    if (
        acknowledged.batchId != binding.batchId ||
        acknowledged.operationId != binding.operationId ||
        acknowledged.outcomesByRecipientKey.keys != envelopes.mapTo(linkedSetOf()) { it.recipientKey }
    ) return null

    return runCatching {
        database.transactionWithResult {
            val now = Clock.System.now().toString()
            val rows = acknowledged.outcomesByRecipientKey.entries
                .sortedBy { it.key.value }
                .map { (key, outcome) ->
                    val persisted = outcome.toResponse(key)
                    database.invitationExperienceQueries.updateDirectInviteRecipientOutcome(
                        status = persisted.status,
                        invitation_id = persisted.invitationId,
                        reason_code = persisted.reasonCode,
                        updated_at = now,
                        batch_id = binding.batchId,
                        recipient_key = key.value
                    )
                    database.invitationExperienceQueries.updateDirectInviteDeliveryEnvelopeState(
                        transport_state = outcome.transportState(),
                        batch_id = binding.batchId,
                        recipient_key = key.value
                    )
                    persisted
                }
            if (rows.any { it.status == "FAILED" }) return@transactionWithResult null
            database.invitationExperienceQueries.updateDirectInviteBatchStatus(
                status = "COMPLETED",
                updated_at = now,
                batch_id = binding.batchId,
                operation_id = binding.operationId
            )
            DirectInviteBatchResponse(
                batchId = binding.batchId,
                operationId = binding.operationId,
                status = "ACKNOWLEDGED",
                outcomes = rows
            )
        }
    }.getOrNull()
}

private fun DirectInviteRecipientOutcome.toResponse(key: RecipientKey): DirectInviteOutcomeResponse =
    when (this) {
        is DirectInviteRecipientOutcome.ServerAccepted -> DirectInviteOutcomeResponse(
            key.value,
            "SERVER_ACCEPTED",
            invitationId
        )
        is DirectInviteRecipientOutcome.Invalid -> DirectInviteOutcomeResponse(
            key.value,
            "INVALID",
            reasonCode = reason
        )
        is DirectInviteRecipientOutcome.Failed -> DirectInviteOutcomeResponse(
            key.value,
            "FAILED",
            reasonCode = error.name
        )
        DirectInviteRecipientOutcome.Cancelled -> DirectInviteOutcomeResponse(
            key.value,
            "CANCELLED"
        )
    }

private fun DirectInviteRecipientOutcome.transportState(): String = when (this) {
    is DirectInviteRecipientOutcome.ServerAccepted -> "SERVER_ACCEPTED"
    is DirectInviteRecipientOutcome.Invalid -> "FAILED_PERMANENT"
    is DirectInviteRecipientOutcome.Failed -> "FAILED_RETRYABLE"
    DirectInviteRecipientOutcome.Cancelled -> "CANCELLED"
}

private fun DirectInviteBackendDeliveryOwner.isConfiguredForProduction(): Boolean =
    (this as? DirectInviteBackendDeliveryAvailability)?.isAvailable ?: true

private fun DirectInviteBackendDeliveryOwner.sealingConfiguration(): Pair<String, Int>? {
    val availability = this as? DirectInviteBackendDeliveryAvailability ?: return null
    val publicKey = availability.sealingPublicKey ?: return null
    val keyVersion = availability.keyVersion ?: return null
    return publicKey to keyVersion
}
