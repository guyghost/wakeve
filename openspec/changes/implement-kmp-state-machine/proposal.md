# Proposition: Implémenter l'Architecture KMP State Machine

## Change ID
`implement-kmp-state-machine`

## Affected Specs
- **Spec**: `event-organization` (Refactoring - Intégration State Machine)
- **Spec**: `scenario-management` (Refactoring - Intégration State Machine)
- **Spec**: `meeting-service` (Refactoring - Intégration State Machine)

## Related Links
- **KMP Skill**: `/Users/guy/.config/opencode/skill/kmp`
- **Active Changes**:
  - `openspec/changes/add-meeting-service/` (Partiellement implémenté)
  - `openspec/changes/add-full-prd-features/` (100% terminé)

## Why

Le code actuel utilise une architecture Repository/Service traditionnelle mais manque d'une approche unifiée et prédictible pour la gestion d'état cross-platform. Le pattern **State Machine (MVI/FSM)** du skill KMP offre :

1. **Gestion d'état centralisée** : Tout l'état est immutable et géré dans une State Machine
2. **Cross-platform unifié** : Android (Jetpack Compose) et iOS (SwiftUI) consomment la même logique
3. **Side-effects explicites** : Events one-shot séparés de l'état (toast, navigation, etc.)
4. **Testabilité maximale** : Logique métier testable en isolation dans `commonTest`
5. **Expérience offline-first native** : StateFlow avec persistance automatique

**Problème résolu** : Actuellement, chaque feature implémente sa propre logique d'état, ce qui crée de la duplication et rend difficile la synchronisation cross-platform.

## What Changes

### 1. Nouvelle Architecture

#### Pattern State Machine (MVI/FSM)

```
┌─────────────────────────────────────────────────────────────────┐
│                    SHARED (commonMain)                          │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ State Machine│  │  Repository  │  │   Use Cases  │          │
│  │  (MVI/FSM)   │──│   (Data)     │──│   (Domain)   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│         │                                                       │
│         ▼                                                       │
│  ┌──────────────────────────────────────────────────────┐      │
│  │                    StateFlow<State>                   │      │
│  └──────────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────────┘
          │                                     │
          ▼                                     ▼
┌─────────────────────┐             ┌─────────────────────┐
│   Android (Compose) │             │    iOS (SwiftUI)    │
│                     │             │                     │
│  collectAsState()   │             │  @Published state   │
│  Material You       │             │  Liquid Glass       │
└─────────────────────┘             └─────────────────────┘
```

#### Base Classes

```kotlin
// shared/src/commonMain/kotlin/presentation/statemachine/StateMachine.kt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Base class pour toutes les state machines
 */
abstract class StateMachine<State, Intent, SideEffect>(
    initialState: State,
    private val scope: CoroutineScope
) {
    // State observable
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    // Side effects (one-shot)
    private val _sideEffect = Channel<SideEffect>(Channel.BUFFERED)
    val sideEffect: Flow<SideEffect> = _sideEffect.receiveAsFlow()

    // Current state accessor
    protected val currentState: State get() = _state.value

    /**
     * Dispatch an intent to the state machine
     */
    fun dispatch(intent: Intent) {
        scope.launch {
            handleIntent(intent)
        }
    }

    /**
     * Override to handle intents
     */
    protected abstract suspend fun handleIntent(intent: Intent)

    /**
     * Update state
     */
    protected fun updateState(reducer: (State) -> State) {
        _state.update(reducer)
    }

    /**
     * Emit side effect
     */
    protected suspend fun emitSideEffect(effect: SideEffect) {
        _sideEffect.send(effect)
    }
}
```

#### iOS Bridge

```kotlin
// shared/src/iosMain/kotlin/presentation/ViewModelWrapper.kt

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Wrapper pour exposer StateFlow à SwiftUI
 * via @Published properties
 */
class ObservableStateMachine<State, Intent, SideEffect>(
    private val stateMachine: StateMachine<State, Intent, SideEffect>
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Callback appelé sur chaque changement d'état
    var onStateChange: ((State) -> Unit)? = null

    // Callback pour les side effects
    var onSideEffect: ((SideEffect) -> Unit)? = null

    // État actuel
    val currentState: State get() = stateMachine.state.value

    init {
        // Observer state
        scope.launch {
            stateMachine.state.collect { state ->
                dispatch_async(dispatch_get_main_queue()) {
                    onStateChange?.invoke(state)
                }
            }
        }

        // Observer side effects
        scope.launch {
            stateMachine.sideEffect.collect { effect ->
                dispatch_async(dispatch_get_main_queue()) {
                    onSideEffect?.invoke(effect)
                }
            }
        }
    }

    fun dispatch(intent: Intent) {
        stateMachine.dispatch(intent)
    }

    fun dispose() {
        scope.cancel()
    }
}
```

