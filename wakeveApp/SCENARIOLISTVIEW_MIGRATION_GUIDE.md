# ScenarioListView - Guide de Migration

## 🎯 Objectif

Migrer `ScenarioListView` de l'architecture **Repository Direct** à l'architecture **ViewModel + State Machine**.

---

## 📋 Avant & Après

### Architecture Ancienne (Repository Direct)

```swift
// ❌ Ancien pattern
struct ScenarioListView: View {
    let repository: ScenarioRepository
    
    @State private var scenarios: [ScenarioWithVotes] = []
    @State private var userVotes: [String: ScenarioVote] = [:]
    @State private var isLoading = true
    @State private var errorMessage = ""
    @State private var showError = false
    
    private func loadScenarios() {
        Task {
            let scenariosWithVotes = repository.getScenariosWithVotes(eventId: event.id)
            // Extraction manuelle des votes
            var votes: [String: ScenarioVote] = [:]
            for swv in scenariosWithVotes {
                if let userVote = swv.votes.first(where: { $0.participantId == participantId }) {
                    votes[swv.scenario.id] = userVote
                }
            }
            // ...
        }
    }
}
```

**Problèmes**:
- ❌ Repository injecté directement
- ❌ État fragmenté en plusieurs @State
- ❌ Logique métier mélangée à la vue
- ❌ Gestion d'erreur locale
- ❌ Pas de réutilisabilité

---

### Architecture Nouvelle (ViewModel + State Machine)

```swift
// ✅ Nouveau pattern
struct ScenarioListView: View {
    @StateObject private var viewModel = ScenarioListViewModel()
    
    var body: some View {
        VStack {
            if viewModel.isLoading {
                loadingView
            } else {
                List(viewModel.scenarios) { scenarioWithVotes in
                    ScenarioCard(
                        userVote: getUserVote(for: scenarioWithVotes),
                        onVote: { voteType in
                            viewModel.voteScenario(
                                scenarioId: scenarioWithVotes.scenario.id,
                                voteType: voteType
                            )
                        }
                    )
                }
            }
        }
        .onAppear {
            viewModel.initialize(eventId: event.id, participantId: participantId)
        }
    }
    
    private func getUserVote(for scenarioWithVotes: ScenarioWithVotes) -> ScenarioVote? {
        scenarioWithVotes.votes.first { $0.participantId == participantId }
    }
}
```

**Avantages**:
- ✅ ViewModel injecté via @StateObject
- ✅ État centralisé via @Published
- ✅ Logique métier dans le State Machine
- ✅ Erreurs gérées uniformément
- ✅ Réutilisable et testable

---

## 🔄 Étapes de Migration

### Étape 1: Préparation (Avant le refactor)

#### 1.1 Vérifier le ViewModel existe
```bash
ls -la iosApp/iosApp/ViewModels/ScenarioListViewModel.swift
```

#### 1.2 Vérifier les contrats Kotlin
```bash
# Vérifier les intents disponibles
grep -r "sealed interface.*Intent" shared/src/commonMain/kotlin/.../ScenarioManagementContract.kt
```

#### 1.3 Lire la documentation
- [x] `ScenarioListViewModel` - Comprendre les @Published properties
- [x] `ScenarioManagementContract` - Comprendre les States/Intents/SideEffects

---

### Étape 2: Refactorisation de la Vue

#### 2.1 Remplacer l'injection
```swift
// ❌ Avant
struct ScenarioListView: View {
    let repository: ScenarioRepository
    
// ✅ Après
struct ScenarioListView: View {
    // Pas de repository !
    @StateObject private var viewModel = ScenarioListViewModel()
```

#### 2.2 Remplacer @State par @Published
```swift
// ❌ Avant
@State private var scenarios: [ScenarioWithVotes] = []
@State private var isLoading = true

// ✅ Après
// Accès via viewModel.scenarios
// Accès via viewModel.isLoading
```

