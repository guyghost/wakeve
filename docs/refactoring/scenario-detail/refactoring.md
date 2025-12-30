# ScenarioDetailScreen Refactoring Summary

## 🎯 Objectif
Refactoriser `ScenarioDetailScreen.kt` pour utiliser le ViewModel avec StateFlow suivant le pattern MVI/FSM au lieu de gérer directement l'état local et les appels au repository.

## ✅ Changements Effectués

### 1. **Imports Mis à Jour**
- ✅ Ajout: `androidx.lifecycle.compose.collectAsStateWithLifecycle`
- ✅ Ajout: `com.guyghost.wakeve.presentation.state.ScenarioManagementContract`
- ✅ Ajout: `com.guyghost.wakeve.viewmodel.ScenarioManagementViewModel`
- ✅ Suppression: `com.guyghost.wakeve.ScenarioRepository` (plus d'appels directs)

### 2. **Refactorisation de l'État**

#### Ancien Pattern (State Local)
```kotlin
var state by remember { mutableStateOf(ScenarioDetailState()) }
```

#### Nouveau Pattern (StateFlow + Local UI State)
```kotlin
// State from ViewModel (persistent)
val vmState by viewModel.state.collectAsStateWithLifecycle()

// Local UI state (editing-only, ephemeral)
var uiState by remember { mutableStateOf(ScenarioDetailUIState()) }
```

**Rationale:**
- **vmState**: Gère l'état persistant (scénario chargé, erreur)
- **uiState**: Gère l'état éphémère de l'édition locale

### 3. **Signature de la Composable**

**Avant:**
```kotlin
fun ScenarioDetailScreen(
    scenarioId: String,
    repository: ScenarioRepository,        // ❌ Injection directe
    commentRepository: CommentRepository,
    isOrganizer: Boolean,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onNavigateToComments: (...)
)
```

**Après:**
```kotlin
fun ScenarioDetailScreen(
    scenarioId: String,
    viewModel: ScenarioManagementViewModel,  // ✅ ViewModel
    commentRepository: CommentRepository,
    isOrganizer: Boolean,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onNavigateToComments: (...)
)
```

### 4. **Chargement du Scénario**

**Avant:**
```kotlin
LaunchedEffect(scenarioId) {
    state = state.copy(isLoading = true, isError = false)
    try {
        val scenario = repository.getScenarioById(scenarioId)
        // ... update local state
    } catch (e: Exception) {
        // ... handle error
    }
}
```

**Après:**
```kotlin
// Load scenario when screen appears
LaunchedEffect(scenarioId) {
    viewModel.selectScenario(scenarioId)
}

// Update UI state when scenario is loaded
LaunchedEffect(vmState.selectedScenario) {
    vmState.selectedScenario?.let { scenario ->
        uiState = uiState.copy(
            editName = scenario.name,
            // ... init edit fields
        )
    }
}
```

### 5. **Gestion des Side Effects**

**Nouveau:**
```kotlin
LaunchedEffect(Unit) {
    viewModel.sideEffect.collect { effect ->
        when (effect) {
            is ScenarioManagementContract.SideEffect.NavigateBack -> {
                onBack()
            }
            is ScenarioManagementContract.SideEffect.ShowError -> {
                // Error shown from vmState.error
            }
            // ... handle other effects
        }
    }
}
```

### 6. **Actions de l'Utilisateur**

#### Update Scénario

**Avant:**
```kotlin
val result = repository.updateScenario(updated)
if (result.isSuccess) {
    state = state.copy(scenario = updated, isEditing = false)
} else {
    state = state.copy(isError = true, errorMessage = "...")
}
```

**Après:**
```kotlin
// Dispatch intent to ViewModel (non-blocking)
viewModel.updateScenario(updated)

// Update local UI state
uiState = uiState.copy(
    isEditing = false,
    isSaving = false
)

// ViewModel updates vmState.selectedScenario via state machine
// which triggers LaunchedEffect(vmState.selectedScenario) 
```

#### Delete Scénario

**Avant:**
```kotlin
val result = repository.deleteScenario(scenarioId)
if (result.isSuccess) {
    onDeleted()
} else {
    state = state.copy(isError = true, ...)
}
```

**Après:**
```kotlin
viewModel.deleteScenario(scenarioId)
uiState = uiState.copy(showDeleteDialog = false)

// ViewModel emits SideEffect.NavigateBack
// which is handled in LaunchedEffect -> calls onDeleted()
```

### 7. **Observation de l'État**

| Aspect | Avant | Après |
|--------|-------|-------|
| Scénario sélectionné | `state.scenario` | `vmState.selectedScenario` |
| Loading | `state.isLoading` | `vmState.isLoading` |
| Erreur | `state.isError`, `state.errorMessage` | `vmState.error` |
| Édition | `state.isEditing` | `uiState.isEditing` |
| Sauvegarde | `state.isSaving` | `uiState.isSaving` |
| Champs d'édition | `state.editName`, etc. | `uiState.editName`, etc. |

### 8. **Suppression de Code Mort**

```kotlin
// ❌ Supprimé: ScenarioDetailState (remplacé par viewModel state)
// ❌ Supprimé: state mutations directes (remplacées par dispatch)
// ❌ Supprimé: rememberCoroutineScope pour repository calls
```

## 🏗️ Architecture Pattern

### MVI (Model-View-Intent) / FSM (Finite State Machine)

```
┌──────────────────────────────────────┐
│         ScenarioDetailScreen         │
│  (UI Layer - Jetpack Compose)        │
└────────────────┬─────────────────────┘
                 │
         ┌───────▼────────┐
         │  collectAsState│
         │  (vmState)     │
         └───────┬────────┘
                 │
    ┌────────────▼─────────────┐
    │ ScenarioManagementViewModel
    │  - state: StateFlow      │
    │  - sideEffect: Flow      │
    │  - selectScenario()      │
    │  - updateScenario()      │
    │  - deleteScenario()      │
    └────────────┬─────────────┘
                 │
    ┌────────────▼──────────────────┐
    │ ScenarioManagementStateMachine│
    │ - dispatch(Intent)           │
    │ - emit(State)                │
    │ - emit(SideEffect)           │
    └────────────┬──────────────────┘
                 │
    ┌────────────▼──────────────────┐
    │   ScenarioRepository         │
    │   - getScenarioById()        │
    │   - updateScenario()         │
    │   - deleteScenario()         │
    └──────────────────────────────┘
```

## 🧪 Tests Requis

### Tests Unitaires (ViewModel)
```kotlin
@Test
fun selectScenario_loadsScenarioAndUpdatesState() {
    // Given: scenarioId = "scenario-1"
    // When: viewModel.selectScenario("scenario-1")
    // Then: vmState.selectedScenario != null
}

@Test
fun updateScenario_dispatchesIntentAndEmitsStateChange() {
    // Given: a scenario to update
    // When: viewModel.updateScenario(scenario)
    // Then: state is updated via state machine
}

@Test
fun deleteScenario_dispatchesIntentAndEmitsSideEffect() {
    // Given: a scenario to delete
    // When: viewModel.deleteScenario(scenarioId)
    // Then: NavigateBack side effect is emitted
}
```

### Tests d'Intégration (Compose)
```kotlin
@Test
fun scenarioDetailScreen_loadsAndDisplaysScenario() {
    // Given: ScenarioDetailScreen with mock ViewModel
    // When: screen is composed
    // Then: scenario details are displayed
}

@Test
fun scenarioDetailScreen_handlesEditingFlow() {
    // Given: screen in edit mode
    // When: user edits fields and saves
    // Then: ViewModel.updateScenario is called
}
```

## 📋 Checklist

- [x] Imports mis à jour
- [x] Signature changée (repository → viewModel)
- [x] État refactorisé (vmState + uiState)
- [x] LaunchedEffects pour selectScenario
- [x] LaunchedEffects pour side effects
- [x] Update scénario migrée vers ViewModel
- [x] Delete scénario migrée vers ViewModel
- [x] Tous les state.* remplacés par vmState.*
- [x] Tous les state mutations remplacés par uiState mutations
- [x] Commentaires et documentation mis à jour

## 🎨 UI - Pas de Changements Visuels

L'UI reste identique visuellement. Seule l'architecture interne a changé:
- ✅ Même layout et composants
- ✅ Même UX et interactions
- ✅ Même gestion des erreurs et loading states
- ✅ Même support de l'édition et suppression

## 🔗 Fichiers Affectés

| Fichier | Statut |
|---------|--------|
| `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ScenarioDetailScreen.kt` | ✅ Refactorisé |
| `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/viewmodel/ScenarioManagementViewModel.kt` | ✅ Existant (inchangé) |
| `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state/ScenarioManagementContract.kt` | ✅ Existant (inchangé) |

## 🚀 Prochaines Étapes

1. **Tests** - Créer tests unitaires et d'intégration
2. **Migration iOS** - Appliquer le même pattern à `iosApp/Views/ScenarioDetailView.swift`
3. **Documentation** - Mettre à jour guides de développement
4. **Review** - Demander revue de code (@review agent)

## 📚 Ressources

- [ViewModel et StateFlow](https://developer.android.com/topic/architecture/ui-layer/state-holders)
- [Jetpack Compose StateFlow](https://developer.android.com/jetpack/compose/state)
- [MVI Pattern](https://developer.android.com/jetpack/guide/navigation/safe-args)
- [ScenarioManagementViewModel](./composeApp/src/commonMain/kotlin/com/guyghost/wakeve/viewmodel/ScenarioManagementViewModel.kt)

---

**Refactoring Complet** ✅ | Date: 2025-12-29
