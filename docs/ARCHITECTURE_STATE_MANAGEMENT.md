# Architecture State Management - Wakeve

## Vue d'ensemble

Wakeve utilise une architecture **MVI (Model-View-Intent)** avec **State Machines** partagées entre Android et iOS via Kotlin Multiplatform.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ARCHITECTURE STATE MANAGEMENT                      │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                            SHARED (Kotlin Multiplatform)                    │
│                              Source de vérité unique                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    STATE MACHINES (commonMain)                       │   │
│  │                                                                      │   │
│  │   ┌─────────────────────┐  ┌─────────────────────┐                  │   │
│  │   │ EventManagementState│  │  AuthStateMachine   │                  │   │
│  │   │      Machine        │  │                     │                  │   │
│  │   │                     │  │  - Login/Logout     │                  │   │
│  │   │  - Load Events      │  │  - OAuth (Google/   │                  │   │
│  │   │  - Create Event     │  │    Apple)           │                  │   │
│  │   │  - Update Event     │  │  - Guest Mode       │                  │   │
│  │   │  - Delete Event     │  │  - Token Refresh    │                  │   │
│  │   │  - State Transitions│  │                     │                  │   │
│  │   │    (DRAFT→POLLING→  │  └─────────────────────┘                  │   │
│  │   │     CONFIRMED→...)  │                                        │   │
│  │   └─────────────────────┘                                        │   │
│  │                              ┌─────────────────────┐              │   │
│  │                              │ ScenarioManagement  │              │   │
│  │                              │    StateMachine     │              │   │
│  │                              │                     │              │   │
│  │                              │  - Create Scenario  │              │   │
│  │                              │  - Vote Scenario    │              │   │
│  │                              │  - Select Final     │              │   │
│  │                              └─────────────────────┘              │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      CONTRACTS (commonMain)                          │   │
│  │                                                                      │   │
│  │  EventManagementContract        AuthContract                        │   │
│  │  ├── State (data class)         ├── State                           │   │
│  │  ├── Intent (sealed class)      ├── Intent                          │   │
│  │  └── SideEffect (sealed class)  └── SideEffect                      │   │
│  │                                                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      REPOSITORIES (commonMain)                       │   │
│  │                                                                      │   │
│  │  EventRepository              AuthRepository                        │   │
│  │  ├── getEvents()              ├── login()                           │   │
│  │  ├── createEvent()            ├── logout()                          │   │
│  │  ├── updateEvent()            └── refreshToken()                    │   │
│  │  └── deleteEvent()                                                  │   │
│  │                                                                      │   │
│  │  SQLDelight Database (SQLite)                                       │   │
│  │  ├── Offline-first                                                  │   │
│  │  └── Cross-platform persistence                                     │   │
│  │                                                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ StateFlow / Flow
                                      │ (Kotlin Coroutines)
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                    ANDROID                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    VIEWMODELS (commonMain)                           │   │
│  │                    Wrappers autour des StateMachines                 │   │
│  │                                                                      │   │
│  │   ┌─────────────────────────┐  ┌─────────────────────────┐          │   │
│  │   │ EventManagementViewModel│  │      AuthViewModel      │          │   │
│  │   │                         │  │                         │          │   │
│  │   │ - state: StateFlow      │  │ - state: StateFlow      │          │   │
│  │   │ - sideEffect: Flow      │  │ - sideEffect: Flow      │          │   │
│  │   │ - dispatch(intent)      │  │ - dispatch(intent)      │          │   │
│  │   │                         │  │                         │          │   │
│  │   │ Expose le StateMachine  │  │ Expose le StateMachine  │          │   │
│  │   │ au format Android       │  │ au format Android       │          │   │
│  │   └─────────────────────────┘  └─────────────────────────┘          │   │
│  │                                                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                      │                                      │
│                                      │ collectAsState()                      │
│                                      ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    UI LAYER (androidMain)                            │   │
│  │                    Jetpack Compose                                   │   │
│  │                                                                      │   │
│  │   @Composable                                                        │   │
│  │   fun HomeScreen(viewModel: EventManagementViewModel = koinInject()) │   │
│  │                                                                      │   │
│  │   val state by viewModel.state.collectAsState()                      │   │
│  │                                                                      │   │
│  │   LaunchedEffect(Unit) {                                             │   │
│  │       viewModel.sideEffect.collect { effect ->                       │   │
│  │           when(effect) {                                             │   │
│  │               is NavigateTo -> navController.navigate(effect.route)  │   │
│  │               is ShowToast -> showToast(effect.message)              │   │
│  │           }                                                          │   │
│  │       }                                                              │   │
│  │   }                                                                  │   │
│  │                                                                      │   │
│  │   EventListContent(                                                  │   │
│  │       state = state,                                                 │   │
│  │       onIntent = { viewModel.dispatch(it) }                          │   │
│  │   )                                                                  │   │
│  │                                                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  Navigation: Jetpack Navigation Compose                                    │
│  DI: Koin                                                                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ Même StateMachine Shared
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                      iOS                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    VIEWMODELS (Swift)                                │   │
│  │                    Wrappers autour des StateMachines                 │   │
│  │                                                                      │   │
│  │   ┌─────────────────────────┐  ┌─────────────────────────┐          │   │
│  │   │  EventListViewModel     │  │    ProfileViewModel     │          │   │
│  │   │  (ObservableObject)     │  │    (ObservableObject)   │          │   │
│  │   │                         │  │                         │          │   │
│  │   │ @Published var state    │  │ @Published var state    │          │   │
│  │   │ @Published var toastMsg │  │ @Published var user     │          │   │
│  │   │                         │  │                         │          │   │
│  │   │ private let stateMachine│  │ private let stateMachine│          │   │
│  │   │ Wrapper: ObservableState│  │ Wrapper: ObservableState│          │   │
│  │   │   Machine<State,Intent, │  │   Machine<State,Intent, │          │   │
│  │   │   SideEffect>           │  │   SideEffect>           │          │   │
│  │   │                         │  │                         │          │   │
│  │   │ func dispatch(_ intent) │  │ func dispatch(_ intent) │          │   │
│  │   │   stateMachine.dispatch │  │   stateMachine.dispatch │          │   │
│  │   │ }                       │  │ }                       │          │   │
│  │   └─────────────────────────┘  └─────────────────────────┘          │   │
│  │                                                                      │   │
│  │   Wrapper Kotlin→Swift: ObservableStateMachine                      │   │
│  │   - Gère la conversion StateFlow → @Published                       │   │
│  │   - Gère les coroutines Kotlin → DispatchQueue.main                 │   │
│  │                                                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                      │                                      │
│                                      │ @StateObject                          │
│                                      ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    UI LAYER (SwiftUI)                                │   │
│  │                                                                      │   │
│  │   struct EventListView: View {                                       │   │
│  │       @StateObject private var viewModel = EventListViewModel()      │   │
│  │                                                                      │   │
│  │       var body: some View {                                          │   │
│  │           List(viewModel.state.events) { event in                    │   │
│  │               EventRow(event)                                        │   │
│  │                   .onTapGesture {                                    │   │
│  │                       viewModel.selectEvent(eventId: event.id)       │   │
│  │                   }                                                  │   │
│  │           }                                                          │   │
│  │           .alert(item: $viewModel.toastMessage) { msg in             │   │
│  │               Alert(title: Text(msg))                                │   │
│  │           }                                                          │   │
│  │       }                                                              │   │
│  │   }                                                                  │   │
│  │                                                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  Navigation: SwiftUI Navigation                                            │
│  Design: Liquid Glass (iOS 26+)                                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Détail des Flux de Données

