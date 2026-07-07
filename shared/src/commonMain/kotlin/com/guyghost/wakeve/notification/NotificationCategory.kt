package com.guyghost.wakeve.notification

import kotlinx.serialization.Serializable

/**
 * Notification categories supported by the system.
 * Categories group notifications and may define default actions that are safe to
 * expose without a server-mutating direct action handler.
 */
@Serializable
enum class NotificationCategory(val identifier: String) {
    /**
     * User invited to event.
     * Invite decisions are handled in-app after opening the event.
     */
    EVENT_INVITE("event_invite"),

    /**
     * Poll deadline approaching reminder.
     * Poll votes are handled in-app after opening the poll.
     */
    POLL_REMINDER("poll_reminder"),

    /**
     * Meeting about to start.
     * Actions: join
     */
    MEETING_STARTING("meeting_starting"),

    /**
     * New scenario proposed for voting.
     * Scenario votes are handled in-app after opening the event.
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
 * Defines semantic notification actions. Only actions with a wired, reliable
 * handler should be exposed by default.
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
 * Users can tap these to perform actions without opening the app only when the
 * platform layer wires the action to a real handler.
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
 * @return Only actions that are safe to expose by default
 */
fun NotificationCategory.getDefaultActions(): List<NotificationAction> = when (this) {
    NotificationCategory.EVENT_INVITE -> emptyList()
    NotificationCategory.POLL_REMINDER -> emptyList()
    NotificationCategory.MEETING_STARTING -> NotificationAction.meetingStartingActions()
    NotificationCategory.SCENARIO_VOTE -> emptyList()
    NotificationCategory.GENERAL -> emptyList()
}
