# Meeting Service Specification

## Version
**Version**: 1.0.0
**Status**: ✅ Implémenté
**Date de création**: 26 décembre 2025
**Auteur**: Équipe Wakeve

## Overview

Le service de réunion de Wakeve permet de générer des liens de réunion virtuelle (Zoom, Google Meet, FaceTime) pour les événements qui nécessitent une coordination à distance, avec gestion des invitations et des rappels.

## Domain Model

### Core Concepts

- **Virtual Meeting**: Réunion en ligne pour coordination d'événements
- **Meeting Link**: Lien de réunion généré
- **Meeting Platform**: Type de plateforme (Zoom, Google Meet, FaceTime)
- **Meeting Invitations**: Invitations envoyées aux participants validés
- **Meeting Reminders**: Rappels automatiques avant la réunion

### MeetingPlatform

```kotlin
enum class MeetingPlatform {
    ZOOM,           // Zoom Meetings
    GOOGLE_MEET,     // Google Meet
    FACETIME,        // Apple FaceTime
    TEAMS,           // Microsoft Teams (future)
    WEBEX            // Cisco Webex (future)
}
```

### VirtualMeeting

```kotlin
@Serializable
data class VirtualMeeting(
    val id: String,
    val eventId: String,
    val organizerId: String,
    val platform: MeetingPlatform,
    val meetingId: String,         // ID plateforme (ex: "abc123")
    val meetingPassword: String?,   // Mot de passe (optionnel)
    val meetingUrl: String,        // URL de réunion complète
    val dialInNumber: String?,     // Numéro téléphone (Zoom)
    val dialInPassword: String?,    // Code PIN (Zoom)
    val title: String,
    val description: String?,
    val scheduledFor: Instant,
    val duration: Duration,        // Durée prévue
    val timezone: String,          // Fuseau horaire
    val participantLimit: Int?,     // Limite de participants (optionnel)
    val requirePassword: Boolean,
    val waitingRoom: Boolean,
    val hostKey: String?,         // Host key (Zoom)
    val createdAt: Instant,
    val status: MeetingStatus
)
```

### MeetingStatus

```kotlin
enum class MeetingStatus {
    SCHEDULED,       // Planifiée
    STARTED,         // En cours
    ENDED,           // Terminée
    CANCELLED         // Annulée
}
```

### MeetingInvitation

```kotlin
@Serializable
data class MeetingInvitation(
    val id: String,
    val meetingId: String,
    val participantId: String,
    val status: InvitationStatus,
    val sentAt: Instant,
    val respondedAt: Instant?,
    val acceptedAt: Instant?
)
```

### InvitationStatus

```kotlin
enum class InvitationStatus {
    PENDING,    // Invitation envoyée, pas de réponse
    ACCEPTED,    // Participant a accepté
    DECLINED,    // Participant a décliné
    TENTATIVE    // Participant est incertain
}
```

### MeetingReminder

```kotlin
enum class MeetingReminderTiming {
    ONE_DAY_BEFORE,
    ONE_HOUR_BEFORE,
    FIFTEEN_MINUTES_BEFORE,
    FIVE_MINUTES_BEFORE
}

@Serializable
data class MeetingReminder(
    val id: String,
    val meetingId: String,
    val participantId: String?,
    val timing: MeetingReminderTiming,
    val scheduledFor: Instant,
    val sentAt: Instant?,
    val status: ReminderStatus
)

enum class ReminderStatus {
    SCHEDULED,
    SENT,
    FAILED
}
```

## MeetingService

### Responsibilities

**Génération de liens de réunion**:
- Zoom Meetings avec mot de passe
- Google Meet (auto-généré)
- FaceTime (Apple ID)

**Gestion des invitations**:
- Invitations aux participants validés
- Suivi des réponses (accepté/décliné)
- Envoi automatique des détails

**Rappels automatiques**:
- Planification des rappels
- Notification avant la réunion
- Gestion des fuseaux horaires

**Platform-specific features**:
- Waiting room
- Host key
- Participant limit
- Dial-in (téléphone)

### API

