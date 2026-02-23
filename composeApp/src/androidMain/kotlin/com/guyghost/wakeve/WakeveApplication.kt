package com.guyghost.wakeve

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.guyghost.wakeve.notification.NotificationChannelManager
import com.guyghost.wakeve.service.FCMService
import com.guyghost.wakeve.workers.WorkManagerHelper
import com.guyghost.wakeve.BuildConfig

/**
 * Wakeve Android Application class.
 *
 * Handles application-level initialization including:
 * - WorkManager for background token refresh
 * - Coil ImageLoader configuration with memory and disk caching
 * - Notification channels for push notifications
 * - FCM token management
 */
class WakeveApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // Create notification channels (required for Android 8.0+)
        // Must be done early, before any notification is sent
        NotificationChannelManager(this).createAllChannels()

        // Initialize WorkManager for background tasks
        // Note: WorkManager initialization happens automatically via androidx.startup
        // We just need to schedule our periodic work here

        // Schedule periodic token refresh
        // This will be started when user logs in and cancelled when user logs out
        // For now, we'll schedule it here to ensure it's set up
        // The actual scheduling should happen after login
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024) // 50MB
                    .build()
            }
            .crossfade(true)
            .crossfade(200) // 200ms animation
            .respectCacheHeaders(false)
            // Debug logger enabled for debug builds
            // Note: Uncomment when BuildConfig is properly configured
            // .apply {
            //     if (BuildConfig.DEBUG) {
            //         logger(DebugLogger())
            //     }
            // }
            .build()
    }

    /**
     * Called when user logs in.
     * Schedule background token refresh and register FCM token with backend.
     *
     * @param accessToken The JWT access token for backend API calls
     */
    fun onUserLoggedIn(accessToken: String? = null) {
        WorkManagerHelper.scheduleTokenRefresh(this)

        // Register FCM token with backend now that user is authenticated
        accessToken?.let { token ->
            // Store access token for FCMService to use on token refresh
            getSharedPreferences("wakeve_prefs", MODE_PRIVATE)
                .edit()
                .putString("access_token", token)
                .apply()

            FCMService.registerStoredTokenWithBackend(this, token)
        }
    }

    /**
     * Called when user logs out.
     * Cancel background token refresh and unregister FCM token.
     */
    fun onUserLoggedOut() {
        WorkManagerHelper.cancelTokenRefresh(this)

        // Unregister FCM token from backend
        val accessToken = getSharedPreferences("wakeve_prefs", MODE_PRIVATE)
            .getString("access_token", null)
        accessToken?.let {
            FCMService.unregisterTokenFromBackend(it)
        }

        // Clear stored access token
        getSharedPreferences("wakeve_prefs", MODE_PRIVATE)
            .edit()
            .remove("access_token")
            .apply()
    }
}
