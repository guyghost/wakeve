package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachine
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import com.guyghost.wakeve.repository.EventRepository
import com.guyghost.wakeve.repository.EventRepositoryInterface
import com.guyghost.wakeve.viewmodel.EventManagementViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Drives the Android lifecycle controller against the real shared EventManagementStateMachine,
 * the same way EventDetailScreen does: one side-effect collector feeding the controller.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventLifecycleTransitionIntegrationTest {

    private val eventId = "event-lifecycle"
    private val organizerId = "organizer-1"

    private class Harness(
        val viewModel: EventManagementViewModel,
        val controller: EventLifecycleTransitionController,
        val outcomes: MutableList<EventLifecycleOutcome>,
        val forwarded: MutableList<EventManagementContract.SideEffect>,
    )

    private fun TestScope.harness(repository: EventRepositoryInterface): Harness {
        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(repository),
            createEventUseCase = CreateEventUseCase(repository),
            eventRepository = repository,
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )
        val viewModel = EventManagementViewModel(stateMachine)
        val controller = EventLifecycleTransitionController(
            eventId = eventId,
            userId = organizerId,
            dispatch = viewModel::dispatch,
            currentState = { viewModel.state.value },
        )
        val outcomes = mutableListOf<EventLifecycleOutcome>()
        val forwarded = mutableListOf<EventManagementContract.SideEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.sideEffect.collect { effect ->
                when (val result = controller.onSideEffect(effect)) {
                    is EventLifecycleSideEffectResult.Settled -> outcomes += result.outcome
                    EventLifecycleSideEffectResult.Consumed -> Unit
                    EventLifecycleSideEffectResult.NotHandled -> forwarded += effect
                }
            }
        }
        viewModel.dispatch(EventManagementContract.Intent.LoadEvents)
        viewModel.dispatch(EventManagementContract.Intent.SelectEvent(eventId))
        return Harness(viewModel, controller, outcomes, forwarded)
    }

    @Test
    fun `organizer moves a confirmed event to organizing then finalized`() = runTest {
        val repository = EventRepository().apply { createEvent(event(EventStatus.CONFIRMED)) }
        val h = harness(repository)
        advanceUntilIdle()
        h.forwarded.clear()

        h.controller.transition(EventLifecycleTarget.ORGANIZING)
        advanceUntilIdle()

        assertEquals(listOf<EventLifecycleOutcome>(EventLifecycleOutcome.Transitioned(EventLifecycleTarget.ORGANIZING)), h.outcomes)
        assertEquals(EventStatus.ORGANIZING, repository.getEvent(eventId)?.status)
        // The detail refreshed its selected event and did not navigate away.
        assertEquals(EventStatus.ORGANIZING, h.viewModel.state.value.selectedEvent?.status)
        assertTrue(h.forwarded.isEmpty(), "unexpected forwarded effects: ${h.forwarded}")

        h.controller.transition(EventLifecycleTarget.FINALIZED)
        advanceUntilIdle()

        assertEquals(EventLifecycleOutcome.Transitioned(EventLifecycleTarget.FINALIZED), h.outcomes.last())
        assertEquals(EventStatus.FINALIZED, h.viewModel.state.value.selectedEvent?.status)
        assertTrue(h.forwarded.isEmpty(), "unexpected forwarded effects: ${h.forwarded}")
    }

    @Test
    fun `blocked finalization reports the blockers and settles again on retry`() = runTest {
        val inner = EventRepository().apply { createEvent(event(EventStatus.ORGANIZING)) }
        val repository = object : EventRepositoryInterface by inner {
            override suspend fun updateEventStatus(id: String, status: EventStatus, finalDate: String?): Result<Boolean> =
                Result.failure(IllegalStateException("Finalization blocked by MEETING_REQUIRED,BUDGET_REQUIRED"))
        }
        val h = harness(repository)
        advanceUntilIdle()

        repeat(2) {
            h.controller.transition(EventLifecycleTarget.FINALIZED)
            advanceUntilIdle()
        }

        val expected = EventLifecycleOutcome.Failed("Finalization blocked by MEETING_REQUIRED,BUDGET_REQUIRED")
        assertEquals(listOf<EventLifecycleOutcome>(expected, expected), h.outcomes)
        assertEquals(EventStatus.ORGANIZING, inner.getEvent(eventId)?.status)
    }

    private fun event(status: EventStatus) = Event(
        id = eventId,
        title = "Week-end",
        description = "Test",
        organizerId = organizerId,
        participants = listOf(organizerId),
        proposedSlots = emptyList(),
        deadline = "2026-10-01T00:00:00Z",
        status = status,
        createdAt = "2026-09-01T00:00:00Z",
        updatedAt = "2026-09-01T00:00:00Z",
    )
}
