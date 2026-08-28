package com.guyghost.wakeve.invitationexperience

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventPlanningMode
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.poll.PollBallotContract
import com.guyghost.wakeve.repository.TimeSlotStorageIdentity
import com.guyghost.wakeve.sync.SyncManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

enum class LibraryProjection {
    DRAFTS,
    HOSTING,
    ATTENDING,
    UPCOMING,
    PAST
}

enum class LibraryNextAction {
    CONTINUE_DRAFT,
    SUBMIT_VOTE,
    VIEW_POLL_RESULTS,
    COMPARE_OPTIONS,
    CONTINUE_ORGANIZATION,
    VIEW_EVENT,
    VIEW_ARCHIVE
}

sealed interface Freshness {
    data object Current : Freshness
    data class Stale(val cachedAt: Instant) : Freshness
    data object Unavailable : Freshness
}

/** Exact stable state captured before a cancellable load. */
sealed interface PreviousStableState<out Snapshot> {
    data object Idle : PreviousStableState<Nothing>
    data class Ready<Snapshot>(
        val snapshot: Snapshot,
        val freshness: Freshness
    ) : PreviousStableState<Snapshot>
    data class Empty(val scope: String) : PreviousStableState<Nothing>
}

sealed interface LibraryLoadState<out Snapshot> {
    data object Idle : LibraryLoadState<Nothing>
    data class Loading<Snapshot>(
        val previousStableState: PreviousStableState<Snapshot>
    ) : LibraryLoadState<Snapshot>
    data class Ready<Snapshot>(
        val snapshot: Snapshot,
        val freshness: Freshness
    ) : LibraryLoadState<Snapshot>
    data class Empty(val projection: LibraryProjection) : LibraryLoadState<Nothing>
    data class Failed<Snapshot>(
        val error: InvitationExperienceError,
        val previousStableState: PreviousStableState<Snapshot>
    ) : LibraryLoadState<Snapshot>
}

enum class InvitationExperienceError {
    NETWORK_UNAVAILABLE,
    REPOSITORY_UNAVAILABLE,
    NOT_FOUND,
    FORBIDDEN,
    VALIDATION,
    CONFLICT,
    REMOTE_ARTWORK_UNAVAILABLE,
    PROVIDER_UNAVAILABLE,
    SERVER_UNAVAILABLE,
    COMMIT_OUTCOME_UNKNOWN,
    RESOLUTION_OUTCOME_UNKNOWN,
    PERMANENT_FAILURE
}

enum class ViewerRole {
    ORGANIZER,
    MEMBER,
    NON_MEMBER
}

sealed interface MembershipState {
    data object NonMember : MembershipState
    data class ActiveMember(val memberId: String) : MembershipState
    data class Left(val memberId: String) : MembershipState
    data class Removed(val memberId: String) : MembershipState
}

enum class RsvpState {
    NOT_APPLICABLE,
    PENDING,
    ACCEPTED,
    DECLINED,
    UNAVAILABLE
}

sealed interface LibrarySyncState {
    data object Synced : LibrarySyncState
    data class Pending(val operationId: String) : LibrarySyncState
    data class Conflict(val operationId: String) : LibrarySyncState
    data class PermanentFailure(val operationId: String) : LibrarySyncState
    data object Unavailable : LibrarySyncState
}

enum class LibrarySyncWarning {
    CONFLICT,
    PERMANENT_FAILURE
}

data class LibraryEventInput(
    val event: Event,
    val viewerId: String,
    val viewerRole: ViewerRole,
    val membershipState: MembershipState,
    val rsvpState: RsvpState,
    val interactiveNextAction: LibraryNextAction?,
    val syncState: LibrarySyncState = LibrarySyncState.Unavailable,
    val archiveActive: Boolean = false,
    val artwork: Artwork = Artwork.None
)

data class LibraryCardProjection(
    val event: Event,
    val temporalClass: TemporalClass,
    val interactionPolicy: InteractionPolicy,
    val memberships: Set<LibraryProjection>,
    val nextAction: LibraryNextAction,
    val structuredSortBound: Instant?,
    val syncState: LibrarySyncState = LibrarySyncState.Unavailable,
    val warning: LibrarySyncWarning? = null,
    val reloadAvailable: Boolean = false,
    val artwork: Artwork = Artwork.None
)

class EventLibraryProjector {
    fun project(input: LibraryEventInput, now: Instant): LibraryCardProjection? {
        if (!hasCoherentAccess(input)) return null

        val temporalClass = EventTemporalClassifier.classify(input.event, now)
        val interactionPolicy = InvitationExperienceInteractionPolicy.derive(
            temporalClass = temporalClass,
            eventStatus = input.event.status,
            archiveActive = input.archiveActive
        )
        val memberships = buildSet {
            if (input.event.status == com.guyghost.wakeve.models.EventStatus.DRAFT) {
                add(LibraryProjection.DRAFTS)
            }
            if (input.viewerRole == ViewerRole.ORGANIZER) {
                add(LibraryProjection.HOSTING)
            }
            if (
                input.membershipState is MembershipState.ActiveMember &&
                input.rsvpState == RsvpState.ACCEPTED
            ) {
                add(LibraryProjection.ATTENDING)
            }
            if (input.event.status != com.guyghost.wakeve.models.EventStatus.DRAFT) {
                when (temporalClass) {
                    TemporalClass.UPCOMING -> add(LibraryProjection.UPCOMING)
                    TemporalClass.PAST -> add(LibraryProjection.PAST)
                    TemporalClass.UNDATED_DRAFT -> Unit
                }
            }
        }

        val warning = when (input.syncState) {
            is LibrarySyncState.Conflict -> LibrarySyncWarning.CONFLICT
            is LibrarySyncState.PermanentFailure -> LibrarySyncWarning.PERMANENT_FAILURE
            else -> null
        }

        return LibraryCardProjection(
            event = input.event,
            temporalClass = temporalClass,
            interactionPolicy = interactionPolicy,
            memberships = memberships,
            nextAction = nextAction(input, interactionPolicy),
            structuredSortBound = EventTemporalClassifier.structuredEndBound(input.event),
            syncState = input.syncState,
            warning = warning,
            reloadAvailable = warning != null,
            artwork = input.artwork
        )
    }

    fun filter(
        cards: List<LibraryCardProjection>,
        projection: LibraryProjection
    ): List<LibraryCardProjection> =
        cards
            .filter { projection in it.memberships }
            .sortedWith(
                compareBy<LibraryCardProjection, Instant?>(nullsLast()) { it.structuredSortBound }
                    .thenBy { it.event.id }
            )

    fun <Snapshot> cancelLoad(
        state: LibraryLoadState<Snapshot>
    ): LibraryLoadState<Snapshot> = when (state) {
        is LibraryLoadState.Loading -> when (val previous = state.previousStableState) {
            PreviousStableState.Idle -> LibraryLoadState.Idle
            is PreviousStableState.Ready -> LibraryLoadState.Ready(
                snapshot = previous.snapshot,
                freshness = previous.freshness
            )
            is PreviousStableState.Empty -> LibraryLoadState.Empty(
                projection = LibraryProjection.valueOf(previous.scope)
            )
        }
        else -> state
    }

    private fun hasCoherentAccess(input: LibraryEventInput): Boolean = when (input.viewerRole) {
        ViewerRole.ORGANIZER -> input.event.organizerId == input.viewerId
        ViewerRole.MEMBER ->
            input.event.organizerId != input.viewerId &&
                input.membershipState is MembershipState.ActiveMember
        ViewerRole.NON_MEMBER -> false
    }

    private fun nextAction(
        input: LibraryEventInput,
        interactionPolicy: InteractionPolicy
    ): LibraryNextAction {
        if (interactionPolicy == InteractionPolicy.READ_ONLY) {
            return LibraryNextAction.VIEW_ARCHIVE
        }
        if (input.event.status == com.guyghost.wakeve.models.EventStatus.DRAFT) {
            return LibraryNextAction.CONTINUE_DRAFT
        }
        return input.interactiveNextAction ?: LibraryNextAction.VIEW_EVENT
    }
}

@Serializable
sealed interface Artwork {
    @Serializable
    data object None : Artwork
    @Serializable
    data class Structured(
        val version: Int,
        val ref: ArtworkRef
    ) : Artwork
    @Serializable
    data class LegacyRemote(val validatedHttpsUrl: String) : Artwork
}

@Serializable
data class ArtworkRef(
    val source: ArtworkSource,
    val alt: ArtworkAlt,
    val focalPoint: ArtworkFocalPoint,
    val crop: ArtworkCrop
)

@Serializable
sealed interface ArtworkSource {
    @Serializable
    data class Preset(val presetId: String) : ArtworkSource
    @Serializable
    data class ServerAsset(
        val assetId: String,
        val canonicalHttpsUrl: String,
        val assetRevision: Long
    ) : ArtworkSource
}

@Serializable
sealed interface ArtworkAlt {
    @Serializable
    data object Decorative : ArtworkAlt
    @Serializable
    data class Informative(val localizedText: String) : ArtworkAlt
}

@Serializable
data class ArtworkFocalPoint(val x: Double, val y: Double)

@Serializable
enum class ArtworkCrop {
    FILL,
    FIT
}

sealed interface ArtworkSelectionCapability {
    data object Hidden : ArtworkSelectionCapability
    data class Unavailable(val reason: String) : ArtworkSelectionCapability
    data class Ready(
        val actorId: String,
        val accessRevision: Long,
        val authorizedAssetsByOpaqueId: Map<String, ArtworkSource.ServerAsset>
    ) : ArtworkSelectionCapability
}

data class ServerArtworkReference(
    val eventId: String,
    val assetId: String,
    val assetRevision: Long
) {
    init {
        require(eventId.isNotBlank() && assetId.isNotBlank() && assetRevision > 0L) {
            "Server artwork reference must be complete"
        }
    }
}

data class ServerArtworkReleaseSignal(
    val assetId: String,
    val assetRevision: Long,
    val releaseOperationId: String
) {
    init {
        require(assetId.isNotBlank() && assetRevision > 0L && releaseOperationId.isNotBlank()) {
            "Server artwork release signal must be complete"
        }
    }
}

sealed interface ServerArtworkReferenceResult {
    data class Bound(
        val reference: ServerArtworkReference,
        val referenceCount: Long
    ) : ServerArtworkReferenceResult

    data class Retained(val referenceCount: Long) : ServerArtworkReferenceResult

    data class FinalReleaseScheduled(
        val signal: ServerArtworkReleaseSignal
    ) : ServerArtworkReferenceResult

    data class Rejected(
        val error: InvitationExperienceError
    ) : ServerArtworkReferenceResult
}

interface ServerArtworkReferenceOwner {
    suspend fun bind(
        reference: ServerArtworkReference,
        operationId: String
    ): ServerArtworkReferenceResult

    suspend fun release(
        reference: ServerArtworkReference,
        operationId: String
    ): ServerArtworkReferenceResult
}

class DatabaseServerArtworkReferenceOwner(
    private val database: WakeveDb
) : ServerArtworkReferenceOwner {
    override suspend fun bind(
        reference: ServerArtworkReference,
        operationId: String
    ): ServerArtworkReferenceResult = runCatching {
        require(operationId.isNotBlank())
        database.transactionWithResult {
            bindInTransaction(reference, operationId)
        }
    }.getOrElse {
        ServerArtworkReferenceResult.Rejected(InvitationExperienceError.REPOSITORY_UNAVAILABLE)
    }

    override suspend fun release(
        reference: ServerArtworkReference,
        operationId: String
    ): ServerArtworkReferenceResult = runCatching {
        require(operationId.isNotBlank())
        database.transactionWithResult {
            releaseInTransaction(reference, operationId)
        }
    }.getOrElse {
        ServerArtworkReferenceResult.Rejected(InvitationExperienceError.REPOSITORY_UNAVAILABLE)
    }

    internal fun bindInTransaction(
        reference: ServerArtworkReference,
        operationId: String
    ): ServerArtworkReferenceResult {
        if (operationId.isBlank()) return rejectedValidation()
        val queries = database.invitationExperienceQueries

        queries.selectServerArtworkReferenceByBindOperationId(operationId)
            .executeAsOneOrNull()
            ?.let { replay ->
                if (!replay.matches(reference)) return rejectedConflict()
                return ServerArtworkReferenceResult.Bound(
                    reference,
                    queries.countServerArtworkReferences(
                        reference.assetId,
                        reference.assetRevision
                    ).executeAsOne()
                )
            }

        queries.selectServerArtworkReferenceOperation(operationId)
            .executeAsOneOrNull()
            ?.let { return rejectedConflict() }

        val artwork = queries.selectArtworkByEventId(reference.eventId).executeAsOneOrNull()
            ?: return rejectedNotFound()
        if (
            artwork.kind != "STRUCTURED" ||
            artwork.source_kind != "SERVER_ASSET" ||
            artwork.server_asset_id != reference.assetId ||
            artwork.asset_revision != reference.assetRevision
        ) return rejectedConflict()

        val existing = queries.selectServerArtworkReferenceByEventId(reference.eventId)
            .executeAsOneOrNull()
        if (existing != null) return rejectedConflict()

        val now = Clock.System.now().toString()
        queries.insertServerArtworkReference(
            event_id = reference.eventId,
            asset_id = reference.assetId,
            asset_revision = reference.assetRevision,
            bind_operation_id = operationId,
            created_at = now
        )
        val referenceCount = queries.countServerArtworkReferences(
            reference.assetId,
            reference.assetRevision
        ).executeAsOne()
        queries.insertServerArtworkReferenceOperation(
            operation_id = operationId,
            action = "BIND",
            event_id = reference.eventId,
            asset_id = reference.assetId,
            asset_revision = reference.assetRevision,
            result_kind = "BOUND",
            reference_count = referenceCount,
            created_at = now
        )
        return ServerArtworkReferenceResult.Bound(reference, referenceCount)
    }

    internal fun releaseInTransaction(
        reference: ServerArtworkReference,
        operationId: String
    ): ServerArtworkReferenceResult {
        if (operationId.isBlank()) return rejectedValidation()
        val queries = database.invitationExperienceQueries

        queries.selectServerArtworkReferenceOperation(operationId)
            .executeAsOneOrNull()
            ?.let { receipt ->
                if (!receipt.matches(reference) || receipt.action != "RELEASE") {
                    return rejectedConflict()
                }
                return when (receipt.result_kind) {
                    "RETAINED" -> ServerArtworkReferenceResult.Retained(
                        receipt.reference_count ?: return rejectedConflict()
                    )
                    "FINAL_RELEASE" -> ServerArtworkReferenceResult.FinalReleaseScheduled(
                        ServerArtworkReleaseSignal(
                            assetId = reference.assetId,
                            assetRevision = reference.assetRevision,
                            releaseOperationId = operationId
                        )
                    )
                    else -> rejectedConflict()
                }
            }

        // A reference can only be released after the event no longer points at
        // the asset. Aggregate replacement/deletion performs both operations
        // in the same SQLDelight transaction.
        if (queries.selectArtworkByEventId(reference.eventId).executeAsOneOrNull() != null) {
            return rejectedConflict()
        }
        val persisted = queries.selectServerArtworkReferenceByEventId(reference.eventId)
            .executeAsOneOrNull()
            ?: return rejectedNotFound()
        if (!persisted.matches(reference)) return rejectedConflict()

        queries.deleteServerArtworkReference(
            reference.eventId,
            reference.assetId,
            reference.assetRevision
        )
        val remaining = queries.countServerArtworkReferences(
            reference.assetId,
            reference.assetRevision
        ).executeAsOne()
        val now = Clock.System.now().toString()
        val resultKind = if (remaining > 0L) "RETAINED" else "FINAL_RELEASE"
        queries.insertServerArtworkReferenceOperation(
            operation_id = operationId,
            action = "RELEASE",
            event_id = reference.eventId,
            asset_id = reference.assetId,
            asset_revision = reference.assetRevision,
            result_kind = resultKind,
            reference_count = remaining,
            created_at = now
        )
        return if (remaining > 0L) {
            ServerArtworkReferenceResult.Retained(remaining)
        } else {
            val existingRelease = queries.selectServerArtworkReleaseOutbox(
                reference.assetId,
                reference.assetRevision
            ).executeAsOneOrNull()
            if (existingRelease != null) return rejectedConflict()
            queries.insertServerArtworkReleaseOutbox(
                asset_id = reference.assetId,
                asset_revision = reference.assetRevision,
                release_operation_id = operationId,
                created_at = now
            )
            ServerArtworkReferenceResult.FinalReleaseScheduled(
                ServerArtworkReleaseSignal(
                    reference.assetId,
                    reference.assetRevision,
                    operationId
                )
            )
        }
    }

    private fun com.guyghost.wakeve.Server_artwork_reference.matches(
        reference: ServerArtworkReference
    ): Boolean =
        event_id == reference.eventId &&
            asset_id == reference.assetId &&
            asset_revision == reference.assetRevision

    private fun com.guyghost.wakeve.Server_artwork_reference_operation.matches(
        reference: ServerArtworkReference
    ): Boolean =
        event_id == reference.eventId &&
            asset_id == reference.assetId &&
            asset_revision == reference.assetRevision

    private fun rejectedValidation() = ServerArtworkReferenceResult.Rejected(
        InvitationExperienceError.VALIDATION
    )

    private fun rejectedConflict() = ServerArtworkReferenceResult.Rejected(
        InvitationExperienceError.CONFLICT
    )

    private fun rejectedNotFound() = ServerArtworkReferenceResult.Rejected(
        InvitationExperienceError.NOT_FOUND
    )
}

