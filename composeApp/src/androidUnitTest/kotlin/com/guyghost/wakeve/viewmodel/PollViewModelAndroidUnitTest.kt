package com.guyghost.wakeve.viewmodel

import com.guyghost.wakeve.analytics.AnalyticsEvent
import com.guyghost.wakeve.analytics.AnalyticsProvider
import com.guyghost.wakeve.confirmation.ConfirmationClock
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachine
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import com.guyghost.wakeve.repository.EventRepositoryInterface
import com.guyghost.wakeve.repository.OrderBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
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
    private lateinit var confirmationMachineScope: TestScope
    private lateinit var confirmationStateMachine: EventManagementStateMachine

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeEventRepository()
        analyticsProvider = RecordingAnalyticsProvider()
        confirmationMachineScope = TestScope(StandardTestDispatcher())
        confirmationStateMachine = EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(repository),
            createEventUseCase = CreateEventUseCase(repository),
            eventRepository = repository,
            confirmationClock = ConfirmationClock { confirmationInstant },
            scope = confirmationMachineScope
        )
        viewModel = PollViewModel(
            eventRepository = repository,
            eventId = eventId,
            analyticsProvider = analyticsProvider,
            confirmationStateMachine = confirmationStateMachine,
            confirmationOperationIdProvider = { "operation-android-adapter" }
        )
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
    fun confirmationStateMachineCommitsTheSecondSelectedSlotWithTheFixedClock() = runTest {
        repository.seedConfirmationEvent(testEvent(listOf("slot-1", "slot-2")))

        confirmationStateMachine.dispatch(
            EventManagementContract.Intent.OpenConfirmPrompt(eventId, "slot-2", "organizer")
        )
        confirmationMachineScope.advanceUntilIdle()
        assertEquals(
            EventManagementContract.ConfirmationPhase.CONFIRM_PROMPT,
            confirmationStateMachine.state.value.confirmationPhase
        )

        confirmationStateMachine.dispatch(
            EventManagementContract.Intent.SubmitConfirmation("operation-second-slot")
        )
        confirmationMachineScope.advanceUntilIdle()

        val command = repository.confirmationCommands.single()
        val receipt = repository.committedReceipts.single()
        assertConfirmationCommand(command, "operation-second-slot")
        assertEquals(confirmationInstant, command.requestedAt)
        assertEquals("operation-second-slot", receipt.operationId)
        assertEquals("slot-2", receipt.slotId)
        assertEquals("2026-07-21T14:00:00Z", repository.persistedEvent(eventId)?.finalDate)
        assertEquals(EventManagementContract.ConfirmationPhase.CONFIRMED_PENDING_SYNC, confirmationStateMachine.state.value.confirmationPhase)
        assertEquals("operation-second-slot", confirmationStateMachine.state.value.confirmationOperationId)
        assertEquals(receipt.receiptId, confirmationStateMachine.state.value.confirmationReceiptId)
        assertTrue(repository.statusUpdates.isEmpty(), "the receipt path must not call updateEventStatus")
    }

    @Test
    fun confirmationStateMachineTreatsAlreadyCommittedSameSlotAsReceiptGovernedSuccess() = runTest {
        repository.seedConfirmationEvent(testEvent(listOf("slot-1", "slot-2")))
        repository.confirmationResult = { command -> repository.alreadyCommitted(command) }

        confirmationStateMachine.dispatch(
            EventManagementContract.Intent.OpenConfirmPrompt(eventId, "slot-2", "organizer")
        )
        confirmationStateMachine.dispatch(
            EventManagementContract.Intent.SubmitConfirmation("operation-idempotent")
        )
        confirmationMachineScope.advanceUntilIdle()

        val command = repository.confirmationCommands.single()
        assertConfirmationCommand(command, "operation-idempotent")
        assertEquals(EventManagementContract.ConfirmationPhase.CONFIRMED_PENDING_SYNC, confirmationStateMachine.state.value.confirmationPhase)
        assertEquals("operation-idempotent", confirmationStateMachine.state.value.confirmationOperationId)
        assertEquals("receipt-operation-idempotent", confirmationStateMachine.state.value.confirmationReceiptId)
        assertTrue(repository.statusUpdates.isEmpty(), "AlreadyCommitted must not fall back to updateEventStatus")
    }

    @Test
    fun confirmationStateMachineTypedFailureKeepsTheSecondSlotAttemptOutOfSuccess() = runTest {
        repository.seedConfirmationEvent(testEvent(listOf("slot-1", "slot-2")))
        repository.confirmationResult = { command ->
            EventManagementContract.ConfirmationResult.Failed(
                operationId = command.operationId,
                failure = EventManagementContract.ConfirmationFailure(
                    EventManagementContract.ConfirmationFailureCode.LOCAL_PERSISTENCE_FAILED,
                    retryable = true
                )
            )
        }

        confirmationStateMachine.dispatch(
            EventManagementContract.Intent.OpenConfirmPrompt(eventId, "slot-2", "organizer")
        )
        confirmationStateMachine.dispatch(
            EventManagementContract.Intent.SubmitConfirmation("operation-failed")
        )
        confirmationMachineScope.advanceUntilIdle()

        assertConfirmationCommand(repository.confirmationCommands.single(), "operation-failed")
        assertEquals(EventManagementContract.ConfirmationPhase.FAILED, confirmationStateMachine.state.value.confirmationPhase)
        assertEquals("operation-failed", confirmationStateMachine.state.value.confirmationOperationId)
        assertEquals(null, confirmationStateMachine.state.value.confirmationReceiptId)
        assertEquals(EventManagementContract.ConfirmationFailureCode.LOCAL_PERSISTENCE_FAILED, confirmationStateMachine.state.value.confirmationFailure?.code)
        assertTrue(repository.statusUpdates.isEmpty(), "a typed failure must not call updateEventStatus")
    }

    @Test
    fun confirmationStateMachineConflictKeepsTheSecondSlotAttemptOutOfSuccess() = runTest {
        repository.seedConfirmationEvent(testEvent(listOf("slot-1", "slot-2")))
        repository.confirmationResult = { command ->
            EventManagementContract.ConfirmationResult.Conflict(
                operationId = command.operationId,
                failure = EventManagementContract.ConfirmationFailure(
                    EventManagementContract.ConfirmationFailureCode.ALREADY_CONFIRMED_DIFFERENT_SLOT,
                    retryable = false
                )
            )
        }

        confirmationStateMachine.dispatch(
            EventManagementContract.Intent.OpenConfirmPrompt(eventId, "slot-2", "organizer")
        )
        confirmationStateMachine.dispatch(
            EventManagementContract.Intent.SubmitConfirmation("operation-conflict")
        )
        confirmationMachineScope.advanceUntilIdle()

        assertConfirmationCommand(repository.confirmationCommands.single(), "operation-conflict")
        assertEquals(EventManagementContract.ConfirmationPhase.FAILED, confirmationStateMachine.state.value.confirmationPhase)
        assertEquals("operation-conflict", confirmationStateMachine.state.value.confirmationOperationId)
        assertEquals(null, confirmationStateMachine.state.value.confirmationReceiptId)
        assertEquals(EventManagementContract.ConfirmationFailureCode.ALREADY_CONFIRMED_DIFFERENT_SLOT, confirmationStateMachine.state.value.confirmationFailure?.code)
        assertTrue(repository.statusUpdates.isEmpty(), "a conflict must not call updateEventStatus")
    }

    /**
     * Android must be an adapter of the authoritative state machine. A direct
     * PollViewModel -> repository.confirmPollDate implementation cannot satisfy
     * this contract because this injected machine would remain in REVIEWING_RESULTS.
     */
    @Test
    fun selectingFinalSlotIsPresentationOnlyAndExplicitRequestUsesTheAuthenticatedActor() = runTest {
        val event = testEvent(listOf("slot-1", "slot-2"))
        repository.seedConfirmationEvent(event)
        var successCalled = false

        viewModel.selectFinalSlot("slot-2")
        confirmationMachineScope.advanceUntilIdle()
        assertEquals(
            EventManagementContract.ConfirmationPhase.REVIEWING_RESULTS,
            confirmationStateMachine.state.value.confirmationPhase
        )
        assertTrue(repository.confirmationCommands.isEmpty(), "selection must not dispatch confirmation")

        viewModel.confirmFinalDate(event, userId = authenticatedActorId) {
            successCalled = true
        }
        confirmationMachineScope.advanceUntilIdle()

        assertEquals(EventManagementContract.ConfirmationPhase.CONFIRM_PROMPT, confirmationStateMachine.state.value.confirmationPhase)
        assertEquals("slot-2", confirmationStateMachine.state.value.confirmationSlotId)
        assertEquals(authenticatedActorId, confirmationStateMachine.state.value.confirmationActorId)
        assertTrue(repository.confirmationCommands.isEmpty(), "opening the prompt must not submit a command")
        assertFalse(successCalled)
        assertFalse(viewModel.hasConfirmedFinalDate.value)
    }

    @Test
    fun submitFinalDateConfirmationWaitsForTheCommittedReceiptBeforeAndroidSuccess() = runTest {
        val event = testEvent(listOf("slot-1", "slot-2"))
        repository.seedConfirmationEvent(event)
        val deferredResult = CompletableDeferred<EventManagementContract.ConfirmationResult>()
        repository.deferredConfirmationResult = deferredResult
        var successCalled = false

        viewModel.selectFinalSlot("slot-2")
        viewModel.confirmFinalDate(event, userId = "organizer") {
            successCalled = true
        }
        confirmationMachineScope.advanceUntilIdle()
        assertEquals(EventManagementContract.ConfirmationPhase.CONFIRM_PROMPT, confirmationStateMachine.state.value.confirmationPhase)
        assertTrue(repository.confirmationCommands.isEmpty())
        assertFalse(successCalled)
        assertFalse(viewModel.hasConfirmedFinalDate.value)

        viewModel.submitFinalDateConfirmation()
        confirmationMachineScope.advanceUntilIdle()

        val command = repository.confirmationCommands.single()
        assertConfirmationCommand(command, "operation-android-adapter")
        assertEquals(EventManagementContract.ConfirmationPhase.CONFIRMING, confirmationStateMachine.state.value.confirmationPhase)
        assertEquals("operation-android-adapter", confirmationStateMachine.state.value.confirmationOperationId)
        assertFalse(successCalled)
        assertFalse(viewModel.hasConfirmedFinalDate.value)
        assertEquals(null, confirmationStateMachine.state.value.confirmationReceiptId)

        deferredResult.complete(repository.committedResult(command))
        confirmationMachineScope.advanceUntilIdle()

        assertEquals(EventManagementContract.ConfirmationPhase.CONFIRMED_PENDING_SYNC, confirmationStateMachine.state.value.confirmationPhase)
        assertEquals("receipt-operation-android-adapter", confirmationStateMachine.state.value.confirmationReceiptId)
        assertTrue(repository.statusUpdates.isEmpty(), "the adapter must not call updateEventStatus")
        assertTrue(successCalled, "only the committed machine receipt may release Android success")
        assertTrue(viewModel.hasConfirmedFinalDate.value)
    }

    @Test
    fun uncorrelatedAlreadyCommittedReceiptNeverReleasesAndroidSuccess() = runTest {
        val uncorrelatedReceipts = listOf(
            UncorrelatedReceipt("historical operation", operationId = "operation-historical"),
            UncorrelatedReceipt("foreign event", eventId = "event-foreign"),
            UncorrelatedReceipt("foreign slot", slotId = "slot-1"),
            UncorrelatedReceipt("forged domain event", domainEventId = "forged-domain-event"),
            UncorrelatedReceipt("forged effect key", effectKey = "forged-effect-key")
        )

        uncorrelatedReceipts.forEach { mismatch ->
            val localRepository = FakeEventRepository()
            val localMachineScope = TestScope(StandardTestDispatcher())
            val localMachine = EventManagementStateMachine(
                loadEventsUseCase = LoadEventsUseCase(localRepository),
                createEventUseCase = CreateEventUseCase(localRepository),
                eventRepository = localRepository,
                confirmationClock = ConfirmationClock { confirmationInstant },
                scope = localMachineScope
            )
            val localViewModel = PollViewModel(
                eventRepository = localRepository,
                eventId = eventId,
                analyticsProvider = RecordingAnalyticsProvider(),
                confirmationStateMachine = localMachine,
                confirmationOperationIdProvider = { "operation-new" }
            )
            val event = testEvent(listOf("slot-1", "slot-2"))
            var successCalled = false
            localRepository.seedConfirmationEvent(event)
            localRepository.confirmationResult = { command ->
                localRepository.alreadyCommittedWithReceipt(command, mismatch)
            }

            localViewModel.selectFinalSlot("slot-2")
            localViewModel.confirmFinalDate(event, userId = "organizer") {
                successCalled = true
            }
            localMachineScope.advanceUntilIdle()
            localViewModel.submitFinalDateConfirmation()
            localMachineScope.advanceUntilIdle()

            assertEquals(
                EventManagementContract.ConfirmationPhase.CONFIRMING,
                localMachine.state.value.confirmationPhase,
                "${mismatch.label} receipt must be ignored rather than confirm operation-new"
            )
            assertFalse(successCalled, "${mismatch.label} receipt must not release Android success")
            assertFalse(
                localViewModel.hasConfirmedFinalDate.value,
                "${mismatch.label} receipt must not render the new attempt as confirmed"
            )
            localViewModel.disposeConfirmationAdapter()
        }
    }

    @Test
    fun adapterFailureNeverReleasesAndroidSuccess() = runTest {
        val event = testEvent(listOf("slot-1", "slot-2"))
        repository.seedConfirmationEvent(event)
        repository.confirmationResult = { command ->
            EventManagementContract.ConfirmationResult.Failed(
                operationId = command.operationId,
                failure = EventManagementContract.ConfirmationFailure(
                    EventManagementContract.ConfirmationFailureCode.LOCAL_PERSISTENCE_FAILED,
                    retryable = true
                )
            )
        }
        var successCalled = false

        viewModel.selectFinalSlot("slot-2")
        viewModel.confirmFinalDate(event, userId = "organizer") { successCalled = true }
        confirmationMachineScope.advanceUntilIdle()
        viewModel.submitFinalDateConfirmation()
        confirmationMachineScope.advanceUntilIdle()

        assertEquals(
            EventManagementContract.ConfirmationPhase.FAILED,
            viewModel.confirmationPhase.value
        )
        assertFalse(successCalled)
        assertFalse(viewModel.hasConfirmedFinalDate.value)
        assertTrue(viewModel.canRetryFinalDateConfirmation.value)
        assertTrue(repository.statusUpdates.isEmpty())
    }

    @Test
    fun adapterConflictNeverReleasesAndroidSuccess() = runTest {
        val event = testEvent(listOf("slot-1", "slot-2"))
        repository.seedConfirmationEvent(event)
        repository.confirmationResult = { command ->
            EventManagementContract.ConfirmationResult.Conflict(
                operationId = command.operationId,
                failure = EventManagementContract.ConfirmationFailure(
                    EventManagementContract.ConfirmationFailureCode.ALREADY_CONFIRMED_DIFFERENT_SLOT,
                    retryable = false
                )
            )
        }
        var successCalled = false

        viewModel.selectFinalSlot("slot-2")
        viewModel.confirmFinalDate(event, userId = "organizer") { successCalled = true }
        confirmationMachineScope.advanceUntilIdle()
        viewModel.submitFinalDateConfirmation()
        confirmationMachineScope.advanceUntilIdle()

        assertEquals(
            EventManagementContract.ConfirmationPhase.FAILED,
            viewModel.confirmationPhase.value
        )
        assertFalse(successCalled)
        assertFalse(viewModel.hasConfirmedFinalDate.value)
        assertFalse(viewModel.canRetryFinalDateConfirmation.value)
        assertTrue(repository.statusUpdates.isEmpty())
    }

    @Test
    fun rehydratedReceiptRendersConfirmedWithoutReplayingCallback() = runTest {
        val event = testEvent(listOf("slot-1", "slot-2")).copy(
            status = EventStatus.CONFIRMED,
            finalDate = "2026-07-21T14:00:00Z"
        )
        repository.seedConfirmationEvent(event)
        repository.loadedConfirmationProjection = EventManagementContract.ConfirmationProjection.Confirmed(
            eventId = event.id,
            slotId = "slot-2",
            receiptId = "receipt-rehydrated",
            decisionSyncStatus = EventManagementContract.DecisionSyncStatus.LOCAL_PENDING,
            effectDispatchStatus = EventManagementContract.EffectDispatchStatus.QUEUED
        )
        val rehydrationScope = TestScope(StandardTestDispatcher())
        val rehydrationMachine = EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(repository),
            createEventUseCase = CreateEventUseCase(repository),
            eventRepository = repository,
            confirmationClock = ConfirmationClock { confirmationInstant },
            scope = rehydrationScope
        )
        val rehydratedViewModel = PollViewModel(
            eventRepository = repository,
            eventId = event.id,
            analyticsProvider = analyticsProvider,
            confirmationStateMachine = rehydrationMachine,
            confirmationOperationIdProvider = { "must-not-be-dispatched" }
        )
        rehydrationScope.advanceUntilIdle()

        assertEquals(
            EventManagementContract.ConfirmationPhase.CONFIRMED_PENDING_SYNC,
            rehydratedViewModel.confirmationPhase.value
        )
        assertTrue(rehydratedViewModel.hasConfirmedFinalDate.value)
        assertTrue(repository.confirmationCommands.isEmpty())

        var callbackReplayed = false
        rehydratedViewModel.selectFinalSlot("slot-2")
        rehydratedViewModel.confirmFinalDate(event, userId = "organizer") {
            callbackReplayed = true
        }
        rehydrationScope.advanceUntilIdle()

        assertFalse(callbackReplayed)
        assertTrue(repository.confirmationCommands.isEmpty())
    }

    @Test
    fun aFreshAdapterMachineCanConfirmASecondEventAfterTheFirstMachineIsTerminal() = runTest {
        val firstEvent = testEvent(listOf("slot-1", "slot-2"))
        repository.seedConfirmationEvent(firstEvent)
        viewModel.selectFinalSlot("slot-2")
        viewModel.confirmFinalDate(firstEvent, userId = "organizer", onSuccess = {})
        confirmationMachineScope.advanceUntilIdle()
        viewModel.submitFinalDateConfirmation()
        confirmationMachineScope.advanceUntilIdle()

        assertEquals(
            EventManagementContract.ConfirmationPhase.CONFIRMED_PENDING_SYNC,
            confirmationStateMachine.state.value.confirmationPhase
        )

        val secondEventId = "event-2"
        val secondEvent = testEvent(listOf("slot-1", "slot-2")).copy(id = secondEventId)
        repository.seedConfirmationEvent(secondEvent)
        val secondMachineScope = TestScope(StandardTestDispatcher())
        val secondMachine = EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(repository),
            createEventUseCase = CreateEventUseCase(repository),
            eventRepository = repository,
            confirmationClock = ConfirmationClock { confirmationInstant },
            scope = secondMachineScope
        )
        val secondViewModel = PollViewModel(
            eventRepository = repository,
            eventId = secondEventId,
            analyticsProvider = analyticsProvider,
            confirmationStateMachine = secondMachine,
            confirmationOperationIdProvider = { "operation-second-event" }
        )
        var secondSuccessCalled = false

        secondViewModel.selectFinalSlot("slot-2")
        secondViewModel.confirmFinalDate(secondEvent, userId = "organizer") {
            secondSuccessCalled = true
        }
        secondMachineScope.advanceUntilIdle()
        secondViewModel.submitFinalDateConfirmation()
        secondMachineScope.advanceUntilIdle()

        assertEquals(
            EventManagementContract.ConfirmationPhase.CONFIRMED_PENDING_SYNC,
            secondMachine.state.value.confirmationPhase
        )
        val secondCommand = repository.confirmationCommands.last()
        assertEquals(secondEventId, secondCommand.eventId)
        assertEquals("slot-2", secondCommand.slotId)
        assertEquals("operation-second-event", secondCommand.operationId)
        assertTrue(secondSuccessCalled)
        assertEquals(EventStatus.CONFIRMED, repository.persistedEvent(secondEventId)?.status)
        assertTrue(repository.statusUpdates.isEmpty())
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

    private fun assertConfirmationCommand(
        command: EventManagementContract.ConfirmPollDateCommand,
        operationId: String
    ) {
        assertEquals(eventId, command.eventId)
        assertEquals("slot-2", command.slotId)
        assertEquals("organizer", command.actorId)
        assertEquals(operationId, command.operationId)
        assertTrue(command.operationId.isNotBlank())
        assertEquals(confirmationInstant, command.requestedAt)
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
                    start = if (slotId == "slot-2") "2026-07-21T14:00:00Z" else "2026-07-14T09:00:00Z",
                    end = if (slotId == "slot-2") "2026-07-21T18:00:00Z" else "2026-07-14T18:00:00Z",
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

    private data class UncorrelatedReceipt(
        val label: String,
        val operationId: String? = null,
        val eventId: String? = null,
        val slotId: String? = null,
        val domainEventId: String? = null,
        val effectKey: String? = null
    )

    private class FakeEventRepository : EventRepositoryInterface {
        val addedVotes = mutableListOf<AddedVote>()
        val statusUpdates = mutableListOf<StatusUpdate>()
        val confirmationCommands = mutableListOf<EventManagementContract.ConfirmPollDateCommand>()
        val committedReceipts = mutableListOf<EventManagementContract.ConfirmationReceipt>()
        private val events = mutableMapOf<String, Event>()
        private val polls = mutableMapOf<String, Poll>()
        val organizerIds = mutableSetOf<String>()
        var addVoteFailure: Throwable? = null
        var deferredConfirmationResult: CompletableDeferred<EventManagementContract.ConfirmationResult>? = null
        var loadedConfirmationProjection: EventManagementContract.ConfirmationProjection? = null
        var confirmationResult: (EventManagementContract.ConfirmPollDateCommand) -> EventManagementContract.ConfirmationResult =
            { command -> committed(command) }

        fun seedConfirmationEvent(event: Event) {
            events[event.id] = event
            polls[event.id] = Poll(
                id = "poll-${event.id}",
                eventId = event.id,
                votes = mapOf("participant-42" to mapOf("slot-2" to Vote.YES))
            )
        }

        fun persistedEvent(eventId: String): Event? = events[eventId]

        override suspend fun createEvent(event: Event): Result<Event> {
            events[event.id] = event
            return Result.success(event)
        }
        override fun getEvent(id: String): Event? = events[id]
        override fun getPoll(eventId: String): Poll? = polls[eventId] ?: Poll(id = "poll-1", eventId = eventId, votes = emptyMap())
        override suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean> = Result.success(true)
        override fun getParticipants(eventId: String): List<String>? = events[eventId]?.participants ?: emptyList()

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

        override suspend fun updateEvent(event: Event): Result<Event> {
            events[event.id] = event
            return Result.success(event)
        }
        override suspend fun updateEventStatus(id: String, status: EventStatus, finalDate: String?): Result<Boolean> {
            statusUpdates += StatusUpdate(id, status, finalDate)
            return Result.success(true)
        }
        override suspend fun confirmPollDate(
            command: EventManagementContract.ConfirmPollDateCommand
        ): EventManagementContract.ConfirmationResult {
            confirmationCommands += command
            deferredConfirmationResult?.let { return it.await() }
            return confirmationResult(command).also { result ->
                if (result is EventManagementContract.ConfirmationResult.Committed) {
                    committedReceipts += result.receipt
                }
            }
        }
        override fun loadConfirmationProjection(
            eventId: String
        ): EventManagementContract.ConfirmationProjection =
            loadedConfirmationProjection ?: EventManagementContract.ConfirmationProjection.Reviewing(eventId)
        override suspend fun saveEvent(event: Event): Result<Event> {
            events[event.id] = event
            return Result.success(event)
        }
        override suspend fun deleteEvent(eventId: String): Result<Unit> {
            events.remove(eventId)
            polls.remove(eventId)
            return Result.success(Unit)
        }
        override fun isDeadlinePassed(deadline: String): Boolean = false
        override fun isOrganizer(eventId: String, userId: String): Boolean =
            events[eventId]?.organizerId == userId || userId in organizerIds
        override fun canModifyEvent(eventId: String, userId: String): Boolean = false
        override fun getAllEvents(): List<Event> = events.values.toList()

        override fun getEventsPaginated(
            page: Int,
            pageSize: Int,
            orderBy: OrderBy
        ): Flow<List<Event>> = flowOf(events.values.toList())

        fun alreadyCommitted(
            command: EventManagementContract.ConfirmPollDateCommand
        ): EventManagementContract.ConfirmationResult.AlreadyCommitted =
            alreadyCommittedWithReceipt(command, UncorrelatedReceipt(label = "correlated"))

        fun alreadyCommittedWithReceipt(
            command: EventManagementContract.ConfirmPollDateCommand,
            mismatch: UncorrelatedReceipt
        ): EventManagementContract.ConfirmationResult.AlreadyCommitted {
            val receiptOperationId = mismatch.operationId ?: command.operationId
            val receiptEventId = mismatch.eventId ?: command.eventId
            val receiptSlotId = mismatch.slotId ?: command.slotId
            val expectedDomainEventId = "poll-date-confirmed:$receiptEventId:$receiptSlotId:v1"
            val receipt = EventManagementContract.ConfirmationReceipt(
                receiptId = "receipt-$receiptOperationId",
                operationId = receiptOperationId,
                eventId = receiptEventId,
                slotId = receiptSlotId,
                actorId = command.actorId,
                committedAt = command.requestedAt.toString(),
                nextNavigationTarget = "event/$receiptEventId/scenarios",
                decisionSyncStatus = EventManagementContract.DecisionSyncStatus.LOCAL_PENDING,
                effectDispatchStatus = EventManagementContract.EffectDispatchStatus.QUEUED,
                effectOutbox = EventManagementContract.ConfirmationEffectOutbox(
                    domainEventId = mismatch.domainEventId ?: expectedDomainEventId,
                    effectKey = mismatch.effectKey ?: "$expectedDomainEventId:confirmation"
                )
            )
            return EventManagementContract.ConfirmationResult.AlreadyCommitted(
                receipt = receipt,
                projection = confirmationProjection(receipt)
            )
        }

        private fun committed(
            command: EventManagementContract.ConfirmPollDateCommand
        ): EventManagementContract.ConfirmationResult.Committed = committedResult(command)

        fun committedResult(
            command: EventManagementContract.ConfirmPollDateCommand
        ): EventManagementContract.ConfirmationResult.Committed {
            val selectedSlot = events[command.eventId]?.proposedSlots?.firstOrNull { it.id == command.slotId }
            if (selectedSlot != null) {
                events[command.eventId] = events.getValue(command.eventId).copy(
                    status = EventStatus.CONFIRMED,
                    finalDate = selectedSlot.start
                )
            }
            val receipt = receiptFor(command)
            return EventManagementContract.ConfirmationResult.Committed(
                receipt = receipt,
                projection = confirmationProjection(receipt)
            )
        }

        private fun receiptFor(
            command: EventManagementContract.ConfirmPollDateCommand
        ): EventManagementContract.ConfirmationReceipt = EventManagementContract.ConfirmationReceipt(
                receiptId = "receipt-${command.operationId}",
                operationId = command.operationId,
                eventId = command.eventId,
                slotId = command.slotId,
                actorId = command.actorId,
                committedAt = command.requestedAt.toString(),
                nextNavigationTarget = "event/${command.eventId}/scenarios",
                decisionSyncStatus = EventManagementContract.DecisionSyncStatus.LOCAL_PENDING,
                effectDispatchStatus = EventManagementContract.EffectDispatchStatus.QUEUED,
                effectOutbox = EventManagementContract.ConfirmationEffectOutbox(
                    domainEventId = "poll-date-confirmed:${command.eventId}:${command.slotId}:v1",
                    effectKey = "poll-date-confirmed:${command.eventId}:${command.slotId}:v1:confirmation"
                )
            )

        private fun confirmationProjection(
            receipt: EventManagementContract.ConfirmationReceipt
        ) = EventManagementContract.ConfirmationProjection.Confirmed(
            eventId = receipt.eventId,
            slotId = receipt.slotId,
            receiptId = receipt.receiptId,
            decisionSyncStatus = receipt.decisionSyncStatus,
            effectDispatchStatus = receipt.effectDispatchStatus
        )
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
        const val authenticatedActorId = "authenticated-session-actor"
        val confirmationInstant: Instant = Instant.parse("2034-05-06T07:08:09Z")
    }
}
