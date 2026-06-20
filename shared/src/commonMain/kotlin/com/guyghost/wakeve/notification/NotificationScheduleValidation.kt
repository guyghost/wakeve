package com.guyghost.wakeve.notification

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal fun futureScheduleDelayMillis(
    targetTime: Instant,
    now: Instant = Clock.System.now()
): Result<Long> {
    val delayMillis = (targetTime - now).inWholeMilliseconds
    return if (delayMillis > 0) {
        Result.success(delayMillis)
    } else {
        Result.failure(IllegalArgumentException("Notification scheduled time must be in the future"))
    }
}
