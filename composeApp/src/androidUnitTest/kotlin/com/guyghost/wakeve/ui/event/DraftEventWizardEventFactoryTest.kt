package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DraftEventWizardEventFactoryTest {

    @Test
    fun generatedDraftIdStaysStableAcrossAutosavesAndCompletion() {
        val factory = DraftEventWizardEventFactory(
            initialEvent = null,
            generatedId = "event-fixed",
            nowIso = { "2026-05-22T10:00:00Z" }
        )

        val firstSave = factory.buildEvent(
            userId = "user-1",
            title = "Week-end",
            description = "Organisation",
            eventType = EventType.OTHER,
            eventTypeCustom = "",
            participantCount = "",
            timeSlots = emptyList(),
            status = EventStatus.DRAFT
        )

        val finalSave = factory.buildEvent(
            userId = "user-1",
            title = "Week-end",
            description = "Organisation complète",
            eventType = EventType.OTHER,
            eventTypeCustom = "",
            participantCount = "8",
            timeSlots = emptyList(),
            status = EventStatus.POLLING
        )

        assertEquals("event-fixed", firstSave.id)
        assertEquals(firstSave.id, finalSave.id)
        assertEquals(EventStatus.POLLING, finalSave.status)
        assertEquals(8, finalSave.expectedParticipants)
    }

    @Test
    fun initialEventIdIsPreservedWhenEditingDraft() {
        val initial = DraftEventWizardEventFactory(
            initialEvent = null,
            generatedId = "event-initial",
            nowIso = { "2026-05-22T10:00:00Z" }
        ).buildEvent(
            userId = "user-1",
            title = "Initial",
            description = "Initial description",
            eventType = EventType.OTHER,
            eventTypeCustom = "",
            participantCount = "",
            timeSlots = emptyList(),
            status = EventStatus.DRAFT
        )

        val factory = DraftEventWizardEventFactory(
            initialEvent = initial,
            generatedId = "event-new",
            nowIso = { "2026-05-22T11:00:00Z" }
        )

        val updated = factory.buildEvent(
            userId = "user-1",
            title = "Updated",
            description = "Updated description",
            eventType = EventType.OTHER,
            eventTypeCustom = "",
            participantCount = "",
            timeSlots = emptyList(),
            status = EventStatus.DRAFT
        )

        assertEquals("event-initial", updated.id)
        assertNotEquals("event-new", updated.id)
        assertEquals(initial.createdAt, updated.createdAt)
        assertEquals("2026-05-22T11:00:00Z", updated.updatedAt)
    }
}
