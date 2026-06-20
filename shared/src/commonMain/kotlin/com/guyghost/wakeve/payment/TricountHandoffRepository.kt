package com.guyghost.wakeve.payment

import com.guyghost.wakeve.database.WakeveDb
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import com.guyghost.wakeve.TricountHandoff as SqlTricountHandoff

@Serializable
data class TricountHandoffRecord(
    val eventId: String,
    val provider: String,
    val providerId: String?,
    val providerUrl: String?,
    val syncStatus: String,
    val trusted: Boolean,
    val explicitNotNeeded: Boolean,
    val lastSyncAt: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class PaymentReadiness(
    val eventId: String,
    val complete: Boolean,
    val blockers: List<String>,
    val handoff: TricountHandoffRecord?
)

class TricountHandoffRepository(private val db: WakeveDb) {
    fun linkHandoff(
        eventId: String,
        provider: String,
        providerId: String,
        providerUrl: String,
        syncStatus: String
    ): TricountHandoffRecord {
        val normalizedEventId = eventId.trim()
        val normalizedProvider = provider.trim().uppercase()
        val normalizedProviderId = providerId.trim()
        val normalizedProviderUrl = providerUrl.trim()
        val normalizedSyncStatus = syncStatus.trim().uppercase()

        require(normalizedEventId.isNotEmpty()) { "eventId is required" }
        require(normalizedProvider == "TRICOUNT") { "Unsupported provider: $normalizedProvider" }
        require(normalizedProviderId.isNotEmpty()) { "providerId is required" }
        require(normalizedProviderId.length <= 200) { "providerId must not exceed 200 characters" }
        require(normalizedSyncStatus in setOf("LINKED", "SYNCED")) {
            "syncStatus must be LINKED or SYNCED"
        }
        require(isTrustedProviderUrl(normalizedProvider, normalizedProviderUrl)) {
            "Suspicious $normalizedProvider URL rejected"
        }

        val now = Clock.System.now().toString()
        db.tricountHandoffQueries.upsertHandoff(
            eventId = normalizedEventId,
            provider = normalizedProvider,
            providerId = normalizedProviderId,
            providerUrl = normalizedProviderUrl,
            syncStatus = normalizedSyncStatus,
            trusted = 1L,
            explicitNotNeeded = 0L,
            lastSyncAt = if (normalizedSyncStatus == "SYNCED") now else null,
            createdAt = now,
            updatedAt = now
        )
        val handoff = getHandoff(normalizedEventId)!!
        queueHandoffSync(handoff, "CREATE")
        return handoff
    }

    fun markNotNeeded(eventId: String, decidedBy: String): TricountHandoffRecord {
        val normalizedEventId = eventId.trim()
        val normalizedDecidedBy = decidedBy.trim()
        require(normalizedEventId.isNotEmpty()) { "eventId is required" }
        require(normalizedDecidedBy.isNotEmpty()) { "decidedBy is required" }
        val now = Clock.System.now().toString()
        db.tricountHandoffQueries.upsertHandoff(
            eventId = normalizedEventId,
            provider = "TRICOUNT",
            providerId = null,
            providerUrl = null,
            syncStatus = "NOT_NEEDED",
            trusted = 1L,
            explicitNotNeeded = 1L,
            lastSyncAt = now,
            createdAt = now,
            updatedAt = now
        )
        val handoff = getHandoff(normalizedEventId)!!
        queueHandoffSync(handoff, "UPDATE")
        return handoff
    }

    fun getHandoff(eventId: String): TricountHandoffRecord? =
        db.tricountHandoffQueries.selectByEventId(eventId.trim()).executeAsOneOrNull()?.toModel()

    fun unlinkHandoff(eventId: String) {
        val normalizedEventId = eventId.trim()
        require(normalizedEventId.isNotEmpty()) { "eventId is required" }
        val existing = getHandoff(normalizedEventId)
        db.tricountHandoffQueries.deleteByEventId(normalizedEventId)
        if (existing != null) {
            queueHandoffSync(
                existing.copy(syncStatus = "UNLINKED", providerUrl = null, providerId = null),
                "DELETE"
            )
        }
    }

    fun getPaymentReadiness(eventId: String): PaymentReadiness {
        val normalizedEventId = eventId.trim()
        val handoff = getHandoff(normalizedEventId)
        val complete = handoff?.explicitNotNeeded == true ||
            (handoff?.trusted == true && handoff.providerId != null && handoff.providerUrl != null &&
                handoff.syncStatus in setOf("LINKED", "SYNCED"))

        return PaymentReadiness(
            eventId = normalizedEventId,
            complete = complete,
            blockers = if (complete) emptyList() else listOf("TRICOUNT_HANDOFF_REQUIRED"),
            handoff = handoff
        )
    }

    fun isTrustedProviderUrl(provider: String, providerUrl: String): Boolean {
        if (!provider.equals("TRICOUNT", ignoreCase = true)) return false
        if (containsTemplateMarker(providerUrl)) return false
        val match = Regex("""^https://([^/?#]+)(?:[/?#].*)?$""").find(providerUrl.trim()) ?: return false
        val host = match.groupValues[1].lowercase()
        return host == "tricount.com" || host == "www.tricount.com" || host.endsWith(".tricount.com")
    }

    private fun queueHandoffSync(handoff: TricountHandoffRecord, operation: String) {
        val baseTimestamp = Clock.System.now().toString()
        val existing = db.syncMetadataQueries
            .selectByEntity("tricount_handoff", handoff.eventId)
            .executeAsList()
        val timestamp = if (existing.none { it.timestamp == baseTimestamp }) {
            baseTimestamp
        } else {
            "$baseTimestamp-${existing.size}"
        }
        db.syncMetadataQueries.insertSyncMetadataWithPayload(
            id = "sync-tricount-${operation.lowercase()}-${handoff.eventId}-$timestamp",
            entityType = "tricount_handoff",
            entityId = handoff.eventId,
            operation = operation,
            payload = buildHandoffPayload(handoff),
            timestamp = timestamp,
            retryState = "READY",
            retryCount = 0,
            synced = 0
        )
    }

    private fun buildHandoffPayload(handoff: TricountHandoffRecord): String = buildString {
        append("{")
        appendJson("eventId", handoff.eventId)
        append(",")
        appendJson("provider", handoff.provider)
        append(",")
        appendJson("providerId", handoff.providerId.orEmpty())
        append(",")
        appendJson("providerUrl", handoff.providerUrl.orEmpty())
        append(",")
        appendJson("syncStatus", handoff.syncStatus)
        append(",")
        append("\"trusted\":")
        append(handoff.trusted)
        append(",")
        append("\"explicitNotNeeded\":")
        append(handoff.explicitNotNeeded)
        append("}")
    }

    private fun containsTemplateMarker(value: String): Boolean =
        value.contains("\${") ||
            value.contains("{") ||
            value.contains("}") ||
            Regex("""\$[A-Za-z_][A-Za-z0-9_]*""").containsMatchIn(value)

    private fun StringBuilder.appendJson(key: String, value: String) {
        append("\"")
        append(key)
        append("\":\"")
        append(value.replace("\\", "\\\\").replace("\"", "\\\""))
        append("\"")
    }

    private fun SqlTricountHandoff.toModel(): TricountHandoffRecord =
        TricountHandoffRecord(
            eventId = eventId,
            provider = provider,
            providerId = providerId,
            providerUrl = providerUrl,
            syncStatus = syncStatus,
            trusted = trusted == 1L,
            explicitNotNeeded = explicitNotNeeded == 1L,
            lastSyncAt = lastSyncAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
