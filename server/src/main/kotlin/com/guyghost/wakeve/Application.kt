package com.guyghost.wakeve

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

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
            route("/events") {
                // Event endpoints (will be implemented in p2-14)
            }
            route("/polls") {
                // Poll endpoints (will be implemented in p2-14)
            }
            route("/votes") {
                // Vote endpoints (will be implemented in p2-14)
            }
        }

        // Root endpoint for compatibility
        get("/") {
            call.respondText("Wakev API v1.0")
        }
    }
}