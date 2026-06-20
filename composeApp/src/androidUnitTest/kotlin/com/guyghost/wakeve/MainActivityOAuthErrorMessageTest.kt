package com.guyghost.wakeve

import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.AuthMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MainActivityOAuthErrorMessageTest {

    @Test
    fun oauthMessages_areStableUserFacingCopy() {
        assertEquals(
            "Impossible de lancer la connexion Google.",
            oauthLaunchFailureMessage(OAuthProviderName.GOOGLE)
        )
        assertEquals(
            "Impossible de terminer la connexion Apple.",
            oauthResultFailureMessage(OAuthProviderName.APPLE)
        )
        assertEquals(
            "Connexion Google indisponible pour le moment.",
            oauthUnavailableMessage(OAuthProviderName.GOOGLE)
        )
    }

    @Test
    fun appleOAuthCallbackFailureMessage_doesNotExposeProviderError() {
        val providerError = "invalid_request: code=AUTH-CODE-123 state=STATE-456 user=test@example.com"

        val result = appleOAuthCallbackFailureMessage(providerError)

        assertEquals("Connexion Apple annulée ou impossible.", result)
        assertDoesNotExposeSensitiveDetails(result)
    }

    @Test
    fun isAppleOAuthCallbackDeepLink_acceptsOnlyCanonicalCallbackAuthority() {
        assertEquals(
            true,
            isAppleOAuthCallbackDeepLink(
                scheme = "wakeve",
                host = "apple-auth-callback",
                encodedUserInfo = null,
                encodedFragment = null,
                port = -1
            )
        )
    }

    @Test
    fun isAppleOAuthCallbackDeepLink_rejectsAmbiguousCallbackAuthority() {
        assertEquals(
            false,
            isAppleOAuthCallbackDeepLink(
                scheme = "wakeve",
                host = "apple-auth-callback",
                encodedUserInfo = "attacker",
                encodedFragment = null,
                port = -1
            )
        )
        assertEquals(
            false,
            isAppleOAuthCallbackDeepLink(
                scheme = "wakeve",
                host = "apple-auth-callback",
                encodedUserInfo = null,
                encodedFragment = "code=AUTH-CODE-123",
                port = -1
            )
        )
        assertEquals(
            false,
            isAppleOAuthCallbackDeepLink(
                scheme = "wakeve",
                host = "apple-auth-callback",
                encodedUserInfo = null,
                encodedFragment = null,
                port = 443
            )
        )
    }

    @Test
    fun authenticationFailureMessage_doesNotExposeOAuthProviderMessage() {
        val providerError = AuthError.OAuthError(
            provider = AuthMethod.GOOGLE,
            message = "invalid_grant for auth_code=SECRET-CODE-123 and user=test@example.com"
        )

        val result = authenticationFailureMessage(providerError)

        assertEquals("Erreur d'authentification", result)
        assertDoesNotExposeSensitiveDetails(result)
    }

    @Test
    fun authenticationFailureMessage_keepsExpectedGenericCategories() {
        assertEquals(
            "Connexion annulée",
            authenticationFailureMessage(AuthError.OAuthCancelled(AuthMethod.APPLE))
        )
        assertEquals(
            "Erreur réseau: Vérifiez votre connexion",
            authenticationFailureMessage(AuthError.NetworkError)
        )
        assertEquals(
            "Erreur d'authentification",
            authenticationFailureMessage(AuthError.UnknownError("backend stack trace"))
        )
    }

    private fun assertDoesNotExposeSensitiveDetails(message: String) {
        listOf(
            "AUTH-CODE-123",
            "SECRET-CODE-123",
            "STATE-456",
            "test@example.com",
            "invalid_grant",
            "invalid_request",
            "auth_code"
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }
}
