package com.guyghost.wakeve.routes

import com.guyghost.wakeve.notification.APNsEnvironment
import com.guyghost.wakeve.notification.BackendDeviceRegistrationRequest
import com.guyghost.wakeve.notification.BackendDeviceRegistrationStoreFactory
import com.guyghost.wakeve.notification.BackendDeviceUnregistrationOutcome
import com.guyghost.wakeve.notification.BackendDeviceUnregistrationResult
import com.guyghost.wakeve.notification.DeviceRegistrationScope
import com.guyghost.wakeve.notification.DeviceRegistrationUnregisteredReason
import com.guyghost.wakeve.notification.LegacyCompatibilityClientGeneration
import com.guyghost.wakeve.notification.LegacyCompatibilityCommand
import com.guyghost.wakeve.notification.LegacyCompatibilityOperation
import com.guyghost.wakeve.notification.LegacyCompatibilityResponseDisposition
import com.guyghost.wakeve.notification.LegacyCompatibilitySnapshot
import com.guyghost.wakeve.notification.LegacyNotificationRegistrationCompatibilityWorker
import com.guyghost.wakeve.notification.NotificationPreferences
import com.guyghost.wakeve.notification.NotificationRequest
import com.guyghost.wakeve.notification.NotificationService
import com.guyghost.wakeve.notification.Platform
import com.guyghost.wakeve.notification.defaultNotificationPreferences
import com.guyghost.wakeve.notification.withDeepLink
import com.guyghost.wakeve.notification.compatibilityTokenFingerprint
import com.guyghost.wakeve.notification.legacyCompatibilityRowKey
import com.guyghost.wakeve.notification.legacyCompatibilityRequestKey
import com.guyghost.wakeve.notification.opaqueCompatibilityDigest
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
    notificationService: NotificationService,
    deviceRegistrationStoreFactory: BackendDeviceRegistrationStoreFactory,
    compatibilityWorker: LegacyNotificationRegistrationCompatibilityWorker =
        LegacyNotificationRegistrationCompatibilityWorker(
            notificationService = notificationService,
            registrationStoreFactory = deviceRegistrationStoreFactory,
            logicalClock = ::currentEpochSeconds
        )
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
                val token = validatePushToken(request.token.orEmpty())
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

                val isV2Request = request.installationId != null ||
                    request.environment != null ||
                    request.topic != null
                if (isV2Request) {
                    val registrationRequest = createV2DeviceRegistrationRequest(
                        request = request,
                        authenticatedUserId = userId,
                        token = token,
                        platform = platform,
                        atEpochSeconds = currentEpochSeconds()
                    ).getOrElse {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to notificationDeviceRegistrationValidationFailureMessage())
                        )
                    }
                    val registration = deviceRegistrationStoreFactory.open().use { store ->
                        store.register(registrationRequest)
                    }
                    call.respond(
                        HttpStatusCode.OK,
                        RegisterDeviceResponse(
                            success = true,
                            registrationId = registration.registrationId,
                            status = registration.status.name.lowercase()
                        )
                    )
                } else {
                    if (platform == Platform.IOS) {
                        val saga = executeLegacyCompatibilityCommand(
                            compatibilityWorker = compatibilityWorker,
                            factory = deviceRegistrationStoreFactory,
                            authenticatedUserId = userId,
                            platform = platform,
                            operation = LegacyCompatibilityOperation.REGISTER,
                            rawToken = token,
                            atEpochSeconds = currentEpochSeconds()
                        ).getOrElse {
                            return@post call.respond(
                                HttpStatusCode.InternalServerError,
                                mapOf("error" to notificationTokenRegisterFailureMessage())
                            )
                        }
                        return@post call.respondLegacyCompatibility(saga)
                    }

                    notificationService.registerPushToken(
                        userId = userId,
                        platform = platform,
                        token = token
                    ).getOrElse {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to notificationTokenRegisterFailureMessage())
                        )
                    }
                    call.respond(HttpStatusCode.OK, mapOf("success" to true))
                }
            } catch (_: io.ktor.server.plugins.BadRequestException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to notificationDeviceRegistrationValidationFailureMessage())
                )
            } catch (_: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to notificationDeviceRegistrationValidationFailureMessage())
                )
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

                val legacyPlatform = call.request.queryParameters["platform"]
                if (legacyPlatform != null) {
                    val platform = parseNotificationPlatform(legacyPlatform).getOrElse {
                        return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to notificationPlatformValidationFailureMessage())
                        )
                    }
                    if (platform == Platform.IOS) {
                        val saga = executeLegacyCompatibilityCommand(
                            compatibilityWorker = compatibilityWorker,
                            factory = deviceRegistrationStoreFactory,
                            authenticatedUserId = userId,
                            platform = platform,
                            operation = LegacyCompatibilityOperation.UNREGISTER,
                            rawToken = null,
                            atEpochSeconds = currentEpochSeconds()
                        ).getOrElse {
                            return@delete call.respond(
                                HttpStatusCode.InternalServerError,
                                mapOf("error" to notificationTokenUnregisterFailureMessage())
                            )
                        }
                        return@delete call.respondLegacyCompatibility(saga)
                    }

                    notificationService.unregisterPushToken(userId, platform).getOrElse {
                        return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to notificationTokenUnregisterFailureMessage())
                        )
                    }
                    return@delete call.respond(HttpStatusCode.OK, mapOf("success" to true))
                }

                val request = call.receive<UnregisterDeviceRequest>()
                val result = unregisterDevice(
                    factory = deviceRegistrationStoreFactory,
                    authenticatedUserId = userId,
                    request = request,
                    atEpochSeconds = currentEpochSeconds()
                ).getOrElse {
                    return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to notificationDeviceRegistrationValidationFailureMessage())
                    )
                }
                if (result.outcome == BackendDeviceUnregistrationOutcome.NOT_OWNED) {
                    return@delete call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to notificationRegistrationNotFoundMessage())
                    )
                }

                call.respond(
                    HttpStatusCode.OK,
                    UnregisterDeviceResponse(
                        success = true,
                        alreadyAbsent = result.outcome == BackendDeviceUnregistrationOutcome.ALREADY_ABSENT
                    )
                )
            } catch (_: io.ktor.server.plugins.BadRequestException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to notificationDeviceRegistrationValidationFailureMessage())
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to notificationTokenUnregisterFailureMessage())
                )
            }
        }

        /** Canonical per-association logout endpoint. */
        delete("/registrations/{registrationId}") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                ?: return@delete call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Missing userId in token")
                )
            val registrationId = call.parameters["registrationId"]?.trim().orEmpty()
            if (registrationId.isEmpty() || userId.isBlank()) {
                return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to notificationDeviceRegistrationValidationFailureMessage())
                )
            }
            val result = try {
                deviceRegistrationStoreFactory.open().use { store ->
                    store.unregisterRegistration(
                        registrationId = registrationId,
                        authenticatedUserId = userId,
                        reason = DeviceRegistrationUnregisteredReason.LOGOUT,
                        atEpochSeconds = currentEpochSeconds()
                    )
                }
            } catch (_: IllegalArgumentException) {
                return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to notificationDeviceRegistrationValidationFailureMessage())
                )
            } catch (_: Exception) {
                return@delete call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to notificationTokenUnregisterFailureMessage())
                )
            }

            if (result.outcome == BackendDeviceUnregistrationOutcome.NOT_OWNED) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to notificationRegistrationNotFoundMessage())
                )
            } else {
                call.respond(HttpStatusCode.NoContent)
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
    val token: String? = null,
    val platform: String? = null,
    val installationId: String? = null,
    val environment: String? = null,
    val topic: String? = null,
    @Suppress("unused") val userId: String? = null,
    @Suppress("unused") val appVersion: String? = null
)