### 2. Workflows à Implémenter

Basé sur les specs existantes, voici les State Machines à créer :

#### Workflow 1: EventManagementStateMachine

**Spec de référence**: `openspec/specs/event-organization/spec.md`

```kotlin
// Contract
object EventManagementContract {
    data class State(
        val isLoading: Boolean = false,
        val events: List<Event> = emptyList(),
        val selectedEvent: Event? = null,
        val participants: List<Participant> = emptyList(),
        val pollResults: PollResult? = null,
        val error: String? = null
    )

    sealed interface Intent {
        data object LoadEvents : Intent
        data class SelectEvent(val eventId: String) : Intent
        data class CreateEvent(val event: Event) : Intent
        data class UpdateEvent(val event: Event) : Intent
        data class DeleteEvent(val eventId: String) : Intent
        data class LoadParticipants(val eventId: String) : Intent
        data class AddParticipant(val participant: Participant) : Intent
        data class LoadPollResults(val eventId: String) : Intent
        data object ClearError : Intent
    }

    sealed interface SideEffect {
        data class ShowToast(val message: String) : SideEffect
        data class NavigateTo(val route: String) : SideEffect
        data object NavigateBack : SideEffect
    }
}
```

#### Workflow 2: ScenarioManagementStateMachine

**Spec de référence**: `openspec/specs/scenario-management/spec.md`

```kotlin
// Contract
object ScenarioManagementContract {
    data class State(
        val isLoading: Boolean = false,
        val scenarios: List<Scenario> = emptyList(),
        val selectedScenario: Scenario? = null,
        val votingResults: Map<String, ScenarioVote> = emptyMap(),
        val comparison: ScenarioComparison? = null,
        val error: String? = null
    )

    sealed interface Intent {
        data object LoadScenarios : Intent
        data class CreateScenario(val scenario: Scenario) : Intent
        data class SelectScenario(val scenarioId: String) : Intent
        data class UpdateScenario(val scenario: Scenario) : Intent
        data class DeleteScenario(val scenarioId: String) : Intent
        data class VoteScenario(val scenarioId: String, val vote: ScenarioVote) : Intent
        data class CompareScenarios(val scenarioIds: List<String>) : Intent
        data object ClearError : Intent
    }

    sealed interface SideEffect {
        data class ShowToast(val message: String) : SideEffect
        data class NavigateTo(val route: String) : SideEffect
        data class NavigateBack : SideEffect
    }
}
```

#### Workflow 3: MeetingServiceStateMachine

**Spec de référence**: `openspec/specs/meeting-service/spec.md`

```kotlin
// Contract
object MeetingServiceContract {
    data class State(
        val isLoading: Boolean = false,
        val meetings: List<Meeting> = emptyList(),
        val selectedMeeting: Meeting? = null,
        val generatedLink: String? = null,
        val error: String? = null
    )

    sealed interface Intent {
        data object LoadMeetings : Intent
        data class CreateMeeting(val meeting: Meeting) : Intent
        data class UpdateMeeting(val meeting: Meeting) : Intent
        data class CancelMeeting(val meetingId: String) : Intent
        data class GenerateMeetingLink(val meeting: Meeting, val platform: MeetingPlatform) : Intent
        data object ClearError : Intent
    }

    sealed interface SideEffect {
        data class ShowToast(val message: String) : SideEffect
        data class NavigateTo(val route: String) : SideEffect
        data class ShareMeetingLink(val link: String) : SideEffect
    }
}
```

### 3. Structure de Fichiers Nouvelle

