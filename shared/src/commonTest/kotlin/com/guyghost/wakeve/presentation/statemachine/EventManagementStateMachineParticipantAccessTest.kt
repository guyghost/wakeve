package com.guyghost.wakeve.presentation.statemachine

import com.guyghost.wakeve.access.DateValidationState
import com.guyghost.wakeve.access.ParticipantAccessMapper
import com.guyghost.wakeve.access.ParticipantAccessState
import com.guyghost.wakeve.access.ParticipantRepositoryRecord
import com.guyghost.wakeve.access.ParticipantRsvp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import com.guyghost.wakeve.repository.EventRepositoryInterface
import com.guyghost.wakeve.repository.OrderBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

interface ParticipantAccessRecordRepository {
    fun getParticipantRecords(eventId: String): List<ParticipantRepositoryRecord>?
}

class EventManagementStateMachineParticipantAccessTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `LoadParticipants populates participant access states from repository records`() = runTest {
        val eventId = "event-access-1"
        val confirmedRecord = ParticipantRepositoryRecord(
            id = "participant-record-confirmed",
            eventId = eventId,
            userId = "confirmed-user",
            role = "MEMBER",
            rsvp = "ACCEPTED",
            hasValidatedDate = 1L
        )
        val pendingRecord = ParticipantRepositoryRecord(
            id = "participant-record-pending",
            eventId = eventId,
            userId = "pending-user",
            role = "MEMBER",
            rsvp = "PENDING",
            hasValidatedDate = 0L
        )
        val repository = ParticipantAccessRepositoryFake(
            participantRecordsByEventId = mutableMapOf(
                eventId to listOf(confirmedRecord, pendingRecord)
            )
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(repository),
            createEventUseCase = CreateEventUseCase(repository),
            eventRepository = repository,
            scope = CoroutineScope(dispatcher + SupervisorJob())
        )

        stateMachine.dispatch(EventManagementContract.Intent.LoadParticipants(eventId))
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals(listOf("confirmed-user", "pending-user"), state.participantIds)
        assertEquals(
            listOf(
                ParticipantAccessMapper.fromRepositoryRecord(confirmedRecord),
                ParticipantAccessMapper.fromRepositoryRecord(pendingRecord)
            ),
            state.participantAccessStates
        )
        assertEquals(
            ParticipantAccessState.member(
                userId = "confirmed-user",
                rsvp = ParticipantRsvp.ACCEPTED,
                dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
            ),
            state.participantAccessStates.first()
        )
        assertEquals(
            ParticipantAccessState.invitedPending("pending-user"),
            state.participantAccessStates.last()
        )
    }
}

private class ParticipantAccessRepositoryFake(
    private val participantRecordsByEventId: MutableMap<String, List<ParticipantRepositoryRecord>>
) : EventRepositoryInterface, ParticipantAccessRecordRepository {
    private val events = mutableMapOf<String, Event>()
    private val polls = mutableMapOf<String, Poll>()

    override fun getParticipantRecords(eventId: String): List<ParticipantRepositoryRecord>? =
        participantRecordsByEventId[eventId]

    override suspend fun createEvent(event: Event): Result<Event> {
        events[event.id] = event
        polls[event.id] = Poll(event.id, event.id, emptyMap())
        return Result.success(event)
    }

    override fun getEvent(id: String): Event? = events[id]

    override fun getPoll(eventId: String): Poll? = polls[eventId]

    override suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean> {
        val currentRecords = participantRecordsByEventId[eventId].orEmpty()
        participantRecordsByEventId[eventId] = currentRecords + ParticipantRepositoryRecord(
            id = "participant-record-$participantId",
            eventId = eventId,
            userId = participantId,
            role = "MEMBER",
            rsvp = "PENDING",
            hasValidatedDate = 0L
        )
        return Result.success(true)
    }

    override fun getParticipants(eventId: String): List<String>? =
        participantRecordsByEventId[eventId]?.map { it.userId }

    override suspend fun addVote(
        eventId: String,
        participantId: String,
        slotId: String,
        vote: Vote
    ): Result<Boolean> = Result.success(true)

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

    override suspend fun saveEvent(event: Event): Result<Event> {
        events[event.id] = event
        return Result.success(event)
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        events.remove(eventId)
        polls.remove(eventId)
        participantRecordsByEventId.remove(eventId)
        return Result.success(Unit)
    }

    override fun isDeadlinePassed(deadline: String): Boolean = false

    override fun isOrganizer(eventId: String, userId: String): Boolean =
        events[eventId]?.organizerId == userId

    override fun canModifyEvent(eventId: String, userId: String): Boolean =
        isOrganizer(eventId, userId)

    override fun getAllEvents(): List<Event> = events.values.toList()

    override fun getEventsPaginated(
        page: Int,
        pageSize: Int,
        orderBy: OrderBy
    ): Flow<List<Event>> = flowOf(events.values.toList().drop(page * pageSize).take(pageSize))
}
