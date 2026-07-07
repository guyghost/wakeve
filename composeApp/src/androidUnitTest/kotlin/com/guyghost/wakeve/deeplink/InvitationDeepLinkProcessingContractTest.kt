package com.guyghost.wakeve.deeplink

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InvitationDeepLinkProcessingContractTest {
    @Test
    fun appDoesNotTreatGuestModeAsAuthenticatedForInviteAcceptance() {
        val source = projectFile("composeApp/src/androidMain/kotlin/com/guyghost/wakeve/App.kt").readText()

        assertTrue(
            source.contains("isAuthenticated = authState.isAuthenticated"),
            "Invite deep links must require a real authenticated account, not guest mode."
        )
        assertFalse(
            source.contains("isAuthenticated = authState.isAuthenticated || authState.isGuest"),
            "Guest mode cannot be passed as authenticated for invite acceptance because /api/invite/{code}/accept requires JWT auth."
        )
    }

    @Test
    fun appAcceptsPendingInviteThroughServerBeforeClearingCode() {
        val source = projectFile("composeApp/src/androidMain/kotlin/com/guyghost/wakeve/App.kt").readText()

        assertTrue(
            source.contains("DeepLinkStateManager.pendingInviteCode.collectAsState()") &&
                source.contains("invitationDeepLinkService.acceptInvitation(input.code)") &&
                source.contains("pendingInviteProcessingResult") &&
                source.contains("action.acceptedEventId") &&
                source.contains("DeepLinkStateManager.clearPendingInviteCode()") &&
                source.contains("Screen.EventDetail.createRoute(action.acceptedEventId)"),
            "Pending invitation codes must be accepted by the server, then cleared and routed to the joined event only on success."
        )
    }

    @Test
    fun invitationDeepLinkServicePostsAuthenticatedAcceptRequest() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/deeplink/AndroidInvitationDeepLinkService.kt"
        ).readText()

        assertTrue(
            source.contains("/api/invite/") &&
                source.contains("/accept") &&
                source.contains("normalizeInvitationAcceptanceCode(code)") &&
                source.contains("normalizeInvitationAcceptanceEventId(body.eventId)") &&
                source.contains("Authorization") &&
                source.contains("Bearer $") &&
                source.contains("InvitationDeepLinkAcceptanceResult.Accepted"),
            "Android invitation deep link service must call the authenticated server accept endpoint and normalize accepted event ids before reporting success."
        )
        assertTrue(
            source.contains("AuthenticationRequired") &&
                source.contains("Rejected") &&
                source.contains("RetryableFailure"),
            "Invitation accept outcomes must distinguish auth, invalid invitation, and retryable network/server failures."
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
