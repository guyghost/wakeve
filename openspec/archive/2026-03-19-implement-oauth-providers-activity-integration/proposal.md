# Change: Implement OAuth Providers Activity Integration

## Status
**ACTIVE** - En cours d'implémentation

## Why

L'intégration OAuth est partiellement implémentée mais ne fonctionne pas correctement. Les composants suivants existent mais ne sont pas connectés :

1. **MainActivity** implémente `AuthCallbacks` avec les méthodes `launchGoogleSignIn()` et `launchAppleSignIn()`
2. **AuthViewModel** existe et wrappe `AuthStateMachine`
3. **AuthScreen** et **EmailAuthScreen` sont implémentés
4. **WakevNavHost** connecte AuthScreen avec les callbacks

Cependant, le flow ne fonctionne pas car :
- Le callback `launchGoogleSignIn()` dans AuthScreen appelle `authViewModel.onGoogleSignInRequested()` puis `authCallbacks.launchGoogleSignIn()`
- Cela crée une duplication : l'Intent est envoyé au StateMachine ET au Activity simultanément
- Le StateMachine attend un résultat qui ne vient jamais
- L'Activity lance le flow OAuth mais le résultat n'est pas propagé au StateMachine

## What Changes

### Phase 1: Corriger l'architecture AuthScreen ↔ AuthViewModel ↔ MainActivity

**Problème actuel :**
```kotlin
// Dans WakevNavHost.kt - AUTH route
AuthScreen(
    onGoogleSignIn = {
        authViewModel.onGoogleSignInRequested()  // ← Envoie Intent au StateMachine
        authCallbacks.launchGoogleSignIn()        // ← Lance le flow Activity
    },
    // ...
)
```

**Solution proposée :**
```kotlin
// Dans WakevNavHost.kt
AuthScreen(
    onGoogleSignIn = {
        // SEULEMENT lancer le flow Activity
        authCallbacks.launchGoogleSignIn()
    },
    // ...
)

// Dans MainActivity - après résultat OAuth réussi
private fun handleAuthResult(authResult: AuthResult) {
    when (authResult) {
        is AuthResult.Success -> {
            // Propager le résultat au StateMachine
            authViewModel.handleOAuthSuccess(authResult.user, authResult.token)
        }
        // ...
    }
}
```

### Phase 2: Connecter MainActivity OAuth Results vers AuthViewModel

**Modifier MainActivity.kt :**
- Ajouter une référence à `AuthViewModel` (via Koin)
- Après `handleAuthResult()`, appeler une méthode dans AuthViewModel pour mettre à jour le StateMachine
- Créer une méthode `handleOAuthSuccess(user, token)` dans AuthViewModel

**Modifier AuthViewModel.kt :**
- Ajouter une méthode `handleOAuthSuccess(user: User, token: AuthToken)`
- Cette méthode enverra un Intent au StateMachine pour mettre à jour l'état
- Le StateMachine émettra un side effect `NavigateToHome` ou `NavigateToOnboarding`

### Phase 3: Corriger le flow Apple Sign-In

**Problème actuel :**
- Apple Sign-In est marqué comme "non disponible sur Android" avec un toast
- Mais l'implémentation web existe (`AppleSignInWebFlow`)

**Solution proposée :**
- Remplacer le toast par l'appel à `authCallbacks.launchAppleSignIn()`
- Implémenter le flow web complet dans MainActivity
- Gérer le callback deep link `wakeve://apple-auth-callback`
- Propager le résultat à AuthViewModel comme pour Google

### Phase 4: Tests et Validation

- Tester le flow Google Sign-In de bout en bout
- Tester le flow Apple Sign-In (web) de bout en bout
- Tester le flow Email/OTP
- Tester le mode invité (Skip)
- Valider que la navigation fonctionne correctement après auth

## Impact

### Affected Files

