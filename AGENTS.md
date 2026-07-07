<!-- OPENSPEC:START -->
# OpenSpec Instructions

Ces instructions sont destinées aux assistants IA travaillant sur ce projet.

Ouvrez systématiquement `@/openspec/AGENTS.md` lorsque la demande:
- Mentionne une planification ou des propositions (termes tels que proposition, spécification, changement, plan)
- Introduit de nouvelles fonctionnalités, des changements importants, des modifications d'architecture ou des travaux majeurs sur les performances/la sécurité
- Semble ambiguë et nécessite la spécification officielle avant de coder

Utilisez `@/openspec/AGENTS.md` pour apprendre:
- Comment créer et appliquer des propositions de changement
- Le format et les conventions de la spécification
- La structure et les directives du projet

Conservez ce bloc géré afin que la commande «openspec update» puisse actualiser les instructions.

<!-- OPENSPEC:END -->

# AGENTS.md - Guide pour les Développeurs et Agents IA

Ce document définit les agents (humains et logiciels) pour l'application mobile de planification d'événements "Wakeve" en Kotlin Multiplatform. Il décrit leurs responsabilités, interfaces et handoffs, ainsi que le workflow de développement piloté par spécifications.

---

## 🤖 Workflow OpenSpec - Développement Piloté par Spécifications

Ce projet utilise **OpenSpec** pour gérer les changements de manière structurée et traçable. Tous les agents IA et développeurs doivent suivre ce workflow pour les nouvelles fonctionnalités significatives.

### Quand utiliser OpenSpec ?

**✅ Créer une proposition OpenSpec pour :**
- Nouvelles fonctionnalités ou capacités
- Changements architecturaux
- Modifications breaking changes
- Optimisations de performance (qui changent le comportement)
- Modifications de sécurité
- Ajout d'agents logiciels (Suggestions, Transport, etc.)

**❌ Pas besoin d'OpenSpec pour :**
- Corrections de bugs (restaurer le comportement prévu)
- Typos, formatage, commentaires
- Mises à jour de dépendances (non-breaking)
- Changements de configuration mineurs
- Tests pour comportement existant

### Gate Product Excellence

Pour toute proposition OpenSpec significative et visible par l'utilisateur, consulter `openspec/specs/product-excellence/spec.md` lorsqu'elle existe. La proposition doit expliquer en quoi le changement:
- aide directement un groupe privé à préparer, décider, coordonner ou finaliser un événement;
- réduit la charge mentale ou les allers-retours hors de Wakeve;
- rend clair ce qui est confirmé, en attente, qui doit agir et la prochaine action utile;
- reste rapide, compréhensible et utilisable sur mobile;
- évite la dérive vers un réseau social, un chat générique, un gestionnaire de tâches, un calendrier ou un workspace générique.

Une fonctionnalité qui ne satisfait pas ce gate doit être rejetée, différée ou rescopée avant implémentation.

### Cycle de Vie OpenSpec

#### 1. **Proposition** (`/openspec-proposal`)

Quand l'utilisateur demande une nouvelle fonctionnalité :

1. **Explorer le contexte existant**
   ```bash
   openspec spec list --long    # Liste des spécifications existantes
   openspec list                 # Changements actifs
   ```

2. **Créer la structure**
   ```
   openspec/changes/<change-id>/
   ├── proposal.md              # Pourquoi, quoi, impact
   ├── tasks.md                 # Checklist d'implémentation
   ├── design.md                # Décisions techniques (optionnel)
   └── specs/
       └── <capability>/
           └── spec.md          # Delta des spécifications
   ```

3. **Choisir un change-id unique**
   - Format : kebab-case, verbe-led
   - Exemples : `add-user-authentication`, `add-transport-optimization`, `update-poll-logic`

4. **Rédiger les deltas de spécifications**
   ```markdown
   ## ADDED Requirements
   ### Requirement: Nouvelle Fonctionnalité
   Le système DOIT fournir...
   
   #### Scenario: Cas de succès
   - **WHEN** l'utilisateur effectue une action
   - **THEN** résultat attendu
   
   ## MODIFIED Requirements
   ### Requirement: Fonctionnalité Existante
   [Texte complet du requirement modifié]
   
   ## REMOVED Requirements
   ### Requirement: Ancienne Fonctionnalité
   **Reason**: [Pourquoi la suppression]
   **Migration**: [Comment gérer]
   ```

5. **Valider la proposition**
   ```bash
   openspec validate <change-id> --strict
   ```

6. **Attendre l'approbation** avant de démarrer l'implémentation

#### 2. **Implémentation** (`/openspec-apply`)

Une fois les specs validées :

1. **Lire la proposition**
   - `proposal.md` : Comprendre le contexte et les objectifs
   - `design.md` : Décisions techniques (si présent)
   - `tasks.md` : Liste des tâches à accomplir

2. **Implémenter séquentiellement**
   - Déléguer chaque tâche à l'agent approprié (voir section Agents IA)
   - Marquer `[x]` dans `tasks.md` au fur et à mesure

3. **S'assurer de la complétude**
   - Tests créés et passants
   - Documentation mise à jour
   - Tests offline/online validés

#### 3. **Archivage** (`/openspec-archive`)

Quand toutes les tâches sont terminées :

1. **Vérifier que tout est complet**
   - Tous les tests passent (36/36 tests minimum)
   - Toutes les tâches sont cochées `[x]`

2. **Archiver le changement**
   ```bash
   openspec archive <change-id> --yes
   ```

3. **Résultat**
   - Les specs delta sont mergées dans `openspec/specs/`
   - Le changement est déplacé vers `openspec/archive/YYYY-MM-DD-<change-id>/`

### Commandes OpenSpec Rapides

