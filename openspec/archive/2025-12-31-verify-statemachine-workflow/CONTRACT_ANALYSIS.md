# Phase 2 - Analyse des Contracts

> **Change ID**: `verify-statemachine-workflow`  
> **Phase**: Phase 2 - Analyse des Contracts  
> **Date**: 2025-12-31  

---

## 📋 Vue d'ensemble

Cette analyse examine les **3 Contracts** (State/Intent/SideEffect) pour identifier les incohérences, les Intents manquants, et les améliorations nécessaires pour supporter le workflow complet.

---

## 🔍 EventStatus - Modèle Actuel

### Dans `models/Event.kt`

```kotlin
enum class EventStatus {
    DRAFT,       // En cours de création
    POLLING,     // Vote sur créneaux actif
    COMPARING,   // ⚠️ Différent de SCENARIO_COMPARISON
    CONFIRMED,   // Date confirmée
    ORGANIZING,  // ⚠️ Différent de ORGANIZATION
    FINALIZED    // Tous détails confirmés
}
```

**🔴 Incohérence détectée** : Les noms de statuts dans le code diffèrent de ceux documentés dans les specs.

**Mapping** :
- `COMPARING` = `SCENARIO_COMPARISON` (dans specs)
- `ORGANIZING` = `ORGANIZATION` (dans specs)

---

## 📊 Analyse Contract par Contract

### 1. EventManagementContract ✅

**Fichier** : `presentation/state/EventManagementContract.kt`

#### State

```kotlin
data class State(
    val isLoading: Boolean = false,
    val events: List<Event> = emptyList(),
    val selectedEvent: Event? = null,
    val participantIds: List<String> = emptyList(),
    val pollVotes: Map<String, Map<String, Vote>> = emptyMap(),
    val error: String? = null
)
```

**✅ Complétude** : State contient toutes les données nécessaires.

**⚠️ Manque** : Pas de champ pour savoir si les scenarios/meetings sont déverrouillés.

**Recommandation** :
```kotlin
data class State(
    // ... existing fields
    val scenariosUnlocked: Boolean = false,
    val meetingsUnlocked: Boolean = false
)
```

#### Intents Actuels

| Intent | Implémenté | Note |
|--------|-----------|------|
| `LoadEvents` | ✅ | OK |
| `SelectEvent` | ✅ | OK |
| `CreateEvent` | ✅ | OK |
| `UpdateEvent` | ✅ | OK |
| `DeleteEvent` | ⚠️ | TODO (non implémenté) |
| `LoadParticipants` | ✅ | OK |
| `AddParticipant` | ✅ | OK |
| `LoadPollResults` | ✅ | OK |
| `ClearError` | ✅ | OK |

**🔴 Intents Manquants** :

1. **`StartPoll`** : Transition DRAFT → POLLING
   ```kotlin
   data class StartPoll(val eventId: String) : Intent
   ```

2. **`ConfirmDate`** : Transition POLLING → CONFIRMED + navigation vers scenarios
   ```kotlin
   data class ConfirmDate(val eventId: String, val slotId: String) : Intent
   ```

3. **`TransitionToOrganizing`** : Transition CONFIRMED → ORGANIZING
   ```kotlin
   data class TransitionToOrganizing(val eventId: String) : Intent
   ```

4. **`MarkAsFinalized`** : Transition ORGANIZING → FINALIZED
   ```kotlin
   data class MarkAsFinalized(val eventId: String) : Intent
   ```

#### SideEffects Actuels

| SideEffect | Usage | Note |
|------------|-------|------|
| `ShowToast(message)` | ✅ | OK |
| `NavigateTo(route)` | ✅ | OK - utilisé pour "detail/$id" |
| `NavigateBack` | ✅ | OK |

**🔴 SideEffects Manquants** :

1. **Navigation vers Scenarios** après ConfirmDate
   - Actuellement : Pas de navigation automatique
   - Besoin : `NavigateTo("scenarios/$eventId")`

2. **Navigation vers Meetings** après TransitionToOrganizing
   - Besoin : `NavigateTo("meetings/$eventId")`

---

### 2. ScenarioManagementContract ✅⚠️

**Fichier** : `presentation/state/ScenarioManagementContract.kt`

#### State

```kotlin
data class State(
    val isLoading: Boolean = false,
    val eventId: String = "",
    val participantId: String = "",
    val scenarios: List<ScenarioWithVotes> = emptyList(),
    val selectedScenario: Scenario? = null,
    val votingResults: Map<String, ScenarioVotingResult> = emptyMap(),
    val comparison: ScenarioComparison? = null,
    val error: String? = null
)
```

**✅ Complétude** : State est complet.

**⚠️ Manque** : Pas de champ pour `eventStatus` (nécessaire pour validation).

