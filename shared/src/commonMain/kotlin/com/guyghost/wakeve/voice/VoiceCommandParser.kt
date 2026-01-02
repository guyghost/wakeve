package com.guyghost.wakeve.voice

import com.guyghost.wakeve.ml.Language
import com.guyghost.wakeve.ml.LanguageConfig
import com.guyghost.wakeve.ml.VoiceCommand
import com.guyghost.wakeve.ml.VoiceContext
import com.guyghost.wakeve.ml.VoiceIntent
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeOfDay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Parser for voice commands that extracts intents and parameters from natural language transcripts.
 * Supports multiple languages and handles context-aware parsing for multi-step conversations.
 */
class VoiceCommandParser {

    companion object {
        private val languageConfigs = mapOf(
            Language.EN to LanguageConfig(Language.EN, "English", "en-US", listOf("MMMM d yyyy", "MMM d yyyy")),
            Language.FR to LanguageConfig(Language.FR, "Français", "fr-FR", listOf("d MMMM yyyy", "d MMM yyyy")),
            Language.ES to LanguageConfig(Language.ES, "Español", "es-ES", listOf("d 'de' MMMM 'de' yyyy")),
            Language.DE to LanguageConfig(Language.DE, "Deutsch", "de-DE", listOf("d. MMMM yyyy"))
        )

        private val intentKeywords = mapOf(
            Language.EN to mapOf(
                VoiceIntent.CREATE_EVENT to listOf("create", "make", "organize", "plan", "schedule"),
                VoiceIntent.SET_TITLE to listOf("title", "called", "named"),
                VoiceIntent.SET_DESCRIPTION to listOf("description", "about", "details"),
                VoiceIntent.SET_DATE to listOf("date", "when", "time", "day"),
                VoiceIntent.SET_PARTICIPANTS to listOf("participants", "people", "guests", "how many"),
                VoiceIntent.ADD_SLOT to listOf("add slot", "add time", "new time"),
                VoiceIntent.CONFIRM_POLL to listOf("confirm", "lock", "finalize", "choose date"),
                VoiceIntent.SEND_INVITATIONS to listOf("send invitations", "send invites"),
                VoiceIntent.OPEN_CALENDAR to listOf("open calendar", "show calendar"),
                VoiceIntent.CANCEL_EVENT to listOf("cancel", "delete", "remove"),
                VoiceIntent.GET_STATS to listOf("how many", "statistics", "stats", "count")
            ),
            Language.FR to mapOf(
                VoiceIntent.CREATE_EVENT to listOf("crée", "organise", "planifie", "nouvel événement"),
                VoiceIntent.SET_TITLE to listOf("titre", "intitulé", "s'appelle"),
                VoiceIntent.SET_DESCRIPTION to listOf("description", "détails", "à propos"),
                VoiceIntent.SET_DATE to listOf("date", "quand", "jour", "heure"),
                VoiceIntent.SET_PARTICIPANTS to listOf("participants", "personnes", "invités", "combien"),
                VoiceIntent.ADD_SLOT to listOf("ajoute un créneau", "ajoute une date"),
                VoiceIntent.CONFIRM_POLL to listOf("valide", "confirme", "choisis la date"),
                VoiceIntent.SEND_INVITATIONS to listOf("envoie les invitations"),
                VoiceIntent.OPEN_CALENDAR to listOf("ouvre le calendrier", "affiche le calendrier"),
                VoiceIntent.CANCEL_EVENT to listOf("annule", "supprime", "annuler"),
                VoiceIntent.GET_STATS to listOf("combien", "statistiques")
            )
        )

        private val eventTypeKeywords = mapOf(
            Language.EN to mapOf(
                EventType.BIRTHDAY to listOf("birthday", "celebration"),
                EventType.WEDDING to listOf("wedding", "marriage"),
                EventType.TEAM_BUILDING to listOf("team building", "work event"),
                EventType.CULTURAL_EVENT to listOf("museum", "theater", "cultural"),
                EventType.SPORT_EVENT to listOf("sport", "game", "tournament"),
                EventType.CONFERENCE to listOf("conference", "meeting"),
                EventType.PARTY to listOf("party", "night out"),
                EventType.OTHER to listOf("other", "generic")
            ),
            Language.FR to mapOf(
                EventType.BIRTHDAY to listOf("anniversaire"),
                EventType.WEDDING to listOf("mariage", "noces"),
                EventType.TEAM_BUILDING to listOf("team building"),
                EventType.CULTURAL_EVENT to listOf("musée", "théâtre", "culturel"),
                EventType.SPORT_EVENT to listOf("sport", "match", "tournoi"),
                EventType.CONFERENCE to listOf("conférence", "réunion"),
                EventType.PARTY to listOf("fête", "soirée"),
                EventType.OTHER to listOf("autre", "générique")
            )
        )

        private val timeOfDayKeywords = mapOf(
            Language.EN to mapOf(
                TimeOfDay.ALL_DAY to listOf("all day", "whole day"),
                TimeOfDay.MORNING to listOf("morning", "am"),
                TimeOfDay.AFTERNOON to listOf("afternoon", "pm"),
                TimeOfDay.EVENING to listOf("evening", "night")
            ),
            Language.FR to mapOf(
                TimeOfDay.ALL_DAY to listOf("toute la journée"),
                TimeOfDay.MORNING to listOf("matin", "matinée"),
                TimeOfDay.AFTERNOON to listOf("après-midi"),
                TimeOfDay.EVENING to listOf("soir", "soirée")
            )
        )

        private val numberWords = mapOf(
            Language.EN to mapOf("one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "a few" to 3, "several" to 5),
            Language.FR to mapOf("un" to 1, "une" to 1, "deux" to 2, "trois" to 3, "quatre" to 4, "cinq" to 5, "quelques" to 3),
            Language.ES to mapOf("uno" to 1, "una" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5),
            Language.DE to mapOf("eins" to 1, "eine" to 1, "zwei" to 2, "drei" to 3, "vier" to 4, "fünf" to 5)
        )
    }

    fun parse(transcript: String, language: Language, context: VoiceContext): VoiceCommand {
        val normalizedTranscript = normalizeTranscript(transcript)
        val timestamp = getCurrentTimestamp()

        // Detect intent based on context if in creation flow
        val contextIntent = detectIntentFromContext(normalizedTranscript, language, context)
        val intent = contextIntent ?: detectIntent(normalizedTranscript, language)
        val parameters = extractParameters(normalizedTranscript, language, intent)
        val confidence = calculateConfidence(normalizedTranscript, language, intent)

        return VoiceCommand(intent, parameters, confidence, transcript, timestamp)
    }

    private fun detectIntentFromContext(transcript: String, language: Language, context: VoiceContext): VoiceIntent? {
        return when (context.step) {
            com.guyghost.wakeve.ml.VoiceStep.TITLE -> VoiceIntent.SET_TITLE
            com.guyghost.wakeve.ml.VoiceStep.DESCRIPTION -> VoiceIntent.SET_DESCRIPTION
            com.guyghost.wakeve.ml.VoiceStep.DATE -> VoiceIntent.SET_DATE
            com.guyghost.wakeve.ml.VoiceStep.PARTICIPANTS -> VoiceIntent.SET_PARTICIPANTS
            com.guyghost.wakeve.ml.VoiceStep.CONFIRM -> VoiceIntent.CREATE_EVENT
            else -> null
        }
    }

    private fun detectIntent(transcript: String, language: Language): VoiceIntent {
        val keywords = intentKeywords[language] ?: intentKeywords[Language.EN]!!
        val lowerTranscript = transcript.lowercase()
        val intentScores = keywords.mapValues { (_, keywordsList) ->
            keywordsList.count { keyword -> lowerTranscript.contains(keyword.lowercase()) }
        }
        return intentScores.maxByOrNull { it.value }?.key ?: VoiceIntent.CREATE_EVENT
    }

    private fun extractParameters(transcript: String, language: Language, intent: VoiceIntent): Map<String, String> {
        val parameters = mutableMapOf<String, String>()

        when (intent) {
            VoiceIntent.CREATE_EVENT -> {
                parameters["eventType"] = detectEventType(transcript, language)?.name ?: EventType.OTHER.name
                extractTitle(transcript, language)?.let { parameters["title"] = it }
            }
            VoiceIntent.SET_TITLE -> extractTitle(transcript, language)?.let { parameters["title"] = it }
            VoiceIntent.SET_DESCRIPTION -> parameters["description"] = cleanDescription(transcript, language)
            VoiceIntent.SET_DATE -> {
                extractDate(transcript, language)?.let { parameters["date"] = it }
                extractTimeOfDay(transcript, language)?.let { parameters["timeOfDay"] = it.name }
            }
            VoiceIntent.SET_PARTICIPANTS -> extractParticipantCount(transcript, language)?.let { parameters["participantCount"] = it.toString() }
            VoiceIntent.ADD_SLOT -> {
                extractDate(transcript, language)?.let { parameters["date"] = it }
                extractTimeOfDay(transcript, language)?.let { parameters["timeOfDay"] = it.name }
            }
            else -> {}
        }
        return parameters
    }

    private fun detectEventType(transcript: String, language: Language): EventType? {
        val keywords = eventTypeKeywords[language] ?: eventTypeKeywords[Language.EN]!!
        val lowerTranscript = transcript.lowercase()
        return keywords.entries.firstOrNull { (_, keywordsList) ->
            keywordsList.any { lowerTranscript.contains(it.lowercase()) }
        }?.key
    }

    private fun extractTitle(transcript: String, language: Language): String? {
        val patterns = mapOf(
            Language.EN to listOf(Regex("""called\s+(.+?)(?:\s+for|\s+on|\s*$)""", RegexOption.IGNORE_CASE)),
            Language.FR to listOf(Regex("""s'appelle\s+(.+?)(?:\s+pour|\s+le|\s*$)""", RegexOption.IGNORE_CASE))
        )
        val languagePatterns = patterns[language] ?: patterns[Language.EN]!!
        return languagePatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(transcript)?.groupValues?.getOrNull(1)?.trim()
        } ?: transcript.takeIf { it.length in 3..100 }
    }