```bash
# Lister les changements actifs
openspec list

# Lister les spécifications existantes
openspec list --specs
openspec spec list --long

# Afficher un changement ou une spec
openspec show <change-id>
openspec show <spec-id> --type spec

# Valider un changement
openspec validate <change-id> --strict

# Archiver un changement (après déploiement)
openspec archive <change-id> --yes
```

---

## 🤝 Agents IA - Délégation et Spécialisation

Ce projet utilise plusieurs agents IA spécialisés. L'**orchestrator** (agent principal) délègue les tâches aux agents appropriés.

### Agents Disponibles

| Agent | Rôle | Invocation | Peut écrire du code |
|-------|------|------------|---------------------|
| **orchestrator** | Agent principal, coordination OpenSpec | (par défaut) | ❌ Délègue uniquement |
| **@codegen** | Génération de code Kotlin/Swift/TypeScript | `@codegen` | ✅ Oui |
| **@tests** | Création et exécution de tests (Kotlin test) | `@tests` | ✅ Oui |
| **@review** | Revue de code, design et accessibilité | `@review` | ❌ Read-only |
| **@docs** | Documentation technique | `@docs` | ✅ Oui |
| **@designer** | Analyse d'images UI (Liquid Glass/Material) | `@designer` | ❌ Analyse uniquement |

### Matrice de Délégation par Stack

| Type de tâche | Agent | Livrable |
|---------------|-------|----------|
| **Kotlin Multiplatform (Shared)** |
| Domain models | `@codegen` | `shared/src/commonMain/kotlin/**/*.kt` |
| Repository | `@codegen` | `shared/src/commonMain/kotlin/repository/*.kt` |
| Services (expect/actual) | `@codegen` | `shared/src/{commonMain,androidMain,iosMain}/*.kt` |
| SQLDelight schema | `@codegen` | `shared/src/commonMain/sqldelight/**/*.sq` |
| Tests unitaires | `@tests` | `shared/src/commonTest/kotlin/**/*.kt` |
| **Android (Jetpack Compose)** |
| Composables UI | `@codegen` | `composeApp/src/commonMain/kotlin/**/*.kt` |
| ViewModels | `@codegen` | `composeApp/src/commonMain/kotlin/viewmodel/*.kt` |
| Material Theme | `@codegen` | `composeApp/src/commonMain/kotlin/theme/*.kt` |
| **iOS (SwiftUI)** |
| Views SwiftUI | `@codegen` | `iosApp/src/Views/*.swift` |
| Services iOS | `@codegen` | `iosApp/src/Services/*.swift` |
| Liquid Glass theme | `@codegen` | `iosApp/src/Theme/*.swift` |
| **Backend (Ktor)** |
| Routes API | `@codegen` | `server/src/main/kotlin/**/*.kt` |
| Tests API | `@tests` | `server/src/test/kotlin/**/*.kt` |
| **Web (React/TypeScript)** |
| Composants React | `@codegen` | `webApp/src/components/**/*.tsx` |
| **Documentation & Design** |
| Documentation technique | `@docs` | `docs/**/*.md`, `README.md` |
| Analyse UI (screenshot) | `@designer` | Feedback design system |
| Revue de code/design/a11y | `@review` | Feedback uniquement |

### Règles de Délégation

1. **L'orchestrator ne code jamais** - Il délègue toujours à `@codegen`
2. **Test-Driven Development** - Déléguer à `@tests` AVANT l'implémentation
3. **Design System obligatoire** - Utiliser `@designer` pour valider conformité Material/Liquid Glass
4. **Demander une review** - Utiliser `@review` avant de considérer une tâche terminée
5. **Mettre à jour tasks.md** - L'orchestrator met à jour la progression dans OpenSpec
6. **Offline-first** - Toujours tester les scénarios offline avec `@tests`

### Exemple de Workflow Complet

```
Utilisateur: "Ajoute l'agent Transport pour optimiser les voyages multi-participants"

Orchestrateur:
1. Crée une proposition OpenSpec: "add-transport-agent"
   - proposal.md: Contexte, objectifs, impact
   - tasks.md: Checklist d'implémentation
   - specs/transport-management/spec.md: Delta des spécifications

2. Délègue à @tests: "Crée les tests pour TransportService et TransportProvider"
   - shared/src/commonTest/kotlin/services/TransportServiceTest.kt

3. Délègue à @codegen: "Implémente TransportService dans shared"
   - shared/src/commonMain/kotlin/services/TransportService.kt
   - shared/src/commonMain/kotlin/providers/TransportProvider.kt
    - shared/src/androidMain/kotlin/platform/AndroidTransportProvider.kt
    - shared/src/iosMain/kotlin/platform/IosTransportProvider.kt

    4. Délègue à @codegen: "Crée l'UI Android pour afficher les options de transport"
    - composeApp/src/commonMain/kotlin/ui/TransportOptionsScreen.kt

    5. Délègue à @codegen: "Crée l'UI iOS pour afficher les options de transport"
    - iosApp/src/Views/TransportOptionsView.swift

6. Délègue à @tests: "Ajoute les tests offline pour TransportService"
   - shared/src/commonTest/kotlin/offline/OfflineTransportTest.kt

7. Délègue à @review: "Valide conformité design system et accessibilité"
   - Feedback sur Material You (Android) et Liquid Glass (iOS)

8. Met à jour tasks.md avec [x]

9. Archive le changement avec `openspec archive add-transport-agent --yes`
```

---

## 👥 Agents Humains et Logiciels

### Agents Humains

#### Organisateur
**Responsabilités:**
- Crée l'événement, description, règles de sondage
- Définit la date limite de vote et autorisations d'ajout de dates
- Valide la date retenue; déclenche accès aux détails pour les validés

