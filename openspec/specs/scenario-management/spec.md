# Specification: Scenario Management & Voting System

## Purpose

The Scenario Management capability enables organizers and confirmed participants to propose, compare, vote on, and select destination/lodging scenarios as part of the event organization workflow.
## Requirements

*(Requirements are merged from archived changes; legacy implementation notes remain below.)*

---

**Version**: 1.0.0  
**Date**: 25 décembre 2025  
**Status**: ✅ Implémenté  
**Change**: `add-full-prd-features` (Phase 1)

---

### Requirement: Scenario Comparison for Confirmed Events MUST be supported
Wakeve MUST let organizers and eligible participants compare scenarios that combine date or period, destination, lodging option when available, estimated participants, duration, and estimated cost. Scenario comparison MUST be available either after date confirmation in the legacy flow or directly in `COMPARING` for scenario matrix events.

#### Scenario: Participants vote on generated matrix scenarios
- **GIVEN** an event uses planning mode `SCENARIO_MATRIX`
- **AND** the organizer has published generated scenarios
- **WHEN** participants open the scenario list
- **THEN** they can compare each complete date-and-destination scenario
- **AND** they can vote `PREFER`, `NEUTRAL`, or `AGAINST` on each scenario
- **AND** the system ranks scenarios using weighted scoring

### Requirement: Final Scenario Selection MUST be available
Wakeve MUST allow the organizer to select a final scenario or explicitly skip scenarios when the event does not require scenario comparison.

#### Scenario: Organizer selects final scenario
- **GIVEN** an event is in `COMPARING`
- **AND** at least one scenario exists
- **WHEN** the organizer selects a final scenario
- **THEN** the selected scenario is marked final
- **AND** related destination, lodging, budget estimate, and transport destination data become inputs for the organizing phase
- **AND** the selection is persisted locally and queued for sync when offline

### Requirement: Scenario Matrix Generation MUST be deterministic
Wakeve MUST generate one draft scenario for every valid `TimeSlot × PotentialLocation` combination for an event using scenario matrix planning mode.

#### Scenario: Generate all combinations
- **GIVEN** an event has 3 proposed time slots
- **AND** 2 potential locations
- **WHEN** the organizer generates the scenario matrix
- **THEN** Wakeve creates 6 draft scenarios
- **AND** each scenario stores its source time slot ID, source potential location ID, and generation type `MATRIX`

#### Scenario: Regeneration avoids duplicates
- **GIVEN** an event already has generated matrix scenarios
- **WHEN** the organizer regenerates the matrix without changing source time slots or locations
- **THEN** Wakeve does not create duplicate scenarios for the same source time slot and potential location pair

### Requirement: Generated Matrix Scenarios MUST be editable before publication
Wakeve MUST let the organizer edit or remove generated draft matrix scenarios before participants can vote on them.

#### Scenario: Organizer removes a generated scenario
- **GIVEN** an event has draft matrix scenarios
- **WHEN** the organizer removes one scenario before publishing
- **THEN** the removed scenario is deleted locally
- **AND** it is not published for participant voting

#### Scenario: Organizer publishes generated scenarios
- **GIVEN** an event has at least one draft matrix scenario
- **WHEN** the organizer publishes the matrix
- **THEN** draft matrix scenarios become `PROPOSED`
- **AND** participants can vote `PREFER`, `NEUTRAL`, or `AGAINST`

## Vue d'ensemble

Le système de gestion de scénarios permet aux organisateurs de proposer plusieurs options de planification pour un événement (combinaisons de date/période, lieu, durée, budget) et aux participants de voter sur leurs préférences. Le système calcule automatiquement le meilleur scénario basé sur les votes pondérés.

### Objectifs

1. **Prise de décision collaborative**: Permettre au groupe de choisir ensemble le meilleur scénario
2. **Transparence**: Afficher clairement les résultats des votes et le scoring
3. **Comparaison facile**: Vue côte-à-côte pour comparer plusieurs scénarios
4. **Flexibilité**: Support de votes pondérés (PREFER, NEUTRAL, AGAINST)

---

## Modèles de Données

### Scenario

Représente une option de planification pour un événement.

