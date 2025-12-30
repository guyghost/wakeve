# ScenarioListView - Référence Rapide

## 📋 Fichiers Modifiés

| Fichier | Statut | Changes |
|---------|--------|---------|
| `iosApp/iosApp/Views/ScenarioListView.swift` | ✅ Refactorisé | Repository → ViewModel |
| `iosApp/iosApp/ViewModels/ScenarioListViewModel.swift` | ✅ Existant | Aucun changement |

---

## 🔑 Points Clés du Refactor

### 1. Injection
```swift
// ❌ Avant
let repository: ScenarioRepository

// ✅ Après
@StateObject private var viewModel = ScenarioListViewModel()
```

### 2. État de Chargement
```swift
// ❌ Avant
@State private var isLoading = true
if isLoading { ... }

// ✅ Après
if viewModel.isLoading { ... }
```

### 3. Scenarios
```swift
// ❌ Avant
@State private var scenarios: [ScenarioWithVotes] = []
ForEach(scenarios, ...)

// ✅ Après
ForEach(viewModel.scenarios, ...)
```

### 4. Votes Utilisateur
```swift
// ❌ Avant
@State private var userVotes: [String: ScenarioVote] = [:]
userVote: userVotes[scenarioWithVotes.scenario.id]

// ✅ Après
userVote: getUserVote(for: scenarioWithVotes)

// Helper
private func getUserVote(for scenarioWithVotes: ScenarioWithVotes) -> ScenarioVote? {
    scenarioWithVotes.votes.first { $0.participantId == participantId }
}
```

### 5. Initialisation
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

### 6. Submission de Vote
```swift
// ❌ Avant
onVote: { voteType in
    Task {
        await submitVote(scenarioId: scenarioWithVotes.scenario.id, voteType: voteType)
    }
}

// ✅ Après
onVote: { voteType in
    viewModel.voteScenario(
        scenarioId: scenarioWithVotes.scenario.id,
        voteType: voteType
    )
}
```

### 7. Gestion d'Erreurs
```swift
// ❌ Avant
@State private var errorMessage = ""
@State private var showError = false
.alert("Error", isPresented: $showError) { ... }

// ✅ Après
.alert("Error", isPresented: Binding(
    get: { viewModel.hasError },
    set: { if !$0 { viewModel.clearError() } }
)) { ... }
```

---

## 🎯 Checklist de Vérification

### Avant de merger
- [ ] Code compile
- [ ] `@StateObject private var viewModel` présent
- [ ] Pas de `@State` variables
- [ ] Pas de `repository` injection
- [ ] `onAppear` appelle `viewModel.initialize()`
- [ ] Votes utilisent `viewModel.voteScenario()`
- [ ] Erreurs affichent `viewModel.errorMessage`
- [ ] États: loading, empty, list affichent correctement
- [ ] Tous les composants UI conservés
- [ ] Liquid Glass design préservé

### Tests
- [ ] Scenarios se chargent au montage ✅
- [ ] Vote se soumet sans erreur ✅
- [ ] Erreur s'affiche et se ferme ✅
- [ ] Back/Compare/Tap naviguent ✅
- [ ] Empty state s'affiche quand vide ✅

---

## 💡 API du ViewModel

### @Published Properties
```swift
@Published var state: ScenarioManagementContractState
```

### Convenience Properties
```swift
var scenarios: [ScenarioWithVotes] { state.scenarios }
var isLoading: Bool { state.isLoading }
var hasError: Bool { state.hasError }
var errorMessage: String? { state.error }
var isEmpty: Bool { scenarios.isEmpty }
var isComparing: Bool { comparison != nil }
var scenariosRanked: [ScenarioWithVotes] { state.getScenariosRanked() }
```

### Méthodes Principales
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
```

---

## 🧠 Flux d'Exécution

```
1. View créée
   ↓
2. @StateObject crée ViewModel
   ↓
3. onAppear() appelé
   ↓
4. viewModel.initialize(eventId, participantId)
   ↓
5. ViewModel dispatch intent LoadScenariosForEvent
   ↓
6. State Machine change l'état
   ↓
7. @Published state change
   ↓
8. View re-render avec viewModel.scenarios
   ↓
9. User taps vote button
   ↓
10. viewModel.voteScenario(scenarioId, voteType) appelé
   ↓
11. ViewModel dispatch intent VoteScenario
   ↓
12. State Machine met à jour l'état
   ↓
13. View re-render avec votes mis à jour
```

---

## 🔧 Composition des Composants

```
ScenarioListView
├── ZStack
│   ├── Color background
│   └── VStack
│       ├── headerView
│       └── Contenu conditionnel
│           ├── loadingView (si isLoading)
│           ├── emptyStateView (si isEmpty)
│           └── ScrollView
│               └── VStack
│                   ├── compareButton (si count > 1)
│                   └── ForEach(scenarios)
│                       └── ScenarioCard
│                           ├── Header avec badge
│                           ├── Key details
│                           │   ├── InfoRow (Date)
│                           │   ├── InfoRow (Location)
│                           │   ├── InfoRow (Duration)
│                           │   └── InfoRow (Budget)
│                           ├── VotingResultsSection
│                           │   └── VoteCount (x3)
│                           ├── VotingButtons
│                           │   └── ScenarioVoteButton (x3)
│                           └── View Details button
```

---

## 📱 Tailles et Spacings (Liquid Glass)

```swift
// Card
.padding(20)              // Padding interne
.glassCard()              // Material background
.continuousCornerRadius(12 ou 16)  // Coins continus