### 1. Flux de données Android

```
┌─────────────┐     Intent      ┌──────────────────────┐
│   UI Layer  │ ───────────────► │ EventManagementView  │
│   (Compose) │                  │       Model          │
└─────────────┘                  └──────────┬───────────┘
      ▲                                     │
      │ StateFlow                           │ dispatch(intent)
      │                                     ▼
      │                          ┌──────────────────────┐
      └──────────────────────────│ EventManagementState │
                                 │       Machine        │
                                 │   (Shared KMP)       │
                                 └──────────┬───────────┘
                                            │
                                            │ Coroutines
                                            ▼
                                 ┌──────────────────────┐
                                 │  EventRepository     │
                                 │  (Shared KMP)        │
                                 └──────────┬───────────┘
                                            │
                                            ▼
                                 ┌──────────────────────┐
                                 │   SQLDelight DB      │
                                 │   (SQLite)           │
                                 └──────────────────────┘
```

### 2. Flux de données iOS

```
┌─────────────┐     Intent      ┌──────────────────────┐
│   UI Layer  │ ───────────────► │ EventListViewModel   │
│   (SwiftUI) │                  │     (Swift)          │
└─────────────┘                  └──────────┬───────────┘
      ▲                                     │
      │ @Published                          │ dispatch(intent)
      │                                     ▼
      │                          ┌──────────────────────┐
      └──────────────────────────│ ObservableStateMachine│
                                 │     (Wrapper KMP→Swift)│
                                 └──────────┬───────────┘
                                            │
                                            │ dispatch(intent)
                                            ▼
                                 ┌──────────────────────┐
                                 │ EventManagementState │
                                 │      Machine         │
                                 │    (Shared KMP)      │
                                 └──────────┬───────────┘
                                            │
                                            ▼
                                 ┌──────────────────────┐
                                 │   EventRepository    │
                                 │    (Shared KMP)      │
                                 └──────────┬───────────┘
                                            │
                                            ▼
                                 ┌──────────────────────┐
                                 │   SQLDelight DB      │
                                 │     (SQLite)         │
                                 └──────────────────────┘
```

