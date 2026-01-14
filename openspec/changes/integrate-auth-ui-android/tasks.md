# Tasks: Integrate Authentication UI on Android

## Phase 1: Connexion AuthScreen ↔ Navigation (P0 - Critique)

### 1.1 Modifier Screen.kt pour ajouter les routes Auth
- [x] Ajouter `Screen.Auth` pour l'écran principal d'authentification
- [x] Ajouter `Screen.EmailAuth` pour l'écran OTP email
- [x] Supprimer ou deprecate `Screen.Login` (remplacé par Auth)
- [x] Mettre à jour `Screen.GetStarted` pour rediriger vers Auth

### 1.2 Modifier WakevNavHost.kt
- [x] Remplacer `LoginScreen` composable par `AuthScreen`
- [x] Ajouter composable pour `EmailAuthScreen`
- [x] Connecter `onGoogleSignIn` callback
- [x] Connecter `onAppleSignIn` callback (disabled sur Android)
- [x] Connecter `onEmailSignIn` callback → navigate to EmailAuth
- [x] Connecter `onSkip` callback → dispatch SkipAuth intent
- [x] Passer `isLoading` et `errorMessage` depuis AuthViewModel

### 1.3 Créer pont MainActivity ↔ AuthScreen
- [x] Créer interface `AuthCallbacks` dans MainActivity
- [x] Exposer `googleSignInLauncher` via callback
- [x] Gérer résultat OAuth et dispatcher à AuthStateMachine
- [x] Utiliser CompositionLocalProvider pattern

## Phase 2: AuthViewModel Integration (P0 - Critique)

### 2.1 Créer AuthViewModel
- [x] Créer `AuthViewModel.kt` dans `ui/auth/`
- [x] Injecter `AuthStateMachine` via Koin
- [x] Exposer `state: StateFlow<AuthUiState>`
- [x] Exposer `sideEffects: SharedFlow<AuthSideEffect>`
- [x] Implémenter `dispatch(intent: AuthIntent)`

### 2.2 Définir AuthUiState
- [x] `isLoading: Boolean`
- [x] `errorMessage: String?`
- [x] `isAuthenticated: Boolean`
- [x] `isGuest: Boolean`
- [x] `currentUser: User?`

### 2.3 Mapper AuthContract → AuthUiState
- [x] Observer `AuthStateMachine.state`
- [x] Transformer en `AuthUiState`
- [x] Collecter side effects pour navigation

### 2.4 Enregistrer AuthViewModel dans Koin
- [x] Ajouter `factory { AuthViewModel(get()) }` dans platformModule
- [x] Vérifier injection dans WakevNavHost

## Phase 3: Navigation State-Driven (P0 - Critique)

### 3.1 Modifier App.kt
- [x] Remplacer check manuel par observation de `AuthStateMachine.state`
- [x] Utiliser `collectAsState` pour navigation réactive
- [x] Gérer les états: Loading, Unauthenticated, Authenticated, Guest

### 3.2 Implémenter navigation automatique
- [x] `AuthState.Unauthenticated` → `Screen.Auth`
- [x] `AuthState.Authenticated + !hasOnboarded` → `Screen.Onboarding`
- [x] `AuthState.Authenticated + hasOnboarded` → `Screen.Home`
- [x] `AuthState.Guest` → Onboarding or Home based on hasOnboarded

### 3.3 Gérer les side effects de navigation
- [x] `SideEffect.NavigateToMain` → `Screen.Home`
- [x] `SideEffect.NavigateToOnboarding` → `Screen.Onboarding`
- [x] `SideEffect.ShowError` → Toast

### 3.4 Nettoyer la navigation après auth
- [x] `popUpTo(Screen.Auth)` après successful auth
- [x] Clear back stack pour éviter retour à Auth
- [ ] Gérer deep links OAuth callback

## Phase 4: Session & Logout (P1 - Important)

### 4.1 Modifier SettingsScreen
- [x] Ajouter section "Compte" avec état connexion
- [x] Afficher email/nom si authentifié
- [x] Afficher "Mode invité" si guest
- [x] Ajouter bouton "Se déconnecter" (si authentifié)
- [x] Ajouter bouton "Créer un compte" (si guest)