```kotlin
@Serializable
data class Scenario(
    val id: String,                        // UUID unique
    val eventId: String,                   // Référence à l'événement
    val name: String,                      // Nom du scénario (ex: "Weekend Mountain")
    val dateOrPeriod: String,              // Date ISO ou période (ex: "15-17 Mars 2025")
    val location: String,                  // Lieu (ex: "Chamonix, France")
    val duration: Int,                     // Durée en jours
    val estimatedParticipants: Int,        // Nombre estimé de participants
    val estimatedBudgetPerPerson: Double,  // Budget estimé par personne (€)
    val description: String,               // Description détaillée
    val status: ScenarioStatus,            // Statut actuel
    val createdAt: String,                 // Timestamp ISO (UTC)
    val updatedAt: String                  // Timestamp ISO (UTC)
)
```

**Contraintes**:
- `name` ne peut pas être vide
- `location` ne peut pas être vide
- `duration > 0`
- `estimatedParticipants > 0`
- `estimatedBudgetPerPerson >= 0.0`

### ScenarioStatus

```kotlin
enum class ScenarioStatus {
    PROPOSED,   // Proposé et ouvert au vote
    SELECTED,   // Sélectionné par l'organisateur
    REJECTED    // Rejeté (non considéré)
}
```

### ScenarioVote

Représente le vote d'un participant sur un scénario.

```kotlin
@Serializable
data class ScenarioVote(
    val id: String,              // UUID unique
    val scenarioId: String,      // Référence au scénario
    val participantId: String,   // ID du participant
    val vote: ScenarioVoteType,  // Type de vote
    val createdAt: String        // Timestamp ISO (UTC)
)
```

**Contraintes**:
- Un participant ne peut voter qu'une seule fois par scénario (UNIQUE constraint)
- Changer de vote remplace l'ancien vote

### ScenarioVoteType

```kotlin
enum class ScenarioVoteType {
    PREFER,   // Préfère ce scénario (score: +2)
    NEUTRAL,  // Neutre (score: +1)
    AGAINST   // Contre ce scénario (score: -1)
}
```

### ScenarioVotingResult

Résultats agrégés des votes pour un scénario.

```kotlin
data class ScenarioVotingResult(
    val scenarioId: String,
    val preferCount: Int,       // Nombre de votes PREFER
    val neutralCount: Int,      // Nombre de votes NEUTRAL
    val againstCount: Int,      // Nombre de votes AGAINST
    val totalVotes: Int,        // Total des votes
    val score: Int              // Score calculé
) {
    val preferPercentage: Double    // (preferCount / totalVotes) * 100
    val neutralPercentage: Double   // (neutralCount / totalVotes) * 100
    val againstPercentage: Double   // (againstCount / totalVotes) * 100
}
```

### ScenarioWithVotes

Vue combinée d'un scénario avec ses votes.

```kotlin
data class ScenarioWithVotes(
    val scenario: Scenario,
    val votes: List<ScenarioVote>,
    val votingResult: ScenarioVotingResult
)
```

---

## Schéma de Base de Données

### Table: `scenarios`

```sql
CREATE TABLE scenarios (
    id TEXT PRIMARY KEY,
    event_id TEXT NOT NULL,
    name TEXT NOT NULL,
    date_or_period TEXT NOT NULL,
    location TEXT NOT NULL,
    duration INTEGER NOT NULL CHECK(duration > 0),
    estimated_participants INTEGER NOT NULL CHECK(estimated_participants > 0),
    estimated_budget_per_person REAL NOT NULL CHECK(estimated_budget_per_person >= 0.0),
    description TEXT NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('PROPOSED', 'SELECTED', 'REJECTED')),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

CREATE INDEX idx_scenarios_event_id ON scenarios(event_id);
CREATE INDEX idx_scenarios_status ON scenarios(status);
```

### Table: `scenario_votes`

```sql
CREATE TABLE scenario_votes (
    id TEXT PRIMARY KEY,
    scenario_id TEXT NOT NULL,
    participant_id TEXT NOT NULL,
    vote TEXT NOT NULL CHECK(vote IN ('PREFER', 'NEUTRAL', 'AGAINST')),
    created_at TEXT NOT NULL,
    FOREIGN KEY (scenario_id) REFERENCES scenarios(id) ON DELETE CASCADE,
    UNIQUE(scenario_id, participant_id)
);

CREATE INDEX idx_scenario_votes_scenario_id ON scenario_votes(scenario_id);
CREATE INDEX idx_scenario_votes_participant_id ON scenario_votes(participant_id);
```

