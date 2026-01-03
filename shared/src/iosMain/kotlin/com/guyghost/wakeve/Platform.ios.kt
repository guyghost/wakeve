package com.guyghost.wakeve

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getCurrentTimeMillis(): Long = 0L

actual fun getCurrentTimeNanos(): Long = 0L

actual fun getNotificationService(): NotificationService = DefaultNotificationService()

actual fun measureMemoryUsageMB(): Double? = null
actual fun getMaxMemoryMB(): Double? = null