```kotlin
class MeetingService(
    private val database: WakevDb,
    private val calendarService: CalendarService,
    private val notificationService: NotificationService
) {

    /**
     * Crée une réunion virtuelle
     */
    suspend fun createMeeting(
        eventId: String,
        organizerId: String,
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: Instant,
        duration: Duration,
        timezone: String,
        participantLimit: Int? = null,
        requirePassword: Boolean = true,
        waitingRoom: Boolean = true
    ): Result<VirtualMeeting> {
        // Generate meeting based on platform
        val meeting = when (platform) {
            MeetingPlatform.ZOOM -> createZoomMeeting(
                eventId = eventId,
                organizerId = organizerId,
                title = title,
                description = description,
                scheduledFor = scheduledFor,
                duration = duration,
                timezone = timezone,
                participantLimit = participantLimit,
                requirePassword = requirePassword,
                waitingRoom = waitingRoom
            )
            MeetingPlatform.GOOGLE_MEET -> createGoogleMeetMeeting(
                eventId = eventId,
                organizerId = organizerId,
                title = title,
                description = description,
                scheduledFor = scheduledFor,
                duration = duration
            )
            MeetingPlatform.FACETIME -> createFaceTimeMeeting(
                eventId = eventId,
                organizerId = organizerId,
                title = title,
                description = description,
                scheduledFor = scheduledFor
            )
            else -> return Result.failure(UnsupportedPlatformException(platform))
        }

        // Save to database
        database.virtualMeetingQueries.insert(meeting)

        // Schedule reminders
        scheduleMeetingReminders(meeting)

        return Result.success(meeting)
    }

    /**
     * Met à jour une réunion existante
     */
    suspend fun updateMeeting(
        meetingId: String,
        title: String? = null,
        description: String? = null,
        scheduledFor: Instant? = null,
        duration: Duration? = null
    ): Result<VirtualMeeting> {
        val existing = database.virtualMeetingQueries
            .selectById(meetingId)
            .executeAsOneOrNull()
            ?: return Result.failure(MeetingNotFoundException(meetingId))

        val updated = existing.copy(
            title = title ?: existing.title,
            description = description ?: existing.description,
            scheduledFor = scheduledFor ?: existing.scheduledFor,
            duration = duration ?: existing.duration
        )

        database.virtualMeetingQueries.update(updated)

        // Update calendar event
        calendarService.updateNativeCalendarEvent(existing.eventId, existing.organizerId)

        // Reschedule reminders
        cancelMeetingReminders(meetingId)
        scheduleMeetingReminders(updated)

        return Result.success(updated)
    }

    /**
     * Annule une réunion
     */
    suspend fun cancelMeeting(meetingId: String): Result<Unit> {
        val meeting = database.virtualMeetingQueries
            .selectById(meetingId)
            .executeAsOneOrNull()
            ?: return Result.failure(MeetingNotFoundException(meetingId))

        val cancelled = meeting.copy(status = MeetingStatus.CANCELLED)
        database.virtualMeetingQueries.update(cancelled)

        // Cancel reminders
        cancelMeetingReminders(meetingId)

        // Cancel calendar event
        calendarService.removeFromNativeCalendar(meeting.eventId, meeting.organizerId)

        // Notify participants
        notifyParticipantsMeetingCancelled(meeting)

        return Result.success(Unit)
    }

    /**
     * Démarre une réunion
     */
    suspend fun startMeeting(meetingId: String): Result<VirtualMeeting> {
        val meeting = database.virtualMeetingQueries
            .selectById(meetingId)
            .executeAsOneOrNull()
            ?: return Result.failure(MeetingNotFoundException(meetingId))

        val started = meeting.copy(status = MeetingStatus.STARTED)
        database.virtualMeetingQueries.update(started)

        return Result.success(started)
    }

    /**
     * Termine une réunion
     */
    suspend fun endMeeting(meetingId: String): Result<VirtualMeeting> {
        val meeting = database.virtualMeetingQueries
            .selectById(meetingId)
            .executeAsOneOrNull()
            ?: return Result.failure(MeetingNotFoundException(meetingId))

        val ended = meeting.copy(status = MeetingStatus.ENDED)
        database.virtualMeetingQueries.update(ended)

        return Result.success(ended)
    }

    /**
     * Envoie des invitations aux participants
     */
    suspend fun sendInvitations(meetingId: String): Result<Unit> {
        val meeting = database.virtualMeetingQueries
            .selectById(meetingId)
            .executeAsOneOrNull()
            ?: return Result.failure(MeetingNotFoundException(meetingId))

        // Get validated participants
        val participants = database.participantQueries
            .selectByEventId(meeting.eventId)
            .executeAsList()
            .filter { it.isValidated } // Only invite validated participants

        // Create invitations
        participants.forEach { participant ->
            val invitation = MeetingInvitation(
                id = UUID.randomUUID().toString(),
                meetingId = meetingId,
                participantId = participant.id,
                status = InvitationStatus.PENDING,
                sentAt = Instant.now(),
                respondedAt = null,
                acceptedAt = null
            )

            database.meetingInvitationQueries.insert(invitation)

            // Send notification
            notificationService.sendNotification(
                title = "Invitation: ${meeting.title}",
                body = "Vous êtes invité à une réunion virtuelle le ${formatDate(meeting.scheduledFor, meeting.timezone)}",
                recipientId = participant.id
            )
        }

        // Add meeting details to calendar
        calendarService.addToNativeCalendar(meeting.eventId, meeting.organizerId)

        return Result.success(Unit)
    }

    /**
     * Enregistre la réponse d'un participant
     */
    suspend fun respondToInvitation(
        invitationId: String,
        status: InvitationStatus
    ): Result<Unit> {
        val invitation = database.meetingInvitationQueries
            .selectById(invitationId)
            .executeAsOneOrNull()
            ?: return Result.failure(InvitationNotFoundException(invitationId))

        val updated = invitation.copy(
            status = status,
            respondedAt = Instant.now(),
            acceptedAt = if (status == InvitationStatus.ACCEPTED) {
                Instant.now()
            } else {
                null
            }
        )

        database.meetingInvitationQueries.update(updated)

        return Result.success(Unit)
    }

    /**
     * Planifie les rappels pour une réunion
     */
    private suspend fun scheduleMeetingReminders(meeting: VirtualMeeting) {
        val timings = listOf(
            MeetingReminderTiming.ONE_DAY_BEFORE,
            MeetingReminderTiming.ONE_HOUR_BEFORE,
            MeetingReminderTiming.FIFTEEN_MINUTES_BEFORE,
            MeetingReminderTiming.FIVE_MINUTES_BEFORE
        )

        val participants = database.participantQueries
            .selectByEventId(meeting.eventId)
            .executeAsList()

        timings.forEach { timing ->
            val scheduledTime = when (timing) {
                MeetingReminderTiming.ONE_DAY_BEFORE ->
                    meeting.scheduledFor.minus(1, ChronoUnit.DAYS)
                MeetingReminderTiming.ONE_HOUR_BEFORE ->
                    meeting.scheduledFor.minus(1, ChronoUnit.HOURS)
                MeetingReminderTiming.FIFTEEN_MINUTES_BEFORE ->
                    meeting.scheduledFor.minus(15, ChronoUnit.MINUTES)
                MeetingReminderTiming.FIVE_MINUTES_BEFORE ->
                    meeting.scheduledFor.minus(5, ChronoUnit.MINUTES)
            }

            participants.forEach { participant ->
                val reminder = MeetingReminder(
                    id = UUID.randomUUID().toString(),
                    meetingId = meeting.id,
                    participantId = participant.id,
                    timing = timing,
                    scheduledFor = scheduledTime,
                    sentAt = null,
                    status = ReminderStatus.SCHEDULED
                )

                database.meetingReminderQueries.insert(reminder)
            }
        }
    }

    /**
     * Annule les rappels d'une réunion
     */
    private suspend fun cancelMeetingReminders(meetingId: String) {
        database.meetingReminderQueries
            .deleteByMeetingId(meetingId)
    }

    private fun createZoomMeeting(/* ... */): VirtualMeeting {
        // Generate Zoom meeting ID and password
        val meetingId = generateZoomMeetingId() // 10 digits
        val meetingPassword = generateRandomPassword(6)

        val meetingUrl = "https://zoom.us/j/${meetingId}?pwd=${meetingPassword}"

        return VirtualMeeting(
            id = UUID.randomUUID().toString(),
            eventId = eventId,
            organizerId = organizerId,
            platform = MeetingPlatform.ZOOM,
            meetingId = meetingId,
            meetingPassword = meetingPassword,
            meetingUrl = meetingUrl,
            dialInNumber = "+33 1 23 45 67 89", // Mock
            dialInPassword = meetingId.substring(0, 6), // Mock
            title = title,
            description = description,
            scheduledFor = scheduledFor,
            duration = duration,
            timezone = timezone,
            participantLimit = participantLimit,
            requirePassword = requirePassword,
            waitingRoom = waitingRoom,
            hostKey = generateHostKey(), // Mock
            createdAt = Instant.now(),
            status = MeetingStatus.SCHEDULED
        )
    }

    private fun createGoogleMeetMeeting(/* ... */): VirtualMeeting {
        // Generate Google Meet code (10 letters)
        val meetCode = generateMeetCode()

        val meetingUrl = "https://meet.google.com/${meetCode}"

        return VirtualMeeting(
            id = UUID.randomUUID().toString(),
            eventId = eventId,
            organizerId = organizerId,
            platform = MeetingPlatform.GOOGLE_MEET,
            meetingId = meetCode,
            meetingPassword = null,
            meetingUrl = meetingUrl,
            dialInNumber = null,
            dialInPassword = null,
            title = title,
            description = description,
            scheduledFor = scheduledFor,
            duration = duration,
            timezone = timezone,
            participantLimit = null,
            requirePassword = false,
            waitingRoom = false,
            hostKey = null,
            createdAt = Instant.now(),
            status = MeetingStatus.SCHEDULED
        )
    }

    private fun createFaceTimeMeeting(/* ... */): VirtualMeeting {
        // FaceTime uses Apple ID or phone number
        // For group FaceTime, all participants need Apple IDs
        val meetingUrl = "facetime://" // App handles this

        return VirtualMeeting(
            id = UUID.randomUUID().toString(),
            eventId = eventId,
            organizerId = organizerId,
            platform = MeetingPlatform.FACETIME,
            meetingId = organizerId, // Use organizer's Apple ID
            meetingPassword = null,
            meetingUrl = meetingUrl,
            dialInNumber = null,
            dialInPassword = null,
            title = title,
            description = description,
            scheduledFor = scheduledFor,
            duration = duration,
            timezone = timezone,
            participantLimit = null,
            requirePassword = false,
            waitingRoom = false,
            hostKey = null,
            createdAt = Instant.now(),
            status = MeetingStatus.SCHEDULED
        )
    }

    private fun generateZoomMeetingId(): String {
        return (1..10).map { Random.nextInt(0, 10) }.joinToString("")
    }

    private fun generateRandomPassword(length: Int): String {
        val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length).map { chars.random() }.joinToString("")
    }

    private fun generateHostKey(): String {
        return (1..6).map { Random.nextInt(0, 10) }.joinToString("")
    }

    private fun generateMeetCode(): String {
        val letters = "abcdefghijklmnopqrstuvwxyz-"
        return (1..10).map { letters.random() }.joinToString("").substring(0, 3) +
               "-" +
               (1..4).map { letters.random() }.joinToString("")
    }

    private fun formatDate(instant: Instant, timezone: String): String {
        // Format date according to timezone
        val zonedDateTime = instant.atZone(ZoneId.of(timezone))
        val formatter = DateTimeFormatter.ofPattern("dd MMM à HH:mm")
        return zonedDateTime.format(formatter)
    }

    private fun notifyParticipantsMeetingCancelled(meeting: VirtualMeeting) {
        // TODO: Integrate with NotificationService
        // notificationService.notifyMeetingCancelled(meeting)
    }
}
```