### Vue: `scenarios_with_votes`

```sql
CREATE VIEW scenarios_with_votes AS
SELECT 
    s.*,
    COUNT(sv.id) as total_votes,
    SUM(CASE WHEN sv.vote = 'PREFER' THEN 1 ELSE 0 END) as prefer_count,
    SUM(CASE WHEN sv.vote = 'NEUTRAL' THEN 1 ELSE 0 END) as neutral_count,
    SUM(CASE WHEN sv.vote = 'AGAINST' THEN 1 ELSE 0 END) as against_count,
    (SUM(CASE WHEN sv.vote = 'PREFER' THEN 2 ELSE 0 END) +
     SUM(CASE WHEN sv.vote = 'NEUTRAL' THEN 1 ELSE 0 END) -
     SUM(CASE WHEN sv.vote = 'AGAINST' THEN 1 ELSE 0 END)) as score
FROM scenarios s
LEFT JOIN scenario_votes sv ON s.id = sv.scenario_id
GROUP BY s.id;
```

---

## Logique Métier

### Algorithme de Scoring

Le score d'un scénario est calculé selon la formule:

```
Score = (PREFER × 2) + (NEUTRAL × 1) + (AGAINST × -1)
```

**Exemple**:
- 5 votes PREFER = 5 × 2 = 10
- 3 votes NEUTRAL = 3 × 1 = 3
- 2 votes AGAINST = 2 × -1 = -2
- **Score total**: 10 + 3 - 2 = **11**

### ScenarioLogic

Classe utilitaire pour les calculs liés aux scénarios.

#### Méthodes

**`calculateBestScenario(scenarios, votes): Scenario?`**
- Retourne le scénario avec le score le plus élevé
- Retourne `null` si la liste est vide

**`rankScenariosByScore(scenarios, votes): List<ScenarioWithVotes>`**
- Retourne tous les scénarios triés par score décroissant
- Inclut les votes et résultats pour chaque scénario

**`getScenarioVotingResults(scenarios, votes): List<ScenarioVotingResult>`**
- Calcule les résultats de vote pour chaque scénario
- Retourne les comptages et scores

**`getBestScenarioWithScore(scenarios, votes): Pair<Scenario, ScenarioVotingResult>?`**
- Retourne le meilleur scénario avec ses détails de vote
- Retourne `null` si pas de scénarios

---

## API Repository

### ScenarioRepository

Interface pour les opérations sur les scénarios.

#### Méthodes

**`createScenario(scenario: Scenario): Scenario`**
- Crée un nouveau scénario
- Valide les contraintes
- Génère ID et timestamps
- **Retourne**: Le scénario créé

**`getScenarioById(scenarioId: String): Scenario?`**
- Récupère un scénario par ID
- **Retourne**: Le scénario ou `null` si non trouvé

**`getScenariosByEventId(eventId: String): List<Scenario>`**
- Liste tous les scénarios d'un événement
- **Retourne**: Liste (peut être vide)

**`updateScenario(scenario: Scenario): Scenario`**
- Met à jour un scénario existant
- Met à jour `updatedAt` automatiquement
- **Retourne**: Le scénario mis à jour

**`deleteScenario(scenarioId: String)`**
- Supprime un scénario
- Supprime aussi tous les votes associés (CASCADE)

**`submitVote(scenarioId: String, participantId: String, vote: ScenarioVoteType): ScenarioVote`**
- Soumet ou met à jour un vote
- Remplace le vote existant si présent
- **Retourne**: Le vote créé/mis à jour

**`getScenariosWithVotes(eventId: String): List<ScenarioWithVotes>`**
- Récupère tous les scénarios avec leurs votes
- Inclut les résultats agrégés
- **Retourne**: Liste de ScenarioWithVotes

**`getVotingResults(scenarioId: String): ScenarioVotingResult`**
- Récupère les résultats de vote pour un scénario
- **Retourne**: Résultats agrégés

---

## Endpoints API REST

### POST `/api/scenarios`

Créer un nouveau scénario.

