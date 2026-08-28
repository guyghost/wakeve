package com.guyghost.wakeve.sync

import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.UserRepository
import com.guyghost.wakeve.repository.TimeSlotStorageIdentity
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.SyncChange
import com.guyghost.wakeve.models.ConfirmationEnvelopeAcknowledgement
import com.guyghost.wakeve.models.SyncConflict
import com.guyghost.wakeve.models.SyncEventData
import com.guyghost.wakeve.models.SyncOperation
import com.guyghost.wakeve.models.SyncParticipantData
import com.guyghost.wakeve.models.SyncRequest
import com.guyghost.wakeve.models.SyncResponse
import com.guyghost.wakeve.invitationexperience.Artwork
import com.guyghost.wakeve.invitationexperience.ArtworkSelectionCapability
import com.guyghost.wakeve.invitationexperience.DatabaseUpdateDraftAggregateUseCase
import com.guyghost.wakeve.invitationexperience.StudioEventFields
import com.guyghost.wakeve.invitationexperience.StudioCommitEnvelopeFactory
import com.guyghost.wakeve.invitationexperience.StudioCommitDisposition
import com.guyghost.wakeve.invitationexperience.StudioCommitSubject
import com.guyghost.wakeve.invitationexperience.StudioPendingSyncSubject
import com.guyghost.wakeve.invitationexperience.StudioSyncAck
import com.guyghost.wakeve.invitationexperience.StudioSyncOutcome
import com.guyghost.wakeve.invitationexperience.StudioCommitTransactionFinalizer
import com.guyghost.wakeve.invitationexperience.UpdateDraftAggregateCommand
import com.guyghost.wakeve.invitationexperience.UpdateDraftAggregateResult
import com.guyghost.wakeve.notification.ConfirmationFanOutReadiness
import com.guyghost.wakeve.models.SyncVoteData
import com.guyghost.wakeve.poll.PollBallotContract
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val LEGACY_VOTES_MUTATION_FORBIDDEN = "LEGACY_VOTES_MUTATION_FORBIDDEN"
private const val STUDIO_COMMIT_TABLE = "studio_commit"

private class StudioSyncRejection(
    val code: String,
    val retryable: Boolean = false
) : IllegalStateException(code)

/**
 * Service de synchronisation serveur pour le traitement des changements offline
 */
