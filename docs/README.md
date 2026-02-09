# Wakeve - Documentation

Bienvenue dans la documentation technique du projet Wakeve, une application mobile de planification d'événements en Kotlin Multiplatform.

## Navigation Rapide

### Démarrage
- [Guide de démarrage rapide](../QUICK_START.md) - 5 minutes pour démarrer
- [Guide utilisateur](USER_GUIDE.md) - Utilisation de l'application
- [Contribuer au projet](../CONTRIBUTING.md) - Guidelines de développement

### Architecture
- [Architecture Overview](architecture/ARCHITECTURE.md) - Vue d'ensemble de l'architecture
- [Meeting Service Architecture](architecture/meeting-service.md) - Architecture FC&IS pour MeetingService
- [KMP State Machine](architecture/kmp/kmp-state-machine.md) - Machine à états Kotlin Multiplatform
- [ViewModels](architecture/viewmodels/viewmodel-implementation-summary.md) - Implémentation des ViewModels
- [Android Integration](architecture/kmp/android-integration.md) - Intégration Android

### API & Intégrations
- [API REST](API.md) - Documentation des endpoints Ktor
- [Meeting API](api/meeting-api.md) - API MeetingProxy (Zoom, Google Meet)
- [Calendar Integration](integrations/calendar/implementation.md) - Intégration calendrier natif
- [OAuth](integrations/oauth.md) - Authentification OAuth

### Guides de développement
- [Koin Setup](guides/developer/koin-setup.md) - Configuration de l'injection de dépendances
- [KMP Normalization Notes](guides/KMP_NORMALIZATION_NOTES.md) - Mapping des modules et chemins normalisés
- [iOS Development](guides/ios/README.md) - Guides spécifiques iOS (voir aussi `wakeveApp/wakeveApp/`)

### Migration & Refactoring
- [Migration Index](migration/README.md) - Vue d'ensemble des migrations
- [Liquid Glass Migration](migration/liquid-glass/README.md) - Migration vers le design system iOS
- [Refactoring Index](refactoring/README.md) - Documentation des refactorings majeurs
- [Scenario Detail Refactoring](refactoring/scenario-detail/README.md) - Refactoring des vues détails

### Tests
- [Testing Overview](testing/README.md) - Vue d'ensemble des tests
- [E2E Testing Guide](testing/E2E_TESTING_GUIDE.md) - Guide des tests End-to-End (50 tests)
- [Quick Start](testing/quick-start.md) - Démarrer avec les tests
- [Onboarding Tests](testing/onboarding/README.md) - Tests d'onboarding
- [Test Reports](testing/reports/index.md) - Rapports de tests

### Implémentation & Roadmap
- [PRD Status](implementation/prd-status.md) - Statut des fonctionnalités du PRD
- [Feature Integration](implementation/feature-integration.md) - Intégration des nouvelles fonctionnalités
- [Draft Event Wizard](implementation/draft-event-wizard-guide.md) - Documentation UX du wizard de création

### Historique & Sessions
- [Session Summaries](meetings/session-summaries.md) - Résumés des sessions de développement
- [Archive](archive/README.md) - Documents historiques

## Organisation du Projet

```
wakeve/
├─ docs/                    # Documentation technique (vous êtes ici)
│  ├─ architecture/         # Documentation d'architecture
│  ├─ integrations/         # Intégrations externes (calendar, OAuth, etc.)
│  ├─ guides/              # Guides de développement
│  ├─ migration/           # Documentation de migration
│  ├─ refactoring/         # Refactorings majeurs
│  ├─ testing/             # Documentation de tests
│  ├─ implementation/      # Statut d'implémentation
│  ├─ meetings/            # Résumés de sessions
│  └─ archive/             # Documents historiques
├─ wakeveApp/wakeveApp/    # Implémentation iOS SwiftUI
├─ openspec/               # Spécifications OpenSpec
├─ AGENTS.md               # Workflow OpenSpec et agents IA
├─ QUICK_START.md          # Guide de démarrage rapide
├─ CONTRIBUTING.md         # Guidelines de contribution
└─ README.md               # Vue d'ensemble du projet
```

## Documentation iOS

La documentation spécifique à iOS (Liquid Glass design system, guides SwiftUI, tests iOS) se trouve dans le dossier `wakeveApp/wakeveApp/`. Consultez [guides/ios/README.md](guides/ios/README.md) pour l'index complet.

## OpenSpec

Ce projet utilise OpenSpec pour gérer les changements de manière structurée. Consultez [openspec/AGENTS.md](../openspec/AGENTS.md) pour comprendre le workflow de développement piloté par spécifications.

## Conventions

- **Liens relatifs** : Utiliser des liens relatifs pour la navigation interne
- **Nommage** : kebab-case pour les fichiers markdown (ex: `kmp-state-machine.md`)
- **Langue** : Documentation en français pour le projet, code en anglais
- **Mise à jour** : Toujours mettre à jour la documentation lors de changements significatifs

## Besoin d'aide ?

- Consulter [AGENTS.md](../AGENTS.md) pour comprendre le workflow OpenSpec
- Consulter [CONTRIBUTING.md](../CONTRIBUTING.md) pour les guidelines de contribution
- Lire [QUICK_START.md](../QUICK_START.md) pour démarrer rapidement

## Archive

Les documents historiques, anciens rapports de session et résumés sont archivés dans `docs/archive/`. Si vous cherchez un document qui semble manquant, consultez l'archive.

---

**Note**: Cette documentation est maintenue activement. Pour toute question ou suggestion, ouvrir une issue ou une pull request.