| Fichier | Action | Description |
|---------|--------|-------------|
| `WakevNavHost.kt` | MODIFY | Corriger les callbacks dans AuthScreen route |
| `MainActivity.kt` | MODIFY | Connecter OAuth results à AuthViewModel |
| `AuthViewModel.kt` | MODIFY | Ajouter méthode handleOAuthSuccess() |
| `AuthStateMachine.kt` | VERIFY | Vérifier que les Intents existent pour mettre à jour l'état |

### Affected Specs
- `openspec/specs/user-auth/spec.md` (pas de changement, implémentation seulement)

## Design Decisions

### 1. Architecture Révisée

```
┌─────────────────────────────────────────────────────────────────┐
│                         AuthScreen                               │
│                              │                                   │
│                              ▼                                   │
│                     (User taps button)                           │
│                              │                                   │
│                              ▼                                   │
│                   AuthCallbacks.launchGoogleSignIn()             │
│                              │                                   │
│                              ▼                                   │
│              MainActivity.onGoogleSignInClick()                   │
│                              │                                   │
│                              ▼                                   │
│                   AuthService.createGoogleSignInClient()          │
│                              │                                   │
│                              ▼                                   │
│               Google Sign-In Flow (Activity Result)               │
│                              │                                   │
│                              ▼                                   │
│              MainActivity.handleAuthResult(authResult)            │
│                              │                                   │
│                              ▼                                   │
│           AuthViewModel.handleOAuthSuccess(user, token)           │
│                              │                                   │
│                              ▼                                   │
│             AuthStateMachine.dispatch(Intent.SignInSuccess)        │
│                              │                                   │
│                              ▼                                   │
│                SideEffect.NavigateToHome/Onboarding               │
└─────────────────────────────────────────────────────────────────┘
```

### 2. Séparation des Responsabilités

- **AuthScreen** : UI seulement, déclenche les callbacks
- **AuthCallbacks (MainActivity)** : Gère les Activity-level OAuth flows
- **AuthViewModel** : Connecte MainActivity results au StateMachine
- **AuthStateMachine** : Gère l'état d'authentification et émet des side effects

### 3. Apple Sign-In Web Flow

- Utiliser `AppleSignInWebFlow` pour générer l'URL d'autorisation
- Ouvrir l'URL dans un Custom Tab Chrome
- Gérer le callback deep link dans `MainActivity.onNewIntent()`
- Échanger le code contre des tokens via `handleAppleAuthCallback()`
- Propager le résultat à AuthViewModel

## Non-Goals (Out of Scope)

- Implémentation iOS (sera fait séparément)
- Biometric authentication
- Social login supplémentaire (Facebook, Twitter)
- Account linking (merge guest → full account)

## Success Criteria

- [ ] Google Sign-In fonctionne de bout en bout
- [ ] Apple Sign-In (web) fonctionne de bout en bout
- [ ] Email/OTP fonctionne
- [ ] Mode invité (Skip) fonctionne
- [ ] Navigation automatique après auth réussie
- [ ] Navigation vers Onboarding si first-time user
- [ ] Navigation vers Home si user existant
- [ ] Erreurs OAuth affichées clairement
- [ ] Tests passent

## Timeline Estimate

| Phase | Durée Estimée | Priorité |
|-------|---------------|----------|
| Phase 1: Corriger architecture AuthScreen | 30 min | P0 - Critique |
| Phase 2: Connecter MainActivity → AuthViewModel | 1h | P0 - Critique |
| Phase 3: Corriger Apple Sign-In | 1h | P0 - Critique |
| Phase 4: Tests et validation | 1h | P1 - Important |
| **Total** | **3.5h** | |

## References

- [AuthStateMachine Implementation](../../shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/shell/statemachine/AuthStateMachine.kt)
- [AuthViewModel](../../wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/auth/AuthViewModel.kt)
- [MainActivity OAuth Setup](../../wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/MainActivity.kt)
- [WakevNavHost](../../wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakevNavHost.kt)
- [AndroidAuthService](../../shared/src/androidMain/kotlin/com/guyghost/wakeve/auth/shell/services/AndroidAuthService.kt)
