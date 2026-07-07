package com.guyghost.wakeve.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable

@Serializable
data class EventPlanningPromptContext(
    val eventId: String? = null,
    val title: String,
    val destination: String? = null,
    val dates: List<String> = emptyList(),
    val participants: List<String> = emptyList(),
    val participantCount: Int? = null,
    val activities: List<String> = emptyList(),
    val constraints: List<String> = emptyList(),
    val budget: MoneyAmount? = null,
    val localeTag: String = "fr-FR"
) {
    fun compactFacts(): String {
        val facts = mutableListOf("title=$title")
        destination?.takeIf { it.isNotBlank() }?.let { facts += "destination=$it" }
        if (dates.isNotEmpty()) facts += "dates=${dates.joinToString(" to ")}"
        participantCount?.let { facts += "participants=$it" }
            ?: participants.takeIf { it.isNotEmpty() }?.let { facts += "participants=${it.size}" }
        if (activities.isNotEmpty()) facts += "activities=${activities.joinToString()}"
        if (constraints.isNotEmpty()) facts += "constraints=${constraints.joinToString()}"
        budget?.let { facts += "budget=${it.amount.formatCompactAmount()} ${it.currencyCode}" }
        return facts.joinToString(" | ")
    }

    fun missingInformationLabels(): List<String> = buildList {
        if (destination.isNullOrBlank()) add("Destination")
        if (dates.isEmpty()) add("Dates")
        if (participantCount == null && participants.isEmpty()) add("Participants")
        if (activities.isEmpty()) add("Activities")
        if (constraints.isEmpty()) add("Constraints")
        if (budget == null) add("Budget")
    }
}

@Serializable
data class EventAiSummary(
    val shortSummary: String,
    val preparationAdvice: List<String>,
    val packingChecklist: List<String>,
    val missingInformation: List<String>,
    val routing: AiRoutingMetadata,
    val metadata: AiInteractionMetadata = routing.defaultMetadata(
        useCase = AiUseCase.EVENT_SUMMARY,
        inputSummary = "Event summary context",
        outputSummary = "Event summary draft",
        reasoningSummary = "Generated or assembled an event summary for user review.",
        validation = AiValidationResult.needsReview()
    )
)

@Serializable
data class GeneratedOrganizerMessage(
    val messageType: OrganizerMessageType,
    val body: String,
    val routing: AiRoutingMetadata,
    val metadata: AiInteractionMetadata = routing.defaultMetadata(
        useCase = AiUseCase.ORGANIZER_MESSAGE,
        inputSummary = "Organizer message request",
        outputSummary = "Organizer message draft",
        reasoningSummary = "Generated organizer copy from supplied event context.",
        validation = AiValidationResult.needsReview()
    )
)

@Serializable
data class OrganizerMessageRequest(
    val context: EventPlanningPromptContext,
    val messageType: OrganizerMessageType,
    val tone: OrganizerMessageTone = OrganizerMessageTone.FRIENDLY,
    val localeTag: String = context.localeTag
)

@Serializable
enum class OrganizerMessageType {
    INVITATION,
    REMINDER,
    RSVP_FOLLOW_UP,
    BUDGET_REMINDER,
    LOGISTICS_UPDATE,
    REVIEW_REQUEST
}

@Serializable
enum class OrganizerMessageTone {
    FRIENDLY,
    CONCISE,
    FORMAL
}

@Serializable
data class PlanningAgentSession(
    val id: String,
    val eventId: String?,
    val title: String,
    val status: PlanningAgentSessionStatus,
    val progressPercent: Int = 0,
    val pendingConfirmation: PlanningAgentConfirmationRequest? = null,
    val metadata: AiInteractionMetadata = AiInteractionMetadata.localFallback(
        useCase = AiUseCase.PLANNING_AGENT,
        providerName = "Wakeve planning agent client",
        sanitizedInputSummary = "Planning agent session context",
        sanitizedOutputSummary = status.name,
        reasoningSummary = "Planning-agent events are review-only until user confirmation.",
        validation = AiValidationResult.needsReview()
    )
)

@Serializable
enum class PlanningAgentSessionStatus {
    RUNNING,
    WAITING_FOR_CONFIRMATION,
    COMPLETED,
    FAILED
}

@Serializable
sealed interface PlanningAgentEvent {
    @Serializable
    data class SessionStarted(val session: PlanningAgentSession) : PlanningAgentEvent

