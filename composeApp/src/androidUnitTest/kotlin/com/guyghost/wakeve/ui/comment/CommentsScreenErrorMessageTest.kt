package com.guyghost.wakeve.ui.comment

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommentsScreenErrorMessageTest {
    @Test
    fun repositoryFailuresMapToLocalizedSanitizedMessages() {
        val source = commentsScreen.readText()
        listOf(
            "R.string.comment_load_error",
            "R.string.comment_submit_error",
            "R.string.comment_delete_error",
        ).forEach { resource -> assertTrue(source.contains(resource), "Missing $resource") }
        assertFalse(source.contains("state.message = e.message"))
        assertFalse(source.contains("submitErrorMessage = e.message"))
        assertFalse(source.contains("deleteErrorMessage = e.message"))
        assertFalse(source.contains("fun commentLoadFailureMessage("))
    }

    private companion object {
        val commentsScreen: File by lazy {
            var root = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
            while (!File(root, "settings.gradle.kts").isFile) root = requireNotNull(root.parentFile)
            root.resolve("composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/comment/CommentsScreen.kt")
        }
    }
}
