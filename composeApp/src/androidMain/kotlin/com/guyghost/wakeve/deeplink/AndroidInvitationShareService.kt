package com.guyghost.wakeve.deeplink

import android.content.Context
import android.net.Uri
import com.guyghost.wakeve.BuildConfig
import com.guyghost.wakeve.models.CreateInvitationRequest
import com.guyghost.wakeve.models.InvitationResponse
import com.guyghost.wakeve.security.AndroidSecureTokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

sealed class InvitationShareCreationResult {
    data class Created(val invitation: InvitationResponse) : InvitationShareCreationResult()
    data class AuthenticationRequired(val message: String) : InvitationShareCreationResult()
    data class Failed(val message: String) : InvitationShareCreationResult()
}

class AndroidInvitationShareService(
    context: Context,
    private val baseUrl: String = BuildConfig.SERVER_URL,
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {
    private val tokenStorage by lazy { AndroidSecureTokenStorage(context.applicationContext) }

    suspend fun createInvitation(eventId: String): InvitationShareCreationResult {
        val accessToken = tokenStorage.getAccessToken()
            ?: return InvitationShareCreationResult.AuthenticationRequired(
                "Connectez-vous pour créer une invitation."
            )

        return try {
            val response = httpClient.post("${baseUrl.trimEnd('/')}/api/events/${Uri.encode(eventId)}/invite") {
                header("Authorization", "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(CreateInvitationRequest())
            }

            when (response.status) {
                HttpStatusCode.Created -> InvitationShareCreationResult.Created(response.body())
                HttpStatusCode.Unauthorized -> InvitationShareCreationResult.AuthenticationRequired(
                    "Connectez-vous pour créer une invitation."
                )
                else -> InvitationShareCreationResult.Failed(
                    response.bodyAsText().ifBlank { "Impossible de créer le lien d'invitation." }
                )
            }
        } catch (e: Exception) {
            InvitationShareCreationResult.Failed(
                e.message ?: "Impossible de créer le lien d'invitation."
            )
        }
    }
}
