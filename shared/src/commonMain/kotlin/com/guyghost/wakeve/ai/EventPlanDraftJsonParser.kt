package com.guyghost.wakeve.ai

import com.guyghost.wakeve.models.EventType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class EventPlanDraftJsonParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }
) {
    fun parse(rawText: String, source: EventPlanDraftSource): EventPlanDraft {
        val payload = rawText.substringAfter('{', rawText).substringBeforeLast('}', rawText)
        val normalizedPayload = if (payload.startsWith('{')) payload else "{$payload}"
        val dto = json.decodeFromString(EventPlanDraftDto.serializer(), normalizedPayload)
        return dto.toDraft(source).withMissingInformation()
    }
}

@Serializable
private data class EventPlanDraftDto(
    val destination: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val participantCount: Int? = null,
    val budgetPerPerson: MoneyAmountDto? = null,
    val eventType: String? = null,
    val constraints: List<String> = emptyList(),
    val missingInformation: List<String> = emptyList()
) {
    fun toDraft(source: EventPlanDraftSource): EventPlanDraft {
        val missing = missingInformation.mapNotNull { it.toMissingInformationOrNull() }
        return EventPlanDraft(
            destination = destination.cleanTextOrNull(),
            startDate = startDate.cleanTextOrNull(),
            endDate = endDate.cleanTextOrNull(),
            participantCount = participantCount?.takeIf { it > 0 },
            budgetPerPerson = budgetPerPerson?.toMoneyAmount(),
            eventType = eventType.toEventTypeOrOther(),
            constraints = constraints.mapNotNull { it.cleanTextOrNull() },
            missingInformation = missing,
            source = source
        )
    }
}

@Serializable
private data class MoneyAmountDto(
    val amount: Double? = null,
    @SerialName("currency")
    val currency: String? = null,
    val currencyCode: String? = null
) {
    fun toMoneyAmount(): MoneyAmount? {
        val value = amount?.takeIf { it > 0.0 } ?: return null
        return MoneyAmount(
            amount = value,
            currencyCode = currencyCode.cleanTextOrNull() ?: currency.cleanTextOrNull() ?: "EUR"
        )
    }
}

internal fun EventPlanDraft.withMissingInformation(): EventPlanDraft {
    val missing = buildSet {
        if (destination == null) add(EventPlanMissingInformation.DESTINATION)
        if (startDate == null) add(EventPlanMissingInformation.START_DATE)
        if (endDate == null) add(EventPlanMissingInformation.END_DATE)
        if (participantCount == null) add(EventPlanMissingInformation.PARTICIPANT_COUNT)
        if (budgetPerPerson == null) add(EventPlanMissingInformation.BUDGET_PER_PERSON)
        if (eventType == EventType.OTHER) add(EventPlanMissingInformation.EVENT_TYPE)
        addAll(missingInformation)
    }
    return copy(missingInformation = missing.toList())
}

internal fun String?.cleanTextOrNull(): String? =
    this?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

private fun String?.toEventTypeOrOther(): EventType {
    val normalized = cleanTextOrNull()
        ?.uppercase()
        ?.replace('-', '_')
        ?.replace(' ', '_')
        ?: return EventType.OTHER
    return EventType.entries.firstOrNull { it.name == normalized } ?: when {
        normalized in setOf("TRIP", "TRAVEL", "VOYAGE", "WEEKEND") -> EventType.OUTDOOR_ACTIVITY
        normalized in setOf("DINNER", "DINER", "RESTAURANT") -> EventType.FOOD_TASTING
        normalized in setOf("BIRTHDAY", "ANNIVERSAIRE") -> EventType.BIRTHDAY
        normalized in setOf("PARTY", "SOIREE", "FETE") -> EventType.PARTY
        else -> EventType.OTHER
    }
}

private fun String.toMissingInformationOrNull(): EventPlanMissingInformation? {
    val normalized = uppercase().replace('-', '_').replace(' ', '_')
    return EventPlanMissingInformation.entries.firstOrNull { it.name == normalized }
}