**Interactions:**
- → Agent Sondage : Création de sondage, date limite, règles
- ← Agent Sondage : Recommandation du meilleur créneau
- → Agent Notifications : Déclenchement des rappels

#### Participant
**Responsabilités:**
- Vote sur les créneaux; peut proposer des dates si autorisé
- Accède aux détails complets uniquement s'il a validé la date retenue
- Contribue à la destination, logement, activités, transport

**Interactions:**
- → Agent Sondage : Votes et propositions de créneaux
- ← Agent Suggestions : Recommandations personnalisées
- ← Agent Calendrier : Invitations ICS
- ← Agent Notifications : Notifications push

---

### Agents Logiciels

#### Agent Sondage & Calendrier
**Responsabilités:**
- Normalise fuseaux horaires; évite overlaps
- Calcule meilleur créneau avec scoring: YES=2, MAYBE=1, NO=-1
- Verrouille le créneau retenu après échéance
- Supporte des créneaux flexibles via `TimeSlot` et `timeOfDay` (Matin, Après-midi, etc.)
- Notifie les agents dépendants

**Implémentation:**
- `shared/src/commonMain/kotlin/services/PollService.kt`
- `shared/src/commonMain/kotlin/domain/PollLogic.kt`
- Tests: `shared/src/commonTest/kotlin/PollLogicTest.kt` (6 tests)

**Interactions:**
- ← Organisateur : Configuration du sondage
- ← Participants : Votes
- → Agents (Suggestions/Calendrier/Transport) : Créneau verrouillé

#### Agent Suggestions (Phase 3 - Planifié)
**Responsabilités:**
- Analyse préférences utilisateur (jours, heures, lieux, activités)
- Utilise `EventType` pour personnaliser les recommandations selon le type d'événement
- Génère recommandations personnalisées avec scoring
- Supporte A/B testing pour optimisation

**Implémentation prévue:**
- `shared/src/commonMain/kotlin/services/SuggestionService.kt`
- `shared/src/commonMain/kotlin/ml/RecommendationEngine.kt`

**Interactions:**
- ← Agent Sondage : Créneau verrouillé
- → Participants : Recommandations personnalisées

#### Agent Calendrier (Phase 6 - Implémenté)
**Responsabilités:**
- Génère invitations ICS conformes à RFC 5545 (TZ-aware DTSTART/DTEND, VALARM reminders)
- Ajoute, met à jour et supprime des événements dans les calendriers natifs (Android CalendarContract, iOS EventKit)
- Gère les fuseaux horaires, UID d'événements, et listes d'invités (attendees)
- Propose des rappels via ICS VALARM and integrates with NotificationService for native reminders (planned enhancements)

**Implémentation (fichiers clés):**
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/calendar/CalendarService.kt` — logique centrale, génération ICS, méthodes add/update/delete
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/calendar/Models.kt` — CalendarEvent, ICSDocument, MeetingReminderTiming
- `shared/src/androidMain/kotlin/com/guyghost/wakeve/calendar/PlatformCalendarService.android.kt` — Android actual implementation (CalendarContract, runtime permission checks)
- `shared/src/iosMain/kotlin/com/guyghost/wakeve/calendar/PlatformCalendarService.ios.kt` — iOS actual implementation bridging to EventKit
- `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/CalendarIntegrationCard.kt` — Compose UI card exposing `onAddToCalendar` and `onShareInvite`
- `iosApp/src/Views/CalendarIntegrationCard.swift` — SwiftUI card calling the shared CalendarService via Kotlin/Native interop
- `server/src/main/kotlin/com/guyghost/wakeve/routes/CalendarRoutes.kt` — server endpoints for ICS generation and download

**Tests:**
- Shared unit tests: `shared/src/commonTest/kotlin/com/guyghost/wakeve/calendar/CalendarServiceTest.kt` (ICS content, timezone, platform result handling)
- Android instrumented tests: `composeApp/src/androidInstrumentedTest/kotlin/com/guyghost/wakeve/ui/event/CalendarIntegrationInstrumentedTest.kt` (runtime permission and add/update/delete flows)
- iOS XCTest: `iosApp/iosAppUITests/CalendarIntegrationUITests.swift` (permission prompts and UI wiring)

**Interactions:**
- ← Agent Sondage : Créneau confirmé
- → Agent Notifications : reminders (via NotificationService integration in future)
- → Participants validés : Invitations ICS / Native calendar entries

**Status:**
- ✅ Implemented on Android (Phase 4) and iOS (Phase 5)
- Tests added for Phase 4.6 (Android) and Phase 5.6 (iOS)

#### Agent Notifications (Phase 3 - Planifié)
**Responsabilités:**
- Envoie notifications push (FCM pour Android, APNs pour iOS)
- Gère tokens d'appareils et permissions
- Supporte rappels programmés et confirmations

**Implémentation prévue:**
- `shared/src/commonMain/kotlin/services/NotificationService.kt`
- `shared/src/androidMain/kotlin/platform/FCMService.kt`
- `shared/src/iosMain/kotlin/platform/APNsService.kt`

**Interactions:**
- → Tous : Notifications push pour événements clés

#### Agent Transport (Phase 3 - Planifié)
**Responsabilités:**
- Calcule routes optimisées multi-participants (coût/temps/équilibré)
- Utilise `expectedParticipants` + `PotentialLocation` pour planifier le transport
- Intègre providers de transport (vols, trains, etc.)
- Planifie points de rencontre pour groupes

**Implémentation prévue:**
- `shared/src/commonMain/kotlin/services/TransportService.kt`
- `shared/src/commonMain/kotlin/providers/TransportProvider.kt`

**Interactions:**
- ← Agent Sondage : Créneau et destinations
- → Participants : Plans de transport optimisés

