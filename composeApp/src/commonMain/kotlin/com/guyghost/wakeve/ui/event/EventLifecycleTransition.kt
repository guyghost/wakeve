package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.presentation.state.EventManagementContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Organizer lifecycle transitions driven from the event detail:
 * CONFIRMED -> ORGANIZING and ORGANIZING -> FINALIZED.
 *
 * Mirrors the iOS `EventLifecycleTransitionController`: the shared
 * EventManagementStateMachine stays the single owner of the status write.
 */
enum class EventLifecycleTarget(val status: EventStatus) {
    ORGANIZING(EventStatus.ORGANIZING),
    FINALIZED(EventStatus.FINALIZED),
}

/** What the organizer lifecycle card shows. `target` is null when there is no action yet. */
enum class EventLifecycleCardMode(val target: EventLifecycleTarget?) {
    READY_TO_ORGANIZE(EventLifecycleTarget.ORGANIZING),
    PICK_FINAL_OPTION(null),
    READY_TO_FINALIZE(EventLifecycleTarget.FINALIZED),
}

/** Organizer-only card, shown for CONFIRMED, COMPARING and ORGANIZING events. */
fun eventLifecycleCardMode(event: Event?, currentUserId: String): EventLifecycleCardMode? {
    if (event == null || event.organizerId != currentUserId) return null
    return when (event.status) {
        EventStatus.CONFIRMED -> EventLifecycleCardMode.READY_TO_ORGANIZE
        EventStatus.COMPARING -> EventLifecycleCardMode.PICK_FINAL_OPTION
        EventStatus.ORGANIZING -> EventLifecycleCardMode.READY_TO_FINALIZE
        else -> null
    }
}

sealed interface EventLifecycleOutcome {
    data class Transitioned(val target: EventLifecycleTarget) : EventLifecycleOutcome

    /** [failure] is the raw shared error; format it with [formatEventLifecycleFailure] before display. */
    data class Failed(val failure: String?) : EventLifecycleOutcome
}

sealed interface EventLifecycleSideEffectResult {
    /** The effect is unrelated to a lifecycle transition: handle it as usual. */
    data object NotHandled : EventLifecycleSideEffectResult

    /** The effect belonged to a lifecycle transition and must not reach the UI. */
    data object Consumed : EventLifecycleSideEffectResult

    data class Settled(val outcome: EventLifecycleOutcome) : EventLifecycleSideEffectResult
}

/**
 * Dispatches lifecycle intents and settles them from the side-effect stream.
 *
 * State updates are conflated by StateFlow, so an identical repeated failure would never be
 * observed through state. Every transition path of the state machine ends with a ShowToast:
 * that toast is the settle signal. The raw English toast is swallowed; the UI shows a
 * localized outcome instead.
 *
 * On success the detail stays on screen and reloads the event (SelectEvent). The navigation
 * effects that follow (meetings after organizing, and SelectEvent's route to this detail)
 * are consumed once so the organizer is not pushed elsewhere.
 */
class EventLifecycleTransitionController(
    private val eventId: String,
    private val userId: String,
    private val dispatch: (EventManagementContract.Intent) -> Unit,
    private val currentState: () -> EventManagementContract.State,
) {
    private val _inFlightTarget = MutableStateFlow<EventLifecycleTarget?>(null)
    val inFlightTarget: StateFlow<EventLifecycleTarget?> = _inFlightTarget.asStateFlow()

    private val routesToConsume = mutableListOf<String>()

    /** Returns false when another transition is still in flight. */
    fun transition(target: EventLifecycleTarget): Boolean {
        if (_inFlightTarget.value != null) return false
        _inFlightTarget.value = target
        dispatch(
            when (target) {
                EventLifecycleTarget.ORGANIZING ->
                    EventManagementContract.Intent.TransitionToOrganizing(eventId, userId)
                EventLifecycleTarget.FINALIZED ->
                    EventManagementContract.Intent.MarkAsFinalized(eventId, userId)
            }
        )
        return true
    }

    fun onSideEffect(effect: EventManagementContract.SideEffect): EventLifecycleSideEffectResult {
        return when (effect) {
            is EventManagementContract.SideEffect.ShowToast -> settle()
            is EventManagementContract.SideEffect.NavigateTo ->
                if (routesToConsume.remove(effect.route)) {
                    EventLifecycleSideEffectResult.Consumed
                } else {
                    EventLifecycleSideEffectResult.NotHandled
                }
            else -> EventLifecycleSideEffectResult.NotHandled
        }
    }

    private fun settle(): EventLifecycleSideEffectResult {
        val target = _inFlightTarget.value ?: return EventLifecycleSideEffectResult.NotHandled
        val state = currentState()
        val status = state.events.firstOrNull { it.id == eventId }?.status
        _inFlightTarget.value = null

        if (status != target.status) {
            return EventLifecycleSideEffectResult.Settled(EventLifecycleOutcome.Failed(state.error))
        }

        if (target == EventLifecycleTarget.ORGANIZING) {
            routesToConsume += "event/$eventId/meetings"
        }
        routesToConsume += "event/$eventId"
        dispatch(EventManagementContract.Intent.SelectEvent(eventId))
        return EventLifecycleSideEffectResult.Settled(EventLifecycleOutcome.Transitioned(target))
    }
}

