# ScenarioDetailScreen - Guide d'Utilisation Post-Refactoring

## 📋 Vue d'ensemble

Après le refactoring, `ScenarioDetailScreen` utilise maintenant le `ScenarioManagementViewModel` pour gérer l'état via StateFlow au lieu de gérer directement l'état local et les appels au repository.

## 🔧 Comment Utiliser

### Avant (Ancien Pattern - ❌ Obsolète)

```kotlin
// ❌ Ne plus utiliser cette approche
Navigation(
    route = "scenario-detail/{scenarioId}",
    arguments = listOf(navArgument("scenarioId") { type = NavType.StringType })
) { backStackEntry ->
    val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: return@Navigation
    
    ScenarioDetailScreen(
        scenarioId = scenarioId,
        repository = repository,  // ❌ Injection directe du repository
        commentRepository = commentRepository,
        isOrganizer = isOrganizer,
        onBack = { navController.popBackStack() },
        onDeleted = { navController.popBackStack() },
        onNavigateToComments = { eventId, section, sectionItemId ->
            navController.navigate("comments/$eventId/$section/$sectionItemId")
        }
    )
}
```

### Après (Nouveau Pattern - ✅ Correct)

```kotlin
// ✅ Utiliser cette approche
Navigation(
    route = "scenario-detail/{scenarioId}",
    arguments = listOf(navArgument("scenarioId") { type = NavType.StringType })
) { backStackEntry ->
    val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: return@Navigation
    
    // Obtenir le ViewModel via Koin (or your DI framework)
    val viewModel: ScenarioManagementViewModel = koinViewModel()
    
    ScenarioDetailScreen(
        scenarioId = scenarioId,
        viewModel = viewModel,  // ✅ Injection du ViewModel
        commentRepository = commentRepository,
        isOrganizer = isOrganizer,
        onBack = { navController.popBackStack() },
        onDeleted = { navController.popBackStack() },
        onNavigateToComments = { eventId, section, sectionItemId ->
            navController.navigate("comments/$eventId/$section/$sectionItemId")
        }
    )
}
```

## 📱 Exemple Complet (Jetpack Compose Navigation)

```kotlin
// Dans votre NavHost ou NavGraph
NavHost(navController = navController, startDestination = "scenario-list") {
    composable("scenario-list") {
        val viewModel: ScenarioManagementViewModel = koinViewModel()
        
        ScenarioListScreen(
            viewModel = viewModel,
            onNavigateToDetail = { scenarioId ->
                navController.navigate("scenario-detail/$scenarioId")
            }
        )
    }
    
    composable(
        route = "scenario-detail/{scenarioId}",
        arguments = listOf(navArgument("scenarioId") { type = NavType.StringType })
    ) { backStackEntry ->
        val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: return@composable
        val viewModel: ScenarioManagementViewModel = koinViewModel()
        val commentRepository = get<CommentRepository>()  // Koin
        
        ScenarioDetailScreen(
            scenarioId = scenarioId,
            viewModel = viewModel,
            commentRepository = commentRepository,
            isOrganizer = true,  // ou depuis viewModel state
            onBack = { navController.popBackStack() },
            onDeleted = { navController.popBackStack() },
            onNavigateToComments = { eventId, section, sectionItemId ->
                navController.navigate("comments/$eventId/$section/${sectionItemId ?: ""}")
            }
        )
    }
    
    composable("comments/{eventId}/{section}/{sectionItemId}") { backStackEntry ->
        val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
        val section = backStackEntry.arguments?.getString("section") ?: return@composable
        
        CommentsScreen(
            eventId = eventId,
            section = section,
            onBack = { navController.popBackStack() }
        )
    }
}
```

## 🎯 Points Clés

### 1. ViewModel Injection
**Toujours** obtenir le ViewModel via Koin (ou autre DI framework):

```kotlin
val viewModel: ScenarioManagementViewModel = koinViewModel()
```

**Ne pas** créer d'instance directement:

```kotlin
// ❌ Ne pas faire ceci
val viewModel = ScenarioManagementViewModel(stateMachine)
```

### 2. State Observation
L'écran observe automatiquement le state du ViewModel:

```kotlin
val vmState by viewModel.state.collectAsStateWithLifecycle()
```

Pas besoin de le faire manuellement en dehors de la composable.

### 3. Handling Side Effects
Les side effects sont gérés automatiquement dans la composable:

```kotlin
LaunchedEffect(Unit) {
    viewModel.sideEffect.collect { effect ->
        when (effect) {
            is ScenarioManagementContract.SideEffect.NavigateBack -> {
                onBack()  // Callback fourni
            }
            // ... autres side effects
        }
    }
}
```

### 4. User Actions
Toutes les actions utilisateur doivent passer par le ViewModel:

```kotlin
// ✅ Correct: dispatch intent via ViewModel
IconButton(onClick = { viewModel.deleteScenario(scenarioId) }) {
    Icon(Icons.Default.Delete, "Delete")
}

// ❌ Incorrect: appel direct au repository
IconButton(onClick = { repository.deleteScenario(scenarioId) }) {
    Icon(Icons.Default.Delete, "Delete")
}
```

## 🔄 Flux de Données

### Chargement du Scénario
1. Screen compose → `LaunchedEffect(scenarioId)`
2. `LaunchedEffect` → `viewModel.selectScenario(scenarioId)`
3. ViewModel → dispatch `Intent.SelectScenario(scenarioId)`
4. StateMachine → update state, emit `selectedScenario`
5. Screen → observe `vmState.selectedScenario` via `collectAsState`
6. Screen recompose avec les nouvelles données ✅