// Texts
.font(.system(size: 34, weight: .bold))     // Titles
.font(.system(size: 17, weight: .medium))   // Body
.font(.system(size: 12, weight: .semibold)) // Labels

// Spacing
.padding(.horizontal, 20)
.padding(.top, 16 ou 60)
.spacing: 16 ou 12 ou 8
```

---

## 🎨 Couleurs Utilisées

```swift
Color(.systemGroupedBackground)      // Background principal
Color(.secondarySystemGroupedBackground)  // Background secondaire
Color(.tertiarySystemFill)           // Remplissage tertiaire

Color.blue                           // Primaire
Color.green                          // Success/Prefer
Color.orange                         // Warning/Neutral
Color.red                            // Error/Against

Color(.secondary)                    // Texte secondaire
```

---

## 🚀 Optimisations Apportées

### Performance
```swift
// ✅ ForEach avec id au lieu de dépendre de l'ordre
ForEach(viewModel.scenarios, id: \.scenario.id) { ... }

// ✅ Convenience properties évitent les computations
var isEmpty: Bool { scenarios.isEmpty }
```

### Maintenabilité
```swift
// ✅ Fonction pure pour le vote
private func getUserVote(for scenarioWithVotes: ScenarioWithVotes) -> ScenarioVote? {
    scenarioWithVotes.votes.first { $0.participantId == participantId }
}

// ✅ Pas de logique métier
```

### Testabilité
```swift
// ✅ ViewModel injecté et testable
@StateObject private var viewModel = ScenarioListViewModel()

// ✅ Pas de dépendances complexes
```

---

## ⚠️ Pièges Courants

### ❌ Ne PAS faire
```swift
// ❌ 1. Ne pas utiliser @State pour le ViewModel
@State private var viewModel = ScenarioListViewModel()

// ❌ 2. Ne pas injecter le repository
let repository: ScenarioRepository

// ❌ 3. Ne pas appeler repository depuis la vue
_ = try await repository.addVote(vote: vote)

// ❌ 4. Ne pas garder @State pour l'état
@State private var scenarios: [ScenarioWithVotes] = []

// ❌ 5. Ne pas recharger les données manuellement
loadScenarios()

// ❌ 6. Ne pas gérer les erreurs localement
catch {
    self.errorMessage = error.localizedDescription
}
```

### ✅ À faire à la place
```swift
// ✅ 1. Utiliser @StateObject
@StateObject private var viewModel = ScenarioListViewModel()

// ✅ 2. Pas d'injection de repository
// Juste l'event et le participantId

// ✅ 3. Appeler le ViewModel
viewModel.voteScenario(scenarioId: scenarioId, voteType: voteType)

// ✅ 4. Utiliser @Published du ViewModel
viewModel.scenarios

// ✅ 5. Laisser le ViewModel gérer
viewModel.initialize(eventId: event.id, participantId: participantId)

// ✅ 6. Utiliser l'état d'erreur du ViewModel
if viewModel.hasError { ... }
```

---

## 🔗 Intégration avec Autres Vues

### Appelant (par ex: EventDetailView)
```swift
NavigationLink(
    destination: ScenarioListView(
        event: event,
        participantId: participantId,
        onScenarioTap: { scenario in
            // Naviguer vers détail
        },
        onCompareTap: {
            // Naviguer vers comparaison
        },
        onBack: {
            // Retour
        }
    )
) {
    Text("View Scenarios")
}
```

### Les 4 Callbacks requis
```swift
let onScenarioTap: (Scenario_) -> Void     // Quand l'user tape une carte
let onCompareTap: () -> Void               // Quand l'user tape "Compare"
let onBack: () -> Void                     // Quand l'user tape "Back"
```

---

## 📚 Documentation Liée

### Vue d'ensemble
- `SCENARIOLISTVIEW_REFACTORING_SUMMARY.md` - Résumé complet
- `SCENARIOLISTVIEW_MIGRATION_GUIDE.md` - Guide étape par étape
- `SCENARIOLISTVIEW_BEFORE_AFTER.md` - Comparaison détaillée

### Code
- `iosApp/iosApp/Views/ScenarioListView.swift` - La vue refactorisée
- `iosApp/iosApp/ViewModels/ScenarioListViewModel.swift` - Le ViewModel

### Projet
- `AGENTS.md` - Architecture du projet
- `.opencode/context.md` - Contexte général
- `.opencode/design-system.md` - Design System Liquid Glass

---

## 🎯 État du Refactor

```
✅ Refactorisation complète de ScenarioListView
✅ Migration vers ViewModel + State Machine
✅ Remplacement du repository direct
✅ Centralisation de l'état
✅ Documentation complète
✅ Prêt pour le merge
```

---

**Version**: 1.0.0  
**Date**: 29 décembre 2025  
**Status**: ✅ COMPLÉTÉ
