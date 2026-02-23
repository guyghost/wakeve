package com.guyghost.wakeve.sync

import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.UserRepository
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.SyncChange
import com.guyghost.wakeve.models.SyncConflict
import com.guyghost.wakeve.models.SyncEventData
import com.guyghost.wakeve.models.SyncOperation
import com.guyghost.wakeve.models.SyncParticipantData
import com.guyghost.wakeve.models.SyncRequest
import com.guyghost.wakeve.models.SyncResponse
import com.guyghost.wakeve.models.SyncVoteData
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

/**
 * Service de synchronisation serveur pour le traitement des changements offline
 */
class SyncService(private val db: WakeveDb) {

    private val eventRepository = DatabaseEventRepository(db)
    private val userRepository = UserRepository(db)
    private val json = Json { ignoreUnknownKeys = true }

    private val participantQueries = db.participantQueries
    private val voteQueries = db.voteQueries

    /**
     * Process a batch of sync changes from client
     */
    suspend fun processSyncChanges(request: SyncRequest, userId: String): SyncResponse {
        val conflicts = mutableListOf<SyncConflict>()
        var appliedChanges = 0

        try {
            for (change in request.changes) {
                // Verify the change belongs to the authenticated user
                if (change.userId != userId) {
                    conflicts.add(SyncConflict(
                        changeId = change.id,
                        table = change.table,
                        recordId = change.recordId,
                        clientData = change.data,
                        serverData = "",
                        resolution = "REJECTED"
                    ))
                    continue
                }

                val result = applySyncChange(change)
                if (result.isSuccess) {
                    appliedChanges++
                } else {
                    // Handle conflict
                    val serverData = getServerData(change.table, change.recordId)
                    conflicts.add(SyncConflict(
                        changeId = change.id,
                        table = change.table,
                        recordId = change.recordId,
                        clientData = change.data,
                        serverData = serverData ?: "",
                        resolution = "SERVER_WINS"  // Last-write-wins strategy
                    ))
                }
            }

            val serverTimestamp = getCurrentUtcIsoString()

            return SyncResponse(
                success = true,
                appliedChanges = appliedChanges,
                conflicts = conflicts,
                serverTimestamp = serverTimestamp,
                message = if (conflicts.isEmpty()) "All changes applied successfully" else "${conflicts.size} conflicts detected"
            )

        } catch (e: Exception) {
            return SyncResponse(
                success = false,
                appliedChanges = appliedChanges,
                conflicts = conflicts,
                serverTimestamp = getCurrentUtcIsoString(),
                message = "Sync failed: ${e.message}"
            )
        }
    }

    /**
     * Apply a single sync change
     */
    private suspend fun applySyncChange(change: SyncChange): Result<Unit> = runCatching {
        when (change.table) {
            "events" -> applyEventChange(change)
            "participants" -> applyParticipantChange(change)
            "votes" -> applyVoteChange(change)
            else -> throw IllegalArgumentException("Unknown table: ${change.table}")
        }
    }

    private suspend fun applyEventChange(change: SyncChange) {
        val eventData = json.decodeFromString<SyncEventData>(change.data)

        when (SyncOperation.valueOf(change.operation)) {
            SyncOperation.CREATE -> {
                // Check if event already exists
                val existing = eventRepository.getEvent(change.recordId)
                if (existing == null) {
                    // Create a full Event object from the sync data
                    val now = getCurrentUtcIsoString()
                    val event = com.guyghost.wakeve.models.Event(
                        id = eventData.id,
                        title = eventData.title,
                        description = eventData.description,
                        organizerId = eventData.organizerId,
                        participants = emptyList<String>(),
                        proposedSlots = emptyList<com.guyghost.wakeve.models.TimeSlot>(), // Will be added separately
                        deadline = eventData.deadline,
                        status = com.guyghost.wakeve.models.EventStatus.DRAFT,
                        createdAt = now,
                        updatedAt = now
                    )
                    eventRepository.createEvent(event)
                }
            }
            SyncOperation.UPDATE -> {
                val existing = eventRepository.getEvent(change.recordId)
                    ?: throw IllegalArgumentException("Event not found: ${change.recordId}")

                // Conflit : si la version serveur est plus recente, le serveur gagne
                if (existing.updatedAt > change.timestamp) {
                    throw IllegalStateException("Server version is newer for event ${change.recordId}")
                }

                // Mettre a jour l'evenement avec les donnees du client
                val updatedEvent = existing.copy(
                    title = eventData.title,
                    description = eventData.description,
                    deadline = eventData.deadline,
                    updatedAt = getCurrentUtcIsoString()
                )
                eventRepository.updateEvent(updatedEvent)
            }
            SyncOperation.DELETE -> {
                // Supprimer l'evenement (cascade vers time slots, participants, votes)
                // Si l'evenement n'existe plus, on ignore silencieusement
                val existing = eventRepository.getEvent(change.recordId)
                if (existing != null) {
                    eventRepository.deleteEvent(change.recordId)
                }
                // Deja supprime : rien a faire
            }
        }
    }

