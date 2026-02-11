# Architecture Diagrams (Mermaid)

## Architecture Overview

```mermaid
graph TB
    subgraph "Shared (Kotlin Multiplatform)"
        SM[StateMachines<br/>EventManagementStateMachine<br/>AuthStateMachine<br/>ScenarioManagementStateMachine]
        CT[Contracts<br/>State / Intent / SideEffect]
        RP[Repositories<br/>EventRepository<br/>AuthRepository]
        DB[(SQLDelight<br/>SQLite Database)]
    end

    subgraph "Android (Jetpack Compose)"
        AV[ViewModels<br/>EventManagementViewModel<br/>AuthViewModel]
        AC[Compose Screens<br/>HomeScreen<br/>EventDetailScreen]
        AN[Navigation<br/>WakevNavHost]
    end

    subgraph "iOS (SwiftUI)"
        IV[ViewModels<br/>EventListViewModel<br/>ProfileViewModel]
        IS[SwiftUI Views<br/>EventListView<br/>EventDetailView]
        IN[Navigation<br/>AppNavigation]
    end

    SM -->|StateFlow| AV
    SM -->|@Published| IV
    AV -->|collectAsState| AC
    IV -->|@StateObject| IS
    AC -->|Intents| AV
    IS -->|dispatch| IV
    AV -->|dispatch| SM
    IV -->|dispatch| SM
    SM -->|Use Cases| RP
    RP -->|Queries| DB
```

## State Flow

```mermaid
sequenceDiagram
    participant User as User
    participant UI as UI Layer<br/>(Compose/SwiftUI)
    participant VM as ViewModel<br/>(Android/iOS)
    participant SM as StateMachine<br/>(Shared KMP)
    participant RP as Repository<br/>(Shared KMP)
    participant DB as Database<br/>(SQLite)

    User->>UI: Tap "Create Event"
    UI->>VM: dispatch(CreateEventIntent)
    VM->>SM: dispatch(intent)
    SM->>SM: Process Intent
    SM->>SM: Validate Logic
    SM->>RP: createEvent(event)
    RP->>DB: INSERT event
    DB-->>RP: Success
    RP-->>SM: Result
    SM->>SM: Update State
    SM-->>VM: Emit New State
    VM-->>UI: state.update()
    UI-->>User: Show Event Created
    
    alt Side Effect
        SM-->>VM: Emit SideEffect(NavigateTo)
        VM-->>UI: Handle Navigation
        UI-->>User: Navigate to Detail
    end
```

## Event State Workflow

```mermaid
stateDiagram-v2
    [*] --> IDLE
    IDLE --> DRAFT: Create Event
    
    DRAFT --> DRAFT: Add TimeSlots
    DRAFT --> DRAFT: Add Locations
    DRAFT --> POLLING: Start Poll
    
    POLLING --> POLLING: Vote (YES/MAYBE/NO)
    POLLING --> CONFIRMED: Confirm Date
    
    CONFIRMED --> SCENARIO_COMPARISON: Create Scenarios
    CONFIRMED --> ORGANIZING: Transition
    
    SCENARIO_COMPARISON --> SCENARIO_COMPARISON: Vote Scenario
    SCENARIO_COMPARISON --> ORGANIZING: Select Final
    
    ORGANIZING --> ORGANIZING: Create Meetings
    ORGANIZING --> ORGANIZING: Meal Planning
    ORGANIZING --> ORGANIZING: Equipment
    ORGANIZING --> FINALIZED: Mark Finalized
    
    FINALIZED --> [*]
```

## Component Architecture

```mermaid
graph TB
    subgraph "Presentation Layer"
        direction TB
        Android[Android UI<br/>Jetpack Compose<br/>Material You]
        iOS[iOS UI<br/>SwiftUI<br/>Liquid Glass]
    end

    subgraph "ViewModel Layer"
        direction TB
        AV[Android ViewModels<br/>EventManagementViewModel<br/>StateFlow]
        IV[iOS ViewModels<br/>EventListViewModel<br/>@Published]
    end

    subgraph "Business Logic Layer (Shared)"
        direction TB
        SM[StateMachines<br/>EventManagementStateMachine<br/>AuthStateMachine]
        UC[Use Cases<br/>LoadEventsUseCase<br/>CreateEventUseCase]
    end

    subgraph "Data Layer (Shared)"
        direction TB
        RP[Repositories<br/>EventRepository]
        DB[(SQLDelight Database)]
        API[API Client<br/>Ktor]
    end

    Android --> AV
    iOS --> IV
    AV --> SM
    IV --> SM
    SM --> UC
    UC --> RP
    RP --> DB
    RP --> API
```

