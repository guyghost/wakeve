# Structure du Projet Wakeve

Ce document décrit l'organisation standard du projet Wakeve selon les conventions Kotlin Multiplatform.

## Vue d'Ensemble

```
wakeve/
├── build.gradle.kts              # Configuration racine Gradle
├── settings.gradle.kts           # Configuration des modules
├── gradle/
│   └── libs.versions.toml        # Catalogue de versions centralisé
│
├── composeApp/                   # Application Android & Desktop (Kotlin)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/    # Code partagé Compose
│       ├── commonTest/kotlin/    # Tests UI
│       ├── androidMain/kotlin/   # Spécifique Android
│       ├── androidUnitTest/      # Tests unitaires Android
│       ├── androidInstrumentedTest/  # Tests instrumentés
│       ├── iosMain/kotlin/       # Bridge iOS (Kotlin expect/actual)
│       ├── jvmMain/kotlin/       # Spécifique Desktop
│       └── jvmTest/kotlin/       # Tests JVM
│
├── iosApp/                       # Application iOS native (Swift)
│   ├── iosApp.xcodeproj/         # Projet Xcode
│   ├── src/                      # Code source Swift
│   │   ├── Views/                # Écrans SwiftUI
│   │   ├── ViewModels/           # ViewModels Swift
│   │   ├── Components/           # Composants réutilisables
│   │   ├── Services/             # Services (Auth, DeepLink, etc.)
│   │   ├── Navigation/           # Navigation SwiftUI
│   │   ├── Models/               # Modèles Swift
│   │   ├── Theme/                # Thème Liquid Glass
│   │   ├── UIComponents/         # Composants UI Liquid Glass
│   │   └── ...
│   ├── iosAppUITests/            # Tests UI iOS
│   └── WakeveTest.xctestplan     # Plan de test
│
├── shared/                       # Logique métier partagée (KMP)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/
│       │   ├── kotlin/           # Code métier Kotlin pur
│       │   │   ├── presentation/statemachine/  # StateMachines
│       │   │   ├── repository/   # Repositories
│       │   │   ├── models/       # Modèles de données
│       │   │   └── ...
│       │   └── resources/        # Ressources partagées
│       │       ├── images/
│       │       ├── strings/
│       │       └── MR/           # Multiplatform Resources
│       ├── commonTest/kotlin/    # Tests métier (Kotlin)
│       ├── androidMain/kotlin/   # Implémentation Android
│       ├── androidUnitTest/      # Tests Android
│       ├── androidInstrumentedTest/
│       ├── iosMain/kotlin/       # Implémentation iOS (Kotlin)
│       ├── jvmMain/kotlin/       # Implémentation JVM
│       └── jvmTest/kotlin/       # Tests JVM
│
└── server/                       # Backend Ktor (Kotlin)
    ├── build.gradle.kts
    └── src/
        ├── main/kotlin/          # Routes, services, models
        └── test/kotlin/          # Tests API
```

## Détail par Module

### 1. composeApp/ - Application Android & Desktop

Ce module contient le code UI pour Android et Desktop en Jetpack Compose.

**Source Sets:**
- `commonMain/` - Code Compose partagé entre Android et Desktop
- `androidMain/` - Code spécifique Android (Activity, Services, etc.)
- `jvmMain/` - Code spécifique Desktop
- `iosMain/` - Bridge expect/actual pour accéder au shared depuis iOS (si nécessaire)

**Contenu type:**
```kotlin
// commonMain - Partagé
@Composable
fun HomeScreen(viewModel: EventManagementViewModel) { ... }

// androidMain - Spécifique
class MainActivity : ComponentActivity() { ... }
```

### 2. iosApp/ - Application iOS Native

Ce dossier contient l'application iOS native développée en Swift avec SwiftUI.

**Structure:**
```
iosApp/
├── iosApp.xcodeproj/           # Projet Xcode
├── iosApp/
│   ├── Views/                  # Écrans SwiftUI
│   │   ├── ModernHomeView.swift
│   │   ├── EventDetailView.swift
│   │   └── ...
│   ├── ViewModels/             # ViewModels Swift
│   │   ├── EventListViewModel.swift
│   │   └── ...
│   ├── Components/             # Composants réutilisables
│   ├── Services/               # Services natifs
│   │   ├── DeepLinkService.swift
│   │   └── AuthenticationService.swift
│   └── ...
└── iosAppUITests/              # Tests UI
```

