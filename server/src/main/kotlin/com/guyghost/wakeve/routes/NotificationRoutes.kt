package com.guyghost.wakeve.routes

import com.guyghost.wakeve.notification.NotificationPreferences
import com.guyghost.wakeve.notification.NotificationRequest
import com.guyghost.wakeve.notification.NotificationService
import com.guyghost.wakeve.notification.Platform
import com.guyghost.wakeve.notification.defaultNotificationPreferences
import com.guyghost.wakeve.notification.withDeepLink
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Notification API routes.
 * Provides endpoints for managing push notifications and user preferences.
 *
 * All routes require JWT authentication. The userId is extracted from the JWT
 * token for register/unregister to prevent token hijacking.
 */
fun Route.notificationRoutes(
    notificationService: NotificationService
) {
    route("/notifications") {

        /**
         * Register device token for push notifications.
         * POST /api/notifications/register
         *
         * The userId is extracted from the JWT token for security.
         */
        post("/register") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Missing userId in token")
                    )

                val request = call.receive<RegisterTokenRequest>()
                val token = validatePushToken(request.token)
                    .getOrElse { error ->
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to pushTokenValidationFailureMessage())
                        )
                    }
                val platform = parseNotificationPlatform(request.platform)
                    .getOrElse { error ->
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to notificationPlatformValidationFailureMessage())
                        )
                    }

                notificationService.registerPushToken(
                    userId = userId,
                    platform = platform,
                    token = token
                ).getOrElse { error ->
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to notificationTokenRegisterFailureMessage())
                    )
                }

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to notificationTokenRegisterFailureMessage())
                )
            }
        }

        /**
         * Unregister device token on logout.
         * DELETE /api/notifications/unregister?platform={platform}
         *
         * The userId is extracted from the JWT token for security.
         */
        delete("/unregister") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@delete call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Missing userId in token")
                    )

                val platform = parseNotificationPlatform(call.request.queryParameters["platform"])
                    .getOrElse { error ->
                        return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to notificationPlatformValidationFailureMessage())
                        )
                    }

                notificationService.unregisterPushToken(userId, platform)
                    .getOrElse { error ->
                        return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to notificationTokenUnregisterFailureMessage())
                        )
                    }

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to notificationTokenUnregisterFailureMessage())
                )
            }
        }

        /**
         * Send a notification (server-internal or admin use).
         * POST /api/notifications/send
         */
        post("/send") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val senderUserId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Missing userId in token")
                    )

                val request = call.receive<NotificationRequest>()
                val authorization = authorizeNotificationSend(
                    senderUserId = senderUserId,
                    targetUserId = request.userId,
                    role = principal.payload.getClaim("role")?.asString(),
                    roles = principal.payload.getClaim("roles")?.asList(String::class.java).orEmpty(),
                    permissions = principal.payload.getClaim("permissions")?.asList(String::class.java).orEmpty()
                )
                authorization.getOrElse { error ->
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to notificationSendForbiddenMessage())
                    )
                }

                val notificationId = notificationService.sendNotification(request.withDeepLink())
                    .getOrElse { error ->
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to notificationSendFailureMessage())
                        )
                    }

                call.respond(
                    HttpStatusCode.OK,
                    SendNotificationResponse(
                        success = true,
                        notificationId = notificationId
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to notificationSendFailureMessage())
                )
            }
        }

        /**
         * Get notification history for the authenticated user.
         * GET /api/notifications?limit={limit}
         */
        get {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Missing userId in token")
                    )
                val limit = parseNotificationHistoryLimit(call.request.queryParameters["limit"])

                val notifications = notificationService.getNotifications(userId, limit)

                call.respond(HttpStatusCode.OK, notifications)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to notificationHistoryFailureMessage())
                )
            }
        }

        /**
         * Get unread notifications for the authenticated user.
         * GET /api/notifications/unread
         */
        get("/unread") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Missing userId in token")
                    )

                val limit = parseNotificationHistoryLimit(call.request.queryParameters["limit"])
                val notifications = notificationService.getUnreadNotifications(userId, limit)

                call.respond(HttpStatusCode.OK, notifications)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to unreadNotificationsFailureMessage())
                )
            }
        }

        /**
         * Mark a notification as read.
         * PUT /api/notifications/{id}/read
         */
        put("/{id}/read") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@put call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Missing userId in token")
                    )

                val id = call.parameters["id"]
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "id required")
                    )

                notificationService.markAsReadForUser(
                    notificationId = id,
                    userId = userId
                )
                    .getOrElse { error ->
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to notificationMarkReadFailureMessage())
                        )
                    }

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to notificationMarkReadFailureMessage())
                )
            }
        }

        /**
         * Mark all notifications as read for the authenticated user.
         * PUT /api/notifications/read-all
         */
        put("/read-all") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@put call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Missing userId in token")
                    )

                notificationService.markAllAsRead(userId)
                    .getOrElse { error ->
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to notificationMarkAllReadFailureMessage())
                        )
                    }

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to notificationMarkAllReadFailureMessage())
                )
            }
        }

        /**
         * Delete a notification.
         * DELETE /api/notifications/{id}
         */
        delete("/{id}") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@delete call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Missing userId in token")
                    )

                val id = call.parameters["id"]
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "id required")
                    )

                notificationService.deleteNotificationForUser(
                    notificationId = id,
                    userId = userId
                )
                    .getOrElse { error ->
                        return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to notificationDeleteFailureMessage())
                        )
                    }

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to notificationDeleteFailureMessage())
                )
            }
        }

        /**
         * Get notification preferences for the authenticated user.
         * GET /api/notifications/preferences
         */
        get("/preferences") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Missing userId in token")
                    )

                val preferences = resolveEffectiveNotificationPreferences(
                    authenticatedUserId = userId,
                    storedPreferences = notificationService.getPreferences(userId)
                )

                call.respond(HttpStatusCode.OK, preferences)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to notificationPreferencesReadFailureMessage())
                )
            }
        }

        /**
         * Update notification preferences for the authenticated user.
         * PUT /api/notifications/preferences
         */
        put("/preferences") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@put call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Missing userId in token")
                    )

                val preferences = bindPreferencesToAuthenticatedUser(
                    preferences = call.receive<NotificationPreferences>(),
                    authenticatedUserId = userId
                ).getOrElse { error ->
                    return@put call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to notificationPreferencesForbiddenMessage())
                    )
                }

                notificationService.updatePreferences(preferences)
                    .getOrElse { error ->
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to notificationPreferencesUpdateFailureMessage())
                        )
                    }

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to notificationPreferencesUpdateFailureMessage())
                )
            }
        }
    }
}

