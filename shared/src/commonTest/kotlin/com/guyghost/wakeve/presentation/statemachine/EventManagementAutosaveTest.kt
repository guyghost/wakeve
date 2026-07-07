package com.guyghost.wakeve.presentation.statemachine

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import com.guyghost.wakeve.repository.EventRepositoryInterface
import com.guyghost.wakeve.repository.OrderBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class EventManagementAutosaveTest {

    @Test
    fun updateEventAutosaveCreatesMissingDraftInsteadOfFailing() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = AutosaveRepository()
        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(repository),
            createEventUseCase = CreateEventUseCase(repository),
            eventRepository = repository,
            scope = CoroutineScope(dispatcher + SupervisorJob())
        )

        val draft = testEvent("event-stable")

        stateMachine.dispatch(EventManagementContract.Intent.UpdateEvent(draft))
        advanceUntilIdle()

        assertNotNull(repository.getEvent("event-stable"))
        assertEquals(listOf("event-stable"), stateMachine.state.value.events.map { it.id })
        assertNull(stateMachine.state.value.error)
    }

    @Test
    fun createEventDoesNotLeaveBufferedNavigateBackForNextScreen() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = AutosaveRepository()
        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(repository),
            createEventUseCase = CreateEventUseCase(repository),
            eventRepository = repository,
            scope = CoroutineScope(dispatcher + SupervisorJob())
        )
        val effects = mutableListOf<EventManagementContract.SideEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            stateMachine.sideEffect.toList(effects)
        }

        stateMachine.dispatch(EventManagementContract.Intent.CreateEvent(testEvent("event-create")))
        advanceUntilIdle()

        assertEquals(
            emptyList(),
            effects.filterIsInstance<EventManagementContract.SideEffect.NavigateBack>()
        )
    }

    private fun testEvent(id: String): Event = Event(
        id = id,
        title = "Team retreat",
        description = "Planification",
        organizerId = "user-1",
        participants = emptyList(),
        proposedSlots = listOf(
            TimeSlot(
                id = "slot-1",
                start = "2026-06-01T10:00:00Z",
                end = "2026-06-01T12:00:00Z",
                timezone = "UTC"
            )
        ),
        deadline = "2026-05-29T10:00:00Z",
        status = EventStatus.DRAFT,
        createdAt = "2026-05-22T10:00:00Z",
        updatedAt = "2026-05-22T10:00:00Z"
    )

    private class AutosaveRepository : EventRepositoryInterface {
        private val events = mutableMapOf<String, Event>()

        override suspend fun createEvent(event: Event): Result<Event> {
            events[event.id] = event
            return Result.success(event)
        }

        override fun getEvent(id: String): Event? = events[id]
        override fun getPoll(eventId: String): Poll? = null
        override suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean> = Result.success(true)
        override fun getParticipants(eventId: String): List<String>? = emptyList()
        override suspend fun addVote(eventId: String, participantId: String, slotId: String, vote: Vote): Result<Boolean> = Result.success(true)

        override suspend fun updateEvent(event: Event): Result<Event> {
            return if (events.containsKey(event.id)) {
                events[event.id] = event
                Result.success(event)
            } else {
                Result.failure(IllegalArgumentException("Event not found"))
            }
        }

        override suspend fun updateEventStatus(id: String, status: EventStatus, finalDate: String?): Result<Boolean> {
            val existing = events[id] ?: return Result.failure(IllegalArgumentException("Event not found"))
            events[id] = existing.copy(status = status, finalDate = finalDate)
            return Result.success(true)
        }

        override suspend fun saveEvent(event: Event): Result<Event> {
            events[event.id] = event
            return Result.success(event)
        }

        override fun isDeadlinePassed(deadline: String): Boolean = false
        override fun isOrganizer(eventId: String, userId: String): Boolean = true
        override fun canModifyEvent(eventId: String, userId: String): Boolean = true
        override fun getAllEvents(): List<Event> = events.values.toList()
        override fun getEventsPaginated(page: Int, pageSize: Int, orderBy: OrderBy): Flow<List<Event>> = flowOf(events.values.toList())
        override suspend fun deleteEvent(eventId: String): Result<Unit> {
            events.remove(eventId)
            return Result.success(Unit)
        }
    }
}
