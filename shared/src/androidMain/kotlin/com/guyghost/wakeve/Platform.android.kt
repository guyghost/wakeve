package com.guyghost.wakeve

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()
actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

// Note: Need to pass context somehow - perhaps through dependency injection
actual fun getCalendarService(): CalendarService = DefaultCalendarService() // AndroidCalendarService(context)
actual fun getNotificationService(): NotificationService = DefaultNotificationService()