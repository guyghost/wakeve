package com.guyghost.wakeve.productlanguage

import com.guyghost.wakeve.models.EventStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ProductLanguageTest {
    @Test
    fun canonicalStatesKeepDomainIdentity() {
        val expected = mapOf(
            EventStatus.DRAFT to "event.state.draft",
            EventStatus.POLLING to "event.state.polling",
            EventStatus.COMPARING to "event.state.comparing",
            EventStatus.CONFIRMED to "event.state.confirmed",
            EventStatus.ORGANIZING to "event.state.organizing",
            EventStatus.FINALIZED to "event.state.finalized",
        )

        expected.forEach { (status, key) ->
            val result = projectEventState(
                ProductLanguageInput(
                    status = status,
                    role = UserRole.ORGANIZER,
                    confirmedFacts = emptySet(),
                    pendingFacts = emptySet(),
                    allowedAction = if (status == EventStatus.FINALIZED) null else AllowedAction.CONTINUE,
                ),
            )

            assertEquals(status, result.domainStatus)
            assertEquals(key, result.title.value)
            assertEquals(status == EventStatus.FINALIZED, result.primaryAction == null)
        }

        assertEquals(setOf("en", "fr", "de", "es", "it", "pt"), SUPPORTED_PRODUCT_LOCALES)
        assertEquals("en", FALLBACK_PRODUCT_LOCALE)
    }

    @Test
    fun syncConflictProjectsUnsharedConflictWithoutUnrelatedAction() {
        val result = projectEventState(input(pendingFacts = setOf(PendingFact.SYNC_CONFLICT)))

        assertEquals("sync.conflict", result.status?.value)
        assertEquals(null, result.primaryAction)
        assertEquals(false, result.sharedConfirmation)
    }

    @Test
    fun syncConflictTakesPriorityOverLocalMutation() {
        val result = projectEventState(
            input(
                pendingFacts = setOf(PendingFact.LOCAL_MUTATION, PendingFact.SYNC_CONFLICT),
                allowedAction = AllowedAction.RETRY_SYNC,
            ),
        )

        assertEquals("sync.conflict", result.status?.value)
        assertEquals("sync.retry", result.primaryAction?.value)
        assertEquals(false, result.sharedConfirmation)
    }

    @Test
    fun finalizedSuppressesContinueAndRetryActions() {
        AllowedAction.entries.forEach { staleAction ->
            val result = projectEventState(
                input(
                    status = EventStatus.FINALIZED,
                    pendingFacts = setOf(PendingFact.LOCAL_MUTATION, PendingFact.SYNC_CONFLICT),
                    allowedAction = staleAction,
                ),
            )

            assertEquals(null, result.status)
            assertEquals(null, result.primaryAction)
        }
    }

    @Test
    fun retrySyncIsHiddenWithoutSyncFact() {
        val result = projectEventState(input(allowedAction = AllowedAction.RETRY_SYNC))

        assertEquals(null, result.primaryAction)
    }

    private fun input(
        status: EventStatus = EventStatus.POLLING,
        pendingFacts: Set<PendingFact> = emptySet(),
        allowedAction: AllowedAction? = AllowedAction.CONTINUE,
    ) = ProductLanguageInput(
        status = status,
        role = UserRole.ORGANIZER,
        confirmedFacts = emptySet(),
        pendingFacts = pendingFacts,
        allowedAction = allowedAction,
    )
}
