package com.guyghost.wakeve.auth

import com.guyghost.wakeve.models.OAuthProvider
import com.guyghost.wakeve.repository.UserRepository
import com.guyghost.wakeve.security.SecurityAuditLogger
import java.time.Instant

class AccountDeletionService(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val auditLogger: SecurityAuditLogger,
    private val appleRevocationService: AppleAccountRevocationService? = null,
    private val jwtExpiryResolver: (String) -> String? = { null },
    private val defaultJwtExpiryProvider: () -> String = { Instant.now().plusSeconds(3600).toString() }
) {
    suspend fun deleteAccount(userId: String, currentJwtToken: String? = null): Result<AccountDeletionResult> = runCatching {
        val existingUser = userRepository.getUserById(userId)

        if (existingUser == null) {
            revokeCurrentJwt(userId, currentJwtToken)
            auditLogger.logSensitiveOperation(
                userId = userId,
                operation = "account_delete_idempotent",
                details = mapOf("status" to "already_deleted")
            )
            return@runCatching AccountDeletionResult(
                userId = userId,
                deleted = false,
                providerRevocationStatus = "already_deleted"
            )
        }

        val providerRevocationStatus = revokeProviderAuthorization(userId, existingUser.provider)

        sessionRepository.revokeAllUserSessions(userId, reason = "account_deleted").getOrThrow()
        revokeCurrentJwt(userId, currentJwtToken)
        userRepository.deleteUser(userId).getOrThrow()
        auditLogger.logSensitiveOperation(
            userId = userId,
            operation = "account_delete",
            details = mapOf(
                "provider" to existingUser.provider.name.lowercase(),
                "status" to "deleted",
                "provider_revocation_status" to providerRevocationStatus
            )
        )

        AccountDeletionResult(
            userId = userId,
            deleted = true,
            providerRevocationStatus = providerRevocationStatus
        )
    }

    private suspend fun revokeCurrentJwt(userId: String, currentJwtToken: String?) {
        currentJwtToken?.let { token ->
            val expiresAt = jwtExpiryResolver(token) ?: defaultJwtExpiryProvider()
            sessionRepository.revokeJwtToken(token, userId, "account_deleted", expiresAt).getOrThrow()
        }
    }

    private suspend fun revokeProviderAuthorization(userId: String, provider: OAuthProvider): String {
        return when (provider) {
            OAuthProvider.APPLE -> revokeAppleAuthorization(userId)
            OAuthProvider.GOOGLE, OAuthProvider.EMAIL, OAuthProvider.GUEST -> "not_applicable"
        }
    }

    private suspend fun revokeAppleAuthorization(userId: String): String {
        val token = userRepository.getTokenByUserId(userId) ?: return "unavailable"
        val tokenValue = token.refreshToken?.takeIf { it.isNotBlank() }
            ?: token.accessToken.takeIf { it.isNotBlank() }
            ?: return "unavailable"
        val tokenTypeHint = if (!token.refreshToken.isNullOrBlank()) {
            "refresh_token"
        } else {
            "access_token"
        }
        val revocationService = appleRevocationService ?: return "unavailable"

        return revocationService.revokeUserAuthorization(tokenValue, tokenTypeHint)
            .fold(
                onSuccess = { "revoked" },
                onFailure = { "failed" }
            )
    }
}

interface AppleAccountRevocationService {
    suspend fun revokeUserAuthorization(token: String, tokenTypeHint: String): Result<Unit>
}

data class AccountDeletionResult(
    val userId: String,
    val deleted: Boolean,
    val providerRevocationStatus: String
)
