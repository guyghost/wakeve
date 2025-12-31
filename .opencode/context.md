# Wakeve - Context de Projet

## Vue d'ensemble

Wakeve est une application mobile de planification collaborative d'événements, construite avec Kotlin Multiplatform. Elle permet à des groupes d'amis et petites équipes de planifier des événements de manière collaborative, du sondage de disponibilités initial jusqu'à l'organisation logistique complète.

## Objectifs du projet

- **Prise de décision collective**: Faciliter la coordination de groupes pour trouver le meilleur moment
- **Transparence budgétaire**: Permettre le suivi et le partage des coûts
- **Partage des responsabilités**: Distribuer les tâches d'organisation entre participants
- **Expérience offline-first**: Fonctionnement complet hors ligne avec synchronisation automatique

## Stack Technique

### Technologies principales
- **Langage**: Kotlin 2.2.20 avec support Multiplatform
- **UI**: 
  - Android: Jetpack Compose
  - iOS: SwiftUI
- **Base de données**: SQLDelight (SQLite) avec requêtes type-safe
- **Backend**: Ktor 3.3.1 REST server
- **Sérialisation**: kotlinx-serialization pour JSON
- **Tests**: Kotlin test framework (36+ tests, 100% passing)

### Structure du projet

```
wakeve/
├── shared/              # Code Kotlin Multiplatform partagé
│   ├── commonMain/     # Logique métier cross-platform
│   ├── androidMain/    # Implémentations Android-specific
│   ├── iosMain/        # Implémentations iOS-specific
│   ├── jvmMain/        # Implémentations JVM-specific
│   └── sqldelight/     # Schéma de base de données
├── composeApp/         # Application Android (Jetpack Compose)
├── iosApp/             # Application iOS (SwiftUI)
├── server/             # Backend Ktor REST API
├── openspec/           # Spécifications et propositions
└── .opencode/          # Configuration OpenCode
```

## Architecture

### Principes architecturaux

1. **Multiplatform-first**: Logique métier partagée en Kotlin, UI native par plateforme
2. **Local-first sync**: SQLDelight comme source de vérité, synchronisation incrémentale
3. **Backend proxy**: Ktor pour agréger les API externes et protéger les clés
4. **Provider pattern**: Abstractions pour services (Transport, Destination, Lodging) avec implémentations mock → réelles
5. **Conflict resolution**: Last-write-wins avec timestamps (évolution vers CRDT prévue)
6. **Test-Driven Development**: Tests écrits avant l'implémentation

### Couches applicatives

```
┌─────────────────────────────────────┐
│  UI Layer (Compose/SwiftUI)         │
├─────────────────────────────────────┤
│  Business Logic (EventRepository)   │
├─────────────────────────────────────┤
│  Persistence (SQLDelight)           │
├─────────────────────────────────────┤
│  Platform Drivers (Android/iOS/JVM) │
└─────────────────────────────────────┘
```

### State Machine Workflow Coordination

**Architecture Pattern**: MVI (Model-View-Intent) avec Finite State Machines (FSM)

Le workflow de Wakeve est coordonné par 3 state machines qui communiquent via un repository partagé:

1. **EventManagementStateMachine**: Gère le cycle de vie DRAFT → POLLING → CONFIRMED → ORGANIZING → FINALIZED
2. **ScenarioManagementStateMachine**: Gère la création et le vote des scénarios (COMPARING)
3. **MeetingServiceStateMachine**: Gère les réunions virtuelles

#### Pattern Repository-Mediated Communication

```kotlin
// State Machine 1 met à jour le status
eventStateMachine.dispatch(Intent.ConfirmDate("event-1", "slot-1"))
// → Repository: Event.status = CONFIRMED

// State Machine 2 lit le status depuis le repository
val event = eventRepository.getEvent("event-1")
val canCreate = event?.status in listOf(CONFIRMED, COMPARING)
```

**Avantages**:
- ✅ Couplage faible entre state machines
- ✅ Cohérence forte via repository partagé
- ✅ Tests simples (mock repository uniquement)
- ✅ Source de vérité claire (Event.status)

#### Workflow Complet

