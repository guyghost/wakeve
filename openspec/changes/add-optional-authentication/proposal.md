# Change: Add Optional Authentication

## Why
Wakeve nécessite une page d'authentification optionnelle pour permettre aux nouveaux utilisateurs de découvrir l'application sans créer de compte. Les utilisateurs peuvent soit s'authentifier avec Google/Apple/email, soit passer cette étape pour accéder directement à l'application en mode invité.

## What Changes
- **NEW**: Page d'authentification optionnelle avec 3 options (Google, Apple, Email)
- **NEW**: Bouton "Passer" en haut à droite pour utiliser l'app sans compte
- **NEW**: Mode invité (guest mode) avec fonctionnalités limitées
- **NEW**: Intégration OAuth (Google Sign-In, Apple Sign-In)
- **NEW**: Système de connexion par email avec OTP
- **MODIFIED**: Onboarding Flow - Auth screen devient la première étape après l'installation
- **MODIFIED**: AppState - Ajout de l'état `isAuthenticated` et `isGuest`

## Impact
- Affected specs: `user-auth` (NEW)
- Affected code:
  - `shared/src/commonMain/kotlin/com/guyghost/wakeve/` - User models, AuthService
  - `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/` - AuthScreen (Android)
  - `iosApp/iosApp/Views/` - AuthView (iOS)
  - `server/src/main/kotlin/com/guyghost/wakeve/` - Auth endpoints

## Design Decisions
- **Functional Core**: Logique d'authentification pure (validation, token parsing)
- **Imperative Shell**: OAuth providers, state machine pour le flux d'auth
- **Offline-first**: Mode invité accessible même offline
- **Privacy**: Respect RGPD (minimisation des données, consentement)

## Non-Goals
- Profil utilisateur complet (Phase 3)
- Gestion des permissions granulaires (Phase 3)
- Rappels de connexion (Phase 3)
