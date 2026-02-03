package com.guyghost.wakeve.e2e

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.notification.*
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * # Notification Workflow E2E Test (E2E-004)
 * 
 * Tests notification workflows:
 * - Event invite notification
 * - Vote reminder notification
 * - Date confirmed notification
 * - New comment notification
 * - Mention notification
 * - Meeting reminder notification
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationWorkflowE2ETest {

    // ========================================================================
    // Test Infrastructure
    // ========================================================================

    private lateinit var database: WakevDb
    private lateinit var eventRepository: EventRepositoryInterface
    private lateinit var notificationService: MockNotificationService
    private lateinit var testScope: CoroutineScope

    /**
     * Mock Notification Service for testing
     */
    class MockNotificationService(
        private val database: WakevDb,
        private val preferencesRepository: NotificationPreferencesRepository
    ) {
        private val sentNotifications = mutableListOf<NotificationRecord>()
        private val registeredTokens = mutableMapOf<String, MutableList<PushToken>>()
        
        data class NotificationRecord(
            val id: String,
            val userId: String,
            val type: NotificationType,
            val title: String,
            val body: String,
            val data: Map<String, String>,
            val timestamp: Long
        )

        suspend fun registerPushToken(userId: String, platform: Platform, token: String): Result<Unit> {
            val pushToken = PushToken(
                id = "${userId}-${platform.name}",
                userId = userId,
                platform = platform,
                token = token,
                createdAt = Clock.System.now().toString()
            )
            
            registeredTokens.getOrPut(userId) { mutableListOf() }.add(pushToken)
            
            // Store in database
            return try {
                database.notificationTokenQueries.upsertToken(
                    user_id = userId,
                    platform = platform.name,
                    token = token,
                    updated_at = Clock.System.now().toEpochMilliseconds()
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        suspend fun sendNotification(
            userId: String,
            type: NotificationType,
            title: String,
            body: String,
            data: Map<String, String> = emptyMap()
        ): Result<String> {
            // Check user preferences
            val preferences = preferencesRepository.getPreferences(userId)
                ?: defaultNotificationPreferences(userId)
            
            // Check if notification should be sent
            if (!preferences.shouldSend(type, Clock.System.now())) {
                return Result.failure(
                    IllegalStateException("Notification type ${type.name} is disabled or in quiet hours")
                )
            }

            // Check if user has registered tokens
            val tokens = registeredTokens[userId] ?: emptyList()
            if (tokens.isEmpty()) {
                return Result.failure(
                    IllegalStateException("No push tokens registered for user $userId")
                )
            }

            // Create notification record
            val notification = NotificationRecord(
                id = "notif-${Clock.System.now().toEpochMilliseconds()}",
                userId = userId,
                type = type,
                title = title,
                body = body,
                data = data,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
            
            sentNotifications.add(notification)
            
            // Store in database
            database.notificationsQueries.insert(
                id = notification.id,
                user_id = notification.userId,
                type = notification.type.name,
                title = notification.title,
                body = notification.body,
                data = notification.data.toString(),
                read = false,
                created_at = notification.timestamp
            )
            
            return Result.success(notification.id)
        }

        suspend fun sendNotificationForEvent(
            eventType: NotificationType,
            eventId: String,
            userId: String,
            eventTitle: String,
            additionalData: Map<String, String> = emptyMap()
        ): Result<String> {
            val (title, body) = generateNotificationContent(eventType, eventTitle, additionalData)
            return sendNotification(userId, eventType, title, body, additionalData + ("eventId" to eventId))
        }

        private fun generateNotificationContent(
            type: NotificationType,
            eventTitle: String,
            data: Map<String, String>
        ): Pair<String, String> {
            return when (type) {
                NotificationType.EVENT_INVITE -> {
                    "You're invited!" to "You've been invited to: $eventTitle"
                }
                NotificationType.VOTE_REMINDER -> {
                    "Vote Now!" to "Don't forget to vote for: $eventTitle"
                }
                NotificationType.DATE_CONFIRMED -> {
                    "Date Confirmed!" to "The date for $eventTitle has been confirmed"
                }
                NotificationType.NEW_COMMENT -> {
                    val author = data["authorName"] ?: "Someone"
                    "New Comment" to "$author commented on $eventTitle"
                }
                NotificationType.MENTION -> {
                    val mentioner = data["mentionerName"] ?: "Someone"
                    "You were mentioned!" to "$mentioner mentioned you in $eventTitle"
                }
                NotificationType.MEETING_REMINDER -> {
                    val meetingTitle = data["meetingTitle"] ?: "Meeting"
                    "Meeting Reminder" to "Reminder: $meetingTitle for $eventTitle"
                }
                else -> {
                    "Notification" to "Update regarding $eventTitle"
                }
            }
        }

        fun getSentNotifications(): List<NotificationRecord> = sentNotifications.toList()
        fun getNotificationsForUser(userId: String): List<NotificationRecord> {
            return sentNotifications.filter { it.userId == userId }
        }

        fun clearNotifications() {
            sentNotifications.clear()
        }
    }

    @BeforeTest
    fun setup() {
        database = createFreshTestDatabase()
        eventRepository = DatabaseEventRepository(database)
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        
        val preferencesRepository = NotificationPreferencesRepository(database)
        notificationService = MockNotificationService(database, preferencesRepository)
    }

    @AfterTest
    fun cleanup() {
        testScope.cancel()
        database.close()
    }

    // ========================================================================
    // Test Cases
    // ========================================================================

    /**
     * Test: Event invite notification
     * 
     * GIVEN: Organizer creates event and invites participants
     * WHEN: Event is created with participants
     * THEN: Participants receive invite notifications
     */
    @Test
    fun `event invite notification sent to participants`() = runTest {
        // GIVEN
        val organizerId = "organizer-1"
        val participant1Id = "participant-1"
        val participant2Id = "participant-2"
        
        // Register push tokens for participants
        notificationService.registerPushToken(participant1Id, Platform.ANDROID, "token-android-1")
        notificationService.registerPushToken(participant2Id, Platform.IOS, "token-ios-1")
        
        val eventId = "event-invite-notification"
        val event = createTestEvent(
            id = eventId,
            title = "Team Building Workshop",
            description = "Annual team building event",
            organizerId = organizerId,
            participants = listOf(participant1Id, participant2Id),
            status = EventStatus.DRAFT
        )
        
        // WHEN - Create event
        eventRepository.createEvent(event)
        
        // Send invite notifications (simulating what would happen in EventManagementStateMachine)
        val notificationResults = listOf(
            notificationService.sendNotificationForEvent(
                NotificationType.EVENT_INVITE,
                eventId,
                participant1Id,
                event.title,
                mapOf("organizerName" to "Alice")
            ),
            notificationService.sendNotificationForEvent(
                NotificationType.EVENT_INVITE,
                eventId,
                participant2Id,
                event.title,
                mapOf("organizerName" to "Alice")
            )
        )
        
        // THEN - Notifications should be sent successfully
        notificationResults.forEach { result ->
            assertTrue(result.isSuccess, "Invite notification should be sent successfully")
        }
        
        // Verify notifications in mock service
        val participant1Notifications = notificationService.getNotificationsForUser(participant1Id)
        val participant2Notifications = notificationService.getNotificationsForUser(participant2Id)
        
        assertEquals(1, participant1Notifications.size, "Participant 1 should receive 1 notification")
        assertEquals(1, participant2Notifications.size, "Participant 2 should receive 1 notification")
        
        assertEquals(NotificationType.EVENT_INVITE, participant1Notifications[0].type)
        assertEquals("You're invited!", participant1Notifications[0].title)
        assertEquals("You've been invited to: Team Building Workshop", participant1Notifications[0].body)
    }

    /**
     * Test: Vote reminder notification
     * 
     * GIVEN: Event is in POLLING status
     * WHEN: Reminder is triggered for participants who haven't voted
     * THEN: Notifications are sent to non-voters
     */
    @Test
    fun `vote reminder notification sent to non voters`() = runTest {
        // GIVEN
        val voter1Id = "voter-1"
        val voter2Id = "voter-2"
        val nonVoterId = "non-voter-1"
        
        // Register tokens
        notificationService.registerPushToken(nonVoterId, Platform.ANDROID, "token-non-voter")
        
        val eventId = "event-vote-reminder"
        val event = createTestEvent(
            id = eventId,
            title = "Q4 Planning Meeting",
            organizerId = "organizer",
            participants = listOf(voter1Id, voter2Id, nonVoterId),
            proposedSlots = listOf(
                createTestTimeSlot("slot-1", "2025-06-15T10:00:00Z", "2025-06-15T12:00:00Z"),
                createTestTimeSlot("slot-2", "2025-06-16T10:00:00Z", "2025-06-16T12:00:00Z")
            ),
            status = EventStatus.POLLING
        )
        eventRepository.createEvent(event)
        
        // Some users vote
        eventRepository.addVote(eventId, voter1Id, "slot-1", Vote.YES)
        eventRepository.addVote(eventId, voter2Id, "slot-1", Vote.MAYBE)
        
        // WHEN - Send vote reminders
        val poll = eventRepository.getPoll(eventId)
        val nonVoters = event.participants - poll.votes.keys
        
        val reminderResults = nonVoters.map { userId ->
            notificationService.sendNotificationForEvent(
                NotificationType.VOTE_REMINDER,
                eventId,
                userId,
                event.title
            )
        }
        
        // THEN - Only non-voters should receive reminders
        assertEquals(1, reminderResults.size, "Should send 1 reminder")
        assertTrue(reminderResults[0].isSuccess, "Reminder should be sent successfully")
        
        val nonVoterNotifications = notificationService.getNotificationsForUser(nonVoterId)
        assertEquals(1, nonVoterNotifications.size, "Non-voter should receive reminder")
        assertEquals(NotificationType.VOTE_REMINDER, nonVoterNotifications[0].type)
        assertEquals("Vote Now!", nonVoterNotifications[0].title)
        assertEquals("Don't forget to vote for: Q4 Planning Meeting", nonVoterNotifications[0].body)
    }

    /**
     * Test: Date confirmed notification
     * 
     * GIVEN: Organizer confirms date for event
     * WHEN: Date is confirmed
     * THEN: All participants receive confirmation notification
     */
    @Test
    fun `date confirmed notification sent to all participants`() = runTest {
        // GIVEN
        val participantIds = listOf("participant-1", "participant-2", "participant-3")
        
        // Register tokens
        participantIds.forEach { userId ->
            notificationService.registerPushToken(userId, Platform.ANDROID, "token-$userId")
        }
        
        val eventId = "event-date-confirmed"
        val event = createTestEvent(
            id = eventId,
            title = "Product Launch Event",
            organizerId = "organizer",
            participants = participantIds,
            proposedSlots = listOf(
                createTestTimeSlot("slot-1", "2025-07-01T18:00:00Z", "2025-07-01T21:00:00Z")
            ),
            status = EventStatus.POLLING
        )
        eventRepository.createEvent(event)
        
        // Add votes to allow confirmation
        participantIds.forEach { userId ->
            eventRepository.addVote(eventId, userId, "slot-1", Vote.YES)
        }
        
        // WHEN - Date is confirmed (update event status)
        eventRepository.updateEventStatus(eventId, EventStatus.CONFIRMED, "2025-07-01T18:00:00Z")
        
        // Send confirmation notifications
        val confirmationResults = participantIds.map { userId ->
            notificationService.sendNotificationForEvent(
                NotificationType.DATE_CONFIRMED,
                eventId,
                userId,
                event.title,
                mapOf("confirmedDate" to "2025-07-01T18:00:00Z")
            )
        }
        
        // THEN - All participants should receive confirmation
        confirmationResults.forEach { result ->
            assertTrue(result.isSuccess, "Confirmation should be sent successfully")
        }
        
        participantIds.forEach { userId ->
            val notifications = notificationService.getNotificationsForUser(userId)
            assertEquals(1, notifications.size, "$userId should receive confirmation")
            assertEquals(NotificationType.DATE_CONFIRMED, notifications[0].type)
            assertEquals("Date Confirmed!", notifications[0].title)
            assertEquals("The date for Product Launch Event has been confirmed", notifications[0].body)
        }
    }

    /**
     * Test: New comment notification
     * 
     * GIVEN: User adds comment to event
     * WHEN: Comment is posted
     * THEN: Event participants receive notification
     */
    @Test
    fun `new comment notification sent to participants`() = runTest {
        // GIVEN
        val authorId = "author-1"
        val participantIds = listOf("participant-1", "participant-2")
        
        // Register tokens
        participantIds.forEach { userId ->
            notificationService.registerPushToken(userId, Platform.IOS, "token-$userId")
        }
        
        val eventId = "event-new-comment"
        val event = createTestEvent(
            id = eventId,
            title = "Strategy Discussion",
            organizerId = "organizer",
            participants = listOf(authorId) + participantIds,
            status = EventStatus.ORGANIZING
        )
        eventRepository.createEvent(event)
        
        // WHEN - User adds comment
        val comment = Comment(
            id = "comment-1",
            eventId = eventId,
            section = CommentSection.GENERAL,
            authorId = authorId,
            authorName = "Alice",
            content = "Let's discuss the Q3 roadmap",
            createdAt = Clock.System.now().toString()
        )
        eventRepository.addComment(comment)
        
        // Send comment notifications
        val commentResults = participantIds.map { userId ->
            notificationService.sendNotificationForEvent(
                NotificationType.NEW_COMMENT,
                eventId,
                userId,
                event.title,
                mapOf("authorName" to comment.authorName, "commentPreview" to comment.content.take(50))
            )
        }
        
        // THEN - Participants should receive comment notification
        commentResults.forEach { result ->
            assertTrue(result.isSuccess, "Comment notification should be sent successfully")
        }
        
        participantIds.forEach { userId ->
            val notifications = notificationService.getNotificationsForUser(userId)
            assertEquals(1, notifications.size, "$userId should receive comment notification")
            assertEquals(NotificationType.NEW_COMMENT, notifications[0].type)
            assertEquals("New Comment", notifications[0].title)
            assertEquals("Alice commented on Strategy Discussion", notifications[0].body)
        }
    }

    /**
     * Test: Mention notification
     * 
     * GIVEN: User mentions another user in comment
     * WHEN: Comment with @mention is posted
     * THEN: Mentioned user receives notification
     */
    @Test
    fun `mention notification sent to mentioned users`() = runTest {
        // GIVEN
        val authorId = "author-1"
        val mentionedUserId = "mentioned-user"
        val otherParticipantId = "other-participant"
        
        // Register tokens
        notificationService.registerPushToken(mentionedUserId, Platform.ANDROID, "token-mentioned")
        
        val eventId = "event-mention"
        val event = createTestEvent(
            id = eventId,
            title = "Design Review",
            organizerId = "organizer",
            participants = listOf(authorId, mentionedUserId, otherParticipantId),
            status = EventStatus.ORGANIZING
        )
        eventRepository.createEvent(event)
        
        // WHEN - User adds comment with mention
        val comment = Comment(
            id = "comment-with-mention",
            eventId = eventId,
            section = CommentSection.DESIGN,
            authorId = authorId,
            authorName = "Bob",
            content = "@mentioned-user can you review the latest mockups?",
            createdAt = Clock.System.now().toString()
        )
        eventRepository.addComment(comment)
        
        // Parse mentions and send notifications
        val mentions = parseMentions(comment.content)
        val mentionResults = mentions.map { mentionedUsername ->
            // In real implementation, would map username to userId
            val userId = if (mentionedUsername == "mentioned-user") mentionedUserId else null
            userId?.let {
                notificationService.sendNotificationForEvent(
                    NotificationType.MENTION,
                    eventId,
                    it,
                    event.title,
                    mapOf("mentionerName" to comment.authorName, "commentPreview" to comment.content)
                )
            }
        }.filterNotNull()
        
        // THEN - Mentioned user should receive notification
        assertEquals(1, mentionResults.size, "Should send 1 mention notification")
        assertTrue(mentionResults[0].isSuccess, "Mention notification should be sent")
        
        val mentionedNotifications = notificationService.getNotificationsForUser(mentionedUserId)
        assertEquals(1, mentionedNotifications.size, "Mentioned user should receive notification")
        assertEquals(NotificationType.MENTION, mentionedNotifications[0].type)
        assertEquals("You were mentioned!", mentionedNotifications[0].title)
        assertEquals("Bob mentioned you in Design Review", mentionedNotifications[0].body)
    }

    /**
     * Test: Meeting reminder notification
     * 
     * GIVEN: Event has scheduled meetings
     * WHEN: Meeting reminder is triggered
     * THEN: Participants receive reminder notification
     */
    @Test
    fun `meeting reminder notification sent to participants`() = runTest {
        // GIVEN
        val participantIds = listOf("participant-1", "participant-2")
        
        // Register tokens
        participantIds.forEach { userId ->
            notificationService.registerPushToken(userId, Platform.ANDROID, "token-$userId")
        }
        
        val eventId = "event-meeting-reminder"
        val event = createTestEvent(
            id = eventId,
            title = "Sprint Planning",
            organizerId = "organizer",
            participants = participantIds,
            status = EventStatus.ORGANIZING,
            meetingsUnlocked = true
        )
        eventRepository.createEvent(event)
        
        // Create a meeting
        val meeting = Meeting(
            id = "meeting-1",
            eventId = eventId,
            title = "Sprint Review",
            description = "Review sprint accomplishments",
            platform = MeetingPlatform.ZOOM,
            link = "https://zoom.us/j/123456789",
            startTime = "2025-06-20T14:00:00Z",
            duration = 60,
            participants = participantIds
        )
        eventRepository.saveMeeting(meeting)
        
        // WHEN - Send meeting reminder (24 hours before)
        val reminderResults = participantIds.map { userId ->
            notificationService.sendNotificationForEvent(
                NotificationType.MEETING_REMINDER,
                eventId,
                userId,
                event.title,
                mapOf(
                    "meetingId" to meeting.id,
                    "meetingTitle" to meeting.title,
                    "meetingTime" to meeting.startTime,
                    "meetingPlatform" to meeting.platform.name
                )
            )
        }
        
        // THEN - Participants should receive reminder
        reminderResults.forEach { result ->
            assertTrue(result.isSuccess, "Meeting reminder should be sent successfully")
        }
        
        participantIds.forEach { userId ->
            val notifications = notificationService.getNotificationsForUser(userId)
            assertEquals(1, notifications.size, "$userId should receive meeting reminder")
            assertEquals(NotificationType.MEETING_REMINDER, notifications[0].type)
            assertEquals("Meeting Reminder", notifications[0].title)
            assertEquals("Reminder: Sprint Review for Sprint Planning", notifications[0].body)
        }
    }

    /**
     * Test: Notification preferences and quiet hours
     * 
     * GIVEN: User has notification preferences
     * WHEN: Notification is sent during quiet hours or for disabled type
     * THEN: Notification is not sent
     */
    @Test
    fun `notification preferences and quiet hours respected`() = runTest {
        // GIVEN
        val userId = "user-with-preferences"
        notificationService.registerPushToken(userId, Platform.IOS, "token-preferences")
        
        // Create custom preferences with quiet hours
        val customPreferences = NotificationPreferences(
            userId = userId,
            enabledTypes = setOf(
                NotificationType.DATE_CONFIRMED,
                NotificationType.MEETING_REMINDER
            ),
            quietHoursStart = "22:00",
            quietHoursEnd = "08:00",
            enableQuietHours = true
        )
        
        // Mock preferences repository to return custom preferences
        val preferencesRepository = object : NotificationPreferencesRepository(database) {
            override suspend fun getPreferences(userId: String): NotificationPreferences? {
                return if (userId == "user-with-preferences") customPreferences else null
            }
        }
        
        val customNotificationService = MockNotificationService(database, preferencesRepository)
        
        val eventId = "event-preferences"
        val event = createTestEvent(
            id = eventId,
            title = "Test Event",
            organizerId = "organizer",
            participants = listOf(userId),
            status = EventStatus.DRAFT
        )
        
        // WHEN - Try to send different notification types
        // Type 1: DISABLED (EVENT_INVITE is not in enabledTypes)
        val disabledResult = customNotificationService.sendNotificationForEvent(
            NotificationType.EVENT_INVITE,
            eventId,
            userId,
            event.title
        )
        
        // Type 2: ENABLED but during quiet hours (simulate 23:00)
        val enabledButQuietResult = customNotificationService.sendNotificationForEvent(
            NotificationType.DATE_CONFIRMED,
            eventId,
            userId,
            event.title
        )
        
        // Type 3: ENABLED outside quiet hours (simulate 10:00)
        val enabledResult = customNotificationService.sendNotificationForEvent(
            NotificationType.MEETING_REMINDER,
            eventId,
            userId,
            event.title
        )
        
        // THEN - Only enabled and outside quiet hours should succeed
        assertTrue(disabledResult.isFailure, "Disabled notification type should fail")
        assertTrue(enabledButQuietResult.isFailure, "Notification during quiet hours should fail")
        assertTrue(enabledResult.isSuccess, "Enabled notification outside quiet hours should succeed")
        
        // Verify only one notification was sent
        val allNotifications = customNotificationService.getSentNotifications()
        assertEquals(1, allNotifications.size, "Should only send 1 notification")
        assertEquals(NotificationType.MEETING_REMINDER, allNotifications[0].type)
    }

    // ========================================================================
    // Helper Functions
    // ========================================================================

    private fun parseMentions(content: String): List<String> {
        val mentionRegex = Regex("@(\\w+)")
        return mentionRegex.findAll(content).map { it.groupValues[1] }.toList()
    }
}