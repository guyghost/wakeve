package com.guyghost.wakeve.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

/**
 * Helper object for managing WorkManager background tasks.
 *
 * Provides methods to schedule and cancel periodic background work for:
 * - Token refresh
 * - Session cleanup
 * - Sync operations
 */
object WorkManagerHelper {

    private const val TOKEN_REFRESH_WORK_NAME = "token_refresh_periodic"

    /**
     * Schedule periodic token refresh worker.
     *
     * This worker runs every 15 minutes (minimum for periodic work) when:
     * - Device has network connectivity
     * - Battery is not in low power mode
     *
     * @param context Application context
     */
    fun scheduleTokenRefresh(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val refreshWorkRequest = PeriodicWorkRequestBuilder<TokenRefreshWorker>(
            repeatInterval = 15, // Minimum interval for periodic work
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(TOKEN_REFRESH_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TOKEN_REFRESH_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
            refreshWorkRequest
        )
    }

    /**
     * Cancel token refresh worker.
     *
     * Call this when user logs out to stop background token refresh.
     *
     * @param context Application context
     */
    fun cancelTokenRefresh(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TOKEN_REFRESH_WORK_NAME)
    }

    /**
     * Cancel all background work.
     *
     * @param context Application context
     */
    fun cancelAllWork(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }

    /**
     * Get token refresh work info.
     *
     * Useful for debugging and monitoring.
     *
     * @param context Application context
     * @return LiveData of work info list
     */
    fun getTokenRefreshWorkInfo(context: Context) =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(TOKEN_REFRESH_WORK_NAME)
}