#### Agent Destination & Logement (Phase 3 - Planifié)
**Responsabilités:**
- Fournit liste courte de destinations et hébergements
- Utilise `PotentialLocation` comme base de recherche
- Score multi-critères: coût, accessibilité, préférences, saisonnalité
- Providers mockés puis réels via backend

**Implémentation prévue:**
- `shared/src/commonMain/kotlin/services/DestinationService.kt`
- `shared/src/commonMain/kotlin/providers/LodgingProvider.kt`

**Interactions:**
- → Participants : Suggestions classées de destinations/hébergements

#### Agent Réunions (Phase 3 - Planifié)
**Responsabilités:**
- Génère liens des réunions virtuelles (Zoom/Meet/FaceTime)
- Place invites pour les validés
- Ajoute rappels; respecte fuseaux horaires

**Implémentation prévue:**
- `shared/src/commonMain/kotlin/services/MeetingService.kt`

**Interactions:**
- ← Agent Sondage : Date confirmée
- → Participants validés : Liens et rappels

#### Agent Paiement & Tricount (Phase 4 - Planifié)
**Responsabilités:**
- Crée cagnotte via providers externes
- Intègre Tricount pour répartition des coûts
- Affiche objectifs et avances

**Implémentation prévue:**
- `shared/src/commonMain/kotlin/services/PaymentService.kt`
- `shared/src/commonMain/kotlin/integrations/TricountClient.kt`

**Interactions:**
- → Tous : Cagnotte, suivi des coûts, liens Tricount

#### Agent Sync & Offline
**Responsabilités:**
- Source de vérité locale (SQLite via SQLDelight)
- Sync incrémentale avec backend
- Stratégie de conflits: last-write-wins + timestamp (évolutif vers CRDT)
- Signale clairement l'état offline/online

**Implémentation:**
- `shared/src/commonMain/kotlin/repository/DatabaseEventRepository.kt`
- `shared/src/commonMain/kotlin/cache/OfflineCache.kt`
- Tests: `shared/src/commonTest/kotlin/OfflineScenarioTest.kt` (7 tests)

**Interactions:**
- ↔ Tous les agents : Persistance, sync, résolution de conflits

#### Agent Sécurité & Auth (Phase 3 - Implémenté)
**Responsabilités:**
- Auth via OAuth (Apple, Google) et Email (OTP)
- Tokens stockés de manière sécurisée (Keychain iOS, Keystore Android)
- Mode invité (guest mode) avec fonctionnalités limitées
- Minimisation des données, droit à l'effacement (RGPD)
- Gestion des sessions et restoration automatique

**Implémentation:**
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/core/` - Functional Core (modèles, validateurs purs)
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/shell/statemachine/AuthStateMachine.kt` - State Machine pour le flux d'auth
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/shell/services/` - Services OAuth, Email, Guest, TokenStorage
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/app/AppState.kt` - Intégration avec l'état global de l'app
- `server/src/main/kotlin/com/guyghost/wakeve/routes/AuthRoutes.kt` - API endpoints backend

**Tests:**
- Tests unitaires Core: 36 tests (validators, models)
- Tests unitaires Shell: 33 tests (services)
- Tests State Machine: 14 tests
- Tests API endpoints: 10 tests
- Tests offline: 9 tests
- Tests RGPD: 10 tests

**Interactions:**
- → Tous : Gestion de l'authentification et des permissions
- → Agent Notifications : Déclenchement des rappels après auth
- → Agent Sync : Mode sync (online) vs mode offline (guest)

---

## 🎨 Design System et Architecture

### Principes de Design Cross-Platform

Wakeve utilise un design system unifié décrit dans `.opencode/design-system.md`:

- **Android**: Material You + Jetpack Compose
- **iOS**: Liquid Glass + SwiftUI (iOS 16+)
- **Web**: TailwindCSS (à définir)

#### Validation du Design System

**Avant d'implémenter un composant UI:**
1. Vérifier la conformité dans `.opencode/design-system.md`
2. Utiliser `@designer` pour analyser les screenshots/maquettes
3. Implémenter selon les guidelines de la plateforme
4. Demander `@review` pour valider accessibilité et design

### Architecture Kotlin Multiplatform

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

### Pattern expect/actual

Services spécifiques à la plateforme utilisent le mécanisme `expect/actual`:

```kotlin
// commonMain
expect class NotificationService {
    fun sendPushNotification(title: String, body: String)
}

// androidMain
actual class NotificationService {
    actual fun sendPushNotification(title: String, body: String) {
        // Implémentation FCM
    }
}

// iosMain
actual class NotificationService {
    actual fun sendPushNotification(title: String, body: String) {
        // Implémentation APNs
    }
}
```

### State Machine Workflow Coordination

Wakeve utilise une architecture **MVI (Model-View-Intent) avec Finite State Machines (FSM)** pour gérer le cycle de vie des événements à travers plusieurs phases coordonnées.

#### Pattern Repository-Mediated Communication

Les state machines communiquent **indirectement via un repository partagé**, sans communication directe entre elles.

```kotlin
// State Machine 1: Met à jour le status dans le repository
eventStateMachine.dispatch(Intent.ConfirmDate("event-1", "slot-1"))
// → Repository: Event.status = CONFIRMED

// State Machine 2: Lit le status depuis le repository
val event = eventRepository.getEvent("event-1")
val canCreate = when (event?.status) {
    EventStatus.CONFIRMED, EventStatus.COMPARING -> true
    else -> false
}
```

**Avantages:**
- ✅ **Couplage faible** entre state machines
- ✅ **Cohérence forte** via repository partagé
- ✅ **Tests simples** (mock repository uniquement)
- ✅ **Source de vérité claire** (Event.status)

#### DRAFT Phase - Event Creation Wizard