**Recommandation** :
```kotlin
data class State(
    // ... existing fields
    val eventStatus: EventStatus? = null, // Pour validation des invariants
)
```

#### Intents Actuels

| Intent | Implémenté | Note |
|--------|-----------|------|
| `LoadScenariosForEvent` | ✅ | OK (preferred) |
| `LoadScenarios` | ✅ | OK (legacy) |
| `CreateScenario` | ✅ | ⚠️ Pas de validation status |
| `SelectScenario` | ✅ | ⚠️ Utilisé pour 2 choses différentes |
| `UpdateScenario` | ✅ | OK |
| `DeleteScenario` | ✅ | OK |
| `VoteScenario` | ✅ | OK |
| `CompareScenarios` | ✅ | OK |
| `ClearComparison` | ✅ | OK |
| `ClearError` | ✅ | OK |

**🔴 Intents Manquants** :

1. **`SelectScenarioAsFinal`** : Sélectionner le scenario final et transitionner vers ORGANIZING
   ```kotlin
   data class SelectScenarioAsFinal(
       val eventId: String,
       val scenarioId: String
   ) : Intent
   ```

   **Note** : Actuellement `SelectScenario` est utilisé à la fois pour :
   - Navigation vers détail (lecture seule)
   - Sélection comme scenario final (action organisateur)
   
   Il faut séparer ces 2 cas.

#### SideEffects Actuels

| SideEffect | Usage | Note |
|------------|-------|------|
| `ShowToast(message)` | ✅ | OK |
| `ShowError(message)` | ✅ | OK |
| `NavigateTo(route)` | ✅ | OK - "scenario/$id", "scenarios/compare" |
| `NavigateBack` | ✅ | OK |
| `ShareScenario(scenario)` | ✅ | OK (pas encore utilisé) |

**🔴 SideEffects Manquants** :

1. **Navigation vers Meetings** après SelectScenarioAsFinal
   - Besoin : `NavigateTo("meetings/$eventId")`

---

### 3. MeetingManagementContract ✅

**Fichier** : `presentation/state/MeetingManagementContract.kt`

#### State

```kotlin
data class State(
    val isLoading: Boolean = false,
    val meetings: List<VirtualMeeting> = emptyList(),
    val selectedMeeting: VirtualMeeting? = null,
    val eventId: String = "",
    val generatedLink: MeetingLinkResponse? = null,
    val error: String? = null
)
```

**✅ Complétude** : State est complet.

**⚠️ Manque** : Pas de champ pour `eventStatus` (nécessaire pour validation).

**Recommandation** :
```kotlin
data class State(
    // ... existing fields
    val eventStatus: EventStatus? = null, // Pour validation des invariants
)
```

#### Intents Actuels

| Intent | Implémenté | Note |
|--------|-----------|------|
| `LoadMeetings` | ✅ | OK |
| `CreateMeeting` | ✅ | ⚠️ Pas de validation status |
| `UpdateMeeting` | ✅ | OK |
| `CancelMeeting` | ✅ | OK |
| `GenerateMeetingLink` | ✅ | OK |
| `SelectMeeting` | ✅ | OK |
| `ClearGeneratedLink` | ✅ | OK |
| `ClearError` | ✅ | OK |

**✅ Intents** : Complets pour MeetingService.

#### SideEffects Actuels

| SideEffect | Usage | Note |
|------------|-------|------|
| `ShowToast(message)` | ✅ | OK |
| `ShowError(message)` | ✅ | OK |
| `NavigateTo(route)` | ✅ | OK - "meeting/$id" |
| `NavigateBack` | ✅ | OK |
| `ShareMeetingLink(link)` | ✅ | OK |

**✅ SideEffects** : Complets pour MeetingService.

---

## 🚨 Résumé des Incohérences

### 1. Noms de Statuts (EventStatus)

| Specs | Code | Impact |
|-------|------|--------|
| `SCENARIO_COMPARISON` | `COMPARING` | ⚠️ Incohérence nommage |
| `ORGANIZATION` | `ORGANIZING` | ⚠️ Incohérence nommage |

**Recommandation** : Utiliser les noms du code (`COMPARING`, `ORGANIZING`) et mettre à jour les specs.

### 2. Intent SelectScenario - Ambiguïté

**Problème** : `SelectScenario` est utilisé pour 2 cas :
1. Navigation vers détail (lecture seule) - actuellement implémenté
2. Sélection comme scenario final (action organisateur) - non implémenté

**Solution** : Créer `SelectScenarioAsFinal` distinct.

### 3. Validation des Invariants - Absente

**Problème** : Aucun Contract ne stocke `eventStatus` pour validation.

**Impact** : Impossible de valider les règles métier (ex: scenarios après CONFIRMED).

**Solution** : Ajouter `eventStatus` dans State de chaque Contract.

---