---

## Tableau Comparatif Implémentation

### ✅ Ce qui est implémenté

| Feature | Android | iOS | Shared (KMP) |
|---------|---------|-----|--------------|
| **Architecture** | MVI + Compose | MVI + SwiftUI | State Machines |
| **State Management** | ViewModel + StateFlow | ObservableObject + @Published | StateFlow/Flow |
| **Navigation** | Jetpack Navigation | SwiftUI Navigation | Routes définies |
| **Auth State** | ✅ AuthStateMachine | ✅ AuthStateMachine | ✅ AuthStateMachine |
| **Event Management** | ✅ EventManagementViewModel | ✅ EventListViewModel | ✅ EventManagementStateMachine |
| **Scenario Management** | ✅ ScenarioManagementViewModel | ✅ ScenarioListViewModel | ✅ ScenarioManagementStateMachine |
| **Meeting Management** | ✅ MeetingManagementViewModel | ✅ MeetingListViewModel | ✅ MeetingServiceStateMachine |
| **Offline-First** | ✅ SQLDelight + Repository | ✅ SQLDelight + Repository | ✅ Repository Pattern |
| **Deep Linking** | ✅ DeepLinkHandler | ✅ DeepLinkService | ✅ DeepLink + Handler |
| **Notifications** | ✅ RichNotificationManager | ⚠️ Partial | ✅ RichNotificationService |

### 🔄 Flux de Transition d'État (Event)

```
                    ┌─────────────────────────────────────────────────────────────┐
                    │                    EVENT STATE WORKFLOW                      │
                    └─────────────────────────────────────────────────────────────┘

┌──────────┐    Create Event     ┌──────────┐    Start Poll     ┌──────────┐
│   IDLE   │ ───────────────────►│  DRAFT   │ ────────────────► │ POLLING  │
└──────────┘                     └──────────┘                   └────┬─────┘
                                                                     │
                       ┌─────────────────────────────────────────────┘
                       │ Confirm Date
                       ▼
┌──────────┐    Create  ┌──────────┐    Vote   ┌──────────┐
│ FINALIZED│◄───────────│ CONFIRMED│◄──────────│ SCENARIO │
└──────────┘            │          │           │ COMPARE  │
   ▲                    └────┬─────┘           └──────────┘
   │                         │
   │ Mark Finalized          │ Select Scenario
   │                         ▼
   │                    ┌──────────┐
   │                    │ORGANIZING│
   │                    │          │ Create Meeting
   │                    └────┬─────┘
   │                         │
   └─────────────────────────┘

State Machine: EventManagementStateMachine
- Gère toutes les transitions
- Valide les règles métier
- Émet les SideEffects (navigation)
```

---

## Détail des State Machines

### 1. EventManagementStateMachine

```kotlin
// Shared (commonMain)
class EventManagementStateMachine(
    private val loadEventsUseCase: LoadEventsUseCase,
    private val createEventUseCase: CreateEventUseCase,
    scope: CoroutineScope
) {
    // État observable
    val state: StateFlow<EventManagementContract.State>
    
    // Effets de bord (navigation, toasts)
    val sideEffect: Flow<EventManagementContract.SideEffect>
    
    // Méthode principale
    fun dispatch(intent: EventManagementContract.Intent)
}

// Contract
object EventManagementContract {
    data class State(
        val isLoading: Boolean = false,
        val events: List<Event> = emptyList(),
        val selectedEvent: Event? = null,
        val error: String? = null
    )
    
    sealed class Intent {
        data object LoadEvents : Intent()
        data class CreateEvent(val title: String, ...) : Intent()
        data class SelectEvent(val eventId: String) : Intent()
        data class StartPoll(val eventId: String) : Intent()
        data class ConfirmDate(val eventId: String, val slotId: String) : Intent()
        // ... etc
    }
    
    sealed class SideEffect {
        data class NavigateTo(val route: String) : SideEffect()
        data class ShowToast(val message: String) : SideEffect()
        data object NavigateBack : SideEffect()
    }
}
```