sealed interface StudioMode {
    data object New : StudioMode
    data class EditExisting(
        val eventId: String,
        val baseRevision: Long
    ) : StudioMode
}

sealed interface StudioBaseRevision {
    data object NotApplicable : StudioBaseRevision
    data class Value(val revision: Long) : StudioBaseRevision
}

data class StudioEventFields(
    val title: String = "",
    val description: String = "",
    val deadline: String = "",
    val eventType: EventType = EventType.OTHER,
    val eventTypeCustom: String? = null,
    val minParticipants: Int? = null,
    val maxParticipants: Int? = null,
    val expectedParticipants: Int? = null,
    val proposedSlots: List<TimeSlot> = emptyList(),
    val planningMode: EventPlanningMode = EventPlanningMode.TIME_SLOT_POLL
)

sealed interface ArtworkChoice {
    data object KeepExisting : ArtworkChoice
    data object None : ArtworkChoice
    data class Preset(val presetId: String) : ArtworkChoice
    data class ExistingServerAsset(val assetId: String) : ArtworkChoice
}

data class CreationDraft(
    val draftRevision: Long,
    val fields: StudioEventFields,
    val artworkChoice: ArtworkChoice
)

/** One immutable identity owns a Studio commit from confirmation through server ACK. */
@Serializable
data class StudioCommitIdentity(
    val operationId: String,
    val draftRevision: Long
)

@Serializable
sealed interface StudioCommitSubject {
    val eventId: String

    @Serializable
    data class New(override val eventId: String) : StudioCommitSubject

    @Serializable
    data class EditExisting(
        override val eventId: String,
        val baseRevision: Long
    ) : StudioCommitSubject
}

@Serializable
data class StudioCommitRequestPayload(
    val schemaVersion: Int = 1,
    val subject: StudioCommitSubject,
    val actorId: String,
    val draftRevision: Long,
    val canonicalDraftJson: String,
    val expectedResultingArtwork: Artwork
)

@Serializable
data class StudioCommitEnvelope(
    val identity: StudioCommitIdentity,
    val requestPayload: StudioCommitRequestPayload,
    val durableOperationRef: String,
    val requestFingerprint: String,
    val maxResolutionAttempts: Int = MAX_RESOLUTION_ATTEMPTS,
    val expectedResultingArtwork: Artwork
)

@Serializable
data class StudioPendingSyncSubject(
    val schemaVersion: Int = 1,
    val eventId: String,
    val committedRevision: Long,
    val localReceiptId: String,
    val envelope: StudioCommitEnvelope,
    val expectedResultingArtwork: Artwork = envelope.expectedResultingArtwork
)

@Serializable
enum class StudioSyncOutcome { APPLIED, ALREADY_APPLIED }

@Serializable
enum class StudioCommitDisposition { CREATED, UPDATED }

@Serializable
data class StudioSyncAck(
    val localReceiptId: String,
    val serverReceiptId: String,
    val eventId: String,
    val committedRevision: Long,
    val durableOperationRef: String,
    val requestFingerprint: String,
    val outcome: StudioSyncOutcome,
    val disposition: StudioCommitDisposition,
    val artwork: Artwork
)

@Serializable
private data class StudioCanonicalSlot(
    val id: String,
    val start: String?,
    val end: String?,
    val timezone: String,
    val timeOfDay: String
)

@Serializable
private data class StudioCanonicalDraft(
    val title: String,
    val description: String,
    val deadline: String,
    val eventType: String,
    val eventTypeCustom: String?,
    val minParticipants: Int?,
    val maxParticipants: Int?,
    val expectedParticipants: Int?,
    val planningMode: String,
    val slots: List<StudioCanonicalSlot>,
    val artwork: Artwork
)

/** Pure KMP owner for the canonical request identity consumed by Swift and persistence. */
object StudioCommitEnvelopeFactory {
    private val canonicalJson = Json { encodeDefaults = true }

    fun build(command: UpdateDraftAggregateCommand): StudioCommitEnvelope {
        val payload = StudioCommitRequestPayload(
            subject = if (command.expectedBaseRevision == 0L) {
                StudioCommitSubject.New(command.eventId)
            } else {
                StudioCommitSubject.EditExisting(command.eventId, command.expectedBaseRevision)
            },
            actorId = command.actorId,
            draftRevision = command.draftRevision,
            expectedResultingArtwork = command.expectedResultingArtwork,
            canonicalDraftJson = canonicalJson.encodeToString(
                StudioCanonicalDraft(
                    title = command.eventDraft.title,
                    description = command.eventDraft.description,
                    deadline = command.eventDraft.deadline,
                    eventType = command.eventDraft.eventType.name,
                    eventTypeCustom = command.eventDraft.eventTypeCustom,
                    minParticipants = command.eventDraft.minParticipants,
                    maxParticipants = command.eventDraft.maxParticipants,
                    expectedParticipants = command.eventDraft.expectedParticipants,
                    planningMode = command.eventDraft.planningMode.name,
                    slots = command.eventDraft.proposedSlots.map { slot ->
                        StudioCanonicalSlot(
                            slot.id, slot.start, slot.end, slot.timezone, slot.timeOfDay.name
                        )
                    },
                    artwork = command.artwork
                )
            )
        )
        return build(
            identity = StudioCommitIdentity(command.operationId, command.draftRevision),
            requestPayload = payload,
            maxResolutionAttempts = MAX_RESOLUTION_ATTEMPTS
        )
    }

    fun build(
        identity: StudioCommitIdentity,
        requestPayload: StudioCommitRequestPayload,
        maxResolutionAttempts: Int
    ): StudioCommitEnvelope {
        val requestFingerprint = requestFingerprint(requestPayload)
        return StudioCommitEnvelope(
            identity = identity,
            requestPayload = requestPayload,
            durableOperationRef = "studio-operation:v1:${identity.operationId.encodeUtf8Hex()}:$requestFingerprint",
            requestFingerprint = requestFingerprint,
            maxResolutionAttempts = maxResolutionAttempts,
            expectedResultingArtwork = requestPayload.expectedResultingArtwork
        )
    }

    fun isValid(envelope: StudioCommitEnvelope): Boolean =
        envelope.identity.operationId.isNotBlank() &&
            envelope.identity.draftRevision in 0L..MAX_SAFE_INTEGER &&
            envelope.requestPayload.schemaVersion == 1 &&
            envelope.requestPayload.subject.eventId.isNotBlank() &&
            envelope.requestPayload.actorId.isNotBlank() &&
            envelope.requestPayload.draftRevision == envelope.identity.draftRevision &&
            envelope.requestPayload.canonicalDraftJson.isNotBlank() &&
            envelope.expectedResultingArtwork == envelope.requestPayload.expectedResultingArtwork &&
            canonicalArtwork(envelope.requestPayload.canonicalDraftJson) ==
                envelope.expectedResultingArtwork &&
            envelope.maxResolutionAttempts > 0 &&
            envelope.requestFingerprint == requestFingerprint(envelope.requestPayload) &&
            envelope.durableOperationRef ==
                "studio-operation:v1:${envelope.identity.operationId.encodeUtf8Hex()}:${envelope.requestFingerprint}"

    private const val MAX_SAFE_INTEGER = 9_007_199_254_740_991L

    private fun requestFingerprint(payload: StudioCommitRequestPayload): String {
        val subject = payload.subject
        val canonical = buildJsonArray {
            add(payload.schemaVersion)
            add(if (subject is StudioCommitSubject.New) "NEW" else "EDIT_EXISTING")
            add(subject.eventId)
            val edit = subject as? StudioCommitSubject.EditExisting
            if (edit == null) add("NOT_APPLICABLE") else add(edit.baseRevision)
            add(payload.actorId)
            add(payload.draftRevision)
            add(payload.canonicalDraftJson)
            add(canonicalJson.encodeToString(Artwork.serializer(), payload.expectedResultingArtwork))
        }.toString()
        return "studio-request:v1:${canonical.encodeUtf8Hex()}"
    }

    private fun canonicalArtwork(canonicalDraftJson: String): Artwork? = runCatching {
        canonicalJson.decodeFromString(StudioCanonicalDraft.serializer(), canonicalDraftJson).artwork
    }.getOrNull()

    private fun String.encodeUtf8Hex(): String = encodeToByteArray().joinToString("") { byte ->
        byte.toUByte().toString(16).padStart(2, '0')
    }
}

sealed interface PreviousStableStudioState {
    data class Idle(val mode: StudioMode) : PreviousStableStudioState
    data class Editing(
        val mode: StudioMode,
        val baseRevision: StudioBaseRevision,
        val draft: CreationDraft
    ) : PreviousStableStudioState
    data class PreviewReady(
        val mode: StudioMode,
        val baseRevision: StudioBaseRevision,
        val draft: CreationDraft,
        val artwork: Artwork
    ) : PreviousStableStudioState
    data class Previewing(
        val mode: StudioMode,
        val baseRevision: StudioBaseRevision,
        val draft: CreationDraft,
        val artwork: Artwork
    ) : PreviousStableStudioState
}

sealed interface CreationStudioState {
    data class Idle(val mode: StudioMode) : CreationStudioState
    data class LoadingExisting(
        val mode: StudioMode.EditExisting,
        val previousStableState: PreviousStableStudioState
    ) : CreationStudioState
    data class Editing(
        val mode: StudioMode,
        val baseRevision: StudioBaseRevision,
        val draft: CreationDraft
    ) : CreationStudioState
    data class ResolvingArtwork(
        val mode: StudioMode,
        val baseRevision: StudioBaseRevision,
        val draft: CreationDraft
    ) : CreationStudioState
    data class PreviewReady(
        val mode: StudioMode,
        val baseRevision: StudioBaseRevision,
        val draft: CreationDraft,
        val artwork: Artwork
    ) : CreationStudioState
    data class Previewing(
        val mode: StudioMode,
        val baseRevision: StudioBaseRevision,
        val draft: CreationDraft,
        val artwork: Artwork
    ) : CreationStudioState
    data class Committing(
        val operationId: String,
        val mode: StudioMode,
        val draft: CreationDraft,
        val artwork: Artwork,
        val durableOperationRef: String,
        val requestFingerprint: String,
        val resolutionRetryBudget: Int,
        val envelope: StudioCommitEnvelope? = null
    ) : CreationStudioState
    data class DetachedCommitting(
        val operationId: String,
        val mode: StudioMode,
        val draft: CreationDraft,
        val artwork: Artwork,
        val durableOperationRef: String,
        val requestFingerprint: String,
        val resolutionRetryBudget: Int,
        val resolutionAttempt: Int = 0,
        val lastAttemptId: String? = null,
        val lastFence: Int? = null,
        val error: InvitationExperienceError? = null,
        val envelope: StudioCommitEnvelope? = null
    ) : CreationStudioState
    data class DetachedResolving(
        val operationId: String,
        val mode: StudioMode,
        val draft: CreationDraft,
        val artwork: Artwork,
        val durableOperationRef: String,
        val requestFingerprint: String,
        val resolutionRetryBudget: Int,
        val resolutionAttempt: Int,
        val attemptId: String,
        val fence: Int,
        val error: InvitationExperienceError = InvitationExperienceError.COMMIT_OUTCOME_UNKNOWN,
        val envelope: StudioCommitEnvelope? = null
    ) : CreationStudioState
    data class DetachedCommitted(
        val eventId: String,
        val committedRevision: Long,
        val operationId: String,
        val pendingSync: Boolean,
        val envelope: StudioCommitEnvelope? = null
    ) : CreationStudioState
    data class DetachedResolutionFailed(
        val operationId: String,
        val error: InvitationExperienceError = InvitationExperienceError.RESOLUTION_OUTCOME_UNKNOWN,
        val durableOperationRef: String,
        val requestFingerprint: String,
        val resolutionRetryBudget: Int,
        val attemptId: String? = null,
        val fence: Int? = null,
        val envelope: StudioCommitEnvelope? = null
    ) : CreationStudioState
    data class PendingSync(
        val eventId: String,
        val committedRevision: Long,
        val operationId: String,
        val envelope: StudioCommitEnvelope? = null
    ) : CreationStudioState
    data class DetachedPendingSync(
        val eventId: String,
        val committedRevision: Long,
        val operationId: String,
        val binding: CreationStudioSyncBinding,
        val envelope: StudioCommitEnvelope
    ) : CreationStudioState
    data class FailedBeforeCommit(
        val error: InvitationExperienceError,
        val mode: StudioMode,
        val draft: CreationDraft,
        val operationId: String? = null,
        val artwork: Artwork? = null,
        val durableOperationRef: String? = null,
        val requestFingerprint: String? = null,
        val resolutionRetryBudget: Int = MAX_RESOLUTION_ATTEMPTS,
        val envelope: StudioCommitEnvelope? = null
    ) : CreationStudioState
    data class SyncFailed(
        val eventId: String,
        val committedRevision: Long,
        val operationId: String,
        val error: InvitationExperienceError,
        val retryable: Boolean = true,
        val code: PollBallotContract.FailureCode? = null,
        val commitOutcome: PollBallotContract.CommitOutcome? = null,
        val envelope: StudioCommitEnvelope? = null
    ) : CreationStudioState
    data class Completed(
        val eventId: String,
        val committedRevision: Long
    ) : CreationStudioState
    data object Closed : CreationStudioState
}

sealed interface CreationStudioEvent {
    data class LoadExisting(val eventId: String, val baseRevision: Long) : CreationStudioEvent
    data class ExistingLoaded(
        val eventId: String,
        val baseRevision: Long,
        val draft: CreationDraft
    ) : CreationStudioEvent
    data class UpdateFields(
        val expectedDraftRevision: Long,
        val fields: StudioEventFields
    ) : CreationStudioEvent
    data class UpdateArtwork(
        val expectedDraftRevision: Long,
        val artworkChoice: ArtworkChoice,
        val capability: ArtworkSelectionCapability = ArtworkSelectionCapability.Hidden
    ) : CreationStudioEvent
    data class RequestPreview(
        val expectedDraftRevision: Long,
        val capability: ArtworkSelectionCapability = ArtworkSelectionCapability.Hidden
    ) : CreationStudioEvent
    data class ArtworkResolved(
        val draftRevision: Long,
        val artwork: Artwork
    ) : CreationStudioEvent
    data class OpenPreview(val expectedDraftRevision: Long) : CreationStudioEvent
    data class ConfirmCommit(
        val expectedDraftRevision: Long,
        val operationId: String,
        val durableOperationRef: String = "studio-operation:$operationId",
        val requestFingerprint: String = "studio-request:$operationId:$expectedDraftRevision",
        val resolutionRetryBudget: Int = MAX_RESOLUTION_ATTEMPTS,
        val envelope: StudioCommitEnvelope? = null
    ) : CreationStudioEvent
    data class LocalCommit(
        val eventId: String,
        val draftRevision: Long,
        val committedRevision: Long,
        val operationId: String,
        val pendingSync: Boolean
    ) : CreationStudioEvent
    data class FailBeforeLocalCommit(
        val draftRevision: Long,
        val operationId: String,
        val error: InvitationExperienceError
    ) : CreationStudioEvent
    data class OutcomeUnknown(
        val draftRevision: Long,
        val operationId: String,
        val retryable: Boolean = true
    ) : CreationStudioEvent
    data class RetryResolution(
        val draftRevision: Long,
        val operationId: String,
        val attemptId: String,
        val fence: Int
    ) : CreationStudioEvent
    data class ResolutionResult(
        val draftRevision: Long,
        val operationId: String,
        val outcome: StudioResolutionOutcome,
        val eventId: String? = null,
        val committedRevision: Long? = null,
        val pendingSync: Boolean = true,
        val attemptId: String,
        val fence: Int
    ) : CreationStudioEvent
    data class RepositoryInconsistent(
        val draftRevision: Long,
        val operationId: String,
        val attemptId: String,
        val fence: Int,
        val retryable: Boolean = false
    ) : CreationStudioEvent
    data class LateLocalCommit(
        val eventId: String,
        val draftRevision: Long,
        val committedRevision: Long,
        val operationId: String,
        val pendingSync: Boolean
    ) : CreationStudioEvent
    data class SyncFailed(
        val eventId: String,
        val committedRevision: Long,
        val operationId: String,
        val error: InvitationExperienceError,
        val retryable: Boolean = true,
        val code: PollBallotContract.FailureCode? = null,
        val commitOutcome: PollBallotContract.CommitOutcome? = null
    ) : CreationStudioEvent
    data class SyncCompleted(
        val eventId: String,
        val committedRevision: Long,
        val operationId: String
    ) : CreationStudioEvent
    data class RetryBeforeCommit(val operationId: String) : CreationStudioEvent
    data class RetrySync(
        val eventId: String,
        val committedRevision: Long,
        val operationId: String
    ) : CreationStudioEvent
    data object CancelLoad : CreationStudioEvent
    data object Close : CreationStudioEvent
}

