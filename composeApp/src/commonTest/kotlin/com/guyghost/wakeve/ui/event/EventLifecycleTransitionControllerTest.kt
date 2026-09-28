package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.state.EventManagementContract.Intent
import com.guyghost.wakeve.presentation.state.EventManagementContract.SideEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventLifecycleTransitionControllerTest {

    private val eventId = "event-1"
    private val organizerId = "organizer-1"

    private val dispatched = mutableListOf<Intent>()
    private var state = EventManagementContract.State(events = listOf(event(EventStatus.CONFIRMED)))

    private val controller = EventLifecycleTransitionController(
        eventId = eventId,
        userId = organizerId,
        dispatch = { dispatched += it },
        currentState = { state },
    )

    @Test
    fun `organizing transition dispatches the shared intent for the organizer`() {
        assertTrue(controller.transition(EventLifecycleTarget.ORGANIZING))

        assertEquals(listOf<Intent>(Intent.TransitionToOrganizing(eventId, organizerId)), dispatched)
        assertEquals(EventLifecycleTarget.ORGANIZING, controller.inFlightTarget.value)
    }

    @Test
    fun `finalization dispatches MarkAsFinalized`() {
        state = state.copy(events = listOf(event(EventStatus.ORGANIZING)))

        controller.transition(EventLifecycleTarget.FINALIZED)

        assertEquals(listOf<Intent>(Intent.MarkAsFinalized(eventId, organizerId)), dispatched)
    }

    @Test
    fun `a second transition is ignored while one is in flight`() {
        controller.transition(EventLifecycleTarget.ORGANIZING)

        assertFalse(controller.transition(EventLifecycleTarget.ORGANIZING))
        assertEquals(1, dispatched.size)
    }

    @Test
    fun `toasts are not handled when no transition is in flight`() {
        assertEquals(
            EventLifecycleSideEffectResult.NotHandled,
            controller.onSideEffect(SideEffect.ShowToast("Event updated successfully")),
        )
    }

    @Test
    fun `success settles on the toast, refreshes the detail and swallows the follow-up navigation`() {
        controller.transition(EventLifecycleTarget.ORGANIZING)
        state = state.copy(events = listOf(event(EventStatus.ORGANIZING)))
        dispatched.clear()

        val result = controller.onSideEffect(SideEffect.ShowToast("Transitioned to organizing phase"))

        assertEquals(
            EventLifecycleSideEffectResult.Settled(EventLifecycleOutcome.Transitioned(EventLifecycleTarget.ORGANIZING)),
            result,
        )
        assertNull(controller.inFlightTarget.value)
        assertEquals(listOf<Intent>(Intent.SelectEvent(eventId)), dispatched)

        // Shared machine asks to open meetings; the detail stays put and reloads instead.
        assertEquals(
            EventLifecycleSideEffectResult.Consumed,
            controller.onSideEffect(SideEffect.NavigateTo("event/$eventId/meetings")),
        )
        // SelectEvent's own navigation back to this very detail is swallowed once.
        assertEquals(
            EventLifecycleSideEffectResult.Consumed,
            controller.onSideEffect(SideEffect.NavigateTo("event/$eventId")),
        )
        assertEquals(
            EventLifecycleSideEffectResult.NotHandled,
            controller.onSideEffect(SideEffect.NavigateTo("event/$eventId")),
        )
    }

    @Test
    fun `finalization success refreshes without expecting a meetings navigation`() {
        state = state.copy(events = listOf(event(EventStatus.ORGANIZING)))
        controller.transition(EventLifecycleTarget.FINALIZED)
        state = state.copy(events = listOf(event(EventStatus.FINALIZED)))

        val result = controller.onSideEffect(SideEffect.ShowToast("Event finalized successfully!"))

        assertEquals(
            EventLifecycleSideEffectResult.Settled(EventLifecycleOutcome.Transitioned(EventLifecycleTarget.FINALIZED)),
            result,
        )
        assertEquals(
            EventLifecycleSideEffectResult.NotHandled,
            controller.onSideEffect(SideEffect.NavigateTo("event/$eventId/meetings")),
        )
    }

    @Test
    fun `failure settles with the shared error and does not refresh`() {
        state = state.copy(events = listOf(event(EventStatus.ORGANIZING)))
        controller.transition(EventLifecycleTarget.FINALIZED)
        state = state.copy(error = "Finalization blocked by MEETING_REQUIRED,BUDGET_REQUIRED")
        dispatched.clear()

        val result = controller.onSideEffect(
            SideEffect.ShowToast("Finalization blocked by MEETING_REQUIRED,BUDGET_REQUIRED")
        )

        assertEquals(
            EventLifecycleSideEffectResult.Settled(
                EventLifecycleOutcome.Failed("Finalization blocked by MEETING_REQUIRED,BUDGET_REQUIRED")
            ),
            result,
        )
        assertNull(controller.inFlightTarget.value)
        assertTrue(dispatched.isEmpty())
    }

    @Test
    fun `identical repeated failures settle every time`() {
        state = state.copy(events = listOf(event(EventStatus.ORGANIZING)), error = "Failed to finalize event")

        repeat(2) {
            controller.transition(EventLifecycleTarget.FINALIZED)
            val result = controller.onSideEffect(SideEffect.ShowToast("Failed to finalize event"))
            assertEquals(
                EventLifecycleSideEffectResult.Settled(EventLifecycleOutcome.Failed("Failed to finalize event")),
                result,
            )
        }
        assertEquals(2, dispatched.size)
    }

    @Test
    fun `lifecycle card is organizer only and follows the event status`() {
        assertEquals(
            EventLifecycleCardMode.READY_TO_ORGANIZE,
            eventLifecycleCardMode(event(EventStatus.CONFIRMED), organizerId),
        )
        assertEquals(
            EventLifecycleCardMode.PICK_FINAL_OPTION,
            eventLifecycleCardMode(event(EventStatus.COMPARING), organizerId),
        )
        assertEquals(
            EventLifecycleCardMode.READY_TO_FINALIZE,
            eventLifecycleCardMode(event(EventStatus.ORGANIZING), organizerId),
        )
        assertEquals(EventLifecycleTarget.ORGANIZING, EventLifecycleCardMode.READY_TO_ORGANIZE.target)
        assertNull(EventLifecycleCardMode.PICK_FINAL_OPTION.target)
        assertEquals(EventLifecycleTarget.FINALIZED, EventLifecycleCardMode.READY_TO_FINALIZE.target)

        listOf(EventStatus.DRAFT, EventStatus.POLLING, EventStatus.FINALIZED).forEach { status ->
            assertNull(eventLifecycleCardMode(event(status), organizerId), "no card for $status")
        }
        assertNull(eventLifecycleCardMode(event(EventStatus.CONFIRMED), "participant-1"))
        assertNull(eventLifecycleCardMode(null, organizerId))
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