/** Blocker codes emitted by the shared finalization readiness check. */
object EventLifecycleBlockerCodes {
    val all: List<String> = listOf(
        "ORGANIZER_REQUIRED",
        "CONFIRMED_PARTICIPANTS_REQUIRED",
        "FINAL_SCENARIO_REQUIRED",
        "DESTINATION_REQUIRED",
        "LODGING_REQUIRED",
        "TRANSPORT_REQUIRED",
        "MEETING_REQUIRED",
        "CALENDAR_CONFIRMED_DATE_REQUIRED",
        "NOTIFICATION_REMINDERS_REQUIRED",
        "BUDGET_REQUIRED",
        "PAYMENT_POT_REQUIRED",
        "TRICOUNT_HANDOFF_REQUIRED",
        "CRITICAL_SYNC_PENDING",
        "CRITICAL_SYNC_FAILED",
        "CRITICAL_CONFLICT_PENDING",
        "UNSAFE_EXTERNAL_LINKS",
        "ACCESS_ORGANIZER_REQUIRED",
        "ACCESS_CONFIRMED_PARTICIPANTS_REQUIRED",
        "EVENT_NOT_ORGANIZING",
    )
}

private const val FINALIZATION_BLOCKED_PREFIX = "Finalization blocked by "

/**
 * Turns a shared lifecycle failure (e.g. "Finalization blocked by MEETING_REQUIRED,BUDGET_REQUIRED")
 * into a sentence the organizer can act on. Raw blocker codes never reach the UI: unknown codes
 * are skipped, and anything else falls back to [genericMessage].
 *
 * @param blockerLabel localized label for an upper-case blocker code, or null when unknown.
 * @param joinLabels locale-aware list join ("a, b et c").
 */
fun formatEventLifecycleFailure(
    failure: String?,
    blockedIntro: String,
    genericMessage: String,
    blockerLabel: (String) -> String?,
    joinLabels: (List<String>) -> String = { it.joinToString(", ") },
): String {
    if (failure == null || !failure.startsWith(FINALIZATION_BLOCKED_PREFIX)) return genericMessage

    val labels = failure
        .removePrefix(FINALIZATION_BLOCKED_PREFIX)
        .split(",")
        .map { it.trim().uppercase() }
        .filter { it.isNotEmpty() }
        .mapNotNull(blockerLabel)
        .distinct()

    if (labels.isEmpty()) return genericMessage
    return "$blockedIntro ${joinLabels(labels)}."
}

/** Localized copy for the organizer lifecycle card, supplied by the platform. */
data class EventLifecycleCopy(
    val organizingTitle: String,
    val organizingBody: String,
    val organizingAction: String,
    val organizingConfirmMessage: String,
    val comparingBody: String,
    val finalizeTitle: String,
    val finalizeBody: String,
    val finalizeAction: String,
    val finalizeConfirmMessage: String,
    val cancel: String,
    val blockedIntro: String,
    val genericError: String,
    val blockerLabels: Map<String, String>,
    val joinLabels: (List<String>) -> String = { it.joinToString(", ") },
) {
    fun failureMessage(failure: String?): String = formatEventLifecycleFailure(
        failure = failure,
        blockedIntro = blockedIntro,
        genericMessage = genericError,
        blockerLabel = blockerLabels::get,
        joinLabels = joinLabels,
    )
}