## 📝 Modifications Nécessaires aux Contracts

### EventManagementContract - Modifications

#### 1. Ajouter au State

```kotlin
data class State(
    // ... existing fields
    val scenariosUnlocked: Boolean = false,
    val meetingsUnlocked: Boolean = false
) {
    // ... existing helpers
    
    /**
     * Check if scenarios are available based on event status
     */
    fun canAccessScenarios(): Boolean =
        selectedEvent?.status in listOf(
            EventStatus.COMPARING,
            EventStatus.CONFIRMED,
            EventStatus.ORGANIZING,
            EventStatus.FINALIZED
        )
    
    /**
     * Check if meetings are available based on event status
     */
    fun canAccessMeetings(): Boolean =
        selectedEvent?.status in listOf(
            EventStatus.CONFIRMED,
            EventStatus.ORGANIZING,
            EventStatus.FINALIZED
        )
}
```

#### 2. Ajouter aux Intents

```kotlin
sealed interface Intent {
    // ... existing intents
    
    /**
     * Start polling on time slots.
     * 
     * Transitions event from DRAFT to POLLING.
     * Only the organizer can start polling.
     * 
     * @property eventId The ID of the event to start polling for
     */
    data class StartPoll(val eventId: String) : Intent
    
    /**
     * Confirm the final date for an event.
     * 
     * Transitions event from POLLING to CONFIRMED.
     * Only the organizer can confirm the date.
     * At least one participant must have voted.
     * 
     * @property eventId The ID of the event
     * @property slotId The ID of the selected time slot
     */
    data class ConfirmDate(
        val eventId: String,
        val slotId: String
    ) : Intent
    
    /**
     * Transition event to organizing phase.
     * 
     * Transitions event from CONFIRMED to ORGANIZING.
     * Only the organizer can trigger this transition.
     * A scenario must have been selected.
     * 
     * @property eventId The ID of the event
     */
    data class TransitionToOrganizing(val eventId: String) : Intent
    
    /**
     * Mark event as finalized.
     * 
     * Transitions event from ORGANIZING to FINALIZED.
     * Only the organizer can finalize.
     * All critical details must be confirmed.
     * 
     * @property eventId The ID of the event
     */
    data class MarkAsFinalized(val eventId: String) : Intent
}
```

#### 3. Pas de modification aux SideEffects

Les SideEffects existants suffisent (`NavigateTo` est flexible).

---

### ScenarioManagementContract - Modifications

#### 1. Ajouter au State

```kotlin
data class State(
    // ... existing fields
    val eventStatus: EventStatus? = null
) {
    // ... existing helpers
    
    /**
     * Check if scenarios can be created based on event status
     */
    fun canCreateScenarios(): Boolean =
        eventStatus in listOf(
            EventStatus.COMPARING,
            EventStatus.CONFIRMED
        )
    
    /**
     * Check if a scenario can be selected as final
     */
    fun canSelectScenarioAsFinal(): Boolean =
        eventStatus == EventStatus.COMPARING
}
```

#### 2. Ajouter aux Intents

```kotlin
sealed interface Intent {
    // ... existing intents
    
    /**
     * Select a scenario as the final choice for the event.
     * 
     * This is different from SelectScenario (which navigates to detail).
     * Only the organizer can select the final scenario.
     * Updates event status from COMPARING to CONFIRMED.
     * Unlocks meeting creation.
     * 
     * @property eventId The ID of the event
     * @property scenarioId The ID of the scenario to select as final
     */
    data class SelectScenarioAsFinal(
        val eventId: String,
        val scenarioId: String
    ) : Intent
}
```

#### 3. Clarifier SelectScenario

```kotlin
/**
 * Select a scenario for viewing details.
 * 
 * Sets selectedScenario and loads its voting data.
 * Emits NavigateTo side effect to navigate to detail screen.
 * 
 * NOTE: This is for navigation/viewing only. To select a scenario
 * as the final choice, use SelectScenarioAsFinal.
 * 
 * @property scenarioId The ID of the scenario to view
 */
data class SelectScenario(val scenarioId: String) : Intent
```

#### 4. Pas de modification aux SideEffects

Les SideEffects existants suffisent.

---

### MeetingManagementContract - Modifications

#### 1. Ajouter au State

```kotlin
data class State(
    // ... existing fields
    val eventStatus: EventStatus? = null
) {
    // ... existing helpers
    
    /**
     * Check if meetings can be created based on event status
     */
    fun canCreateMeetings(): Boolean =
        eventStatus in listOf(
            EventStatus.CONFIRMED,
            EventStatus.ORGANIZING,
            EventStatus.FINALIZED
        )
}
```

#### 2. Pas de modification aux Intents

Les Intents existants sont complets.

#### 3. Pas de modification aux SideEffects

