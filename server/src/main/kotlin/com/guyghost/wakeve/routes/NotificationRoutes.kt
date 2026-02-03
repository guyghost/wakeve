package com.guyghost.wakeve.routes

import com.guyghost.wakeve.notification.NotificationPreferences
import com.guyghost.wakeve.notification.NotificationRequest
import com.guyghost.wakeve.notification.NotificationService
import com.guyghost.wakeve.notification.Platform
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Notification API routes.
 * Provides endpoints for managing push notifications and user preferences.
 */
fun Route.notificationRoutes(
    notificationService: NotificationService
) {
    route("/api/notifications") {

        /**
         * Register device token for push notifications.
         * POST /api/notifications/register
         */
        post("/register") {
            try {
                val request = call.receive<RegisterTokenRequest>()
                val platform = when (request.platform.lowercase()) {
                    "android" -> Platform.ANDROID
                    "ios" -> Platform.IOS
                    else -> return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Invalid platform: ${request.platform}")
                    )
                }

                notificationService.registerPushToken(
                    userId = request.userId,
                    platform = platform,
                    token = request.token
                )

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to register token: ${e.message}")
                )
            }
        }

        /**
         * Unregister device token.
         * DELETE /api/notifications/unregister?userId={userId}&platform={platform}
         */
        delete("/unregister") {
            try {
                val userId = call.request.queryParameters["userId"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("userId required"))
                val platformParam = call.request.queryParameters["platform"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("platform required"))

                val platform = when (platformParam.lowercase()) {
                    "android" -> Platform.ANDROID
                    "ios" -> Platform.IOS
                    else -> return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Invalid platform: $platformParam")
                    )
                }

                notificationService.unregisterPushToken(userId, platform)

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to unregister token: ${e.message}")
                )
            }
        }

        /**
         * Send a notification.
         * POST /api/notifications/send
         */
        post("/send") {
            try {
                val request = call.receive<NotificationRequest>()

                val notificationId = notificationService.sendNotification(request)
                    .getOrElse { error ->
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Failed to send notification: ${error.message}")
                        )
                    }

                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "notificationId" to notificationId
                ))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to send notification: ${e.message}")
                )
            }
        }

        /**
         * Get notification history for a user.
         * GET /api/notifications?userId={userId}&limit={limit}
         */
        get {
            try {
                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("userId required"))
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50

                val notifications = notificationService.getNotifications(userId, limit)

                call.respond(HttpStatusCode.OK, notifications)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to get notifications: ${e.message}")
                )
            }
        }

        /**
         * Get unread notifications for a user.
         * GET /api/notifications/unread?userId={userId}
         */
        get("/unread") {
            try {
                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("userId required"))

                val notifications = notificationService.getUnreadNotifications(userId)

                call.respond(HttpStatusCode.OK, notifications)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to get unread notifications: ${e.message}")
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
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("id required"))

                notificationService.markAsRead(id)
                    .getOrElse { error ->
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Failed to mark as read: ${error.message}")
                        )
                    }

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to mark as read: ${e.message}")
                )
            }
        }

        /**
         * Mark all notifications as read for a user.
         * PUT /api/notifications/read-all?userId={userId}
         */
        put("/read-all") {
            try {
                val userId = call.request.queryParameters["userId"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("userId required"))

                notificationService.markAllAsRead(userId)
                    .getOrElse { error ->
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Failed to mark all as read: ${error.message}")
                        )
                    }

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to mark all as read: ${e.message}")
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
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("id required"))

                notificationService.deleteNotification(id)
                    .getOrElse { error ->
                        return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Failed to delete notification: ${error.message}")
                        )
                    }

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to delete notification: ${e.message}")
                )
            }
        }

        /**
         * Get notification preferences for a user.
         * GET /api/notifications/preferences?userId={userId}
         */
        get("/preferences") {
            try {
                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("userId required"))

                val preferences = notificationService.getPreferences(userId)

                if (preferences != null) {
                    call.respond(HttpStatusCode.OK, preferences)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Preferences not found"))
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to get preferences: ${e.message}")
                )
            }
        }

        /**
         * Update notification preferences for a user.
         * PUT /api/notifications/preferences
         */
        put("/preferences") {
            try {
                val preferences = call.receive<NotificationPreferences>()

                notificationService.updatePreferences(preferences)
                    .getOrElse { error ->
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Failed to update preferences: ${error.message}")
                        )
                    }

                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Failed to update preferences: ${e.message}")
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
    val userId: String,
    val token: String,
    val platform: String // "android" or "ios"
)

/**
 * Error response DTO.
 */
@kotlinx.serialization.Serializable
data class ErrorResponse(
    val error: String
)
