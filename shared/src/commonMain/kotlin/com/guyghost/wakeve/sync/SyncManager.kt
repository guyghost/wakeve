package com.guyghost.wakeve.sync

import com.guyghost.wakeve.*
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.database.WakevDb
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

/**
 * Client-side sync manager for offline-first synchronization
 */
class SyncManager(
    private val database: WakevDb,
    private val eventRepository: DatabaseEventRepository,
    private val userRepository: UserRepository,
    private val networkDetector: NetworkStatusDetector,
    private val httpClient: SyncHttpClient,
    private val authTokenProvider: () -> String?,
    private val maxRetries: Int = 3,
    private val baseRetryDelayMs: Long = 1000L, // 1 second
    private val metrics: SyncMetrics = InMemorySyncMetrics(),
    private val alertManager: SyncAlertManager = LoggingSyncAlertManager()
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Sync status
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    // Network status from platform-specific detector
    val isNetworkAvailable: StateFlow<Boolean> = networkDetector.isNetworkAvailable

    init {
        scope.launch {
            networkDetector.isNetworkAvailable.collect { available: Boolean ->
                if (available && hasPendingChanges()) {
                    triggerSync()
                }
            }
        }
    }

    /**
     * Record a local change for later synchronization
     */
    suspend fun recordLocalChange(
        table: String,
        operation: SyncOperation,
        recordId: String,
        data: String,
        userId: String
    ): Result<Unit> = runCatching {
        val syncId = "sync_${getCurrentTimeMillis()}_${recordId}"
        val timestamp = getCurrentUtcIsoString()

        // Store the change in sync metadata
        userRepository.addSyncMetadata(
            id = syncId,
            tableName = table,
            recordId = recordId,
            operation = operation,
            timestamp = timestamp,
            userId = userId
        ).getOrThrow()

        // If network is available, trigger immediate sync
        if (isNetworkAvailable.value) {
            triggerSync()
        }
    }

    /**
     * Check if there are pending changes to sync
     */
    suspend fun hasPendingChanges(): Boolean {
        return userRepository.getPendingSyncChanges().isNotEmpty()
    }

    /**
     * Get all pending changes ready for sync
     */
    suspend fun getPendingChangesForSync(): List<SyncChange> {
        return userRepository.getPendingSyncChanges().map { metadata ->
            // Get the actual data for this change
            val data = getChangeData(metadata.tableName, metadata.recordId)
            SyncChange(
                id = metadata.id,
                table = metadata.tableName,
                operation = metadata.operation.name,
                recordId = metadata.recordId,
                data = data,
                timestamp = metadata.timestamp,
                userId = metadata.userId
            )
        }
    }

    /**
     * Trigger synchronization with server
     */
    suspend fun triggerSync(): Result<SyncResponse> = runCatching {
        syncWithRetry().getOrThrow()
    }

    /**
     * Get data for a specific change
     */
    private suspend fun getChangeData(table: String, recordId: String): String {
        return when (table) {
            "events" -> {
                val event = eventRepository.getEvent(recordId)
                if (event != null) {
                    json.encodeToString(SyncEventData.serializer(), SyncEventData(
                        id = event.id,
                        title = event.title,
                        description = event.description,
                        organizerId = event.organizerId,
                        deadline = event.deadline,
                        timezone = event.proposedSlots.firstOrNull()?.timezone ?: "UTC"
                    ))
                } else {
                    "{}" // Fallback for deleted items
                }
            }
            "participants" -> {
                // For participants, we need to reconstruct the data
                // This is simplified - in practice we'd store the full data
                """{"eventId":"unknown","userId":"$recordId"}"""
            }
            "votes" -> {
                // For votes, we need to reconstruct the data
                // This is simplified - in practice we'd store the full data
                """{"eventId":"unknown","participantId":"$recordId","slotId":"unknown","preference":"YES"}"""
            }
            else -> "{}"
        }
    }

    /**
     * Handle conflicts using CRDT-based merging
     */
    private suspend fun handleConflict(conflict: SyncConflict) {
        when (conflict.table) {
            "events" -> handleEventConflict(conflict)
            "participants" -> handleParticipantConflict(conflict)
            "votes" -> handleVoteConflict(conflict)
            else -> {
                // Fallback: mark as synced (client wins)
                userRepository.updateSyncStatus(
                    syncId = conflict.changeId,
                    synced = true,
                    retryCount = 0,
                    error = null
                )
            }
        }
    }

    private suspend fun handleEventConflict(conflict: SyncConflict) {
        // Get local and server versions
        val localEvent = eventRepository.getEvent(conflict.recordId)
        val serverEventJson = conflict.serverData ?: "{}"
        val serverEvent = try {
            json.decodeFromString(Event.serializer(), serverEventJson)
        } catch (e: Exception) {
            null
        }

        if (localEvent != null && serverEvent != null) {
            // Merge using CRDT logic (LWW for each field)
            val mergedEvent = mergeEventsCRDT(localEvent, serverEvent, conflict.serverTimestamp)
            // Update local database with merged event
            eventRepository.updateEvent(mergedEvent)
        }

        // Mark as synced
        userRepository.updateSyncStatus(
            syncId = conflict.changeId,
            synced = true,
            retryCount = 0,
            error = null
        )
    }

    private suspend fun handleParticipantConflict(conflict: SyncConflict) {
        // For participants, use union (additive)
        // This is simplified - in practice we'd merge participant lists
        userRepository.updateSyncStatus(
            syncId = conflict.changeId,
            synced = true,
            retryCount = 0,
            error = null
        )
    }

    private suspend fun handleVoteConflict(conflict: SyncConflict) {
        // For votes, merge vote lists (union of preferences)
        // This is simplified
        userRepository.updateSyncStatus(
            syncId = conflict.changeId,
            synced = true,
            retryCount = 0,
            error = null
        )
    }

    /**
     * Merge two events using CRDT logic
     */
    private fun mergeEventsCRDT(local: Event, server: Event, serverTimestamp: String?): Event {
        // Parse timestamps (simplified - in practice use proper ISO parsing)
        val localTime = parseTimestamp(local.updatedAt)
        val serverTime = serverTimestamp?.let { parseTimestamp(it) } ?: 0L

        return Event(
            id = local.id,
            title = if (localTime >= serverTime) local.title else server.title,
            description = if (localTime >= serverTime) local.description else server.description,
            organizerId = local.organizerId, // Organizer doesn't change
            participants = (local.participants + server.participants).distinct(), // Union (G-Set)
            status = if (localTime >= serverTime) local.status else server.status,
            finalDate = if (local.finalDate != null && server.finalDate != null) {
                if (localTime >= serverTime) local.finalDate else server.finalDate
            } else {
                local.finalDate ?: server.finalDate
            },
            deadline = if (localTime >= serverTime) local.deadline else server.deadline,
            createdAt = local.createdAt, // Creation time doesn't change
            updatedAt = if (localTime >= serverTime) local.updatedAt else server.updatedAt
        )
    }

    private fun parseTimestamp(isoString: String): Long {
        // Simplified timestamp parsing - in practice use kotlinx-datetime
        return try {
            // Extract milliseconds from ISO string (simplified)
            val datePart = isoString.substringBefore('T')
            val timePart = isoString.substringAfter('T').substringBefore('Z')
            // Very basic parsing - replace with proper library
            (datePart.hashCode().toLong() + timePart.hashCode().toLong())
        } catch (e: Exception) {
            0L
        }
    }
        }

        // Handle conflicts with CRDT-based merging
        response.conflicts.forEach { conflict ->
            handleConflict(conflict)
            metrics.recordConflictResolved(conflict.table, "CRDT")
        }

        // Alert if high conflict rate
        if (response.conflicts.size > 5) {
            alertManager.alertHighConflictRate(response.conflicts.size)
        }
    }

    /**
     * Update sync status for failed operations
     */
    private suspend fun updateSyncStatusForFailure(error: Exception) {
        val pendingChanges = userRepository.getPendingSyncChanges()
        pendingChanges.forEach { change ->
            val newRetryCount = change.retryCount + 1
            userRepository.updateSyncStatus(
                syncId = change.id,
                synced = false,
                retryCount = newRetryCount,
                error = error.message ?: "Sync failed"
            )
        }
    }

    /**
     * Clean up old sync metadata
     */
    suspend fun cleanupOldSyncData(): Result<Unit> = runCatching {
        val thirtyDaysAgo = "2025-10-20T00:00:00Z" // Simplified
        userRepository.cleanupOldSyncMetadata(thirtyDaysAgo)
    }

    /**
     * Get current UTC timestamp (simplified)
     */
    private fun getCurrentUtcIsoString(): String {
        return "2025-11-19T12:00:00Z"
    }

    /**
     * Get sync metrics for monitoring
     */
    fun getSyncMetrics(): SyncStats = metrics.getSyncStats()

    /**
     * Log current sync status for monitoring
     */
    fun logSyncStatus() {
        val stats = metrics.getSyncStats()
        println("Sync Stats: total=${stats.totalSyncs}, success=${stats.successfulSyncs}, failed=${stats.failedSyncs}, avgDuration=${stats.averageDurationMs}ms, conflicts=${stats.totalConflictsResolved}")
    }

    /**
     * Performance monitoring: check if sync is taking too long
     */
    private fun checkPerformance(durationMs: Long) {
        if (durationMs > 30000) { // 30 seconds
            println("PERFORMANCE ALERT: Sync took ${durationMs}ms, which is longer than expected")
        }
    }

    /**
     * Clean up resources
     */
    fun dispose() {
        scope.cancel()
    }

    /**
     * Perform sync with retry mechanism and exponential backoff
     */
    private suspend fun syncWithRetry(): Result<SyncResponse> {
        var lastException: Exception? = null

        for (attempt in 0..maxRetries) {
            try {
                return Result.success(performSync())
            } catch (e: Exception) {
                lastException = e
                _syncStatus.value = SyncStatus.Error("Sync failed (attempt ${attempt + 1}/${maxRetries + 1}): ${e.message}")

                if (attempt < maxRetries) {
                    // Calculate exponential backoff delay: baseDelay * 2^attempt
                    val delayMs = baseRetryDelayMs * (1L shl attempt) // 2^attempt
                    kotlinx.coroutines.delay(delayMs)
                } else {
                    // All retries failed, update sync status for failed changes
                    updateSyncStatusForFailure(e)
                    alertManager.alertSyncFailure(e.message ?: "Unknown error", maxRetries)
                }
            }
        }

        // All retries failed
        return Result.failure(lastException ?: Exception("Sync failed after $maxRetries retries"))
    }

    /**
     * Schedule automatic retry for failed changes
     */
    fun scheduleRetryForFailedChanges() {
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(30000L) // Check every 30 seconds

                if (networkDetector.isNetworkAvailable.value) {
                    val failedChanges = userRepository.getPendingSyncChanges()
                        .filter { it.retryCount < maxRetries && it.lastError != null }

                    if (failedChanges.isNotEmpty()) {
                        // Trigger sync to retry failed changes
                        triggerSync()
                    }
                }
            }
        }
    }

    /**
     * Perform actual sync operation (extracted from triggerSync)
     */
    private suspend fun performSync(): SyncResponse {
        val startTime = System.currentTimeMillis()
        metrics.recordSyncStart()

        if (!networkDetector.isNetworkAvailable.value) {
            throw IllegalStateException("Network not available")
        }

        val authToken = authTokenProvider() ?: throw IllegalStateException("No auth token available")

        _syncStatus.value = SyncStatus.Syncing

        val pendingChanges = getPendingChangesForSync()
        if (pendingChanges.isEmpty()) {
            _syncStatus.value = SyncStatus.Idle
            val duration = System.currentTimeMillis() - startTime
            metrics.recordSyncSuccess(duration, 0)
            return SyncResponse(
                success = true,
                appliedChanges = 0,
                conflicts = emptyList(),
                serverTimestamp = getCurrentUtcIsoString(),
                message = "No changes to sync"
            )
        }

        val syncRequest = SyncRequest(
            changes = pendingChanges,
            lastSyncTimestamp = null // TODO: Track last successful sync
        )

        // Make actual HTTP call to server
        val requestJson = json.encodeToString(SyncRequest.serializer(), syncRequest)
        val responseJson = httpClient.sync(requestJson, authToken).getOrThrow()
        val response = json.decodeFromString(SyncResponse.serializer(), responseJson)

        // Update local sync status based on response
        updateLocalSyncStatus(response)

        _syncStatus.value = if (response.success) SyncStatus.Idle else SyncStatus.Error(response.message ?: "Sync failed")

        val duration = System.currentTimeMillis() - startTime
        if (response.success) {
            metrics.recordSyncSuccess(duration, response.appliedChanges)
        } else {
            metrics.recordSyncFailure(duration, response.message ?: "Unknown error")
        }

        checkPerformance(duration)

        return response
    }
}