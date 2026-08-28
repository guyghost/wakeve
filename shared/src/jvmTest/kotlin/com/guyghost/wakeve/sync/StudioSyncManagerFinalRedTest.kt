package com.guyghost.wakeve.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.invitationexperience.Artwork
import com.guyghost.wakeve.invitationexperience.ArtworkAlt
import com.guyghost.wakeve.invitationexperience.ArtworkCrop
import com.guyghost.wakeve.invitationexperience.ArtworkFocalPoint
import com.guyghost.wakeve.invitationexperience.ArtworkRef
import com.guyghost.wakeve.invitationexperience.ArtworkSelectionCapability
import com.guyghost.wakeve.invitationexperience.ArtworkSource
import com.guyghost.wakeve.invitationexperience.CreationStudioSyncResult
import com.guyghost.wakeve.invitationexperience.DatabaseCreationStudioSyncOwner
import com.guyghost.wakeve.invitationexperience.DatabaseUpdateDraftAggregateUseCase
import com.guyghost.wakeve.invitationexperience.StudioCommitDisposition
import com.guyghost.wakeve.invitationexperience.StudioCommitEnvelopeFactory
import com.guyghost.wakeve.invitationexperience.StudioEventFields
import com.guyghost.wakeve.invitationexperience.StudioPendingSyncSubject
import com.guyghost.wakeve.invitationexperience.StudioSyncAck
import com.guyghost.wakeve.invitationexperience.StudioSyncOutcome
import com.guyghost.wakeve.invitationexperience.UpdateDraftAggregateCommand
import com.guyghost.wakeve.invitationexperience.UpdateDraftAggregateResult
import com.guyghost.wakeve.invitationexperience.InvitationExperienceError
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.SyncConflict
import com.guyghost.wakeve.models.SyncRequest
import com.guyghost.wakeve.models.SyncResponse
import com.guyghost.wakeve.poll.PollBallotContract
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StudioSyncManagerFinalRedTest {

    @Test
    fun `local Studio commit immediately dispatches its exact pending subject`() = runBlocking {
        val fixture = fixture()
        val committed = assertIs<UpdateDraftAggregateResult.Committed>(
            fixture.aggregateOwner.execute(command())
        )
        val binding = assertNotNull(fixture.aggregateOwner.loadSyncBinding(EVENT_ID, OPERATION_ID))

        assertEquals(CreationStudioSyncResult.Pending(binding), fixture.syncOwner.observe(binding))

        val request = assertNotNull(fixture.http.requests.singleOrNull(), "Local commit must start SyncManager immediately.")
        val change = assertNotNull(request.changes.singleOrNull())
        assertEquals("studio_commit", change.table)
        assertEquals(committed.operationId, change.recordId)
        assertEquals(1L, fixture.number("SELECT COUNT(*) FROM event_operation_receipt"))
    }

    @Test
    fun `relaunch retries the persisted pending receipt idempotently without another aggregate commit`() = runBlocking {
        val fixture = fixture()
        val committed = assertIs<UpdateDraftAggregateResult.Committed>(fixture.aggregateOwner.execute(command()))
        val revision = fixture.number("SELECT aggregateRevision FROM event WHERE id = '$EVENT_ID'")
        val relaunchedOwner = DatabaseCreationStudioSyncOwner(fixture.database, fixture.syncManager)
        val binding = assertNotNull(relaunchedOwner.loadBinding(EVENT_ID, OPERATION_ID))

        relaunchedOwner.retry(binding)
        relaunchedOwner.retry(binding)

        assertEquals(2, fixture.http.requests.size, "Each explicit retry must reach transport while the exact receipt is pending.")
        assertEquals(
            fixture.http.requests[0].changes.single().data,
            fixture.http.requests[1].changes.single().data,
            "A relaunch retry must reuse the persisted command, never generate another operation."
        )
        assertEquals(committed.committedRevision, revision)
        assertEquals(1L, fixture.number("SELECT COUNT(*) FROM event_operation_receipt"))
    }

    @Test
    fun `divergent inner commit envelope is visible as terminal repository inconsistency unknown`() = runBlocking {
        val fixture = fixture()
        assertIs<UpdateDraftAggregateResult.Committed>(fixture.aggregateOwner.execute(command()))
        val divergent = StudioCommitEnvelopeFactory.build(command(operationId = "inner-other-operation"))
        fixture.driver.execute(
            identifier = null,
            sql = "UPDATE event_operation_receipt SET commit_envelope = ? WHERE operation_id = ?",
            parameters = 2
        ) {
            bindString(0, Json.encodeToString(divergent))
            bindString(1, OPERATION_ID)
        }.value

        val inconsistent = assertIs<PendingStudioSyncJoinProjection.Inconsistent>(
            fixture.syncManager.getPendingStudioSyncJoinProjections().single()
        )
        assertFalse(inconsistent.retryable)
        assertEquals(PollBallotContract.CommitOutcome.UNKNOWN, inconsistent.commitOutcome)
        val code = inconsistent.javaClass.methods
            .singleOrNull { it.name == "getCode" && it.parameterCount == 0 }
            ?.invoke(inconsistent)
        assertEquals(
            PollBallotContract.FailureCode.REPOSITORY_INCONSISTENT,
            code,
            "Envelope divergence must expose the stable repository failure code, not only a transport diagnostic."
        )
    }

    @Test
    fun `rehydration blocks the editor when the durable commit envelope is missing`() = runBlocking {
        val (fixture, binding) = committedFixture()
        fixture.execute(
            "UPDATE event_operation_receipt SET commit_envelope = '' WHERE operation_id = ?",
            OPERATION_ID
        )

        assertRepositoryInconsistentUnknown(fixture.syncOwner.observe(binding))
    }

    @Test
    fun `rehydration blocks the editor when the durable commit envelope is malformed`() = runBlocking {
        val (fixture, binding) = committedFixture()
        fixture.execute(
            "UPDATE event_operation_receipt SET commit_envelope = ? WHERE operation_id = ?",
            "{not-an-envelope",
            OPERATION_ID
        )

        assertRepositoryInconsistentUnknown(fixture.syncOwner.observe(binding))
    }

    @Test
    fun `rehydration blocks the editor when the pending Studio subject is missing`() = runBlocking {
        val (fixture, binding) = committedFixture()
        fixture.execute("DELETE FROM syncMetadata WHERE id = ?", "studio:$OPERATION_ID")

        assertRepositoryInconsistentUnknown(fixture.syncOwner.observe(binding))
    }

    @Test
    fun `rehydration blocks the editor when the pending Studio subject is malformed`() = runBlocking {
        val (fixture, binding) = committedFixture()
        fixture.execute(
            "UPDATE syncMetadata SET payload = ? WHERE id = ?",
            "{not-a-studio-subject",
            "studio:$OPERATION_ID"
        )

        assertRepositoryInconsistentUnknown(fixture.syncOwner.observe(binding))
    }

    @Test
    fun `rehydration blocks the editor when the pending subject carries a divergent inner envelope`() = runBlocking {
        val (fixture, binding) = committedFixture()
        val pending = Json.decodeFromString(
            StudioPendingSyncSubject.serializer(),
            fixture.text("SELECT payload FROM syncMetadata WHERE id = 'studio:$OPERATION_ID'")!!
        )
        val divergent = pending.copy(
            envelope = StudioCommitEnvelopeFactory.build(command(operationId = "foreign-inner-operation"))
        )
        fixture.execute(
            "UPDATE syncMetadata SET payload = ? WHERE id = ?",
            Json.encodeToString(divergent),
            "studio:$OPERATION_ID"
        )

        assertRepositoryInconsistentUnknown(fixture.syncOwner.observe(binding))
    }

    @Test
    fun `FORBIDDEN Studio rejection is correlated terminal and cannot be retried`() = runBlocking {
        assertTypedServerRejectionBecomesTerminal(
            code = "FORBIDDEN",
            expectedError = InvitationExperienceError.FORBIDDEN
        )
    }

    @Test
    fun `EVENT_NOT_DRAFT Studio rejection is correlated terminal and cannot be retried`() = runBlocking {
        assertTypedServerRejectionBecomesTerminal(
            code = "EVENT_NOT_DRAFT",
            expectedError = InvitationExperienceError.CONFLICT
        )
    }

    @Test
    fun `STALE_BASE_REVISION Studio rejection is correlated terminal and cannot be retried`() = runBlocking {
        assertTypedServerRejectionBecomesTerminal(
            code = "STALE_BASE_REVISION",
            expectedError = InvitationExperienceError.CONFLICT
        )
    }

    @Test
    fun `KEEP_EXISTING Studio commit becomes completed only for the exact disposition and resulting artwork ACK`() = runBlocking {
        val existingArtwork = presetArtwork("weekend")
        val command = command(
            operationId = "studio-keep-existing-exact",
            expectedBaseRevision = 1,
            artwork = existingArtwork
        )
        val exactHttp = CapturingHttp { request ->
            val subject = Json.decodeFromString(
                StudioPendingSyncSubject.serializer(),
                request.changes.single().data
            )
            studioAckResponse(
                subject = subject,
                disposition = StudioCommitDisposition.UPDATED,
                artwork = existingArtwork
            )
        }
        val fixture = fixture(exactHttp)
        DatabaseEventRepository(fixture.database).createEvent(existingEvent()).getOrThrow()
        assertIs<UpdateDraftAggregateResult.Committed>(fixture.aggregateOwner.execute(command))
        val binding = assertNotNull(
            fixture.aggregateOwner.loadSyncBinding(EVENT_ID, command.operationId)
        )

        assertIs<CreationStudioSyncResult.Completed>(fixture.syncOwner.observe(binding))
        assertEquals(
            "COMMITTED",
            fixture.text("SELECT status FROM event_operation_receipt WHERE operation_id = '${command.operationId}'")
        )
        assertEquals(
            1L,
            fixture.number("SELECT synced FROM syncMetadata WHERE id = 'studio:${command.operationId}'")
        )
    }

    @Test
    fun `Studio ACK with substituted disposition never marks the local receipt committed`() = runBlocking {
        assertMismatchedAckRemainsPending(
            operationId = "studio-ack-wrong-disposition",
            disposition = StudioCommitDisposition.CREATED,
            artwork = presetArtwork("weekend")
        )
    }

    @Test
    fun `Studio ACK with substituted resulting artwork never marks the local receipt committed`() = runBlocking {
        assertMismatchedAckRemainsPending(
            operationId = "studio-ack-wrong-artwork",
            disposition = StudioCommitDisposition.UPDATED,
            artwork = Artwork.None
        )
    }

    private suspend fun assertMismatchedAckRemainsPending(
        operationId: String,
        disposition: StudioCommitDisposition,
        artwork: Artwork
    ) {
        val expectedArtwork = presetArtwork("weekend")
        val http = CapturingHttp { request ->
            val subject = Json.decodeFromString(
                StudioPendingSyncSubject.serializer(),
                request.changes.single().data
            )
            studioAckResponse(subject, disposition, artwork)
        }
        val fixture = fixture(http)
        DatabaseEventRepository(fixture.database).createEvent(existingEvent()).getOrThrow()
        val edit = command(
            operationId = operationId,
            expectedBaseRevision = 1,
            artwork = expectedArtwork
        )
        assertIs<UpdateDraftAggregateResult.Committed>(fixture.aggregateOwner.execute(edit))
        val binding = assertNotNull(fixture.aggregateOwner.loadSyncBinding(EVENT_ID, operationId))

        assertIs<CreationStudioSyncResult.Pending>(
            fixture.syncOwner.observe(binding),
            "A mismatched ACK is not proof of the fingerprint-bound Studio result."
        )
        assertEquals(
            "PENDING_SYNC",
            fixture.text("SELECT status FROM event_operation_receipt WHERE operation_id = '$operationId'")
        )
        assertEquals(
            0L,
            fixture.number("SELECT synced FROM syncMetadata WHERE id = 'studio:$operationId'")
        )
    }

    private suspend fun committedFixture(http: CapturingHttp = CapturingHttp()): Pair<Fixture, com.guyghost.wakeve.invitationexperience.CreationStudioSyncBinding> {
        val fixture = fixture(http)
        assertIs<UpdateDraftAggregateResult.Committed>(fixture.aggregateOwner.execute(command()))
        val binding = assertNotNull(fixture.aggregateOwner.loadSyncBinding(EVENT_ID, OPERATION_ID))
        return fixture to binding
    }

    private fun assertRepositoryInconsistentUnknown(result: CreationStudioSyncResult) {
        val failed = assertIs<CreationStudioSyncResult.Failed>(
            result,
            "A corrupt durable record is terminal and must never restore the editable Studio."
        )
        assertEquals(InvitationExperienceError.COMMIT_OUTCOME_UNKNOWN, failed.error)
        assertEquals(
            PollBallotContract.FailureCode.REPOSITORY_INCONSISTENT,
            reflectedValue(failed, "Code")
        )
        assertEquals(PollBallotContract.CommitOutcome.UNKNOWN, reflectedValue(failed, "CommitOutcome"))
        assertEquals(false, reflectedValue(failed, "Retryable"))
    }

    private suspend fun assertTypedServerRejectionBecomesTerminal(
        code: String,
        expectedError: InvitationExperienceError
    ) {
        val http = CapturingHttp { request ->
            val change = request.changes.single()
            SyncResponse(
                success = true,
                appliedChanges = 0,
                conflicts = listOf(
                    SyncConflict(
                        changeId = change.id,
                        table = change.table,
                        recordId = change.recordId,
                        clientData = change.data,
                        serverData = "",
                        resolution = "REJECTED",
                        code = code,
                        retryable = false
                    )
                ),
                serverTimestamp = "2030-01-01T00:00:00Z"
            )
        }
        val (fixture, binding) = committedFixture(http)

        val observed = assertIs<CreationStudioSyncResult.Failed>(fixture.syncOwner.observe(binding))
        assertEquals(expectedError, observed.error, "The typed server code must survive client correlation.")
        assertEquals("PERMANENT_FAILURE", fixture.text("SELECT retryState FROM syncMetadata WHERE id = 'studio:$OPERATION_ID'"))
        assertEquals(0L, fixture.number("SELECT synced FROM syncMetadata WHERE id = 'studio:$OPERATION_ID'"))
        val requestCount = http.requests.size

        val retried = assertIs<CreationStudioSyncResult.Failed>(fixture.syncOwner.retry(binding))
        assertEquals(expectedError, retried.error)
        assertEquals(requestCount, http.requests.size, "A typed non-retryable rejection must never be dispatched again.")
    }

    private fun reflectedValue(instance: Any, suffix: String): Any? = instance.javaClass.methods
        .singleOrNull { it.name == "get$suffix" && it.parameterCount == 0 }
        ?.invoke(instance)

    private fun fixture(http: CapturingHttp = CapturingHttp()): Fixture {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WakeveDb.Schema.create(driver)
        val database = WakeveDb(driver)
        val repository = DatabaseEventRepository(database)
        val network = OnlineNetwork()
        val manager = SyncManager(
            database = database,
            eventRepository = repository,
            userRepository = UserRepository(database),
            networkDetector = network,
            httpClient = http,
            authTokenProvider = { "studio-token" },
            maxRetries = 0
        )
        return Fixture(
            driver,
            database,
            DatabaseUpdateDraftAggregateUseCase(database),
            manager,
            DatabaseCreationStudioSyncOwner(database, manager),
            http
        )
    }

    private fun command(
        operationId: String = OPERATION_ID,
        expectedBaseRevision: Long = 0,
        artwork: Artwork = Artwork.None
    ) = UpdateDraftAggregateCommand(
        eventId = EVENT_ID,
        actorId = ACTOR_ID,
        expectedBaseRevision = expectedBaseRevision,
        eventDraft = StudioEventFields(
            title = "Studio durable sync",
            description = "One local commit owns one durable server command.",
            deadline = "2099-01-01T00:00:00Z",
            eventType = EventType.OTHER
        ),
        artwork = artwork,
        operationId = operationId,
        artworkCapability = ArtworkSelectionCapability.Hidden,
        draftRevision = 0
    )

    private fun existingEvent() = Event(
        id = EVENT_ID,
        title = "Existing Studio draft",
        description = "The KEEP_EXISTING snapshot owns this artwork.",
        organizerId = ACTOR_ID,
        proposedSlots = emptyList(),
        deadline = "2099-01-01T00:00:00Z",
        status = EventStatus.DRAFT,
        createdAt = "2030-01-01T00:00:00Z",
        updatedAt = "2030-01-01T00:00:00Z"
    )

    private fun presetArtwork(presetId: String) = Artwork.Structured(
        version = 1,
        ref = ArtworkRef(
            source = ArtworkSource.Preset(presetId),
            alt = ArtworkAlt.Decorative,
            focalPoint = ArtworkFocalPoint(0.5, 0.5),
            crop = ArtworkCrop.FILL
        )
    )

    private fun studioAckResponse(
        subject: StudioPendingSyncSubject,
        disposition: StudioCommitDisposition,
        artwork: Artwork
    ) = SyncResponse(
        success = true,
        appliedChanges = 1,
        studioAcknowledgements = listOf(
            StudioSyncAck(
                localReceiptId = subject.localReceiptId,
                serverReceiptId = "server:${subject.localReceiptId}",
                eventId = subject.eventId,
                committedRevision = subject.committedRevision,
                durableOperationRef = subject.envelope.durableOperationRef,
                requestFingerprint = subject.envelope.requestFingerprint,
                outcome = StudioSyncOutcome.APPLIED,
                disposition = disposition,
                artwork = artwork
            )
        ),
        serverTimestamp = "2030-01-01T00:00:00Z"
    )

    private class OnlineNetwork : NetworkStatusDetector {
        override val isNetworkAvailable: StateFlow<Boolean> = MutableStateFlow(true)
    }

    private class CapturingHttp(
        private val response: (SyncRequest) -> SyncResponse = {
            SyncResponse(
                success = true,
                appliedChanges = 1,
                serverTimestamp = "2030-01-01T00:00:00Z"
            )
        }
    ) : SyncHttpClient {
        val requests = mutableListOf<SyncRequest>()
        override suspend fun sync(requestJson: String, authToken: String): Result<String> = runCatching {
            val request = Json.decodeFromString(SyncRequest.serializer(), requestJson)
            requests += request
            Json.encodeToString(response(request))
        }
    }

    private data class Fixture(
        val driver: JdbcSqliteDriver,
        val database: WakeveDb,
        val aggregateOwner: DatabaseUpdateDraftAggregateUseCase,
        val syncManager: SyncManager,
        val syncOwner: DatabaseCreationStudioSyncOwner,
        val http: CapturingHttp
    ) {
        fun execute(sql: String, vararg values: String) {
            driver.execute(
                identifier = null,
                sql = sql,
                parameters = values.size
            ) {
                values.forEachIndexed { index, value -> bindString(index, value) }
            }.value
        }

        fun number(sql: String): Long? = driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                app.cash.sqldelight.db.QueryResult.Value(cursor.getLong(0))
            },
            parameters = 0
        ).value

        fun text(sql: String): String? = driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                app.cash.sqldelight.db.QueryResult.Value(cursor.getString(0))
            },
            parameters = 0
        ).value
    }

    private companion object {
        const val EVENT_ID = "studio-immediate-sync-event"
        const val ACTOR_ID = "studio-immediate-sync-actor"
        const val OPERATION_ID = "studio-immediate-sync-operation"
    }
}
