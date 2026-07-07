package com.guyghost.wakeve.ai

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LegacyFakeAiAssistantsTest {
    @Test
    fun legacyFakeEventPlanningAssistantDoesNotEmitDrafts() = runTest {
        @Suppress("DEPRECATION")
        val assistant = FakeEventPlanningAiAssistant()

        assertEquals(EventPlanningAiAvailability.UNAVAILABLE, assistant.availability())
        val updates = assistant.extractEventPlan(EventPlanningPrompt("Plan Biarritz")).toList()

        assertTrue(updates.any { it is EventPlanningAiUpdate.Availability })
        assertTrue(updates.any { it is EventPlanningAiUpdate.Error })
        assertTrue(updates.none { it is EventPlanningAiUpdate.Draft })
    }

    @Test
    fun legacyFakePlanningAgentClientFailsInsteadOfEmittingPlan() = runTest {
        @Suppress("DEPRECATION")
        val client = FakePlanningAgentClient()

        val events = client.startSession(
            EventPlanningPromptContext(title = "Weekend surf")
        ).toList()

        assertTrue(events.first() is PlanningAgentEvent.SessionStarted)
        assertTrue(events.last() is PlanningAgentEvent.Failed)
        assertTrue(events.none { it is PlanningAgentEvent.SuggestedPlan })
    }
}