class SyncService(
    private val db: WakeveDb,
    internal val studioPreReadNoneBarrier: suspend (String) -> Unit = {},
    internal val studioAfterCommitTransactionBarrier: suspend (String) -> Unit = {}
) {

    private val eventRepository = DatabaseEventRepository(db)
    private val userRepository = UserRepository(db)
    private val json = Json { ignoreUnknownKeys = true }

    private val participantQueries = db.participantQueries
    private val voteQueries = db.voteQueries
    private val confirmationEnvelopeIngestor = ConfirmationEnvelopeIngestor(
        db = db,
        eventRepository = eventRepository,
        json = json,
        fanOutReadiness = ConfirmationFanOutReadiness.DISABLED
    )

    /**
     * Process a batch of sync changes from client
     */
    suspend fun processSyncChanges(request: SyncRequest, userId: String): SyncResponse {
        val conflicts = mutableListOf<SyncConflict>()
        var appliedChanges = 0
        val confirmationAcknowledgements = mutableListOf<ConfirmationEnvelopeAcknowledgement>()
        val ballotAcknowledgements = mutableListOf<PollBallotContract.BallotServerAck>()
        val studioAcknowledgements = mutableListOf<StudioSyncAck>()

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
                        resolution = "REJECTED",
                        code = if (change.table == STUDIO_COMMIT_TABLE) "FORBIDDEN" else null,
                        retryable = if (change.table == STUDIO_COMMIT_TABLE) false else null
                    ))
                    continue
                }

                if (change.table == "votes") {
                    conflicts.add(
                        SyncConflict(
                            changeId = change.id,
                            table = change.table,
                            recordId = change.recordId,
                            clientData = change.data,
                            serverData = getServerData(change.table, change.recordId).orEmpty(),
                            resolution = "REJECTED",
                            code = LEGACY_VOTES_MUTATION_FORBIDDEN,
                            retryable = false
                        )
                    )
                    continue
                }

                val result = applySyncChange(change)
                if (result.isSuccess) {
                    appliedChanges++
                    result.getOrNull()?.confirmationAcknowledgement?.let(confirmationAcknowledgements::add)
                    result.getOrNull()?.ballotAcknowledgement?.let(ballotAcknowledgements::add)
                    result.getOrNull()?.studioAcknowledgement?.let(studioAcknowledgements::add)
                } else {
                    // Handle conflict
                    val serverData = getServerData(change.table, change.recordId)
                    val typedRejection = result.exceptionOrNull() as? StudioSyncRejection
                    conflicts.add(SyncConflict(
                        changeId = change.id,
                        table = change.table,
                        recordId = change.recordId,
                        clientData = change.data,
                        serverData = serverData ?: "",
                        resolution = if (typedRejection != null) "REJECTED" else "SERVER_WINS",
                        code = typedRejection?.code,
                        retryable = typedRejection?.retryable
                    ))
                }
            }

            val serverTimestamp = getCurrentUtcIsoString()

            return SyncResponse(
                success = true,
                appliedChanges = appliedChanges,
                conflicts = conflicts,
                serverTimestamp = serverTimestamp,
                message = syncMessage(
                    conflicts = conflicts,
                    acknowledgedConfirmationEnvelope = confirmationAcknowledgements.isNotEmpty()
                ),
                confirmationAcknowledgements = confirmationAcknowledgements,
                ballotAcknowledgements = ballotAcknowledgements,
                studioAcknowledgements = studioAcknowledgements
            )

        } catch (e: Exception) {
            return SyncResponse(
                success = false,
                appliedChanges = appliedChanges,
                conflicts = conflicts,
                serverTimestamp = getCurrentUtcIsoString(),
                message = serverSyncFailureMessage(),
                confirmationAcknowledgements = confirmationAcknowledgements,
                ballotAcknowledgements = ballotAcknowledgements,
                studioAcknowledgements = studioAcknowledgements
            )
        }
    }

    /**
     * Apply a single sync change
     */
    private suspend fun applySyncChange(change: SyncChange): Result<SyncChangeDisposition> = runCatching {
        when (change.table) {
            "events" -> {
                applyEventChange(change)
                SyncChangeDisposition.STANDARD
            }
            "participants" -> {
                applyParticipantChange(change)
                SyncChangeDisposition.STANDARD
            }
            "votes" -> error(LEGACY_VOTES_MUTATION_FORBIDDEN)
            "poll_ballot" -> applyPollBallotChange(change)
            "studio_commit" -> applyStudioCommit(change)
            "confirmation_effect_outbox" -> confirmationEnvelopeIngestor.acknowledge(change)
            else -> throw IllegalArgumentException("Unknown table: ${change.table}")
        }
    }

    private suspend fun applyPollBallotChange(change: SyncChange): SyncChangeDisposition {
        require(SyncOperation.valueOf(change.operation) == SyncOperation.UPDATE) {
            "Complete ballot sync only accepts UPDATE"
        }
        val payload = json.decodeFromString(
            PollBallotContract.BallotSyncPayload.serializer(),
            change.data
        )
        require(payload.localReceiptId == change.recordId) {
            "Ballot sync record does not match its local receipt"
        }
        require(payload.command.identity.actorId == change.userId) {
            "Cannot sync a ballot for another actor"
        }
        return SyncChangeDisposition(
            ballotAcknowledgement = eventRepository.applySyncedCompleteBallot(
                payload = payload,
                authenticatedActorId = change.userId
            )
        )
    }

    private suspend fun applyStudioCommit(change: SyncChange): SyncChangeDisposition {
        val subject = json.decodeFromString(StudioPendingSyncSubject.serializer(), change.data)
        require(subject.schemaVersion == 1 && subject.localReceiptId == change.recordId) {
            "Studio sync subject identity is invalid"
        }
        if (subject.envelope.requestPayload.actorId != change.userId) {
            throw StudioSyncRejection("FORBIDDEN")
        }
        require(StudioCommitEnvelopeFactory.isValid(subject.envelope)) {
            "Studio commit envelope is not canonical"
        }
        require(subject.envelope.requestPayload.subject.eventId == subject.eventId) {
            "Studio commit subject event is divergent"
        }
        require(subject.expectedResultingArtwork == subject.envelope.expectedResultingArtwork) {
            "Studio expected artwork is divergent"
        }
        val draft = json.decodeFromString<ServerStudioCanonicalDraft>(
            subject.envelope.requestPayload.canonicalDraftJson
        )
        require(draft.artwork == subject.expectedResultingArtwork) {
            "Studio resulting artwork does not match the fingerprinted snapshot"
        }
        val commitSubject = subject.envelope.requestPayload.subject
        val expectedBaseRevision = when (commitSubject) {
            is StudioCommitSubject.New -> {
                require(SyncOperation.valueOf(change.operation) == SyncOperation.CREATE &&
                    subject.committedRevision == 1L
                ) { "New Studio commits require CREATE at revision 1" }
                0L
            }
            is StudioCommitSubject.EditExisting -> {
                require(SyncOperation.valueOf(change.operation) == SyncOperation.UPDATE &&
                    subject.committedRevision == commitSubject.baseRevision + 1L
                ) { "Studio edit revision is divergent" }
                commitSubject.baseRevision
            }
        }

        val replayBeforeMutation = try {
            readDurableStudioAck(subject, change.userId, commitSubject)
        } catch (rejection: StudioSyncRejection) {
            throw rejection
        } catch (_: Exception) {
            awaitDurableStudioAck(subject, change.userId, commitSubject)
        }
        replayBeforeMutation?.let { durableAck ->
            return SyncChangeDisposition(
                studioAcknowledgement = durableAck
            )
        }

        val currentEvent = db.eventQueries.selectById(subject.eventId).executeAsOneOrNull()
        when (commitSubject) {
            is StudioCommitSubject.New -> {
                if (currentEvent != null) {
                    readDurableStudioAck(subject, change.userId, commitSubject)?.let { durableAck ->
                        return SyncChangeDisposition(studioAcknowledgement = durableAck)
                    }
                    if (currentEvent.organizerId != change.userId) throw StudioSyncRejection("FORBIDDEN")
                    if (currentEvent.status != com.guyghost.wakeve.models.EventStatus.DRAFT.name) {
                        throw StudioSyncRejection("EVENT_NOT_DRAFT")
                    }
                    throw StudioSyncRejection("STALE_BASE_REVISION")
                }
            }
            is StudioCommitSubject.EditExisting -> {
                if (currentEvent == null || currentEvent.organizerId != change.userId) {
                    throw StudioSyncRejection("FORBIDDEN")
                }
                if (currentEvent.status != com.guyghost.wakeve.models.EventStatus.DRAFT.name) {
                    throw StudioSyncRejection("EVENT_NOT_DRAFT")
                }
                if (currentEvent.aggregateRevision != commitSubject.baseRevision) {
                    throw StudioSyncRejection("STALE_BASE_REVISION")
                }
            }
        }

        val command = draft.toCommand(
            eventId = subject.eventId,
            actorId = change.userId,
            operationId = subject.localReceiptId,
            expectedBaseRevision = expectedBaseRevision,
            draftRevision = subject.envelope.identity.draftRevision
        )
        require(StudioCommitEnvelopeFactory.build(command) == subject.envelope) {
            "Studio command reconstruction diverges from its canonical envelope"
        }
        studioPreReadNoneBarrier(subject.localReceiptId)
        var transactionAck: StudioSyncAck? = null
        val aggregateOwner = DatabaseUpdateDraftAggregateUseCase(db)
        val finalizer = StudioCommitTransactionFinalizer { committed, envelope, committedAt ->
                val ack = StudioSyncAck(
                    localReceiptId = committed.operationId,
                    serverReceiptId = "studio-server-ack:${envelope.durableOperationRef}",
                    eventId = committed.eventId,
                    committedRevision = committed.committedRevision,
                    durableOperationRef = envelope.durableOperationRef,
                    requestFingerprint = envelope.requestFingerprint,
                    outcome = StudioSyncOutcome.APPLIED,
                    disposition = when (commitSubject) {
                        is StudioCommitSubject.New -> StudioCommitDisposition.CREATED
                        is StudioCommitSubject.EditExisting -> StudioCommitDisposition.UPDATED
                    },
                    artwork = command.expectedResultingArtwork
                )
                db.invitationExperienceQueries.finalizeStudioServerOperationReceipt(
                    server_receipt_id = ack.serverReceiptId,
                    server_ack_payload = json.encodeToString(StudioSyncAck.serializer(), ack),
                    updated_at = committedAt,
                    operation_id = committed.operationId,
                    event_id = committed.eventId,
                    aggregate_revision = committed.committedRevision,
                    durable_operation_ref = envelope.durableOperationRef,
                    request_fingerprint = envelope.requestFingerprint
                )
                val finalized = db.invitationExperienceQueries
                    .selectOperationReceiptByOperationId(committed.operationId)
                    .executeAsOneOrNull()
                check(finalized?.server_ack_payload == json.encodeToString(StudioSyncAck.serializer(), ack) &&
                    finalized.status == "COMMITTED" && finalized.server_receipt_id == ack.serverReceiptId
                ) { "Studio server acknowledgement was not finalized atomically" }
                db.syncMetadataQueries.markSynced("studio:${committed.operationId}")
            transactionAck = ack
        }
        val committed = when (val result = aggregateOwner.executeWithFinalizer(command, finalizer)) {
            is UpdateDraftAggregateResult.Committed -> {
                if (transactionAck == null) {
                    transactionAck = result.serverReceiptProof?.let { proof ->
                        val captured = runCatching {
                            json.decodeFromString(
                                StudioSyncAck.serializer(),
                                proof.serverAckPayload
                            )
                        }.getOrNull() ?: throw StudioSyncRejection("REPOSITORY_INCONSISTENT")
                        validateCapturedStudioAck(captured, proof, subject, commitSubject)
                    } ?: readDurableStudioAck(subject, change.userId, commitSubject)
                        ?: awaitDurableStudioAck(subject, change.userId, commitSubject)
                }
                result
            }
            is UpdateDraftAggregateResult.Rejected -> {
                awaitDurableStudioAck(subject, change.userId, commitSubject)?.let { durableAck ->
                    return SyncChangeDisposition(studioAcknowledgement = durableAck)
                }
                when (result.error) {
                    com.guyghost.wakeve.invitationexperience.InvitationExperienceError.FORBIDDEN ->
                        throw StudioSyncRejection("FORBIDDEN")
                    com.guyghost.wakeve.invitationexperience.InvitationExperienceError.CONFLICT ->
                        throw StudioSyncRejection("STALE_BASE_REVISION")
                    else -> error("Studio commit rejected: ${result.error}")
                }
            }
            is UpdateDraftAggregateResult.OutcomeUnknown -> {
                awaitDurableStudioAck(subject, change.userId, commitSubject)?.let { durableAck ->
                    return SyncChangeDisposition(studioAcknowledgement = durableAck)
                }
                error("Studio commit outcome is unknown")
            }
        }
        check(committed.committedRevision == subject.committedRevision) {
            "Studio transaction result revision diverges from its durable subject"
        }
        studioAfterCommitTransactionBarrier(subject.localReceiptId)
        return SyncChangeDisposition(
            studioAcknowledgement = transactionAck
                ?: error("Studio server acknowledgement was not produced in the aggregate transaction")
        )
    }

    private fun validateCapturedStudioAck(
        acknowledgement: StudioSyncAck,
        receiptProof: UpdateDraftAggregateResult.StudioServerReceiptProof,
        subject: StudioPendingSyncSubject,
        commitSubject: StudioCommitSubject
    ): StudioSyncAck {
        val expectedDisposition = when (commitSubject) {
            is StudioCommitSubject.New -> StudioCommitDisposition.CREATED
            is StudioCommitSubject.EditExisting -> StudioCommitDisposition.UPDATED
        }
        if (receiptProof.status != "COMMITTED" ||
            receiptProof.serverReceiptId.isNullOrBlank() ||
            receiptProof.serverReceiptId != acknowledgement.serverReceiptId ||
            acknowledgement.localReceiptId != subject.localReceiptId ||
            acknowledgement.serverReceiptId.isBlank() ||
            acknowledgement.eventId != subject.eventId ||
            acknowledgement.committedRevision != subject.committedRevision ||
            acknowledgement.durableOperationRef != subject.envelope.durableOperationRef ||
            acknowledgement.requestFingerprint != subject.envelope.requestFingerprint ||
            acknowledgement.disposition != expectedDisposition ||
            acknowledgement.artwork != subject.expectedResultingArtwork ||
            acknowledgement.outcome !in setOf(
                StudioSyncOutcome.APPLIED,
                StudioSyncOutcome.ALREADY_APPLIED
            )
        ) throw StudioSyncRejection("REPOSITORY_INCONSISTENT")
        return acknowledgement
    }

    private fun readDurableStudioAck(
        subject: StudioPendingSyncSubject,
        authenticatedActorId: String,
        commitSubject: StudioCommitSubject
    ): StudioSyncAck? {
        val receipt = db.invitationExperienceQueries
            .selectOperationReceiptByOperationId(subject.localReceiptId)
            .executeAsOneOrNull()
            ?: return null
        val persistedEnvelope = runCatching {
            json.decodeFromString(
                com.guyghost.wakeve.invitationexperience.StudioCommitEnvelope.serializer(),
                receipt.commit_envelope
            )
        }.getOrNull()
        if (receipt.event_id != subject.eventId ||
            receipt.actor_id != authenticatedActorId ||
            receipt.action != "UPDATE_DRAFT_AGGREGATE" ||
            receipt.aggregate_revision != subject.committedRevision ||
            receipt.durable_operation_ref != subject.envelope.durableOperationRef ||
            receipt.request_fingerprint != subject.envelope.requestFingerprint ||
            persistedEnvelope != subject.envelope ||
            subject.expectedResultingArtwork != subject.envelope.expectedResultingArtwork
        ) throw StudioSyncRejection("IDEMPOTENCY_CONFLICT")
        val durableAckPayload = receipt.server_ack_payload
            ?: throw StudioSyncRejection("REPOSITORY_INCONSISTENT")
        val durableAck = runCatching {
            json.decodeFromString(StudioSyncAck.serializer(), durableAckPayload)
        }.getOrNull() ?: throw StudioSyncRejection("REPOSITORY_INCONSISTENT")
        validateCapturedStudioAck(
            durableAck,
            UpdateDraftAggregateResult.StudioServerReceiptProof(
                status = receipt.status,
                serverReceiptId = receipt.server_receipt_id,
                serverAckPayload = durableAckPayload
            ),
            subject,
            commitSubject
        )
        if (durableAck.localReceiptId != receipt.operation_id ||
            durableAck.eventId != receipt.event_id ||
            durableAck.committedRevision != receipt.aggregate_revision ||
            durableAck.durableOperationRef != receipt.durable_operation_ref ||
            durableAck.requestFingerprint != receipt.request_fingerprint
        ) throw StudioSyncRejection("REPOSITORY_INCONSISTENT")
        return durableAck
    }

    private suspend fun awaitDurableStudioAck(
        subject: StudioPendingSyncSubject,
        authenticatedActorId: String,
        commitSubject: StudioCommitSubject
    ): StudioSyncAck? {
        repeat(40) {
            try {
                readDurableStudioAck(subject, authenticatedActorId, commitSubject)?.let { return it }
            } catch (rejection: StudioSyncRejection) {
                throw rejection
            } catch (_: Exception) {
                // A second SQLite connection can briefly observe BUSY while the winner commits.
            }
            delay(5)
        }
        return readDurableStudioAck(subject, authenticatedActorId, commitSubject)
    }

    private suspend fun applyEventChange(change: SyncChange) {
        val eventData = json.decodeFromString<SyncEventData>(change.data)

        when (SyncOperation.valueOf(change.operation)) {
            SyncOperation.CREATE -> {
                if (eventData.organizerId != change.userId) {
                    throw IllegalArgumentException("Cannot create an event for another organizer")
                }
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
                if (existing.organizerId != change.userId) {
                    throw IllegalArgumentException("Only the event organizer can sync event updates")
                }

                if (eventData.status == com.guyghost.wakeve.models.EventStatus.CONFIRMED.name) {
                    applyConfirmedEventDecision(change, eventData)
                    return
                }

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
                    if (existing.organizerId != change.userId) {
                        throw IllegalArgumentException("Only the event organizer can sync event deletion")
                    }
                    eventRepository.deleteEvent(change.recordId)
                }
                // Deja supprime : rien a faire
            }
        }
    }

    /**
     * Applies the server-side part of an already committed local confirmation before the
     * dependent envelope is ingested. This is a single-server transaction only; it does not
     * claim an atomic transaction with the mobile database or any fan-out provider.
     */
    private fun applyConfirmedEventDecision(change: SyncChange, eventData: SyncEventData) {
        val slotId = eventData.confirmedSlotId
            ?: throw IllegalArgumentException("Confirmed event sync requires confirmedSlotId")
        val finalDate = eventData.finalDate
            ?: throw IllegalArgumentException("Confirmed event sync requires finalDate")

        db.transaction {
            val event = db.eventQueries.selectById(change.recordId).executeAsOneOrNull()
                ?: throw IllegalArgumentException("Event not found: ${change.recordId}")
            if (event.organizerId != change.userId) {
                throw IllegalArgumentException("Only the event organizer can sync confirmation")
            }
            val persistedSlotId = TimeSlotStorageIdentity.physicalId(change.recordId, slotId)
            val slot = db.timeSlotQueries.selectById(persistedSlotId).executeAsOneOrNull()
                ?: throw IllegalArgumentException("Confirmed event sync slot was not found")
            if (slot.eventId != change.recordId || slot.startTime != finalDate) {
                throw IllegalArgumentException("Confirmed event sync does not match the selected slot")
            }

            val confirmedDate = db.confirmedDateQueries.selectByEventId(change.recordId).executeAsOneOrNull()
            when {
                confirmedDate == null && event.status == com.guyghost.wakeve.models.EventStatus.POLLING.name -> {
                    val now = getCurrentUtcIsoString()
                    db.eventQueries.updateEventStatus(
                        status = com.guyghost.wakeve.models.EventStatus.CONFIRMED.name,
                        updatedAt = now,
                        id = change.recordId
                    )
                    db.confirmedDateQueries.insertConfirmedDate(
                        id = "confirmed_${change.recordId}",
                        eventId = change.recordId,
                        timeslotId = persistedSlotId,
                        confirmedByOrganizerId = change.userId,
                        confirmedAt = change.timestamp,
                        updatedAt = now
                    )
                }
                confirmedDate?.timeslotId == persistedSlotId &&
                    event.status == com.guyghost.wakeve.models.EventStatus.CONFIRMED.name -> Unit
                else -> throw IllegalStateException("Confirmed event sync conflicts with the durable server decision")
            }
        }
    }

    private suspend fun applyParticipantChange(change: SyncChange) {
        val participantData = json.decodeFromString<SyncParticipantData>(change.data)
        val event = eventRepository.getEvent(participantData.eventId)
            ?: throw IllegalArgumentException("Event not found: ${participantData.eventId}")
        if (event.organizerId != change.userId) {
            throw IllegalArgumentException("Only the event organizer can sync participant changes")
        }

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
            "poll_ballot" -> db.pollBallotReceiptQueries
                .selectByReceiptId(recordId)
                .executeAsOneOrNull()
                ?.syncPayload
            else -> null
        }
    }

    /**
     * Horodatage UTC actuel au format ISO 8601
     */
    private fun getCurrentUtcIsoString(): String {
        return Clock.System.now().toString()
    }

    private fun syncMessage(
        conflicts: List<SyncConflict>,
        acknowledgedConfirmationEnvelope: Boolean
    ): String = when {
        acknowledgedConfirmationEnvelope && conflicts.isEmpty() -> CONFIRMATION_ENVELOPE_ACKNOWLEDGED_PENDING_DISPATCH
        conflicts.isEmpty() -> "All changes applied successfully"
        else -> "${conflicts.size} conflicts detected"
    }
}

