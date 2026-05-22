package com.guyghost.wakeve.transport

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.DepartureLocationRecord
import com.guyghost.wakeve.models.OptimizationType
import com.guyghost.wakeve.models.TransportLocation
import com.guyghost.wakeve.models.TransportPlan
import com.guyghost.wakeve.models.TransportReadiness

/**
 * Swift-friendly adapter around TransportRepository.
 *
 * Kotlin Result is awkward across the Objective-C bridge, so this class exposes
 * simple nullable/boolean methods while preserving the shared repository as the
 * only writer for transport offline state and sync metadata.
 */
class TransportRepositoryBridge(database: WakeveDb) {
    private val transportRepository = TransportRepository(database)

    suspend fun getReadiness(
        eventId: String,
        destination: TransportLocation
    ): TransportReadiness? {
        return runCatching {
            transportRepository.getReadiness(eventId, destination)
        }.getOrNull()
    }

    suspend fun saveDepartureLocation(
        eventId: String,
        participantId: String,
        location: TransportLocation,
        updatedByUserId: String
    ): DepartureLocationRecord? {
        return transportRepository.saveDepartureLocation(
            eventId = eventId,
            participantId = participantId,
            location = location,
            updatedByUserId = updatedByUserId
        ).getOrNull()
    }

    suspend fun generatePlan(
        eventId: String,
        destination: TransportLocation,
        optimizationType: OptimizationType,
        generatedByUserId: String
    ): TransportPlan? {
        return transportRepository.generatePlan(
            eventId = eventId,
            destination = destination,
            optimizationType = optimizationType,
            generatedByUserId = generatedByUserId
        ).getOrNull()
    }

    fun getPlansByEvent(eventId: String): List<TransportPlan> {
        return transportRepository.getPlansByEvent(eventId)
    }

    fun getSelectedPlanId(eventId: String): String? {
        return transportRepository.getSelectedPlanId(eventId)
    }

    fun selectFinalPlan(
        eventId: String,
        planId: String,
        selectedByOrganizerId: String
    ): Boolean {
        return transportRepository.selectFinalPlan(
            eventId = eventId,
            planId = planId,
            selectedByOrganizerId = selectedByOrganizerId
        ).isSuccess
    }

    fun markTransportNotNeeded(eventId: String, updatedByUserId: String): Boolean {
        return transportRepository.markTransportNotNeeded(
            eventId = eventId,
            updatedByUserId = updatedByUserId
        ).isSuccess
    }

    fun hasPendingTransportSync(eventId: String): Boolean {
        return transportRepository.hasPendingTransportSync(eventId)
    }
}
