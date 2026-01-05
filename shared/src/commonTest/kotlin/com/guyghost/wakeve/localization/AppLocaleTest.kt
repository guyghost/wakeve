package com.guyghost.wakeve.localization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [AppLocale] enum.
 *
 * Tests the pure functional model for locales, including:
 * - Enum values and properties
 * - fromCode lookup function
 * - Fallback behavior
 * - Default English behavior
 *
 * All tests are pure (no side effects).
 */
class AppLocaleTest {

    // ==================== fromCode Tests ====================

    /**
     * Test that fromCode returns FRENCH for "fr" code.
     *
     * GIVEN a valid French code
     * WHEN calling fromCode
     * THEN FRENCH locale is returned
     */
    @Test
    fun `fromCode returns FRENCH for fr code`() {
        val result = AppLocale.fromCode("fr")
        assertEquals(AppLocale.FRENCH, result)
    }

    /**
     * Test that fromCode returns ENGLISH for "en" code.
     *
     * GIVEN a valid English code
     * WHEN calling fromCode
     * THEN ENGLISH locale is returned
     */
    @Test
    fun `fromCode returns ENGLISH for en code`() {
        val result = AppLocale.fromCode("en")
        assertEquals(AppLocale.ENGLISH, result)
    }

    /**
     * Test that fromCode returns SPANISH for "es" code.
     *
     * GIVEN a valid Spanish code
     * WHEN calling fromCode
     * THEN SPANISH locale is returned
     */
    @Test
    fun `fromCode returns SPANISH for es code`() {
        val result = AppLocale.fromCode("es")
        assertEquals(AppLocale.SPANISH, result)
    }

    /**
     * Test fallback to English for unknown code.
     *
     * GIVEN an unknown language code (e.g., "de" for German)
     * WHEN calling fromCode
     * THEN ENGLISH is returned as fallback
     */
    @Test
    fun `fromCode returns ENGLISH for unknown code`() {
        val result = AppLocale.fromCode("de")
        assertEquals(AppLocale.ENGLISH, result)
    }

    /**
     * Test fallback to English for empty code.
     *
     * GIVEN an empty string
     * WHEN calling fromCode
     * THEN ENGLISH is returned as fallback
     */
    @Test
    fun `fromCode returns ENGLISH for empty code`() {
        val result = AppLocale.fromCode("")
        assertEquals(AppLocale.ENGLISH, result)
    }

    /**
     * Test fallback to English for null-like string.
     *
     * GIVEN a "null" string
     * WHEN calling fromCode
     * THEN ENGLISH is returned as fallback
     */
    @Test
    fun `fromCode returns ENGLISH for null string`() {
        val result = AppLocale.fromCode("null")
        assertEquals(AppLocale.ENGLISH, result)
    }

    /**
     * Test fallback to English for random invalid code.
     *
     * GIVEN random invalid codes
     * WHEN calling fromCode
     * THEN ENGLISH is returned as fallback
     */
    @Test
    fun `fromCode returns ENGLISH for random invalid codes`() {
        val invalidCodes = listOf("xyz", "123", "pt", "it", "ru", "ja")
        invalidCodes.forEach { code ->
            val result = AppLocale.fromCode(code)
            assertEquals(AppLocale.ENGLISH, result, "Failed for code: $code")
        }
    }

    /**
     * Test case-sensitive fromCode (exact match required).
     *
     * GIVEN uppercase language codes
     * WHEN calling fromCode
     * THEN ENGLISH is returned as fallback (case-sensitive)
     */
    @Test
    fun `fromCode is case-sensitive and requires lowercase`() {
        assertEquals(AppLocale.ENGLISH, AppLocale.fromCode("FR"))
        assertEquals(AppLocale.ENGLISH, AppLocale.fromCode("Fr"))
        assertEquals(AppLocale.ENGLISH, AppLocale.fromCode("EN"))
        assertEquals(AppLocale.ENGLISH, AppLocale.fromCode("Es"))
    }

    // ==================== Enum Properties Tests ====================

    /**
     * Test FRENCH locale properties.
     *
     * GIVEN the FRENCH enum value
     * WHEN accessing its properties
     * THEN code is "fr" and displayName is "Français"
     */
    @Test
    fun `FRENCH has correct code and displayName`() {
        assertEquals("fr", AppLocale.FRENCH.code)
        assertEquals("Français", AppLocale.FRENCH.displayName)
    }

    /**
     * Test ENGLISH locale properties.
     *
     * GIVEN the ENGLISH enum value
     * WHEN accessing its properties
     * THEN code is "en" and displayName is "English"
     */
    @Test
    fun `ENGLISH has correct code and displayName`() {
        assertEquals("en", AppLocale.ENGLISH.code)
        assertEquals("English", AppLocale.ENGLISH.displayName)
    }

    /**
     * Test SPANISH locale properties.
     *
     * GIVEN the SPANISH enum value
     * WHEN accessing its properties
     * THEN code is "es" and displayName is "Español"
     */
    @Test
    fun `SPANISH has correct code and displayName`() {
        assertEquals("es", AppLocale.SPANISH.code)
        assertEquals("Español", AppLocale.SPANISH.displayName)
    }

