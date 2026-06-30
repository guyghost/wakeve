package com.guyghost.wakeve.auth

import com.guyghost.wakeve.BuildConfig
import com.guyghost.wakeve.auth.shell.services.AccountDeletionGateway
import com.guyghost.wakeve.models.AccountDeletionResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Android implementation of authenticated account deletion against the Wakeve API.
 */
class AndroidAccountDeletionGateway(
    serverBaseUrl: String = resolveAndroidAuthServerBaseUrl(BuildConfig.SERVER_URL),
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) : AccountDeletionGateway {

    private val apiBaseUrl = serverBaseUrl

    override suspend fun deleteAccount(accessToken: String): Result<AccountDeletionResponse> = runCatching {
        val response = httpClient.delete("$apiBaseUrl/api/user/delete") {
            header("Authorization", "Bearer $accessToken")
            header("Accept", "application/json")
        }

        when (response.status) {
            HttpStatusCode.OK -> response.body<AccountDeletionResponse>()
            HttpStatusCode.Unauthorized -> throw AccountDeletionException("Authentication required")
            else -> throw AccountDeletionException("Account deletion failed")
        }
    }
}

internal class AccountDeletionException(message: String) : Exception(message)
