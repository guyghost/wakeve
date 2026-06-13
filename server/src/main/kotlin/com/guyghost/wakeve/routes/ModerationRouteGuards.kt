package com.guyghost.wakeve.routes

import com.guyghost.wakeve.moderation.ModerationPolicy
import com.guyghost.wakeve.moderation.ModerationStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

data class ModeratedTextField(
    val name: String,
    val value: String?
)

suspend fun ApplicationCall.rejectRejectedModeratedText(
    moderationPolicy: ModerationPolicy,
    fields: List<ModeratedTextField>
): Boolean {
    val rejected = fields
        .asSequence()
        .filter { !it.value.isNullOrBlank() }
        .map { field -> field to moderationPolicy.evaluate(field.value.orEmpty()) }
        .firstOrNull { (_, result) -> result.status == ModerationStatus.REJECTED }
        ?: return false

    val (field, result) = rejected
    respond(
        HttpStatusCode.BadRequest,
        mapOf(
            "error" to result.userMessage,
            "reasonCode" to result.reasonCode,
            "field" to field.name
        )
    )
    return true
}
