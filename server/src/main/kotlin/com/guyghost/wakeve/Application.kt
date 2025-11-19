package com.guyghost.wakeve

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.auth.*
import com.guyghost.wakeve.database.WakevDb
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.json.Json
import com.guyghost.wakeve.sync.SyncService
import com.guyghost.wakeve.routes.*

const val SERVER_PORT = 8080

fun main() {
    // Initialize database
    val database = DatabaseProvider.getDatabase(JvmDatabaseFactory("wakev_server.db"))
    val eventRepository = DatabaseEventRepository(database)

    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = {
        module(database, eventRepository)
    }).start(wait = true)
}

fun Application.module(
    database: WakevDb,
    eventRepository: DatabaseEventRepository = DatabaseEventRepository(database)
) {
    // Initialize authentication services
    val jwtSecret = System.getenv("JWT_SECRET") ?: "default-secret-key-change-in-production"
    val jwtIssuer = System.getenv("JWT_ISSUER") ?: "wakev-api"
    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "wakev-client"

    // OAuth2 services (optional - only if configured)
    val googleOAuth2 = System.getenv("GOOGLE_CLIENT_ID")?.let { clientId ->
        System.getenv("GOOGLE_CLIENT_SECRET")?.let { clientSecret ->
            GoogleOAuth2Service(
                clientId = clientId,
                clientSecret = clientSecret,
                redirectUri = System.getenv("GOOGLE_REDIRECT_URI") ?: "http://localhost:8080/auth/google/callback"
            )
        }
    }

    val appleOAuth2 = System.getenv("APPLE_CLIENT_ID")?.let { clientId ->
        System.getenv("APPLE_TEAM_ID")?.let { teamId ->
            System.getenv("APPLE_KEY_ID")?.let { keyId ->
                System.getenv("APPLE_PRIVATE_KEY")?.let { privateKey ->
                    AppleOAuth2Service(
                        clientId = clientId,
                        teamId = teamId,
                        keyId = keyId,
                        privateKey = privateKey,
                        redirectUri = System.getenv("APPLE_REDIRECT_URI") ?: "http://localhost:8080/auth/apple/callback"
                    )
                }
            }
        }
    }

    val authService = AuthenticationService(
        db = database,
        jwtSecret = jwtSecret,
        jwtIssuer = jwtIssuer,
        jwtAudience = jwtAudience,
        googleService = googleOAuth2,
        appleService = appleOAuth2
    )

    val syncService = SyncService(database)

    // Install plugins
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        })
    }

    // Install rate limiting for auth endpoints
    install(RateLimit) {
        register(RateLimitName("auth")) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
        }
    }

    // Install JWT authentication
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Wakev API"
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .withAudience(jwtAudience)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId")?.asString() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    // Configure routing
    routing {
        // Health check endpoint
        get("/health") {
            call.respondText("OK")
        }

        // Authentication endpoints (public but rate limited)
        rateLimit(RateLimitName("auth")) {
            authRoutes(authService)
        }

        // API endpoints (protected by JWT authentication)
        authenticate("auth-jwt") {
            route("/api") {
                eventRoutes(eventRepository)
                participantRoutes(eventRepository)
                voteRoutes(eventRepository)
                syncRoutes(syncService)
            }
        }

        // Root endpoint for compatibility
        get("/") {
            call.respondText("Wakev API v1.0")
        }
    }
}