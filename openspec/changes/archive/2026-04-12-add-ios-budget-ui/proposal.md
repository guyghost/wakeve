# Proposal: iOS Budget UI

## Pourquoi

L'application Android dispose d'une UI Budget complète (1167 LOC :
`BudgetOverviewScreen.kt` + `BudgetDetailScreen.kt`). L'iOS affiche uniquement
`"Budget Overview - Coming Soon"` et `"Budget Detail - Coming Soon"`.

La logique métier partagée est 100 % prête :
- `shared/.../budget/BudgetRepository.kt` (494 LOC)
- `shared/.../budget/BudgetCalculator.kt` (495 LOC)
- `shared/.../models/BudgetModels.kt` (Budget, BudgetItem, BudgetCategory, etc.)

Il ne manque que les vues SwiftUI et le ViewModel iOS.

## Quoi

Implémenter les vues SwiftUI de gestion budgétaire pour iOS, en parité
fonctionnelle avec l'Android existant :

| Fichier | Rôle |
|---------|------|
| `iosApp/src/ViewModels/BudgetViewModel.swift` | ViewModel connecté au BudgetRepository KMP |
| `iosApp/src/Views/Budget/BudgetOverviewView.swift` | Vue principale : résumé, catégories, progression |
| `iosApp/src/Views/Budget/BudgetDetailView.swift` | Liste des dépenses, ajout/édition d'items |
| `iosApp/src/Views/Budget/BudgetItemRow.swift` | Composant ligne d'item budgétaire |
| `iosApp/src/Views/Budget/AddBudgetItemSheet.swift` | Sheet d'ajout d'une dépense |
| `iosApp/src/ContentView.swift` | Branchement navigation (remplacer "Coming Soon") |

## Périmètre fonctionnel

### BudgetOverviewView
- Résumé total : estimé vs réel, barre de progression globale
- Breakdown par catégorie (Transport, Hébergement, Repas, Activités, Équipement, Autre)
- Coût par participant
- Solde de chaque participant (qui doit quoi)
- Bouton → BudgetDetailView

### BudgetDetailView
- Liste des BudgetItems groupés par catégorie
- Indicateur payé/non payé par item
- Coût partagé / coût par personne
- FAB → AddBudgetItemSheet
- Swipe-to-delete sur un item

### AddBudgetItemSheet
- Champs : nom, description, catégorie, coût estimé, payé par, partagé par
- Validation avant sauvegarde

## Impact

- **Aucun breaking change** — ajout de fichiers uniquement
- **Shared module** : aucune modification
- **Android** : aucune modification
- **Server** : aucune modification

## Hors périmètre

- Intégration Tricount (payment-management, phase future)
- Synchronisation backend des BudgetItems (sync offline déjà gérée par BudgetRepository)
- Accommodation, Equipment, MealPlanning iOS (futurs changements)
