package com.guyghost.wakeve.localization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for AppLocale enum and localization utilities.
 */
class AppLocaleTest {

    @Test
    fun `AppLocale has 3 supported languages`() {
        assertEquals(3, AppLocale.entries.size)
    }

    @Test
    fun `FRENCH has correct code and name`() {
        assertEquals("fr", AppLocale.FRENCH.code)
        assertEquals("Français", AppLocale.FRENCH.displayName)
    }

    @Test
    fun `ENGLISH has correct code and name`() {
        assertEquals("en", AppLocale.ENGLISH.code)
        assertEquals("English", AppLocale.ENGLISH.displayName)
    }

    @Test
    fun `SPANISH has correct code and name`() {
        assertEquals("es", AppLocale.SPANISH.code)
        assertEquals("Español", AppLocale.SPANISH.displayName)
    }

    @Test
    fun `fromCode returns correct locale for fr`() {
        assertEquals(AppLocale.FRENCH, AppLocale.fromCode("fr"))
    }

    @Test
    fun `fromCode returns correct locale for en`() {
        assertEquals(AppLocale.ENGLISH, AppLocale.fromCode("en"))
    }

    @Test
    fun `fromCode returns correct locale for es`() {
        assertEquals(AppLocale.SPANISH, AppLocale.fromCode("es"))
    }

    @Test
    fun `fromCode defaults to ENGLISH for unknown code`() {
        assertEquals(AppLocale.ENGLISH, AppLocale.fromCode("de"))
    }

    @Test
    fun `fromCode defaults to ENGLISH for empty string`() {
        assertEquals(AppLocale.ENGLISH, AppLocale.fromCode(""))
    }

    @Test
    fun `all locales have non-blank codes`() {
        AppLocale.entries.forEach { locale ->
            assertTrue(locale.code.isNotBlank(), "${locale.name} should have non-blank code")
        }
    }

    @Test
    fun `all locale codes are 2 characters`() {
        AppLocale.entries.forEach { locale ->
            assertEquals(2, locale.code.length, "${locale.name} code should be 2 chars")
        }
    }

    @Test
    fun `all locales have unique codes`() {
        val codes = AppLocale.entries.map { it.code }
        assertEquals(codes.size, codes.toSet().size, "All locale codes should be unique")
    }
}
