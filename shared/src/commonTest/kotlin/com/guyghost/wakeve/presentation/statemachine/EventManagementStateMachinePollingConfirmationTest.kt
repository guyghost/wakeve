package com.guyghost.wakeve.presentation.statemachine

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
import com.guyghost.wakeve.workflow.WorkflowOutboxType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    fun `confirm date refuses polling event when poll deadline has passed even with votes`() = runTest {
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
        assertEquals(EventStatus.POLLING, event.status)
        assertNull(event.finalDate)
        assertTrue(
            stateMachine.state.value.error?.contains("deadline", ignoreCase = true) == true,
            "ConfirmDate must reject confirmation after the poll deadline has passed"
        )
        assertTrue(repository.getWorkflowOutbox(eventId).isEmpty())
    }

    @Test
    fun `confirm date records local-first notification and calendar workflow artifacts`() = runTest {
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

        val outbox = repository.getWorkflowOutbox(eventId)
        assertTrue(
            outbox.any { it.type == WorkflowOutboxType.DATE_CONFIRMATION_NOTIFICATION },
            "ConfirmDate must queue a local date-confirmation notification artifact"
        )
        assertTrue(
            outbox.any { it.type == WorkflowOutboxType.CALENDAR_INVITATION_ARTIFACT },
            "ConfirmDate must queue a local calendar invitation artifact"
        )
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
        assertNotNull(
            repository.getWorkflowOutbox(eventId).firstOrNull {
                it.type == WorkflowOutboxType.DATE_CONFIRMATION_NOTIFICATION
            },
            "Happy path must prove the date-confirmation notification is queued locally"
        )
        assertNotNull(
            repository.getWorkflowOutbox(eventId).firstOrNull {
                it.type == WorkflowOutboxType.CALENDAR_INVITATION_ARTIFACT
            },
            "Happy path must prove the calendar artifact is queued locally"
        )
    }

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

    private suspend fun collectSideEffects(count: Int): List<EventManagementContract.SideEffect> =
        withTimeout(1_000) {
            buildList {
                repeat(count) {
                    add(stateMachine.sideEffect.first())
                }
            }
        }
}

private class PollingConfirmationRepository : EventRepositoryInterface {
    val events = mutableMapOf<String, Event>()
    val polls = mutableMapOf<String, Poll>()
    var deadlinePassed = false
    private val workflowOutbox = mutableListOf<WorkflowOutboxRecord>()

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
