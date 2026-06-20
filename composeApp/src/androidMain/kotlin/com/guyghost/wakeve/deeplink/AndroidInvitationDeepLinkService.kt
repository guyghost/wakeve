package com.guyghost.wakeve.deeplink

import android.content.Context
import android.net.Uri
import com.guyghost.wakeve.BuildConfig
import com.guyghost.wakeve.models.InvitationAcceptResponse
import com.guyghost.wakeve.security.AndroidSecureTokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

sealed class InvitationDeepLinkAcceptanceResult {
    data class Accepted(val eventId: String, val message: String) : InvitationDeepLinkAcceptanceResult()
    data class AuthenticationRequired(val message: String) : InvitationDeepLinkAcceptanceResult()
    data class Rejected(val message: String) : InvitationDeepLinkAcceptanceResult()
    data class RetryableFailure(val message: String) : InvitationDeepLinkAcceptanceResult()
}

internal fun normalizeInvitationAcceptanceCode(code: String?): String? {
    return normalizeDeepLinkPathSegment(code)
}

internal fun normalizeInvitationAcceptanceEventId(eventId: String?): String? {
    return normalizeDeepLinkPathSegment(eventId)
}

internal fun invitationAcceptanceRejectedMessage(status: HttpStatusCode): String {
    return when (status) {
        HttpStatusCode.BadRequest -> "Lien d'invitation invalide."
        HttpStatusCode.NotFound,
        HttpStatusCode.Gone -> "Invitation invalide ou expirée."
        else -> "Invitation invalide ou expirée."
    }
}

internal fun invitationAcceptanceRetryableFailureMessage(): String {
    return "Impossible de rejoindre l'événement pour le moment. Réessayez plus tard."
}

internal fun invitationAcceptanceSuccessMessage(): String {
    return "Invitation acceptée."
}

internal fun invitationAcceptanceRejectedByServerMessage(): String {
    return "Invitation invalide ou expirée."
}

class AndroidInvitationDeepLinkService(
    context: Context,
    private val baseUrl: String = BuildConfig.SERVER_URL,
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {
    private val tokenStorage by lazy { AndroidSecureTokenStorage(context.applicationContext) }

    suspend fun acceptInvitation(code: String): InvitationDeepLinkAcceptanceResult {
        val normalizedCode = normalizeInvitationAcceptanceCode(code)
            ?: return InvitationDeepLinkAcceptanceResult.Rejected("Lien d'invitation invalide.")

        val accessToken = tokenStorage.getAccessToken()
            ?: return InvitationDeepLinkAcceptanceResult.AuthenticationRequired(
                "Connectez-vous pour accepter cette invitation."
            )

        return try {
            val response = httpClient.post("${baseUrl.trimEnd('/')}/api/invite/${Uri.encode(normalizedCode)}/accept") {
                header("Authorization", "Bearer $accessToken")
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val body = response.body<InvitationAcceptResponse>()
                    if (body.success) {
                        val acceptedEventId = normalizeInvitationAcceptanceEventId(body.eventId)
                            ?: return InvitationDeepLinkAcceptanceResult.Rejected("Réponse d'invitation invalide.")

                        InvitationDeepLinkAcceptanceResult.Accepted(
                            eventId = acceptedEventId,
                            message = invitationAcceptanceSuccessMessage()
                        )
                    } else {
                        InvitationDeepLinkAcceptanceResult.Rejected(invitationAcceptanceRejectedByServerMessage())
                    }
                }
                HttpStatusCode.Unauthorized -> InvitationDeepLinkAcceptanceResult.AuthenticationRequired(
                    "Connectez-vous pour accepter cette invitation."
                )
                HttpStatusCode.BadRequest,
                HttpStatusCode.NotFound,
                HttpStatusCode.Gone -> InvitationDeepLinkAcceptanceResult.Rejected(
                    invitationAcceptanceRejectedMessage(response.status)
                )
                else -> InvitationDeepLinkAcceptanceResult.RetryableFailure(
                    invitationAcceptanceRetryableFailureMessage()
                )
            }
        } catch (e: Exception) {
            InvitationDeepLinkAcceptanceResult.RetryableFailure(
                invitationAcceptanceRetryableFailureMessage()
            )
        }
    }
}