    // ==================== Uniqueness Tests ====================

    /**
     * Test that all locales have unique codes.
     *
     * GIVEN all locales
     * WHEN checking for duplicate codes
     * THEN all codes are unique
     */
    @Test
    fun `all locales have unique codes`() {
        val codes = AppLocale.values().map { it.code }
        val uniqueCodes = codes.toSet()
        assertEquals(
            codes.size,
            uniqueCodes.size,
            "Found duplicate codes: $codes"
        )
    }

    /**
     * Test that all locales have unique display names.
     *
     * GIVEN all locales
     * WHEN checking for duplicate display names
     * THEN all display names are unique
     */
    @Test
    fun `all locales have unique displayNames`() {
        val displayNames = AppLocale.values().map { it.displayName }
        val uniqueNames = displayNames.toSet()
        assertEquals(
            displayNames.size,
            uniqueNames.size,
            "Found duplicate display names: $displayNames"
        )
    }

    // ==================== Enumeration Tests ====================

    /**
     * Test that the enum has exactly 3 values.
     *
     * GIVEN the AppLocale enum
     * WHEN counting values
     * THEN exactly 3 locales exist
     */
    @Test
    fun `enum has exactly 3 values`() {
        assertEquals(3, AppLocale.values().size)
    }

    /**
     * Test that ENGLISH, FRENCH, and SPANISH are present.
     *
     * GIVEN the AppLocale enum
     * WHEN checking for expected values
     * THEN all three locales are present
     */
    @Test
    fun `enum contains expected locales`() {
        val locales = AppLocale.values().toList()
        assertTrue(locales.contains(AppLocale.ENGLISH))
        assertTrue(locales.contains(AppLocale.FRENCH))
        assertTrue(locales.contains(AppLocale.SPANISH))
    }

    // ==================== Inverse Lookup Tests ====================

    /**
     * Test round-trip: code -> locale -> code.
     *
     * GIVEN a valid code
     * WHEN looking up the locale and getting its code back
     * THEN the codes match
     */
    @Test
    fun `round-trip code lookup preserves code`() {
        val codes = listOf("fr", "en", "es")
        codes.forEach { originalCode ->
            val locale = AppLocale.fromCode(originalCode)
            val resultCode = locale.code
            assertEquals(originalCode, resultCode)
        }
    }

    /**
     * Test round-trip: locale -> code -> locale.
     *
     * GIVEN all locales
     * WHEN converting to code and back
     * THEN the locales match
     */
    @Test
    fun `round-trip locale lookup preserves locale`() {
        AppLocale.values().forEach { originalLocale ->
            val locale = AppLocale.fromCode(originalLocale.code)
            assertEquals(originalLocale, locale)
        }
    }

    // ==================== Default Locale Tests ====================

    /**
     * Test that ENGLISH is the default/fallback locale.
     *
     * GIVEN the fallback behavior
     * WHEN checking for invalid codes
     * THEN ENGLISH is always returned
     */
    @Test
    fun `ENGLISH is the default fallback locale`() {
        // Unknown codes should fallback to ENGLISH
        val unknownCodes = listOf("", "null", "de", "it", "pt", "ru", "ar", "zh")
        unknownCodes.forEach { code ->
            val result = AppLocale.fromCode(code)
            assertEquals(AppLocale.ENGLISH, result)
        }
    }

    // ==================== Edge Cases ====================

    /**
     * Test with whitespace in code.
     *
     * GIVEN codes with whitespace
     * WHEN calling fromCode
     * THEN ENGLISH is returned (whitespace doesn't match)
     */
    @Test
    fun `fromCode with whitespace returns ENGLISH`() {
        assertEquals(AppLocale.ENGLISH, AppLocale.fromCode(" fr "))
        assertEquals(AppLocale.ENGLISH, AppLocale.fromCode("fr "))
        assertEquals(AppLocale.ENGLISH, AppLocale.fromCode(" en"))
    }

    /**
     * Test with very long code.
     *
     * GIVEN a very long code string
     * WHEN calling fromCode
     * THEN ENGLISH is returned
     */
    @Test
    fun `fromCode with very long code returns ENGLISH`() {
        val longCode = "fr".repeat(100)
        val result = AppLocale.fromCode(longCode)
        assertEquals(AppLocale.ENGLISH, result)
    }

    /**
     * Test with special characters in code.
     *
     * GIVEN codes with special characters
     * WHEN calling fromCode
     * THEN ENGLISH is returned
     */
    @Test
    fun `fromCode with special characters returns ENGLISH`() {
        assertEquals(AppLocale.ENGLISH, AppLocale.fromCode("fr-FR"))
        assertEquals(AppLocale.ENGLISH, AppLocale.fromCode("fr_FR"))
        assertEquals(AppLocale.ENGLISH, AppLocale.fromCode("fr@"))
    }
}
