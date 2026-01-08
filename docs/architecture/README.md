# Architecture Documentation

Documentation de l'architecture du projet Wakeve.

## Vue d'ensemble

Wakeve utilise une architecture Kotlin Multiplatform (KMP) avec une séparation claire des responsabilités entre les couches.

## Architecture Globale

```
┌──────────────────────────────────────────────────────────┐
│                   Presentation Layer                      │
│  ┌──────────────────┐          ┌──────────────────┐      │
│  │ Jetpack Compose  │          │    SwiftUI       │      │
│  │   (Android)      │          │     (iOS)        │      │
│  └──────────────────┘          └──────────────────┘      │
├──────────────────────────────────────────────────────────┤
│                   Business Logic Layer                    │
│        (Kotlin Multiplatform - commonMain)                │
│  ┌──────────────────────────────────────────────────┐    │
│  │  EventRepository, PollService, SyncService       │    │
│  └──────────────────────────────────────────────────┘    │
├──────────────────────────────────────────────────────────┤
│                   Persistence Layer                       │
│  ┌──────────────────────────────────────────────────┐    │
│  │  SQLDelight (SQLite) - Source de vérité locale   │    │
│  └──────────────────────────────────────────────────┘    │
├──────────────────────────────────────────────────────────┤
│                   Platform Layer                          │
│  ┌──────────────────┐          ┌──────────────────┐      │
│  │ Android Drivers  │          │   iOS Drivers    │      │
│  │ (expect/actual)  │          │ (expect/actual)  │      │
│  └──────────────────┘          └──────────────────┘      │
└──────────────────────────────────────────────────────────┘
```

## Documents

### Kotlin Multiplatform
- [KMP State Machine](kmp/kmp-state-machine.md) - Machine à états partagée
- [Implementation Summary](kmp/implementation-summary.md) - Résumé de l'implémentation
- [Android Integration](kmp/android-integration.md) - Intégration Android
- [Status](kmp/status.md) - Statut de l'intégration

### ViewModels
- [Implementation Summary](viewmodels/viewmodel-implementation-summary.md) - Implémentation des ViewModels

### Architecture Détaillée
- [ARCHITECTURE.md](ARCHITECTURE.md) - Documentation d'architecture complète

## Principes Architecturaux

### 1. Separation of Concerns
Chaque couche a une responsabilité unique :
- **Presentation** : UI et interactions utilisateur
- **Business Logic** : Logique métier (KMP shared)
- **Persistence** : Stockage local (SQLDelight)
- **Platform** : Intégrations spécifiques plateforme

### 2. Offline-First
- SQLite comme source de vérité locale
- Synchronisation incrémentale avec backend
- Résolution de conflits (last-write-wins → CRDT futur)

### 3. Expect/Actual Pattern
Services spécifiques à la plateforme :
```kotlin
// commonMain
expect class NotificationService {
    fun sendPushNotification(title: String, body: String)
}

// androidMain
actual class NotificationService {
    actual fun sendPushNotification(title: String, body: String) {
        // FCM implementation
    }
}

// iosMain
actual class NotificationService {
    actual fun sendPushNotification(title: String, body: String) {
        // APNs implementation
    }
}
```

### 4. Dependency Injection
Koin pour l'injection de dépendances cross-platform.

## Modules

### shared/
Code partagé Kotlin Multiplatform :
- Domain models
- Repository
- Services
- SQLDelight schemas

### composeApp/
Application Android (Jetpack Compose) :
- Composables UI
- ViewModels Android
- Material You theme

### iosApp/
Application iOS (SwiftUI) :
- SwiftUI Views
- Services iOS
- Liquid Glass theme

### server/
Backend Ktor :
- REST API
- Database
- Business logic serveur

## Design Patterns

- **MVVM** : Model-View-ViewModel pour la présentation
- **Repository Pattern** : Abstraction de la persistance
- **State Machine** : Gestion d'état prédictible
- **Dependency Injection** : Koin pour la configuration
- **Offline-First** : SQLite comme source de vérité

## Liens Utiles

- [ARCHITECTURE.md](ARCHITECTURE.md) - Documentation complète
- [API Documentation](../API.md) - Endpoints REST
- [Testing](../testing/README.md) - Stratégie de tests