### 2. AuthStateMachine

```kotlin
// Shared (commonMain)
class AuthStateMachine(
    private val authService: AuthService,
    scope: CoroutineScope
) {
    val state: StateFlow<AuthContract.State>
    val sideEffect: Flow<AuthContract.SideEffect>
    
    fun dispatch(intent: AuthContract.Intent)
}

// Contract
object AuthContract {
    data class State(
        val isLoading: Boolean = false,
        val isAuthenticated: Boolean = false,
        val isGuest: Boolean = false,
        val currentUser: User? = null,
        val error: String? = null
    )
    
    sealed class Intent {
        data class LoginWithEmail(val email: String, val password: String) : Intent()
        data class LoginWithGoogle(val token: String) : Intent()
        data class LoginWithApple(val token: String) : Intent()
        data object LoginAsGuest : Intent()
        data object Logout : Intent()
        data object CheckSession : Intent()
    }
    
    sealed class SideEffect {
        data object NavigateToMain : SideEffect()
        data object NavigateToOnboarding : SideEffect()
        data class ShowError(val message: String) : SideEffect()
    }
}
```

---

## Implémentation Android Détail

### ViewModel Pattern

```kotlin
// Android (commonMain)
class EventManagementViewModel(
    private val stateMachine: EventManagementStateMachine
) : ViewModel() {

    // Expose le StateFlow du StateMachine
    val state: StateFlow<EventManagementContract.State> = stateMachine.state
    
    // Expose les side effects
    val sideEffect: Flow<EventManagementContract.SideEffect> = stateMachine.sideEffect
    
    // Delegation au StateMachine
    fun dispatch(intent: EventManagementContract.Intent) {
        stateMachine.dispatch(intent)
    }
}
```

### Compose Integration

```kotlin
// Android (androidMain)
@Composable
fun HomeScreen(
    viewModel: EventManagementViewModel = koinInject()
) {
    // Collecte l'état
    val state by viewModel.state.collectAsState()
    
    // Gère les side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is SideEffect.NavigateTo -> navController.navigate(effect.route)
                is SideEffect.ShowToast -> showToast(effect.message)
                is SideEffect.NavigateBack -> navController.popBackStack()
            }
        }
    }
    
    // UI réactive
    EventListContent(
        events = state.events,
        isLoading = state.isLoading,
        onEventClick = { eventId ->
            viewModel.dispatch(Intent.SelectEvent(eventId))
        },
        onCreateEvent = { title, description ->
            viewModel.dispatch(Intent.CreateEvent(title, description))
        }
    )
}
```

---

## Implémentation iOS Détail

### ViewModel Pattern

```swift
// iOS (Swift)
@MainActor
class EventListViewModel: ObservableObject {
    
    // État observable par SwiftUI
    @Published var state: EventManagementContract.State
    @Published var toastMessage: String?
    @Published var navigationRoute: String?
    
    // Wrapper du StateMachine KMP
    private let stateMachineWrapper: ObservableStateMachine<
        EventManagementContract.State,
        EventManagementContractIntent,
        EventManagementContractSideEffect
    >
    
    init() {
        // Création via la factory iOS
        let database = RepositoryProvider.shared.database
        self.stateMachineWrapper = IosFactory.shared.createEventStateMachine(database: database)
        
        // État initial
        self.state = self.stateMachineWrapper.currentState!
        
        // Observation des changements d'état
        self.stateMachineWrapper.onStateChange = { [weak self] newState in
            guard let self = self, let newState = newState else { return }
            DispatchQueue.main.async {
                self.state = newState
            }
        }
        
        // Observation des side effects
        self.stateMachineWrapper.onSideEffect = { [weak self] effect in
            guard let self = self, let effect = effect else { return }
            DispatchQueue.main.async {
                self.handleSideEffect(effect)
            }
        }
    }
    
    func dispatch(_ intent: EventManagementContractIntent) {
        stateMachineWrapper.dispatch(intent: intent)
    }
    
    private func handleSideEffect(_ effect: EventManagementContractSideEffect) {
        switch effect {
        case let navigate as EventManagementContractSideEffectNavigateTo:
            self.navigationRoute = navigate.route
        case let toast as EventManagementContractSideEffectShowToast:
            self.toastMessage = toast.message
        default:
            break
        }
    }
}
```

