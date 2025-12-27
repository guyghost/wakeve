package com.guyghost.wakeve.meeting

import kotlinx.serialization.Serializable

/**
 * Plateforme de réunion
 */
@Serializable
enum class MeetingPlatform(val displayName: String) {
    ZOOM("Zoom"),
    GOOGLE_MEET("Google Meet"),
    FACETIME("FaceTime")
}