## Auth Flow

```mermaid
sequenceDiagram
    participant User
    participant UI as Auth Screen
    participant VM as AuthViewModel
    participant SM as AuthStateMachine
    participant Auth as AuthService
    participant Store as SecureStorage

    User->>UI: Enter Credentials
    UI->>VM: dispatch(LoginIntent)
    VM->>SM: dispatch(intent)
    SM->>Auth: authenticate(email, password)
    Auth->>Auth: Validate
    Auth->>Store: Save Token
    Store-->>Auth: Success
    Auth-->>SM: User
    SM->>SM: Update State
    SM-->>VM: State Updated
    VM-->>UI: isAuthenticated = true
    
    alt Success
        SM-->>VM: SideEffect(NavigateToMain)
        VM-->>UI: Navigate to Home
    else Error
        SM-->>VM: SideEffect(ShowError)
        VM-->>UI: Show Error Message
    end
```

## Offline-First Sync

```mermaid
graph LR
    subgraph "Local"
        UI[UI Layer]
        VM[ViewModel]
        SM[StateMachine]
        RPL[LocalRepository]
        DB[(SQLite)]
    end

    subgraph "Sync"
        SQ[SyncQueue]
        SE[SyncEngine]
    end

    subgraph "Remote"
        API[API Server<br/>Ktor]
        SDB[(PostgreSQL)]
    end

    UI -->|1. Action| VM
    VM -->|2. Intent| SM
    SM -->|3. Save Local| RPL
    RPL -->|4. Persist| DB
    SM -->|5. Queue Sync| SQ
    SQ -->|6. Process| SE
    SE -->|7. HTTP| API
    API -->|8. Store| SDB
    SE -->|9. Confirm| RPL
```

## Notification Flow

```mermaid
sequenceDiagram
    participant Event as Event Trigger
    participant NS as NotificationService
    participant RNS as RichNotificationService
    participant Sched as NotificationScheduler
    participant FCM as FCM/APNs
    participant Device as User Device

    Event->>NS: Event Created
    NS->>NS: Get User Tokens
    NS->>RNS: Build Rich Notification
    RNS->>RNS: Add Image/Actions
    RNS->>Sched: Schedule/Dispatch
    
    alt Immediate
        Sched->>FCM: Send Now
    else Scheduled
        Sched->>Sched: Queue
        Sched->>FCM: Send at Time
    end
    
    FCM->>Device: Push Notification
    Device->>Device: Display
```

## Deep Link Handling

```mermaid
graph TB
    subgraph "Source"
        PN[Push Notification]
        WEB[Web Link]
        QR[QR Code]
    end

    subgraph "Platform"
        AD[Android<br/>DeepLinkHandler]
        ID[iOS<br/>DeepLinkService]
    end

    subgraph "Shared"
        DL[DeepLink Parser]
        DF[DeepLinkFactory]
        RT[Router]
    end

    subgraph "Navigation"
        EV[Event Detail]
        PL[Poll Screen]
        SC[Scenario Compare]
    end

    PN -->|wakeve://event/123| AD
    PN -->|wakeve://event/123| ID
    WEB --> AD
    WEB --> ID
    QR --> AD
    QR --> ID
    
    AD -->|Parse| DL
    ID -->|Parse| DL
    DL -->|Validate| DF
    DF -->|Route| RT
    RT -->|event/*| EV
    RT -->|poll/*| PL
    RT -->|scenario/*| SC
```

## Multi-Platform State Sharing

```mermaid
graph TB
    subgraph "Kotlin Multiplatform"
        direction TB
        SM[StateMachine]
        SF[StateFlow<State>]
        EF[Flow<SideEffect>]
        
        SM --> SF
        SM --> EF
    end

    subgraph "Android Bridge"
        direction TB
        AVM[ViewModel]
        ACS[collectAsState]
        AL[LaunchedEffect]
        
        AVM -->|wraps| SF
    end

    subgraph "iOS Bridge"
        direction TB
        IVM[ObservableObject]
        IOP[@Published]
        IOS[onStateChange]
        
        IVM -->|wraps| SF
    end

    subgraph "Android UI"
        AC[Compose<br/>@Composable]
    end

    subgraph "iOS UI"
        IS[SwiftUI<br/>View]
    end

    SF -->|StateFlow| AVM
    SF -->|ObservableStateMachine| IVM
    
    AVM -->|State| ACS
    ACS -->|state| AC
    
    IVM -->|@Published| IOP
    IOP -->|state| IS
    
    AC -->|Intent| AVM
    IS -->|Intent| IVM
    AVM -->|dispatch| SM
    IVM -->|dispatch| SM
```

