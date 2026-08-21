package com.guyghost.wakeve.presentation.statemachine

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HistoricalDeleteReadOnlyRedTest {

    @Test
    fun `stale DeleteEvent is rejected when structured event bounds are past`() = runTest {
        val repository = MockEventRepository()
        val historicalEvent = Event(
            id = "past-organizing-event",
            title = "Past event",
            description = "Must remain available through Archive",
            organizerId = "organizer-1",
            status = EventStatus.ORGANIZING,
            eventType = EventType.OTHER,
            proposedSlots = listOf(
                TimeSlot(
                    id = "past-slot",
                    start = "2020-06-01T10:00:00Z",
                    end = "2020-06-01T12:00:00Z",
                    timezone = "UTC",
                    timeOfDay = TimeOfDay.SPECIFIC
                )
            ),
            participants = emptyList(),
            deadline = "2020-05-01T00:00:00Z",
            finalDate = "2020-06-01T10:00:00Z",
            createdAt = "2020-01-01T00:00:00Z",
            updatedAt = "2020-01-01T00:00:00Z"
        )
        repository.events[historicalEvent.id] = historicalEvent

        val dispatcher = StandardTestDispatcher(testScheduler)
        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(repository),
            createEventUseCase = CreateEventUseCase(repository),
            eventRepository = repository,
            scope = CoroutineScope(dispatcher + SupervisorJob())
        )

        stateMachine.dispatch(
            EventManagementContract.Intent.DeleteEvent(
                eventId = historicalEvent.id,
                userId = historicalEvent.organizerId
            )
        )
        advanceUntilIdle()

        assertNotNull(
            repository.getEvent(historicalEvent.id),
            "A PAST event must survive a stale user-facing DeleteEvent and remain available to Archive."
        )
        assertTrue(
            stateMachine.state.value.error != null,
            "The owner must return a typed/read-only rejection instead of reporting deletion success."
        )
    }
}
