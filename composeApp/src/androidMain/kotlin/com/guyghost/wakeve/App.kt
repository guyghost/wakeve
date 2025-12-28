package com.guyghost.wakeve

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.DatabaseProvider
import com.guyghost.wakeve.SplashScreen
import com.guyghost.wakeve.LoginScreen
import com.guyghost.wakeve.OnboardingScreen
import com.guyghost.wakeve.ScenarioListScreen
import com.guyghost.wakeve.auth.AndroidAuthenticationService
import com.guyghost.wakeve.auth.AuthState
import com.guyghost.wakeve.auth.AuthStateManager
import com.guyghost.wakeve.auth.GoogleSignInHelper
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.ScenarioRepository
import com.guyghost.wakeve.security.AndroidSecureTokenStorage
import com.guyghost.wakeve.sync.AndroidNetworkStatusDetector
import com.guyghost.wakeve.sync.KtorSyncHttpClient
import com.guyghost.wakeve.sync.SyncManager
import com.guyghost.wakeve.ui.event.ModernEventDetailView
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val PREFS_NAME = "wakeve_prefs"
private const val HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"

fun getSharedPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

fun hasCompletedOnboarding(context: Context): Boolean {
    return getSharedPreferences(context).getBoolean(HAS_COMPLETED_ONBOARDING, false)
}

fun markOnboardingComplete(context: Context) {
    getSharedPreferences(context).edit().putBoolean(HAS_COMPLETED_ONBOARDING, true).apply()
}

enum class AppRoute {
    SPLASH,
    ONBOARDING,
    LOGIN,
    HOME,
    EVENT_CREATION,
    PARTICIPANT_MANAGEMENT,
    POLL_VOTING,
    POLL_RESULTS,
    EVENT_DETAIL,
    SCENARIO_LIST,
    SCENARIO_DETAIL,
    SCENARIO_COMPARISON,
    BUDGET_OVERVIEW,
    BUDGET_DETAIL,
    ACCOMMODATION,
    MEAL_PLANNING,
    EQUIPMENT_CHECKLIST,
    ACTIVITY_PLANNING,
    COMMENTS
}

