package com.guyghost.wakeve.localization

import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

/**
 * iOS implementation of LocalizationService.
 *
 * Uses iOS's standard NSBundle system (Localizable.strings) and NSUserDefaults
 * for persistence.
 *
 * ## Platform Specifics
 * - **Persistence**: NSUserDefaults with key "app_locale"
 * - **Resources**: Localizable.strings files in .lproj bundles
 * - **Fallback**: English .lproj bundle when translation is missing
 * - **Note**: iOS requires app restart to apply language changes
 */
actual class LocalizationService {

    private val userDefaults = NSUserDefaults.standardUserDefaults

    /**
     * Returns the currently selected locale.
     *
     * First checks user preference in NSUserDefaults,
     * then falls back to system locale.
     */
    actual fun getCurrentLocale(): AppLocale {
        val userLocaleCode = userDefaults.stringForKey(KEY_APP_LOCALE)
        if (userLocaleCode != null) {
            return AppLocale.fromCode(userLocaleCode)
        }

        // Get system language from preferred languages
        val systemLocale = NSLocale.preferredLanguages.firstOrNull() as? String
        return if (systemLocale != null) {
            // Use the full locale identifier (e.g., "en-US", "fr-FR")
            AppLocale.fromCode(systemLocale)
        } else {
            AppLocale.ENGLISH
        }
    }

    /**
     * Sets the application locale.
     *
     * Persists the choice to NSUserDefaults.
     * Note: iOS requires app restart to apply the language change.
     */
    actual fun setLocale(locale: AppLocale) {
        userDefaults.setObject(locale.code, forKey = KEY_APP_LOCALE)
    }

    /**
     * Returns a translated string for the given key.
     *
     * Falls back to English if the key is not found.
     * When a key is not found, NSBundle returns the key itself.
     */
    actual fun getString(key: String): String {
        val result = NSBundle.mainBundle.localizedStringForKey(
            key = key,
            value = null,
            table = null
        )

        return if (result == key) {
            // Key not found, try English bundle
            getEnglishString(key)
        } else {
            result
        }
    }

    /**
     * Returns a formatted translated string with arguments.
     * Note: String formatting with arguments is simplified on iOS.
     */
    actual fun getString(key: String, vararg args: Any): String {
        val format = getString(key)
        // Simple format replacement - for production, use NSString.stringWithFormat properly
        return if (args.isEmpty()) {
            format
        } else {
            try {
                var result = format
                for (arg in args) {
                    result = replaceFirstString(result, "%s", arg.toString())
                }
                result
            } catch (e: Exception) {
                format
            }
        }
    }

    /**
     * Simple string replacement helper.
     */
    private fun replaceFirstString(source: String, target: String, replacement: String): String {
        val index = source.indexOf(target)
        return if (index >= 0) {
            source.substring(0, index) + replacement + source.substring(index + target.length)
        } else {
            source
        }
    }

    /**
     * Retrieves a string from English bundle as fallback.
     */
    private fun getEnglishString(key: String): String {
        return try {
            val englishBundlePath = NSBundle.mainBundle.pathForResource(
                "en",
                ofType = "lproj"
            )

            if (englishBundlePath != null) {
                val englishBundle = NSBundle(path = englishBundlePath)
                englishBundle.localizedStringForKey(
                    key = key,
                    value = key,
                    table = null
                )
            } else {
                key
            }
        } catch (e: Exception) {
            key
        }
    }

    actual companion object {
        private const val KEY_APP_LOCALE = "app_locale"

        private var instance: LocalizationService? = null

        /**
         * Initialize the singleton. No-op on iOS.
         */
        actual fun initialize(context: Any?) {
            // No initialization needed on iOS
        }

        /**
         * Returns the singleton LocalizationService instance.
         */
        actual fun getInstance(): LocalizationService {
            return instance ?: LocalizationService().also { instance = it }
        }
    }
}
