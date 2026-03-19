package com.guyghost.wakeve

import com.guyghost.wakeve.notification.DefaultNotificationService
import com.guyghost.wakeve.notification.NotificationServiceInterface
import kotlin.js.Date

class JsPlatform: Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()
actual fun getCurrentTimeMillis(): Long = Date.now().toLong()
actual fun getNotificationService(): NotificationServiceInterface = DefaultNotificationService()