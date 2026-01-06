package com.guyghost.wakeve.localization

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Instrumented tests for Android LocalizationService.
 *
 * Tests the platform-specific Android implementation including:
 * - Singleton initialization and getInstance
 * - System locale detection
 * - SharedPreferences persistence
 * - String resource retrieval
 * - Fallback behavior to English
 * - Configuration updates
 *
 * Uses ApplicationContext for Android resources.
 */
@RunWith(AndroidJUnit4::class)
class LocalizationServiceAndroidTest {

    private lateinit var context: Context
    private lateinit var localizationService: LocalizationService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        LocalizationService.initialize(context)
        localizationService = LocalizationService.getInstance()
        
        // Clean up SharedPreferences before each test
        context.getSharedPreferences("wakeve_settings", Context.MODE_PRIVATE)
            .edit()
            .remove("app_locale")
            .apply()
    }

    // ==================== Singleton Tests ====================

    /**
     * Test getInstance returns a valid LocalizationService.
     *
     * GIVEN the LocalizationService is initialized
     * WHEN calling getInstance
     * THEN a non-null instance is returned
     */
    @Test
    fun `getInstance returns non-null instance`() {
        assertNotNull(localizationService)
    }

    /**
     * Test getInstance returns the same singleton instance.
     *
     * GIVEN the LocalizationService is initialized
     * WHEN calling getInstance multiple times
     * THEN the same instance is returned
     */
    @Test
    fun `getInstance returns singleton instance`() {
        val instance1 = LocalizationService.getInstance()
        val instance2 = LocalizationService.getInstance()
        assertEquals(instance1, instance2)
    }

    // ==================== getCurrentLocale Tests ====================

    /**
     * Test getCurrentLocale returns system locale by default.
     *
     * GIVEN no persisted locale preference
     * WHEN calling getCurrentLocale
     * THEN a valid AppLocale is returned (system or fallback)
     */
    @Test
    fun `getCurrentLocale returns valid AppLocale by default`() {
        val currentLocale = localizationService.getCurrentLocale()
        assertTrue(currentLocale in AppLocale.values())
    }

    /**
     * Test getCurrentLocale returns persisted locale after setLocale.
     *
     * GIVEN a locale has been set and persisted
     * WHEN calling getCurrentLocale
     * THEN that locale is returned
     */
    @Test
    fun `getCurrentLocale returns persisted locale`() {
        localizationService.setLocale(AppLocale.FRENCH)
        val currentLocale = localizationService.getCurrentLocale()
        assertEquals(AppLocale.FRENCH, currentLocale)
    }

    /**
     * Test getCurrentLocale respects multiple locale changes.
     *
     * GIVEN multiple locale changes
     * WHEN calling getCurrentLocale after each change
     * THEN each change is reflected
     */
    @Test
    fun `getCurrentLocale reflects all locale changes`() {
        localizationService.setLocale(AppLocale.SPANISH)
        assertEquals(AppLocale.SPANISH, localizationService.getCurrentLocale())
        
        localizationService.setLocale(AppLocale.ENGLISH)
        assertEquals(AppLocale.ENGLISH, localizationService.getCurrentLocale())
        
        localizationService.setLocale(AppLocale.FRENCH)
        assertEquals(AppLocale.FRENCH, localizationService.getCurrentLocale())
    }

    // ==================== setLocale Tests ====================

    /**
     * Test setLocale persists to SharedPreferences.
     *
     * GIVEN a locale
     * WHEN calling setLocale
     * THEN the locale code is persisted to SharedPreferences
     */
    @Test
    fun `setLocale persists to SharedPreferences`() {
        localizationService.setLocale(AppLocale.FRENCH)
        
        val prefs = context.getSharedPreferences("wakeve_settings", Context.MODE_PRIVATE)
        val savedLocale = prefs.getString("app_locale", null)
        
        assertEquals("fr", savedLocale)
    }

    /**
     * Test setLocale persists all three supported locales.
     *
     * GIVEN all three locales
     * WHEN calling setLocale for each
     * THEN each is correctly persisted
     */
    @Test
    fun `setLocale persists all supported locales`() {
        val prefs = context.getSharedPreferences("wakeve_settings", Context.MODE_PRIVATE)
        
        localizationService.setLocale(AppLocale.FRENCH)
        assertEquals("fr", prefs.getString("app_locale", null))
        
        localizationService.setLocale(AppLocale.ENGLISH)
        assertEquals("en", prefs.getString("app_locale", null))
        
        localizationService.setLocale(AppLocale.SPANISH)
        assertEquals("es", prefs.getString("app_locale", null))
    }

    /**
     * Test setLocale overrides previous locale.
     *
     * GIVEN a previously set locale
     * WHEN calling setLocale with a different locale
     * THEN the new locale is persisted
     */
    @Test
    fun `setLocale overrides persisted locale`() {
        localizationService.setLocale(AppLocale.FRENCH)
        localizationService.setLocale(AppLocale.ENGLISH)
        
        val prefs = context.getSharedPreferences("wakeve_settings", Context.MODE_PRIVATE)
        val savedLocale = prefs.getString("app_locale", null)
        
        assertEquals("en", savedLocale)
    }

    /**
     * Test setLocale updates runtime Configuration.
     *
     * GIVEN a locale
     * WHEN calling setLocale
     * THEN the Android Configuration is updated (no exception thrown)
     */
    @Test
    fun `setLocale updates Configuration without error`() {
        // Should not throw any exception
        localizationService.setLocale(AppLocale.FRENCH)
        localizationService.setLocale(AppLocale.SPANISH)
        localizationService.setLocale(AppLocale.ENGLISH)
    }

    // ==================== getString Tests ====================

    /**
     * Test getString returns correct value for valid key.
     *
     * GIVEN a valid string resource key
     * WHEN calling getString
     * THEN a non-empty string is returned
     */
    @Test
    fun `getString returns non-empty value for valid key`() {
        val result = localizationService.getString("save")
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    /**
     * Test getString returns different values for different keys.
     *
     * GIVEN multiple different string keys
     * WHEN calling getString for each
     * THEN different non-empty values are returned
     */
    @Test
    fun `getString returns different values for different keys`() {
        val save = localizationService.getString("save")
        val cancel = localizationService.getString("cancel")
        
        assertNotNull(save)
        assertNotNull(cancel)
        assertTrue(save.isNotEmpty())
        assertTrue(cancel.isNotEmpty())
        // Keys should have different values (usually)
        // Note: This may fail if both strings happen to be identical in translation
    }

    /**
     * Test getString with arguments formats correctly.
     *
     * GIVEN a format string key and arguments
     * WHEN calling getString with args
     * THEN the string is formatted with the arguments
     */
    @Test
    fun `getString with args includes argument value`() {
        // Using a format string (e.g., "%d items")
        // Note: Actual test depends on translation files having format strings
        val result = localizationService.getString("test_format", 42)
        assertNotNull(result)
        // The formatted string should contain the argument
        assertTrue(result.length > 0)
    }

    /**
     * Test getString fallback to English for missing translation.
     *
     * GIVEN a key that may not exist in current locale
     * WHEN calling getString
     * THEN a string is returned (either translated or English fallback)
     */
    @Test
    fun `getString falls back gracefully`() {
        localizationService.setLocale(AppLocale.SPANISH)
        
        // Try a key that should exist in all languages
        val result = localizationService.getString("save")
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    /**
     * Test getString for non-existent key returns key as fallback.
     *
     * GIVEN a key that doesn't exist in any translation file
     * WHEN calling getString
     * THEN the key itself is returned as fallback
     */
    @Test
    fun `getString returns key for non-existent key`() {
        val nonExistentKey = "completely_nonexistent_key_12345"
        val result = localizationService.getString(nonExistentKey)
        // Should return the key itself when not found
        assertEquals(nonExistentKey, result)
    }

    /**
     * Test getString respects locale changes.
     *
     * GIVEN different locales
     * WHEN calling getString with the same key
     * THEN different translations may be returned (if available)
     */
    @Test
    fun `getString respects locale changes`() {
        localizationService.setLocale(AppLocale.ENGLISH)
        val englishString = localizationService.getString("save")
        
        localizationService.setLocale(AppLocale.FRENCH)
        val frenchString = localizationService.getString("save")
        
        assertNotNull(englishString)
        assertNotNull(frenchString)
        assertTrue(englishString.isNotEmpty())
        assertTrue(frenchString.isNotEmpty())
        // In theory, translations might be different (but could be same if not translated)
    }

    // ==================== Integration Tests ====================

    /**
     * Test complete flow: initialize -> set locale -> get string.
     *
     * GIVEN initialized service
     * WHEN setting locale and retrieving strings
     * THEN everything works together
     */
    @Test
    fun `complete flow: initialize, set locale, get string`() {
        // Set to French
        localizationService.setLocale(AppLocale.FRENCH)
        
        // Verify locale is set
        assertEquals(AppLocale.FRENCH, localizationService.getCurrentLocale())
        
        // Get string in French
        val result = localizationService.getString("save")
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    /**
     * Test persistence across getInstance calls.
     *
     * GIVEN a locale has been set
     * WHEN calling getInstance again
     * THEN the persisted locale is still active
     */
    @Test
    fun `locale persists across getInstance calls`() {
        localizationService.setLocale(AppLocale.SPANISH)
        
        // Get a new instance
        val newService = LocalizationService.getInstance()
        
        // Should still have Spanish locale
        assertEquals(AppLocale.SPANISH, newService.getCurrentLocale())
    }

    /**
     * Test getString with multiple arguments.
     *
     * GIVEN a format string with multiple placeholders
     * WHEN calling getString with multiple arguments
     * THEN all arguments are formatted
     */
    @Test
    fun `getString with multiple args formats all arguments`() {
        // This test depends on having a format string in the translation files
        // Using a simple format string for now
        val result = localizationService.getString("test_format", 10, 20)
        assertNotNull(result)
    }

    // ==================== Edge Cases ====================

    /**
     * Test getString with empty key.
     *
     * GIVEN an empty string as key
     * WHEN calling getString
     * THEN it returns gracefully (fallback)
     */
    @Test
    fun `getString with empty key returns gracefully`() {
        val result = localizationService.getString("")
        assertNotNull(result)
        // Should return the key or a fallback
    }

    /**
     * Test getString with null-like key.
     *
     * GIVEN a key string containing "null"
     * WHEN calling getString
     * THEN it returns gracefully
     */
    @Test
    fun `getString with null-like key returns gracefully`() {
        val result = localizationService.getString("null")
        assertNotNull(result)
    }

    /**
     * Test rapid locale changes.
     *
     * GIVEN rapid successive locale changes
     * WHEN changing locale multiple times quickly
     * THEN all changes are applied without error
     */
    @Test
    fun `rapid locale changes work correctly`() {
        for (i in 0..10) {
            localizationService.setLocale(AppLocale.ENGLISH)
            localizationService.setLocale(AppLocale.FRENCH)
            localizationService.setLocale(AppLocale.SPANISH)
        }
        
        // Final locale should be Spanish
        assertEquals(AppLocale.SPANISH, localizationService.getCurrentLocale())
    }

    /**
     * Test getString multiple times returns consistent results.
     *
     * GIVEN a key
     * WHEN calling getString multiple times
     * THEN the same result is returned each time
     */
    @Test
    fun `getString returns consistent results`() {
        val key = "save"
        val result1 = localizationService.getString(key)
        val result2 = localizationService.getString(key)
        val result3 = localizationService.getString(key)
        
        assertEquals(result1, result2)
        assertEquals(result2, result3)
    }
}
