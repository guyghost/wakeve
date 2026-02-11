package com.guyghost.wakeve.notification

import kotlinx.serialization.Serializable

/**
 * Rich notification data class with enhanced visual and interactive capabilities.
 * Supports images, custom sounds, vibration patterns, LED colors, actions, and deep links.
 *
 * @property id Unique identifier for this notification
 * @property userId Target user identifier
 * @property title Notification title (required)
 * @property body Notification body text (required)
 * @property imageUrl Optional URL for a large image to display in the notification
 * @property largeIcon Optional URL or resource identifier for a large icon (profile picture, app icon variant)
 * @property actions List of actionable buttons displayed on the notification
 * @property priority Priority level determining interruption behavior
 * @property category Category determining default actions and grouping
 * @property deepLink Optional deep link URI to navigate within the app when tapped
 * @property customSound Optional custom sound resource identifier or URL
 * @property vibrationPattern Optional vibration pattern as array of on/off durations in milliseconds
 * @property ledColor Optional LED color for devices with notification LEDs (ARGB integer)
 */
@Serializable
data class RichNotification(
    val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val imageUrl: String? = null,
    val largeIcon: String? = null,
    val actions: List<NotificationAction> = emptyList(),
    val priority: RichNotificationPriority = RichNotificationPriority.DEFAULT,
    val category: NotificationCategory = NotificationCategory.GENERAL,
    val deepLink: String? = null,
    val customSound: String? = null,
    val vibrationPattern: List<Int>? = null,
    val ledColor: Int? = null
) {
    /**
     * Validate that the notification has all required fields.
     * @return null if valid, error message if invalid
     */
    fun validate(): String? {
        if (title.isBlank()) return "Title cannot be blank"
        if (body.isBlank()) return "Body cannot be blank"
        if (userId.isBlank()) return "UserId cannot be blank"
        if (id.isBlank()) return "Id cannot be blank"
        return null
    }

    /**
     * Check if this notification has any rich media content.
     */
    fun hasRichContent(): Boolean =
        imageUrl != null ||
            largeIcon != null ||
            customSound != null ||
            vibrationPattern != null ||
            ledColor != null

    /**
     * Check if this notification has actionable buttons.
     */
    fun hasActions(): Boolean = actions.isNotEmpty()

    /**
     * Check if this notification has a deep link for navigation.
     */
    fun hasDeepLink(): Boolean = !deepLink.isNullOrBlank()

    companion object {
        /**
         * Default vibration pattern: 300ms on, 200ms off, 300ms on
         */
        val DEFAULT_VIBRATION_PATTERN = listOf(300, 200, 300)

        /**
         * Short vibration pattern: 100ms on
         */
        val SHORT_VIBRATION_PATTERN = listOf(100)

        /**
         * Long vibration pattern for urgent notifications
         */
        val URGENT_VIBRATION_PATTERN = listOf(500, 200, 500, 200, 500)

        /**
         * Default LED color (Wakeve brand blue: #4285F4)
         */
        const val DEFAULT_LED_COLOR = 0xFF4285F4.toInt()

        /**
         * Urgent LED color (red)
         */
        const val URGENT_LED_COLOR = 0xFFFF0000.toInt()

        /**
         * Success LED color (green)
         */
        const val SUCCESS_LED_COLOR = 0xFF00FF00.toInt()
    }
}

/**
 * Builder class for constructing RichNotification instances with a fluent API.
 */
class RichNotificationBuilder {
    private var id: String = ""
    private var userId: String = ""
    private var title: String = ""
    private var body: String = ""
    private var imageUrl: String? = null
    private var largeIcon: String? = null
    private var actions: MutableList<NotificationAction> = mutableListOf()
    private var priority: RichNotificationPriority = RichNotificationPriority.DEFAULT
    private var category: NotificationCategory = NotificationCategory.GENERAL
    private var deepLink: String? = null
    private var customSound: String? = null
    private var vibrationPattern: List<Int>? = null
    private var ledColor: Int? = null

    fun id(id: String) = apply { this.id = id }
    fun userId(userId: String) = apply { this.userId = userId }
    fun title(title: String) = apply { this.title = title }
    fun body(body: String) = apply { this.body = body }
    fun imageUrl(imageUrl: String?) = apply { this.imageUrl = imageUrl }
    fun largeIcon(largeIcon: String?) = apply { this.largeIcon = largeIcon }
    fun action(action: NotificationAction) = apply { this.actions.add(action) }
    fun actions(actions: List<NotificationAction>) = apply { this.actions.addAll(actions) }
    fun priority(priority: RichNotificationPriority) = apply { this.priority = priority }
    fun category(category: NotificationCategory) = apply { this.category = category }
    fun deepLink(deepLink: String?) = apply { this.deepLink = deepLink }
    fun customSound(customSound: String?) = apply { this.customSound = customSound }
    fun vibrationPattern(vibrationPattern: List<Int>?) = apply { this.vibrationPattern = vibrationPattern }
    fun ledColor(ledColor: Int?) = apply { this.ledColor = ledColor }

    /**
     * Apply default actions for the current category.
     */
    fun withDefaultActions(): RichNotificationBuilder = apply {
        this.actions.clear()
        this.actions.addAll(category.getDefaultActions())
    }

    fun build(): RichNotification {
        return RichNotification(
            id = id,
            userId = userId,
            title = title,
            body = body,
            imageUrl = imageUrl,
            largeIcon = largeIcon,
            actions = actions.toList(),
            priority = priority,
            category = category,
            deepLink = deepLink,
            customSound = customSound,
            vibrationPattern = vibrationPattern,
            ledColor = ledColor
        )
    }
}

/**
 * Create a RichNotification using the builder DSL.
 */
inline fun richNotification(block: RichNotificationBuilder.() -> Unit): RichNotification {
    return RichNotificationBuilder().apply(block).build()
}
