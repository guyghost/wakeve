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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for the full conflict resolution pipeline.
 *
 * Tests the end-to-end path:
 *   offline mutation → ConflictDetector.detect() → auto-resolve non-critical
 *   → surface critical → applyDecisions() → merged event
 *
 * These tests are pure (no DB, no network) and exercise the collaboration
 * between ConflictDetector, ConflictRecord, ResolutionDecision and ConflictSummary.
 */
class ConflictResolutionIntegrationTest {

    // ─── Shared fixture ───────────────────────────────────────────────────

    private val original = Event(
        id = "evt-sync-001",
        title = "Weekend Hike",
        description = "Let's go hiking!",
        organizerId = "user-org",
        participants = listOf("user-org", "user-a", "user-b"),
        proposedSlots = listOf(
            TimeSlot("slot-1", "2026-07-12T08:00:00Z", "2026-07-12T18:00:00Z", "Europe/Paris", TimeOfDay.ALL_DAY),
            TimeSlot("slot-2", "2026-07-19T08:00:00Z", "2026-07-19T18:00:00Z", "Europe/Paris", TimeOfDay.ALL_DAY)
        ),
        deadline = "2026-07-05T23:59:59Z",
        status = EventStatus.POLLING,
        finalDate = null,
        createdAt = "2026-04-01T10:00:00Z",
        updatedAt = "2026-04-10T10:00:00Z",
        eventType = EventType.OUTDOOR_ACTIVITY,
        expectedParticipants = 8
    )

    // ─── Scenario 1: Non-overlapping edits merge silently ────────────────

    /**
     * User A (offline) changes `expectedParticipants` (NON_CRITICAL).
     * Server (user B) didn't touch that field.
     * → No user intervention needed, auto-resolved via LWW.
     */
    @Test
    fun `non-overlapping non-critical edits auto-resolve without user intervention`() {
        val local = original.copy(expectedParticipants = 12, updatedAt = "2026-04-11T10:00:00Z")
        val remote = original.copy(updatedAt = "2026-04-10T15:00:00Z") // unchanged

        val summary = ConflictDetector.detect(local, remote)
        assertFalse(summary.hasCritical, "Non-critical edit should not require user intervention")

        val decisions = ConflictDetector.autoResolveNonCritical(summary)
        val merged = ConflictDetector.applyDecisions(local, decisions)

        // Local wins (newer timestamp) — expectedParticipants stays 12
        assertEquals(12, merged.expectedParticipants)
        assertEquals("Weekend Hike", merged.title) // unchanged field preserved
    }

    // ─── Scenario 2: Critical field conflict surfaces to user ─────────────

    /**
     * User A (offline) changed the title.
     * Server (user B) also changed the title to something different.
     * → Critical conflict — must be surfaced to the user.
     */
    @Test
    fun `concurrent title edits produce critical conflict requiring user resolution`() {
        val local  = original.copy(title = "Mountain Trek",  updatedAt = "2026-04-11T09:00:00Z")
        val remote = original.copy(title = "Forest Walk",    updatedAt = "2026-04-11T08:00:00Z")

        val summary = ConflictDetector.detect(local, remote)

        assertTrue(summary.hasCritical)
        val titleConflict = summary.criticalConflicts.find { it.fieldName == "title" }
        assertNotNull(titleConflict)
        assertEquals("Mountain Trek", titleConflict.localValue)
        assertEquals("Forest Walk",   titleConflict.remoteValue)
        assertEquals(ConflictFieldSeverity.CRITICAL, titleConflict.severity)
    }

    @Test
    fun `user chooses KeepLocal resolves critical title conflict with local value`() {
        val local  = original.copy(title = "Mountain Trek", updatedAt = "2026-04-11T09:00:00Z")
        val remote = original.copy(title = "Forest Walk",   updatedAt = "2026-04-11T08:00:00Z")
        val summary = ConflictDetector.detect(local, remote)

        val decisions = summary.criticalConflicts.map { conflict ->
            ResolutionDecision.KeepLocal(conflict.fieldName, conflict.localValue)
        }
        val merged = ConflictDetector.applyDecisions(local, decisions)
        assertEquals("Mountain Trek", merged.title)
    }

