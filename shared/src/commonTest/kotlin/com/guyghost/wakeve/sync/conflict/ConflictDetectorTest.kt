package com.guyghost.wakeve.sync.conflict

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.sample.SampleEventFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD tests for [ConflictDetector].
 *
 * Covers:
 * - No-conflict path
 * - Critical field conflict detection
 * - Non-critical field auto-resolution
 * - Structural field detection (participants, slots)
 * - Sample event exclusion
 * - Decision application
 */
class ConflictDetectorTest {

    // ─── Fixtures ──────────────────────────────────────────────────────────

    private val base = Event(
        id = "evt-001",
        title = "Team BBQ",
        description = "Summer party",
        organizerId = "user-org",
        participants = listOf("user-org", "user-a"),
        proposedSlots = listOf(
            TimeSlot("slot-1", "2026-06-01T14:00:00Z", "2026-06-01T18:00:00Z", "Europe/Paris", TimeOfDay.AFTERNOON)
        ),
        deadline = "2026-05-25T23:59:59Z",
        status = EventStatus.POLLING,
        finalDate = null,
        createdAt = "2026-04-01T10:00:00Z",
        updatedAt = "2026-04-10T10:00:00Z",
        eventType = EventType.PARTY
    )

    // ─── No-conflict path ──────────────────────────────────────────────────

    @Test
    fun `identical events produce empty conflict summary`() {
        val summary = ConflictDetector.detect(base, base.copy())
        assertFalse(summary.hasCritical)
        assertEquals(0, summary.totalConflicts)
    }

    @Test
    fun `only updatedAt differs produces no conflict`() {
        // updatedAt is NON_CRITICAL and same canonical value after serialisation
        val remote = base.copy(updatedAt = "2026-04-11T09:00:00Z")
        val summary = ConflictDetector.detect(base, remote)
        // updatedAt change alone → no critical, 1 non-critical auto-resolved
        assertFalse(summary.hasCritical)
    }

    // ─── Critical field detection ─────────────────────────────────────────

    @Test
    fun `title change is detected as critical`() {
        val remote = base.copy(title = "Team Beach Day", updatedAt = "2026-04-11T09:00:00Z")
        val summary = ConflictDetector.detect(base, remote)

        assertTrue(summary.hasCritical)
        val conflict = summary.criticalConflicts.find { it.fieldName == "title" }
        assertEquals("Team BBQ", conflict?.localValue)
        assertEquals("Team Beach Day", conflict?.remoteValue)
        assertEquals(ConflictFieldSeverity.CRITICAL, conflict?.severity)
    }

    @Test
    fun `status change is detected as critical`() {
        val remote = base.copy(status = EventStatus.CONFIRMED, updatedAt = "2026-04-11T09:00:00Z")
        val summary = ConflictDetector.detect(base, remote)

        assertTrue(summary.hasCritical)
        assertTrue(summary.criticalConflicts.any { it.fieldName == "status" })
    }

    @Test
    fun `finalDate change is detected as critical`() {
        val remote = base.copy(finalDate = "2026-06-01T14:00:00Z", updatedAt = "2026-04-11T09:00:00Z")
        val summary = ConflictDetector.detect(base, remote)

        assertTrue(summary.hasCritical)
        assertTrue(summary.criticalConflicts.any { it.fieldName == "finalDate" })
    }

    @Test
    fun `participants list change is detected as critical`() {
        val remote = base.copy(
            participants = listOf("user-org", "user-a", "user-b"),
            updatedAt = "2026-04-11T09:00:00Z"
        )
        val summary = ConflictDetector.detect(base, remote)

        assertTrue(summary.hasCritical)
        assertTrue(summary.criticalConflicts.any { it.fieldName == "participants" })
    }

    @Test
    fun `proposedSlots change is detected as critical`() {
        val remote = base.copy(
            proposedSlots = listOf(
                TimeSlot("slot-1", "2026-06-01T14:00:00Z", "2026-06-01T18:00:00Z", "Europe/Paris", TimeOfDay.AFTERNOON),
                TimeSlot("slot-2", "2026-06-08T10:00:00Z", "2026-06-08T18:00:00Z", "Europe/Paris", TimeOfDay.ALL_DAY)
            ),
            updatedAt = "2026-04-11T09:00:00Z"
        )
        val summary = ConflictDetector.detect(base, remote)

        assertTrue(summary.hasCritical)
        assertTrue(summary.criticalConflicts.any { it.fieldName == "proposedSlots" })
    }

    // ─── Non-critical field auto-resolution ───────────────────────────────

    @Test
    fun `expectedParticipants change is classified as non-critical`() {
        val remote = base.copy(expectedParticipants = 15, updatedAt = "2026-04-11T09:00:00Z")
        val summary = ConflictDetector.detect(base, remote)

        assertFalse(summary.hasCritical)
        assertTrue(summary.autoResolved.any { it.fieldName == "expectedParticipants" })
    }

