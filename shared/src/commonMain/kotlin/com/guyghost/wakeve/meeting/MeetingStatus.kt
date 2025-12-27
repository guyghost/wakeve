package com.guyghost.wakeve.meeting

import kotlinx.serialization.Serializable

/**
 * Statut de réunion
 */
@Serializable
enum class MeetingStatus {
    SCHEDULED,
    STARTED,
    ENDED,
    CANCELLED
}