package com.guyghost.wakeve.auth.core.logic

/**
 * Parses a JSON Web Token (JWT) without verification.
 * 
 * This function decodes the middle segment (payload) of a JWT token
 * and returns its claims as a map. It does NOT verify the token's
 * signature or expiration - that should be done by the backend.
 * 
 * JWT Structure: header.payload.signature
 * - Header: Base64URL encoded JSON
 * - Payload: Base64URL encoded JSON (this function parses this)
 * - Signature: Base64URL encoded binary data
 * 
 * @param jwt The JWT token string to parse
 * @return JWTPayload containing the decoded claims, or null if parsing fails
 * 
 * @example
 * ```
 * val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
 * val payload = parseJWT(token)
 * payload?.subject // "123456789"
 * payload?.get("name") // "John Doe"
 * payload?.issuedAt // 1516239022
 * ```
 */
fun parseJWT(jwt: String): JWTPayload? {
    if (jwt.isBlank()) {
        return null
    }

    val parts = jwt.split(".")
    if (parts.size != 3) {
        return null
    }

    val payloadBase64 = parts[1]
    if (payloadBase64.isBlank()) {
        return null
    }

    return try {
        val json = base64UrlDecode(payloadBase64)
        parsePayloadJson(json)
    } catch (e: Exception) {
        null
    }
}

/**
 * Parses the JSON payload into a JWTPayload object.
 */
private fun parsePayloadJson(json: String): JWTPayload? {
    return try {
        // Simple JSON parsing without external library
        // This is a basic implementation - for production, use a JSON library
        val claims = mutableMapOf<String, Any?>()

        // Remove braces and trim
        val content = json.trim().removePrefix("{").removeSuffix("}")
        if (content.isBlank()) {
            return JWTPayload(emptyMap())
        }

        // Parse key-value pairs
        val pairs = content.split(",")
        for (pair in pairs) {
            val keyValue = pair.split(":")
            if (keyValue.size == 2) {
                val key = keyValue[0].trim().removeSurrounding("\"")
                val value = keyValue[1].trim()

                claims[key] = when {
                    value.startsWith("\"") && value.endsWith("\"") -> 
                        value.removeSurrounding("\"")
                    value == "true" -> true
                    value == "false" -> false
                    value == "null" -> null
                    value.contains(".") -> value.toDoubleOrNull()
                    else -> value.toLongOrNull() ?: value
                }
            }
        }

        JWTPayload(claims)
    } catch (e: Exception) {
        null
    }
}

/**
 * Decodes a Base64URL encoded string to JSON.
 * Base64URL is URL-safe Base64 encoding (uses - and _ instead of + and /).
 */
private fun base64UrlDecode(input: String): String {
    val base64 = input
        .replace('-', '+')
        .replace('_', '/')
        .padEnd((input.length + 3) / 4 * 4, '=')

    return try {
        decodeBase64ToString(base64)
    } catch (e: Exception) {
        ""
    }
}

/**
 * Platform-specific Base64 decoding.
 */
internal expect fun decodeBase64ToString(input: String): String

/**
 * Represents the parsed payload of a JWT token.
 * 
 * @property claims The claims extracted from the JWT payload
 */
data class JWTPayload(
    val claims: Map<String, Any?>
) {
    /**
     * Returns the subject claim (user identifier).
     */
    val subject: String?
        get() = claims["sub"] as? String

    /**
     * Returns the issuer claim.
     */
    val issuer: String?
        get() = claims["iss"] as? String

    /**
     * Returns the audience claim.
     */
    val audience: String?
        get() = claims["aud"] as? String

    /**
     * Returns the expiration time claim as a Unix timestamp.
     */
    val expirationTime: Long?
        get() = (claims["exp"] as? Number)?.toLong()

    /**
     * Returns the issued at claim as a Unix timestamp.
     */
    val issuedAt: Long?
        get() = (claims["iat"] as? Number)?.toLong()

    /**
     * Returns the not before claim as a Unix timestamp.
     */
    val notBefore: Long?
        get() = (claims["nbf"] as? Number)?.toLong()

    /**
     * Returns a specific claim by key.
     */
    operator fun get(key: String): Any? = claims[key]

    /**
     * Returns a claim as a specific type.
     */
    inline fun <reified T> getAs(key: String): T? = claims[key] as? T

    /**
     * Returns true if the token is expired based on the exp claim.
     */
    fun isExpired(currentTime: Long = currentTimeMillis()): Boolean {
        val exp = expirationTime ?: return false
        return currentTime >= exp * 1000 // Convert Unix timestamp to milliseconds
    }

    /**
     * Returns true if the token is not yet valid based on the nbf claim.
     */
    fun isNotYetValid(currentTime: Long = currentTimeMillis()): Boolean {
        val nbf = notBefore ?: return false
        return currentTime < nbf * 1000 // Convert Unix timestamp to milliseconds
    }
}

/**
 * Platform-specific current time in milliseconds.
 */
internal expect fun currentTimeMillis(): Long
