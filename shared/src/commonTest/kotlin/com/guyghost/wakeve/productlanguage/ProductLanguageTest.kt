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
}
