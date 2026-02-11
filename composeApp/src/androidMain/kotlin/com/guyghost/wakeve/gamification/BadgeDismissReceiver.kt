package com.guyghost.wakeve.gamification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.guyghost.wakeve.gamification.AndroidBadgeNotificationService

/**
 * Broadcast receiver for handling badge notification dismiss actions.
 * Receives the dismiss action broadcast and clears the notification.
 *
 * This receiver must be registered in the AndroidManifest.xml:
 * ```xml
 * <receiver
 *     android:name=".gamification.BadgeDismissReceiver"
 *     android:exported="false">
 *     <intent-filter>
 *         <action android:name="com.guyghost.wakeve.DISMISS_BADGE" />
 *     </intent-filter>
 * </receiver>
 * ```
 */
class BadgeDismissReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "com.guyghost.wakeve.DISMISS_BADGE"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_DISMISS) return

        val notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID) ?: return

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.cancel(notificationId.hashCode())
            
            // Optionally update badge count if this was the last notification
            // This would require tracking active notifications
        } catch (e: SecurityException) {
            // Notification permission not granted - silently ignore
        } catch (e: Exception) {
            // Log error in production
        }
    }
}

/**
 * Builder helper for creating dismiss intents.
 */
object BadgeDismissIntentBuilder {
    
    /**
     * Creates an intent to dismiss a specific badge notification.
     *
     * @param context The application context
     * @param notificationId The ID of the notification to dismiss
     * @return Intent configured to dismiss the notification
     */
    fun createDismissIntent(context: Context, notificationId: String): Intent {
        return Intent(context, BadgeDismissReceiver::class.java).apply {
            action = BadgeDismissReceiver.ACTION_DISMISS
            putExtra(BadgeDismissReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
    }
}
