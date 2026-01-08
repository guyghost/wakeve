# Tasks: Add Optional Authentication

## 1. Functional Core (Shared)
- [x] 1.1 Créer les modèles Core (`User`, `AuthResult`, `AuthMethod`)
- [x] 1.2 Créer les validateurs purs (`validateEmail`, `validateOTP`, `parseJWT`)
- [x] 1.3 Créer les types d'erreur (`AuthError`, `ValidationError`)

## 2. Imperative Shell - AuthService
- [x] 2.1 Créer l'interface `AuthService` (expect)
- [x] 2.2 Implémenter `AuthService` Android (Google Sign-In, Apple Sign-In)
- [x] 2.3 Implémenter `AuthService` iOS (Apple Sign-In, Google Sign-In)
- [x] 2.4 Créer `EmailAuthService` avec OTP
- [x] 2.5 Créer `GuestModeService` pour mode invité

## 3. State Machine (Auth Flow)
- [x] 3.1 Créer `AuthContract` (Intents, State, SideEffect)
- [x] 3.2 Créer `AuthStateMachine`
- [x] 3.3 Intégrer avec AppState global

## 4. UI - Android (Jetpack Compose)
- [x] 4.1 Créer `AuthScreen` avec boutons Google/Apple/Email
- [x] 4.2 Créer bouton "Passer" en haut à droite
- [x] 4.3 Créer `EmailAuthScreen` pour saisie email + OTP
- [x] 4.4 Styling Material You (couleurs, typographie)
- [x] 4.5 Intégration avec AuthStateMachine

## 5. UI - iOS (SwiftUI)
- [x] 5.1 Créer `AuthView` avec boutons Google/Apple/Email
- [x] 5.2 Créer bouton "Passer" en haut à droite
- [x] 5.3 Créer `EmailAuthView` pour saisie email + OTP
- [x] 5.4 Styling Liquid Glass (glassmorphism)
- [x] 5.5 Intégration avec AuthStateMachine

## 6. Backend API (Ktor)
- [x] 6.1 Créer endpoint `/api/auth/google` (OAuth callback)
- [x] 6.2 Créer endpoint `/api/auth/apple` (OAuth callback)
- [x] 6.3 Créer endpoint `/api/auth/email/request` (send OTP)
- [x] 6.4 Créer endpoint `/api/auth/email/verify` (verify OTP)
- [x] 6.5 Créer endpoint `/api/auth/guest` (create guest session)
- [x] 6.6 Tests API endpoints

## 7. Database & Persistence
- [x] 7.1 Créer schema SQLDelight pour `User` table
- [x] 7.2 Créer `UserRepository` pour persistance locale
- [x] 7.3 Stockage sécurisé des tokens (Keychain/Keystore)

## 8. Tests
- [x] 8.1 Tests unitaires Core (validators, models) - 10 tests attendus | 36 tests créés ✅
- [x] 8.2 Tests unitaires AuthService (mock providers) - 8 tests attendus | 33 tests créés ✅
- [x] 8.3 Tests State Machine - 6 tests attendus | 14 tests créés ✅
- [x] 8.4 Tests UI (Android Compose tests) - 5 tests attendus | 15 tests créés ✅
- [x] 8.5 Tests UI (iOS XCTest) - 5 tests attendus | 15 tests créés ✅
- [x] 8.6 Tests API endpoints - 6 tests attendus | 10 tests créés ✅
- [x] 8.7 Tests offline (guest mode) - 3 tests attendus | 9 tests créés ✅
- [x] 8.8 Tests RGPD (minimisation des données) - 2 tests attendus | 10 tests créés ✅

## 9. Integration
- [x] 9.1 Intégrer AuthScreen avec Onboarding Flow
- [x] 9.2 Mettre à jour navigation après auth/guest
- [x] 9.3 Synchroniser avec événements existants (mode invité vs authentifié)
- [x] 9.4 Tests E2E (login complet flow)

## 10. Documentation
- [x] 10.1 Mettre à jour AGENTS.md (Auth Service)
- [x] 10.2 Documentation API endpoints
- [x] 10.3 Guide d'intégration AuthFlow

## Total Tasks: 48
