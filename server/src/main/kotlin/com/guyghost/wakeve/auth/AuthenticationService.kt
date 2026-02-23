package com.guyghost.wakeve.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.guyghost.wakeve.UserRepository
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.AuthResponse
import com.guyghost.wakeve.models.OAuthLoginRequest
import com.guyghost.wakeve.models.OAuthLoginResponse
import com.guyghost.wakeve.models.OAuthProvider
import com.guyghost.wakeve.models.User
import com.guyghost.wakeve.models.UserDTO
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.Date
import java.util.UUID

/**
 * Main authentication service that handles OAuth2 login flow
 */
class AuthenticationService(
    private val db: WakeveDb,
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
            OAuthProvider.EMAIL, OAuthProvider.GUEST ->
                throw OAuth2Exception("Use dedicated email/guest endpoints instead of OAuth")
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
     * Authentifie un utilisateur par email après vérification OTP.
     *
     * Cherche un utilisateur existant par email ou en crée un nouveau.
     * Génère un JWT et un refresh token pour la session.
     *
     * @param email L'adresse email vérifiée
     * @return AuthResponse contenant les tokens et les informations utilisateur
     */
    suspend fun loginWithEmail(email: String): Result<AuthResponse> = runCatching {
        // Chercher ou créer l'utilisateur par email
        val user = userRepository.getUserByEmail(email)
            ?: userRepository.createUser(
                providerId = "email_${UUID.randomUUID()}",
                email = email,
                name = email.substringBefore("@"),
                avatarUrl = null,
                provider = OAuthProvider.EMAIL
            ).getOrThrow()

        // Générer le JWT
        val jwtToken = generateJwtToken(user)

        // Créer un token de rafraîchissement en base
        val expiresAt = calculateTokenExpiry(3600)
        val refreshTokenValue = UUID.randomUUID().toString()
        userRepository.createToken(
            userId = user.id,
            accessToken = jwtToken,
            refreshToken = refreshTokenValue,
            expiresAt = expiresAt
        ).getOrThrow()

        AuthResponse(
            user = UserDTO(
                id = user.id,
                email = user.email,
                name = user.name,
                isGuest = false,
                authMethod = "EMAIL"
            ),
            accessToken = jwtToken,
            refreshToken = refreshTokenValue,
            expiresInSeconds = 3600
        )
    }

    /**
     * Crée une session invité avec permissions limitées.
     *
     * Génère un utilisateur invité temporaire avec un JWT contenant le rôle "guest".
     * Les invités n'ont pas de refresh token et des permissions réduites.
     *
     * @param deviceId Identifiant optionnel de l'appareil
     * @return AuthResponse contenant le token invité
     */
    suspend fun loginAsGuest(deviceId: String?): Result<AuthResponse> = runCatching {
        val guestId = "guest_${UUID.randomUUID()}"
        val effectiveDeviceId = deviceId ?: "device_${UUID.randomUUID()}"

        // Créer un utilisateur invité en base
        val user = userRepository.createUser(
            providerId = effectiveDeviceId,
            email = "$guestId@guest.wakeve.local",
            name = "Invité",
            avatarUrl = null,
            provider = OAuthProvider.GUEST
        ).getOrThrow()

        // Générer le JWT avec rôle USER (permissions standard limitées)
        val jwtToken = generateJwtToken(user)

        AuthResponse(
            user = UserDTO(
                id = user.id,
                email = null,
                name = "Invité",
                isGuest = true,
                authMethod = "GUEST"
            ),
            accessToken = jwtToken,
            refreshToken = null,  // Pas de refresh token pour les invités
            expiresInSeconds = 3600
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

    /**
     * Get authorization URL for OAuth provider
     */
    fun getAuthorizationUrl(provider: OAuthProvider, state: String): String {
        return getOAuthService(provider).getAuthorizationUrl(state)
    }

    private fun getOAuthService(provider: OAuthProvider): OAuth2Service {
        return when (provider) {
            OAuthProvider.GOOGLE -> googleOAuth2 ?: throw OAuth2Exception("Google OAuth2 not configured")
            OAuthProvider.APPLE -> appleOAuth2 ?: throw OAuth2Exception("Apple OAuth2 not configured")
            OAuthProvider.EMAIL, OAuthProvider.GUEST ->
                throw OAuth2Exception("${provider.name} does not use OAuth2 service")
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

        // Get permissions for user's role
        val permissions = com.guyghost.wakeve.auth.RolePermissions
            .getPermissions(user.role)
            .map { it.name }

        return JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withSubject(user.id)
            .withClaim("userId", user.id)
            .withClaim("email", user.email)
            .withClaim("provider", user.provider.name)
            .withClaim("role", user.role.name)  // Add role claim
            .withArrayClaim("permissions", permissions.toTypedArray())  // Add permissions claim
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