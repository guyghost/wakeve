package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.models.MeetingPlatform
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * Mises à jour de réunion
 */
@Serializable
data class MeetingUpdates(
    val title: String? = null,
    val description: String? = null,
    val startTime: Instant? = null,
    val duration: Duration? = null,
    val platform: MeetingPlatform? = null,
    val meetingLink: String? = null
)