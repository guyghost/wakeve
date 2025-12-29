## ADDED Requirements

### Requirement: API Endpoints for Calendar Management
The server SHALL provide 5 REST API endpoints for calendar management

#### Scenario: Generate ICS Invitation
- **GIVEN** Un événement existe avec ID "event-1"
- **WHEN** Le client POST sur `/api/events/event-1/calendar/ics` avec `{"invitees": ["user1@example.com"]}`
- **THEN** Le serveur retourne un document ICS valide
- **AND** Le content contient "BEGIN:VCALENDAR" et "END:VCALENDAR"
- **AND** Le content contient les détails de l'événement (SUMMARY, DTSTART, DTEND)
- **AND** Le filename est formaté comme "Event_Name_invitation.ics"
- **AND** Le status code est 200

#### Scenario: Download ICS File
- **GIVEN** Une invitation ICS existe pour l'événement "event-1"
- **WHEN** Le client GET sur `/api/events/event-1/calendar/ics`
- **THEN** Le serveur retourne le fichier ICS
- **AND** Le Content-Type est "text/calendar"
- **AND** Le Content-Disposition est "attachment; filename=\"Event_Name_invitation.ics\""
- **AND** Le fichier peut être téléchargé et ouvert par des applications calendrier

#### Scenario: Add to Native Calendar
- **GIVEN** Un événement "event-1" existe et l'utilisateur "user-1" est participant
- **WHEN** Le client POST sur `/api/events/event-1/calendar/native` avec `{"participantId": "user-1"}`
- **THEN** Le serveur appelle le `PlatformCalendarService` approprié
- **AND** L'événement est ajouté au calendrier natif (Android CalendarContract ou iOS EventKit)
- **AND** Le serveur retourne `{"success": true, "calendarEventId": "event-1_user-1"}`
- **AND** Le status code est 200
- **AND** Sur Android, si la permission WRITE_CALENDAR est refusée, le status est 403

#### Scenario: Update Native Calendar Event
- **GIVEN** Un événement calendrier existe pour "event-1_user-1"
- **WHEN** Le client PUT sur `/api/events/event-1/calendar/native/user-1` avec `{"title": "Updated Title"}`
- **THEN** Le serveur met à jour l'événement dans le calendrier natif
- **AND** Le titre est mis à jour
- **AND** Le serveur retourne `{"success": true}`
- **AND** Le status code est 200
- **AND** Si l'événement n'existe pas, le status est 404

#### Scenario: Delete from Native Calendar
- **GIVEN** Un événement calendrier existe pour "event-1_user-1"
- **WHEN** Le client DELETE sur `/api/events/event-1/calendar/native/user-1`
- **THEN** Le serveur supprime l'événement du calendrier natif
- **AND** Le serveur retourne `{"success": true}`
- **AND** Le status code est 200
- **AND** Si l'événement n'existe pas, le status est 404

---

### Requirement: Android UI for Calendar Management
The Android app SHALL provide a complete UI for calendar management with Material You design

#### Scenario: View Calendar Options on Android
- **GIVEN** Un utilisateur Android ouvre les détails d'un événement confirmé
- **WHEN** L'utilisateur clique sur le bouton "Calendrier"
- **THEN** L'application navigue vers `CalendarScreen`
- **AND** L'écran affiche les détails de l'événement calendrier
- **AND** L'utilisateur voit un bouton "Ajouter au calendrier natif"
- **AND** L'utilisateur voit un bouton "Télécharger invitation ICS"
- **AND** Si l'événement est déjà ajouté, un bouton "Supprimer du calendrier" est visible
- **AND** L'UI respecte les guidelines Material You

#### Scenario: Add to Native Calendar with Permission Granted
- **GIVEN** Un utilisateur Android sur CalendarScreen avec la permission WRITE_CALENDAR
- **WHEN** L'utilisateur clique sur "Ajouter au calendrier natif"
- **THEN** L'application appelle `CalendarService.addToNativeCalendar(eventId, participantId)`
- **AND** Un indicateur de chargement s'affiche
- **AND** Après succès, un message de confirmation apparaît
- **AND** Le bouton change pour "Supprimer du calendrier"
- **AND** L'événement est visible dans l'application Calendrier Android

#### Scenario: Add to Native Calendar with Permission Denied
- **GIVEN** Un utilisateur Android sur CalendarScreen SANS la permission WRITE_CALENDAR
- **WHEN** L'utilisateur clique sur "Ajouter au calendrier natif"
- **THEN** L'application demande la permission WRITE_CALENDAR
- **AND** Si l'utilisateur refuse, un message explicite s'affiche
- **AND** Le bouton "Télécharger invitation ICS" est suggéré comme alternative
- **AND** Aucune erreur n'est affichée (graceful degradation)

