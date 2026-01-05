package com.guyghost.wakeve.localization

/**
 * Enum of locales supported by the application.
 *
 * @property code The ISO 639-1 language code (e.g., "fr", "en", "es")
 * @property displayName The human-readable display name of the locale
 */
enum class AppLocale(
    val code: String,
    val displayName: String
) {
    /** French locale - Français */
    FRENCH("fr", "Français"),

    /** English locale - English (default fallback language) */
    ENGLISH("en", "English"),

    /** Spanish locale - Español */
    SPANISH("es", "Español");

    companion object {
        /**
         * Returns the AppLocale for the given language code.
         * Defaults to [ENGLISH] if the code is not recognized.
         *
         * Pure function - no side effects.
         *
         * @param code The ISO 639-1 language code to look up
         * @return The corresponding AppLocale or ENGLISH as fallback
         */
        fun fromCode(code: String): AppLocale {
            return values().find { it.code == code } ?: ENGLISH
        }
    }
}