## Database Schema

### VirtualMeeting.sq

```sql
CREATE TABLE virtual_meeting (
    id TEXT PRIMARY KEY,
    event_id TEXT NOT NULL,
    organizer_id TEXT NOT NULL,
    platform TEXT NOT NULL,
    meeting_id TEXT NOT NULL,
    meeting_password TEXT,
    meeting_url TEXT NOT NULL,
    dial_in_number TEXT,
    dial_in_password TEXT,
    title TEXT NOT NULL,
    description TEXT,
    scheduled_for INTEGER NOT NULL,
    duration INTEGER NOT NULL,
    timezone TEXT NOT NULL,
    participant_limit INTEGER,
    require_password INTEGER NOT NULL DEFAULT 1,
    waiting_room INTEGER NOT NULL DEFAULT 1,
    host_key TEXT,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    status TEXT NOT NULL DEFAULT 'SCHEDULED',
    FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE,
    FOREIGN KEY (organizer_id) REFERENCES participant(id) ON DELETE CASCADE
);

CREATE INDEX idx_virtual_meeting_event ON virtual_meeting(event_id);
CREATE INDEX idx_virtual_meeting_organizer ON virtual_meeting(organizer_id);
CREATE INDEX idx_virtual_meeting_status ON virtual_meeting(status);
CREATE INDEX idx_virtual_meeting_scheduled_for ON virtual_meeting(scheduled_for);

insertVirtualMeeting:
INSERT INTO virtual_meeting (id, event_id, organizer_id, platform, meeting_id, meeting_password, meeting_url, dial_in_number, dial_in_password, title, description, scheduled_for, duration, timezone, participant_limit, require_password, waiting_room, host_key, created_at, status)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

updateVirtualMeeting:
UPDATE virtual_meeting
SET title = ?, description = ?, scheduled_for = ?, duration = ?, status = ?
WHERE id = ?;

selectById:
SELECT * FROM virtual_meeting WHERE id = ?;

selectByEventId:
SELECT * FROM virtual_meeting WHERE event_id = ?;

selectByOrganizerId:
SELECT * FROM virtual_meeting WHERE organizer_id = ?;
```

