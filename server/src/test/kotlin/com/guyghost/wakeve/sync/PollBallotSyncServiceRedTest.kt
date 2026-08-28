package com.guyghost.wakeve.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.SyncChange
import com.guyghost.wakeve.models.SyncOperation
import com.guyghost.wakeve.models.SyncRequest
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.TimeSlotStorageIdentity
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PollBallotSyncServiceRedTest {

    @BeforeTest
    fun setup() {
        DatabaseProvider.resetDatabase()
    }

    @AfterTest
    fun teardown() {
        DatabaseProvider.resetDatabase()
    }

    @Test
    fun `server accepts one complete poll ballot and returns its correlated acknowledgement`() = runBlocking {
        val fixture = fixture()

        val response = fixture.service.processSyncChanges(
            request(fixture.pollRevision),
            ACTOR_ID
        )

        assertTrue(response.success, response.message.orEmpty())
        assertEquals(1, response.appliedChanges, response.conflicts.toString())
        assertTrue(response.conflicts.isEmpty(), response.conflicts.toString())
        assertEquals(
            mapOf("slot-1" to "YES", "slot-2" to "MAYBE"),
            persistedVotes(fixture.database)
        )

        val acknowledgement = assertNotNull(
            ballotAcknowledgements(response).singleOrNull(),
            "The server must acknowledge the exact local receipt and command tuple."
        )
        assertEquals(LOCAL_RECEIPT_ID, acknowledgementValue(acknowledgement, "LocalReceiptId"))
        assertTrue(acknowledgementValue(acknowledgement, "ServerReceiptId")?.toString()?.isNotBlank() == true)
        assertEquals(FINGERPRINT, acknowledgementValue(acknowledgement, "BallotFingerprint"))
    }

    @Test
    fun `server ballot replay is idempotent and returns the same durable acknowledgement`() = runBlocking {
        val fixture = fixture()
        val request = request(fixture.pollRevision)

        val first = fixture.service.processSyncChanges(request, ACTOR_ID)
        val replay = SyncService(fixture.database).processSyncChanges(request, ACTOR_ID)

        assertEquals(1, first.appliedChanges, first.conflicts.toString())
        assertEquals(1, replay.appliedChanges, replay.conflicts.toString())
        val firstAck = assertNotNull(ballotAcknowledgements(first).singleOrNull())
        val replayAck = assertNotNull(ballotAcknowledgements(replay).singleOrNull())
        assertEquals(
            acknowledgementValue(firstAck, "ServerReceiptId"),
            acknowledgementValue(replayAck, "ServerReceiptId")
        )
        assertEquals(2, persistedVotes(fixture.database).size)
    }

    @Test
    fun `server revalidates actor deadline status and poll revision`() = runBlocking {
        suspend fun rejected(
            authenticatedUser: String = ACTOR_ID,
            actorId: String = ACTOR_ID,
            pollRevisionDelta: Long = 0,
            eventTransform: (Event) -> Event = { it }
        ) {
            val fixture = fixture(eventTransform)
            val response = fixture.service.processSyncChanges(
                request(
                    pollRevision = fixture.pollRevision + pollRevisionDelta,
                    actorId = actorId,
                    changeUserId = authenticatedUser
                ),
                authenticatedUser
            )
            assertEquals(0, response.appliedChanges)
            assertEquals(1, response.conflicts.size)
            assertTrue(persistedVotes(fixture.database).isEmpty())
            assertTrue(ballotAcknowledgements(response).isEmpty())
        }

        rejected(authenticatedUser = "intruder", actorId = "intruder")
        rejected(pollRevisionDelta = 1)
        rejected(eventTransform = { it.copy(status = EventStatus.DRAFT) })
        rejected(eventTransform = { it.copy(deadline = "invalid-deadline") })
        rejected(eventTransform = { it.copy(deadline = "2000-01-01T00:00:00Z") })
    }

    @Test
    fun `server admits accepted active member and rejects non accepted membership axes`() = runBlocking {
        val accepted = fixture()
        insertMember(accepted.database, "accepted-member", "ACCEPTED")
        val acceptedResponse = accepted.service.processSyncChanges(
            request(
                pollRevision = accepted.pollRevision,
                actorId = "accepted-member",
                changeUserId = "accepted-member"
            ),
            "accepted-member"
        )
        assertEquals(1, acceptedResponse.appliedChanges, acceptedResponse.conflicts.toString())
        assertTrue(acceptedResponse.conflicts.isEmpty())

        for (rsvp in listOf("PENDING", "DECLINED", "UNAVAILABLE")) {
            val rejected = fixture()
            val actorId = "${rsvp.lowercase()}-member"
            insertMember(rejected.database, actorId, rsvp)
            val response = rejected.service.processSyncChanges(
                request(
                    pollRevision = rejected.pollRevision,
                    actorId = actorId,
                    changeUserId = actorId
                ),
                actorId
            )
            assertEquals(0, response.appliedChanges, "rsvp=$rsvp")
            assertEquals(1, response.conflicts.size, "rsvp=$rsvp")
            assertTrue(persistedVotes(rejected.database).isEmpty(), "rsvp=$rsvp")
        }
    }

    @Test
    fun `server rolls back every vote and receipt when one ballot row fails`() = runBlocking {
        val fixture = fixture()
        val failingPhysicalSlotId = TimeSlotStorageIdentity.physicalId(EVENT_ID, "slot-2")
        fixture.database.voteQueries.selectByEventId(EVENT_ID).executeAsList()
        fixture.driver.execute(
            null,
            """
                CREATE TRIGGER fail_server_second_ballot_vote
                BEFORE INSERT ON vote
                WHEN NEW.timeslotId = '$failingPhysicalSlotId'
                BEGIN
                    SELECT RAISE(ABORT, 'forced server ballot failure');
                END
            """.trimIndent(),
            0
        ).value

        val response = fixture.service.processSyncChanges(request(fixture.pollRevision), ACTOR_ID)

        assertEquals(0, response.appliedChanges)
        assertEquals(1, response.conflicts.size)
        assertTrue(persistedVotes(fixture.database).isEmpty())
        assertEquals(
            null,
            fixture.database.pollBallotReceiptQueries
                .selectByOperationId(OPERATION_ID)
                .executeAsOneOrNull(),
            "A partial server write must roll back its idempotency receipt as well as every vote."
        )
        assertTrue(ballotAcknowledgements(response).isEmpty())
    }

    @Test
    fun `legacy votes create update and delete are terminal non retryable and never mutate`() = runBlocking {
        SyncOperation.entries.forEach { operation ->
            val fixture = fixture()
            if (operation != SyncOperation.CREATE) {
                fixture.database.voteQueries.insertVote(
                    id = "vote_slot-1_$ACTOR_ID",
                    eventId = EVENT_ID,
                    timeslotId = TimeSlotStorageIdentity.physicalId(EVENT_ID, "slot-1"),
                    participantId = "org_$EVENT_ID",
                    vote = "YES",
                    createdAt = "2026-08-28T10:00:00Z",
                    updatedAt = "2026-08-28T10:00:00Z"
                )
            }
            val before = persistedVotes(fixture.database)

            val response = fixture.service.processSyncChanges(
                SyncRequest(changes = listOf(legacyVoteChange(operation))),
                ACTOR_ID
            )

            assertEquals(0, response.appliedChanges, "operation=$operation")
            assertEquals(before, persistedVotes(fixture.database), "operation=$operation")
            assertTrue(ballotAcknowledgements(response).isEmpty(), "operation=$operation")
            val conflict = assertNotNull(response.conflicts.singleOrNull(), "operation=$operation")
            assertEquals("votes", conflict.table)
            assertEquals(
                "LEGACY_VOTES_MUTATION_FORBIDDEN",
                reflectedConflictValue(conflict, "Code"),
                "Legacy slot-level writes must carry a stable terminal failure code. operation=$operation"
            )
            assertFalse(
                reflectedConflictValue(conflict, "Retryable") as? Boolean ?: true,
                "A legacy votes mutation must never be retried. operation=$operation"
            )
        }
    }

    private suspend fun fixture(
        transform: (Event) -> Event = { it }
    ): Fixture {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WakeveDb.Schema.create(driver)
        val database = WakeveDb(driver)
        val repository = DatabaseEventRepository(database)
        repository.createEvent(
            transform(
                Event(
                    id = EVENT_ID,
                    title = "Server ballot",
                    description = "Atomic server ballot contract",
                    organizerId = ACTOR_ID,
                    proposedSlots = listOf(
                        TimeSlot("slot-1", "2099-09-01T10:00:00Z", null, "UTC", TimeOfDay.SPECIFIC),
                        TimeSlot("slot-2", "2099-09-02T10:00:00Z", null, "UTC", TimeOfDay.SPECIFIC)
                    ),
                    deadline = "2099-08-28T12:00:00Z",
                    status = EventStatus.POLLING,
                    createdAt = "2026-08-28T11:00:00Z",
                    updatedAt = "2026-08-28T11:00:00Z"
                )
            )
        ).getOrThrow()
        val event = requireNotNull(repository.getEvent(EVENT_ID))
        return Fixture(driver, database, SyncService(database), event.aggregateRevision)
    }

    private fun request(
        pollRevision: Long,
        actorId: String = ACTOR_ID,
        changeUserId: String = actorId
    ) = SyncRequest(
        changes = listOf(
            SyncChange(
                id = "sync-$LOCAL_RECEIPT_ID",
                table = "poll_ballot",
                operation = SyncOperation.UPDATE.name,
                recordId = LOCAL_RECEIPT_ID,
                data = """
                    {
                      "schemaVersion":1,
                      "localReceiptId":"$LOCAL_RECEIPT_ID",
                      "command":{
                        "schemaVersion":1,
                        "identity":{
                          "eventId":"$EVENT_ID",
                          "actorId":"$actorId",
                          "pollRevision":$pollRevision,
                          "operationId":"$OPERATION_ID"
                        },
                        "entries":[
                          {"slotId":"slot-1","choice":"YES"},
                          {"slotId":"slot-2","choice":"MAYBE"}
                        ],
                        "ballotFingerprint":"$FINGERPRINT"
                      }
                    }
                """.trimIndent(),
                timestamp = "2026-08-28T11:00:00Z",
                userId = changeUserId
            )
        )
    )

    private fun legacyVoteChange(operation: SyncOperation) = SyncChange(
        id = "legacy-vote-${operation.name.lowercase()}",
        table = "votes",
        operation = operation.name,
        recordId = "vote_slot-1_$ACTOR_ID",
        data = """
            {
              "eventId":"$EVENT_ID",
              "participantId":"$ACTOR_ID",
              "slotId":"slot-1",
              "preference":"NO"
            }
        """.trimIndent(),
        timestamp = "2026-08-28T11:00:00Z",
        userId = ACTOR_ID
    )

    private fun persistedVotes(database: WakeveDb): Map<String, String> = database.voteQueries
        .selectByEventId(EVENT_ID)
        .executeAsList()
        .associate {
            requireNotNull(TimeSlotStorageIdentity.logicalId(EVENT_ID, it.timeslotId)) to it.vote
        }

    private fun insertMember(database: WakeveDb, actorId: String, rsvp: String) {
        database.participantQueries.insertParticipantWithAxes(
            id = "participant-$actorId",
            eventId = EVENT_ID,
            userId = actorId,
            role = "PARTICIPANT",
            hasValidatedDate = 0,
            rsvpState = rsvp,
            dateValidationState = "NOT_VALIDATED",
            joinedAt = "2026-08-28T10:00:00Z",
            updatedAt = "2026-08-28T10:00:00Z"
        )
    }

    private fun ballotAcknowledgements(response: Any): List<Any> {
        val getter = response.javaClass.methods.singleOrNull {
            it.name == "getBallotAcknowledgements" && it.parameterCount == 0
        } ?: return emptyList()
        return (getter.invoke(response) as? Iterable<*>)?.filterNotNull().orEmpty()
    }

    private fun acknowledgementValue(acknowledgement: Any, suffix: String): Any? =
        acknowledgement.javaClass.methods
            .singleOrNull { it.name == "get$suffix" && it.parameterCount == 0 }
            ?.invoke(acknowledgement)

    private fun reflectedConflictValue(conflict: Any, suffix: String): Any? =
        conflict.javaClass.methods
            .singleOrNull { it.name == "get$suffix" && it.parameterCount == 0 }
            ?.invoke(conflict)

    private data class Fixture(
        val driver: JdbcSqliteDriver,
        val database: WakeveDb,
        val service: SyncService,
        val pollRevision: Long
    )

    private companion object {
        const val EVENT_ID = "server-ballot-event"
        const val ACTOR_ID = "server-ballot-actor"
        const val OPERATION_ID = "server-ballot-operation"
        const val LOCAL_RECEIPT_ID = "local-server-ballot-receipt"
        const val FINGERPRINT = "v1|736c6f742d31=YES|736c6f742d32=MAYBE"
    }
}
