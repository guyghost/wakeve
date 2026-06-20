package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.presentation.state.EventManagementContract
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class EventWorkspaceModelsTest {
    @Test
    fun `maps event state to filtered immutable workspace state`() {
        val state = EventManagementContract.State(
            events = listOf(
                event(id = "draft", title = "Draft dinner", status = EventStatus.DRAFT, organizerId = "me"),
                event(id = "final", title = "Finished trip", status = EventStatus.FINALIZED, organizerId = "friend")
            )
        )

        val uiState = state.toEventWorkspaceUiState(
            currentUserId = "me",
            selectedFilter = EventListFilter.Upcoming,
            searchQuery = "dinner",
            selectedEventId = null
        )

        assertFalse(uiState.isLoading)
        assertEquals(null, uiState.error)
        assertEquals(listOf("draft"), uiState.events.map { it.id })
        assertNull(uiState.selectedEvent)
        assertEquals(true, uiState.events.single().isOrganizer)
        assertEquals("Brouillon", uiState.events.single().statusLabel)
        assertEquals("À reprendre", uiState.events.single().nextActionLabel)
    }

    @Test
    fun `uses selected event id for expanded list detail state`() {
        val state = EventManagementContract.State(
            events = listOf(
                event(id = "first", title = "First event", status = EventStatus.POLLING),
                event(id = "second", title = "Second event", status = EventStatus.CONFIRMED)
            )
        )

        val uiState = state.toEventWorkspaceUiState(
            currentUserId = "me",
            selectedFilter = EventListFilter.Upcoming,
            searchQuery = "",
            selectedEventId = "second"
        )

        assertEquals("second", uiState.selectedEvent?.id)
    }

    @Test
    fun `workspace summary prioritizes organizer draft recovery`() {
        val state = EventManagementContract.State(
            events = listOf(
                event(id = "poll", title = "Vote dinner", status = EventStatus.POLLING, participants = listOf("me", "alice")),
                event(id = "draft", title = "Draft trip", status = EventStatus.DRAFT, organizerId = "me")
            ),
            pollVotes = mapOf("poll" to mapOf("me" to Vote.YES))
        )

        val uiState = state.toEventWorkspaceUiState(
            currentUserId = "me",
            selectedFilter = EventListFilter.Upcoming,
            searchQuery = "",
            selectedEventId = null
        )

        assertEquals("draft", uiState.actionSummary?.eventId)
        assertEquals("Reprenez Draft trip", uiState.actionSummary?.title)
        assertEquals("Continuer", uiState.actionSummary?.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.OpenEvent, uiState.actionSummary?.action)
    }

    @Test
    fun `workspace summary opens poll directly when votes are the next blocker`() {
        val state = EventManagementContract.State(
            events = listOf(
                event(
                    id = "poll",
                    title = "Vote dinner",
                    status = EventStatus.POLLING,
                    participants = listOf("me", "alice", "sam")
                )
            ),
            pollVotes = mapOf("poll" to mapOf("me" to Vote.YES))
        )

        val uiState = state.toEventWorkspaceUiState(
            currentUserId = "me",
            selectedFilter = EventListFilter.Upcoming,
            searchQuery = "",
            selectedEventId = null
        )

        assertEquals("poll", uiState.actionSummary?.eventId)
        assertEquals("Faites avancer le sondage", uiState.actionSummary?.title)
        assertEquals("2 participants à relancer avant de bloquer une date.", uiState.actionSummary?.body)
        assertEquals("Ouvrir le vote", uiState.actionSummary?.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.OpenPoll, uiState.actionSummary?.action)
    }

    @Test
    fun `workspace summary recreates from most recent finalized event`() {
        val state = EventManagementContract.State(
            events = listOf(
                event(
                    id = "older",
                    title = "Spring dinner",
                    status = EventStatus.FINALIZED,
                    updatedAt = "2026-05-01T08:00:00Z"
                ),
                event(
                    id = "newer",
                    title = "Summer retreat",
                    description = "A weekend by the sea",
                    status = EventStatus.FINALIZED,
                    eventType = EventType.OUTDOOR_ACTIVITY,
                    updatedAt = "2026-06-15T08:00:00Z"
                )
            )
        )

        val uiState = state.toEventWorkspaceUiState(
            currentUserId = "me",
            selectedFilter = EventListFilter.Past,
            searchQuery = "",
            selectedEventId = null
        )

        assertEquals("newer", uiState.actionSummary?.eventId)
        assertEquals("Réorganisez Summer retreat", uiState.actionSummary?.title)
        assertEquals("Réutiliser", uiState.actionSummary?.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.RecreateFromTemplate, uiState.actionSummary?.action)
        assertEquals(
            EventWorkspaceCreationTemplate(
                title = "Summer retreat",
                description = "A weekend by the sea",
                eventType = EventType.OUTDOOR_ACTIVITY
            ),
            uiState.actionSummary?.template
        )
    }

    @Test
    fun `finalized event builds quick reorganization summary`() {
        val event = event(
            id = "finalized",
            title = "Summer retreat",
            description = "A weekend by the sea",
            status = EventStatus.FINALIZED,
            eventType = EventType.OUTDOOR_ACTIVITY
        )

        val summary = event.toReorganizationSummary()

        assertEquals("Réorganiser rapidement", summary?.title)
        assertEquals(
            "Créez une nouvelle édition de Summer retreat avec le titre, la description et le type déjà repris.",
            summary?.body
        )
        assertEquals("Créer une nouvelle édition", summary?.actionLabel)
        assertEquals(
            EventWorkspaceCreationTemplate(
                title = "Summer retreat",
                description = "A weekend by the sea",
                eventType = EventType.OUTDOOR_ACTIVITY
            ),
            summary?.template
        )
    }

    @Test
    fun `active event does not build reorganization summary`() {
        assertEquals(
            null,
            event(
                id = "active",
                title = "Summer retreat",
                status = EventStatus.ORGANIZING
            ).toReorganizationSummary()
        )
    }

    @Test
    fun `event widget summary promotes event happening today`() {
        val summary = listOf(
            event(
                id = "tomorrow",
                title = "Tomorrow dinner",
                status = EventStatus.CONFIRMED,
                finalDate = "2026-06-21T18:00:00Z"
            ),
            event(
                id = "today",
                title = "Today barbecue",
                status = EventStatus.ORGANIZING,
                participants = listOf("me", "alice", "sam"),
                finalDate = "2026-06-20T12:00:00Z"
            )
        ).toEventWidgetSummary(
            now = Instant.parse("2026-06-20T08:00:00Z"),
            timeZone = TimeZone.UTC,
            currentUserId = "me"
        )

        assertEquals(EventWidgetKind.EventToday, summary.kind)
        assertEquals("today", summary.eventId)
        assertEquals("Aujourd'hui", summary.title)
        assertEquals("Today barbecue", summary.headline)
        assertEquals("3 participants attendus", summary.body)
        assertEquals("Ouvrir", summary.actionLabel)
    }

    @Test
    fun `event widget summary shows countdown for next dated event`() {
        val summary = listOf(
            event(
                id = "future",
                title = "Weekend trip",
                status = EventStatus.CONFIRMED,
                finalDate = "2026-06-24T09:00:00Z"
            )
        ).toEventWidgetSummary(
            now = Instant.parse("2026-06-20T08:00:00Z"),
            timeZone = TimeZone.UTC,
            currentUserId = "me"
        )

        assertEquals(EventWidgetKind.Countdown, summary.kind)
        assertEquals("future", summary.eventId)
        assertEquals("Weekend trip", summary.title)
        assertEquals("J-4", summary.headline)
        assertEquals("À préparer", summary.body)
        assertEquals("Préparer", summary.actionLabel)
    }

    @Test
    fun `event widget summary falls back to next task when no date is known`() {
        val summary = listOf(
            event(
                id = "draft",
                title = "Draft dinner",
                status = EventStatus.DRAFT,
                proposedSlots = emptyList()
            )
        ).toEventWidgetSummary(
            now = Instant.parse("2026-06-20T08:00:00Z"),
            timeZone = TimeZone.UTC,
            currentUserId = "me"
        )

        assertEquals(EventWidgetKind.NextTask, summary.kind)
        assertEquals("draft", summary.eventId)
        assertEquals("Prochaine tâche", summary.title)
        assertEquals("Reprenez Draft dinner", summary.headline)
        assertEquals("Continuer", summary.body)
        assertEquals("Continuer", summary.actionLabel)
    }

    @Test
    fun `event widget summary explains empty state`() {
        val summary = emptyList<Event>().toEventWidgetSummary(
            now = Instant.parse("2026-06-20T08:00:00Z"),
            timeZone = TimeZone.UTC,
            currentUserId = "me"
        )

        assertEquals(EventWidgetKind.Empty, summary.kind)
        assertEquals(null, summary.eventId)
        assertEquals("Wakeve", summary.title)
        assertEquals("Aucun événement actif", summary.headline)
        assertEquals("Créez un événement pour afficher le prochain rendez-vous ici.", summary.body)
        assertEquals("Créer", summary.actionLabel)
    }

    private fun event(
        id: String,
        title: String,
        status: EventStatus,
        organizerId: String = "me",
        participants: List<String> = listOf("me"),
        description: String = "Description",
        eventType: EventType = EventType.OTHER,
        updatedAt: String = "2026-06-01T08:00:00Z",
        finalDate: String? = null,
        proposedSlots: List<TimeSlot> = listOf(
            TimeSlot(
                id = "$id-slot",
                start = "2026-07-14T09:00:00Z",
                end = "2026-07-14T18:00:00Z",
                timezone = "Europe/Paris"
            )
        )
    ): Event =
        Event(
            id = id,
            title = title,
            description = description,
            organizerId = organizerId,
            participants = participants,
            proposedSlots = proposedSlots,
            deadline = "2026-07-01T12:00:00Z",
            status = status,
            finalDate = finalDate,
            createdAt = "2026-06-01T08:00:00Z",
            updatedAt = updatedAt,
            eventType = eventType
        )
}
