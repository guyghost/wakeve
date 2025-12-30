# Migration Guide: ScenarioComparisonScreen Refactor

## Résumé

Ce guide explique comment mettre à jour les écrans parents et la navigation qui utilisent `ScenarioComparisonScreen` suite au refactor vers l'architecture State Machine.

---

## Avant vs Après

### Ancienne signature
```kotlin
fun ScenarioComparisonScreen(
    event: Event,
    repository: ScenarioRepository,
    onBack: () -> Unit
)
```

### Nouvelle signature
```kotlin
fun ScenarioComparisonScreen(
    scenarioIds: List<String>,
    eventTitle: String,
    viewModel: ScenarioManagementViewModel,
    onBack: () -> Unit
)
```

---

## Changements requis

### 1. Mise à jour des imports

#### Avant
```kotlin
import com.guyghost.wakeve.ScenarioRepository
import com.guyghost.wakeve.models.Event
```

#### Après
```kotlin
import com.guyghost.wakeve.viewmodel.ScenarioManagementViewModel
```

### 2. Mise à jour des appels de navigation

#### Avant - Navigation avec Event et Repository
```kotlin
// Écran parent qui navigue vers ScenarioComparisonScreen
Button(onClick = {
    navController.navigate("comparison") {
        launchSingleTop = true
    }
    // Passer Event et Repository via arguments ou state
}) {
    Text("Compare Scenarios")
}

// Dans ScenarioComparisonScreen
val event = navBackStackEntry.arguments?.getParcelable<Event>("event")
val repository = getRepository() // Injector pattern
```

#### Après - Navigation avec scenario IDs et titre
```kotlin
// Écran parent qui navigue vers ScenarioComparisonScreen
Button(onClick = {
    val scenarioIds = listOf("s1", "s2", "s3")
    navController.navigate("comparison/$scenarioIds") {
        launchSingleTop = true
    }
}) {
    Text("Compare Scenarios")
}

// Dans ScenarioComparisonScreen
val scenarioIds = navBackStackEntry.arguments?.getStringArray("scenarioIds")?.toList() ?: emptyList()
val eventTitle = navBackStackEntry.arguments?.getString("eventTitle") ?: ""
val viewModel: ScenarioManagementViewModel = koinViewModel()
```

### 3. Mise à jour de l'écran parent

#### Avant - Transmission directe du repository
```kotlin
@Composable
fun ScenarioListScreen(
    event: Event,
    repository: ScenarioRepository,
    navController: NavController
) {
    Column {
        // ...
        
        Button(onClick = {
            val selectedScenarioIds = listOf("s1", "s2")
            
            // Naviguer et passer Event et Repository
            navController.navigate(
                Screen.Comparison.createRoute(
                    event = event,
                    repository = repository
                )
            )
        }) {
            Text("Compare")
        }
    }
    
    // Dans la navigation:
    composable<Screen.Comparison> { entry ->
        val event = entry.arguments?.getParcelable<Event>("event")
        val repository = entry.arguments?.getSerializable("repository")
        
        ScenarioComparisonScreen(
            event = event,
            repository = repository,
            onBack = { navController.popBackStack() }
        )
    }
}
```

#### Après - Transmission via ViewModel injecté
```kotlin
@Composable
fun ScenarioListScreen(
    navController: NavController,
    viewModel: ScenarioManagementViewModel = koinViewModel()
) {
    val scenarios by viewModel.scenarios.collectAsStateWithLifecycle()
    
    Column {
        // ...
        
        Button(onClick = {
            val selectedScenarioIds = scenarios
                .filter { it.scenario.isSelected }
                .map { it.scenario.id }
            
            // Naviguer avec IDs seulement
            navController.navigate("comparison/${selectedScenarioIds.joinToString(",")}")
        }) {
            Text("Compare")
        }
    }
    
    // Dans la navigation:
    composable("comparison/{scenarioIds}") { entry ->
        val scenarioIds = entry.arguments?.getString("scenarioIds")
            ?.split(",")
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        
        // ViewModel obtenu via Koin
        val comparisonViewModel: ScenarioManagementViewModel = koinViewModel()
        
        ScenarioComparisonScreen(
            scenarioIds = scenarioIds,
            eventTitle = "My Event",
            viewModel = comparisonViewModel,
            onBack = { navController.popBackStack() }
        )
    }
}
```