enum class StudioResolutionOutcome {
    LOCAL_COMMITTED,
    PROVEN_NOT_COMMITTED,
    UNKNOWN
}

const val MAX_RESOLUTION_ATTEMPTS: Int = 3

class CreationStudioStateMachine {
    fun transition(
        state: CreationStudioState,
        event: CreationStudioEvent
    ): CreationStudioState {
        if (state is CreationStudioState.Completed || state is CreationStudioState.Closed) {
            return state
        }

        return when (event) {
            is CreationStudioEvent.UpdateFields -> updateFields(state, event)
            is CreationStudioEvent.UpdateArtwork -> updateArtwork(state, event)
            is CreationStudioEvent.RequestPreview -> requestPreview(state, event)
            is CreationStudioEvent.ArtworkResolved -> artworkResolved(state, event)
            is CreationStudioEvent.OpenPreview -> openPreview(state, event)
            is CreationStudioEvent.ConfirmCommit -> confirmCommit(state, event)
            is CreationStudioEvent.LocalCommit -> localCommit(state, event)
            is CreationStudioEvent.FailBeforeLocalCommit -> failBeforeCommit(state, event)
            is CreationStudioEvent.OutcomeUnknown -> outcomeUnknown(state, event)
            is CreationStudioEvent.RetryResolution -> retryResolution(state, event)
            is CreationStudioEvent.ResolutionResult -> resolutionResult(state, event)
            is CreationStudioEvent.RepositoryInconsistent -> repositoryInconsistent(state, event)
            is CreationStudioEvent.LateLocalCommit -> lateLocalCommit(state, event)
            is CreationStudioEvent.SyncFailed -> syncFailed(state, event)
            is CreationStudioEvent.SyncCompleted -> syncCompleted(state, event)
            is CreationStudioEvent.RetryBeforeCommit -> retryBeforeCommit(state, event)
            is CreationStudioEvent.RetrySync -> retrySync(state, event)
            is CreationStudioEvent.ExistingLoaded -> existingLoaded(state, event)
            CreationStudioEvent.CancelLoad -> cancelLoad(state)
            CreationStudioEvent.Close -> close(state)
            is CreationStudioEvent.LoadExisting -> state
        }
    }

    private fun updateFields(
        state: CreationStudioState,
        event: CreationStudioEvent.UpdateFields
    ): CreationStudioState {
        val editable = state.editableSnapshot() ?: return state
        if (event.expectedDraftRevision != editable.draft.draftRevision) return state
        return CreationStudioState.Editing(
            mode = editable.mode,
            baseRevision = editable.baseRevision,
            draft = editable.draft.copy(
                draftRevision = editable.draft.draftRevision + 1,
                fields = event.fields
            )
        )
    }

    private fun updateArtwork(
        state: CreationStudioState,
        event: CreationStudioEvent.UpdateArtwork
    ): CreationStudioState {
        val editable = state.editableSnapshot() ?: return state
        if (event.expectedDraftRevision != editable.draft.draftRevision) return state
        if (!isReleaseOneChoiceAllowed(editable.mode, event.artworkChoice, event.capability)) return state
        return CreationStudioState.Editing(
            mode = editable.mode,
            baseRevision = editable.baseRevision,
            draft = editable.draft.copy(
                draftRevision = editable.draft.draftRevision + 1,
                artworkChoice = event.artworkChoice
            )
        )
    }

    private fun requestPreview(
        state: CreationStudioState,
        event: CreationStudioEvent.RequestPreview
    ): CreationStudioState {
        val editing = state as? CreationStudioState.Editing ?: return state
        if (event.expectedDraftRevision != editing.draft.draftRevision) return state
        if (!isCoherentBaseRevision(editing.mode, editing.baseRevision)) return state
        if (!isReleaseOneChoiceAllowed(
                editing.mode,
                editing.draft.artworkChoice,
                event.capability
            )
        ) return state
        return CreationStudioState.ResolvingArtwork(
            mode = editing.mode,
            baseRevision = editing.baseRevision,
            draft = editing.draft
        )
    }

    private fun artworkResolved(
        state: CreationStudioState,
        event: CreationStudioEvent.ArtworkResolved
    ): CreationStudioState {
        val resolving = state as? CreationStudioState.ResolvingArtwork ?: return state
        if (event.draftRevision != resolving.draft.draftRevision) return state
        return CreationStudioState.PreviewReady(
            mode = resolving.mode,
            baseRevision = resolving.baseRevision,
            draft = resolving.draft,
            artwork = event.artwork
        )
    }

    private fun openPreview(
        state: CreationStudioState,
        event: CreationStudioEvent.OpenPreview
    ): CreationStudioState {
        val ready = state as? CreationStudioState.PreviewReady ?: return state
        if (event.expectedDraftRevision != ready.draft.draftRevision) return state
        return CreationStudioState.Previewing(
            mode = ready.mode,
            baseRevision = ready.baseRevision,
            draft = ready.draft,
            artwork = ready.artwork
        )
    }

    private fun confirmCommit(
        state: CreationStudioState,
        event: CreationStudioEvent.ConfirmCommit
    ): CreationStudioState {
        val previewing = state as? CreationStudioState.Previewing ?: return state
        if (event.expectedDraftRevision != previewing.draft.draftRevision) return state
        if (event.operationId.isBlank() ||
            event.durableOperationRef.isBlank() ||
            event.requestFingerprint.isBlank() ||
            event.resolutionRetryBudget <= 0 ||
            (event.envelope != null &&
                (!StudioCommitEnvelopeFactory.isValid(event.envelope) ||
                    event.envelope.identity.operationId != event.operationId ||
                    event.envelope.identity.draftRevision != event.expectedDraftRevision ||
                    event.envelope.durableOperationRef != event.durableOperationRef ||
                    event.envelope.requestFingerprint != event.requestFingerprint ||
                    event.envelope.maxResolutionAttempts != event.resolutionRetryBudget))
        ) return state
        return CreationStudioState.Committing(
            operationId = event.operationId,
            mode = previewing.mode,
            draft = previewing.draft,
            artwork = previewing.artwork,
            durableOperationRef = event.durableOperationRef,
            requestFingerprint = event.requestFingerprint,
            resolutionRetryBudget = event.resolutionRetryBudget,
            envelope = event.envelope
        )
    }

    private fun localCommit(
        state: CreationStudioState,
        event: CreationStudioEvent.LocalCommit
    ): CreationStudioState {
        val committing = when (state) {
            is CreationStudioState.Committing -> state
            is CreationStudioState.DetachedCommitting -> return detachedCommit(state, event)
            is CreationStudioState.DetachedResolving -> return detachedCommit(state, event)
            else -> return state
        }
        if (
            event.draftRevision != committing.draft.draftRevision ||
            event.operationId != committing.operationId ||
            event.eventId.isBlank() ||
            event.committedRevision < 1 ||
            (committing.mode is StudioMode.EditExisting && committing.mode.eventId != event.eventId)
        ) return state

        return if (event.pendingSync) {
            CreationStudioState.PendingSync(
                eventId = event.eventId,
                committedRevision = event.committedRevision,
                operationId = event.operationId,
                envelope = committing.envelope
            )
        } else {
            CreationStudioState.Completed(event.eventId, event.committedRevision)
        }
    }

    private fun detachedCommit(
        state: CreationStudioState,
        event: CreationStudioEvent.LocalCommit
    ): CreationStudioState {
        val envelope = when (state) {
            is CreationStudioState.DetachedCommitting -> DetachedEnvelope(
                state.operationId, state.mode, state.draft, state.artwork,
                state.durableOperationRef, state.requestFingerprint, state.resolutionRetryBudget,
                state.envelope
            )
            is CreationStudioState.DetachedResolving -> DetachedEnvelope(
                state.operationId, state.mode, state.draft, state.artwork,
                state.durableOperationRef, state.requestFingerprint, state.resolutionRetryBudget,
                state.envelope
            )
            else -> return state
        }
        if (event.draftRevision != envelope.draft.draftRevision ||
            event.operationId != envelope.operationId ||
            event.eventId.isBlank() ||
            event.committedRevision < 1 ||
            (envelope.mode is StudioMode.EditExisting && envelope.mode.eventId != event.eventId)
        ) return state
        val durableEnvelope = envelope.durableEnvelope
        return if (event.pendingSync) {
            if (durableEnvelope != null) {
                CreationStudioState.DetachedPendingSync(
                    eventId = event.eventId,
                    committedRevision = event.committedRevision,
                    operationId = event.operationId,
                    binding = CreationStudioSyncBinding(
                        eventId = event.eventId,
                        aggregateRevision = event.committedRevision,
                        operationId = event.operationId,
                        durableOperationRef = durableEnvelope.durableOperationRef,
                        requestFingerprint = durableEnvelope.requestFingerprint
                    ),
                    envelope = durableEnvelope
                )
            } else {
                CreationStudioState.DetachedResolutionFailed(
                    operationId = event.operationId,
                    error = InvitationExperienceError.COMMIT_OUTCOME_UNKNOWN,
                    durableOperationRef = envelope.durableOperationRef,
                    requestFingerprint = envelope.requestFingerprint,
                    resolutionRetryBudget = envelope.resolutionRetryBudget
                )
            }
        } else {
            CreationStudioState.DetachedCommitted(
                eventId = event.eventId,
                committedRevision = event.committedRevision,
                operationId = event.operationId,
                pendingSync = event.pendingSync,
                envelope = durableEnvelope
            )
        }
    }

    private fun outcomeUnknown(
        state: CreationStudioState,
        event: CreationStudioEvent.OutcomeUnknown
    ): CreationStudioState {
        val envelope = when (state) {
            is CreationStudioState.Committing -> DetachedEnvelope(
                state.operationId, state.mode, state.draft, state.artwork,
                state.durableOperationRef, state.requestFingerprint, state.resolutionRetryBudget,
                state.envelope
            )
            is CreationStudioState.DetachedCommitting -> DetachedEnvelope(
                state.operationId, state.mode, state.draft, state.artwork,
                state.durableOperationRef, state.requestFingerprint, state.resolutionRetryBudget,
                state.envelope
            )
            else -> return state
        }
        if (event.draftRevision != envelope.draft.draftRevision ||
            event.operationId != envelope.operationId
        ) return state
        if (!event.retryable) {
            return CreationStudioState.DetachedResolutionFailed(
                operationId = envelope.operationId,
                error = InvitationExperienceError.COMMIT_OUTCOME_UNKNOWN,
                durableOperationRef = envelope.durableOperationRef,
                requestFingerprint = envelope.requestFingerprint,
                resolutionRetryBudget = envelope.resolutionRetryBudget,
                envelope = envelope.durableEnvelope
            )
        }
        return CreationStudioState.DetachedCommitting(
            operationId = envelope.operationId,
            mode = envelope.mode,
            draft = envelope.draft,
            artwork = envelope.artwork,
            durableOperationRef = envelope.durableOperationRef,
            requestFingerprint = envelope.requestFingerprint,
            resolutionRetryBudget = envelope.resolutionRetryBudget,
            error = InvitationExperienceError.COMMIT_OUTCOME_UNKNOWN,
            envelope = envelope.durableEnvelope
        )
    }

    private fun retryResolution(
        state: CreationStudioState,
        event: CreationStudioEvent.RetryResolution
    ): CreationStudioState {
        val detached = state as? CreationStudioState.DetachedCommitting ?: return state
        if (event.draftRevision != detached.draft.draftRevision ||
            event.operationId != detached.operationId ||
            event.attemptId.isBlank() ||
            event.fence != detached.resolutionAttempt + 1 ||
            event.attemptId == detached.lastAttemptId ||
            detached.resolutionAttempt >= detached.resolutionRetryBudget
        ) return state
        return CreationStudioState.DetachedResolving(
            operationId = detached.operationId,
            mode = detached.mode,
            draft = detached.draft,
            artwork = detached.artwork,
            durableOperationRef = detached.durableOperationRef,
            requestFingerprint = detached.requestFingerprint,
            resolutionRetryBudget = detached.resolutionRetryBudget,
            resolutionAttempt = event.fence,
            attemptId = event.attemptId,
            fence = event.fence,
            error = detached.error ?: InvitationExperienceError.COMMIT_OUTCOME_UNKNOWN,
            envelope = detached.envelope
        )
    }

    private fun resolutionResult(
        state: CreationStudioState,
        event: CreationStudioEvent.ResolutionResult
    ): CreationStudioState {
        val resolving = state as? CreationStudioState.DetachedResolving ?: return state
        if (event.draftRevision != resolving.draft.draftRevision ||
            event.operationId != resolving.operationId ||
            event.attemptId != resolving.attemptId ||
            event.fence != resolving.fence
        ) return state
        return when (event.outcome) {
            StudioResolutionOutcome.LOCAL_COMMITTED -> {
                val eventId = event.eventId ?: return state
                val revision = event.committedRevision ?: return state
                if (eventId.isBlank() || revision < 1) return state
                val durableEnvelope = resolving.envelope
                if (event.pendingSync) {
                    if (durableEnvelope != null) {
                        CreationStudioState.DetachedPendingSync(
                            eventId = eventId,
                            committedRevision = revision,
                            operationId = event.operationId,
                            binding = CreationStudioSyncBinding(
                                eventId = eventId,
                                aggregateRevision = revision,
                                operationId = event.operationId,
                                durableOperationRef = durableEnvelope.durableOperationRef,
                                requestFingerprint = durableEnvelope.requestFingerprint
                            ),
                            envelope = durableEnvelope
                        )
                    } else {
                        CreationStudioState.DetachedResolutionFailed(
                            operationId = event.operationId,
                            error = InvitationExperienceError.COMMIT_OUTCOME_UNKNOWN,
                            durableOperationRef = resolving.durableOperationRef,
                            requestFingerprint = resolving.requestFingerprint,
                            resolutionRetryBudget = resolving.resolutionRetryBudget,
                            attemptId = resolving.attemptId,
                            fence = resolving.fence
                        )
                    }
                } else {
                    CreationStudioState.DetachedCommitted(
                        eventId, revision, event.operationId, event.pendingSync, durableEnvelope
                    )
                }
            }
            StudioResolutionOutcome.PROVEN_NOT_COMMITTED -> CreationStudioState.Closed
            StudioResolutionOutcome.UNKNOWN -> if (
                resolving.resolutionAttempt < resolving.resolutionRetryBudget
            ) {
                CreationStudioState.DetachedCommitting(
                    operationId = resolving.operationId,
                    mode = resolving.mode,
                    draft = resolving.draft,
                    artwork = resolving.artwork,
                    durableOperationRef = resolving.durableOperationRef,
                    requestFingerprint = resolving.requestFingerprint,
                    resolutionRetryBudget = resolving.resolutionRetryBudget,
                    resolutionAttempt = resolving.resolutionAttempt,
                    lastAttemptId = resolving.attemptId,
                    lastFence = resolving.fence,
                    error = InvitationExperienceError.RESOLUTION_OUTCOME_UNKNOWN,
                    envelope = resolving.envelope
                )
            } else {
                CreationStudioState.DetachedResolutionFailed(
                    operationId = event.operationId,
                    error = InvitationExperienceError.RESOLUTION_OUTCOME_UNKNOWN,
                    durableOperationRef = resolving.durableOperationRef,
                    requestFingerprint = resolving.requestFingerprint,
                    resolutionRetryBudget = resolving.resolutionRetryBudget,
                    attemptId = resolving.attemptId,
                    fence = resolving.fence,
                    envelope = resolving.envelope
                )
            }
        }
    }

    private fun repositoryInconsistent(
        state: CreationStudioState,
        event: CreationStudioEvent.RepositoryInconsistent
    ): CreationStudioState {
        val resolving = state as? CreationStudioState.DetachedResolving ?: return state
        if (event.retryable ||
            event.draftRevision != resolving.draft.draftRevision ||
            event.operationId != resolving.operationId ||
            event.attemptId != resolving.attemptId ||
            event.fence != resolving.fence
        ) return state
        return CreationStudioState.DetachedResolutionFailed(
            operationId = resolving.operationId,
            error = InvitationExperienceError.COMMIT_OUTCOME_UNKNOWN,
            durableOperationRef = resolving.durableOperationRef,
            requestFingerprint = resolving.requestFingerprint,
            resolutionRetryBudget = resolving.resolutionRetryBudget,
            attemptId = resolving.attemptId,
            fence = resolving.fence,
            envelope = resolving.envelope
        )
    }

    private fun lateLocalCommit(
        state: CreationStudioState,
        event: CreationStudioEvent.LateLocalCommit
    ): CreationStudioState = localCommit(
        state,
        CreationStudioEvent.LocalCommit(
            event.eventId,
            event.draftRevision,
            event.committedRevision,
            event.operationId,
            event.pendingSync
        )
    )

