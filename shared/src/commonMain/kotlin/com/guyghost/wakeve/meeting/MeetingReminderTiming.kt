package com.guyghost.wakeve.meeting

import kotlinx.serialization.Serializable

/**
 * Timing des rappels
 */
@Serializable
enum class MeetingReminderTiming {
    ONE_DAY_BEFORE,
    ONE_HOUR_BEFORE,
    FIFTEEN_MINUTES_BEFORE
}