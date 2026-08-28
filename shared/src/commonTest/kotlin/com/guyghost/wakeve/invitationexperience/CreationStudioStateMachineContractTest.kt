package com.guyghost.wakeve.invitationexperience

import com.guyghost.wakeve.models.TimeSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CreationStudioStateMachineContractTest {
    private val machine = CreationStudioStateMachine()

    @Test
    fun `new mode uses not-applicable base revision and invalidates preview on every edit`() {
        val initial = editing(
            mode = StudioMode.New,
            baseRevision = StudioBaseRevision.NotApplicable,
            revision = 0,
            fields = validFields(title = "Before"),
            artworkChoice = ArtworkChoice.None
        )

        val afterFields = assertIs<CreationStudioState.Editing>(
            machine.transition(
                initial,
                CreationStudioEvent.UpdateFields(
                    expectedDraftRevision = 0,
                    fields = validFields(title = "After")
                )
            )
        )
        assertEquals(StudioBaseRevision.NotApplicable, afterFields.baseRevision)
        assertEquals(1L, afterFields.draft.draftRevision)
        assertEquals("After", afterFields.draft.fields.title)

        val previewing = CreationStudioState.Previewing(
            mode = StudioMode.New,
            baseRevision = StudioBaseRevision.NotApplicable,
            draft = afterFields.draft,
            artwork = Artwork.None
        )
        val invalidatedPreview = assertIs<CreationStudioState.Editing>(
            machine.transition(
                previewing,
                CreationStudioEvent.UpdateArtwork(
                    expectedDraftRevision = 1,
                    artworkChoice = ArtworkChoice.Preset("celebration")
                )
            )
        )
        assertEquals(2L, invalidatedPreview.draft.draftRevision)
        assertEquals(ArtworkChoice.Preset("celebration"), invalidatedPreview.draft.artworkChoice)
    }

    @Test
    fun `keep existing is edit-only while none and preset remain valid in new mode`() {
        val newState = editing(
            mode = StudioMode.New,
            baseRevision = StudioBaseRevision.NotApplicable,
            revision = 3,
            artworkChoice = ArtworkChoice.None
        )
        assertEquals(
            newState,
            machine.transition(
                newState,
                CreationStudioEvent.UpdateArtwork(3, ArtworkChoice.KeepExisting)
            ),
            "KEEP_EXISTING must fail closed in NEW mode without changing the draft."
        )

        val preset = assertIs<CreationStudioState.Editing>(
            machine.transition(
                newState,
                CreationStudioEvent.UpdateArtwork(3, ArtworkChoice.Preset("dinner"))
            )
        )
        assertEquals(ArtworkChoice.Preset("dinner"), preset.draft.artworkChoice)
        assertEquals(4L, preset.draft.draftRevision)

        val editState = editing(
            mode = StudioMode.EditExisting("event-1", 7),
            baseRevision = StudioBaseRevision.Value(7),
            revision = 0,
            artworkChoice = ArtworkChoice.None
        )
        val keepExisting = assertIs<CreationStudioState.Editing>(
            machine.transition(
                editState,
                CreationStudioEvent.UpdateArtwork(0, ArtworkChoice.KeepExisting)
            )
        )
        assertEquals(ArtworkChoice.KeepExisting, keepExisting.draft.artworkChoice)
        assertEquals(1L, keepExisting.draft.draftRevision)
    }

    @Test
    fun `edit load cancellation restores exact state and stale load callbacks are ignored`() {
        val mode = StudioMode.EditExisting("event-1", 7)
        val captured = PreviousStableStudioState.Idle(mode)
        val loading = CreationStudioState.LoadingExisting(mode, captured)

        assertEquals(
            CreationStudioState.Idle(mode),
            machine.transition(loading, CreationStudioEvent.CancelLoad)
        )
        assertEquals(
            loading,
            machine.transition(
                loading,
                CreationStudioEvent.ExistingLoaded(
                    eventId = "other-event",
                    baseRevision = 7,
                    draft = draft(0)
                )
            )
        )

        val loaded = assertIs<CreationStudioState.Editing>(
            machine.transition(
                loading,
                CreationStudioEvent.ExistingLoaded(
                    eventId = "event-1",
                    baseRevision = 7,
                    draft = draft(0, artworkChoice = ArtworkChoice.KeepExisting)
                )
            )
        )
        assertEquals(StudioBaseRevision.Value(7), loaded.baseRevision)
        assertEquals(0L, loaded.draft.draftRevision)
    }

    @Test
    fun `preview and commit accept only the current draft revision and operation`() {
        val editing = editing(
            mode = StudioMode.New,
            baseRevision = StudioBaseRevision.NotApplicable,
            revision = 4,
            artworkChoice = ArtworkChoice.Preset("weekend")
        )
        val resolving = assertIs<CreationStudioState.ResolvingArtwork>(
            machine.transition(editing, CreationStudioEvent.RequestPreview(4))
        )
        assertEquals(
            resolving,
            machine.transition(resolving, CreationStudioEvent.ArtworkResolved(3, Artwork.None)),
            "A stale artwork callback must not replace current preview data."
        )

        val previewReady = assertIs<CreationStudioState.PreviewReady>(
            machine.transition(
                resolving,
                CreationStudioEvent.ArtworkResolved(
                    draftRevision = 4,
                    artwork = presetArtwork("weekend")
                )
            )
        )
        val previewing = assertIs<CreationStudioState.Previewing>(
            machine.transition(previewReady, CreationStudioEvent.OpenPreview(4))
        )
        assertEquals(
            previewing,
            machine.transition(previewing, CreationStudioEvent.ConfirmCommit(3, "operation-1"))
        )

        val committing = assertIs<CreationStudioState.Committing>(
            machine.transition(previewing, CreationStudioEvent.ConfirmCommit(4, "operation-1"))
        )
        assertEquals(
            committing,
            machine.transition(
                committing,
                CreationStudioEvent.LocalCommit(
                    eventId = "event-1",
                    draftRevision = 4,
                    committedRevision = 1,
                    operationId = "stale-operation",
                    pendingSync = true
                )
            )
        )

        assertEquals(
            CreationStudioState.PendingSync("event-1", 1, "operation-1"),
            machine.transition(
                committing,
                CreationStudioEvent.LocalCommit(
                    eventId = "event-1",
                    draftRevision = 4,
                    committedRevision = 1,
                    operationId = "operation-1",
                    pendingSync = true
                )
            )
        )
    }

    @Test
    fun `server asset selection requires the current owner supplied opaque capability`() {
        val serverAsset = ArtworkSource.ServerAsset(
            assetId = "asset-1",
            canonicalHttpsUrl = "https://cdn.wakeve.app/assets/asset-1.jpg",
            assetRevision = 9
        )
        val state = editing(
            mode = StudioMode.EditExisting("event-1", 7),
            baseRevision = StudioBaseRevision.Value(7),
            revision = 2,
            artworkChoice = ArtworkChoice.KeepExisting
        )
        val choice = ArtworkChoice.ExistingServerAsset("asset-1")

        assertEquals(
            state,
            machine.transition(
                state,
                CreationStudioEvent.UpdateArtwork(
                    expectedDraftRevision = 2,
                    artworkChoice = choice,
                    capability = ArtworkSelectionCapability.Hidden
                )
            )
        )

        val selected = assertIs<CreationStudioState.Editing>(
            machine.transition(
                state,
                CreationStudioEvent.UpdateArtwork(
                    expectedDraftRevision = 2,
                    artworkChoice = choice,
                    capability = ArtworkSelectionCapability.Ready(
                        actorId = "organizer-1",
                        accessRevision = 4,
                        authorizedAssetsByOpaqueId = mapOf("asset-1" to serverAsset)
                    )
                )
            )
        )
        assertEquals(3L, selected.draft.draftRevision)
        assertEquals(choice, selected.draft.artworkChoice)

        assertEquals(
            selected,
            machine.transition(
                selected,
                CreationStudioEvent.RequestPreview(
                    expectedDraftRevision = 3,
                    capability = ArtworkSelectionCapability.Unavailable("STALE_BINDING")
                )
            ),
            "A missing or stale asset capability must preserve the draft for later review."
        )
    }

    @Test
    fun `post commit retry replays only the matching sync operation`() {
        val failed = CreationStudioState.SyncFailed(
            eventId = "event-1",
            committedRevision = 8,
            operationId = "operation-1",
            error = InvitationExperienceError.NETWORK_UNAVAILABLE
        )

        assertEquals(
            failed,
            machine.transition(
                failed,
                CreationStudioEvent.RetrySync("event-1", 8, "other-operation")
            )
        )
        assertEquals(
            CreationStudioState.PendingSync("event-1", 8, "operation-1"),
            machine.transition(
                failed,
                CreationStudioEvent.RetrySync("event-1", 8, "operation-1")
            )
        )
    }

    @Test
    fun `pre commit retry preserves the exact reviewed draft and stable idempotency key`() {
        val draft = draft(
            revision = 6,
            artworkChoice = ArtworkChoice.Preset("weekend")
        )
        val previewing = CreationStudioState.Previewing(
            mode = StudioMode.EditExisting("event-1", 4),
            baseRevision = StudioBaseRevision.Value(4),
            draft = draft,
            artwork = presetArtwork("weekend")
        )
        val committing = assertIs<CreationStudioState.Committing>(
            machine.transition(
                previewing,
                CreationStudioEvent.ConfirmCommit(
                    expectedDraftRevision = 6,
                    operationId = "operation-1"
                )
            )
        )
        val failed = assertIs<CreationStudioState.FailedBeforeCommit>(
            machine.transition(
                committing,
                CreationStudioEvent.FailBeforeLocalCommit(
                    draftRevision = 6,
                    operationId = "operation-1",
                    error = InvitationExperienceError.NETWORK_UNAVAILABLE
                )
            )
        )

        assertEquals(failed, machine.transition(failed, CreationStudioEvent.RetryBeforeCommit("other-operation")))
        assertEquals(
            committing,
            machine.transition(failed, CreationStudioEvent.RetryBeforeCommit("operation-1")),
            "Retry must reuse the reviewed draft, resolved artwork, mode, and stable operation id."
        )

        val permanentFailure = failed.copy(error = InvitationExperienceError.PERMANENT_FAILURE)
        assertEquals(
            permanentFailure,
            machine.transition(permanentFailure, CreationStudioEvent.RetryBeforeCommit("operation-1")),
            "A permanent pre-commit failure must not enter an automatic retry loop."
        )
    }

    @Test
    fun `completed and closed are terminal`() {
        val completed = CreationStudioState.Completed("event-1", 8)
        assertEquals(completed, machine.transition(completed, CreationStudioEvent.Close))
        assertEquals(
            CreationStudioState.Closed,
            machine.transition(CreationStudioState.Closed, CreationStudioEvent.UpdateFields(0, validFields()))
        )
    }

    @Test
    fun `close during commit detaches and a second close is consumed without cancellation`() {
        val committing = committingState("operation-detach")

        val detached = machine.transition(committing, CreationStudioEvent.Close)

        assertEquals(
            "DetachedCommitting",
            detached::class.simpleName,
            "Close must detach presentation while preserving the durable commit envelope."
        )
        assertEquals(
            detached,
            machine.transition(detached, CreationStudioEvent.Close),
            "A repeated close during detached work is consumed and cannot cancel the operation."
        )
    }

    @Test
    fun `commit gate close is inert when no durable commit is active`() {
        val idle = CreationStudioState.Idle(StudioMode.New)

        assertEquals(
            idle,
            machine.transition(idle, CreationStudioEvent.Close),
            "Presentation dismissal outside a commit must not fabricate a durable terminal workflow state."
        )
    }

    @Test
    fun `late local commit after close exposes detached pending binding for immediate sync`() {
        val legacyCommitting = committingState("operation-late")
        val envelope = StudioCommitEnvelopeFactory.build(
            UpdateDraftAggregateCommand(
                eventId = "event-late",
                actorId = "actor-late",
                expectedBaseRevision = 0,
                eventDraft = legacyCommitting.draft.fields,
                artwork = legacyCommitting.artwork,
                operationId = legacyCommitting.operationId,
                draftRevision = legacyCommitting.draft.draftRevision
            )
        )
        val committing = legacyCommitting.copy(
            durableOperationRef = envelope.durableOperationRef,
            requestFingerprint = envelope.requestFingerprint,
            resolutionRetryBudget = envelope.maxResolutionAttempts,
            envelope = envelope
        )
        val detached = machine.transition(committing, CreationStudioEvent.Close)

        val late = machine.transition(
            detached,
            CreationStudioEvent.LocalCommit(
                eventId = "event-late",
                draftRevision = committing.draft.draftRevision,
                committedRevision = 1,
                operationId = committing.operationId,
                pendingSync = true
            )
        )

        val pending = assertIs<CreationStudioState.DetachedPendingSync>(
            late,
            "A late local receipt must remain visibly pending and provide the exact durable sync subject."
        )
        assertEquals("event-late", pending.binding.eventId)
        assertEquals(1L, pending.binding.aggregateRevision)
        assertEquals(committing.operationId, pending.binding.operationId)
        assertEquals(envelope.durableOperationRef, pending.binding.durableOperationRef)
        assertEquals(envelope.requestFingerprint, pending.binding.requestFingerprint)
        assertEquals(envelope, pending.envelope)
        assertIs<CreationStudioState.Completed>(
            machine.transition(
                pending,
                CreationStudioEvent.SyncCompleted(
                    eventId = pending.binding.eventId,
                    committedRevision = pending.binding.aggregateRevision,
                    operationId = pending.binding.operationId
                )
            ),
            "The binding exposed to the immediate trigger must accept its correlated sync ACK."
        )
    }

    @Test
    fun `initial commit unknown and resolution attempt unknown remain distinct failure codes`() {
        val codes = InvitationExperienceError.entries.map { it.name }.toSet()

        assertTrue("COMMIT_OUTCOME_UNKNOWN" in codes)
        assertTrue("RESOLUTION_OUTCOME_UNKNOWN" in codes)
        assertTrue(
            "COMMIT_OUTCOME_UNKNOWN" != "RESOLUTION_OUTCOME_UNKNOWN",
            "An initial persistence uncertainty cannot be folded into a resolution retry failure."
        )
    }

    private fun committingState(operationId: String): CreationStudioState.Committing {
        val previewing = CreationStudioState.Previewing(
            mode = StudioMode.New,
            baseRevision = StudioBaseRevision.NotApplicable,
            draft = draft(9, ArtworkChoice.Preset("weekend")),
            artwork = presetArtwork("weekend")
        )
        return assertIs(
            machine.transition(
                previewing,
                CreationStudioEvent.ConfirmCommit(9, operationId)
            )
        )
    }

    private fun editing(
        mode: StudioMode,
        baseRevision: StudioBaseRevision,
        revision: Long,
        fields: StudioEventFields = validFields(),
        artworkChoice: ArtworkChoice
    ) = CreationStudioState.Editing(
        mode = mode,
        baseRevision = baseRevision,
        draft = CreationDraft(revision, fields, artworkChoice)
    )

    private fun draft(
        revision: Long,
        artworkChoice: ArtworkChoice = ArtworkChoice.None
    ) = CreationDraft(revision, validFields(), artworkChoice)

    private fun validFields(title: String = "Weekend") = StudioEventFields(
        title = title,
        description = "A planned event",
        deadline = "2030-05-31T18:00:00Z",
        proposedSlots = listOf(
            TimeSlot(
                id = "slot-1",
                start = "2030-06-20T10:00:00Z",
                end = "2030-06-20T12:00:00Z",
                timezone = "UTC"
            )
        )
    )

    private fun presetArtwork(id: String) = Artwork.Structured(
        version = 1,
        ref = ArtworkRef(
            source = ArtworkSource.Preset(id),
            alt = ArtworkAlt.Decorative,
            focalPoint = ArtworkFocalPoint(0.5, 0.5),
            crop = ArtworkCrop.FILL
        )
    )
}
