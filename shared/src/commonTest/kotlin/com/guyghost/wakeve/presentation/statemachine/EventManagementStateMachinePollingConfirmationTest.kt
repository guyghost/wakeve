package com.guyghost.wakeve.presentation.statemachine

import com.guyghost.wakeve.confirmation.confirmationEffectKeys
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import com.guyghost.wakeve.repository.EventRepositoryInterface
import com.guyghost.wakeve.repository.OrderBy
import com.guyghost.wakeve.workflow.WorkflowOutboxRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EventManagementStateMachinePollingConfirmationTest {

    private lateinit var repository: PollingConfirmationRepository
    private lateinit var stateMachine: EventManagementStateMachine
    private lateinit var machineScope: TestScope
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        repository = PollingConfirmationRepository()
        machineScope = TestScope(dispatcher)
        stateMachine = EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(repository),
            createEventUseCase = CreateEventUseCase(repository),
            eventRepository = repository,
            scope = machineScope
        )
    }

    @Test
    fun `deadline closes votes but still permits organizer confirmation`() = runTest {
        val eventId = "event-deadline-passed"
        val organizerId = "organizer-1"
        val slotId = "slot-1"
        repository.events[eventId] = eventFixture(
            id = eventId,
            organizerId = organizerId,
            status = EventStatus.POLLING,
            deadline = "2026-05-01T10:00:00Z"
        )
        repository.polls[eventId] = pollWithYesVote(eventId, slotId)
        repository.deadlinePassed = true

        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, slotId, organizerId))
        machineScope.advanceUntilIdle()

        val event = repository.events.getValue(eventId)
        assertEquals(EventStatus.CONFIRMED, event.status)
        assertEquals("2026-06-10T09:00:00Z", event.finalDate)
        assertNull(stateMachine.state.value.error)
    }

    @Test
    fun `confirmation remains eligible before the injected deadline`() = runTest {
        assertConfirmationAllowedAtDeadlineBoundary(deadlinePassed = false, eventId = "event-before-deadline")
    }

    @Test
    fun `confirmation remains eligible exactly at the injected deadline`() = runTest {
        assertConfirmationAllowedAtDeadlineBoundary(deadlinePassed = true, eventId = "event-at-deadline")
    }

    @Test
    fun `vote mutation is closed exactly at the injected deadline`() = runTest {
        val eventId = "event-vote-at-deadline"
        repository.events[eventId] = eventFixture(eventId, "organizer-1", EventStatus.POLLING)
        repository.polls[eventId] = pollWithYesVote(eventId, "slot-1")
        repository.deadlinePassed = true

        val result = repository.addVote(eventId, "participant-1", "slot-1", Vote.MAYBE)

        assertTrue(result.isFailure, "Vote mutation must close at now == deadline")
    }

    @Test
    fun `opening and cancelling prompt dispatches zero confirmation commands`() = runTest {
        val eventId = "event-cancel"
        repository.events[eventId] = eventFixture(eventId, "organizer-1", EventStatus.POLLING)
        repository.polls[eventId] = pollWithYesVote(eventId, "slot-1")

        stateMachine.dispatch(EventManagementContract.Intent.OpenConfirmPrompt(eventId, "slot-1", "organizer-1"))
        machineScope.advanceUntilIdle()
        assertEquals(EventManagementContract.ConfirmationPhase.CONFIRM_PROMPT, stateMachine.state.value.confirmationPhase)

        stateMachine.dispatch(EventManagementContract.Intent.CancelConfirmation)
        machineScope.advanceUntilIdle()

        assertEquals(EventManagementContract.ConfirmationPhase.REVIEWING_RESULTS, stateMachine.state.value.confirmationPhase)
        assertNull(stateMachine.state.value.confirmationSlotId)
        assertEquals(0, repository.confirmCommandCount)
    }

    @Test
    fun `submit captures one operation and duplicate submit dispatches one command`() = runTest {
        val eventId = "event-one-flight"
        repository.events[eventId] = eventFixture(eventId, "organizer-1", EventStatus.POLLING)
        repository.polls[eventId] = pollWithYesVote(eventId, "slot-1")

        stateMachine.dispatch(EventManagementContract.Intent.OpenConfirmPrompt(eventId, "slot-1", "organizer-1"))
        machineScope.advanceUntilIdle()
        stateMachine.dispatch(EventManagementContract.Intent.SubmitConfirmation("operation-1"))
        stateMachine.dispatch(EventManagementContract.Intent.SubmitConfirmation("operation-2"))
        machineScope.advanceUntilIdle()

        assertEquals(1, repository.confirmCommandCount)
        assertEquals("operation-1", stateMachine.state.value.confirmationOperationId)
        assertEquals(EventManagementContract.ConfirmationPhase.CONFIRMED_PENDING_SYNC, stateMachine.state.value.confirmationPhase)
    }

    @Test
    fun `retry reuses its original operation id`() = runTest {
        val eventId = "event-stable-retry"
        repository.events[eventId] = eventFixture(eventId, "organizer-1", EventStatus.POLLING)
        repository.polls[eventId] = pollWithYesVote(eventId, "slot-1")
        repository.confirmFailuresRemaining = 1

        stateMachine.dispatch(EventManagementContract.Intent.OpenConfirmPrompt(eventId, "slot-1", "organizer-1"))
        stateMachine.dispatch(EventManagementContract.Intent.SubmitConfirmation("stable-operation"))
        machineScope.advanceUntilIdle()
        stateMachine.dispatch(EventManagementContract.Intent.RetryConfirmation)
        machineScope.advanceUntilIdle()

        assertEquals(listOf("stable-operation", "stable-operation"), repository.confirmOperationIds)
    }

    @Test
    fun `sync completion moves only the matching pending receipt to confirmed synced`() = runTest {
        val eventId = "event-sync-completed"
        repository.events[eventId] = eventFixture(eventId, "organizer-1", EventStatus.POLLING)
        repository.polls[eventId] = pollWithYesVote(eventId, "slot-1")

        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "slot-1", "organizer-1"))
        machineScope.advanceUntilIdle()
        val receiptId = stateMachine.state.value.confirmationReceiptId!!
        assertEquals(EventManagementContract.ConfirmationPhase.CONFIRMED_PENDING_SYNC, stateMachine.state.value.confirmationPhase)

        stateMachine.dispatch(EventManagementContract.Intent.SyncCompleted(receiptId))
        machineScope.advanceUntilIdle()

        assertEquals(EventManagementContract.ConfirmationPhase.CONFIRMED_SYNCED, stateMachine.state.value.confirmationPhase)
        assertEquals(EventManagementContract.DecisionSyncStatus.SERVER_ACKNOWLEDGED, stateMachine.state.value.confirmationDecisionSyncStatus)
    }

    @Test
    fun `rehydrate confirmed projection restores state without command or navigation`() = runTest {
        val sideEffects = mutableListOf<EventManagementContract.SideEffect>()
        val collector = machineScope.launch {
            stateMachine.sideEffect.collect { sideEffects += it }
        }
        machineScope.advanceUntilIdle()
        val projection = EventManagementContract.ConfirmationProjection.Confirmed(
            eventId = "event-rehydrate",
            slotId = "slot-1",
            receiptId = "receipt-rehydrate",
            decisionSyncStatus = EventManagementContract.DecisionSyncStatus.LOCAL_PENDING,
            effectDispatchStatus = EventManagementContract.EffectDispatchStatus.QUEUED
        )

        stateMachine.dispatch(EventManagementContract.Intent.RehydrateConfirmation(projection))
        machineScope.advanceUntilIdle()

        assertEquals(EventManagementContract.ConfirmationPhase.CONFIRMED_PENDING_SYNC, stateMachine.state.value.confirmationPhase)
        assertEquals("receipt-rehydrate", stateMachine.state.value.confirmationReceiptId)
        assertEquals(0, repository.confirmCommandCount)
        assertTrue(sideEffects.isEmpty(), "rehydration must not replay navigation or success feedback")
        collector.cancel()
    }

    @Test
    fun `rehydrate legacy projections are read only and emit no confirmation effects`() = runTest {
        val sideEffects = mutableListOf<EventManagementContract.SideEffect>()
        val collector = machineScope.launch {
            stateMachine.sideEffect.collect { sideEffects += it }
        }
        machineScope.advanceUntilIdle()

        stateMachine.dispatch(
            EventManagementContract.Intent.RehydrateConfirmation(
                EventManagementContract.ConfirmationProjection.LegacyApplied(
                    eventId = "event-legacy-applied",
                    slotId = "slot-legacy-applied",
                    receiptId = "legacy-confirmation:event-legacy-applied"
                )
            )
        )
        machineScope.advanceUntilIdle()

        assertEquals(EventManagementContract.ConfirmationPhase.LEGACY_APPLIED, stateMachine.state.value.confirmationPhase)
        assertEquals(null, stateMachine.state.value.confirmationDecisionSyncStatus)
        assertEquals(null, stateMachine.state.value.confirmationEffectDispatchStatus)
        stateMachine.dispatch(
            EventManagementContract.Intent.OpenConfirmPrompt(
                "event-legacy-applied",
                "slot-legacy-applied",
                "organizer-1"
            )
        )
        machineScope.advanceUntilIdle()
        assertEquals(EventManagementContract.ConfirmationPhase.LEGACY_APPLIED, stateMachine.state.value.confirmationPhase)

        stateMachine.dispatch(
            EventManagementContract.Intent.ConfirmDate(
                "event-legacy-applied",
                "slot-legacy-applied",
                "organizer-1"
            )
        )
        machineScope.advanceUntilIdle()
        assertEquals(EventManagementContract.ConfirmationPhase.LEGACY_APPLIED, stateMachine.state.value.confirmationPhase)

        stateMachine.dispatch(
            EventManagementContract.Intent.RehydrateConfirmation(
                EventManagementContract.ConfirmationProjection.Quarantined(
                    eventId = "event-legacy-quarantined",
                    reason = "missing-or-invalid-confirmed-date"
                )
            )
        )
        machineScope.advanceUntilIdle()

        assertEquals(EventManagementContract.ConfirmationPhase.QUARANTINED, stateMachine.state.value.confirmationPhase)
        assertEquals("missing-or-invalid-confirmed-date", stateMachine.state.value.confirmationDiagnosticReason)
        assertEquals(null, stateMachine.state.value.confirmationDecisionSyncStatus)
        assertEquals(null, stateMachine.state.value.confirmationEffectDispatchStatus)
        stateMachine.dispatch(
            EventManagementContract.Intent.OpenConfirmPrompt(
                "event-legacy-quarantined",
                "slot-1",
                "organizer-1"
            )
        )
        machineScope.advanceUntilIdle()

        assertEquals(EventManagementContract.ConfirmationPhase.QUARANTINED, stateMachine.state.value.confirmationPhase)
        stateMachine.dispatch(
            EventManagementContract.Intent.ConfirmDate(
                "event-legacy-quarantined",
                "slot-1",
                "organizer-1"
            )
        )
        machineScope.advanceUntilIdle()
        assertEquals(EventManagementContract.ConfirmationPhase.QUARANTINED, stateMachine.state.value.confirmationPhase)
        assertEquals(0, repository.confirmCommandCount)
        assertTrue(sideEffects.isEmpty(), "legacy rehydration must not emit navigation or success feedback")
        collector.cancel()
    }

    @Test
    fun `read only replay result updates history state without toast or navigation`() = runTest {
        val sideEffects = mutableListOf<EventManagementContract.SideEffect>()
        val collector = machineScope.launch {
            stateMachine.sideEffect.collect { sideEffects += it }
        }
        machineScope.advanceUntilIdle()
        val eventId = "event-read-only-replay"
        repository.events[eventId] = eventFixture(eventId, "organizer-1", EventStatus.POLLING)
        repository.polls[eventId] = pollWithYesVote(eventId, "slot-1")
        repository.readOnlyReplayProjection = EventManagementContract.ConfirmationProjection.LegacyApplied(
            eventId = eventId,
            slotId = "slot-1",
            receiptId = "legacy-confirmation:$eventId"
        )

        stateMachine.dispatch(EventManagementContract.Intent.OpenConfirmPrompt(eventId, "slot-1", "organizer-1"))
        machineScope.advanceUntilIdle()
        stateMachine.dispatch(EventManagementContract.Intent.SubmitConfirmation("operation-read-only-replay"))
        machineScope.advanceUntilIdle()

        assertEquals(EventManagementContract.ConfirmationPhase.LEGACY_APPLIED, stateMachine.state.value.confirmationPhase)
        assertEquals(null, stateMachine.state.value.confirmationDecisionSyncStatus)
        assertEquals(null, stateMachine.state.value.confirmationEffectDispatchStatus)
        assertEquals(0, repository.confirmCommandCount)
        assertTrue(sideEffects.isEmpty(), "read-only recovery must not emit toast or navigation")
        collector.cancel()
    }

    @Test
    fun `retry reuses the failed operation id and exposes typed failure`() = runTest {
        val eventId = "event-retry"
        repository.events[eventId] = eventFixture(eventId, "organizer-1", EventStatus.POLLING)
        repository.polls[eventId] = pollWithYesVote(eventId, "slot-1")
        repository.confirmFailuresRemaining = 1

        stateMachine.dispatch(EventManagementContract.Intent.OpenConfirmPrompt(eventId, "slot-1", "organizer-1"))
        machineScope.advanceUntilIdle()
        stateMachine.dispatch(EventManagementContract.Intent.SubmitConfirmation("operation-retry"))
        machineScope.advanceUntilIdle()

        assertEquals(EventManagementContract.ConfirmationPhase.FAILED, stateMachine.state.value.confirmationPhase)
        assertEquals(
            EventManagementContract.ConfirmationFailureCode.LOCAL_PERSISTENCE_FAILED,
            stateMachine.state.value.confirmationFailure?.code
        )

        stateMachine.dispatch(EventManagementContract.Intent.RetryConfirmation)
        machineScope.advanceUntilIdle()

        assertEquals(2, repository.confirmCommandCount)
        assertEquals("operation-retry", stateMachine.state.value.confirmationOperationId)
        assertEquals(EventManagementContract.ConfirmationPhase.CONFIRMED_PENDING_SYNC, stateMachine.state.value.confirmationPhase)
    }

    @Test
    fun `confirmation queues one domain envelope rather than provider artifacts`() = runTest {
        val eventId = "event-confirmation-artifacts"
        val organizerId = "organizer-1"
        val slotId = "slot-1"
        repository.events[eventId] = eventFixture(
            id = eventId,
            organizerId = organizerId,
            status = EventStatus.POLLING
        )
        repository.polls[eventId] = pollWithYesVote(eventId, slotId)

        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, slotId, organizerId))
        machineScope.advanceUntilIdle()

        assertEquals(
            0,
            repository.getWorkflowOutbox(eventId).size,
            "The legacy workflow outbox must not stand in for the dedicated confirmation envelope"
        )
    }

    @Test
    fun `non organizer failure is exposed as stable typed code`() = runTest {
        val eventId = "event-not-organizer"
        repository.events[eventId] = eventFixture(eventId, "organizer-1", EventStatus.POLLING)
        repository.polls[eventId] = pollWithYesVote(eventId, "slot-1")

        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "slot-1", "participant-1"))
        machineScope.advanceUntilIdle()

        assertEquals(ObservedConfirmationFailure.Typed(ConfirmationFailureCode.NOT_ORGANIZER), observedFailure())
        assertEquals(0, repository.confirmCommandCount)
    }

    @Test
    fun `invalid status failure is exposed as stable typed code`() = runTest {
        val eventId = "event-invalid-status"
        repository.events[eventId] = eventFixture(eventId, "organizer-1", EventStatus.DRAFT)
        repository.polls[eventId] = pollWithYesVote(eventId, "slot-1")

        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "slot-1", "organizer-1"))
        machineScope.advanceUntilIdle()

        assertEquals(ObservedConfirmationFailure.Typed(ConfirmationFailureCode.INVALID_EVENT_STATUS), observedFailure())
        assertEquals(0, repository.confirmCommandCount)
    }

    @Test
    fun `no votes failure is exposed as stable typed code`() = runTest {
        val eventId = "event-no-votes"
        repository.events[eventId] = eventFixture(eventId, "organizer-1", EventStatus.POLLING)
        repository.polls[eventId] = Poll("poll-$eventId", eventId, emptyMap())

        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "slot-1", "organizer-1"))
        machineScope.advanceUntilIdle()

        assertEquals(ObservedConfirmationFailure.Typed(ConfirmationFailureCode.NO_VOTES), observedFailure())
        assertEquals(0, repository.confirmCommandCount)
    }

    @Test
    fun `invalid slot failure is exposed as stable typed code`() = runTest {
        val eventId = "event-invalid-slot"
        repository.events[eventId] = eventFixture(eventId, "organizer-1", EventStatus.POLLING)
        repository.polls[eventId] = pollWithYesVote(eventId, "slot-1")

        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "missing", "organizer-1"))
        machineScope.advanceUntilIdle()

        assertEquals(ObservedConfirmationFailure.Typed(ConfirmationFailureCode.SLOT_NOT_FOUND), observedFailure())
        assertEquals(0, repository.confirmCommandCount)
    }

    @Test
    fun `duplicate dispatch has only one effective command in flight`() = runTest {
        val eventId = "event-duplicate-dispatch"
        repository.events[eventId] = eventFixture(eventId, "organizer-1", EventStatus.POLLING)
        repository.polls[eventId] = pollWithYesVote(eventId, "slot-1")

        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "slot-1", "organizer-1"))
        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "slot-1", "organizer-1"))
        machineScope.advanceUntilIdle()

        assertEquals(1, repository.confirmCommandCount)
        assertEquals(0, repository.getWorkflowOutbox(eventId).size)
    }

    @Test
    fun `draft to polling to confirmed sets final date unlocks scenarios navigates and queues confirmation work`() = runTest {
        val eventId = "event-happy-path"
        val organizerId = "organizer-1"
        val slotId = "slot-1"
        repository.events[eventId] = eventFixture(
            id = eventId,
            organizerId = organizerId,
            status = EventStatus.DRAFT
        )
        repository.polls[eventId] = pollWithYesVote(eventId, slotId)

        stateMachine.dispatch(EventManagementContract.Intent.StartPoll(eventId, organizerId))
        machineScope.advanceUntilIdle()
        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, slotId, organizerId))
        machineScope.advanceUntilIdle()
        val effects = collectSideEffects(count = 3)

        val confirmedEvent = repository.events.getValue(eventId)
        assertEquals(EventStatus.CONFIRMED, confirmedEvent.status)
        assertEquals("2026-06-10T09:00:00Z", confirmedEvent.finalDate)
        assertTrue(stateMachine.state.value.scenariosUnlocked)
        assertTrue(
            effects.any {
                it is EventManagementContract.SideEffect.NavigateTo &&
                    it.route == "event/$eventId/scenarios"
            }
        )
        assertEquals(
            0,
            repository.getWorkflowOutbox(eventId).size,
            "The state machine must not create a second, generic outbox envelope"
        )
    }

    private fun observedFailure(): ObservedConfirmationFailure =
        stateMachine.state.value.confirmationFailure?.code?.let {
            ObservedConfirmationFailure.Typed(ConfirmationFailureCode.valueOf(it.name))
        } ?: stateMachine.state.value.error?.let(ObservedConfirmationFailure::LegacyUntyped)
            ?: ObservedConfirmationFailure.None

    private fun eventFixture(
        id: String,
        organizerId: String,
        status: EventStatus,
        deadline: String = "2026-06-01T18:00:00Z"
    ): Event = Event(
        id = id,
        title = "Planning dinner",
        description = "Choose a date and continue organization",
        organizerId = organizerId,
        participants = listOf(organizerId, "participant-1"),
        proposedSlots = listOf(
            TimeSlot(
                id = "slot-1",
                start = "2026-06-10T09:00:00Z",
                end = "2026-06-10T11:00:00Z",
                timezone = "Europe/Paris",
                timeOfDay = TimeOfDay.SPECIFIC
            )
        ),
        deadline = deadline,
        status = status,
        createdAt = "2026-05-20T10:00:00Z",
        updatedAt = "2026-05-20T10:00:00Z",
        eventType = EventType.OTHER
    )

    private fun pollWithYesVote(eventId: String, slotId: String): Poll =
        Poll(
            id = "poll-$eventId",
            eventId = eventId,
            votes = mapOf("participant-1" to mapOf(slotId to Vote.YES))
        )

    private suspend fun assertConfirmationAllowedAtDeadlineBoundary(deadlinePassed: Boolean, eventId: String) {
        repository.events[eventId] = eventFixture(eventId, "organizer-1", EventStatus.POLLING)
        repository.polls[eventId] = pollWithYesVote(eventId, "slot-1")
        repository.deadlinePassed = deadlinePassed

        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "slot-1", "organizer-1"))
        machineScope.advanceUntilIdle()

        assertEquals(EventStatus.CONFIRMED, repository.events.getValue(eventId).status)
        assertEquals(1, repository.confirmCommandCount)
    }

    private suspend fun collectSideEffects(count: Int): List<EventManagementContract.SideEffect> =
        withTimeout(1_000) {
            buildList {
                repeat(count) {
                    add(stateMachine.sideEffect.first())
                }
            }
        }
}