```
Event(DRAFT) 
  → StartPoll 
  → Event(POLLING)
  → ConfirmDate 
  → Event(CONFIRMED) + scenariosUnlocked + NavigateTo("scenarios/$id")
  → [User creates scenarios]
  → SelectScenarioAsFinal (optional)
  → TransitionToOrganizing 
  → Event(ORGANIZING) + meetingsUnlocked + NavigateTo("meetings/$id")
  → [User creates meetings]
  → MarkAsFinalized 
  → Event(FINALIZED)
```

#### Business Rules par EventStatus

| EventStatus | Scénarios | Réunions | Actions |
|-------------|-----------|----------|---------|
| DRAFT | ❌ | ❌ | CreateEvent, StartPoll |
| POLLING | ❌ | ❌ | Vote, ConfirmDate |
| CONFIRMED | ✅ | ❌ | CreateScenario, TransitionToOrganizing |
| COMPARING | ✅ | ❌ | VoteScenario, SelectScenarioAsFinal |
| ORGANIZING | ❌ | ✅ | CreateMeeting, MarkAsFinalized |
| FINALIZED | ❌ | ❌ | Read-only |

#### Tests

- **Unit Tests**: 13 tests (EventManagementStateMachineTest.kt) - 100% passing
- **Integration Tests**: 6 tests (WorkflowIntegrationTest.kt) - 100% passing
- **Pattern validé**: Repository-mediated communication

**Documentation complète**: `openspec/changes/verify-statemachine-workflow/`
- [WORKFLOW_DIAGRAMS.md](openspec/changes/verify-statemachine-workflow/WORKFLOW_DIAGRAMS.md): Diagrammes Mermaid
- [TROUBLESHOOTING.md](openspec/changes/verify-statemachine-workflow/TROUBLESHOOTING.md): Guide de dépannage

## Domaine Métier

### Cycle de vie d'un événement

1. **Idée / Brouillon**
   - Création de l'événement
   - Invitation des participants

2. **Sondage**
   - Proposition de dates/périodes multiples
   - Vote des participants (Oui / Peut-être / Non)

3. **Comparaison de scénarios** (optionnel)
   - Shortlist de scénarios avec:
     - Date ou période
     - Destination
     - Durée
     - Nombre estimé de participants
     - Budget approximatif par personne

4. **Confirmé**
   - Date unique verrouillée par l'organisateur
   - Scénario éventuellement sélectionné

5. **Organisation**
   - Planification détaillée:
     - Transport
     - Hébergement
     - Restauration
     - Équipement & activités
     - Coûts partagés

6. **Finalisé**
   - Toutes les informations critiques confirmées
   - Événement prêt pour exécution

### Agents humains

#### Organisateur
- Crée et configure l'événement
- Propose dates et scénarios
- Définit les dates limites
- Confirme la date finale
- Supervise la logistique et le budget

#### Participant
- Vote sur les dates et scénarios
- Confirme sa participation
- Fournit son lieu de départ
- Contribue à la logistique, restauration et équipement après confirmation de la date

### Agents logiciels

#### Agent Sondage & Calendrier
- Gère créneaux horaires, votes, deadlines et fuseaux horaires
- Calcule la meilleure date/période possible
- Scoring: YES=2, MAYBE=1, NO=-1 avec pénalités conflits de timezone

#### Agent Scénarios & Budget
- Gère les shortlists de scénarios
- Agrège les coûts estimés (transport, hébergement, restauration)
- Calcule les approximations de budget par personne

#### Agent Suggestions
- Analyse les préférences utilisateur (jours, heures, lieux, activités)
- Génère des recommandations personnalisées avec scoring
- Support A/B testing pour optimisation

#### Agent Calendrier
- Génère invitations ICS avec détails complets
- Intègre calendriers natifs (Android: CalendarContract, iOS: EventKit)
- Gère fuseaux horaires et mises à jour d'événements

#### Agent Notifications
- Envoie notifications push (FCM pour Android, APNs pour iOS)
- Gère tokens d'appareils et permissions
- Rappels programmés et confirmations

#### Agent Transport
- Calcule routes optimisées multi-participants (coût/temps/équilibré)
- Intègre providers de transport (vols, trains, etc.)
- Planifie points de rencontre pour groupes

#### Agent Destination & Logement
- Suggère destinations et hébergements
- Score multi-critères: coût, accessibilité, préférences, saisonnalité
- Providers mockés puis réels via backend

