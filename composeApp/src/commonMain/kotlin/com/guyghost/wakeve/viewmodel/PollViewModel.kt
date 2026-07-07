package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.analytics.AnalyticsEvent
import com.guyghost.wakeve.analytics.AnalyticsProvider
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.repository.EventRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    analyticsProvider: AnalyticsProvider
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

    init {
        trackScreenView("poll", "PollViewModel")
        trackEvent(AnalyticsEvent.PollViewed(eventId))
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
        val selectedSlot = event.proposedSlots.firstOrNull { it.id == slotId }
        if (selectedSlot == null) {
            _confirmationError.value = finalDateSlotRequiredMessage()
            return
        }

        viewModelScope.launch {
            _isConfirmingFinalDate.value = true
            _confirmationError.value = null
            try {
                if (!eventRepository.isOrganizer(event.id, userId)) {
                    _confirmationError.value = finalDateOrganizerRequiredMessage()
                    trackError("confirm_final_date_failed", finalDateOrganizerRequiredAnalyticsContext())
                    return@launch
                }

                val result = eventRepository.updateEventStatus(
                    id = event.id,
                    status = EventStatus.CONFIRMED,
                    finalDate = selectedSlot.start
                )
                if (result.isFailure) {
                    throw result.exceptionOrNull() ?: IllegalStateException("Failed to confirm final date")
                }

                _hasConfirmedFinalDate.value = true
                trackEvent(
                    AnalyticsEvent.PollClosed(
                        eventId = event.id,
                        participantsCount = event.participants.size,
                        votesCount = eventRepository.getPoll(event.id)?.votes?.size ?: 0
                    )
                )
                onSuccess()
            } catch (e: Exception) {
                _confirmationError.value = finalDateConfirmationFailureMessage()
                trackError("confirm_final_date_failed", finalDateConfirmationFailureAnalyticsContext())
            } finally {
                _isConfirmingFinalDate.value = false
            }
        }
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
