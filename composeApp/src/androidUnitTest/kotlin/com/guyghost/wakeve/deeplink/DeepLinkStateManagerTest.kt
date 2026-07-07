package com.guyghost.wakeve.deeplink

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeepLinkStateManagerTest {
    @AfterTest
    fun tearDown() {
        DeepLinkStateManager.clearPendingDeepLink()
        DeepLinkStateManager.clearPendingInviteCode()
    }

    @Test
    fun isSupportedPendingDeepLink_acceptsSupportedDeepLink() {
        val supported = isSupportedPendingDeepLink(
            scheme = "wakeve",
            host = "event",
            pathSegments = listOf("event-123", "details"),
            queryParameters = mapOf("tab" to "comments")
        )

        assertTrue(supported)
    }

    @Test
    fun isSupportedPendingDeepLink_rejectsUnsupportedDeepLinks() {
        assertFalse(
            isSupportedPendingDeepLink(
                scheme = "https",
                host = "evil.example",
                pathSegments = listOf("invite", "INVITE123")
            )
        )
        assertFalse(
            isSupportedPendingDeepLink(
                scheme = "wakeve",
                host = "event",
                pathSegments = listOf("event-123", "cancel")
            )
        )
        assertFalse(
            isSupportedPendingDeepLink(
                scheme = "http",
                host = "wakeve.app",
                pathSegments = listOf("invite", "INVITE123")
            )
        )
    }

    @Test
    fun updatePendingDeepLinkRejectsUnsupportedLinksWithoutClearingExistingPendingDestination() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/deeplink/DeepLinkStateManager.kt"
        ).readText()
        val updateMethod = source.substringAfter("fun updatePendingDeepLink(uri: Uri): Boolean")
            .substringBefore("fun clearPendingDeepLink()")

        assertTrue(
            updateMethod.contains("_pendingDeepLink.value = uri"),
            "Supported deep links must still replace the pending destination."
        )
        assertFalse(
            updateMethod.contains("_pendingDeepLink.value = null"),
            "Rejected deep links must not clear an existing pending destination that may be waiting for auth replay."
        )
    }

    @Test
    fun updatePendingInviteCode_exposesNormalizedCodeForAppLevelProcessing() {
        val stored = DeepLinkStateManager.updatePendingInviteCode(" invite-code-123 ")

        assertTrue(stored)
        assertEquals("invite-code-123", DeepLinkStateManager.pendingInviteCode.value)
        assertTrue(DeepLinkStateManager.hasPendingInviteCode())
    }

    @Test
    fun updatePendingInviteCode_rejectsUnsafeCodeAndClearsStaleInvite() {
        DeepLinkStateManager.updatePendingInviteCode("invite-code-123")

        val stored = DeepLinkStateManager.updatePendingInviteCode("INV/ITE")

        assertFalse(stored)
        assertNull(DeepLinkStateManager.pendingInviteCode.value)
        assertFalse(DeepLinkStateManager.hasPendingInviteCode())
    }

    @Test
    fun clearPendingInviteCode_removesStoredCode() {
        DeepLinkStateManager.updatePendingInviteCode("invite-code-123")

        DeepLinkStateManager.clearPendingInviteCode()

        assertNull(DeepLinkStateManager.pendingInviteCode.value)
        assertFalse(DeepLinkStateManager.hasPendingInviteCode())
    }

    @Test
    fun androidDeepLinkHandlerStoresInvitesInSharedStateManager() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/deeplink/DeepLinkHandler.kt"
        ).readText()

        assertTrue(
            source.contains("DeepLinkStateManager.pendingInviteCode.value") &&
                source.contains("DeepLinkStateManager.updatePendingInviteCode(value)") &&
                source.contains("DeepLinkStateManager.clearPendingInviteCode()"),
            "Invite deep links must be stored in DeepLinkStateManager, not in a handler-local field that no UI can consume."
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