#### Agent Réunions
- Génère liens réunions virtuelles (Zoom/Meet/FaceTime)
- Crée invitations pour participants validés
- Ajoute rappels avec respect des fuseaux horaires

#### Agent Paiement & Tricount
- Crée cagnottes via providers externes
- Intègre Tricount pour répartition des coûts
- Affiche objectifs et avances

#### Agent Sync & Offline
- Source de vérité locale (SQLite via SQLDelight)
- Sync incrémentale avec backend
- Résolution de conflits: last-write-wins + timestamp (évolution vers CRDT)
- Signale clairement l'état offline/online

#### Agent Sécurité & Auth
- OAuth (Apple/Google)
- Tokens stockés de manière sécurisée
- Permissions pour localisation
- Minimisation des données, droit à l'effacement (RGPD)

### Flux de données entre agents

1. Organisateur → Agent Sondage: création, deadline, règles
2. Participants → Agent Sondage: votes et propositions; calcul du meilleur slot
3. Agent Sondage → Organisateur: recommandation; validation du créneau
4. Agent Sondage → Agents (Suggestions/Calendrier/Transport): créneau verrouillé
5. Agent Suggestions → Participants: recommandations personnalisées
6. Agent Calendrier → Participants validés: invitations ICS
7. Agent Notifications → Tous: notifications push
8. Agent Transport → Participants: plans de transport optimisés
9. Agents Destination/Transport → Participants: suggestions classées
10. Agent Réunions → Participants validés: liens, rappels
11. Agent Paiement → Tous: cagnotte, suivi des coûts
12. Agent Sync & Offline ↔ Tous: persistance, sync, conflits

## API REST

### Endpoints disponibles (8 endpoints)

```
GET    /health                        # Health check
GET    /api/events                    # Liste tous les événements
GET    /api/events/{id}               # Détails d'un événement
POST   /api/events                    # Créer un événement
PUT    /api/events/{id}/status        # Mettre à jour le statut
GET    /api/events/{id}/participants  # Liste des participants
POST   /api/events/{id}/participants  # Ajouter un participant
GET    /api/events/{id}/poll          # Résultats du sondage
POST   /api/events/{id}/poll/votes    # Soumettre un vote
```

### Exemple de requête

```json
POST /api/events
{
  "title": "Team Meeting",
  "description": "Q4 Planning",
  "organizerId": "user-1",
  "deadline": "2025-11-20T18:00:00Z",
  "proposedSlots": [{
    "id": "slot-1",
    "start": "2025-12-01T10:00:00Z",
    "end": "2025-12-01T12:00:00Z",
    "timezone": "UTC"
  }]
}
```

## Conventions de Code

### Kotlin (Shared, Android, Backend)
- Conformité stricte aux conventions officielles Kotlin
- `ktlint` intégré au build pour l'application automatique
- Préférer expressions plutôt que statements
- Utiliser `val` autant que possible
- Code idiomatique utilisant la stdlib
- Jetpack Compose: noms de Composables avec majuscule, idempotence

### Swift (iOS)
- Conformité aux Swift API Design Guidelines d'Apple
- `SwiftLint` pour l'application des conventions
- Clarté au point d'utilisation

### SQL (SQLDelight)
- Requêtes claires et lisibles dans fichiers `.sq`
- Noms de tables et colonnes en `snake_case`

### Principes généraux
- Code auto-documenté avec noms significatifs
- Commentaires uniquement pour logique complexe ou justifications ("pourquoi", pas "quoi")
- Tests écrits avant implémentation (TDD)

## Tests

### Couverture actuelle: 36/36 tests (100%)

```
EventRepositoryTest          10 tests ✅
PollLogicTest                 6 tests ✅
DatabaseEventRepositoryTest  13 tests ✅
OfflineScenarioTest           7 tests ✅
```

### Domaines couverts
- Création et cycle de vie des événements
- Gestion des participants
- Soumission et agrégation des votes
- Persistance en base de données
- Récupération de données hors ligne
- Endpoints API

### Commandes

```bash
# Tous les tests
./gradlew shared:test

# Test spécifique
./gradlew shared:test --tests "EventRepositoryTest"

# Mode debug
./gradlew shared:test --tests "TestName" -d
```

## Workflow Git