```
shared/src/commonMain/kotlin/com/guyghost/wakeve/
├── presentation/                        # 🆕 Nouveau package
│   ├── statemachine/
│   │   ├── StateMachine.kt            # Base class
│   │   ├── EventManagementStateMachine.kt
│   │   ├── ScenarioManagementStateMachine.kt
│   │   ├── MeetingServiceStateMachine.kt
│   │   └── BudgetManagementStateMachine.kt
│   ├── state/                         # 🆕 State & Intent definitions
│   │   ├── EventManagementContract.kt
│   │   ├── ScenarioManagementContract.kt
│   │   ├── MeetingServiceContract.kt
│   │   └── BudgetManagementContract.kt
│   └── usecase/                       # 🆕 Use cases
│       ├── LoadEventsUseCase.kt
│       ├── CreateEventUseCase.kt
│       ├── LoadScenariosUseCase.kt
│       └── CreateScenarioUseCase.kt
├── models/                            # 🔄 Existing - Unchanged
│   ├── Event.kt
│   ├── ScenarioModels.kt
│   └── MeetingModels.kt
├── repository/                        # 🔄 Existing - Refactored as UseCases
│   ├── EventRepository.kt
│   ├── ScenarioRepository.kt
│   └── MeetingRepository.kt
└── services/                          # 🔄 Existing - Used by State Machines
    ├── PollLogic.kt
    ├── ScenarioLogic.kt
    └── calendar/
```

### 4. Dependency Injection (Koin)

```kotlin
// shared/src/commonMain/kotlin/di/SharedModule.kt

val sharedModule = module {
    // Repository (data layer)
    single<EventRepository> { DatabaseEventRepository(get()) }
    single<ScenarioRepository> { ScenarioRepository(get()) }

    // Use Cases (domain layer)
    factory { LoadEventsUseCase(get()) }
    factory { CreateEventUseCase(get()) }
    factory { LoadScenariosUseCase(get()) }
    factory { CreateScenarioUseCase(get()) }

    // State Machines (presentation layer - scoped per screen)
    factory { (scope: CoroutineScope) ->
        EventManagementStateMachine(
            loadEventsUseCase = get(),
            createEventUseCase = get(),
            scope = scope
        )
    }
    factory { (scope: CoroutineScope) ->
        ScenarioManagementStateMachine(
            loadScenariosUseCase = get(),
            createScenarioUseCase = get(),
            scope = scope
        )
    }
}

// Init Koin
fun initKoin() {
    startKoin {
        modules(sharedModule)
    }
}
```

### 5. Consommation Android (Jetpack Compose)

```kotlin
// composeApp/src/androidMain/kotlin/com/guyghost/wakeve/EventListScreen.kt

@Composable
fun EventListScreen(
    viewModel: EventManagementViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is EventManagementContract.SideEffect.ShowToast -> {
                    // Show snackbar
                }
                is EventManagementContract.SideEffect.NavigateTo -> {
                    // Navigate
                }
                is EventManagementContract.SideEffect.NavigateBack -> {
                    // Pop back
                }
            }
        }
    }

    EventListContent(
        state = state,
        onIntent = { viewModel.dispatch(it) }
    )
}

@Composable
private fun EventListContent(
    state: EventManagementContract.State,
    onIntent: (EventManagementContract.Intent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events") }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.hasError -> {
                ErrorContent(
                    message = state.error ?: "Error",
                    onRetry = { onIntent(EventManagementContract.Intent.LoadEvents) }
                )
            }
            else -> {
                EventsList(
                    events = state.events,
                    onItemClick = { eventId ->
                        onIntent(EventManagementContract.Intent.SelectEvent(eventId))
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}
```

### 6. Consommation iOS (SwiftUI)

```swift
// iosApp/iosApp/Views/EventListView.swift

import SwiftUI
import shared

class EventListViewModel: ObservableObject {
    @Published var state: EventManagementContract.State

    private let stateMachine: ObservableStateMachine<
        EventManagementContract.State,
        EventManagementContract.Intent,
        EventManagementContract.SideEffect
    >

    init() {
        self.stateMachine = IosFactory().createEventStateMachine()
        self.state = stateMachine.currentState

        stateMachine.onStateChange = { [weak self] newState in
            self?.state = newState
        }

        stateMachine.onSideEffect = { [weak self] effect in
            self?.handleSideEffect(effect)
        }
    }

    func dispatch(_ intent: EventManagementContract.Intent) {
        stateMachine.dispatch(intent: intent)
    }

    private func handleSideEffect(_ effect: EventManagementContract.SideEffect) {
        switch effect {
        case let toast as EventManagementContract.SideEffectShowToast:
            // Show toast
            break
        case let nav as EventManagementContract.SideEffectNavigateTo:
            // Navigate
            break
        default:
            break
        }
    }

    deinit {
        stateMachine.dispose()
    }
}

struct EventListView: View {
    @StateObject private var viewModel = EventListViewModel()

    var body: some View {
        ZStack {
            content

            if viewModel.state.isLoading {
                ProgressView()
            }
        }
        .navigationTitle("Events")
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.state.hasError {
            ErrorView(
                message: viewModel.state.error ?? "Error",
                onRetry: {
                    viewModel.dispatch(.LoadEvents())
                }
            )
        } else {
            List(viewModel.state.events, id: \.id) { event in
                EventRow(event: event)
                    .onTapGesture {
                        viewModel.dispatch(.SelectEvent(eventId: event.id))
                    }
            }
        }
    }
}
```

