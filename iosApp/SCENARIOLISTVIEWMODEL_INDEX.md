# ScenarioListViewModel - Index et Navigation

📅 **Créé**: 29 décembre 2025
📍 **Status**: ✅ Complet et validé
🔗 **Git Commit**: 7f12f56

## 📍 Localisation des Fichiers

### ViewController Principale
```
iosApp/iosApp/ViewModels/ScenarioListViewModel.swift
```
- @MainActor class
- ObservableObject
- 364 lignes
- 10+ méthodes publiques
- State Machine pattern

### Documentation
```
iosApp/SCENARIO_VIEWMODEL_GUIDE.md
```
- Guide complet d'utilisation
- 506 lignes
- Exemples complets
- Architecture détaillée
- Testing guide

### Factory (Kotlin)
```
shared/src/iosMain/kotlin/com/guyghost/wakeve/di/IosFactory.kt
```
- `createScenarioStateMachine()` method
- Dependency injection
- Observable wrapper

## 🎯 Quick Start

### 1. Importer et Créer le ViewModel
```swift
import SwiftUI
import Shared

@StateObject private var viewModel = ScenarioListViewModel()
```

### 2. Initialiser avec EventId et ParticipantId
```swift
.onAppear {
    viewModel.initialize(
        eventId: "event-1",
        participantId: "participant-1"
    )
}
```

### 3. Afficher les Scénarios
```swift
List(viewModel.scenarios) { scenario in
    Text(scenario.scenario.name)
}
```

### 4. Dispatching d'Intents
```swift
// Voter
viewModel.voteScenario(
    scenarioId: "s1",
    voteType: .prefer
)

// Créer
viewModel.createScenario(
    name: "Beach Trip",
    eventId: "event-1",
    dateOrPeriod: "2025-12-20",
    location: "Maldives",
    duration: 3,
    estimatedParticipants: 8,
    estimatedBudgetPerPerson: 1500.0,
    description: "Fun beach vacation"
)

// Comparer
viewModel.compareScenarios(
    scenarioIds: ["s1", "s2", "s3"]
)
```

## 📚 Propriétés @Published

| Propriété | Type | Usage |
|-----------|------|-------|
| `state` | `ScenarioManagementContractState` | État principal |
| `toastMessage` | `String?` | Messages toast |
| `navigationRoute` | `String?` | Routes navigation |
| `shouldNavigateBack` | `Bool` | Retour précédent |

## 🎬 Convenience Properties

### État
```swift
viewModel.isLoading      // Bool - En chargement?
viewModel.hasError       // Bool - Erreur?
viewModel.errorMessage   // String? - Message erreur
```

### Données
```swift
viewModel.scenarios      // [ScenarioWithVotes]
viewModel.selectedScenario // Scenario?
viewModel.votingResults  // [String: ScenarioVotingResult]
viewModel.comparison     // ScenarioComparison?
```

### Utiles
```swift
viewModel.isEmpty        // Bool - Liste vide?
viewModel.isComparing    // Bool - En mode comparaison?
viewModel.scenariosRanked // Triés par score
```

## 🛠️ Méthodes Publiques

### Chargement
```swift
func initialize(eventId: String, participantId: String)
func loadScenarios()
```

### Opérations CRUD
```swift
func createScenario(...) -> Void
func selectScenario(scenarioId: String)
func updateScenario(scenario: Scenario)
func deleteScenario(scenarioId: String)
```

### Voting
```swift
func voteScenario(scenarioId: String, voteType: ScenarioVoteType)
```

### Comparaison
```swift
func compareScenarios(scenarioIds: [String])
func clearComparison()
```

### Gestion d'Erreur
```swift
func clearError()
```

## 🎨 Exemples Complets

### ScenarioListView
Voir `iosApp/SCENARIO_VIEWMODEL_GUIDE.md` - Section "Exemple complet"

