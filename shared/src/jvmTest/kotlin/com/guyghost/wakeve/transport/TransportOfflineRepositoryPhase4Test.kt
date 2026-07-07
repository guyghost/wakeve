package com.guyghost.wakeve.transport

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.OptimizationType
import com.guyghost.wakeve.models.TransportLocation
import com.guyghost.wakeve.models.TransportMode
import com.guyghost.wakeve.models.TransportOption
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.sync.PendingSyncOperation
import com.guyghost.wakeve.test.createTestEvent
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransportOfflineRepositoryPhase4Test {

    private lateinit var db: WakeveDb
    private lateinit var eventRepository: DatabaseEventRepository
    private lateinit var transportRepository: TransportRepository
    private lateinit var provider: DeterministicTransportProvider
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @BeforeTest
    fun setUp() {
        db = createFreshTestDatabase()
        eventRepository = DatabaseEventRepository(db)
        provider = DeterministicTransportProvider()
        transportRepository = TransportRepository(
            database = db,
            optionProvider = provider::optionsFor
        )
    }

    @Test
    fun `readiness reports missing departure locations and blocks plan generation`() = runBlocking {
        val eventId = "event-transport-readiness"
        createConfirmedEventWithParticipants(eventId)
        transportRepository.saveDepartureLocation(
            eventId = eventId,
            participantId = "participant-alice",
            location = paris(),
            updatedByUserId = "organizer-1"
        ).getOrThrow()

        val readiness = transportRepository.getReadiness(eventId, destination = bordeaux())

        assertFalse(readiness.isComplete)
        assertFalse(readiness.canGeneratePlan)
        assertEquals(listOf("participant-bob"), readiness.missingDepartureParticipantIds)
        assertEquals(listOf("Bob Martin"), readiness.missingDepartureParticipantNames)

        val blocked = transportRepository.generatePlan(
            eventId = eventId,
            destination = bordeaux(),
            optimizationType = OptimizationType.BALANCED,
            generatedByUserId = "organizer-1"
        )
        assertTrue(blocked.isFailure)
        assertTrue(
            blocked.exceptionOrNull()?.message?.contains("departure", ignoreCase = true) == true,
            "Plan generation should explain that participant departure data is missing"
        )

        transportRepository.markTransportNotNeeded(
            eventId = eventId,
            updatedByUserId = "organizer-1"
        ).getOrThrow()

        val skipped = transportRepository.getReadiness(eventId, destination = bordeaux())
        assertFalse(skipped.canGeneratePlan)
        assertTrue(skipped.transportNotNeeded)
        assertTrue(skipped.canFinalizeWithoutPlan)
    }

    @Test
    fun `organizer saves and updates participant departure location locally with pending sync metadata`() = runBlocking {
        val eventId = "event-transport-departure-sync"
        createConfirmedEventWithParticipants(eventId)

        transportRepository.saveDepartureLocation(
            eventId = eventId,
            participantId = "participant-alice",
            location = paris(),
            updatedByUserId = "organizer-1"
        ).getOrThrow()
        val updated = transportRepository.saveDepartureLocation(
            eventId = eventId,
            participantId = "participant-alice",
            location = lyon(),
            updatedByUserId = "organizer-1"
        ).getOrThrow()

        assertEquals(lyon(), updated.location)
        assertEquals("organizer-1", updated.updatedByUserId)
        assertEquals(
            lyon(),
            transportRepository.getDepartureLocation(eventId, "participant-alice")?.location
        )

        val pending = db.syncMetadataQueries.selectPending().executeAsList()
        assertTrue(
            pending.any {
                it.entityType == "transport_departure_location" &&
                    it.entityId == "$eventId:participant-alice" &&
                    it.operation == "UPSERT" &&
                    it.synced == 0L
            },
            "Saving a departure location must be local-first and queue replayable sync metadata"
        )
    }

    @Test
    fun `balanced plan persists one route per confirmed participant and can be selected as final`() = runBlocking {
        val eventId = "event-transport-balanced"
        createConfirmedEventWithParticipants(eventId)
        saveAllDepartureLocations(eventId)

        val plan = transportRepository.generatePlan(
            eventId = eventId,
            destination = bordeaux(),
            optimizationType = OptimizationType.BALANCED,
            generatedByUserId = "organizer-1"
        ).getOrThrow()

        assertEquals(eventId, plan.eventId)
        assertEquals(OptimizationType.BALANCED, plan.optimizationType)
        assertEquals(setOf("participant-alice", "participant-bob"), plan.participantRoutes.keys)
        assertEquals(2, transportRepository.getRoutesByPlan(plan.id).size)
        assertEquals(plan.totalGroupCost, plan.participantRoutes.values.sumOf { it.totalCost })

        transportRepository.selectFinalPlan(
            eventId = eventId,
            planId = plan.id,
            selectedByOrganizerId = "organizer-1"
        ).getOrThrow()

        val selected = transportRepository.getSelectedPlanSummary(eventId)
        assertNotNull(selected)
        assertEquals(plan.id, selected.planId)
        assertEquals(plan.totalGroupCost, selected.totalCost)
        assertTrue(selected.readiness.isComplete)
    }

    @Test
    fun `generate plan requires an explicit organizer actor before local persistence and sync`() {
        val source = projectFile(
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/transport/TransportRepository.kt"
        ).readText()

        assertTrue(
            source.contains("suspend fun generatePlan("),
            "TransportRepository.generatePlan must exist in the checked source."
        )
        assertFalse(
            Regex("""generatedByUserId\s*:\s*String\?\s*=\s*null""").containsMatchIn(source),
            "TransportRepository.generatePlan must not expose a default nullable actor; every local/offline generation must name the organizer."
        )
        assertTrue(
            Regex("""generatedByUserId\s*:\s*String(?!\?)""").containsMatchIn(source),
            "TransportRepository.generatePlan should require a non-null organizer actor parameter."
        )
        assertFalse(
            Regex("""generatedByUserId\?\s*\.\s*let\s*\{\s*requireOrganizer\(eventId,\s*it\)\s*\}""").containsMatchIn(source),
            "Organizer authorization must be unconditional for plan generation, not skipped when generatedByUserId is null."
        )
    }

    @Test
    fun `local plan generation by non organizer persists no plan routes or pending sync`() = runBlocking {
        val eventId = "event-transport-generate-denied-actor"
        createConfirmedEventWithParticipants(eventId)
        seedAllDepartureLocations(eventId)

        val result = transportRepository.generatePlan(
            eventId = eventId,
            destination = bordeaux(),
            optimizationType = OptimizationType.BALANCED,
            generatedByUserId = "participant-alice"
        )

        assertTrue(
            result.isFailure,
            "Local/offline transport plan generation must require an explicit organizer actor"
        )
        assertEquals(
            0,
            db.transportQueries.selectPlansByEvent(eventId).executeAsList().size,
            "Rejected generation must not create a local transport_plan row"
        )
        assertEquals(
            0,
            routeCount(eventId),
            "Rejected generation must not create local transport_route rows"
        )
        assertFalse(
            hasPendingTransportSync(eventId),
            "Rejected generation must not queue pending transport sync metadata"
        )
    }

    @Test
    fun `plan generation passes confirmed slot start to option provider instead of poll deadline`() = runBlocking {
        val eventId = "event-transport-confirmed-time"
        val confirmedSlotStart = "2026-06-15T09:00:00Z"
        val pollDeadline = "2025-12-31T23:59:59Z"
        val capturedEventTimes = mutableListOf<String>()
        val capturingRepository = TransportRepository(
            database = db,
            optionProvider = { participantId, departure, destination, eventTime ->
                capturedEventTimes += eventTime
                provider.optionsFor(participantId, departure, destination, eventTime)
            }
        )
        createConfirmedEventWithParticipants(eventId)
        db.timeSlotQueries.insertTimeSlot(
            id = "confirmed-slot-$eventId",
            eventId = eventId,
            startTime = confirmedSlotStart,
            endTime = "2026-06-15T12:00:00Z",
            timezone = "UTC",
            proposedByParticipantId = null,
            createdAt = "2026-05-22T10:00:00Z",
            updatedAt = "2026-05-22T10:00:00Z",
            timeOfDay = "SPECIFIC"
        )
        db.confirmedDateQueries.insertConfirmedDate(
            id = "confirmed-date-$eventId",
            eventId = eventId,
            timeslotId = "confirmed-slot-$eventId",
            confirmedByOrganizerId = "organizer-1",
            confirmedAt = "2026-05-22T10:00:00Z",
            updatedAt = "2026-05-22T10:00:00Z"
        )
        saveAllDepartureLocations(eventId)

        capturingRepository.generatePlan(
            eventId = eventId,
            destination = bordeaux(),
            optimizationType = OptimizationType.BALANCED,
            generatedByUserId = "organizer-1"
        ).getOrThrow()

        assertEquals(
            listOf(confirmedSlotStart, confirmedSlotStart),
            capturedEventTimes,
            "Transport options must be generated for the confirmed event date, not the poll deadline"
        )
        assertFalse(
            capturedEventTimes.contains(pollDeadline),
            "The poll deadline is not a valid transport departure planning time"
        )
    }

    @Test
    fun `optimization strategies use deterministic provider scoring rules`() = runBlocking {
        val eventId = "event-transport-scoring"
        createConfirmedEventWithParticipants(eventId)
        saveAllDepartureLocations(eventId)

        val costPlan = transportRepository.generatePlan(eventId, bordeaux(), OptimizationType.COST_MINIMIZE, "organizer-1")
            .getOrThrow()
        val timePlan = transportRepository.generatePlan(eventId, bordeaux(), OptimizationType.TIME_MINIMIZE, "organizer-1")
            .getOrThrow()
        val balancedPlan = transportRepository.generatePlan(eventId, bordeaux(), OptimizationType.BALANCED, "organizer-1")
            .getOrThrow()

        assertEquals(
            listOf("alice-bus-cheap", "bob-bus-cheap"),
            costPlan.routeOptionIds(),
            "Cost minimization should choose the cheapest option for every participant"
        )
        assertEquals(
            listOf("alice-flight-fast", "bob-flight-fast"),
            timePlan.routeOptionIds(),
            "Time minimization should choose the shortest option for every participant"
        )
        assertEquals(
            listOf("alice-train-balanced", "bob-train-balanced"),
            balancedPlan.routeOptionIds(),
            "Balanced optimization should choose the deterministic cost/time compromise"
        )
    }

    @Test
    fun `offline replay keeps latest selected plan during deterministic conflict resolution`() = runBlocking {
        val eventId = "event-transport-selection-conflict"
        createConfirmedEventWithParticipants(eventId)
        saveAllDepartureLocations(eventId)

        val olderPlan = transportRepository.generatePlan(eventId, bordeaux(), OptimizationType.COST_MINIMIZE, "organizer-1")
            .getOrThrow()
        val newerPlan = transportRepository.generatePlan(eventId, bordeaux(), OptimizationType.TIME_MINIMIZE, "organizer-1")
            .getOrThrow()

        transportRepository.selectFinalPlan(
            eventId = eventId,
            planId = olderPlan.id,
            selectedByOrganizerId = "organizer-1",
            selectedAt = "2026-06-01T10:00:00Z"
        ).getOrThrow()
        transportRepository.selectFinalPlan(
            eventId = eventId,
            planId = newerPlan.id,
            selectedByOrganizerId = "organizer-1",
            selectedAt = "2026-06-01T10:05:00Z"
        ).getOrThrow()

        transportRepository.applyRemoteSelectedPlan(
            eventId = eventId,
            planId = olderPlan.id,
            selectedAt = "2026-06-01T10:02:00Z"
        ).getOrThrow()

        val selected = transportRepository.getSelectedPlanSummary(eventId)
        assertNotNull(selected)
        assertEquals(
            newerPlan.id,
            selected.planId,
            "Offline conflict resolution should keep the latest selected transport plan"
        )

        val replayed = mutableListOf<PendingSyncOperation>()
        transportRepository.replayPendingSync { operation ->
            replayed += operation
            Result.success(Unit)
        }.getOrThrow()

        assertTrue(
            replayed.any {
                it.entityType == "transport_plan_selection" &&
                    it.entityId == eventId &&
                    it.operation.name == "UPDATE"
            },
                "The resolved selected transport plan must be replayable through pending sync"
        )
        assertFalse(
            db.syncMetadataQueries.selectPending().executeAsList().any {
                it.entityType == "transport_plan_selection" &&
                    it.entityId == eventId &&
                    it.operation == "CONFLICT_RESOLVED"
            },
            "Audit-only conflict-resolution metadata must not remain pending after replay because replayPendingSync does not send CONFLICT_RESOLVED"
        )
        assertFalse(
            transportRepository.hasPendingTransportSync(eventId),
            "Shared transport pending indicators must not count non-replayable conflict-resolution metadata"
        )
    }

    @Test
    fun `remote selected plan conflict resolution is not counted as replayable pending transport sync`() = runBlocking {
        val eventId = "event-transport-conflict-audit-only"
        createConfirmedEventWithParticipants(eventId)
        val planId = seedTransportPlan(eventId)
        db.transportQueries.upsertSelectedPlan(
            event_id = eventId,
            plan_id = planId,
            selected_at = "2026-06-01T10:05:00Z",
            selected_by_user_id = "organizer-1"
        )

        transportRepository.applyRemoteSelectedPlan(
            eventId = eventId,
            planId = planId,
            selectedAt = "2026-06-01T10:02:00Z"
        ).getOrThrow()

        val replayed = mutableListOf<PendingSyncOperation>()
        transportRepository.replayPendingSync { operation ->
            replayed += operation
            Result.success(Unit)
        }.getOrThrow()

        assertTrue(
            replayed.isEmpty(),
            "A stale remote selected-plan conflict resolution is audit-only and must not be replayed as a transport mutation"
        )
        assertFalse(
            db.syncMetadataQueries.selectPending().executeAsList().any {
                it.entityType.startsWith("transport_") &&
                    it.entityId == eventId &&
                    it.operation == "CONFLICT_RESOLVED" &&
                    it.synced == 0L
            },
            "Non-replayable CONFLICT_RESOLVED rows must either be marked synced or not inserted as pending"
        )
        assertFalse(
            transportRepository.hasPendingTransportSync(eventId),
            "hasPendingTransportSync must expose only replayable transport operations"
        )
    }

    @Test
    fun `successful offline replay marks transport operations as synced`() = runBlocking {
        val eventId = "event-transport-replay-synced"
        createConfirmedEventWithParticipants(eventId)
        saveAllDepartureLocations(eventId)

        val plan = transportRepository.generatePlan(eventId, bordeaux(), OptimizationType.BALANCED, "organizer-1")
            .getOrThrow()
        transportRepository.selectFinalPlan(eventId, plan.id, "organizer-1").getOrThrow()

        val replayed = mutableListOf<PendingSyncOperation>()
        val pendingBeforeReplay = db.syncMetadataQueries.selectPending()
            .executeAsList()
            .filter { it.entityType.startsWith("transport_") }

        assertTrue(pendingBeforeReplay.isNotEmpty(), "Transport operations must be queued before replay")

        transportRepository.replayPendingSync { operation ->
            replayed += operation
            Result.success(Unit)
        }.getOrThrow()

        assertEquals(pendingBeforeReplay.size, replayed.size)
        pendingBeforeReplay.forEach { pending ->
            assertEquals(
                1L,
                db.syncMetadataQueries.selectById(pending.id).executeAsOne().synced,
                "Successful replay must mark ${pending.entityType}:${pending.entityId} as synced"
            )
        }
        assertFalse(
            db.syncMetadataQueries.selectPending()
                .executeAsList()
                .any { it.entityType.startsWith("transport_") },
            "No successfully replayed transport operation should remain pending"
        )
    }

    @Test
    fun `local departure saves are rejected outside mutable transport workflow states`() = runBlocking {
        listOf(EventStatus.DRAFT, EventStatus.POLLING, EventStatus.FINALIZED).forEach { status ->
            val eventId = "event-transport-save-guard-${status.name.lowercase()}"
            createEventWithConfirmedParticipants(eventId, status)
            seedDepartureLocation(eventId, "participant-alice", paris())

            val result = transportRepository.saveDepartureLocation(
                eventId = eventId,
                participantId = "participant-alice",
                location = lyon(),
                updatedByUserId = "organizer-1"
            )

            assertTrue(
                result.isFailure,
                "Saving a departure location must be rejected while event is $status"
            )
            assertWorkflowGuardFailure(result.exceptionOrNull(), status)
            assertEquals(
                paris(),
                transportRepository.getDepartureLocation(eventId, "participant-alice")?.location,
                "Rejected departure saves must leave the local offline row unchanged for $status"
            )
            assertFalse(
                hasPendingTransportSync(eventId),
                "Rejected departure saves must not queue replay sync metadata for $status"
            )
        }
    }

    @Test
    fun `local transport generation is rejected before organization and after finalization`() = runBlocking {
        listOf(EventStatus.DRAFT, EventStatus.POLLING, EventStatus.FINALIZED).forEach { status ->
            val eventId = "event-transport-generate-guard-${status.name.lowercase()}"
            createEventWithConfirmedParticipants(eventId, status)
            seedAllDepartureLocations(eventId)
            val planCountBefore = planCount(eventId)

            val result = transportRepository.generatePlan(
                eventId = eventId,
                destination = bordeaux(),
                optimizationType = OptimizationType.BALANCED,
                generatedByUserId = "organizer-1"
            )

            assertTrue(
                result.isFailure,
                "Generating a transport plan must be rejected while event is $status"
            )
            assertWorkflowGuardFailure(result.exceptionOrNull(), status)
            assertEquals(
                planCountBefore,
                planCount(eventId),
                "Rejected generation must not persist a local transport plan for $status"
            )
            assertFalse(
                hasPendingTransportSync(eventId),
                "Rejected generation must not queue replay sync metadata for $status"
            )
        }
    }

    @Test
    fun `local final plan selection is rejected before organization and after finalization`() = runBlocking {
        listOf(EventStatus.DRAFT, EventStatus.POLLING, EventStatus.FINALIZED).forEach { status ->
            val eventId = "event-transport-select-guard-${status.name.lowercase()}"
            createEventWithConfirmedParticipants(eventId, status)
            val planId = seedTransportPlan(eventId)

            val result = transportRepository.selectFinalPlan(
                eventId = eventId,
                planId = planId,
                selectedByOrganizerId = "organizer-1"
            )

            assertTrue(
                result.isFailure,
                "Selecting a final transport plan must be rejected while event is $status"
            )
            assertWorkflowGuardFailure(result.exceptionOrNull(), status)
            assertNull(
                db.transportQueries.selectSelectedPlan(eventId).executeAsOneOrNull(),
                "Rejected final plan selection must not update the local selected plan for $status"
            )
            assertFalse(
                hasPendingTransportSync(eventId),
                "Rejected final plan selection must not queue replay sync metadata for $status"
            )
        }
    }

    @Test
    fun `local mark transport not needed is rejected before organization and after finalization`() = runBlocking {
        listOf(EventStatus.DRAFT, EventStatus.POLLING, EventStatus.FINALIZED).forEach { status ->
            val eventId = "event-transport-not-needed-guard-${status.name.lowercase()}"
            createEventWithConfirmedParticipants(eventId, status)

            val result = transportRepository.markTransportNotNeeded(
                eventId = eventId,
                updatedByUserId = "organizer-1"
            )

            assertTrue(
                result.isFailure,
                "Marking transport not needed must be rejected while event is $status"
            )
            assertWorkflowGuardFailure(result.exceptionOrNull(), status)
            assertNull(
                db.transportQueries.selectTransportEventStatus(eventId).executeAsOneOrNull(),
                "Rejected mark-not-needed must not update local transport event status for $status"
            )
            assertFalse(
                hasPendingTransportSync(eventId),
                "Rejected mark-not-needed must not queue replay sync metadata for $status"
            )
        }
    }

    @Test
    fun `local transport plan deletion is rejected before organization and after finalization`() = runBlocking {
        listOf(EventStatus.DRAFT, EventStatus.POLLING, EventStatus.FINALIZED).forEach { status ->
            val eventId = "event-transport-delete-guard-${status.name.lowercase()}"
            createEventWithConfirmedParticipants(eventId, status)
            val planId = seedTransportPlan(eventId)

            val result = transportRepository.deletePlan(
                eventId = eventId,
                planId = planId,
                deletedByUserId = "organizer-1"
            )

            assertTrue(
                result.isFailure,
                "Deleting a transport plan must be rejected while event is $status"
            )
            assertWorkflowGuardFailure(result.exceptionOrNull(), status)
            assertNotNull(
                db.transportQueries.selectPlanById(planId).executeAsOneOrNull(),
                "Rejected deletion must leave the local transport plan untouched for $status"
            )
            assertFalse(
                db.syncMetadataQueries.selectPending().executeAsList().any {
                    it.entityType == "transport_plan" &&
                        it.entityId == planId &&
                        it.operation == "DELETE"
                },
                "Rejected deletion must not queue transport_plan DELETE sync metadata for $status"
            )
            assertFalse(
                hasPendingTransportSync(eventId),
                "Rejected deletion must not queue any transport sync metadata for $status"
            )
        }
    }

    @Test
    fun `local departure saves reject non member and unconfirmed participant actors without pending sync`() = runBlocking {
        val eventId = "event-transport-departure-actor-denied"
        createEventWithParticipantAccessStates(eventId, EventStatus.ORGANIZING)

        listOf(
            "non-member-zoe",
            "participant-invited",
            "participant-pending",
            "participant-declined"
        ).forEach { actorId ->
            val result = transportRepository.saveDepartureLocation(
                eventId = eventId,
                participantId = actorId,
                location = lyon(),
                updatedByUserId = actorId
            )

            assertTrue(
                result.isFailure,
                "Transport departure writes must reject actor $actorId unless they are organizer or confirmed for the retained date"
            )
            assertNull(
                transportRepository.getDepartureLocation(eventId, actorId),
                "Rejected actor $actorId must not persist a local departure location"
            )
            assertFalse(
                hasPendingTransportSync(eventId, actorId),
                "Rejected actor $actorId must not queue a replayable transport sync operation"
            )
        }
    }

    @Test
    fun `confirmed participant departure save is limited to their own participant record`() = runBlocking {
        val eventId = "event-transport-departure-own-record"
        createEventWithParticipantAccessStates(eventId, EventStatus.ORGANIZING)
        seedDepartureLocation(eventId, "participant-bob", paris())

        transportRepository.saveDepartureLocation(
            eventId = eventId,
            participantId = "participant-alice",
            location = lyon(),
            updatedByUserId = "participant-alice"
        ).getOrThrow()

        val deniedCrossParticipantWrite = transportRepository.saveDepartureLocation(
            eventId = eventId,
            participantId = "participant-bob",
            location = bordeaux(),
            updatedByUserId = "participant-alice"
        )

        assertTrue(
            deniedCrossParticipantWrite.isFailure,
            "A confirmed participant may persist only their own departure location"
        )
        assertEquals(
            paris(),
            transportRepository.getDepartureLocation(eventId, "participant-bob")?.location,
            "Rejected cross-participant departure writes must leave the other participant row unchanged"
        )
        assertFalse(
            db.syncMetadataQueries.selectPending().executeAsList().any {
                it.entityType == "transport_departure_location" &&
                    it.entityId == "$eventId:participant-bob"
            },
            "Rejected cross-participant departure writes must not queue sync metadata for the other participant"
        )
    }

    @Test
    fun `organizer departure writes are limited to confirmed participants and rejected targets queue no sync`() = runBlocking {
        val eventId = "event-transport-organizer-departure-targets"
        createEventWithParticipantAccessStates(eventId, EventStatus.ORGANIZING)

        transportRepository.saveDepartureLocation(
            eventId = eventId,
            participantId = "participant-bob",
            location = paris(),
            updatedByUserId = "organizer-1"
        ).getOrThrow()

        assertEquals(
            paris(),
            transportRepository.getDepartureLocation(eventId, "participant-bob")?.location,
            "Organizer must be able to save a departure for a participant confirmed for the retained date"
        )
        assertTrue(
            hasPendingTransportSyncForEntity("transport_departure_location", "$eventId:participant-bob"),
            "Allowed organizer departure save must queue sync metadata for the confirmed participant"
        )

        val pendingAfterAllowedSave = pendingTransportSyncCount(eventId)
        listOf(
            "non-member-zoe",
            "participant-invited",
            "participant-pending",
            "participant-declined"
        ).forEach { targetParticipantId ->
            val result = transportRepository.saveDepartureLocation(
                eventId = eventId,
                participantId = targetParticipantId,
                location = lyon(),
                updatedByUserId = "organizer-1"
            )

            assertTrue(
                result.isFailure,
                "Organizer departure writes must reject non-members and participants who have not validated the retained date; target was $targetParticipantId"
            )
            assertNull(
                transportRepository.getDepartureLocation(eventId, targetParticipantId),
                "Rejected organizer write for $targetParticipantId must not persist a local departure location"
            )
            assertEquals(
                pendingAfterAllowedSave,
                pendingTransportSyncCount(eventId),
                "Rejected organizer write for $targetParticipantId must not queue additional transport sync metadata"
            )
            assertFalse(
                hasPendingTransportSyncForEntity("transport_departure_location", "$eventId:$targetParticipantId"),
                "Rejected organizer write for $targetParticipantId must not queue sync metadata for that participant"
            )
        }
    }

    @Test
    fun `organizer departure guard must not bypass target participant confirmation`() {
        val source = projectFile(
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/transport/TransportRepository.kt"
        ).readText()
        val guard = Regex(
            """private\s+fun\s+requireCanSaveDepartureLocation\([\s\S]*?\n\s*private\s+fun\s+requireOrganizer"""
        ).find(source)?.value.orEmpty()
        val compactGuard = guard.replace(Regex("""\s+"""), "")

        assertTrue(
            guard.contains("requireCanSaveDepartureLocation"),
            "TransportRepository.requireCanSaveDepartureLocation must exist in the checked source."
        )
        val confirmationIndex = compactGuard.indexOf("require(isConfirmedParticipant(eventId,participantId))")
        val organizerReturnIndex = compactGuard.indexOf("if(isOrganizer(eventId,updatedByUserId)){return}")

        assertTrue(
            confirmationIndex >= 0,
            "Organizer departure writes must validate the target participant is confirmed before any organizer bypass."
        )
        assertTrue(
            organizerReturnIndex >= 0,
            "Organizer departure writes may return early only after target participant confirmation has been required."
        )
        assertTrue(
            confirmationIndex < organizerReturnIndex,
            "Organizer departure writes must not bypass target participant confirmation; rejected targets must persist nothing and queue no sync."
        )
    }

    @Test
    fun `local final plan selection is organizer only and denied actors queue no sync`() = runBlocking {
        listOf("participant-alice", "participant-pending", "non-member-zoe").forEach { actorId ->
            val eventId = "event-transport-selection-actor-$actorId"
            createEventWithParticipantAccessStates(eventId, EventStatus.ORGANIZING)
            val planId = seedTransportPlan(eventId)

            val result = transportRepository.selectFinalPlan(
                eventId = eventId,
                planId = planId,
                selectedByOrganizerId = actorId
            )

            assertTrue(
                result.isFailure,
                "Only the event organizer may select the final transport plan; actor $actorId must be denied"
            )
            assertNull(
                db.transportQueries.selectSelectedPlan(eventId).executeAsOneOrNull(),
                "Denied final-plan selection by $actorId must not persist local selected-plan state"
            )
            assertFalse(
                db.syncMetadataQueries.selectPending().executeAsList().any {
                    it.entityType == "transport_plan_selection" && it.entityId == eventId
                },
                "Denied final-plan selection by $actorId must not queue replay sync metadata"
            )
        }
    }

    @Test
    fun `local transport plan deletion is organizer only and denied actors queue no sync`() = runBlocking {
        listOf("participant-alice", "participant-pending", "non-member-zoe").forEach { actorId ->
            val eventId = "event-transport-delete-actor-$actorId"
            createEventWithParticipantAccessStates(eventId, EventStatus.ORGANIZING)
            val planId = seedTransportPlan(eventId)

            val result = transportRepository.deletePlan(
                eventId = eventId,
                planId = planId,
                deletedByUserId = actorId
            )

            assertTrue(
                result.isFailure,
                "Only the event organizer may delete a local transport plan; actor $actorId must be denied"
            )
            assertNotNull(
                db.transportQueries.selectPlanById(planId).executeAsOneOrNull(),
                "Denied transport deletion by $actorId must leave the local plan intact"
            )
            assertFalse(
                db.syncMetadataQueries.selectPending().executeAsList().any {
                    it.entityType == "transport_plan" &&
                        it.entityId == planId &&
                        it.operation == "DELETE"
                },
                "Denied transport deletion by $actorId must not queue DELETE sync metadata"
            )
        }
    }

    @Test
    fun `local mark transport not needed is organizer only and denied actors queue no sync`() = runBlocking {
        listOf("participant-alice", "participant-pending", "non-member-zoe").forEach { actorId ->
            val eventId = "event-transport-not-needed-actor-$actorId"
            createEventWithParticipantAccessStates(eventId, EventStatus.ORGANIZING)

            val result = transportRepository.markTransportNotNeeded(
                eventId = eventId,
                updatedByUserId = actorId
            )

            assertTrue(
                result.isFailure,
                "Only the event organizer may mark transport not needed; actor $actorId must be denied"
            )
            assertNull(
                db.transportQueries.selectTransportEventStatus(eventId).executeAsOneOrNull(),
                "Denied mark-not-needed by $actorId must not persist local transport status"
            )
            assertFalse(
                hasPendingTransportSync(eventId),
                "Denied mark-not-needed by $actorId must not queue replay sync metadata"
            )
        }
    }

    @Test
    fun `organizer can manage final plan deletion and not needed in mutable transport statuses`() = runBlocking {
        listOf(EventStatus.CONFIRMED, EventStatus.COMPARING, EventStatus.ORGANIZING).forEach { status ->
            val eventId = "event-transport-organizer-manage-${status.name.lowercase()}"
            createEventWithParticipantAccessStates(eventId, status)
            val planId = seedTransportPlan(eventId)

            transportRepository.selectFinalPlan(
                eventId = eventId,
                planId = planId,
                selectedByOrganizerId = "organizer-1"
            ).getOrThrow()

            assertEquals(
                planId,
                db.transportQueries.selectSelectedPlan(eventId).executeAsOne().plan_id,
                "Organizer must be able to select final transport plan while event is $status"
            )

            transportRepository.markTransportNotNeeded(
                eventId = eventId,
                updatedByUserId = "organizer-1"
            ).getOrThrow()

            assertEquals(
                1L,
                db.transportQueries.selectTransportEventStatus(eventId).executeAsOne().transport_not_needed,
                "Organizer must be able to mark transport not needed while event is $status"
            )

            transportRepository.deletePlan(
                eventId = eventId,
                planId = planId,
                deletedByUserId = "organizer-1"
            ).getOrThrow()

            assertNull(
                db.transportQueries.selectPlanById(planId).executeAsOneOrNull(),
                "Organizer must be able to delete local transport plan while event is $status"
            )
        }
    }

    private suspend fun createConfirmedEventWithParticipants(eventId: String) {
        createEventWithConfirmedParticipants(eventId, EventStatus.CONFIRMED)
    }

    private suspend fun createEventWithConfirmedParticipants(eventId: String, status: EventStatus) {
        eventRepository.createEvent(
            createTestEvent(
                id = eventId,
                organizerId = "organizer-1",
                participants = listOf("participant-alice", "participant-bob"),
                status = status,
                finalDate = "2026-06-15T09:00:00Z"
            )
        ).getOrThrow()

        insertUser("participant-alice", "Alice Durand")
        insertUser("participant-bob", "Bob Martin")
        insertConfirmedParticipant("participant-row-alice-$eventId", eventId, "participant-alice")
        insertConfirmedParticipant("participant-row-bob-$eventId", eventId, "participant-bob")
    }

    private suspend fun createEventWithParticipantAccessStates(eventId: String, status: EventStatus) {
        eventRepository.createEvent(
            createTestEvent(
                id = eventId,
                organizerId = "organizer-1",
                participants = listOf(
                    "participant-alice",
                    "participant-bob",
                    "participant-invited",
                    "participant-pending",
                    "participant-declined"
                ),
                status = status,
                finalDate = "2026-06-15T09:00:00Z"
            )
        ).getOrThrow()

        insertUser("participant-alice", "Alice Durand")
        insertUser("participant-bob", "Bob Martin")
        insertUser("participant-invited", "Iris Invited")
        insertUser("participant-pending", "Paul Pending")
        insertUser("participant-declined", "Diane Declined")
        insertUser("non-member-zoe", "Zoe Outsider")

        insertParticipantWithAccess(
            id = "participant-row-alice-$eventId",
            eventId = eventId,
            userId = "participant-alice",
            role = "PARTICIPANT",
            hasValidatedDate = true
        )
        insertParticipantWithAccess(
            id = "participant-row-bob-$eventId",
            eventId = eventId,
            userId = "participant-bob",
            role = "PARTICIPANT",
            hasValidatedDate = true
        )
        insertParticipantWithAccess(
            id = "participant-row-invited-$eventId",
            eventId = eventId,
            userId = "participant-invited",
            role = "INVITED",
            hasValidatedDate = false
        )
        insertParticipantWithAccess(
            id = "participant-row-pending-$eventId",
            eventId = eventId,
            userId = "participant-pending",
            role = "PARTICIPANT",
            hasValidatedDate = false
        )
        insertParticipantWithAccess(
            id = "participant-row-declined-$eventId",
            eventId = eventId,
            userId = "participant-declined",
            role = "DECLINED",
            hasValidatedDate = false
        )
    }

    private suspend fun saveAllDepartureLocations(eventId: String) {
        transportRepository.saveDepartureLocation(eventId, "participant-alice", paris(), "organizer-1")
            .getOrThrow()
        transportRepository.saveDepartureLocation(eventId, "participant-bob", lyon(), "organizer-1")
            .getOrThrow()
    }

    private fun seedAllDepartureLocations(eventId: String) {
        seedDepartureLocation(eventId, "participant-alice", paris())
        seedDepartureLocation(eventId, "participant-bob", lyon())
    }

    private fun seedDepartureLocation(
        eventId: String,
        participantId: String,
        location: TransportLocation
    ) {
        db.transportQueries.upsertDepartureLocation(
            event_id = eventId,
            participant_id = participantId,
            location_json = json.encodeToString(location),
            updated_by_user_id = "seed",
            updated_at = "2026-05-22T10:00:00Z"
        )
    }

    private fun seedTransportPlan(eventId: String): String {
        val planId = "seed-plan-$eventId"
        db.transportQueries.insertPlan(
            id = planId,
            event_id = eventId,
            destination_json = json.encodeToString(bordeaux()),
            optimization_type = OptimizationType.BALANCED.name,
            total_group_cost = 0.0,
            group_arrivals_json = "[]",
            created_at = "2026-05-22T10:00:00Z"
        )
        return planId
    }

    private fun planCount(eventId: String): Int {
        return db.transportQueries.selectPlansByEvent(eventId).executeAsList().size
    }

    private fun routeCount(eventId: String): Int {
        return db.transportQueries.selectPlansByEvent(eventId)
            .executeAsList()
            .sumOf { plan -> db.transportQueries.selectRoutesByPlan(plan.id).executeAsList().size }
    }

    private fun hasPendingTransportSync(eventId: String): Boolean {
        return db.syncMetadataQueries.selectPending()
            .executeAsList()
            .any { pending ->
                pending.entityType.startsWith("transport_") &&
                    (pending.entityId == eventId ||
                        pending.entityId.startsWith("$eventId:") ||
                        pending.entityId.contains(eventId))
            }
    }

    private fun hasPendingTransportSync(eventId: String, actorId: String): Boolean {
        return db.syncMetadataQueries.selectPending()
            .executeAsList()
            .any { pending ->
                pending.entityType.startsWith("transport_") &&
                    (pending.entityId == eventId ||
                        pending.entityId.startsWith("$eventId:") ||
                        pending.entityId.contains(eventId) ||
                    pending.entityId.contains(actorId))
            }
    }

    private fun pendingTransportSyncCount(eventId: String): Int {
        return db.syncMetadataQueries.selectPending()
            .executeAsList()
            .count { pending ->
                pending.entityType.startsWith("transport_") &&
                    (pending.entityId == eventId ||
                        pending.entityId.startsWith("$eventId:") ||
                        pending.entityId.contains(eventId))
            }
    }

    private fun hasPendingTransportSyncForEntity(entityType: String, entityId: String): Boolean {
        return db.syncMetadataQueries.selectPending()
            .executeAsList()
            .any { pending ->
                pending.entityType == entityType &&
                    pending.entityId == entityId &&
                    pending.synced == 0L
            }
    }

    private fun assertWorkflowGuardFailure(error: Throwable?, status: EventStatus) {
        val message = error?.message.orEmpty()
        assertTrue(
            message.contains(status.name, ignoreCase = true) ||
                message.contains("workflow", ignoreCase = true) ||
                message.contains("read-only", ignoreCase = true) ||
                message.contains("readonly", ignoreCase = true),
            "Transport mutation guard should explain the workflow/read-only status $status, but was: $message"
        )
    }

    private fun insertUser(userId: String, name: String) {
        if (db.userQueries.selectUserById(userId).executeAsOneOrNull() != null) {
            return
        }
        db.userQueries.insertUser(
            id = userId,
            provider_id = "$userId-provider",
            email = "$userId@example.com",
            name = name,
            avatar_url = null,
            provider = "google",
            role = "USER",
            created_at = "2026-05-22T10:00:00Z",
            updated_at = "2026-05-22T10:00:00Z"
        )
    }

    private fun insertConfirmedParticipant(id: String, eventId: String, userId: String) {
        insertParticipantWithAccess(
            id = id,
            eventId = eventId,
            userId = userId,
            role = "PARTICIPANT",
            hasValidatedDate = true
        )
    }

    private fun insertParticipantWithAccess(
        id: String,
        eventId: String,
        userId: String,
        role: String,
        hasValidatedDate: Boolean
    ) {
        db.participantQueries.insertParticipant(
            id = id,
            eventId = eventId,
            userId = userId,
            role = role,
            hasValidatedDate = if (hasValidatedDate) 1 else 0,
            joinedAt = "2026-05-22T10:00:00Z",
            updatedAt = "2026-05-22T10:00:00Z"
        )
    }

    private fun paris() = TransportLocation(
        name = "Paris",
        address = "Gare de Lyon, Paris",
        latitude = 48.8443,
        longitude = 2.3730
    )

    private fun lyon() = TransportLocation(
        name = "Lyon",
        address = "Gare Part-Dieu, Lyon",
        latitude = 45.7606,
        longitude = 4.8594
    )

    private fun bordeaux() = TransportLocation(
        name = "Bordeaux",
        address = "Place de la Bourse, Bordeaux",
        latitude = 44.8412,
        longitude = -0.5693
    )

    private fun com.guyghost.wakeve.models.TransportPlan.routeOptionIds(): List<String> =
        participantRoutes.toSortedMap().values.map { route -> route.segments.single().id }

    private fun projectFile(relativePath: String): File {
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { File(it, relativePath).exists() }
        return File(root, relativePath)
    }

}

