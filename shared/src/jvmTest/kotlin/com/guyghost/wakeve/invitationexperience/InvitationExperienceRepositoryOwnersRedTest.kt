package com.guyghost.wakeve.invitationexperience

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class InvitationExperienceRepositoryOwnersRedTest {

    @Test
    fun `Library owner derives organizer draft projection artwork-safe event and pending sync from repository`() = runTest {
        val fixture = fixture()
        fixture.seedEvent("draft-1", organizerId = "viewer-1", status = "DRAFT")
        fixture.execute(
            "INSERT INTO event_artwork(event_id, kind, structured_version, source_kind, preset_id, alt_kind, focal_x, focal_y, crop, updated_at) " +
                "VALUES ('draft-1', 'STRUCTURED', 1, 'PRESET', 'weekend', 'DECORATIVE', 0.5, 0.5, 'FILL', '2030-01-01T00:00:00Z')"
        )
        fixture.execute(
            "INSERT INTO syncMetadata(id, entityType, entityId, operation, timestamp, synced) " +
                "VALUES ('operation-draft-1', 'event', 'draft-1', 'UPDATE', '2030-01-02T00:00:00Z', 0)"
        )

        val state = DatabaseInvitationExperienceProjectionRepository(fixture.database).library(
            viewerId = "viewer-1",
            projection = LibraryProjection.DRAFTS,
            now = Instant.parse("2030-01-03T00:00:00Z")
        )

        val ready = assertIs<LibraryLoadState.Ready<*>>(state)
        val cards = ready.snapshot as List<*>
        val card = assertIs<LibraryCardProjection>(cards.single())
        assertEquals("draft-1", card.event.id)
        assertEquals(setOf(LibraryProjection.DRAFTS, LibraryProjection.HOSTING), card.memberships)
        assertEquals(LibraryNextAction.CONTINUE_DRAFT, card.nextAction)
        assertEquals(LibrarySyncState.Pending("operation-draft-1"), card.syncState)
        val artworkGetter = card.javaClass.methods.firstOrNull { it.name == "getArtwork" }
        assertNotNull(
            artworkGetter,
            "A Library card must carry the total typed artwork projection; Event.heroImageUrl cannot represent presets."
        )
        assertEquals(
            Artwork.Structured(
                version = 1,
                ref = ArtworkRef(
                    source = ArtworkSource.Preset("weekend"),
                    alt = ArtworkAlt.Decorative,
                    focalPoint = ArtworkFocalPoint(0.5, 0.5),
                    crop = ArtworkCrop.FILL
                )
            ),
            artworkGetter.invoke(card)
        )
        assertIs<Freshness.Current>(ready.freshness)
    }

    @Test
    fun `Archive owner accepts past or finalized only and reloads total artwork read-only`() = runTest {
        val fixture = fixture()
        fixture.seedEvent("past-1", organizerId = "viewer-1", status = "FINALIZED")
        fixture.execute(
            "INSERT INTO event_artwork(event_id, kind, legacy_remote_url, updated_at) " +
                "VALUES ('past-1', 'LEGACY_REMOTE', 'https://cdn.wakeve.app/events/past.jpg', '2030-01-01T00:00:00Z')"
        )
        fixture.execute(
            "INSERT INTO timeSlot(id, eventId, startTime, endTime, timezone, createdAt, updatedAt) " +
                "VALUES ('past-slot', 'past-1', '2030-01-01T10:00:00Z', '2030-01-01T12:00:00Z', 'UTC', '2029-12-01T00:00:00Z', '2029-12-01T00:00:00Z')"
        )
        fixture.execute(
            "INSERT INTO confirmedDate(id, eventId, timeslotId, confirmedByOrganizerId, confirmedAt, updatedAt) " +
                "VALUES ('past-confirmed', 'past-1', 'past-slot', 'viewer-1', '2029-12-02T00:00:00Z', '2029-12-02T00:00:00Z')"
        )
        fixture.seedEvent("future-draft", organizerId = "viewer-1", status = "DRAFT")
        fixture.execute(
            "INSERT INTO event_artwork(event_id, kind, updated_at) " +
                "VALUES ('future-draft', 'NONE', '2030-01-01T00:00:00Z')"
        )

        val repository = DatabaseInvitationExperienceProjectionRepository(fixture.database)
        val past = assertIs<ArchiveLoadState.Ready>(
            repository.archive(
                eventId = "past-1",
                viewerId = "viewer-1",
                now = Instant.parse("2030-01-03T00:00:00Z")
            )
        )
        assertEquals(TemporalClass.PAST, past.snapshot.temporalClass)
        assertEquals(InteractionPolicy.READ_ONLY, past.snapshot.interactionPolicy)
        assertEquals(
            Artwork.LegacyRemote("https://cdn.wakeve.app/events/past.jpg"),
            past.snapshot.artwork
        )
        assertTrue(
            past.snapshot.settledSummary.isNotEmpty(),
            "A FINALIZED event with a confirmed structured date needs a settled Archive summary."
        )
        assertIs<Freshness.Current>(past.freshness)

        assertIs<ArchiveLoadState.Empty>(
            repository.archive(
                eventId = "future-draft",
                viewerId = "viewer-1",
                now = Instant.parse("2030-01-03T00:00:00Z")
            )
        )
    }

    @Test
    fun `Library owner excludes nonmembers and forces FINALIZED next action to Archive`() = runTest {
        val fixture = fixture()
        fixture.seedEvent("finalized-1", organizerId = "viewer-1", status = "FINALIZED")
        fixture.execute("INSERT INTO event_artwork(event_id, kind, updated_at) VALUES ('finalized-1', 'NONE', '2030-01-01T00:00:00Z')")
        fixture.seedEvent("other-draft", organizerId = "other-user", status = "DRAFT")
        fixture.execute("INSERT INTO event_artwork(event_id, kind, updated_at) VALUES ('other-draft', 'NONE', '2030-01-01T00:00:00Z')")
        val repository = DatabaseInvitationExperienceProjectionRepository(fixture.database)

        val hosting = assertIs<LibraryLoadState.Ready<*>>(
            repository.library(
                viewerId = "viewer-1",
                projection = LibraryProjection.HOSTING,
                now = Instant.parse("2030-01-03T00:00:00Z")
            )
        )
        val hostedCard = assertIs<LibraryCardProjection>((hosting.snapshot as List<*>).single())
        assertEquals("finalized-1", hostedCard.event.id)
        assertEquals(InteractionPolicy.READ_ONLY, hostedCard.interactionPolicy)
        assertEquals(LibraryNextAction.VIEW_ARCHIVE, hostedCard.nextAction)

        assertIs<LibraryLoadState.Empty>(
            repository.library(
                viewerId = "viewer-1",
                projection = LibraryProjection.DRAFTS,
                now = Instant.parse("2030-01-03T00:00:00Z")
            )
        )
    }

    @Test
    fun `direct invite owner persists DRAFT batch and cancellation across repository relaunch`() = runTest {
        val fixture = fixture()
        fixture.seedEvent("event-1", organizerId = "organizer-1", status = "DRAFT", revision = 4)
        fixture.execute(
            "INSERT INTO event_artwork(event_id, kind, updated_at) VALUES ('event-1', 'NONE', '2030-01-01T00:00:00Z')"
        )
        val recipients = setOf(RecipientKey("hmac-v1-a1b2c3"), RecipientKey("hmac-v1-d4e5f6"))
        val capability = DirectInviteCapability.Ready(
            eventId = "event-1",
            actorId = "organizer-1",
            accessRevision = 4,
            allowedEventStatuses = setOf(EventStatus.DRAFT)
        )
        val command = SubmitDirectInviteBatchCommand(
            eventId = "event-1",
            actorId = "organizer-1",
            eventStatus = EventStatus.DRAFT,
            batchId = "batch-1",
            operationId = "operation-1",
            recipientKeys = recipients,
            capability = capability
        )

        val repository = deliveryRepository(fixture.database)
        val submitted = assertIs<DirectInviteOperation.PendingSync>(
            repository.submitProtected(command)
        )
        assertEquals(recipients, submitted.recipientKeys)
        assertEquals(1L, fixture.number("SELECT COUNT(*) FROM direct_invite_batch WHERE batch_id = 'batch-1'"))
        assertEquals(2L, fixture.number("SELECT COUNT(*) FROM direct_invite_recipient_outcome WHERE batch_id = 'batch-1'"))
        assertTrue(
            checkNotNull(fixture.text("SELECT expires_at FROM direct_invite_batch WHERE batch_id = 'batch-1'")) <
                "2100-01-01T00:00:00Z",
            "Protected invite material must be short-lived, never retained with a year-9999 sentinel."
        )
        assertTrue(
            checkNotNull(fixture.text("SELECT MAX(expires_at) FROM direct_invite_recipient_outcome WHERE batch_id = 'batch-1'")) <
                "2100-01-01T00:00:00Z"
        )
        assertEquals(submitted, DatabaseDirectInviteBatchRepository(fixture.database).load("batch-1"))

        val cancelled = assertIs<DirectInviteOperation.Cancelled>(
            repository.cancel(CancelDirectInviteBatchCommand(submitted, capability))
        )
        assertEquals(cancelled, DatabaseDirectInviteBatchRepository(fixture.database).load("batch-1"))
    }

    @Test
    fun `direct invite persistence exposes protected input and exact acknowledgement owners`() {
        val protectedInputOwner = runCatching {
            Class.forName(
                "com.guyghost.wakeve.invitationexperience.DirectInviteRecipientKeyOwner"
            )
        }.getOrNull()
        assertNotNull(
            protectedInputOwner,
            "Raw recipient input needs a trusted normalization plus keyed-HMAC owner before RecipientKey persistence."
        )
        assertNotNull(
            protectedInputOwner.methods.firstOrNull { it.name == "protect" },
            "The protected-input owner must expose one explicit protect boundary; callers must not construct keys from raw input."
        )
        assertNotNull(
            DatabaseDirectInviteBatchRepository::class.java.methods.firstOrNull {
                it.name == "acknowledge"
            },
            "Server outcomes need a repository acknowledgement owner that persists exact batch/operation/key matches."
        )
    }

    @Test
    fun `direct invite protected input is normalized before keyed HMAC and never exposes raw identity`() {
        val normalizedInputs = mutableListOf<String>()
        val owner = DirectInviteRecipientKeyOwner(
            digestPort = DirectInviteRecipientDigestPort { normalized ->
                normalizedInputs += normalized
                "a1".repeat(32)
            },
            keyVersion = 3
        )

        val mixedCaseEmail = assertNotNull(owner.protect("  Alice@Example.COM  "))
        val normalizedEmail = assertNotNull(owner.protect("alice@example.com"))
        val formattedPhone = assertNotNull(owner.protect(" +33 (0)6 12 34 56 78 "))

        assertEquals(mixedCaseEmail, normalizedEmail)
        assertEquals(
            listOf("alice@example.com", "alice@example.com", "+330612345678"),
            normalizedInputs,
            "Only normalized transient recipient input may cross the keyed-digest port."
        )
        assertEquals("hmac-v3-${"a1".repeat(32)}", mixedCaseEmail.value)
        assertTrue("alice" !in mixedCaseEmail.value && "example.com" !in mixedCaseEmail.value)
        assertTrue("0612345678" !in formattedPhone.value)
        assertNull(owner.protect("   "))
        assertNull(owner.protect("not-an-address"))
    }

    @Test
    fun `direct invite acknowledgement persists only an exact batch operation key and capability match`() = runTest {
        val fixture = fixture()
        fixture.seedEvent("event-1", organizerId = "organizer-1", status = "DRAFT", revision = 4)
        fixture.execute(
            "INSERT INTO event_artwork(event_id, kind, updated_at) VALUES ('event-1', 'NONE', '2030-01-01T00:00:00Z')"
        )
        val accepted = RecipientKey("hmac-v1-a1b2c3")
        val invalid = RecipientKey("hmac-v1-d4e5f6")
        val recipients = setOf(accepted, invalid)
        val capability = DirectInviteCapability.Ready(
            eventId = "event-1",
            actorId = "organizer-1",
            accessRevision = 4,
            allowedEventStatuses = setOf(EventStatus.DRAFT)
        )
        val repository = deliveryRepository(fixture.database)
        val pending = assertIs<DirectInviteOperation.PendingSync>(
            repository.submitProtected(
                SubmitDirectInviteBatchCommand(
                    eventId = "event-1",
                    actorId = "organizer-1",
                    eventStatus = EventStatus.DRAFT,
                    batchId = "batch-1",
                    operationId = "operation-1",
                    recipientKeys = recipients,
                    capability = capability
                )
            )
        )
        val outcomes = mapOf(
            accepted to DirectInviteRecipientOutcome.ServerAccepted("invitation-1"),
            invalid to DirectInviteRecipientOutcome.Invalid("INVALID_RECIPIENT")
        )

        val mismatches = listOf(
            AcknowledgeDirectInviteBatchCommand(
                batchId = "batch-1",
                operationId = "wrong-operation",
                outcomesByRecipientKey = outcomes,
                capability = capability
            ),
            AcknowledgeDirectInviteBatchCommand(
                batchId = "batch-1",
                operationId = "operation-1",
                outcomesByRecipientKey = outcomes - invalid,
                capability = capability
            ),
            AcknowledgeDirectInviteBatchCommand(
                batchId = "batch-1",
                operationId = "operation-1",
                outcomesByRecipientKey = outcomes,
                capability = capability.copy(actorId = "forged-actor")
            ),
            AcknowledgeDirectInviteBatchCommand(
                batchId = "batch-1",
                operationId = "operation-1",
                outcomesByRecipientKey = outcomes,
                capability = capability.copy(accessRevision = 3)
            )
        )
        mismatches.forEach { mismatch ->
            assertEquals(pending, repository.acknowledge(mismatch))
            assertEquals(
                pending,
                DatabaseDirectInviteBatchRepository(fixture.database).load("batch-1"),
                "A mismatched acknowledgement must not partially persist or report success."
            )
        }

        val completed = assertIs<DirectInviteOperation.Completed>(
            repository.acknowledge(
                AcknowledgeDirectInviteBatchCommand(
                    batchId = "batch-1",
                    operationId = "operation-1",
                    outcomesByRecipientKey = outcomes,
                    capability = capability
                )
            )
        )
        assertEquals(outcomes, completed.outcomesByRecipientKey)
        assertEquals(
            completed,
            DatabaseDirectInviteBatchRepository(fixture.database).load("batch-1"),
            "An exact acknowledgement must survive repository relaunch."
        )
    }

    @Test
    fun `direct invite owner revalidates persisted DRAFT status and access revision before writing`() = runTest {
        val fixture = fixture()
        fixture.seedEvent("event-1", organizerId = "organizer-1", status = "POLLING", revision = 5)
        val recipients = setOf(RecipientKey("hmac-v1-a1b2c3"))
        val repository = deliveryRepository(fixture.database)
        val result = repository.submitProtected(
            SubmitDirectInviteBatchCommand(
                eventId = "event-1",
                actorId = "organizer-1",
                eventStatus = EventStatus.DRAFT,
                batchId = "forged-batch",
                operationId = "forged-operation",
                recipientKeys = recipients,
                capability = DirectInviteCapability.Ready(
                    eventId = "event-1",
                    actorId = "organizer-1",
                    accessRevision = 4,
                    allowedEventStatuses = setOf(EventStatus.DRAFT)
                )
            )
        )

        assertIs<DirectInviteOperation.Failed>(result)
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM direct_invite_batch"))
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM direct_invite_recipient_outcome"))
    }

    @Test
    fun `direct invite owner rejects a temporally PAST draft before persistence`() = runTest {
        val fixture = fixture()
        fixture.seedEvent("past-draft", organizerId = "organizer-1", status = "DRAFT", revision = 4)
        fixture.execute(
            "INSERT INTO timeSlot(id, eventId, startTime, endTime, timezone, createdAt, updatedAt) " +
                "VALUES ('past-slot', 'past-draft', '2000-01-01T10:00:00Z', '2000-01-01T12:00:00Z', " +
                "'UTC', '1999-12-01T00:00:00Z', '1999-12-01T00:00:00Z')"
        )
        val capability = DirectInviteCapability.Ready(
            eventId = "past-draft",
            actorId = "organizer-1",
            accessRevision = 4,
            allowedEventStatuses = setOf(EventStatus.DRAFT)
        )

        val repository = deliveryRepository(fixture.database)
        val result = repository.submitProtected(
            SubmitDirectInviteBatchCommand(
                eventId = "past-draft",
                actorId = "organizer-1",
                eventStatus = EventStatus.DRAFT,
                batchId = "past-batch",
                operationId = "past-operation",
                recipientKeys = setOf(RecipientKey("hmac-v1-a1b2c3")),
                capability = capability
            )
        )

        assertIs<DirectInviteOperation.Failed>(result)
        assertEquals(
            0L,
            fixture.number("SELECT COUNT(*) FROM direct_invite_batch"),
            "The global PAST override must run before the DRAFT-only direct-invite write."
        )
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM direct_invite_recipient_outcome"))
    }

    @Test
    fun `direct invite owner rejects retry when persisted access revision is stale`() = runTest {
        val fixture = fixture()
        fixture.seedEvent("event-1", organizerId = "organizer-1", status = "DRAFT", revision = 5)
        val retryable = RecipientKey("hmac-v1-a1b2c3")
        val failed = DirectInviteOperation.Failed(
            batchId = "batch-1",
            operationId = "operation-1",
            requestedRecipientKeys = setOf(retryable),
            outcomesByRecipientKey = mapOf(
                retryable to DirectInviteRecipientOutcome.Failed(
                    InvitationExperienceError.NETWORK_UNAVAILABLE
                )
            ),
            batchError = InvitationExperienceError.NETWORK_UNAVAILABLE
        )

        val result = DatabaseDirectInviteBatchRepository(fixture.database).retry(
            RetryDirectInviteBatchCommand(
                operation = failed,
                capability = DirectInviteCapability.Ready(
                    eventId = "event-1",
                    actorId = "organizer-1",
                    accessRevision = 4,
                    allowedEventStatuses = setOf(EventStatus.DRAFT)
                )
            )
        )

        assertEquals(
            failed,
            result,
            "Retry is an owner mutation and must fail closed when its persisted revision no longer matches."
        )
    }

    @Test
    fun `direct invite owner persists unresolved-only retry across repository relaunch`() = runTest {
        val fixture = fixture()
        fixture.seedEvent("event-1", organizerId = "organizer-1", status = "DRAFT", revision = 4)
        fixture.execute(
            "INSERT INTO event_artwork(event_id, kind, updated_at) VALUES ('event-1', 'NONE', '2030-01-01T00:00:00Z')"
        )
        val accepted = RecipientKey("hmac-v1-a1b2c3")
        val retryable = RecipientKey("hmac-v1-d4e5f6")
        val invalid = RecipientKey("hmac-v1-a7b8c9")
        val capability = DirectInviteCapability.Ready(
            eventId = "event-1",
            actorId = "organizer-1",
            accessRevision = 4,
            allowedEventStatuses = setOf(EventStatus.DRAFT)
        )
        val repository = deliveryRepository(fixture.database)
        repository.submitProtected(
            SubmitDirectInviteBatchCommand(
                eventId = "event-1",
                actorId = "organizer-1",
                eventStatus = EventStatus.DRAFT,
                batchId = "batch-1",
                operationId = "operation-1",
                recipientKeys = setOf(accepted, retryable, invalid),
                capability = capability
            )
        )
        fixture.execute("UPDATE direct_invite_batch SET status = 'FAILED' WHERE batch_id = 'batch-1'")
        fixture.execute(
            "UPDATE direct_invite_recipient_outcome SET status = 'SERVER_ACCEPTED', invitation_id = 'invite-1' " +
                "WHERE batch_id = 'batch-1' AND recipient_key = '${accepted.value}'"
        )
        fixture.execute(
            "UPDATE direct_invite_recipient_outcome SET status = 'FAILED', reason_code = 'NETWORK_UNAVAILABLE' " +
                "WHERE batch_id = 'batch-1' AND recipient_key = '${retryable.value}'"
        )
        fixture.execute(
            "UPDATE direct_invite_recipient_outcome SET status = 'INVALID', reason_code = 'INVALID_RECIPIENT' " +
                "WHERE batch_id = 'batch-1' AND recipient_key = '${invalid.value}'"
        )
        val failed = DirectInviteOperation.Failed(
            batchId = "batch-1",
            operationId = "operation-1",
            requestedRecipientKeys = setOf(accepted, retryable, invalid),
            outcomesByRecipientKey = mapOf(
                accepted to DirectInviteRecipientOutcome.ServerAccepted("invite-1"),
                retryable to DirectInviteRecipientOutcome.Failed(
                    InvitationExperienceError.NETWORK_UNAVAILABLE
                ),
                invalid to DirectInviteRecipientOutcome.Invalid("INVALID_RECIPIENT")
            ),
            batchError = InvitationExperienceError.NETWORK_UNAVAILABLE
        )

        assertEquals(
            DirectInviteOperation.PendingSync("batch-1", "operation-1", setOf(retryable)),
            repository.retry(RetryDirectInviteBatchCommand(failed, capability))
        )
        assertEquals(
            "SERVER_ACCEPTED",
            fixture.text(
                "SELECT status FROM direct_invite_recipient_outcome " +
                    "WHERE batch_id = 'batch-1' AND recipient_key = '${accepted.value}'"
            )
        )
        assertEquals(
            "QUEUED_LOCAL",
            fixture.text(
                "SELECT status FROM direct_invite_recipient_outcome " +
                    "WHERE batch_id = 'batch-1' AND recipient_key = '${retryable.value}'"
            )
        )
        assertEquals(
            "INVALID",
            fixture.text(
                "SELECT status FROM direct_invite_recipient_outcome " +
                    "WHERE batch_id = 'batch-1' AND recipient_key = '${invalid.value}'"
            )
        )
        assertEquals(
            DirectInviteOperation.PendingSync("batch-1", "operation-1", setOf(retryable)),
            DatabaseDirectInviteBatchRepository(fixture.database).load("batch-1")
        )
    }

    @Test
    fun `expired protected recipient outcomes cannot be rehydrated or retried`() = runTest {
        val fixture = fixture()
        fixture.seedEvent("event-1", organizerId = "organizer-1", status = "DRAFT", revision = 4)
        val recipient = RecipientKey("hmac-v1-a1b2c3")
        val capability = DirectInviteCapability.Ready(
            eventId = "event-1",
            actorId = "organizer-1",
            accessRevision = 4,
            allowedEventStatuses = setOf(EventStatus.DRAFT)
        )
        val repository = deliveryRepository(fixture.database)
        repository.submitProtected(
            SubmitDirectInviteBatchCommand(
                eventId = "event-1",
                actorId = "organizer-1",
                eventStatus = EventStatus.DRAFT,
                batchId = "expired-batch",
                operationId = "expired-operation",
                recipientKeys = setOf(recipient),
                capability = capability
            )
        )
        fixture.execute(
            "UPDATE direct_invite_batch SET expires_at = '2000-01-01T00:00:00Z' " +
                "WHERE batch_id = 'expired-batch'"
        )
        fixture.execute(
            "UPDATE direct_invite_recipient_outcome SET expires_at = '2000-01-01T00:00:00Z' " +
                "WHERE batch_id = 'expired-batch'"
        )

        assertNull(
            DatabaseDirectInviteBatchRepository(fixture.database).load("expired-batch"),
            "A repository relaunch must not reconstruct protected recipient identities past retention expiry."
        )
        assertEquals(
            0L,
            fixture.number(
                "SELECT COUNT(*) FROM direct_invite_recipient_outcome WHERE batch_id = 'expired-batch'"
            ),
            "The read boundary must enforce or trigger the bounded retention policy."
        )
    }

    @Test
    fun `event notification preference persists exact operation idempotently across relaunch`() = runTest {
        val fixture = fixture()
        fixture.seedUser("viewer-1")
        fixture.seedEvent("event-1", organizerId = "viewer-1", status = "DRAFT")
        val key = OperationKey(
            subject = OperationSubject.EventNotification("event-1", "viewer-1"),
            action = InformationOperationAction.SAVE_EVENT_PREFERENCE,
            target = OperationTarget.User("viewer-1"),
            operationId = "preference-operation-1"
        )
        val repository = DatabaseEventNotificationPreferenceRepository(fixture.database)

        val saved = repository.save(key, EventNotificationPreference.ESSENTIAL_ONLY).getOrThrow()
        assertTrue(saved.pendingSync)
        assertEquals(saved, DatabaseEventNotificationPreferenceRepository(fixture.database).get("event-1", "viewer-1"))
        assertEquals(saved, repository.save(key, EventNotificationPreference.ESSENTIAL_ONLY).getOrThrow())
        assertTrue(repository.save(key, EventNotificationPreference.MUTED).isFailure)
        assertEquals(1L, fixture.number("SELECT COUNT(*) FROM event_notification_preference"))
        assertEquals(
            1L,
            fixture.number(
                "SELECT COUNT(*) FROM syncMetadata WHERE entityType = 'event_notification_preference' AND operation = 'UPDATE' AND synced = 0"
            ),
            "A pending preference record needs one durable sync operation, not only a UI status string."
        )
    }

    @Test
    fun `event notification preference owner rejects FINALIZED writes before persistence`() = runTest {
        val fixture = fixture()
        fixture.seedUser("viewer-1")
        fixture.seedEvent("event-1", organizerId = "viewer-1", status = "FINALIZED")
        val key = OperationKey(
            subject = OperationSubject.EventNotification("event-1", "viewer-1"),
            action = InformationOperationAction.SAVE_EVENT_PREFERENCE,
            target = OperationTarget.User("viewer-1"),
            operationId = "historical-preference-operation"
        )

        assertTrue(
            DatabaseEventNotificationPreferenceRepository(fixture.database)
                .save(key, EventNotificationPreference.MUTED)
                .isFailure
        )
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM event_notification_preference"))
    }

    @Test
    fun `event notification preference owner rejects a nonmember before persistence`() = runTest {
        val fixture = fixture()
        fixture.seedUser("outsider-1")
        fixture.seedEvent("event-1", organizerId = "organizer-1", status = "DRAFT")
        val key = OperationKey(
            subject = OperationSubject.EventNotification("event-1", "outsider-1"),
            action = InformationOperationAction.SAVE_EVENT_PREFERENCE,
            target = OperationTarget.User("outsider-1"),
            operationId = "outsider-preference-operation"
        )

        assertTrue(
            DatabaseEventNotificationPreferenceRepository(fixture.database)
                .save(key, EventNotificationPreference.MUTED)
                .isFailure
        )
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM event_notification_preference"))
    }

    @Test
    fun `Information owner loads historical repository truth with all mutations disabled`() = runTest {
        val fixture = fixture()
        fixture.seedUser("viewer-1")
        fixture.seedEvent("event-1", organizerId = "viewer-1", status = "FINALIZED")
        fixture.execute(
            "INSERT INTO event_artwork(event_id, kind, updated_at) VALUES ('event-1', 'NONE', '2030-01-01T00:00:00Z')"
        )

        val ready = assertIs<EventInformationLoadState.Ready>(
            DatabaseEventInformationRepository(fixture.database).load(
                eventId = "event-1",
                viewerId = "viewer-1",
                now = Instant.parse("2030-01-03T00:00:00Z")
            )
        )

        assertEquals(InteractionPolicy.READ_ONLY, ready.snapshot.interactionPolicy)
        assertEquals(
            EventInformationCapabilities(
                canLeave = false,
                canRemoveParticipant = false,
                canDelete = false,
                canWriteEventPreference = false
            ),
            ready.snapshot.capabilities
        )
        assertIs<InformationDestinationState.Hidden>(ready.snapshot.calendar)
        assertIs<InformationDestinationState.Hidden>(ready.snapshot.maps)
        assertIs<InformationDestinationState.Hidden>(ready.snapshot.weather)
        assertEquals(
            "UNAVAILABLE",
            ready.snapshot.systemAuthorization.name,
            "A repository without an injected OS authorization reading must expose unavailable, not fabricate the user decision NOT_DETERMINED."
        )
        assertNull(ready.snapshot.eventPreferenceRecord)
        assertIs<Freshness.Current>(ready.freshness)
    }

    @Test
    fun `Information owner projects persisted account notification types and active quiet hours`() = runTest {
        val fixture = fixture()
        fixture.seedUser("viewer-1")
        fixture.seedEvent("event-1", organizerId = "viewer-1", status = "DRAFT")
        fixture.execute(
            "INSERT INTO notification_preferences(" +
                "user_id, enabled_types, quiet_hours_start, quiet_hours_end, sound_enabled, vibration_enabled, updated_at" +
                ") VALUES (" +
                "'viewer-1', '[\"EVENT_INVITE\",\"EVENT_UPDATE\"]', '00:00', '23:59', 1, 1, 1893672000000" +
                ")"
        )

        val ready = assertIs<EventInformationLoadState.Ready>(
            DatabaseEventInformationRepository(fixture.database).load(
                eventId = "event-1",
                viewerId = "viewer-1",
                now = Instant.parse("2030-01-03T12:00:00Z")
            )
        )

        assertEquals(
            setOf(EventNotificationType.EVENT_INVITE, EventNotificationType.EVENT_UPDATE),
            ready.snapshot.accountEnabledTypes,
            "Information must project the persisted account axis instead of hard-coding an empty set."
        )
        assertTrue(
            ready.snapshot.quietHoursActive,
            "Information must evaluate the persisted quiet-hours window at the supplied instant."
        )
    }

    @Test
    fun `Information owner exposes only structured ready Calendar Maps and Weather destinations`() = runTest {
        val fixture = fixture()
        fixture.seedUser("viewer-1")
        fixture.seedEvent("event-1", organizerId = "viewer-1", status = "CONFIRMED")
        fixture.execute(
            "INSERT INTO timeSlot(id, eventId, startTime, endTime, timezone, createdAt, updatedAt) " +
                "VALUES ('slot-1', 'event-1', '2030-02-10T10:00:00Z', '2030-02-10T12:00:00Z', 'UTC', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')"
        )
        fixture.execute(
            "INSERT INTO confirmedDate(id, eventId, timeslotId, confirmedByOrganizerId, confirmedAt, updatedAt) " +
                "VALUES ('confirmed-1', 'event-1', 'slot-1', 'viewer-1', '2030-01-02T00:00:00Z', '2030-01-02T00:00:00Z')"
        )
        fixture.execute(
            "INSERT INTO resolvedEventLocation(id, eventId, sourceLocationId, label, latitude, longitude, providerName, resolvedAt) " +
                "VALUES ('resolved-1', 'event-1', 'location-1', 'Paris', 48.85, 2.35, 'maps-owner', '2030-01-02T00:00:00Z')"
        )
        fixture.execute(
            "INSERT INTO eventWeatherSnapshot(id, eventId, locationId, locationLabel, latitude, longitude, startDate, endDate, providerName, fetchedAt, expiresAt, dailyForecastsJson) " +
                "VALUES ('weather-1', 'event-1', 'resolved-1', 'Paris', 48.85, 2.35, '2030-02-10', '2030-02-10', 'weather-owner', '2030-01-02T00:00:00Z', '2030-02-11T00:00:00Z', '[]')"
        )

        val ready = assertIs<EventInformationLoadState.Ready>(
            DatabaseEventInformationRepository(fixture.database).load(
                eventId = "event-1",
                viewerId = "viewer-1",
                now = Instant.parse("2030-01-03T00:00:00Z")
            )
        )

        assertEquals(InteractionPolicy.INTERACTIVE, ready.snapshot.interactionPolicy)
        assertEquals(
            InformationDestinationState.Ready(InformationDestination.Calendar("event-1")),
            ready.snapshot.calendar
        )
        assertEquals(
            InformationDestinationState.Ready(InformationDestination.Maps("resolved-1")),
            ready.snapshot.maps
        )
        assertEquals(
            InformationDestinationState.Ready(InformationDestination.Weather("resolved-1")),
            ready.snapshot.weather
        )
        assertTrue(ready.snapshot.capabilities.canDelete)
        assertTrue(ready.snapshot.capabilities.canWriteEventPreference)
        assertEquals(false, ready.snapshot.capabilities.canLeave)
    }

    private fun fixture(): Fixture {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0).value
        WakeveDb.Schema.create(driver)
        return Fixture(driver, WakeveDb(driver))
    }

    private fun deliveryRepository(database: WakeveDb) = DatabaseDirectInviteBatchRepository(
        database = database,
        deliveryTransport = DirectInviteDeliveryTransport {
            DirectInviteDeliveryResult.Deferred(InvitationExperienceError.NETWORK_UNAVAILABLE)
        }
    )

    private suspend fun DatabaseDirectInviteBatchRepository.submitProtected(
        command: SubmitDirectInviteBatchCommand
    ): DirectInviteOperation {
        val capability = command.capability as? DirectInviteCapability.Ready
            ?: return submit(command)
        val binding = DirectInviteDeliveryBinding(
            eventId = command.eventId,
            actorId = command.actorId,
            accessRevision = capability.accessRevision,
            batchId = command.batchId,
            operationId = command.operationId
        )
        val expiresAt = Clock.System.now().plus(29.days).toString()
        return submit(
            command = command,
            deliveryEnvelopes = command.recipientKeys.mapTo(linkedSetOf()) { key ->
                DirectInviteDeliveryEnvelope(
                    binding = binding,
                    recipientKey = key,
                    ciphertext = "test-ciphertext-${key.value.takeLast(12)}",
                    keyVersion = 1,
                    expiresAt = expiresAt
                )
            }
        )
    }

    private class Fixture(
        val driver: SqlDriver,
        val database: WakeveDb
    ) {
        fun seedEvent(
            id: String,
            organizerId: String,
            status: String,
            revision: Long = 1
        ) {
            execute(
                """INSERT INTO event(
                    id, organizerId, title, description, status, deadline, createdAt, updatedAt,
                    aggregateRevision, aggregateSchemaVersion
                ) VALUES (
                    '$id', '$organizerId', 'Event $id', 'Description', '$status',
                    '2030-01-10T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z',
                    $revision, 1
                )"""
            )
        }

        fun seedUser(id: String) {
            execute(
                """INSERT INTO user(
                    id, provider_id, email, name, provider, created_at, updated_at
                ) VALUES (
                    '$id', 'provider-$id', '$id@example.com', 'Viewer', 'apple',
                    '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z'
                )"""
            )
        }

        fun execute(sql: String) {
            driver.execute(null, sql, 0).value
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
}
