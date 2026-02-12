package com.guyghost.wakeve

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.analytics.AnalyticsDashboard
import com.guyghost.wakeve.auth.AppleOAuth2Service
import com.guyghost.wakeve.auth.AuthenticationService
import com.guyghost.wakeve.auth.GoogleOAuth2Service
import com.guyghost.wakeve.cache.JwtBlacklistCache
import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.calendar.PlatformCalendarServiceImpl
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.metrics.AuthMetricsCollector
import com.guyghost.wakeve.routes.ChatService
import com.guyghost.wakeve.routes.analyticsRoutes
import com.guyghost.wakeve.routes.authRoutes
import com.guyghost.wakeve.routes.budgetRoutes
import com.guyghost.wakeve.routes.calendarRoutes
import com.guyghost.wakeve.routes.chatRoutes
import com.guyghost.wakeve.routes.chatWebSocketRoute
import com.guyghost.wakeve.routes.commentRoutes
import com.guyghost.wakeve.routes.eventRoutes
import com.guyghost.wakeve.routes.accommodationRoutes
import com.guyghost.wakeve.routes.mealRoutes
import com.guyghost.wakeve.routes.meetingProxyRoutes
import com.guyghost.wakeve.routes.participantRoutes
import com.guyghost.wakeve.routes.potentialLocationRoutes
import com.guyghost.wakeve.routes.scenarioRoutes
import com.guyghost.wakeve.routes.sessionRoutes
import com.guyghost.wakeve.routes.syncRoutes
import com.guyghost.wakeve.routes.voteRoutes
import com.guyghost.wakeve.sync.SyncService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.request.host
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.minutes

const val SERVER_PORT = 8080

/**
 * Configuration for JWT Blacklist checking plugin.
 */
class JWTBlacklistConfig {
    var sessionRepository: SessionRepository? = null
    var jwtBlacklistCache: JwtBlacklistCache? = null
}

/**
 * Plugin to check JWT tokens against blacklist.
 *
 * This plugin intercepts authenticated requests and checks if the JWT token
 * has been revoked/blacklisted. If the token is blacklisted, it responds
 * with 401 Unauthorized.
 */
val JWTBlacklistPlugin = createRouteScopedPlugin(
    name = "JWTBlacklistPlugin",
    createConfiguration = ::JWTBlacklistConfig
) {
    val sessionRepository = pluginConfig.sessionRepository
        ?: error("SessionRepository must be configured for JWTBlacklistPlugin")
    val jwtBlacklistCache = pluginConfig.jwtBlacklistCache
        ?: error("JwtBlacklistCache must be configured for JWTBlacklistPlugin")

    onCall { call ->
        // Get JWT principal (set by JWT authentication)
        val principal = call.principal<JWTPrincipal>() ?: return@onCall

        // Extract the raw JWT token from the Authorization header
        val authHeader = call.request.headers["Authorization"]
        val token = authHeader?.removePrefix("Bearer ")?.trim()

        if (token != null) {
            // Check cache first (non-blocking)
            val cachedResult = jwtBlacklistCache.get(token)

            val isBlacklisted = if (cachedResult != null) {
                // Use cached result
                cachedResult
            } else {
                // Check database and cache result
                val result = sessionRepository.isTokenBlacklisted(token)
                    .getOrElse {
                        // SECURITY: Fail closed - if we can't verify the token status,
                        // assume it's revoked to be safe. Log the error for debugging.
                        this@createRouteScopedPlugin.environment.log.error("Failed to check token blacklist", it)
                        true // Fail closed for security
                    }
                jwtBlacklistCache.put(token, result)
                result
            }

            if (isBlacklisted) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "error" to "token_revoked",
                        "message" to "This token has been revoked. Please login again."
                    )
                )
                // Return early to prevent further processing
                return@onCall
            }
        }
    }
}

/**
 * Configuration for Permission checking plugin.
 */
class PermissionCheckConfig {
    var requiredPermissions: Set<com.guyghost.wakeve.auth.Permission> = emptySet()
}