private class DeterministicTransportProvider {

    suspend fun optionsFor(
        participantId: String,
        departure: TransportLocation,
        destination: TransportLocation,
        eventTime: String
    ): List<TransportOption> {
        val prefix = when (participantId) {
            "participant-alice" -> "alice"
            "participant-bob" -> "bob"
            else -> participantId
        }

        return listOf(
            option(
                id = "$prefix-bus-cheap",
                mode = TransportMode.BUS,
                provider = "FlixBus",
                departure = departure,
                destination = destination,
                eventTime = eventTime,
                durationMinutes = 600,
                cost = 30.0
            ),
            option(
                id = "$prefix-train-balanced",
                mode = TransportMode.TRAIN,
                provider = "SNCF",
                departure = departure,
                destination = destination,
                eventTime = eventTime,
                durationMinutes = 180,
                cost = 80.0
            ),
            option(
                id = "$prefix-flight-fast",
                mode = TransportMode.FLIGHT,
                provider = "Air France",
                departure = departure,
                destination = destination,
                eventTime = eventTime,
                durationMinutes = 70,
                cost = 220.0
            )
        )
    }

    private fun option(
        id: String,
        mode: TransportMode,
        provider: String,
        departure: TransportLocation,
        destination: TransportLocation,
        eventTime: String,
        durationMinutes: Int,
        cost: Double
    ) = TransportOption(
        id = id,
        mode = mode,
        provider = provider,
        departure = departure,
        arrival = destination,
        departureTime = eventTime,
        arrivalTime = eventTime,
        durationMinutes = durationMinutes,
        cost = cost
    )
}
