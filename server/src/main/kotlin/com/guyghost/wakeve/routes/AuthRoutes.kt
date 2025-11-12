package com.guyghost.wakeve.routes

import com.guyghost.wakeve.models.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Routes d'authentification
 */
fun Route.authRoutes() {
    val json = Json { ignoreUnknownKeys = true }
    val users = mutableMapOf<String, StoredUser>()
    val tokens = mutableMapOf<String, TokenInfo>()
    
    route("/auth") {
        // POST /api/auth/login
        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
                
                // Validation simple (à remplacer par une véritable base de données)
                val user = users.values.find { 
                    it.user.email == request.email && it.passwordHash == hashPassword(request.password)
                }
                
                if (user != null) {
                    val tokens = generateTokens(user.user)
                    call.respond(HttpStatusCode.OK, tokens.first)
                } else {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Email ou mot de passe incorrect")
                    )
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }
        
        // POST /api/auth/signup
        post("/signup") {
            try {
                val request = call.receive<SignUpRequest>()
                
                // Vérifier si l'email existe déjà
                if (users.values.any { it.user.email == request.email }) {
                    call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Cet email existe déjà")
                    )
                    return@post
                }
                
                // Créer un nouvel utilisateur
                val userId = "user-${Random.nextLong(1000000)}"
                val now = System.currentTimeMillis().toString()
                val newUser = User(
                    id = userId,
                    email = request.email,
                    name = request.name,
                    provider = AuthProvider.LOCAL,
                    createdAt = now
                )
                
                users[userId] = StoredUser(newUser, hashPassword(request.password))
                
                val tokens = generateTokens(newUser)
                call.respond(HttpStatusCode.Created, tokens.first)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }
        
        // POST /api/auth/oauth/callback
        post("/oauth/callback") {
            try {
                val request = call.receive<OAuthLoginRequest>()
                
                // Vérifier et décoder le token ID (implémentation simplifiée)
                val userInfo = verifyOAuthToken(request.provider, request.idToken)
                    ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Token OAuth invalide")
                    )
                
                // Récupérer ou créer l'utilisateur
                var user = users.values.find { it.user.email == userInfo.email }?.user
                
                if (user == null) {
                    val userId = "user-${Random.nextLong(1000000)}"
                    val now = System.currentTimeMillis().toString()
                    user = User(
                        id = userId,
                        email = userInfo.email,
                        name = userInfo.name,
                        avatar = userInfo.avatar,
                        provider = AuthProvider.valueOf(request.provider.uppercase()),
                        createdAt = now
                    )
                    users[userId] = StoredUser(user, "")
                } else {
                    // Mettre à jour l'avatar et le nom si disponibles
                    user = user.copy(
                        name = userInfo.name,
                        avatar = userInfo.avatar ?: user.avatar
                    )
                    users[user.id] = users[user.id]!!.copy(user = user)
                }
                
                val tokens = generateTokens(user)
                call.respond(HttpStatusCode.OK, tokens.first)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }
        
        // POST /api/auth/refresh
        post("/refresh") {
            try {
                val request = call.receive<RefreshTokenRequest>()
                
                // Vérifier le refresh token
                val tokenInfo = tokens[request.refreshToken]
                    ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Refresh token invalide")
                    )
                
                if (tokenInfo.refreshExpiresAt < System.currentTimeMillis()) {
                    tokens.remove(request.refreshToken)
                    return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Refresh token expiré")
                    )
                }
                
                val user = users[tokenInfo.userId]?.user
                    ?: return@post call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Utilisateur non trouvé")
                    )
                
                // Générer de nouveaux tokens
                val newTokens = generateTokens(user)
                
                // Invalider l'ancien refresh token
                tokens.remove(request.refreshToken)
                
                call.respond(HttpStatusCode.OK, newTokens.first)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }
        
        // POST /api/auth/logout
        post("/logout") {
            // Dans une vraie application, invalider le token ici
            call.respond(HttpStatusCode.OK, mapOf("message" to "Déconnecté avec succès"))
        }
    }
}

/**
 * Données stockées pour un utilisateur
 */
private data class StoredUser(
    val user: User,
    val passwordHash: String
)

/**
 * Informations de token
 */
private data class TokenInfo(
    val userId: String,
    val accessExpiresAt: Long,
    val refreshExpiresAt: Long
)

/**
 * Informations utilisateur récupérées d'OAuth
 */
private data class OAuthUserInfo(
    val email: String,
    val name: String,
    val avatar: String? = null
)

/**
 * Hash du mot de passe (implémentation simple pour développement)
 */
private fun hashPassword(password: String): String {
    return password.hashCode().toString()
}

/**
 * Génère les tokens d'accès et rafraîchissement
 */
private fun generateTokens(user: User): Pair<AuthResponse, TokenInfo> {
    val accessTokenExpiresIn = 3600L // 1 heure
    val refreshTokenExpiresIn = 7 * 24 * 3600L // 7 jours
    
    val now = System.currentTimeMillis()
    val accessToken = "access-${Random.nextLong()}"
    val refreshToken = "refresh-${Random.nextLong()}"
    
    val tokenInfo = TokenInfo(
        userId = user.id,
        accessExpiresAt = now + (accessTokenExpiresIn * 1000),
        refreshExpiresAt = now + (refreshTokenExpiresIn * 1000)
    )
    
    val authResponse = AuthResponse(
        user = user,
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = accessTokenExpiresIn
    )
    
    return authResponse to tokenInfo
}

/**
 * Vérifie et décode un token OAuth (implémentation simplifiée)
 */
private fun verifyOAuthToken(provider: String, idToken: String): OAuthUserInfo? {
    // Dans une vraie application, vérifier la signature du token auprès du provider
    // Pour le développement, accepter les tokens de test
    
    return when (provider.lowercase()) {
        "apple" -> OAuthUserInfo(
            email = "user-apple-${Random.nextLong(10000)}@example.com",
            name = "Utilisateur Apple",
            avatar = null
        )
        "google" -> OAuthUserInfo(
            email = "user-google-${Random.nextLong(10000)}@example.com",
            name = "Utilisateur Google",
            avatar = null
        )
        else -> null
    }
}
