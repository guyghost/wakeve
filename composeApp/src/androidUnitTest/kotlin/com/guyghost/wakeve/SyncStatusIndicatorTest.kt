package com.guyghost.wakeve

import com.guyghost.wakeve.sync.SyncStatus
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SyncStatusIndicatorTest {
    @Test
    fun networkStatusLabelsUseLocalizedUserCopy() {
        assertEquals("En ligne", networkStatusLabel(isNetworkAvailable = true))
        assertEquals("Hors ligne", networkStatusLabel(isNetworkAvailable = false))
    }

    @Test
    fun syncStatusLabelsUseLocalizedUserCopy() {
        assertEquals("A jour", syncStatusLabel(SyncStatus.Idle))
        assertEquals("Synchronisation", syncStatusLabel(SyncStatus.Syncing))
        assertEquals("Erreur de synchronisation", syncStatusLabel(SyncStatus.Error("SECRET token")))
    }

    @Test
    fun syncStatusLabelsDoNotExposeTechnicalEnglishOrErrors() {
        listOf(
            networkStatusLabel(isNetworkAvailable = true),
            networkStatusLabel(isNetworkAvailable = false),
            syncStatusLabel(SyncStatus.Idle),
            syncStatusLabel(SyncStatus.Syncing),
            syncStatusLabel(SyncStatus.Error("SQL failed for secret@example.com")),
            pendingChangesLabel(hasPendingChanges = true).orEmpty()
        ).forEach { label ->
            listOf("Online", "Offline", "Idle", "Syncing", "Sync Error", "SECRET", "SQL failed").forEach {
                assertFalse(label.contains(it, ignoreCase = true), "Label should not contain `$it`: $label")
            }
        }
    }

    @Test
    fun pendingChangesLabelOnlyShowsWhenNeeded() {
        assertEquals("Modifications en attente", pendingChangesLabel(hasPendingChanges = true))
        assertNull(pendingChangesLabel(hasPendingChanges = false))
    }
}
