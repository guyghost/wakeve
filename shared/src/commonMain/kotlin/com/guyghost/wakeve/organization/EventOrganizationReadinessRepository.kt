package com.guyghost.wakeve.organization

import com.guyghost.wakeve.budget.BudgetReadiness
import com.guyghost.wakeve.budget.BudgetRepository
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.BookingStatus
import com.guyghost.wakeve.payment.PaymentReadiness
import com.guyghost.wakeve.payment.PaymentPotRepository
import com.guyghost.wakeve.payment.TricountHandoffRepository
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class OrganizationReadinessSection(
    val eventId: String,
    val complete: Boolean,
    val blockers: List<String>,
    val count: Int = 0,
    val explicitNotNeeded: Boolean = false
)

@Serializable
data class MeetingReadiness(
    val eventId: String,
    val complete: Boolean,
    val blockers: List<String>,
    val explicitNotNeeded: Boolean,
    val meetingCount: Int
)

@Serializable
data class EventOrganizationReadiness(
    val eventId: String,
    val participants: OrganizationReadinessSection,
    val scenario: OrganizationReadinessSection,
    val destination: OrganizationReadinessSection,
    val lodging: OrganizationReadinessSection,
    val transport: OrganizationReadinessSection,
    val meetings: MeetingReadiness,
    val calendar: OrganizationReadinessSection,
    val notifications: OrganizationReadinessSection,
    val budget: BudgetReadiness,
    val payment: OrganizationReadinessSection,
    val tricount: PaymentReadiness,
    val sync: OrganizationReadinessSection,
    val unsafeLinks: OrganizationReadinessSection,
    val accessControl: OrganizationReadinessSection,
    val complete: Boolean,
    val blockers: List<String>
)