#### 2.3 Mettre à jour onAppear
```swift
// ❌ Avant
.onAppear {
    loadScenarios()
}

// ✅ Après
.onAppear {
    viewModel.initialize(eventId: event.id, participantId: participantId)
}
```

#### 2.4 Remplacer les appels au repository
```swift
// ❌ Avant
private func submitVote(scenarioId: String, voteType: ScenarioVoteType) async {
    let vote = ScenarioVote(...)
    _ = try await repository.addVote(vote: vote)
    loadScenarios()
}

// ✅ Après
viewModel.voteScenario(scenarioId: scenarioId, voteType: voteType)
```

#### 2.5 Mettre à jour la gestion d'erreur
```swift
// ❌ Avant
.alert("Error", isPresented: $showError) {
    Button("OK", role: .cancel) {}
} message: {
    Text(errorMessage)
}

// ✅ Après
.alert("Error", isPresented: Binding(
    get: { viewModel.hasError },
    set: { if !$0 { viewModel.clearError() } }
)) {
    Button("OK", role: .cancel) { viewModel.clearError() }
} message: {
    Text(viewModel.errorMessage ?? "An error occurred")
}
```

---

### Étape 3: Tester la Migration

#### 3.1 Tests unitaires du ViewModel
```swift
// Dans iosApp/iosApp/Tests/ViewModelTests.swift
@MainActor
class ScenarioListViewModelTests: XCTestCase {
    var viewModel: ScenarioListViewModel!
    
    override func setUp() {
        super.setUp()
        viewModel = ScenarioListViewModel()
    }
    
    func testInitialize_LoadsScenarios() {
        // Given
        let eventId = "event-1"
        let participantId = "user-1"
        
        // When
        viewModel.initialize(eventId: eventId, participantId: participantId)
        
        // Then
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertFalse(viewModel.scenarios.isEmpty)
    }
    
    func testVoteScenario_UpdatesState() {
        // Given
        let scenarioId = "scenario-1"
        let voteType = ScenarioVoteType.prefer
        
        // When
        viewModel.voteScenario(scenarioId: scenarioId, voteType: voteType)
        
        // Then
        let vote = viewModel.scenarios
            .first { $0.scenario.id == scenarioId }?
            .votes
            .first(where: { $0.vote == voteType })
        XCTAssertNotNil(vote)
    }
}
```

#### 3.2 Tests UI
```swift
// Dans iosApp/iosApp/Tests/ScenarioListViewTests.swift
@MainActor
class ScenarioListViewTests: XCTestCase {
    func testViewLoadsScenarios() {
        // Given
        let view = ScenarioListView(
            event: Event(...),
            participantId: "user-1",
            onScenarioTap: { _ in },
            onCompareTap: { },
            onBack: { }
        )
        
        // When
        // View se render avec viewModel.initialize()
        
        // Then
        // Vérifier que les scenarios s'affichent
    }
}
```

#### 3.3 Tests manuels
- [ ] Lancer l'app
- [ ] Ouvrir ScenarioListView
- [ ] Vérifier que les scenarios se chargent
- [ ] Cliquer sur un vote → vérifier la mise à jour
- [ ] Cliquer sur "Back" → retourner à la vue précédente
- [ ] Cliquer sur "Compare" → vérifier la navigation
- [ ] Forcer une erreur → vérifier l'alert

---

## 🔗 Connexions avec Autres Vues

### Appelants de ScenarioListView

#### 1. Depuis EventDetailView
```swift
NavigationLink(
    destination: ScenarioListView(
        event: event,
        participantId: participantId,
        onScenarioTap: { scenario in
            // Handle scenario selection
        },
        onCompareTap: {
            // Navigate to comparison
        },
        onBack: {
            // Pop back
        }
    )
) {
    Text("View Scenarios")
}
```

#### 2. Paramètres requis
```swift
let event: Event                           // L'événement courant
let participantId: String                  // ID du participant
let onScenarioTap: (Scenario_) -> Void    // Callback pour sélection
let onCompareTap: () -> Void              // Callback pour comparaison
let onBack: () -> Void                    // Callback pour retour
```