### MeetingInvitation.sq

```sql
CREATE TABLE meeting_invitation (
    id TEXT PRIMARY KEY,
    meeting_id TEXT NOT NULL,
    participant_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    sent_at INTEGER NOT NULL,
    responded_at INTEGER,
    accepted_at INTEGER,
    FOREIGN KEY (meeting_id) REFERENCES virtual_meeting(id) ON DELETE CASCADE,
    FOREIGN KEY (participant_id) REFERENCES participant(id) ON DELETE CASCADE,
    UNIQUE (meeting_id, participant_id)
);

CREATE INDEX idx_meeting_invitation_meeting ON meeting_invitation(meeting_id);
CREATE INDEX idx_meeting_invitation_participant ON meeting_invitation(participant_id);
CREATE INDEX idx_meeting_invitation_status ON meeting_invitation(status);

insertMeetingInvitation:
INSERT INTO meeting_invitation (id, meeting_id, participant_id, status, sent_at, responded_at, accepted_at)
VALUES (?, ?, ?, ?, ?, ?, ?);

updateMeetingInvitation:
UPDATE meeting_invitation
SET status = ?, responded_at = ?, accepted_at = ?
WHERE id = ?;

selectById:
SELECT * FROM meeting_invitation WHERE id = ?;

selectByMeetingId:
SELECT * FROM meeting_invitation WHERE meeting_id = ?;

selectByParticipantId:
SELECT * FROM meeting_invitation WHERE participant_id = ?;
```