/**
 * The sync response remains deliberately generic for regular data changes. A confirmation
 * envelope gets a narrow acknowledgement that means only server-side persistence succeeded;
 * participant notification and calendar work are independent later stages.
 */
private const val CONFIRMATION_ENVELOPE_ACKNOWLEDGED_PENDING_DISPATCH =
    "confirmation-envelope-acknowledged; effect-dispatch-pending; fan-out-disabled"

@Serializable
private data class ServerStudioCanonicalSlot(
    val id: String,
    val start: String?,
    val end: String?,
    val timezone: String,
    val timeOfDay: String
)

@Serializable
private data class ServerStudioCanonicalDraft(
    val title: String,
    val description: String,
    val deadline: String,
    val eventType: String,
    val eventTypeCustom: String?,
    val minParticipants: Int?,
    val maxParticipants: Int?,
    val expectedParticipants: Int?,
    val planningMode: String,
    val slots: List<ServerStudioCanonicalSlot>,
    val artwork: Artwork
) {
    fun toCommand(
        eventId: String,
        actorId: String,
        operationId: String,
        expectedBaseRevision: Long,
        draftRevision: Long
    ): UpdateDraftAggregateCommand = UpdateDraftAggregateCommand(
        eventId = eventId,
        actorId = actorId,
        expectedBaseRevision = expectedBaseRevision,
        eventDraft = StudioEventFields(
            title = title,
            description = description,
            proposedSlots = slots.map { slot ->
                com.guyghost.wakeve.models.TimeSlot(
                    id = slot.id,
                    start = slot.start,
                    end = slot.end,
                    timezone = slot.timezone,
                    timeOfDay = com.guyghost.wakeve.models.TimeOfDay.valueOf(slot.timeOfDay)
                )
            },
            deadline = deadline,
            eventType = com.guyghost.wakeve.models.EventType.valueOf(eventType),
            eventTypeCustom = eventTypeCustom,
            minParticipants = minParticipants,
            maxParticipants = maxParticipants,
            expectedParticipants = expectedParticipants,
            planningMode = com.guyghost.wakeve.models.EventPlanningMode.valueOf(planningMode)
        ),
        artwork = artwork,
        operationId = operationId,
        artworkCapability = ArtworkSelectionCapability.Hidden,
        draftRevision = draftRevision
    )
}

