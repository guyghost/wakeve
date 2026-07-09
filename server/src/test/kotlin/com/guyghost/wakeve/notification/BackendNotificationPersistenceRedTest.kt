package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** RED persistence contracts require a production implementation registered through the production factory port. */
class BackendNotificationPersistenceRedTest {
    @Test
    fun missingInstallationPersistsPendingTargetThenFansOutAfterRegistration() = runBlocking {
        val store = productionStore()
        val recipient = BackendNotificationRecipient(
            RecipientKey("recipient-1"), EffectKey("effect-1"), BackendRecipientStatus.PENDING_TARGET, emptySet(), 5_000
        )
        assertTrue(store.persistPendingRecipient(recipient))
        assertEquals(BackendRecipientStatus.PENDING_TARGET, store.recipient(recipient.recipientKey)?.status)

        store.registerInstallation(recipient.recipientKey, "installation-a")

        assertEquals(setOf("installation-a"), store.recipient(recipient.recipientKey)?.installationIds)
    }

    @Test
    fun duplicateEnqueueCreatesOneDeliveryWithExactDeliveryKey() = runBlocking {
        val store = productionStore()
        val expected = DeliveryKey("effect-1:participant-1:apns:installation-a:apns")
        val delivery = queuedDelivery(expected)
        val first = store.enqueue(delivery)
        val duplicate = store.enqueue(delivery)

        assertEquals(expected, first?.delivery?.deliveryKey)
        assertFalse(duplicate?.created == true, "duplicate enqueue must reuse rather than recreate logical delivery")
        assertEquals(1, store.deliveryCount(expected))
    }

    @Test
    fun expiredLeaseIsRecoverableAfterWorkerRestartWithSameIdentity() = runBlocking {
        val firstWorker = productionStore()
        val delivery = queuedDelivery(DeliveryKey("recipient-1:installation-a:apns"))
        assertTrue(firstWorker.enqueue(delivery)?.created == true)
        assertTrue(firstWorker.acquireLease(delivery.deliveryKey, "worker-a", 100, 200))

        val restartedWorker = productionStore()
        assertFalse(restartedWorker.acquireLease(delivery.deliveryKey, "worker-b", 150, 250))
        assertTrue(restartedWorker.acquireLease(delivery.deliveryKey, "worker-b", 201, 301))
        assertEquals(delivery.deliveryKey, restartedWorker.delivery(delivery.deliveryKey)?.deliveryKey)
    }

    @Test
    fun retryAttemptAndScheduleSurviveRestartUntilBusinessExpiry() = runBlocking {
        val firstWorker = productionStore()
        val delivery = queuedDelivery(DeliveryKey("recipient-1:installation-a:apns"), expiresAt = 1_000)
        assertTrue(firstWorker.enqueue(delivery)?.created == true)
        assertTrue(firstWorker.recordRetry(delivery.deliveryKey, 1, 500))

        val restartedWorker = productionStore()
        assertEquals(1, restartedWorker.delivery(delivery.deliveryKey)?.attempt)
        assertEquals(500, restartedWorker.delivery(delivery.deliveryKey)?.nextAttemptAtEpochSeconds)
        assertTrue(restartedWorker.isEligible(delivery.deliveryKey, 500))
        assertFalse(restartedWorker.isEligible(delivery.deliveryKey, 1_000))
    }

    private fun productionStore(): BackendNotificationDeliveryStore {
        val factory = ServiceLoader.load(BackendNotificationDeliveryStoreFactory::class.java).firstOrNull()
        return assertNotNull(factory, "backend production delivery store is not implemented").open()
    }

    private fun queuedDelivery(key: DeliveryKey, expiresAt: Long = 10_000) = BackendNotificationDelivery(
        key, RecipientKey("recipient-1"), "installation-a", "apns", BackendDeliveryStatus.QUEUED,
        attempt = 0, nextAttemptAtEpochSeconds = null, expiresAtEpochSeconds = expiresAt
    )
}