---

## 📊 État du ViewModel

### Properties @Published

```swift
// État actuel du State Machine
@Published var state: ScenarioManagementContractState

// Informations dérivées (convenience properties)
var scenarios: [ScenarioWithVotes] {
    state.scenarios
}

var isLoading: Bool {
    state.isLoading
}

var hasError: Bool {
    state.hasError
}

var errorMessage: String? {
    state.error
}

var isEmpty: Bool {
    scenarios.isEmpty
}
```

### Méthodes principales

```swift
// Initialisation
viewModel.initialize(eventId: String, participantId: String)

// Votes
viewModel.voteScenario(scenarioId: String, voteType: ScenarioVoteType)

// Comparaison
viewModel.compareScenarios(scenarioIds: [String])
viewModel.clearComparison()

// Erreurs
viewModel.clearError()

// Dispatch brut (avancé)
viewModel.dispatch(_ intent: ScenarioManagementContractIntent)
```

---

## 🧠 Flux de Données

```
┌──────────────────────────────────────┐
│        ScenarioListView (UI)         │
│  - Affiche viewModel.scenarios       │
│  - Appelle viewModel.voteScenario()  │
│  - Affiche viewModel.isLoading       │
│  - Affiche viewModel.hasError        │
└──────────────┬───────────────────────┘
               │ .onAppear
               │ viewModel.initialize()
               │ onVote: viewModel.voteScenario()
               ▼
┌──────────────────────────────────────┐
│   ScenarioListViewModel (@Published) │
│  - @Published var state              │
│  - dispatch() les intents            │
│  - Observe state changes             │
│  - Gère side effects                 │
└──────────────┬───────────────────────┘
               │ dispatch(intent)
               ▼
┌──────────────────────────────────────┐
│    State Machine (Kotlin)            │
│  - ScenarioManagementStateMachine    │
│  - Intents: LoadScenarios, Vote...   │
│  - Mutations d'état                  │
│  - Side effects: ShowError, Toast... │
└──────────────┬───────────────────────┘
               │
               ▼
┌──────────────────────────────────────┐
│  Repository & Database (Shared)      │
│  - Persistance SQLite                │
│  - Sync offline-first                │
└──────────────────────────────────────┘
```

---

## 🚨 Points Critiques

### 1. Initialisation via @StateObject
```swift
// ✅ Correct: Le ViewModel persiste tout au long du cycle de vie
@StateObject private var viewModel = ScenarioListViewModel()

// ❌ Incorrect: Le ViewModel est recrée à chaque render
@State private var viewModel = ScenarioListViewModel()

// ❌ Incorrect: Le ViewModel ne peut pas être observé
var viewModel = ScenarioListViewModel()
```

### 2. Appel de initialize() dans onAppear
```swift
// ✅ Correct: Initialisation au montage de la vue
.onAppear {
    viewModel.initialize(eventId: event.id, participantId: participantId)
}

// ❌ Incorrect: Initialisation non déclarée
// (Les données ne se chargeront jamais)
```

### 3. Utilisation des convenience properties
```swift
// ✅ Correct: Utiliser les convenience properties
if viewModel.isLoading { }
if viewModel.isEmpty { }
if viewModel.hasError { }

// ❌ Incorrect: Accéder directement à l'état
if viewModel.state.isLoading { }
```

### 4. Gestion du participantId
```swift
// ✅ Correct: Le participantId est passé à initialize()
viewModel.initialize(eventId: event.id, participantId: participantId)

// ❌ Incorrect: Créer une @State locale
@State private var participantId = ""  // ❌ À éviter
```

---

## 📱 Compatibilité iOS

```swift
// iOS 16+ requis (SwiftUI 4.0)
// @StateObject supporté dans SwiftUI 4.0+

// Utilisation de Binding pour l'alert
.alert(isPresented: Binding(get: {...}, set: {...}))

// Liquid Glass supporté
.glassCard()
```

---

## ✅ Checklist de Validation

