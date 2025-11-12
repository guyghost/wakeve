package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Types d'opération de synchronisation
 */
enum class SyncOperation {
    CREATE,    // Créer une nouvelle ressource
    UPDATE,    // Mettre à jour une ressource existante
    DELETE     // Supprimer une ressource
}

/**
 * Statut de synchronisation d'une opération
 */
enum class SyncStatus {
    PENDING,    // En attente de synchronisation
    IN_PROGRESS, // Synchronisation en cours
    SUCCESS,    // Synchronisation réussie
    FAILED,     // Synchronisation échouée
    CONFLICT    // Conflit détecté
}

/**
 * Changement en attente de synchronisation
 */
@Serializable
data class SyncChange(
    val id: String,                    // ID unique du changement
    val userId: String,                // ID de l'utilisateur qui a effectué le changement
    val entityType: String,            // Type d'entité (event, participant, vote, etc.)
    val entityId: String,              // ID de l'entité affectée
    val operation: SyncOperation,      // Type d'opération
    val data: String,                  // Données sérialisées (JSON)
    val timestamp: Long,               // Timestamp du changement (client)
    val deviceId: String,              // ID du device qui a créé le changement
    val status: SyncStatus = SyncStatus.PENDING,
    val errorMessage: String? = null,  // Message d'erreur si échec
    val retryCount: Int = 0,          // Nombre de tentatives
    val syncedAt: Long? = null,       // Timestamp de synchronisation réussie
    val serverTimestamp: Long? = null // Timestamp du serveur après synchronisation
)

/**
 * Conflit de synchronisation
 */
@Serializable
data class SyncConflict(
    val id: String,
    val changeId: String,              // ID du changement qui a causé le conflit
    val entityType: String,
    val entityId: String,
    val conflictType: ConflictType,
    val localVersion: String,          // Version locale (JSON)
    val remoteVersion: String,         // Version serveur (JSON)
    val timestamp: Long,
    val resolved: Boolean = false,
    val resolvedAt: Long? = null,
    val resolution: ConflictResolution? = null
)

/**
 * Types de conflits
 */
enum class ConflictType {
    CONCURRENT_UPDATE,  // Deux mises à jour concurrentes
    DELETE_UPDATE,      // Suppression vs mise à jour
    CREATE_EXISTS,      // Création alors qu'existe déjà
    VERSION_MISMATCH    // Désynchronisation de version
}

/**
 * Résolution de conflit (dernière écriture gagne par défaut)
 */
@Serializable
data class ConflictResolution(
    val strategy: ResolutionStrategy,
    val selectedVersion: String,       // Version choisie (local ou remote)
    val timestamp: Long
)

/**
 * Stratégies de résolution
 */
enum class ResolutionStrategy {
    LAST_WRITE_WINS,    // Dernière écriture remporte (défaut)
    CLIENT_WINS,        // Version client choisie
    SERVER_WINS,        // Version serveur choisie
    MANUAL              // Résolution manuelle utilisateur
}

/**
 * Requête de synchronisation
 */
@Serializable
data class SyncRequest(
    val userId: String,
    val deviceId: String,
    val lastSyncTimestamp: Long,       // Dernière synchronisation réussie
    val changes: List<SyncChange>,     // Changements à synchroniser
    val clientVersion: String = "1.0"  // Version du client
)

/**
 * Réponse de synchronisation
 */
@Serializable
data class SyncResponse(
    val success: Boolean,
    val syncedChanges: List<SyncedChange>,  // Changements synchronisés avec timestamps serveur
    val conflicts: List<SyncConflict>,      // Conflits détectés
    val serverChanges: List<ServerChange>,  // Changements du serveur (pull)
    val newTimestamp: Long,                 // Nouveau timestamp de synchronisation
    val requiresFullSync: Boolean = false   // Si vrai, client doit faire une sync complète
)

/**
 * Changement qui a été synchronisé
 */
@Serializable
data class SyncedChange(
    val changeId: String,
    val status: SyncStatus,
    val serverTimestamp: Long,
    val serverEntityId: String? = null // ID serveur assigné (pour CREATE)
)

/**
 * Changement du serveur (à appliquer client)
 */
@Serializable
data class ServerChange(
    val id: String,
    val entityType: String,
    val entityId: String,
    val operation: SyncOperation,
    val data: String,
    val timestamp: Long,
    val userId: String               // Qui a fait ce changement sur le serveur
)

/**
 * État de la synchronisation
 */
@Serializable
data class SyncState(
    val isOnline: Boolean,
    val isSyncing: Boolean,
    val lastSyncTimestamp: Long?,
    val pendingChangesCount: Int,
    val failedChangesCount: Int,
    val conflictsCount: Int,
    val lastSyncError: String? = null
)

/**
 * Métadonnées de synchronisation pour traçage
 */
@Serializable
data class SyncMetadata(
    val deviceId: String,              // Identifiant unique du device
    val lastSyncTimestamp: Long?,      // Dernier sync réussi
    val lastSyncCheckTimestamp: Long?, // Dernier check (même sans changements)
    val pendingChangesCount: Int,
    val totalSyncedChanges: Long,      // Total historique
    val syncErrors: Int,
    val currentVersion: String = "1.0"
)

/**
 * Politique de retry pour les changements échoués
 */
data class SyncRetryPolicy(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000,   // 1 seconde
    val maxDelayMs: Long = 60000,      // 1 minute
    val backoffMultiplier: Double = 2.0
) {
    fun getDelayForAttempt(attemptNumber: Int): Long {
        val delay = initialDelayMs * Math.pow(backoffMultiplier, attemptNumber.toDouble()).toLong()
        return minOf(delay, maxDelayMs)
    }
}

/**
 * Événement de synchronisation pour l'observabilité
 */
sealed class SyncEvent {
    data class SyncStarted(val timestamp: Long) : SyncEvent()
    data class SyncProgress(val changesProcessed: Int, val totalChanges: Int) : SyncEvent()
    data class SyncCompleted(val timestamp: Long, val syncedCount: Int) : SyncEvent()
    data class SyncFailed(val error: String, val timestamp: Long) : SyncEvent()
    data class ConflictDetected(val conflict: SyncConflict) : SyncEvent()
    data class OfflineStatusChanged(val isOnline: Boolean) : SyncEvent()
}
