package com.guyghost.wakeve

import android.app.Application
import com.guyghost.wakeve.workers.WorkManagerHelper

/**
 * Wakeve Android Application class.
 *
 * Handles application-level initialization including:
 * - WorkManager for background token refresh
 * - Crash reporting (future)
 * - Analytics (future)
 */
class WakevApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize WorkManager for background tasks
        // Note: WorkManager initialization happens automatically via androidx.startup
        // We just need to schedule our periodic work here

        // Schedule periodic token refresh
        // This will be started when user logs in and cancelled when user logs out
        // For now, we'll schedule it here to ensure it's set up
        // The actual scheduling should happen after login
    }

    /**
     * Called when user logs in.
     * Schedule background token refresh.
     */
    fun onUserLoggedIn() {
        WorkManagerHelper.scheduleTokenRefresh(this)
    }

    /**
     * Called when user logs out.
     * Cancel background token refresh.
     */
    fun onUserLoggedOut() {
        WorkManagerHelper.cancelTokenRefresh(this)
    }
}