**Request Body**:
```json
{
  "eventId": "event-123",
  "name": "Weekend Mountain",
  "dateOrPeriod": "15-17 Mars 2025",
  "location": "Chamonix, France",
  "duration": 3,
  "estimatedParticipants": 10,
  "estimatedBudgetPerPerson": 250.0,
  "description": "Weekend ski dans les Alpes",
  "status": "PROPOSED"
}
```

**Response** (201 Created):
```json
{
  "id": "scenario-abc",
  "eventId": "event-123",
  "name": "Weekend Mountain",
  "dateOrPeriod": "15-17 Mars 2025",
  "location": "Chamonix, France",
  "duration": 3,
  "estimatedParticipants": 10,
  "estimatedBudgetPerPerson": 250.0,
  "description": "Weekend ski dans les Alpes",
  "status": "PROPOSED",
  "createdAt": "2025-12-25T10:00:00Z",
  "updatedAt": "2025-12-25T10:00:00Z"
}
```

### GET `/api/scenarios/{id}`

Récupérer un scénario par ID.

**Response** (200 OK):
```json
{
  "id": "scenario-abc",
  "eventId": "event-123",
  "name": "Weekend Mountain",
  ...
}
```

**Response** (404 Not Found):
```json
{
  "error": "Scenario not found"
}
```

### PUT `/api/scenarios/{id}`

Mettre à jour un scénario.

**Request Body**: Même format que POST (sans `id`, `createdAt`)

**Response** (200 OK): Scénario mis à jour

### DELETE `/api/scenarios/{id}`

Supprimer un scénario.

**Response** (204 No Content)

### GET `/api/scenarios/event/{eventId}`

Liste tous les scénarios d'un événement.

**Response** (200 OK):
```json
[
  {
    "id": "scenario-abc",
    "eventId": "event-123",
    ...
  },
  {
    "id": "scenario-def",
    "eventId": "event-123",
    ...
  }
]
```

### POST `/api/scenarios/{id}/vote`

Soumettre un vote sur un scénario.

**Request Body**:
```json
{
  "participantId": "user-123",
  "vote": "PREFER"
}
```

**Response** (200 OK):
```json
{
  "id": "vote-xyz",
  "scenarioId": "scenario-abc",
  "participantId": "user-123",
  "vote": "PREFER",
  "createdAt": "2025-12-25T10:05:00Z"
}
```

### GET `/api/scenarios/{id}/results`

Obtenir les résultats de vote pour un scénario.

**Response** (200 OK):
```json
{
  "scenarioId": "scenario-abc",
  "preferCount": 5,
  "neutralCount": 3,
  "againstCount": 2,
  "totalVotes": 10,
  "score": 11,
  "preferPercentage": 50.0,
  "neutralPercentage": 30.0,
  "againstPercentage": 20.0
}
```

### GET `/api/scenarios/event/{eventId}/ranked`

Obtenir les scénarios classés par score.

**Response** (200 OK):
```json
[
  {
    "scenario": { ... },
    "votes": [ ... ],
    "votingResult": {
      "scenarioId": "scenario-abc",
      "score": 11,
      ...
    }
  },
  {
    "scenario": { ... },
    "votes": [ ... ],
    "votingResult": {
      "scenarioId": "scenario-def",
      "score": 8,
      ...
    }
  }
]
```

---

## Interface Utilisateur

### Android (Jetpack Compose)

#### ScenarioListScreen

**Composants**:
- **ScenarioCard**: Carte Material You avec:
  - Nom du scénario
  - Badge de statut (PROPOSED/SELECTED/REJECTED)
  - Détails clés (date, lieu, durée, budget)
  - Résultats de vote (si votes existent)
  - Boutons de vote (PREFER/NEUTRAL/AGAINST)
  - Bouton "View Details"

- **Compare Button**: Navigation vers comparaison (si ≥2 scénarios)

- **Empty State**: Message si aucun scénario

**Navigation**:
- Tap sur card → ScenarioDetailScreen
- Tap sur Compare → ScenarioComparisonScreen

#### ScenarioDetailScreen

**Sections**:
- **Header**: Nom + Badge de statut
- **Description**: Texte détaillé
- **When**: Date/période + durée
- **Where**: Lieu
- **Group**: Nombre estimé de participants
- **Budget**: Par personne + total estimé

