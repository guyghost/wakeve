package com.guyghost.wakeve.ai

import android.os.Build
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class MlKitEventPlanningAiAssistant(
    private val fallback: EventPlanningAiAssistant = RuleBasedEventPlanningAiAssistant(),
    private val parser: EventPlanDraftJsonParser = EventPlanDraftJsonParser()
) : EventPlanningAiAssistant {
    private val generativeModel by lazy { Generation.getClient() }

    override suspend fun availability(): EventPlanningAiAvailability =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            EventPlanningAiAvailability.UNAVAILABLE
        } else {
            generativeModel.checkStatus().toWakeveAvailability()
        }

    override fun extractEventPlan(prompt: EventPlanningPrompt): Flow<EventPlanningAiUpdate> = flow {
        when (val availability = availability()) {
            EventPlanningAiAvailability.AVAILABLE -> {
                emit(EventPlanningAiUpdate.Availability(EventPlanningAiAvailability.AVAILABLE))
                val response = generativeModel.generateContent(
                    generateContentRequest(TextPart(buildPrompt(prompt))) {
                        temperature = 0.1f
                        topK = 10
                        candidateCount = 1
                        maxOutputTokens = 256
                    }
                )
                val text = response.candidates.firstOrNull()?.text.orEmpty()
                val draft = parser.parse(text, EventPlanDraftSource.ML_KIT_GENAI)
                emit(EventPlanningAiUpdate.Draft(draft))
            }
            EventPlanningAiAvailability.DOWNLOADABLE -> {
                emit(EventPlanningAiUpdate.Availability(EventPlanningAiAvailability.DOWNLOADABLE))
                var downloaded = false
                generativeModel.download().collect { status ->
                    when (status) {
                        DownloadStatus.DownloadCompleted -> downloaded = true
                        is DownloadStatus.DownloadFailed -> downloaded = false
                        is DownloadStatus.DownloadProgress,
                        is DownloadStatus.DownloadStarted -> Unit
                    }
                }
                if (downloaded && availability() == EventPlanningAiAvailability.AVAILABLE) {
                    emitAll(extractEventPlan(prompt))
                } else {
                    emitAll(fallback.extractEventPlan(prompt))
                }
            }
            else -> {
                emit(EventPlanningAiUpdate.Availability(availability))
                emitAll(fallback.extractEventPlan(prompt))
            }
        }
    }.catch {
        emit(EventPlanningAiUpdate.Error("Gemini Nano is unavailable. Using local fallback."))
        emitAll(fallback.extractEventPlan(prompt))
    }

    private fun buildPrompt(prompt: EventPlanningPrompt): String =
        """
        Extract a Wakeve event planning draft from the user's text.
        Return only compact JSON with these keys:
        destination string|null,
        startDate ISO-8601 yyyy-MM-dd string|null,
        endDate ISO-8601 yyyy-MM-dd string|null,
        participantCount integer|null,
        budgetPerPerson object|null with amount number and currencyCode string,
        eventType one of BIRTHDAY,WEDDING,TEAM_BUILDING,CONFERENCE,WORKSHOP,PARTY,SPORTS_EVENT,CULTURAL_EVENT,FAMILY_GATHERING,SPORT_EVENT,OUTDOOR_ACTIVITY,FOOD_TASTING,TECH_MEETUP,WELLNESS_EVENT,CREATIVE_WORKSHOP,OTHER,CUSTOM,
        constraints string array,
        missingInformation string array using DESTINATION,START_DATE,END_DATE,PARTICIPANT_COUNT,BUDGET_PER_PERSON,EVENT_TYPE,CONSTRAINTS.
        Do not invent values. Use locale ${prompt.localeTag}. If a date omits year, use ${prompt.referenceYear ?: 2026}.
        User text: ${prompt.text}
        """.trimIndent()

    private fun Int.toWakeveAvailability(): EventPlanningAiAvailability = when (this) {
        FeatureStatus.AVAILABLE -> EventPlanningAiAvailability.AVAILABLE
        FeatureStatus.DOWNLOADABLE -> EventPlanningAiAvailability.DOWNLOADABLE
        FeatureStatus.DOWNLOADING -> EventPlanningAiAvailability.DOWNLOADING
        else -> EventPlanningAiAvailability.UNAVAILABLE
    }
}
