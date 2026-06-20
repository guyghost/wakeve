package com.guyghost.wakeve.deeplink

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InvitationShareServerContractTest {
    @Test
    fun navHostCreatesInvitationThroughServerInsteadOfLocalRandomCode() {
        val source = projectFile("composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt").readText()

        assertTrue(
            source.contains("AndroidInvitationShareService") &&
                source.contains("invitationShareService.createInvitation(eventId)") &&
                source.contains("InvitationShareCreationResult.Created") &&
                source.contains("invitationCode = result.invitation.code"),
            "Invitation sharing must use the backend-created invitation code before showing a share link."
        )
        assertFalse(
            source.contains("chars.random()") || source.contains("(1..8).map"),
            "Invitation sharing must not generate local random codes that the backend cannot resolve."
        )
        assertFalse(
            source.contains("in production, call server API"),
            "The production Android flow must call the server API now, not leave fake-link TODOs in the route."
        )
    }

    @Test
    fun invitationShareServicePostsAuthenticatedCreateRequest() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/deeplink/AndroidInvitationShareService.kt"
        ).readText()

        assertTrue(
            source.contains("/api/events/") &&
                source.contains("/invite") &&
                source.contains("Authorization") &&
                source.contains("Bearer $") &&
                source.contains("CreateInvitationRequest()") &&
                source.contains("InvitationShareCreationResult.Created"),
            "Android invitation share service must call the authenticated backend create-invitation endpoint."
        )
    }

    @Test
    fun shareScreenDoesNotRenderLinkOrQrBeforeRealCodeExists() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/invitation/InvitationShareScreen.kt"
        ).readText()

        assertTrue(
            source.contains("invitationCode: String?") &&
                source.contains("val inviteUrl = invitationCode?.let") &&
                source.contains("if (inviteUrl != null)") &&
                source.contains("enabled = inviteUrl != null && !isLoading"),
            "Invitation share screen must not expose copy/share/QR affordances before a backend code exists."
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