    private fun close(state: CreationStudioState): CreationStudioState = when (state) {
        is CreationStudioState.Committing -> CreationStudioState.DetachedCommitting(
            state.operationId,
            state.mode,
            state.draft,
            state.artwork,
            state.durableOperationRef,
            state.requestFingerprint,
            state.resolutionRetryBudget,
            envelope = state.envelope
        )
        is CreationStudioState.DetachedCommitting,
        is CreationStudioState.DetachedResolving,
        is CreationStudioState.DetachedCommitted,
        is CreationStudioState.DetachedPendingSync,
        is CreationStudioState.DetachedResolutionFailed -> state
        is CreationStudioState.PendingSync -> {
            val envelope = state.envelope ?: return state
            CreationStudioState.DetachedPendingSync(
                eventId = state.eventId,
                committedRevision = state.committedRevision,
                operationId = state.operationId,
                binding = CreationStudioSyncBinding(
                    eventId = state.eventId,
                    aggregateRevision = state.committedRevision,
                    operationId = state.operationId,
                    durableOperationRef = envelope.durableOperationRef,
                    requestFingerprint = envelope.requestFingerprint
                ),
                envelope = envelope
            )
        }
        else -> state
    }

    private data class DetachedEnvelope(
        val operationId: String,
        val mode: StudioMode,
        val draft: CreationDraft,
        val artwork: Artwork,
        val durableOperationRef: String,
        val requestFingerprint: String,
        val resolutionRetryBudget: Int,
        val durableEnvelope: StudioCommitEnvelope?
    )

    private fun failBeforeCommit(
        state: CreationStudioState,
        event: CreationStudioEvent.FailBeforeLocalCommit
    ): CreationStudioState {
        val committing = state as? CreationStudioState.Committing ?: return state
        if (
            event.draftRevision != committing.draft.draftRevision ||
            event.operationId != committing.operationId
        ) return state
        return CreationStudioState.FailedBeforeCommit(
            error = event.error,
            mode = committing.mode,
            draft = committing.draft,
            operationId = committing.operationId,
            artwork = committing.artwork,
            durableOperationRef = committing.durableOperationRef,
            requestFingerprint = committing.requestFingerprint,
            resolutionRetryBudget = committing.resolutionRetryBudget,
            envelope = committing.envelope
        )
    }

    private fun syncFailed(
        state: CreationStudioState,
        event: CreationStudioEvent.SyncFailed
    ): CreationStudioState {
        val pending = when (state) {
            is CreationStudioState.PendingSync -> PendingStudioIdentity(
                state.eventId, state.committedRevision, state.operationId, state.envelope
            )
            is CreationStudioState.DetachedPendingSync -> PendingStudioIdentity(
                state.eventId, state.committedRevision, state.operationId, state.envelope
            )
            is CreationStudioState.SyncFailed -> PendingStudioIdentity(
                state.eventId, state.committedRevision, state.operationId, state.envelope
            )
            else -> return state
        }
        if (
            pending.eventId != event.eventId ||
            pending.committedRevision != event.committedRevision ||
            pending.operationId != event.operationId
        ) return state
        return CreationStudioState.SyncFailed(
            eventId = event.eventId,
            committedRevision = event.committedRevision,
            operationId = event.operationId,
            error = event.error,
            retryable = event.retryable,
            code = event.code,
            commitOutcome = event.commitOutcome,
            envelope = pending.envelope
        )
    }

    private fun syncCompleted(
        state: CreationStudioState,
        event: CreationStudioEvent.SyncCompleted
    ): CreationStudioState {
        val matches = when (state) {
            is CreationStudioState.PendingSync ->
                state.eventId == event.eventId &&
                    state.committedRevision == event.committedRevision &&
                    state.operationId == event.operationId
            is CreationStudioState.SyncFailed ->
                state.eventId == event.eventId &&
                    state.committedRevision == event.committedRevision &&
                    state.operationId == event.operationId
            is CreationStudioState.DetachedPendingSync ->
                state.eventId == event.eventId &&
                    state.committedRevision == event.committedRevision &&
                    state.operationId == event.operationId
            else -> false
        }
        return if (matches) {
            CreationStudioState.Completed(event.eventId, event.committedRevision)
        } else {
            state
        }
    }

    private fun retrySync(
        state: CreationStudioState,
        event: CreationStudioEvent.RetrySync
    ): CreationStudioState {
        val failed = state as? CreationStudioState.SyncFailed ?: return state
        if (
            failed.eventId != event.eventId ||
            failed.committedRevision != event.committedRevision ||
            failed.operationId != event.operationId ||
            !failed.retryable
        ) return state
        return CreationStudioState.PendingSync(
            eventId = failed.eventId,
            committedRevision = failed.committedRevision,
            operationId = failed.operationId,
            envelope = failed.envelope
        )
    }

    private data class PendingStudioIdentity(
        val eventId: String,
        val committedRevision: Long,
        val operationId: String,
        val envelope: StudioCommitEnvelope?
    )

    private fun retryBeforeCommit(
        state: CreationStudioState,
        event: CreationStudioEvent.RetryBeforeCommit
    ): CreationStudioState {
        val failed = state as? CreationStudioState.FailedBeforeCommit ?: return state
        val operationId = failed.operationId ?: return state
        val artwork = failed.artwork ?: return state
        val durableOperationRef = failed.durableOperationRef ?: return state
        val requestFingerprint = failed.requestFingerprint ?: return state
        if (operationId != event.operationId || !failed.error.isRetryableBeforeCommit()) return state
        return CreationStudioState.Committing(
            operationId = operationId,
            mode = failed.mode,
            draft = failed.draft,
            artwork = artwork,
            durableOperationRef = durableOperationRef,
            requestFingerprint = requestFingerprint,
            resolutionRetryBudget = failed.resolutionRetryBudget,
            envelope = failed.envelope
        )
    }

    private fun InvitationExperienceError.isRetryableBeforeCommit(): Boolean = when (this) {
        InvitationExperienceError.NETWORK_UNAVAILABLE,
        InvitationExperienceError.REPOSITORY_UNAVAILABLE,
        InvitationExperienceError.REMOTE_ARTWORK_UNAVAILABLE,
        InvitationExperienceError.PROVIDER_UNAVAILABLE,
        InvitationExperienceError.SERVER_UNAVAILABLE -> true
        InvitationExperienceError.NOT_FOUND,
        InvitationExperienceError.FORBIDDEN,
        InvitationExperienceError.VALIDATION,
        InvitationExperienceError.CONFLICT,
        InvitationExperienceError.COMMIT_OUTCOME_UNKNOWN,
        InvitationExperienceError.RESOLUTION_OUTCOME_UNKNOWN,
        InvitationExperienceError.PERMANENT_FAILURE -> false
    }

    private fun existingLoaded(
        state: CreationStudioState,
        event: CreationStudioEvent.ExistingLoaded
    ): CreationStudioState {
        val loading = state as? CreationStudioState.LoadingExisting ?: return state
        if (
            loading.mode.eventId != event.eventId ||
            loading.mode.baseRevision != event.baseRevision ||
            event.draft.draftRevision != 0L
        ) return state
        return CreationStudioState.Editing(
            mode = loading.mode,
            baseRevision = StudioBaseRevision.Value(event.baseRevision),
            draft = event.draft
        )
    }

    private fun cancelLoad(state: CreationStudioState): CreationStudioState {
        val loading = state as? CreationStudioState.LoadingExisting ?: return state
        return when (val previous = loading.previousStableState) {
            is PreviousStableStudioState.Idle -> CreationStudioState.Idle(previous.mode)
            is PreviousStableStudioState.Editing -> CreationStudioState.Editing(
                previous.mode,
                previous.baseRevision,
                previous.draft
            )
            is PreviousStableStudioState.PreviewReady -> CreationStudioState.PreviewReady(
                previous.mode,
                previous.baseRevision,
                previous.draft,
                previous.artwork
            )
            is PreviousStableStudioState.Previewing -> CreationStudioState.Previewing(
                previous.mode,
                previous.baseRevision,
                previous.draft,
                previous.artwork
            )
        }
    }

    private fun isReleaseOneChoiceAllowed(
        mode: StudioMode,
        choice: ArtworkChoice,
        capability: ArtworkSelectionCapability
    ): Boolean =
        when (choice) {
            ArtworkChoice.None -> true
            ArtworkChoice.KeepExisting -> mode is StudioMode.EditExisting
            is ArtworkChoice.Preset -> choice.presetId.isNotBlank()
            is ArtworkChoice.ExistingServerAsset ->
                capability is ArtworkSelectionCapability.Ready &&
                    capability.authorizedAssetsByOpaqueId.containsKey(choice.assetId)
        }

    private fun isCoherentBaseRevision(
        mode: StudioMode,
        baseRevision: StudioBaseRevision
    ): Boolean = when (mode) {
        StudioMode.New -> baseRevision == StudioBaseRevision.NotApplicable
        is StudioMode.EditExisting ->
            baseRevision is StudioBaseRevision.Value &&
                baseRevision.revision == mode.baseRevision
    }

    private data class EditableSnapshot(
        val mode: StudioMode,
        val baseRevision: StudioBaseRevision,
        val draft: CreationDraft
    )

    private fun CreationStudioState.editableSnapshot(): EditableSnapshot? = when (this) {
        is CreationStudioState.Editing -> EditableSnapshot(mode, baseRevision, draft)
        is CreationStudioState.PreviewReady -> EditableSnapshot(mode, baseRevision, draft)
        is CreationStudioState.Previewing -> EditableSnapshot(mode, baseRevision, draft)
        else -> null
    }?.takeIf { isCoherentBaseRevision(it.mode, it.baseRevision) }
}

data class UpdateDraftAggregateCommand(
    val eventId: String,
    val actorId: String,
    val expectedBaseRevision: Long,
    val eventDraft: StudioEventFields,
    val artwork: Artwork,
    val operationId: String,
    val artworkCapability: ArtworkSelectionCapability = ArtworkSelectionCapability.Hidden,
    val draftRevision: Long = expectedBaseRevision
) {
    /** Fingerprint-bound snapshot of the artwork the aggregate must expose after commit. */
    val expectedResultingArtwork: Artwork
        get() = artwork
}

sealed interface UpdateDraftAggregateResult {
    data class StudioServerReceiptProof(
        val status: String,
        val serverReceiptId: String?,
        val serverAckPayload: String
    )

    data class Committed(
        val eventId: String,
        val committedRevision: Long,
        val operationId: String,
        val pendingSync: Boolean,
        val serverAckPayload: String? = null,
        val serverReceiptProof: StudioServerReceiptProof? = null
    ) : UpdateDraftAggregateResult
    data class Rejected(
        val operationId: String,
        val error: InvitationExperienceError
    ) : UpdateDraftAggregateResult
    data class OutcomeUnknown(
        val operationId: String,
        val error: InvitationExperienceError = InvitationExperienceError.COMMIT_OUTCOME_UNKNOWN
    ) : UpdateDraftAggregateResult
}

fun interface UpdateDraftAggregateUseCase {
    suspend fun execute(command: UpdateDraftAggregateCommand): UpdateDraftAggregateResult
}

/** Optional server-side finalizer executed inside the aggregate's SQL transaction. */
fun interface StudioCommitTransactionFinalizer {
    fun finalize(
        committed: UpdateDraftAggregateResult.Committed,
        envelope: StudioCommitEnvelope,
        committedAt: String
    )
}

data class CreationStudioSyncBinding(
    val eventId: String,
    val aggregateRevision: Long,
    val operationId: String,
    val durableOperationRef: String = "",
    val requestFingerprint: String = ""
) {
    init {
        require(
            eventId.isNotBlank() && aggregateRevision > 0L && operationId.isNotBlank() &&
                (durableOperationRef.isBlank() == requestFingerprint.isBlank())
        ) {
            "Creation Studio sync binding must be complete"
        }
    }
}

sealed interface CreationStudioSyncResult {
    val binding: CreationStudioSyncBinding

    data class Pending(
        override val binding: CreationStudioSyncBinding
    ) : CreationStudioSyncResult

    data class Completed(
        override val binding: CreationStudioSyncBinding
    ) : CreationStudioSyncResult

    data class Failed(
        override val binding: CreationStudioSyncBinding,
        val error: InvitationExperienceError,
        val code: PollBallotContract.FailureCode? = null,
        val retryable: Boolean = false,
        val commitOutcome: PollBallotContract.CommitOutcome? = null
    ) : CreationStudioSyncResult
}

interface CreationStudioSyncOwner {
    suspend fun observe(binding: CreationStudioSyncBinding): CreationStudioSyncResult
    suspend fun retry(binding: CreationStudioSyncBinding): CreationStudioSyncResult
}

