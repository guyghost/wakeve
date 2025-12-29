## 1. Implementation

### 1.1 Architecture de Base
- [x] Créer `presentation/statemachine/StateMachine.kt` (base class)
- [x] Créer `presentation/statemachine/ViewModelWrapper.kt` pour iOS
- [x] Créer `di/SharedModule.kt` pour Koin DI
- [x] Créer `di/IosFactory.kt` pour iOS factory
- [x] Tests de base: `StateMachineBasicTest.kt`

### 1.2 Event Management Workflow
- [x] Créer `presentation/state/EventManagementContract.kt`
- [x] Créer `presentation/statemachine/EventManagementStateMachine.kt`
- [x] Créer `presentation/usecase/LoadEventsUseCase.kt`
- [x] Créer `presentation/usecase/CreateEventUseCase.kt`
- [x] Tests: `StateMachineBasicTest.kt` (4 tests)
- [ ] UI Android: Mise à jour `EventListScreen.kt` avec `collectAsState()`
- [ ] UI iOS: Créer `EventListView.swift` avec `@Published state`

### 1.3 Scenario Management Workflow
- [ ] Créer `presentation/state/ScenarioManagementContract.kt`
- [ ] Créer `presentation/statemachine/ScenarioManagementStateMachine.kt`
- [ ] Créer `presentation/usecase/LoadScenariosUseCase.kt`
- [ ] Créer `presentation/usecase/CreateScenarioUseCase.kt`
- [ ] Créer `presentation/usecase/VoteScenarioUseCase.kt`
- [ ] Tests: `ScenarioManagementStateMachineTest.kt` (min 5 tests)
- [ ] UI Android: Mise à jour `ScenarioListScreen.kt` avec `collectAsState()`
- [ ] UI iOS: Mise à jour `ScenarioListView.swift` avec `@Published state`

### 1.4 Meeting Service Workflow
- [ ] Créer `presentation/state/MeetingServiceContract.kt`
- [ ] Créer `presentation/statemachine/MeetingServiceStateMachine.kt`
- [ ] Créer `presentation/usecase/CreateMeetingUseCase.kt`
- [ ] Créer `presentation/usecase/GenerateMeetingLinkUseCase.kt`
- [ ] Tests: `MeetingServiceStateMachineTest.kt` (min 5 tests)
- [ ] UI Android: Créer `MeetingCreationScreen.kt` avec `collectAsState()`
- [ ] UI iOS: Créer `MeetingCreationView.swift` avec `@Published state`

## 2. Tests

### 2.1 Tests Unitaires
- [ ] `StateMachineTest.kt` - Tests de la base class
- [ ] `ObservableStateMachineTest.kt` - Tests du bridge iOS
- [ ] `EventManagementStateMachineTest.kt` - (≥5 tests)
- [ ] `ScenarioManagementStateMachineTest.kt` - (≥5 tests)
- [ ] `MeetingServiceStateMachineTest.kt` - (≥5 tests)

### 2.2 Tests d'Intégration
- [ ] Test flux complet: Event → Scénarios → Meeting
- [ ] Test sync offline avec State Machine
- [ ] Test side effects (navigation, toasts)

## 3. Documentation

### 3.1 Architecture Documentation
- [ ] Mise à jour `docs/ARCHITECTURE.md` avec le pattern State Machine
- [ ] Guide "Comment créer une nouvelle State Machine"
- [ ] Guide "Consommer une State Machine sur Android"
- [ ] Guide "Consommer une State Machine sur iOS"

### 3.2 API Documentation
- [ ] Documenter StateMachine class
- [ ] Documenter ObservableStateMachine (iOS bridge)
- [ ] Documenter Koin DI setup
- [ ] Exemples de Use Cases

## 4. Migration Progressif

### 4.1 Features Existantes
- [ ] Audit des écrans existants à migrer
- [ ] Priorisation de la migration (high value features first)
- [ ] Plan de migration par feature

### 4.2 Nouvelles Features
- [ ] Toutes les nouvelles features doivent utiliser State Machine
- [ ] Template/boilerplate pour nouvelles State Machines
- [ ] Checklist pour reviewer les PRs avec State Machines

## 5. Revue et Validation

- [ ] Revue de code par @review
- [ ] Tous les tests passent (≥20 tests)
- [ ] Conformité design system (Material You / Liquid Glass)
- [ ] Documentation complète
- [ ] Team training sur MVI/FSM pattern

## 6. Archivage

- [ ] Toutes tâches cochées [x]
- [ ] Tests 100% passing
- [ ] Documentation complète
- [ ] Archiver avec `openspec archive implement-kmp-state-machine --yes`
