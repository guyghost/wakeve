package com.guyghost.wakeve.ui.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventLifecycleBlockerFormatterTest {

    private val labels = mapOf(
        "MEETING_REQUIRED" to "une réunion",
        "BUDGET_REQUIRED" to "le budget",
        "ORGANIZER_REQUIRED" to "un organisateur",
        "ACCESS_ORGANIZER_REQUIRED" to "un organisateur",
    )

    private fun format(failure: String?) = formatEventLifecycleFailure(
        failure = failure,
        blockedIntro = "Il reste à régler :",
        genericMessage = "Impossible de changer l’étape de l’événement. Réessayez.",
        blockerLabel = labels::get,
        joinLabels = { it.joinToString(" | ") },
    )

    @Test
    fun `known blocker codes become plain-language labels after the intro`() {
        assertEquals(
            "Il reste à régler : une réunion | le budget.",
            format("Finalization blocked by MEETING_REQUIRED,BUDGET_REQUIRED"),
        )
    }

    @Test
    fun `unknown codes are skipped and never shown raw`() {
        val message = format("Finalization blocked by MEETING_REQUIRED,SOMETHING_NEW")

        assertEquals("Il reste à régler : une réunion.", message)
        assertFalse(message.contains("SOMETHING_NEW"))
        assertFalse(message.contains("MEETING_REQUIRED"))
    }

    @Test
    fun `only unknown codes fall back to the generic message`() {
        assertEquals(
            "Impossible de changer l’étape de l’événement. Réessayez.",
            format("Finalization blocked by SOMETHING_NEW"),
        )
    }

    @Test
    fun `non blocker failures fall back to the generic message`() {
        val generic = "Impossible de changer l’étape de l’événement. Réessayez."
        assertEquals(generic, format(null))
        assertEquals(generic, format(""))
        assertEquals(generic, format("Failed to finalize event"))
        assertEquals(generic, format("Cannot transition to organizing: Event is not in CONFIRMED status"))
        assertEquals(generic, format("Finalization blocked by "))
    }

    @Test
    fun `duplicate labels are listed once and whitespace is tolerated`() {
        assertEquals(
            "Il reste à régler : un organisateur.",
            format("Finalization blocked by ORGANIZER_REQUIRED, ACCESS_ORGANIZER_REQUIRED ,"),
        )
    }

    @Test
    fun `codes are matched case-insensitively`() {
        assertEquals("Il reste à régler : le budget.", format("Finalization blocked by budget_required"))
    }

    @Test
    fun `every shared finalization blocker code is known`() {
        val expected = setOf(
            "ORGANIZER_REQUIRED",
            "CONFIRMED_PARTICIPANTS_REQUIRED",
            "FINAL_SCENARIO_REQUIRED",
            "DESTINATION_REQUIRED",
            "LODGING_REQUIRED",
            "TRANSPORT_REQUIRED",
            "MEETING_REQUIRED",
            "CALENDAR_CONFIRMED_DATE_REQUIRED",
            "NOTIFICATION_REMINDERS_REQUIRED",
            "BUDGET_REQUIRED",
            "PAYMENT_POT_REQUIRED",
            "TRICOUNT_HANDOFF_REQUIRED",
            "CRITICAL_SYNC_PENDING",
            "CRITICAL_SYNC_FAILED",
            "CRITICAL_CONFLICT_PENDING",
            "UNSAFE_EXTERNAL_LINKS",
            "ACCESS_ORGANIZER_REQUIRED",
            "ACCESS_CONFIRMED_PARTICIPANTS_REQUIRED",
            "EVENT_NOT_ORGANIZING",
        )
        assertEquals(expected, EventLifecycleBlockerCodes.all.toSet())
        assertTrue(EventLifecycleBlockerCodes.all.size == expected.size)
    }
}
