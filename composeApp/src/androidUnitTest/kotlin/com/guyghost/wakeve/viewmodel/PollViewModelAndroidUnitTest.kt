package com.guyghost.wakeve.viewmodel

import com.guyghost.wakeve.analytics.AnalyticsEvent
import com.guyghost.wakeve.analytics.AnalyticsProvider
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.repository.EventRepositoryInterface
import com.guyghost.wakeve.repository.OrderBy
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PollViewModelAndroidUnitTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeEventRepository
    private lateinit var analyticsProvider: RecordingAnalyticsProvider
    private lateinit var viewModel: PollViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeEventRepository()
        analyticsProvider = RecordingAnalyticsProvider()
        viewModel = PollViewModel(repository, eventId, analyticsProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectVoteUpdatesImmutableVoteState() {
        viewModel.selectVote("slot-1", Vote.YES)

        assertEquals(mapOf("slot-1" to Vote.YES), viewModel.selectedVotes.value)
        assertEquals(null, viewModel.errorMessage.value)
    }

    @Test
    fun submitVotesRequiresEveryProposedSlot() = runTest {
        val event = testEvent(listOf("slot-1", "slot-2"))

        viewModel.selectVote("slot-1", Vote.YES)
        viewModel.submitVotes(event, participantId = "participant-1", onSuccess = {})
        advanceUntilIdle()

        assertEquals(pollVoteAllSlotsRequiredMessage(), viewModel.errorMessage.value)
        assertTrue(repository.addedVotes.isEmpty())
    }

    @Test
    fun submitVotesWritesVotesForProvidedParticipant() = runTest {
        val event = testEvent(listOf("slot-1", "slot-2"))
        var successCalled = false

        viewModel.selectVote("slot-1", Vote.YES)
        viewModel.selectVote("slot-2", Vote.MAYBE)
        viewModel.submitVotes(event, participantId = "participant-42") {
            successCalled = true
        }
        advanceUntilIdle()

        assertEquals(
            listOf(
                AddedVote(eventId, "participant-42", "slot-1", Vote.YES),
                AddedVote(eventId, "participant-42", "slot-2", Vote.MAYBE)
            ),
            repository.addedVotes
        )
        assertTrue(successCalled)
        assertTrue(viewModel.hasSubmitted.value)
        assertTrue(analyticsProvider.events.filterIsInstance<AnalyticsEvent.PollVoted>().size == 2)
    }

    @Test
    fun submitVotesUsesGenericMessageWhenRepositoryFails() = runTest {
        val event = testEvent(listOf("slot-1"))
        repository.addVoteFailure = IllegalStateException(
            "SQL constraint failed for user secret@example.com token=SECRET"
        )

        viewModel.selectVote("slot-1", Vote.YES)
        viewModel.submitVotes(event, participantId = "participant-42", onSuccess = {})
        advanceUntilIdle()

        assertEquals(pollVoteSubmissionFailureMessage(), viewModel.errorMessage.value)
        assertFalse(viewModel.hasSubmitted.value)
        assertLastErrorContextDoesNotExpose("secret@example.com")
        assertLastErrorContextDoesNotExpose("SECRET")
        assertLastErrorContextDoesNotExpose("SQL constraint")
    }

    @Test
    fun confirmFinalDateRequiresSelectedSlot() = runTest {
        val event = testEvent(listOf("slot-1"))

        viewModel.confirmFinalDate(event, userId = "organizer", onSuccess = {})
        advanceUntilIdle()

        assertEquals(finalDateSlotRequiredMessage(), viewModel.confirmationError.value)
        assertTrue(repository.statusUpdates.isEmpty())
    }

    @Test
    fun confirmFinalDateShowsStableOrganizerRequiredMessage() = runTest {
        val event = testEvent(listOf("slot-1"))

        viewModel.selectFinalSlot("slot-1")
        viewModel.confirmFinalDate(event, userId = "participant-42", onSuccess = {})
        advanceUntilIdle()

        assertEquals(finalDateOrganizerRequiredMessage(), viewModel.confirmationError.value)
        assertTrue(repository.statusUpdates.isEmpty())
        assertEquals(
            finalDateOrganizerRequiredAnalyticsContext(),
            analyticsProvider.events.filterIsInstance<AnalyticsEvent.ErrorOccurred>().last().errorContext
        )
    }

    @Test
    fun confirmFinalDateUpdatesEventStatusForOrganizer() = runTest {
        val event = testEvent(listOf("slot-1"))
        repository.organizerIds += "organizer"
        var successCalled = false

        viewModel.selectFinalSlot("slot-1")
        viewModel.confirmFinalDate(event, userId = "organizer") {
            successCalled = true
        }
        advanceUntilIdle()

        assertEquals(
            listOf(StatusUpdate(eventId, EventStatus.CONFIRMED, "2026-07-14T09:00:00Z")),
            repository.statusUpdates
        )
        assertTrue(successCalled)
        assertTrue(viewModel.hasConfirmedFinalDate.value)
        assertFalse(viewModel.isConfirmingFinalDate.value)
    }

    @Test
    fun confirmFinalDateUsesGenericMessageWhenRepositoryFails() = runTest {
        val event = testEvent(listOf("slot-1"))
        repository.organizerIds += "organizer"
        repository.updateStatusFailure = IllegalStateException(
            "Backend 500 while confirming event-1 for secret@example.com token=SECRET"
        )

        viewModel.selectFinalSlot("slot-1")
        viewModel.confirmFinalDate(event, userId = "organizer", onSuccess = {})
        advanceUntilIdle()

        assertEquals(finalDateConfirmationFailureMessage(), viewModel.confirmationError.value)
        assertFalse(viewModel.hasConfirmedFinalDate.value)
        assertLastErrorContextDoesNotExpose("secret@example.com")
        assertLastErrorContextDoesNotExpose("SECRET")
        assertLastErrorContextDoesNotExpose("Backend 500")
    }

    @Test
    fun pollErrorHelpersUseStableCopyAndContexts() {
        assertEquals("Impossible d'enregistrer vos votes. Réessayez.", pollVoteSubmissionFailureMessage())
        assertEquals("Votez sur tous les créneaux avant d'envoyer vos réponses.", pollVoteAllSlotsRequiredMessage())
        assertEquals("Impossible de confirmer la date finale. Réessayez.", finalDateConfirmationFailureMessage())
        assertEquals("Sélectionnez un créneau avant de confirmer.", finalDateSlotRequiredMessage())
        assertEquals("Seul l'organisateur peut confirmer la date finale.", finalDateOrganizerRequiredMessage())
        assertEquals("vote_submission_failed", pollVoteFailureAnalyticsContext())
        assertEquals("poll_close_failed", pollCloseFailureAnalyticsContext())
        assertEquals("final_date_confirmation_failed", finalDateConfirmationFailureAnalyticsContext())
        assertEquals("organizer_required", finalDateOrganizerRequiredAnalyticsContext())
    }

    @Test
    fun pollUserFacingErrorsDoNotUseEnglishDefaults() {
        listOf(
            pollVoteSubmissionFailureMessage(),
            pollVoteAllSlotsRequiredMessage(),
            finalDateConfirmationFailureMessage(),
            finalDateSlotRequiredMessage(),
            finalDateOrganizerRequiredMessage()
        ).forEach { message ->
            listOf(
                "Please vote",
                "time slots",
                "Select a time slot",
                "before confirming",
                "Failed to"
            ).forEach { englishCopy ->
                assertFalse(
                    message.contains(englishCopy, ignoreCase = true),
                    "Message should not contain `$englishCopy`: $message"
                )
            }
        }
    }

    private fun testEvent(slotIds: List<String>): Event =
        Event(
            id = eventId,
            title = "Poll event",
            description = "Poll description",
            organizerId = "organizer",
            participants = listOf("participant-42"),
            proposedSlots = slotIds.map { slotId ->
                TimeSlot(
                    id = slotId,
                    start = "2026-07-14T09:00:00Z",
                    end = "2026-07-14T18:00:00Z",
                    timezone = "Europe/Paris"
                )
            },
            deadline = "2026-07-01T12:00:00Z",
            status = EventStatus.POLLING,
            createdAt = "2026-06-01T08:00:00Z",
            updatedAt = "2026-06-01T08:00:00Z"
        )

    private data class AddedVote(
        val eventId: String,
        val participantId: String,
        val slotId: String,
        val vote: Vote
    )

    private data class StatusUpdate(
        val eventId: String,
        val status: EventStatus,
        val finalDate: String?
    )

    private class FakeEventRepository : EventRepositoryInterface {
        val addedVotes = mutableListOf<AddedVote>()
        val statusUpdates = mutableListOf<StatusUpdate>()
        val organizerIds = mutableSetOf<String>()
        var addVoteFailure: Throwable? = null
        var updateStatusFailure: Throwable? = null

        override suspend fun createEvent(event: Event): Result<Event> = Result.success(event)
        override fun getEvent(id: String): Event? = null
        override fun getPoll(eventId: String): Poll? = Poll(id = "poll-1", eventId = eventId, votes = emptyMap())
        override suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean> = Result.success(true)
        override fun getParticipants(eventId: String): List<String>? = emptyList()

        override suspend fun addVote(
            eventId: String,
            participantId: String,
            slotId: String,
            vote: Vote
        ): Result<Boolean> {
            addVoteFailure?.let { return Result.failure(it) }
            addedVotes += AddedVote(eventId, participantId, slotId, vote)
            return Result.success(true)
        }

        override suspend fun updateEvent(event: Event): Result<Event> = Result.success(event)
        override suspend fun updateEventStatus(id: String, status: EventStatus, finalDate: String?): Result<Boolean> {
            updateStatusFailure?.let { return Result.failure(it) }
            statusUpdates += StatusUpdate(id, status, finalDate)
            return Result.success(true)
        }
        override suspend fun saveEvent(event: Event): Result<Event> = Result.success(event)
        override suspend fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
        override fun isDeadlinePassed(deadline: String): Boolean = false
        override fun isOrganizer(eventId: String, userId: String): Boolean = userId in organizerIds
        override fun canModifyEvent(eventId: String, userId: String): Boolean = false
        override fun getAllEvents(): List<Event> = emptyList()

        override fun getEventsPaginated(
            page: Int,
            pageSize: Int,
            orderBy: OrderBy
        ): Flow<List<Event>> = flowOf(emptyList())
    }

    private class RecordingAnalyticsProvider : AnalyticsProvider {
        val events = mutableListOf<AnalyticsEvent>()

        override fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any?>) {
            events += event
        }

        override fun setUserProperty(name: String, value: String) = Unit
        override fun setUserId(userId: String?) = Unit
        override fun setEnabled(enabled: Boolean) = Unit
        override fun clearUserData() = Unit
    }

    private fun assertLastErrorContextDoesNotExpose(value: String) {
        val lastError = analyticsProvider.events.filterIsInstance<AnalyticsEvent.ErrorOccurred>().last()
        assertFalse(
            lastError.errorContext?.contains(value, ignoreCase = true) == true,
            "Analytics error context should not expose `$value`: ${lastError.errorContext}"
        )
    }

    private companion object {
        const val eventId = "event-1"
    }
}