/**
 * Plugin to check if the authenticated user has required permissions.
 *
 * This plugin verifies that the JWT token contains the necessary permissions
 * for accessing protected routes. Permissions are extracted from the JWT claims.
 */
val PermissionCheckPlugin = createRouteScopedPlugin(
    name = "PermissionCheckPlugin",
    createConfiguration = ::PermissionCheckConfig
) {
    val requiredPerms = pluginConfig.requiredPermissions

    // Skip if no permissions required
    if (requiredPerms.isEmpty()) {
        return@createRouteScopedPlugin
    }

    onCall { call ->
        // Get JWT principal (set by JWT authentication)
        val principal = call.principal<JWTPrincipal>() ?: run {
            call.respond(
                HttpStatusCode.Unauthorized,
                mapOf(
                    "error" to "unauthorized",
                    "message" to "Authentication required"
                )
            )
            return@onCall
        }

        // Extract permissions from JWT claims
        val userPermissions = principal.payload.getClaim("permissions")
            ?.asList(String::class.java)
            ?.mapNotNull { com.guyghost.wakeve.auth.Permission.fromString(it) }
            ?.toSet()
            ?: emptySet()

        // Check if user has all required permissions
        val missingPermissions = requiredPerms - userPermissions

        if (missingPermissions.isNotEmpty()) {
            call.respond(
                HttpStatusCode.Forbidden,
                mapOf(
                    "error" to "insufficient_permissions",
                    "message" to "Missing permissions: ${missingPermissions.joinToString(", ") { it.name }}",
                    "required" to requiredPerms.map { it.name },
                    "missing" to missingPermissions.map { it.name }
                )
            )
            return@onCall
        }
    }
}

fun main() {
    // Initialize database
    val database = DatabaseProvider.getDatabase(JvmDatabaseFactory("wakev_server.db"))
    val eventRepository = DatabaseEventRepository(database)
    val scenarioRepository = ScenarioRepository(database)
    val budgetRepository = com.guyghost.wakeve.budget.BudgetRepository(database)
    val mealRepository = com.guyghost.wakeve.meal.MealRepository(database)
    val commentRepository = com.guyghost.wakeve.comment.CommentRepository(database)
    val locationRepository = PotentialLocationRepository(eventRepository)
    val accommodationRepository = com.guyghost.wakeve.accommodation.AccommodationRepository(database)
    
    // Initialize Calendar Service
    val platformCalendarService = PlatformCalendarServiceImpl()
    val calendarService = CalendarService(database, platformCalendarService)
    
    // Initialize Chat Service
    val chatService = ChatService(database)

    // Initialize Analytics Dashboard
    val analyticsDashboard = AnalyticsDashboard(database)

    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = {
        module(
            database,
            eventRepository,
            scenarioRepository,
            budgetRepository,
            mealRepository,
            commentRepository,
            locationRepository,
            calendarService,
            chatService,
            accommodationRepository,
            analyticsDashboard
        )
    }).start(wait = true)
}

