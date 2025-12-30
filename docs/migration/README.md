# Migration Documentation

Documentation des migrations majeures du projet Wakeve.

## Vue d'ensemble

Ce dossier regroupe toutes les migrations significatives effectuées sur le projet, notamment les migrations de design system et d'architecture.

## Migrations Majeures

### Liquid Glass Migration (iOS)
- [Vue d'ensemble](liquid-glass/README.md) - Index complet de la migration
- [Summary](liquid-glass/migration-summary.md) - Résumé de la migration
- [Validation](liquid-glass/validation.md) - Validation et conformité
- [Checklist](liquid-glass/checklist.md) - Checklist finale
- [Report](liquid-glass/report.md) - Rapport détaillé
- [Cards Summary](liquid-glass/cards-summary.md) - Migration des cards
- [Patterns](liquid-glass/event-creation-patterns.md) - Patterns Event Creation
- [ModernView](liquid-glass/modernview.md) - Migration ModernView

### Autres Migrations
- [Event Creation Sheet](event-creation-sheet.md) - Migration EventCreationSheet
- [Equipment Migration](equipment-report.md) - Rapport de migration équipement
- [Complete Files](complete_files.md) - Fichiers migrés

## Processus de Migration

1. **Analyse** - Identifier les composants à migrer
2. **Planification** - Créer un plan avec checklist
3. **Implémentation** - Migration progressive
4. **Validation** - Tests et conformité design system
5. **Documentation** - Documenter les changements
6. **Revue** - Code review et validation finale

## Design Systems

### iOS - Liquid Glass
Le design system Liquid Glass apporte :
- Effets visuels modernes (glassmorphism)
- Animations fluides
- Conformité HIG (Human Interface Guidelines)
- Accessibilité améliorée

Documentation complète : `iosApp/LIQUID_GLASS_GUIDELINES.md`

### Android - Material You
- Material Design 3
- Dynamic color
- Adaptive layouts
- Accessibility

## Liens Utiles

- [iOS Design Guidelines](../../iosApp/LIQUID_GLASS_GUIDELINES.md)
- [Refactoring Documentation](../refactoring/README.md)
- [Testing Documentation](../testing/README.md)
