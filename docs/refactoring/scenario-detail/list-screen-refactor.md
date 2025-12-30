# ScenarioListScreen Refactoring Guide

## Vue d'ensemble

Le fichier `ScenarioListScreen.kt` a été refactorisé pour utiliser le **ViewModel avec StateFlow** suivant le pattern **State Machine (MVI/FSM)**.

### Ancien Pattern (Avant)
```kotlin
// ❌ Ancien pattern
@Composable
fun ScenarioListScreen(
    event: Event,
    repository: ScenarioRepository,  // Repository injecté
    participantId: String,
    onScenarioClick: (String) -> Unit,
    onCreateScenario: () -> Unit,
    onCompareScenarios: () -> Unit
) {
    var state by remember { mutableStateOf(ScenarioListState(...)) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(event.id) {
        // Appels directs au repository
        val scenarios = repository.getScenariosWithVotes(event.id)
        state = state.copy(scenarios = scenarios)
    }
    
    // Logique métier en Composable
    Button(onClick = {
        scope.launch {
            val vote = ScenarioVote(...)
            repository.addVote(vote)
        }
    })
}
```

**Problèmes:**
- État local fragmenté dans le Composable
- Appels directs au repository dans les LaunchedEffect
- Logique métier mélangée avec la logique UI
- Difficile à tester
- Side effects non centralisés

### Nouveau Pattern (Après)
```kotlin
// ✅ Nouveau pattern avec ViewModel + StateFlow
@Composable
fun ScenarioListScreen(
    event: Event,
    viewModel: ScenarioManagementViewModel,  // ViewModel injecté
    onScenarioClick: (String) -> Unit,
    onCreateScenario: () -> Unit,
    onCompareScenarios: () -> Unit
) {
    val state by viewModel.state.collectAsState()  // Observe l'état
    
    LaunchedEffect(event.id) {
        viewModel.initialize(event.id, participantId)  // Dispatch une intention
    }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->     // Gère les side effects
            when (effect) {
                is ScenarioManagementContract.SideEffect.ShowError -> {
                    // Gère l'erreur
                }
                // ...
            }
        }
    }
    
    // UI simple et réactive
    Button(onClick = {
        viewModel.voteScenario(scenarioId, voteType)  // Dispatch une intention
    })
}
```

**Avantages:**
- ✅ État centralisé dans le ViewModel (StateFlow)
- ✅ Logique métier dans le State Machine
- ✅ Composable léger et réactif
- ✅ Testable (logique séparée de la UI)
- ✅ Side effects centralisés et prévisibles

---

## Architecture Refactorisée

### 1. **State Management**

#### Avant
```kotlin
data class ScenarioListState(
    val eventId: String = "",
    val participantId: String = "",
    val scenarios: List<ScenarioWithVotes> = emptyList(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val errorMessage: String = "",
    val userVotes: Map<String, ScenarioVoteType> = emptyMap()
)

var state by remember { mutableStateOf(ScenarioListState(...)) }
state = state.copy(isLoading = true)  // Mutation manuelle
```

#### Après
```kotlin
// Le ViewModel expose la StateFlow
val state by viewModel.state.collectAsState()

// State vient du ScenarioManagementContract
data class State(
    val isLoading: Boolean = false,
    val eventId: String = "",
    val participantId: String = "",
    val scenarios: List<ScenarioWithVotes> = emptyList(),
    val votingResults: Map<String, ScenarioVotingResult> = emptyMap(),
    val selectedScenario: Scenario? = null,
    val error: String? = null,
    val isComparing: Boolean = false,
    val comparison: ScenarioComparison? = null,
    // ... autres champs
)

// Mutations gérées par la State Machine via Intents
viewModel.voteScenario(scenarioId, voteType)  // Dispatch Intent
```

### 2. **Intent Handling**

#### Avant
```kotlin
// Appels directs au repository
scope.launch {
    try {
        val vote = ScenarioVote(...)
        val result = repository.addVote(vote)
        state = state.copy(userVotes = state.userVotes + ...)
    } catch (e: Exception) {
        state = state.copy(isError = true, errorMessage = e.message)
    }
}
```

