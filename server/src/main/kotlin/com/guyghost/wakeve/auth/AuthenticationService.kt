package com.guyghost.wakeve.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.guyghost.wakeve.UserRepository
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.OAuthLoginRequest
import com.guyghost.wakeve.models.OAuthLoginResponse
import com.guyghost.wakeve.models.OAuthProvider
import com.guyghost.wakeve.models.User
import com.guyghost.wakeve.models.UserToken
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.*

/**
 * Main authentication service that handles OAuth2 login flow
 */
class AuthenticationService(
    private val db: WakevDb,
    private val jwtSecret: String,
    private val jwtIssuer: String,
    private val jwtAudience: String,
    googleService: GoogleOAuth2Service? = null,
    appleService: AppleOAuth2Service? = null
) {
    private val userRepository = UserRepository(db)
    private val googleOAuth2 = googleService
    private val appleOAuth2 = appleService
    private val jwtAlgorithm = Algorithm.HMAC256(jwtSecret)

    /**
     * Handle OAuth2 login from authorization code
     */
    suspend fun loginWithOAuth(request: OAuthLoginRequest): Result<OAuthLoginResponse> = runCatching {
        val provider = OAuthProvider.valueOf(request.provider.uppercase())
        val service = getOAuthService(provider)
        val code = request.authorizationCode ?: request.idToken ?: throw OAuth2Exception("No authorization code or id token provided")
        val tokenResponse = service.exchangeCodeForToken(code)

        // Get user info from OAuth provider
        val userInfo = when (provider) {
            OAuthProvider.GOOGLE -> service.getUserInfo(tokenResponse.access_token)
            OAuthProvider.APPLE -> {
                // For Apple, user info comes from the request
                appleOAuth2?.parseUserInfoFromAuthResponse(request.accessToken)
                    ?: throw OAuth2Exception("Apple user info not provided")
            }
        }

        // Find or create user
        val user = findOrCreateUser(userInfo)

        // Create or update token
        val expiresAt = calculateTokenExpiry(tokenResponse.expires_in)
        val userToken = userRepository.createToken(
            userId = user.id,
            accessToken = tokenResponse.access_token,
            refreshToken = tokenResponse.refresh_token,
            expiresAt = expiresAt,
            scope = tokenResponse.scope
        ).getOrThrow()

        // Generate JWT for our API
        val jwtToken = generateJwtToken(user)

        OAuthLoginResponse(
            user = com.guyghost.wakeve.models.UserResponse(
                id = user.id,
                email = user.email,
                name = user.name ?: "",
                avatarUrl = user.avatarUrl,
                provider = user.provider.name.lowercase(),
                createdAt = user.createdAt
            ),
            accessToken = jwtToken,
            refreshToken = userToken.refreshToken,
            expiresIn = tokenResponse.expires_in ?: 3600,
            tokenType = "Bearer"
        )
    }

    /**
     * Refresh an access token using refresh token
     */
    suspend fun refreshToken(refreshToken: String): Result<OAuthLoginResponse> = runCatching {
        // Find the user token by refresh token
        val userToken = userRepository.getUserTokenByRefreshToken(refreshToken)
            ?: throw OAuth2Exception("Invalid refresh token")

        // Check if refresh token is expired
        val now = Instant.now()
        val refreshExpiry = Instant.parse(userToken.expiresAt)
        if (now.isAfter(refreshExpiry)) {
            throw OAuth2Exception("Refresh token expired")
        }

        // Get the user
        val user = userRepository.getUserById(userToken.userId)
            ?: throw OAuth2Exception("User not found")

        // Generate new JWT token
        val jwtToken = generateJwtToken(user)

        // Update the token in database with new expiry
        val newExpiry = calculateTokenExpiry(3600) // 1 hour
        userRepository.updateTokenExpiry(userToken.id, newExpiry)

        OAuthLoginResponse(
            user = com.guyghost.wakeve.models.UserResponse(
                id = user.id,
                email = user.email,
                name = user.name ?: "",
                avatarUrl = user.avatarUrl,
                provider = user.provider.name.lowercase(),
                createdAt = user.createdAt
            ),
            accessToken = jwtToken,
            refreshToken = refreshToken, // Keep the same refresh token
            expiresIn = 3600,
            tokenType = "Bearer"
        )
    }

    /**
     * Validate JWT token and return user
     */
    fun validateToken(call: ApplicationCall): User? {
        val principal = call.principal<JWTPrincipal>() ?: return null
        val userId = principal.payload.getClaim("userId")?.asString() ?: return null

        return runBlocking {
            userRepository.getUserById(userId)
        }
    }

    /**
     * Clean up expired tokens
     */
    suspend fun cleanupExpiredTokens(): Result<Unit> {
        return userRepository.cleanupExpiredTokens()
    }

    private fun getOAuthService(provider: OAuthProvider): OAuth2Service {
        return when (provider) {
            OAuthProvider.GOOGLE -> googleOAuth2 ?: throw OAuth2Exception("Google OAuth2 not configured")
            OAuthProvider.APPLE -> appleOAuth2 ?: throw OAuth2Exception("Apple OAuth2 not configured")
        }
    }

    private suspend fun findOrCreateUser(userInfo: OAuthUserInfo): User {
        // Try to find existing user
        val existingUser = userRepository.getUserByProviderId(userInfo.id, userInfo.provider)
            ?: userRepository.getUserByEmail(userInfo.email)

        return if (existingUser != null) {
            // Update user info if needed
            if (existingUser.name != userInfo.name || existingUser.avatarUrl != userInfo.avatarUrl) {
                userRepository.updateUser(
                    userId = existingUser.id,
                    name = userInfo.name ?: existingUser.name,
                    avatarUrl = userInfo.avatarUrl ?: existingUser.avatarUrl
                ).getOrThrow()
            } else {
                existingUser
            }
        } else {
            // Create new user
            userRepository.createUser(
                providerId = userInfo.id,
                email = userInfo.email,
                name = userInfo.name ?: "Unknown",
                avatarUrl = userInfo.avatarUrl,
                provider = userInfo.provider
            ).getOrThrow()
        }
    }

    private fun calculateTokenExpiry(expiresIn: Long?): String {
        val expiry = Instant.now().plusSeconds(expiresIn ?: 3600)
        return expiry.toString()
    }

    /**
     * Generate JWT token for user (public for testing)
     */
    fun generateJwtToken(user: User): String {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(3600) // 1 hour expiry

        return JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withSubject(user.id)
            .withClaim("userId", user.id)
            .withClaim("email", user.email)
            .withClaim("provider", user.provider.name)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expiresAt))
            .sign(jwtAlgorithm)
    }

    /**
     * Verify and decode a JWT token (public for testing)
     */
    fun verifyJwtToken(token: String): DecodedJWT? {
        return try {
            val verifier = JWT.require(jwtAlgorithm)
                .withIssuer(jwtIssuer)
                .withAudience(jwtAudience)
                .build()
            verifier.verify(token)
        } catch (e: JWTVerificationException) {
            null
        }
    }

    /**
     * Extract user from verified JWT token
     */
    fun getUserFromJwtToken(token: String): User? {
        val decoded = verifyJwtToken(token) ?: return null
        val userId = decoded.subject ?: return null

        return runBlocking {
            userRepository.getUserById(userId)
        }
    }
}