package com.guyghost.wakeve.ui.comment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CommentsScreenErrorMessageTest {

    @Test
    fun commentLoadFailureMessage_doesNotExposeRepositoryFailureDetails() {
        val repositoryFailure = "SQL read failed for event-1 user secret@example.com token=SECRET"

        val result = commentLoadFailureMessage()

        assertEquals("Impossible de charger les commentaires. Reessayez.", result)
        assertFalse(result.contains(repositoryFailure))
        assertFalse(result.contains("secret@example.com", ignoreCase = true))
        assertFalse(result.contains("SECRET", ignoreCase = true))
        assertFalse(result.contains("SQL read", ignoreCase = true))
        assertFalse(result.contains("token=", ignoreCase = true))
    }

    @Test
    fun commentSubmitFailureMessage_doesNotExposeRepositoryFailureDetails() {
        val repositoryFailure = "SQL write failed for event-1 user secret@example.com token=SECRET"

        val result = commentSubmitFailureMessage()

        assertEquals("Impossible d'enregistrer le commentaire. Reessayez.", result)
        assertFalse(result.contains(repositoryFailure))
        assertFalse(result.contains("secret@example.com", ignoreCase = true))
        assertFalse(result.contains("SECRET", ignoreCase = true))
        assertFalse(result.contains("SQL write", ignoreCase = true))
        assertFalse(result.contains("token=", ignoreCase = true))
    }

    @Test
    fun commentDeleteFailureMessage_doesNotExposeRepositoryFailureDetails() {
        val repositoryFailure = "SQL delete failed for event-1 user secret@example.com token=SECRET"

        val result = commentDeleteFailureMessage()

        assertEquals("Impossible de supprimer le commentaire. Reessayez.", result)
        assertFalse(result.contains(repositoryFailure))
        assertFalse(result.contains("secret@example.com", ignoreCase = true))
        assertFalse(result.contains("SECRET", ignoreCase = true))
        assertFalse(result.contains("SQL delete", ignoreCase = true))
        assertFalse(result.contains("token=", ignoreCase = true))
    }
}