#### Scenario: Download ICS Invitation on Android
- **GIVEN** Un utilisateur Android sur CalendarScreen
- **WHEN** L'utilisateur clique sur "Télécharger invitation ICS"
- **THEN** L'application appelle `CalendarService.generateICSInvitation(eventId, invitees)`
- **AND** Le fichier ICS est sauvegardé dans les Downloads
- **AND** Une option de partage via Android ShareSheet apparaît
- **AND** L'utilisateur peut envoyer le fichier par email, ouvrir dans Google Calendar, etc.

#### Scenario: Delete from Native Calendar on Android
- **GIVEN** Un utilisateur Android a ajouté un événement au calendrier
- **WHEN** L'utilisateur clique sur "Supprimer du calendrier"
- **THEN** L'application appelle `CalendarService.removeFromNativeCalendar(eventId, participantId)`
- **AND** L'événement est supprimé du calendrier Android
- **AND** Le bouton redevient "Ajouter au calendrier natif"
- **AND** Un message de confirmation apparaît

---

### Requirement: iOS UI for Calendar Management
The iOS app SHALL provide a complete UI for calendar management with Liquid Glass design

#### Scenario: View Calendar Options on iOS
- **GIVEN** Un utilisateur iOS ouvre les détails d'un événement confirmé
- **WHEN** L'utilisateur clique sur le bouton "Calendrier"
- **THEN** L'application navigue vers `CalendarView`
- **AND** L'écran affiche les détails de l'événement calendrier
- **AND** L'utilisateur voit un bouton "Ajouter au calendrier"
- **AND** L'utilisateur voit un bouton "Télécharger invitation ICS"
- **AND** Si l'événement est déjà ajouté, un bouton "Supprimer du calendrier" est visible
- **AND** L'UI respecte les guidelines Liquid Glass avec `.glassCard()`

#### Scenario: Add to Native Calendar on iOS
- **GIVEN** Un utilisateur iOS sur CalendarView
- **WHEN** L'utilisateur clique sur "Ajouter au calendrier"
- **THEN** L'application appelle `CalendarService.addToNativeCalendar(eventId, participantId)`
- **AND** Un indicateur de chargement s'affiche
- **AND** Après succès, une alerte de confirmation apparaît
- **AND** Le bouton change pour "Supprimer du calendrier"
- **AND** L'événement est visible dans l'application Calendrier iOS

#### Scenario: Download ICS Invitation on iOS
- **GIVEN** Un utilisateur iOS sur CalendarView
- **WHEN** L'utilisateur clique sur "Télécharger invitation ICS"
- **THEN** L'application appelle `CalendarService.generateICSInvitation(eventId, invitees)`
- **AND** Un UIDocumentPicker apparaît pour sauvegarder le fichier
- **AND** L'utilisateur peut choisir l'emplacement de sauvegarde
- **AND** Après sauvegarde, un UIActivityViewController apparaît pour partager le fichier

#### Scenario: Delete from Native Calendar on iOS
- **GIVEN** Un utilisateur iOS a ajouté un événement au calendrier
- **WHEN** L'utilisateur clique sur "Supprimer du calendrier"
- **THEN** L'application appelle `CalendarService.removeFromNativeCalendar(eventId, participantId)`
- **AND** L'événement est supprimé du calendrier iOS via EventKit
- **AND** Le bouton redevient "Ajouter au calendrier"
- **AND** Une alerte de confirmation apparaît

---

### Requirement: Test Coverage
The system SHALL have 100% test coverage with at least 15 tests for calendar functionality

#### Scenario: Generate ICS Document with All Event Details
- **GIVEN** Un événement existe avec titre "Team Meeting", description "Weekly sync", location "Room A"
- **WHEN** Le système génère une invitation ICS via `generateICSInvitation(eventId, invitees)`
- **THEN** Le document ICS contient "BEGIN:VCALENDAR" et "END:VCALENDAR"
- **AND** Le document contient "SUMMARY:Team Meeting"
- **AND** Le document contient "DESCRIPTION:Weekly sync"
- **AND** Le document contient "LOCATION:Room A"
- **AND** Le document contient "DTSTART:" et "DTEND:"
- **AND** Le test passe

#### Scenario: ICS Document Includes Correct Timezone
- **GIVEN** Un événement avec une date dans le timezone "Europe/Paris"
- **WHEN** Le système génère l'ICS
- **THEN** Le document contient le timezone correct ("TZID:Europe/Paris")
- **AND** Les dates sont correctement converties en UTC
- **AND** Le test passe

#### Scenario: ICS Document Contains VALARM for Reminders
- **GIVEN** Un événement confirmé
- **WHEN** Le système génère l'ICS
- **THEN** Le document contient deux VALARM
- **AND** Le premier VALARM a le TRIGGER "-P1DT090000" (1 jour avant à 9h)
- **AND** Le deuxième VALARM a le TRIGGER "-P1W" (1 semaine avant)
- **AND** Les VALARM ont ACTION:DISPLAY
- **AND** Le test passe

