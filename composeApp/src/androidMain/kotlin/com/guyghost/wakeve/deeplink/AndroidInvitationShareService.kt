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
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.net.URI

sealed class InvitationShareCreationResult {
    data class Created(val invitation: InvitationResponse) : InvitationShareCreationResult()
    data class AuthenticationRequired(val message: String) : InvitationShareCreationResult()
    data class Failed(val message: String) : InvitationShareCreationResult()
}

internal fun normalizeInvitationShareEventId(eventId: String?): String? {
    return normalizeDeepLinkPathSegment(eventId)
}

internal fun invitationShareFailureMessage(status: HttpStatusCode? = null): String {
    return when (status) {
        HttpStatusCode.Forbidden -> "Vous n'avez pas l'autorisation de créer une invitation pour cet événement."
        HttpStatusCode.NotFound -> "Événement introuvable."
        else -> "Impossible de créer le lien d'invitation."
    }
}

internal fun normalizeCreatedInvitationResponse(
    invitation: InvitationResponse,
    expectedEventId: String
): InvitationResponse? {
    val normalizedExpectedEventId = normalizeInvitationShareEventId(expectedEventId) ?: return null
    val normalizedResponseEventId = normalizeInvitationShareEventId(invitation.eventId) ?: return null
    if (normalizedResponseEventId != normalizedExpectedEventId) {
        return null
    }

    val normalizedCode = normalizeDeepLinkPathSegment(invitation.code) ?: return null
    val inviteUrl = normalizeCanonicalInviteUrl(invitation.inviteUrl, normalizedCode) ?: return null
    val deepLinkUrl = normalizeCanonicalInviteDeepLink(invitation.deepLinkUrl, normalizedCode) ?: return null

    return invitation.copy(
        code = normalizedCode,
        eventId = normalizedResponseEventId,
        inviteUrl = inviteUrl,
        deepLinkUrl = deepLinkUrl
    )
}

private fun normalizeCanonicalInviteUrl(rawUrl: String, expectedCode: String): String? {
    return runCatching {
        val uri = URI(rawUrl.trim())
        val pathSegments = uri.path
            ?.split("/")
            ?.filter { it.isNotBlank() }
            .orEmpty()

        val code = normalizeDeepLinkPathSegment(pathSegments.getOrNull(1))
        if (
            uri.scheme == "https" &&
            uri.host == "wakeve.app" &&
            uri.userInfo == null &&
            uri.port == -1 &&
            uri.fragment == null &&
            uri.rawQuery == null &&
            pathSegments.size == 2 &&
            pathSegments.firstOrNull() == "invite" &&
            code == expectedCode
        ) {
            "https://wakeve.app/invite/$expectedCode"
        } else {
            null
        }
    }.getOrNull()
}

private fun normalizeCanonicalInviteDeepLink(rawUrl: String, expectedCode: String): String? {
    return runCatching {
        val uri = URI(rawUrl.trim())
        val pathSegments = uri.path
            ?.split("/")
            ?.filter { it.isNotBlank() }
            .orEmpty()

        val code = normalizeDeepLinkPathSegment(pathSegments.singleOrNull())
        if (
            uri.scheme == "wakeve" &&
            uri.host == "invite" &&
            uri.userInfo == null &&
            uri.port == -1 &&
            uri.fragment == null &&
            uri.rawQuery == null &&
            code == expectedCode
        ) {
            "wakeve://invite/$expectedCode"
        } else {
            null
        }
    }.getOrNull()
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
        val normalizedEventId = normalizeInvitationShareEventId(eventId)
            ?: return InvitationShareCreationResult.Failed("Événement introuvable.")

        val accessToken = tokenStorage.getAccessToken()
            ?: return InvitationShareCreationResult.AuthenticationRequired(
                "Connectez-vous pour créer une invitation."
            )

        return try {
            val response = httpClient.post("${baseUrl.trimEnd('/')}/api/events/${Uri.encode(normalizedEventId)}/invite") {
                header("Authorization", "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(CreateInvitationRequest())
            }

            when (response.status) {
                HttpStatusCode.Created -> {
                    val invitation = normalizeCreatedInvitationResponse(
                        invitation = response.body(),
                        expectedEventId = normalizedEventId
                    )
                    if (invitation != null) {
                        InvitationShareCreationResult.Created(invitation)
                    } else {
                        InvitationShareCreationResult.Failed(invitationShareFailureMessage())
                    }
                }
                HttpStatusCode.Unauthorized -> InvitationShareCreationResult.AuthenticationRequired(
                    "Connectez-vous pour créer une invitation."
                )
                else -> InvitationShareCreationResult.Failed(
                    invitationShareFailureMessage(response.status)
                )
            }
        } catch (e: Exception) {
            InvitationShareCreationResult.Failed(
                invitationShareFailureMessage()
            )
        }
    }
}