/**
 * Request DTO for registering a push token.
 */
@kotlinx.serialization.Serializable
data class RegisterTokenRequest(
    val token: String,
    val platform: String // "android" or "ios"
)

@kotlinx.serialization.Serializable
data class SendNotificationResponse(
    val success: Boolean,
    val notificationId: String
)

internal fun validatePushToken(token: String): Result<String> {
    val normalizedToken = token.trim()
    return if (normalizedToken.isBlank()) {
        Result.failure(IllegalArgumentException("Push token must not be blank"))
    } else {
        Result.success(normalizedToken)
    }
}

internal fun parseNotificationPlatform(platform: String?): Result<Platform> {
    val normalizedPlatform = platform?.trim()?.lowercase().orEmpty()
    return when (normalizedPlatform) {
        "" -> Result.failure(IllegalArgumentException("platform query parameter required"))
        "android" -> Result.success(Platform.ANDROID)
        "ios" -> Result.success(Platform.IOS)
        else -> Result.failure(
            IllegalArgumentException("Invalid platform: $platform. Must be 'android' or 'ios'")
        )
    }
}

internal fun bindPreferencesToAuthenticatedUser(
    preferences: NotificationPreferences,
    authenticatedUserId: String
): Result<NotificationPreferences> {
    val normalizedAuthenticatedUserId = authenticatedUserId.trim()
    val requestedUserId = preferences.userId.trim()
    return when {
        normalizedAuthenticatedUserId.isBlank() -> Result.failure(
            IllegalArgumentException("Missing userId in token")
        )
        requestedUserId.isBlank() -> Result.success(
            preferences.copy(userId = normalizedAuthenticatedUserId)
        )
        requestedUserId == normalizedAuthenticatedUserId -> Result.success(
            preferences.copy(userId = normalizedAuthenticatedUserId)
        )
        else -> Result.failure(
            IllegalArgumentException("Cannot update notification preferences for another user")
        )
    }
}

