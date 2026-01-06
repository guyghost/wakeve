# ScenarioListViewModel - Guide d'Utilisation iOS

## Vue d'ensemble

`ScenarioListViewModel` expose la logique métier de gestion des scénarios (State Machine Kotlin/Multiplatform) à SwiftUI via des `@Published` properties.

## Fichier

```
iosApp/iosApp/ViewModels/ScenarioListViewModel.swift
```

## Architecture

Le ViewModel utilise le pattern **State Machine** (MVI/FSM):

```
┌─────────────┐
│   Intent    │  (Utilisateur clique, vote, etc.)
│ (action)    │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│ StateMachine    │  (Logique métier Kotlin)
│ (IosFactory)    │
└──────┬──────────┘
       │
       ▼
┌─────────────┐
│   State     │  (@Published)
│  (reactive) │
└─────────────┘
```

## Initialisation

### Création du ViewModel

```swift
@StateObject private var viewModel = ScenarioListViewModel()
```

Le ViewModel:
1. Crée une State Machine via `IosFactory().createScenarioStateMachine(database:)`
2. Observe les changements d'état
3. Observe les side effects (navigation, toasts)
4. Expose des méthodes pour dispatcher des intents

### Chargement initial des scénarios

```swift
.onAppear {
    viewModel.initialize(eventId: "event-1", participantId: "participant-1")
}
```

## État (@Published)

### State Principal

```swift
@Published var state: ScenarioManagementContractState
```

Contient:
- `scenarios: [ScenarioWithVotes]` - Liste des scénarios avec votes
- `selectedScenario: Scenario?` - Scénario sélectionné
- `votingResults: [String: ScenarioVotingResult]` - Résultats du voting
- `comparison: ScenarioComparison?` - Données de comparaison
- `isLoading: Bool` - Lors du chargement
- `error: String?` - Message d'erreur

### Propriétés de commodité

```swift
var scenarios: [ScenarioWithVotes]              // Liste des scénarios
var selectedScenario: Scenario?                 // Scénario courant
var isLoading: Bool                             // État de chargement
var hasError: Bool                              // Y-a-t-il une erreur?
var errorMessage: String?                       // Message d'erreur
var isEmpty: Bool                               // Liste vide?
var isComparing: Bool                           // Mode comparaison?
var scenariosRanked: [ScenarioWithVotes]       // Triés par score
```

### Side Effects

```swift
@Published var toastMessage: String?            // Message toast
@Published var navigationRoute: String?         // Route de navigation
@Published var shouldNavigateBack = false       // Revenir en arrière?
```

## Actions - Dispatcher des Intents

### Charger des scénarios

```swift
// Charger pour un événement spécifique
viewModel.initialize(eventId: "event-1", participantId: "participant-1")

// Charger tous les scénarios
viewModel.loadScenarios()
```

### Créer un scénario

```swift
viewModel.createScenario(
    name: "Beach Weekend",
    eventId: "event-1",
    dateOrPeriod: "2025-12-20 to 2025-12-22",
    location: "Maldives",
    duration: 3,
    estimatedParticipants: 8,
    estimatedBudgetPerPerson: 1500.0,
    description: "Relaxing beach vacation"
)
```

### Sélectionner un scénario

```swift
viewModel.selectScenario(scenarioId: "scenario-123")
```

Cela charge les détails du scénario et déclenche une navigation vers la vue de détail.

### Voter sur un scénario

```swift
viewModel.voteScenario(
    scenarioId: "scenario-123",
    voteType: ScenarioVoteType.prefer  // ou .neutral, .against
)
```

Votes possibles:
- `.prefer` - J'aime ce scénario (score +2)
- `.neutral` - Ça me convient (score +1)
- `.against` - Je ne veux pas de ce scénario (score -1)

### Mettre à jour un scénario

```swift
var updated = scenario
updated.name = "New Name"
updated.location = "New Location"
viewModel.updateScenario(scenario: updated)
```

### Supprimer un scénario

```swift
viewModel.deleteScenario(scenarioId: "scenario-123")
```

### Comparer des scénarios

```swift
viewModel.compareScenarios(scenarioIds: ["scenario-1", "scenario-2", "scenario-3"])
```

Charge les données de comparaison et déclenche la navigation.

### Annuler la comparaison

