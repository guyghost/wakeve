# Context: Fix iOS Compilation Errors

## Objectif
Corriger les erreurs de compilation iOS causées par des redéclarations de composants et des problèmes d'accessibilité des types.

## Contraintes
- Plateforme : iOS (SwiftUI)
- Offline first : non
- Design system : Liquid Glass

## Décisions Techniques
| Décision | Justification | Agent |
|----------|---------------|-------|
| Garder composants dans UIComponents/ | Composants réutilisables doivent être centralisés | @orchestrator |
| Supprimer les déclarations privées dans les vues | Éviter la duplication et utiliser composants partagés | @orchestrator |
| Exposer BadgeType comme type public | Nécessaire pour l'accès depuis plusieurs vues | @orchestrator |

## Artéfacts Produits
| Fichier | Agent | Status |
|---------|-------|--------|
| proposal.md | @orchestrator | completed |
| context-log.jsonl | @orchestrator | completed |
| context.md | @orchestrator | completed |

## Notes Inter-Agents
<!-- Format: [@source → @destination] Message -->

## Erreurs de Compilation Identifiées

### Redéclarations Multiples
1. **FilterChip** (3 déclarations)
   - `iosApp/iosApp/Components/SharedComponents.swift:89`
   - `iosApp/iosApp/Views/InboxView.swift:252`
   - `iosApp/iosApp/Views/BudgetDetailView.swift:841`

2. **LiquidGlassCard** (3 déclarations)
   - `iosApp/iosApp/UIComponents/LiquidGlassCard.swift:14`
   - `iosApp/iosApp/Views/ActivityPlanningView.swift:35`
   - `iosApp/iosApp/Views/ScenarioManagementView.swift:35`

3. **LiquidGlassButton** (2 déclarations)
   - `iosApp/iosApp/UIComponents/LiquidGlassButton.swift:40`
   - `iosApp/iosApp/Views/ScenarioManagementView.swift:72`

4. **LiquidGlassBadge** (2 déclarations)
   - `iosApp/iosApp/UIComponents/LiquidGlassBadge.swift:24`
   - `iosApp/iosApp/Views/ScenarioManagementView.swift:182`

5. **LiquidGlassDivider** (2 déclarations)
   - `iosApp/iosApp/UIComponents/LiquidGlassDivider.swift:12`
   - `iosApp/iosApp/Views/ScenarioManagementView.swift:241`

6. **LiquidGlassListItem** (2 déclarations)
   - `iosApp/iosApp/UIComponents/LiquidGlassListItem.swift:20`
   - `iosApp/iosApp/Views/ScenarioManagementView.swift:277`

7. **EventStatusBadge** (2 déclarations)
   - `iosApp/iosApp/Views/ExploreView.swift:206`
   - `iosApp/iosApp/Views/ModernHomeView.swift:404`

8. **BestSlotCard** (2 déclarations)
   - `iosApp/iosApp/Views/ModernPollResultsView.swift:216`
   - `iosApp/iosApp/Views/PollResultsView.swift:242`

9. **ConfirmedDateCard** (2 déclarations)
   - `iosApp/iosApp/Views/ModernPollResultsView.swift:277`
   - `iosApp/iosApp/Views/PollResultsView.swift:303`

### Problèmes d'Accessibilité BadgeType
1. `iosApp/iosApp/Views/ExploreView.swift:240` - `LiquidGlassBadge.BadgeType`
2. `iosApp/iosApp/Views/ProfileScreen.swift:249` - `LiquidGlassBadge.BadgeType`
3. `iosApp/iosApp/Views/ProfileScreen.swift:384` - `LiquidGlassBadge.BadgeType`
4. `iosApp/iosApp/Views/ScenarioDetailView.swift:480` - `LiquidGlassBadge.BadgeType`

## Stratégie de Correction

1. **Analyser chaque composant dupliqué** et décider :
   - Garder la version dans UIComponents/
   - Mettre à jour les vues pour utiliser le composant partagé
   - Supprimer les déclarations locales

2. **Corriger BadgeType** :
   - Option 1: Rendre BadgeType public dans LiquidGlassBadge
   - Option 2: Créer un type partagé dans SharedModels
   - Option 3: Remplacer par enum ou struct distinct

3. **Tester après chaque correction** :
   - Compiler iOS
   - Vérifier que l'UI fonctionne correctement
