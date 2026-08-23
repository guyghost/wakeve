package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BackendNotificationCanonicalIdentityHardeningRedTest {
    @Test
    fun `effect recipient and delivery identities match the approved ek2 rk2 dk2 vectors`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("identity-vector")
            val receipt = ingestion(fixture).ingest(
                BackendNotificationIngestionCommand(
                    domainEventId = "tenant:event:42",
                    effectType = "DATE_CONFIRMED",
                    schemaVersion = 2,
                    logicalNotificationId = "logical-vector",
                    recipients = listOf(
                        BackendNotificationRecipientIntent(
                            participantId = "p-1",
                            channel = "push",
                            provider = "apns",
                            registrationIds = listOf(registration.registrationId),
                            expiresAtEpochSeconds = 1_000
                        )
                    )
                )
            )

            assertEquals(
                "ek2.15:tenant:event:4214:DATE_CONFIRMED.v2",
                receipt.effectKey.value
            )
            val expectedRecipient =
                "rk2.42:ek2.15:tenant:event:4214:DATE_CONFIRMED.v23:p-14:push"
            fixture.deliveryFactory.open().use { store ->
                val delivery = store.delivery(receipt.deliveryKeys.single())
                assertEquals(expectedRecipient, delivery?.recipientKey?.value)
            }
            assertEquals(
                "dk2.60:$expectedRecipient${registration.registrationId.length}:${registration.registrationId}4:apns",
                receipt.deliveryKeys.single().value
            )
        }
    }

    @Test
    fun `length prefixes keep concat colon and legacy hash collision candidates injective`() {
        val first = canonicalEffectKey("ab", "c", 1)
        val second = canonicalEffectKey("a", "bc", 1)
        assertNotEquals(first, second)
        assertTrue(first.value.startsWith("ek2."))
        assertTrue(second.value.startsWith("ek2."))

        val colonA = canonicalEffectKey(
            "pdc2.11:event:other4:slot",
            "confirmation",
            1
        )
        val colonB = canonicalEffectKey(
            "pdc2.5:event10:other:slot",
            "confirmation",
            1
        )
        assertNotEquals(colonA, colonB)
        assertTrue(colonA.value.contains("25:pdc2.11:event:other4:slot"))

        val recipientA = canonicalRecipientKey(first, "a:b", "c")
        val recipientB = canonicalRecipientKey(first, "a", "b:c")
        assertNotEquals(recipientA, recipientB)
        assertTrue(recipientA.value.startsWith("rk2."))

        val deliveryA = canonicalDeliveryKey(recipientA, "r:1", "apns")
        val deliveryB = canonicalDeliveryKey(recipientA, "r", "1:apns")
        assertNotEquals(deliveryA, deliveryB)
        assertTrue(deliveryA.value.startsWith("dk2."))
        assertTrue(deliveryA.value.none { it == '#' })
    }

    @Test
    fun `poll confirmation model vector remains byte exact`() {
        assertEquals(
            "ek2.22:pdc2.7:event-16:slot-112:confirmation.v1",
            canonicalEffectKey(
                "pdc2.7:event-16:slot-1",
                "confirmation",
                1
            ).value
        )
    }

    @Test
    fun `non ASCII and astral vectors use Kotlin and TypeScript UTF-16 String length`() {
        val effect = canonicalEffectKey("événement:🚀", "通知", 2)
        assertEquals("ek2.12:événement:🚀2:通知.v2", effect.value)
        assertEquals(26, effect.value.length)

        val recipient = canonicalRecipientKey(effect, "参加者😀", "push")
        assertEquals("rk2.26:ek2.12:événement:🚀2:通知.v25:参加者😀4:push", recipient.value)
        assertEquals(46, recipient.value.length)

        val delivery = canonicalDeliveryKey(recipient, "reg:📱", "apns")
        assertEquals(
            "dk2.46:rk2.26:ek2.12:événement:🚀2:通知.v25:参加者😀4:push6:reg:📱4:apns",
            delivery.value
        )
        assertEquals(67, delivery.value.length)
    }

    private fun ingestion(fixture: BackendNotificationDurabilityTestFixture) =
        BackendNotificationIngestionService(
            storeFactory = fixture.deliveryFactory,
            faultInjector = BackendNotificationIngestionFaultInjector { },
            committedPort = BackendNotificationIngestionCommittedPort { }
        )

    private fun canonicalEffectKey(
        domainEventId: String,
        effectType: String,
        schemaVersion: Int
    ): EffectKey = BackendCanonicalNotificationIdentity.effectKey(domainEventId, effectType, schemaVersion)

    private fun canonicalRecipientKey(
        effectKey: EffectKey,
        participantId: String,
        channel: String
    ): RecipientKey = BackendCanonicalNotificationIdentity.recipientKey(effectKey, participantId, channel)

    private fun canonicalDeliveryKey(
        recipientKey: RecipientKey,
        registrationId: String,
        provider: String
    ): DeliveryKey = BackendCanonicalNotificationIdentity.deliveryKey(recipientKey, registrationId, provider)
}
