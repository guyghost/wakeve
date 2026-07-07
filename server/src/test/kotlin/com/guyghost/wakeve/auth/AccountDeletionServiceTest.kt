package com.guyghost.wakeve.auth

import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.repository.UserRepository
import com.guyghost.wakeve.security.SecurityAuditLogger
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountDeletionServiceTest {

    private lateinit var database: WakeveDb
    private lateinit var userRepository: UserRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var auditLogger: CapturingAuditLogger
    private lateinit var appleRevocationService: FakeAppleRevocationService

    @BeforeTest
    fun setup() {
        DatabaseProvider.resetDatabase()
        database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        userRepository = UserRepository(database)
        sessionRepository = SessionRepository(database)
        auditLogger = CapturingAuditLogger()
        appleRevocationService = FakeAppleRevocationService()
    }

    @AfterTest
    fun teardown() {
        DatabaseProvider.resetDatabase()
    }

    @Test
    fun `apple deletion revokes stored refresh token before deleting local data and audits result`() = runBlocking {
        seedUser("apple-delete-user", provider = "apple")
        seedToken("apple-delete-user", accessToken = "apple-access", refreshToken = "apple-refresh")
        val service = createService()

        val result = service.deleteAccount("apple-delete-user").getOrThrow()

        assertEquals(true, result.deleted)
        assertEquals("revoked", result.providerRevocationStatus)
        assertEquals(listOf("apple-refresh" to "refresh_token"), appleRevocationService.calls)
        assertNull(database.userQueries.selectUserById("apple-delete-user").executeAsOneOrNull())
        assertNull(database.userQueries.selectTokenByUserId("apple-delete-user").executeAsOneOrNull())
        val audit = auditLogger.events.single()
        assertEquals("account_delete", audit.operation)
        assertEquals("apple", audit.details["provider"])
        assertEquals("revoked", audit.details["provider_revocation_status"])
    }

    @Test
    fun `apple revocation failure does not block local account deletion and is audited`() = runBlocking {
        seedUser("apple-failure-user", provider = "apple")
        seedToken("apple-failure-user", accessToken = "apple-access", refreshToken = "apple-refresh")
        appleRevocationService.failure = OAuth2Exception("apple unavailable")
        val service = createService()

        val result = service.deleteAccount("apple-failure-user").getOrThrow()

        assertEquals(true, result.deleted)
        assertEquals("failed", result.providerRevocationStatus)
        assertNull(database.userQueries.selectUserById("apple-failure-user").executeAsOneOrNull())
        val audit = auditLogger.events.single()
        assertEquals("account_delete", audit.operation)
        assertEquals("failed", audit.details["provider_revocation_status"])
    }

    @Test
    fun `missing user deletion is idempotent and emits audit evidence`() = runBlocking {
        val service = createService()

        val result = service.deleteAccount("already-deleted-user").getOrThrow()

        assertEquals(false, result.deleted)
        assertEquals("already_deleted", result.providerRevocationStatus)
        val audit = auditLogger.events.single()
        assertEquals("account_delete_idempotent", audit.operation)
        assertEquals("already_deleted", audit.details["status"])
    }

    private fun createService(): AccountDeletionService {
        return AccountDeletionService(
            userRepository = userRepository,
            sessionRepository = sessionRepository,
            auditLogger = auditLogger,
            appleRevocationService = appleRevocationService
        )
    }

    private fun seedUser(userId: String, provider: String) {
        database.userQueries.insertUser(
            id = userId,
            provider_id = "provider-$userId",
            email = "$userId@example.com",
            name = "Delete Test",
            avatar_url = null,
            provider = provider,
            role = "USER",
            created_at = "2026-06-07T00:00:00Z",
            updated_at = "2026-06-07T00:00:00Z"
        )
    }

    private fun seedToken(userId: String, accessToken: String, refreshToken: String?) {
        database.userQueries.insertToken(
            id = "token-$userId",
            user_id = userId,
            access_token = accessToken,
            refresh_token = refreshToken,
            token_type = "Bearer",
            expires_at = "2099-01-01T00:00:00Z",
            scope = null,
            created_at = "2026-06-07T00:00:00Z",
            updated_at = "2026-06-07T00:00:00Z"
        )
    }
}

private class FakeAppleRevocationService : AppleAccountRevocationService {
    val calls = mutableListOf<Pair<String, String>>()
    var failure: Throwable? = null

    override suspend fun revokeUserAuthorization(token: String, tokenTypeHint: String): Result<Unit> {
        calls += token to tokenTypeHint
        return failure?.let { Result.failure(it) } ?: Result.success(Unit)
    }
}

private class CapturingAuditLogger : SecurityAuditLogger {
    val events = mutableListOf<CapturedAuditEvent>()

    override fun logSensitiveOperation(userId: String, operation: String, details: Map<String, String>) {
        events += CapturedAuditEvent(userId, operation, details)
    }
}

private data class CapturedAuditEvent(
    val userId: String,
    val operation: String,
    val details: Map<String, String>
)
