package com.guyghost.wakeve.transport

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.DepartureLocationRecord
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.OptimizationType
import com.guyghost.wakeve.models.Route
import com.guyghost.wakeve.models.SelectedTransportPlanSummary
import com.guyghost.wakeve.models.TransportLocation
import com.guyghost.wakeve.models.TransportOption
import com.guyghost.wakeve.models.TransportPlan
import com.guyghost.wakeve.models.TransportReadiness
import com.guyghost.wakeve.sync.PendingSyncOperation
import com.guyghost.wakeve.sync.SyncOperationType
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TransportRepository(
    private val database: WakeveDb,
    private val optionProvider: suspend (
        participantId: String,
        departure: TransportLocation,
        destination: TransportLocation,
        eventTime: String
    ) -> List<TransportOption> = NoConfiguredTransportOptionProvider::optionsFor
) {
    private val transportQueries = database.transportQueries
    private val participantQueries = database.participantQueries
    private val eventQueries = database.eventQueries
    private val userQueries = database.userQueries
    private val syncMetadataQueries = database.syncMetadataQueries
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun getReadiness(eventId: String, destination: TransportLocation): TransportReadiness {
        val confirmedParticipants = confirmedParticipants(eventId)
        val departures = transportQueries.selectDepartureLocationsByEvent(eventId)
            .executeAsList()
            .associateBy { it.participant_id }
        val missing = confirmedParticipants.filterNot { departures.containsKey(it.userId) }
        val transportNotNeeded = transportQueries.selectTransportEventStatus(eventId)
            .executeAsOneOrNull()
            ?.transport_not_needed == 1L

        return TransportReadiness(
            eventId = eventId,
            destination = destination,
            isComplete = confirmedParticipants.isNotEmpty() && missing.isEmpty(),
            canGeneratePlan = !transportNotNeeded && confirmedParticipants.isNotEmpty() && missing.isEmpty(),
            transportNotNeeded = transportNotNeeded,
            canFinalizeWithoutPlan = transportNotNeeded,
            missingDepartureParticipantIds = missing.map { it.userId },
            missingDepartureParticipantNames = missing.map { it.displayName }
        )
    }

    suspend fun saveDepartureLocation(
        eventId: String,
        participantId: String,
        location: TransportLocation,
        updatedByUserId: String
    ): Result<DepartureLocationRecord> = runCatching {
        requireMutableTransportWorkflow(eventId)
        requireCanSaveDepartureLocation(eventId, participantId, updatedByUserId)
        val normalizedLocation = location.normalizedAndValidated()
        val now = now()
        transportQueries.upsertDepartureLocation(
            event_id = eventId,
            participant_id = participantId,
            location_json = json.encodeToString(normalizedLocation),
            updated_by_user_id = updatedByUserId,
            updated_at = now
        )
        queueSyncMetadata(
            id = "sync_transport_departure_${eventId}_${participantId}_$now",
            entityType = "transport_departure_location",
            entityId = "$eventId:$participantId",
            operation = "UPSERT",
            timestamp = now
        )
        DepartureLocationRecord(eventId, participantId, normalizedLocation, updatedByUserId, now)
    }

    fun getDepartureLocation(eventId: String, participantId: String): DepartureLocationRecord? {
        return transportQueries.selectDepartureLocation(eventId, participantId)
            .executeAsOneOrNull()
            ?.let { row ->
                DepartureLocationRecord(
                    eventId = row.event_id,
                    participantId = row.participant_id,
                    location = json.decodeFromString(row.location_json),
                    updatedByUserId = row.updated_by_user_id,
                    updatedAt = row.updated_at
                )
            }
    }

    fun markTransportNotNeeded(eventId: String, updatedByUserId: String): Result<Unit> = runCatching {
        requireMutableTransportWorkflow(eventId)
        requireOrganizer(eventId, updatedByUserId)
        val now = now()
        transportQueries.upsertTransportEventStatus(
            event_id = eventId,
            transport_not_needed = 1,
            updated_by_user_id = updatedByUserId,
            updated_at = now
        )
        queueSyncMetadata(
            id = "sync_transport_not_needed_${eventId}_$now",
            entityType = "transport_event_status",
            entityId = eventId,
            operation = "UPDATE",
            timestamp = now
        )
    }

    suspend fun generatePlan(
        eventId: String,
        destination: TransportLocation,
        optimizationType: OptimizationType,
        generatedByUserId: String
    ): Result<TransportPlan> = runCatching {
        requireMutableTransportWorkflow(eventId)
        requireOrganizer(eventId, generatedByUserId)
        val normalizedDestination = destination.normalizedAndValidated()
        requireRealSelectedDestination(normalizedDestination)
        val readiness = getReadiness(eventId, normalizedDestination)
        require(readiness.canGeneratePlan) {
            "Cannot generate transport plan: departure locations are missing"
        }

        val event = eventQueries.selectById(eventId).executeAsOneOrNull()
            ?: error("Event not found")
        val eventTime = database.confirmedDateQueries
            .selectWithTimeslotDetails(eventId)
            .executeAsOneOrNull()
            ?.startTime
            ?: event.deadline
        val createdAt = now()
        val planId = "transport_plan_${eventId}_${optimizationType.name}_${createdAt.hashCode().toUInt()}"
        val routes = confirmedParticipants(eventId).associate { participant ->
            val departure = getDepartureLocation(eventId, participant.userId)?.location
                ?: error("Missing departure location for ${participant.userId}")
            val options = optionProvider(participant.userId, departure, normalizedDestination, eventTime)
            val option = chooseBestOption(options, optimizationType)
            participant.userId to Route(
                id = "transport_route_${planId}_${participant.userId}",
                segments = listOf(option),
                totalDurationMinutes = option.durationMinutes,
                totalCost = option.cost,
                currency = option.currency,
                score = score(option, optimizationType)
            )
        }
        val groupArrivals = routes.values
            .flatMap { route -> route.segments.map { it.arrivalTime } }
            .distinct()
            .sorted()
        val plan = TransportPlan(
            id = planId,
            eventId = eventId,
            participantRoutes = routes,
            groupArrivals = groupArrivals,
            totalGroupCost = routes.values.sumOf { it.totalCost },
            optimizationType = optimizationType,
            createdAt = createdAt
        )

        database.transaction {
            transportQueries.insertPlan(
                id = plan.id,
                event_id = eventId,
                destination_json = json.encodeToString(normalizedDestination),
                optimization_type = optimizationType.name,
                total_group_cost = plan.totalGroupCost,
                group_arrivals_json = json.encodeToString(groupArrivals),
                created_at = createdAt
            )
            routes.forEach { (participantId, route) ->
                transportQueries.insertRoute(
                    id = route.id,
                    plan_id = plan.id,
                    event_id = eventId,
                    participant_id = participantId,
                    segments = encodeOptions(route.segments),
                    total_duration_minutes = route.totalDurationMinutes.toLong(),
                    total_cost = route.totalCost,
                    currency = route.currency,
                    score = route.score,
                    optimization_type = optimizationType.name,
                    created_at = createdAt,
                    updated_at = createdAt
                )
            }
        }
        queueSyncMetadata(
            id = "sync_transport_plan_${plan.id}",
            entityType = "transport_plan",
            entityId = plan.id,
            operation = "CREATE",
            timestamp = createdAt
        )
        plan
    }

    fun getRoutesByPlan(planId: String): List<Route> {
        return transportQueries.selectRoutesByPlan(planId)
            .executeAsList()
            .map { row ->
                Route(
                    id = row.id,
                    segments = decodeOptions(row.segments),
                    totalDurationMinutes = row.total_duration_minutes.toInt(),
                    totalCost = row.total_cost,
                    currency = row.currency,
                    score = row.score
                )
            }
    }

    fun getPlansByEvent(eventId: String): List<TransportPlan> {
        return transportQueries.selectPlansByEvent(eventId)
            .executeAsList()
            .map { row ->
                val routes = transportQueries.selectRoutesByPlan(row.id)
                    .executeAsList()
                    .associate { routeRow ->
                        routeRow.participant_id to Route(
                            id = routeRow.id,
                            segments = decodeOptions(routeRow.segments),
                            totalDurationMinutes = routeRow.total_duration_minutes.toInt(),
                            totalCost = routeRow.total_cost,
                            currency = routeRow.currency,
                            score = routeRow.score
                        )
                    }

                TransportPlan(
                    id = row.id,
                    eventId = row.event_id,
                    participantRoutes = routes,
                    groupArrivals = json.decodeFromString(ListSerializer(String.serializer()), row.group_arrivals_json),
                    totalGroupCost = row.total_group_cost,
                    optimizationType = OptimizationType.valueOf(row.optimization_type),
                    createdAt = row.created_at
                )
            }
    }

    fun getSelectedPlanId(eventId: String): String? {
        return transportQueries.selectSelectedPlan(eventId)
            .executeAsOneOrNull()
            ?.plan_id
    }

    fun hasPendingTransportSync(eventId: String): Boolean {
        return syncMetadataQueries.selectPending()
            .executeAsList()
            .any { pending ->
                isReplayableTransportSyncForEvent(
                    entityType = pending.entityType,
                    entityId = pending.entityId,
                    operation = pending.operation,
                    eventId = eventId
                )
            }
    }

    fun selectFinalPlan(
        eventId: String,
        planId: String,
        selectedByOrganizerId: String,
        selectedAt: String = now()
    ): Result<Unit> = runCatching {
        requireMutableTransportWorkflow(eventId)
        requireOrganizer(eventId, selectedByOrganizerId)
        val plan = transportQueries.selectPlanById(planId).executeAsOneOrNull()
            ?: error("Transport plan not found")
        require(plan.event_id == eventId) { "Transport plan does not belong to this event" }
        transportQueries.upsertSelectedPlan(eventId, planId, selectedAt, selectedByOrganizerId)
        queueSyncMetadata(
            id = "sync_transport_selection_${eventId}_$selectedAt",
            entityType = "transport_plan_selection",
            entityId = eventId,
            operation = "UPDATE",
            timestamp = selectedAt
        )
    }

    fun deletePlan(
        eventId: String,
        planId: String,
        deletedByUserId: String
    ): Result<Unit> = runCatching {
        requireMutableTransportWorkflow(eventId)
        requireOrganizer(eventId, deletedByUserId)
        val plan = transportQueries.selectPlanById(planId).executeAsOneOrNull()
            ?: error("Transport plan not found")
        require(plan.event_id == eventId) { "Transport plan does not belong to this event" }

        val deletedAt = now()
        transportQueries.deletePlan(planId)
        queueSyncMetadata(
            id = "sync_transport_plan_delete_${planId}_$deletedAt",
            entityType = "transport_plan",
            entityId = planId,
            operation = "DELETE",
            timestamp = deletedAt
        )
    }

    suspend fun getSelectedPlanSummary(eventId: String): SelectedTransportPlanSummary? {
        val selection = transportQueries.selectSelectedPlan(eventId).executeAsOneOrNull() ?: return null
        val plan = transportQueries.selectPlanById(selection.plan_id).executeAsOneOrNull() ?: return null
        val destination = json.decodeFromString<TransportLocation>(plan.destination_json)
        return SelectedTransportPlanSummary(
            eventId = eventId,
            planId = plan.id,
            totalCost = plan.total_group_cost,
            optimizationType = OptimizationType.valueOf(plan.optimization_type),
            selectedAt = selection.selected_at,
            readiness = getReadiness(eventId, destination)
        )
    }

    fun applyRemoteSelectedPlan(eventId: String, planId: String, selectedAt: String): Result<Unit> = runCatching {
        val plan = transportQueries.selectPlanById(planId).executeAsOneOrNull()
            ?: error("Transport plan not found")
        require(plan.event_id == eventId) { "Transport plan does not belong to this event" }
        val current = transportQueries.selectSelectedPlan(eventId).executeAsOneOrNull()
        if (current == null || selectedAt > current.selected_at) {
            transportQueries.upsertSelectedPlan(eventId, planId, selectedAt, current?.selected_by_user_id)
        }
        queueSyncMetadata(
            id = "sync_transport_selection_conflict_${eventId}_$selectedAt",
            entityType = "transport_plan_selection",
            entityId = eventId,
            operation = "CONFLICT_RESOLVED",
            timestamp = "${selectedAt}_CONFLICT_RESOLVED",
            synced = 1
        )
    }

    fun replayPendingSync(send: (PendingSyncOperation) -> Result<Unit>): Result<Unit> = runCatching {
        val pendingTransportSync = syncMetadataQueries.selectPending()
            .executeAsList()
            .filter { it.entityType.startsWith("transport_") }

        pendingTransportSync
            .filterNot { isReplayableTransportSync(it.entityType, it.operation) }
            .forEach { auditOnly -> syncMetadataQueries.markSynced(auditOnly.id) }

        pendingTransportSync
            .filter { isReplayableTransportSync(it.entityType, it.operation) }
            .forEach { pending ->
                val operation = PendingSyncOperation(
                    id = pending.id,
                    entityType = pending.entityType,
                    entityId = pending.entityId,
                    operation = pending.operation.toSyncOperationType(),
                    createdAt = pending.timestamp
                )
                send(operation).getOrThrow()
                syncMetadataQueries.markSynced(pending.id)
            }
    }

    private fun chooseBestOption(
        options: List<TransportOption>,
        optimizationType: OptimizationType
    ): TransportOption {
        require(options.isNotEmpty()) { "No transport option available" }
        return when (optimizationType) {
            OptimizationType.COST_MINIMIZE -> options.minWith(compareBy<TransportOption> { it.cost }.thenBy { it.durationMinutes })
            OptimizationType.TIME_MINIMIZE -> options.minWith(compareBy<TransportOption> { it.durationMinutes }.thenBy { it.cost })
            OptimizationType.BALANCED -> {
                val minCost = options.minOf { it.cost }
                val maxCost = options.maxOf { it.cost }
                val minDuration = options.minOf { it.durationMinutes }
                val maxDuration = options.maxOf { it.durationMinutes }
                options.minWith(
                    compareBy<TransportOption> {
                        normalized(it.cost, minCost, maxCost) + normalized(
                            it.durationMinutes.toDouble(),
                            minDuration.toDouble(),
                            maxDuration.toDouble()
                        )
                    }.thenBy { it.cost }.thenBy { it.durationMinutes }
                )
            }
        }
    }

    private fun score(option: TransportOption, optimizationType: OptimizationType): Double {
        return when (optimizationType) {
            OptimizationType.COST_MINIMIZE -> option.cost
            OptimizationType.TIME_MINIMIZE -> option.durationMinutes.toDouble()
            OptimizationType.BALANCED -> option.cost + option.durationMinutes
        }
    }

    private fun normalized(value: Double, min: Double, max: Double): Double {
        return if (max == min) 0.0 else (value - min) / (max - min)
    }

    private fun confirmedParticipants(eventId: String): List<ConfirmedTransportParticipant> {
        return participantQueries.selectValidated(eventId)
            .executeAsList()
            .map { participant ->
                ConfirmedTransportParticipant(
                    userId = participant.userId,
                    displayName = userQueries.selectUserById(participant.userId)
                        .executeAsOneOrNull()
                        ?.name
                        ?: participant.userId
                )
            }
    }

    private fun requireMutableTransportWorkflow(eventId: String) {
        val status = eventQueries.selectById(eventId)
            .executeAsOneOrNull()
            ?.status
            ?.let { value -> runCatching { EventStatus.valueOf(value) }.getOrNull() }
            ?: error("Event not found")
        require(status in mutableTransportStatuses) {
            "Transport workflow is read-only while event is $status"
        }
    }

    private fun requireCanSaveDepartureLocation(
        eventId: String,
        participantId: String,
        updatedByUserId: String
    ) {
        require(isConfirmedParticipant(eventId, participantId)) {
            "Transport departure location requires a confirmed participant"
        }

        if (isOrganizer(eventId, updatedByUserId)) {
            return
        }

        require(updatedByUserId == participantId) {
            "Participants may update only their own departure location"
        }
    }

    private fun requireOrganizer(eventId: String, userId: String) {
        require(isOrganizer(eventId, userId)) {
            "Only the event organizer may manage transport plans"
        }
    }

    private fun isOrganizer(eventId: String, userId: String): Boolean {
        return eventQueries.selectById(eventId)
            .executeAsOneOrNull()
            ?.organizerId == userId
    }

    private fun isConfirmedParticipant(eventId: String, userId: String): Boolean {
        val participant = participantQueries.selectByEventIdAndUserId(eventId, userId)
            .executeAsOneOrNull()
            ?: return false

        return participant.hasValidatedDate == 1L &&
            participant.role != "DECLINED" &&
            participant.role != "INVITED"
    }

    private fun requireRealSelectedDestination(destination: TransportLocation) {
        val isFallbackDestination = destination.name.equals("Destination confirmed", ignoreCase = true) ||
            destination.name.equals("Destination confirmee", ignoreCase = true)
        require(destination.name.isNotBlank() && !isFallbackDestination) {
            "Transport plan generation requires a selected scenario destination"
        }
    }

    private fun TransportLocation.normalizedAndValidated(): TransportLocation {
        val normalizedName = name.trim()
        val normalizedAddress = address?.trim()?.takeIf { it.isNotBlank() }
        val normalizedIataCode = iataCode?.trim()?.uppercase()?.takeIf { it.isNotBlank() }

        require(normalizedName.isNotBlank()) { "Transport location name is required" }
        require(normalizedName.length <= MAX_LOCATION_NAME_LENGTH) {
            "Transport location name is too long"
        }
        require(normalizedAddress == null || normalizedAddress.length <= MAX_LOCATION_ADDRESS_LENGTH) {
            "Transport location address is too long"
        }
        require(latitude == null || (latitude.isFinite() && latitude in -90.0..90.0)) {
            "Transport location latitude is invalid"
        }
        require(longitude == null || (longitude.isFinite() && longitude in -180.0..180.0)) {
            "Transport location longitude is invalid"
        }
        require(normalizedIataCode == null || normalizedIataCode.matches(Regex("[A-Z]{3}"))) {
            "Transport location IATA code is invalid"
        }

        return copy(
            name = normalizedName,
            address = normalizedAddress,
            iataCode = normalizedIataCode
        )
    }

    private fun encodeOptions(options: List<TransportOption>): String {
        return json.encodeToString(ListSerializer(TransportOption.serializer()), options)
    }

    private fun decodeOptions(optionsJson: String): List<TransportOption> {
        return json.decodeFromString(ListSerializer(TransportOption.serializer()), optionsJson)
    }

    private fun queueSyncMetadata(
        id: String,
        entityType: String,
        entityId: String,
        operation: String,
        timestamp: String,
        synced: Long = 0
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
            synced = synced
        )
    }

    private fun isReplayableTransportSyncForEvent(
        entityType: String,
        entityId: String,
        operation: String,
        eventId: String
    ): Boolean {
        return isReplayableTransportSync(entityType, operation) &&
            (entityId == eventId ||
                entityId.startsWith("$eventId:") ||
                entityId.contains(eventId))
    }

    private fun isReplayableTransportSync(entityType: String, operation: String): Boolean {
        return entityType.startsWith("transport_") && operation in replayableTransportSyncOperations
    }

    private fun String.toSyncOperationType(): SyncOperationType {
        return when (this) {
            "CREATE" -> SyncOperationType.CREATE
            "DELETE" -> SyncOperationType.DELETE
            else -> SyncOperationType.UPDATE
        }
    }

    private fun now(): String = Clock.System.now().toString()

    private data class ConfirmedTransportParticipant(
        val userId: String,
        val displayName: String
    )

    private companion object {
        val mutableTransportStatuses = setOf(
            EventStatus.CONFIRMED,
            EventStatus.COMPARING,
            EventStatus.ORGANIZING
        )
        val replayableTransportSyncOperations = setOf("CREATE", "UPDATE", "DELETE", "UPSERT")
        const val MAX_LOCATION_NAME_LENGTH = 160
        const val MAX_LOCATION_ADDRESS_LENGTH = 300
    }
}
