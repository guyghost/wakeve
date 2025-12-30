# Intégration State Machine dans Android Screens

Ce document décrit comment intégrer la nouvelle architecture State Machine avec StateFlow dans les écrans Android Jetpack Compose.

## ✅ Tâches Complètes

### 1. ✅ Créer EventManagementViewModel.kt
**Fichier**: `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/viewmodel/EventManagementViewModel.kt`

Le ViewModel wrappre la State Machine `EventManagementStateMachine` et l'expose à Jetpack Compose via:
- `state: StateFlow<EventManagementContract.State>` - Pour observer l'état
- `sideEffect: Flow<EventManagementContract.SideEffect>` - Pour les événements one-time
- `dispatch(intent)` - Pour dispatcher des intents

**Fonctionnalités:**
- Wrapper autour de la State Machine
- Expose StateFlow pour les recompositions Compose
- Methodes de commodité: `loadEvents()`, `selectEvent()`, `clearError()`
- Documentation complète avec exemples d'usage

### 2. ✅ Mettre à jour HomeScreen.kt
**Fichier**: `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/HomeScreen.kt`

**Changements:**
- ✅ Injection du ViewModel via `koinViewModel()`
- ✅ Observation de l'état via `state.collectAsState()`
- ✅ Chargement automatique des événements dans `LaunchedEffect(Unit)`
- ✅ Gestion des side effects (navigation, toasts) dans `LaunchedEffect(Unit)`
- ✅ Dispatch des intents via `viewModel.dispatch(intent)`
- ✅ Affichage du loading state si `state.isLoading`
- ✅ Gestion des erreurs avec `ErrorState` composable
- ✅ Filtrage des événements par statut
- ✅ Conformité Material You

**Architecture:**
```
UI Events (tap, scroll)
    ↓
viewModel.dispatch(Intent)
    ↓
StateMachine.handleIntent()
    ↓
updateState() & emitSideEffect()
    ↓
collectAsState() & collect sideEffect
    ↓
Recomposition Compose
```

### 3. ✅ Créer EventDetailScreen.kt
**Fichier**: `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/EventDetailScreen.kt`

**Fonctionnalités:**
- ✅ Injection du ViewModel via `koinViewModel()`
- ✅ Affichage des détails de l'événement sélectionné
- ✅ Affichage des participants
- ✅ Affichage des résultats de sondage
- ✅ Actions organisateur (éditer, supprimer)
- ✅ Gestion des erreurs
- ✅ Navigation via side effects
- ✅ Dialogue de confirmation de suppression
- ✅ Conformité Material You

**Composables secondaires:**
- `EventInfoCard` - Affiche titre, description, dates
- `StatusCard` - Affiche le statut de l'événement
- `ParticipantsHeader` & `ParticipantItem` - Liste des participants
- `PollResultsHeader` & `PollVoteItem` - Résultats du sondage
- `VoteChip` - Badge pour afficher le type de vote
- `StatusChip` - Badge pour afficher le statut d'événement

### 4. ✅ Créer AppModule.kt (Koin)
**Fichier**: `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/di/AppModule.kt`

**Configuration:**
- ✅ Fournisseur de `EventManagementStateMachine` en tant que singleton
- ✅ Fournisseur de `EventManagementViewModel` en tant que viewModel
- ✅ CoroutineScope approprié pour la State Machine
- ✅ Fonction `initializeKoin()` pour initialiser Koin
- ✅ Documentation avec exemples d'usage

## 📋 À Faire

### 1. Ajouter Koin aux dépendances de composeApp

**Fichier**: `composeApp/build.gradle.kts`

```kotlin
commonMain.dependencies {
    // Koin for dependency injection
    implementation(libs.koin.core)
    implementation(libs.koin.androidx.viewmodel)
    implementation(libs.koin.androidx.compose)
}
```

Vérifier `gradle/libs.versions.toml` pour les versions exactes.

### 2. Initialiser Koin dans l'Activity Android

