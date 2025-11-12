package com.guyghost.wakeve

import com.guyghost.wakeve.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository pour la gestion de la synchronisation offline-first
 * Gère les changements locaux, les conflits et la synchronisation avec le serveur
 */
class SyncRepository(
    private val eventRepository: EventRepository,
    private val httpClient: HttpClient,
    private val localDatabase: SyncDatabase,
    private val syncMetadataStorage: SyncMetadataStorage,
    private val networkMonitor: NetworkMonitor,
    private val baseUrl: String = "http://localhost:8080",
    private val retryPolicy: SyncRetryPolicy = SyncRetryPolicy()
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var syncListeners = mutableListOf<(SyncEvent) -> Unit>()
    
    /**
     * Enregistre un listener pour les événements de sync
     */
    fun addSyncListener(listener: (SyncEvent) -> Unit) {
        syncListeners.add(listener)
    }
    
    /**
     * Enregistre un changement (opération offline-first)
     */
    suspend fun recordChange(
        userId: String,
        deviceId: String,
        entityType: String,
        entityId: String,
        operation: SyncOperation,
        data: Any
    ): Result<SyncChange> = try {
        val changeId = "change-${generateId()}"
        val dataJson = json.encodeToString(data)
        
        val change = SyncChange(
            id = changeId,
            userId = userId,
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            data = dataJson,
            timestamp = getCurrentTimeMillis(),
            deviceId = deviceId,
            status = SyncStatus.PENDING
        )
        
        // Sauvegarder le changement localement
        localDatabase.saveChange(change)
        
        notifyListeners(SyncEvent.SyncProgress(1, 1))
        
        Result.success(change)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Synchronise les changements en attente avec le serveur
     */
    suspend fun syncChanges(
        userId: String,
        deviceId: String,
        accessToken: String
    ): Result<SyncResponse> = try {
        notifyListeners(SyncEvent.SyncStarted(getCurrentTimeMillis()))
        
        // Récupérer les changements en attente
        val pendingChanges = localDatabase.getPendingChanges(userId)
        
        if (pendingChanges.isEmpty()) {
            val metadata = syncMetadataStorage.getMetadata(deviceId)
            return Result.success(
                SyncResponse(
                    success = true,
                    syncedChanges = emptyList(),
                    conflicts = emptyList(),
                    serverChanges = emptyList(),
                    newTimestamp = getCurrentTimeMillis()
                )
            )
        }
        
        val lastSyncTimestamp = syncMetadataStorage.getLastSyncTimestamp(deviceId) ?: 0L
        
        // Créer la requête de sync
        val syncRequest = SyncRequest(
            userId = userId,
            deviceId = deviceId,
            lastSyncTimestamp = lastSyncTimestamp,
            changes = pendingChanges
        )
        
        val requestBody = json.encodeToString(syncRequest)
        
        // Envoyer la requête au serveur
        val response = httpClient.post(
            url = "$baseUrl/api/sync",
            body = requestBody,
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
                "Content-Type" to "application/json"
            )
        )
        
        if (response.statusCode != 200) {
            return Result.failure(
                Exception("Sync failed with status ${response.statusCode}")
            )
        }
        
        val syncResponse = json.decodeFromString<SyncResponse>(response.body)
        
        // Traiter les réponses du serveur
        processSyncResponse(syncResponse, userId, deviceId)
        
        notifyListeners(
            SyncEvent.SyncCompleted(
                getCurrentTimeMillis(),
                syncResponse.syncedChanges.size
            )
        )
        
        Result.success(syncResponse)
    } catch (e: Exception) {
        notifyListeners(
            SyncEvent.SyncFailed(e.message ?: "Unknown error", getCurrentTimeMillis())
        )
        Result.failure(e)
    }
    
    /**
     * Résout un conflit de synchronisation
     */
    suspend fun resolveConflict(
        conflictId: String,
        strategy: ResolutionStrategy,
        userId: String
    ): Result<Unit> = try {
        val conflict = localDatabase.getConflict(conflictId)
            ?: return Result.failure(Exception("Conflict not found"))
        
        val selectedVersion = when (strategy) {
            ResolutionStrategy.LAST_WRITE_WINS -> {
                // Comparer les timestamps et utiliser le plus récent
                if (conflict.timestamp > (conflict.serverTimestamp ?: 0L)) {
                    conflict.localVersion
                } else {
                    conflict.remoteVersion
                }
            }
            ResolutionStrategy.CLIENT_WINS -> conflict.localVersion
            ResolutionStrategy.SERVER_WINS -> conflict.remoteVersion
            ResolutionStrategy.MANUAL -> {
                // Nécessite une intervention utilisateur
                return Result.failure(Exception("Manual resolution required"))
            }
        }
        
        // Sauvegarder la résolution
        val resolution = ConflictResolution(
            strategy = strategy,
            selectedVersion = selectedVersion,
            timestamp = getCurrentTimeMillis()
        )
        
        localDatabase.resolveConflict(conflictId, resolution)
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Récupère l'état courant de la synchronisation
     */
    suspend fun getSyncState(deviceId: String): Result<SyncState> = try {
        val isOnline = networkMonitor.isNetworkAvailable()
        val pendingChanges = localDatabase.getPendingChangesCount()
        val failedChanges = localDatabase.getFailedChangesCount()
        val conflicts = localDatabase.getUnresolvedConflictsCount()
        val lastSyncTimestamp = syncMetadataStorage.getLastSyncTimestamp(deviceId)
        
        Result.success(
            SyncState(
                isOnline = isOnline,
                isSyncing = false,
                lastSyncTimestamp = lastSyncTimestamp,
                pendingChangesCount = pendingChanges,
                failedChangesCount = failedChanges,
                conflictsCount = conflicts
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Récupère tous les conflits non résolus
     */
    suspend fun getUnresolvedConflicts(): Result<List<SyncConflict>> = try {
        val conflicts = localDatabase.getUnresolvedConflicts()
        Result.success(conflicts)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Retire les changements qui ont été synchronisés
     */
    suspend fun clearSyncedChanges(changeIds: List<String>): Result<Unit> = try {
        localDatabase.markChangesAsSynced(changeIds)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    // MARK: - Private Methods
    
    /**
     * Traite la réponse de synchronisation du serveur
     */
    private suspend fun processSyncResponse(
        response: SyncResponse,
        userId: String,
        deviceId: String
    ) {
        // Marquer les changements comme synchronisés
        response.syncedChanges.forEach { synced ->
            localDatabase.markChangesAsSynced(listOf(synced.changeId))
            synced.serverEntityId?.let { newId ->
                localDatabase.updateEntityId(synced.changeId, newId)
            }
        }
        
        // Traiter les conflits
        response.conflicts.forEach { conflict ->
            localDatabase.saveConflict(conflict)
            notifyListeners(SyncEvent.ConflictDetected(conflict))
        }
        
        // Appliquer les changements du serveur
        response.serverChanges.forEach { serverChange ->
            applyServerChange(serverChange, userId, deviceId)
        }
        
        // Mettre à jour les métadonnées
        syncMetadataStorage.updateLastSyncTimestamp(deviceId, response.newTimestamp)
        
        if (response.requiresFullSync) {
            // Forcer une synchronisation complète
            localDatabase.markAllAsNeedingSync()
        }
    }
    
    /**
     * Applique un changement reçu du serveur
     */
    private suspend fun applyServerChange(
        change: ServerChange,
        userId: String,
        deviceId: String
    ) {
        try {
            val data = json.parseToJsonElement(change.data)
            
            when (change.entityType) {
                "event" -> {
                    // Appliquer le changement d'événement
                    when (change.operation) {
                        SyncOperation.CREATE, SyncOperation.UPDATE -> {
                            // Mettre à jour localement
                        }
                        SyncOperation.DELETE -> {
                            // Supprimer localement
                        }
                    }
                }
                "participant" -> {
                    // Appliquer le changement de participant
                }
                "vote" -> {
                    // Appliquer le changement de vote
                }
                else -> {
                    // Type d'entité inconnu
                }
            }
        } catch (e: Exception) {
            // Log l'erreur mais continue
        }
    }
    
    private fun notifyListeners(event: SyncEvent) {
        syncListeners.forEach { it(event) }
    }
}

/**
 * Interface pour la base de données locale
 */
interface SyncDatabase {
    suspend fun saveChange(change: SyncChange)
    suspend fun getPendingChanges(userId: String): List<SyncChange>
    suspend fun getPendingChangesCount(): Int
    suspend fun getFailedChangesCount(): Int
    suspend fun markChangesAsSynced(changeIds: List<String>)
    suspend fun updateEntityId(changeId: String, newId: String)
    suspend fun saveConflict(conflict: SyncConflict)
    suspend fun getConflict(conflictId: String): SyncConflict?
    suspend fun getUnresolvedConflicts(): List<SyncConflict>
    suspend fun getUnresolvedConflictsCount(): Int
    suspend fun resolveConflict(conflictId: String, resolution: ConflictResolution)
    suspend fun markAllAsNeedingSync()
}

/**
 * Interface pour le stockage des métadonnées
 */
interface SyncMetadataStorage {
    suspend fun getMetadata(deviceId: String): SyncMetadata?
    suspend fun saveMetadata(metadata: SyncMetadata)
    suspend fun getLastSyncTimestamp(deviceId: String): Long?
    suspend fun updateLastSyncTimestamp(deviceId: String, timestamp: Long)
}

/**
 * Interface pour le monitoring réseau
 */
interface NetworkMonitor {
    fun isNetworkAvailable(): Boolean
    fun addNetworkListener(listener: (Boolean) -> Unit)
    fun removeNetworkListener(listener: (Boolean) -> Unit)
}

/**
 * Générateur d'ID
 */
private fun generateId(): String {
    return (0..15).map {
        "0123456789abcdef"[kotlin.random.Random.nextInt(16)]
    }.joinToString("")
}
