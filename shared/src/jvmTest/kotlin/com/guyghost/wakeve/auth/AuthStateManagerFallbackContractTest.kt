package com.guyghost.wakeve.auth

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class AuthStateManagerFallbackContractTest {
    @Test
    fun dummySecureStorageDoesNotReportSuccessfulWrites() {
        val source = projectFile("shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/AuthStateManager.kt").readText()
        val dummyStorage = source.substringAfter("private class DummySecureTokenStorage")
            .substringBefore("private class DummyAuthenticationService")

        assertFalse(
            dummyStorage.contains("Result.success(Unit)"),
            "Fallback secure token storage must not report successful writes when no platform storage is configured."
        )
        assertContains(dummyStorage, "storageUnavailable()")
        assertContains(dummyStorage, "Use platform-specific secure token storage")
    }

    @Test
    fun dummyAuthenticationServiceDoesNotReportSuccessfulLogout() {
        val source = projectFile("shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/AuthStateManager.kt").readText()
        val dummyAuth = source.substringAfter("private class DummyAuthenticationService")
            .substringBefore("/**\n * Result wrapper for auth operations.")

        assertFalse(
            dummyAuth.contains("Result.success(Unit)"),
            "Fallback authentication service must not report successful mutations when no platform auth is configured."
        )
        assertContains(dummyAuth, "Use platform-specific implementation")
    }

    private fun projectFile(relativePath: String): File {
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { File(it, relativePath).exists() }
        return File(root, relativePath)
    }
}