### 4. Configuration Koin (si pas déjà fait)

Ajouter à votre module Koin:

```kotlin
// Dans appModule ou scenarioModule
val scenarioModule = module {
    // State machine
    single { ScenarioManagementStateMachine(get(), get()) }
    
    // ViewModel
    viewModel { ScenarioManagementViewModel(get()) }
}
```

---

## Checklist de migration

- [ ] Mettre à jour les imports dans tous les fichiers appelant ScenarioComparisonScreen
- [ ] Remplacer les paramètres (event, repository) par (scenarioIds, eventTitle, viewModel)
- [ ] Mettre à jour la logique de navigation
- [ ] Ajouter le ViewModel à la configuration Koin
- [ ] Tester la navigation et l'affichage de la comparaison
- [ ] Supprimer les imports inutiles de Event et ScenarioRepository
- [ ] Vérifier que collectAsStateWithLifecycle() est utilisé

---

## Exemples de fichiers à mettre à jour

1. **Navigation/Routes**
   - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/navigation/Routes.kt`
   - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/navigation/Navigation.kt`

2. **Écrans parents**
   - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/ScenarioListScreen.kt`
   - Tout écran qui appelle ScenarioComparisonScreen

3. **Configuration**
   - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/di/KoinModule.kt`

---

## Problèmes courants et solutions

### Problème: "Cannot find ScenarioManagementViewModel"
**Solution:** Ajouter le module à la configuration Koin
```kotlin
startKoin {
    modules(scenarioModule) // Ajouter cette ligne
}
```

### Problème: "collectAsStateWithLifecycle is not available"
**Solution:** Ajouter l'import manquant
```kotlin
import androidx.lifecycle.compose.collectAsStateWithLifecycle
```

### Problème: LaunchedEffect() dispose trop rapidement
**Solution:** Utiliser Key pour contrôler quand relancer l'effet
```kotlin
LaunchedEffect(scenarioIds) { // scenarioIds est la Key
    viewModel.compareScenarios(scenarioIds)
}
```

### Problème: Navigation ne fonctionne pas après refactor
**Solution:** Vérifier que onBack() est correctement appelé
```kotlin
// Correct - Navigation via callback
onBack = { navController.popBackStack() }

// Dans le screen
LaunchedEffect(Unit) {
    viewModel.sideEffect.collect { effect ->
        when (effect) {
            is ScenarioManagementContract.SideEffect.NavigateBack -> {
                onBack() // Appelle navController.popBackStack()
            }
            // ...
        }
    }
}
```

---

## Teste la migration

### Test 1: Navigation fonctionne
```kotlin
@Test
fun scenarioListScreen_navigateToComparison_shouldDisplayComparisonScreen() = runTest {
    // Navigate to scenario list
    // Click "Compare" button
    // Verify ScenarioComparisonScreen is displayed
}
```

### Test 2: ViewModel se charge
```kotlin
@Test
fun scenarioComparisonScreen_shouldLoadComparisonData() = runTest {
    val viewModel = koinViewModel<ScenarioManagementViewModel>()
    val comparison = viewModel.comparison.value
    
    assertNotNull(comparison)
    assertEquals(2, comparison.scenarios.size)
}
```

### Test 3: État se met à jour
```kotlin
@Test
fun scenarioComparisonScreen_stateUpdates_shouldReflectInUI() = runTest {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    // Dispatcher une action
    viewModel.compareScenarios(scenarioIds)
    
    // Vérifier que l'état a changé
    assertEquals(false, state.isLoading)
    assertNotNull(state.comparison)
}
```

---

## Documentation associée

- `SCENARIO_COMPARISON_REFACTOR.md` - Détails du refactor
- `SCENARIO_COMPARISON_TEST_EXAMPLES.kt` - Exemples de tests
- `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/viewmodel/ScenarioManagementViewModel.kt` - Documentation du ViewModel

---

## Support

Si vous rencontrez des problèmes:

1. Vérifier que tous les imports sont corrects
2. Vérifier que le module Koin est configuré
3. Vérifier les logs de debug pour les erreurs spécifiques
4. Consulter les fichiers de référence existants

