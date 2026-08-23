package com.guyghost.wakeve.notification

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class APNsRequestRedactionRedTest {
    @Test
    fun `HTTP2 request toString and nested interpolation redact path authorization payload and headers`() {
        val deviceToken = "raw-device-token-super-secret"
        val jwt = "eyJhbGciOiJFUzI1NiJ9.secret-provider-jwt.signature"
        val payloadSecret = "private-event-payload-secret"
        val privateHeader = "private-header-secret"
        val request = APNsHttp2Request(
            authority = "api.push.apple.com",
            path = "/3/device/$deviceToken",
            headers = mapOf(
                "authorization" to "bearer $jwt",
                "apns-topic" to "com.guyghost.wakeve",
                "x-private-context" to privateHeader
            ),
            body = "{\"aps\":{\"alert\":\"$payloadSecret\"}}",
            correlationId = "safe-correlation-id",
            mayBeWritten = true
        )

        val renderings = listOf(
            request.toString(),
            "provider request=$request",
            mapOf("nested" to listOf(request)).toString()
        )
        renderings.forEach { rendering ->
            listOf(request.authority, request.method, request.correlationId).forEach { safeField ->
                assertTrue(safeField in rendering, "request diagnostic omitted safe field `$safeField`: $rendering")
            }
            listOf(deviceToken, jwt, payloadSecret, privateHeader, "bearer $jwt").forEach { secret ->
                assertFalse(secret in rendering, "request diagnostic leaked `$secret`: $rendering")
            }
        }
    }

    @Test
    fun `provider request toString and nested interpolation redact device token and payload`() {
        val deviceToken = "provider-device-token-super-secret"
        val payloadSecret = "provider-private-event-payload"
        val request = APNsProviderRequest(
            deliveryKey = DeliveryKey("safe-delivery-key"),
            apnsId = "safe-apns-id",
            deviceToken = deviceToken,
            payload = "{\"secret\":\"$payloadSecret\"}",
            expirationEpochSeconds = 1_000,
            priority = 10,
            pushType = "alert"
        )

        listOf(
            request.toString(),
            "provider boundary=$request",
            listOf(mapOf("request" to request)).toString()
        ).forEach { rendering ->
            listOf(
                request.deliveryKey.value,
                request.apnsId,
                request.priority.toString(),
                request.pushType,
                request.expirationEpochSeconds.toString()
            ).forEach { safeField ->
                assertTrue(safeField in rendering, "provider diagnostic omitted safe field `$safeField`: $rendering")
            }
            assertFalse(deviceToken in rendering, "provider request diagnostic leaked its device token: $rendering")
            assertFalse(payloadSecret in rendering, "provider request diagnostic leaked its payload: $rendering")
        }
    }
}
