package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class PushNotificationSenderTest {
    @Test
    fun fcmLegacyResponseSucceedsOnlyWhenDeliverySucceeded() {
        val result = validateFcmLegacyResponse(
            """
            {
              "multicast_id": 123,
              "success": 1,
              "failure": 0,
              "canonical_ids": 0,
              "results": [{"message_id": "0:123"}]
            }
            """.trimIndent()
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun fcmLegacyResponseFailsWhenHttp200ContainsDeliveryFailure() {
        val result = validateFcmLegacyResponse(
            """
            {
              "multicast_id": 123,
              "success": 0,
              "failure": 1,
              "canonical_ids": 0,
              "results": [{"error": "InvalidRegistration"}]
            }
            """.trimIndent()
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "InvalidRegistration")
    }

    @Test
    fun fcmLegacyResponseFailsWhenSuccessAndFailureCountsAreMissing() {
        val result = validateFcmLegacyResponse("""{"message_id":"0:123"}""")

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "success count")
    }

    @Test
    fun fcmSenderFailsWhenServerKeyIsMissing() = runBlocking {
        val result = ServerFCMSender(fcmServerKey = null).sendNotification(
            token = "device-token",
            title = "Invitation",
            body = "Nouvelle invitation",
            data = emptyMap()
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "FCM_SERVER_KEY")
    }

    @Test
    fun apnsSenderFailsWhenCredentialsAreMissing() = runBlocking {
        val result = ServerAPNsSender(
            apnsKeyId = null,
            apnsTeamId = null
        ).sendNotification(
            token = "device-token",
            title = "Invitation",
            body = "Nouvelle invitation",
            data = emptyMap()
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "APNs credentials")
    }

    @Test
    fun apnsSenderFailsUntilHttp2ProviderIsImplemented() = runBlocking {
        val result = ServerAPNsSender(
            apnsKeyId = "key-id",
            apnsTeamId = "team-id",
            apnsAuthKey = "p8-content",
            apnsBundleId = "com.guyghost.wakeve",
            apnsEnvironment = "sandbox"
        ).sendNotification(
            token = "device-token",
            title = "Invitation",
            body = "Nouvelle invitation",
            data = emptyMap()
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "not implemented")
    }
}
