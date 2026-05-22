package com.guyghost.wakeve.payment

import com.guyghost.wakeve.database.WakeveDb
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import com.guyghost.wakeve.Pot as SqlPot

@Serializable
data class PaymentPotRecord(
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
    val createdAt: String,
    val closedAt: String?
)

class PaymentPotRepository(private val db: WakeveDb) {
    fun createPot(
        eventId: String,
        organizerId: String,
        goalAmount: Double,
        title: String,
        currency: String = "EUR",
        paymentProvider: String = "TRICOUNT",
        tricountGroupId: String? = null,
        tricountGroupUrl: String? = null
    ): PaymentPotRecord {
        require(goalAmount >= 0.0) { "Goal amount cannot be negative" }
        require(tricountGroupUrl == null || isTrustedPaymentLink(paymentProvider, tricountGroupUrl)) {
            "Suspicious $paymentProvider URL rejected"
        }
        val now = Clock.System.now().toString()
        val id = "pot-${Clock.System.now().toEpochMilliseconds()}-${(0..9999).random()}"
        db.potQueries.insertPot(
            id = id,
            eventId = eventId,
            organizerId = organizerId,
            goalAmount = goalAmount,
            currentAmount = 0.0,
            currency = currency,
            title = title,
            status = "ACTIVE",
            paymentProvider = paymentProvider,
            tricountGroupId = tricountGroupId,
            tricountGroupUrl = tricountGroupUrl,
            createdAt = now,
            closedAt = null
        )
        val pot = getPotById(id)!!
        queuePotSync(pot, "CREATE")
        return pot
    }

    fun getPotById(id: String): PaymentPotRecord? =
        db.potQueries.selectById(id).executeAsOneOrNull()?.toModel()

    fun getActivePotForEvent(eventId: String): PaymentPotRecord? =
        db.potQueries.selectActiveByEvent(eventId).executeAsOneOrNull()?.toModel()

    fun closePot(id: String): PaymentPotRecord? {
        db.potQueries.updatePotStatus(
            status = "CLOSED",
            closedAt = Clock.System.now().toString(),
            id = id
        )
        val pot = getPotById(id)
        if (pot != null) {
            queuePotSync(pot, "UPDATE")
        }
        return pot
    }

    private fun queuePotSync(pot: PaymentPotRecord, operation: String) {
        val baseTimestamp = Clock.System.now().toString()
        val existing = db.syncMetadataQueries.selectByEntity("payment_pot", pot.id).executeAsList()
        val timestamp = if (existing.none { it.timestamp == baseTimestamp }) {
            baseTimestamp
        } else {
            "$baseTimestamp-${existing.size}"
        }
        db.syncMetadataQueries.insertSyncMetadataWithPayload(
            id = "sync-payment-pot-${operation.lowercase()}-${pot.id}-$timestamp",
            entityType = "payment_pot",
            entityId = pot.id,
            operation = operation,
            payload = buildPotPayload(pot),
            timestamp = timestamp,
            retryState = "READY",
            retryCount = 0,
            synced = 0
        )
    }

    private fun buildPotPayload(pot: PaymentPotRecord): String = buildString {
        append("{")
        appendJson("id", pot.id)
        append(",")
        appendJson("eventId", pot.eventId)
        append(",")
        appendJson("organizerId", pot.organizerId)
        append(",")
        append("\"goalAmount\":")
        append(pot.goalAmount)
        append(",")
        appendJson("currency", pot.currency)
        append(",")
        appendJson("title", pot.title)
        append(",")
        appendJson("status", pot.status)
        append(",")
        appendJson("paymentProvider", pot.paymentProvider)
        append(",")
        appendJson("tricountGroupId", pot.tricountGroupId.orEmpty())
        append(",")
        appendJson("tricountGroupUrl", pot.tricountGroupUrl.orEmpty())
        append("}")
    }

    private fun StringBuilder.appendJson(key: String, value: String) {
        append("\"")
        append(key)
        append("\":\"")
        append(value.replace("\\", "\\\\").replace("\"", "\\\""))
        append("\"")
    }

    private fun isTrustedPaymentLink(provider: String, url: String): Boolean {
        if (containsTemplateMarker(url)) return false
        val match = Regex("""^https://([^/?#]+)(?:[/?#].*)?$""").find(url.trim()) ?: return false
        val host = match.groupValues[1].lowercase()
        return if (provider.equals("TRICOUNT", ignoreCase = true)) {
            host == "tricount.com" || host == "www.tricount.com" || host.endsWith(".tricount.com")
        } else {
            host.isNotBlank()
        }
    }

    private fun containsTemplateMarker(value: String): Boolean =
        value.contains("\${") ||
            value.contains("{") ||
            value.contains("}") ||
            Regex("""\$[A-Za-z_][A-Za-z0-9_]*""").containsMatchIn(value)

    private fun SqlPot.toModel(): PaymentPotRecord =
        PaymentPotRecord(
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
            createdAt = createdAt,
            closedAt = closedAt
        )
}
