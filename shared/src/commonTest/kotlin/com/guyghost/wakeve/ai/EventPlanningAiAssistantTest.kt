package com.guyghost.wakeve.ai

import com.guyghost.wakeve.models.EventType
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventPlanningAiAssistantTest {
    @Test
    fun `rule based assistant extracts structured event plan from French trip prompt`() = runTest {
        val assistant = RuleBasedEventPlanningAiAssistant(defaultYear = 2026)

        val draft = assistant.extractEventPlan(
            EventPlanningPrompt(
                text = "On part à Biarritz du 12 au 15 juillet, 8 personnes, budget 300€ par personne.",
                referenceYear = 2026
            )
        ).filterIsInstance<EventPlanningAiUpdate.Draft>().last().draft

        assertEquals("Biarritz", draft.destination)
        assertEquals("2026-07-12", draft.startDate)
        assertEquals("2026-07-15", draft.endDate)
        assertEquals(8, draft.participantCount)
        assertEquals(300.0, draft.budgetPerPerson?.amount)
        assertEquals("EUR", draft.budgetPerPerson?.currencyCode)
        assertEquals(EventType.OUTDOOR_ACTIVITY, draft.eventType)
        assertEquals(EventPlanDraftSource.RULE_BASED, draft.source)
        assertFalse(EventPlanMissingInformation.DESTINATION in draft.missingInformation)
    }

    @Test
    fun `rule based assistant returns missing information for partial prompt`() = runTest {
        val assistant = RuleBasedEventPlanningAiAssistant(defaultYear = 2026)

        val draft = assistant.parse(EventPlanningPrompt(text = "Petit diner entre amis"))

        assertEquals(EventType.FOOD_TASTING, draft.eventType)
        assertTrue(EventPlanMissingInformation.DESTINATION in draft.missingInformation)
        assertTrue(EventPlanMissingInformation.START_DATE in draft.missingInformation)
        assertTrue(EventPlanMissingInformation.END_DATE in draft.missingInformation)
        assertTrue(EventPlanMissingInformation.PARTICIPANT_COUNT in draft.missingInformation)
        assertTrue(EventPlanMissingInformation.BUDGET_PER_PERSON in draft.missingInformation)
    }

    @Test
    fun `json parser normalizes ml kit structured output`() {
        val parser = EventPlanDraftJsonParser()

        val draft = parser.parse(
            rawText = """
                {
                  "destination": "Biarritz",
                  "startDate": "2026-07-12",
                  "endDate": "2026-07-15",
                  "participantCount": 8,
                  "budgetPerPerson": {"amount": 300, "currencyCode": "EUR"},
                  "eventType": "OUTDOOR_ACTIVITY",
                  "constraints": ["budget per person"],
                  "missingInformation": []
                }
            """.trimIndent(),
            source = EventPlanDraftSource.ML_KIT_GENAI
        )

        assertEquals("Biarritz", draft.destination)
        assertEquals(EventPlanDraftSource.ML_KIT_GENAI, draft.source)
        assertEquals(EventType.OUTDOOR_ACTIVITY, draft.eventType)
        assertFalse(EventPlanMissingInformation.DESTINATION in draft.missingInformation)
    }

    @Test
    fun `fake assistant supports unsupported device tests without platform APIs`() = runTest {
        val assistant = FakeEventPlanningAiAssistant(
            availability = EventPlanningAiAvailability.UNAVAILABLE
        )

        val availability = assistant.extractEventPlan(EventPlanningPrompt("test"))
            .filterIsInstance<EventPlanningAiUpdate.Availability>()
            .last()

        assertEquals(EventPlanningAiAvailability.UNAVAILABLE, availability.availability)
    }

    @Test
    fun `fallback wrapper uses rule based assistant when primary is unsupported`() = runTest {
        val primary = FakeEventPlanningAiAssistant(availability = EventPlanningAiAvailability.UNAVAILABLE)
        val assistant = FallbackEventPlanningAiAssistant(
            primary = primary,
            fallback = RuleBasedEventPlanningAiAssistant(defaultYear = 2026)
        )

        val draft = assistant.extractEventPlan(
            EventPlanningPrompt("On part à Biarritz du 12 au 15 juillet, 8 personnes, budget 300€ par personne.")
        ).filterIsInstance<EventPlanningAiUpdate.Draft>().last().draft

        assertEquals(EventPlanDraftSource.RULE_BASED, draft.source)
        assertEquals("Biarritz", draft.destination)
    }
}
