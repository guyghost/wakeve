package com.guyghost.wakeve

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class WakeveLegalUrlsTest {
    @Test
    fun legalUrlsUseProductionDomain() {
        assertEquals("https://wakeve.app/privacy", WakeveLegalUrls.PRIVACY)
        assertEquals("https://wakeve.app/support", WakeveLegalUrls.SUPPORT)
        assertEquals("https://wakeve.app/terms", WakeveLegalUrls.TERMS)
    }

    @Test
    fun legalUrlsDoNotUseLegacyDomain() {
        val urls = listOf(
            WakeveLegalUrls.PRIVACY,
            WakeveLegalUrls.SUPPORT,
            WakeveLegalUrls.TERMS
        )

        assertFalse(urls.any { it.contains("wakeve.com") })
    }
}