private enum class ConfirmationFailureCode {
    NOT_ORGANIZER,
    INVALID_EVENT_STATUS,
    NO_VOTES,
    SLOT_NOT_FOUND
}

private sealed interface ObservedConfirmationFailure {
    data class Typed(val code: ConfirmationFailureCode) : ObservedConfirmationFailure
    data class LegacyUntyped(val message: String) : ObservedConfirmationFailure
    data object None : ObservedConfirmationFailure
}

private class PollingConfirmationRepository : EventRepositoryInterface {
    val events = mutableMapOf<String, Event>()
    val polls = mutableMapOf<String, Poll>()
    var deadlinePassed = false
    var confirmCommandCount = 0
    val confirmOperationIds = mutableListOf<String>()
    var confirmFailuresRemaining = 0
    var readOnlyReplayProjection: EventManagementContract.ConfirmationProjection.ReadOnly? = null
    private val workflowOutbox = mutableListOf<WorkflowOutboxRecord>()
    private val confirmationReceipts = mutableMapOf<String, EventManagementContract.ConfirmationReceipt>()
    private val confirmationSyncStatuses = mutableMapOf<String, EventManagementContract.DecisionSyncStatus>()

    override suspend fun createEvent(event: Event): Result<Event> {
        events[event.id] = event
        polls[event.id] = Poll(id = "poll-${event.id}", eventId = event.id, votes = emptyMap())
        return Result.success(event)
    }