### Mise à Jour du Scénario
1. User → tap edit button → `uiState.isEditing = true`
2. User → edit fields → `uiState.editName = "..."`
3. User → tap save → `viewModel.updateScenario(updated)`
4. ViewModel → dispatch `Intent.UpdateScenario(updated)`
5. StateMachine → update repository & state
6. Screen → observe `vmState.selectedScenario` updated
7. `LaunchedEffect(vmState.selectedScenario)` → update `uiState`
8. Screen recompose avec les champs restaurés ✅

### Suppression du Scénario
1. User → tap delete → `uiState.showDeleteDialog = true`
2. User → confirm delete → `viewModel.deleteScenario(scenarioId)`
3. ViewModel → dispatch `Intent.DeleteScenario(scenarioId)`
4. StateMachine → delete from repository, emit `NavigateBack` side effect
5. `LaunchedEffect(sideEffect)` → `onBack()` callback
6. Navigation → pop back stack ✅

## 📊 State vs UIState

### vmState (ViewModel State - Persistent)
```kotlin
val vmState by viewModel.state.collectAsStateWithLifecycle()

vmState.isLoading          // From ViewModel
vmState.selectedScenario   // From ViewModel
vmState.error              // From ViewModel
vmState.votingResults      // From ViewModel
```

**Responsabilités:**
- Données chargées du repository
- État persistant entre recompositions
- Partagé avec le ViewModel

### uiState (Local UI State - Ephemeral)
```kotlin
var uiState by remember { mutableStateOf(ScenarioDetailUIState()) }

uiState.isEditing      // Local UI only
uiState.isSaving       // Local UI only
uiState.editName       // Local UI only (avant sauvegarde)
uiState.showDeleteDialog  // Local UI only
```

**Responsabilités:**
- État éphémère (édition locale)
- Pas besoin de persistance
- Reset lors de navigation

## 🧪 Testing

### Tests de la Composable
```kotlin
@Test
fun scenarioDetailScreen_loadsScenarioOnMount() {
    val viewModel = mockScenarioManagementViewModel()
    val commentRepository = mockCommentRepository()
    
    composeTestRule.setContent {
        ScenarioDetailScreen(
            scenarioId = "scenario-1",
            viewModel = viewModel,
            commentRepository = commentRepository,
            isOrganizer = true,
            onBack = {},
            onDeleted = {},
            onNavigateToComments = { _, _, _ -> }
        )
    }
    
    verify(viewModel).selectScenario("scenario-1")
}

@Test
fun scenarioDetailScreen_callsOnBackWhenNavigateBackEffectEmitted() {
    val onBack = mockk<() -> Unit>()
    val viewModel = mockScenarioManagementViewModel()
    
    // Setup viewModel to emit NavigateBack side effect
    coEvery { viewModel.sideEffect } returns flow {
        emit(ScenarioManagementContract.SideEffect.NavigateBack)
    }
    
    composeTestRule.setContent {
        ScenarioDetailScreen(
            scenarioId = "scenario-1",
            viewModel = viewModel,
            commentRepository = mockCommentRepository(),
            isOrganizer = true,
            onBack = onBack,
            onDeleted = {},
            onNavigateToComments = { _, _, _ -> }
        )
    }
    
    verify { onBack() }
}
```

## ⚠️ Erreurs Courantes

### ❌ Erreur 1: Appel Direct au Repository
```kotlin
// ❌ MAUVAIS
IconButton(onClick = {
    repository.deleteScenario(scenarioId)  // Direct repository call!
}) { ... }
```

**Correction:**
```kotlin
// ✅ CORRECT
IconButton(onClick = {
    viewModel.deleteScenario(scenarioId)   // Via ViewModel
}) { ... }
```

### ❌ Erreur 2: Création du ViewModel
```kotlin
// ❌ MAUVAIS
val viewModel = ScenarioManagementViewModel(stateMachine)  // Manual creation!
```

**Correction:**
```kotlin
// ✅ CORRECT
val viewModel: ScenarioManagementViewModel = koinViewModel()  // Via DI
```

### ❌ Erreur 3: Mutation du vmState Directement
```kotlin
// ❌ MAUVAIS
vmState = vmState.copy(isLoading = true)  // Can't mutate collected state!
```

**Correction:**
```kotlin
// ✅ CORRECT - Use ViewModel methods
viewModel.selectScenario(scenarioId)  // Dispatch intent, ViewModel updates state
```

### ❌ Erreur 4: Oubli de LaunchedEffect
```kotlin
// ❌ MAUVAIS - Scenario not loaded
ScenarioDetailScreen(scenarioId = "scenario-1", ...)
// Screen composes but selectScenario() never called!
```

**Correction:**
```kotlin
// ✅ CORRECT - Inside ScenarioDetailScreen
LaunchedEffect(scenarioId) {
    viewModel.selectScenario(scenarioId)  // Load on mount
}
```

## 🔗 Ressources

- [ScenarioManagementViewModel](./composeApp/src/commonMain/kotlin/com/guyghost/wakeve/viewmodel/ScenarioManagementViewModel.kt)
- [ScenarioManagementContract](./shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state/ScenarioManagementContract.kt)
- [Refactoring Summary](./SCENARIO_DETAIL_REFACTORING.md)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [ViewModel & StateFlow Guide](https://developer.android.com/topic/architecture/ui-layer/state-holders)

## 📞 Support

Pour des questions sur le refactoring ou l'utilisation du nouveau pattern:
1. Consulter [SCENARIO_DETAIL_REFACTORING.md](./SCENARIO_DETAIL_REFACTORING.md)
2. Lire les commentaires dans [ScenarioManagementViewModel.kt](./composeApp/src/commonMain/kotlin/com/guyghost/wakeve/viewmodel/ScenarioManagementViewModel.kt)
3. Examiner les exemples dans ce guide

---

**Guide Complet** ✅ | Date: 2025-12-29