@kotlinx.serialization.Serializable
data class RegisterDeviceResponse(
    val success: Boolean,
    val registrationId: String,
    val status: String
)

@kotlinx.serialization.Serializable
data class UnregisterDeviceRequest(
    val registrationId: String? = null,
    val installationId: String? = null
)

@kotlinx.serialization.Serializable
data class UnregisterDeviceResponse(
    val success: Boolean,
    val alreadyAbsent: Boolean
)

@kotlinx.serialization.Serializable
private data class LegacyCompatibilityConvergedResponse(
    val success: Boolean,
    val compatibilityState: String,
    val sagaId: String
)

@kotlinx.serialization.Serializable
private data class LegacyCompatibilityAcceptedResponse(
    val accepted: Boolean,
    val compatibilityState: String,
    val sagaId: String
)

@kotlinx.serialization.Serializable
private data class LegacyCompatibilityBlockedResponse(
    val accepted: Boolean,
    val compatibilityState: String,
    val sagaId: String
)

@kotlinx.serialization.Serializable
data class SendNotificationResponse(
    val success: Boolean,
    val notificationId: String
)

internal fun createV2DeviceRegistrationRequest(
    request: RegisterTokenRequest,
    authenticatedUserId: String,
    token: String,
    platform: Platform,
    atEpochSeconds: Long
): Result<BackendDeviceRegistrationRequest> = runCatching {
    require(platform == Platform.IOS) { "Only iOS APNs registrations are accepted" }
    val environment = when (request.environment?.trim()?.lowercase()) {
        "sandbox" -> APNsEnvironment.SANDBOX
        "production" -> APNsEnvironment.PRODUCTION
        else -> throw IllegalArgumentException("APNs environment is invalid")
    }
    val scope = DeviceRegistrationScope.create(environment, request.topic).getOrThrow()
    BackendDeviceRegistrationRequest.create(
        installationId = request.installationId,
        authenticatedUserId = authenticatedUserId,
        platform = platform,
        scope = scope,
        rawToken = token,
        registeredAtEpochSeconds = atEpochSeconds
    ).getOrThrow()
}