#### Scenario: PlatformCalendarService Android AddEvent
- **GIVEN** Un contexte Android avec permission WRITE_CALENDAR accordée
- **WHEN** `PlatformCalendarServiceImpl.addEvent(event)` est appelé
- **THEN** L'événement est inséré dans CalendarContract.Events
- **AND** Les participants sont insérés dans CalendarContract.Attendees
- **AND** Le résultat est `Result.success(Unit)`
- **AND** Le test passe

#### Scenario: PlatformCalendarService Android Permission Denied
- **GIVEN** Un contexte Android SANS permission WRITE_CALENDAR
- **WHEN** `PlatformCalendarServiceImpl.addEvent(event)` est appelé
- **THEN** Une `CalendarPermissionDeniedException` est levée
- **AND** Le résultat est `Result.failure(...)`
- **AND** Le test passe

#### Scenario: PlatformCalendarService iOS AddEvent
- **GIVEN** Un store EventKit sur iOS
- **WHEN** `PlatformCalendarServiceImpl.addEvent(event)` est appelé
- **THEN** Un EKEvent est créé avec les détails de l'événement
- **AND** L'événement est sauvegardé dans le calendrier via `EKEventStore.saveEvent`
- **AND** Le résultat est `Result.success(Unit)`
- **AND** Le test passe

#### Scenario: API POST /api/events/{id}/calendar/ics Generates Valid ICS
- **GIVEN** Le serveur Ktor est en cours d'exécution
- **WHEN** Un client POST sur `/api/events/event-1/calendar/ics` avec `{"invitees": ["user1@example.com"]}`
- **THEN** Le status code est 200
- **AND** Le response body contient `{ "content": "BEGIN:VCALENDAR...", "filename": "..." }`
- **AND** Le document ICS est valide
- **AND** Le test passe

#### Scenario: API POST /api/events/{id}/calendar/native Adds to Calendar
- **GIVEN** Le serveur Ktor est en cours d'exécution avec un PlatformCalendarService mock
- **WHEN** Un client POST sur `/api/events/event-1/calendar/native` avec `{"participantId": "user-1"}`
- **THEN** Le PlatformCalendarService.addEvent est appelé
- **AND** Le status code est 200
- **AND** Le response body contient `{ "success": true, "calendarEventId": "event-1_user-1" }`
- **AND** Le test passe

#### Scenario: API DELETE /api/events/{id}/calendar/native/{participantId} Removes from Calendar
- **GIVEN** Le serveur Ktor est en cours d'exécution avec un événement calendrier existant
- **WHEN** Un client DELETE sur `/api/events/event-1/calendar/native/user-1`
- **THEN** Le PlatformCalendarService.deleteEvent est appelé
- **AND** Le status code est 200
- **AND** Le response body contient `{ "success": true }`
- **AND** Le test passe

**Requirement:** All tests MUST be active (not disabled) and MUST pass (100% success rate)

---

### Requirement: Documentation
The project SHALL provide comprehensive documentation for calendar features

#### Scenario: User Guide for Android Calendar Integration
- **GIVEN** La documentation utilisateur est créée
- **WHEN** Un développeur ou utilisateur lit `docs/CALENDAR_GUIDE.md`
- **THEN** Le guide contient une section "Comment ajouter un événement au calendrier (Android)"
- **AND** Le guide explique comment gérer les permissions WRITE_CALENDAR
- **AND** Le guide inclut des screenshots de l'UI
- **AND** Le guide inclut une FAQ sur les problèmes courants

#### Scenario: User Guide for iOS Calendar Integration
- **GIVEN** La documentation utilisateur est créée
- **WHEN** Un développeur ou utilisateur lit `docs/CALENDAR_GUIDE.md`
- **THEN** Le guide contient une section "Comment ajouter un événement au calendrier (iOS)"
- **AND** Le guide explique comment utiliser EventKit
- **AND** Le guide inclut des screenshots de l'UI iOS avec Liquid Glass
- **AND** Le guide inclut une FAQ spécifique iOS

#### Scenario: Technical Architecture Documentation
- **GIVEN** La documentation technique est mise à jour
- **WHEN** Un développeur lit `CALENDAR_IMPLEMENTATION_SUMMARY.md`
- **THEN** Le document explique l'architecture finale (sans duplication)
- **AND** Le document liste tous les fichiers modifiés/créés
- **AND** Le document décrit l'API avec 5 endpoints et exemples
- **AND** Le document inclut le tests coverage report

