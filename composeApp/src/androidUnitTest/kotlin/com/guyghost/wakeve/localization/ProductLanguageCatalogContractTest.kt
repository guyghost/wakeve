package com.guyghost.wakeve.localization

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
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
        val catalogs = localeDirectories.associateWith(::catalog)

        catalogs.forEach { (locale, entries) ->
            required.forEach { key -> assertTrue(key in entries, "$key missing from $locale") }
        }

        val canonical = catalogs.getValue("values")
        catalogs.forEach { (locale, entries) ->
            assertEquals(canonical.keys, entries.keys, "$locale catalog keys differ from values")
            canonical.forEach { (key, expected) ->
                val actual = entries.getValue(key)
                assertEquals(expected.kind, actual.kind, "$locale resource kind differs for $key")
                assertEquals(expected.pluralQuantities, actual.pluralQuantities, "$locale plural quantities differ for $key")
                assertEquals(expected.itemCount, actual.itemCount, "$locale array item structure differs for $key")
                assertEquals(expected.placeholders, actual.placeholders, "$locale positional placeholders differ for $key")
            }
        }
    }

    private fun catalog(locale: String): Map<String, ResourceContract> {
        val file = projectFile("composeApp/src/androidMain/res/$locale/strings.xml")
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        return buildMap {
            listOf("string", "plurals", "string-array").forEach { tag ->
                val nodes = document.getElementsByTagName(tag)
                repeat(nodes.length) { index ->
                    val element = nodes.item(index) as Element
                    val name = element.getAttribute("name")
                    val items = element.childElements("item")
                    put(
                        name,
                        ResourceContract(
                            kind = tag,
                            pluralQuantities = items.map { it.getAttribute("quantity") }.filter(String::isNotEmpty).toSet(),
                            itemCount = items.size.takeIf { tag == "string-array" },
                            placeholders = if (items.isEmpty()) {
                                listOf(positionalPlaceholders(element.textContent))
                            } else {
                                items.map { positionalPlaceholders(it.textContent) }
                            },
                        ),
                    )
                }
            }
        }
    }

    private fun Element.childElements(tag: String): List<Element> =
        (0 until childNodes.length)
            .map { childNodes.item(it) }
            .filterIsInstance<Element>()
            .filter { it.tagName == tag }

    private fun positionalPlaceholders(value: String): List<String> =
        positionalPlaceholder.findAll(value).map { it.value }.sorted().toList()

    private data class ResourceContract(
        val kind: String,
        val pluralQuantities: Set<String>,
        val itemCount: Int?,
        val placeholders: List<List<String>>,
    )

    private fun projectFile(path: String): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (!File(current, "settings.gradle.kts").isFile) {
            current = requireNotNull(current.parentFile) { "Could not locate project root for $path" }
        }
        return current.resolve(path)
    }

    private companion object {
        val localeDirectories = listOf("values", "values-en", "values-de", "values-es", "values-it", "values-pt")
        val positionalPlaceholder = Regex("%\\d+\\$[-#+ 0,(<]*\\d*(?:\\.\\d+)?[a-zA-Z]")
    }
}
