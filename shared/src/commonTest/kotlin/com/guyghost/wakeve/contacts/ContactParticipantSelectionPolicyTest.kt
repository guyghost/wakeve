package com.guyghost.wakeve.contacts

import kotlin.test.Test
import kotlin.test.assertEquals

class ContactParticipantSelectionPolicyTest {
    @Test
    fun normalizesSelectedEmails() {
        val result = ContactParticipantSelectionPolicy.prepareSelection(
            selectedContacts = listOf(
                ContactParticipantCandidate("Alice", "  ALICE@Example.COM ")
            ),
            existingParticipantIds = emptyList()
        )

        assertEquals(listOf("alice@example.com"), result.emailsToAdd)
        assertEquals(emptyList(), result.skippedDuplicateEmails)
        assertEquals(emptyList(), result.skippedInvalidEmails)
    }

    @Test
    fun skipsExistingParticipantsAndSelectedDuplicates() {
        val result = ContactParticipantSelectionPolicy.prepareSelection(
            selectedContacts = listOf(
                ContactParticipantCandidate("Alice", "alice@example.com"),
                ContactParticipantCandidate("Alice Work", " ALICE@example.com "),
                ContactParticipantCandidate("Bob", "bob@example.com")
            ),
            existingParticipantIds = listOf("bob@example.com")
        )

        assertEquals(listOf("alice@example.com"), result.emailsToAdd)
        assertEquals(listOf("alice@example.com", "bob@example.com"), result.skippedDuplicateEmails)
    }

    @Test
    fun skipsInvalidEmails() {
        val result = ContactParticipantSelectionPolicy.prepareSelection(
            selectedContacts = listOf(
                ContactParticipantCandidate("No Email", ""),
                ContactParticipantCandidate("No Domain", "person@"),
                ContactParticipantCandidate("Valid", "valid@example.com")
            ),
            existingParticipantIds = emptyList()
        )

        assertEquals(listOf("valid@example.com"), result.emailsToAdd)
        assertEquals(listOf("", "person@"), result.skippedInvalidEmails)
    }
}