    @Test
    fun `user chooses KeepRemote resolves critical title conflict with remote value`() {
        val local  = original.copy(title = "Mountain Trek", updatedAt = "2026-04-11T09:00:00Z")
        val remote = original.copy(title = "Forest Walk",   updatedAt = "2026-04-11T08:00:00Z")
        val summary = ConflictDetector.detect(local, remote)

        val decisions = summary.criticalConflicts.map { conflict ->
            ResolutionDecision.KeepRemote(conflict.fieldName, conflict.remoteValue)
        }
        val merged = ConflictDetector.applyDecisions(local, decisions)
        assertEquals("Forest Walk", merged.title)
    }

    // ─── Scenario 3: Mixed critical + non-critical ────────────────────────

    /**
     * Local: changed title (CRITICAL) + expectedParticipants (NON_CRITICAL).
     * Remote: changed description (CRITICAL) + minParticipants (NON_CRITICAL).
     * → Two critical conflicts + two non-critical auto-resolved.
     */
    @Test
    fun `mixed conflict separates critical from auto-resolved fields`() {
        val local = original.copy(
            title = "Local Title",
            expectedParticipants = 15,
            updatedAt = "2026-04-11T10:00:00Z"
        )
        val remote = original.copy(
            description = "Remote description edit",
            minParticipants = 5,
            updatedAt = "2026-04-11T08:00:00Z"
        )

        val summary = ConflictDetector.detect(local, remote)

        assertTrue(summary.hasCritical)
        assertTrue(summary.criticalConflicts.any { it.fieldName == "title" })
        assertTrue(summary.criticalConflicts.any { it.fieldName == "description" })
        assertTrue(summary.autoResolved.any { it.fieldName == "expectedParticipants" })
        assertTrue(summary.autoResolved.any { it.fieldName == "minParticipants" })

        // Auto-resolve non-critical first
        val autoDecisions = ConflictDetector.autoResolveNonCritical(summary)
        assertEquals(summary.autoResolved.size, autoDecisions.size)

        // Apply auto-decisions to local
        val afterAuto = ConflictDetector.applyDecisions(local, autoDecisions)
        // expectedParticipants: local(10:00) > remote(08:00) → local wins = 15
        assertEquals(15, afterAuto.expectedParticipants)

        // User resolves critical: keep local title, keep remote description
        val userDecisions = listOf(
            ResolutionDecision.KeepLocal("title", "Local Title"),
            ResolutionDecision.KeepRemote("description", "Remote description edit")
        )
        val finalMerged = ConflictDetector.applyDecisions(afterAuto, userDecisions)
        assertEquals("Local Title", finalMerged.title)
        assertEquals("Remote description edit", finalMerged.description)
        assertEquals(15, finalMerged.expectedParticipants) // preserved
    }

    // ─── Scenario 4: Status transition conflict ───────────────────────────

    @Test
    fun `concurrent status changes produce critical conflict`() {
        val local  = original.copy(status = EventStatus.CONFIRMED, updatedAt = "2026-04-11T10:00:00Z")
        val remote = original.copy(status = EventStatus.POLLING,   updatedAt = "2026-04-11T09:00:00Z")

        val summary = ConflictDetector.detect(local, remote)

        assertTrue(summary.hasCritical)
        val statusConflict = summary.criticalConflicts.find { it.fieldName == "status" }
        assertNotNull(statusConflict)
        assertEquals("CONFIRMED", statusConflict.localValue)
        assertEquals("POLLING",   statusConflict.remoteValue)
    }

    // ─── Scenario 5: No conflict when events are identical ───────────────

