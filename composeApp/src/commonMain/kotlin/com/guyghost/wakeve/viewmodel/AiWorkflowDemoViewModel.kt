package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.ai.AiInferenceRoute
import com.guyghost.wakeve.ai.AiModelAvailability
import com.guyghost.wakeve.ai.EventAiSummary
import com.guyghost.wakeve.ai.EventPlanningPromptContext
import com.guyghost.wakeve.ai.EventSummaryAiAssistant
import com.guyghost.wakeve.ai.EventSummaryAiUpdate
import com.guyghost.wakeve.ai.FakeAiTextGenerationClient
import com.guyghost.wakeve.ai.FakePlanningAgentClient
import com.guyghost.wakeve.ai.GeneratedOrganizerMessage
import com.guyghost.wakeve.ai.HybridOrganizerMessageAiAssistant
import com.guyghost.wakeve.ai.MoneyAmount
import com.guyghost.wakeve.ai.OnDeviceEventSummaryAiAssistant
import com.guyghost.wakeve.ai.OrganizerMessageAiAssistant
import com.guyghost.wakeve.ai.OrganizerMessageAiUpdate
import com.guyghost.wakeve.ai.OrganizerMessageRequest
import com.guyghost.wakeve.ai.OrganizerMessageType
import com.guyghost.wakeve.ai.PlanningAgentClient
import com.guyghost.wakeve.ai.PlanningAgentEvent
import com.guyghost.wakeve.ai.PlanningAgentSession
import com.guyghost.wakeve.ui.ai.PlanningAgentEventUiItem
import com.guyghost.wakeve.ui.ai.toPlanningAgentEventUiItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiWorkflowDemoUiState(
    val context: EventPlanningPromptContext = sampleAiWorkflowContext(),
    val summaryAvailability: AiModelAvailability = AiModelAvailability.UNAVAILABLE,
    val isGeneratingSummary: Boolean = false,
    val summary: EventAiSummary? = null,
    val summaryError: String? = null,
    val selectedMessageType: OrganizerMessageType = OrganizerMessageType.INVITATION,
    val isGeneratingMessage: Boolean = false,
    val generatedMessage: GeneratedOrganizerMessage? = null,
    val messageError: String? = null,
    val agentSession: PlanningAgentSession? = null,
    val isAgentRunning: Boolean = false,
    val agentEvents: List<PlanningAgentEventUiItem> = emptyList(),
    val agentError: String? = null
) {
    val summaryRouteLabel: String
        get() = summary?.routing?.route?.label() ?: "Not generated"

    val messageRouteLabel: String
        get() = generatedMessage?.routing?.route?.label() ?: "Not generated"
}

