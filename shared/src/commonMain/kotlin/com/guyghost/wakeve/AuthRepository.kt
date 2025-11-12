package com.guyghost.wakeve

import com.guyghost.wakeve.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository pour la gestion de l'authentification
 * Gère les sessions, les tokens et la communication avec le serveur
 */
class AuthRepository(
    private val httpClient: HttpClient,
    private val secureStorage: SecureStorage,
    private val baseUrl: String = "http://localhost:8080"
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var currentSession: UserSession? = null
    
    /**
     * Récupère la session actuelle s'il existe
     */
    fun getCurrentSession(): UserSession? {
        return currentSession
    }
    
    /**
     * Récupère l'utilisateur actuel
     */
    fun getCurrentUser(): User? {
        return currentSession?.user
    }
    
    /**
     * Vérifie si l'utilisateur est authentifié
     */
    fun isAuthenticated(): Boolean {
        return currentSession?.isValid() == true
    }
    
    /**
     * Se connecter avec email et mot de passe
     */
    suspend fun login(email: String, password: String): Result<User> = try {
        val request = LoginRequest(email, password)
        val requestBody = json.encodeToString(request)
        
        val response = httpClient.post(
            url = "$baseUrl/api/auth/login",
            body = requestBody,
            headers = mapOf(
                "Content-Type" to "application/json"
            )
        )
        
        if (response.statusCode != 200) {
            return Result.failure(
                AuthError.InvalidCredentials("Email ou mot de passe incorrect")
            )
        }
        
        val authResponse = json.decodeFromString<AuthResponse>(response.body)
        createSession(authResponse)
        
        Result.success(authResponse.user)
    } catch (e: Exception) {
        Result.failure(AuthError.NetworkError("Erreur de connexion: ${e.message}"))
    }
    
    /**
     * Se connecter via OAuth (Google ou Apple)
     */
    suspend fun loginWithOAuth(
        provider: String,
        idToken: String,
        accessToken: String? = null
    ): Result<User> = try {
        val request = OAuthLoginRequest(
            provider = provider,
            idToken = idToken,
            accessToken = accessToken
        )
        val requestBody = json.encodeToString(request)
        
        val response = httpClient.post(
            url = "$baseUrl/api/auth/oauth/callback",
            body = requestBody,
            headers = mapOf(
                "Content-Type" to "application/json"
            )
        )
        
        if (response.statusCode != 200) {
            return Result.failure(
                AuthError.InvalidToken("Authentification OAuth échouée")
            )
        }
        
        val authResponse = json.decodeFromString<AuthResponse>(response.body)
        createSession(authResponse)
        
        Result.success(authResponse.user)
    } catch (e: Exception) {
        Result.failure(AuthError.NetworkError("Erreur OAuth: ${e.message}"))
    }
    
    /**
     * S'inscrire avec email et mot de passe
     */
    suspend fun signUp(email: String, name: String, password: String): Result<User> = try {
        val request = SignUpRequest(email, name, password)
        val requestBody = json.encodeToString(request)
        
        val response = httpClient.post(
            url = "$baseUrl/api/auth/signup",
            body = requestBody,
            headers = mapOf(
                "Content-Type" to "application/json"
            )
        )
        
        when (response.statusCode) {
            201 -> {
                val authResponse = json.decodeFromString<AuthResponse>(response.body)
                createSession(authResponse)
                Result.success(authResponse.user)
            }
            409 -> Result.failure(AuthError.EmailAlreadyExists("Cet email existe déjà"))
            else -> Result.failure(AuthError.UnknownError("Erreur lors de l'inscription"))
        }
    } catch (e: Exception) {
        Result.failure(AuthError.NetworkError("Erreur d'inscription: ${e.message}"))
    }
    
    /**
     * Se déconnecter
     */
    suspend fun logout(): Result<Unit> = try {
        val token = currentSession?.accessToken
        if (token != null) {
            httpClient.post(
                url = "$baseUrl/api/auth/logout",
                body = "",
                headers = mapOf(
                    "Authorization" to "Bearer $token",
                    "Content-Type" to "application/json"
                )
            )
        }
        
        clearSession()
        Result.success(Unit)
    } catch (e: Exception) {
        // Forcer le nettoyage même en cas d'erreur
        clearSession()
        Result.failure(AuthError.NetworkError("Erreur de déconnexion: ${e.message}"))
    }
    
    /**
     * Rafraîchir le token d'accès
     */
    suspend fun refreshAccessToken(): Result<String> = try {
        val refreshToken = currentSession?.refreshToken
            ?: return Result.failure(AuthError.InvalidToken("Pas de refresh token disponible"))
        
        val request = RefreshTokenRequest(refreshToken)
        val requestBody = json.encodeToString(request)
        
        val response = httpClient.post(
            url = "$baseUrl/api/auth/refresh",
            body = requestBody,
            headers = mapOf(
                "Content-Type" to "application/json"
            )
        )
        
        if (response.statusCode != 200) {
            clearSession()
            return Result.failure(AuthError.TokenExpired("Refresh token expiré"))
        }
        
        val refreshResponse = json.decodeFromString<RefreshTokenResponse>(response.body)
        val expiresAt = getCurrentTimeMillis() + (refreshResponse.expiresIn * 1000)
        
        currentSession = currentSession?.copy(
            accessToken = refreshResponse.accessToken,
            refreshToken = refreshResponse.refreshToken,
            expiresAt = expiresAt
        )
        
        // Sauvegarder en stockage sécurisé
        currentSession?.let { saveSessionToStorage(it) }
        
        Result.success(refreshResponse.accessToken)
    } catch (e: Exception) {
        Result.failure(AuthError.NetworkError("Erreur de rafraîchissement: ${e.message}"))
    }
    
    /**
     * Obtient le token actuel avec rafraîchissement automatique si nécessaire
     */
    suspend fun getValidAccessToken(): Result<String> {
        val session = currentSession
            ?: return Result.failure(AuthError.InvalidToken("Pas de session active"))
        
        if (!session.isAccessTokenExpired()) {
            return Result.success(session.accessToken)
        }
        
        return refreshAccessToken()
    }
    
    /**
     * Initialise la session depuis le stockage sécurisé
     */
    suspend fun restoreSession(): Result<User> = try {
        val sessionData = secureStorage.getSession()
            ?: return Result.failure(AuthError.InvalidToken("Pas de session sauvegardée"))
        
        currentSession = sessionData
        
        // Vérifier si le token doit être rafraîchi
        if (sessionData.isAccessTokenExpired()) {
            val refreshResult = refreshAccessToken()
            if (refreshResult.isFailure) {
                clearSession()
                return Result.failure(refreshResult.exceptionOrNull() ?: Exception("Token refresh failed"))
            }
        }
        
        val user = sessionData.user
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(AuthError.NetworkError("Erreur de restauration: ${e.message}"))
    }
    
    /**
     * Crée une nouvelle session et la sauvegarde
     */
    private suspend fun createSession(authResponse: AuthResponse) {
        val expiresAt = getCurrentTimeMillis() + (authResponse.expiresIn * 1000)
        val session = UserSession(
            user = authResponse.user,
            accessToken = authResponse.accessToken,
            refreshToken = authResponse.refreshToken,
            expiresAt = expiresAt,
            createdAt = getCurrentTimeMillis()
        )
        
        currentSession = session
        saveSessionToStorage(session)
    }
    
    /**
     * Sauvegarde la session en stockage sécurisé
     */
    private suspend fun saveSessionToStorage(session: UserSession) {
        secureStorage.saveSession(session)
    }
    
    /**
     * Efface la session courante
     */
    private fun clearSession() {
        currentSession = null
        secureStorage.clearSession()
    }
}

/**
 * Interface pour le stockage sécurisé des tokens
 */
interface SecureStorage {
    suspend fun saveSession(session: UserSession)
    suspend fun getSession(): UserSession?
    fun clearSession()
}

/**
 * Interface pour les requêtes HTTP
 */
interface HttpClient {
    suspend fun post(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap()
    ): HttpResponse
    
    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): HttpResponse
}

/**
 * Réponse HTTP
 */
data class HttpResponse(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap()
)
