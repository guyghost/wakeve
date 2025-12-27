package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Represents an item in the user's inbox (notifications, updates, etc.)
 */
@Serializable
data class InboxItem(
    val id: String,
    val type: InboxItemType,
    val status: InboxItemStatus,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val eventId: String? = null,
    val eventTitle: String? = null,
    val timestamp: String, // ISO 8601
    val isRead: Boolean = false,
    val commentCount: Int = 0,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Type of inbox item - determines the icon and behavior
 */
@Serializable
enum class InboxItemType {
    /** New event invitation */
    EVENT_INVITATION,
    
    /** Poll has been created or updated */
    POLL_UPDATE,
    
    /** Vote deadline reminder */
    VOTE_REMINDER,
    
    /** Event date has been confirmed */
    EVENT_CONFIRMED,
    
    /** New participant joined */
    PARTICIPANT_JOINED,
    
    /** Someone voted on a poll */
    VOTE_SUBMITTED,
    
    /** New comment posted */
    COMMENT_POSTED,
    
    /** Reply to your comment */
    COMMENT_REPLY,
    
    /** Budget update */
    BUDGET_UPDATE,
    
    /** Activity added or modified */
    ACTIVITY_UPDATE,
    
    /** Accommodation update */
    ACCOMMODATION_UPDATE,
    
    /** General notification */
    GENERAL
}

/**
 * Status of the inbox item - determines the status indicator color
 */
@Serializable
enum class InboxItemStatus {
    /** Requires action (e.g., vote, confirm) */
    ACTION_REQUIRED,
    
    /** Informational update, no action needed */
    INFO,
    
    /** Successfully completed action */
    SUCCESS,
    
    /** Warning or attention needed */
    WARNING,
    
    /** Completed/closed item */
    COMPLETED
}

/**
 * Filter options for the inbox
 */
@Serializable
enum class InboxFilter(val label: String) {
    ALL("Toutes"),
    UNREAD("Non lues"),
    EVENTS("Événements"),
    COMMENTS("Commentaires"),
    ACTIONS("À traiter")
}
