# Proposal: Suppression d'événements

## Why

Currently, users can create events in DRAFT mode but cannot delete them. The `DeleteEvent` intent exists in the contract but its implementation is a placeholder (`TODO: Implement deletion in Phase 2`).

Users need to be able to:
1. Delete drafts they no longer want to finalize
2. Cancel events they created (with notification to participants)

Without this feature, users accumulate unwanted draft events with no way to clean up their event list.

## What Changes

### Core Changes
1. **EventRepositoryInterface** - Add `deleteEvent(eventId: String): Result<Unit>` method
2. **DatabaseEventRepository** - Implement cascade delete with SQLite transaction
3. **EventManagementStateMachine** - Implement `DeleteEvent` intent handler with authorization check

### UI Changes
4. **Android (EventDetailScreen)** - Add delete button with Material You confirmation dialog
5. **iOS (ModernEventDetailView)** - Add delete button with SwiftUI alert and haptic feedback

### Test Coverage
6. **Unit tests** - Repository delete and cascade verification
7. **Integration tests** - State machine authorization and side effects
8. **E2E tests** - Full delete workflow on both platforms

## Contexte

Actuellement, les utilisateurs peuvent créer des événements en mode DRAFT mais ne peuvent pas les supprimer. L'intent `DeleteEvent` existe dans le contrat mais son implémentation est un placeholder (`TODO: Implement deletion in Phase 2`).

Les utilisateurs ont besoin de pouvoir :
1. Supprimer des brouillons qu'ils ne veulent plus finaliser
2. Annuler des événements qu'ils ont créés (avec notification aux participants)

## Objectifs

1. **Implémenter la suppression d'événements** pour l'organisateur
2. **Gérer les cas selon le status** de l'événement (DRAFT vs autres)
3. **Cascade delete** des données liées (participants, votes, time slots)
4. **UI de confirmation** avant suppression (action destructive)
5. **Synchronisation offline** de la suppression

## Scope

### In Scope
- Suppression d'événements en status DRAFT (sans confirmation complexe)
- Suppression d'événements en status POLLING/CONFIRMED (avec confirmation)
- Cascade delete des données liées dans SQLite
- UI Android (Compose) avec dialog de confirmation
- UI iOS (SwiftUI) avec alert de confirmation
- Mise à jour de la liste d'événements après suppression
- Navigation vers l'accueil après suppression

### Out of Scope
- Notification push aux participants (Phase 3 - Agent Notifications)
- Soft delete / archivage (future feature)
- Restauration d'événements supprimés
- Suppression en batch

## Impact

### Fichiers à modifier

**Shared (Kotlin Multiplatform)**
- `EventRepositoryInterface.kt` - Ajouter `deleteEvent(eventId: String): Result<Unit>`
- `EventRepository.kt` - Implémenter `deleteEvent` (in-memory)
- `DatabaseEventRepository.kt` - Implémenter `deleteEvent` (SQLite avec cascade)
- `EventManagementStateMachine.kt` - Implémenter `deleteEvent()` handler
- `Event.sq` - Vérifier les queries de suppression en cascade

**Android**
- `EventDetailScreen.kt` - Ajouter bouton de suppression avec dialog
- `HomeScreen.kt` - Option de suppression sur les cartes d'événements (optionnel)

**iOS**
- `EventDetailView.swift` - Ajouter bouton de suppression avec alert
- UI de confirmation native iOS

**Tests**
- Tests unitaires pour `deleteEvent` dans le repository
- Tests d'intégration pour la state machine
- Tests de cascade delete

## Risques

| Risque | Mitigation |
|--------|------------|
| Suppression accidentelle | Dialog de confirmation obligatoire |
| Données orphelines | Cascade delete dans les transactions SQLite |
| Sync conflict si suppression offline | Stratégie last-write-wins avec tombstone |

## Métriques de succès

- [x] L'organisateur peut supprimer un événement DRAFT
- [x] L'organisateur peut supprimer un événement POLLING/CONFIRMED avec confirmation
- [x] Les données liées sont supprimées (participants, votes, time slots)
- [x] La liste d'événements est mise à jour après suppression
- [x] L'utilisateur est redirigé vers l'accueil après suppression
- [x] Tests unitaires et d'intégration passent (couverture > 80%)
