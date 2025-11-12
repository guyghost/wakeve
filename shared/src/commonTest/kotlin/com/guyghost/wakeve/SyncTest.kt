package com.guyghost.wakeve

import com.guyghost.wakeve.models.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests pour la synchronisation
 */
class SyncTest {
    
    @Test
    fun testSyncChangeCreation() {
        val change = SyncChange(
            id = "change-1",
            userId = "user-1",
            entityType = "event",
            entityId = "event-1",
            operation = SyncOperation.CREATE,
            data = "{\"title\": \"Test Event\"}",
            timestamp = getCurrentTimeMillis(),
            deviceId = "device-1",
            status = SyncStatus.PENDING
        )
        
        assertEquals("change-1", change.id)
        assertEquals(SyncStatus.PENDING, change.status)
        assertEquals(SyncOperation.CREATE, change.operation)
    }
    
    @Test
    fun testConflictDetection() {
        val localVersion = "{\"status\": \"DRAFT\"}"
        val remoteVersion = "{\"status\": \"POLLING\"}"
        
        val conflict = SyncConflict(
            id = "conflict-1",
            changeId = "change-1",
            entityType = "event",
            entityId = "event-1",
            conflictType = ConflictType.CONCURRENT_UPDATE,
            localVersion = localVersion,
            remoteVersion = remoteVersion,
            timestamp = getCurrentTimeMillis(),
            resolved = false
        )
        
        assertEquals(ConflictType.CONCURRENT_UPDATE, conflict.conflictType)
        assertTrue(!conflict.resolved)
        assertEquals(localVersion, conflict.localVersion)
        assertEquals(remoteVersion, conflict.remoteVersion)
    }
    
    @Test
    fun testConflictResolution() {
        val conflict = SyncConflict(
            id = "conflict-1",
            changeId = "change-1",
            entityType = "event",
            entityId = "event-1",
            conflictType = ConflictType.CONCURRENT_UPDATE,
            localVersion = "{\"status\": \"DRAFT\"}",
            remoteVersion = "{\"status\": \"POLLING\"}",
            timestamp = getCurrentTimeMillis(),
            resolved = false
        )
        
        val resolution = ConflictResolution(
            strategy = ResolutionStrategy.LAST_WRITE_WINS,
            selectedVersion = conflict.remoteVersion,
            timestamp = getCurrentTimeMillis()
        )
        
        val resolved = conflict.copy(
            resolved = true,
            resolution = resolution,
            resolvedAt = getCurrentTimeMillis()
        )
        
        assertTrue(resolved.resolved)
        assertNotNull(resolved.resolution)
        assertEquals(ResolutionStrategy.LAST_WRITE_WINS, resolved.resolution?.strategy)
    }
    
    @Test
    fun testSyncRetryPolicy() {
        val policy = SyncRetryPolicy(
            maxRetries = 3,
            initialDelayMs = 1000,
            maxDelayMs = 60000,
            backoffMultiplier = 2.0
        )
        
        val delay0 = policy.getDelayForAttempt(0)
        val delay1 = policy.getDelayForAttempt(1)
        val delay2 = policy.getDelayForAttempt(2)
        
        assertEquals(1000L, delay0)
        assertEquals(2000L, delay1)
        assertEquals(4000L, delay2)
    }
    
    @Test
    fun testSyncRetryPolicyMaxDelay() {
        val policy = SyncRetryPolicy(
            maxRetries = 10,
            initialDelayMs = 1000,
            maxDelayMs = 30000,
            backoffMultiplier = 2.0
        )
        
        val delayMax = policy.getDelayForAttempt(10)
        assertTrue(delayMax <= 30000L)
    }
    
    @Test
    fun testSyncStateTracking() {
        val state = SyncState(
            isOnline = true,
            isSyncing = false,
            lastSyncTimestamp = getCurrentTimeMillis() - 60000,
            pendingChangesCount = 3,
            failedChangesCount = 1,
            conflictsCount = 0
        )
        
        assertTrue(state.isOnline)
        assertTrue(!state.isSyncing)
        assertEquals(3, state.pendingChangesCount)
        assertEquals(1, state.failedChangesCount)
    }
    
    @Test
    fun testSyncMetadata() {
        val metadata = SyncMetadata(
            deviceId = "device-1",
            lastSyncTimestamp = getCurrentTimeMillis(),
            lastSyncCheckTimestamp = getCurrentTimeMillis(),
            pendingChangesCount = 2,
            totalSyncedChanges = 10,
            syncErrors = 0
        )
        
        assertEquals("device-1", metadata.deviceId)
        assertEquals(10, metadata.totalSyncedChanges)
    }
    
    @Test
    fun testSyncRequest() {
        val change = SyncChange(
            id = "change-1",
            userId = "user-1",
            entityType = "event",
            entityId = "event-1",
            operation = SyncOperation.CREATE,
            data = "{}",
            timestamp = getCurrentTimeMillis(),
            deviceId = "device-1"
        )
        
        val request = SyncRequest(
            userId = "user-1",
            deviceId = "device-1",
            lastSyncTimestamp = 0L,
            changes = listOf(change)
        )
        
        assertEquals(1, request.changes.size)
        assertEquals("change-1", request.changes[0].id)
    }
}
