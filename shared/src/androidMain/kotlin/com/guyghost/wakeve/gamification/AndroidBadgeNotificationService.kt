package com.guyghost.wakeve.gamification

import android.util.Log

/**
 * Android implementation stub of BadgeNotificationService.
 * 
 * Note: Full Android notification implementation requires access to Android resources
 * (R.string.*) which are not available in the shared module.
 * 
 * This stub implementation logs all operations but does not show actual notifications.
 * For full functionality, move this class to the composeApp module where Android
 * resources are accessible.
 *
 * Architecture: This file is in androidMain (Shell) and provides platform-specific
 * implementation. It cannot access app resources from shared module.
 */
class AndroidBadgeNotificationService : BadgeNotificationService {

    companion object {
        private const val TAG = "AndroidBadgeNotif"
    }

    init {
        Log.d(TAG, "AndroidBadgeNotificationService initialized (stub implementation)")
    }

    /**
     * Stub implementation - logs badge notification but does not display.
     */
    override suspend fun showBadgeUnlockedNotification(
        userId: String,
        badge: Badge,
        pointsEarned: Int
    ) {
        Log.d(TAG, "STUB: Badge unlocked - ${badge.name} for user $userId ($pointsEarned points)")
        // TODO: Implement full notification in composeApp module
        // Requires: R.string.notification_channel_badges_name
    }

    /**
     * Stub implementation - logs points notification but does not display.
     */
    override suspend fun showPointsEarnedNotification(
        userId: String,
        points: Int,
        action: String
    ) {
        Log.d(TAG, "STUB: Points earned - $points points for $action by user $userId")
        // TODO: Implement full notification in composeApp module
    }

    /**
     * Stub implementation - logs error notification but does not display.
     */
    override suspend fun showVoiceAssistantError(
        userId: String,
        error: String
    ) {
        Log.d(TAG, "STUB: Voice assistant error for user $userId: $error")
        // TODO: Implement full notification in composeApp module
    }

    /**
     * Stub - always returns true since we can't check actual permissions.
     */
    override suspend fun requestNotificationPermission(): Boolean {
        Log.d(TAG, "STUB: Permission request called")
        // TODO: Implement actual permission check in composeApp module
        return true
    }

    /**
     * Stub - always returns true since we can't check actual permissions.
     */
    override fun isNotificationEnabled(): Boolean {
        Log.d(TAG, "STUB: Checking notification enabled status")
        // TODO: Implement actual permission check in composeApp module
        return true
    }
}
