package com.guyghost.wakeve.routes

import com.guyghost.wakeve.notification.NotificationPreferences
import com.guyghost.wakeve.notification.NotificationRequest
import com.guyghost.wakeve.notification.NotificationService
import com.guyghost.wakeve.notification.Platform
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
                val platform = when (request.platform.lowercase()) {
                    "android" -> Platform.ANDROID
                    "ios" -> Platform.IOS
                    else -> return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid platform: ${request.platform}. Must be 'android' or 'ios'")
                    )
                }

                notificationService.registerPushToken(
                    userId = userId,
                    platform = platform,
                    token = request.token
                )

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to register token: ${e.message}")
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

                val platformParam = call.request.queryParameters["platform"]
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "platform query parameter required")
                    )

                val platform = when (platformParam.lowercase()) {
                    "android" -> Platform.ANDROID
                    "ios" -> Platform.IOS
                    else -> return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid platform: $platformParam")
                    )
                }

                notificationService.unregisterPushToken(userId, platform)

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to unregister token: ${e.message}")
                )
            }
        }

        /**
         * Send a notification (server-internal or admin use).
         * POST /api/notifications/send
         */
        post("/send") {
            try {
                val request = call.receive<NotificationRequest>()

                val notificationId = notificationService.sendNotification(request)
                    .getOrElse { error ->
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to send notification: ${error.message}")
                        )
                    }

                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "notificationId" to notificationId
                ))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to send notification: ${e.message}")
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
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50

                val notifications = notificationService.getNotifications(userId, limit)

                call.respond(HttpStatusCode.OK, notifications)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to get notifications: ${e.message}")
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

                val notifications = notificationService.getUnreadNotifications(userId)

                call.respond(HttpStatusCode.OK, notifications)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to get unread notifications: ${e.message}")
                )
            }
        }

        /**
         * Mark a notification as read.
         * PUT /api/notifications/{id}/read
         */
        put("/{id}/read") {
            try {
                val id = call.parameters["id"]
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "id required")
                    )

                notificationService.markAsRead(id)
                    .getOrElse { error ->
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to mark as read: ${error.message}")
                        )
                    }

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to mark as read: ${e.message}")
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
                            mapOf("error" to "Failed to mark all as read: ${error.message}")
                        )
                    }

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to mark all as read: ${e.message}")
                )
            }
        }

        /**
         * Delete a notification.
         * DELETE /api/notifications/{id}
         */
        delete("/{id}") {
            try {
                val id = call.parameters["id"]
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "id required")
                    )

                notificationService.deleteNotification(id)
                    .getOrElse { error ->
                        return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to delete notification: ${error.message}")
                        )
                    }

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to delete notification: ${e.message}")
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

                val preferences = notificationService.getPreferences(userId)

                if (preferences != null) {
                    call.respond(HttpStatusCode.OK, preferences)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Preferences not found"))
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to get preferences: ${e.message}")
                )
            }
        }

        /**
         * Update notification preferences for the authenticated user.
         * PUT /api/notifications/preferences
         */
        put("/preferences") {
            try {
                val preferences = call.receive<NotificationPreferences>()

                notificationService.updatePreferences(preferences)
                    .getOrElse { error ->
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to update preferences: ${error.message}")
                        )
                    }

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to update preferences: ${e.message}")
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
