package com.guyghost.wakeve.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScreenRouteEncodingContractTest {
    @Test
    fun parameterizedScreenRouteBuildersEncodePathSegments() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/Screen.kt"
        ).readText()

        assertTrue(
            source.contains("internal fun routePathSegment(value: String): String = Uri.encode(value)"),
            "Screen route builders must use one path-segment encoding helper."
        )
        assertFalse(
            rawPathInterpolationPattern.containsMatchIn(source),
            "Screen route builders must not interpolate raw route parameters into path segments."
        )
    }

    @Test
    fun notificationsRouteBuilderOnlyPreservesSupportedFilter() {
        assertEquals("notifications", Screen.Notifications.createRoute())
        assertEquals("notifications?filter=unread", Screen.Notifications.createRoute(" unread "))
        assertEquals("notifications", Screen.Notifications.createRoute("all"))
        assertEquals("notifications", Screen.Notifications.createRoute("unread&admin=true"))
    }

    private val rawPathInterpolationPattern = Regex(
        """event/${'$'}(eventId|scenarioId|budgetItemId)|meeting/${'$'}meetingId|event/${'$'}\{(eventId|scenarioId|budgetItemId)\}|meeting/${'$'}\{meetingId\}"""
    )

    private fun projectFile(relativePath: String): File {
        val userDir = requireNotNull(System.getProperty("user.dir")) { "user.dir is not set" }
        var current: File? = File(userDir).absoluteFile
        while (current != null) {
            val candidate = File(current, relativePath)
            if (candidate.exists()) return candidate
            current = current.parentFile
        }
        error("Could not find project file: $relativePath")
    }
}
