package com.guyghost.wakeve.ai

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiOutputMetadataContractTest {
    @Test
    fun `event plan draft exposes deterministic interaction metadata`() = runTest {
        val draft = RuleBasedEventPlanningAiAssistant(defaultYear = 2026)
            .extractEventPlan(
                EventPlanningPrompt(
                    text = "On part à Biarritz du 12 au 15 juillet, 8 personnes, budget 300€ par personne.",
                    referenceYear = 2026
                )
            )
            .filterIsInstance<EventPlanningAiUpdate.Draft>()
            .last()
            .draft

        assertEquals(AiUseCase.EVENT_PLAN_DRAFT, draft.metadata.useCase)
        assertEquals(AiInferenceRoute.LOCAL_FALLBACK, draft.metadata.routing.route)
        assertTrue(AiInteractionMetadataPolicy.canExposeApplyAction(draft.metadata))
    }

    @Test
    fun `event summary exposes metadata in addition to routing`() = runTest {
        val assistant = OnDeviceEventSummaryAiAssistant(
            localClient = DeterministicAiTextGenerationClient(
                availability = AiModelAvailability.AVAILABLE,
                response = """
                    SUMMARY: Weekend in Biarritz.
                    ADVICE:
                    - Confirm trains.
                    PACKING:
                    - Sunscreen
                    MISSING:
                    - Lodging address
                """.trimIndent()
            )
        )

        val summary = assistant.summarize(sampleContext())
            .filterIsInstance<EventSummaryAiUpdate.SummaryReady>()
            .last()
            .summary

        assertEquals(summary.routing, summary.metadata.routing)
        assertEquals(AiUseCase.EVENT_SUMMARY, summary.metadata.useCase)
        assertEquals("Event summary draft", summary.metadata.sanitizedOutputSummary)
        assertFalse(summary.metadata.sanitizedOutputSummary.contains(summary.shortSummary))
        assertFalse(summary.metadata.routing.cloudUsed)
        assertTrue(AiInteractionMetadataPolicy.canExposeApplyAction(summary.metadata))
    }

    @Test
    fun `generated organizer message records cloud metadata when cloud fallback is used`() = runTest {
        val assistant = HybridOrganizerMessageAiAssistant(
            onDeviceClient = DeterministicAiTextGenerationClient(
                availability = AiModelAvailability.UNAVAILABLE,
                response = "Unused local response"
            ),
            cloudClient = DeterministicAiTextGenerationClient(
                availability = AiModelAvailability.AVAILABLE,
                response = "Petit rappel budget: confirmez les derniers details.",
                providerName = "Firebase AI Logic",
                modelName = "gemini-flash-lite",
                route = AiInferenceRoute.CLOUD_FIREBASE_AI_LOGIC
            )
        )

        val message = assistant.generateMessage(
            OrganizerMessageRequest(
                context = sampleContext(),
                messageType = OrganizerMessageType.BUDGET_REMINDER
            )
        ).filterIsInstance<OrganizerMessageAiUpdate.MessageReady>().last().message

        assertEquals(message.routing, message.metadata.routing)
        assertEquals(AiUseCase.ORGANIZER_MESSAGE, message.metadata.useCase)
        assertEquals("Organizer message draft", message.metadata.sanitizedOutputSummary)
        assertFalse(message.metadata.sanitizedOutputSummary.contains(message.body))
        assertTrue(message.metadata.routing.cloudUsed)
        assertTrue(AiInteractionMetadataPolicy.canExposeApplyAction(message.metadata))
    }

    @Test
    fun `planning agent session starts with review only metadata`() = runTest {
        val session = DeterministicPlanningAgentClient()
            .startSession(sampleContext())
            .filterIsInstance<PlanningAgentEvent.SessionStarted>()
            .first()
            .session

        assertEquals(AiUseCase.PLANNING_AGENT, session.metadata.useCase)
        assertEquals("Planning agent session context", session.metadata.sanitizedInputSummary)
        assertFalse(session.metadata.sanitizedInputSummary.contains(session.title))
        assertEquals(AiValidationStatus.NEEDS_REVIEW, session.metadata.validation.status)
        assertTrue(AiInteractionMetadataPolicy.canExposeApplyAction(session.metadata))
    }

    private fun sampleContext(): EventPlanningPromptContext =
        EventPlanningPromptContext(
            eventId = "event-biarritz",
            title = "Weekend surf",
            destination = "Biarritz",
            dates = listOf("2026-07-12", "2026-07-15"),
            participants = listOf("Alice", "Mehdi", "Nora", "Jules"),
            participantCount = 8,
            activities = listOf("Surf", "Beach dinner"),
            constraints = listOf("Train preferred", "Vegetarian dinner option"),
            budget = MoneyAmount(amount = 300.0, currencyCode = "EUR")
        )
}