**Fichier**: `composeApp/src/androidMain/kotlin/MainActivity.kt` (ou équivalent)

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Koin
        initializeKoin()
        
        setContent {
            MyApp()
        }
    }
}
```

### 3. Ajouter SharedModule.kt à la configuration Koin

**Fichier**: `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/di/AppModule.kt`

```kotlin
fun initializeKoin() {
    val koinApplication = org.koin.core.context.startKoin {
        modules(
            sharedModule,  // From shared/di/SharedModule.kt
            appModule      // From composeApp/di/AppModule.kt
        )
    }
}
```

### 4. Mettre à jour les autres écrans (optionnel, par étapes)

Les écrans suivants pourraient être intégrés à la State Machine par la suite:
- `EventCreationScreen.kt` - Utiliser `Intent.CreateEvent`
- `PollVotingScreen.kt` - Intégrer les votes
- `PollResultsScreen.kt` - Afficher les résultats
- `ParticipantManagementScreen.kt` - Gérer les participants
- `InboxScreen.kt` - Afficher les notifications

**Pattern à suivre:**
```kotlin
@Composable
fun MyScreen(
    viewModel: EventManagementViewModel = koinViewModel(),
    onNavigateTo: (String) -> Unit = {},
    onShowToast: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.dispatch(EventManagementContract.Intent.LoadEvents)
    }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is EventManagementContract.SideEffect.NavigateTo -> onNavigateTo(effect.route)
                is EventManagementContract.SideEffect.ShowToast -> onShowToast(effect.message)
                is EventManagementContract.SideEffect.NavigateBack -> { /* ... */ }
            }
        }
    }
    
    // Render UI with state
}
```

### 5. Tester l'intégration

**Checklist de test:**
- [ ] HomeScreen charge et affiche les événements
- [ ] HomeScreen filtre par onglet (Tous, À venir, Passés)
- [ ] Cliquer sur un événement navigue vers EventDetailScreen
- [ ] EventDetailScreen affiche les détails corrects
- [ ] Erreurs sont affichées avec bouton "Réessayer"
- [ ] Suppression d'événement fonctionne avec confirmation
- [ ] Navigation entre les écrans fonctionne sans confusion d'état
- [ ] Pas de memory leaks lors de la destruction d'Activity

## 🏗️ Architecture

### Data Flow

```
User Action (tap, input)
    ↓
Composable calls viewModel.dispatch(intent)
    ↓
ViewModel.dispatch(intent)
    ↓
StateMachine.dispatch(intent) [async]
    ↓
StateMachine.handleIntent() [suspend]
    ↓
updateState() + emitSideEffect()
    ↓
StateFlow<State> + Flow<SideEffect> update
    ↓
Composable collects state & sideEffect
    ↓
Recomposition with new state
```

### Scopes & Lifecycles

- **State Machine**: Singleton, survit les recompositions Compose et changements de configuration
- **ViewModel**: ViewModel standard Android, créé/réutilisé par Jetpack Compose
- **Coroutine Scope**: `SupervisorJob` pour une gestion robuste des erreurs

### Thread Safety

- Tous les state updates via `MutableStateFlow.value` sont thread-safe
- Side effects via `Channel` sont thread-safe (buffered)
- ViewModel scope se connecte au viewModelScope ou à un coroutineScope global

## 📚 Références

### Material You Design System
- Couleurs: `Color(0xFF2563EB)` pour primary
- Typography: `MaterialTheme.typography.*`
- Spacing: `4.dp, 8.dp, 12.dp, 16.dp, 24.dp`
- Shapes: `RoundedCornerShape(12.dp)`

### Composables Clés
- `collectAsState()` - Convertit StateFlow en State Compose
- `LaunchedEffect(key)` - Exécute du code côté effet
- `koinViewModel<T>()` - Injecte le ViewModel Koin

### Documentation
- [Jetpack Compose StateFlow](https://developer.android.com/jetpack/compose/state)
- [Lifecycle in Compose](https://developer.android.com/jetpack/compose/side-effects)
- [Koin + Compose](https://insert-koin.io/docs/reference/koin-compose/viewmodel)

## ✨ Prochaines Étapes

1. **Ajouter dépendances Koin** à `composeApp/build.gradle.kts`
2. **Initialiser Koin** dans Android Activity/Application
3. **Tester HomeScreen** - Vérifier que les événements chargent correctement
4. **Tester EventDetailScreen** - Vérifier la navigation et l'affichage des détails
5. **Intégrer les autres écrans** par étapes (EventCreationScreen, PollVotingScreen, etc.)
6. **Ajouter des tests** unitaires et d'intégration

## 📝 Notes Importantes

### Injection Koin
- Ne pas oublier `initializeKoin()` dans l'Activity/Application Android
- Les modules doivent être chargés AVANT d'utiliser `koinViewModel()`

### StateFlow vs State
- `StateFlow<T>` = Flow cold avec state partagé
- `collectAsState()` = Convertit StateFlow en Compose State pour recompositions

### Side Effects
- Toujours collecter dans un `LaunchedEffect(Unit)` distinct
- Ne pas appeler côté effet dans les composables directement
- Une seule responsabilité par side effect collector

### Performance
- State Machine en singleton évite les allocations répétées
- Recompositions ciblées grâce à StateFlow (ne recompose que les dépendances)
- LaunchedEffect avec clé correcte évite les exécutions répétées

---

**Statut**: Implémentation en cours  
**Prochaine étape**: Ajouter Koin aux dépendances et initialiser dans Android Activity  
**Date**: 29 décembre 2025