### 4.2 Modifier ProfileScreen
- [x] Afficher avatar/initiales utilisateur
- [x] Afficher badge "Guest" si mode invité
- [x] Quick access au logout
- [x] Ajouter bouton "Créer un compte" (si guest)

### 4.3 Implémenter Logout Flow
- [x] Créer `AuthViewModel.signOut()`
- [x] Dispatch `Intent.SignOut` à AuthStateMachine
- [x] Clear tokens via `TokenStorage`
- [x] Navigate to `Screen.Auth`
- [x] Clear back stack

### 4.4 Session Restoration
- [x] Vérifier token au démarrage via AuthStateMachine init
- [x] Si token valide → `AuthState.Authenticated`
- [ ] Si token expiré → try refresh
- [x] Si no session → `AuthState.Unauthenticated`

## Phase 5: Tests & Polish (P1 - Important)

### 5.1 Tests UI (Espresso)
- [ ] Test: Auth screen displays all buttons
- [ ] Test: Skip button navigates to Home (guest mode)
- [ ] Test: Google button triggers OAuth flow
- [ ] Test: Error message displays correctly
- [ ] Test: Loading state shows progress indicator

### 5.2 Tests Integration
- [ ] Test: Full auth flow (mock OAuth)
- [ ] Test: Session restoration after app restart
- [ ] Test: Logout clears session and navigates to Auth
- [ ] Test: Guest mode persists across restarts

### 5.3 Polish
- [ ] Animations de transition entre écrans
- [ ] Haptic feedback sur boutons
- [ ] Accessibility labels (contentDescription)
- [ ] Dark mode support verification
- [ ] Tablet layout adaptation

## Validation Checklist

### Functional
- [ ] Google Sign-In fonctionne end-to-end
- [x] Mode invité (Skip) fonctionne
- [ ] Email OTP flow complet
- [x] Logout fonctionne (navigation + AuthStateMachine.signOut)
- [x] Session restore fonctionne (via AuthStateMachine init)

### UX
- [x] Bouton Skip visible en haut à droite
- [x] Loading indicator pendant OAuth
- [x] Messages d'erreur clairs (Toast)
- [x] Transition fluide après auth
- [x] Profile affiche badge "Mode invité" pour guests
- [x] Settings affiche état de connexion
- [x] Avatar avec initiales si nom disponible

### Technical
- [ ] Pas de régression sur tests existants
- [x] AuthStateMachine utilisé correctement
- [x] Tokens stockés de façon sécurisée
- [x] Navigation stack propre
- [x] AuthViewModel injecté via Koin dans Profile et Settings

## Summary of Changes

### Files Created
1. `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/auth/AuthViewModel.kt`
   - ViewModel wrapping AuthStateMachine for Compose
   - Exposes AuthUiState and AuthSideEffect

2. `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/AuthCallbacks.kt`
   - Interface for Activity-level OAuth callbacks
   - LocalAuthCallbacks CompositionLocal

### Files Modified
1. `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/Screen.kt`
   - Added Screen.Auth and Screen.EmailAuth
   - Deprecated Screen.Login

2. `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/di/PlatformModule.android.kt`
   - Added TokenStorage, AuthService, EmailAuthService, GuestModeService
   - Added AuthStateMachine singleton
   - Added AuthViewModel factory

3. `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakevNavHost.kt`
   - Replaced LoginScreen with AuthScreen + EmailAuthScreen
   - Connected all callbacks via AuthViewModel
   - Added side effect handling (navigation, toasts)
   - Updated ProfileTabScreen composable to use AuthViewModel
   - Updated SettingsScreen composable to use AuthViewModel

4. `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/App.kt`
   - Simplified to use AuthStateMachine from Koin
   - Reactive navigation based on auth state

5. `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/MainActivity.kt`
   - Implemented AuthCallbacks interface
   - Wrapped App with CompositionLocalProvider

6. `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/SettingsScreen.kt` (Phase 4)
   - Added account section with auth state display
   - Shows email/name if authenticated
   - Shows "Mode invité" badge if guest
   - Logout button for authenticated users
   - "Create account" button for guests

7. `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/ProfileTabScreen.kt` (Phase 4)
   - Shows user avatar with initials or default icon
   - Shows "Mode invité" badge for guest users
   - Shows user email/name if authenticated
   - Sign out button for authenticated users
   - "Create account" button for guests