    @Serializable
    data class Progress(
        val message: String,
        val step: Int,
        val totalSteps: Int
    ) : PlanningAgentEvent

    @Serializable
    data class SuggestedPlan(
        val title: String,
        val items: List<String>
    ) : PlanningAgentEvent

    @Serializable
    data class ParticipantTasksSuggested(
        val tasks: List<ParticipantTaskSuggestion>
    ) : PlanningAgentEvent

    @Serializable
    data class BudgetCategoriesSuggested(
        val categories: List<BudgetCategorySuggestion>
    ) : PlanningAgentEvent

    @Serializable
    data class MissingLogisticsIdentified(
        val items: List<String>
    ) : PlanningAgentEvent

    @Serializable
    data class ConfirmationRequested(
        val request: PlanningAgentConfirmationRequest
    ) : PlanningAgentEvent

    @Serializable
    data class ConfirmationResolved(
        val requestId: String,
        val accepted: Boolean
    ) : PlanningAgentEvent

    @Serializable
    data class Completed(
        val summary: String
    ) : PlanningAgentEvent

    @Serializable
    data class Failed(
        val message: String
    ) : PlanningAgentEvent
}

@Serializable
data class ParticipantTaskSuggestion(
    val participantName: String,
    val task: String
)

@Serializable
data class BudgetCategorySuggestion(
    val name: String,
    val description: String,
    val estimatedAmount: MoneyAmount? = null
)

@Serializable
data class PlanningAgentConfirmationRequest(
    val id: String,
    val title: String,
    val description: String,
    val confirmLabel: String = "Confirm",
    val dismissLabel: String = "Dismiss"
)

@Serializable
enum class AiModelAvailability {
    AVAILABLE,
    DOWNLOADABLE,
    DOWNLOADING,
    UNAVAILABLE
}

@Serializable
enum class AiInferenceRoute {
    ON_DEVICE,
    CLOUD_FIREBASE_AI_LOGIC,
    LOCAL_FALLBACK
}

@Serializable
data class AiRoutingMetadata(
    val route: AiInferenceRoute,
    val providerName: String,
    val modelName: String? = null,
    val cloudUsed: Boolean = false,
    val reason: String? = null
)

data class AiTextGenerationConfig(
    val temperature: Float = 0.2f,
    val maxOutputTokens: Int = 256
)

data class AiTextGenerationResult(
    val text: String,
    val routing: AiRoutingMetadata
)

interface AiTextGenerationClient {
    suspend fun availability(): AiModelAvailability

    suspend fun generateText(
        prompt: String,
        config: AiTextGenerationConfig = AiTextGenerationConfig()
    ): AiTextGenerationResult
}

class UnavailableAiTextGenerationClient(
    private val reason: String = "AI text generation client is not configured."
) : AiTextGenerationClient {
    override suspend fun availability(): AiModelAvailability = AiModelAvailability.UNAVAILABLE

    override suspend fun generateText(
        prompt: String,
        config: AiTextGenerationConfig
    ): AiTextGenerationResult {
        error(reason)
    }
}

interface EventSummaryAiAssistant {
    suspend fun availability(): AiModelAvailability

    fun summarize(context: EventPlanningPromptContext): Flow<EventSummaryAiUpdate>
}

sealed interface EventSummaryAiUpdate {
    data class Routing(val routing: AiRoutingMetadata) : EventSummaryAiUpdate
    data class SummaryReady(val summary: EventAiSummary) : EventSummaryAiUpdate
    data class Unavailable(val message: String) : EventSummaryAiUpdate
    data class Error(val message: String) : EventSummaryAiUpdate
}

interface OrganizerMessageAiAssistant {
    suspend fun availability(): AiModelAvailability

    fun generateMessage(request: OrganizerMessageRequest): Flow<OrganizerMessageAiUpdate>
}

sealed interface OrganizerMessageAiUpdate {
    data class Routing(val routing: AiRoutingMetadata) : OrganizerMessageAiUpdate
    data class MessageReady(val message: GeneratedOrganizerMessage) : OrganizerMessageAiUpdate
    data class Unavailable(val message: String) : OrganizerMessageAiUpdate
    data class Error(val message: String) : OrganizerMessageAiUpdate
}

interface PlanningAgentClient {
    fun startSession(context: EventPlanningPromptContext): Flow<PlanningAgentEvent>

    suspend fun submitConfirmation(
        sessionId: String,
        requestId: String,
        accepted: Boolean
    ): PlanningAgentEvent
}

