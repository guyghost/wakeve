# Tasks: Suppression d'événements

## Phase 1: Core (Shared)

- [x] **1.1** Ajouter `deleteEvent(eventId: String): Result<Unit>` à `EventRepositoryInterface`
- [x] **1.2** Implémenter `deleteEvent` dans `EventRepository` (in-memory)
- [x] **1.3** Implémenter `deleteEvent` dans `DatabaseEventRepository` avec cascade delete
- [x] **1.4** Ajouter queries SQLDelight pour cascade delete (participants, votes, time slots, scenarios)
- [x] **1.5** Implémenter le handler `deleteEvent()` dans `EventManagementStateMachine`
  - Vérifier que l'utilisateur est l'organisateur
  - Vérifier les règles métier selon le status
  - Appeler le repository
  - Émettre les side effects (ShowToast, NavigateBack)
  - Mettre à jour le state (retirer l'événement de la liste)

## Phase 2: Tests

- [x] **2.1** Tests unitaires `deleteEvent` dans `EventRepository`
- [x] **2.2** Tests unitaires `deleteEvent` dans `DatabaseEventRepository`
- [x] **2.3** Tests cascade delete (vérifier que participants, votes, time slots sont supprimés)
- [x] **2.4** Tests `EventManagementStateMachine.deleteEvent()`
  - Test suppression DRAFT (pas de confirmation requise côté backend)
  - Test suppression POLLING (autorisé)
  - Test suppression par non-organisateur (refusé)
  - Test suppression événement inexistant (erreur)

## Phase 3: UI Android

- [x] **3.1** Ajouter bouton "Supprimer" dans `EventDetailScreen`
- [x] **3.2** Implémenter dialog de confirmation Material You
- [x] **3.3** Gérer le loading state pendant la suppression
- [x] **3.4** Navigation vers l'accueil après suppression réussie
- [x] **3.5** Afficher Toast de confirmation/erreur

## Phase 4: UI iOS

- [x] **4.1** Ajouter bouton "Supprimer" dans `EventDetailView`
- [x] **4.2** Implémenter alert de confirmation SwiftUI
- [x] **4.3** Gérer le loading state pendant la suppression
- [x] **4.4** Navigation vers l'accueil après suppression réussie
- [ ] **4.5** Feedback haptique et visuel

## Phase 5: Intégration

- [ ] **5.1** Vérifier la synchronisation offline (suppression en mode offline)
- [ ] **5.2** Tests E2E Android
- [ ] **5.3** Tests E2E iOS
- [ ] **5.4** Revue de code et accessibilité

## Definition of Done

- Tous les tests passent
- L'organisateur peut supprimer ses événements
- Les participants ne peuvent PAS supprimer les événements
- Les données liées sont supprimées en cascade
- UI conforme au design system (Material You / Liquid Glass)
- Accessibilité validée (VoiceOver/TalkBack)