La **DRAFT Phase** est la première étape de création d'un événement. Elle utilise un wizard en 4 étapes pour guider l'organisateur à travers la configuration de l'événement.

**Structure du Wizard DRAFT:**

```
┌──────────────────────────────────┐
│       Step 1: Basic Info         │
│  • Title (required)              │
│  • Description (required)        │
│  • Event Type (optional)         │
│  • Custom Type if OTHER          │
└──────────────────────────────────┘
              ↓ (auto-save)
┌──────────────────────────────────┐
│    Step 2: Participants Est.     │
│  • Min Participants (opt)        │
│  • Max Participants (opt)        │
│  • Expected Participants (opt)   │
└──────────────────────────────────┘
              ↓ (auto-save)
┌──────────────────────────────────┐
│    Step 3: Potential Locations   │
│  • Add Location (optional)       │
│  • Remove Location               │
└──────────────────────────────────┘
              ↓ (auto-save)
┌──────────────────────────────────┐
│       Step 4: Time Slots         │
│  • Add Time Slot (required)      │
│  • Remove Time Slot              │
│  • Time of Day selection         │
└──────────────────────────────────┘
              ↓ (validation + StartPoll)
         Event(POLLING)
```

**Key Features:**

- **Auto-save**: Données sauvegardées à chaque transition d'étape
- **Validation stricte**: Navigation bloquée si l'étape actuelle est invalide
- **Navigation flexible**: L'utilisateur peut revenir à l'étape précédente
- **Persistance offline**: Données stockées localement en SQLite
- **Recovery**: Si l'utilisateur ferme l'app, il peut continuer plus tard

**Intents DRAFT:**

```kotlin
// Step 1: Basic Info
Intent.CreateEvent(
    title: String,
    description: String,
    eventType: EventType = OTHER,
    eventTypeCustom: String? = null
)

// Step 2: Participants
Intent.UpdateDraftEvent(
    event = event.copy(
        minParticipants = 5,
        maxParticipants = 20,
        expectedParticipants = 12
    )
)

// Step 3: Locations
Intent.AddPotentialLocation(location: PotentialLocation)
Intent.RemovePotentialLocation(locationId: String)

// Step 4: Time Slots
Intent.AddTimeSlot(timeSlot: TimeSlot)
Intent.RemoveTimeSlot(slotId: String)

// Completion
Intent.StartPoll(eventId: String)  // DRAFT → POLLING
```

**Validation Rules by Step:**

| Step | Field | Rule |
|------|-------|------|
| **1** | title | Non-empty, trimmed (required) |
| **1** | description | Non-empty, trimmed (required) |
| **1** | eventType | Valid enum or CUSTOM (optional) |
| **1** | eventTypeCustom | Non-empty if eventType==CUSTOM (conditional) |
| **2** | minParticipants | Positive integer (optional) |
| **2** | maxParticipants | Positive integer (optional) |
| **2** | expectedParticipants | Positive integer (optional) |
| **2** | Constraint | max >= min (if both provided) |
| **3** | locations | Can be empty, but recommended to add ≥1 |
| **4** | timeSlots | At least 1 required (required) |
| **4** | timeOfDay | Valid enum value (required per slot) |

**Side Effects:**

- `UpdateDraftEvent` → Auto-save to repository
- `StartPoll` → Navigate to poll view + mark event as POLLING
- Navigation errors → ShowError side effect with message
- Validation errors → ShowToast with field-specific feedback

**Documentation:**

Pour une intégration complète, consulter:
- [DRAFT Wizard Usage Guide](./docs/guides/DRAFT_WIZARD_USAGE.md) - Guide d'utilisation du wizard Android/iOS
- [State Machine Integration Guide](./docs/guides/STATE_MACHINE_INTEGRATION_GUIDE.md) - Guide d'intégration MVI + FSM
- [Migration Guide: EventCreationScreen → DraftEventWizard](./docs/migration/EVENTCREATIONSCREEN_TO_DRAFTEVENTWIZARD.md) - Guide de migration
- [Workflow Coordination Specification](./openspec/specs/workflow-coordination/spec.md) - Spécification complète du workflow DRAFT

#### Workflow Complet: DRAFT → FINALIZED

```
Event(DRAFT) 
  → StartPoll 
  → Event(POLLING)
  → ConfirmDate 
  → Event(CONFIRMED) + scenariosUnlocked + NavigateTo("scenarios/$id")
  → [User creates scenarios]
  → SelectScenarioAsFinal (optional)
  → Event(CONFIRMED) + NavigateTo("meetings/$id")
  → TransitionToOrganizing 
  → Event(ORGANIZING) + meetingsUnlocked
  → [User creates meetings]
  → MarkAsFinalized 
  → Event(FINALIZED)
```

#### State Machines et Responsabilités

| State Machine | Responsabilité | Intents Clés |
|---------------|----------------|--------------|
| **EventManagementStateMachine** | Gestion du cycle de vie de l'événement | StartPoll, ConfirmDate, TransitionToOrganizing, MarkAsFinalized |
| **ScenarioManagementStateMachine** | Gestion des scénarios de destination/hébergement | CreateScenario, VoteScenario, SelectScenarioAsFinal |
| **MeetingServiceStateMachine** | Gestion des réunions virtuelles | CreateMeeting, GenerateMeetingLink |

#### Navigation Side Effects

Les transitions de status **émettent des side effects de navigation** pour guider l'utilisateur:

```kotlin
// EventManagementStateMachine.kt
private fun handleConfirmDate(eventId: String, slotId: String) {
    // Mettre à jour le status
    repository.updateEvent(
        eventId = eventId,
        status = EventStatus.CONFIRMED,
        finalDate = date,
        scenariosUnlocked = true
    )
    
    // Émettre la navigation
    emitSideEffect(NavigateTo("scenarios/$eventId"))
}
```

