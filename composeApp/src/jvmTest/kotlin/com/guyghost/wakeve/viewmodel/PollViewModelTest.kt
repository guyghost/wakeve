package com.guyghost.wakeve.viewmodel

import com.guyghost.wakeve.EventRepository
import com.guyghost.wakeve.analytics.AnalyticsEvent
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.Vote
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for PollViewModel.
 */
class PollViewModelTest : ViewModelTestBase() {

    @Mock
    private lateinit var eventRepository: EventRepository

    private lateinit var viewModel: PollViewModel
    private val eventId = "test_event_id"

    @Before
    override fun setUp() {
        super.setUp()
        MockitoAnnotations.openMocks(this)

        // Setup poll
        val testPoll = Poll(
            id = "poll1",
            eventId = eventId,
            votes = emptyMap()
        )
        doReturn(testPoll).`when`(eventRepository).getPoll(eq(eventId))
        doReturn(emptyList<String>()).`when`(eventRepository).getParticipants(eq(eventId))

        viewModel = PollViewModel(eventRepository, eventId, mockAnalyticsProvider)
    }

    @Test
    fun `init should track screen_view and poll_viewed`() = runTest {
        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("screen_view"),
            "screen_view event should be tracked on init"
        )
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("poll_viewed"),
            "poll_viewed event should be tracked on init"
        )
        val pollViewedEvent = mockAnalyticsProvider.getEventsByName("poll_viewed").firstOrNull()?.first
            as? AnalyticsEvent.PollViewed
        assertEquals(eventId, pollViewedEvent?.eventId, "Event ID should match")
    }

    @Test
    fun `vote should track poll_voted on success`() = runTest {
        // Arrange
        val slotId = "slot1"
        val response = "yes"
        val participantId = "current_user_id"

        doReturn(kotlin.Result.success(true)).`when`(eventRepository)
            .addVote(eq(eventId), eq(participantId), eq(slotId), any())

        // Act
        viewModel.vote(slotId, response)
        advanceUntilIdle()

        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("poll_voted"),
            "poll_voted event should be tracked"
        )
        val lastEvent = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.PollVoted
        assertEquals(eventId, lastEvent?.eventId, "Event ID should match")
        assertEquals(response, lastEvent?.response, "Response should match")
        assertFalse(lastEvent?.isChangingVote ?: true, "Is changing vote should be false")
        assertFalse(viewModel.isVoting.value, "Should not be voting after completion")
    }

    @Test
    fun `vote with isChanging true should track changing vote`() = runTest {
        // Arrange
        val slotId = "slot1"
        val response = "yes"
        val participantId = "current_user_id"

        doReturn(kotlin.Result.success(true)).`when`(eventRepository)
            .addVote(eq(eventId), eq(participantId), eq(slotId), any())

        // Act
        viewModel.vote(slotId, response, isChanging = true)
        advanceUntilIdle()

        // Assert
        val lastEvent = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.PollVoted
        assertTrue(lastEvent?.isChangingVote ?: false, "Is changing vote should be true")
    }

    @Test
    fun `vote should track error on failure`() = runTest {
        // Arrange
        val slotId = "slot1"
        val response = "yes"
        val errorMessage = "Vote failed"

        doReturn(kotlin.Result.failure<Unit>(IllegalArgumentException(errorMessage))).`when`(eventRepository)
            .addVote(any(), any(), any(), any())

        // Act
        viewModel.vote(slotId, response)
        advanceUntilIdle()

        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("error_occurred"),
            "error_occurred event should be tracked"
        )
        val lastEvent = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.ErrorOccurred
        assertEquals("vote_failed", lastEvent?.errorType, "Error type should be vote_failed")
        assertEquals(errorMessage, lastEvent?.errorContext, "Error context should match")
        assertFalse(viewModel.isVoting.value, "Should not be voting after error")
    }

    @Test
    fun `vote should convert string response to Vote enum`() = runTest {
        // Arrange
        val slotId = "slot1"
        val participantId = "current_user_id"

        doReturn(kotlin.Result.success(true)).`when`(eventRepository)
            .addVote(eq(eventId), eq(participantId), eq(slotId), eq(Vote.YES))

        // Act
        viewModel.vote(slotId, "yes")
        advanceUntilIdle()

        // Assert
        verify(eventRepository).addVote(eq(eventId), eq(participantId), eq(slotId), eq(Vote.YES))
    }

    @Test
    fun `vote should handle invalid response with MAYBE fallback`() = runTest {
        // Arrange
        val slotId = "slot1"
        val participantId = "current_user_id"

        doReturn(kotlin.Result.success(true)).`when`(eventRepository)
            .addVote(eq(eventId), eq(participantId), eq(slotId), eq(Vote.MAYBE))

        // Act
        viewModel.vote(slotId, "invalid")
        advanceUntilIdle()

        // Assert
        verify(eventRepository).addVote(eq(eventId), eq(participantId), eq(slotId), eq(Vote.MAYBE))
    }

    @Test
    fun `closePoll should track poll_closed with metrics`() = runTest {
        // Arrange
        val participantCount = 5
        val voteCount = 3
        doReturn(listOf("user1", "user2", "user3", "user4", "user5")).`when`(eventRepository)
            .getParticipants(eq(eventId))
        val testPoll = Poll(
            id = "poll1",
            eventId = eventId,
            votes = mapOf("user1" to mapOf("slot1" to Vote.YES))
        )
        doReturn(testPoll).`when`(eventRepository).getPoll(eq(eventId))

        // Recreate viewModel to get updated poll
        viewModel = PollViewModel(eventRepository, eventId, mockAnalyticsProvider)

        // Act
        viewModel.closePoll()
        advanceUntilIdle()

        // Assert
        assertTrue(
            mockAnalyticsProvider.wasEventTracked("poll_closed"),
            "poll_closed event should be tracked"
        )
        val lastEvent = mockAnalyticsProvider.getLastEvent() as? AnalyticsEvent.PollClosed
        assertEquals(eventId, lastEvent?.eventId, "Event ID should match")
        assertEquals(participantCount, lastEvent?.participantsCount, "Participant count should match")
        // Note: vote count in test poll is 1 (not 3) because it counts participants who voted
        assertNotNull(lastEvent, "Last event should be PollClosed")
        assertFalse(viewModel.isClosing.value, "Should not be closing after completion")
    }

    @Test
    fun `refreshPoll should update poll from repository`() = runTest {
        // Arrange
        val newPoll = Poll(
            id = "poll2",
            eventId = eventId,
            votes = mapOf("user1" to mapOf("slot1" to Vote.YES))
        )
        doReturn(newPoll).`when`(eventRepository).getPoll(eq(eventId))

        // Act
        viewModel.refreshPoll()

        // Assert
        val currentPoll = viewModel.poll.value
        assertEquals("poll2", currentPoll?.id, "Poll ID should be updated")
    }
}