    private suspend fun applyParticipantChange(change: SyncChange) {
        val participantData = json.decodeFromString<SyncParticipantData>(change.data)

        when (SyncOperation.valueOf(change.operation)) {
            SyncOperation.CREATE -> {
                // Check if participant already exists
                val existing = eventRepository.getParticipants(participantData.eventId)?.contains(participantData.userId) ?: false
                if (!existing) {
                    eventRepository.addParticipant(participantData.eventId, participantData.userId)
                }
            }
            SyncOperation.UPDATE -> {
                // Mettre a jour le role/statut du participant
                val participantRecord = participantQueries
                    .selectByEventIdAndUserId(participantData.eventId, participantData.userId)
                    .executeAsOneOrNull()
                    ?: throw IllegalArgumentException("Participant not found: ${participantData.userId} in event ${participantData.eventId}")

                // Conflit : si la version serveur est plus recente
                if (participantRecord.updatedAt > change.timestamp) {
                    throw IllegalStateException("Server version is newer for participant ${change.recordId}")
                }

                val now = getCurrentUtcIsoString()
                participantQueries.updateParticipant(
                    role = participantRecord.role,  // Conserver le role existant (le client ne peut pas changer le role via sync)
                    hasValidatedDate = participantRecord.hasValidatedDate,
                    updatedAt = now,
                    id = participantRecord.id
                )
            }
            SyncOperation.DELETE -> {
                // Supprimer le participant de l'evenement
                // Les votes associes seront supprimes en cascade (FK ON DELETE CASCADE)
                val participantRecord = participantQueries
                    .selectByEventIdAndUserId(participantData.eventId, participantData.userId)
                    .executeAsOneOrNull()

                if (participantRecord != null) {
                    participantQueries.deleteParticipant(participantRecord.id)
                }
                // Deja supprime : rien a faire
            }
        }
    }

    private suspend fun applyVoteChange(change: SyncChange) {
        val voteData = json.decodeFromString<SyncVoteData>(change.data)

        when (SyncOperation.valueOf(change.operation)) {
            SyncOperation.CREATE -> {
                // For votes, we need to check if it already exists
                // Since the repository doesn't have a getVote method, we'll try to add it and handle the error
                try {
                    val preference = com.guyghost.wakeve.models.Vote.valueOf(voteData.preference)
                    eventRepository.addVote(voteData.eventId, voteData.participantId, voteData.slotId, preference)
                } catch (e: Exception) {
                    // Vote might already exist, skip
                }
            }
            SyncOperation.UPDATE -> {
                // Mettre a jour la preference du vote (last-write-wins sur le timestamp)
                val voteId = "vote_${voteData.slotId}_${voteData.participantId}"
                val existingVote = voteQueries.selectById(voteId).executeAsOneOrNull()
                    ?: throw IllegalArgumentException("Vote not found: $voteId")

                // Conflit : last-write-wins base sur le timestamp
                if (existingVote.updatedAt > change.timestamp) {
                    throw IllegalStateException("Server version is newer for vote $voteId")
                }

                val now = getCurrentUtcIsoString()
                voteQueries.updateVote(
                    vote = voteData.preference,
                    updatedAt = now,
                    id = voteId
                )
            }
            SyncOperation.DELETE -> {
                // Supprimer le vote
                val voteId = "vote_${voteData.slotId}_${voteData.participantId}"
                val existingVote = voteQueries.selectById(voteId).executeAsOneOrNull()

                if (existingVote != null) {
                    voteQueries.deleteVote(voteId)
                }
                // Deja supprime : rien a faire
            }
        }
    }

    /**
     * Recuperer les donnees serveur actuelles pour la resolution de conflits
     */
    private suspend fun getServerData(table: String, recordId: String): String? {
        return when (table) {
            "events" -> eventRepository.getEvent(recordId)?.let { json.encodeToString(it) }
            "participants" -> {
                // Le recordId pour les participants est au format "part_{eventId}_{userId}"
                val participantRecord = participantQueries.selectById(recordId).executeAsOneOrNull()
                if (participantRecord != null) {
                    json.encodeToString(SyncParticipantData(
                        eventId = participantRecord.eventId,
                        userId = participantRecord.userId
                    ))
                } else {
                    null
                }
            }
            "votes" -> {
                // Le recordId pour les votes est au format "vote_{slotId}_{participantId}"
                val voteRecord = voteQueries.selectById(recordId).executeAsOneOrNull()
                if (voteRecord != null) {
                    json.encodeToString(SyncVoteData(
                        eventId = voteRecord.eventId,
                        participantId = voteRecord.participantId,
                        slotId = voteRecord.timeslotId,
                        preference = voteRecord.vote
                    ))
                } else {
                    null
                }
            }
            else -> null
        }
    }

    /**
     * Horodatage UTC actuel au format ISO 8601
     */
    private fun getCurrentUtcIsoString(): String {
        return Clock.System.now().toString()
    }
}