class UnavailablePlanningAgentClient(
    private val reason: String = "Planning agent is not configured on this build."
) : PlanningAgentClient {
    override fun startSession(context: EventPlanningPromptContext): Flow<PlanningAgentEvent> = flow {
        emit(
            PlanningAgentEvent.SessionStarted(
                PlanningAgentSession(
                    id = "unavailable-${context.eventId ?: context.title.slug()}",
                    eventId = context.eventId,
                    title = context.title,
                    status = PlanningAgentSessionStatus.FAILED
                )
            )
        )
        emit(PlanningAgentEvent.Failed(reason))
    }

    override suspend fun submitConfirmation(
        sessionId: String,
        requestId: String,
        accepted: Boolean
    ): PlanningAgentEvent = PlanningAgentEvent.Failed(reason)
}

class OnDeviceEventSummaryAiAssistant(
    private val localClient: AiTextGenerationClient,
    private val parser: EventSummaryTextParser = EventSummaryTextParser()
) : EventSummaryAiAssistant {
    override suspend fun availability(): AiModelAvailability = localClient.availability()

    override fun summarize(context: EventPlanningPromptContext): Flow<EventSummaryAiUpdate> = flow {
        when (val availability = localClient.availability()) {
            AiModelAvailability.AVAILABLE -> {
                val prompt = buildPrompt(context)
                val result = localClient.generateText(
                    prompt = prompt,
                    config = AiTextGenerationConfig(temperature = 0.1f, maxOutputTokens = 220)
                )
                val routing = result.routing.copy(
                    route = AiInferenceRoute.ON_DEVICE,
                    cloudUsed = false
                )
                emit(EventSummaryAiUpdate.Routing(routing))
                emit(EventSummaryAiUpdate.SummaryReady(parser.parse(result.text, context, routing)))
            }

            else -> {
                emit(EventSummaryAiUpdate.Unavailable("On-device summary model is $availability."))
                val routing = AiRoutingMetadata(
                    route = AiInferenceRoute.LOCAL_FALLBACK,
                    providerName = "Wakeve local summary fallback",
                    cloudUsed = false,
                    reason = availability.name
                )
                emit(EventSummaryAiUpdate.Routing(routing))
                emit(EventSummaryAiUpdate.SummaryReady(buildFallbackSummary(context, routing)))
            }
        }
    }.catch {
        val routing = AiRoutingMetadata(
            route = AiInferenceRoute.LOCAL_FALLBACK,
            providerName = "Wakeve local summary fallback",
            cloudUsed = false,
            reason = "LOCAL_GENERATION_FAILED"
        )
        emit(EventSummaryAiUpdate.Error("On-device summary generation failed."))
        emit(EventSummaryAiUpdate.Routing(routing))
        emit(EventSummaryAiUpdate.SummaryReady(buildFallbackSummary(context, routing)))
    }

    fun buildPrompt(context: EventPlanningPromptContext): String =
        """
        Summarize this event for Wakeve. Keep it short.
        Output exactly:
        SUMMARY: one sentence
        ADVICE:
        - max 3 prep tips
        PACKING:
        - max 5 items
        MISSING:
        - unknown info only
        Facts: ${context.compactFacts()}
        Locale: ${context.localeTag}
        """.trimIndent()

    private fun buildFallbackSummary(
        context: EventPlanningPromptContext,
        routing: AiRoutingMetadata
    ): EventAiSummary {
        val destination = context.destination ?: "destination to confirm"
        val dates = context.dates.joinToString(" to ").ifBlank { "dates to confirm" }
        val participants = context.participantCount ?: context.participants.size.takeIf { it > 0 }
        val budget = context.budget?.let { "${it.amount.formatCompactAmount()} ${it.currencyCode}" }

        return EventAiSummary(
            shortSummary = buildString {
                append(context.title)
                append(" in ")
                append(destination)
                append(" on ")
                append(dates)
                participants?.let { append(" for $it participants") }
                budget?.let { append(" with a budget of $it") }
                append(".")
            },
            preparationAdvice = listOf(
                "Confirm the final schedule.",
                "Share logistics with participants.",
                "Check budget and constraints before departure."
            ),
            packingChecklist = listOf("ID or tickets", "Weather-appropriate clothes", "Chargers"),
            missingInformation = context.missingInformationLabels(),
            routing = routing
        )
    }
}

