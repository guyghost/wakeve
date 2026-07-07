package com.guyghost.wakeve.auth.shell.services

import com.guyghost.wakeve.models.AccountDeletionResponse

/**
 * Deletes the authenticated Wakeve account on the backend.
 *
 * Local credentials must only be cleared after this call succeeds so offline
 * failures remain retryable and do not falsely imply backend erasure.
 */
interface AccountDeletionGateway {
    suspend fun deleteAccount(accessToken: String): Result<AccountDeletionResponse>
}

/**
 * Default gateway used when no platform implementation is configured.
 */
class UnconfiguredAccountDeletionGateway : AccountDeletionGateway {
    override suspend fun deleteAccount(accessToken: String): Result<AccountDeletionResponse> =
        Result.failure(IllegalStateException("Account deletion is not configured"))
}
