# Context: Integrate Authentication UI on Android

## Objectif
Connecter l'authentification existante (shared module) à l'UI Android pour permettre :
- Connexion Google/Apple/Email
- Mode invité (Skip)
- Logout
- Session restoration

## Contraintes
- Plateforme : Android
- Offline first : Oui (mode invité fonctionnel offline)
- Design system : Material You

## Décisions Techniques

| Décision | Justification | Agent |
|----------|---------------|-------|
| CompositionLocal pour AuthCallbacks | Permet de passer les callbacks OAuth de MainActivity aux composables sans prop drilling | @orchestrator |
| AuthViewModel séparé | Isolation de la logique UI d'auth, réutilisable sur plusieurs écrans | @orchestrator |
| Navigation réactive via LaunchedEffect | Pattern recommandé pour navigation basée sur l'état dans Compose | @orchestrator |
| | | |

## Artéfacts Produits

| Fichier | Agent | Status |
|---------|-------|--------|
| `navigation/AuthCallbacks.kt` | @codegen | pending |
| `ui/auth/AuthViewModel.kt` | @codegen | pending |
| `Screen.kt` (modified) | @codegen | pending |
| `WakevNavHost.kt` (modified) | @codegen | pending |
| `App.kt` (modified) | @codegen | pending |
| `MainActivity.kt` (modified) | @codegen | pending |
| `SettingsScreen.kt` (modified) | @codegen | pending |
| Tests UI Espresso | @tests | pending |
| | | |

## Notes Inter-Agents

<!-- Format: [@source → @destination] Message -->

[@orchestrator → @codegen] Le fichier AuthScreen.kt existe déjà dans commonMain et est complet. Ne pas le recréer, juste l'utiliser dans WakevNavHost.

[@orchestrator → @codegen] MainActivity contient déjà tout le setup OAuth (googleSignInLauncher, providers, etc). Il faut juste exposer les méthodes via l'interface AuthCallbacks.

[@orchestrator → @codegen] AuthStateMachine est dans shared module et fonctionne. L'injecter via Koin dans AuthViewModel.

[@orchestrator → @tests] Tester le flow complet : Auth → Skip → Home et Auth → Google → Home

[@orchestrator → @review] Vérifier que la séparation Core/Shell est maintenue. AuthViewModel est Shell (orchestration), AuthStateMachine est Shell (state machine).
