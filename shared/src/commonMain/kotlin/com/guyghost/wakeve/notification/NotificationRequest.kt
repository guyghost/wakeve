package com.guyghost.wakeve.notification

import kotlinx.serialization.Serializable

@Serializable
data class NotificationRequest(
    val userId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val eventId: String? = null,
    val data: Map<String, String> = emptyMap(),
    val commentId: String? = null,
    val parentCommentId: String? = null,
    val section: String? = null,
    val sectionItemId: String? = null
)