### MeetingReminder.sq

```sql
CREATE TABLE meeting_reminder (
    id TEXT PRIMARY KEY,
    meeting_id TEXT NOT NULL,
    participant_id TEXT,
    timing TEXT NOT NULL,
    scheduled_for INTEGER NOT NULL,
    sent_at INTEGER,
    status TEXT NOT NULL DEFAULT 'SCHEDULED',
    FOREIGN KEY (meeting_id) REFERENCES virtual_meeting(id) ON DELETE CASCADE,
    FOREIGN KEY (participant_id) REFERENCES participant(id) ON DELETE CASCADE
);

CREATE INDEX idx_meeting_reminder_meeting ON meeting_reminder(meeting_id);
CREATE INDEX idx_meeting_reminder_participant ON meeting_reminder(participant_id);
CREATE INDEX idx_meeting_reminder_scheduled_for ON meeting_reminder(scheduled_for);
CREATE INDEX idx_meeting_reminder_status ON meeting_reminder(status);

insertMeetingReminder:
INSERT INTO meeting_reminder (id, meeting_id, participant_id, timing, scheduled_for, sent_at, status)
VALUES (?, ?, ?, ?, ?, ?, ?);

updateMeetingReminder:
UPDATE meeting_reminder
SET status = ?, sent_at = ?
WHERE id = ?;

selectByMeetingId:
SELECT * FROM meeting_reminder WHERE meeting_id = ?;

selectPendingForParticipant:
SELECT * FROM meeting_reminder WHERE participant_id = ? AND status = 'SCHEDULED';

deleteByMeetingId:
DELETE FROM meeting_reminder WHERE meeting_id = ?;
```

## API Endpoints

```
POST   /api/events/{id}/meetings                    # Créer réunion
GET    /api/events/{id}/meetings                    # Liste réunions événement
GET    /api/meetings/{meetingId}                     # Détails réunion
PUT    /api/meetings/{meetingId}                     # Mettre à jour réunion
DELETE /api/meetings/{meetingId}                     # Annuler réunion
POST   /api/meetings/{meetingId}/start               # Démarrer réunion
POST   /api/meetings/{meetingId}/end                 # Terminer réunion
POST   /api/meetings/{meetingId}/invitations          # Envoyer invitations
PUT    /api/meetings/invitations/{invitationId}/respond  # Répondre invitation
GET    /api/meetings/{meetingId}/invitations          # Liste invitations
GET    /api/meetings/{meetingId}/reminders           # Liste rappels
POST   /api/meetings/{meetingId}/platforms          # Générer lien plateforme
```

## Scenarios

### SCENARIO 1: Create Zoom Meeting