Les SideEffects existants sont complets.

---

## 🎯 Matrice de Modifications

| Contract | State | Intents | SideEffects |
|----------|-------|---------|-------------|
| **EventManagement** | +2 champs (scenariosUnlocked, meetingsUnlocked) + 2 helpers | +4 Intents (StartPoll, ConfirmDate, TransitionToOrganizing, MarkAsFinalized) | ✅ OK |
| **ScenarioManagement** | +1 champ (eventStatus) + 2 helpers | +1 Intent (SelectScenarioAsFinal) + clarifier SelectScenario | ✅ OK |
| **MeetingManagement** | +1 champ (eventStatus) + 1 helper | ✅ OK | ✅ OK |

**Total** : 4 champs, 5 nouveaux Intents, 5 helpers

---

## 📊 Diagramme des Transitions avec Nouveaux Intents

```
EventManagementStateMachine
    │
    ├─ CreateEvent ──────────> Event(status=DRAFT)
    │
    ├─ StartPoll ────────────> Event(status=POLLING)
    │   [NEW INTENT]
    │
    ├─ ConfirmDate ──────────> Event(status=CONFIRMED)
    │   [NEW INTENT]            ↓
    │                          NavigateTo("scenarios/$id")
    │
    └─ TransitionToOrganizing > Event(status=ORGANIZING)
        [NEW INTENT]            ↓
                               NavigateTo("meetings/$id")

ScenarioManagementStateMachine
    │
    ├─ LoadScenariosForEvent ─> Load scenarios
    │   ⚠️ Guard: eventStatus == COMPARING || CONFIRMED
    │
    ├─ CreateScenario ────────> Create scenario
    │   ⚠️ Guard: eventStatus == COMPARING || CONFIRMED
    │
    ├─ VoteScenario ──────────> Vote on scenario
    │
    └─ SelectScenarioAsFinal ─> Event(status=CONFIRMED)
        [NEW INTENT]            ↓
                               NavigateTo("meetings/$id")

MeetingServiceStateMachine
    │
    ├─ LoadMeetings ──────────> Load meetings
    │
    └─ CreateMeeting ─────────> Create meeting
        ⚠️ Guard: eventStatus == CONFIRMED || ORGANIZING || FINALIZED
```

---

## ✅ Validation des Invariants avec Nouveaux Contracts

| Invariant | Contract | Validation |
|-----------|----------|------------|
| 1. Ordre des phases | EventManagement | ✅ StartPoll, ConfirmDate, TransitionToOrganizing, MarkAsFinalized respectent l'ordre |
| 2. Pas de retour arrière | EventManagement | ✅ Pas d'Intents pour retour arrière |
| 3. Scenarios après CONFIRMED | ScenarioManagement | ✅ canCreateScenarios() vérifie status |
| 4. Meetings après CONFIRMED | MeetingManagement | ✅ canCreateMeetings() vérifie status |
| 5. Votes après deadline | EventManagement | ⚠️ À implémenter dans StateMachine |
| 6. Actions organisateur | Tous | ⚠️ À implémenter guards dans StateMachines |
| 7. Au moins 1 vote | EventManagement | ⚠️ À valider dans ConfirmDate handler |

**Progrès** : 4/7 invariants validés par les Contracts (3 restent à implémenter dans StateMachines).

---

## 🚀 Plan d'Implémentation

### Étape 1 : Modifier les Contracts (Cette session)

1. ✅ Analyser les Contracts actuels (complété)
2. ⏳ Modifier EventManagementContract.kt
3. ⏳ Modifier ScenarioManagementContract.kt
4. ⏳ Modifier MeetingManagementContract.kt
5. ⏳ Valider compilation

### Étape 2 : Implémenter dans StateMachines (Phase 3)

1. EventManagementStateMachine : Ajouter handlers pour nouveaux Intents
2. ScenarioManagementStateMachine : Ajouter SelectScenarioAsFinal handler
3. MeetingServiceStateMachine : Ajouter guards de validation
4. Implémenter observation du repository

### Étape 3 : Tests (Phase 4)

1. Tests unitaires pour nouveaux Intents
2. Tests end-to-end workflow complet
3. Tests validation invariants

---

## 📚 Documentation Mise à Jour

### Specs à Corriger

1. **`workflow-coordination/spec.md`** : Remplacer `SCENARIO_COMPARISON` par `COMPARING`, `ORGANIZATION` par `ORGANIZING`
2. **`event-organization/spec.md`** : Ajouter requirements pour StartPoll et ConfirmDate

---

## ✅ Conclusion Phase 2

**✅ Analyse complète** : 3 Contracts analysés, incohérences identifiées, modifications spécifiées.

**Prochaine étape** : Implémenter les modifications aux Contracts (Phase 2 continuation).

**Ready to implement?** 🚀
