package com.guyghost.wakeve.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * RED interface boundary for confirmation envelope fan-out.
 *
 * Recipient and delivery identifiers already exist. The remaining contracts make calendar
 * identity, a readiness kill switch, single delivery authority, and bounded zero-target
 * resolution explicit before any participant effect is enabled.
 */
class ConfirmationFanOutContractsRedTest {
    @Test
    fun fanOutContractsExposeStableRecipientDeliveryAndCalendarArtifactIdentities() {
        assertTrue(BackendNotificationRecipient::class.java.declaredFields.any { it.name == "recipientKey" })
        assertTrue(BackendNotificationDelivery::class.java.declaredFields.any { it.name == "deliveryKey" })
        assertEquals("recipient-1", RecipientKey("recipient-1").value)
        assertEquals("delivery-1", DeliveryKey("delivery-1").value)

        val calendarArtifactKey = requireProductionClass("com.guyghost.wakeve.notification.CalendarArtifactKey")
        assertTrue(calendarArtifactKey.isAssignableFrom(calendarArtifactKey))
    }

    @Test
    fun fanOutReadinessStartsDisabledAndStoreSeparatesAuthorityFromLeaseAndRecipientResolution() {
        val readiness = requireProductionClass("com.guyghost.wakeve.notification.ConfirmationFanOutReadiness")
        assertTrue(readiness.isEnum, "Readiness must be an explicit closed set, not an implicit configuration flag")
        assertTrue(
            readiness.enumConstants.orEmpty().map { (it as Enum<*>).name }.contains("DISABLED"),
            "Participant fan-out must remain disabled until rollout readiness succeeds"
        )

        val storeMethods = BackendNotificationDeliveryStore::class.java.methods.map { it.name }.toSet()
        assertTrue(
            "acquireDeliveryAuthority" in storeMethods,
            "Delivery authority must be independently persisted and unique; a lease alone is insufficient"
        )
        assertTrue(
            "resolvePendingRecipient" in storeMethods,
            "Zero-target recipients need a retryable, persisted resolution boundary"
        )
        assertTrue(
            "recordRecipientTerminalAcknowledgement" in storeMethods,
            "Recipient expiry/retry exhaustion must be acknowledged without creating a delivery"
        )
    }

    private fun requireProductionClass(qualifiedName: String): Class<*> {
        val type = runCatching { Class.forName(qualifiedName) }.getOrNull()
        return assertNotNull(type, "Missing production fan-out contract: $qualifiedName")
    }
}
