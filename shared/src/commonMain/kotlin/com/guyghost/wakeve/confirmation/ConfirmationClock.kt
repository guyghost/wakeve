package com.guyghost.wakeve.confirmation

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

fun interface ConfirmationClock {
    fun now(): Instant
}

object SystemConfirmationClock : ConfirmationClock {
    override fun now(): Instant = Clock.System.now()
}

data class ConfirmationEffectKeys(
    val domainEventId: String,
    val effectKey: String
)

fun confirmationEffectKeys(eventId: String, slotId: String): ConfirmationEffectKeys {
    val domainEventId = "poll-date-confirmed:$eventId:$slotId:v1"
    return ConfirmationEffectKeys(
        domainEventId = domainEventId,
        effectKey = "$domainEventId:confirmation"
    )
}