#### Après
```kotlin
// Dispatch une intention via le ViewModel
viewModel.voteScenario(scenarioId, voteType)

// La State Machine gère la logique
class ScenarioManagementStateMachine : StateMachine<State, Intent, SideEffect>() {
    override suspend fun handleIntent(intent: Intent) {
        when (intent) {
            is Intent.VoteScenario -> {
                // 1. Valide l'état actuel
                // 2. Appelle le use case
                // 3. Met à jour l'état
                // 4. Émet les side effects
            }
        }
    }
}
```

### 3. **Side Effects**

#### Avant
```kotlin
// Mélangé avec la logique métier
scope.launch {
    try {
        repository.addVote(vote)
        // Pas de notification d'erreur centralisée
    } catch (e: Exception) {
        // Gestion d'erreur locale
    }
}
```

#### Après
```kotlin
// Side effects centralisés
LaunchedEffect(Unit) {
    viewModel.sideEffect.collect { effect ->
        when (effect) {
            is SideEffect.ShowError -> {
                // Affiche l'erreur via Snackbar
            }
            is SideEffect.ShowToast -> {
                // Affiche un toast
            }
            is SideEffect.NavigateTo -> {
                // Navigation
            }
            is SideEffect.NavigateBack -> {
                // Back navigation
            }
        }
    }
}
```

---

## Fichiers Impactés et Modifications

### `ScenarioListScreen.kt` (REFACTORISÉ)

#### Signature de la Fonction
```kotlin
@Composable
fun ScenarioListScreen(
    event: Event,
    viewModel: ScenarioManagementViewModel,  // ✨ Changement clé
    onScenarioClick: (String) -> Unit,
    onCreateScenario: () -> Unit,
    onCompareScenarios: () -> Unit
)
```

#### Initialisation
```kotlin
// ✅ Charge les scénarios via le ViewModel
LaunchedEffect(event.id) {
    viewModel.initialize(event.id, "participant_id")
}
```

#### Observation d'État
```kotlin
// ✅ Utilise collectAsState() au lieu de mutableStateOf()
val state by viewModel.state.collectAsState()
```

#### Gestion des Votes
```kotlin
// ✅ Dispatch Intent au lieu d'appel direct au repository
ScenarioCard(
    scenarioWithVotes = scenarioWithVotes,
    userVote = userVote,
    onVote = { voteType ->
        viewModel.voteScenario(scenarioWithVotes.scenario.id, voteType)
    },
    onClick = { ... }
)
```

#### Gestion des Erreurs
```kotlin
// ✅ Utilise state.error au lieu de state.isError + state.errorMessage
state.error?.let { errorMessage ->
    Card(...) {
        Text(errorMessage)
        Button(onClick = { viewModel.clearError() })
    }
}
```

#### Gestion de la Comparaison
```kotlin
// ✅ Utilise le ViewModel pour dispatcher l'intention
Button(
    onClick = {
        viewModel.compareScenarios(state.scenarios.map { it.scenario.id })
        onCompareScenarios()
    }
)
```

---

## Comment Intégrer le Refactoring

### 1. **Injection du ViewModel**

#### Option A: Via Koin (Recommandé)
```kotlin
// Dans votre configuration Koin
val appModule = module {
    single { ScenarioManagementStateMachine(get(), get()) }
    viewModel { ScenarioManagementViewModel(get()) }
}

// Dans le Composable
@Composable
fun MyScreen(
    viewModel: ScenarioManagementViewModel = koinViewModel()
) {
    ScenarioListScreen(
        event = event,
        viewModel = viewModel,
        onScenarioClick = { ... },
        onCreateScenario = { ... },
        onCompareScenarios = { ... }
    )
}
```

#### Option B: Injection Manuelle
```kotlin
@Composable
fun MyScreen(
    viewModel: ScenarioManagementViewModel
) {
    ScenarioListScreen(
        event = event,
        viewModel = viewModel,
        onScenarioClick = { ... },
        onCreateScenario = { ... },
        onCompareScenarios = { ... }
    )
}
```

### 2. **Mise à Jour des Appels**

