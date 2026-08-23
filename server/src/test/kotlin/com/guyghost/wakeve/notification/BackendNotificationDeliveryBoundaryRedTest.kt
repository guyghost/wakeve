package com.guyghost.wakeve.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackendNotificationDeliveryBoundaryRedTest {
    @Test
    fun `persisted provider reason is a closed vocabulary and free text is normalized`() {
        assertEquals(
            setOf(
                BackendPersistedProviderReason.HTTP_200,
                BackendPersistedProviderReason.HTTP_5XX,
                BackendPersistedProviderReason.TOO_MANY_REQUESTS,
                BackendPersistedProviderReason.IDLE_TIMEOUT,
                BackendPersistedProviderReason.TOKEN_INVALID,
                BackendPersistedProviderReason.PAYLOAD_REJECTED,
                BackendPersistedProviderReason.PROVIDER_AUTH_REJECTED,
                BackendPersistedProviderReason.UNKNOWN_PROVIDER_REASON,
                BackendPersistedProviderReason.TRANSPORT_BEFORE_WRITE,
                BackendPersistedProviderReason.TRANSPORT_OUTCOME_UNKNOWN,
                BackendPersistedProviderReason.INVALID_RETRY_AFTER,
                BackendPersistedProviderReason.RETRY_WOULD_REACH_EXPIRY,
                BackendPersistedProviderReason.RETRY_BUDGET_EXHAUSTED
            ),
            BackendPersistedProviderReason.entries.toSet()
        )
        val classified = BackendDeliveryObservationClassifier.classify(
            observation = BackendProviderRawObservation.Http(
                statusCode = 400,
                rawReason = "provider-free-text-that-must-not-be-persisted",
                retryAfterEpochSeconds = null,
                providerRequestId = "apns-unknown",
                observedAtEpochSeconds = 100
            ),
            context = retryContext(),
            jitter = BackendDeliveryJitterSource { _, _ -> 0.5 }
        )

        assertEquals(BackendDurableProviderOutcome.UNKNOWN_OUTCOME, classified.outcome)
        assertEquals(BackendPersistedProviderReason.UNKNOWN_PROVIDER_REASON, classified.reason)
        assertTrue(classified.reason.name.none { it.isLowerCase() })
        assertNull(classified.acceptedAtEpochSeconds)
    }

    @Test
    fun `Retry After rejects past fractional non finite overflow over cap and expiry values`() {
        val invalidValues = listOf(
            99.0,
            100.0,
            100.5,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.MAX_VALUE,
            401.0
        )
        invalidValues.forEach { retryAfter ->
            val result = classifyRetry(retryAfter, retryContext())
            assertEquals(BackendDurableProviderOutcome.UNKNOWN_OUTCOME, result.outcome, "retryAfter=$retryAfter")
            assertEquals(BackendPersistedProviderReason.INVALID_RETRY_AFTER, result.reason, "retryAfter=$retryAfter")
            assertNull(result.nextAttemptAtEpochSeconds)
        }

        val atExpiry = classifyRetry(1_000.0, retryContext())
        assertEquals(BackendDurableProviderOutcome.EXPIRED, atExpiry.outcome)
        assertEquals(BackendPersistedProviderReason.RETRY_WOULD_REACH_EXPIRY, atExpiry.reason)
        assertNull(atExpiry.nextAttemptAtEpochSeconds)

        val valid = classifyRetry(101.0, retryContext())
        assertEquals(BackendDurableProviderOutcome.RETRY, valid.outcome)
        assertEquals(101L, valid.nextAttemptAtEpochSeconds)
    }

    @Test
    fun `full jitter is injected overflow safe future capped and bounded by retry budget and expiry`() {
        val cases = listOf(
            Triple(0L, 0.0, 1L),
            Triple(1L, 0.75, 1L),
            Triple(2L, 0.75, 3L),
            Triple(20L, 1.0, 300L),
            Triple(1_000_000L, 2.0, 300L),
            Triple(1_000_000L, Double.NaN, 1L)
        )
        cases.forEach { (attempt, sample, expectedDelay) ->
            val context = retryContext(attempt = attempt, maxAttempts = Long.MAX_VALUE, expiresAt = 10_000)
            val result = BackendDeliveryObservationClassifier.classify(
                observation = BackendProviderRawObservation.Http(
                    statusCode = 503,
                    rawReason = "ServiceUnavailable",
                    retryAfterEpochSeconds = null,
                    providerRequestId = "apns-jitter-$attempt",
                    observedAtEpochSeconds = 100
                ),
                context = context,
                jitter = BackendDeliveryJitterSource { deliveryKey, nextAttempt ->
                    assertEquals(context.deliveryKey, deliveryKey)
                    assertTrue(nextAttempt > attempt)
                    sample
                }
            )
            assertEquals(BackendDurableProviderOutcome.RETRY, result.outcome)
            assertEquals(100 + expectedDelay, result.nextAttemptAtEpochSeconds)
            assertTrue(assertNotNull(result.nextAttemptAtEpochSeconds) in 101..400)
        }

        val exhausted = BackendDeliveryObservationClassifier.classify(
            observation = BackendProviderRawObservation.Http(503, null, null, null, 100),
            context = retryContext(attempt = Long.MAX_VALUE, maxAttempts = Long.MAX_VALUE),
            jitter = BackendDeliveryJitterSource { _, _ -> error("budget exhaustion must precede jitter") }
        )
        assertEquals(BackendDurableProviderOutcome.RETRY_EXHAUSTED, exhausted.outcome)
        assertEquals(BackendPersistedProviderReason.RETRY_BUDGET_EXHAUSTED, exhausted.reason)
        assertNull(exhausted.nextAttemptAtEpochSeconds)

        val reachesExpiry = BackendDeliveryObservationClassifier.classify(
            observation = BackendProviderRawObservation.Http(500, null, null, null, 100),
            context = retryContext(attempt = 20, maxAttempts = 30, expiresAt = 400),
            jitter = BackendDeliveryJitterSource { _, _ -> 1.0 }
        )
        assertEquals(BackendDurableProviderOutcome.EXPIRED, reachesExpiry.outcome)
        assertEquals(BackendPersistedProviderReason.RETRY_WOULD_REACH_EXPIRY, reachesExpiry.reason)
    }

    private fun classifyRetry(
        retryAfterEpochSeconds: Double,
        context: BackendDeliveryRetryContext
    ) = BackendDeliveryObservationClassifier.classify(
        observation = BackendProviderRawObservation.Http(
            statusCode = 429,
            rawReason = "TooManyRequests",
            retryAfterEpochSeconds = retryAfterEpochSeconds,
            providerRequestId = "apns-retry-after",
            observedAtEpochSeconds = context.nowEpochSeconds
        ),
        context = context,
        jitter = BackendDeliveryJitterSource { _, _ -> error("Retry-After is not replaced by jitter") }
    )

    private fun retryContext(
        attempt: Long = 0,
        maxAttempts: Long = 5,
        expiresAt: Long = 1_000
    ) = BackendDeliveryRetryContext(
        deliveryKey = DeliveryKey("boundary-delivery"),
        nowEpochSeconds = 100,
        expiresAtEpochSeconds = expiresAt,
        attempt = attempt,
        maxAttempts = maxAttempts
    )
}