    override fun getEvent(id: String): Event? = events[id]

    override fun getPoll(eventId: String): Poll? = polls[eventId]

    override suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean> {
        val event = events[eventId] ?: return Result.failure(IllegalArgumentException("Event not found"))
        events[eventId] = event.copy(participants = event.participants + participantId)
        return Result.success(true)
    }

    override fun getParticipants(eventId: String): List<String>? = events[eventId]?.participants

    override suspend fun addVote(
        eventId: String,
        participantId: String,
        slotId: String,
        vote: Vote
    ): Result<Boolean> {
        if (deadlinePassed) return Result.failure(IllegalStateException("Voting deadline has passed"))
        val poll = polls[eventId] ?: return Result.failure(IllegalArgumentException("Poll not found"))
        val participantVotes = poll.votes[participantId].orEmpty() + (slotId to vote)
        polls[eventId] = poll.copy(votes = poll.votes + (participantId to participantVotes))
        return Result.success(true)
    }

    override suspend fun updateEvent(event: Event): Result<Event> {
        events[event.id] = event
        return Result.success(event)
    }

    override suspend fun updateEventStatus(
        id: String,
        status: EventStatus,
        finalDate: String?
    ): Result<Boolean> {
        val event = events[id] ?: return Result.failure(IllegalArgumentException("Event not found"))
        events[id] = event.copy(status = status, finalDate = finalDate)
        return Result.success(true)
    }

