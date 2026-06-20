package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.postevent.PostEventControlSummary
import com.guyghost.wakeve.postevent.PostEventItemStatus
import com.guyghost.wakeve.postevent.PostEventPrimaryAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventPhotosFollowUpModelsTest {
    @Test
    fun fallbackStateExplainsMissingPhotoSelection() {
        val state = fallbackEventPhotosFollowUpUiState(eventId = "event-1")

        assertEquals("event-1", state.eventId)
        assertEquals("Photos a preparer", state.title)
        assertEquals("Selection manquante", state.statusLabel)
        assertEquals("Ajouter des photos", state.recommendedActionLabel)
        assertFalse(state.canShareNow)
        assertEquals(3, state.checklist.size)
        assertTrue(state.checklist.all { !it.isComplete })
        assertEquals("Importer les photos", state.checklist.first().title)
    }

    @Test
    fun completeSummaryAllowsSharingAndShowsPhotoCount() {
        val state = summary(
            photoStatus = PostEventItemStatus.COMPLETE,
            sharedPhotoCount = 4
        ).toEventPhotosFollowUpUiState()

        assertEquals("Photos a partager", state.title)
        assertEquals("4 photos pretes pour le groupe", state.subtitle)
        assertEquals("Selection prete", state.statusLabel)
        assertEquals("Partager les photos", state.recommendedActionLabel)
        assertTrue(state.canShareNow)
        assertEquals("Selection partageable", state.checklist.first().title)
        assertEquals("OK", state.checklist.first().statusLabel)
        assertTrue(state.checklist.first().isComplete)
    }

    @Test
    fun needsActionSummaryKeepsSharingBlockedUntilPhotosAreVerified() {
        val state = summary(
            photoStatus = PostEventItemStatus.NEEDS_ACTION,
            sharedPhotoCount = 0,
            settlementStatus = PostEventItemStatus.NEEDS_ACTION
        ).toEventPhotosFollowUpUiState()

        assertEquals("Photos a verifier", state.title)
        assertEquals("Selection a verifier", state.statusLabel)
        assertEquals("Verifier les photos", state.recommendedActionLabel)
        assertFalse(state.canShareNow)
        assertEquals("A verifier", state.checklist.first().statusLabel)
        assertFalse(state.checklist.first().isComplete)
        assertEquals("A suivre", state.checklist[1].statusLabel)
    }

    private fun summary(
        photoStatus: PostEventItemStatus,
        sharedPhotoCount: Int,
        settlementStatus: PostEventItemStatus = PostEventItemStatus.COMPLETE,
        reorganizationStatus: PostEventItemStatus = PostEventItemStatus.COMPLETE
    ): PostEventControlSummary =
        PostEventControlSummary(
            eventId = "event-1",
            title = "Apres week-end",
            isAfterEvent = true,
            settlementStatus = settlementStatus,
            photoStatus = photoStatus,
            reorganizationStatus = reorganizationStatus,
            unresolvedSettlementCount = 0,
            unresolvedSettlementTotal = 0.0,
            sharedPhotoCount = sharedPhotoCount,
            headline = "Recap",
            body = "Recap",
            settlementLabel = "Remboursements soldes",
            photoLabel = "Photos",
            reorganizationLabel = "Relance rapide disponible",
            primaryAction = PostEventPrimaryAction.OPEN_PHOTOS,
            missingAnswers = emptyList()
        )
}