**GIVEN**: Organisateur crée événement
**WHEN**: Organisateur crée réunion Zoom
**THEN**: Système génère:
  - Meeting ID: 1234567890
  - Password: abc123
  - URL: https://zoom.us/j/1234567890?pwd=abc123
  - Waiting room: enabled
  - Host key: 654321
**AND**: Réunion sauvegardée en base
**AND**: Rappels planifiés

```kotlin
val meeting = meetingService.createMeeting(
    eventId = "event-1",
    organizerId = "organizer-1",
    platform = MeetingPlatform.ZOOM,
    title = "Team Planning Session",
    description = "Planification de l'événement",
    scheduledFor = Instant.parse("2025-12-27T10:00:00Z"),
    duration = Duration.ofHours(1),
    timezone = "Europe/Paris",
    requirePassword = true,
    waitingRoom = true
)

assertTrue(meeting.isSuccess)
val result = meeting.getOrThrow()
assertEquals(result.platform, MeetingPlatform.ZOOM)
assertTrue(result.meetingId.length == 10)
assertTrue(result.meetingPassword?.length == 6)
assertTrue(result.waitingRoom)
```

### SCENARIO 2: Create Google Meet

**GIVEN**: Organisateur préfère Google Meet
**WHEN**: Organisateur crée réunion Google Meet
**THEN**: Système génère:
  - Meet Code: abc-defgh
  - URL: https://meet.google.com/abc-defgh
  - Pas de mot de passe
  - Pas de waiting room

```kotlin
val meeting = meetingService.createMeeting(
    eventId = "event-1",
    organizerId = "organizer-1",
    platform = MeetingPlatform.GOOGLE_MEET,
    title = "Team Planning Session",
    description = null,
    scheduledFor = Instant.parse("2025-12-27T10:00:00Z"),
    duration = Duration.ofHours(1),
    timezone = "Europe/Paris"
)

assertTrue(meeting.isSuccess)
val result = meeting.getOrThrow()
assertEquals(result.platform, MeetingPlatform.GOOGLE_MEET)
assertTrue(result.meetingId.contains("-"))
assertNull(result.meetingPassword)
assertFalse(result.waitingRoom)
```

### SCENARIO 3: Create FaceTime Meeting

**GIVEN**: Organisateur et participants Apple
**WHEN**: Organisateur crée réunion FaceTime
**THEN**: Système génère:
  - Meeting ID: Apple ID de l'organisateur
  - URL: facetime://
  - Pas de mot de passe
  - Pas de participant limit

```kotlin
val meeting = meetingService.createMeeting(
    eventId = "event-1",
    organizerId = "organizer-appleid@icloud.com",
    platform = MeetingPlatform.FACETIME,
    title = "Team Planning Session",
    description = null,
    scheduledFor = Instant.parse("2025-12-27T10:00:00Z"),
    duration = Duration.ofHours(1),
    timezone = "Europe/Paris"
)

assertTrue(meeting.isSuccess)
val result = meeting.getOrThrow()
assertEquals(result.platform, MeetingPlatform.FACETIME)
assertEquals(result.meetingId, "organizer-appleid@icloud.com")
assertNull(result.meetingPassword)
```

### SCENARIO 4: Send Invitations

**GIVEN**: Réunion créée avec 3 participants validés
**WHEN**: Organisateur envoie invitations
**THEN**: Système envoie:
  - Notifications à tous les participants validés
  - Statut: PENDING
  - Ajout au calendrier natif
**AND**: Enregistre invitations en base

```kotlin
meetingService.sendInvitations("meeting-1")

val invitations = database.meetingInvitationQueries
    .selectByMeetingId("meeting-1")
    .executeAsList()

assertTrue(invitations.size == 3)
assertTrue(invitations.all { it.status == InvitationStatus.PENDING })
```

### SCENARIO 5: Respond to Invitation

**GIVEN**: Participant reçoit invitation
**WHEN**: Participant accepte l'invitation
**THEN**: Statut mis à jour: ACCEPTED
**AND**: Date d'acceptation enregistrée
**AND**: Rappels planifiés pour ce participant

```kotlin
meetingService.respondToInvitation(
    invitationId = "invitation-1",
    status = InvitationStatus.ACCEPTED
)

val invitation = database.meetingInvitationQueries
    .selectById("invitation-1")
    .executeAsOne()

assertEquals(invitation.status, InvitationStatus.ACCEPTED)
assertNotNull(invitation.acceptedAt)
```

### SCENARIO 6: Cancel Meeting

**GIVEN**: Réunion planifiée
**WHEN**: Organisateur annule la réunion
**THEN**: Statut: CANCELLED
**AND**: Rappels annulés
**AND**: Participants notifiés
**AND**: Événement supprimé du calendrier

