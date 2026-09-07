package com.guyghost.wakeve.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verrouille les tokens de fond de la proposition Swarm DAO #27
 * (fond ivoire chaud #F6F1EA, mesuré au pixel sur l'écran Résultats
 * qa-screenshots/cycle-2026-09-04/28-poll-results.png).
 */
class DesignTokensTest {

    @Test
    fun warmIvoryMatchesReferenceScreenshot() {
        assertEquals(Color(0xFFF6F1EA), WarmIvory)
    }

    @Test
    fun warmIvoryDarkIsAlignedWithIosMidnight() {
        assertEquals(Color(0xFF071421), WarmIvoryDark)
    }

    @Test
    fun backgroundLightTokenUsesWarmIvory() {
        assertEquals(WarmIvory, WakeveBackgroundLight)
    }

    @Test
    fun surfaceLightTokenUsesWarmIvory() {
        assertEquals(WarmIvory, WakeveSurfaceLight)
    }

    @Test
    fun homeBackgroundLightTokenUsesWarmIvory() {
        assertEquals(WarmIvory, HomeBackgroundLight)
    }
}
