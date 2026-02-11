package com.guyghost.wakeve.notification

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.TimeOfDay
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Advanced notification scheduler with intelligent reminder capabilities.
 *
 * This service provides high-level scheduling methods for common use cases:
 * - Event reminders with customizable advance notice
 * - Poll deadline reminders
 * - Recurring reminders for multi-day events
 * - Smart reminders based on user preferences
 *
 * ## Architecture
 * - **Functional Core**: Pure functions for calculating reminder times
 * - **Imperative Shell**: Delegates to [NotificationScheduler] for platform I/O
 *
 * ## Usage
 * ```kotlin
 * val scheduler = AdvancedNotificationScheduler()
 * scheduler.scheduleEventReminder(event, reminderMinutesBefore = 60)
 * ```
 *
 * @see NotificationScheduler for low-level platform-specific scheduling
 */
class AdvancedNotificationScheduler(
    private val notificationScheduler: NotificationScheduler = NotificationScheduler.getInstance()
) {

    /**
     * Schedule a reminder for an upcoming event.
     *
     * Calculates the reminder time based on the event's final date and schedules
     * a notification to be shown before the event starts.
     *
     * @param event The event to remind about (must have status CONFIRMED and finalDate set)
     * @param reminderMinutesBefore Minutes before event start to show reminder (default: 60)
     * @return Result with notification ID on success, error on failure
     */
    suspend fun scheduleEventReminder(
        event: Event,
        reminderMinutesBefore: Int = DEFAULT_EVENT_REMINDER_MINUTES
    ): Result<String> {
        // Validate event status
        if (event.status != EventStatus.CONFIRMED && event.status != EventStatus.ORGANIZING) {
            return Result.failure(
                IllegalStateException("Cannot schedule reminder: Event must be CONFIRMED or ORGANIZING, but was ${event.status}")
            )
        }

        // Validate final date exists
        val finalDateStr = event.finalDate
            ?: return Result.failure(
                IllegalStateException("Cannot schedule reminder: Event has no confirmed final date")
            )

        // Parse final date and calculate reminder time
        val finalDate = parseIsoDateTime(finalDateStr)
            ?: return Result.failure(
                IllegalArgumentException("Invalid final date format: $finalDateStr")
            )

        val reminderTime = finalDate.minus(DateTimePeriod(minutes = reminderMinutesBefore))

        // Skip if reminder time has already passed
        val now = Clock.System.now()
        if (reminderTime <= now) {
            return Result.success("skipped-past")
        }

        val title = "⏰ ${event.title}"
        val body = buildString {
            append("Your event starts in ")
            when {
                reminderMinutesBefore >= 1440 -> {
                    val days = reminderMinutesBefore / 1440
                    append("$days day${if (days > 1) "s" else ""}")
                }
                reminderMinutesBefore >= 60 -> {
                    val hours = reminderMinutesBefore / 60
                    append("$hours hour${if (hours > 1) "s" else ""}")
                }
                else -> append("$reminderMinutesBefore minutes")
            }
            append("!")
        }

        return notificationScheduler.scheduleEventReminder(
            eventId = event.id,
            title = title,
            body = body,
            scheduledTime = reminderTime
        ).map { generateNotificationId("event", event.id) }
    }

    /**
     * Schedule a reminder for an upcoming poll deadline.
     *
     * Calculates the reminder time based on the poll's deadline and schedules
     * a notification to remind users to vote before the deadline.
     *
     * @param poll The poll with deadline information
     * @param event The associated event (for context and navigation)
     * @param reminderHoursBefore Hours before deadline to show reminder (default: 24)
     * @return Result with notification ID on success, error on failure
     */
    suspend fun schedulePollDeadlineReminder(
        poll: Poll,
        event: Event,
        reminderHoursBefore: Int = DEFAULT_POLL_REMINDER_HOURS
    ): Result<String> {
        // Validate event status allows voting
        if (event.status != EventStatus.POLLING && event.status != EventStatus.COMPARING) {
            return Result.failure(
                IllegalStateException("Cannot schedule poll reminder: Event must be POLLING or COMPARING, but was ${event.status}")
            )
        }

        // Parse deadline and calculate reminder time
        val deadline = parseIsoDateTime(event.deadline)
            ?: return Result.failure(
                IllegalArgumentException("Invalid deadline format: ${event.deadline}")
            )

        val reminderTime = deadline.minus(DateTimePeriod(hours = reminderHoursBefore))

        // Skip if reminder time has already passed
        val now = Clock.System.now()
        if (reminderTime <= now) {
            return Result.success("skipped-past")
        }

        val title = "🗳️ Vote Closing Soon"
        val body = buildString {
            append("Poll for '${event.title}' closes in ")
            when {
                reminderHoursBefore >= 24 -> {
                    val days = reminderHoursBefore / 24
                    append("$days day${if (days > 1) "s" else ""}")
                }
                else -> append("$reminderHoursBefore hour${if (reminderHoursBefore > 1) "s" else ""}")
            }
            append(". Cast your vote now!")
        }

        return notificationScheduler.schedulePollDeadlineReminder(
            pollId = poll.id,
            eventId = event.id,
            title = title,
            body = body,
            deadlineTime = reminderTime
        ).map { generateNotificationId("poll", poll.id) }
    }

    /**
     * Schedule recurring reminders for an event.
     *
     * Useful for multi-day events or events requiring preparation.
     * Schedules multiple reminders based on the recurrence pattern.
     *
     * @param event The event to schedule recurring reminders for
     * @param recurrencePattern Pattern defining when reminders should be sent
     * @return Result with list of notification IDs on success, error on failure
     */
    suspend fun scheduleRecurringReminder(
        event: Event,
        recurrencePattern: RecurrencePattern
    ): Result<List<String>> {
        // Validate event status
        if (event.status != EventStatus.CONFIRMED && 
            event.status != EventStatus.ORGANIZING && 
            event.status != EventStatus.FINALIZED) {
            return Result.failure(
                IllegalStateException("Cannot schedule recurring reminders: Event must be CONFIRMED, ORGANIZING, or FINALIZED")
            )
        }

        val finalDateStr = event.finalDate
            ?: return Result.failure(
                IllegalStateException("Cannot schedule recurring reminders: Event has no confirmed final date")
            )

        val finalDate = parseIsoDateTime(finalDateStr)
            ?: return Result.failure(
                IllegalArgumentException("Invalid final date format: $finalDateStr")
            )

        val notificationIds = mutableListOf<String>()
        val errors = mutableListOf<String>()

        // Calculate reminder times based on pattern
        val reminderTimes = calculateReminderTimes(finalDate, recurrencePattern)

        reminderTimes.forEachIndexed { index, reminderTime ->
            val now = Clock.System.now()
            if (reminderTime <= now) {
                return@forEachIndexed // Skip past reminders
            }

            val daysUntil = calculateDaysUntil(finalDate, reminderTime)
            val title = "📅 ${event.title}"
            val body = when {
                daysUntil == 0 -> "Your event is today!"
                daysUntil == 1 -> "Your event is tomorrow!"
                else -> "Your event is in $daysUntil days. Get ready!"
            }

            val notificationId = "${generateNotificationId("event", event.id)}-recurring-$index"

            val result = notificationScheduler.scheduleEventReminder(
                eventId = event.id,
                title = title,
                body = body,
                scheduledTime = reminderTime
            )

            result.fold(
                onSuccess = { notificationIds.add(notificationId) },
                onFailure = { errors.add(it.message ?: "Unknown error") }
            )
        }

        return if (notificationIds.isNotEmpty()) {
            Result.success(notificationIds)
        } else if (errors.isNotEmpty()) {
            Result.failure(IllegalStateException("Failed to schedule reminders: ${errors.joinToString(", ")}"))
        } else {
            Result.success(emptyList()) // All reminders were in the past
        }
    }

    /**
     * Schedule a smart reminder based on user preferences.
     *
     * Adapts the reminder timing based on:
     * - User's quiet hours (avoids scheduling during quiet time)
     * - Event type (different default reminders for different types)
     * - Notification preferences (only schedules enabled types)
     *
     * @param event The event to schedule a reminder for
     * @param userPreferences User's notification preferences
     * @return Result with notification ID on success, error on failure
     */
    suspend fun scheduleSmartReminder(
        event: Event,
        userPreferences: NotificationPreferences
    ): Result<String> {
        // Check if event reminders are enabled
        if (NotificationType.EVENT_UPDATE !in userPreferences.enabledTypes &&
            NotificationType.MEETING_REMINDER !in userPreferences.enabledTypes) {
            return Result.failure(
                IllegalStateException("Event reminders are disabled in user preferences")
            )
        }

        // Validate event status
        if (event.status != EventStatus.CONFIRMED && event.status != EventStatus.ORGANIZING) {
            return Result.failure(
                IllegalStateException("Cannot schedule smart reminder: Event must be CONFIRMED or ORGANIZING")
            )
        }

        val finalDateStr = event.finalDate
            ?: return Result.failure(
                IllegalStateException("Cannot schedule smart reminder: Event has no confirmed final date")
            )

        val finalDate = parseIsoDateTime(finalDateStr)
            ?: return Result.failure(
                IllegalArgumentException("Invalid final date format: $finalDateStr")
            )

        // Calculate smart reminder time
        val baseReminderTime = finalDate.minus(
            DateTimePeriod(minutes = getDefaultReminderMinutesForEventType(event.eventType))
        )

        // Adjust for quiet hours if necessary
        val adjustedTime = adjustForQuietHours(baseReminderTime, userPreferences)

        // Skip if adjusted time has already passed
        val now = Clock.System.now()
        if (adjustedTime <= now) {
            return Result.success("skipped-past")
        }

        // Determine notification type based on event
        val notificationType = when (event.eventType) {
            com.guyghost.wakeve.models.EventType.WORKSHOP,
            com.guyghost.wakeve.models.EventType.CONFERENCE,
            com.guyghost.wakeve.models.EventType.TEAM_BUILDING -> NotificationType.MEETING_REMINDER
            else -> NotificationType.EVENT_UPDATE
        }

        // Check if this specific type is enabled
        if (notificationType !in userPreferences.enabledTypes) {
            return Result.failure(
                IllegalStateException("$notificationType notifications are disabled in user preferences")
            )
        }

        val title = "🔔 ${event.title}"
        val body = generateSmartReminderBody(event, adjustedTime, finalDate)

        return notificationScheduler.scheduleEventReminder(
            eventId = event.id,
            title = title,
            body = body,
            scheduledTime = adjustedTime
        ).map { generateNotificationId("smart", event.id) }
    }

    /**
     * Cancel all reminders for an event.
     *
     * Cancels event reminders, poll reminders, and any recurring reminders.
     *
     * @param eventId The event ID to cancel reminders for
     * @return Result with Unit on success, error on failure
     */
    suspend fun cancelEventReminders(eventId: String): Result<Unit> {
        val baseId = generateNotificationId("event", eventId)
        val pollId = generateNotificationId("poll", eventId)
        val smartId = generateNotificationId("smart", eventId)

        // Cancel base event reminder
        val results = mutableListOf<Result<Unit>>()
        results.add(notificationScheduler.cancelScheduledNotification(baseId))
        results.add(notificationScheduler.cancelScheduledNotification(pollId))
        results.add(notificationScheduler.cancelScheduledNotification(smartId))

        // Note: Recurring reminders would need to track their individual IDs
        // This is a simplified implementation

        val failures = results.filter { it.isFailure }
        return if (failures.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed to cancel some reminders"))
        }
    }

    // ============ Private Helper Methods ============

    /**
     * Parse ISO 8601 date/time string to Instant.
     */
    private fun parseIsoDateTime(isoString: String): Instant? {
        return try {
            Instant.parse(isoString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generate unique notification ID.
     */
    private fun generateNotificationId(type: String, id: String): String {
        return "$type-$id"
    }

    /**
     * Calculate reminder times based on recurrence pattern.
     */
    private fun calculateReminderTimes(
        finalDate: Instant,
        pattern: RecurrencePattern
    ): List<Instant> {
        val times = mutableListOf<Instant>()

        pattern.intervalsDays.forEach { days ->
            val reminderTime = finalDate.minus(DateTimePeriod(days = days))
            // Adjust time of day if specified
            val adjustedTime = if (pattern.timeOfDay != null) {
                adjustTimeOfDay(reminderTime, pattern.timeOfDay, pattern.timeZone)
            } else {
                reminderTime
            }
            times.add(adjustedTime)
        }

        return times.sorted()
    }

    /**
     * Adjust instant to specific time of day.
     */
    private fun adjustTimeOfDay(
        instant: Instant,
        timeOfDay: TimeOfDay,
        timeZone: TimeZone
    ): Instant {
        val localDateTime = instant.toLocalDateTime(timeZone)
        val date = localDateTime.date

        val (hour, minute) = when (timeOfDay) {
            TimeOfDay.MORNING -> Pair(9, 0)
            TimeOfDay.AFTERNOON -> Pair(14, 0)
            TimeOfDay.EVENING -> Pair(19, 0)
            TimeOfDay.ALL_DAY -> Pair(9, 0)
            TimeOfDay.SPECIFIC -> return instant // Keep original time
        }

        return date.atTime(hour, minute).toInstant(timeZone)
    }

    /**
     * Calculate days between reminder time and event date.
     */
    private fun calculateDaysUntil(eventDate: Instant, reminderTime: Instant): Int {
        val diff = eventDate.epochSeconds - reminderTime.epochSeconds
        return (diff / 86400).toInt()
    }

    /**
     * Adjust reminder time to avoid quiet hours.
     */
    private fun adjustForQuietHours(
        originalTime: Instant,
        preferences: NotificationPreferences
    ): Instant {
        val quietStart = preferences.quietHoursStart
        val quietEnd = preferences.quietHoursEnd

        if (quietStart == null || quietEnd == null) {
            return originalTime
        }

        // Convert to minutes since midnight for comparison
        // This is a simplified check - in production, consider timezone
        val originalMinutes = (originalTime.epochSeconds / 60 % 1440).toInt()
        val quietStartMinutes = quietStart.hour * 60 + quietStart.minute
        val quietEndMinutes = quietEnd.hour * 60 + quietEnd.minute

        val inQuietHours = if (quietStartMinutes > quietEndMinutes) {
            // Overnight quiet hours (e.g., 22:00 - 08:00)
            originalMinutes >= quietStartMinutes || originalMinutes < quietEndMinutes
        } else {
            // Same day quiet hours
            originalMinutes in quietStartMinutes until quietEndMinutes
        }

        return if (inQuietHours) {
            // Move to end of quiet hours (next day if overnight)
            val adjustmentMinutes = if (quietStartMinutes > quietEndMinutes && originalMinutes >= quietStartMinutes) {
                // After quiet start, before midnight - move to next morning
                (1440 - originalMinutes) + quietEndMinutes
            } else {
                quietEndMinutes - originalMinutes
            }
            originalTime.plus(DateTimePeriod(minutes = adjustmentMinutes))
        } else {
            originalTime
        }
    }

    /**
     * Get default reminder minutes based on event type.
     */
    private fun getDefaultReminderMinutesForEventType(
        eventType: com.guyghost.wakeve.models.EventType
    ): Int {
        return when (eventType) {
            com.guyghost.wakeve.models.EventType.WORKSHOP -> 30
            com.guyghost.wakeve.models.EventType.WEDDING -> 10080 // 1 week
            com.guyghost.wakeve.models.EventType.CONFERENCE -> 1440 // 1 day
            com.guyghost.wakeve.models.EventType.TEAM_BUILDING -> 2880 // 2 days
            else -> DEFAULT_EVENT_REMINDER_MINUTES
        }
    }

    /**
     * Generate personalized reminder body based on event and timing.
     */
    private fun generateSmartReminderBody(
        event: com.guyghost.wakeve.models.Event,
        reminderTime: Instant,
        finalDate: Instant
    ): String {
        val hoursUntil = ((finalDate.epochSeconds - reminderTime.epochSeconds) / 3600).toInt()
        
        return when {
            hoursUntil <= 1 -> "${event.title} starts in less than an hour!"
            hoursUntil <= 24 -> "${event.title} starts in $hoursUntil hours."
            hoursUntil <= 48 -> "${event.title} is tomorrow. Get ready!"
            else -> {
                val daysUntil = hoursUntil / 24
                "${event.title} is in $daysUntil days. ${event.eventType.displayName} time!"
            }
        }
    }

    companion object {
        /** Default reminder time before events: 1 hour */
        const val DEFAULT_EVENT_REMINDER_MINUTES = 60

        /** Default reminder time before poll deadline: 24 hours */
        const val DEFAULT_POLL_REMINDER_HOURS = 24
    }
}

/**
 * Pattern for recurring reminders.
 *
 * @property intervalsDays List of days before event to send reminders (e.g., [7, 1, 0] for week, day, and day-of)
 * @property timeOfDay Optional time of day to send reminders (defaults to event time)
 * @property timeZone TimeZone for time of day calculations
 */
data class RecurrencePattern(
    val intervalsDays: List<Int>,
    val timeOfDay: TimeOfDay? = null,
    val timeZone: TimeZone = TimeZone.currentSystemDefault()
) {
    init {
        require(intervalsDays.isNotEmpty()) { "Intervals list cannot be empty" }
        require(intervalsDays.all { it >= 0 }) { "Intervals must be non-negative" }
    }

    companion object {
        /** Standard pattern: 1 week, 1 day, and day-of reminders */
        fun standard(): RecurrencePattern = RecurrencePattern(
            intervalsDays = listOf(7, 1, 0),
            timeOfDay = TimeOfDay.MORNING
        )

        /** Minimal pattern: Day-of reminder only */
        fun minimal(): RecurrencePattern = RecurrencePattern(
            intervalsDays = listOf(0)
        )

        /** Aggressive pattern: 1 week, 3 days, 1 day, day-of */
        fun aggressive(): RecurrencePattern = RecurrencePattern(
            intervalsDays = listOf(7, 3, 1, 0),
            timeOfDay = TimeOfDay.MORNING
        )
    }
}

/**
 * Simple date/time period for calculations.
 * Wrapper around kotlinx.datetime.DatePeriod and duration.
 */
private data class DateTimePeriod(
    val days: Int = 0,
    val hours: Int = 0,
    val minutes: Int = 0
) {
    fun toDurationMinutes(): Long {
        return days * 1440L + hours * 60L + minutes
    }
}

/**
 * Subtract DateTimePeriod from Instant.
 */
private fun Instant.minus(period: DateTimePeriod): Instant {
    val totalMinutes = period.days * 1440L + period.hours * 60L + period.minutes
    return this.minus(totalMinutes.minutes)
}

/**
 * Add DateTimePeriod to Instant.
 */
private fun Instant.plus(period: DateTimePeriod): Instant {
    val totalMinutes = period.days * 1440L + period.hours * 60L + period.minutes
    return this.plus(totalMinutes.minutes)
}