class DatabaseCreationStudioSyncOwner(
    private val database: WakeveDb,
    private val syncManager: SyncManager? = null
) : CreationStudioSyncOwner {
    private val studioJson = Json { encodeDefaults = true; ignoreUnknownKeys = false }
    private val immediateDispatchMutex = Mutex()
    private val immediatelyDispatchedOperations = mutableSetOf<String>()

    fun loadBinding(eventId: String, operationId: String): CreationStudioSyncBinding? {
        val receipt = database.invitationExperienceQueries
            .selectOperationReceipt(operationId, eventId)
            .executeAsOneOrNull()
            ?: return null
        if (receipt.durable_operation_ref.isBlank() || receipt.request_fingerprint.isBlank()) return null
        return CreationStudioSyncBinding(
            eventId = receipt.event_id,
            aggregateRevision = receipt.aggregate_revision,
            operationId = receipt.operation_id,
            durableOperationRef = receipt.durable_operation_ref,
            requestFingerprint = receipt.request_fingerprint
        )
    }

    override suspend fun observe(binding: CreationStudioSyncBinding): CreationStudioSyncResult {
        val observed = inspect(binding)
        if (observed is CreationStudioSyncResult.Pending) {
            val shouldDispatch = immediateDispatchMutex.withLock {
                immediatelyDispatchedOperations.add(binding.operationId)
            }
            if (shouldDispatch) {
                syncManager?.retryPendingStudioSync(binding)
                return inspect(binding)
            }
        }
        return observed
    }

    private fun inspect(binding: CreationStudioSyncBinding): CreationStudioSyncResult {
        return try {
            val receipt = database.invitationExperienceQueries
                .selectOperationReceipt(binding.operationId, binding.eventId)
                .executeAsOneOrNull()
                ?: return if (binding.hasDurableIdentity()) {
                    binding.repositoryInconsistent()
                } else {
                    binding.syncFailure(InvitationExperienceError.FORBIDDEN)
                }
            val event = database.eventQueries.selectById(binding.eventId).executeAsOneOrNull()
                ?: return binding.syncFailure(InvitationExperienceError.NOT_FOUND)
            val sync = database.syncMetadataQueries
                .selectById("studio:${binding.operationId}")
                .executeAsOneOrNull()
                ?: return if (binding.hasDurableIdentity()) {
                    binding.repositoryInconsistent()
                } else {
                    binding.syncFailure(InvitationExperienceError.REPOSITORY_UNAVAILABLE)
                }
            if (binding.hasDurableIdentity()) {
                val envelope = decodeStudioEnvelope(receipt.commit_envelope)
                    ?: return binding.repositoryInconsistent()
                val subject = decodeStudioPendingSubject(sync.payload)
                    ?: return binding.repositoryInconsistent()
                val expectedOperation = when (envelope.requestPayload.subject) {
                    is StudioCommitSubject.New -> "CREATE"
                    is StudioCommitSubject.EditExisting -> "UPDATE"
                }
                if (!StudioCommitEnvelopeFactory.isValid(envelope) ||
                    envelope.identity.operationId != binding.operationId ||
                    envelope.requestPayload.subject.eventId != binding.eventId ||
                    envelope.durableOperationRef != binding.durableOperationRef ||
                    envelope.requestFingerprint != binding.requestFingerprint ||
                    receipt.actor_id != envelope.requestPayload.actorId ||
                    receipt.durable_operation_ref != envelope.durableOperationRef ||
                    receipt.request_fingerprint != envelope.requestFingerprint ||
                    subject.schemaVersion != 1 ||
                    subject.eventId != binding.eventId ||
                    subject.committedRevision != binding.aggregateRevision ||
                    subject.localReceiptId != binding.operationId ||
                    subject.envelope != envelope ||
                    subject.expectedResultingArtwork != envelope.expectedResultingArtwork ||
                    sync.id != "studio:${binding.operationId}" ||
                    sync.operation != expectedOperation
                ) {
                    return binding.repositoryInconsistent()
                }
            }
            if (
                receipt.action != UPDATE_DRAFT_AGGREGATE_ACTION ||
                receipt.aggregate_revision != binding.aggregateRevision ||
                (binding.durableOperationRef.isNotBlank() &&
                    receipt.durable_operation_ref != binding.durableOperationRef) ||
                (binding.requestFingerprint.isNotBlank() &&
                    receipt.request_fingerprint != binding.requestFingerprint) ||
                event.aggregateRevision != binding.aggregateRevision ||
                event.aggregateSchemaVersion != SUPPORTED_AGGREGATE_SCHEMA_VERSION ||
                sync.entityId != binding.eventId ||
                sync.entityType !in setOf("event", STUDIO_SYNC_ENTITY_TYPE)
            ) {
                return if (binding.hasDurableIdentity()) {
                    binding.repositoryInconsistent()
                } else {
                    binding.syncFailure(InvitationExperienceError.FORBIDDEN)
                }
            }
            when {
                receipt.status == "COMMITTED" && sync.synced == 1L ->
                    CreationStudioSyncResult.Completed(binding)
                receipt.status == PENDING_SYNC_STATUS && sync.synced == 0L &&
                    sync.retryState == "READY" -> CreationStudioSyncResult.Pending(binding)
                sync.retryState == "PERMANENT_FAILURE" -> binding.syncFailure(
                    when (sync.rejectionCode) {
                        "FORBIDDEN" -> InvitationExperienceError.FORBIDDEN
                        "EVENT_NOT_DRAFT", "STALE_BASE_REVISION", "IDEMPOTENCY_CONFLICT" ->
                            InvitationExperienceError.CONFLICT
                        "REPOSITORY_INCONSISTENT" -> InvitationExperienceError.COMMIT_OUTCOME_UNKNOWN
                        else -> InvitationExperienceError.PERMANENT_FAILURE
                    }
                )
                sync.retryState == "CONFLICT" ->
                    binding.syncFailure(InvitationExperienceError.CONFLICT)
                else -> binding.syncFailure(InvitationExperienceError.SERVER_UNAVAILABLE)
            }
        } catch (_: Exception) {
            if (binding.hasDurableIdentity()) {
                binding.repositoryInconsistent()
            } else {
                binding.syncFailure(InvitationExperienceError.REPOSITORY_UNAVAILABLE)
            }
        }
    }

    override suspend fun retry(binding: CreationStudioSyncBinding): CreationStudioSyncResult {
        val observed = inspect(binding)
        if (
            observed is CreationStudioSyncResult.Completed ||
            (observed is CreationStudioSyncResult.Failed &&
                observed.error in setOf(
                    InvitationExperienceError.PERMANENT_FAILURE,
                    InvitationExperienceError.CONFLICT,
                    InvitationExperienceError.FORBIDDEN,
                    InvitationExperienceError.NOT_FOUND,
                    InvitationExperienceError.COMMIT_OUTCOME_UNKNOWN
                ))
        ) return observed

        val manager = syncManager
        if (manager != null) {
            manager.retryPendingStudioSync(binding)
            return inspect(binding)
        }
        // Compatibility for callers which have not yet injected transport. Production iOS injects
        // SyncManager; this adapter only preserves old local fixtures without recreating an event.
        return LegacyStudioSyncRetryAdapter(database).prepare(binding, observed)
    }

    private fun CreationStudioSyncBinding.syncFailure(error: InvitationExperienceError) =
        CreationStudioSyncResult.Failed(
            binding = this,
            error = error,
            retryable = error in setOf(
                InvitationExperienceError.NETWORK_UNAVAILABLE,
                InvitationExperienceError.SERVER_UNAVAILABLE,
                InvitationExperienceError.REPOSITORY_UNAVAILABLE
            )
        )

    private fun CreationStudioSyncBinding.repositoryInconsistent() = CreationStudioSyncResult.Failed(
        binding = this,
        error = InvitationExperienceError.COMMIT_OUTCOME_UNKNOWN,
        code = PollBallotContract.FailureCode.REPOSITORY_INCONSISTENT,
        retryable = false,
        commitOutcome = PollBallotContract.CommitOutcome.UNKNOWN
    )

    private fun CreationStudioSyncBinding.hasDurableIdentity(): Boolean =
        durableOperationRef.isNotBlank() && requestFingerprint.isNotBlank()

    private fun decodeStudioEnvelope(value: String): StudioCommitEnvelope? = try {
        studioJson.decodeFromString(StudioCommitEnvelope.serializer(), value)
    } catch (_: Exception) {
        null
    }

    private fun decodeStudioPendingSubject(value: String): StudioPendingSyncSubject? = try {
        studioJson.decodeFromString(StudioPendingSyncSubject.serializer(), value)
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val SUPPORTED_AGGREGATE_SCHEMA_VERSION = 1L
        const val UPDATE_DRAFT_AGGREGATE_ACTION = "UPDATE_DRAFT_AGGREGATE"
        const val PENDING_SYNC_STATUS = "PENDING_SYNC"
        const val STUDIO_SYNC_ENTITY_TYPE = "studio_commit"
    }
}

private class LegacyStudioSyncRetryAdapter(private val database: WakeveDb) {
    fun prepare(
        binding: CreationStudioSyncBinding,
        fallback: CreationStudioSyncResult
    ): CreationStudioSyncResult = try {
        database.transactionWithResult {
            val syncId = "studio:${binding.operationId}"
            val before = database.syncMetadataQueries.selectById(syncId).executeAsOneOrNull()
                ?: return@transactionWithResult fallback
            if (before.entityId != binding.eventId || before.synced != 0L || before.retryState != "FAILED") {
                return@transactionWithResult fallback
            }
            database.syncMetadataQueries.markStudioSyncReadyForDispatch(syncId, binding.eventId)
            CreationStudioSyncResult.Pending(binding)
        }
    } catch (_: Exception) {
        fallback
    }
}

class DatabaseUpdateDraftAggregateUseCase(
    private val database: WakeveDb
) : UpdateDraftAggregateUseCase {
    private val studioJson = Json { encodeDefaults = true; ignoreUnknownKeys = false }

    fun loadSyncBinding(eventId: String, operationId: String): CreationStudioSyncBinding? {
        val receipt = database.invitationExperienceQueries
            .selectOperationReceipt(operationId, eventId)
            .executeAsOneOrNull()
            ?: return null
        val envelope = decodeStudioEnvelope(receipt.commit_envelope) ?: return null
        val metadata = database.syncMetadataQueries
            .selectById("studio:$operationId")
            .executeAsOneOrNull()
            ?: return null
        val pendingSubject = try {
            studioJson.decodeFromString(StudioPendingSyncSubject.serializer(), metadata.payload)
        } catch (_: Exception) {
            null
        } ?: return null
        if (!StudioCommitEnvelopeFactory.isValid(envelope) ||
            receipt.durable_operation_ref != envelope.durableOperationRef ||
            receipt.request_fingerprint != envelope.requestFingerprint ||
            envelope.identity.operationId != operationId ||
            envelope.requestPayload.subject.eventId != eventId ||
            pendingSubject.schemaVersion != 1 ||
            pendingSubject.eventId != eventId ||
            pendingSubject.committedRevision != receipt.aggregate_revision ||
            pendingSubject.localReceiptId != operationId ||
            pendingSubject.envelope != envelope ||
            pendingSubject.expectedResultingArtwork != envelope.expectedResultingArtwork ||
            metadata.entityId != eventId ||
            metadata.entityType != "event"
        ) return null
        return CreationStudioSyncBinding(
            eventId = receipt.event_id,
            aggregateRevision = receipt.aggregate_revision,
            operationId = receipt.operation_id,
            durableOperationRef = envelope.durableOperationRef,
            requestFingerprint = envelope.requestFingerprint
        )
    }

    fun loadCommitEnvelope(eventId: String, operationId: String): StudioCommitEnvelope? {
        val receipt = database.invitationExperienceQueries
            .selectOperationReceipt(operationId, eventId)
            .executeAsOneOrNull()
            ?: return null
        val envelope = decodeStudioEnvelope(receipt.commit_envelope) ?: return null
        return envelope.takeIf {
            StudioCommitEnvelopeFactory.isValid(it) &&
                receipt.durable_operation_ref == it.durableOperationRef &&
                receipt.request_fingerprint == it.requestFingerprint
        }
    }

    fun loadPendingSyncSubject(eventId: String, operationId: String): StudioPendingSyncSubject? {
        val receipt = database.invitationExperienceQueries
            .selectOperationReceipt(operationId, eventId)
            .executeAsOneOrNull()
            ?: return null
        val envelope = decodeStudioEnvelope(receipt.commit_envelope) ?: return null
        val metadata = database.syncMetadataQueries.selectById("studio:$operationId")
            .executeAsOneOrNull()
            ?: return null
        val subject = try {
            studioJson.decodeFromString(StudioPendingSyncSubject.serializer(), metadata.payload)
        } catch (_: Exception) {
            null
        } ?: return null
        return subject.takeIf {
            StudioCommitEnvelopeFactory.isValid(envelope) &&
                it.schemaVersion == 1 &&
                it.eventId == eventId &&
                it.committedRevision == receipt.aggregate_revision &&
                it.localReceiptId == operationId &&
                it.envelope == envelope &&
                it.expectedResultingArtwork == envelope.expectedResultingArtwork &&
                metadata.entityType == "event" &&
                metadata.entityId == eventId &&
                receipt.durable_operation_ref == envelope.durableOperationRef &&
                receipt.request_fingerprint == envelope.requestFingerprint
        }
    }

    override suspend fun execute(
        command: UpdateDraftAggregateCommand
    ): UpdateDraftAggregateResult = executeInternal(command, null)

    suspend fun executeWithFinalizer(
        command: UpdateDraftAggregateCommand,
        transactionFinalizer: StudioCommitTransactionFinalizer
    ): UpdateDraftAggregateResult = executeInternal(command, transactionFinalizer)

    private suspend fun executeInternal(
        command: UpdateDraftAggregateCommand,
        transactionFinalizer: StudioCommitTransactionFinalizer?
    ): UpdateDraftAggregateResult {
        val persistedArtwork = command.artwork.toPersistedArtwork()
            ?: return command.rejected(InvitationExperienceError.VALIDATION)
        if (!command.isStructurallyValid() || !command.isArtworkAuthorized()) {
            return command.rejected(InvitationExperienceError.VALIDATION)
        }
        val envelope = StudioCommitEnvelopeFactory.build(command)
        if (!StudioCommitEnvelopeFactory.isValid(envelope)) {
            return command.rejected(InvitationExperienceError.VALIDATION)
        }
        val requestFingerprint = envelope.requestFingerprint

        return try {
            database.transactionWithResult {
                val receipt = database.invitationExperienceQueries
                    .selectOperationReceiptByOperationId(command.operationId)
                    .executeAsOneOrNull()
                if (receipt != null) {
                    return@transactionWithResult if (
                        receipt.event_id == command.eventId &&
                        receipt.actor_id == command.actorId &&
                        receipt.action == UPDATE_DRAFT_AGGREGATE_ACTION &&
                        receipt.request_fingerprint == requestFingerprint &&
                        receipt.durable_operation_ref == envelope.durableOperationRef &&
                        decodeStudioEnvelope(receipt.commit_envelope) == envelope
                    ) {
                        val serverReceiptProof = receipt.server_ack_payload?.let { serverAckPayload ->
                            UpdateDraftAggregateResult.StudioServerReceiptProof(
                                status = receipt.status,
                                serverReceiptId = receipt.server_receipt_id,
                                serverAckPayload = serverAckPayload
                            )
                        }
                        UpdateDraftAggregateResult.Committed(
                            eventId = receipt.event_id,
                            committedRevision = receipt.aggregate_revision,
                            operationId = receipt.operation_id,
                            pendingSync = receipt.status == PENDING_SYNC_STATUS,
                            serverAckPayload = receipt.server_ack_payload,
                            serverReceiptProof = serverReceiptProof
                        )
                    } else {
                        command.rejected(InvitationExperienceError.CONFLICT)
                    }
                }

                var event = database.eventQueries.selectById(command.eventId).executeAsOneOrNull()
                val now = Clock.System.now().toString()
                val fields = command.eventDraft
                val isNewAggregate = event == null && command.expectedBaseRevision == 0L
                if (isNewAggregate) {
                    database.eventQueries.insertEvent(
                        id = command.eventId,
                        organizerId = command.actorId,
                        title = fields.title.trim(),
                        description = fields.description.trim(),
                        status = com.guyghost.wakeve.models.EventStatus.DRAFT.name,
                        deadline = fields.deadline,
                        createdAt = now,
                        updatedAt = now,
                        version = 1L,
                        eventType = fields.eventType.name,
                        eventTypeCustom = fields.eventTypeCustom?.trim(),
                        minParticipants = fields.minParticipants?.toLong(),
                        maxParticipants = fields.maxParticipants?.toLong(),
                        expectedParticipants = fields.expectedParticipants?.toLong(),
                        isSample = 0L
                    )
                    val creationAuthorizationId = "studio-create:${command.operationId}"
                    database.invitationExperienceQueries.authorizeAggregateWrite(
                        writer_schema_version = SUPPORTED_AGGREGATE_SCHEMA_VERSION,
                        operation_id = creationAuthorizationId,
                        created_at = now,
                        id = command.eventId,
                        aggregateRevision = 1L,
                        aggregateSchemaVersion = SUPPORTED_AGGREGATE_SCHEMA_VERSION
                    )
                    database.eventQueries.setEventPlanningModeWithinAggregateWrite(
                        planningMode = fields.planningMode.name,
                        updatedAt = now,
                        id = command.eventId
                    )
                    database.invitationExperienceQueries.clearAggregateWriteAuthorization(
                        command.eventId,
                        creationAuthorizationId
                    )
                    database.participantQueries.insertParticipantWithAxes(
                        id = "org_${command.eventId}",
                        eventId = command.eventId,
                        userId = command.actorId,
                        role = "ORGANIZER",
                        hasValidatedDate = 0L,
                        rsvpState = "NOT_APPLICABLE",
                        dateValidationState = "NOT_APPLICABLE",
                        joinedAt = now,
                        updatedAt = now
                    )
                    event = database.eventQueries.selectById(command.eventId).executeAsOneOrNull()
                }
                val persistedEvent = event ?: return@transactionWithResult command.rejected(
                    InvitationExperienceError.NOT_FOUND
                )
                if (
                    persistedEvent.organizerId != command.actorId ||
                    persistedEvent.status != com.guyghost.wakeve.models.EventStatus.DRAFT.name
                ) {
                    return@transactionWithResult command.rejected(
                        InvitationExperienceError.FORBIDDEN
                    )
                }
                if (persistedEvent.aggregateSchemaVersion != SUPPORTED_AGGREGATE_SCHEMA_VERSION ||
                    (!isNewAggregate && persistedEvent.aggregateRevision != command.expectedBaseRevision) ||
                    (isNewAggregate && persistedEvent.aggregateRevision != 1L)
                ) {
                    return@transactionWithResult command.rejected(
                        InvitationExperienceError.CONFLICT
                    )
                }

                val committedRevision = command.expectedBaseRevision + 1L
                if (!isNewAggregate) {
                    database.invitationExperienceQueries.authorizeAggregateWrite(
                        writer_schema_version = SUPPORTED_AGGREGATE_SCHEMA_VERSION,
                        operation_id = "studio-aggregate:${command.operationId}",
                        created_at = now,
                        id = command.eventId,
                        aggregateRevision = command.expectedBaseRevision,
                        aggregateSchemaVersion = SUPPORTED_AGGREGATE_SCHEMA_VERSION
                    )
                    val authorization = database.invitationExperienceQueries
                        .selectAggregateWriteAuthorization(command.eventId)
                        .executeAsOneOrNull()
                    if (
                        authorization?.operation_id != "studio-aggregate:${command.operationId}" ||
                        authorization.expected_revision != command.expectedBaseRevision
                    ) {
                        return@transactionWithResult command.rejected(
                            InvitationExperienceError.CONFLICT
                        )
                    }
                    database.eventQueries.updateEvent(
                        title = fields.title.trim(),
                        description = fields.description.trim(),
                        status = com.guyghost.wakeve.models.EventStatus.DRAFT.name,
                        deadline = fields.deadline,
                        updatedAt = now,
                        eventType = fields.eventType.name,
                        eventTypeCustom = fields.eventTypeCustom?.trim(),
                        minParticipants = fields.minParticipants?.toLong(),
                        maxParticipants = fields.maxParticipants?.toLong(),
                        expectedParticipants = fields.expectedParticipants?.toLong(),
                        isSample = persistedEvent.isSample,
                        id = command.eventId
                    )
                    database.eventQueries.setEventPlanningModeWithinAggregateWrite(
                        planningMode = fields.planningMode.name,
                        updatedAt = now,
                        id = command.eventId
                    )
                    database.invitationExperienceQueries.clearAggregateWriteAuthorization(
                        command.eventId,
                        "studio-aggregate:${command.operationId}"
                    )
                }

                val committedEvent = database.eventQueries.selectById(command.eventId)
                    .executeAsOneOrNull()
                if (committedEvent?.aggregateRevision != committedRevision) {
                    rollback(command.rejected(InvitationExperienceError.CONFLICT))
                }

                val referenceOwner = DatabaseServerArtworkReferenceOwner(database)
                val previousServerReference = database.invitationExperienceQueries
                    .selectServerArtworkReferenceByEventId(command.eventId)
                    .executeAsOneOrNull()
                    ?.let { persisted ->
                        ServerArtworkReference(
                            eventId = persisted.event_id,
                            assetId = persisted.asset_id,
                            assetRevision = persisted.asset_revision
                        )
                    }
                val nextServerReference = persistedArtwork.serverAssetId?.let { assetId ->
                    val assetRevision = persistedArtwork.assetRevision
                        ?: rollback(command.rejected(InvitationExperienceError.VALIDATION))
                    ServerArtworkReference(
                        eventId = command.eventId,
                        assetId = assetId,
                        assetRevision = assetRevision
                    )
                }
                val serverReferenceChanges = previousServerReference != nextServerReference

                if (serverReferenceChanges) {
                    // releaseInTransaction intentionally requires the artwork
                    // pointer to be absent: the aggregate cannot expose an
                    // asset after relinquishing its durable ownership.
                    database.invitationExperienceQueries.deleteEventArtworkByEventId(command.eventId)
                    previousServerReference?.let { reference ->
                        val released = referenceOwner.releaseInTransaction(
                            reference = reference,
                            operationId = "studio-artwork-release:${command.operationId}"
                        )
                        if (released is ServerArtworkReferenceResult.Rejected) {
                            rollback(command.rejected(released.error))
                        }
                    }
                }

                database.timeSlotQueries.deleteByEventId(command.eventId)
                fields.proposedSlots.forEach { slot ->
                    database.timeSlotQueries.insertTimeSlot(
                        id = TimeSlotStorageIdentity.physicalId(command.eventId, slot.id),
                        eventId = command.eventId,
                        startTime = slot.start,
                        endTime = slot.end,
                        timezone = slot.timezone,
                        proposedByParticipantId = null,
                        createdAt = now,
                        updatedAt = now,
                        timeOfDay = slot.timeOfDay.name
                    )
                }

                database.invitationExperienceQueries.upsertEventArtwork(
                    event_id = command.eventId,
                    kind = persistedArtwork.kind,
                    structured_version = persistedArtwork.structuredVersion,
                    source_kind = persistedArtwork.sourceKind,
                    preset_id = persistedArtwork.presetId,
                    server_asset_id = persistedArtwork.serverAssetId,
                    canonical_https_url = persistedArtwork.canonicalHttpsUrl,
                    asset_revision = persistedArtwork.assetRevision,
                    alt_kind = persistedArtwork.altKind,
                    alt_text = persistedArtwork.altText,
                    focal_x = persistedArtwork.focalX,
                    focal_y = persistedArtwork.focalY,
                    crop = persistedArtwork.crop,
                    legacy_remote_url = persistedArtwork.legacyRemoteUrl,
                    updated_at = now
                )
                if (serverReferenceChanges) {
                    nextServerReference?.let { reference ->
                        val bound = referenceOwner.bindInTransaction(
                            reference = reference,
                            operationId = "studio-artwork-bind:${command.operationId}"
                        )
                        if (bound is ServerArtworkReferenceResult.Rejected) {
                            rollback(command.rejected(bound.error))
                        }
                    }
                }
                database.invitationExperienceQueries.insertEventOperationReceipt(
                    operation_id = command.operationId,
                    event_id = command.eventId,
                    actor_id = command.actorId,
                    action = UPDATE_DRAFT_AGGREGATE_ACTION,
                    aggregate_revision = committedRevision,
                    request_fingerprint = requestFingerprint,
                    durable_operation_ref = envelope.durableOperationRef,
                    commit_envelope = studioJson.encodeToString(envelope),
                    server_receipt_id = null,
                    status = PENDING_SYNC_STATUS,
                    created_at = now,
                    updated_at = now
                )
                val pendingSubject = StudioPendingSyncSubject(
                    eventId = command.eventId,
                    committedRevision = committedRevision,
                    localReceiptId = command.operationId,
                    envelope = envelope,
                    expectedResultingArtwork = command.expectedResultingArtwork
                )
                database.syncMetadataQueries.insertSyncMetadataWithPayload(
                    id = "studio:${command.operationId}",
                    entityType = "event",
                    entityId = command.eventId,
                    operation = if (isNewAggregate) "CREATE" else "UPDATE",
                    payload = studioJson.encodeToString(pendingSubject),
                    timestamp = now,
                    retryState = "READY",
                    retryCount = 0L,
                    synced = 0L
                )

                val committed = UpdateDraftAggregateResult.Committed(
                    eventId = command.eventId,
                    committedRevision = committedRevision,
                    operationId = command.operationId,
                    pendingSync = true
                )
                transactionFinalizer?.finalize(committed, envelope, now)
                committed
            }
        } catch (_: Exception) {
            UpdateDraftAggregateResult.OutcomeUnknown(command.operationId)
        }
    }

    private fun UpdateDraftAggregateCommand.isStructurallyValid(): Boolean {
        val fields = eventDraft
        if (
            eventId.isBlank() || actorId.isBlank() || operationId.isBlank() || draftRevision < 0L ||
            expectedBaseRevision < 0L || fields.title.isBlank() || fields.description.isBlank() ||
            runCatching { Instant.parse(fields.deadline) }.isFailure ||
            (fields.eventType == EventType.CUSTOM && fields.eventTypeCustom.isNullOrBlank()) ||
            fields.minParticipants?.let { it < 1 } == true ||
            fields.maxParticipants?.let { it < 1 } == true ||
            fields.expectedParticipants?.let { it < 1 } == true ||
            (fields.minParticipants != null && fields.maxParticipants != null &&
                fields.maxParticipants < fields.minParticipants) ||
            fields.proposedSlots.any { it.id.isBlank() || it.timezone.isBlank() || it.validate() != null } ||
            fields.proposedSlots.map { it.id }.toSet().size != fields.proposedSlots.size
        ) {
            return false
        }
        return true
    }

    private fun UpdateDraftAggregateCommand.isArtworkAuthorized(): Boolean = when (val value = artwork) {
        Artwork.None -> true
        is Artwork.LegacyRemote -> {
            val existing = database.invitationExperienceQueries
                .selectArtworkByEventId(eventId)
                .executeAsOneOrNull()
            expectedBaseRevision > 0L &&
                existing?.kind == "LEGACY_REMOTE" &&
                existing.legacy_remote_url == value.validatedHttpsUrl &&
                value.validatedHttpsUrl.isAllowedHttpsArtworkUrl()
        }
        is Artwork.Structured -> when (val source = value.ref.source) {
            is ArtworkSource.Preset -> source.presetId in RELEASE_ONE_PRESET_IDS
            is ArtworkSource.ServerAsset -> {
                val capability = artworkCapability as? ArtworkSelectionCapability.Ready
                capability != null &&
                    capability.actorId == actorId &&
                    capability.accessRevision == expectedBaseRevision &&
                    capability.authorizedAssetsByOpaqueId[source.assetId] == source
            }
        }
    }

    private fun Artwork.toPersistedArtwork(): PersistedArtwork? {
        return when (this) {
            Artwork.None -> PersistedArtwork(kind = "NONE")
            is Artwork.LegacyRemote -> if (validatedHttpsUrl.isAllowedHttpsArtworkUrl()) {
                PersistedArtwork(kind = "LEGACY_REMOTE", legacyRemoteUrl = validatedHttpsUrl)
            } else {
                null
            }
            is Artwork.Structured -> {
            if (
                version < 1 ||
                ref.focalPoint.x !in 0.0..1.0 ||
                ref.focalPoint.y !in 0.0..1.0
            ) return null
            val altKind: String
            val altText: String?
            when (val alt = ref.alt) {
                ArtworkAlt.Decorative -> {
                    altKind = "DECORATIVE"
                    altText = null
                }
                is ArtworkAlt.Informative -> {
                    if (alt.localizedText.isBlank()) return null
                    altKind = "INFORMATIVE"
                    altText = alt.localizedText.trim()
                }
            }
                when (val source = ref.source) {
                    is ArtworkSource.Preset -> {
                        if (source.presetId.isBlank()) return null
                        PersistedArtwork(
                            kind = "STRUCTURED",
                            structuredVersion = version.toLong(),
                            sourceKind = "PRESET",
                            presetId = source.presetId,
                            altKind = altKind,
                            altText = altText,
                            focalX = ref.focalPoint.x,
                            focalY = ref.focalPoint.y,
                            crop = ref.crop.name
                        )
                    }
                    is ArtworkSource.ServerAsset -> {
                        if (
                            source.assetId.isBlank() || source.assetRevision < 1L ||
                            !source.canonicalHttpsUrl.isAllowedHttpsArtworkUrl()
                        ) return null
                        PersistedArtwork(
                            kind = "STRUCTURED",
                            structuredVersion = version.toLong(),
                            sourceKind = "SERVER_ASSET",
                            serverAssetId = source.assetId,
                            canonicalHttpsUrl = source.canonicalHttpsUrl,
                            assetRevision = source.assetRevision,
                            altKind = altKind,
                            altText = altText,
                            focalX = ref.focalPoint.x,
                            focalY = ref.focalPoint.y,
                            crop = ref.crop.name
                        )
                    }
                }
            }
        }
    }

    private fun String.isAllowedHttpsArtworkUrl(): Boolean =
        ALLOWED_ARTWORK_URL.matches(this)

    private fun UpdateDraftAggregateCommand.rejected(
        error: InvitationExperienceError
    ) = UpdateDraftAggregateResult.Rejected(operationId = operationId, error = error)

    private fun decodeStudioEnvelope(value: String): StudioCommitEnvelope? = try {
        studioJson.decodeFromString(StudioCommitEnvelope.serializer(), value)
    } catch (_: Exception) {
        null
    }

    private data class PersistedArtwork(
        val kind: String,
        val structuredVersion: Long? = null,
        val sourceKind: String? = null,
        val presetId: String? = null,
        val serverAssetId: String? = null,
        val canonicalHttpsUrl: String? = null,
        val assetRevision: Long? = null,
        val altKind: String? = null,
        val altText: String? = null,
        val focalX: Double? = null,
        val focalY: Double? = null,
        val crop: String? = null,
        val legacyRemoteUrl: String? = null
    )

    private companion object {
        const val SUPPORTED_AGGREGATE_SCHEMA_VERSION = 1L
        const val UPDATE_DRAFT_AGGREGATE_ACTION = "UPDATE_DRAFT_AGGREGATE"
        const val PENDING_SYNC_STATUS = "PENDING_SYNC"
        const val STUDIO_SYNC_ENTITY_TYPE = "studio_commit"
        val RELEASE_ONE_PRESET_IDS = setOf("weekend", "wakeve-celebration")
        val ALLOWED_ARTWORK_URL = Regex(
            "^https://(?:cdn|api)\\.wakeve\\.app(?:/[^@?#]*)?$",
            RegexOption.IGNORE_CASE
        )
    }
}