private data class SyncChangeDisposition(
    val confirmationAcknowledgement: ConfirmationEnvelopeAcknowledgement? = null,
    val ballotAcknowledgement: PollBallotContract.BallotServerAck? = null,
    val studioAcknowledgement: StudioSyncAck? = null
) {
    companion object {
        val STANDARD = SyncChangeDisposition()
    }
}

@Serializable
private data class ConfirmationEffectEnvelopePayload(
    val domainEventId: String,
    val effectKey: String,
    val eventId: String,
    val slotId: String,
    val operationId: String,
    val createdAt: String
)

/**
 * Server-side acknowledgement boundary for a locally committed confirmation envelope.
 *
 * The local database and the server database are distinct. This component never implies a
 * cross-database transaction and never invokes recipient, calendar, or provider fan-out.
 */
private class ConfirmationEnvelopeIngestor(
    private val db: WakeveDb,
    private val eventRepository: DatabaseEventRepository,
    private val json: Json,
    private val fanOutReadiness: ConfirmationFanOutReadiness
) {
    fun acknowledge(change: SyncChange): SyncChangeDisposition {
        require(SyncOperation.valueOf(change.operation) == SyncOperation.CREATE) {
            "Confirmation envelope acknowledgement only accepts CREATE"
        }
        val envelope = json.decodeFromString<ConfirmationEffectEnvelopePayload>(change.data)
        require(envelope.domainEventId == change.recordId) {
            "Confirmation envelope record identity does not match its domain event identity"
        }
        require(envelope.effectKey == "${envelope.domainEventId}:confirmation") {
            "Confirmation envelope effect identity is invalid"
        }
        require(envelope.domainEventId == "poll-date-confirmed:${envelope.eventId}:${envelope.slotId}:v1") {
            "Confirmation envelope domain identity is invalid"
        }
        require(envelope.operationId.isNotBlank() && envelope.createdAt.isNotBlank()) {
            "Confirmation envelope requires operation and creation timestamps"
        }

        val event = eventRepository.getEvent(envelope.eventId)
            ?: error("Confirmation envelope event was not found")
        require(event.organizerId == change.userId) {
            "Only the event organizer can acknowledge a confirmation envelope"
        }
        require(event.status == com.guyghost.wakeve.models.EventStatus.CONFIRMED) {
            "Confirmation envelope event is not confirmed"
        }
        val persistedSlotId = TimeSlotStorageIdentity.physicalId(envelope.eventId, envelope.slotId)
        val slot = db.timeSlotQueries.selectById(persistedSlotId).executeAsOneOrNull()
            ?: error("Confirmation envelope slot was not found")
        require(slot.eventId == envelope.eventId) {
            "Confirmation envelope slot does not belong to its event"
        }
        val confirmedDate = db.confirmedDateQueries
            .selectByEventId(envelope.eventId)
            .executeAsOneOrNull()
            ?: error("Confirmation envelope event does not have a durable confirmed date")
        require(confirmedDate.timeslotId == persistedSlotId) {
            "Confirmation envelope slot does not match the durable confirmed date"
        }
        db.transaction {
            val existing = db.confirmationEffectOutboxQueries
                .selectByDomainEventId(envelope.domainEventId)
                .executeAsOneOrNull()
            if (existing == null) {
                db.confirmationEffectOutboxQueries.insertEnvelope(
                    domainEventId = envelope.domainEventId,
                    effectKey = envelope.effectKey,
                    eventId = envelope.eventId,
                    slotId = persistedSlotId,
                    operationId = envelope.operationId,
                    status = ACKNOWLEDGED_PENDING_DISPATCH_STATUS,
                    createdAt = envelope.createdAt
                )
            } else {
                require(
                    existing.effectKey == envelope.effectKey &&
                        existing.eventId == envelope.eventId &&
                        existing.slotId == persistedSlotId &&
                        existing.operationId == envelope.operationId
                ) {
                    "Confirmation envelope conflicts with a prior acknowledgement"
                }
            }
        }

        // The readiness value is deliberately consumed here rather than inferred from a
        // configuration string. Until a future rollout uses SHADOW_WRITE/ENABLED, no effect
        // dispatcher is reachable from this acknowledgement path.
        check(fanOutReadiness == ConfirmationFanOutReadiness.DISABLED) {
            "Confirmation fan-out readiness must be explicitly implemented before dispatch"
        }
        return SyncChangeDisposition(
            confirmationAcknowledgement = ConfirmationEnvelopeAcknowledgement(
                domainEventId = envelope.domainEventId,
                effectKey = envelope.effectKey,
                operationId = envelope.operationId,
                receiptId = acknowledgementReceiptId(envelope.domainEventId)
            )
        )
    }

    private companion object {
        const val ACKNOWLEDGED_PENDING_DISPATCH_STATUS = "ACKNOWLEDGED_PENDING_DISPATCH"

        fun acknowledgementReceiptId(domainEventId: String): String =
            "confirmation-envelope-ack:$domainEventId"
    }
}

internal fun serverSyncFailureMessage(): String =
    "Sync failed. Please retry when your connection is stable."
