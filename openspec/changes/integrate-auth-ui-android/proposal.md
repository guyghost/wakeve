# Change: Integrate Authentication UI on Android

## Status
**ACTIVE** - Prêt pour implémentation

## Why

L'authentification et le mode invité sont **entièrement implémentés dans le shared module** (112 tests passants) mais **ne sont pas du tout connectés à l'UI Android**. Les utilisateurs ne peuvent actuellement pas :
- Se connecter avec Google/Apple/Email
- Utiliser l'application en mode invité (Skip)
- Voir leur état d'authentification
- Se déconnecter proprement

### Problèmes Identifiés

1. **AuthScreen existe mais n'est jamais utilisé** - Le composable `AuthScreen.kt` est complet mais `WakevNavHost` utilise un `LoginScreen` stub
2. **MainActivity a OAuth prêt mais non connecté** - Les méthodes `onGoogleSignInClick()` et `onAppleSignInClick()` existent mais ne sont jamais appelées
3. **Mode invité absent de l'UI** - Le bouton "Skip" existe dans `AuthScreen` mais n'est pas affiché
4. **Navigation non pilotée par AuthStateMachine** - Les side effects ne déclenchent pas de navigation
5. **AuthStateManager non intégré au flow** - L'état d'auth n'influence pas correctement la navigation

## What Changes

### Phase 1: Connexion AuthScreen ↔ Navigation (Critique)
- **REPLACE**: `LoginScreen` par `AuthScreen` dans `WakevNavHost.kt`
- **ADD**: Route `/auth/email` pour `EmailAuthScreen`
- **CONNECT**: Callbacks OAuth de `MainActivity` vers `AuthScreen`
- **WIRE**: Bouton "Skip" → `AuthStateMachine.dispatch(Intent.SkipAuth)`

### Phase 2: AuthViewModel Integration
- **CREATE**: `AuthViewModel` pour gérer l'état UI d'authentification
- **CONNECT**: `AuthViewModel` ↔ `AuthStateMachine`
- **OBSERVE**: Side effects pour déclencher la navigation
- **HANDLE**: Erreurs et états de chargement

### Phase 3: Navigation State-Driven
- **MODIFY**: `App.kt` pour écouter `AuthStateManager.authState`
- **IMPLEMENT**: Navigation automatique selon l'état auth
- **ADD**: Deep linking pour callbacks OAuth
- **TEST**: Flow complet (Guest, Google, Apple, Email)

### Phase 4: Session & Logout
- **IMPLEMENT**: Bouton logout dans `ProfileScreen`/`SettingsScreen`
- **CONNECT**: Logout → `AuthStateMachine.dispatch(Intent.Logout)`
- **HANDLE**: Session restoration au démarrage
- **CLEAR**: Navigation stack après logout

## Impact

### Affected Files

| Fichier | Action | Description |
|---------|--------|-------------|
| `WakevNavHost.kt` | MODIFY | Remplacer LoginScreen par AuthScreen |
| `Screen.kt` | MODIFY | Ajouter route Auth, EmailAuth |
| `App.kt` | MODIFY | Intégrer AuthStateManager pour navigation |
| `MainActivity.kt` | MODIFY | Exposer callbacks OAuth au composable |
| `AuthViewModel.kt` | CREATE | ViewModel pour AuthScreen |
| `SettingsScreen.kt` | MODIFY | Ajouter bouton logout |
| `ProfileScreen.kt` | MODIFY | Afficher état connexion + logout |

### Affected Specs
- `openspec/specs/user-auth/spec.md` (existante)
- `openspec/specs/onboarding/spec.md` (modification flow)

## Design Decisions

### 1. Architecture ViewModel
```
┌─────────────────────────────────────────────────────────────────┐
│                         AuthScreen                               │
│                              │                                   │
│                              ▼                                   │
│                       AuthViewModel                              │
│                              │                                   │
│                              ▼                                   │
│                     AuthStateMachine                             │
│                              │                                   │
│           ┌──────────────────┼──────────────────┐               │
│           ▼                  ▼                  ▼               │
│    GoogleProvider     EmailService      TokenStorage            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2. Flow de Navigation Piloté par État
```kotlin
// Dans App.kt
LaunchedEffect(authState) {
    when (authState) {
        is AuthState.Unauthenticated -> navController.navigate(Screen.Auth.route)
        is AuthState.Authenticated -> {
            if (!hasOnboarded) navController.navigate(Screen.Onboarding.route)
            else navController.navigate(Screen.Home.route)
        }
        is AuthState.Loading -> { /* Show splash */ }
        is AuthState.Error -> { /* Show error */ }
    }
}
```

### 3. Mode Invité (Guest)
- Accessible via bouton "Skip" en haut à droite de AuthScreen
- Crée un `GuestUser` avec UUID unique
- Permissions limitées (pas de sync cloud, pas de notifications push)
- Données stockées localement uniquement
- Possibilité de "upgrade" vers compte complet plus tard

### 4. Gestion des Erreurs OAuth
- Timeout configurable (30s par défaut)
- Retry automatique (1 fois) en cas d'erreur réseau
- Messages d'erreur localisés
- Fallback vers mode invité si OAuth échoue

## Non-Goals (Out of Scope)

- Implémentation iOS (sera fait séparément)
- Biometric authentication (Phase 4)
- Social login (Facebook, Twitter) - Non prévu
- Account linking (merge guest → full account) - Phase 4
- Multi-device session management - Phase 4

## Success Criteria

- [ ] Utilisateur peut se connecter avec Google
- [ ] Utilisateur peut utiliser l'app en mode invité (Skip)
- [ ] Utilisateur peut voir son état de connexion dans Profile
- [ ] Utilisateur peut se déconnecter
- [ ] Session restaurée au redémarrage de l'app
- [ ] Navigation automatique après auth (→ Onboarding ou Home)
- [ ] Erreurs affichées clairement à l'utilisateur
- [ ] Tests UI passants (Espresso)

## Timeline Estimate

| Phase | Durée Estimée | Priorité |
|-------|---------------|----------|
| Phase 1: AuthScreen ↔ Navigation | 2h | P0 - Critique |
| Phase 2: AuthViewModel | 1h | P0 - Critique |
| Phase 3: Navigation State-Driven | 1h | P0 - Critique |
| Phase 4: Session & Logout | 1h | P1 - Important |
| Tests & Polish | 1h | P1 - Important |
| **Total** | **6h** | |

## References

- [AuthStateMachine Implementation](../../shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/shell/statemachine/AuthStateMachine.kt)
- [AuthScreen UI](../../wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/auth/AuthScreen.kt)
- [MainActivity OAuth Setup](../../wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/MainActivity.kt)
- [Previous Proposal (Archived)](./archive/2026-01-08-add-optional-authentication/proposal.md)