## File Structure

```mermaid
graph TD
    subgraph "Project Root"
        SHARED[shared/]
        ANDROID[wakeveApp/]
        IOS[wakeveApp/wakeveApp/]
    end

    subgraph "Shared Module"
        SHARED --> COMMON[commonMain/]
        SHARED --> ANDROID_MAIN[androidMain/]
        SHARED --> IOS_MAIN[iosMain/]
        
        COMMON --> PRESENTATION[presentation/]
        COMMON --> REPOSITORIES[repository/]
        COMMON --> SERVICES[services/]
        
        PRESENTATION --> SM[statemachine/]
        PRESENTATION --> CONTRACTS[state/]
    end

    subgraph "Android App"
        ANDROID --> A_COMMON[commonMain/]
        ANDROID --> A_ANDROID[androidMain/]
        
        A_COMMON --> VIEWMODELS[viewmodel/]
        A_ANDROID --> UI[ui/]
        A_ANDROID --> NAV[navigation/]
    end

    subgraph "iOS App"
        IOS --> SWIFT_VIEWMODELS[ViewModels/]
        IOS --> VIEWS[Views/]
        IOS --> NAV_IOS[Navigation/]
        IOS --> COMPONENTS[Components/]
    end

    SM -->|Uses| CONTRACTS
    VIEWMODELS -->|Wraps| SM
    SWIFT_VIEWMODELS -->|Wraps| SM
    
    UI -->|Uses| VIEWMODELS
    VIEWS -->|Uses| SWIFT_VIEWMODELS
```

## Testing Architecture

```mermaid
graph TB
    subgraph "Test Layers"
        UNIT[Unit Tests]
        INTEGRATION[Integration Tests]
        E2E[End-to-End Tests]
    end

    subgraph "Shared Tests"
        UNIT --> SM_TEST[StateMachine Tests]
        UNIT --> UC_TEST[UseCase Tests]
        UNIT --> REPO_TEST[Repository Tests]
        
        INTEGRATION --> WORKFLOW[Workflow Tests]
        INTEGRATION --> SYNC[Sync Tests]
    end

    subgraph "Android Tests"
        UNIT --> VM_ANDROID[ViewModel Tests]
        INTEGRATION --> UI_ANDROID[UI Tests]
        E2E --> E2E_ANDROID[App Tests]
    end

    subgraph "iOS Tests"
        UNIT --> VM_IOS[ViewModel Tests]
        INTEGRATION --> UI_IOS[UI Tests]
        E2E --> E2E_IOS[App Tests]
    end

    SM_TEST -->|Tests| SM[StateMachines]
    UC_TEST -->|Tests| UC[Use Cases]
    REPO_TEST -->|Tests| RP[Repositories]
    
    WORKFLOW -->|Tests| FULL[Full Workflows]
    SYNC -->|Tests| OFFLINE[Offline Sync]
    
    VM_ANDROID -->|Tests| AV[Android ViewModels]
    UI_ANDROID -->|Tests| AC[Compose UI]
    
    VM_IOS -->|Tests| IV[iOS ViewModels]
    UI_IOS -->|Tests| IS[SwiftUI]
```

## Deployment Pipeline

```mermaid
graph LR
    DEV[Development]
    TEST[Test]
    BUILD[Build]
    DEPLOY[Deploy]

    subgraph "Development"
        DEV --> CODE[Code Changes]
        CODE --> UNIT[Unit Tests]
    end

    subgraph "Integration"
        UNIT -->|Pass| INT[Integration Tests]
        INT --> E2E[End-to-End Tests]
    end

    subgraph "Build"
        E2E -->|Pass| SHARED[Build Shared]
        SHARED --> A_BUILD[Build Android]
        SHARED --> I_BUILD[Build iOS]
    end

    subgraph "Deploy"
        A_BUILD --> A_DEPLOY[Deploy Android]
        I_BUILD --> I_DEPLOY[Deploy iOS]
    end

    A_DEPLOY --> PROD[Production]
    I_DEPLOY --> PROD
```

## Legend

| Symbol | Meaning |
|--------|---------|
| ⭕ | Component/Module |
| 📦 | Package/Group |
| 🔄 | Flow/Process |
| 📱 | Platform-specific |
| 🔗 | Shared/Common |

---

*Generated for Wakeve Architecture Documentation*
