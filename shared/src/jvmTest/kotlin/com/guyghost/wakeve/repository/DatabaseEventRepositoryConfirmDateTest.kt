package com.guyghost.wakeve.repository

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatabaseEventRepositoryConfirmDateTest {

    private lateinit var db: WakeveDb
    private lateinit var repository: DatabaseEventRepository

    @BeforeTest
    fun setup() {
        db = createFreshTestDatabase()
        repository = DatabaseEventRepository(db)
    }

    @Test
    fun `confirmEventDate stores the selected slot instead of the first proposed slot`() = runBlocking {
        val firstSlot = createTestTimeSlot(
            id = "slot-first",
            start = "2026-07-10T09:00:00Z",
            end = "2026-07-10T11:00:00Z"
        )
        val selectedSecondSlot = createTestTimeSlot(
            id = "slot-selected-second",
            start = "2026-07-11T14:00:00Z",
            end = "2026-07-11T16:00:00Z"
        )
        val event = createTestEvent(
            id = "event-confirm-selected-slot",
            organizerId = "organizer-1",
            proposedSlots = listOf(firstSlot, selectedSecondSlot),
            status = EventStatus.POLLING,
            deadline = "2026-07-01T23:59:59Z"
        )
        assertTrue(repository.createEvent(event).isSuccess)

        val result = repository.confirmEventDate(
            eventId = event.id,
            slotId = selectedSecondSlot.id,
            confirmedByOrganizerId = event.organizerId
        )

        assertTrue(result.isSuccess)
        val confirmedDate = db.confirmedDateQueries
            .selectByEventId(event.id)
            .executeAsOne()
        assertEquals(selectedSecondSlot.id, confirmedDate.timeslotId)
        assertEquals(event.organizerId, confirmedDate.confirmedByOrganizerId)

        val storedEvent = db.eventQueries.selectById(event.id).executeAsOne()
        assertEquals(EventStatus.CONFIRMED.name, storedEvent.status)
    }
}
