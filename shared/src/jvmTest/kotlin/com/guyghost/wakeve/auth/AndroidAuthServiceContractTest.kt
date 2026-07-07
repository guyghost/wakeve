package com.guyghost.wakeve.auth

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class AndroidAuthServiceContractTest {
    @Test
    fun androidOAuthSignInDoesNotReturnPlaceholderUsersWhenProvidersAreMissing() {
        val source = projectFile("shared/src/androidMain/kotlin/com/guyghost/wakeve/auth/shell/services/AndroidAuthService.kt").readText()

        assertFalse(
            source.contains("google_id_token_placeholder") ||
                source.contains("apple_identity_token_placeholder") ||
                source.contains("google_user_123") ||
                source.contains("apple_user_456"),
            "Android OAuth sign-in must not authenticate placeholder users when providers are missing."
        )
        assertFalse(
            source.contains("falls back to mock", ignoreCase = true) ||
                source.contains("Mock implementation", ignoreCase = true),
            "Android OAuth sign-in must fail honestly instead of documenting or keeping a mock fallback."
        )
        assertContains(
            source,
            "Google OAuth provider not configured",
            message = "Missing Google provider should produce an explicit configuration error."
        )
        assertContains(
            source,
            "Apple Sign-In provider not configured",
            message = "Missing Apple provider should produce an explicit configuration error."
        )
    }

    @Test
    fun androidOAuthAvailabilityReflectsInjectedProviders() {
        val source = projectFile("shared/src/androidMain/kotlin/com/guyghost/wakeve/auth/shell/services/AndroidAuthService.kt").readText()

        assertContains(
            source,
            "AuthMethod.GOOGLE -> oauthProvider != null",
            message = "Google availability should be false until a Google provider is injected."
        )
        assertContains(
            source,
            "AuthMethod.APPLE -> appleSignInProvider != null",
            message = "Apple availability should be false until an Apple provider is injected."
        )
    }

    private fun projectFile(relativePath: String): File {
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { File(it, relativePath).exists() }
        return File(root, relativePath)
    }
}