### ScenarioDetailView
Voir `iosApp/SCENARIO_VIEWMODEL_GUIDE.md` - Section "Détails d'un scénario"

### Voting Workflow
Voir `iosApp/SCENARIO_VIEWMODEL_GUIDE.md` - Section "Voting workflow"

### Comparaison
Voir `iosApp/SCENARIO_VIEWMODEL_GUIDE.md` - Section "Comparaison"

## 🔄 Side Effects Gérés

| Side Effect | Action |
|-------------|--------|
| `ShowToast` | Affiche message toast |
| `NavigateTo` | Navigation vers route |
| `NavigateBack` | Retour écran précédent |
| `ShowError` | Affiche erreur |
| `ShareScenario` | Partage scénario |

## 📋 Checklist d'Utilisation

- [ ] Importer `Shared` module
- [ ] Créer `@StateObject private var viewModel = ScenarioListViewModel()`
- [ ] Appeler `initialize(eventId:participantId:)` dans `.onAppear`
- [ ] Observer `@Published` properties pour UI updates
- [ ] Dispatcher intents via méthodes publiques
- [ ] Gérer side effects (navigation, toasts)
- [ ] Afficher states (`isLoading`, `hasError`)
- [ ] Implémenter error alerts
- [ ] Tester offline-first
- [ ] Valider threading (@MainActor)

## 🧪 Testing

Voir `iosApp/SCENARIO_VIEWMODEL_GUIDE.md` - Section "Testing"

Exemples:
- Test de chargement
- Test de voting
- Test de comparaison
- Test offline

## 📖 Ressources

### Fichiers du Projet
- `iosApp/iosApp/ViewModels/EventListViewModel.swift` - Référence (pattern identique)
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state/ScenarioManagementContract.kt` - Types Kotlin
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/ScenarioManagementStateMachine.kt` - Logique métier

### Documentation Externe
- [SwiftUI @Published](https://developer.apple.com/documentation/combine/published)
- [MainActor](https://developer.apple.com/documentation/swift/mainactor)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [State Machine Pattern](https://en.wikipedia.org/wiki/State_machine)

## 🆘 Troubleshooting

### ViewModel n'est pas créé
**Problème**: `Cannot find 'ScenarioListViewModel' in scope`
**Solution**: Vérifier que `import Shared` est présent

### État n'est pas mis à jour
**Problème**: UI ne réagit pas aux changements
**Solution**: 
- Vérifier que ViewModel est @StateObject
- Vérifier que properties sont @Published
- Vérifier que intents sont dispatchés correctement

### Navigation ne marche pas
**Problème**: `navigationRoute` ne déclenche pas navigation
**Solution**:
- Implémenter `.onChange(of: viewModel.navigationRoute)`
- Parser la route et naviguer en conséquence
- Réinitialiser `navigationRoute = nil` après utilisation

### Erreurs de thread
**Problème**: `Publishing changes from background thread`
**Solution**:
- ViewModel est @MainActor - ne devrait pas arriver
- Vérifier que state machine dispatch est sur Main thread

## 🚀 Prochaines Étapes

1. **Créer les vues**
   - ScenarioListView.swift
   - ScenarioDetailView.swift
   - ScenarioComparisonView.swift

2. **Créer les composants**
   - ScenarioRow.swift
   - ScenarioVotingButtons.swift

3. **Ajouter tests**
   - ScenarioListViewModelTests.swift
   - Tests de voting
   - Tests offline

4. **Intégrer dans l'app**
   - Ajouter à ContentView
   - Wiring avec EventListViewModel
   - Deep linking

## 📞 Support

Pour des questions:
- Voir `iosApp/SCENARIO_VIEWMODEL_GUIDE.md`
- Étudier `EventListViewModel.swift`
- Consulter `ScenarioManagementContract.kt`
- Référencer les exemples fournis

---

**Status**: ✅ COMPLET ET PRÊT
**Commit**: 7f12f56
**Date**: 2025-12-29
**Branch**: main