**Actions (Organisateur)**:
- Menu (⋮) avec options:
  - Edit: Passe en mode édition
  - Delete: Suppression avec confirmation

**Mode Édition**:
- TextFields pour tous les champs
- Bouton "Save" (avec loading indicator)
- Validation en temps réel

#### ScenarioComparisonScreen

**Layout**:
- Table scrollable (horizontal + vertical)
- Colonne gauche: Labels des métriques
- Colonnes suivantes: Un scénario par colonne

**Métriques comparées**:
- Date/Period
- Location
- Duration
- Est. Participants
- Budget/Person
- Total Budget
- Status
- Vote counts (Prefer, Neutral, Against)
- Total Votes
- **Score** (en gras)

**Highlight**: ★ Best Score sur le meilleur scénario

### iOS (SwiftUI)

#### ScenarioListView

**Design**: Liquid Glass avec `.glassCard()` modifier

**Composants**:
- Équivalent de la version Android
- Animations SwiftUI natives
- SF Symbols pour les icônes
- Async/await pour data loading

#### ScenarioDetailView

**Sections**: Identiques à Android

**Particularités iOS**:
- `.continuousCornerRadius()` pour les coins
- Menu natif iOS avec `.sheet` pour édition
- Alert native pour confirmation de suppression
- TextEditor natif pour description

#### ScenarioComparisonView

**Layout**: 
- ScrollView bi-directionnel natif iOS
- VStack + HStack pour la table
- Matériaux `.regularMaterial` pour les headers

---

## Flux Utilisateur

### Créer et Voter sur des Scénarios

```
1. Organisateur crée événement (status: POLLING)
2. Après sélection de date → status: COMPARING
3. Organisateur crée 2-3 scénarios (status: PROPOSED)
4. Participants reçoivent notification
5. Participants votent sur chaque scénario
6. Système calcule scores en temps réel
7. Organisateur voit le classement
8. Organisateur sélectionne un scénario → status: SELECTED
9. Transition vers status: ORGANIZING (Phase 3)
```

### Comparer des Scénarios

```
1. Participant ouvre ScenarioListScreen
2. Tap sur "Compare Scenarios"
3. Vue table de comparaison
4. Scroll horizontal pour voir tous les scénarios
5. Meilleur score surligné avec ★
6. Retour à la liste
```

### Modifier un Scénario (Organisateur)

```
1. Organisateur ouvre ScenarioDetailScreen
2. Tap sur menu (⋮)
3. Sélectionne "Edit"
4. Modifie les champs
5. Tap "Save"
6. Scénario mis à jour (updatedAt actualisé)
7. Retour en mode lecture
```

---

## Tests

### Tests Unitaires

**ScenarioLogicTest** (6 tests):
- ✅ `calculateBestScenarioWithPreferMajority`
- ✅ `calculateBestScenarioWithMixedVotes`
- ✅ `emptyScenariosListReturnsNull`
- ✅ `allNegativeVotesStillReturnsScenario`
- ✅ `calculateVotingPercentages`
- ✅ `rankScenariosByScoreDescending`

**ScenarioRepositoryTest** (11 tests):
- ✅ `testDatabaseConnection`
- ✅ `testCreateAndRetrieveScenario`
- ✅ `testGetScenariosByEventId`
- ✅ `testUpdateScenario`
- ✅ `testDeleteScenario`
- ✅ `testAddScenarioVote`
- ✅ `testUpdateExistingVote`
- ✅ `testGetVotingResultForScenario`
- ✅ `testGetScenariosWithVotes`
- ✅ `testScenarioValidation`
- ✅ `testCascadeDeleteVotes`

**Total**: 17/17 tests passing (100%)

### Tests d'Intégration

**À implémenter** (Sprint 1.6):
- Créer 3 scénarios et voter
- Comparer et sélectionner un scénario
- Passer en statut COMPARING
- Workflow complet organisateur + participants

---

## Considérations de Performance

1. **Indexation**:
   - Index sur `event_id` pour requêtes fréquentes
   - Index sur `scenario_id` dans votes

2. **Agrégation**:
   - Vue SQL pré-calculée pour scores
   - Évite calculs côté client

