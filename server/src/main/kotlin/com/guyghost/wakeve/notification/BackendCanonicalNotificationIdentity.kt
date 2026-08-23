package com.guyghost.wakeve.notification

/**
 * Canonical v2 identities. Kotlin String.length intentionally counts UTF-16 code units,
 * matching the approved TypeScript model for BMP and astral input.
 */
object BackendCanonicalNotificationIdentity {
    fun effectKey(domainEventId: String, effectType: String, schemaVersion: Int): EffectKey {
        requireCanonicalComponent("domainEventId", domainEventId)
        requireCanonicalComponent("effectType", effectType)
        require(schemaVersion > 0) { "schemaVersion must be positive" }
        return EffectKey("ek2.${component(domainEventId)}${component(effectType)}.v$schemaVersion")
    }

    fun recipientKey(effectKey: EffectKey, participantId: String, channel: String): RecipientKey {
        requireCanonicalComponent("effectKey", effectKey.value)
        requireCanonicalComponent("participantId", participantId)
        requireCanonicalComponent("channel", channel)
        return RecipientKey("rk2.${component(effectKey.value)}${component(participantId)}${component(channel)}")
    }

    fun deliveryKey(recipientKey: RecipientKey, registrationId: String, provider: String): DeliveryKey {
        requireCanonicalComponent("recipientKey", recipientKey.value)
        requireCanonicalComponent("registrationId", registrationId)
        requireCanonicalComponent("provider", provider)
        return DeliveryKey("dk2.${component(recipientKey.value)}${component(registrationId)}${component(provider)}")
    }

    private fun component(value: String): String = "${value.length}:$value"

    private fun requireCanonicalComponent(label: String, value: String) {
        require(value.isNotEmpty() && value.trim() == value) {
            "$label must be a non-empty canonical identity component"
        }
    }
}
