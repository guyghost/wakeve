# Context: Delete Event Feature

## Objectif
Implémenter la fonctionnalité de suppression d'événements pour l'organisateur, avec cascade delete des données liées et UI de confirmation.

## Contraintes
- Plateforme : KMP (Android + iOS)
- Offline first : Oui (sync metadata pour tombstone)
- Design system : Material You (Android) + Liquid Glass (iOS)

## Décisions Techniques
| Décision | Justification | Agent |
|----------|---------------|-------|
| Transaction SQLite pour cascade | Garantir atomicité | @orchestrator |
| userId dans DeleteEvent intent | Vérification autorisation côté state machine | @orchestrator |
| Soft-block FINALIZED status | Événement terminé ne doit pas être supprimé | @orchestrator |

## Artéfacts Produits
| Fichier | Agent | Status |
|---------|-------|--------|
| | | |

## Notes Inter-Agents
<!-- Format: [@source → @destination] Message -->