**Caractéristiques:**
- Utilise le framework `Shared` généré par le module `shared`
- Architecture MVVM avec `@ObservableObject`
- Design System Liquid Glass pour iOS 26+

### 3. shared/ - Logique Métier Partagée

Ce module contient tout le code métier partagé entre les plateformes.

**Architecture en couches:**
```
commonMain/
├── presentation/
│   └── statemachine/        # StateMachines (MVI)
│       ├── EventManagementStateMachine.kt
│       ├── AuthStateMachine.kt
│       └── ...
├── repository/              # Accès données
│   ├── EventRepository.kt
│   └── ...
├── models/                  # Modèles de données
│   ├── Event.kt
│   └── ...
└── services/                # Services partagés
    ├── NotificationService.kt
    └── ...
```

**Règles:**
- Pas de dépendance Android/iOS dans `commonMain/`
- Utiliser `expect/actual` pour le code platform-specific
- SQLDelight pour la base de données locale

### 4. server/ - Backend Ktor

API REST développée avec Ktor.

**Structure:**
```
server/src/main/kotlin/
├── routes/                  # Endpoints API
│   ├── EventRoutes.kt
│   └── ...
├── models/                  # DTOs
├── services/                # Logique métier serveur
└── ...
```

## Flux de Données Cross-Platform

```
Android (Compose)              iOS (SwiftUI)
       │                            │
       └──────────┬─────────────────┘
                  │
       ┌──────────▼─────────────────┐
       │   composeApp/src/commonMain │  ← UI Kotlin (Android/Desktop)
       └──────────┬─────────────────┘
                  │
       ┌──────────▼─────────────────┐
       │        shared/              │  ← Logique métier (KMP)
       │  - StateMachines            │
       │  - Repositories             │
       │  - Database                 │
       └──────────┬─────────────────┘
                  │
       ┌──────────▼─────────────────┐
       │       iosApp/src            │  ← UI Swift natif
       └────────────────────────────┘
```

## Conventions de Nommage

### Fichiers Kotlin
- `PascalCase` pour les classes: `EventManagementStateMachine`
- `camelCase` pour les fonctions: `loadEvents()`
- `SCREAMING_SNAKE_CASE` pour les constantes: `MAX_RETRY_COUNT`

### Fichiers Swift
- `PascalCase` pour les structs/classes: `EventListViewModel`
- `camelCase` pour les fonctions: `loadEvents()`
- Suffixes des ViewModels: `...ViewModel`
- Suffixes des Views: `...View` ou `...Screen`

### Packages/Packages
- Kotlin: `com.guyghost.wakeve.<feature>.<layer>`
- Swift: Organisation par dossiers fonctionnels

## Build & Exécution

### Android
```bash
./gradlew :composeApp:assembleDebug
```

### iOS
Ouvrir `iosApp/iosApp.xcodeproj` dans Xcode et build.

### Desktop
```bash
./gradlew :composeApp:run
```

### Backend
```bash
./gradlew :server:run
```

## Tests

### Tests Partagés
```bash
./gradlew :shared:test
```

### Tests Android
```bash
./gradlew :composeApp:test
./gradlew :composeApp:connectedAndroidTest
```

### Tests iOS
Via Xcode: `Cmd+U` sur le projet `iosApp.xcodeproj`

## Ressources

### Ressources Partagées (shared/src/commonMain/resources/)
```
resources/
├── images/                 # Images partagées (PNG, WebP)
├── strings/               # Chaînes XML
└── MR/                    # Multiplatform Resources
    ├── base/              # Langue par défaut
    ├── en/
    ├── fr/
    └── ...
```

### Ressources Android (composeApp/src/androidMain/res/)
- Utiliser le système de ressources Android standard
- Drawable, mipmap, values, etc.

### Ressources iOS (iosApp/src/)
- `Assets.xcassets/` pour les images
- `Localizable.strings` pour les traductions

## Notes Importantes

1. **Ne pas mélanger les codes:**
   - Swift → uniquement dans `iosApp/`
   - Kotlin UI → uniquement dans `composeApp/`
   - Kotlin métier → uniquement dans `shared/`

2. **Communication entre modules:**
   - `iosApp` utilise le framework `Shared` généré par `shared`
   - `composeApp` dépend du module `shared` comme dépendance Gradle

3. **Database:**
   - SQLDelight génère du code pour toutes les plateformes
   - Schémas dans `shared/src/commonMain/sqldelight/`

---

*Dernière mise à jour: Février 2026*
