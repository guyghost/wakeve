package com.guyghost.wakeve.localization

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductLanguageCatalogContractTest {
    @Test
    fun everyAndroidLocaleHasCanonicalKeys() {
        val required = setOf(
            "tab_ideas",
            "event_state_draft",
            "event_state_polling",
            "event_state_comparing",
            "event_state_confirmed",
            "event_state_organizing",
            "event_state_finalized",
            "notifications_filter_todo",
            "notifications_filter_information",
            "ai_prepare_options",
            "ai_proposal_to_review",
            "sync_waiting",
            "sync_retry",
            "milestone_first_event",
            "milestone_regular_voting",
            "milestone_organization_ready",
        )
        val catalogs = localeDirectories.associateWith(::catalogKeys)

        catalogs.forEach { (locale, keys) ->
            required.forEach { key -> assertTrue(key in keys, "$key missing from $locale") }
        }

        val canonicalKeys = catalogs.getValue("values")
        catalogs.forEach { (locale, keys) ->
            assertEquals(canonicalKeys, keys, "$locale catalog keys differ from values")
        }
    }

    private fun catalogKeys(locale: String): Set<String> {
        val file = projectFile("composeApp/src/androidMain/res/$locale/strings.xml")
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        return buildSet {
            listOf("string", "plurals", "string-array").forEach { tag ->
                val nodes = document.getElementsByTagName(tag)
                repeat(nodes.length) { index -> add(nodes.item(index).attributes.getNamedItem("name").nodeValue) }
            }
        }
    }

    private fun projectFile(path: String): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (!File(current, "settings.gradle.kts").isFile) {
            current = requireNotNull(current.parentFile) { "Could not locate project root for $path" }
        }
        return current.resolve(path)
    }

    private companion object {
        val localeDirectories = listOf("values", "values-en", "values-de", "values-es", "values-it", "values-pt")
    }
}