```kotlin
meetingService.cancelMeeting("meeting-1")

val meeting = database.virtualMeetingQueries
    .selectById("meeting-1")
    .executeAsOne()

assertEquals(meeting.status, MeetingStatus.CANCELLED)
```

### SCENARIO 7: Meeting Reminders

**GIVEN**: Réunion planifiée pour demain à 10h
**WHEN**: Rappels planifiés
**THEN**: Rappels à:
  - J-1: 27 déc à 10h
  - J: 28 déc à 9h
  - J: 28 déc à 9:45
  - J: 28 déc à 9:55
**AND**: Notifications envoyées aux participants

```kotlin
val meeting = meetingService.createMeeting(
    // ... parameters
    scheduledFor = Instant.parse("2025-12-28T10:00:00Z"),
    timezone = "Europe/Paris"
)

val reminders = database.meetingReminderQueries
    .selectByMeetingId(meeting.getOrThrow().id)
    .executeAsList()

assertTrue(reminders.size == 4 * 3) // 4 timings * 3 participants
assertTrue(reminders.any { it.timing == MeetingReminderTiming.ONE_DAY_BEFORE })
assertTrue(reminders.any { it.timing == MeetingReminderTiming.ONE_HOUR_BEFORE })
```

## Testing

### Unit Tests

```kotlin
class MeetingServiceTest {
    @Test
    fun `createZoomMeeting generates valid meeting ID and password`() {
        // When
        val result = meetingService.createMeeting(
            eventId = "event-1",
            organizerId = "org-1",
            platform = MeetingPlatform.ZOOM,
            title = "Test Meeting",
            description = null,
            scheduledFor = Instant.now(),
            duration = Duration.ofHours(1),
            timezone = "Europe/Paris"
        )

        // Then
        assertTrue(result.isSuccess)
        val meeting = result.getOrThrow()
        assertEquals(meeting.platform, MeetingPlatform.ZOOM)
        assertTrue(meeting.meetingId.length == 10)
        assertTrue(meeting.meetingPassword?.length == 6)
        assertTrue(meeting.meetingUrl.contains("zoom.us/j/"))
    }

    @Test
    fun `createGoogleMeet generates valid meet code`() {
        // When
        val result = meetingService.createMeeting(
            eventId = "event-1",
            organizerId = "org-1",
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Test Meeting",
            description = null,
            scheduledFor = Instant.now(),
            duration = Duration.ofHours(1),
            timezone = "Europe/Paris"
        )

        // Then
        assertTrue(result.isSuccess)
        val meeting = result.getOrThrow()
        assertEquals(meeting.platform, MeetingPlatform.GOOGLE_MEET)
        assertTrue(meeting.meetingId.contains("-"))
        assertTrue(meeting.meetingUrl.contains("meet.google.com/"))
        assertNull(meeting.meetingPassword)
    }

    @Test
    fun `sendInvitations only invites validated participants`() {
        // Given
        val event = createTestEvent()
        val validatedParticipant = createTestParticipant(event.id, isValidated = true)
        val unvalidatedParticipant = createTestParticipant(event.id, isValidated = false)

        // When
        val result = meetingService.sendInvitations(meetingId = "meeting-1")

        // Then
        assertTrue(result.isSuccess)

        val invitations = database.meetingInvitationQueries
            .selectByMeetingId("meeting-1")
            .executeAsList()

        assertTrue(invitations.any { it.participantId == validatedParticipant.id })
        assertFalse(invitations.any { it.participantId == unvalidatedParticipant.id })
    }

    @Test
    fun `respondToInvitation updates status and timestamps`() {
        // Given
        val invitation = createTestInvitation(status = InvitationStatus.PENDING)

        // When
        meetingService.respondToInvitation(
            invitationId = invitation.id,
            status = InvitationStatus.ACCEPTED
        )

        // Then
        val updated = database.meetingInvitationQueries
            .selectById(invitation.id)
            .executeAsOne()

        assertEquals(updated.status, InvitationStatus.ACCEPTED)
        assertNotNull(updated.respondedAt)
        assertNotNull(updated.acceptedAt)
    }

    @Test
    fun `cancelMeeting updates status and cancels reminders`() {
        // Given
        val meeting = createTestMeeting(status = MeetingStatus.SCHEDULED)
        createTestReminders(meeting.id, count = 4)

        // When
        meetingService.cancelMeeting(meeting.id)

        // Then
        val updated = database.virtualMeetingQueries
            .selectById(meeting.id)
            .executeAsOne()

        assertEquals(updated.status, MeetingStatus.CANCELLED)

        val reminders = database.meetingReminderQueries
            .selectByMeetingId(meeting.id)
            .executeAsList()

        assertTrue(reminders.isEmpty())
    }

    @Test
    fun `startMeeting updates status to STARTED`() {
        // Given
        val meeting = createTestMeeting(status = MeetingStatus.SCHEDULED)

        // When
        val result = meetingService.startMeeting(meeting.id)

        // Then
        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(updated.status, MeetingStatus.STARTED)
    }

    @Test
    fun `endMeeting updates status to ENDED`() {
        // Given
        val meeting = createTestMeeting(status = MeetingStatus.STARTED)

        // When
        val result = meetingService.endMeeting(meeting.id)

        // Then
        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(updated.status, MeetingStatus.ENDED)
    }

    @Test
    fun `scheduleMeetingReminders creates reminders for all timings`() {
        // Given
        val meeting = createTestMeeting(
            scheduledFor = Instant.parse("2025-12-28T10:00:00Z"),
            timezone = "Europe/Paris"
        )

        // When
        meetingService.scheduleMeetingReminders(meeting)

        // Then
        val reminders = database.meetingReminderQueries
            .selectByMeetingId(meeting.id)
            .executeAsList()

        assertTrue(reminders.isNotEmpty())
        assertTrue(reminders.any { it.timing == MeetingReminderTiming.ONE_DAY_BEFORE })
        assertTrue(reminders.any { it.timing == MeetingReminderTiming.ONE_HOUR_BEFORE })
        assertTrue(reminders.any { it.timing == MeetingReminderTiming.FIFTEEN_MINUTES_BEFORE })
        assertTrue(reminders.any { it.timing == MeetingReminderTiming.FIVE_MINUTES_BEFORE })
    }
}
```

