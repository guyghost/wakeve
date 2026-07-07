package com.guyghost.wakeve.ui.components

import com.guyghost.wakeve.models.LocationType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LocationTypeCopyTest {
    @Test
    fun locationTypeLabelsUseFrenchUserCopy() {
        assertEquals("Ville", locationTypeLabel(LocationType.CITY))
        assertEquals("Region", locationTypeLabel(LocationType.REGION))
        assertEquals("Lieu precis", locationTypeLabel(LocationType.SPECIFIC_VENUE))
        assertEquals("En ligne", locationTypeLabel(LocationType.ONLINE))
    }

    @Test
    fun locationTypeHelpersDoNotExposeEnglishDefaults() {
        LocationType.entries
            .flatMap { type -> listOf(locationTypeLabel(type), locationTypeHelpText(type)) }
            .forEach { copy ->
                listOf("City", "Specific Venue", "Online", "Great for", "Perfect for", "Use this for").forEach {
                    assertFalse(copy.contains(it, ignoreCase = true), "Copy should not contain `$it`: $copy")
                }
            }
    }
}
