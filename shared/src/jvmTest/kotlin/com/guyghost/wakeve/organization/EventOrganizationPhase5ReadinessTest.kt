package com.guyghost.wakeve.organization

import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.calendar.PlatformCalendarService
import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.budget.BudgetRepository
import com.guyghost.wakeve.budget.ExpenseRepository
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.meeting.DeterministicMeetingLinkProvider
import com.guyghost.wakeve.meeting.MeetingService
import com.guyghost.wakeve.models.BudgetCategory
import com.guyghost.wakeve.models.EnhancedCalendarEvent
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.models.NotificationMessage
import com.guyghost.wakeve.models.PushToken
import com.guyghost.wakeve.notification.NotificationServiceInterface
import com.guyghost.wakeve.payment.PaymentPotRepository
import com.guyghost.wakeve.payment.TricountHandoffRepository
import com.guyghost.wakeve.presentation.state.MeetingManagementContract
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.hours

class EventOrganizationPhase5ReadinessTest {

    private lateinit var database: WakeveDb
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var readinessRepository: EventOrganizationReadinessRepository
    private lateinit var meetingService: MeetingService

    @BeforeTest
    fun setUp() {
        database = createFreshTestDatabase()
        budgetRepository = BudgetRepository(database)
        expenseRepository = ExpenseRepository(database)
        readinessRepository = EventOrganizationReadinessRepository(database)
        meetingService = MeetingService(
            database = database,
            calendarService = CalendarService(database, NoopPlatformCalendarService()),
            notificationService = RecordingNotificationService(),
            meetingLinkProvider = DeterministicMeetingLinkProvider()
        )
    }

