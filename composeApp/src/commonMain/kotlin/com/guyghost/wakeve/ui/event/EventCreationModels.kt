package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.TimeSlot

data class DraftEventCreationRouteUiState(
    val hasPersistedDraft: Boolean = false,
    val lastSavedEventId: String? = null
)

data class DraftEventWizardUiState(
    val currentStep: Int = 0,
    val title: String = "",
    val description: String = "",
    val eventType: EventType = EventType.OTHER,
    val eventTypeCustom: String = "",
    val participantCount: String = "",
    val locations: List<PotentialLocation> = emptyList(),
    val timeSlots: List<TimeSlot> = emptyList(),
    val editingTimeSlot: TimeSlot? = null,
    val showLocationDialog: Boolean = false,
    val showTimeSlotInput: Boolean = false
)

enum class EventCreationComplexityLevel {
    Simple,
    Intermediate,
    Complex,
    VeryComplex
}

data class EventCreationFrictionSummary(
    val title: String,
    val complexityLabel: String,
    val estimatedTimeLabel: String,
    val actionCountLabel: String,
    val frictionLabel: String,
    val abandonmentRiskLabel: String,
    val userScoreLabel: String,
    val nextStepLabel: String
)

fun Event?.toDraftEventWizardUiState(): DraftEventWizardUiState =
    DraftEventWizardUiState(
        title = this?.title.orEmpty(),
        description = this?.description.orEmpty(),
        eventType = this?.eventType ?: EventType.OTHER,
        eventTypeCustom = this?.eventTypeCustom.orEmpty(),
        participantCount = this?.expectedParticipants?.toString().orEmpty(),
        timeSlots = this?.proposedSlots.orEmpty()
    )

fun DraftEventWizardUiState.toCreationFrictionSummary(): EventCreationFrictionSummary {
    val expectedParticipants = participantCount.toIntOrNull()
    val level = eventType.creationComplexityLevel(expectedParticipants)
    return EventCreationFrictionSummary(
        title = "Friction de creation",
        complexityLabel = level.creationComplexityLabel(),
        estimatedTimeLabel = level.creationEstimatedTimeLabel(),
        actionCountLabel = level.creationActionCountLabel(),
        frictionLabel = level.creationFrictionLabel(),
        abandonmentRiskLabel = level.creationAbandonmentRiskLabel(),
        userScoreLabel = level.creationUserScoreLabel(),
        nextStepLabel = level.creationNextStepLabel()
    )
}

internal fun EventType.creationComplexityLevel(expectedParticipants: Int?): EventCreationComplexityLevel {
    val baseLevel = when (this) {
        EventType.FOOD_TASTING,
        EventType.PARTY,
        EventType.SPORT_EVENT,
        EventType.WELLNESS_EVENT ->
            EventCreationComplexityLevel.Simple
        EventType.BIRTHDAY,
        EventType.FAMILY_GATHERING,
        EventType.CULTURAL_EVENT,
        EventType.SPORTS_EVENT,
        EventType.TECH_MEETUP,
        EventType.CREATIVE_WORKSHOP,
        EventType.OTHER,
        EventType.CUSTOM ->
            EventCreationComplexityLevel.Intermediate
        EventType.WORKSHOP,
        EventType.CONFERENCE,
        EventType.TEAM_BUILDING,
        EventType.OUTDOOR_ACTIVITY ->
            EventCreationComplexityLevel.Complex
        EventType.WEDDING ->
            EventCreationComplexityLevel.VeryComplex
    }

    return when {
        expectedParticipants != null && expectedParticipants >= 25 ->
            EventCreationComplexityLevel.VeryComplex
        expectedParticipants != null && expectedParticipants >= 10 &&
            baseLevel == EventCreationComplexityLevel.Simple ->
            EventCreationComplexityLevel.Intermediate
        expectedParticipants != null && expectedParticipants >= 15 &&
            baseLevel == EventCreationComplexityLevel.Intermediate ->
            EventCreationComplexityLevel.Complex
        else -> baseLevel
    }
}

private fun EventCreationComplexityLevel.creationComplexityLabel(): String =
    when (this) {
        EventCreationComplexityLevel.Simple -> "Parcours simple"
        EventCreationComplexityLevel.Intermediate -> "Parcours intermediaire"
        EventCreationComplexityLevel.Complex -> "Parcours complexe"
        EventCreationComplexityLevel.VeryComplex -> "Parcours tres complexe"
    }

private fun EventCreationComplexityLevel.creationEstimatedTimeLabel(): String =
    when (this) {
        EventCreationComplexityLevel.Simple -> "Temps estime : 2 a 4 min"
        EventCreationComplexityLevel.Intermediate -> "Temps estime : 4 a 7 min"
        EventCreationComplexityLevel.Complex -> "Temps estime : 8 a 12 min"
        EventCreationComplexityLevel.VeryComplex -> "Temps estime : 12 a 20 min"
    }

private fun EventCreationComplexityLevel.creationActionCountLabel(): String =
    when (this) {
        EventCreationComplexityLevel.Simple -> "Actions : 6 a 8"
        EventCreationComplexityLevel.Intermediate -> "Actions : 8 a 12"
        EventCreationComplexityLevel.Complex -> "Actions : 12 a 18"
        EventCreationComplexityLevel.VeryComplex -> "Actions : 18+"
    }

private fun EventCreationComplexityLevel.creationFrictionLabel(): String =
    when (this) {
        EventCreationComplexityLevel.Simple -> "Friction : faible"
        EventCreationComplexityLevel.Intermediate -> "Friction : moyenne"
        EventCreationComplexityLevel.Complex -> "Friction : elevee"
        EventCreationComplexityLevel.VeryComplex -> "Friction : tres elevee"
    }

private fun EventCreationComplexityLevel.creationAbandonmentRiskLabel(): String =
    when (this) {
        EventCreationComplexityLevel.Simple -> "Risque d'abandon : faible"
        EventCreationComplexityLevel.Intermediate -> "Risque d'abandon : modere"
        EventCreationComplexityLevel.Complex -> "Risque d'abandon : eleve"
        EventCreationComplexityLevel.VeryComplex -> "Risque d'abandon : critique sans guidage"
    }

private fun EventCreationComplexityLevel.creationUserScoreLabel(): String =
    when (this) {
        EventCreationComplexityLevel.Simple -> "Note attendue : 8/10"
        EventCreationComplexityLevel.Intermediate -> "Note attendue : 7/10"
        EventCreationComplexityLevel.Complex -> "Note attendue : 6/10"
        EventCreationComplexityLevel.VeryComplex -> "Note attendue : 5/10"
    }

private fun EventCreationComplexityLevel.creationNextStepLabel(): String =
    when (this) {
        EventCreationComplexityLevel.Simple ->
            "Gardez le flux court : titre, date, invites."
        EventCreationComplexityLevel.Intermediate ->
            "Ajoutez vite participants, lieux possibles et creneaux."
        EventCreationComplexityLevel.Complex ->
            "Cadrez transport, budget et programme des la creation."
        EventCreationComplexityLevel.VeryComplex ->
            "Decoupez en decisions : dates, destination, budget, logement, roles."
    }
