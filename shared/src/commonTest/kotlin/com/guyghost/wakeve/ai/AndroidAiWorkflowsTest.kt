package com.guyghost.wakeve.ai

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidAiWorkflowsTest {
    @Test
    fun eventSummaryUsesOnDeviceModelWhenAvailable() = runTest {
        val localClient = FakeAiTextGenerationClient(
            availability = AiModelAvailability.AVAILABLE,
            response = """
                SUMMARY: Weekend in Biarritz for 8 participants from July 12 to July 15.
                ADVICE:
                - Confirm surf reservations.
                - Share arrival times.
                PACKING:
                - Swimsuit
                - Sunscreen
                MISSING:
                - Accommodation address
            """.trimIndent()
        )
        val assistant = OnDeviceEventSummaryAiAssistant(localClient)

        val summary = assistant.summarize(sampleContext())
            .filterIsInstance<EventSummaryAiUpdate.SummaryReady>()
            .last()
            .summary

        assertEquals("Weekend in Biarritz for 8 participants from July 12 to July 15.", summary.shortSummary)
        assertEquals(listOf("Confirm surf reservations.", "Share arrival times."), summary.preparationAdvice)
        assertEquals(listOf("Swimsuit", "Sunscreen"), summary.packingChecklist)
        assertEquals(listOf("Accommodation address"), summary.missingInformation)
        assertEquals(AiInferenceRoute.ON_DEVICE, summary.routing.route)
        assertFalse(summary.routing.cloudUsed)
        assertTrue(localClient.lastPrompt.orEmpty().length < 900)
    }

    @Test
    fun eventSummaryFallsBackLocallyWhenOnDeviceModelIsUnavailable() = runTest {
        val localClient = FakeAiTextGenerationClient(
            availability = AiModelAvailability.UNAVAILABLE,
            response = "This response should not be used."
        )
        val assistant = OnDeviceEventSummaryAiAssistant(localClient)

        val updates = assistant.summarize(sampleContext()).toList()
        val summary = updates.filterIsInstance<EventSummaryAiUpdate.SummaryReady>().last().summary

        assertEquals(AiInferenceRoute.LOCAL_FALLBACK, summary.routing.route)
        assertFalse(summary.routing.cloudUsed)
        assertEquals(0, localClient.generateCallCount)
        assertTrue(summary.shortSummary.contains("Biarritz"))
        assertTrue(updates.any { it is EventSummaryAiUpdate.Unavailable })
    }

    @Test
    fun hybridOrganizerMessageUsesLocalInferenceWhenAvailable() = runTest {
        val localClient = FakeAiTextGenerationClient(
            availability = AiModelAvailability.AVAILABLE,
            response = "Salut tout le monde, voici l'invitation pour Biarritz."
        )
        val cloudClient = FakeAiTextGenerationClient(
            availability = AiModelAvailability.AVAILABLE,
            response = "Cloud message"
        )
        val assistant = HybridOrganizerMessageAiAssistant(
            onDeviceClient = localClient,
            cloudClient = cloudClient
        )

        val message = assistant.generateMessage(
            OrganizerMessageRequest(
                context = sampleContext(),
                messageType = OrganizerMessageType.INVITATION
            )
        ).filterIsInstance<OrganizerMessageAiUpdate.MessageReady>().last().message

        assertEquals(AiInferenceRoute.ON_DEVICE, message.routing.route)
        assertFalse(message.routing.cloudUsed)
        assertEquals(1, localClient.generateCallCount)
        assertEquals(0, cloudClient.generateCallCount)
        assertTrue(message.body.contains("Biarritz"))
    }

    @Test
    fun hybridOrganizerMessageFallsBackToFirebaseCloudWhenLocalModelUnavailable() = runTest {
        val localClient = FakeAiTextGenerationClient(
            availability = AiModelAvailability.UNAVAILABLE,
            response = "Local should not run."
        )
        val cloudClient = FakeAiTextGenerationClient(
            availability = AiModelAvailability.AVAILABLE,
            response = "Petit rappel budget: pensez à confirmer les 300 EUR par personne.",
            providerName = "Firebase AI Logic",
            modelName = "gemini-3.1-flash-lite",
            route = AiInferenceRoute.CLOUD_FIREBASE_AI_LOGIC
        )
        val assistant = HybridOrganizerMessageAiAssistant(
            onDeviceClient = localClient,
            cloudClient = cloudClient
        )

        val message = assistant.generateMessage(
            OrganizerMessageRequest(
                context = sampleContext(),
                messageType = OrganizerMessageType.BUDGET_REMINDER
            )
        ).filterIsInstance<OrganizerMessageAiUpdate.MessageReady>().last().message

        assertEquals(AiInferenceRoute.CLOUD_FIREBASE_AI_LOGIC, message.routing.route)
        assertTrue(message.routing.cloudUsed)
        assertEquals("Firebase AI Logic", message.routing.providerName)
        assertEquals("gemini-3.1-flash-lite", message.routing.modelName)
        assertEquals(0, localClient.generateCallCount)
        assertEquals(1, cloudClient.generateCallCount)
    }

    @Test
    fun fakePlanningAgentEmitsProgressAndConfirmationRequest() = runTest {
        val client = FakePlanningAgentClient()

        val events = client.startSession(sampleContext()).toList()

        assertTrue(events.first() is PlanningAgentEvent.SessionStarted)
        assertTrue(events.any { it is PlanningAgentEvent.Progress })
        assertTrue(events.any { it is PlanningAgentEvent.SuggestedPlan })
        assertTrue(events.any { it is PlanningAgentEvent.ParticipantTasksSuggested })
        assertTrue(events.any { it is PlanningAgentEvent.BudgetCategoriesSuggested })
        assertTrue(events.any { it is PlanningAgentEvent.MissingLogisticsIdentified })
        assertTrue(events.any { it is PlanningAgentEvent.ConfirmationRequested })
        assertTrue(events.last() is PlanningAgentEvent.Completed)
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
