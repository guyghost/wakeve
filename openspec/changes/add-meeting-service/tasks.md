# Tâches pour Add Meeting Service

## Modèles de Données
- [x] Créer Meeting.kt avec toutes les propriétés
- [x] Créer MeetingUpdates.kt
- [x] Créer MeetingPlatform.kt enum
- [x] Créer MeetingStatus.kt enum
- [x] Créer MeetingReminderTiming.kt enum

## Service Core
- [x] Implémenter MeetingService.kt
- [x] Créer MeetingPlatformProvider interface
- [x] Implémenter MockMeetingPlatformProvider.kt

## Base de Données
- [x] Ajouter table Meeting dans SQLDelight schema
- [x] Créer MeetingRepository.kt pour persistance
- [x] Implémenter les queries SQLDelight

## Tests
- [x] Créer MeetingServiceTest.kt (6 tests minimum)
- [ ] Tests pour génération de liens par plateforme
- [ ] Tests pour validation d'événement confirmé
- [ ] Tests pour rappels et invitations
- [ ] Tests offline pour persistance

## Intégrations
- [ ] Intégrer avec NotificationService pour emails/rappels
- [ ] Gestion des fuseaux horaires
- [ ] Validation des participants acceptés uniquement

## Revue et Validation
- [ ] Revue de code par @review
- [ ] Tests passent (36+ tests)
- [ ] Conformité design system et accessibilité
- [ ] Documentation mise à jour

## Archivage
- [ ] Toutes tâches cochées [x]
- [ ] Archiver avec `openspec archive add-meeting-service --yes`