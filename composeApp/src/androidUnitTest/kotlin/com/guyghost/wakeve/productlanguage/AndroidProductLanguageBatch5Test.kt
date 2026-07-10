package com.guyghost.wakeve.productlanguage

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.w3c.dom.Element

class AndroidProductLanguageBatch5Test {
    @Test
    fun everyOwnedSurfaceUsesResourcesForDirectAndIndirectVisibleCopy() {
        val result = ownedPaths.flatMap(::findings)
        assertTrue(result.isEmpty(), result.joinToString("\n"))
    }

    @Test
    fun workflowProjectionCoversEveryStateAndFinalizedModeHasNoCallToAction() {
        val source = source(eventDetailPath)
        EventStateKeys.forEach { key -> assertTrue("R.string.$key" in source, "missing projection $key") }
        val finalized = source.substringAfter("private fun FinalizedModeActions").substringBefore("internal fun eventDetailStatusLabel")
        assertFalse(Regex("\\b(?:Button|TextButton|OutlinedButton|FilledTonalButton)\\s*\\(").containsMatchIn(finalized), "terminal mode must not expose a CTA")
    }

    @Test
    fun pendingOfflineErrorCancellationPermissionAndRetryOutcomesAreExplicit() {
        val all = ownedPaths.joinToString("\n") { source(it) }
        OutcomeKeys.forEach { key -> assertTrue("R.string.$key" in all, "missing outcome resource $key") }
    }

    @Test
    fun notificationsKeepFiltersAndExposeLocalizedAttentionAndTimeProjection() {
        val source = source(notificationPath)
        assertTrue("NotificationInboxFilter.ALL" in source && "NotificationInboxFilter.UNREAD" in source)
        NotificationKeys.forEach { key -> assertTrue("R.string.$key" in source || "R.plurals.$key" in source, "missing notification resource $key") }
    }

    @Test
    fun invitationAlbumsImagesAndSyncConflictHaveActionTargetStateSemantics() {
        val all = listOf(imagePath, invitationPath, albumsPath, conflictPath).joinToString("\n") { source(it) }
        A11yKeys.forEach { key -> assertTrue("R.string.$key" in all || "R.plurals.$key" in all, "missing semantic resource $key") }
        assertTrue("clearAndSetSemantics" in source(conflictPath), "conflict choices must suppress duplicate descendant speech")
    }

    @Test
    fun everyDerivedBatchFiveKeyHasNaturalSixLocaleParity() {
        val expected = reviewedKeys
        val french = catalog("values")
        assertTrue(expected.all(french::containsKey), "French catalog missing ${expected - french.keys}")
        val english = catalog("values-en")
        localeDirectories.forEach { directory ->
            val localized = catalog(directory)
            assertTrue(expected.all(localized::containsKey), "$directory missing ${expected - localized.keys}")
            expected.forEach { key ->
                assertEquals(placeholders(english.getValue(key)), placeholders(localized.getValue(key)), "$directory:$key placeholders")
            }
        }
        listOf("values-de", "values-es", "values-it", "values-pt").forEach { directory ->
            val localized = catalog(directory)
            expected.filterNot { it in exactTechnicalResourceKeys }.forEach { key ->
                assertTrue(localized.getValue(key) != english.getValue(key), "$directory:$key copied English")
            }
        }
    }

    @Test
    fun technicalLiteralsAreAllowedOnlyAtExactReviewedSites() {
        ownedPaths.forEach { path ->
            source(path).withoutComments().lineSequence().forEachIndexed { index, line ->
                literal.findAll(line).forEach { match ->
                    val value = match.groupValues[1]
                    if (value in technicalValues) {
                        assertTrue(ReviewedTechnicalOccurrence(path, line.trim()) in technicalOccurrences, "$path:${index + 1}: context-free technical literal '$value'")
                    }
                }
            }
        }
    }

    private fun findings(path: String): List<String> {
        val text = source(path).withoutComments()
        return direct.flatMap { (kind, regex) -> regex.findAll(text).map { "$path:${text.lineAt(it.range.first)}:$kind" }.toList() } +
            literal.findAll(text).mapNotNull { match ->
                val value = match.groupValues[1]
                val line = text.lineAt(match.range.first)
                val occurrence = ReviewedTechnicalOccurrence(path, text.lineSequence().elementAt(line - 1).trim())
                if (!value.any(Char::isLetter) || occurrence in technicalOccurrences) null else "$path:$line:indirect '$value'"
            }
    }