sealed interface InviteDeliveryState {
    data object None : InviteDeliveryState
    data class QueuedLocal(val operationId: String) : InviteDeliveryState
    data class Submitting(val operationId: String) : InviteDeliveryState
    data class ServerAccepted(val invitationId: String) : InviteDeliveryState
    data class DeliveryPending(val invitationId: String) : InviteDeliveryState
    data class Delivered(val invitationId: String) : InviteDeliveryState
    data class FailedBeforeServer(
        val operationId: String,
        val error: InvitationExperienceError
    ) : InviteDeliveryState
    data class FailedAfterServer(
        val invitationId: String,
        val error: InvitationExperienceError
    ) : InviteDeliveryState
    data class Revoked(val invitationId: String) : InviteDeliveryState
}

sealed interface ApprovalState {
    data object NotApplicable : ApprovalState
    data class Requested(val requestId: String) : ApprovalState
    data class Approved(val requestId: String) : ApprovalState
    data class Rejected(val requestId: String) : ApprovalState
}

enum class DateValidationState {
    NOT_APPLICABLE,
    NOT_VALIDATED,
    VALIDATED_RETAINED_DATE,
    UNAVAILABLE
}

enum class GuestApprovalPolicy {
    NOT_SUPPORTED,
    AUTO_ACCEPT,
    REQUIRE_APPROVAL
}

data class AudienceIdentityAxes(
    val identityKey: String,
    val delivery: InviteDeliveryState,
    val approval: ApprovalState,
    val membership: MembershipState,
    val rsvp: RsvpState,
    val dateValidation: DateValidationState
)

data class AudienceCounts(
    val totalIdentities: Int,
    val deliveredInvitations: Int,
    val pendingApprovals: Int,
    val activeMembers: Int,
    val acceptedRsvps: Int,
    val validatedDates: Int
)

data class AudienceProjection(
    val identities: List<AudienceIdentityAxes>,
    val counts: AudienceCounts,
    val freshness: Freshness
)

class AudienceProjector {
    fun project(
        identities: List<AudienceIdentityAxes>,
        freshness: Freshness
    ): AudienceProjection = AudienceProjection(
        identities = identities,
        counts = AudienceCounts(
            totalIdentities = identities.size,
            deliveredInvitations = identities.count {
                it.delivery is InviteDeliveryState.Delivered
            },
            pendingApprovals = identities.count {
                it.approval is ApprovalState.Requested
            },
            activeMembers = identities.count {
                it.membership is MembershipState.ActiveMember
            },
            acceptedRsvps = identities.count {
                it.rsvp == RsvpState.ACCEPTED
            },
            validatedDates = identities.count {
                it.dateValidation == DateValidationState.VALIDATED_RETAINED_DATE
            }
        ),
        freshness = freshness
    )
}

sealed interface DirectInviteCapability {
    data object Hidden : DirectInviteCapability
    data class Unavailable(val reason: String) : DirectInviteCapability
    data class Ready(
        val eventId: String,
        val actorId: String,
        val accessRevision: Long,
        val allowedEventStatuses: Set<com.guyghost.wakeve.models.EventStatus>
    ) : DirectInviteCapability
}

sealed interface DirectInviteDeliveryAvailability {
    data object Available : DirectInviteDeliveryAvailability
    data class Unavailable(val reason: String) : DirectInviteDeliveryAvailability
}

interface DirectInviteCapabilityOwner {
    fun load(eventId: String, actorId: String): DirectInviteCapability
}

class DatabaseDirectInviteCapabilityOwner(
    private val database: WakeveDb,
    private val deliveryAvailability: DirectInviteDeliveryAvailability
) : DirectInviteCapabilityOwner {
    override fun load(eventId: String, actorId: String): DirectInviteCapability {
        if (deliveryAvailability is DirectInviteDeliveryAvailability.Unavailable) {
            return DirectInviteCapability.Unavailable(deliveryAvailability.reason)
        }
        val event = database.eventQueries.selectById(eventId).executeAsOneOrNull()
            ?: return DirectInviteCapability.Hidden
        if (
            event.organizerId != actorId || event.status != EventStatus.DRAFT.name ||
            event.aggregateSchemaVersion != 1L
        ) return DirectInviteCapability.Hidden
        return DirectInviteCapability.Ready(
            eventId = eventId,
            actorId = actorId,
            accessRevision = event.aggregateRevision,
            allowedEventStatuses = setOf(EventStatus.DRAFT)
        )
    }
}

data class RecipientKey(val value: String) {
    init {
        require(PROTECTED_KEY.matches(value)) {
            "RecipientKey must be an opaque identifier or a versioned keyed digest"
        }
    }

    private companion object {
        val PROTECTED_KEY = Regex(
            "hmac-v[0-9]+-[A-Fa-f0-9]{6,}|" +
                "hmac:v[0-9]+:[A-Za-z0-9._-]{8,}|" +
                "opaque:v[0-9]+:[A-Za-z0-9._-]{8,}|" +
                "opaque[-_:][A-Za-z0-9._:-]{16,}|" +
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-" +
                "[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
        )
    }
}

/** Platform/backend port holding the keyed secret; common code never owns it. */
fun interface DirectInviteRecipientDigestPort {
    fun hmacSha256(normalizedRecipient: String): String?
}

data class DirectInviteDeliveryBinding(
    val eventId: String,
    val actorId: String,
    val accessRevision: Long,
    val batchId: String,
    val operationId: String
) {
    init {
        require(
            eventId.isNotBlank() && actorId.isNotBlank() && accessRevision >= 0L &&
                batchId.isNotBlank() && operationId.isNotBlank()
        ) { "Direct-invite delivery binding must be complete" }
    }
}

data class DirectInviteDeliveryEnvelope(
    val binding: DirectInviteDeliveryBinding,
    val recipientKey: RecipientKey,
    val ciphertext: String,
    val keyVersion: Int,
    val expiresAt: String
) {
    init {
        require(ciphertext.isNotBlank() && ciphertext != recipientKey.value) {
            "Direct-invite delivery ciphertext must be opaque"
        }
        require(keyVersion > 0 && runCatching { Instant.parse(expiresAt) }.isSuccess) {
            "Direct-invite delivery retention metadata is invalid"
        }
    }
}