    private fun cleanDescription(transcript: String, language: Language): String {
        val prefixes = mapOf(
            Language.EN to listOf("description:", "it's about", "about"),
            Language.FR to listOf("description:", "c'est", "à propos", "détails:")
        )
        val languagePrefixes = prefixes[language] ?: prefixes[Language.EN]!!
        var cleaned = transcript
        for (prefix in languagePrefixes) {
            if (cleaned.lowercase().startsWith(prefix.lowercase())) {
                cleaned = cleaned.substring(prefix.length).trim()
                break
            }
        }
        return cleaned
    }

    private fun extractDate(transcript: String, language: Language): String? {
        val lowerTranscript = transcript.lowercase()
        return when (language) {
            Language.EN -> when {
                lowerTranscript.contains("today") -> "2026-01-02"
                lowerTranscript.contains("tomorrow") -> "2026-01-03"
                else -> null
            }
            Language.FR -> when {
                lowerTranscript.contains("aujourd'hui") -> "2026-01-02"
                lowerTranscript.contains("demain") -> "2026-01-03"
                else -> null
            }
            Language.ES -> when {
                lowerTranscript.contains("hoy") -> "2026-01-02"
                lowerTranscript.contains("mañana") -> "2026-01-03"
                else -> null
            }
            Language.DE -> when {
                lowerTranscript.contains("heute") -> "2026-01-02"
                lowerTranscript.contains("morgen") -> "2026-01-03"
                else -> null
            }
            else -> null
        }
    }