### 7. iOS Factory

```kotlin
// shared/src/iosMain/kotlin/di/IosFactory.kt

object IosFactory {

    fun createEventStateMachine(): ObservableStateMachine<
        EventManagementContract.State,
        EventManagementContract.Intent,
        EventManagementContract.SideEffect
    > {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = Koin.get(),
            createEventUseCase = Koin.get(),
            scope = scope
        )
        return ObservableStateMachine(stateMachine)
    }

    fun createScenarioStateMachine(): ObservableStateMachine<
        ScenarioManagementContract.State,
        ScenarioManagementContract.Intent,
        ScenarioManagementContract.SideEffect
    > {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val stateMachine = ScenarioManagementStateMachine(
            loadScenariosUseCase = Koin.get(),
            createScenarioUseCase = Koin.get(),
            scope = scope
        )
        return ObservableStateMachine(stateMachine)
    }

    fun createMeetingServiceStateMachine(): ObservableStateMachine<
        MeetingServiceContract.State,
        MeetingServiceContract.Intent,
        MeetingServiceContract.SideEffect
    > {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val stateMachine = MeetingServiceStateMachine(
            // Dependencies from Koin
            scope = scope
        )
        return ObservableStateMachine(stateMachine)
    }
}
```

### 8. Tests Unitaires

```kotlin
// shared/src/commonTest/kotlin/presentation/statemachine/EventManagementStateMachineTest.kt

import kotlinx.coroutines.test.*
import kotlin.test.*

class EventManagementStateMachineTest {

    private lateinit var stateMachine: EventManagementStateMachine
    private lateinit var testScope: TestScope

    @BeforeTest
    fun setup() {
        testScope = TestScope()
        val mockLoadEventsUseCase = MockLoadEventsUseCase()
        val mockCreateEventUseCase = MockCreateEventUseCase()
        stateMachine = EventManagementStateMachine(
            loadEventsUseCase = mockLoadEventsUseCase,
            createEventUseCase = mockCreateEventUseCase,
            scope = testScope
        )
    }

    @Test
    fun `initial state is loading`() {
        val state = stateMachine.state.value
        assertTrue(state.isLoading)
        assertTrue(state.events.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `LoadEvents intent should load events`() = runTest {
        stateMachine.dispatch(EventManagementContract.Intent.LoadEvents)

        testScope.advanceUntilIdle()

        val state = stateMachine.state.value
        assertFalse(state.isLoading)
        assertFalse(state.events.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `SelectEvent intent should emit NavigateTo side effect`() = runTest {
        val eventId = "event-123"
        stateMachine.dispatch(EventManagementContract.Intent.SelectEvent(eventId))

        testScope.advanceUntilIdle()

        val sideEffects = mutableListOf<SideEffect>()
        val job = launch {
            stateMachine.sideEffect.collect { sideEffects.add(it) }
        }

        testScope.advanceUntilIdle()

        val navigateEffect = sideEffects.find {
            it is EventManagementContract.SideEffect.NavigateTo
        } as? EventManagementContract.SideEffect.NavigateTo

        assertNotNull(navigateEffect)
        assertEquals("detail/$eventId", navigateEffect.route)

        job.cancel()
    }
}
```

## Impact

