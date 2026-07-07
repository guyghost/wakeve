package com.guyghost.wakeve.notification

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class PushSenderConfigurationTest {
    @Test
    fun noConfiguredFcmSenderFailsInsteadOfReportingDelivery() = runTest {
        val result = NoConfiguredFCMSender.sendNotification(
            token = "token",
            title = "Title",
            body = "Body",
            data = emptyMap()
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not configured") == true)
    }

    @Test
    fun noConfiguredApnsSenderFailsInsteadOfReportingDelivery() = runTest {
        val result = NoConfiguredAPNsSender.sendNotification(
            token = "token",
            title = "Title",
            body = "Body",
            data = emptyMap()
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not configured") == true)
    }

    @Test
    fun legacyMockFcmSenderFailsInsteadOfReportingDelivery() = runTest {
        @Suppress("DEPRECATION")
        val result = MockFCMSender().sendNotification(
            token = "token",
            title = "Title",
            body = "Body",
            data = emptyMap()
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not configured") == true)
    }

    @Test
    fun legacyMockApnsSenderFailsInsteadOfReportingDelivery() = runTest {
        @Suppress("DEPRECATION")
        val result = MockAPNsSender().sendNotification(
            token = "token",
            title = "Title",
            body = "Body",
            data = emptyMap()
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not configured") == true)
    }

    @Test
    fun noConfiguredRichFcmSenderFailsInsteadOfReportingDelivery() = runTest {
        val result = NoConfiguredRichFCMSender.sendRichNotification(
            token = "token",
            notification = richNotification()
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not configured") == true)
    }

    @Test
    fun legacyMockRichApnsSenderFailsInsteadOfReportingDelivery() = runTest {
        @Suppress("DEPRECATION")
        val result = MockRichAPNsSender().sendRichNotification(
            token = "token",
            notification = richNotification()
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not configured") == true)
    }

    private fun richNotification(): RichNotification {
        return RichNotification(
            id = "notification-1",
            userId = "user-1",
            title = "Title",
            body = "Body"
        )
    }
}
