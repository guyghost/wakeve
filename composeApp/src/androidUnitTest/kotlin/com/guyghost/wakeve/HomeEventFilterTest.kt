package com.guyghost.wakeve

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeEventFilterTest {

    @Test
    fun upcomingKeepsNewDraftAndPollingEventsVisible() {
        val draft = event("draft", EventStatus.DRAFT)
        val polling = event("polling", EventStatus.POLLING)
        val finalized = event("finalized", EventStatus.FINALIZED)

        val visible = filterEvents(
            events = listOf(draft, polling, finalized),
            filter = HomeEventFilter.UPCOMING
        )

        assertEquals(listOf("draft", "polling"), visible.map { it.id })
    }

    private fun event(id: String, status: EventStatus): Event = Event(
        id = id,
        title = id,
        description = "$id description",
        organizerId = "currentUser",
        participants = emptyList(),
        proposedSlots = emptyList(),
        deadline = "2026-05-29T10:00:00Z",
        status = status,
        createdAt = "2026-05-22T10:00:00Z",
        updatedAt = "2026-05-22T10:00:00Z"
    )
}