internal suspend fun unregisterDevice(
    factory: BackendDeviceRegistrationStoreFactory,
    authenticatedUserId: String,
    request: UnregisterDeviceRequest,
    atEpochSeconds: Long
): Result<BackendDeviceUnregistrationResult> = runCatching {
    val registrationId = request.registrationId?.trim()?.takeIf(String::isNotEmpty)
    val installationId = request.installationId?.trim()?.takeIf(String::isNotEmpty)
    require(authenticatedUserId.isNotBlank()) { "Authenticated user is required" }
    require(registrationId != null || installationId != null) {
        "registrationId or installationId is required"
    }

    factory.open().use { store ->
        if (registrationId != null) {
            val persisted = store.registration(registrationId)
            if (
                persisted != null &&
                persisted.userId == authenticatedUserId &&
                installationId != null
            ) {
                require(persisted.installationId == installationId) {
                    "registrationId and installationId do not identify the same association"
                }
            }
            store.unregisterRegistration(
                registrationId = registrationId,
                authenticatedUserId = authenticatedUserId,
                reason = DeviceRegistrationUnregisteredReason.LOGOUT,
                atEpochSeconds = atEpochSeconds
            )
        } else {
            store.unregisterInstallation(
                installationId = checkNotNull(installationId),
                authenticatedUserId = authenticatedUserId,
                reason = DeviceRegistrationUnregisteredReason.LOGOUT,
                atEpochSeconds = atEpochSeconds
            )
        }
    }
}

private fun legacyIosScope(): DeviceRegistrationScope = DeviceRegistrationScope.create(
    environment = APNsEnvironment.PRODUCTION,
    topic = LEGACY_IOS_TOPIC
).getOrThrow()

private fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1_000L

