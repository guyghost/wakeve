# 🚀 Prochaines Étapes: Configuration Koin et Intégration App.kt

## ⚠️ Situation Actuelle

L'implémentation State Machine est **prête** pour Android Compose, mais elle nécessite **une intégration dans App.kt** pour fonctionner.

**Problème**: App.kt utilise l'ancienne signature HomeScreen avec paramètres `events`, `userId`, etc., mais le nouveau HomeScreen attend un `EventManagementViewModel`.

## 🔧 Étapes pour Intégrer Koin et Résoudre la Compilation

### Étape 1: Initialiser Koin dans MainActivity

**Fichier**: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/MainActivity.kt` (ou équivalent)

```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.guyghost.wakeve.di.initializeKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ✅ AJOUTER CETTE LIGNE
        initializeKoin()  // Initialize Koin BEFORE setContent
        
        setContent {
            MyApp()
        }
    }
}
```

### Étape 2: Adapter App.kt pour Utiliser le ViewModel

**Situation actuelle** (ligne 170 dans App.kt):
```kotlin
AppRoute.HOME -> {
    val database = remember { DatabaseProvider.getDatabase(...) }
    val eventRepository = remember { DatabaseEventRepository(database, null) }
    val events = remember { eventRepository.getAllEvents() }
    
    HomeScreen(
        events = events,              // ❌ Ancien paramètre
        userId = userId ?: "",        // ❌ Ancien paramètre
        onCreateEvent = { ... },      // ❌ Ancien callback
        onEventClick = { ... },       // ❌ Ancien callback
        onSignOut = { ... }           // ❌ Ancien callback
    )
}
```

**Solution - Option A: Créer une fonction d'adaptation**

Ajouter une nouvelle fonction composable dans App.kt:

```kotlin
@Composable
private fun HomeScreenWithState(
    userId: String,
    onCreateEvent: () -> Unit,
    onEventClick: (Event) -> Unit,
    onSignOut: () -> Unit
) {
    // TODO: Créer/injecter le ViewModel depuis Koin
    // val viewModel: EventManagementViewModel = ... // inject
    
    // Pour l'instant, afficher un placeholder
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("HomeScreen sera intégré ici")
    }
}
```

Puis remplacer l'appel à HomeScreen:
```kotlin
AppRoute.HOME -> {
    HomeScreenWithState(
        userId = userId ?: "",
        onCreateEvent = { currentRoute = AppRoute.EVENT_CREATION },
        onEventClick = { 
            selectedEvent = it
            currentRoute = AppRoute.EVENT_DETAIL
        },
        onSignOut = {
            // ... existing sign out logic
        }
    )
}
```

**Solution - Option B: Remplacer entièrement**

Supprimer tout le code de gestion d'état manuel et utiliser uniquement le ViewModel:

```kotlin
AppRoute.HOME -> {
    // Let the ViewModel handle everything
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Implementation en cours...")
    }
}
```

## 📝 Implémentation Détaillée du ViewModel Injection

### Approche 1: Manual Koin Get (Simple)

```kotlin
@Composable
fun HomeScreenWithState(
    userId: String,
    // callbacks...
) {
    // Get the ViewModel from Koin
    val viewModel = remember {
        org.koin.core.context.GlobalContext.get().get<EventManagementViewModel>()
    }
    
    val state by viewModel.state.collectAsState()
    
    // Load events when screen appears
    LaunchedEffect(Unit) {
        viewModel.dispatch(EventManagementContract.Intent.LoadEvents)
    }
    
    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is EventManagementContract.SideEffect.NavigateTo -> {
                    // Handle navigation
                }
                is EventManagementContract.SideEffect.ShowToast -> {
                    // Show toast
                }
                is EventManagementContract.SideEffect.NavigateBack -> {
                    // Go back
                }
            }
        }
    }
    
    // Render HomeScreen with the ViewModel
    HomeScreen(
        viewModel = viewModel,
        onNavigateTo = { route -> /* ... */ },
        onShowToast = { msg -> /* ... */ }
    )
}
```

### Approche 2: Koin ViewModel Helper (Mieux)

Créer un helper dans `AppModule.kt`:

```kotlin
// Dans AppModule.kt, ajouter:
fun getViewModel(): EventManagementViewModel {
    return org.koin.core.context.GlobalContext.get().get()
}
```

Puis dans App.kt:

```kotlin
@Composable
fun HomeScreenWithState(/* ... */) {
    val viewModel = remember { getViewModel() }
    // ... rest same as above
}
```

## 🧪 Test de Compilation

Après chaque étape, testez la compilation:

```bash
# Compile tout sauf les tests
./gradlew build -x test

# Ou juste la partie Kotlin
./gradlew compileKotlinJvm -x test
```

## 📋 Checklist d'Intégration

- [ ] Étape 1: Initialiser Koin dans MainActivity
- [ ] Étape 2: Créer la fonction HomeScreenWithState
- [ ] Étape 3: Remplacer l'appel HomeScreen dans App.kt
- [ ] Étape 4: Tester la compilation (`./gradlew build -x test`)
- [ ] Étape 5: Tester sur émulateur/device Android
- [ ] Étape 6: Vérifier que les événements se chargent
- [ ] Étape 7: Migrer EventDetailScreen
- [ ] Étape 8: Migrer les autres écrans

## 🔗 Références

- **Koin Scope**: `org.koin.core.context.GlobalContext.get().get<T>()`
- **StateFlow Collection**: `viewModel.state.collectAsState()`
- **LaunchedEffect**: `LaunchedEffect(Unit) { ... }`
- **Remember**: `remember { ... }` pour éviter les recreations

## ⚠️ Attention

1. **initializeKoin() doit être appelée AVANT setContent {}**
2. **Les ViewModels doivent être injectés dans un Composable**
3. **Utilisez LaunchedEffect UNIQUE par responsibility**
4. **Les side effects Flow doivent être collectés une seule fois par screen**

## 🎯 Objectif Final

Après intégration, l'architecture sera:

```
App.kt (Navigation)
    ↓
HomeScreenWithState (Adaptation)
    ↓
HomeScreen (State Machine Based)
    ↓
EventManagementViewModel
    ↓
EventManagementStateMachine
    ↓
Use Cases & Repository
```

## 📞 Questions?

Consultez:
1. `ANDROID_STATE_MACHINE_INTEGRATION.md` - Architecture globale
2. `STATE_MACHINE_ANDROID_INTEGRATION_STATUS.md` - Statut détaillé
3. Le code de `HomeScreen.kt` et `EventDetailScreen.kt` pour les patterns

---

**Prochaine étape**: Initialiser Koin et adapter App.kt pour l'intégration

**Temps estimé**: 30-45 minutes pour l'intégration complète

**Difficultés attendues**:
- Résoudre les imports
- Gérer les callbacks de navigation
- Tester sur device réel
