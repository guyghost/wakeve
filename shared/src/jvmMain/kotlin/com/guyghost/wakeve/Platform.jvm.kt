package com.guyghost.wakeve

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()
actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

actual fun getCalendarService(): CalendarService = DefaultCalendarService()
actual fun getNotificationService(): NotificationService = DefaultNotificationService()