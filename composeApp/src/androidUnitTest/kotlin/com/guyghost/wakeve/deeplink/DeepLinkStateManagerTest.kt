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
        DeepLinkStateManager.clearPendingInviteCode()
    }

    @Test
    fun updatePendingInviteCode_exposesCodeForAppLevelProcessing() {
        DeepLinkStateManager.updatePendingInviteCode("invite-code-123")

        assertEquals("invite-code-123", DeepLinkStateManager.pendingInviteCode.value)
        assertTrue(DeepLinkStateManager.hasPendingInviteCode())
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
