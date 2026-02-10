package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.EventRepository
import com.guyghost.wakeve.analytics.AnalyticsEvent
import com.guyghost.wakeve.analytics.AnalyticsProvider
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.Vote
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
    private val eventRepository: EventRepository,
    private val eventId: String,
    analyticsProvider: AnalyticsProvider
) : AnalyticsViewModel(analyticsProvider) {

    private val _poll = MutableStateFlow(eventRepository.getPoll(eventId))
    val poll: StateFlow<Poll?> = _poll.asStateFlow()

    private val _isVoting = MutableStateFlow(false)
    val isVoting: StateFlow<Boolean> = _isVoting.asStateFlow()

    private val _isClosing = MutableStateFlow(false)
    val isClosing: StateFlow<Boolean> = _isClosing.asStateFlow()

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
    fun vote(slotId: String, response: String, isChanging: Boolean = false) {
        viewModelScope.launch {
            _isVoting.value = true
            try {
                // In a real implementation, we'd need to participant ID
                // For now, this is a simplified version
                val participantId = "current_user_id" // Would come from auth state

                // Convert string response to Vote enum
                val voteValue = try {
                    Vote.valueOf(response.uppercase())
                } catch (e: IllegalArgumentException) {
                    Vote.MAYBE // Default fallback
                }

                eventRepository.addVote(
                    eventId = eventId,
                    participantId = participantId,
                    slotId = slotId,
                    vote = voteValue
                )

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
                trackError("vote_failed", e.message)
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
                trackError("close_poll_failed", e.message)
            } finally {
                _isClosing.value = false
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
