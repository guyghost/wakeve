package com.guyghost.wakeve.ai

import com.guyghost.wakeve.models.EventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RuleBasedEventPlanningAiAssistant(
    private val defaultYear: Int = 2026
) : EventPlanningAiAssistant {
    override suspend fun availability(): EventPlanningAiAvailability = EventPlanningAiAvailability.FALLBACK_ONLY

    override fun extractEventPlan(prompt: EventPlanningPrompt): Flow<EventPlanningAiUpdate> = flow {
        emit(EventPlanningAiUpdate.Availability(EventPlanningAiAvailability.FALLBACK_ONLY))
        emit(EventPlanningAiUpdate.Draft(parse(prompt)))
    }

    fun parse(prompt: EventPlanningPrompt): EventPlanDraft {
        val text = prompt.text.trim()
        val year = prompt.referenceYear ?: defaultYear
        return EventPlanDraft(
            destination = extractDestination(text),
            startDate = extractDateRange(text, year).first,
            endDate = extractDateRange(text, year).second,
            participantCount = extractParticipantCount(text),
            budgetPerPerson = extractBudget(text),
            eventType = inferEventType(text),
            constraints = extractConstraints(text),
            source = EventPlanDraftSource.RULE_BASED
        ).withMissingInformation()
    }

    private fun extractDestination(text: String): String? {
        val patterns = listOf(
            Regex("""(?i)(?:\ba\b|à|au|aux|vers|pour|destination)\s+(.+?)(?=\s+(?:du|de|des|le|la)\s+\d{1,2}\b|,\s*\d|\s+\d{1,3}\s*(?:personnes|participants|invites|invités|people|guests)\b|,|\.|$)"""),
            Regex("""(?i)\b(?:in|to)\s+(.+?)(?=\s+(?:from|on)\s+\d{1,2}\b|,\s*\d|\s+\d{1,3}\s*(?:people|guests)\b|,|\.|$)""")
        )
        return patterns.firstNotNullOfOrNull { regex ->
            regex.find(text)?.groupValues?.getOrNull(1)
        }?.trim()?.trimEnd(',', '.', ';')
    }

    private fun extractDateRange(text: String, year: Int): Pair<String?, String?> {
        val sameMonth = Regex(
            """(?i)\bdu\s+(\d{1,2})\s+(?:au|a|à|-)\s+(\d{1,2})\s+([a-zà-û]+)"""
        ).find(text)
        if (sameMonth != null) {
            val startDay = sameMonth.groupValues[1].toIntOrNull()
            val endDay = sameMonth.groupValues[2].toIntOrNull()
            val month = parseMonth(sameMonth.groupValues[3])
            if (startDay != null && endDay != null && month != null) {
                return "${year}-${month.twoDigits()}-${startDay.twoDigits()}" to
                    "${year}-${month.twoDigits()}-${endDay.twoDigits()}"
            }
        }

        val isoDates = Regex("""\b(\d{4}-\d{2}-\d{2})\b""").findAll(text).map { it.value }.toList()
        return isoDates.getOrNull(0) to isoDates.getOrNull(1)
    }

    private fun extractParticipantCount(text: String): Int? {
        val regexes = listOf(
            Regex("""(?i)\b(\d{1,3})\s*(?:personnes|participants|invites|invités|people|guests)\b"""),
            Regex("""(?i)\b(?:pour|for)\s+(\d{1,3})\b""")
        )
        return regexes.firstNotNullOfOrNull { regex ->
            regex.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }?.takeIf { it > 0 }
    }

    private fun extractBudget(text: String): MoneyAmount? {
        val match = Regex("""(?i)\b(?:budget\s*)?(\d+(?:[,.]\d+)?)\s*(€|eur|euros|usd|\$)""").find(text)
            ?: return null
        val amount = match.groupValues[1].replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
        val currency = when (match.groupValues[2].lowercase()) {
            "$", "usd" -> "USD"
            else -> "EUR"
        }
        return MoneyAmount(amount = amount, currencyCode = currency)
    }

    private fun inferEventType(text: String): EventType {
        val normalized = text.lowercase()
        return when {
            listOf("weekend", "voyage", "part ", "partir", "trip", "biarritz", "plage").any { it in normalized } ->
                EventType.OUTDOOR_ACTIVITY
            listOf("anniversaire", "birthday").any { it in normalized } -> EventType.BIRTHDAY
            listOf("diner", "dîner", "restaurant", "repas", "dinner").any { it in normalized } -> EventType.FOOD_TASTING
            listOf("soirée", "soiree", "fête", "fete", "party").any { it in normalized } -> EventType.PARTY
            else -> EventType.OTHER
        }
    }

    private fun extractConstraints(text: String): List<String> {
        val constraints = mutableListOf<String>()
        if (Regex("""(?i)\bpar personne\b|\bper person\b""").containsMatchIn(text)) {
            constraints += "Budget per person"
        }
        if (Regex("""(?i)\bsans voiture\b|\btrain\b|\btransport\b""").containsMatchIn(text)) {
            constraints += "Transport preference"
        }
        return constraints
    }

    private fun parseMonth(raw: String): Int? = when (raw.lowercase().trimEnd('.')) {
        "janvier", "january" -> 1
        "fevrier", "février", "february" -> 2
        "mars", "march" -> 3
        "avril", "april" -> 4
        "mai", "may" -> 5
        "juin", "june" -> 6
        "juillet", "july" -> 7
        "aout", "août", "august" -> 8
        "septembre", "september" -> 9
        "octobre", "october" -> 10
        "novembre", "november" -> 11
        "decembre", "décembre", "december" -> 12
        else -> null
    }

    private fun Int.twoDigits(): String = toString().padStart(2, '0')
}
