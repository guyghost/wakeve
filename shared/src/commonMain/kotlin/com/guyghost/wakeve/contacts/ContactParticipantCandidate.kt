package com.guyghost.wakeve.contacts

import kotlinx.serialization.Serializable

@Serializable
data class ContactParticipantCandidate(
    val displayName: String,
    val email: String
)

@Serializable
data class ContactParticipantSelectionResult(
    val emailsToAdd: List<String>,
    val skippedDuplicateEmails: List<String>,
    val skippedInvalidEmails: List<String>
)

object ContactParticipantSelectionPolicy {
    fun prepareSelection(
        selectedContacts: List<ContactParticipantCandidate>,
        existingParticipantIds: List<String>
    ): ContactParticipantSelectionResult {
        val existingEmails = existingParticipantIds
            .mapNotNull { normalizeEmailOrNull(it) }
            .toSet()
        val emailsToAdd = mutableListOf<String>()
        val seenSelectedEmails = mutableSetOf<String>()
        val skippedDuplicateEmails = mutableListOf<String>()
        val skippedInvalidEmails = mutableListOf<String>()

        selectedContacts.forEach { contact ->
            val normalizedEmail = normalizeEmailOrNull(contact.email)
            if (normalizedEmail == null) {
                skippedInvalidEmails += contact.email.trim()
                return@forEach
            }

            if (normalizedEmail in existingEmails || normalizedEmail in seenSelectedEmails) {
                skippedDuplicateEmails += normalizedEmail
                return@forEach
            }

            seenSelectedEmails += normalizedEmail
            emailsToAdd += normalizedEmail
        }

        return ContactParticipantSelectionResult(
            emailsToAdd = emailsToAdd,
            skippedDuplicateEmails = skippedDuplicateEmails.distinct(),
            skippedInvalidEmails = skippedInvalidEmails.distinct()
        )
    }

    fun normalizeEmailOrNull(email: String): String? {
        val normalized = email.trim().lowercase()
        if (normalized.isBlank()) return null
        if (!normalized.contains("@")) return null
        val parts = normalized.split("@")
        if (parts.size != 2) return null
        val domain = parts[1]
        if (parts[0].isBlank() || domain.isBlank() || !domain.contains(".")) return null
        return normalized
    }
}
