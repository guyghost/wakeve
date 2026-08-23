package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.analytics.AnalyticsEvent
import com.guyghost.wakeve.analytics.AnalyticsProvider
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachine
import com.guyghost.wakeve.repository.EventRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel for the poll/voting screen with analytics tracking.
 *
 * This ViewModel manages the poll state for an event, handles voting operations,
 * and tracks user interactions for analytics purposes.
 *
 * ## Usage in Compose
 *
 * ```kotlin
 * @Composable
 * fun PollScreen(
 *     eventId: String,
 *     viewModel: PollViewModel = koinViewModel(parameters = { parametersOf(eventId) })
 * ) {
 *     val poll by viewModel.poll.collectAsState()
 *     val isVoting by viewModel.isVoting.collectAsState()
 *
 *     PollContent(
 *         poll = poll,
 *         isVoting = isVoting,
 *         onVote = { slotId, response ->
 *             viewModel.vote(slotId, response)
 *         },
 *         onClosePoll = { viewModel.closePoll() }
 *     )
 * }
 * ```
 *
 * @property eventRepository Repository for accessing event and poll data
 * @property eventId The ID of the event for this poll
 * @property analyticsProvider Analytics provider for tracking user actions
 */
class PollViewModel(
    private val eventRepository: EventRepositoryInterface,
    private val eventId: String,
    analyticsProvider: AnalyticsProvider,
    private val confirmationStateMachine: EventManagementStateMachine,
    private val confirmationOperationIdProvider: () -> String
) : AnalyticsViewModel(analyticsProvider) {

    private val _poll = MutableStateFlow(eventRepository.getPoll(eventId))
    val poll: StateFlow<Poll?> = _poll.asStateFlow()

    private val _isVoting = MutableStateFlow(false)
    val isVoting: StateFlow<Boolean> = _isVoting.asStateFlow()

    private val _isClosing = MutableStateFlow(false)
    val isClosing: StateFlow<Boolean> = _isClosing.asStateFlow()

    private val _selectedVotes = MutableStateFlow<Map<String, Vote>>(emptyMap())
    val selectedVotes: StateFlow<Map<String, Vote>> = _selectedVotes.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _hasSubmitted = MutableStateFlow(false)
    val hasSubmitted: StateFlow<Boolean> = _hasSubmitted.asStateFlow()

    private val _selectedFinalSlotId = MutableStateFlow<String?>(null)
    val selectedFinalSlotId: StateFlow<String?> = _selectedFinalSlotId.asStateFlow()

    private val _isConfirmingFinalDate = MutableStateFlow(false)
    val isConfirmingFinalDate: StateFlow<Boolean> = _isConfirmingFinalDate.asStateFlow()

    private val _confirmationError = MutableStateFlow<String?>(null)
    val confirmationError: StateFlow<String?> = _confirmationError.asStateFlow()

    private val _hasConfirmedFinalDate = MutableStateFlow(false)
    val hasConfirmedFinalDate: StateFlow<Boolean> = _hasConfirmedFinalDate.asStateFlow()

    private val _confirmationPhase = MutableStateFlow(
        EventManagementContract.ConfirmationPhase.REVIEWING_RESULTS
    )
    val confirmationPhase: StateFlow<EventManagementContract.ConfirmationPhase> =
        _confirmationPhase.asStateFlow()

    private val _canRetryFinalDateConfirmation = MutableStateFlow(false)
    val canRetryFinalDateConfirmation: StateFlow<Boolean> =
        _canRetryFinalDateConfirmation.asStateFlow()

    private var confirmationEventId: String? = null
    private var confirmationSlotId: String? = null
    private var confirmationOperationId: String? = null
    private var confirmationSuccessCallback: (() -> Unit)? = null
    private var deliveredConfirmationReceiptId: String? = null
    private val confirmationStateObservationJob = viewModelScope.launch {
        confirmationStateMachine.state.collect(::applyConfirmationState)
    }

    init {
        trackScreenView("poll", "PollViewModel")
        trackEvent(AnalyticsEvent.PollViewed(eventId))

        runCatching { eventRepository.loadConfirmationProjection(eventId) }
            .getOrNull()
            ?.let { projection ->
                confirmationStateMachine.dispatch(
                    EventManagementContract.Intent.RehydrateConfirmation(projection)
                )
            }
    }

    /**
     * Submit a vote for a specific time slot.
     *
     * Tracks the voting action including whether this is a new vote or
     * a change to an existing vote. This data can help understand user
     * decision-making patterns.
     *
     * @param slotId The ID of the time slot being voted on
     * @param response The vote response (yes, no, maybe)
     * @param isChanging Whether this user is changing an existing vote
     */
    fun vote(
        slotId: String,
        response: String,
        isChanging: Boolean = false,
        participantId: String = "current_user_id"
    ) {
        viewModelScope.launch {
            _isVoting.value = true
            try {
                // Convert string response to Vote enum
                val voteValue = try {
                    Vote.valueOf(response.uppercase())
                } catch (e: IllegalArgumentException) {
                    Vote.MAYBE // Default fallback
                }

                val result = eventRepository.addVote(
                    eventId = eventId,
                    participantId = participantId,
                    slotId = slotId,
                    vote = voteValue
                )
                if (result.isFailure) {
                    throw result.exceptionOrNull() ?: IllegalStateException("Failed to submit vote")
                }

                // Update the poll state
                _poll.value = eventRepository.getPoll(eventId)

                // Track the vote event
                trackEvent(
                    AnalyticsEvent.PollVoted(
                        eventId = eventId,
                        response = response,
                        isChangingVote = isChanging
                    )
                )
            } catch (e: Exception) {
                trackError("vote_failed", pollVoteFailureAnalyticsContext())
            } finally {
                _isVoting.value = false
            }
        }
    }

    fun selectVote(slotId: String, vote: Vote) {
        _selectedVotes.value = _selectedVotes.value + (slotId to vote)
        _errorMessage.value = null
    }

    fun selectFinalSlot(slotId: String) {
        _selectedFinalSlotId.value = slotId
        _confirmationError.value = null
    }

    fun submitVotes(
        event: Event,
        participantId: String,
        onSuccess: () -> Unit
    ) {
        val votes = _selectedVotes.value
        if (votes.size != event.proposedSlots.size) {
            _errorMessage.value = pollVoteAllSlotsRequiredMessage()
            return
        }

        viewModelScope.launch {
            _isVoting.value = true
            _errorMessage.value = null
            try {
                votes.forEach { (slotId, vote) ->
                    val result = eventRepository.addVote(
                        eventId = event.id,
                        participantId = participantId,
                        slotId = slotId,
                        vote = vote
                    )
                    if (result.isFailure) {
                        throw result.exceptionOrNull() ?: IllegalStateException("Failed to submit vote")
                    }

                    trackEvent(
                        AnalyticsEvent.PollVoted(
                            eventId = event.id,
                            response = vote.name.lowercase(),
                            isChangingVote = false
                        )
                    )
                }

                _poll.value = eventRepository.getPoll(event.id)
                _hasSubmitted.value = true
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = pollVoteSubmissionFailureMessage()
                trackError("vote_failed", pollVoteFailureAnalyticsContext())
            } finally {
                _isVoting.value = false
            }
        }
    }

    /**
     * Close the poll and confirm the event date.
     *
     * This method:
     * 1. Gets the current participant count
     * 2. Gets the vote count
     * 3. Closes the poll via the repository
     * 4. Tracks the closing event with participant and vote metrics
     *
     * These metrics help understand poll engagement and can be used
     * to optimize the polling experience.
     */
    fun closePoll() {
        viewModelScope.launch {
            _isClosing.value = true
            try {
                // Get metrics before closing
                val participantCount = eventRepository.getParticipants(eventId)?.size ?: 0
                val poll = eventRepository.getPoll(eventId)
                val voteCount = poll?.votes?.size ?: 0

                // Close the poll (this would update event status in real implementation)
                // For now, we track the metrics
                trackEvent(
                    AnalyticsEvent.PollClosed(
                        eventId = eventId,
                        participantsCount = participantCount,
                        votesCount = voteCount
                    )
                )
            } catch (e: Exception) {
                trackError("close_poll_failed", pollCloseFailureAnalyticsContext())
            } finally {
                _isClosing.value = false
            }
        }
    }

    fun confirmFinalDate(
        event: Event,
        userId: String,
        onSuccess: () -> Unit
    ) {
        val slotId = _selectedFinalSlotId.value
        if (slotId == null) {
            _confirmationError.value = finalDateSlotRequiredMessage()
            return
        }

        if (_confirmationPhase.value != EventManagementContract.ConfirmationPhase.REVIEWING_RESULTS) {
            return
        }

        val operationId = confirmationOperationIdProvider()
        if (operationId.isBlank()) {
            _confirmationError.value = finalDateConfirmationFailureMessage()
            trackError("confirm_final_date_failed", finalDateConfirmationFailureAnalyticsContext())
            return
        }

        confirmationEventId = event.id
        confirmationSlotId = slotId
        confirmationOperationId = operationId
        confirmationSuccessCallback = onSuccess
        deliveredConfirmationReceiptId = null
        _confirmationError.value = null
        _hasConfirmedFinalDate.value = false

        confirmationStateMachine.dispatch(
            EventManagementContract.Intent.OpenConfirmPrompt(
                eventId = event.id,
                slotId = slotId,
                actorId = userId
            )
        )
    }

    fun submitFinalDateConfirmation() {
        val operationId = confirmationOperationId ?: return
        val state = confirmationStateMachine.state.value
        if (state.confirmationPhase != EventManagementContract.ConfirmationPhase.CONFIRM_PROMPT ||
            state.confirmationEventId != confirmationEventId ||
            state.confirmationSlotId != confirmationSlotId
        ) return

        confirmationStateMachine.dispatch(
            EventManagementContract.Intent.SubmitConfirmation(operationId)
        )
    }

    fun cancelFinalDateConfirmation() {
        if (_confirmationPhase.value != EventManagementContract.ConfirmationPhase.CONFIRM_PROMPT) return
        confirmationStateMachine.dispatch(EventManagementContract.Intent.CancelConfirmation)
        clearActiveConfirmationAttempt()
    }

    fun retryFinalDateConfirmation() {
        if (_confirmationPhase.value != EventManagementContract.ConfirmationPhase.FAILED ||
            !_canRetryFinalDateConfirmation.value ||
            confirmationOperationId == null
        ) return
        confirmationStateMachine.dispatch(EventManagementContract.Intent.RetryConfirmation)
    }

    fun dismissFinalDateConfirmationFailure() {
        if (_confirmationPhase.value != EventManagementContract.ConfirmationPhase.FAILED) return
        confirmationStateMachine.dispatch(EventManagementContract.Intent.DismissConfirmationFailure)
        clearActiveConfirmationAttempt()
    }

    internal fun disposeConfirmationAdapter() {
        confirmationStateObservationJob.cancel()
        clearActiveConfirmationAttempt()
    }

    private fun applyConfirmationState(state: EventManagementContract.State) {
        val phase = state.confirmationPhase
        val belongsToThisEvent = state.confirmationEventId == eventId
        if (!belongsToThisEvent && phase != EventManagementContract.ConfirmationPhase.REVIEWING_RESULTS) {
            return
        }

        _confirmationPhase.value = phase
        _isConfirmingFinalDate.value = phase == EventManagementContract.ConfirmationPhase.CONFIRMING
        _canRetryFinalDateConfirmation.value =
            phase == EventManagementContract.ConfirmationPhase.FAILED &&
                state.confirmationFailure?.retryable == true

        when (phase) {
            EventManagementContract.ConfirmationPhase.REVIEWING_RESULTS,
            EventManagementContract.ConfirmationPhase.CONFIRM_PROMPT,
            EventManagementContract.ConfirmationPhase.CONFIRMING -> {
                _confirmationError.value = null
                _hasConfirmedFinalDate.value = false
            }

            EventManagementContract.ConfirmationPhase.FAILED -> {
                _confirmationError.value = confirmationFailureMessage(state.confirmationFailure?.code)
                _hasConfirmedFinalDate.value = false
                trackError("confirm_final_date_failed", finalDateConfirmationFailureAnalyticsContext())
            }

            EventManagementContract.ConfirmationPhase.CONFIRMED_PENDING_SYNC,
            EventManagementContract.ConfirmationPhase.CONFIRMED_SYNCED -> {
                _confirmationError.value = null
                _hasConfirmedFinalDate.value = true
                deliverConfirmationSuccessIfCorrelated(state)
            }

            EventManagementContract.ConfirmationPhase.LEGACY_APPLIED,
            EventManagementContract.ConfirmationPhase.QUARANTINED -> {
                _confirmationError.value = finalDateConfirmationFailureMessage()
                _hasConfirmedFinalDate.value = false
            }
        }
    }

    private fun deliverConfirmationSuccessIfCorrelated(state: EventManagementContract.State) {
        val receiptId = state.confirmationReceiptId ?: return
        val operationId = confirmationOperationId ?: return
        if (state.confirmationOperationId != operationId ||
            state.confirmationEventId != confirmationEventId ||
            state.confirmationSlotId != confirmationSlotId ||
            deliveredConfirmationReceiptId == receiptId
        ) return

        deliveredConfirmationReceiptId = receiptId
        trackEvent(
            AnalyticsEvent.PollClosed(
                eventId = eventId,
                participantsCount = eventRepository.getParticipants(eventId)?.size ?: 0,
                votesCount = eventRepository.getPoll(eventId)?.votes?.size ?: 0
            )
        )
        confirmationSuccessCallback?.invoke()
        confirmationSuccessCallback = null
    }

    private fun clearActiveConfirmationAttempt() {
        confirmationEventId = null
        confirmationSlotId = null
        confirmationOperationId = null
        confirmationSuccessCallback = null
        deliveredConfirmationReceiptId = null
        _confirmationError.value = null
        _isConfirmingFinalDate.value = false
        _canRetryFinalDateConfirmation.value = false
    }

    /**
     * Refresh the poll data from the repository.
     *
     * Useful for polling updates from the backend or after offline sync.
     */
    fun refreshPoll() {
        _poll.value = eventRepository.getPoll(eventId)
    }
}

