package com.guyghost.wakeve.ui.collaboration

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollaborationCommentCopyTest {
    @Test
    fun collaborationSurfacesUseResourceBackedCopy() {
        val source = listOf("CommentInput.kt", "CommentItem.kt", "CommentListScreen.kt")
            .joinToString("\n") { file -> productionDirectory.resolve(file).readText() }

        listOf(
            "R.string.comment_input_placeholder", "R.string.a11y_comment_send",
            "R.string.comments_empty", "R.plurals.comment_reply_count",
            "R.string.comment_reply", "R.string.comment_edit", "R.string.comment_pin",
            "R.string.comment_delete", "R.string.a11y_comment_reply",
        ).forEach { resource -> assertTrue(source.contains(resource), "Missing $resource") }

        listOf(
            "commentInputPlaceholder", "commentSendContentDescription", "getSectionTitle",
            "loadMoreRepliesLabel", "emptyCommentsTitle", "emptyCommentsSubtitle",
            "commentReplyActionLabel", "commentEditActionLabel", "commentPinActionLabel",
            "commentDeleteActionLabel",
        ).forEach { helper -> assertFalse(source.contains("fun $helper("), "Legacy helper $helper") }
    }

    private companion object {
        val productionDirectory: File by lazy {
            var root = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
            while (!File(root, "settings.gradle.kts").isFile) root = requireNotNull(root.parentFile)
            root.resolve("composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/collaboration")
        }
    }
}
