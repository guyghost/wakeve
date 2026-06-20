package com.guyghost.wakeve.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class PostEventPhotosNavigationContractTest {
    @Test
    fun screenDefinesEventScopedPhotosRoute() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/Screen.kt"
        ).readText()

        assertTrue(
            source.contains("""data object EventPhotos : Screen("event/{eventId}/photos")"""),
            "Android navigation must expose an event-scoped photos route."
        )
        assertTrue(
            source.contains("""fun createRoute(eventId: String) = "event/${'$'}{routePathSegment(eventId)}/photos""""),
            "The photos route builder must encode the event ID path segment."
        )
    }

    @Test
    fun navHostRegistersPostEventPhotosDestination() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()

        assertTrue(
            source.contains("route = Screen.EventPhotos.route"),
            "Post-event photos action must navigate to a registered destination."
        )
        assertTrue(
            source.contains("EventPhotosFollowUpScreen("),
            "The registered destination must render the photos follow-up screen."
        )
    }

    @Test
    fun eventDetailPostEventPhotosActionNavigatesToPhotosRoute() {
        val source = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/EventDetailScreen.kt"
        ).readText()

        assertTrue(
            source.contains("""PostEventPrimaryAction.OPEN_PHOTOS -> onNavigateTo("event/${'$'}{event.id}/photos")"""),
            "Post-event photos action must not stay disabled or navigate to a missing route."
        )
        assertTrue(
            source.contains("""PostEventPrimaryAction.OPEN_PHOTOS -> "Voir les photos""""),
            "Post-event photos action must use a concrete user-facing action label."
        )
    }

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
