package com.guyghost.wakeve.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.mockito.kotlin.*

/**
 * Instrumented tests for NotificationChannelManager.
 *
 * Tests the creation and configuration of notification channels for Android O+.
 */
@RunWith(AndroidJUnit4::class)
class NotificationChannelManagerTest {

    private lateinit var context: Context
    private lateinit var mockNotificationManager: NotificationManager
    private lateinit var channelManager: NotificationChannelManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        mockNotificationManager = mock()
        channelManager = NotificationChannelManager(context, mockNotificationManager)
    }

    @After
    fun tearDown() {
        // Clean up any created channels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val realNotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            NotificationChannelManager.ChannelId.values().forEach { channelId ->
                realNotificationManager.deleteNotificationChannel(channelId.id)
            }
        }
    }

    @Test
    fun `createAllChannels should create all defined channels on Android O+`() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // When
            channelManager.createAllChannels()

            // Then
            NotificationChannelManager.ChannelId.values().forEach { channelId ->
                verify(mockNotificationManager, atLeastOnce())
                    .createNotificationChannel(check { channel ->
                        assertEquals(channelId.id, channel.id)
                    })
            }
        }
    }

    @Test
    fun `createAllChannels should configure default channel with correct settings`() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // When
            channelManager.createAllChannels()

            // Then
            verify(mockNotificationManager).createNotificationChannel(check { channel ->
                assertEquals(NotificationChannelManager.ChannelId.DEFAULT.id, channel.id)
                assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
                assertTrue(channel.shouldVibrate())
                assertFalse(channel.shouldShowBadge())
            })
        }
    }

    @Test
    fun `createAllChannels should configure high priority channel with correct settings`() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // When
            channelManager.createAllChannels()

            // Then
            verify(mockNotificationManager).createNotificationChannel(check { channel ->
                assertEquals(NotificationChannelManager.ChannelId.HIGH_PRIORITY.id, channel.id)
                assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
                assertTrue(channel.shouldVibrate())
                assertTrue(channel.shouldShowBadge())
            })
        }
    }

    @Test
    fun `createAllChannels should configure events channel with correct settings`() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // When
            channelManager.createAllChannels()

            // Then
            verify(mockNotificationManager).createNotificationChannel(check { channel ->
                assertEquals(NotificationChannelManager.ChannelId.EVENTS.id, channel.id)
                assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
                assertTrue(channel.shouldVibrate())
                assertTrue(channel.shouldShowBadge())
            })
        }
    }

    @Test
    fun `createAllChannels should configure reminders channel with correct settings`() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // When
            channelManager.createAllChannels()

            // Then
            verify(mockNotificationManager).createNotificationChannel(check { channel ->
                assertEquals(NotificationChannelManager.ChannelId.REMINDERS.id, channel.id)
                assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
                assertTrue(channel.shouldVibrate())
                assertTrue(channel.shouldShowBadge())
            })
        }
    }

    @Test
    fun `createAllChannels should configure progress channel with correct settings`() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // When
            channelManager.createAllChannels()

            // Then
            verify(mockNotificationManager).createNotificationChannel(check { channel ->
                assertEquals(NotificationChannelManager.ChannelId.PROGRESS.id, channel.id)
                assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
                assertFalse(channel.shouldVibrate())
                assertFalse(channel.shouldShowBadge())
            })
        }
    }

    @Test
    fun `getChannelId should return correct channel ID for notification type`() {
        // Then
        assertEquals(
            NotificationChannelManager.ChannelId.HIGH_PRIORITY,
            NotificationChannelManager.getChannelId(com.guyghost.wakeve.notification.NotificationType.EVENT_INVITE)
        )
        assertEquals(
            NotificationChannelManager.ChannelId.REMINDERS,
            NotificationChannelManager.getChannelId(com.guyghost.wakeve.notification.NotificationType.VOTE_REMINDER)
        )
        assertEquals(
            NotificationChannelManager.ChannelId.EVENTS,
            NotificationChannelManager.getChannelId(com.guyghost.wakeve.notification.NotificationType.EVENT_UPDATE)
        )
    }

    @Test
    @RequiresApi(Build.VERSION_CODES.O)
    fun `createChannel should create channel with custom settings`() {
        // Given
        val customChannelId = "custom_channel"
        val customName = "Custom Channel"
        val customDescription = "A custom notification channel"
        val customImportance = NotificationManager.IMPORTANCE_LOW

        // When
        channelManager.createChannel(
            id = customChannelId,
            name = customName,
            description = customDescription,
            importance = customImportance
        )

        // Then
        verify(mockNotificationManager).createNotificationChannel(check { channel ->
            assertEquals(customChannelId, channel.id)
            assertEquals(customName, channel.name)
            assertEquals(customDescription, channel.description)
            assertEquals(customImportance, channel.importance)
        })
    }
}
