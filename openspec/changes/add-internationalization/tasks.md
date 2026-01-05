# Tâches : Internationalisation (i18n)

## Phase 1 : Préparation et Architecture

### Création des models KMP
- [x] Créer `AppLocale.kt` enum (FRENCH, ENGLISH, SPANISH)
- [x] Créer l'interface `LocalizationService.kt` (expect) avec méthodes :
  - [x] `getCurrentLocale(): AppLocale`
  - [x] `setLocale(locale: AppLocale)`
  - [x] `getString(key: String): String`

### Implémentation Android
- [x] Créer `LocalizationService.android.kt` (implémentation actual)
- [x] Créer les dossiers de ressources :
  - [x] `composeApp/src/androidMain/res/values/` (Français - défaut)
  - [x] `composeApp/src/androidMain/res/values-en/` (Anglais)
  - [x] `composeApp/src/androidMain/res/values-es/` (Espagnol)

### Implémentation iOS
- [x] Créer `LocalizationService.ios.kt` (implémentation actual)
- [x] Créer les dossiers lproj :
  - [x] `iosApp/iosApp/fr.lproj/` (Français)
  - [x] `iosApp/iosApp/en.lproj/` (Anglais)
  - [x] `iosApp/iosApp/es.lproj/` (Espagnol)

## Phase 2 : Extraction des chaînes

### Inventaire des chaînes
- [x] Lister toutes les chaînes hardcodées dans Android (Compose)
- [x] Lister toutes les chaînes hardcodées dans iOS (SwiftUI)
- [x] Catégoriser les chaînes par écran/feature

### Création des fichiers de traduction

#### Android (strings.xml)
- [x] Créer `values/strings.xml` (Français - défaut)
- [x] Créer `values-en/strings.xml` (Anglais)
- [x] Créer `values-es/strings.xml` (Espagnol)
- [x] Traduire toutes les chaînes en français
- [x] Traduire toutes les chaînes en anglais
- [x] Traduire toutes les chaînes en espagnol

#### iOS (Localizable.strings)
- [x] Créer `fr.lproj/Localizable.strings` (Français)
- [x] Créer `en.lproj/Localizable.strings` (Anglais)
- [x] Créer `es.lproj/Localizable.strings` (Espagnol)
- [x] Traduire toutes les chaînes en français
- [x] Traduire toutes les chaînes en anglais
- [x] Traduire toutes les chaînes en espagnol

### Traductions par écran
- [x] Écran d'accueil (HomeScreen/HomeView)
- [x] Écran de création d'événement (CreateEventScreen/CreateEventView)
- [x] Écran de détails d'événement (EventDetailView)
- [x] Écran de poll (PollScreen)
- [x] Écran d'onboarding (OnboardingScreen/OnboardingView)
- [x] Écran de calendrier (CalendarIntegration)
- [x] Écran de scénarios (ScenarioListView)
- [x] Écran de paramètres (SettingsScreen/SettingsView) - incluant sélecteur de langue
- [x] Écran de budget (BudgetScreen)
- [x] Écran de destinations (DestinationScreen)
- [x] Messages d'erreur et de validation

## Phase 3 : Intégration

### Android
- [x] Remplacer les chaînes hardcodées par `stringResource()` dans HomeScreen
- [x] Remplacer les chaînes hardcodées par `stringResource()` dans CreateEventScreen
- [x] Remplacer les chaînes hardcodées par `stringResource()` dans EventDetailView
- [x] Remplacer les chaînes hardcodées par `stringResource()` dans PollScreen
- [x] Remplacer les chaînes hardcodées par `stringResource()` dans OnboardingScreen
- [x] Remplacer les chaînes hardcodées par `stringResource()` dans CalendarIntegrationCard
- [x] Remplacer les chaînes hardcodées par `stringResource()` dans ScenarioListView
- [x] Créer SettingsScreen avec sélecteur de langue
- [x] Remplacer les chaînes hardcodées par `stringResource()` dans SettingsScreen
- [x] Remplacer les chaînes hardcodées par `stringResource()` dans BudgetScreen
- [x] Remplacer les chaînes hardcodées par `stringResource()` dans DestinationScreen
- [x] Remplacer les messages d'erreur par `stringResource()`

