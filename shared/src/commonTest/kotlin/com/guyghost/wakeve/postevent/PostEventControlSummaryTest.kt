package com.guyghost.wakeve.postevent

import com.guyghost.wakeve.models.Album
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.payment.SettlementRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PostEventControlSummaryTest {
    @Test
    fun `finalized event with pending settlements prioritizes reimbursements`() {
        val event = event(status = EventStatus.FINALIZED)

        val summary = event.toPostEventControlSummary(
            settlements = listOf(
                settlement(eventId = event.id, amount = 80.0),
                settlement(eventId = event.id, amount = 40.0)
            ),
            albums = listOf(album(eventId = event.id, photoIds = listOf("photo-1", "photo-2", "photo-3")))
        )

        assertTrue(summary.isAfterEvent)
        assertEquals(PostEventItemStatus.NEEDS_ACTION, summary.settlementStatus)
        assertEquals(2, summary.unresolvedSettlementCount)
        assertEquals(120.0, summary.unresolvedSettlementTotal)
        assertEquals(PostEventItemStatus.COMPLETE, summary.photoStatus)
        assertEquals(PostEventPrimaryAction.OPEN_SETTLEMENTS, summary.primaryAction)
        assertFalse(PostEventQuestion.WHO_OWES_WHOM in summary.missingAnswers)
        assertTrue(summary.body.contains("120"))
    }

    @Test
    fun `finalized event without settlements or photos exposes missing post event answers`() {
        val event = event(status = EventStatus.FINALIZED)

        val summary = event.toPostEventControlSummary(
            settlements = emptyList(),
            albums = emptyList()
        )

        assertEquals(PostEventItemStatus.MISSING, summary.settlementStatus)
        assertEquals(PostEventItemStatus.MISSING, summary.photoStatus)
        assertEquals(PostEventItemStatus.COMPLETE, summary.reorganizationStatus)
        assertEquals(PostEventPrimaryAction.OPEN_SETTLEMENTS, summary.primaryAction)
        assertEquals(
            listOf(
                PostEventQuestion.WHO_OWES_WHOM,
                PostEventQuestion.WHICH_PHOTOS_TO_SHARE
            ),
            summary.missingAnswers
        )
    }

    @Test
    fun `finalized event with settled payments and photos can be recreated`() {
        val event = event(status = EventStatus.FINALIZED, title = "Biarritz Weekend")

        val summary = event.toPostEventControlSummary(
            settlements = listOf(settlement(eventId = event.id, amount = 55.0, status = "PAID")),
            albums = listOf(
                album(eventId = event.id, photoIds = listOf("photo-1", "photo-2")),
                album(eventId = event.id, photoIds = listOf("photo-3"))
            )
        )

        assertEquals(PostEventItemStatus.COMPLETE, summary.settlementStatus)
        assertEquals(PostEventItemStatus.COMPLETE, summary.photoStatus)
        assertEquals(PostEventItemStatus.COMPLETE, summary.reorganizationStatus)
        assertEquals(3, summary.sharedPhotoCount)
        assertEquals(PostEventPrimaryAction.RECREATE_EVENT, summary.primaryAction)
        assertTrue(summary.missingAnswers.isEmpty())
        assertTrue(summary.reorganizationLabel.contains("Biarritz Weekend"))
    }

    @Test
    fun `non finalized event keeps post event recap unavailable`() {
        val event = event(status = EventStatus.ORGANIZING)

        val summary = event.toPostEventControlSummary(
            settlements = listOf(settlement(eventId = event.id, amount = 20.0)),
            albums = listOf(album(eventId = event.id, photoIds = listOf("photo-1")))
        )

        assertFalse(summary.isAfterEvent)
        assertEquals(PostEventItemStatus.MISSING, summary.settlementStatus)
        assertEquals(PostEventItemStatus.MISSING, summary.photoStatus)
        assertEquals(PostEventItemStatus.MISSING, summary.reorganizationStatus)
        assertEquals(PostEventPrimaryAction.OPEN_EVENT, summary.primaryAction)
        assertEquals(
            listOf(
                PostEventQuestion.WHO_OWES_WHOM,
                PostEventQuestion.WHICH_PHOTOS_TO_SHARE,
                PostEventQuestion.HOW_TO_REORGANIZE
            ),
            summary.missingAnswers
        )
    }

    private fun event(
        status: EventStatus,
        id: String = "event-1",
        title: String = "Team Dinner"
    ): Event =
        Event(
            id = id,
            title = title,
            description = "Shared event",
            organizerId = "organizer-1",
            participants = listOf("organizer-1", "guest-1", "guest-2"),
            proposedSlots = emptyList(),
            deadline = "2026-01-01T00:00:00Z",
            status = status,
            finalDate = "2026-01-05T19:00:00Z",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-06T00:00:00Z",
            eventType = EventType.FOOD_TASTING
        )

    private fun settlement(
        eventId: String,
        amount: Double,
        status: String = "PERSISTED"
    ): SettlementRecord =
        SettlementRecord(
            settlementId = "settlement-$amount-$status",
            eventId = eventId,
            budgetId = "budget-1",
            fromParticipantId = "guest-1",
            toParticipantId = "organizer-1",
            amount = amount,
            status = status,
            createdAt = "2026-01-06T00:00:00Z",
            updatedAt = "2026-01-06T00:00:00Z"
        )

    private fun album(eventId: String, photoIds: List<String>): Album =
        Album(
            id = "album-${photoIds.size}",
            eventId = eventId,
            name = "Event photos",
            coverPhotoId = photoIds.firstOrNull(),
            photoIds = photoIds,
            createdAt = "2026-01-06T00:00:00Z",
            isAutoGenerated = true,
            updatedAt = "2026-01-06T00:00:00Z"
        )
}
