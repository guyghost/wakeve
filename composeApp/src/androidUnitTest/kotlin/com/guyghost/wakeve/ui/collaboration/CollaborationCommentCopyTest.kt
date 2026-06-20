package com.guyghost.wakeve.ui.collaboration

import com.guyghost.wakeve.models.CommentSection
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CollaborationCommentCopyTest {
    @Test
    fun commentInputAndActionsUseFrenchCopy() {
        assertEquals("Ajouter un commentaire...", commentInputPlaceholder())
        assertEquals("Envoyer le commentaire", commentSendContentDescription())
        assertEquals("Commentaire epingle", commentPinnedContentDescription())
        assertEquals("Options du commentaire", commentOptionsContentDescription())
        assertEquals("Repondre", commentReplyActionLabel())
        assertEquals("Modifier", commentEditActionLabel())
        assertEquals("Epingler", commentPinActionLabel(isPinned = false))
        assertEquals("Retirer l'epingle", commentPinActionLabel(isPinned = true))
        assertEquals("Supprimer", commentDeleteActionLabel(isOwnComment = true))
        assertEquals("Retirer", commentDeleteActionLabel(isOwnComment = false))
    }

    @Test
    fun commentListStatesUseFrenchCopy() {
        assertEquals("Afficher plus de reponses (3)", loadMoreRepliesLabel(3))
        assertEquals("Aucun commentaire", emptyCommentsTitle())
        assertEquals("Lancez la discussion pour aider le groupe a avancer.", emptyCommentsSubtitle())
    }

    @Test
    fun commentSectionTitlesUseFrenchCopy() {
        assertEquals("Commentaires", getSectionTitle(CommentSection.GENERAL))
        assertEquals("Commentaires des options", getSectionTitle(CommentSection.SCENARIO))
        assertEquals("Commentaires du sondage", getSectionTitle(CommentSection.POLL))
        assertEquals("Commentaires transport", getSectionTitle(CommentSection.TRANSPORT))
        assertEquals("Commentaires logement", getSectionTitle(CommentSection.ACCOMMODATION))
        assertEquals("Commentaires repas", getSectionTitle(CommentSection.MEAL))
        assertEquals("Commentaires equipement", getSectionTitle(CommentSection.EQUIPMENT))
        assertEquals("Commentaires activites", getSectionTitle(CommentSection.ACTIVITY))
        assertEquals("Commentaires budget", getSectionTitle(CommentSection.BUDGET))
    }

    @Test
    fun collaborationCommentCopyDoesNotUseEnglishDefaults() {
        val copy = buildList {
            add(commentInputPlaceholder())
            add(commentSendContentDescription())
            add(commentPinnedContentDescription())
            add(commentOptionsContentDescription())
            add(commentReplyActionLabel())
            add(commentEditActionLabel())
            add(commentPinActionLabel(isPinned = false))
            add(commentPinActionLabel(isPinned = true))
            add(commentDeleteActionLabel(isOwnComment = true))
            add(commentDeleteActionLabel(isOwnComment = false))
            add(loadMoreRepliesLabel(2))
            add(emptyCommentsTitle())
            add(emptyCommentsSubtitle())
            CommentSection.entries.forEach { section -> add(getSectionTitle(section)) }
        }

        copy.forEach { label ->
	            listOf(
	                "Add a comment",
	                "Send",
	                "Pinned",
	                "Reply",
	                "Edit",
	                "Unpin",
	                "Delete",
	                "Remove",
                "Load more replies",
                "No comments yet",
                "Be the first",
                "Comments"
            ).forEach { englishCopy ->
                assertFalse(
                    label.contains(englishCopy, ignoreCase = true),
                    "Copy should not contain `$englishCopy`: $label"
                )
            }
        }
    }
}