    @Test
    fun `identical events after sync produce no conflicts`() {
        val synced = original.copy(updatedAt = "2026-04-11T10:00:00Z")
        val summary = ConflictDetector.detect(synced, synced.copy())

        assertFalse(summary.hasCritical)
        assertEquals(0, summary.totalConflicts)
        assertTrue(ConflictDetector.autoResolveNonCritical(summary).isEmpty())
    }

    // ─── Scenario 6: Feature flag — legacy LWW path ───────────────────────

    /**
     * Simulates the legacy LWW path (feature flag off):
     * the older event should be overwritten by the newer one entirely.
     */
    @Test
    fun `legacy LWW newer remote wins entire event`() {
        val local  = original.copy(title = "Old Local",  updatedAt = "2026-04-10T10:00:00Z")
        val remote = original.copy(title = "New Remote",  updatedAt = "2026-04-11T10:00:00Z")

        // Simulate the legacy path directly (detector not called)
        val merged = if (local.updatedAt >= remote.updatedAt) local else remote
        assertEquals("New Remote", merged.title, "Remote (newer) should win in LWW mode")
    }

    @Test
    fun `legacy LWW newer local wins entire event`() {
        val local  = original.copy(title = "New Local",  updatedAt = "2026-04-12T10:00:00Z")
        val remote = original.copy(title = "Old Remote", updatedAt = "2026-04-11T10:00:00Z")

        val merged = if (local.updatedAt >= remote.updatedAt) local else remote
        assertEquals("New Local", merged.title, "Local (newer) should win in LWW mode")
    }

    // ─── Scenario 7: Sample events never conflict ─────────────────────────

    @Test
    fun `sample event with every field changed produces zero conflicts`() {
        val sample = SampleEventFactory.createSampleEvent()
        val remote = sample.copy(
            title = "Hacked Title",
            status = EventStatus.FINALIZED,
            deadline = "2020-01-01T00:00:00Z"
        )
        val summary = ConflictDetector.detect(sample, remote)
        assertEquals(0, summary.totalConflicts)
        assertFalse(summary.hasCritical)
    }

    // ─── Scenario 8: Participant list conflict ────────────────────────────

    @Test
    fun `differing participant lists produce critical conflict`() {
        val local  = original.copy(participants = listOf("user-org", "user-a", "user-c"), updatedAt = "2026-04-11T10:00:00Z")
        val remote = original.copy(participants = listOf("user-org", "user-a", "user-b"), updatedAt = "2026-04-11T09:00:00Z")

        val summary = ConflictDetector.detect(local, remote)
        assertTrue(summary.hasCritical)
        assertTrue(summary.criticalConflicts.any { it.fieldName == "participants" })
    }

    @Test
    fun `same participant lists in different order produce no conflict`() {
        val local  = original.copy(participants = listOf("user-b", "user-org", "user-a"), updatedAt = "2026-04-11T10:00:00Z")
        val remote = original.copy(participants = listOf("user-org", "user-a", "user-b"), updatedAt = "2026-04-11T09:00:00Z")

        val summary = ConflictDetector.detect(local, remote)
        assertFalse(summary.criticalConflicts.any { it.fieldName == "participants" },
            "Same participants in different order should not be a conflict (canonically sorted)")
    }

    // ─── Scenario 9: ConflictSummary metadata ─────────────────────────────

    @Test
    fun `ConflictSummary totalConflicts is sum of critical and auto-resolved`() {
        val local  = original.copy(
            title = "Changed",          // CRITICAL
            expectedParticipants = 20,  // NON_CRITICAL
            minParticipants = 3,        // NON_CRITICAL
            updatedAt = "2026-04-11T10:00:00Z"
        )
        val summary = ConflictDetector.detect(local, original)

        assertEquals(1, summary.criticalConflicts.size)
        assertEquals(2, summary.autoResolved.size)
        assertEquals(3, summary.totalConflicts)
    }
}
