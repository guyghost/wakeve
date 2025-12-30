# Testing Documentation

Documentation complète des tests du projet Wakeve.

## Vue d'ensemble

Ce dossier contient toute la documentation relative aux tests : guides, rapports, stratégies et résultats.

## Quick Start

**Nouveau sur les tests ?** Commencez ici :
- [Quick Start Guide](quick-start.md) - Démarrer avec les tests en 5 minutes
- [Testing Checklist](checklist.md) - Checklist des tests à effectuer

## Structure

### Guides
- [Quick Reference](quick-reference.md) - Référence rapide des commandes
- [Correction Guide](guides/correction-guide.md) - Guide de correction des tests

### Tests par Fonctionnalité

#### Onboarding
- [Overview](onboarding/README.md) - Vue d'ensemble des tests d'onboarding
- [Tests Index](onboarding/tests-index.md) - Index des tests
- [Documentation](onboarding/tests-documentation.md) - Documentation détaillée
- [Quick Start](onboarding/quick-start.md) - Démarrage rapide

#### KMP State Machine
- [Tests Summary](kmp-state-machine-tests.md) - Résumé des tests de la state machine

### Rapports
- [Reports Index](reports/index.md) - Index des rapports de tests
- [Execution Report](reports/execution-report.md) - Rapport d'exécution
- [Analysis Report](reports/analysis-report.md) - Rapport d'analyse
- [Fix Action Plan](reports/fix-action-plan.md) - Plan d'action pour les corrections
- [Fix Results](reports/fix-results.md) - Résultats des corrections

## Stratégie de Tests

### Test-Driven Development (TDD)
Le projet suit le cycle TDD :
1. 🔴 **Red** : Écrire un test qui échoue
2. 🟢 **Green** : Implémenter le minimum pour passer
3. 🔵 **Refactor** : Améliorer le code

### Pyramide des Tests
```
       /\
      /UI\
     /----\
    /Integ.\
   /--------\
  /  Unit    \
 /------------\
```

- **Unit Tests** : Tests unitaires (logique métier)
- **Integration Tests** : Tests d'intégration (interaction composants)
- **UI Tests** : Tests UI (parcours utilisateur)

## Commandes Utiles

```bash
# Tous les tests
./gradlew test

# Tests shared (KMP)
./gradlew shared:test

# Tests Android
./gradlew composeApp:testDebugUnitTest

# Tests avec logs
./gradlew test --info

# Tests en mode debug
./gradlew test --debug-jvm
```

## Couverture Actuelle

Voir les rapports dans [reports/](reports/) pour la couverture détaillée.

## Offline Testing

Tous les services doivent être testés en mode offline :
- Création d'événements offline
- Votes offline
- Synchronisation à la reconnexion
- Résolution de conflits

## CI/CD

Les tests sont exécutés automatiquement :
- Sur chaque commit
- Sur chaque pull request
- Avant chaque merge dans `main`

## Bonnes Pratiques

1. **Tests d'abord** : Écrire les tests avant le code (TDD)
2. **Tests isolés** : Chaque test doit être indépendant
3. **Noms descriptifs** : `test_createEvent_offline_shouldSyncOnReconnection()`
4. **Arrange-Act-Assert** : Structure claire des tests
5. **Mock externe** : Mocker les dépendances externes

## Liens Utiles

- [CONTRIBUTING.md](../../CONTRIBUTING.md) - Guidelines TDD
- [AGENTS.md](../../AGENTS.md) - Workflow OpenSpec avec tests
- [Architecture](../architecture/README.md) - Architecture testable