### Affected Components
- ✅ **shared/presentation/** - Nouveau package avec State Machines
- ✅ **shared/di/** - Koin pour DI cross-platform
- ✅ **shared/src/iosMain/** - Bridges iOS (ObservableStateMachine)
- ✅ **composeApp/** - Mise à jour des ViewModels pour utiliser StateFlow
- ✅ **iosApp/** - Mise à jour des ViewModels SwiftUI pour utiliser @Published

### Database Migration
- ✅ Aucune migration nécessaire - La base de données reste inchangée

### API Breaking Changes
- ✅ Aucun - Les repositories existants restent compatibles
- ✅ Les State Machines utilisent les repositories existants comme Use Cases

### Backward Compatibility
- ✅ Les repositories existants peuvent être utilisés directement par du code legacy
- ✅ Les nouvelles State Machines coexistent avec l'ancienne architecture
- ✅ Migration progressive écran par écran

## Prioritization

### Phase 1 - Base Architecture (Sprint 1)
**Objectif**: Mise en place des fondations

- ✅ Base `StateMachine` class
- ✅ `ViewModelWrapper` pour iOS (`ObservableStateMachine`)
- ✅ Koin DI setup
- ✅ Tests unitaires de base

**Critère de succès**: Architecture testée avec 3 State Machines simples

### Phase 2 - Event Management (Sprint 2)
**Objectif**: Refactoring de la gestion d'événements

- `EventManagementContract` (State, Intent, SideEffect)
- `EventManagementStateMachine` implémentation
- Use Cases: `LoadEventsUseCase`, `CreateEventUseCase`
- UI Android: `EventListScreen` avec `collectAsState()`
- UI iOS: `EventListView` avec `@Published`
- Tests complets

**Critère de succès**: Liste d'événements fonctionnelle avec State Machine

### Phase 3 - Scenario Management (Sprint 3)
**Objectif**: Refactoring de la gestion de scénarios

- `ScenarioManagementContract`
- `ScenarioManagementStateMachine`
- Use Cases: `LoadScenariosUseCase`, `CreateScenarioUseCase`, `VoteScenarioUseCase`
- UI Android: `ScenarioListScreen`, `ScenarioDetailScreen`
- UI iOS: `ScenarioListView`, `ScenarioDetailView`
- Tests complets

**Critère de succès**: Gestion complète des scénarios avec State Machine

### Phase 4 - Meeting Service (Sprint 4)
**Objectif**: Refactoring du service de réunions

- `MeetingServiceContract`
- `MeetingServiceStateMachine`
- Use Cases: `CreateMeetingUseCase`, `GenerateMeetingLinkUseCase`
- UI Android: `MeetingCreationScreen`, `MeetingDetailScreen`
- UI iOS: `MeetingCreationView`, `MeetingDetailView`
- Tests complets

**Critère de succès**: Création et gestion de réunions avec State Machine

### Phase 5 - Migration Restante (Sprint 5-6)
**Objectif**: Migration progressive de toutes les features

- Budget Management State Machine
- Accommodation State Machine
- Meal Planning State Machine
- Equipment State Machine
- Activity State Machine

**Critère de succès**: Toutes les features utilisent le pattern State Machine

## Risks & Mitigations

### Risque 1: Complexité d'apprentissage
**Impact**: Équipe unfamiliarisée avec MVI/FSM
**Mitigation**:
- Documentation exhaustive avec exemples
- Workshops internes sur le pattern
- Code reviews strictes

### Risque 2: Performance du bridge iOS
**Impact**: Le bridge Kotlin/Swift peut introduire de la latence
**Mitigation**:
- Utiliser `dispatch_async` pour les callbacks
- Minimiser les appels entre Kotlin et Swift
- Profiling régulier

### Risque 3: Effort de refactorisation
**Impact**: 4-6 sprints de migration
**Mitigation**:
- Migration progressive par feature
- Nouvelles features utilisent d'abord le pattern
- Tests automatisés pour éviter régressions

### Risque 4: Conflits avec code existant
**Impact**: Difficulté de merge avec branches parallèles
**Mitigation**:
- Utiliser feature flags
- Communication fréquente
- Intégration continue continue

## Success Metrics

### Adoption
- ✅ 100% des nouvelles features utilisent State Machine
- ✅ 80% des features existantes migrées dans 6 mois
- ✅ Tous les développeurs formés au pattern

### Technique
- ✅ 100% des State Machines ont des tests unitaires
- ✅ Latence UI < 50ms (Android + iOS)
- ✅ 0 régression de fonctionnalité

### Qualité
- ✅ Architecture unifiée cross-platform
- ✅ Code plus maintenable et testable
- ✅ Expérience développeur améliorée

## Next Steps

1. ✅ **Approbation de la proposition** par l'équipe
2. ✅ **Setup Phase 1** - Architecture de base
3. ✅ **Démarrer Phase 2** - Event Management
4. ✅ **Formation équipe** sur le pattern MVI/FSM
5. ✅ **Documentation** des guidelines de contribution

---

**Proposition créée**: 29 décembre 2025
**Auteur**: Équipe Wakeve
**Status**: En attente d'approbation
**Effort estimé**: 4-6 sprints (8-12 semaines)
