package com.guyghost.wakeve.notification

import kotlinx.serialization.Serializable

/**
 * Notification categories supported by the system.
 * Each category defines the available actions for user interaction.
 */
@Serializable
enum class NotificationCategory(val identifier: String) {
    /**
     * User invited to event.
     * Actions: accept/decline/maybe
     */
    EVENT_INVITE("event_invite"),

    /**
     * Poll deadline approaching reminder.
     * Actions: vote
     */
    POLL_REMINDER("poll_reminder"),

    /**
     * Meeting about to start.
     * Actions: join
     */
    MEETING_STARTING("meeting_starting"),

    /**
     * New scenario proposed for voting.
     * Actions: yes/no
     */
    SCENARIO_VOTE("scenario_vote"),

    /**
     * General informational notification.
     * Actions: none
     */
    GENERAL("general");

    companion object {
        /**
         * Find category by identifier string.
         * @return Matching category or null if not found
         */
        fun fromString(identifier: String): NotificationCategory? =
            values().find { it.identifier == identifier }
    }
}

/**
 * Available actions for notification categories.
 * Defines what user can do directly from the notification.
 */
@Serializable
enum class ActionType {
    /**
     * Vote YES on a poll/scenario
     */
    VOTE_YES,

    /**
     * Vote NO on a poll/scenario
     */
    VOTE_NO,

    /**
     * Vote MAYBE on an event invite
     */
    VOTE_MAYBE,

    /**
     * Join an event
     */
    JOIN_EVENT,

    /**
     * Join a meeting
     */
    JOIN_MEETING,

    /**
     * Reply to a comment
     */
    REPLY_COMMENT
}

/**
 * Represents an actionable button in a notification.
 * Users can tap these to perform actions without opening the app.
 */
@Serializable
data class NotificationAction(
    /**
     * Unique identifier for this action
     */
    val identifier: String,

    /**
     * User-visible label (localized)
     */
    val label: String,

    /**
     * Type of action to perform
     */
    val type: ActionType
) {
    companion object {
        /**
         * Create EVENT_INVITE category actions (accept/decline/maybe)
         */
        fun eventInviteActions() = listOf(
            NotificationAction("accept", "Accept", ActionType.JOIN_EVENT),
            NotificationAction("maybe", "Maybe", ActionType.VOTE_MAYBE),
            NotificationAction("decline", "Decline", ActionType.VOTE_NO)
        )

        /**
         * Create POLL_REMINDER category actions (vote)
         */
        fun pollReminderActions() = listOf(
            NotificationAction("vote", "Vote Now", ActionType.VOTE_YES)
        )

        /**
         * Create MEETING_STARTING category actions (join)
         */
        fun meetingStartingActions() = listOf(
            NotificationAction("join", "Join Meeting", ActionType.JOIN_MEETING)
        )

        /**
         * Create SCENARIO_VOTE category actions (yes/no)
         */
        fun scenarioVoteActions() = listOf(
            NotificationAction("yes", "Yes", ActionType.VOTE_YES),
            NotificationAction("no", "No", ActionType.VOTE_NO)
        )
    }
}

/**
 * Get default actions for a notification category.
 * @return List of actions or empty list for GENERAL category
 */
fun NotificationCategory.getDefaultActions(): List<NotificationAction> = when (this) {
    NotificationCategory.EVENT_INVITE -> NotificationAction.eventInviteActions()
    NotificationCategory.POLL_REMINDER -> NotificationAction.pollReminderActions()
    NotificationCategory.MEETING_STARTING -> NotificationAction.meetingStartingActions()
    NotificationCategory.SCENARIO_VOTE -> NotificationAction.scenarioVoteActions()
    NotificationCategory.GENERAL -> emptyList()
}