### SwiftUI Integration

```swift
// iOS (SwiftUI)
struct EventListView: View {
    @StateObject private var viewModel = EventListViewModel()
    
    var body: some View {
        NavigationStack {
            List(viewModel.state.events, id: \.id) { event in
                EventRow(event: event)
                    .onTapGesture {
                        viewModel.dispatch(
                            EventManagementContractIntentSelectEvent(eventId: event.id)
                        )
                    }
            }
            .navigationTitle("Événements")
            .toolbar {
                Button("Créer") {
                    viewModel.dispatch(
                        EventManagementContractIntentCreateEvent(...)
                    )
                }
            }
            // Navigation via side effect
            .navigationDestination(for: String.self) { route in
                if route.starts(with: "detail/") {
                    EventDetailView(eventId: String(route.dropFirst(7)))
                }
            }
            // Toast via side effect
            .alert("Message", isPresented: .constant(viewModel.toastMessage != nil)) {
                Button("OK") { viewModel.toastMessage = nil }
            } message: {
                Text(viewModel.toastMessage ?? "")
            }
        }
        .onAppear {
            viewModel.dispatch(EventManagementContractIntentLoadEvents())
        }
    }
}
```

---

## Avantages de cette Architecture

### 1. **Code Partagé (KMP)**
- ✅ Logique métier unique (StateMachines, Repositories)
- ✅ Tests partagés
- ✅ Pas de duplication de logique

### 2. **UI Native**
- ✅ Android: Jetpack Compose (Material You)
- ✅ iOS: SwiftUI (Liquid Glass)
- ✅ Chaque plateforme a sa meilleure UI

### 3. **State Management Consistent**
- ✅ Même pattern MVI sur les deux plateformes
- ✅ Même flux de données unidirectionnel
- ✅ Mêmes règles métier appliquées

### 4. **Testabilité**
- ✅ StateMachines testables unitairement
- ✅ UI testable séparément
- ✅ Mocks faciles grâce aux interfaces

---

## Fichiers Clés par Plateforme

### Android
```
composeApp/src/
├── commonMain/kotlin/
│   └── viewmodel/
│       ├── EventManagementViewModel.kt    # Wrapper StateMachine
│       ├── AuthViewModel.kt               # Wrapper Auth
│       ├── ScenarioManagementViewModel.kt
│       └── MeetingManagementViewModel.kt
├── androidMain/kotlin/
│   ├── App.kt                             # Point d'entrée
│   ├── navigation/
│   │   └── WakevNavHost.kt                # Navigation Compose
│   └── ui/                                # Écrans Compose
└── androidMain/kotlin/
    └── notification/                      # Implémentation notifications
```

### iOS
```
iosApp/src/
├── ViewModels/
│   ├── EventListViewModel.swift          # Wrapper StateMachine
│   ├── EventDetailViewModel.swift
│   ├── ProfileViewModel.swift
│   ├── ScenarioListViewModel.swift
│   └── MeetingListViewModel.swift
├── Navigation/
│   └── AppNavigation.swift               # Navigation SwiftUI
├── Views/                                # Écrans SwiftUI
├── Services/
│   └── DeepLinkService.swift             # Deep linking
└── iOSApp.swift                          # Point d'entrée
```

### Shared (KMP)
```
shared/src/commonMain/kotlin/
├── presentation/
│   ├── statemachine/
│   │   ├── EventManagementStateMachine.kt   # Logique événements
│   │   ├── AuthStateMachine.kt              # Logique auth
│   │   ├── ScenarioManagementStateMachine.kt
│   │   └── MeetingServiceStateMachine.kt
│   └── state/
│       ├── EventManagementContract.kt       # State/Intent/SideEffect
│       └── AuthContract.kt
├── repository/
│   └── EventRepository.kt                   # Data layer
└── app/
    └── AppState.kt                          # État global
```

---

## Conclusion

Cette architecture assure:
1. **Cohérence**: Même logique métier sur les deux plateformes
2. **Maintenabilité**: Code métier centralisé dans le shared module
3. **Performance**: UI native réactive sur chaque plateforme
4. **Testabilité**: Tests partagés pour la logique, tests natifs pour l'UI
5. **Évolutivité**: Ajout de nouvelles features simple et consistant

Les deux plateformes utilisent **exactement la même StateMachine**, garantissant que les règles métier sont identiques sur Android et iOS.
