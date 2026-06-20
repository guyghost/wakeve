package com.guyghost.wakeve.viewmodel

import com.guyghost.wakeve.ai.AiModelAvailability
import com.guyghost.wakeve.ai.EventPlanDraft
import com.guyghost.wakeve.ai.EventPlanningAiAssistant
import com.guyghost.wakeve.ai.EventPlanningAiAvailability
import com.guyghost.wakeve.ai.EventPlanningAiUpdate
import com.guyghost.wakeve.ai.EventPlanningPrompt
import com.guyghost.wakeve.ai.EventPlanningPromptContext
import com.guyghost.wakeve.ai.EventSummaryAiAssistant
import com.guyghost.wakeve.ai.EventSummaryAiUpdate
import com.guyghost.wakeve.ai.OrganizerMessageAiAssistant
import com.guyghost.wakeve.ai.OrganizerMessageAiUpdate
import com.guyghost.wakeve.ai.OrganizerMessageRequest
import com.guyghost.wakeve.ai.PlanningAgentClient
import com.guyghost.wakeve.ai.PlanningAgentEvent
import com.guyghost.wakeve.ai.PlanningAgentSession
import com.guyghost.wakeve.ai.PlanningAgentSessionStatus
import com.guyghost.wakeve.ui.ai.planningAgentFailureDisplayMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class AiAssistantErrorMessageTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun summaryGenerationUsesGenericMessageWhenAssistantFails() = runTest {
        val viewModel = AiWorkflowDemoViewModel(
            summaryAssistant = SummaryAssistant(EventSummaryAiUpdate.Error(sensitiveFailureMessage()))
        )

        viewModel.generateSummary()
        advanceUntilIdle()

        val message = viewModel.state.value.summaryError.orEmpty()
        assertEquals(aiSummaryFailureMessage(), message)
        assertDoesNotExposeSensitiveDetails(message)
    }

    @Test
    fun organizerMessageUsesGenericMessageWhenAssistantFails() = runTest {
        val viewModel = AiWorkflowDemoViewModel(
            messageAssistant = MessageAssistant(OrganizerMessageAiUpdate.Error(sensitiveFailureMessage()))
        )

        viewModel.generateMessage()
        advanceUntilIdle()

        val message = viewModel.state.value.messageError.orEmpty()
        assertEquals(aiOrganizerMessageFailureMessage(), message)
        assertDoesNotExposeSensitiveDetails(message)
    }

    @Test
    fun planningAgentFailureUsesGenericMessageInStateAndTimeline() = runTest {
        val viewModel = AiWorkflowDemoViewModel(
            planningAgentClient = FailingPlanningAgentClient()
        )

        viewModel.startPlanningAgent()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(planningAgentFailureDisplayMessage(), state.agentError)
        assertEquals(planningAgentFailureDisplayMessage(), state.agentEvents.last().body)
        assertDoesNotExposeSensitiveDetails(state.agentError.orEmpty())
        assertDoesNotExposeSensitiveDetails(state.agentEvents.last().body)
    }

    @Test
    fun eventPlanningExtractionUsesGenericMessageAndKeepsFallbackDraft() = runTest {
        val fallbackDraft = EventPlanDraft(destination = "Biarritz")
        val viewModel = EventPlanningAssistantViewModel(
            assistant = PlanningAssistant(EventPlanningAiUpdate.Error(sensitiveFailureMessage(), fallbackDraft))
        )

        viewModel.updatePrompt("Weekend surf a Biarritz pour 8 personnes")
        viewModel.extract(referenceYear = 2026)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(eventPlanExtractionFailureMessage(), state.errorMessage)
        assertEquals(fallbackDraft, state.draft)
        assertDoesNotExposeSensitiveDetails(state.errorMessage.orEmpty())
    }

    @Test
    fun aiFailureHelpersUseStableSafeCopy() {
        listOf(
            aiSummaryUnavailableMessage(),
            aiSummaryFailureMessage(),
            aiOrganizerMessageUnavailableMessage(),
            aiOrganizerMessageFailureMessage(),
            planningAgentFailureDisplayMessage(),
            eventPlanExtractionFailureMessage()
        ).forEach { message ->
            assertFalse(message.isBlank())
            assertDoesNotExposeSensitiveDetails(message)
        }
    }

    private fun sensitiveFailureMessage(): String =
        "SQL constraint failed for secret@example.com token=SECRET http://internal.local/ai"

    private fun assertDoesNotExposeSensitiveDetails(message: String) {
        listOf(
            "secret@example.com",
            "SECRET",
            "SQL constraint",
            "http://internal.local",
            "token="
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }

    private class SummaryAssistant(
        private val update: EventSummaryAiUpdate
    ) : EventSummaryAiAssistant {
        override suspend fun availability(): AiModelAvailability = AiModelAvailability.AVAILABLE
        override fun summarize(context: EventPlanningPromptContext): Flow<EventSummaryAiUpdate> = flowOf(update)
    }

    private class MessageAssistant(
        private val update: OrganizerMessageAiUpdate
    ) : OrganizerMessageAiAssistant {
        override suspend fun availability(): AiModelAvailability = AiModelAvailability.AVAILABLE
        override fun generateMessage(request: OrganizerMessageRequest): Flow<OrganizerMessageAiUpdate> = flowOf(update)
    }

    private class FailingPlanningAgentClient : PlanningAgentClient {
        override fun startSession(context: EventPlanningPromptContext): Flow<PlanningAgentEvent> = flowOf(
            PlanningAgentEvent.SessionStarted(
                PlanningAgentSession(
                    id = "session-1",
                    eventId = context.eventId,
                    title = context.title,
                    status = PlanningAgentSessionStatus.FAILED
                )
            ),
            PlanningAgentEvent.Failed(
                "Provider 500 for secret@example.com token=SECRET http://internal.local/agent"
            )
        )

        override suspend fun submitConfirmation(
            sessionId: String,
            requestId: String,
            accepted: Boolean
        ): PlanningAgentEvent = PlanningAgentEvent.Failed(sensitiveProviderFailure)

        private val sensitiveProviderFailure =
            "Provider 500 for secret@example.com token=SECRET http://internal.local/agent"
    }

    private class PlanningAssistant(
        private val update: EventPlanningAiUpdate
    ) : EventPlanningAiAssistant {
        override suspend fun availability(): EventPlanningAiAvailability = EventPlanningAiAvailability.AVAILABLE
        override fun extractEventPlan(prompt: EventPlanningPrompt): Flow<EventPlanningAiUpdate> = flowOf(update)
    }
}