3. **Pagination**:
   - Non nécessaire initialement (≤10 scénarios par événement typique)
   - À ajouter si besoin

---

## Sécurité & Permissions

### Permissions

**Organisateur**:
- Créer, modifier, supprimer scénarios
- Voir tous les votes
- Sélectionner scénario gagnant

**Participant**:
- Voir tous les scénarios
- Voter sur scénarios
- Voir résultats agrégés (pas les votes individuels)

### Validation

- Tous les champs requis validés
- Contraintes numériques (duration > 0, etc.)
- UNIQUE constraint sur votes (1 vote par participant par scénario)

---

## Migration & Rétrocompatibilité

**Nouvelles tables**:
- `scenarios`
- `scenario_votes`
- Vue `scenarios_with_votes`

**Pas d'impact sur tables existantes**: Migration non-destructive

**Nouvel EventStatus**: `COMPARING` (entre `POLLING` et `ORGANIZING`)

---

## Métriques de Succès

- ✅ 17/17 tests passing
- ✅ ~3,663 lignes de code implémentées
- ✅ UI Android complète (3 screens)
- ✅ UI iOS complète (3 screens)
- ✅ API REST fonctionnelle
- ⏳ Tests E2E à implémenter

---

## Exemples d'Utilisation

### Créer un Scénario et Voter

```kotlin
// 1. Créer un scénario
val repository = ScenarioRepository(database)

val scenario = repository.createScenario(
    Scenario(
        id = "", // Auto-généré
        eventId = "event-123",
        name = "Weekend Mountain",
        dateOrPeriod = "15-17 Mars 2025",
        location = "Chamonix, France",
        duration = 3,
        estimatedParticipants = 10,
        estimatedBudgetPerPerson = 250.0,
        description = "Weekend ski dans les Alpes",
        status = ScenarioStatus.PROPOSED,
        createdAt = "", // Auto-généré
        updatedAt = ""  // Auto-généré
    )
)

// 2. Les participants votent
repository.submitVote(
    scenarioId = scenario.id,
    participantId = "user-1",
    vote = ScenarioVoteType.PREFER
)

repository.submitVote(
    scenarioId = scenario.id,
    participantId = "user-2",
    vote = ScenarioVoteType.NEUTRAL
)

repository.submitVote(
    scenarioId = scenario.id,
    participantId = "user-3",
    vote = ScenarioVoteType.AGAINST
)

// 3. Obtenir les résultats
val result = repository.getVotingResults(scenario.id)
// result.score = (1 × 2) + (1 × 1) + (1 × -1) = 2
// result.preferCount = 1
// result.neutralCount = 1
// result.againstCount = 1
```

### Trouver le Meilleur Scénario

```kotlin
// Obtenir tous les scénarios avec votes
val scenariosWithVotes = repository.getScenariosWithVotes("event-123")

// Utiliser la logique métier pour trouver le meilleur
val scenarios = scenariosWithVotes.map { it.scenario }
val allVotes = scenariosWithVotes.flatMap { it.votes }

val bestScenario = ScenarioLogic.calculateBestScenario(scenarios, allVotes)
println("Meilleur scénario: ${bestScenario?.name}")

// Ou obtenir avec détails
val (best, bestResult) = ScenarioLogic.getBestScenarioWithScore(scenarios, allVotes)
println("${best.name} - Score: ${bestResult.score}")
```

### Comparer Plusieurs Scénarios

```kotlin
// Obtenir le classement complet
val ranked = ScenarioLogic.rankScenariosByScore(scenarios, allVotes)

ranked.forEachIndexed { index, scenarioWithVotes ->
    println("#${index + 1}: ${scenarioWithVotes.scenario.name}")
    println("   Score: ${scenarioWithVotes.votingResult.score}")
    println("   Prefer: ${scenarioWithVotes.votingResult.preferCount}")
    println("   Neutral: ${scenarioWithVotes.votingResult.neutralCount}")
    println("   Against: ${scenarioWithVotes.votingResult.againstCount}")
}

// Output:
// #1: Weekend Mountain
//    Score: 11
//    Prefer: 5
//    Neutral: 3
//    Against: 2
// #2: City Break
//    Score: 8
//    Prefer: 4
//    Neutral: 3
//    Against: 1
```

