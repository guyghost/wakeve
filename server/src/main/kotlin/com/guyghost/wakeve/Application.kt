package com.guyghost.wakeve

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import com.guyghost.wakeve.routes.eventRoutes
import com.guyghost.wakeve.routes.participantRoutes
import com.guyghost.wakeve.routes.voteRoutes
import com.guyghost.wakeve.routes.authRoutes
import com.guyghost.wakeve.routes.syncRoutes

fun main() {
    // Initialize database
    val database = DatabaseProvider.getDatabase(JvmDatabaseFactory("wakev_server.db"))
    val eventRepository = DatabaseEventRepository(database)

    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = { 
        module(eventRepository)
    }).start(wait = true)
}

fun Application.module(eventRepository: DatabaseEventRepository = DatabaseEventRepository(
    DatabaseProvider.getDatabase(JvmDatabaseFactory("wakev_server.db"))
)) {
    // Install plugins
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        })
    }

    // Configure routing
    routing {
        // Health check endpoint
        get("/health") {
            call.respondText("OK")
        }

        // API endpoints
        route("/api") {
            // Authentication routes
            authRoutes()
            
            // Synchronization routes
            syncRoutes()
            
            // Event management routes
            eventRoutes(eventRepository)
            participantRoutes(eventRepository)
            voteRoutes(eventRepository)
        }

        // Root endpoint for compatibility
        get("/") {
            call.respondText("Wakev API v1.0")
        }
    }
}