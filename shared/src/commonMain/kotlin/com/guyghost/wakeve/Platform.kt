package com.guyghost.wakeve

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
expect fun getCurrentTimeMillis(): Long
expect fun getCalendarService(): CalendarService
expect fun getNotificationService(): NotificationService