    private fun source(path: String) = root.resolve(path).readText()
    private fun catalog(directory: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(root.resolve("composeApp/src/androidMain/res/$directory/strings.xml"))
        return listOf("string", "plurals").flatMap { tag ->
            val nodes = document.getElementsByTagName(tag)
            (0 until nodes.length).map { index ->
                val element = nodes.item(index) as Element
                element.getAttribute("name") to element.textContent.trim()
            }
        }.toMap()
    }
    private fun placeholders(value: String) = Regex("""%\d+\$(?:\.\d+)?[a-z]""").findAll(value).map { it.value }.toList()
    private fun String.withoutComments() = replace(Regex("""(?s)/\*.*?\*/""")) { " ".repeat(it.value.length) }
        .lineSequence().joinToString("\n") { if (it.trimStart().startsWith("//")) " ".repeat(it.length) else it }
    private fun String.lineAt(index: Int) = take(index).count { it == '\n' } + 1

    private data class ReviewedTechnicalOccurrence(val path: String, val line: String)

    private companion object {
        val root: File by lazy { var file = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile; while (!File(file, "settings.gradle.kts").isFile) file = requireNotNull(file.parentFile); file }
        const val imagePath = "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/components/WakeveAsyncImage.kt"
        const val wizardPath = "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/event/DraftEventWizard.kt"
        const val eventDetailPath = "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/event/ModernEventDetailView.kt"
        const val invitationPath = "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/invitation/InvitationShareScreen.kt"
        const val notificationPath = "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/notification/NotificationsScreen.kt"
        const val albumsPath = "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/screens/AlbumsScreen.kt"
        const val conflictPath = "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/sync/ConflictResolutionDialog.kt"
        val ownedPaths = listOf(imagePath, wizardPath, eventDetailPath, invitationPath, notificationPath, albumsPath, conflictPath)
        val direct = listOf("Text" to Regex("""\bText\s*\(\s*(?:text\s*=\s*)?\""""), "argument" to Regex("""\b(?:label|placeholder|supportingText|contentDescription)\s*=\s*\""""))
        val literal = Regex("""\"([^\"\\]*(?:\\.[^\"\\]*)*)\"""")
        val technicalValues = setOf("currentUser", "temp-event-id", "step_transition", "https://wakeve.app/invite/${'$'}it", "text/plain", "UTF-8")
        val technicalOccurrences = setOf(
            ReviewedTechnicalOccurrence(wizardPath, "userId: String = \"currentUser\","),
            ReviewedTechnicalOccurrence(wizardPath, "label = \"step_transition\""),
            ReviewedTechnicalOccurrence(wizardPath, "eventId = initialEvent?.id ?: \"temp-event-id\""),
            ReviewedTechnicalOccurrence(invitationPath, "?.let { \"https://wakeve.app/invite/${'$'}it\" }"),
            ReviewedTechnicalOccurrence(invitationPath, "type = \"text/plain\""),
            ReviewedTechnicalOccurrence(invitationPath, "EncodeHintType.CHARACTER_SET to \"UTF-8\","),
        )
        val EventStateKeys = setOf("event_state_draft", "event_state_polling", "event_state_comparing", "event_state_confirmed", "event_state_organizing", "event_state_finalized", "event_next_step_draft", "event_next_step_polling", "event_next_step_comparing", "event_next_step_confirmed", "event_next_step_organizing", "event_terminal_summary")
        val OutcomeKeys = setOf("sync_waiting", "offline_status", "error_generic", "action_retry", "action_cancel", "permission_required", "invitation_loading", "invitation_error")
        val NotificationKeys = setOf("notifications_filter_all", "notifications_filter_unread", "notification_time_now", "notification_minutes_ago", "notification_hours_ago", "notification_days_ago", "notification_attention_required", "notification_information")
        val A11yKeys = setOf("a11y_event_cover_image", "a11y_user_avatar", "a11y_invitation_qr", "a11y_copy_invitation_link", "a11y_album_open", "a11y_album_cover", "a11y_photo", "a11y_conflict_choice", "a11y_conflict_summary")
        val reviewedKeys = EventStateKeys + OutcomeKeys + NotificationKeys + A11yKeys + setOf("album_photo_count", "sync_conflict_count", "draft_time_range", "draft_location_summary", "draft_slot_summary")
        val exactTechnicalResourceKeys = emptySet<String>()
        val localeDirectories = listOf("values", "values-en", "values-de", "values-es", "values-it", "values-pt")
    }
}