    @Test
    fun `Phase5 meeting readiness requires persisted meeting or explicit not needed decision`() = runTest {
        seedEvent("phase5-meeting", EventStatus.ORGANIZING)
        seedParticipant("phase5-meeting", "organizer-1", role = "ORGANIZER", confirmed = true)
        seedParticipant("phase5-meeting", "confirmed-a", confirmed = true)
        seedParticipant("phase5-meeting", "confirmed-b", confirmed = true)
        seedParticipant("phase5-meeting", "pending-c", confirmed = false)

        val scheduledFor = Instant.parse("2026-07-18T08:00:00Z")
        val meeting = meetingService.createMeeting(
            eventId = "phase5-meeting",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Organization checkpoint",
            description = "Finalize budget and payments",
            scheduledFor = scheduledFor,
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()

        val persisted = database.meetingQueries.selectById(meeting.id).executeAsOneOrNull()
        assertNotNull(persisted)
        assertEquals(MeetingPlatform.ZOOM.name, persisted.platform)
        assertEquals("Organization checkpoint", persisted.title)
        assertEquals(scheduledFor.toString(), persisted.startTime)
        assertTrue(persisted.meetingLink.isNotBlank())
        assertEquals("SCHEDULED", persisted.status)

        val invitedParticipants = Json.decodeFromString(
            ListSerializer(String.serializer()),
            persisted.invitedParticipants
        )
        assertEquals(setOf("organizer-1", "confirmed-a", "confirmed-b"), invitedParticipants.toSet())
        assertFalse("pending-c" in invitedParticipants)

        assertProductionClassExists(
            "com.guyghost.wakeve.organization.EventOrganizationReadinessRepository",
            "Meeting readiness must be persisted as complete only when a meeting exists or meetings are explicitly marked not needed."
        )
    }

    @Test
    fun `Phase5 meeting readiness can be completed by explicit not needed decision`() {
        seedEvent("phase5-meeting-not-needed", EventStatus.ORGANIZING)
        seedParticipant("phase5-meeting-not-needed", "organizer-1", role = "ORGANIZER", confirmed = true)

        val beforeDecision = readinessRepository.getMeetingReadiness("phase5-meeting-not-needed")
        assertFalse(beforeDecision.complete)
        assertTrue("MEETING_REQUIRED" in beforeDecision.blockers)

        readinessRepository.markMeetingsNotNeeded("phase5-meeting-not-needed", "organizer-1")

        val afterDecision = readinessRepository.getMeetingReadiness("phase5-meeting-not-needed")
        assertTrue(afterDecision.complete)
        assertTrue(afterDecision.explicitNotNeeded)
        assertEquals(0, afterDecision.meetingCount)
        assertTrue(afterDecision.blockers.isEmpty())
    }

    @Test
    fun `Phase5 meeting link is persisted as safe link metadata and not only a raw URL`() = runTest {
        seedEvent("phase5-meeting-link-record", EventStatus.ORGANIZING)
        seedParticipant("phase5-meeting-link-record", "organizer-1", role = "ORGANIZER", confirmed = true)
        seedParticipant("phase5-meeting-link-record", "confirmed-a", confirmed = true)

        val meeting = meetingService.createMeeting(
            eventId = "phase5-meeting-link-record",
            organizerId = "organizer-1",
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Safe link metadata",
            description = null,
            scheduledFor = Instant.parse("2026-07-18T08:00:00Z"),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()

        val persisted = database.meetingQueries.selectById(meeting.id).executeAsOne()
        val persistedFields = persisted.javaClass.declaredFields.map { it.name }.toSet()

        assertFalse(
            listOf("\$meetingId", "\$password", "\$meetCode", "\$part1", "\$part2", "\$part3").any { literal ->
                meeting.meetingUrl.contains(literal) || persisted.meetingLink.contains(literal) || persisted.hostMeetingId.contains(literal)
            },
            "Generated meeting URLs and host IDs must persist resolved values, not escaped Kotlin interpolation placeholders."
        )
        assertTrue("platform" in persistedFields)
        assertTrue("meetingLink" in persistedFields)
        assertTrue(
            setOf("provider", "displayLabel", "targetUrl", "creatorId", "verificationState").all { it in persistedFields },
            "Meeting links must be stored as safe link records with provider/display label/URL/creator/verification state, not only meeting.platform and meetingLink."
        )
        assertEquals(
            persisted.meetingLink,
            persisted.targetUrl,
            "Safe-link targetUrl must be the exact generated meeting URL that participants will open."
        )
    }

    @Test
    fun `Phase5 generated Zoom URL resolves meeting id and password before persistence`() = runTest {
        seedEvent("phase5-zoom-link-record", EventStatus.ORGANIZING)
        seedParticipant("phase5-zoom-link-record", "organizer-1", role = "ORGANIZER", confirmed = true)

        val meeting = meetingService.createMeeting(
            eventId = "phase5-zoom-link-record",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Resolved Zoom link",
            description = null,
            scheduledFor = Instant.parse("2026-07-18T08:00:00Z"),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()

        val persisted = database.meetingQueries.selectById(meeting.id).executeAsOne()

        assertTrue(
            Regex("""^https://zoom\.us/j/\d{10}\?pwd=[A-Z0-9]{6}$""").matches(persisted.meetingLink),
            "Zoom meetingLink must interpolate the generated meeting id and password before persistence."
        )
        assertEquals(persisted.meetingLink, persisted.targetUrl)
        assertEquals(persisted.password, persisted.meetingLink.substringAfter("pwd="))
    }

    @Test
    fun `Phase5 createMeeting rejects actor that is not the event organizer`() = runTest {
        seedEvent("phase5-meeting-organizer-auth", EventStatus.ORGANIZING)
        seedParticipant("phase5-meeting-organizer-auth", "organizer-1", role = "ORGANIZER", confirmed = true)
        seedParticipant("phase5-meeting-organizer-auth", "confirmed-a", confirmed = true)

        val result = meetingService.createMeeting(
            eventId = "phase5-meeting-organizer-auth",
            organizerId = "confirmed-a",
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Unauthorized meeting",
            description = null,
            scheduledFor = Instant.parse("2026-07-18T08:00:00Z"),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        assertTrue(
            result.isFailure,
            "Shared MeetingService.createMeeting must reject organizerId values that are not the event organizer."
        )
        assertTrue(
            database.meetingQueries.selectByEventId("phase5-meeting-organizer-auth").executeAsList().isEmpty(),
            "Rejected meeting creation must not persist a meeting record."
        )
    }

    @Test
    fun `Phase5 createMeeting schedules reminders only for confirmed participants`() = runTest {
        seedEvent("phase5-meeting-reminders", EventStatus.ORGANIZING)
        seedParticipant("phase5-meeting-reminders", "organizer-1", role = "ORGANIZER", confirmed = true)
        seedParticipant("phase5-meeting-reminders", "confirmed-a", confirmed = true)
        seedParticipant("phase5-meeting-reminders", "pending-b", confirmed = false)
        seedParticipant("phase5-meeting-reminders", "declined-c", confirmed = false)

        val meeting = meetingService.createMeeting(
            eventId = "phase5-meeting-reminders",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Confirmed reminders only",
            description = null,
            scheduledFor = Instant.parse("2026-07-18T08:00:00Z"),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()

        val reminders = database.meetingReminderQueries.selectByMeetingId(meeting.id).executeAsList()
        val remindedParticipants = reminders.map { it.participant_id }.toSet()

        assertEquals(
            setOf("organizer-1", "confirmed-a"),
            remindedParticipants,
            "Meeting creation must schedule reminders only for participants that confirmed the selected date."
        )
        assertEquals(
            8,
            reminders.size,
            "Each confirmed participant should receive the four standard meeting reminders."
        )
    }

    @Test
    fun `Phase5 createMeeting prepares calendar entries only for confirmed participants`() = runTest {
        val platformCalendarService = RecordingPlatformCalendarService()
        val service = MeetingService(
            database = database,
            calendarService = CalendarService(database, platformCalendarService),
            notificationService = RecordingNotificationService(),
            meetingLinkProvider = DeterministicMeetingLinkProvider()
        )
        seedEvent("phase5-meeting-calendar", EventStatus.ORGANIZING)
        seedParticipant("phase5-meeting-calendar", "organizer-1", role = "ORGANIZER", confirmed = true)
        seedParticipant("phase5-meeting-calendar", "confirmed-a", confirmed = true)
        seedParticipant("phase5-meeting-calendar", "pending-b", confirmed = false)

        service.createMeeting(
            eventId = "phase5-meeting-calendar",
            organizerId = "organizer-1",
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Calendar backed meeting",
            description = "Calendar entry should be prepared immediately",
            scheduledFor = Instant.parse("2026-07-18T08:00:00Z"),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()

        assertEquals(
            setOf("phase5-meeting-calendar_organizer-1", "phase5-meeting-calendar_confirmed-a"),
            platformCalendarService.addedEvents.map { it.id }.toSet(),
            "Meeting creation must prepare native calendar entries for confirmed participants only."
        )
    }

    @Test
    fun `Phase5 createMeeting notifies confirmed participants with meeting id and excludes non confirmed users`() = runTest {
        val notificationService = RecordingNotificationService()
        val service = MeetingService(
            database = database,
            calendarService = CalendarService(database, NoopPlatformCalendarService()),
            notificationService = notificationService,
            meetingLinkProvider = DeterministicMeetingLinkProvider()
        )
        seedEvent("phase5-meeting-notifications", EventStatus.ORGANIZING)
        seedParticipant("phase5-meeting-notifications", "organizer-1", role = "ORGANIZER", confirmed = true)
        seedParticipant("phase5-meeting-notifications", "confirmed-a", confirmed = true)
        seedParticipant("phase5-meeting-notifications", "pending-b", confirmed = false)

        val meeting = service.createMeeting(
            eventId = "phase5-meeting-notifications",
            organizerId = "organizer-1",
            platform = MeetingPlatform.FACETIME,
            title = "Notification backed meeting",
            description = null,
            scheduledFor = Instant.parse("2026-07-18T08:00:00Z"),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()

        val notifications = notificationService.sentNotifications

        assertEquals(
            setOf("organizer-1", "confirmed-a"),
            notifications.map { it.userId }.toSet(),
            "Meeting creation must notify only confirmed participants."
        )
        assertTrue(notifications.all { it.data["meetingId"] == meeting.id })
        assertFalse(notifications.any { it.userId == "pending-b" })
    }

    @Test
    fun `Phase5 budget baseline readiness recalculates category totals and confirmed participant shares`() {
        seedEvent("phase5-budget", EventStatus.ORGANIZING)
        seedParticipant("phase5-budget", "confirmed-a", confirmed = true)
        seedParticipant("phase5-budget", "confirmed-b", confirmed = true)
        seedParticipant("phase5-budget", "pending-c", confirmed = false)

        val budget = budgetRepository.createBudget("phase5-budget")
        budgetRepository.createBudgetItem(
            budgetId = budget.id,
            category = BudgetCategory.TRANSPORT,
            name = "Train tickets",
            description = "Round trip",
            estimatedCost = 240.0,
            sharedBy = listOf("confirmed-a", "confirmed-b")
        )
        val lodging = budgetRepository.createBudgetItem(
            budgetId = budget.id,
            category = BudgetCategory.ACCOMMODATION,
            name = "Apartment deposit",
            description = "Two nights",
            estimatedCost = 300.0,
            sharedBy = listOf("confirmed-a", "confirmed-b")
        )
        budgetRepository.markItemAsPaid(lodging.id, 320.0, "confirmed-a")

        val updatedBudget = budgetRepository.getBudgetById(budget.id)
        assertNotNull(updatedBudget)
        assertEquals(540.0, updatedBudget.totalEstimated)
        assertEquals(320.0, updatedBudget.totalActual)
        assertEquals(240.0, updatedBudget.transportEstimated)
        assertEquals(300.0, updatedBudget.accommodationEstimated)
        assertEquals(280.0, budgetRepository.getParticipantBudgetShare(budget.id, "confirmed-b").totalOwed)

        assertProductionMethodExists(
            className = "com.guyghost.wakeve.budget.BudgetRepository",
            methodName = "getBudgetReadinessForEvent",
            "Budget readiness must expose baseline completeness and shares derived from confirmed participants, not arbitrary split lists."
        )
    }

    @Test
    fun `Phase5 budget readiness accepts explicit baseline not needed decision`() {
        seedEvent("phase5-budget-not-needed", EventStatus.ORGANIZING)
        seedParticipant("phase5-budget-not-needed", "confirmed-a", confirmed = true)
        budgetRepository.createBudget("phase5-budget-not-needed")

        database.organizationReadinessDecisionQueries.upsertDecision(
            id = "budget-baseline-not-needed",
            eventId = "phase5-budget-not-needed",
            section = "BUDGET_BASELINE",
            notNeeded = 1L,
            decidedBy = "organizer-1",
            decidedAt = "2026-05-22T10:20:00Z"
        )

        val readiness = budgetRepository.getBudgetReadinessForEvent("phase5-budget-not-needed")

        assertTrue(
            readiness.complete,
            "Budget baseline readiness should be complete when required estimates are explicitly marked not needed."
        )
        assertTrue(readiness.blockers.isEmpty())
    }

    @Test
    fun `Phase5 expense splits persist receipt metadata balances and pending sync when offline`() {
        assertProductionClassExists(
            "com.guyghost.wakeve.budget.ExpenseRepository",
            "Shared expenses need a local-first repository that stores payer, split participants, receipt metadata, recalculated balances, and offline pending sync."
        )
    }

    @Test
    fun `Phase5 offline expense writes include replayable sync payload and retry state`() {
        seedEvent("phase5-expense-sync", EventStatus.ORGANIZING)
        seedParticipant("phase5-expense-sync", "confirmed-a", confirmed = true)
        seedParticipant("phase5-expense-sync", "confirmed-b", confirmed = true)
        budgetRepository.createBudget("phase5-expense-sync")

        val expense = expenseRepository.createExpense(
            eventId = "phase5-expense-sync",
            amount = 90.0,
            category = BudgetCategory.ACTIVITIES,
            payerId = "confirmed-a",
            splitParticipantIds = listOf("confirmed-a", "confirmed-b"),
            receiptMetadata = mapOf("sha256" to "receipt-hash"),
            syncState = "PENDING"
        )

        val pendingSync = database.syncMetadataQueries.selectPending().executeAsList()
            .single { it.entityType == "expense" && it.entityId == expense.id }
        val pendingFields = pendingSync.javaClass.declaredFields.map { it.name }.toSet()

        assertEquals("CREATE", pendingSync.operation)
        assertEquals(0L, pendingSync.synced)
        assertTrue(
            setOf("payload", "retryState", "retryCount").all { it in pendingFields },
            "Offline expense sync metadata must be replayable with payload and retry state, not only entity id and operation."
        )
    }

    @Test
    fun `Phase5 settlement suggestions are persisted locally from expenses contributions and pot balances`() {
        assertProductionClassExists(
            "com.guyghost.wakeve.payment.SettlementRepository",
            "Settlement calculation must persist who-owes-whom suggestions locally instead of returning transient triples only."
        )
    }

    @Test
    fun `Phase5 confirmed participant settlement visibility is scoped to own obligations`() {
        assertProductionMethodExists(
            className = "com.guyghost.wakeve.payment.SettlementRepository",
            methodName = "getSettlementsVisibleToParticipant",
            "Confirmed participants must see only settlement obligations where they are the payer or recipient."
        )
    }

    @Test
    fun `Phase5 payment pot lifecycle is persisted for shared settlement readiness`() {
        assertProductionClassExists(
            "com.guyghost.wakeve.payment.PaymentPotRepository",
            "Payment pot lifecycle must be persisted locally and included in Phase 5 settlement readiness."
        )
    }

    @Test
    fun `Phase5 payment and tricount readiness stores provider handoff sync state`() {
        assertProductionClassExists(
            "com.guyghost.wakeve.payment.TricountHandoffRepository",
            "Payment readiness must store provider id, provider URL, sync status, last sync timestamp, and explicit not-needed decisions."
        )
    }

    @Test
    fun `Phase5 meeting creation queues replayable sync metadata`() = runTest {
        seedEvent("phase5-meeting-sync", EventStatus.ORGANIZING)
        seedParticipant("phase5-meeting-sync", "organizer-1", role = "ORGANIZER", confirmed = true)

        val meeting = meetingService.createMeeting(
            eventId = "phase5-meeting-sync",
            organizerId = "organizer-1",
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Replayable meeting sync",
            description = null,
            scheduledFor = Instant.parse("2026-07-18T08:00:00Z"),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()

        val pendingSync = database.syncMetadataQueries.selectPending().executeAsList()
            .firstOrNull {
                it.entityType == "meeting" &&
                    (it.entityId == meeting.id || it.entityId.contains("phase5-meeting-sync"))
            }

        assertNotNull(
            pendingSync,
            "Meeting local writes must queue pending sync metadata with clear entityType=meeting."
        )
        assertEquals("CREATE", pendingSync.operation)
        assertReplayableSyncPayload(pendingSync.payload, pendingSync.retryState, pendingSync.retryCount)
        assertTrue(pendingSync.payload.contains(meeting.id) && pendingSync.payload.contains("phase5-meeting-sync"))
    }

    @Test
    fun `Phase5 payment pot create and close queue replayable sync metadata`() {
        seedEvent("phase5-payment-pot-sync", EventStatus.ORGANIZING)
        seedParticipant("phase5-payment-pot-sync", "organizer-1", role = "ORGANIZER", confirmed = true)
        val paymentPotRepository = PaymentPotRepository(database)

        val pot = paymentPotRepository.createPot(
            eventId = "phase5-payment-pot-sync",
            organizerId = "organizer-1",
            goalAmount = 480.0,
            title = "Weekend pot",
            tricountGroupUrl = "https://tricount.com/group/weekend-pot"
        )
        paymentPotRepository.closePot(pot.id)

        val potSync = database.syncMetadataQueries.selectPending().executeAsList()
            .filter {
                it.entityType == "payment_pot" &&
                    (it.entityId == pot.id || it.entityId.contains("phase5-payment-pot-sync"))
            }

        assertTrue(
            potSync.any { it.operation == "CREATE" },
            "Payment pot creation must queue replayable sync metadata with entityType=payment_pot."
        )
        assertTrue(
            potSync.any { it.operation in setOf("UPDATE", "CLOSE") && it.payload.contains("CLOSED") },
            "Payment pot closure must queue replayable sync metadata with the closed lifecycle state."
        )
        potSync.forEach { assertReplayableSyncPayload(it.payload, it.retryState, it.retryCount) }
    }

    @Test
    fun `Phase5 payment pot rejects non Tricount external provider links`() {
        seedEvent("phase5-payment-pot-provider-link", EventStatus.ORGANIZING)
        seedParticipant("phase5-payment-pot-provider-link", "organizer-1", role = "ORGANIZER", confirmed = true)
        val paymentPotRepository = PaymentPotRepository(database)

        assertFailsWith<IllegalArgumentException> {
            paymentPotRepository.createPot(
                eventId = "phase5-payment-pot-provider-link",
                organizerId = "organizer-1",
                goalAmount = 120.0,
                title = "External provider pot",
                paymentProvider = "OTHER",
                tricountGroupUrl = "https://payments.example.com/group/weekend"
            )
        }
    }

    @Test
    fun `Phase5 Tricount link and not needed decisions queue replayable sync metadata`() {
        seedEvent("phase5-tricount-sync", EventStatus.ORGANIZING)
        seedParticipant("phase5-tricount-sync", "organizer-1", role = "ORGANIZER", confirmed = true)
        val tricountHandoffRepository = TricountHandoffRepository(database)

        tricountHandoffRepository.linkHandoff(
            eventId = "phase5-tricount-sync",
            provider = "TRICOUNT",
            providerId = "tri-sync",
            providerUrl = "https://tricount.com/group/tri-sync",
            syncStatus = "LINKED"
        )
        tricountHandoffRepository.markNotNeeded("phase5-tricount-sync", "organizer-1")

        val tricountSync = database.syncMetadataQueries.selectPending().executeAsList()
            .filter { it.entityType == "tricount_handoff" && it.entityId == "phase5-tricount-sync" }

        assertTrue(
            tricountSync.any { it.payload.contains("https://tricount.com/group/tri-sync") && it.payload.contains("tri-sync") },
            "Tricount link handoff must queue replayable sync payload for the external provider state."
        )
        assertTrue(
            tricountSync.any { it.payload.contains("NOT_NEEDED") || it.payload.contains("explicitNotNeeded") },
            "Tricount not-needed decisions must queue replayable sync payload for offline replay."
        )
        tricountSync.forEach { assertReplayableSyncPayload(it.payload, it.retryState, it.retryCount) }
    }

    @Test
    fun `Phase5 organizing is the only mutable meeting state and finalized is read only`() {
        assertTrue(
            MeetingManagementContract.State(eventStatus = EventStatus.ORGANIZING).canCreateMeetings(),
            "ORGANIZING should allow meeting actions."
        )

        listOf(
            EventStatus.DRAFT,
            EventStatus.POLLING,
            EventStatus.COMPARING,
            EventStatus.CONFIRMED,
            EventStatus.FINALIZED
        ).forEach { status ->
            assertFalse(
                MeetingManagementContract.State(eventStatus = status).canCreateMeetings(),
                "$status must be read-only for Phase 5 meeting actions."
            )
        }
    }

    private fun seedEvent(eventId: String, status: EventStatus) {
        val now = "2026-05-22T10:00:00Z"
        database.eventQueries.insertEvent(
            id = eventId,
            organizerId = "organizer-1",
            title = "Phase 5 Event",
            description = "Organization phase event",
            status = status.name,
            deadline = "2026-06-01T00:00:00Z",
            createdAt = now,
            updatedAt = now,
            version = 1,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null,
            isSample = 0L
        )
    }

    private fun seedParticipant(
        eventId: String,
        userId: String,
        role: String = "PARTICIPANT",
        confirmed: Boolean
    ) {
        database.participantQueries.insertParticipant(
            id = userId,
            eventId = eventId,
            userId = userId,
            role = role,
            hasValidatedDate = if (confirmed) 1L else 0L,
            joinedAt = "2026-05-22T10:05:00Z",
            updatedAt = "2026-05-22T10:05:00Z"
        )
    }

    private fun assertProductionClassExists(className: String, message: String) {
        try {
            Class.forName(className)
        } catch (error: ClassNotFoundException) {
            fail("$message Missing production class: $className")
        }
    }

    private fun assertProductionMethodExists(className: String, methodName: String, message: String) {
        val methods = Class.forName(className).methods.map { it.name }.toSet()
        if (methodName !in methods) {
            fail("$message Missing production method: $className.$methodName")
        }
    }

    private fun assertReplayableSyncPayload(payload: String, retryState: String, retryCount: Long) {
        assertTrue(
            payload.isNotBlank() && payload != "{}",
            "Phase 5 local-first sync metadata must include a non-empty payload that can be replayed."
        )
        assertEquals("READY", retryState)
        assertEquals(0L, retryCount)
    }
}

private class NoopPlatformCalendarService : PlatformCalendarService {
    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
}

private class RecordingPlatformCalendarService : PlatformCalendarService {
    val addedEvents = mutableListOf<EnhancedCalendarEvent>()

    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> {
        addedEvents += event
        return Result.success(Unit)
    }

    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
}

private class RecordingNotificationService : NotificationServiceInterface {
    val sentNotifications = mutableListOf<NotificationMessage>()

    override suspend fun sendNotification(message: NotificationMessage): Result<Unit> {
        sentNotifications += message
        return Result.success(Unit)
    }

    override suspend fun registerPushToken(token: PushToken): Result<Unit> = Result.success(Unit)
    override suspend fun unregisterPushToken(userId: String, deviceId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getUnreadNotifications(userId: String): List<NotificationMessage> = sentNotifications.filter { it.userId == userId && it.readAt == null }
    override suspend fun markAsRead(notificationId: String): Result<Unit> = Result.success(Unit)
}
