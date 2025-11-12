package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Utilisateur authentifié avec ses informations de profil
 */
@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String,
    val avatar: String? = null,
    val provider: AuthProvider = AuthProvider.LOCAL,
    val createdAt: String, // ISO 8601
    val lastLogin: String? = null // ISO 8601
)

/**
 * Fournisseur d'authentification
 */
enum class AuthProvider {
    LOCAL, GOOGLE, APPLE
}

/**
 * Réponse d'authentification avec token et utilisateur
 */
@Serializable
data class AuthResponse(
    val user: User,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long // Secondes jusqu'à l'expiration
)

/**
 * Demande de connexion avec email et mot de passe
 */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Demande de connexion OAuth
 */
@Serializable
data class OAuthLoginRequest(
    val provider: String, // "google", "apple"
    val idToken: String, // Token ID d'OAuth
    val accessToken: String? = null // Access token (optionnel pour Apple)
)

/**
 * Demande d'inscription
 */
@Serializable
data class SignUpRequest(
    val email: String,
    val name: String,
    val password: String
)

/**
 * Demande de rafraîchissement du token
 */
@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

/**
 * Réponse de rafraîchissement du token
 */
@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)

/**
 * Session utilisateur en mémoire
 */
data class UserSession(
    val user: User,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long, // Timestamp
    val createdAt: Long // Timestamp de création de session
) {
    /**
     * Vérifie si le token a expiré
     */
    fun isAccessTokenExpired(currentTime: Long = getCurrentTimeMillis()): Boolean {
        // Considérer le token expiré 5 minutes avant l'heure d'expiration réelle
        return currentTime > (expiresAt - 5 * 60 * 1000)
    }
    
    /**
     * Vérifie si la session est valide
     */
    fun isValid(currentTime: Long = getCurrentTimeMillis()): Boolean {
        return !isAccessTokenExpired(currentTime)
    }
}

/**
 * Erreur d'authentification
 */
sealed class AuthError(message: String) : Exception(message) {
    data class InvalidCredentials(val message: String) : AuthError(message)
    data class UserNotFound(val message: String) : AuthError(message)
    data class EmailAlreadyExists(val message: String) : AuthError(message)
    data class InvalidToken(val message: String) : AuthError(message)
    data class TokenExpired(val message: String) : AuthError(message)
    data class NetworkError(val message: String) : AuthError(message)
    data class UnknownError(val message: String) : AuthError(message)
}

/**
 * Récupère le timestamp actuel en millisecondes
 */
expect fun getCurrentTimeMillis(): Long
