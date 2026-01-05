package com.guyghost.wakeve.localization

import java.util.Locale
import java.util.prefs.Preferences

/**
 * JVM implementation of LocalizationService.
 *
 * Uses Java's Preferences API for persistence and Locale for system locale detection.
 * This implementation is primarily for testing and JVM-based applications.
 *
 * ## Platform Specifics
 * - **Persistence**: Java Preferences with key "app_locale"
 * - **System Locale**: Locale.getDefault().language
 * - **Fallback**: English locale when translation is missing
 */
actual class LocalizationService {

    private val preferences = Preferences.userRoot().node("com.guyghost.wakeve")

    /**
     * Returns the currently selected locale.
     *
     * First checks user preference in Preferences,
     * then falls back to system locale.
     */
    actual fun getCurrentLocale(): AppLocale {
        val userLocaleCode = preferences.get(KEY_APP_LOCALE, null)
        if (userLocaleCode != null) {
            return AppLocale.fromCode(userLocaleCode)
        }

        val systemLocale = Locale.getDefault().language
        return AppLocale.fromCode(systemLocale)
    }

    /**
     * Sets the application locale.
     *
     * Persists the choice to Preferences.
     */
    actual fun setLocale(locale: AppLocale) {
        preferences.put(KEY_APP_LOCALE, locale.code)
        preferences.flush()
    }

    /**
     * Returns a translated string for the given key.
     *
     * Falls back to English if the key is not found.
     * In JVM/test environment, we return the key as-is since
     * string resources are typically handled at the UI layer.
     */
    actual fun getString(key: String): String {
        // In a real JVM application, this would load from resource bundles
        // For now, return the key as fallback
        return try {
            val bundle = java.util.ResourceBundle.getBundle(
                "strings",
                Locale(getCurrentLocale().code)
            )
            bundle.getString(key)
        } catch (e: Exception) {
            // Fallback to English
            try {
                val englishBundle = java.util.ResourceBundle.getBundle(
                    "strings",
                    Locale.ENGLISH
                )
                englishBundle.getString(key)
            } catch (e: Exception) {
                key
            }
        }
    }

    /**
     * Returns a formatted translated string with arguments.
     */
    actual fun getString(key: String, vararg args: Any): String {
        val format = getString(key)
        return String.format(format, *args)
    }

    actual companion object {
        private const val KEY_APP_LOCALE = "app_locale"

        @Volatile
        private var instance: LocalizationService? = null

        /**
         * Returns the singleton LocalizationService instance.
         *
         * @return The initialized [LocalizationService] instance
         */
        actual fun getInstance(): LocalizationService {
            return instance ?: synchronized(this) {
                instance ?: LocalizationService().also { instance = it }
            }
        }

        /**
         * Initialize the singleton with application context.
         * No-op on JVM.
         *
         * @param context Unused on JVM
         */
        actual fun initialize(context: Any?) {
            // No-op on JVM
        }
    }
}
