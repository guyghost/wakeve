# Proposition: Ajouter le Service de Réunions

## Pourquoi ?

L'application Wakeve nécessite un service pour générer et gérer des liens de réunions virtuelles (Zoom, Google Meet, FaceTime) afin de faciliter les interactions entre participants lors de la phase d'organisation d'événements.

## Qu'est-ce qui sera ajouté ?

- **MeetingService**: Service principal pour créer, mettre à jour et annuler des réunions
- **MeetingPlatformProvider**: Interface pour intégrer avec les plateformes de réunion (Zoom, Meet, FaceTime)
- **Modèles de données**: Meeting, MeetingPlatform, etc.
- **Mock provider**: Implémentation mockée pour les tests et développement
- **Tests**: Couverture complète avec tests unitaires

## Impact

- **Fonctionnalité**: Permet aux organisateurs de créer des réunions virtuelles directement depuis l'app
- **Architecture**: Étend le pattern provider pour les intégrations externes
- **UI**: Nécessitera des vues pour créer/gérer les réunions (pas inclus dans cette proposition)
- **Phase**: Phase 4 - Réunions Virtuelles

## Migration

Aucune migration nécessaire, nouvelle fonctionnalité.

## Tests

- Tests unitaires pour MeetingService
- Tests d'intégration avec MockMeetingPlatformProvider
- Tests offline pour persistance

## Risques

- Dépendance aux API externes (Zoom SDK, Google Meet API)
- Gestion des fuseaux horaires pour les rappels
- Sécurité des liens de réunion