@Composable
@Preview
fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // App navigation state
    var currentRoute by remember { mutableStateOf(AppRoute.SPLASH) }
    var isAuthenticated by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf<String?>(null) }
    var hasOnboarded by remember { mutableStateOf(false) }
    
    // Event and scenario navigation state
    var selectedEvent by remember { mutableStateOf<com.guyghost.wakeve.models.Event?>(null) }
    var selectedScenarioId by remember { mutableStateOf<String?>(null) }
    var selectedBudgetItemId by remember { mutableStateOf<String?>(null) }
    
    // Initialize app dependencies
    LaunchedEffect(Unit) {
        // Initialize authentication
        val authService = AndroidAuthenticationService(context)
        val authStateManager = AuthStateManager(
            secureStorage = com.guyghost.wakeve.security.AndroidSecureTokenStorage(context),
            authService = authService,
            enableOAuth = false // TODO: Enable OAuth when configured
        )
        authStateManager.initialize()
        
        // Check onboarding status
        hasOnboarded = hasCompletedOnboarding(context)
        
        // Check authentication state
        authStateManager.authState.collect { state ->
            when (state) {
                is AuthState.Authenticated -> {
                    isAuthenticated = true
                    userId = state.userId
                    // Do not set currentRoute here, let splash decide
                }
                is AuthState.Unauthenticated -> {
                    isAuthenticated = false
                    userId = null
                    // Do not set currentRoute here, let splash decide
                }
                else -> {
                    // Loading or Error, stay on current route
                }
            }
        }
    }
    
    MaterialTheme {
        when (currentRoute) {
            AppRoute.SPLASH -> {
                SplashScreen(
                    onAnimationComplete = {
                        // Route to appropriate screen based on auth and onboarding state
                        currentRoute = when {
                            !isAuthenticated -> AppRoute.LOGIN
                            isAuthenticated && !hasOnboarded -> AppRoute.ONBOARDING
                            isAuthenticated && hasOnboarded -> AppRoute.HOME
                            else -> AppRoute.HOME // fallback
                        }
                    }
                )
            }
            AppRoute.ONBOARDING -> {
                OnboardingScreen(
                    onOnboardingComplete = {
                        scope.launch {
                            markOnboardingComplete(context)
                            currentRoute = AppRoute.HOME // User is authenticated after onboarding
                        }
                    }
                )
            }
            AppRoute.LOGIN -> {
                LoginScreen(
                    onGoogleSignIn = { /* TODO: Implement Google Sign-In */ },
                    onAppleSignIn = { /* Not available on Android */ }
                )
            }
            AppRoute.HOME -> {
                val database = remember { DatabaseProvider.getDatabase(com.guyghost.wakeve.AndroidDatabaseFactory(context)) }
                val eventRepository = remember { com.guyghost.wakeve.DatabaseEventRepository(database, null) }
                val events = remember { eventRepository.getAllEvents() }
                
                HomeScreen(
                    events = events,
                    userId = userId ?: "",
                    onCreateEvent = { currentRoute = AppRoute.EVENT_CREATION },
                    onEventClick = { 
                        selectedEvent = it
                        currentRoute = AppRoute.EVENT_DETAIL
                    },
                    onSignOut = {
                        scope.launch {
                            val authService = AndroidAuthenticationService(context)
                            val authStateManager = AuthStateManager(
                                secureStorage = com.guyghost.wakeve.security.AndroidSecureTokenStorage(context),
                                authService = authService,
                                enableOAuth = false
                            )
                            authStateManager.logout()
                            currentRoute = AppRoute.LOGIN
                        }
                    }
                )
            }
            AppRoute.EVENT_DETAIL -> {
                selectedEvent?.let { event ->
                    ModernEventDetailView(
                        event = event,
                        userId = userId ?: "",
                        onNavigateToScenarioList = { currentRoute = AppRoute.SCENARIO_LIST },
                        onNavigateToBudgetOverview = { currentRoute = AppRoute.BUDGET_OVERVIEW },
                        onNavigateToAccommodation = { currentRoute = AppRoute.ACCOMMODATION },
                        onNavigateToMealPlanning = { currentRoute = AppRoute.MEAL_PLANNING },
                        onNavigateToEquipmentChecklist = { currentRoute = AppRoute.EQUIPMENT_CHECKLIST },
                        onNavigateToActivityPlanning = { currentRoute = AppRoute.ACTIVITY_PLANNING },
                        onNavigateToComments = { currentRoute = AppRoute.COMMENTS },
                        onNavigateToHome = { currentRoute = AppRoute.HOME }
                    )
                } ?: run {
                    // Fallback if no event is selected
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Événement non trouvé")
                        Button(onClick = { currentRoute = AppRoute.HOME }) {
                            Text("Retour à l'accueil")
                        }
                    }
                }
            }
            AppRoute.SCENARIO_LIST -> {
                selectedEvent?.let { event ->
                    // In a real implementation, we would inject the repository
                    // For now, we'll create a mock repository instance
                    val mockRepository = com.guyghost.wakeve.ScenarioRepository(
                        com.guyghost.wakeve.DatabaseProvider.getDatabase(
                            com.guyghost.wakeve.AndroidDatabaseFactory(context)
                        )
                    )
                    
                    ScenarioListScreen(
                        event = event,
                        repository = mockRepository,
                        participantId = userId ?: "",
                        onScenarioClick = { scenarioId -> 
                            selectedScenarioId = scenarioId
                            currentRoute = AppRoute.SCENARIO_DETAIL
                        },
                        onCreateScenario = { /* TODO: Implement create scenario */ },
                        onCompareScenarios = { currentRoute = AppRoute.SCENARIO_COMPARISON }
                    )
                } ?: run {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Événement non sélectionné")
                        Button(onClick = { currentRoute = AppRoute.HOME }) {
                            Text("Retour à l'accueil")
                        }
                    }
                }
            }
            AppRoute.SCENARIO_DETAIL -> {
                // Placeholder - will implement with real scenario detail screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Détail du scénario")
                    Button(onClick = { currentRoute = AppRoute.SCENARIO_LIST }) {
                        Text("Retour à la liste")
                    }
                }
            }
            AppRoute.SCENARIO_COMPARISON -> {
                // Placeholder - will implement with real scenario comparison screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Comparaison des scénarios")
                    Button(onClick = { currentRoute = AppRoute.SCENARIO_LIST }) {
                        Text("Retour à la liste")
                    }
                }
            }
            AppRoute.BUDGET_OVERVIEW -> {
                // Placeholder - will implement with real budget overview screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Aperçu du budget")
                    Button(onClick = { currentRoute = AppRoute.HOME }) {
                        Text("Retour à l'accueil")
                    }
                }
            }
            AppRoute.BUDGET_DETAIL -> {
                // Placeholder - will implement with real budget detail screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Détail du budget")
                    Button(onClick = { currentRoute = AppRoute.BUDGET_OVERVIEW }) {
                        Text("Retour à l'aperçu")
                    }
                }
            }
            AppRoute.EVENT_CREATION -> {
                // Placeholder - will implement with real event creation screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Création d'événement")
                    Button(onClick = { currentRoute = AppRoute.HOME }) {
                        Text("Retour à l'accueil")
                    }
                }
            }
            AppRoute.PARTICIPANT_MANAGEMENT -> {
                // Placeholder - will implement with real participant management screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Gestion des participants")
                    Button(onClick = { currentRoute = AppRoute.HOME }) {
                        Text("Retour à l'accueil")
                    }
                }
            }
            AppRoute.POLL_VOTING -> {
                // Placeholder - will implement with real poll voting screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Vote du sondage")
                    Button(onClick = { currentRoute = AppRoute.HOME }) {
                        Text("Retour à l'accueil")
                    }
                }
            }
            AppRoute.POLL_RESULTS -> {
                // Placeholder - will implement with real poll results screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Résultats du sondage")
                    Button(onClick = { currentRoute = AppRoute.HOME }) {
                        Text("Retour à l'accueil")
                    }
                }
            }
            AppRoute.ACCOMMODATION -> {
                // Placeholder - will implement with real accommodation screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Hébergement")
                    Button(onClick = { currentRoute = AppRoute.HOME }) {
                        Text("Retour à l'accueil")
                    }
                }
            }
            AppRoute.MEAL_PLANNING -> {
                // Placeholder - will implement with real meal planning screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Planification des repas")
                    Button(onClick = { currentRoute = AppRoute.HOME }) {
                        Text("Retour à l'accueil")
                    }
                }
            }
            AppRoute.EQUIPMENT_CHECKLIST -> {
                // Placeholder - will implement with real equipment checklist screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Liste d'équipement")
                    Button(onClick = { currentRoute = AppRoute.HOME }) {
                        Text("Retour à l'accueil")
                    }
                }
            }
            AppRoute.ACTIVITY_PLANNING -> {
                // Placeholder - will implement with real activity planning screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Planification des activités")
                    Button(onClick = { currentRoute = AppRoute.HOME }) {
                        Text("Retour à l'accueil")
                    }
                }
            }
            AppRoute.COMMENTS -> {
                // Placeholder - will implement with real comments screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Commentaires")
                    Button(onClick = { currentRoute = AppRoute.HOME }) {
                        Text("Retour à l'accueil")
                    }
                }
            }
        }
    }
}

@Composable
fun AppPreview() {
    MaterialTheme {
        App()
    }
}
