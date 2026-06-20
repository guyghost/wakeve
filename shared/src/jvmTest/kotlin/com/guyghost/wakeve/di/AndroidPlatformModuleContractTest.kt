package com.guyghost.wakeve.di

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class AndroidPlatformModuleContractTest {
    @Test
    fun platformModuleDoesNotInjectMockPushSenders() {
        val source = projectFile("composeApp/src/androidMain/kotlin/com/guyghost/wakeve/di/PlatformModule.android.kt").readText()
        val start = source.indexOf("NotificationService(")
        require(start >= 0) { "NotificationService creation was not found in Android platform module." }
        val end = source.indexOf("\n        )", start)
        require(end > start) { "Could not isolate NotificationService arguments in Android platform module." }
        val invocation = source.substring(start, end)

        assertFalse(
            invocation.contains("MockFCMSender") || invocation.contains("MockAPNsSender"),
            "Android production DI must not silently report push delivery success with mock senders."
        )
        assertContains(
            invocation,
            "NoConfiguredFCMSender",
            message = "Android production DI should fail honestly until a real FCM sender is configured."
        )
        assertContains(
            invocation,
            "NoConfiguredAPNsSender",
            message = "Android production DI should fail honestly until a real APNs sender is configured."
        )
    }

    private fun projectFile(relativePath: String): File {
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { File(it, relativePath).exists() }
        return File(root, relativePath)
    }
}
