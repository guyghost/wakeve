package com.guyghost.wakeve.auth.shell.services

import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.AuthResult
import com.guyghost.wakeve.auth.core.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JVM implementation of AuthService for testing and desktop applications.
 */
actual class AuthService(
    private val tokenStorage: TokenStorage = InMemoryTokenStorage()
) {

    actual suspend fun signInWithGoogle(): AuthResult = withContext(Dispatchers.Main) {
        AuthResult.success(
            User(
                id = "jvm_google_user",
                email = "user@gmail.com",
                name = "JVM Google User",
                authMethod = AuthMethod.GOOGLE,
                isGuest = false,
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            ),
            com.guyghost.wakeve.auth.core.models.AuthToken.createLongLived(
                value = "jvm_google_token",
                expiresInDays = 30
            )
        )
    }

    actual suspend fun signInWithApple(): AuthResult = withContext(Dispatchers.Main) {
        AuthResult.success(
            User(
                id = "jvm_apple_user",
                email = "user@icloud.com",
                name = "JVM Apple User",
                authMethod = AuthMethod.APPLE,
                isGuest = false,
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            ),
            com.guyghost.wakeve.auth.core.models.AuthToken.createLongLived(
                value = "jvm_apple_token",
                expiresInDays = 30
            )
        )
    }

    actual suspend fun signOut() {
        withContext(Dispatchers.IO) {
            tokenStorage.clearAll()
        }
    }

    actual suspend fun isAuthenticated(): Boolean {
        return withContext(Dispatchers.IO) {
            tokenStorage.getString(TokenKeys.ACCESS_TOKEN) != null
        }
    }

    actual suspend fun getCurrentUser(): User? {
        return withContext(Dispatchers.IO) {
            val userId = tokenStorage.getString(TokenKeys.USER_ID)
            val authMethodStr = tokenStorage.getString(TokenKeys.AUTH_METHOD)
            
            if (userId != null && authMethodStr != null) {
                User(
                    id = userId,
                    email = null,
                    name = null,
                    authMethod = AuthMethod.valueOf(authMethodStr),
                    isGuest = false,
                    createdAt = System.currentTimeMillis(),
                    lastLoginAt = System.currentTimeMillis()
                )
            } else {
                null
            }
        }
    }

    actual suspend fun refreshToken(): AuthResult {
        return AuthResult.success(
            User(
                id = "jvm_refreshed",
                email = null,
                name = null,
                authMethod = AuthMethod.GOOGLE,
                isGuest = false,
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            ),
            com.guyghost.wakeve.auth.core.models.AuthToken.createLongLived(
                value = "jvm_refreshed_token",
                expiresInDays = 30
            )
        )
    }

    actual suspend fun isProviderAvailable(provider: AuthMethod): Boolean {
        return provider == AuthMethod.GOOGLE || provider == AuthMethod.APPLE
    }
}

/**
 * In-memory TokenStorage implementation for JVM.
 */
class InMemoryTokenStorage : TokenStorage {
    private val storage = mutableMapOf<String, String>()

    override suspend fun storeString(key: String, value: String) {
        storage[key] = value
    }

    override suspend fun getString(key: String): String? {
        return storage[key]
    }

    override suspend fun remove(key: String) {
        storage.remove(key)
    }

    override suspend fun contains(key: String): Boolean {
        return storage.containsKey(key)
    }

    override suspend fun clearAll() {
        storage.clear()
    }
}
