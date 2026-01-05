package com.guyghost.wakeve.localization

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * Android implementation of LocalizationService.
 *
 * Uses Android's standard resources system (strings.xml) and SharedPreferences
 * for persistence.
 *
 * ## Platform Specifics
 * - **Persistence**: SharedPreferences with key "app_locale"
 * - **Resources**: Standard Android strings.xml resources
 * - **Configuration**: Uses [Configuration.setLocale] for runtime locale changes
 * - **Fallback**: English locale resources when translation is missing
 */
actual class LocalizationService(
    private val context: Context
) {

    private val preferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns the currently selected locale.
     *
     * First checks user preference in SharedPreferences,
     * then falls back to system locale.
     */
    actual fun getCurrentLocale(): AppLocale {
        val userLocaleCode = preferences.getString(KEY_APP_LOCALE, null)
        if (userLocaleCode != null) {
            return AppLocale.fromCode(userLocaleCode)
        }

        val systemLocale = context.resources.configuration.locales[0].language
        return AppLocale.fromCode(systemLocale)
    }

    /**
     * Sets the application locale.
     *
     * Updates the runtime configuration and persists the choice
     * to SharedPreferences.
     */
    actual fun setLocale(locale: AppLocale) {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale(locale.code))

        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(
            config,
            context.resources.displayMetrics
        )

        preferences.edit()
            .putString(KEY_APP_LOCALE, locale.code)
            .apply()
    }

    /**
     * Returns a translated string for the given key.
     *
     * Falls back to English if the key is not found.
     */
    actual fun getString(key: String): String {
        return try {
            val resourceId = context.resources.getIdentifier(
                key,
                "string",
                context.packageName
            )
            if (resourceId != 0) {
                context.getString(resourceId)
            } else {
                getEnglishString(key)
            }
        } catch (e: Exception) {
            getEnglishString(key)
        }
    }

    /**
     * Returns a formatted translated string with arguments.
     */
    actual fun getString(key: String, vararg args: Any): String {
        val format = getString(key)
        return String.format(format, *args)
    }

    /**
     * Retrieves a string from English resources as fallback.
     */
    private fun getEnglishString(key: String): String {
        return try {
            val englishConfig = Configuration(context.resources.configuration)
            englishConfig.setLocale(Locale.ENGLISH)
            val englishResources = Resources(
                context.assets,
                context.resources.displayMetrics,
                englishConfig
            )

            @Suppress("DEPRECATION")
            englishResources.updateConfiguration(
                englishConfig,
                context.resources.displayMetrics
            )

            val resourceId = englishResources.getIdentifier(
                key,
                "string",
                context.packageName
            )

            if (resourceId != 0) {
                englishResources.getString(resourceId)
            } else {
                key
            }
        } catch (e: Exception) {
            key
        }
    }

    actual companion object {
        private const val PREFS_NAME = "wakeve_settings"
        private const val KEY_APP_LOCALE = "app_locale"

        @Volatile
        private var instance: LocalizationService? = null

        private lateinit var applicationContext: Context

        /**
         * Initialize the singleton with the application context.
         * Must be called before [getInstance].
         *
         * @param context The Android application context
         */
        actual fun initialize(context: Any?) {
            @Suppress("UNCHECKED_CAST")
            applicationContext = context as Context
        }

        /**
         * Returns the singleton LocalizationService instance.
         *
         * @return The initialized [LocalizationService] instance
         * @throws IllegalStateException If [initialize] was not called first
         */
        actual fun getInstance(): LocalizationService {
            return instance ?: synchronized(this) {
                instance ?: check(::applicationContext.isInitialized) {
                    "LocalizationService must be initialized with initialize(context) before getInstance()"
                }.let {
                    LocalizationService(applicationContext).also { instance = it }
                }
            }
        }
    }
}

/**
 * Extension property to check if late-init variable is initialized.
 */
private val Any?.isInitialized: Boolean
    get() = this != null
