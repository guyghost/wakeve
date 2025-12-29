package com.guyghost.wakeve

import kotlin.js.Date

class JsPlatform: Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()
actual fun getCurrentTimeMillis(): Long = Date.now().toLong()
actual fun getNotificationService(): NotificationService = DefaultNotificationService()