internal fun authorizeNotificationSend(
    senderUserId: String?,
    targetUserId: String,
    role: String?,
    roles: List<String>,
    permissions: List<String>
): Result<Unit> {
    val normalizedSenderUserId = senderUserId?.trim().orEmpty()
    val normalizedTargetUserId = targetUserId.trim()
    if (normalizedSenderUserId.isBlank()) {
        return Result.failure(IllegalArgumentException("Missing userId in token"))
    }
    if (normalizedTargetUserId.isBlank()) {
        return Result.failure(IllegalArgumentException("Notification target userId must not be blank"))
    }
    if (normalizedSenderUserId == normalizedTargetUserId) {
        return Result.success(Unit)
    }

    val normalizedRoles = (roles + listOfNotNull(role))
        .map { it.trim().uppercase() }
        .filter { it.isNotBlank() }
        .toSet()
    val normalizedPermissions = permissions
        .map { it.trim().uppercase() }
        .filter { it.isNotBlank() }
        .toSet()

    val hasPrivilegedRole = normalizedRoles.any { it == "ADMIN" || it == "SERVICE" || it == "MODERATOR" }
    val hasPrivilegedPermission = normalizedPermissions.any {
        it == "NOTIFICATIONS_SEND" || it == "ADMIN" || it == "MODERATE"
    }

    return if (hasPrivilegedRole || hasPrivilegedPermission) {
        Result.success(Unit)
    } else {
        Result.failure(IllegalArgumentException("Cannot send notifications to another user"))
    }
}

internal fun parseNotificationHistoryLimit(rawLimit: String?): Int {
    val parsedLimit = rawLimit?.trim()?.toIntOrNull() ?: DEFAULT_NOTIFICATION_HISTORY_LIMIT
    return parsedLimit.coerceIn(MIN_NOTIFICATION_HISTORY_LIMIT, MAX_NOTIFICATION_HISTORY_LIMIT)
}

internal fun resolveEffectiveNotificationPreferences(
    authenticatedUserId: String,
    storedPreferences: NotificationPreferences?
): NotificationPreferences {
    return storedPreferences ?: defaultNotificationPreferences(authenticatedUserId.trim())
}

internal fun pushTokenValidationFailureMessage(): String =
    "Push token is required."

internal fun notificationPlatformValidationFailureMessage(): String =
    "Notification platform must be android or ios."

internal fun notificationTokenRegisterFailureMessage(): String =
    "Failed to register notification token. Please try again."

internal fun notificationTokenUnregisterFailureMessage(): String =
    "Failed to unregister notification token. Please try again."

internal fun notificationSendForbiddenMessage(): String =
    "You are not allowed to send this notification."

internal fun notificationSendFailureMessage(): String =
    "Failed to send notification. Please try again."

internal fun notificationHistoryFailureMessage(): String =
    "Failed to fetch notifications. Please try again."

internal fun unreadNotificationsFailureMessage(): String =
    "Failed to fetch unread notifications. Please try again."

internal fun notificationMarkReadFailureMessage(): String =
    "Failed to mark notification as read. Please try again."

internal fun notificationMarkAllReadFailureMessage(): String =
    "Failed to mark notifications as read. Please try again."

internal fun notificationDeleteFailureMessage(): String =
    "Failed to delete notification. Please try again."

internal fun notificationPreferencesReadFailureMessage(): String =
    "Failed to fetch notification preferences. Please try again."

internal fun notificationPreferencesForbiddenMessage(): String =
    "You are not allowed to update these notification preferences."

internal fun notificationPreferencesUpdateFailureMessage(): String =
    "Failed to update notification preferences. Please try again."

private const val DEFAULT_NOTIFICATION_HISTORY_LIMIT = 50
private const val MIN_NOTIFICATION_HISTORY_LIMIT = 1
private const val MAX_NOTIFICATION_HISTORY_LIMIT = 100