```swift
viewModel.clearComparison()
```

### Effacer les erreurs

```swift
viewModel.clearError()
```

## Exemple complet - Liste de scénarios

```swift
struct ScenarioListView: View {
    @StateObject private var viewModel = ScenarioListViewModel()
    @State private var showCreateSheet = false
    
    var body: some View {
        NavigationView {
            ZStack {
                // Liste
                if viewModel.isLoading {
                    ProgressView()
                } else if viewModel.isEmpty {
                    VStack {
                        Image(systemName: "scroll")
                            .font(.system(size: 48))
                        Text("No Scenarios")
                            .font(.headline)
                        Text("Create one to start planning")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                } else {
                    List {
                        ForEach(viewModel.scenariosRanked, id: \.scenario.id) { scenario in
                            NavigationLink(
                                destination: ScenarioDetailView(
                                    scenario: scenario.scenario,
                                    votingResult: scenario.votingResult,
                                    onVote: { voteType in
                                        viewModel.voteScenario(
                                            scenarioId: scenario.scenario.id,
                                            voteType: voteType
                                        )
                                    }
                                )
                            ) {
                                ScenarioRow(
                                    scenario: scenario.scenario,
                                    votingResult: scenario.votingResult
                                )
                            }
                        }
                        .onDelete { indices in
                            for index in indices {
                                let scenarioId = viewModel.scenariosRanked[index].scenario.id
                                viewModel.deleteScenario(scenarioId: scenarioId)
                            }
                        }
                    }
                }
                
                // Message toast
                if let message = viewModel.toastMessage {
                    VStack {
                        Spacer()
                        Text(message)
                            .font(.caption)
                            .padding()
                            .background(Color.black.opacity(0.8))
                            .foregroundColor(.white)
                            .cornerRadius(8)
                            .padding()
                    }
                    .transition(.opacity)
                }
                
                // Alerte d'erreur
                .alert("Error", isPresented: .constant(viewModel.hasError), actions: {
                    Button("OK") { viewModel.clearError() }
                }, message: {
                    if let error = viewModel.errorMessage {
                        Text(error)
                    }
                })
            }
            .navigationTitle("Scenarios")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showCreateSheet = true }) {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showCreateSheet) {
                CreateScenarioSheet(
                    eventId: "event-1",
                    onCreate: { name, location, duration, budget, description in
                        viewModel.createScenario(
                            name: name,
                            eventId: "event-1",
                            dateOrPeriod: Date().description,
                            location: location,
                            duration: Int32(duration),
                            estimatedParticipants: 8,
                            estimatedBudgetPerPerson: budget,
                            description: description
                        )
                        showCreateSheet = false
                    }
                )
            }
            .onAppear {
                viewModel.initialize(eventId: "event-1", participantId: "participant-1")
            }
        }
    }
}
```

## Exemple - Détails d'un scénario

```swift
struct ScenarioDetailView: View {
    let scenario: Scenario
    let votingResult: ScenarioVotingResult
    let onVote: (ScenarioVoteType) -> Void
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Titre
                Text(scenario.name)
                    .font(.headline)
                
                // Info générale
                HStack {
                    Image(systemName: "mappin.circle")
                    Text(scenario.location)
                    Spacer()
                }
                .font(.subheadline)
                
                HStack {
                    Image(systemName: "calendar")
                    Text(scenario.dateOrPeriod)
                    Spacer()
                }
                .font(.subheadline)
                
                Divider()
                
                // Résultat du voting
                VStack(alignment: .leading) {
                    Text("Voting Results")
                        .font(.headline)
                    
                    HStack {
                        Label("\(votingResult.preferCount) Prefer", systemImage: "hand.thumbsup")
                        Spacer()
                        Text("Score: \(votingResult.score)")
                            .font(.caption)
                            .padding(4)
                            .background(Color.green.opacity(0.2))
                            .cornerRadius(4)
                    }
                    
                    HStack {
                        Label("\(votingResult.neutralCount) Neutral", systemImage: "minus.circle")
                        Spacer()
                    }
                    
                    HStack {
                        Label("\(votingResult.againstCount) Against", systemImage: "hand.thumbsdown")
                        Spacer()
                    }
                }
                
                Divider()
                
                // Voting buttons
                Text("Your Vote")
                    .font(.headline)
                
                HStack(spacing: 12) {
                    Button(action: { onVote(.prefer) }) {
                        Label("Prefer", systemImage: "hand.thumbsup")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    
                    Button(action: { onVote(.neutral) }) {
                        Label("Neutral", systemImage: "minus.circle")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    
                    Button(action: { onVote(.against) }) {
                        Label("Against", systemImage: "hand.thumbsdown")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }
                
                Spacer()
            }
            .padding()
        }
        .navigationTitle("Scenario Details")
        .navigationBarTitleDisplayMode(.inline)
    }
}
```