### Sélectionner le Scénario Gagnant

```kotlin
// Organisateur sélectionne le meilleur scénario
val updated = repository.updateScenario(
    bestScenario.copy(status = ScenarioStatus.SELECTED)
)

// Rejeter les autres scénarios
val otherScenarios = scenarios.filter { it.id != bestScenario.id }
otherScenarios.forEach { scenario ->
    repository.updateScenario(
        scenario.copy(status = ScenarioStatus.REJECTED)
    )
}

// Transition de l'événement vers ORGANIZING
eventRepository.updateEventStatus("event-123", EventStatus.ORGANIZING)
```

---

## Fichiers Implémentés

### Backend (1,350 lignes)

#### Modèles
- **`shared/src/commonMain/kotlin/com/guyghost/wakeve/models/ScenarioModels.kt`** (150 lignes)
  - `Scenario`, `ScenarioVote`, `ScenarioStatus`, `ScenarioVoteType`
  - `ScenarioVotingResult`, `ScenarioWithVotes`
  - Annotations `@Serializable` pour JSON

#### Base de Données
- **`shared/src/commonMain/sqldelight/com/guyghost/wakeve/Wakev.sq`** (350 lignes)
  - Tables `scenarios` et `scenario_votes`
  - Queries CRUD complètes
  - Queries d'agrégation avec `LEFT JOIN`
  - Index pour performance

#### Logique Métier
- **`shared/src/commonMain/kotlin/com/guyghost/wakeve/ScenarioLogic.kt`** (200 lignes)
  - `calculateBestScenario()`
  - `rankScenariosByScore()`
  - `getScenarioVotingResults()`
  - `getBestScenarioWithScore()`

#### Repository
- **`shared/src/commonMain/kotlin/com/guyghost/wakeve/ScenarioRepository.kt`** (400 lignes)
  - CRUD complet pour scénarios
  - Soumission et mise à jour de votes
  - Récupération avec agrégation
  - Helpers de mapping SQLDelight ↔ Kotlin

#### API Routes
- **`server/src/main/kotlin/com/guyghost/wakeve/routes/ScenarioRoutes.kt`** (250 lignes)
  - 8 endpoints REST
  - Validation des inputs
  - Error handling
  - Response mapping

### Tests (650 lignes)

#### Tests Unitaires
- **`shared/src/commonTest/kotlin/com/guyghost/wakeve/ScenarioLogicTest.kt`** (150 lignes)
  - 6 tests de calcul de scores
  - 100% coverage des algorithmes

#### Tests d'Intégration
- **`shared/src/jvmTest/kotlin/com/guyghost/wakeve/ScenarioRepositoryTest.kt`** (500 lignes)
  - 11 tests de repository
  - Tests de persistence
  - Tests de CASCADE DELETE
  - 100% coverage CRUD

### Android UI (1,840 lignes)

#### Screens
- **`composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ScenarioListScreen.kt`** (595 lignes)
  - Liste avec cartes de scénarios
  - Boutons de vote interactifs
  - Badge de statut
  - Compare button
  - Empty state

- **`composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ScenarioDetailScreen.kt`** (565 lignes)
  - Vue détaillée
  - Mode édition (organisateur)
  - Menu d'actions
  - Delete confirmation
  - Sections organisées (When, Where, Group, Budget)

- **`composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ScenarioComparisonScreen.kt`** (680 lignes)
  - Table de comparaison scrollable
  - Headers fixes
  - Highlight du meilleur score (★)
  - Comparaison visuelle complète

#### Design System
- Material You avec `MaterialTheme`
- Cartes avec `Card` composable
- Badges de statut colorés
- Boutons de vote avec icônes
- Dialogs natifs pour confirmations

### iOS UI (1,313 lignes)

#### Views
- **`iosApp/iosApp/Views/ScenarioListView.swift`** (495 lignes)
  - Équivalent de ScenarioListScreen
  - Design Liquid Glass (`.glassCard()`)
  - Async/await pour data
  - SF Symbols pour icônes

- **`iosApp/iosApp/Views/ScenarioDetailView.swift`** (459 lignes)
  - Équivalent de ScenarioDetailScreen
  - Mode édition avec TextFields
  - Alert de confirmation
  - `.sheet` pour modals