**Side effects de navigation:**
- `ConfirmDate` → `NavigateTo("scenarios/{eventId}")`
- `SelectScenarioAsFinal` → `NavigateTo("meetings/{eventId}")`
- `TransitionToOrganizing` → `NavigateTo("meetings/{eventId}")`

#### Business Rules et Guards

Chaque transition est **protégée par des guards** qui vérifient l'EventStatus:

```kotlin
// Exemple: StartPoll guard
if (event.status != EventStatus.DRAFT) {
    emitSideEffect(ShowError("Cannot start poll: Event not in DRAFT status"))
    return
}

// Exemple: canCreateScenarios helper
fun State.canCreateScenarios(): Boolean {
    return eventStatus in listOf(EventStatus.COMPARING, EventStatus.CONFIRMED)
}
```

**Règles métier par EventStatus:**

| EventStatus | Scénarios Autorisés | Réunions Autorisées | Actions Possibles |
|-------------|---------------------|---------------------|-------------------|
| DRAFT | ❌ | ❌ | CreateEvent, StartPoll |
| POLLING | ❌ | ❌ | Vote, ConfirmDate |
| CONFIRMED | ✅ | ❌ | CreateScenario, TransitionToOrganizing |
| COMPARING | ✅ | ❌ | VoteScenario, SelectScenarioAsFinal |
| ORGANIZING | ❌ | ✅ | CreateMeeting, MarkAsFinalized |
| FINALIZED | ❌ | ❌ | (Read-only) |

#### Fichiers Clés

**Contracts:**
```
shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state/
├── EventManagementContract.kt      # Intents, State, SideEffect pour EventManagement
├── ScenarioManagementContract.kt   # Intents, State, SideEffect pour ScenarioManagement
└── MeetingManagementContract.kt    # Intents, State, SideEffect pour MeetingService
```

**State Machines:**
```
shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/
├── EventManagementStateMachine.kt      # Gestion cycle de vie événement
└── ScenarioManagementStateMachine.kt   # Gestion scénarios
```

**Tests:**
```
shared/src/commonTest/kotlin/com/guyghost/wakeve/
├── presentation/statemachine/
│   ├── EventManagementStateMachineTest.kt    # 13 tests unitaires
│   └── ScenarioManagementStateMachineTest.kt # Tests helpers
└── workflow/
    └── WorkflowIntegrationTest.kt             # 6 tests d'intégration
```

#### Documentation Complète

Pour plus de détails, consulter:
- **[WORKFLOW_DIAGRAMS.md](openspec/changes/verify-statemachine-workflow/WORKFLOW_DIAGRAMS.md)**: Diagrammes séquence et état
- **[TROUBLESHOOTING.md](openspec/changes/verify-statemachine-workflow/TROUBLESHOOTING.md)**: Guide de résolution de problèmes
- **[INDEX.md](openspec/changes/verify-statemachine-workflow/INDEX.md)**: Navigation dans la documentation du changement

#### Exemple d'Implémentation

```kotlin
// 1. Créer un événement (DRAFT)
eventStateMachine.dispatch(Intent.CreateEvent(title, description))

// 2. Démarrer le poll (DRAFT → POLLING)
eventStateMachine.dispatch(Intent.StartPoll(eventId))

// 3. Confirmer la date (POLLING → CONFIRMED)
eventStateMachine.dispatch(Intent.ConfirmDate(eventId, slotId))
// Side effect: NavigateTo("scenarios/$eventId")

// 4. Créer des scénarios (CONFIRMED/COMPARING)
scenarioStateMachine.dispatch(Intent.CreateScenario(eventId, destination, lodging))

// 5. Optionnel: Sélectionner scénario final
scenarioStateMachine.dispatch(Intent.SelectScenarioAsFinal(eventId, scenarioId))
// Side effect: NavigateTo("meetings/$eventId")

// 6. Transition vers organisation (CONFIRMED → ORGANIZING)
eventStateMachine.dispatch(Intent.TransitionToOrganizing(eventId))
// Side effect: NavigateTo("meetings/$eventId")

// 7. Créer des réunions (ORGANIZING)
meetingStateMachine.dispatch(Intent.CreateMeeting(eventId, platform))

// 8. Finaliser l'événement (ORGANIZING → FINALIZED)
eventStateMachine.dispatch(Intent.MarkAsFinalized(eventId))
```

#### Stratégie de Tests

**Tests Unitaires** (EventManagementStateMachineTest.kt):
- Test de chaque Intent handler individuellement
- Validation des transitions de status
- Test des guards et cas d'erreur
- **Couverture**: 13 tests, 100% passing

**Tests d'Intégration** (WorkflowIntegrationTest.kt):
- Test de coordination cross-state-machine
- Validation du pattern repository-mediated communication
- Test du workflow complet (DRAFT → FINALIZED)
- Validation de l'application des règles métier
- **Couverture**: 6 tests, 100% passing

**Commandes de test:**
```bash
# Tests unitaires state machine
./gradlew shared:jvmTest --tests "*EventManagementStateMachineTest*"

# Tests d'intégration workflow
./gradlew shared:jvmTest --tests "*WorkflowIntegrationTest*"
```

---

## 🔄 Git Flow - Workflow de Développement

Ce projet utilise **Trunk-Based Development** avec une branche unique `main`.

### Principes

- **Branche unique**: `main` contient toujours le code stable
- **Commits fréquents**: Petits commits incrémentaux
- **Feature flags**: Pour les grandes fonctionnalités non terminées
- **CI/CD**: Tests automatiques sur chaque commit

### Workflow Standard

