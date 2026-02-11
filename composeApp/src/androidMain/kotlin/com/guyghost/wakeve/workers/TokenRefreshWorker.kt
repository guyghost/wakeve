package com.guyghost.wakeve.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.guyghost.wakeve.auth.AndroidAuthenticationService
import com.guyghost.wakeve.security.AndroidSecureTokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker for refreshing authentication tokens.
 *
 * This WorkManager worker ensures tokens are refreshed even when the app
 * is in the background or not running. Runs periodically to check and
 * refresh tokens before they expire.
 *
 * Schedule this worker with PeriodicWorkRequest:
 * ```kotlin
 * val refreshWork = PeriodicWorkRequestBuilder<TokenRefreshWorker>(
 *     15, TimeUnit.MINUTES // Minimum interval for periodic work
 * ).setConstraints(
 *     Constraints.Builder()
 *         .setRequiredNetworkType(NetworkType.CONNECTED)
 *         .build()
 * ).build()
 * ```
 */
class TokenRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Initialize services
            val secureStorage = AndroidSecureTokenStorage(applicationContext)
            val authService = AndroidAuthenticationService(applicationContext)

            // Check if user is authenticated
            if (!authService.isLoggedIn()) {
                // User not authenticated, no need to refresh
                return@withContext Result.success()
            }

            // Check if token is still valid
            if (!secureStorage.hasValidToken()) {
                // Token expired, cannot refresh in background
                return@withContext Result.failure()
            }

            // Check if token needs refresh (expires in less than 10 minutes)
            val expiryTimestamp = secureStorage.getTokenExpiry()
            val currentTime = System.currentTimeMillis()

            if (expiryTimestamp != null && (expiryTimestamp - currentTime) < 10 * 60 * 1000) {
                // Attempt to refresh token
                authService.refreshToken()
                    .onSuccess {
                        // Token refreshed successfully
                        return@withContext Result.success()
                    }
                    .onFailure { error ->
                        // Token refresh failed - retry with backoff
                        return@withContext Result.retry()
                    }
            }

            // Token doesn't need refresh yet
            Result.success()

        } catch (e: Exception) {
            // Worker failed, retry with backoff
            Result.retry()
        }
    }
}
