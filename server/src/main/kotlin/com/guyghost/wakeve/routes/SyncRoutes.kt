package com.guyghost.wakeve.routes

import com.guyghost.wakeve.models.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Routes de synchronisation offline-first
 */
fun Route.syncRoutes() {
    val json = Json { ignoreUnknownKeys = true }
    
    // In-memory storage for sync tracking (à remplacer par une vraie BD)
    val changes = mutableMapOf<String, SyncChange>()
    val conflicts = mutableMapOf<String, SyncConflict>()
    val deviceMetadata = mutableMapOf<String, SyncMetadata>()
    
    route("/sync") {
        // POST /api/sync - Synchroniser les changements
        post {
            try {
                val request = call.receive<SyncRequest>()
                
                // Valider l'utilisateur
                val userId = request.userId
                
                // Traiter les changements du client
                val syncedChanges = mutableListOf<SyncedChange>()
                val detectedConflicts = mutableListOf<SyncConflict>()
                
                request.changes.forEach { change ->
                    // Vérifier les conflits
                    val hasConflict = detectConflict(change, changes)
                    
                    if (hasConflict) {
                        // Créer un enregistrement de conflit
                        val conflict = SyncConflict(
                            id = "conflict-${Random.nextLong(1000000)}",
                            changeId = change.id,
                            entityType = change.entityType,
                            entityId = change.entityId,
                            conflictType = ConflictType.CONCURRENT_UPDATE,
                            localVersion = change.data,
                            remoteVersion = "", // À récupérer de la BD
                            timestamp = getCurrentTimeMillis(),
                            resolved = false
                        )
                        conflicts[conflict.id] = conflict
                        detectedConflicts.add(conflict)
                        
                        syncedChanges.add(
                            SyncedChange(
                                changeId = change.id,
                                status = SyncStatus.CONFLICT,
                                serverTimestamp = getCurrentTimeMillis()
                            )
                        )
                    } else {
                        // Appliquer le changement
                        val updatedChange = change.copy(
                            status = SyncStatus.SUCCESS,
                            serverTimestamp = getCurrentTimeMillis()
                        )
                        changes[change.id] = updatedChange
                        
                        // Générer un nouvel ID si création
                        val newEntityId = if (change.operation == SyncOperation.CREATE) {
                            "${change.entityType}-${Random.nextLong(1000000)}"
                        } else {
                            null
                        }
                        
                        syncedChanges.add(
                            SyncedChange(
                                changeId = change.id,
                                status = SyncStatus.SUCCESS,
                                serverTimestamp = getCurrentTimeMillis(),
                                serverEntityId = newEntityId
                            )
                        )
                    }
                }
                
                // Récupérer les changements du serveur depuis le dernier sync
                val serverChanges = getServerChanges(userId, request.lastSyncTimestamp)
                
                // Mettre à jour les métadonnées
                val now = getCurrentTimeMillis()
                val metadata = deviceMetadata.getOrPut(request.deviceId) {
                    SyncMetadata(
                        deviceId = request.deviceId,
                        lastSyncTimestamp = null,
                        lastSyncCheckTimestamp = null,
                        pendingChangesCount = 0,
                        totalSyncedChanges = 0,
                        syncErrors = 0
                    )
                }
                
                deviceMetadata[request.deviceId] = metadata.copy(
                    lastSyncTimestamp = now,
                    lastSyncCheckTimestamp = now,
                    totalSyncedChanges = metadata.totalSyncedChanges + syncedChanges.size
                )
                
                // Créer la réponse
                val response = SyncResponse(
                    success = detectedConflicts.isEmpty(),
                    syncedChanges = syncedChanges,
                    conflicts = detectedConflicts,
                    serverChanges = serverChanges,
                    newTimestamp = now,
                    requiresFullSync = false
                )
                
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to (e.message ?: "Sync failed"))
                )
            }
        }
        
        // GET /api/sync/status - Obtenir le statut de sync
        get("/status") {
            try {
                val deviceId = call.request.queryParameters["deviceId"] ?: ""
                val metadata = deviceMetadata[deviceId]
                
                if (metadata != null) {
                    call.respond(HttpStatusCode.OK, metadata)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Device not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }
        
        // POST /api/sync/resolve-conflict - Résoudre un conflit
        post("/resolve-conflict") {
            try {
                val conflictData = call.receive<Map<String, String>>()
                val conflictId = conflictData["conflictId"] ?: ""
                val strategy = conflictData["strategy"] ?: "LAST_WRITE_WINS"
                
                val conflict = conflicts[conflictId]
                if (conflict != null) {
                    // Appliquer la résolution
                    val resolved = conflict.copy(
                        resolved = true,
                        resolvedAt = getCurrentTimeMillis(),
                        resolution = ConflictResolution(
                            strategy = ResolutionStrategy.valueOf(strategy),
                            selectedVersion = conflict.remoteVersion,
                            timestamp = getCurrentTimeMillis()
                        )
                    )
                    conflicts[conflictId] = resolved
                    
                    call.respond(HttpStatusCode.OK, mapOf("success" to true))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Conflict not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }
    }
}

/**
 * Détecte les conflits potentiels
 */
private fun detectConflict(
    change: SyncChange,
    existingChanges: Map<String, SyncChange>
): Boolean {
    // Vérifier s'il existe un changement concurrent pour la même entité
    return existingChanges.values.any { existing ->
        existing.entityType == change.entityType &&
        existing.entityId == change.entityId &&
        existing.timestamp != change.timestamp &&
        existing.status != SyncStatus.SUCCESS
    }
}

/**
 * Récupère les changements du serveur depuis le dernier sync
 */
private fun getServerChanges(userId: String, lastSyncTimestamp: Long): List<ServerChange> {
    // Simuler quelques changements du serveur
    return if (Random.nextDouble() > 0.7) {
        listOf(
            ServerChange(
                id = "server-change-${Random.nextLong(1000000)}",
                entityType = "event",
                entityId = "event-1",
                operation = SyncOperation.UPDATE,
                data = "{\"status\": \"POLLING\"}",
                timestamp = getCurrentTimeMillis(),
                userId = "user-2"
            )
        )
    } else {
        emptyList()
    }
}

/**
 * Obtient le timestamp actuel
 */
private fun getCurrentTimeMillis(): Long {
    return System.currentTimeMillis()
}
