package com.guyghost.wakeve.routes

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

/**
 * Standard error response for all API endpoints
 *
 * Provides consistent error response structure across the application.
 * This improves client-side error handling and API consistency.
 *
 * @property error The error code (e.g., "validation_error", "not_found")
 * @property message Human-readable error message
 * @property details Optional additional details about the error
 * @property status HTTP status code (for reference)
 */
@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
    val details: Map<String, String>? = null,
    val status: Int? = null
) {
    companion object {
        /**
         * Create a validation error response
         */
        fun validation(message: String, fieldErrors: Map<String, String> = emptyMap()) =
            ErrorResponse(
                error = "validation_error",
                message = message,
                details = if (fieldErrors.isNotEmpty()) fieldErrors else null
            )

        /**
         * Create a not found error response
         */
        fun notFound(resource: String, id: String? = null) =
            ErrorResponse(
                error = "not_found",
                message = if (id != null) "$resource with ID '$id' not found" else "$resource not found"
            )

        /**
         * Create an unauthorized error response
         */
        fun unauthorized(message: String = "Authentication required") =
            ErrorResponse(
                error = "unauthorized",
                message = message
            )

        /**
         * Create a forbidden error response
         */
        fun forbidden(message: String = "Access denied") =
            ErrorResponse(
                error = "forbidden",
                message = message
            )

        /**
         * Create an internal server error response
         */
        fun internal(message: String = "An internal error occurred") =
            ErrorResponse(
                error = "internal_error",
                message = message
            )

        /**
         * Create a service unavailable error response
         */
        fun serviceUnavailable(service: String) =
            ErrorResponse(
                error = "service_unavailable",
                message = "$service is not available. Please try again later."
            )
    }
}

/**
 * Extension function to respond with ErrorResponse
 */
suspend fun io.ktor.server.application.ApplicationCall.respondError(
    errorResponse: ErrorResponse,
    status: HttpStatusCode = HttpStatusCode.BadRequest
) {
    this.respond(status, errorResponse.copy(status = status.value))
}
