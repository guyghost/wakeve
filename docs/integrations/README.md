# Integrations Documentation

Documentation des intégrations externes du projet Wakeve.

## Vue d'ensemble

Ce dossier contient la documentation des intégrations avec des services externes et des APIs natives.

## Intégrations Disponibles

### Calendar Integration
- [Implementation](calendar/implementation.md) - Implémentation de l'intégration calendrier
- [Tests](calendar/tests.md) - Tests de l'intégration
- [Tests Index](calendar/tests-index.md) - Index des tests calendrier

L'intégration calendrier permet :
- Génération d'invitations ICS conformes RFC 5545
- Ajout/mise à jour/suppression d'événements dans calendriers natifs
- Support Android (CalendarContract) et iOS (EventKit)
- Gestion des fuseaux horaires
- Rappels natifs

**Status** : ✅ Implémenté sur Android et iOS

### OAuth Authentication
- [OAuth Implementation](oauth.md) - Authentification OAuth

Support prévu :
- Google OAuth
- Apple Sign In
- Tokens sécurisés

**Status** : 🚧 Phase 3 (planifié)

## Intégrations Futures

### Notifications Push
- FCM (Android)
- APNs (iOS)
- Rappels programmés

**Status** : 🚧 Phase 3 (planifié)

### Transport Providers
- Calcul de routes multi-participants
- Intégration providers de transport
- Points de rencontre optimisés

**Status** : 🚧 Phase 3 (planifié)

### Payment & Tricount
- Cagnotte collaborative
- Intégration Tricount
- Répartition des coûts

**Status** : 🚧 Phase 4 (planifié)

## Architecture des Intégrations

Les intégrations utilisent le pattern `expect/actual` de KMP :

```kotlin
// shared/src/commonMain/kotlin
expect class CalendarService {
    suspend fun addEventToCalendar(event: CalendarEvent): Result<Unit>
}

// shared/src/androidMain/kotlin
actual class CalendarService {
    actual suspend fun addEventToCalendar(event: CalendarEvent): Result<Unit> {
        // Android CalendarContract implementation
    }
}

// shared/src/iosMain/kotlin
actual class CalendarService {
    actual suspend fun addEventToCalendar(event: CalendarEvent): Result<Unit> {
        // iOS EventKit implementation (via Kotlin/Native)
    }
}
```

## Principes

1. **Abstraction** : Interface commune dans `commonMain`
2. **Platform-specific** : Implémentations `actual` par plateforme
3. **Error Handling** : Gestion d'erreurs uniforme (Result<T>)
4. **Permissions** : Vérification des permissions runtime
5. **Testing** : Tests unitaires + instrumented tests

## Tests

Chaque intégration doit avoir :
- Tests unitaires (shared/commonTest)
- Tests Android (androidInstrumentedTest)
- Tests iOS (XCTest)

## Liens Utiles

- [Architecture](../architecture/README.md) - Architecture KMP
- [Testing](../testing/README.md) - Documentation des tests
- [CALENDAR_GUIDE.md](../CALENDAR_GUIDE.md) - Guide d'intégration calendrier détaillé