fun Application.module(
    database: WakeveDb,
    eventRepository: DatabaseEventRepository = DatabaseEventRepository(database),
    scenarioRepository: ScenarioRepository = ScenarioRepository(database),
    budgetRepository: com.guyghost.wakeve.budget.BudgetRepository = com.guyghost.wakeve.budget.BudgetRepository(database),
    mealRepository: com.guyghost.wakeve.meal.MealRepository = com.guyghost.wakeve.meal.MealRepository(database),
    commentRepository: com.guyghost.wakeve.comment.CommentRepository = com.guyghost.wakeve.comment.CommentRepository(database),
    locationRepository: PotentialLocationRepositoryInterface = PotentialLocationRepository(eventRepository),
    calendarService: CalendarService = CalendarService(database, PlatformCalendarServiceImpl()),
    chatService: ChatService = ChatService(database),
    accommodationRepository: com.guyghost.wakeve.accommodation.AccommodationRepository = com.guyghost.wakeve.accommodation.AccommodationRepository(database),
    analyticsDashboard: AnalyticsDashboard = AnalyticsDashboard(database)
) {
    // Initialize metrics
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val authMetrics = AuthMetricsCollector()

    // Bind metrics collectors to registry
    authMetrics.bindTo(meterRegistry)
    JvmMemoryMetrics().bindTo(meterRegistry)
    JvmThreadMetrics().bindTo(meterRegistry)
    ProcessorMetrics().bindTo(meterRegistry)

    // Initialize authentication services
    // SECURITY: JWT_SECRET environment variable is required
    val jwtSecret = System.getenv("JWT_SECRET")
        ?: throw IllegalStateException("JWT_SECRET environment variable is required. Please set a secure random string.")
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

    val sessionRepository = SessionRepository(database)
    val sessionManager = SessionManager(database)
    val jwtBlacklistCache = JwtBlacklistCache()
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
        register(RateLimitName("api")) {
            rateLimiter(limit = 100, refillPeriod = 1.minutes)
        }
    }

    // Install WebSocket support for real-time chat
    install(WebSockets) {
        // Configure WebSocket ping/pong for connection health
        pingPeriodMillis = 30_000 // 30 seconds
        timeoutMillis = 60_000 // 60 seconds
        maxFrameSize = Long.MAX_VALUE
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
                val userId = credential.payload.getClaim("userId")?.asString()
                if (userId != null) {
                    // Extract JWT token from Authorization header
                    // Note: This is a simplified check. In production, use a proper interceptor
                    // to avoid blocking the validation flow
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

        // WebSocket endpoint for real-time chat
        chatWebSocketRoute()

        // Metrics endpoint (Prometheus format) - protected by IP whitelist
        get("/metrics") {
            // Get whitelisted IPs from environment variable (comma-separated)
            val whitelistIps = System.getenv("METRICS_WHITELIST_IPS")?.split(",")?.map { it.trim() } ?: emptyList()

            // Get client IP - check X-Forwarded-For header first (for proxy/reverse proxy setups)
            val clientIp = call.request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
                ?: call.request.headers["X-Real-IP"]
                ?: call.request.headers["Host"]

            // Log all metrics access attempts
            this@module.environment.log.info("Metrics access attempt from IP: $clientIp")

            // Check if IP is whitelisted (or if whitelist is empty - fail open in dev, fail closed in prod)
            val isProduction = System.getenv("ENVIRONMENT") == "production"
            val isAllowed = whitelistIps.isEmpty() && !isProduction || whitelistIps.any { it == clientIp || it == "*" }

            if (!isAllowed) {
                this@module.environment.log.warn("Metrics access denied for IP: $clientIp (not in whitelist)")
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "access_denied", "message" to "Your IP is not authorized to access metrics"))
                return@get
            }

            this@module.environment.log.info("Metrics access granted to IP: $clientIp")
            call.respondText(meterRegistry.scrape(), io.ktor.http.ContentType.parse("text/plain; version=0.0.4"))
        }

        // Authentication endpoints (public but rate limited)
        rateLimit(RateLimitName("auth")) {
            authRoutes(authService)
        }

                // API endpoints (protected by JWT authentication + blacklist check)
                authenticate("auth-jwt") {
                    rateLimit(RateLimitName("api")) {
                        route("/api") {
                            // Install JWT blacklist checking for all API routes
                            install(JWTBlacklistPlugin) {
                                this.sessionRepository = sessionRepository
                                this.jwtBlacklistCache = jwtBlacklistCache
                            }

                            eventRoutes(eventRepository)
                            participantRoutes(eventRepository)
                            voteRoutes(eventRepository)
                            scenarioRoutes(scenarioRepository)
                            budgetRoutes(budgetRepository, eventRepository)
                            mealRoutes(mealRepository)
                            commentRoutes(commentRepository)
                            potentialLocationRoutes(locationRepository)
                            syncRoutes(syncService)
                            accommodationRoutes(accommodationRepository)
                            sessionRoutes(sessionManager)
                            calendarRoutes(calendarService)
                            chatRoutes(chatService)
                            meetingProxyRoutes()
                            analyticsRoutes(analyticsDashboard)
                        }
                    }
                }

        // Root endpoint for compatibility
        get("/") {
            call.respondText("Wakev API v1.0")
        }
    }
}