## Gestion des Side Effects

### Toast Messages

```swift
if let message = viewModel.toastMessage {
    Text(message)
        .toast() // Custom modifier
}
```

### Navigation

```swift
.onChange(of: viewModel.navigationRoute) { route in
    if let route = route {
        navigateTo(route)
        viewModel.navigationRoute = nil
    }
}
```

### Retour à l'écran précédent

```swift
.onChange(of: viewModel.shouldNavigateBack) { shouldBack in
    if shouldBack {
        presentationMode.wrappedValue.dismiss()
        viewModel.shouldNavigateBack = false
    }
}
```

## Lifecycle et Cleanup

Le ViewModel gère automatiquement:
- ✅ Création de la State Machine avec le bon dispatcher (Main)
- ✅ Observation des changements d'état (avec @MainActor)
- ✅ Observation des side effects
- ✅ Cleanup de la State Machine en deinit

```swift
deinit {
    stateMachineWrapper.dispose()
}
```

## @MainActor - Thread Safety

Le ViewModel est annoté avec `@MainActor` pour garantir que tous les changements d'état sont appliqués sur le thread principal:

```swift
@MainActor
class ScenarioListViewModel: ObservableObject {
    @Published var state: ScenarioManagementContractState
    
    // Tous les changements sont garantis sur le Main thread
}
```

## Testing

Pour tester le ViewModel:

```swift
@MainActor
class ScenarioListViewModelTests: XCTestCase {
    var viewModel: ScenarioListViewModel!
    
    override func setUp() {
        super.setUp()
        viewModel = ScenarioListViewModel()
    }
    
    func testLoadScenarios() async {
        viewModel.initialize(eventId: "event-1", participantId: "participant-1")
        
        // Attendre que l'état se mette à jour
        try? await Task.sleep(seconds: 1)
        
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertGreater(viewModel.scenarios.count, 0)
    }
    
    func testVoteOnScenario() async {
        viewModel.initialize(eventId: "event-1", participantId: "participant-1")
        try? await Task.sleep(seconds: 0.5)
        
        let scenarioId = viewModel.scenarios.first?.scenario.id ?? ""
        viewModel.voteScenario(scenarioId: scenarioId, voteType: .prefer)
        
        // Vérifier que le vote a été enregistré
        try? await Task.sleep(seconds: 0.5)
        let result = viewModel.votingResults[scenarioId]
        XCTAssertGreater(result?.preferCount ?? 0, 0)
    }
}
```

## Points clés

- ✅ **@MainActor**: Thread-safe pour SwiftUI
- ✅ **@Published**: Reactive state pour l'UI
- ✅ **State Machine**: Logique métier centralisée
- ✅ **Side Effects**: Callbacks séparés pour navigation/toasts
- ✅ **Type-safe**: Intents et State typés
- ✅ **Offline-first**: Données locales via SQLite
- ✅ **Multiplatform**: Code partagé Kotlin

## Fichiers connexes

- `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/ScenarioManagementStateMachine.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state/ScenarioManagementContract.kt`
- `shared/src/iosMain/kotlin/com/guyghost/wakeve/di/IosFactory.kt`
- `iosApp/iosApp/Views/ScenarioListView.swift` (à créer)
- `iosApp/iosApp/Components/ScenarioRow.swift` (à créer)

## Ressources

- [State Machine Pattern](https://en.wikipedia.org/wiki/State_machine)
- [SwiftUI @Published](https://developer.apple.com/documentation/combine/published)
- [MainActor](https://developer.apple.com/documentation/swift/mainactor)
- [Kotlin Multiplatform Mobile](https://kotlinlang.org/docs/multiplatform-mobile-getting-started.html)
