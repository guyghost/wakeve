# Spec Delta: Budget Management (iOS)

## ADDED Requirements

### Requirement: Vue d'aperçu budgétaire iOS
L'application iOS MUST afficher une vue d'aperçu du budget d'un événement,
accessible depuis l'écran de détail de l'événement. Le système SHALL utiliser
le BudgetRepository KMP partagé comme unique source de vérité.

#### Scenario: Affichage du résumé budgétaire
- **WHEN** l'utilisateur navigue vers le budget d'un événement
- **THEN** le montant total estimé est affiché
- **AND** le montant total réel (dépensé) est affiché
- **AND** une barre de progression globale indique le taux d'utilisation
- **AND** le coût estimé par participant est calculé et affiché

#### Scenario: Breakdown par catégorie
- **WHEN** la vue d'aperçu est affichée
- **THEN** chaque catégorie (Transport, Hébergement, Repas, Activités, Équipement, Autre)
  affiche son montant estimé, réel, et une barre de progression
- **AND** les catégories à 0€ sont masquées si aucun item n'y est associé

#### Scenario: Soldes des participants
- **WHEN** des items budgétaires sont partagés entre participants
- **THEN** le solde de chaque participant est affiché (montant dû vs montant payé)
- **AND** un indicateur visuel distingue "doit de l'argent" vs "se fait rembourser"

### Requirement: Vue de détail budgétaire iOS
L'application iOS MUST afficher la liste complète des dépenses d'un budget,
groupées par catégorie. Elle SHALL permettre l'ajout et la suppression de dépenses.

#### Scenario: Liste des items par catégorie
- **WHEN** l'utilisateur ouvre la vue de détail
- **THEN** les BudgetItems sont groupés par BudgetCategory
- **AND** chaque item affiche : nom, coût estimé, coût réel, statut payé/non payé

#### Scenario: Ajout d'une dépense
- **WHEN** l'utilisateur tape le bouton d'ajout
- **THEN** une sheet s'ouvre avec les champs : nom, catégorie, coût estimé, payé par
- **AND** après validation, l'item est sauvegardé via BudgetRepository
- **AND** la vue se met à jour immédiatement

#### Scenario: Suppression d'une dépense
- **WHEN** l'utilisateur swipe-to-delete sur un item
- **THEN** une confirmation est demandée
- **AND** après confirmation, l'item est supprimé via BudgetRepository