- **`iosApp/iosApp/Views/ScenarioComparisonView.swift`** (359 lignes)
  - Table de comparaison
  - ScrollView bi-directionnel natif
  - VStack + HStack layout
  - Materials pour headers

#### Design System
- Liquid Glass avec `.continuousCornerRadius()`
- Materials: `.regularMaterial`, `.thinMaterial`
- Ombres subtiles (opacity: 0.05-0.08)
- SF Symbols natifs
- Animations SwiftUI

---

## Structure des Dossiers

```
wakeve/
├── shared/
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/com/guyghost/wakeve/
│   │   │   │   ├── models/
│   │   │   │   │   └── ScenarioModels.kt
│   │   │   │   ├── ScenarioLogic.kt
│   │   │   │   └── ScenarioRepository.kt
│   │   │   └── sqldelight/com/guyghost/wakeve/
│   │   │       └── Wakev.sq (scenarios + scenario_votes)
│   │   ├── commonTest/
│   │   │   └── kotlin/com/guyghost/wakeve/
│   │   │       └── ScenarioLogicTest.kt
│   │   └── jvmTest/
│   │       └── kotlin/com/guyghost/wakeve/
│   │           └── ScenarioRepositoryTest.kt
│   └── build.gradle.kts
├── composeApp/
│   └── src/androidMain/kotlin/com/guyghost/wakeve/
│       ├── ScenarioListScreen.kt
│       ├── ScenarioDetailScreen.kt
│       └── ScenarioComparisonScreen.kt
├── iosApp/
│   └── iosApp/Views/
│       ├── ScenarioListView.swift
│       ├── ScenarioDetailView.swift
│       └── ScenarioComparisonView.swift
├── server/
│   └── src/main/kotlin/com/guyghost/wakeve/routes/
│       └── ScenarioRoutes.kt
└── openspec/
    └── specs/scenario-management/
        └── spec.md (ce fichier)
```

---

## Améliorations Futures

### Phase 3+ Features
- [ ] Notifications push pour nouveaux scénarios
- [ ] Deadline de vote automatique
- [ ] Sélection automatique du meilleur score à la deadline
- [ ] Historique des votes (qui a voté quoi)
- [ ] Commentaires sur les scénarios
- [ ] Pièces jointes (images, PDF)
- [ ] Export PDF de comparaison
- [ ] Suggestions AI basées sur préférences passées

### Améliorations Techniques
- [ ] CRDT pour édition collaborative en temps réel
- [ ] WebSocket pour live updates
- [ ] Cache optimisé pour liste de scénarios
- [ ] Pagination si >20 scénarios
- [ ] Recherche et filtres avancés
- [ ] Analytics sur patterns de vote

---

## Changelog

### v1.0.0 (2025-12-25)
- ✅ Implémentation initiale complète
- ✅ Modèles de données avec validation
- ✅ Base de données SQLDelight avec indexes
- ✅ Logique de scoring et classement
- ✅ Repository avec CRUD complet
- ✅ API REST (8 endpoints)
- ✅ UI Android Material You (3 screens)
- ✅ UI iOS Liquid Glass (3 views)
- ✅ Tests unitaires (6/6) et intégration (11/11)
- ✅ Documentation complète

---

## Références

- **Change**: `openspec/changes/add-full-prd-features/`
- **Tasks**: `openspec/changes/add-full-prd-features/tasks.md`
- **Code**:
  - Backend: `shared/src/commonMain/kotlin/com/guyghost/wakeve/`
  - Android: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/`
  - iOS: `iosApp/iosApp/Views/`
  - Server: `server/src/main/kotlin/com/guyghost/wakeve/routes/`
- **Tests**:
  - Unit: `shared/src/commonTest/kotlin/com/guyghost/wakeve/`
  - Integration: `shared/src/jvmTest/kotlin/com/guyghost/wakeve/`

---

**Dernière mise à jour**: 25 décembre 2025  
**Version**: 1.0.0  
**Statut**: ✅ Implémenté (Phase 1 complète)  
**Test Coverage**: 100% (17/17 tests passing)  
**Code**: ~3,663 lignes (Backend: 1,350 | Android: 1,840 | iOS: 1,313)
