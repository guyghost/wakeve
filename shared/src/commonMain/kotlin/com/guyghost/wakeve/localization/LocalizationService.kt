package com.guyghost.wakeve.localization

/**
 * Localization service (expect interface for expect/actual pattern).
 *
 * This service provides a cross-platform interface for localization,
 * with platform-specific implementations for Android and iOS.
 *
 * The service handles:
 * - Getting and setting the current locale
 * - Retrieving translated strings by key
 * - Formatting strings with arguments
 *
 * ## Functional Core & Imperative Shell Pattern
 * - **Core**: [AppLocale] enum - pure model with no side effects
 * - **Shell**: This service - handles I/O and platform-specific behavior
 *
 * @see [LocalizationService.android.kt] for Android implementation
 * @see [LocalizationService.ios.kt] for iOS implementation
 */
expect class LocalizationService {

    /**
     * Returns the currently selected locale.
     *
     * If the user has manually selected a locale, it returns that.
     * Otherwise, it returns the system locale (with fallback to English).
     *
     * @return The currently active [AppLocale]
     */
    fun getCurrentLocale(): AppLocale

    /**
     * Sets the application locale.
     *
     * This persists the choice and updates the UI language.
     * Note: On iOS, the app may need to be restarted for changes to take effect.
     *
     * @param locale The [AppLocale] to set as the current locale
     */
    fun setLocale(locale: AppLocale)

    /**
     * Returns a translated string for the given key.
     *
     * Falls back to English if the string does not exist in the current locale.
     *
     * @param key The string resource key (e.g., "welcome_message", "common.cancel")
     * @return The translated string in the current locale, or English fallback
     */
    fun getString(key: String): String

    /**
     * Returns a formatted translated string with arguments.
     *
     * Falls back to English if the string does not exist in the current locale.
     *
     * @param key The string resource key (e.g., "greeting.name", "event.count")
     * @param args The arguments to format into the string using %s placeholders
     * @return The formatted translated string in the current locale, or English fallback
     */
    fun getString(key: String, vararg args: Any): String

    companion object {
        /**
         * Returns the LocalizationService singleton instance.
         *
         * The instance is platform-specific and initialized on first access.
         * On Android, [initialize] must be called first with the application context.
         *
         * @return The singleton [LocalizationService] instance
         * @throws IllegalStateException On Android if [initialize] was not called first
         */
        fun getInstance(): LocalizationService

        /**
         * Initializes the LocalizationService with platform-specific resources.
         *
         * Required on Android before calling [getInstance].
         * No-op on iOS.
         *
         * @param context The Android application context
         */
        fun initialize(context: Any?)
    }
}