#### Avant
```kotlin
ScenarioListScreen(
    event = event,
    repository = scenarioRepository,  // ❌ Ancien
    participantId = participantId,
    onScenarioClick = { ... },
    onCreateScenario = { ... },
    onCompareScenarios = { ... }
)
```

#### Après
```kotlin
ScenarioListScreen(
    event = event,
    viewModel = scenarioManagementViewModel,  // ✅ Nouveau
    onScenarioClick = { ... },
    onCreateScenario = { ... },
    onCompareScenarios: { ... }
)
```

### 3. **Gérer le Participant ID**

**Attention:** Le participant ID doit être obtenu du contexte actuel.

```kotlin
// Option 1: Via dependency injection
@Composable
fun MyScreen(
    currentUserId: String,
    viewModel: ScenarioManagementViewModel
) {
    LaunchedEffect(event.id) {
        viewModel.initialize(event.id, currentUserId)
    }
}

// Option 2: Via ViewModel property
// Définir un currentParticipantId dans le ViewModel
@Composable
fun MyScreen(
    viewModel: ScenarioManagementViewModel
) {
    LaunchedEffect(event.id) {
        viewModel.initialize(event.id, viewModel.currentParticipantId)
    }
}

// Option 3: Via AuthService
@Composable
fun MyScreen(
    authService: AuthService,
    viewModel: ScenarioManagementViewModel
) {
    LaunchedEffect(event.id) {
        viewModel.initialize(event.id, authService.getCurrentUserId())
    }
}
```

---

## Composants UI Inchangés

Tous les composants UI existants restent inchangés:
- ✅ `ScenarioCard`
- ✅ `StatusBadge`
- ✅ `InfoChip`
- ✅ `VotingResultsSection`
- ✅ `VoteBreakdownChip`
- ✅ `VotingButtons`
- ✅ `VoteButton`

Seule la **logique de gestion d'état** a changé.

---

## Testing

### Test du Composable

```kotlin
@Test
fun testScenarioListScreenLoadsScenarios() {
    val mockViewModel = mockk<ScenarioManagementViewModel> {
        coEvery { state } returns flowOf(ScenarioManagementContract.State(...))
        coEvery { sideEffect } returns emptyFlow()
        every { initialize(any(), any()) } just runs
    }
    
    composeRule.setContent {
        ScenarioListScreen(
            event = testEvent,
            viewModel = mockViewModel,
            onScenarioClick = {},
            onCreateScenario = {},
            onCompareScenarios = {}
        )
    }
    
    composeRule.onNodeWithText("Scenario Voting").assertExists()
    verify { mockViewModel.initialize(testEvent.id, any()) }
}
```

### Test du ViewModel

```kotlin
@Test
fun testVoteScenario() = runTest {
    val viewModel = ScenarioManagementViewModel(mockStateMachine)
    
    viewModel.voteScenario("scenario-1", ScenarioVoteType.PREFER)
    
    verify {
        mockStateMachine.dispatch(
            ScenarioManagementContract.Intent.VoteScenario("scenario-1", ScenarioVoteType.PREFER)
        )
    }
}
```

---

## Checklist de Migration

- [ ] ViewModel implémenté (`ScenarioManagementViewModel.kt`)
- [ ] State Machine implémenté (`ScenarioManagementStateMachine.kt`)
- [ ] Contract défini (`ScenarioManagementContract.kt`)
- [ ] `ScenarioListScreen.kt` refactorisé
- [ ] Injection du ViewModel configurée (Koin)
- [ ] Appels du Composable mis à jour
- [ ] Participant ID correctement obtenu du contexte
- [ ] Tests du Composable ajoutés
- [ ] Tests du ViewModel ajoutés
- [ ] Tests d'intégration réussis
- [ ] Documentation mise à jour

---

## Ressources

- **ViewModel**: `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/viewmodel/ScenarioManagementViewModel.kt`
- **State Machine**: `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/ScenarioManagementStateMachine.kt`
- **Contract**: `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state/ScenarioManagementContract.kt`
- **Refactored Screen**: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ScenarioListScreen.kt`

---

**Note**: Ce refactoring s'aligne avec l'architecture State Machine (MVI/FSM) utilisée par le projet Wakeve pour tous les écrans Compose.
