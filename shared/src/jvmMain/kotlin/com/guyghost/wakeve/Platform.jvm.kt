package com.guyghost.wakeve

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()
actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
actual fun getCurrentTimeNanos(): Long = System.nanoTime()
actual fun getNotificationService(): NotificationService = DefaultNotificationService()

actual fun measureMemoryUsageMB(): Double? {
    return try {
        val runtime = Runtime.getRuntime()
        (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0)
    } catch (e: Exception) {
        null
    }
}

actual fun getMaxMemoryMB(): Double? {
    return try {
        val runtime = Runtime.getRuntime()
        runtime.maxMemory() / (1024.0 * 1024.0)
    } catch (e: Exception) {
        null
    }
}