#### Scenario: API Endpoints Documentation
- **GIVEN** La documentation API est créée
- **WHEN** Un développeur lit la documentation
- **THEN** Les 5 endpoints sont documentés avec:
  - Nom de l'endpoint
  - Méthode HTTP (POST/GET/PUT/DELETE)
  - Paramètres de path et body
  - Format de la réponse (JSON)
  - Codes d'erreur possibles (404, 403, 500)
  - Exemples de requêtes cURL ou HTTP
- **AND** Le document est intégré dans `docs/API.md`

#### Scenario: AGENTS.md Update for Calendar Agent
- **GIVEN** `AGENTS.md` existe
- **WHEN** Un développeur lit la section "Agent Calendrier"
- **THEN** La section décrit les responsabilités complètes de l'Agent Calendrier
- **AND** Les interactions avec les autres agents sont documentées
- **AND** Les fichiers implémentés sont listés
- **AND** Les scénarios de la spec sont référencés

---

## MODIFIED Requirements

### Requirement: Calendar Service Architecture
The system SHALL use a unified CalendarService architecture with PlatformCalendarService interface

**Old Location:**
```
shared/src/commonMain/kotlin/com/guyghost/wakeve/CalendarService.kt
```

**New Location:**
```
shared/src/commonMain/kotlin/com/guyghost/wakeve/calendar/CalendarService.kt
```

**Architecture:**
- `CalendarService`: Service principal pour la génération ICS et intégration native
- `PlatformCalendarService`: Interface pour les implémentations spécifiques à la plateforme
- `EnhancedCalendarService`: Interface publique pour le service de calendrier

#### Scenario: Migration vers la nouvelle architecture
- **GIVEN** Le code utilise l'ancien `CalendarService` dans le package racine
- **WHEN** Le développeur met à jour les imports vers `com.guyghost.wakeve.calendar.CalendarService`
- **THEN** Les méthodes `generateICSInvitation` et `addToNativeCalendar` sont disponibles
- **AND** L'ancien fichier `CalendarService.kt` est supprimé
- **AND** Le code compile sans erreurs

#### Scenario: Utilisation de PlatformCalendarService
- **GIVEN** Un événement doit être ajouté au calendrier natif
- **WHEN** Le système appelle `calendarService.addToNativeCalendar(eventId, participantId)`
- **THEN** Le service délègue à `platformCalendarService.addEvent(event)`
- **AND** L'implémentation Android utilise CalendarContract
- **AND** L'implémentation iOS utilise EventKit

---

### Requirement: Calendar Event Model
The system SHALL use EnhancedCalendarEvent with Instant dates instead of CalendarEvent with ISO strings

**Legacy Model (deprecated):**
```kotlin
@Deprecated("Use EnhancedCalendarEvent instead")
@Serializable
data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String,
    val startTime: String, // ISO 8601
    val endTime: String, // ISO 8601
    val timezone: String,
    val location: String? = null,
    val attendees: List<String> = emptyList(), // emails
    val organizer: String, // email
    val eventId: String // reference to our Event
)
```

**Current Model:**
```kotlin
@Serializable
data class EnhancedCalendarEvent(
    val id: String,
    val title: String,
    val description: String?,
    val location: String,
    val startDate: Instant,
    val endDate: Instant,
    val attendees: List<String>,
    val organizer: String
)
```

#### Scenario: Création d'EnhancedCalendarEvent
- **GIVEN** Un événement Wakeve avec une date confirmée
- **WHEN** Le système crée un `EnhancedCalendarEvent` pour l'ajouter au calendrier
- **THEN** Le champ `startDate` est un `Instant` (pas une chaîne ISO)
- **AND** Le champ `endDate` est un `Instant`
- **AND** La description est nullable (`String?`)
- **AND** La liste des participants contient leurs emails

#### Scenario: Conversion depuis Event vers EnhancedCalendarEvent
- **GIVEN** Un événement Wakeve avec `confirmedDate`
- **WHEN** `CalendarService` extrait la date de début et de fin
- **THEN** Les dates `Instant` sont extraites depuis `confirmedDate.startTime` et `confirmedDate.endTime`
- **AND** Si pas de `confirmedDate`, les dates sont extraites depuis le premier `TimeSlot`
- **AND** La durée par défaut est de 2 heures si aucune date n'est disponible

---

### Requirement: Specification Status
The specification status SHALL accurately reflect the implementation state

#### Scenario: Status Accurately Reflects Implementation State
- **GIVEN** La spec calendar-management/spec.md était marquée "✅ Implémenté"
- **WHEN** Le développement du changement `cleanup-complete-calendar-management` commence
- **THEN** Le statut est mis à jour à "🚧 En cours"
- **AND** La spec reflète l'état réel de l'implémentation
- **AND** Une fois toutes les tâches complétées, le statut sera mis à jour à "✅ Implémenté"

---

## REMOVED Requirements

Aucun requirement supprimé dans cette version.
