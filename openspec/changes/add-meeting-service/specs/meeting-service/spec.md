# Spécifications du Service de Réunions

## ADDED Requirements

### Requirement: Service de Réunions
Le système MUST fournir un Service de Réunions pour générer et gérer des liens de réunions virtuelles.

#### Scenario: Création d'une réunion
- **GIVEN** Un événement confirmé ou en organisation
- **AND** Un organisateur authentifié
- **WHEN** L'organisateur demande la création d'une réunion
- **THEN** Le système génère un lien valide pour la plateforme choisie (Zoom/Meet/FaceTime)
- **AND** Sauvegarde la réunion dans la base de données
- **AND** Retourne l'objet Meeting avec tous les détails

#### Scenario: Invitation des participants
- **GIVEN** Une réunion créée
- **AND** Une liste de participants validés
- **WHEN** Le système envoie les invitations
- **THEN** Chaque participant reçoit un email avec le lien, mot de passe et détails
- **AND** Seuls les participants avec statut ACCEPTED reçoivent l'invitation

#### Scenario: Rappels de réunion
- **GIVEN** Une réunion programmée
- **WHEN** Le timing de rappel est atteint (1 jour, 1 heure, 15 min avant)
- **THEN** Le système envoie des notifications push à tous les participants invités

#### Scenario: Mise à jour de réunion
- **GIVEN** Une réunion existante
- **WHEN** L'organisateur met à jour les détails (heure, durée, plateforme)
- **THEN** Le système met à jour la réunion dans la base de données
- **AND** Envoie des notifications de mise à jour aux participants

#### Scenario: Annulation de réunion
- **GIVEN** Une réunion programmée
- **WHEN** L'organisateur annule la réunion
- **THEN** Le système annule la réunion sur la plateforme externe
- **AND** Envoie des notifications d'annulation à tous les participants
- **AND** Met à jour le statut à CANCELLED

### Requirement: Plateformes Supportées
Le système MUST supporter les plateformes suivantes :
- Zoom (SDK/API)
- Google Meet (API)
- FaceTime (liens iOS)

#### Scenario: Génération de lien Zoom
- **WHEN** Plateforme = ZOOM
- **THEN** Génère un lien https://zoom.us/j/{meetingId}

#### Scenario: Génération de lien Google Meet
- **WHEN** Plateforme = GOOGLE_MEET
- **THEN** Génère un lien https://meet.google.com/{meetingId}

#### Scenario: Génération de lien FaceTime
- **WHEN** Plateforme = FACETIME
- **THEN** Génère un lien facetime://{meetingId}

### Requirement: Sécurité et Validation
Le système MUST valider les accès et sécuriser les liens.

#### Scenario: Validation d'événement
- **GIVEN** Un événement avec statut DRAFT ou POLLING
- **WHEN** Tentative de création de réunion
- **THEN** Échec avec InvalidEventStatusException

#### Scenario: Participants validés uniquement
- **GIVEN** Des participants avec statut PENDING
- **WHEN** Envoi d'invitations
- **THEN** Ces participants ne reçoivent pas d'invitation

#### Scenario: Mots de passe sécurisés
- **WHEN** Génération de réunion
- **THEN** Génère un mot de passe alphanumérique de 8 caractères

## MODIFIED Requirements

### Requirement: Intégration avec NotificationService
Le NotificationService MUST être étendu pour supporter les emails et rappels programmés.

#### Scenario: Envoi d'emails
- **WHEN** Envoi d'invitation ou rappel
- **THEN** Utilise NotificationService.sendEmail()

## REMOVED Requirements

Aucun requirement supprimé.

## Technical Specifications

### Data Models

#### Meeting
- id: String (unique)
- eventId: String (foreign key)
- organizerId: String
- title: String
- description: String? (optional)
- startTime: Instant
- duration: Duration
- platform: MeetingPlatform
- meetingLink: String
- hostMeetingId: String (extracted from link)
- password: String (8 chars alphanum)
- invitedParticipants: List<String>
- status: MeetingStatus (SCHEDULED, STARTED, ENDED, CANCELLED)
- createdAt: String (ISO timestamp)

#### MeetingPlatform Enum
- ZOOM ("Zoom")
- GOOGLE_MEET ("Google Meet")
- FACETIME ("FaceTime")

#### MeetingStatus Enum
- SCHEDULED
- STARTED
- ENDED
- CANCELLED

#### MeetingReminderTiming Enum
- ONE_DAY_BEFORE
- ONE_HOUR_BEFORE
- FIFTEEN_MINUTES_BEFORE

### Service Interface

#### MeetingService
- createMeeting(eventId, organizerId, title, description?, startTime, duration, platform, invitedParticipants): Result<Meeting>
- sendInvitation(meetingId, participantId, email, name): Result<Unit>
- sendMeetingReminders(meetingId, timing): Unit
- updateMeeting(meetingId, updates): Result<Meeting>
- cancelMeeting(meetingId): Result<Unit>

#### MeetingPlatformProvider Interface
- generateMeetingLink(platform, title, description?, startTime, duration): String
- getHostMeetingId(meetingLink): String
- cancelMeeting(platform, hostMeetingId): Unit

### Exceptions
- EventNotFoundException(eventId)
- MeetingNotFoundException(meetingId)
- InvalidEventStatusException(status)

### Database Schema
Ajouter table `meeting` avec colonnes correspondantes aux propriétés de Meeting.

### Test Coverage
Minimum 6 tests unitaires dans MeetingServiceTest.kt couvrant :
- Création réussie
- Échec sur événement non confirmé
- Génération de liens par plateforme
- Rappels programmés
- Annulation
- Mise à jour