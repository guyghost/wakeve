package com.guyghost.wakeve.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guyghost.wakeve.notification.NotificationChannelManager.Companion.ChannelId
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

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
        mockNotificationManager = mockk(relaxed = true)
        channelManager = NotificationChannelManager(context, mockNotificationManager)
    }

    @After
    fun tearDown() {
        // Clean up any created channels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val realNotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            ChannelId.values().forEach { channelId ->
                realNotificationManager.deleteNotificationChannel(channelId.id)
            }
        }
    }

    @Test
    fun createAllChannels_should_create_all_defined_channels_on_Android_O() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // When
            channelManager.createAllChannels()

            // Then
            ChannelId.values().forEach { channelId ->
                verify {
                    mockNotificationManager.createNotificationChannel(
                        match { channel -> channel.id == channelId.id }
                    )
                }
            }
        }
    }

    @Test
    fun createAllChannels_should_configure_default_channel_with_correct_settings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // When
            channelManager.createAllChannels()

            // Then
            verify {
                mockNotificationManager.createNotificationChannel(match { channel ->
                    channel.id == ChannelId.DEFAULT.id &&
                        channel.importance == NotificationManager.IMPORTANCE_DEFAULT &&
                        channel.shouldVibrate()
                })
            }
        }
    }

    @Test
    fun createAllChannels_should_configure_high_priority_channel_with_correct_settings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // When
            channelManager.createAllChannels()

            // Then
            verify {
                mockNotificationManager.createNotificationChannel(match { channel ->
                    channel.id == ChannelId.HIGH_PRIORITY.id &&
                        channel.importance == NotificationManager.IMPORTANCE_HIGH &&
                        channel.shouldVibrate()
                })
            }
        }
    }

    @Test
    fun createAllChannels_should_configure_events_channel_with_correct_settings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // When
            channelManager.createAllChannels()

            // Then
            verify {
                mockNotificationManager.createNotificationChannel(match { channel ->
                    channel.id == ChannelId.EVENTS.id &&
                        channel.importance == NotificationManager.IMPORTANCE_HIGH &&
                        channel.shouldVibrate()
                })
            }
        }
    }

    @Test
    fun createAllChannels_should_configure_reminders_channel_with_correct_settings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // When
            channelManager.createAllChannels()

            // Then
            verify {
                mockNotificationManager.createNotificationChannel(match { channel ->
                    channel.id == ChannelId.REMINDERS.id &&
                        channel.importance == NotificationManager.IMPORTANCE_DEFAULT &&
                        channel.shouldVibrate()
                })
            }
        }
    }

    @Test
    fun createAllChannels_should_configure_progress_channel_with_correct_settings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // When
            channelManager.createAllChannels()

            // Then
            verify {
                mockNotificationManager.createNotificationChannel(match { channel ->
                    channel.id == ChannelId.PROGRESS.id &&
                        channel.importance == NotificationManager.IMPORTANCE_LOW &&
                        !channel.shouldVibrate()
                })
            }
        }
    }

    @Test
    fun getChannelId_should_return_correct_channel_ID_for_notification_type() {
        // Then
        assertEquals(
            ChannelId.HIGH_PRIORITY,
            NotificationChannelManager.getChannelId(com.guyghost.wakeve.notification.NotificationType.EVENT_INVITE)
        )
        assertEquals(
            ChannelId.REMINDERS,
            NotificationChannelManager.getChannelId(com.guyghost.wakeve.notification.NotificationType.VOTE_REMINDER)
        )
        assertEquals(
            ChannelId.DEFAULT,
            NotificationChannelManager.getChannelId(com.guyghost.wakeve.notification.NotificationType.EVENT_UPDATE)
        )
    }

    @Test
    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel_should_create_channel_with_custom_settings() {
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
        verify {
            mockNotificationManager.createNotificationChannel(match { channel ->
                channel.id == customChannelId &&
                    channel.name == customName &&
                    channel.description == customDescription &&
                    channel.importance == customImportance
            })
        }
    }
}