internal fun pollVoteSubmissionFailureMessage(): String {
    return "Impossible d'enregistrer vos votes. Réessayez."
}

internal fun pollVoteAllSlotsRequiredMessage(): String {
    return "Votez sur tous les créneaux avant d'envoyer vos réponses."
}

internal fun finalDateConfirmationFailureMessage(): String {
    return "Impossible de confirmer la date finale. Réessayez."
}

internal fun finalDateSlotRequiredMessage(): String {
    return "Sélectionnez un créneau avant de confirmer."
}

internal fun finalDateOrganizerRequiredMessage(): String {
    return "Seul l'organisateur peut confirmer la date finale."
}

internal fun pollVoteFailureAnalyticsContext(): String {
    return "vote_submission_failed"
}

internal fun pollCloseFailureAnalyticsContext(): String {
    return "poll_close_failed"
}

internal fun finalDateConfirmationFailureAnalyticsContext(): String {
    return "final_date_confirmation_failed"
}

internal fun finalDateOrganizerRequiredAnalyticsContext(): String {
    return "organizer_required"
}

private fun confirmationFailureMessage(
    code: EventManagementContract.ConfirmationFailureCode?
): String = when (code) {
    EventManagementContract.ConfirmationFailureCode.NOT_ORGANIZER ->
        finalDateOrganizerRequiredMessage()
    EventManagementContract.ConfirmationFailureCode.SLOT_NOT_FOUND,
    EventManagementContract.ConfirmationFailureCode.SLOT_NOT_CONFIRMABLE ->
        finalDateSlotRequiredMessage()
    else -> finalDateConfirmationFailureMessage()
}

@OptIn(ExperimentalUuidApi::class)
internal fun newPollConfirmationOperationId(): String = Uuid.random().toString()