### Modèle: Trunk-Based Development
- Branche unique: `main`
- Commits fréquents et incrémentaux
- Feature flags pour grandes fonctionnalités

### Format des commits

```
[#<issue>] <type>: <description>

<corps optionnel>
```

**Types**: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`

**Exemples**:
```
[#2] feat: Implement event creation API
[#15] fix: Handle timezone conversion
[#20] test: Add offline sync scenarios
```

## Processus OpenSpec

Wakeve suit un processus de développement dirigé par spécifications:

1. **Créer une Issue** → Problème ou fonctionnalité identifiée
2. **Créer une Proposition** → Document de proposition dans `openspec/changes/`
3. **Créer une Spec** → Spécification détaillée dans `openspec/specs/`
4. **Obtenir l'Approbation** → Revue et validation
5. **Implémenter avec Tests** → TDD avec tests écrits en premier
6. **Merger & Déployer** → Intégration dans `main`

Voir `openspec/PROCESS.md` pour les détails.

## Contraintes Importantes

### Sécurité
- OAuth pour authentification (Google, Apple) - Planifié Phase 3
- Stockage sécurisé des tokens
- Validation des entrées sur tous les endpoints
- Gestion des liens externes (anti-phishing)

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

### Contrôle d'accès
- Avant confirmation de date: vue limitée aux infos générales
- Après confirmation: détails complets uniquement pour participants validés
- Contrôle basé sur les rôles (organisateur vs participant)

## Dépendances Externes

### Authentification
- OAuth via Google et Apple (Phase 3)

### Réunions
- Génération de liens Zoom, Google Meet, FaceTime

### Paiements
- Providers externes pour cagnottes
- Intégration Tricount pour répartition des coûts

### Transport
- APIs de providers de transport (vols, trains, etc.)
- À implémenter via backend proxy

### Destination & Logement
- Agrégation de destinations et hébergements
- Providers mockés → réels via backend

## État Actuel du Projet

### Phase 2 Complète ✅
- Organisation d'événements
- Système de vote avec pondération
- Calcul automatique du meilleur créneau
- Persistance offline-first
- Backend REST API
- Support multiplatform (Android/iOS/JVM)

### Phase 3 Planifiée 🚀
- Authentification utilisateur OAuth2
- Synchronisation offline automatique
- Notifications push
- Intégration calendrier natif
- Recommandations personnalisées
- Optimisation transport multi-participants

## Statistiques du Projet

| Métrique | Valeur |
|----------|--------|
| Phases complètes | 2/5 |
| Tests | 36/36 (100%) ✅ |
| Lignes de code | ~3,500 |
| Fichiers | 30+ |
| Endpoints API | 8 |
| Tables DB | 6 |
| Plateformes supportées | 3 (Android, iOS, JVM) |

## Commandes Utiles

```bash
# Build complet
./gradlew build

# Tests
./gradlew shared:test

# Démarrer le serveur
./gradlew server:run

# Build Android
./gradlew composeApp:assembleDebug

# Format du code
./gradlew spotlessApply

# Clean build
./gradlew clean build
```

## Axes d'Amélioration Identifiés

- ✅ Migration last-write-wins → CRDT pour édition collaborative
- ✅ Recommandations personnalisées basées sur historique
- ✅ Intégration calendrier natif et invitations ICS
- ✅ Optimisation transport multi-participants
- ✅ Observabilité: métriques, traces, alertes

## Documentation

- **QUICK_START.md**: Guide de démarrage rapide (5 minutes)
- **CONTRIBUTING.md**: Guidelines de développement
- **IMPLEMENTATION_CHECKLIST.md**: Checklist Phase 2
- **openspec/specs/**: Spécifications détaillées
- **openspec/PROCESS.md**: Processus OpenSpec
- **AGENTS.md**: Définition des agents et responsabilités

## Support et Contact

- **Issues**: Créer une GitHub Issue pour bugs/fonctionnalités
- **Discussions**: GitHub Discussions pour questions
- **Documentation**: Docs complètes dans le repository
- **Email**: Contacter les mainteneurs pour problèmes de sécurité

## License

Wakeve est sous license MIT. Voir le fichier LICENSE pour détails.

---

**Mission de Wakeve**: Rendre la planification collaborative d'événements sans effort en combinant polling intelligent, planification automatique et principes offline-first.