private suspend fun executeLegacyCompatibilityCommand(
    compatibilityWorker: LegacyNotificationRegistrationCompatibilityWorker,
    factory: BackendDeviceRegistrationStoreFactory,
    authenticatedUserId: String,
    platform: Platform,
    operation: LegacyCompatibilityOperation,
    rawToken: String?,
    atEpochSeconds: Long
): Result<LegacyCompatibilitySnapshot> = runCatching {
    require(platform == Platform.IOS) { "Only iOS uses the legacy compatibility saga" }
    val legacyRowKey = legacyCompatibilityRowKey(authenticatedUserId, platform)
    val identity = factory.deriveLegacyCompatibilityIdentity(legacyRowKey)
    val tokenFingerprint = rawToken?.let(::compatibilityTokenFingerprint)
    val compatibilityGeneration = factory.openCompatibilitySagaStore().use { store ->
        store.allocateCompatibilityGeneration(
            authenticatedUserId = authenticatedUserId,
            stableTargetIdentity = identity.registrationId,
            operation = operation,
            tokenFingerprint = tokenFingerprint
        )
    }
    val requestKey = legacyCompatibilityRequestKey(
        operation = operation,
        authenticatedUserId = authenticatedUserId,
        compatibilityGeneration = compatibilityGeneration,
        stableTargetIdentity = identity.registrationId,
        tokenFingerprint = tokenFingerprint
    )
    val command = LegacyCompatibilityCommand.create(
        sagaId = opaqueCompatibilityDigest(listOf("compatibility-saga", requestKey)),
        operation = operation,
        clientGeneration = LegacyCompatibilityClientGeneration.N_MINUS_1,
        authenticatedUserId = authenticatedUserId,
        platform = platform,
        legacyPrimaryKeyFingerprint = opaqueCompatibilityDigest(
            listOf("legacy-primary-key", legacyRowKey)
        ),
        legacyInstallationId = identity.installationId,
        legacyRegistrationId = identity.registrationId,
        targetInstallationId = identity.installationId,
        targetRegistrationId = null,
        tokenFingerprint = tokenFingerprint,
        compatibilityGeneration = compatibilityGeneration,
        maxAttemptsPerStore = LEGACY_COMPATIBILITY_MAX_ATTEMPTS,
        initialNowEpochSeconds = atEpochSeconds,
        scope = legacyIosScope()
    ).getOrThrow()
    compatibilityWorker.execute(command, rawToken)
}

private suspend fun ApplicationCall.respondLegacyCompatibility(
    snapshot: LegacyCompatibilitySnapshot
) {
    when (snapshot.responseDisposition) {
        LegacyCompatibilityResponseDisposition.CONVERGED_SUCCESS -> respond(
            HttpStatusCode.OK,
            LegacyCompatibilityConvergedResponse(
                success = true,
                compatibilityState = "converged",
                sagaId = snapshot.sagaId
            )
        )
        LegacyCompatibilityResponseDisposition.RECONCILIATION_ACCEPTED -> respond(
            HttpStatusCode.Accepted,
            LegacyCompatibilityAcceptedResponse(
                accepted = true,
                compatibilityState = "pending",
                sagaId = snapshot.sagaId
            )
        )
        LegacyCompatibilityResponseDisposition.BLOCKED_FAILURE -> respond(
            HttpStatusCode.InternalServerError,
            LegacyCompatibilityBlockedResponse(
                accepted = false,
                compatibilityState = "blocked",
                sagaId = snapshot.sagaId
            )
        )
    }
}

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

internal fun notificationDeviceRegistrationValidationFailureMessage(): String =
    "Notification device registration is invalid."

internal fun notificationRegistrationNotFoundMessage(): String =
    "Notification registration was not found."

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
private const val LEGACY_IOS_TOPIC = "com.guyghost.wakeve"
private const val LEGACY_COMPATIBILITY_MAX_ATTEMPTS = 3
