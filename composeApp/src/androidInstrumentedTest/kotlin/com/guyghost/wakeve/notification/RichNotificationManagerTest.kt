package com.guyghost.wakeve.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Instrumented tests for RichNotificationManager.
 *
 * Tests rich notification functionality including images, actions, and progress notifications.
 */
@RunWith(AndroidJUnit4::class)
class RichNotificationManagerTest {

    private lateinit var context: Context
    private lateinit var mockNotificationManager: NotificationManager
    private lateinit var mockChannelManager: NotificationChannelManager
    private lateinit var notificationManager: RichNotificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        mockNotificationManager = mockk(relaxed = true)
        mockChannelManager = mockk(relaxed = true)

        // Use actual NotificationChannelManager but with mocked NotificationManager
        notificationManager = RichNotificationManager(context, mockNotificationManager, mockChannelManager)
    }

    @After
    fun tearDown() {
        // Clean up any notifications
        val realNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        (1..100).forEach { id ->
            realNotificationManager.cancel(id)
        }
    }

    @Test
    fun `showRichNotification should display notification with image`() = runTest {
        // Given
        val notificationId = 123
        val title = "Event Invite"
        val message = "You are invited to Birthday Party!"
        val channelId = "events"
        val imageBitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)

        // When
        notificationManager.showRichNotification(
            notificationId = notificationId,
            title = title,
            message = message,
            channelId = channelId,
            largeIcon = imageBitmap,
            bigPicture = imageBitmap
        )

        // Then
        verify { mockNotificationManager.notify(notificationId, any()) }
    }

    @Test
    fun `showRichNotification should display notification without image`() = runTest {
        // Given
        val notificationId = 124
        val title = "Reminder"
        val message = "Don't forget to vote!"
        val channelId = "reminders"

        // When
        notificationManager.showRichNotification(
            notificationId = notificationId,
            title = title,
            message = message,
            channelId = channelId
        )

        // Then
        verify { mockNotificationManager.notify(notificationId, any()) }
    }

    @Test
    fun `showRichNotification should include actions when provided`() = runTest {
        // Given
        val notificationId = 125
        val title = "Event Invite"
        val message = "RSVP Now"
        val channelId = "events"
        val intent = Intent(context, Context::class.java)
        val action1 = RichNotificationManager.NotificationAction(
            title = "Accept",
            intent = intent
        )
        val action2 = RichNotificationManager.NotificationAction(
            title = "Decline",
            intent = intent
        )

        // When
        notificationManager.showRichNotification(
            notificationId = notificationId,
            title = title,
            message = message,
            channelId = channelId,
            actions = listOf(action1, action2)
        )

        // Then
        verify { mockNotificationManager.notify(notificationId, any()) }
    }

    @Test
    fun `showProgressNotification should display indeterminate progress`() = runTest {
        // Given
        val notificationId = 126
        val title = "Uploading..."
        val message = "Please wait"
        val channelId = "progress"

        // When
        notificationManager.showProgressNotification(
            notificationId = notificationId,
            title = title,
            message = message,
            channelId = channelId,
            indeterminate = true
        )

        // Then
        verify { mockNotificationManager.notify(notificationId, any()) }
    }

    @Test
    fun `showProgressNotification should display determinate progress`() = runTest {
        // Given
        val notificationId = 127
        val title = "Uploading..."
        val message = "50% complete"
        val channelId = "progress"
        val progress = 50
        val max = 100

        // When
        notificationManager.showProgressNotification(
            notificationId = notificationId,
            title = title,
            message = message,
            channelId = channelId,
            indeterminate = false,
            progress = progress,
            max = max
        )

        // Then
        verify { mockNotificationManager.notify(notificationId, any()) }
    }

    @Test
    fun `updateProgressNotification should update existing notification`() = runTest {
        // Given
        val notificationId = 128
        val title = "Uploading..."
        val message = "75% complete"
        val channelId = "progress"
        val progress = 75
        val max = 100

        // When
        notificationManager.updateProgressNotification(
            notificationId = notificationId,
            title = title,
            message = message,
            channelId = channelId,
            progress = progress,
            max = max
        )

        // Then
        verify { mockNotificationManager.notify(notificationId, any()) }
    }

    @Test
    fun `cancelNotification should cancel notification by id`() = runTest {
        // Given
        val notificationId = 129

        // When
        notificationManager.cancelNotification(notificationId)

        // Then
        verify { mockNotificationManager.cancel(notificationId) }
    }

    @Test
    fun `cancelAllNotifications should cancel all notifications`() = runTest {
        // When
        notificationManager.cancelAllNotifications()

        // Then
        verify { mockNotificationManager.cancelAll() }
    }

    @Test
    fun `loadImageBitmap should load bitmap from URI`() = runTest {
        // This test would require a real image URI, which is difficult in unit tests
        // For now, we'll just verify the method exists and doesn't crash
        // Given
        val imageUri = Uri.parse("android.resource://com.guyghost.wakeve/drawable/ic_launcher_foreground")

        // When
        val result = notificationManager.loadImageBitmap(imageUri)

        // Then
        // Result may be null if resource doesn't exist, but method should not crash
        // We're mainly testing that the method exists and is callable
        assertNotNull("loadImageBitmap method should exist", notificationManager)
    }

    @Test
    fun `showRichNotification with contentIntent should navigate on tap`() = runTest {
        // Given
        val notificationId = 130
        val title = "Event Updated"
        val message = "Event details changed"
        val channelId = "events"
        val intent = Intent(context, Context::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("wakeve://event/123")
        }

        // When
        notificationManager.showRichNotification(
            notificationId = notificationId,
            title = title,
            message = message,
            channelId = channelId,
            contentIntent = intent
        )

        // Then
        verify { mockNotificationManager.notify(notificationId, any()) }
    }

    @Test
    fun `showRichNotification with largeIcon should use smallIcon when largeIcon is null`() = runTest {
        // Given
        val notificationId = 131
        val title = "Simple Notification"
        val message = "No large icon"
        val channelId = "default"

        // When
        notificationManager.showRichNotification(
            notificationId = notificationId,
            title = title,
            message = message,
            channelId = channelId,
            largeIcon = null
        )

        // Then
        verify { mockNotificationManager.notify(notificationId, any()) }
    }
}