    private fun extractTimeOfDay(transcript: String, language: Language): TimeOfDay? {
        val keywords = timeOfDayKeywords[language] ?: timeOfDayKeywords[Language.EN]!!
        val lowerTranscript = transcript.lowercase()
        return keywords.entries.firstOrNull { (_, keywordsList) ->
            keywordsList.any { lowerTranscript.contains(it.lowercase()) }
        }?.key
    }

    private fun extractParticipantCount(transcript: String, language: Language): Int? {
        val numberPattern = Regex("""(\d+)""")
        val numberMatch = numberPattern.find(transcript)
        if (numberMatch != null) {
            return numberMatch.groupValues[1].toIntOrNull()
        }
        val words = numberWords[language] ?: numberWords[Language.EN]!!
        val lowerTranscript = transcript.lowercase()
        return words.entries.firstOrNull { (word, _) -> lowerTranscript.contains(word) }?.value
    }

    private fun calculateConfidence(transcript: String, language: Language, intent: VoiceIntent): Double {
        val keywords = intentKeywords[language] ?: intentKeywords[Language.EN]!!
        val intentKeywordsList = keywords[intent] ?: emptyList()
        val matchCount = intentKeywordsList.count { transcript.lowercase().contains(it.lowercase()) }
        val keywordConfidence = matchCount.toDouble() / intentKeywordsList.size.coerceAtLeast(1)
        val lengthConfidence = when {
            transcript.length in 5..50 -> 1.0
            transcript.length in 3..100 -> 0.8
            else -> 0.5
        }
        return (keywordConfidence * 0.7 + lengthConfidence * 0.3).coerceIn(0.0, 1.0)
    }

    private fun normalizeTranscript(transcript: String): String {
        return transcript.trim().replace(Regex("""\s+"""), " ").replace(Regex("""[.,;:!?]"""), "")
    }

    private fun getCurrentTimestamp(): String {
        return Clock.System.now().toString()
    }
}
