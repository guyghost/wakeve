package com.guyghost.wakeve.ui.ai

import com.guyghost.wakeve.ai.PlanningAgentConfirmationRequest
import com.guyghost.wakeve.ai.PlanningAgentEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiWorkflowEventRenderingTest {
    @Test
    fun planningAgentConfirmationRequestProjectsToUserActionItem() {
        val item = PlanningAgentEvent.ConfirmationRequested(
            request = PlanningAgentConfirmationRequest(
                id = "confirm-budget",
                title = "Apply budget categories",
                description = "Use transport, lodging, food, and activities for the shared budget.",
                confirmLabel = "Apply",
                dismissLabel = "Skip"
            )
        ).toPlanningAgentEventUiItem()

        assertEquals("Action needed", item.title)
        assertTrue(item.body.contains("Apply budget categories"))
        assertTrue(item.requiresUserAction)
        assertEquals("Apply", item.primaryActionLabel)
        assertEquals("Skip", item.secondaryActionLabel)
    }

    @Test
    fun planningAgentProgressProjectsToPassiveProgressItem() {
        val item = PlanningAgentEvent.Progress(
            message = "Checking missing logistics",
            step = 2,
            totalSteps = 5
        ).toPlanningAgentEventUiItem()

        assertEquals("Step 2 of 5", item.title)
        assertEquals("Checking missing logistics", item.body)
        assertFalse(item.requiresUserAction)
    }
}