    override suspend fun confirmEventDate(
        eventId: String,
        slotId: String,
        confirmedByOrganizerId: String
    ): Result<Boolean> {
        confirmCommandCount += 1
        if (confirmFailuresRemaining > 0) {
            confirmFailuresRemaining -= 1
            return Result.failure(IllegalStateException("Injected confirmation failure"))
        }
        val event = events[eventId] ?: return Result.failure(IllegalArgumentException("Event not found"))
        if (event.organizerId != confirmedByOrganizerId) {
            return Result.failure(IllegalStateException("Only event organizer can confirm dates"))
        }
        val selectedSlot = event.proposedSlots.find { it.id == slotId }
            ?: return Result.failure(IllegalArgumentException("Selected time slot not found"))
        val finalDate = selectedSlot.start
            ?: return Result.failure(IllegalStateException("Selected time slot has no confirmed start date"))
        events[eventId] = event.copy(status = EventStatus.CONFIRMED, finalDate = finalDate)
        return Result.success(true)
    }

    override suspend fun confirmPollDate(
        command: EventManagementContract.ConfirmPollDateCommand
    ): EventManagementContract.ConfirmationResult {
        confirmOperationIds += command.operationId
        readOnlyReplayProjection?.let { projection ->
            return EventManagementContract.ConfirmationResult.ReadOnly(command.operationId, projection)
        }
        confirmationReceipts[command.operationId]?.let { receipt ->
            return EventManagementContract.ConfirmationResult.AlreadyCommitted(
                receipt = receipt,
                projection = confirmationProjection(receipt)
            )
        }
        confirmationReceipts.values.firstOrNull { it.eventId == command.eventId }?.let { receipt ->
            return if (receipt.slotId == command.slotId) {
                EventManagementContract.ConfirmationResult.AlreadyCommitted(
                    receipt = receipt,
                    projection = confirmationProjection(receipt)
                )
            } else {
                EventManagementContract.ConfirmationResult.Conflict(
                    operationId = command.operationId,
                    failure = EventManagementContract.ConfirmationFailure(
                        EventManagementContract.ConfirmationFailureCode.ALREADY_CONFIRMED_DIFFERENT_SLOT,
                        retryable = false
                    )
                )
            }
        }
        val legacyResult = confirmEventDate(
            eventId = command.eventId,
            slotId = command.slotId,
            confirmedByOrganizerId = command.actorId
        )
        return legacyResult.fold(
            onSuccess = {
                val effectKeys = confirmationEffectKeys(command.eventId, command.slotId)
                val receipt = EventManagementContract.ConfirmationReceipt(
                    receiptId = command.operationId,
                    operationId = command.operationId,
                    eventId = command.eventId,
                    slotId = command.slotId,
                    actorId = command.actorId,
                    committedAt = command.requestedAt.toString(),
                    nextNavigationTarget = "event/${command.eventId}/scenarios",
                    decisionSyncStatus = EventManagementContract.DecisionSyncStatus.LOCAL_PENDING,
                    effectDispatchStatus = EventManagementContract.EffectDispatchStatus.QUEUED,
                    effectOutbox = EventManagementContract.ConfirmationEffectOutbox(
                        effectKeys.domainEventId,
                        effectKeys.effectKey
                    )
                )
                confirmationReceipts[command.operationId] = receipt
                confirmationSyncStatuses[receipt.receiptId] = receipt.decisionSyncStatus
                EventManagementContract.ConfirmationResult.Committed(
                    receipt = receipt,
                    projection = confirmationProjection(receipt)
                )
            },
            onFailure = {
                EventManagementContract.ConfirmationResult.Failed(
                    operationId = command.operationId,
                    failure = EventManagementContract.ConfirmationFailure(
                        EventManagementContract.ConfirmationFailureCode.LOCAL_PERSISTENCE_FAILED,
                        retryable = true
                    )
                )
            }
        )
    }

