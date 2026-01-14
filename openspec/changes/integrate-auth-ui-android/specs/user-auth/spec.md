# Spec Delta: User Authentication UI Integration (Android)

## ADDED Requirements

### Requirement: Auth Screen Navigation Integration

Le système DOIT afficher `AuthScreen` comme premier écran pour les utilisateurs non authentifiés.

#### Scenario: User launches app without session
- **GIVEN** l'utilisateur n'a pas de session active
- **WHEN** l'application démarre
- **THEN** l'écran `AuthScreen` est affiché
- **AND** les options Google, Email et Skip sont visibles

#### Scenario: User has valid session
- **GIVEN** l'utilisateur a une session valide stockée
- **WHEN** l'application démarre
- **THEN** l'utilisateur est redirigé vers `HomeScreen`
- **AND** l'écran `AuthScreen` n'est pas affiché

#### Scenario: User has expired session
- **GIVEN** l'utilisateur a une session expirée
- **WHEN** l'application démarre
- **THEN** le système tente de rafraîchir le token
- **AND** si le refresh échoue, l'utilisateur est redirigé vers `AuthScreen`

---

### Requirement: Skip Authentication (Guest Mode)

Le système DOIT permettre aux utilisateurs de passer l'authentification et utiliser l'application en mode invité.

#### Scenario: User taps Skip button
- **GIVEN** l'utilisateur est sur `AuthScreen`
- **WHEN** l'utilisateur appuie sur le bouton "Skip" (en haut à droite)
- **THEN** un `GuestUser` est créé avec un UUID unique
- **AND** l'utilisateur est redirigé vers `OnboardingScreen` (si première utilisation) ou `HomeScreen`
- **AND** les données sont stockées localement uniquement

#### Scenario: Guest user limitations
- **GIVEN** l'utilisateur est en mode invité
- **THEN** la synchronisation cloud est désactivée
- **AND** les notifications push sont désactivées
- **AND** un badge "Invité" est affiché dans le profil

---

### Requirement: Google Sign-In Integration

Le système DOIT permettre l'authentification via Google Sign-In sur Android.

#### Scenario: Successful Google Sign-In
- **GIVEN** l'utilisateur est sur `AuthScreen`
- **WHEN** l'utilisateur appuie sur "Se connecter avec Google"
- **THEN** le flow Google Sign-In est lancé
- **AND** après authentification réussie, l'utilisateur est redirigé vers `HomeScreen`
- **AND** le token est stocké de façon sécurisée

#### Scenario: Google Sign-In cancelled
- **GIVEN** l'utilisateur a lancé Google Sign-In
- **WHEN** l'utilisateur annule le flow
- **THEN** l'utilisateur reste sur `AuthScreen`
- **AND** un message "Connexion annulée" est affiché

#### Scenario: Google Sign-In error
- **GIVEN** l'utilisateur a lancé Google Sign-In
- **WHEN** une erreur se produit (réseau, configuration)
- **THEN** l'utilisateur reste sur `AuthScreen`
- **AND** un message d'erreur explicite est affiché
- **AND** l'utilisateur peut réessayer

---

### Requirement: Email OTP Authentication

Le système DOIT permettre l'authentification via email avec code OTP.

#### Scenario: Request OTP
- **GIVEN** l'utilisateur est sur `EmailAuthScreen`
- **WHEN** l'utilisateur entre son email et appuie sur "Envoyer le code"
- **THEN** un code OTP est envoyé à l'email
- **AND** l'écran passe en mode "saisie OTP"
- **AND** un timer de validité (5 min) est affiché

#### Scenario: Verify OTP success
- **GIVEN** l'utilisateur a reçu un OTP valide
- **WHEN** l'utilisateur entre le code correct
- **THEN** l'authentification réussit
- **AND** l'utilisateur est redirigé vers `HomeScreen`

#### Scenario: OTP expired
- **GIVEN** l'utilisateur a un OTP expiré
- **WHEN** l'utilisateur entre le code
- **THEN** un message "Code expiré" est affiché
- **AND** le bouton "Renvoyer le code" est activé

---

### Requirement: Logout Functionality

Le système DOIT permettre aux utilisateurs de se déconnecter.

#### Scenario: Logout from Settings
- **GIVEN** l'utilisateur est authentifié
- **WHEN** l'utilisateur appuie sur "Se déconnecter" dans Settings
- **THEN** la session est supprimée
- **AND** les tokens sont effacés du stockage sécurisé
- **AND** l'utilisateur est redirigé vers `AuthScreen`
- **AND** la pile de navigation est effacée

#### Scenario: Logout in guest mode
- **GIVEN** l'utilisateur est en mode invité
- **WHEN** l'utilisateur appuie sur "Se déconnecter"
- **THEN** les données locales du guest sont supprimées
- **AND** l'utilisateur est redirigé vers `AuthScreen`

---

### Requirement: Auth State Display

Le système DOIT afficher l'état d'authentification dans l'interface utilisateur.

#### Scenario: Display authenticated user info
- **GIVEN** l'utilisateur est authentifié
- **WHEN** l'utilisateur accède à `ProfileScreen` ou `SettingsScreen`
- **THEN** le nom/email de l'utilisateur est affiché
- **AND** l'avatar (si disponible) est affiché

#### Scenario: Display guest mode indicator
- **GIVEN** l'utilisateur est en mode invité
- **WHEN** l'utilisateur accède à `ProfileScreen`
- **THEN** un badge "Mode invité" est affiché
- **AND** un bouton "Créer un compte" est visible

---

## MODIFIED Requirements

### Requirement: App Navigation Flow (Modified)

**Avant**: L'app affichait GetStarted → Login (stub) → Onboarding → Home

**Après**: L'app vérifie l'état d'authentification et navigue de façon réactive :
- Pas de session → Auth
- Session valide + pas onboarded → Onboarding
- Session valide + onboarded → Home

#### Scenario: Navigation based on auth state
- **GIVEN** l'application démarre
- **WHEN** l'état d'authentification est déterminé
- **THEN** la navigation est déclenchée automatiquement selon l'état
- **AND** la pile de navigation est nettoyée pour éviter le retour arrière vers Auth

---

## REMOVED Requirements

### Requirement: LoginScreen (Stub)

**Reason**: Remplacé par `AuthScreen` qui est complètement fonctionnel.

**Migration**: Supprimer `LoginScreen` composable et les références dans `WakevNavHost`. Utiliser `AuthScreen` à la place.

### Requirement: GetStarted as Auth Entry Point

**Reason**: `GetStarted` devient optionnel (branding screen) et n'est plus requis dans le flow principal.

**Migration**: `AuthScreen` devient le point d'entrée pour les utilisateurs non authentifiés. `GetStarted` peut être conservé comme écran de bienvenue optionnel ou supprimé.
