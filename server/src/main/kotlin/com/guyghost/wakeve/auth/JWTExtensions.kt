package com.guyghost.wakeve.auth

import io.ktor.server.auth.jwt.JWTPrincipal

/**
 * Extension property to get userId from JWT payload
 */
val JWTPrincipal.userId: String
    get() = payload.getClaim("userId")?.asString()
        ?: throw IllegalStateException("JWT token missing userId claim")

/**
 * Extension property to get sessionId from JWT payload
 * Falls back to generating from token if not present
 */
val JWTPrincipal.sessionId: String
    get() = payload.getClaim("sessionId")?.asString()
        ?: payload.getClaim("jti")?.asString() // Use JWT ID as session ID if available
        ?: "session-${payload.getClaim("userId")?.asString()}-${System.currentTimeMillis()}"

/**
 * Extension property to get email from JWT payload
 */
val JWTPrincipal.email: String?
    get() = payload.getClaim("email")?.asString()

/**
 * Extension property to get roles from JWT payload
 */
val JWTPrincipal.roles: List<String>
    get() = payload.getClaim("roles")?.asList(String::class.java) ?: emptyList()

/**
 * Extension property to get permissions from JWT payload
 */
val JWTPrincipal.permissions: List<String>
    get() = payload.getClaim("permissions")?.asList(String::class.java) ?: emptyList()
