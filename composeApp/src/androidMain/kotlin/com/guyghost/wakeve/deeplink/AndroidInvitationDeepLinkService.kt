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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

sealed class InvitationDeepLinkAcceptanceResult {
    data class Accepted(val eventId: String, val message: String) : InvitationDeepLinkAcceptanceResult()
    data class AuthenticationRequired(val message: String) : InvitationDeepLinkAcceptanceResult()
    data class Rejected(val message: String) : InvitationDeepLinkAcceptanceResult()
    data class RetryableFailure(val message: String) : InvitationDeepLinkAcceptanceResult()
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
        val accessToken = tokenStorage.getAccessToken()
            ?: return InvitationDeepLinkAcceptanceResult.AuthenticationRequired(
                "Connectez-vous pour accepter cette invitation."
            )

        return try {
            val response = httpClient.post("${baseUrl.trimEnd('/')}/api/invite/${Uri.encode(code)}/accept") {
                header("Authorization", "Bearer $accessToken")
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val body = response.body<InvitationAcceptResponse>()
                    if (body.success) {
                        InvitationDeepLinkAcceptanceResult.Accepted(
                            eventId = body.eventId,
                            message = body.message
                        )
                    } else {
                        InvitationDeepLinkAcceptanceResult.Rejected(body.message)
                    }
                }
                HttpStatusCode.Unauthorized -> InvitationDeepLinkAcceptanceResult.AuthenticationRequired(
                    "Connectez-vous pour accepter cette invitation."
                )
                HttpStatusCode.BadRequest,
                HttpStatusCode.NotFound,
                HttpStatusCode.Gone -> InvitationDeepLinkAcceptanceResult.Rejected(
                    response.bodyAsText().ifBlank { "Invitation invalide ou expirée." }
                )
                else -> InvitationDeepLinkAcceptanceResult.RetryableFailure(
                    "Impossible de rejoindre l'événement pour le moment. Réessayez plus tard."
                )
            }
        } catch (e: Exception) {
            InvitationDeepLinkAcceptanceResult.RetryableFailure(
                e.message ?: "Impossible de rejoindre l'événement pour le moment."
            )
        }
    }
}