data class DirectInviteProtectedRecipient(
    val recipientKey: RecipientKey,
    val envelope: DirectInviteDeliveryEnvelope
)

fun interface DirectInviteDeliverySealer {
    fun seal(
        binding: DirectInviteDeliveryBinding,
        recipientKey: RecipientKey,
        normalizedRecipient: String,
        expiresAt: String
    ): DirectInviteDeliveryEnvelope?
}

data class DirectInviteDeliveryRequest(
    val binding: DirectInviteDeliveryBinding,
    val envelopes: Set<DirectInviteDeliveryEnvelope>
)

sealed interface DirectInviteDeliveryResult {
    data class Deferred(val error: InvitationExperienceError) : DirectInviteDeliveryResult
    data class Acknowledged(
        val batchId: String,
        val operationId: String,
        val outcomesByRecipientKey: Map<RecipientKey, DirectInviteRecipientOutcome>
    ) : DirectInviteDeliveryResult
    data class Rejected(val error: InvitationExperienceError) : DirectInviteDeliveryResult
}

fun interface DirectInviteDeliveryTransport {
    suspend fun dispatch(request: DirectInviteDeliveryRequest): DirectInviteDeliveryResult
}

/** Transient normalization and pseudonymization boundary for direct recipients. */
class DirectInviteRecipientKeyOwner(
    private val digestPort: DirectInviteRecipientDigestPort,
    private val keyVersion: Int = 1
) {
    fun protect(rawRecipientInput: String): RecipientKey? {
        val normalized = normalize(rawRecipientInput) ?: return null
        return protectNormalized(normalized)
    }

    fun protectAndSeal(
        rawRecipientInput: String,
        binding: DirectInviteDeliveryBinding,
        expiresAt: String,
        sealer: DirectInviteDeliverySealer
    ): DirectInviteProtectedRecipient? {
        val normalized = normalize(rawRecipientInput) ?: return null
        val recipientKey = protectNormalized(normalized) ?: return null
        val envelope = sealer.seal(binding, recipientKey, normalized, expiresAt)
            ?: return null
        if (
            envelope.binding != binding || envelope.recipientKey != recipientKey ||
            envelope.keyVersion != keyVersion || envelope.expiresAt != expiresAt ||
            envelope.ciphertext == normalized || envelope.ciphertext == recipientKey.value
        ) return null
        return DirectInviteProtectedRecipient(recipientKey, envelope)
    }

    private fun protectNormalized(normalized: String): RecipientKey? {
        if (keyVersion < 1) return null
        val digest = digestPort.hmacSha256(normalized)
            ?.trim()
            ?.lowercase()
            ?.takeIf { HEX_DIGEST.matches(it) }
            ?: return null
        return RecipientKey("hmac-v$keyVersion-$digest")
    }

    private fun normalize(raw: String): String? {
        val candidate = raw.trim()
        if (candidate.isEmpty()) return null
        if ('@' in candidate) {
            val normalized = candidate.lowercase()
            val parts = normalized.split('@')
            return normalized.takeIf {
                parts.size == 2 && parts[0].isNotBlank() && '.' in parts[1] &&
                    parts[1].substringBeforeLast('.').isNotBlank() &&
                    parts[1].substringAfterLast('.').isNotBlank() &&
                    normalized.none(Char::isWhitespace)
            }
        }
        val phone = candidate.filter { it.isDigit() }
        return phone.takeIf { it.length in 7..15 }?.let { "+$it" }
    }

    private companion object {
        val HEX_DIGEST = Regex("[a-f0-9]{32,128}")
    }
}

sealed interface DirectInviteRecipientOutcome {
    data class ServerAccepted(val invitationId: String) : DirectInviteRecipientOutcome
    data class Invalid(val reason: String) : DirectInviteRecipientOutcome
    data class Failed(val error: InvitationExperienceError) : DirectInviteRecipientOutcome
    data object Cancelled : DirectInviteRecipientOutcome
}

sealed interface DirectInviteOperation {
    data object Idle : DirectInviteOperation
    data class Submitting(
        val batchId: String,
        val operationId: String,
        val recipientKeys: Set<RecipientKey>
    ) : DirectInviteOperation
    data class PendingSync(
        val batchId: String,
        val operationId: String,
        val recipientKeys: Set<RecipientKey>
    ) : DirectInviteOperation
    data class Completed(
        val batchId: String,
        val outcomesByRecipientKey: Map<RecipientKey, DirectInviteRecipientOutcome>
    ) : DirectInviteOperation
    data class Failed(
        val batchId: String,
        val operationId: String,
        val requestedRecipientKeys: Set<RecipientKey>,
        val outcomesByRecipientKey: Map<RecipientKey, DirectInviteRecipientOutcome>,
        val batchError: InvitationExperienceError
    ) : DirectInviteOperation
    data class Cancelled(
        val batchId: String,
        val outcomesByRecipientKey: Map<RecipientKey, DirectInviteRecipientOutcome>
    ) : DirectInviteOperation
}

class DirectInviteOperationReducer {
    fun acknowledge(
        state: DirectInviteOperation,
        batchId: String,
        operationId: String,
        outcomesByRecipientKey: Map<RecipientKey, DirectInviteRecipientOutcome>
    ): DirectInviteOperation {
        val active = when (state) {
            is DirectInviteOperation.Submitting -> ActiveDirectInviteOperation(
                state.batchId,
                state.operationId,
                state.recipientKeys
            )
            is DirectInviteOperation.PendingSync -> ActiveDirectInviteOperation(
                state.batchId,
                state.operationId,
                state.recipientKeys
            )
            DirectInviteOperation.Idle,
            is DirectInviteOperation.Completed,
            is DirectInviteOperation.Failed,
            is DirectInviteOperation.Cancelled -> return state
        }
        if (
            active.batchId != batchId ||
            active.operationId != operationId ||
            active.recipientKeys != outcomesByRecipientKey.keys
        ) {
            return state
        }

        val failure = outcomesByRecipientKey.values
            .filterIsInstance<DirectInviteRecipientOutcome.Failed>()
            .firstOrNull()
        return if (failure == null) {
            DirectInviteOperation.Completed(
                batchId = active.batchId,
                outcomesByRecipientKey = outcomesByRecipientKey
            )
        } else {
            DirectInviteOperation.Failed(
                batchId = active.batchId,
                operationId = active.operationId,
                requestedRecipientKeys = active.recipientKeys,
                outcomesByRecipientKey = outcomesByRecipientKey,
                batchError = failure.error
            )
        }
    }

    private data class ActiveDirectInviteOperation(
        val batchId: String,
        val operationId: String,
        val recipientKeys: Set<RecipientKey>
    )
}

data class SubmitDirectInviteBatchCommand(
    val eventId: String,
    val actorId: String,
    val eventStatus: com.guyghost.wakeve.models.EventStatus,
    val batchId: String,
    val operationId: String,
    val recipientKeys: Set<RecipientKey>,
    val capability: DirectInviteCapability
)

data class RetryDirectInviteBatchCommand(
    val operation: DirectInviteOperation.Failed,
    val capability: DirectInviteCapability
)

data class CancelDirectInviteBatchCommand(
    val operation: DirectInviteOperation,
    val capability: DirectInviteCapability
)

data class AcknowledgeDirectInviteBatchCommand(
    val batchId: String,
    val operationId: String,
    val outcomesByRecipientKey: Map<RecipientKey, DirectInviteRecipientOutcome>,
    val capability: DirectInviteCapability
)

interface DirectInviteBatchUseCase {
    suspend fun submit(command: SubmitDirectInviteBatchCommand): DirectInviteOperation
    suspend fun retry(command: RetryDirectInviteBatchCommand): DirectInviteOperation
    suspend fun cancel(command: CancelDirectInviteBatchCommand): DirectInviteOperation
}

/**
 * Pure release-one policy seam. Persistence/delivery remains the invitation owner's
 * responsibility; this class only validates typed capabilities and derives the next operation.
 */
class DefaultDirectInviteBatchUseCase : DirectInviteBatchUseCase {
    override suspend fun submit(command: SubmitDirectInviteBatchCommand): DirectInviteOperation {
        val capability = command.capability as? DirectInviteCapability.Ready
        val authorized = capability != null &&
            command.eventStatus == com.guyghost.wakeve.models.EventStatus.DRAFT &&
            command.eventStatus in capability.allowedEventStatuses &&
            capability.eventId == command.eventId &&
            capability.actorId == command.actorId &&
            capability.accessRevision >= 0L &&
            command.batchId.isNotBlank() &&
            command.operationId.isNotBlank() &&
            command.recipientKeys.isNotEmpty()

        return if (authorized) {
            DirectInviteOperation.Submitting(
                batchId = command.batchId,
                operationId = command.operationId,
                recipientKeys = command.recipientKeys
            )
        } else {
            command.forbiddenOperation()
        }
    }

    override suspend fun retry(command: RetryDirectInviteBatchCommand): DirectInviteOperation {
        if (!command.capability.allowsDraftInvites()) return command.operation

        val retryableRecipients = command.operation.requestedRecipientKeys.filterTo(linkedSetOf()) {
            val outcome = command.operation.outcomesByRecipientKey[it]
            outcome is DirectInviteRecipientOutcome.Failed && outcome.error.isRetryableDeliveryError()
        }
        return if (retryableRecipients.isEmpty()) {
            command.operation
        } else {
            DirectInviteOperation.Submitting(
                batchId = command.operation.batchId,
                operationId = command.operation.operationId,
                recipientKeys = retryableRecipients
            )
        }
    }

    override suspend fun cancel(command: CancelDirectInviteBatchCommand): DirectInviteOperation {
        if (!command.capability.allowsDraftInvites()) return command.operation

        return when (val operation = command.operation) {
            is DirectInviteOperation.Submitting -> DirectInviteOperation.Cancelled(
                batchId = operation.batchId,
                outcomesByRecipientKey = operation.recipientKeys.associateWith {
                    DirectInviteRecipientOutcome.Cancelled
                }
            )
            is DirectInviteOperation.PendingSync -> DirectInviteOperation.Cancelled(
                batchId = operation.batchId,
                outcomesByRecipientKey = operation.recipientKeys.associateWith {
                    DirectInviteRecipientOutcome.Cancelled
                }
            )
            is DirectInviteOperation.Failed -> DirectInviteOperation.Cancelled(
                batchId = operation.batchId,
                outcomesByRecipientKey = operation.requestedRecipientKeys.associateWith { recipientKey ->
                    when (val outcome = operation.outcomesByRecipientKey[recipientKey]) {
                        is DirectInviteRecipientOutcome.ServerAccepted,
                        is DirectInviteRecipientOutcome.Invalid,
                        DirectInviteRecipientOutcome.Cancelled -> outcome
                        is DirectInviteRecipientOutcome.Failed,
                        null -> DirectInviteRecipientOutcome.Cancelled
                    }
                }
            )
            DirectInviteOperation.Idle,
            is DirectInviteOperation.Completed,
            is DirectInviteOperation.Cancelled -> operation
        }
    }

    private fun SubmitDirectInviteBatchCommand.forbiddenOperation() = DirectInviteOperation.Failed(
        batchId = batchId,
        operationId = operationId,
        requestedRecipientKeys = recipientKeys,
        outcomesByRecipientKey = recipientKeys.associateWith {
            DirectInviteRecipientOutcome.Failed(InvitationExperienceError.FORBIDDEN)
        },
        batchError = InvitationExperienceError.FORBIDDEN
    )

    private fun DirectInviteCapability.allowsDraftInvites(): Boolean =
        this is DirectInviteCapability.Ready &&
            com.guyghost.wakeve.models.EventStatus.DRAFT in allowedEventStatuses

    private fun InvitationExperienceError.isRetryableDeliveryError(): Boolean = when (this) {
        InvitationExperienceError.NETWORK_UNAVAILABLE,
        InvitationExperienceError.REPOSITORY_UNAVAILABLE,
        InvitationExperienceError.PROVIDER_UNAVAILABLE,
        InvitationExperienceError.SERVER_UNAVAILABLE -> true
        InvitationExperienceError.NOT_FOUND,
        InvitationExperienceError.FORBIDDEN,
        InvitationExperienceError.VALIDATION,
        InvitationExperienceError.CONFLICT,
        InvitationExperienceError.REMOTE_ARTWORK_UNAVAILABLE,
        InvitationExperienceError.COMMIT_OUTCOME_UNKNOWN,
        InvitationExperienceError.RESOLUTION_OUTCOME_UNKNOWN,
        InvitationExperienceError.PERMANENT_FAILURE -> false
    }
}

sealed interface SecureShareCapability {
    data object Hidden : SecureShareCapability
    data class Unavailable(val reason: String) : SecureShareCapability
    data object Loading : SecureShareCapability
    data class Ready(
        val eventId: String,
        val actorId: String,
        val accessRevision: Long,
        val capabilityId: String,
        val serverIssuedPayload: String
    ) : SecureShareCapability
    data class Failed(val reason: String) : SecureShareCapability
}

enum class EventNotificationPreference {
    INHERIT_ACCOUNT,
    ALL_EVENT_UPDATES,
    ESSENTIAL_ONLY,
    MUTED
}

enum class SystemNotificationAuthorization {
    UNAVAILABLE,
    NOT_DETERMINED,
    PROVISIONAL,
    AUTHORIZED,
    EPHEMERAL,
    DENIED,
    RESTRICTED
}

enum class EventNotificationType(
    val criticalSecurity: Boolean,
    val essentialEvent: Boolean
) {
    SECURITY_CRITICAL(criticalSecurity = true, essentialEvent = false),
    EVENT_INVITE(criticalSecurity = false, essentialEvent = true),
    VOTE_REMINDER(criticalSecurity = false, essentialEvent = true),
    DATE_CONFIRMED(criticalSecurity = false, essentialEvent = true),
    ORGANIZATION_UPDATE(criticalSecurity = false, essentialEvent = true),
    EVENT_UPDATE(criticalSecurity = false, essentialEvent = false),
    COMMENT(criticalSecurity = false, essentialEvent = false)
}

data class EventNotificationPolicyInput(
    val notificationType: EventNotificationType,
    val eventPreference: EventNotificationPreference,
    val accountEnabledTypes: Set<EventNotificationType>,
    val quietHoursActive: Boolean,
    val systemAuthorization: SystemNotificationAuthorization
)

enum class EffectiveNotificationReason {
    BLOCKED_BY_SYSTEM,
    BLOCKED_BY_ACCOUNT,
    BLOCKED_BY_EVENT,
    DEFERRED_BY_QUIET_HOURS,
    ELIGIBLE
}

data class EffectiveNotificationDecision(
    val eligible: Boolean,
    val deferred: Boolean,
    val reason: EffectiveNotificationReason
)

class EventNotificationPolicy {
    fun evaluate(input: EventNotificationPolicyInput): EffectiveNotificationDecision {
        if (!input.systemAuthorization.allowsDelivery()) {
            return blocked(EffectiveNotificationReason.BLOCKED_BY_SYSTEM)
        }
        if (input.notificationType !in input.accountEnabledTypes) {
            return blocked(EffectiveNotificationReason.BLOCKED_BY_ACCOUNT)
        }
        if (!input.notificationType.criticalSecurity && !input.eventPreference.allows(input.notificationType)) {
            return blocked(EffectiveNotificationReason.BLOCKED_BY_EVENT)
        }
        if (input.quietHoursActive && !input.notificationType.criticalSecurity) {
            return EffectiveNotificationDecision(
                eligible = true,
                deferred = true,
                reason = EffectiveNotificationReason.DEFERRED_BY_QUIET_HOURS
            )
        }
        return EffectiveNotificationDecision(
            eligible = true,
            deferred = false,
            reason = EffectiveNotificationReason.ELIGIBLE
        )
    }

    private fun SystemNotificationAuthorization.allowsDelivery(): Boolean = when (this) {
        SystemNotificationAuthorization.PROVISIONAL,
        SystemNotificationAuthorization.AUTHORIZED,
        SystemNotificationAuthorization.EPHEMERAL -> true
        SystemNotificationAuthorization.UNAVAILABLE,
        SystemNotificationAuthorization.NOT_DETERMINED,
        SystemNotificationAuthorization.DENIED,
        SystemNotificationAuthorization.RESTRICTED -> false
    }

    private fun EventNotificationPreference.allows(type: EventNotificationType): Boolean = when (this) {
        EventNotificationPreference.INHERIT_ACCOUNT,
        EventNotificationPreference.ALL_EVENT_UPDATES -> true
        EventNotificationPreference.ESSENTIAL_ONLY -> type.essentialEvent
        EventNotificationPreference.MUTED -> false
    }

    private fun blocked(reason: EffectiveNotificationReason) = EffectiveNotificationDecision(
        eligible = false,
        deferred = false,
        reason = reason
    )
}

sealed interface OperationSubject {
    data class EventNotification(val eventId: String, val userId: String) : OperationSubject
    data class Membership(val eventId: String, val memberId: String) : OperationSubject
    data class Event(val eventId: String) : OperationSubject
}