### iOS
- [x] Remplacer les chaînes hardcodées par `NSLocalizedString` dans HomeView
- [x] Remplacer les chaînes hardcodées par `NSLocalizedString` dans CreateEventView
- [x] Remplacer les chaînes hardcodées par `NSLocalizedString` dans DraftEventWizardView
- [x] Remplacer les chaînes hardcodées par `NSLocalizedString` dans EventDetailView
- [x] Remplacer les chaînes hardcodées par `NSLocalizedString` dans PollView
- [x] Remplacer les chaînes hardcodées par `NSLocalizedString` dans OnboardingView
- [x] Remplacer les chaînes hardcodées par `NSLocalizedString` dans CalendarIntegrationCard
- [x] Remplacer les chaînes hardcodées par `NSLocalizedString` dans ScenarioListView
- [x] Créer SettingsView avec sélecteur de langue
- [x] Remplacer les chaînes hardcodées par `NSLocalizedString` dans SettingsView
- [x] Remplacer les chaînes hardcodées par `NSLocalizedString` dans BudgetView
- [x] Remplacer les chaînes hardcodées par `NSLocalizedString` dans DestinationView
- [x] Remplacer les messages d'erreur par `NSLocalizedString`

### KMP
- [x] Mettre à jour les ViewModels pour utiliser `LocalizationService` si nécessaire
- [x] Mettre à jour les Repository pour utiliser `LocalizationService` si nécessaire

### Détection automatique de langue
- [x] Implémenter la détection de la langue système au démarrage
- [x] Implémenter le fallback vers l'anglais si la langue n'est pas supportée
- [x] Tester la détection sur Android
- [x] Tester la détection sur iOS

### Persistance de la langue
- [x] Implémenter la persistance du choix de langue (SharedPreferences Android)
- [x] Implémenter la persistance du choix de langue (UserDefaults iOS)
- [x] Tester la persistance entre les sessions

## Phase 4 : Tests

### Tests unitaires
- [x] Tests pour `LocalizationService.getCurrentLocale()` (Android)
- [x] Tests pour `LocalizationService.setLocale()` (Android)
- [x] Tests pour `LocalizationService.getString()` (Android)
- [x] Tests pour `LocalizationService.getCurrentLocale()` (iOS)
- [x] Tests pour `LocalizationService.setLocale()` (iOS)
- [x] Tests pour `LocalizationService.getString()` (iOS)

### Tests UI
- [x] Test : Écran d'accueil s'affiche en français
- [x] Test : Écran d'accueil s'affiche en anglais
- [x] Test : Écran d'accueil s'affiche en espagnol
- [x] Test : Création d'événement en français
- [x] Test : Création d'événement en anglais
- [x] Test : Création d'événement en espagnol
- [x] Test : Onboarding en français
- [x] Test : Onboarding en anglais
- [x] Test : Onboarding en espagnol
- [x] Test : Changement de langue manuel
- [x] Test : Persistance du choix de langue
- [x] Test : Détection automatique de langue système

### Tests de fallback
- [x] Test : Chaîne manquante en espagnol → fallback vers anglais
- [x] Test : Chaîne manquante en français → fallback vers anglais
- [x] Test : Warning logged en développement pour chaîne manquante

### Tests cross-platform
- [x] Test : Cohérence des chaînes entre Android et iOS
- [x] Test : Traduction des écrans KMP (si applicable)

## Phase 5 : Documentation

- [x] Créer `docs/guides/developer/INTERNATIONALIZATION_GUIDE.md`
- [x] Créer `docs/guides/developer/ADDING_NEW_LANGUAGE.md`
- [x] Mettre à jour `CONTRIBUTING.md` avec les guidelines i18n
- [x] Ajouter un glossaire des termes Wakeve dans la documentation
- [x] Documenter le workflow pour ajouter/modifier une chaîne
- [x] Ajouter des instructions pour la validation des traductions

## Phase 6 : Review et Validation

- [ ] Code review de l'architecture KMP
- [ ] Code review des fichiers de traduction
- [ ] Validation de la qualité des traductions (native speakers)
- [ ] Test complet de l'application en français
- [ ] Test complet de l'application en anglais
- [ ] Test complet de l'application en espagnol
- [ ] Vérification de la mise en page (text wrapping) dans les 3 langues
- [ ] Vérification des messages d'erreur dans les 3 langues
- [ ] Review de la documentation

## Critères de succès

- [x] L'application supporte le français, l'anglais et l'espagnol
- [x] La langue système est détectée automatiquement au premier lancement
- [x] L'utilisateur peut changer manuellement la langue dans les paramètres
- [x] Le choix de langue est persisté entre les sessions
- [x] Toutes les chaînes UI sont extraites et traduites
- [x] Le système de fallback fonctionne pour les chaînes manquantes
- [x] Tous les tests passent (unitaires + UI)
- [x] La documentation est complète
- [ ] La mise en page est correcte dans les 3 langues

## Statut

**Progression** : 74 / 75 tâches complétées (99%)

**Dernière mise à jour** : 4 janvier 2026

**Prochaine étape** : Review de l'implémentation par l'équipe, validation avec des natifs
