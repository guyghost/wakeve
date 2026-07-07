package com.guyghost.wakeve.auth.shell.statemachine

import com.guyghost.wakeve.auth.shell.services.AccountDeletionGateway
import com.guyghost.wakeve.auth.shell.services.AuthService
import com.guyghost.wakeve.auth.shell.services.EmailAuthService
import com.guyghost.wakeve.auth.shell.services.GuestModeService
import com.guyghost.wakeve.auth.shell.services.TokenKeys
import com.guyghost.wakeve.auth.shell.services.TokenStorage
import com.guyghost.wakeve.models.AccountDeletionResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthStateMachineAccountDeletionTest {

    @Test
    fun deleteAccount_clearsLocalStateAfterBackendSuccess() = runTest {
        val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val tokenStorage = RecordingTokenStorage()
        tokenStorage.storeString(TokenKeys.ACCESS_TOKEN, "access-token")
        val gateway = FakeAccountDeletionGateway(
            Result.success(
                AccountDeletionResponse(
                    success = true,
                    deleted = true,
                    message = "Account deleted successfully",
                    localCleanupRequired = true,
                    providerRevocationStatus = "NOT_APPLICABLE"
                )
            )
        )
        val stateMachine = AuthStateMachine(
            authService = AuthService(tokenStorage = tokenStorage),
            emailAuthService = EmailAuthService(),
            guestModeService = GuestModeService(tokenStorage),
            tokenStorage = tokenStorage,
            accountDeletionGateway = gateway,
            scope = scope
        )

        stateMachine.handleIntent(AuthContract.Intent.DeleteAccount)

        assertTrue(gateway.deleteCalled)
        assertFalse(tokenStorage.hasAccessToken)
        assertFalse(stateMachine.state.value.isAuthenticated)
        assertFalse(stateMachine.state.value.isGuest)
        assertTrue(
            stateMachine.sideEffect.first { it is AuthContract.SideEffect.NavigateToAuthAfterDeletion }
                is AuthContract.SideEffect.NavigateToAuthAfterDeletion
        )
    }

    @Test
    fun deleteAccount_keepsCredentialsWhenBackendFails() = runTest {
        val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val tokenStorage = RecordingTokenStorage()
        tokenStorage.storeString(TokenKeys.ACCESS_TOKEN, "access-token")
        val gateway = FakeAccountDeletionGateway(Result.failure(Exception("network")))
        val stateMachine = AuthStateMachine(
            authService = AuthService(tokenStorage = tokenStorage),
            emailAuthService = EmailAuthService(),
            guestModeService = GuestModeService(tokenStorage),
            tokenStorage = tokenStorage,
            accountDeletionGateway = gateway,
            scope = scope
        )

        stateMachine.handleIntent(AuthContract.Intent.DeleteAccount)

        assertTrue(gateway.deleteCalled)
        assertTrue(tokenStorage.hasAccessToken)
    }

    @Test
    fun deleteGuestData_clearsLocalGuestSessionWithoutBackendCall() = runTest {
        val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val tokenStorage = RecordingTokenStorage()
        tokenStorage.storeString("guest_user_id", "guest-123")
        val gateway = FakeAccountDeletionGateway(
            Result.success(
                AccountDeletionResponse(
                    success = true,
                    deleted = true,
                    message = "unused",
                    localCleanupRequired = true,
                    providerRevocationStatus = "NOT_APPLICABLE"
                )
            )
        )
        val stateMachine = AuthStateMachine(
            authService = AuthService(tokenStorage = tokenStorage),
            emailAuthService = EmailAuthService(),
            guestModeService = GuestModeService(tokenStorage),
            tokenStorage = tokenStorage,
            accountDeletionGateway = gateway,
            scope = scope
        )

        stateMachine.handleIntent(AuthContract.Intent.DeleteGuestData)

        assertFalse(gateway.deleteCalled)
        assertFalse(tokenStorage.contains("guest_user_id"))
        assertTrue(
            stateMachine.sideEffect.first { it is AuthContract.SideEffect.NavigateToAuthAfterDeletion }
                is AuthContract.SideEffect.NavigateToAuthAfterDeletion
        )
    }

    private class FakeAccountDeletionGateway(
        private val result: Result<AccountDeletionResponse>
    ) : AccountDeletionGateway {
        var deleteCalled = false

        override suspend fun deleteAccount(accessToken: String): Result<AccountDeletionResponse> {
            deleteCalled = true
            assertEquals("access-token", accessToken)
            return result
        }
    }

    private class RecordingTokenStorage : TokenStorage {
        private val storage = mutableMapOf<String, String>()
        var hasAccessToken = false

        override suspend fun storeString(key: String, value: String) {
            storage[key] = value
            if (key == TokenKeys.ACCESS_TOKEN) {
                hasAccessToken = true
            }
        }

        override suspend fun getString(key: String): String? = storage[key]

        override suspend fun remove(key: String) {
            storage.remove(key)
            if (key == TokenKeys.ACCESS_TOKEN) {
                hasAccessToken = false
            }
        }

        override suspend fun contains(key: String): Boolean = storage.containsKey(key)

        override suspend fun clearAll() {
            storage.clear()
            hasAccessToken = false
        }
    }
}
