package com.guyghost.wakeve

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.auth.AppleOAuth2Service
import com.guyghost.wakeve.auth.AuthenticationService
import com.guyghost.wakeve.auth.GoogleOAuth2Service
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.routes.authRoutes
import com.guyghost.wakeve.routes.eventRoutes
import com.guyghost.wakeve.routes.participantRoutes
import com.guyghost.wakeve.routes.syncRoutes
import com.guyghost.wakeve.routes.voteRoutes
import com.guyghost.wakeve.sync.SyncService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.minutes

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