### Avant le merge
- [ ] Code compile sans erreurs (sauf module 'Shared' warnings du build system)
- [ ] Toutes les @State supprimées
- [ ] @StateObject viewModel présent
- [ ] onAppear appelle viewModel.initialize()
- [ ] Tous les appels au repository supprimés
- [ ] Gestion d'erreur utilise viewModel.hasError
- [ ] Votes utilisent viewModel.voteScenario()
- [ ] Helper getUserVote() implanté
- [ ] Tous les composants UI conservés
- [ ] Liquid Glass design system préservé

### Tests
- [ ] Les scenarios se chargent au montage
- [ ] Les votes se soumettent sans erreur
- [ ] Les erreurs s'affichent correctement
- [ ] La navigation fonctionne (back, tap, compare)
- [ ] L'état offline est géré

### Code Review
- [ ] Pas de code mort laissé
- [ ] Commentaires à jour
- [ ] Nommage cohérent
- [ ] Pas de force-unwrap dangereuse

---

## 🔍 Débogage

### Observer les changements d'état
```swift
.onReceive(viewModel.$state) { newState in
    print("État mis à jour:", newState)
}
```

### Logger les intents dispatch
```swift
// Dans le ViewModel ou State Machine:
viewModel.dispatch(.voteScenario(scenarioId: id, vote: type))
// Vérifier les logs dans Xcode Console
```

### Tester offline-first
```swift
// Débrancher l'internet
// Vérifier que la vue affiche les données en cache
// Reconnecter et vérifier la sync
```

---

## 🎓 Apprentissage

### Concepts clés

1. **@StateObject vs @State**
   - @StateObject: Pour les ObservableObject (ViewModel)
   - @State: Pour les types simples (Int, String, etc.)

2. **@Published**
   - Propriétés publiées par ObservableObject
   - Automatiquement observées par SwiftUI
   - Déclenchent un re-render quand elles changent

3. **State Machine**
   - States: Représentent l'état de l'app
   - Intents: Actions déclenchées par l'utilisateur
   - Side Effects: Effets secondaires (toasts, navigation)

4. **Offline-First**
   - Les données proviennent toujours de la base locale
   - La sync en arrière-plan met à jour les données
   - Les mutations en attente sont reflétées immédiatement

---

## 📚 Ressources

### SwiftUI
- [Apple SwiftUI Documentation](https://developer.apple.com/xcode/swiftui/)
- [StateObject vs State](https://www.hackingwithswift.com/quick-start/swiftui/what-is-the-stateobject-property-wrapper)

### Architecture
- `AGENTS.md` - Architecture générale du projet
- `.opencode/context.md` - Contexte du projet
- `.opencode/design-system.md` - Design System

### Kotlin Multiplatform
- `shared/src/commonMain/kotlin/.../ScenarioManagementContract.kt`
- `shared/src/commonMain/kotlin/.../ScenarioManagementStateMachine.kt`

---

## ❓ FAQ

**Q: Pourquoi utiliser @StateObject et pas @State?**  
R: @StateObject est conçu pour les ObservableObject (comme les ViewModel). @State est pour les types simples.

**Q: Le ViewModel doit-il être @StateObject ou @Published?**  
R: Le ViewModel est un ObservableObject avec @Published properties. La vue utilise @StateObject pour l'injecter.

**Q: Comment gérer multiple Binding pour l'alert?**  
R: Utiliser `Binding(get:set:)` pour créer un binding personnalisé à partir des propriétés du ViewModel.

**Q: Comment passer le participantId au ViewModel?**  
R: Via `viewModel.initialize(eventId:participantId:)` dans `onAppear`.

**Q: Peut-on appeler le ViewModel depuis onAppear de ScenarioCard?**  
R: Non, utiliser le closure `onVote` passé en paramètre et gérer la logique dans ScenarioListView.

---

**Dernière mise à jour**: 29 décembre 2025  
**Version**: 1.0.0  
**Statut**: ✅ Complété et prêt pour le merge
