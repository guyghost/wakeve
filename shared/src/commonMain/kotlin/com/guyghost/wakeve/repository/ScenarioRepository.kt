package com.guyghost.wakeve.repository

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Coordinates
import com.guyghost.wakeve.models.EventPlanningMode
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.LocationType
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioGenerationType
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVote
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.models.ScenarioVotingResult
import com.guyghost.wakeve.models.ScenarioWithVotes
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.scenario.ScenarioMatrixGenerationService
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.nanoseconds

/**
 * Repository for managing scenarios and scenario votes in the database.
 * Provides CRUD operations and voting functionality for event planning scenarios.
 */
class ScenarioRepository(private val db: WakeveDb) {
    private val scenarioQueries = db.scenarioQueries
    private val scenarioVoteQueries = db.scenarioVoteQueries
    private val eventQueries = db.eventQueries
    private val timeSlotQueries = db.timeSlotQueries
    private val potentialLocationQueries = db.potentialLocationQueries
    private val confirmedDateQueries = db.confirmedDateQueries
    private val syncMetadataQueries = db.syncMetadataQueries
    private val invitationExperienceQueries = db.invitationExperienceQueries

    /**
     * Create a new scenario in the database.
     */
    suspend fun createScenario(scenario: Scenario): Result<Scenario> {
        return try {
            val normalized = scenario.normalized()
            val now = nextMutationTimestamp(normalized.updatedAt)
            val committed = db.transactionWithResult {
                val parent = eventQueries.selectById(normalized.eventId).executeAsOneOrNull()
                    ?: return@transactionWithResult false
                if (parent.aggregateSchemaVersion != 1L || parent.status == EventStatus.FINALIZED.name) {
                    return@transactionWithResult false
                }
                val authorizationId = "scenario-create:${normalized.id}:${parent.aggregateRevision}"
                if (!authorizeAggregateWrite(
                        normalized.eventId,
                        parent.aggregateRevision,
                        authorizationId,
                        now
                    )
                ) return@transactionWithResult false
                val scenarioCountBeforeInsert = scenarioQueries.countByEventId(normalized.eventId).executeAsOne()
                insertScenario(normalized, now)

                if (scenarioCountBeforeInsert == 0L && parent.status == EventStatus.CONFIRMED.name) {
                    eventQueries.updateEventStatusIfRevision(
                        status = "COMPARING",
                        updatedAt = now,
                        id = normalized.eventId,
                        aggregateRevision = parent.aggregateRevision
                    )
                    queueSyncMetadata(
                        id = "sync_scenario_compare_${normalized.eventId}_${normalized.id}",
                        entityType = "event",
                        entityId = normalized.eventId,
                        operation = "UPDATE",
                        timestamp = "${now}_COMPARING_${normalized.id}"
                    )
                } else {
                    eventQueries.advanceAggregateRevisionIfCurrent(
                        updatedAt = now,
                        id = normalized.eventId,
                        aggregateRevision = parent.aggregateRevision
                    )
                }

                val updatedParent = eventQueries.selectById(normalized.eventId).executeAsOneOrNull()
                if (updatedParent?.aggregateRevision != parent.aggregateRevision + 1L) {
                    rollback(false)
                }
                invitationExperienceQueries.clearAggregateWriteAuthorization(
                    normalized.eventId,
                    authorizationId
                )

                queueSyncMetadata(
                    id = "sync_scenario_${normalized.id}",
                    entityType = "scenario",
                    entityId = normalized.id,
                    operation = "CREATE",
                    timestamp = "${now}_CREATE_${normalized.id}"
                )
                true
            }
            if (!committed) {
                return Result.failure(
                    IllegalStateException("Scenario aggregate writer is incompatible or stale")
                )
            }
            Result.success(normalized.copy(updatedAt = now))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a scenario by its ID.
     */
    fun getScenarioById(id: String): Scenario? {
        return scenarioQueries.selectById(id).executeAsOneOrNull()?.let { row ->
            Scenario(
                id = row.id,
                eventId = row.eventId,
                name = row.name,
                dateOrPeriod = row.dateOrPeriod,
                location = row.location,
                duration = row.duration.toInt(),
                estimatedParticipants = row.estimatedParticipants.toInt(),
                estimatedBudgetPerPerson = row.estimatedBudgetPerPerson,
                description = row.description,
                status = ScenarioStatus.valueOf(row.status),
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
                sourceTimeSlotId = logicalSourceTimeSlotId(row.eventId, row.sourceTimeSlotId),
                sourcePotentialLocationId = row.sourcePotentialLocationId,
                generationType = parseScenarioGenerationType(row.generationType)
            )
        }
    }

    /**
     * Get all scenarios for a specific event.
     */
    fun getScenariosByEventId(eventId: String): List<Scenario> {
        return scenarioQueries.selectByEventId(eventId).executeAsList().map { row ->
            Scenario(
                id = row.id,
                eventId = row.eventId,
                name = row.name,
                dateOrPeriod = row.dateOrPeriod,
                location = row.location,
                duration = row.duration.toInt(),
                estimatedParticipants = row.estimatedParticipants.toInt(),
                estimatedBudgetPerPerson = row.estimatedBudgetPerPerson,
                description = row.description,
                status = ScenarioStatus.valueOf(row.status),
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
                sourceTimeSlotId = logicalSourceTimeSlotId(row.eventId, row.sourceTimeSlotId),
                sourcePotentialLocationId = row.sourcePotentialLocationId,
                generationType = parseScenarioGenerationType(row.generationType)
            )
        }
    }

    /**
     * Get scenarios by event ID and status.
     */
    fun getScenariosByEventIdAndStatus(eventId: String, status: ScenarioStatus): List<Scenario> {
        return scenarioQueries.selectByEventIdAndStatus(eventId, status.name)
            .executeAsList()
            .map { row ->
                    Scenario(
                        id = row.id,
                        eventId = row.eventId,
                        name = row.name,
                        dateOrPeriod = row.dateOrPeriod,
                        location = row.location,
                        duration = row.duration.toInt(),
                        estimatedParticipants = row.estimatedParticipants.toInt(),
                        estimatedBudgetPerPerson = row.estimatedBudgetPerPerson,
                        description = row.description,
                        status = ScenarioStatus.valueOf(row.status),
                        createdAt = row.createdAt,
                        updatedAt = row.updatedAt,
                        sourceTimeSlotId = logicalSourceTimeSlotId(row.eventId, row.sourceTimeSlotId),
                        sourcePotentialLocationId = row.sourcePotentialLocationId,
                        generationType = parseScenarioGenerationType(row.generationType)
                    )
                }
    }

    /**
     * Get the selected scenario for an event (if any).
     */
    fun getSelectedScenario(eventId: String): Scenario? {
        return scenarioQueries.selectSelectedByEventId(eventId).executeAsOneOrNull()?.let { row ->
            Scenario(
                id = row.id,
                eventId = row.eventId,
                name = row.name,
                dateOrPeriod = row.dateOrPeriod,
                location = row.location,
                duration = row.duration.toInt(),
                estimatedParticipants = row.estimatedParticipants.toInt(),
                estimatedBudgetPerPerson = row.estimatedBudgetPerPerson,
                description = row.description,
                status = ScenarioStatus.valueOf(row.status),
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
                sourceTimeSlotId = logicalSourceTimeSlotId(row.eventId, row.sourceTimeSlotId),
                sourcePotentialLocationId = row.sourcePotentialLocationId,
                generationType = parseScenarioGenerationType(row.generationType)
            )
        }
    }

    /**
     * Update an existing scenario.
     */
    suspend fun updateScenario(scenario: Scenario): Result<Scenario> {
        return try {
            val normalized = scenario.normalized()
            val existingScenario = scenarioQueries.selectById(normalized.id).executeAsOneOrNull()
                ?: return Result.failure(
                    IllegalArgumentException("Scenario not found: ${normalized.id}")
                )
            if (existingScenario.eventId != normalized.eventId) {
                return Result.failure(
                    IllegalArgumentException("Scenario not found: ${normalized.id}")
                )
            }
            val now = getCurrentUtcIsoString()
            val committed = db.transactionWithResult {
                val currentScenario = scenarioQueries.selectById(normalized.id).executeAsOneOrNull()
                    ?: return@transactionWithResult false
                val parent = eventQueries.selectById(normalized.eventId).executeAsOneOrNull()
                    ?: return@transactionWithResult false
                if (
                    currentScenario.eventId != normalized.eventId ||
                    currentScenario.updatedAt != normalized.updatedAt ||
                    parent.aggregateSchemaVersion != 1L ||
                    parent.status == EventStatus.FINALIZED.name
                ) {
                    return@transactionWithResult false
                }

                val authorizationId = "scenario-update:${normalized.id}:${parent.aggregateRevision}"
                if (!authorizeAggregateWrite(
                        normalized.eventId,
                        parent.aggregateRevision,
                        authorizationId,
                        now
                    )
                ) return@transactionWithResult false

                scenarioQueries.updateScenarioWithMetadataIfUnchanged(
                    name = normalized.name,
                    dateOrPeriod = normalized.dateOrPeriod,
                    location = normalized.location,
                    duration = normalized.duration.toLong(),
                    estimatedParticipants = normalized.estimatedParticipants.toLong(),
                    estimatedBudgetPerPerson = normalized.estimatedBudgetPerPerson,
                    description = normalized.description,
                    updatedAt = now,
                    sourceTimeSlotId = persistedSourceTimeSlotId(
                        normalized.eventId,
                        normalized.sourceTimeSlotId
                    ),
                    sourcePotentialLocationId = normalized.sourcePotentialLocationId,
                    generationType = normalized.generationType.name,
                    id = normalized.id,
                    eventId = normalized.eventId,
                    updatedAt_ = normalized.updatedAt
                )
                val updatedScenario = scenarioQueries.selectById(normalized.id).executeAsOneOrNull()
                if (updatedScenario?.updatedAt != now) rollback(false)

                eventQueries.advanceAggregateRevisionIfCurrent(
                    updatedAt = now,
                    id = normalized.eventId,
                    aggregateRevision = parent.aggregateRevision
                )
                val updatedParent = eventQueries.selectById(normalized.eventId).executeAsOneOrNull()
                if (updatedParent?.aggregateRevision != parent.aggregateRevision + 1L) {
                    rollback(false)
                }
                invitationExperienceQueries.clearAggregateWriteAuthorization(
                    normalized.eventId,
                    authorizationId
                )
                true
            }
            if (!committed) {
                return Result.failure(
                    IllegalStateException("Scenario aggregate writer is incompatible or stale")
                )
            }
            Result.success(normalized.copy(updatedAt = now))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update the status of a scenario.
     */
    suspend fun updateScenarioStatus(scenarioId: String, status: ScenarioStatus): Result<Unit> {
        return try {
            val scenario = scenarioQueries.selectById(scenarioId).executeAsOneOrNull()
                ?: return Result.failure(IllegalArgumentException("Scenario not found: $scenarioId"))
            val parent = eventQueries.selectById(scenario.eventId).executeAsOneOrNull()
                ?: return Result.failure(IllegalArgumentException("Event not found: ${scenario.eventId}"))
            if (parent.status == EventStatus.DRAFT.name && parent.aggregateRevision > 1L) {
                return Result.failure(
                    IllegalStateException("Scenario status requires an actor-bound expected revision")
                )
            }
            val now = getCurrentUtcIsoString()
            val committed = db.transactionWithResult {
                val currentParent = eventQueries.selectById(scenario.eventId).executeAsOneOrNull()
                    ?: return@transactionWithResult false
                if (
                    currentParent.aggregateRevision != parent.aggregateRevision ||
                    currentParent.aggregateSchemaVersion != 1L ||
                    currentParent.status == EventStatus.FINALIZED.name
                ) return@transactionWithResult false
                val authorizationId = "scenario-status:$scenarioId:${parent.aggregateRevision}"
                if (!authorizeAggregateWrite(
                        scenario.eventId,
                        parent.aggregateRevision,
                        authorizationId,
                        now
                    )
                ) return@transactionWithResult false
                scenarioQueries.updateScenarioStatus(status.name, now, scenarioId)
                eventQueries.advanceAggregateRevisionIfCurrent(
                    now,
                    scenario.eventId,
                    parent.aggregateRevision
                )
                val updatedParent = eventQueries.selectById(scenario.eventId).executeAsOneOrNull()
                if (updatedParent?.aggregateRevision != parent.aggregateRevision + 1L) {
                    rollback(false)
                }
                invitationExperienceQueries.clearAggregateWriteAuthorization(
                    scenario.eventId,
                    authorizationId
                )
                true
            }
            if (!committed) {
                return Result.failure(
                    IllegalStateException("Scenario aggregate writer is incompatible or stale")
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Select one final scenario for an event and explicitly reject all competitors.
     */
    suspend fun selectFinalScenario(eventId: String, scenarioId: String): Result<Unit> {
        return try {
            val now = getCurrentUtcIsoString()
            val event = eventQueries.selectById(eventId).executeAsOneOrNull()
                ?: return Result.failure(IllegalArgumentException("Event not found"))
            if (event.status == "FINALIZED") {
                return Result.failure(IllegalStateException("Finalized events are read-only"))
            }
            val scenarios = getScenariosByEventId(eventId)
            if (scenarios.none { it.id == scenarioId }) {
                return Result.failure(IllegalArgumentException("Scenario not found"))
            }

            db.transaction {
                val authorizationId = "scenario-select:$scenarioId:${event.aggregateRevision}"
                if (!authorizeAggregateWrite(
                        eventId,
                        event.aggregateRevision,
                        authorizationId,
                        now
                    )
                ) error("Scenario selection aggregate writer is incompatible or stale")
                scenarios.forEach { scenario ->
                    val status = if (scenario.id == scenarioId) {
                        ScenarioStatus.SELECTED
                    } else {
                        ScenarioStatus.REJECTED
                    }
                    scenarioQueries.updateScenarioStatus(
                        status = status.name,
                        updatedAt = now,
                        id = scenario.id
                    )
                }
                val isMatrix = scenarios.firstOrNull { it.id == scenarioId }
                    ?.generationType == ScenarioGenerationType.MATRIX
                if (isMatrix) {
                    confirmMatrixScenario(eventId, scenarioId, event.organizerId, now)
                } else {
                    eventQueries.advanceAggregateRevisionIfCurrent(
                        now,
                        eventId,
                        event.aggregateRevision
                    )
                }
                val updatedParent = eventQueries.selectById(eventId).executeAsOneOrNull()
                if (updatedParent?.aggregateRevision != event.aggregateRevision + 1L) {
                    error("Scenario selection aggregate writer is incompatible or stale")
                }
                queueSyncMetadata(
                    id = "sync_scenario_selection_${eventId}_$scenarioId",
                    entityType = "scenario_selection",
                    entityId = eventId,
                    operation = "UPSERT",
                    timestamp = "${now}_SELECTED_$scenarioId"
                )
                invitationExperienceQueries.clearAggregateWriteAuthorization(
                    eventId,
                    authorizationId
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a scenario (cascade deletes votes).
     */
    suspend fun deleteScenario(scenarioId: String): Result<Unit> {
        return try {
            val scenario = scenarioQueries.selectById(scenarioId).executeAsOneOrNull()
                ?: return Result.failure(IllegalArgumentException("Scenario not found: $scenarioId"))
            val parent = eventQueries.selectById(scenario.eventId).executeAsOneOrNull()
                ?: return Result.failure(IllegalArgumentException("Event not found: ${scenario.eventId}"))
            if (parent.status == EventStatus.DRAFT.name && parent.aggregateRevision > 1L) {
                return Result.failure(
                    IllegalStateException("Scenario deletion requires an actor-bound expected revision")
                )
            }
            val now = getCurrentUtcIsoString()
            val committed = db.transactionWithResult {
                val currentParent = eventQueries.selectById(scenario.eventId).executeAsOneOrNull()
                    ?: return@transactionWithResult false
                if (
                    currentParent.aggregateRevision != parent.aggregateRevision ||
                    currentParent.aggregateSchemaVersion != 1L ||
                    currentParent.status == EventStatus.FINALIZED.name
                ) return@transactionWithResult false
                val authorizationId = "scenario-delete:$scenarioId:${parent.aggregateRevision}"
                if (!authorizeAggregateWrite(
                        scenario.eventId,
                        parent.aggregateRevision,
                        authorizationId,
                        now
                    )
                ) return@transactionWithResult false
                scenarioQueries.deleteScenario(scenarioId)
                eventQueries.advanceAggregateRevisionIfCurrent(
                    now,
                    scenario.eventId,
                    parent.aggregateRevision
                )
                val updatedParent = eventQueries.selectById(scenario.eventId).executeAsOneOrNull()
                if (updatedParent?.aggregateRevision != parent.aggregateRevision + 1L) {
                    rollback(false)
                }
                invitationExperienceQueries.clearAggregateWriteAuthorization(
                    scenario.eventId,
                    authorizationId
                )
                true
            }
            if (!committed) {
                return Result.failure(
                    IllegalStateException("Scenario aggregate writer is incompatible or stale")
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add or update a vote for a scenario.
     * If the participant has already voted, their vote is updated.
     */
    suspend fun addVote(vote: ScenarioVote): Result<ScenarioVote> {
        return try {
            requireScenarioExists(vote.scenarioId)
            // Check if vote already exists
            val existingVote = scenarioVoteQueries.selectByScenarioIdAndParticipantId(
                vote.scenarioId,
                vote.participantId
            ).executeAsOneOrNull()

            if (existingVote != null) {
                // Update existing vote
                scenarioVoteQueries.updateScenarioVote(
                    vote = vote.vote.name,
                    scenarioId = vote.scenarioId,
                    participantId = vote.participantId
                )
            } else {
                // Insert new vote
                scenarioVoteQueries.insertScenarioVote(
                    id = vote.id,
                    scenarioId = vote.scenarioId,
                    participantId = vote.participantId,
                    vote = vote.vote.name,
                    createdAt = vote.createdAt
                )
            }
            queueSyncMetadata(
                id = "sync_scenario_vote_${vote.scenarioId}_${vote.participantId}",
                entityType = "scenario_vote",
                entityId = "${vote.scenarioId}:${vote.participantId}",
                operation = "UPSERT",
                timestamp = "${getCurrentUtcIsoString()}_UPSERT_${vote.scenarioId}_${vote.participantId}"
            )
            Result.success(vote)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an existing vote.
     */
    suspend fun updateVote(vote: ScenarioVote): Result<Unit> {
        return try {
            requireVoteExists(vote.scenarioId, vote.participantId)
            scenarioVoteQueries.updateScenarioVote(
                vote = vote.vote.name,
                scenarioId = vote.scenarioId,
                participantId = vote.participantId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all votes for a scenario.
     */
    fun getVotesByScenarioId(scenarioId: String): List<ScenarioVote> {
        return scenarioVoteQueries.selectByScenarioId(scenarioId).executeAsList().map { row ->
            ScenarioVote(
                id = row.id,
                scenarioId = row.scenarioId,
                participantId = row.participantId,
                vote = ScenarioVoteType.valueOf(row.vote),
                createdAt = row.createdAt
            )
        }
    }

    /**
     * Get voting result for a scenario.
     */
    fun getVotingResult(scenarioId: String): ScenarioVotingResult? {
        val result = scenarioVoteQueries.selectVotingResultByScenarioId(scenarioId)
            .executeAsOneOrNull()

        return if (result != null) {
            val preferCount = result.preferCount?.toInt() ?: 0
            val neutralCount = result.neutralCount?.toInt() ?: 0
            val againstCount = result.againstCount?.toInt() ?: 0
            val totalVotes = result.totalVotes?.toInt() ?: 0
            val score = preferCount * 2 + neutralCount - againstCount

            ScenarioVotingResult(
                scenarioId = scenarioId,
                preferCount = preferCount,
                neutralCount = neutralCount,
                againstCount = againstCount,
                totalVotes = totalVotes,
                score = score
            )
        } else {
            // No votes yet
            ScenarioVotingResult(
                scenarioId = scenarioId,
                preferCount = 0,
                neutralCount = 0,
                againstCount = 0,
                totalVotes = 0,
                score = 0
            )
        }
    }

    /**
     * Get scenarios with their votes and voting results.
     */
    fun getScenariosWithVotes(eventId: String): List<ScenarioWithVotes> {
        val scenarios = getScenariosByEventId(eventId)
        return scenarios.map { scenario ->
            val votes = getVotesByScenarioId(scenario.id)
            val votingResult = getVotingResult(scenario.id) ?: ScenarioVotingResult(
                scenarioId = scenario.id,
                preferCount = 0,
                neutralCount = 0,
                againstCount = 0,
                totalVotes = 0,
                score = 0
            )
            ScenarioWithVotes(
                scenario = scenario,
                votes = votes,
                votingResult = votingResult
            )
        }
    }

    /**
     * Delete a vote.
     */
    suspend fun deleteVote(scenarioId: String, participantId: String): Result<Unit> {
        return try {
            requireVoteExists(scenarioId, participantId)
            scenarioVoteQueries.deleteByScenarioIdAndParticipantId(scenarioId, participantId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Count scenarios for an event.
     */
    fun countScenarios(eventId: String): Long {
        return scenarioQueries.countByEventId(eventId).executeAsOne()
    }

    /**
     * Count scenarios by status.
     */
    fun countScenariosByStatus(eventId: String, status: ScenarioStatus): Long {
        return scenarioQueries.countByEventIdAndStatus(eventId, status.name).executeAsOne()
    }

    /**
     * Generate missing draft matrix scenarios from the event's time slots and potential locations.
     */
    suspend fun generateScenarioMatrix(eventId: String): Result<List<Scenario>> {
        return try {
            val event = eventQueries.selectById(eventId).executeAsOneOrNull()
                ?: return Result.failure(IllegalArgumentException("Event not found"))
            if (parseEventPlanningMode(event.planningMode) != EventPlanningMode.SCENARIO_MATRIX) {
                return Result.failure(IllegalStateException("Event is not using scenario matrix planning mode"))
            }
            if (parseEventStatus(event.status) != EventStatus.DRAFT) {
                return Result.failure(IllegalStateException("Scenario matrix can only be generated while event is DRAFT"))
            }

            val now = getCurrentUtcIsoString()
            val existingScenarios = getScenariosByEventId(eventId)
            val generated = ScenarioMatrixGenerationService.generateDraftScenarios(
                eventId = eventId,
                timeSlots = timeSlotQueries.selectByEventId(eventId).executeAsList().map {
                    TimeSlot(
                        id = logicalSourceTimeSlotId(eventId, it.id)
                            ?: error("Time slot identity is missing"),
                        start = it.startTime,
                        end = it.endTime,
                        timezone = it.timezone,
                        timeOfDay = parseTimeOfDay(it.timeOfDay)
                    )
                },
                potentialLocations = potentialLocationQueries.selectByEventId(eventId).executeAsList().map {
                    PotentialLocation(
                        id = it.id,
                        eventId = it.eventId,
                        name = it.name,
                        locationType = parseLocationType(it.locationType),
                        address = it.address,
                        coordinates = it.coordinates?.let(Coordinates::fromJson),
                        createdAt = it.createdAt
                    )
                },
                existingScenarios = existingScenarios,
                estimatedParticipants = event.expectedParticipants?.toInt()
                    ?: event.maxParticipants?.toInt()
                    ?: event.minParticipants?.toInt()
                    ?: participantCountForEvent(eventId),
                now = now
            )

            db.transaction {
                val authorizationId = "scenario-matrix-generate:$eventId:${event.aggregateRevision}"
                if (!authorizeAggregateWrite(
                        eventId,
                        event.aggregateRevision,
                        authorizationId,
                        now
                    )
                ) error("Scenario matrix aggregate writer is incompatible or stale")
                generated.forEach { scenario ->
                    val normalized = scenario.normalized()
                    insertScenario(normalized, now)
                    queueSyncMetadata(
                        id = "sync_scenario_${normalized.id}",
                        entityType = "scenario",
                        entityId = normalized.id,
                        operation = "CREATE",
                        timestamp = "${now}_MATRIX_CREATE_${normalized.id}"
                    )
                }
                if (generated.isNotEmpty()) {
                    eventQueries.advanceAggregateRevisionIfCurrent(
                        now,
                        eventId,
                        event.aggregateRevision
                    )
                    val updatedParent = eventQueries.selectById(eventId).executeAsOneOrNull()
                    if (updatedParent?.aggregateRevision != event.aggregateRevision + 1L) {
                        error("Scenario matrix aggregate writer is incompatible or stale")
                    }
                }
                invitationExperienceQueries.clearAggregateWriteAuthorization(
                    eventId,
                    authorizationId
                )
            }

            Result.success(generated.map { it.normalized() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun publishScenarioMatrix(eventId: String): Result<Unit> {
        return try {
            val event = eventQueries.selectById(eventId).executeAsOneOrNull()
                ?: return Result.failure(IllegalArgumentException("Event not found"))
            if (parseEventPlanningMode(event.planningMode) != EventPlanningMode.SCENARIO_MATRIX) {
                return Result.failure(IllegalStateException("Event is not using scenario matrix planning mode"))
            }
            if (parseEventStatus(event.status) != EventStatus.DRAFT) {
                return Result.failure(IllegalStateException("Scenario matrix can only be published from DRAFT status"))
            }
            if (countScenariosByStatus(eventId, ScenarioStatus.DRAFT) == 0L) {
                return Result.failure(IllegalStateException("No draft matrix scenarios to publish"))
            }

            val now = getCurrentUtcIsoString()
            db.transaction {
                val authorizationId = "scenario-matrix-publish:$eventId:${event.aggregateRevision}"
                if (!authorizeAggregateWrite(
                        eventId,
                        event.aggregateRevision,
                        authorizationId,
                        now
                    )
                ) error("Scenario matrix aggregate writer is incompatible or stale")
                scenarioQueries.publishDraftMatrixScenarios(now, eventId)
                eventQueries.updateEventStatusIfRevision(
                    EventStatus.COMPARING.name,
                    now,
                    eventId,
                    event.aggregateRevision
                )
                val updatedParent = eventQueries.selectById(eventId).executeAsOneOrNull()
                if (updatedParent?.aggregateRevision != event.aggregateRevision + 1L) {
                    error("Scenario matrix aggregate writer is incompatible or stale")
                }
                queueSyncMetadata(
                    id = "sync_scenario_matrix_publish_$eventId",
                    entityType = "scenario_matrix",
                    entityId = eventId,
                    operation = "PUBLISH",
                    timestamp = "${now}_PUBLISH_$eventId"
                )
                queueSyncMetadata(
                    id = "sync_event_matrix_publish_$eventId",
                    entityType = "event",
                    entityId = eventId,
                    operation = "UPDATE",
                    timestamp = "${now}_COMPARING_$eventId"
                )
                invitationExperienceQueries.clearAggregateWriteAuthorization(
                    eventId,
                    authorizationId
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun selectFinalMatrixScenario(eventId: String, scenarioId: String): Result<Unit> {
        return selectFinalScenario(eventId, scenarioId)
    }

    /**
     * Helper function to get current UTC ISO string.
     */
    private fun getCurrentUtcIsoString(): String {
        // This is a simplified implementation
        // In production, use kotlinx-datetime or platform-specific date APIs
        return "2025-11-25T10:00:00Z"
    }

    private fun nextMutationTimestamp(previous: String): String =
        runCatching { Instant.parse(previous).plus(1.nanoseconds).toString() }
            .getOrElse { Clock.System.now().toString() }

    private fun authorizeAggregateWrite(
        eventId: String,
        expectedRevision: Long,
        operationId: String,
        now: String
    ): Boolean {
        invitationExperienceQueries.authorizeAggregateWrite(
            writer_schema_version = 1L,
            operation_id = operationId,
            created_at = now,
            id = eventId,
            aggregateRevision = expectedRevision,
            aggregateSchemaVersion = 1L
        )
        val authorization = invitationExperienceQueries
            .selectAggregateWriteAuthorization(eventId)
            .executeAsOneOrNull()
        return authorization?.operation_id == operationId &&
            authorization.expected_revision == expectedRevision
    }

    private fun insertScenario(scenario: Scenario, updatedAt: String) {
        scenarioQueries.insertScenarioWithMetadata(
            id = scenario.id,
            eventId = scenario.eventId,
            name = scenario.name,
            dateOrPeriod = scenario.dateOrPeriod,
            location = scenario.location,
            duration = scenario.duration.toLong(),
            estimatedParticipants = scenario.estimatedParticipants.toLong(),
            estimatedBudgetPerPerson = scenario.estimatedBudgetPerPerson,
            description = scenario.description,
            status = scenario.status.name,
            createdAt = scenario.createdAt,
            updatedAt = updatedAt,
            sourceTimeSlotId = persistedSourceTimeSlotId(scenario.eventId, scenario.sourceTimeSlotId),
            sourcePotentialLocationId = scenario.sourcePotentialLocationId,
            generationType = scenario.generationType.name
        )
    }

    private fun requireScenarioExists(scenarioId: String) {
        require(scenarioQueries.selectById(scenarioId).executeAsOneOrNull() != null) {
            "Scenario not found: $scenarioId"
        }
    }

    private fun requireVoteExists(scenarioId: String, participantId: String) {
        require(
            scenarioVoteQueries.selectByScenarioIdAndParticipantId(scenarioId, participantId)
                .executeAsOneOrNull() != null
        ) {
            "Scenario vote not found: $scenarioId/$participantId"
        }
    }

    private fun confirmMatrixScenario(eventId: String, scenarioId: String, organizerId: String, now: String) {
        val scenario = getScenarioById(scenarioId) ?: return
        val slotId = scenario.sourceTimeSlotId ?: throw IllegalStateException("Matrix scenario has no source time slot")
        val persistedSlotId = persistedSourceTimeSlotId(eventId, slotId)
            ?: throw IllegalStateException("Matrix scenario has no source time slot")
        val selectedSlot = timeSlotQueries.selectById(persistedSlotId).executeAsOneOrNull()
            ?: throw IllegalStateException("Source time slot not found")
        if (selectedSlot.eventId != eventId) {
            throw IllegalStateException("Source time slot does not belong to event")
        }

        eventQueries.updateEventStatus(EventStatus.CONFIRMED.name, now, eventId)
        confirmedDateQueries.deleteByEventId(eventId)
        confirmedDateQueries.insertConfirmedDate(
            id = "confirmed_$eventId",
            eventId = eventId,
            timeslotId = persistedSlotId,
            confirmedByOrganizerId = organizerId,
            confirmedAt = now,
            updatedAt = now
        )
        queueSyncMetadata(
            id = "sync_matrix_confirm_${eventId}_$scenarioId",
            entityType = "event",
            entityId = eventId,
            operation = "UPDATE",
            timestamp = "${now}_MATRIX_CONFIRMED_$scenarioId"
        )
    }

    private fun parseScenarioGenerationType(value: String?): ScenarioGenerationType {
        return enumValueOrNull<ScenarioGenerationType>(value) ?: ScenarioGenerationType.MANUAL
    }

    private fun parseEventPlanningMode(value: String?): EventPlanningMode {
        return enumValueOrNull<EventPlanningMode>(value) ?: EventPlanningMode.TIME_SLOT_POLL
    }

    private fun parseEventStatus(value: String?): EventStatus {
        return enumValueOrNull<EventStatus>(value) ?: EventStatus.DRAFT
    }

    private fun parseTimeOfDay(value: String?): TimeOfDay {
        return enumValueOrNull<TimeOfDay>(value) ?: TimeOfDay.SPECIFIC
    }

    private fun parseLocationType(value: String?): LocationType {
        return enumValueOrNull<LocationType>(value) ?: LocationType.CITY
    }

    private fun participantCountForEvent(eventId: String): Int {
        return db.participantQueries.selectByEventId(eventId).executeAsList().size.coerceAtLeast(1)
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? {
        val normalized = value?.trim()?.uppercase()?.replace('-', '_') ?: return null
        return enumValues<T>().firstOrNull { it.name == normalized }
    }

    private fun queueSyncMetadata(
        id: String,
        entityType: String,
        entityId: String,
        operation: String,
        timestamp: String
    ) {
        val existingForEntity = syncMetadataQueries.selectByEntity(entityType, entityId).executeAsList()
        val uniqueId = if (syncMetadataQueries.selectById(id).executeAsOneOrNull() == null) {
            id
        } else {
            "${id}_${existingForEntity.size}"
        }
        val uniqueTimestamp = if (existingForEntity.none { it.timestamp == timestamp }) {
            timestamp
        } else {
            "${timestamp}_${existingForEntity.size}"
        }
        syncMetadataQueries.insertSyncMetadata(
            id = uniqueId,
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            timestamp = uniqueTimestamp,
            synced = 0
        )
    }

    private fun persistedSourceTimeSlotId(eventId: String, slotId: String?): String? {
        slotId ?: return null
        val decoded = TimeSlotStorageIdentity.decode(slotId)
        return if (decoded?.eventId == eventId) {
            slotId
        } else {
            TimeSlotStorageIdentity.physicalId(eventId, slotId)
        }
    }

    private fun logicalSourceTimeSlotId(eventId: String, slotId: String?): String? {
        slotId ?: return null
        val decoded = TimeSlotStorageIdentity.decode(slotId) ?: return slotId
        check(decoded.eventId == eventId) { "Scenario source slot belongs to another event" }
        return decoded.logicalSlotId
    }
}
