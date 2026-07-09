package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** RED persistence contracts require a production implementation registered through the production factory port. */
class BackendNotificationPersistenceRedTest {
    @Test
    fun productionStoreFailsClosedWhenNoDurableStorageIsConfigured() {
        val previous = System.getProperty(DELIVERY_STORE_PROPERTY)
        try {
            System.clearProperty(DELIVERY_STORE_PROPERTY)
            assertTrue(
                System.getenv(DELIVERY_STORE_ENVIRONMENT).isNullOrBlank(),
                "This contract requires the test process to omit delivery-store configuration"
            )

            assertFailsWith<IllegalStateException>(
                "The production factory must reject the old process-local temporary database fallback"
            ) {
                SqliteBackendNotificationDeliveryStoreFactory().open()
            }
        } finally {
            restoreDeliveryStoreProperty(previous)
        }
    }

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

    @Test
    fun configuredStorePersistsRecipientUpsertAuthorityAndZeroTargetResolutionAcrossInstances() = runBlocking {
        val databasePath = Files.createTempDirectory("wakeve-delivery-store-contract-")
            .resolve("delivery.sqlite")
        val previous = System.getProperty(DELIVERY_STORE_PROPERTY)
        try {
            System.setProperty(DELIVERY_STORE_PROPERTY, databasePath.toString())
            val recipient = BackendNotificationRecipient(
                recipientKey = RecipientKey("zero-target-recipient"),
                effectKey = EffectKey("confirmation-effect"),
                status = BackendRecipientStatus.PENDING_TARGET,
                installationIds = emptySet(),
                expiresAtEpochSeconds = 1_000
            )
            val firstStore = productionStore()

            assertTrue(firstStore.persistPendingRecipient(recipient))
            assertFalse(firstStore.persistPendingRecipient(recipient), "recipient upsert must not create a second logical target")
            assertTrue(firstStore.acquireDeliveryAuthority("confirmation-delivery", DeliveryAuthority("worker-a")))

            val restartedStore = productionStore()
            assertFalse(
                restartedStore.acquireDeliveryAuthority("confirmation-delivery", DeliveryAuthority("worker-b")),
                "A second worker must never become a second durable delivery authority"
            )
            assertEquals(
                BackendRecipientStatus.PENDING_TARGET,
                restartedStore.resolvePendingRecipient(recipient.recipientKey, nowEpochSeconds = 999)
            )
            assertEquals(
                BackendRecipientStatus.EXPIRED,
                restartedStore.resolvePendingRecipient(recipient.recipientKey, nowEpochSeconds = 1_000)
            )
            assertTrue(
                restartedStore.recordRecipientTerminalAcknowledgement(
                    BackendRecipientTerminalAcknowledgement(
                        recipient.recipientKey,
                        BackendRecipientTerminalReason.EXPIRED_WITHOUT_TARGET,
                        acknowledgedAtEpochSeconds = 1_000
                    )
                )
            )

            val afterRestart = productionStore()
            assertEquals(BackendRecipientStatus.EXPIRED, afterRestart.recipient(recipient.recipientKey)?.status)
            assertNull(
                afterRestart.delivery(DeliveryKey("confirmation-effect:zero-target-recipient:apns")),
                "Zero-target resolution must remain a recipient acknowledgement and must not create a provider delivery"
            )
        } finally {
            restoreDeliveryStoreProperty(previous)
            Files.deleteIfExists(databasePath)
            Files.deleteIfExists(databasePath.parent)
        }
    }

    private fun productionStore(): BackendNotificationDeliveryStore {
        val factory = ServiceLoader.load(BackendNotificationDeliveryStoreFactory::class.java).firstOrNull()
        return assertNotNull(factory, "backend production delivery store is not implemented").open()
    }

    private fun queuedDelivery(key: DeliveryKey, expiresAt: Long = 10_000) = BackendNotificationDelivery(
        key, RecipientKey("recipient-1"), "installation-a", "apns", BackendDeliveryStatus.QUEUED,
        attempt = 0, nextAttemptAtEpochSeconds = null, expiresAtEpochSeconds = expiresAt
    )

    private fun restoreDeliveryStoreProperty(previous: String?) {
        if (previous == null) {
            System.clearProperty(DELIVERY_STORE_PROPERTY)
        } else {
            System.setProperty(DELIVERY_STORE_PROPERTY, previous)
        }
    }

    private companion object {
        const val DELIVERY_STORE_PROPERTY = "wakeve.notification.delivery.db.path"
        const val DELIVERY_STORE_ENVIRONMENT = "WAKEVE_NOTIFICATION_DELIVERY_DB_PATH"
    }
}