    @Test
    fun `eventTypeCustom change is classified as non-critical`() {
        val local = base.copy(eventType = EventType.CUSTOM, eventTypeCustom = "Picnic")
        val remote = local.copy(eventTypeCustom = "Garden Party", updatedAt = "2026-04-11T09:00:00Z")
        val summary = ConflictDetector.detect(local, remote)

        assertFalse(summary.hasCritical)
        assertTrue(summary.autoResolved.any { it.fieldName == "eventTypeCustom" })
    }

    @Test
    fun `auto-resolve non-critical picks most recent timestamp`() {
        val local = base.copy(
            expectedParticipants = 10,
            updatedAt = "2026-04-10T10:00:00Z"  // older
        )
        val remote = base.copy(
            expectedParticipants = 20,
            updatedAt = "2026-04-11T10:00:00Z"  // newer
        )
        val summary = ConflictDetector.detect(local, remote)
        val decisions = ConflictDetector.autoResolveNonCritical(summary)

        val decision = decisions.find { it.fieldName == "expectedParticipants" }
            as? ResolutionDecision.AutoResolved
        assertEquals("20", decision?.chosenValue,
            "Remote (newer) value should win via LWW")
        assertEquals(ResolutionDecision.AutoStrategy.LAST_WRITE_WINS, decision?.strategy)
    }

    @Test
    fun `auto-resolve non-critical picks local when local is newer`() {
        val local = base.copy(
            expectedParticipants = 10,
            updatedAt = "2026-04-12T10:00:00Z"  // newer
        )
        val remote = base.copy(
            expectedParticipants = 20,
            updatedAt = "2026-04-11T10:00:00Z"  // older
        )
        val summary = ConflictDetector.detect(local, remote)
        val decisions = ConflictDetector.autoResolveNonCritical(summary)

        val decision = decisions.find { it.fieldName == "expectedParticipants" }
            as? ResolutionDecision.AutoResolved
        assertEquals("10", decision?.chosenValue,
            "Local (newer) value should win via LWW")
    }

    // ─── Mixed conflicts ───────────────────────────────────────────────────

    @Test
    fun `mixed critical and non-critical conflicts are classified separately`() {
        val remote = base.copy(
            title = "Changed Title",         // CRITICAL
            expectedParticipants = 25,       // NON_CRITICAL
            updatedAt = "2026-04-11T09:00:00Z"
        )
        val summary = ConflictDetector.detect(base, remote)

        assertTrue(summary.hasCritical)
        assertTrue(summary.criticalConflicts.any { it.fieldName == "title" })
        assertTrue(summary.autoResolved.any { it.fieldName == "expectedParticipants" })
    }

    // ─── Sample event exclusion ────────────────────────────────────────────

    @Test
    fun `sample events are never flagged as conflicting`() {
        val sampleEvent = SampleEventFactory.createSampleEvent()
        val remote = sampleEvent.copy(title = "Completely Different Title")

        val summary = ConflictDetector.detect(sampleEvent, remote)

        assertFalse(summary.hasCritical, "Sample events must never produce conflicts")
        assertEquals(0, summary.totalConflicts)
    }

    // ─── Decision application ──────────────────────────────────────────────

    @Test
    fun `applyDecisions KeepRemote overwrites local value`() {
        val remote = base.copy(title = "Remote Title", updatedAt = "2026-04-11T09:00:00Z")
        val summary = ConflictDetector.detect(base, remote)

        val decisions = summary.criticalConflicts
            .filter { it.fieldName == "title" }
            .map { ResolutionDecision.KeepRemote("title", it.remoteValue) }

        val merged = ConflictDetector.applyDecisions(base, decisions)
        assertEquals("Remote Title", merged.title)
    }

    @Test
    fun `applyDecisions KeepLocal preserves local value`() {
        val remote = base.copy(title = "Remote Title", updatedAt = "2026-04-11T09:00:00Z")
        val summary = ConflictDetector.detect(base, remote)

        val decisions = summary.criticalConflicts
            .filter { it.fieldName == "title" }
            .map { ResolutionDecision.KeepLocal("title", it.localValue) }

        val merged = ConflictDetector.applyDecisions(base, decisions)
        assertEquals("Team BBQ", merged.title)
    }

    @Test
    fun `applyDecisions does not affect non-targeted fields`() {
        val remote = base.copy(title = "Remote Title", updatedAt = "2026-04-11T09:00:00Z")
        val decisions = listOf(ResolutionDecision.KeepRemote("title", "Remote Title"))

        val merged = ConflictDetector.applyDecisions(base, decisions)
        // Other fields unchanged
        assertEquals(base.description, merged.description)
        assertEquals(base.status, merged.status)
        assertEquals(base.deadline, merged.deadline)
    }

    // ─── Field registry ────────────────────────────────────────────────────

    @Test
    fun `unknown fields default to NON_CRITICAL severity`() {
        assertEquals(ConflictFieldSeverity.NON_CRITICAL, EventFieldRegistry.severityOf("unknownField"))
    }

    @Test
    fun `all critical fields are correctly classified`() {
        listOf("title", "description", "status", "finalDate", "deadline", "proposedSlots", "participants")
            .forEach { field ->
                assertEquals(ConflictFieldSeverity.CRITICAL, EventFieldRegistry.severityOf(field),
                    "Expected $field to be CRITICAL")
            }
    }
}