## Platform-Specific Features

### Zoom
- **Meeting ID**: 10 digits (ex: 1234567890)
- **Password**: 6 characters alphanumeric
- **Waiting Room**: Optional, enabled by default
- **Host Key**: 6 digits for in-meeting controls
- **Dial-in**: Phone number with PIN
- **Participant Limit**: Optional

### Google Meet
- **Meet Code**: 10 characters with dash (ex: abc-defgh)
- **No Password**: No password required
- **No Waiting Room**: No waiting room feature
- **No Dial-in**: No phone dial-in
- **Participant Limit**: Optional

### FaceTime
- **Meeting ID**: Apple ID of organizer
- **Group FaceTime**: Requires all participants have Apple IDs
- **No Password**: No password required
- **No Waiting Room**: No waiting room feature
- **No Participant Limit**: No limit

## Error Handling

```kotlin
class MeetingNotFoundException(meetingId: String) :
    Exception("Meeting not found: $meetingId")

class InvitationNotFoundException(invitationId: String) :
    Exception("Invitation not found: $invitationId")

class UnsupportedPlatformException(platform: MeetingPlatform) :
    Exception("Unsupported platform: $platform")

class MeetingStartedException(meetingId: String) :
    Exception("Meeting already started: $meetingId")

class MeetingAlreadyEndedException(meetingId: String) :
    Exception("Meeting already ended: $meetingId")
```

## Limitations

**Phase 1** (Current):
1. **Mock platform APIs**: Pas d'intégration réelle avec Zoom/Google Meet
2. **Pas de gestion de participants en direct**: Pas de mute/unmute, kick, etc.
3. **Pas d'enregistrement**: Pas d'enregistrement automatique des réunions
4. **FaceTime group limit**: Limitations Apple FaceTime
5. **Pas de récurrentes**: Réunions uniques uniquement

**Phase 2** (Future):
1. **Intégration réelle APIs**:
   - Zoom API pour création de réunions
   - Google Calendar API pour Meet
   - FaceTime intégration native
2. **Gestion en direct**: Mute, kick, raise hand, etc.
3. **Enregistrement**: Option d'enregistrement automatique
4. **Réunions récurrentes**: Daily, weekly, monthly
5. **Statistiques de participation**: Temps de présence, etc.
6. **Intégration chat**: Chat intégré aux réunions

## Related Specs

- `event-organization/spec.md` - Main event management
- `calendar-management/spec.md` - Calendar integration
- `collaboration-management/spec.md` - Comments and notifications
- `notification-management/spec.md` - Push notifications (future)

---

**Version**: 1.0.0
**Last Updated**: 26 décembre 2025
**Maintainer**: Équipe Wakeve