class HybridOrganizerMessageAiAssistant(
    private val onDeviceClient: AiTextGenerationClient,
    private val cloudClient: AiTextGenerationClient,
    private val allowCloudFallback: Boolean = true
) : OrganizerMessageAiAssistant {
    override suspend fun availability(): AiModelAvailability =
        if (onDeviceClient.availability() == AiModelAvailability.AVAILABLE) {
            AiModelAvailability.AVAILABLE
        } else {
            cloudClient.availability()
        }

    override fun generateMessage(request: OrganizerMessageRequest): Flow<OrganizerMessageAiUpdate> = flow {
        val localAvailability = onDeviceClient.availability()
        if (localAvailability == AiModelAvailability.AVAILABLE) {
            emit(generateWith(onDeviceClient, request, forceRoute = AiInferenceRoute.ON_DEVICE))
            return@flow
        }

        emit(OrganizerMessageAiUpdate.Unavailable("On-device message model is $localAvailability."))
        if (allowCloudFallback && cloudClient.availability() == AiModelAvailability.AVAILABLE) {
            emit(generateWith(cloudClient, request, forceRoute = AiInferenceRoute.CLOUD_FIREBASE_AI_LOGIC))
        } else {
            val routing = AiRoutingMetadata(
                route = AiInferenceRoute.LOCAL_FALLBACK,
                providerName = "Wakeve local message fallback",
                cloudUsed = false,
                reason = localAvailability.name
            )
            emit(OrganizerMessageAiUpdate.Routing(routing))
            emit(
                OrganizerMessageAiUpdate.MessageReady(
                    GeneratedOrganizerMessage(
                        messageType = request.messageType,
                        body = buildFallbackMessage(request),
                        routing = routing
                    )
                )
            )
        }
    }.catch {
        val routing = AiRoutingMetadata(
            route = AiInferenceRoute.LOCAL_FALLBACK,
            providerName = "Wakeve local message fallback",
            cloudUsed = false,
            reason = "HYBRID_GENERATION_FAILED"
        )
        emit(OrganizerMessageAiUpdate.Error("Organizer message generation failed."))
        emit(OrganizerMessageAiUpdate.Routing(routing))
        emit(
            OrganizerMessageAiUpdate.MessageReady(
                GeneratedOrganizerMessage(
                    messageType = request.messageType,
                    body = buildFallbackMessage(request),
                    routing = routing
                )
            )
        )
    }

    private suspend fun generateWith(
        client: AiTextGenerationClient,
        request: OrganizerMessageRequest,
        forceRoute: AiInferenceRoute
    ): OrganizerMessageAiUpdate.MessageReady {
        val result = client.generateText(
            prompt = buildPrompt(request),
            config = AiTextGenerationConfig(temperature = 0.4f, maxOutputTokens = 180)
        )
        val routing = result.routing.copy(
            route = forceRoute,
            cloudUsed = forceRoute == AiInferenceRoute.CLOUD_FIREBASE_AI_LOGIC
        )
        return OrganizerMessageAiUpdate.MessageReady(
            GeneratedOrganizerMessage(
                messageType = request.messageType,
                body = result.text.trim().ifBlank { buildFallbackMessage(request) },
                routing = routing
            )
        )
    }

    fun buildPrompt(request: OrganizerMessageRequest): String =
        """
        Write one ${request.messageType.promptLabel()} for the organizer.
        Tone: ${request.tone.name.lowercase()}. Keep it short and clear. No emoji.
        Facts: ${request.context.compactFacts()}
        Locale: ${request.localeTag}
        Return only the message body.
        """.trimIndent()

    private fun buildFallbackMessage(request: OrganizerMessageRequest): String {
        val context = request.context
        val destination = context.destination ?: "the destination"
        return when (request.messageType) {
            OrganizerMessageType.INVITATION ->
                "Hi everyone, you are invited to ${context.title} in $destination. Please confirm if you can join."
            OrganizerMessageType.REMINDER ->
                "Quick reminder for ${context.title}: please check the latest details and confirm what is still pending."
            OrganizerMessageType.RSVP_FOLLOW_UP ->
                "Can you confirm your RSVP for ${context.title}? It will help finalize the plan."
            OrganizerMessageType.BUDGET_REMINDER ->
                "Budget reminder for ${context.title}: please review your share and flag any issue before we confirm."
            OrganizerMessageType.LOGISTICS_UPDATE ->
                "Logistics update for ${context.title}: please review the latest travel, activity, and timing details."
            OrganizerMessageType.REVIEW_REQUEST ->
                "Thanks for joining ${context.title}. Share a quick review so we can improve the next plan."
        }
    }
}

