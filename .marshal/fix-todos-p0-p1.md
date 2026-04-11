# Fix TODOs critiques — P0 et P1

Implémentation des fonctionnalités stub et correction des lacunes identifiées lors du tour des TODOs.

## Goals
- Implémenter le tracking des interactions suggestions (7 stubs → vraie DB)
- Implémenter lastSyncTimestamp dans SyncManager (full-sync → incrémental)
- Écrire de vrais tests pour MeetingServiceStateMachine et CancelMeetingUseCase
- Corriger les 3 failles auth dans CommentRoutes (optionnel si complexe)

## Pipeline Checklist

### Groupe 1 — Suggestion Interactions (P0)
- [x] @codegen — Implémenter les 7 méthodes stub dans `DatabaseSuggestionPreferencesRepository`
  - `trackInteraction()` → `insertInteraction` SQLDelight
  - `trackInteractionWithMetadata()` → `insertInteraction` avec metadata JSON
  - `getInteractionHistory()` → `selectInteractionsByUserId`
  - `getRecentInteractions()` → `selectRecentInteractionsByUserId`
  - `getInteractionCountsByType()` → `countInteractionsByType`
  - `getTopSuggestions()` → `getTopSuggestionsByInteractions`
  - `cleanupOldInteractions()` → `deleteOldInteractions`
- [x] @tests — 17 tests unitaires pour les 7 nouvelles méthodes (SuggestionInteractionTrackingTest)
- [x] Vérifier : `./gradlew shared:jvmTest` ✅

### Groupe 2 — SyncManager lastSyncTimestamp (P0)
- [x] @codegen — Implémenter le tracking du timestamp dans `SyncManager.kt:390`
  - Stocker le timestamp après sync réussie
  - L'utiliser pour les syncs incrémentales
- [x] Vérifier : `./gradlew shared:jvmTest` ✅

### Groupe 3 — Meeting Tests (P1)
- [x] @tests — 10 vrais tests pour `MeetingServiceStateMachineTest` (jvmTest)
- [x] @tests — 2 vrais tests pour `CancelMeetingUseCaseTest` (jvmTest)
- [x] Vérifier : `./gradlew shared:jvmTest` ✅

### Validation finale
- [x] Tests passent : BUILD SUCCESSFUL
- [x] Commits par groupe (3 commits Conventional Commits)
- [x] @review — Prêt

## Correction Loop (max 10 en Marshal)
- En attente

## Verification
- Tests : `./gradlew shared:jvmTest --rerun-tasks --no-configuration-cache`
- Fichiers principaux :
  - `shared/src/commonMain/kotlin/com/guyghost/wakeve/suggestions/DatabaseSuggestionPreferencesRepository.kt`
  - `shared/src/commonMain/kotlin/com/guyghost/wakeve/sync/SyncManager.kt`
  - `shared/src/commonTest/kotlin/com/guyghost/wakeve/presentation/statemachine/MeetingServiceStateMachineTest.kt`
  - `shared/src/commonTest/kotlin/com/guyghost/wakeve/presentation/usecase/CancelMeetingUseCaseTest.kt`

## Notes
- La table `suggestion_interactions` et toutes les queries SQLDelight sont déjà créées
- `MockMeetingRepository` existe dans commonTest
- `createFreshTestDatabase()` est disponible dans jvmTest helpers
- Pattern: FC&IS — les méthodes de repository sont dans l'Imperative Shell
