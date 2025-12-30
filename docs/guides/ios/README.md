# iOS Development Guides

Documentation spécifique au développement iOS de Wakeve.

## Vue d'ensemble

La documentation iOS complète se trouve dans le dossier `iosApp/` à la racine du projet. Cette page sert d'index pour naviguer rapidement vers les ressources iOS.

## Documentation iOS Principale

### Design System - Liquid Glass
- [Liquid Glass Guidelines](../../../iosApp/LIQUID_GLASS_GUIDELINES.md) - Guidelines complètes du design system
- [LiquidGlassCard Reference](../../../iosApp/LIQUIDGLASSCARD_REFERENCE.md) - Référence du composant LiquidGlassCard
- [LiquidGlassCard Usage Examples](../../../iosApp/LIQUIDGLASSCARD_USAGE_EXAMPLES.md) - Exemples d'utilisation
- [LiquidGlassCard Implementation](../../../iosApp/README_LIQUIDGLASSCARD.md) - Détails d'implémentation

### Testing
- [Tests Index](../../../iosApp/INDEX_TESTS.md) - Index des tests iOS
- [Testing Guide](../../../iosApp/TESTING_GUIDE.md) - Guide de tests
- [Tests Summary](../../../iosApp/TESTS_SUMMARY.md) - Résumé des tests
- [README Tests](../../../iosApp/README_TESTS.md) - Documentation des tests

### Calendar Integration
- [Calendar Integration Guide](../../../iosApp/CALENDAR_INTEGRATION_GUIDE.md) - Guide d'intégration
- [Calendar Tests Index](../../../iosApp/CALENDAR_TESTS_INDEX.md) - Index des tests calendrier
- [Calendar Tests Summary](../../../iosApp/CALENDAR_INTEGRATION_TESTS_SUMMARY.md) - Résumé des tests
- [Quick Reference](../../../iosApp/CALENDAR_TESTS_QUICK_REFERENCE.md) - Référence rapide

### ViewModels & Architecture
- [ScenarioListView Refactoring](../../../iosApp/SCENARIOLISTVIEW_REFACTORING_SUMMARY.md) - Refactoring ScenarioListView
- [ScenarioListView Before/After](../../../iosApp/SCENARIOLISTVIEW_BEFORE_AFTER.md) - Comparaison avant/après
- [ScenarioListView Migration](../../../iosApp/SCENARIOLISTVIEW_MIGRATION_GUIDE.md) - Guide de migration
- [ScenarioListView Implementation](../../../iosApp/SCENARIOLISTVIEW_IMPLEMENTATION_INDEX.md) - Index d'implémentation
- [ScenarioListViewModel Index](../../../iosApp/SCENARIOLISTVIEWMODEL_INDEX.md) - Index du ViewModel
- [Scenario ViewModel Guide](../../../iosApp/SCENARIO_VIEWMODEL_GUIDE.md) - Guide des ViewModels
- [ViewModel Integration](../../../iosApp/VIEWMODEL_INTEGRATION.md) - Intégration des ViewModels

### Quick Start
- [Start Here](../../../iosApp/START_HERE.md) - Point d'entrée pour iOS
- [Start with LiquidGlassCard](../../../iosApp/START_WITH_LIQUIDGLASSCARD.md) - Démarrer avec LiquidGlassCard
- [Start ScenarioListView Refactor](../../../iosApp/START_SCENARIOLISTVIEW_REFACTOR.md) - Démarrer le refactoring

### Refactoring Guides
- [ScenarioListView Quick Reference](../../../iosApp/SCENARIOLISTVIEW_QUICK_REFERENCE.md) - Référence rapide
- [ScenarioListView Refactoring Complete](../../../iosApp/SCENARIOLISTVIEW_REFACTORING_COMPLETE.md) - Refactoring complet
- [LiquidGlassCard Index](../../../iosApp/LIQUIDGLASSCARD_INDEX.md) - Index LiquidGlassCard
- [LiquidGlassCard Implementation Summary](../../../iosApp/LIQUIDGLASSCARD_IMPLEMENTATION_SUMMARY.md) - Résumé d'implémentation

### Test Configuration
- [Test Configuration](../../../iosApp/TEST_CONFIGURATION.md) - Configuration des tests

## Structure du Code iOS

```
iosApp/
├─ iosApp/
│  ├─ Views/              # SwiftUI Views
│  ├─ Components/         # Composants réutilisables
│  ├─ Services/           # Services iOS (expect/actual)
│  ├─ Theme/              # Liquid Glass theme
│  └─ Assets.xcassets/    # Assets visuels
├─ iosAppUITests/         # UI Tests
└─ Documentation (.md)    # Toute la documentation iOS
```

## Conventions iOS

### SwiftUI
- Utiliser SwiftUI pour toutes les interfaces
- Respecter les guidelines Liquid Glass
- Préférer les ViewModels pour la logique

### Design System
- Liquid Glass pour l'apparence moderne
- Conformité HIG (Human Interface Guidelines)
- Support du mode sombre
- Accessibilité (VoiceOver, Dynamic Type)

### Tests
- XCTest pour les tests unitaires
- XCUITest pour les tests UI
- Tests d'accessibilité obligatoires

## Liens Utiles

- [Migration Documentation](../../migration/liquid-glass/README.md) - Migration Liquid Glass
- [Architecture](../../architecture/README.md) - Architecture KMP
- [Testing](../../testing/README.md) - Documentation des tests
- [Integrations](../../integrations/README.md) - Intégrations (Calendar, etc.)

## Commandes Utiles

```bash
# Ouvrir le projet Xcode
open iosApp/iosApp.xcodeproj

# Build depuis CLI
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp build

# Run tests
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp
```

---

**Note** : Pour toute modification de la documentation iOS, éditer directement les fichiers dans `iosApp/` plutôt que dans `docs/`.