@Deprecated(
    message = "Use UnavailablePlanningAgentClient in production or DeterministicPlanningAgentClient in tests.",
    replaceWith = ReplaceWith("UnavailablePlanningAgentClient()")
)
class FakePlanningAgentClient : PlanningAgentClient {
    private val delegate = UnavailablePlanningAgentClient("Planning agent test fake is not available in production.")

    override fun startSession(context: EventPlanningPromptContext): Flow<PlanningAgentEvent> = flow {
        delegate.startSession(context).collect { emit(it) }
    }

    override suspend fun submitConfirmation(
        sessionId: String,
        requestId: String,
        accepted: Boolean
    ): PlanningAgentEvent = delegate.submitConfirmation(sessionId, requestId, accepted)
}

@Deprecated(
    message = "Use UnavailableAiTextGenerationClient in production or DeterministicAiTextGenerationClient in tests.",
    replaceWith = ReplaceWith("UnavailableAiTextGenerationClient()")
)
class FakeAiTextGenerationClient(
    private val availability: AiModelAvailability = AiModelAvailability.AVAILABLE,
    private val response: String,
    private val providerName: String = "Gemini Nano",
    private val modelName: String? = "gemini-nano",
    private val route: AiInferenceRoute = AiInferenceRoute.ON_DEVICE
) : AiTextGenerationClient {
    var generateCallCount: Int = 0
        private set

    var lastPrompt: String? = null
        private set

    override suspend fun availability(): AiModelAvailability = AiModelAvailability.UNAVAILABLE

    override suspend fun generateText(
        prompt: String,
        config: AiTextGenerationConfig
    ): AiTextGenerationResult {
        error("AI text generation test fake is not available in production.")
    }
}

class EventSummaryTextParser {
    fun parse(
        rawText: String,
        context: EventPlanningPromptContext,
        routing: AiRoutingMetadata
    ): EventAiSummary {
        val sections = parseSections(rawText)
        val summary = sections["SUMMARY"]?.firstOrNull()
            ?: rawText.lineSequence().firstOrNull { it.isNotBlank() }?.trim()
            ?: context.title

        return EventAiSummary(
            shortSummary = summary.removePrefix("SUMMARY:").trim(),
            preparationAdvice = sections["ADVICE"].orEmpty().ifEmpty {
                listOf("Confirm logistics with participants.")
            },
            packingChecklist = sections["PACKING"].orEmpty().ifEmpty {
                listOf("Weather-appropriate clothes", "Chargers")
            },
            missingInformation = sections["MISSING"].orEmpty().ifEmpty {
                context.missingInformationLabels()
            },
            routing = routing
        )
    }

    private fun parseSections(rawText: String): Map<String, List<String>> {
        val sections = mutableMapOf<String, MutableList<String>>()
        var currentSection: String? = null

        rawText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                val section = line.substringBefore(':').uppercase()
                if (section in setOf("SUMMARY", "ADVICE", "PACKING", "MISSING") && line.contains(':')) {
                    currentSection = section
                    val remainder = line.substringAfter(':').trim()
                    if (remainder.isNotBlank()) {
                        sections.getOrPut(section) { mutableListOf() } += remainder
                    } else {
                        sections.getOrPut(section) { mutableListOf() }
                    }
                } else {
                    currentSection?.let { key ->
                        sections.getOrPut(key) { mutableListOf() } += line.trimStart('-', '*').trim()
                    }
                }
            }

        return sections
    }
}

private fun OrganizerMessageType.promptLabel(): String = when (this) {
    OrganizerMessageType.INVITATION -> "invitation message"
    OrganizerMessageType.REMINDER -> "reminder message"
    OrganizerMessageType.RSVP_FOLLOW_UP -> "RSVP follow-up"
    OrganizerMessageType.BUDGET_REMINDER -> "budget reminder"
    OrganizerMessageType.LOGISTICS_UPDATE -> "logistics update"
    OrganizerMessageType.REVIEW_REQUEST -> "post-event review request"
}

private fun String.slug(): String =
    lowercase()
        .replace(Regex("""[^a-z0-9]+"""), "-")
        .trim('-')
        .ifBlank { "session" }

private fun Double.formatCompactAmount(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
