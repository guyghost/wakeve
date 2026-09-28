package com.guyghost.wakeve.repository

import com.guyghost.wakeve.confirmation.ConfirmationClock
import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * QA iOS 2026-09-27 (IOS-3): "Date à décider avec le groupe" creates a DRAFT without
 * any date. The poll can only start once at least one date has been added.
 */
class DraftWithoutDatesRedTest {

    private lateinit var db: WakeveDb
    private lateinit var repository: DatabaseEventRepository

    @BeforeTest
    fun setup() {
        db = createFreshTestDatabase()
        repository = DatabaseEventRepository(db, ConfirmationClock { Instant.parse("2026-09-27T21:00:00Z") })
    }

    private suspend fun createDraftWithoutDates(): String {
        val created = CreateEventUseCase(repository)(
            createTestEvent(
                id = "event-watch-party",
                title = "Watch party finale LDC",
                description = "Date a decider avec le groupe",
                organizerId = "org-nora",
                proposedSlots = emptyList(),
                deadline = "2026-10-04T18:00:00Z",
                status = EventStatus.DRAFT,
                createdAt = "2026-09-27T20:00:00Z",
                updatedAt = "2026-09-27T20:00:00Z"
            )
        )
        assertTrue(created.isSuccess, "A DRAFT without dates must be persisted: ${created.exceptionOrNull()}")
        return "event-watch-party"
    }

    @Test
    fun draftWithoutDatesIsPersisted() = runBlocking {
        val eventId = createDraftWithoutDates()

        val stored = repository.getEvent(eventId)
        assertEquals(EventStatus.DRAFT, stored?.status)
        assertTrue(stored?.proposedSlots.orEmpty().isEmpty())
    }

    @Test
    fun pollCannotStartWithoutAnyDate() = runBlocking {
        val eventId = createDraftWithoutDates()

        val polling = repository.updateEventStatus(eventId, EventStatus.POLLING, null)

        assertTrue(polling.isFailure, "Nobody can vote on a poll without dates")
        assertEquals("Poll requires at least one time slot", polling.exceptionOrNull()?.message)
        assertEquals(EventStatus.DRAFT, repository.getEvent(eventId)?.status)
    }

    @Test
    fun organizerAddsDatesThenStartsThePoll() = runBlocking {
        val eventId = createDraftWithoutDates()
        val draft = requireNotNull(repository.getEvent(eventId))

        val saved = repository.saveEvent(
            draft.copy(
                proposedSlots = listOf(
                    createTestTimeSlot(id = "slot-final-1", start = "2027-05-29T19:00:00Z", end = "2027-05-29T23:00:00Z"),
                    createTestTimeSlot(id = "slot-final-2", start = "2027-05-30T19:00:00Z", end = "2027-05-30T23:00:00Z")
                )
            )
        )
        assertTrue(saved.isSuccess, "Adding dates to a draft must succeed: ${saved.exceptionOrNull()}")
        assertEquals(2, repository.getEvent(eventId)?.proposedSlots?.size)

        val polling = repository.updateEventStatus(eventId, EventStatus.POLLING, null)
        assertTrue(polling.isSuccess, "The poll starts once dates exist: ${polling.exceptionOrNull()}")
    }
}