class EventOrganizationReadinessRepository(
    private val db: WakeveDb,
    private val budgetRepository: BudgetRepository = BudgetRepository(db),
    private val paymentPotRepository: PaymentPotRepository = PaymentPotRepository(db),
    private val tricountHandoffRepository: TricountHandoffRepository = TricountHandoffRepository(db)
) {
    fun getMeetingReadiness(eventId: String): MeetingReadiness {
        val meetings = db.meetingQueries.selectByEventId(eventId).executeAsList()
        val notNeeded = db.organizationReadinessDecisionQueries
            .selectByEventAndSection(eventId, "MEETINGS")
            .executeAsOneOrNull()
            ?.notNeeded == 1L
        val complete = meetings.isNotEmpty() || notNeeded
        return MeetingReadiness(
            eventId = eventId,
            complete = complete,
            blockers = if (complete) emptyList() else listOf("MEETING_REQUIRED"),
            explicitNotNeeded = notNeeded,
            meetingCount = meetings.size
        )
    }

    fun markMeetingsNotNeeded(eventId: String, decidedBy: String) {
        db.organizationReadinessDecisionQueries.upsertDecision(
            id = "readiness-${Clock.System.now().toEpochMilliseconds()}-${(0..9999).random()}",
            eventId = eventId,
            section = "MEETINGS",
            notNeeded = 1L,
            decidedBy = decidedBy,
            decidedAt = Clock.System.now().toString()
        )
    }

    fun getReadiness(eventId: String): EventOrganizationReadiness {
        val participants = getParticipantReadiness(eventId)
        val scenario = getScenarioReadiness(eventId)
        val destination = getDestinationReadiness(eventId)
        val lodging = getLodgingReadiness(eventId)
        val transport = getTransportReadiness(eventId)
        val meetings = getMeetingReadiness(eventId)
        val calendar = getCalendarReadiness(eventId)
        val notifications = getNotificationReadiness(eventId, meetings)
        val budget = budgetRepository.getBudgetReadinessForEvent(eventId)
        val payment = getPaymentPotReadiness(eventId)
        val tricount = tricountHandoffRepository.getPaymentReadiness(eventId)
        val sync = getSyncReadiness(eventId)
        val unsafeLinks = getUnsafeLinkReadiness(eventId)
        val accessControl = getAccessControlReadiness(eventId)
        val blockers = listOf(
            participants.blockers,
            scenario.blockers,
            destination.blockers,
            lodging.blockers,
            transport.blockers,
            meetings.blockers,
            calendar.blockers,
            notifications.blockers,
            budget.blockers,
            payment.blockers,
            tricount.blockers,
            sync.blockers,
            unsafeLinks.blockers,
            accessControl.blockers
        ).flatten()
        return EventOrganizationReadiness(
            eventId = eventId,
            participants = participants,
            scenario = scenario,
            destination = destination,
            lodging = lodging,
            transport = transport,
            meetings = meetings,
            calendar = calendar,
            notifications = notifications,
            budget = budget,
            payment = payment,
            tricount = tricount,
            sync = sync,
            unsafeLinks = unsafeLinks,
            accessControl = accessControl,
            complete = blockers.isEmpty(),
            blockers = blockers
        )
    }

    private fun getParticipantReadiness(eventId: String): OrganizationReadinessSection {
        val participants = db.participantQueries.selectByEventId(eventId).executeAsList()
        val confirmed = participants.filter { it.hasValidatedDate == 1L }
        val hasOrganizer = participants.any { it.role == "ORGANIZER" }
        val blockers = buildList {
            if (!hasOrganizer) add("ORGANIZER_REQUIRED")
            if (confirmed.isEmpty()) add("CONFIRMED_PARTICIPANTS_REQUIRED")
        }
        return section(eventId, blockers, count = confirmed.size)
    }

    private fun getScenarioReadiness(eventId: String): OrganizationReadinessSection {
        val selected = db.scenarioQueries.selectSelectedByEventId(eventId).executeAsOneOrNull()
        return section(
            eventId = eventId,
            blockers = if (selected == null) listOf("FINAL_SCENARIO_REQUIRED") else emptyList(),
            count = if (selected == null) 0 else 1
        )
    }

    private fun getDestinationReadiness(eventId: String): OrganizationReadinessSection {
        val selected = db.scenarioQueries.selectSelectedByEventId(eventId).executeAsOneOrNull()
        val hasDestination = !selected?.location.isNullOrBlank()
        return section(
            eventId = eventId,
            blockers = if (hasDestination) emptyList() else listOf("DESTINATION_REQUIRED"),
            count = if (hasDestination) 1 else 0
        )
    }

    private fun getLodgingReadiness(eventId: String): OrganizationReadinessSection {
        val confirmed = db.accommodationQueries
            .getConfirmedAccommodations(eventId)
            .executeAsList()
            .filter { runCatching { BookingStatus.valueOf(it.booking_status) }.getOrNull() == BookingStatus.CONFIRMED }
        val finalScenarioCarriesLodgingDecision =
            db.scenarioQueries.selectSelectedByEventId(eventId).executeAsOneOrNull() != null
        val complete = confirmed.isNotEmpty() || finalScenarioCarriesLodgingDecision
        return section(
            eventId = eventId,
            blockers = if (complete) emptyList() else listOf("LODGING_REQUIRED"),
            count = confirmed.size
        )
    }

    private fun getTransportReadiness(eventId: String): OrganizationReadinessSection {
        val notNeeded = db.transportQueries
            .selectTransportEventStatus(eventId)
            .executeAsOneOrNull()
            ?.transport_not_needed == 1L
        val selectedPlan = db.transportQueries.selectSelectedPlan(eventId).executeAsOneOrNull()
        val complete = notNeeded || selectedPlan != null
        return section(
            eventId = eventId,
            blockers = if (complete) emptyList() else listOf("TRANSPORT_REQUIRED"),
            count = if (selectedPlan == null) 0 else 1,
            explicitNotNeeded = notNeeded
        )
    }

    private fun getCalendarReadiness(eventId: String): OrganizationReadinessSection {
        val confirmedDate = db.confirmedDateQueries.selectByEventId(eventId).executeAsOneOrNull()
        return section(
            eventId = eventId,
            blockers = if (confirmedDate == null) listOf("CALENDAR_CONFIRMED_DATE_REQUIRED") else emptyList(),
            count = if (confirmedDate == null) 0 else 1
        )
    }

    private fun getNotificationReadiness(
        eventId: String,
        meetings: MeetingReadiness
    ): OrganizationReadinessSection {
        val reminders = db.meetingQueries.selectByEventId(eventId)
            .executeAsList()
            .flatMap { meeting ->
                db.meetingReminderQueries.selectByMeetingId(meeting.id).executeAsList()
            }
        val complete = meetings.explicitNotNeeded || reminders.isNotEmpty()
        return section(
            eventId = eventId,
            blockers = if (complete) emptyList() else listOf("NOTIFICATION_REMINDERS_REQUIRED"),
            count = reminders.size,
            explicitNotNeeded = meetings.explicitNotNeeded
        )
    }

    private fun getPaymentPotReadiness(eventId: String): OrganizationReadinessSection {
        val pot = paymentPotRepository.getActivePotForEvent(eventId)
        val tricount = tricountHandoffRepository.getPaymentReadiness(eventId)
        val complete = pot != null || tricount.complete
        return section(
            eventId = eventId,
            blockers = if (complete) emptyList() else listOf("PAYMENT_POT_REQUIRED"),
            count = if (pot == null) 0 else 1,
            explicitNotNeeded = tricount.handoff?.explicitNotNeeded == true
        )
    }

    private fun getSyncReadiness(eventId: String): OrganizationReadinessSection {
        val pendingCritical = db.syncMetadataQueries.selectPending()
            .executeAsList()
            .filter { isCriticalSyncRecord(it.entityType, it.entityId, it.operation, it.payload, eventId) }
        val pendingConflicts = db.conflictLogQueries.selectPendingCritical()
            .executeAsList()
            .filter { it.event_id == eventId }
        val blockers = buildList {
            if (pendingCritical.any { it.retryState == "FAILED" }) add("CRITICAL_SYNC_FAILED")
            if (pendingCritical.any { it.retryState != "FAILED" }) add("CRITICAL_SYNC_PENDING")
            if (pendingConflicts.isNotEmpty()) add("CRITICAL_CONFLICT_PENDING")
        }
        return section(eventId, blockers, count = pendingCritical.size + pendingConflicts.size)
    }

    private fun getUnsafeLinkReadiness(eventId: String): OrganizationReadinessSection {
        val unsafeCount =
            db.meetingQueries.selectByEventId(eventId).executeAsList().count { row ->
                isUnsafeExternalUrl(row.meetingLink) || isUnsafeExternalUrl(row.targetUrl)
            } +
                db.accommodationQueries.getAccommodationsByEventId(eventId).executeAsList().count { row ->
                    row.booking_url?.let(::isUnsafeExternalUrl) == true
                } +
                db.potQueries.selectByEvent(eventId).executeAsList().count { row ->
                    row.tricountGroupUrl?.let(::isUnsafeExternalUrl) == true
                } +
                listOfNotNull(tricountHandoffRepository.getHandoff(eventId)).count { handoff ->
                    handoff.providerUrl?.let(::isUnsafeExternalUrl) == true
                }
        return section(
            eventId = eventId,
            blockers = if (unsafeCount == 0) emptyList() else listOf("UNSAFE_EXTERNAL_LINKS"),
            count = unsafeCount
        )
    }

    private fun getAccessControlReadiness(eventId: String): OrganizationReadinessSection {
        val participants = db.participantQueries.selectByEventId(eventId).executeAsList()
        val confirmed = participants.filter { it.hasValidatedDate == 1L }
        val blockers = buildList {
            if (participants.none { it.role == "ORGANIZER" }) add("ACCESS_ORGANIZER_REQUIRED")
            if (confirmed.isEmpty()) add("ACCESS_CONFIRMED_PARTICIPANTS_REQUIRED")
        }
        return section(eventId, blockers, count = confirmed.size)
    }

    private fun section(
        eventId: String,
        blockers: List<String>,
        count: Int = 0,
        explicitNotNeeded: Boolean = false
    ): OrganizationReadinessSection =
        OrganizationReadinessSection(
            eventId = eventId,
            complete = blockers.isEmpty(),
            blockers = blockers,
            count = count,
            explicitNotNeeded = explicitNotNeeded
        )

    private fun isCriticalSyncRecord(
        entityType: String,
        entityId: String,
        operation: String,
        payload: String,
        eventId: String
    ): Boolean {
        if (entityType.startsWith("transport_") && operation !in replayableTransportSyncOperations) {
            return false
        }
        if (entityType !in criticalSyncEntityTypes && criticalSyncEntityTypes.none { entityType.startsWith(it) }) {
            return false
        }
        return entityId == eventId ||
            entityId.startsWith("$eventId:") ||
            entityId.contains(eventId) ||
            payload.contains("\"eventId\":\"$eventId\"") ||
            payload.contains("\"eventId\":\"$entityId\"") ||
            payload.contains("\"critical\":true")
    }

    private fun isUnsafeExternalUrl(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return false
        if (trimmed.contains("\${") || trimmed.contains("{") || trimmed.contains("}")) return true
        if (Regex("""\$[A-Za-z_][A-Za-z0-9_]*""").containsMatchIn(trimmed)) return true
        return trimmed.startsWith("http://", ignoreCase = true)
    }

    private companion object {
        val criticalSyncEntityTypes = setOf(
            "scenario",
            "scenario_vote",
            "scenario_selection",
            "lodging_selection",
            "transport_event_status",
            "transport_plan",
            "transport_plan_selection",
            "meeting",
            "expense",
            "payment_pot",
            "tricount_handoff"
        )
        val replayableTransportSyncOperations = setOf("CREATE", "UPDATE", "DELETE", "UPSERT")
    }
}
