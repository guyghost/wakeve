package com.guyghost.wakeve.repository

import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventRepositoryConfirmationLifecycleTest {
    @Test
    fun confirmationRejectsDraftWithoutChangingEvent() = assertStatusRejected(EventStatus.DRAFT)

    @Test
    fun confirmationRejectsComparingWithoutChangingEvent() = assertStatusRejected(EventStatus.COMPARING)

    @Test
    fun confirmationRejectsOrganizingWithoutChangingEvent() = assertStatusRejected(EventStatus.ORGANIZING)

    @Test
    fun confirmationRejectsFinalizedWithoutChangingEvent() = assertStatusRejected(EventStatus.FINALIZED)

    private fun assertStatusRejected(status: EventStatus) = runTest {
        val repository = EventRepository()
        val slot = createTestTimeSlot()
        val event = createTestEvent(status = status, proposedSlots = listOf(slot))
        repository.createEvent(event).getOrThrow()

        val result = repository.confirmEventDate(event.id, slot.id, event.organizerId)

        assertTrue(result.isFailure, "Confirmation must reject $status")
        assertEquals(event, repository.getEvent(event.id))
    }

    @Test
    fun pollingConfirmationStoresTheSelectedProposedDate() = runTest {
        val repository = EventRepository()
        val firstSlot = createTestTimeSlot()
        val selectedSlot = createTestTimeSlot(id = "slot-2", start = "2026-10-20T10:00:00Z")
        val event = createTestEvent(status = EventStatus.POLLING, proposedSlots = listOf(firstSlot, selectedSlot))
        repository.createEvent(event).getOrThrow()

        assertTrue(repository.confirmEventDate(event.id, selectedSlot.id, event.organizerId).isSuccess)
        assertEquals(event.copy(status = EventStatus.CONFIRMED, finalDate = selectedSlot.start), repository.getEvent(event.id))
    }

    @Test
    fun repeatingTheConfirmedDateSucceedsWithoutChangingEvent() = runTest {
        val repository = EventRepository()
        val slot = createTestTimeSlot()
        val event = createTestEvent(status = EventStatus.POLLING, proposedSlots = listOf(slot))
        repository.createEvent(event).getOrThrow()
        repository.confirmEventDate(event.id, slot.id, event.organizerId).getOrThrow()
        val confirmed = repository.getEvent(event.id)

        assertTrue(repository.confirmEventDate(event.id, slot.id, event.organizerId).isSuccess)
        assertEquals(confirmed, repository.getEvent(event.id))
    }

    @Test
    fun repeatingConfirmationWithADifferentDateFailsWithoutChangingEvent() = runTest {
        val repository = EventRepository()
        val slot = createTestTimeSlot()
        val otherSlot = createTestTimeSlot(id = "slot-2", start = "2026-10-20T10:00:00Z")
        val event = createTestEvent(status = EventStatus.POLLING, proposedSlots = listOf(slot, otherSlot))
        repository.createEvent(event).getOrThrow()
        repository.confirmEventDate(event.id, slot.id, event.organizerId).getOrThrow()
        val confirmed = repository.getEvent(event.id)

        assertTrue(repository.confirmEventDate(event.id, otherSlot.id, event.organizerId).isFailure)
        assertEquals(confirmed, repository.getEvent(event.id))
    }

    @Test
    fun anotherActorCannotConfirmOrRepeatAConfirmation() = runTest {
        val repository = EventRepository()
        val slot = createTestTimeSlot()
        val event = createTestEvent(status = EventStatus.POLLING, proposedSlots = listOf(slot))
        repository.createEvent(event).getOrThrow()

        assertTrue(repository.confirmEventDate(event.id, slot.id, "another-actor").isFailure)
        assertEquals(event, repository.getEvent(event.id))
        repository.confirmEventDate(event.id, slot.id, event.organizerId).getOrThrow()
        val confirmed = repository.getEvent(event.id)
        assertTrue(repository.confirmEventDate(event.id, slot.id, "another-actor").isFailure)
        assertEquals(confirmed, repository.getEvent(event.id))
    }

    @Test
    fun aSlotOutsideTheProposalCannotBeConfirmed() = runTest {
        val repository = EventRepository()
        val event = createTestEvent(status = EventStatus.POLLING, proposedSlots = listOf(createTestTimeSlot()))
        repository.createEvent(event).getOrThrow()

        assertTrue(repository.confirmEventDate(event.id, "missing-slot", event.organizerId).isFailure)
        assertEquals(event, repository.getEvent(event.id))
    }

    @Test
    fun aFlexibleSlotWithoutAStartCannotBeConfirmed() = runTest {
        val repository = EventRepository()
        val slot = createTestTimeSlot(start = null, end = null, timeOfDay = TimeOfDay.MORNING)
        val event = createTestEvent(status = EventStatus.POLLING, proposedSlots = listOf(slot))
        repository.createEvent(event).getOrThrow()

        assertTrue(repository.confirmEventDate(event.id, slot.id, event.organizerId).isFailure)
        assertEquals(event, repository.getEvent(event.id))
    }
}