```bash
# 1. Synchroniser avec main
git checkout main
git pull origin main

# 2. Créer une branche de travail courte durée (optionnel)
git checkout -b feature/add-transport-agent

# 3. Développement TDD
# - Écrire les tests
# - Implémenter la fonctionnalité
# - Valider les tests

# 4. Commits fréquents avec Conventional Commits
git add .
git commit -m "feat: add TransportService for multi-participant optimization"

# 5. Merger rapidement dans main
git checkout main
git merge feature/add-transport-agent
git push origin main

# 6. Supprimer la branche de travail
git branch -d feature/add-transport-agent
```

---

## 📝 Conventional Commits

**OBLIGATOIRE** : Tous les commits doivent suivre la spécification [Conventional Commits](https://www.conventionalcommits.org/).

### Format

```
<type>[scope optionnel]: <description>

[corps optionnel]

[footer(s) optionnel(s)]
```

### Types de Commits pour Wakeve

- **feat**: Nouvelle fonctionnalité
  ```
  feat: add TransportService for route optimization
  feat(poll): implement weighted voting algorithm
  feat(ios): add Liquid Glass theme for event cards
  ```

- **fix**: Correction de bug
  ```
  fix: resolve timezone conflict in poll calculation
  fix(android): correct Material You color scheme
  fix(offline): handle sync conflict resolution
  ```

- **test**: Ajout ou modification de tests
  ```
  test: add offline scenario tests for EventRepository
  test(poll): add edge cases for voting logic
  ```

- **refactor**: Refactorisation du code
  ```
  refactor: simplify PollLogic scoring algorithm
  refactor(db): optimize SQLDelight queries
  ```

- **docs**: Documentation uniquement
  ```
  docs: update AGENTS.md with transport agent
  docs(api): add Ktor endpoints documentation
  ```

- **style**: Changements de style (formatage)
  ```
  style: apply Kotlin code style with ktlint
  style(ios): align SwiftUI views with Liquid Glass
  ```

- **perf**: Amélioration des performances
  ```
  perf: optimize event list rendering in Compose
  perf(db): add indexes for faster queries
  ```

- **chore**: Maintenance, configuration
  ```
  chore: update Kotlin to 2.2.20
  chore(deps): bump SQLDelight version
  ```

### Breaking Changes

Pour les changements incompatibles, ajouter `!` après le type :

```
feat!: change EventStatus enum values

BREAKING CHANGE: EventStatus.COMPARING renamed to EventStatus.SCENARIO_COMPARISON.
Clients must update their status checks.
```

### Exemples Complets

```
feat(transport): add multi-participant route optimization

Implements TransportService to calculate optimal routes for groups:
- Cost-optimized routing
- Time-optimized routing
- Balanced routing (cost + time)

Includes mock provider for testing and expect/actual pattern for
Android/iOS platform-specific implementations.

Closes #45
```

```
test(offline): add comprehensive offline sync scenarios

Adds 7 new tests for offline functionality:
- Create event offline
- Vote offline
- Sync on reconnection
- Conflict resolution (last-write-wins)

All tests passing (36/36).

Related to openspec/changes/add-offline-sync
```

---

## ✅ Test-Driven Development (TDD)

### Principe Obligatoire

**TOUJOURS** écrire les tests AVANT l'implémentation:

1. 🔴 **Red**: Écrire un test qui échoue
2. 🟢 **Green**: Implémenter le minimum pour passer le test
3. 🔵 **Refactor**: Améliorer le code en gardant les tests verts

### Structure des Tests

```
shared/src/
├── commonTest/kotlin/
│   ├── EventRepositoryTest.kt        (10 tests)
│   ├── PollLogicTest.kt              (6 tests)
│   ├── DatabaseEventRepositoryTest.kt (13 tests)
│   └── OfflineScenarioTest.kt        (7 tests)
```

### Commandes de Test

```bash
# Tous les tests
./gradlew shared:test

# Test spécifique
./gradlew shared:test --tests "EventRepositoryTest"

# Tests avec logs détaillés
./gradlew shared:test --info

# Tests en mode debug
./gradlew shared:test --debug-jvm
```

### Couverture Actuelle

```
✅ 36/36 tests (100% passing)
- EventRepositoryTest: 10 tests
- PollLogicTest: 6 tests
- DatabaseEventRepositoryTest: 13 tests
- OfflineScenarioTest: 7 tests
```

---

## 📋 Checklist pour Pull Requests

Avant de merger dans `main`, vérifier :

- [ ] Le code respecte Conventional Commits
- [ ] Les tests sont écrits AVANT l'implémentation (TDD)
- [ ] Tous les tests passent (./gradlew shared:test)
- [ ] Le code suit les conventions Kotlin (ktlint)
- [ ] Le design system est respecté (Material You / Liquid Glass)
- [ ] Les scénarios offline sont testés
- [ ] La documentation est à jour (si nécessaire)
- [ ] Aucun secret ou donnée sensible n'est commité
- [ ] L'accessibilité est validée (pour les composants UI)
- [ ] tasks.md est mis à jour (si OpenSpec)

---

## 📚 Stack Technique Wakeve

### Technologies Principales

- **Kotlin Multiplatform** 2.2.20 - Code partagé cross-platform
- **SQLDelight** 2.1.0 - Base de données type-safe
- **Ktor** 3.3.1 - Backend REST API
- **Jetpack Compose** - UI Android (Material You)
- **SwiftUI** - UI iOS (Liquid Glass)
- **kotlinx-serialization** - Sérialisation JSON

### Domain Models
- **Event**: Ajout de eventType, eventTypeCustom, minParticipants, maxParticipants, expectedParticipants
- **EventType**: Enum (11 types: BIRTHDAY, WEDDING, TEAM_BUILDING, etc.)
- **PotentialLocation**: Nouveau modèle pour lieux potentiels
- **LocationType**: Enum (CITY, REGION, SPECIFIC_VENUE, ONLINE)
- **TimeSlot**: Ajout de timeOfDay
- **TimeOfDay**: Enum (ALL_DAY, MORNING, AFTERNOON, EVENING, SPECIFIC)

### Frameworks UI

#### Android (Jetpack Compose + Material You)
```kotlin
// composeApp/src/commonMain/kotlin/theme/
Color.kt           // Palette de couleurs unifiée
Typography.kt      // Système typographique
Theme.kt           // Configuration Material Theme 3
Spacing.kt         // Échelle d'espacements

// Composants
WakevButton.kt, WakevCard.kt, WakevTextField.kt
```

#### iOS (SwiftUI + Liquid Glass)
```swift
// iosApp/src/Theme/
WakevColors.swift           // Palette de couleurs
WakevTypography.swift       // Système typographique
LiquidGlassModifier.swift   // Extensions Liquid Glass

// Composants
WakevButton.swift, WakevCard.swift
```

### Base de Données

```sql
-- shared/src/commonMain/sqldelight/com/guyghost/wakeve/db/
Event.sq           -- Table events
Participant.sq     -- Table participants
ProposedSlot.sq    -- Table proposed_slots
Vote.sq            -- Table votes
Scenario.sq        -- Table scenarios
Budget.sq          -- Table budgets
```

### API REST (Ktor)

```
server/src/main/kotlin/com/guyghost/wakeve/
├── Application.kt          -- Point d'entrée
├── routing/
│   ├── EventRoutes.kt      -- Routes /api/events
│   └── PollRoutes.kt       -- Routes /api/poll
└── models/
    └── DTOs.kt             -- Data Transfer Objects
```

**Endpoints disponibles (8):**
```
GET    /health
GET    /api/events
GET    /api/events/{id}
POST   /api/events
PUT    /api/events/{id}/status
GET    /api/events/{id}/participants
POST   /api/events/{id}/participants
GET    /api/events/{id}/poll
POST   /api/events/{id}/poll/votes
```

---

## 🚀 Commandes Utiles

### Développement

```bash
# Build complet
./gradlew build

# Tests
./gradlew shared:test

# Démarrer le serveur Ktor
./gradlew server:run

# Build Android
./gradlew :composeApp:assembleDebug

# Build iOS (macOS uniquement)
open iosApp/iosApp.xcodeproj

# Format du code Kotlin
./gradlew spotlessApply

# Clean build
./gradlew clean build
```

### OpenSpec

```bash
# Lister les changements actifs
openspec list

# Lister les specs
openspec spec list --long

# Valider un changement
openspec validate <change-id> --strict

# Archiver un changement
openspec archive <change-id> --yes
```

---

## 📖 Ressources et Documentation

### Documentation Projet

- **QUICK_START.md** - Guide de démarrage rapide (5 minutes)
- **CONTRIBUTING.md** - Guidelines de développement
- **openspec/AGENTS.md** - Détails du workflow OpenSpec (référence complète)
- **.opencode/context.md** - Contexte complet du projet
- **.opencode/design-system.md** - Design system unifié

### Documentation Externe

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [Ktor](https://ktor.io/docs/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [SwiftUI](https://developer.apple.com/xcode/swiftui/)
- [Material Design 3](https://m3.material.io/)
- [Liquid Glass Guidelines](https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass)
- [Conventional Commits](https://www.conventionalcommits.org/)

---

## 🎯 État du Projet

### Phase 2 Complète ✅
- Organisation d'événements
- Système de vote pondéré
- Calcul automatique du meilleur créneau
- Persistance offline-first (SQLDelight)
- Backend REST API (Ktor)
- Support multiplatform (Android/iOS/JVM)
- 36/36 tests (100%)

### Phase 3 En Planification 🚀
- Authentification OAuth (Google, Apple)
- Agent Suggestions (recommandations personnalisées)
- Agent Calendrier (intégration native + ICS)
- Agent Notifications (FCM/APNs)
- Agent Transport (optimisation multi-participants)
- Agent Destination & Logement

### Phase 2.6 Complète ✅
- First Time Onboarding (Android + iOS)
- 4-screen onboarding flow
- Material You (Android) + Liquid Glass (iOS)
- Local persistence (SharedPreferences/UserDefaults)
- 35 automated tests (25 Android + 10 iOS)
- **Enhanced DRAFT Phase** (event types, participants estimation, potential locations, flexible time slots)

### Phase 4 Future 🔮
- Agent Réunions (Zoom/Meet/FaceTime)
- Agent Paiement & Tricount
- CRDT pour résolution de conflits
- Observabilité complète

---

## ⚠️ Points d'Attention

### Sécurité
- OAuth pour authentification (Phase 3)
- Stockage sécurisé des tokens
- Validation des entrées sur tous les endpoints
- Anti-phishing pour liens externes

### Conformité RGPD
- Gestion des consentements
- Minimisation des données
- Export et suppression de données
- Logs d'audit pour actions critiques

### Performance & UX
- Cohérence des fuseaux horaires
- Transparence de l'état offline/online
- Actions en file d'attente clairement indiquées
- Synchronisation automatique en arrière-plan

### Offline-First
- SQLite comme source de vérité locale
- Sync incrémentale avec backend
- Résolution de conflits (last-write-wins → CRDT)
- Tests offline obligatoires pour chaque fonctionnalité

---

**Note importante pour les agents IA :** Ce document doit être suivi rigoureusement pour tous les commits et modifications de code. Toute contribution doit respecter ces conventions. L'orchestrator ne code JAMAIS directement - il délègue aux agents spécialisés.

**Pour les détails complets du workflow OpenSpec, consultez `openspec/AGENTS.md`.**
