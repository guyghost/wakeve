package com.guyghost.wakeve

import com.guyghost.wakeve.models.*

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
expect fun getCurrentTimeMillis(): Long
expect fun getCalendarService(): CalendarService
expect fun getNotificationService(): NotificationService