class AiWorkflowDemoViewModel(
    private val summaryAssistant: EventSummaryAiAssistant = OnDeviceEventSummaryAiAssistant(
        FakeAiTextGenerationClient(
            availability = AiModelAvailability.UNAVAILABLE,
            response = ""
        )
    ),
    private val messageAssistant: OrganizerMessageAiAssistant = HybridOrganizerMessageAiAssistant(
        onDeviceClient = FakeAiTextGenerationClient(
            availability = AiModelAvailability.UNAVAILABLE,
            response = ""
        ),
        cloudClient = FakeAiTextGenerationClient(
            response = "Cloud fallback message",
            providerName = "Firebase AI Logic",
            modelName = "gemini-3.1-flash-lite",
            route = AiInferenceRoute.CLOUD_FIREBASE_AI_LOGIC
        )
    ),
    private val planningAgentClient: PlanningAgentClient = FakePlanningAgentClient()
) : ViewModel() {
    private val _state = MutableStateFlow(AiWorkflowDemoUiState())
    val state: StateFlow<AiWorkflowDemoUiState> = _state.asStateFlow()

    private var summaryJob: Job? = null
    private var messageJob: Job? = null
    private var agentJob: Job? = null

    init {
        viewModelScope.launch {
            _state.update { it.copy(summaryAvailability = summaryAssistant.availability()) }
        }
    }

    fun selectMessageType(type: OrganizerMessageType) {
        _state.update { it.copy(selectedMessageType = type, messageError = null) }
    }

    fun generateSummary() {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            _state.update { it.copy(isGeneratingSummary = true, summary = null, summaryError = null) }
            summaryAssistant.summarize(_state.value.context).collect { update ->
                when (update) {
                    is EventSummaryAiUpdate.Routing -> Unit
                    is EventSummaryAiUpdate.SummaryReady -> {
                        _state.update {
                            it.copy(
                                isGeneratingSummary = false,
                                summary = update.summary,
                                summaryError = null
                            )
                        }
                    }
                    is EventSummaryAiUpdate.Unavailable -> {
                        _state.update { it.copy(summaryError = update.message) }
                    }
                    is EventSummaryAiUpdate.Error -> {
                        _state.update {
                            it.copy(isGeneratingSummary = false, summaryError = update.message)
                        }
                    }
                }
            }
            _state.update { it.copy(isGeneratingSummary = false) }
        }
    }

    fun generateMessage() {
        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            _state.update {
                it.copy(isGeneratingMessage = true, generatedMessage = null, messageError = null)
            }
            messageAssistant.generateMessage(
                OrganizerMessageRequest(
                    context = _state.value.context,
                    messageType = _state.value.selectedMessageType
                )
            ).collect { update ->
                when (update) {
                    is OrganizerMessageAiUpdate.Routing -> Unit
                    is OrganizerMessageAiUpdate.MessageReady -> {
                        _state.update {
                            it.copy(
                                isGeneratingMessage = false,
                                generatedMessage = update.message,
                                messageError = null
                            )
                        }
                    }
                    is OrganizerMessageAiUpdate.Unavailable -> {
                        _state.update { it.copy(messageError = update.message) }
                    }
                    is OrganizerMessageAiUpdate.Error -> {
                        _state.update {
                            it.copy(isGeneratingMessage = false, messageError = update.message)
                        }
                    }
                }
            }
            _state.update { it.copy(isGeneratingMessage = false) }
        }
    }

    fun startPlanningAgent() {
        agentJob?.cancel()
        agentJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isAgentRunning = true,
                    agentSession = null,
                    agentEvents = emptyList(),
                    agentError = null
                )
            }
            planningAgentClient.startSession(_state.value.context).collect { event ->
                applyAgentEvent(event)
            }
            _state.update { it.copy(isAgentRunning = false) }
        }
    }

    fun resolveAgentConfirmation(requestId: String, accepted: Boolean) {
        viewModelScope.launch {
            val sessionId = _state.value.agentSession?.id ?: return@launch
            applyAgentEvent(
                planningAgentClient.submitConfirmation(
                    sessionId = sessionId,
                    requestId = requestId,
                    accepted = accepted
                )
            )
        }
    }

    private fun applyAgentEvent(event: PlanningAgentEvent) {
        _state.update { state ->
            val session = when (event) {
                is PlanningAgentEvent.SessionStarted -> event.session
                else -> state.agentSession
            }
            state.copy(
                agentSession = session,
                agentEvents = state.agentEvents + event.toPlanningAgentEventUiItem(),
                agentError = (event as? PlanningAgentEvent.Failed)?.message
            )
        }
    }
}

private fun AiInferenceRoute.label(): String = when (this) {
    AiInferenceRoute.ON_DEVICE -> "On-device"
    AiInferenceRoute.CLOUD_FIREBASE_AI_LOGIC -> "Cloud via Firebase AI Logic"
    AiInferenceRoute.LOCAL_FALLBACK -> "Local fallback"
}

private fun sampleAiWorkflowContext(): EventPlanningPromptContext =
    EventPlanningPromptContext(
        eventId = "demo-biarritz",
        title = "Weekend surf",
        destination = "Biarritz",
        dates = listOf("2026-07-12", "2026-07-15"),
        participants = listOf("Alice", "Mehdi", "Nora", "Jules"),
        participantCount = 8,
        activities = listOf("Surf", "Beach dinner"),
        constraints = listOf("Train preferred", "Vegetarian dinner option"),
        budget = MoneyAmount(amount = 300.0, currencyCode = "EUR")
    )