enum class InformationOperationAction {
    SAVE_EVENT_PREFERENCE,
    LEAVE_EVENT,
    REMOVE_PARTICIPANT,
    DELETE_EVENT
}

sealed interface OperationTarget {
    data class User(val userId: String) : OperationTarget
    data class Event(val eventId: String) : OperationTarget
}

data class OperationKey(
    val subject: OperationSubject,
    val action: InformationOperationAction,
    val target: OperationTarget,
    val operationId: String
)

data class EventNotificationPreferenceRecord(
    val eventId: String,
    val userId: String,
    val preference: EventNotificationPreference,
    val operationId: String,
    val pendingSync: Boolean
)

interface EventNotificationPreferenceRepository {
    suspend fun get(eventId: String, userId: String): EventNotificationPreferenceRecord?
    suspend fun save(
        operationKey: OperationKey,
        preference: EventNotificationPreference
    ): Result<EventNotificationPreferenceRecord>
}

class InMemoryEventNotificationPreferenceRepository : EventNotificationPreferenceRepository {
    private val records = mutableMapOf<Pair<String, String>, EventNotificationPreferenceRecord>()
    private val operationBindings = mutableMapOf<String, Pair<String, String>>()

    override suspend fun get(
        eventId: String,
        userId: String
    ): EventNotificationPreferenceRecord? = records[eventId to userId]

    override suspend fun save(
        operationKey: OperationKey,
        preference: EventNotificationPreference
    ): Result<EventNotificationPreferenceRecord> {
        val subject = operationKey.subject as? OperationSubject.EventNotification
            ?: return invalidOperationKey()
        val target = operationKey.target as? OperationTarget.User
            ?: return invalidOperationKey()
        if (
            operationKey.action != InformationOperationAction.SAVE_EVENT_PREFERENCE ||
            operationKey.operationId.isBlank() ||
            subject.eventId.isBlank() ||
            subject.userId.isBlank() ||
            target.userId != subject.userId ||
            operationBindings[operationKey.operationId]?.let { binding ->
                binding != (subject.eventId to subject.userId)
            } == true
        ) {
            return invalidOperationKey()
        }

        operationBindings[operationKey.operationId]?.let { binding ->
            val existing = records[binding] ?: return invalidOperationKey()
            return if (existing.preference == preference) {
                Result.success(existing)
            } else {
                invalidOperationKey()
            }
        }

        val record = EventNotificationPreferenceRecord(
            eventId = subject.eventId,
            userId = subject.userId,
            preference = preference,
            operationId = operationKey.operationId,
            pendingSync = true
        )
        records[subject.eventId to subject.userId] = record
        operationBindings[operationKey.operationId] = subject.eventId to subject.userId
        return Result.success(record)
    }

    private fun invalidOperationKey(): Result<EventNotificationPreferenceRecord> =
        Result.failure(IllegalArgumentException("Invalid event-scoped notification operation key"))
}

class InvitationExperienceRetentionRepository(
    private val database: WakeveDb
) {
    suspend fun purgeExpiredProtectedRecipientData(nowIso: String): Long {
        if (nowIso.isBlank()) return 0L
        return database.transactionWithResult {
            val count = database.invitationExperienceQueries
                .countExpiredDirectInviteRecipientOutcomes(nowIso)
                .executeAsOne()
            database.syncMetadataQueries.deleteExpiredDirectInviteSubjects(nowIso)
            database.invitationExperienceQueries
                .deleteExpiredDirectInviteRecipientOutcomes(nowIso)
            database.invitationExperienceQueries.deleteExpiredDirectInviteBatches(nowIso)
            count
        }
    }
}

sealed interface EventNotificationPreferenceWriteState {
    data class Stable(
        val record: EventNotificationPreferenceRecord?
    ) : EventNotificationPreferenceWriteState

    data class Saving(
        val operationKey: OperationKey,
        val preference: EventNotificationPreference,
        val previous: Stable
    ) : EventNotificationPreferenceWriteState

    data class PendingSync(
        val operationKey: OperationKey,
        val record: EventNotificationPreferenceRecord
    ) : EventNotificationPreferenceWriteState

    data class Failed(
        val operationKey: OperationKey,
        val preference: EventNotificationPreference,
        val error: InvitationExperienceError,
        val previous: Stable,
        val committedRecord: EventNotificationPreferenceRecord?
    ) : EventNotificationPreferenceWriteState
}

class EventNotificationPreferenceWriteReducer {
    fun cancel(
        state: EventNotificationPreferenceWriteState,
        operationKey: OperationKey
    ): EventNotificationPreferenceWriteState = when (state) {
        is EventNotificationPreferenceWriteState.Saving ->
            if (state.operationKey == operationKey) state.previous else state
        is EventNotificationPreferenceWriteState.Failed ->
            if (state.operationKey == operationKey && state.committedRecord == null) {
                state.previous
            } else {
                state
            }
        is EventNotificationPreferenceWriteState.Stable,
        is EventNotificationPreferenceWriteState.PendingSync -> state
    }

    fun acknowledge(
        state: EventNotificationPreferenceWriteState,
        operationKey: OperationKey
    ): EventNotificationPreferenceWriteState {
        if (state.operationKeyOrNull() != operationKey) return state
        return when (state) {
            is EventNotificationPreferenceWriteState.Saving -> {
                val record = state.operationKey.toPendingRecord(state.preference) ?: return state
                EventNotificationPreferenceWriteState.PendingSync(
                    operationKey = state.operationKey,
                    record = record
                )
            }
            is EventNotificationPreferenceWriteState.PendingSync ->
                EventNotificationPreferenceWriteState.Stable(
                    state.record.copy(pendingSync = false)
                )
            is EventNotificationPreferenceWriteState.Stable,
            is EventNotificationPreferenceWriteState.Failed -> state
        }
    }

    fun fail(
        state: EventNotificationPreferenceWriteState,
        operationKey: OperationKey,
        error: InvitationExperienceError
    ): EventNotificationPreferenceWriteState {
        if (state.operationKeyOrNull() != operationKey) return state
        return when (state) {
            is EventNotificationPreferenceWriteState.Saving ->
                EventNotificationPreferenceWriteState.Failed(
                    operationKey = state.operationKey,
                    preference = state.preference,
                    error = error,
                    previous = state.previous,
                    committedRecord = null
                )
            is EventNotificationPreferenceWriteState.PendingSync ->
                EventNotificationPreferenceWriteState.Failed(
                    operationKey = state.operationKey,
                    preference = state.record.preference,
                    error = error,
                    previous = EventNotificationPreferenceWriteState.Stable(
                        state.record.copy(pendingSync = false)
                    ),
                    committedRecord = state.record
                )
            is EventNotificationPreferenceWriteState.Stable,
            is EventNotificationPreferenceWriteState.Failed -> state
        }
    }

    fun retry(
        state: EventNotificationPreferenceWriteState,
        operationKey: OperationKey
    ): EventNotificationPreferenceWriteState {
        if (state !is EventNotificationPreferenceWriteState.Failed ||
            state.operationKey != operationKey
        ) {
            return state
        }
        return state.committedRecord?.let { committed ->
            EventNotificationPreferenceWriteState.PendingSync(
                operationKey = state.operationKey,
                record = committed
            )
        } ?: EventNotificationPreferenceWriteState.Saving(
            operationKey = state.operationKey,
            preference = state.preference,
            previous = state.previous
        )
    }

    private fun EventNotificationPreferenceWriteState.operationKeyOrNull(): OperationKey? =
        when (this) {
            is EventNotificationPreferenceWriteState.Saving -> operationKey
            is EventNotificationPreferenceWriteState.PendingSync -> operationKey
            is EventNotificationPreferenceWriteState.Failed -> operationKey
            is EventNotificationPreferenceWriteState.Stable -> null
        }

    private fun OperationKey.toPendingRecord(
        preference: EventNotificationPreference
    ): EventNotificationPreferenceRecord? {
        val subject = subject as? OperationSubject.EventNotification ?: return null
        val target = target as? OperationTarget.User ?: return null
        if (
            action != InformationOperationAction.SAVE_EVENT_PREFERENCE ||
            operationId.isBlank() ||
            subject.eventId.isBlank() ||
            subject.userId.isBlank() ||
            target.userId != subject.userId
        ) {
            return null
        }
        return EventNotificationPreferenceRecord(
            eventId = subject.eventId,
            userId = subject.userId,
            preference = preference,
            operationId = operationId,
            pendingSync = true
        )
    }
}

enum class InvitationExperienceCanvasAction {
    EDIT_DRAFT,
    SUBMIT_VOTE,
    VIEW_POLL_RESULTS,
    COMPARE_OPTIONS,
    CONTINUE_ORGANIZATION,
    SHOW_ACCESS_STATE,
    SHOW_DETAILS,
    VIEW_FINAL_DETAILS
}

enum class InvitationExperienceRouteCapability {
    DRAFT_EDITOR,
    POLL,
    PARTICIPANTS,
    ORGANIZATION,
    EVENT_INFORMATION,
    ARCHIVE_DETAIL
}

enum class InvitationExperienceDeepLinkIntent {
    READ,
    MUTATE
}

sealed interface InvitationExperienceRouteRequest {
    data class CanvasAction(
        val action: InvitationExperienceCanvasAction
    ) : InvitationExperienceRouteRequest

    data object Participants : InvitationExperienceRouteRequest
    data object EventInformation : InvitationExperienceRouteRequest

    data class DeepLink(
        val target: InvitationExperienceRouteCapability,
        val intent: InvitationExperienceDeepLinkIntent
    ) : InvitationExperienceRouteRequest
}

data class InvitationExperienceRouteAccess(
    val canEditDraft: Boolean = false,
    val canUsePoll: Boolean = false,
    val canReadPollResults: Boolean = false,
    val canOpenParticipants: Boolean = false,
    val canOpenOrganization: Boolean = false
)

data class InvitationExperienceRouteContext(
    val eventStatus: com.guyghost.wakeve.models.EventStatus,
    val temporalClass: TemporalClass,
    val viewerRole: ViewerRole,
    val access: InvitationExperienceRouteAccess,
    val installedRoutes: Set<InvitationExperienceRouteCapability>
)

enum class InvitationExperienceAccessDeniedReason {
    READ_ONLY,
    ROLE_NOT_PERMITTED,
    PARTICIPANT_ACCESS_REQUIRED,
    OWNER_CAPABILITY_UNAVAILABLE
}

sealed interface InvitationExperienceRouteResolution {
    data class Destination(
        val route: InvitationExperienceRouteCapability
    ) : InvitationExperienceRouteResolution

    data class LocalDetails(
        val readOnly: Boolean
    ) : InvitationExperienceRouteResolution

    data class AccessDenied(
        val reason: InvitationExperienceAccessDeniedReason
    ) : InvitationExperienceRouteResolution

    data class Unavailable(
        val requiredRoute: InvitationExperienceRouteCapability?
    ) : InvitationExperienceRouteResolution
}

class InvitationExperienceRouter {
    fun resolve(
        request: InvitationExperienceRouteRequest,
        context: InvitationExperienceRouteContext
    ): InvitationExperienceRouteResolution {
        if (
            context.temporalClass == TemporalClass.PAST ||
            context.eventStatus == com.guyghost.wakeve.models.EventStatus.FINALIZED
        ) {
            return if (
                InvitationExperienceRouteCapability.ARCHIVE_DETAIL in context.installedRoutes
            ) {
                InvitationExperienceRouteResolution.Destination(
                    InvitationExperienceRouteCapability.ARCHIVE_DETAIL
                )
            } else {
                InvitationExperienceRouteResolution.LocalDetails(readOnly = true)
            }
        }

        val deniedReason = request.deniedReason(context)
        if (deniedReason != null) {
            return InvitationExperienceRouteResolution.AccessDenied(deniedReason)
        }

        val requiredRoute = request.requiredRoute()
            ?: return InvitationExperienceRouteResolution.LocalDetails(readOnly = false)
        return if (requiredRoute in context.installedRoutes) {
            InvitationExperienceRouteResolution.Destination(requiredRoute)
        } else {
            InvitationExperienceRouteResolution.LocalDetails(readOnly = false)
        }
    }

    private fun InvitationExperienceRouteRequest.deniedReason(
        context: InvitationExperienceRouteContext
    ): InvitationExperienceAccessDeniedReason? {
        val access = context.access
        return when (this) {
            is InvitationExperienceRouteRequest.CanvasAction -> when (action) {
                InvitationExperienceCanvasAction.EDIT_DRAFT -> when {
                    context.viewerRole != ViewerRole.ORGANIZER ->
                        InvitationExperienceAccessDeniedReason.ROLE_NOT_PERMITTED
                    context.eventStatus != com.guyghost.wakeve.models.EventStatus.DRAFT ||
                        !access.canEditDraft ->
                        InvitationExperienceAccessDeniedReason.OWNER_CAPABILITY_UNAVAILABLE
                    else -> null
                }
                InvitationExperienceCanvasAction.SUBMIT_VOTE ->
                    if (access.canUsePoll) null
                    else InvitationExperienceAccessDeniedReason.PARTICIPANT_ACCESS_REQUIRED
                InvitationExperienceCanvasAction.VIEW_POLL_RESULTS ->
                    if (access.canReadPollResults) null
                    else InvitationExperienceAccessDeniedReason.PARTICIPANT_ACCESS_REQUIRED
                InvitationExperienceCanvasAction.COMPARE_OPTIONS,
                InvitationExperienceCanvasAction.CONTINUE_ORGANIZATION ->
                    if (access.canOpenOrganization) null
                    else InvitationExperienceAccessDeniedReason.PARTICIPANT_ACCESS_REQUIRED
                InvitationExperienceCanvasAction.SHOW_ACCESS_STATE,
                InvitationExperienceCanvasAction.SHOW_DETAILS,
                InvitationExperienceCanvasAction.VIEW_FINAL_DETAILS -> null
            }
            InvitationExperienceRouteRequest.Participants ->
                if (access.canOpenParticipants) null
                else InvitationExperienceAccessDeniedReason.PARTICIPANT_ACCESS_REQUIRED
            InvitationExperienceRouteRequest.EventInformation -> null
            is InvitationExperienceRouteRequest.DeepLink -> when (target) {
                InvitationExperienceRouteCapability.DRAFT_EDITOR -> when {
                    context.viewerRole != ViewerRole.ORGANIZER ->
                        InvitationExperienceAccessDeniedReason.ROLE_NOT_PERMITTED
                    context.eventStatus != com.guyghost.wakeve.models.EventStatus.DRAFT ||
                        !access.canEditDraft ->
                        InvitationExperienceAccessDeniedReason.OWNER_CAPABILITY_UNAVAILABLE
                    else -> null
                }
                InvitationExperienceRouteCapability.POLL -> {
                    val permitted = when (intent) {
                        InvitationExperienceDeepLinkIntent.READ -> access.canReadPollResults
                        InvitationExperienceDeepLinkIntent.MUTATE -> access.canUsePoll
                    }
                    if (permitted) null
                    else InvitationExperienceAccessDeniedReason.PARTICIPANT_ACCESS_REQUIRED
                }
                InvitationExperienceRouteCapability.PARTICIPANTS ->
                    if (access.canOpenParticipants) null
                    else InvitationExperienceAccessDeniedReason.PARTICIPANT_ACCESS_REQUIRED
                InvitationExperienceRouteCapability.ORGANIZATION ->
                    if (access.canOpenOrganization) null
                    else InvitationExperienceAccessDeniedReason.PARTICIPANT_ACCESS_REQUIRED
                InvitationExperienceRouteCapability.EVENT_INFORMATION -> null
                InvitationExperienceRouteCapability.ARCHIVE_DETAIL ->
                    InvitationExperienceAccessDeniedReason.READ_ONLY
            }
        }
    }

    private fun InvitationExperienceRouteRequest.requiredRoute(): InvitationExperienceRouteCapability? =
        when (this) {
            is InvitationExperienceRouteRequest.CanvasAction -> when (action) {
                InvitationExperienceCanvasAction.EDIT_DRAFT ->
                    InvitationExperienceRouteCapability.DRAFT_EDITOR
                InvitationExperienceCanvasAction.SUBMIT_VOTE,
                InvitationExperienceCanvasAction.VIEW_POLL_RESULTS ->
                    InvitationExperienceRouteCapability.POLL
                InvitationExperienceCanvasAction.COMPARE_OPTIONS,
                InvitationExperienceCanvasAction.CONTINUE_ORGANIZATION ->
                    InvitationExperienceRouteCapability.ORGANIZATION
                InvitationExperienceCanvasAction.SHOW_ACCESS_STATE,
                InvitationExperienceCanvasAction.SHOW_DETAILS,
                InvitationExperienceCanvasAction.VIEW_FINAL_DETAILS -> null
            }
            InvitationExperienceRouteRequest.Participants ->
                InvitationExperienceRouteCapability.PARTICIPANTS
            InvitationExperienceRouteRequest.EventInformation ->
                InvitationExperienceRouteCapability.EVENT_INFORMATION
            is InvitationExperienceRouteRequest.DeepLink -> target
        }
}