    override suspend fun markConfirmationSynced(
        receiptId: String
    ): EventManagementContract.ConfirmationProjection? {
        val receipt = confirmationReceipts[receiptId] ?: return null
        confirmationSyncStatuses[receiptId] = EventManagementContract.DecisionSyncStatus.SERVER_ACKNOWLEDGED
        return confirmationProjection(receipt)
    }

    private fun confirmationProjection(
        receipt: EventManagementContract.ConfirmationReceipt
    ): EventManagementContract.ConfirmationProjection.Confirmed =
        EventManagementContract.ConfirmationProjection.Confirmed(
            eventId = receipt.eventId,
            slotId = receipt.slotId,
            receiptId = receipt.receiptId,
            decisionSyncStatus = confirmationSyncStatuses[receipt.receiptId]
                ?: receipt.decisionSyncStatus,
            effectDispatchStatus = receipt.effectDispatchStatus
        )

    override suspend fun saveEvent(event: Event): Result<Event> {
        events[event.id] = event
        return Result.success(event)
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        events.remove(eventId)
        polls.remove(eventId)
        workflowOutbox.removeAll { it.eventId == eventId }
        return Result.success(Unit)
    }

    override fun isDeadlinePassed(deadline: String): Boolean = deadlinePassed

    override fun isOrganizer(eventId: String, userId: String): Boolean =
        events[eventId]?.organizerId == userId

    override fun canModifyEvent(eventId: String, userId: String): Boolean =
        isOrganizer(eventId, userId)

    override fun getAllEvents(): List<Event> = events.values.toList()

    override fun getEventsPaginated(page: Int, pageSize: Int, orderBy: OrderBy): Flow<List<Event>> =
        flowOf(events.values.toList().drop(page * pageSize).take(pageSize))

    override suspend fun queueWorkflowOutbox(record: WorkflowOutboxRecord): Result<Boolean> {
        workflowOutbox += record
        return Result.success(true)
    }

    override fun getWorkflowOutbox(eventId: String): List<WorkflowOutboxRecord> =
        workflowOutbox.filter { it.eventId == eventId }
}
