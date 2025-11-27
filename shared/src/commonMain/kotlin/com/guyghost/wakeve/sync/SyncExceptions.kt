package com.guyghost.wakeve.sync

/**
 * Exception thrown when authentication fails (HTTP 401)
 */
class UnauthorizedException(
    message: String = "Authentication failed",
    val statusCode: Int = 401
) : Exception(message)

/**
 * Exception thrown when a resource is forbidden (HTTP 403)
 */
class ForbiddenException(
    message: String = "Access forbidden",
    val statusCode: Int = 403
) : Exception(message)

/**
 * Exception thrown for other HTTP errors
 */
class HttpException(
    val statusCode: Int,
    message: String
) : Exception(message)
