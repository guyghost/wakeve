# Proposal: Verify and Improve State Machine Workflow Connections

> **Change ID**: `verify-statemachine-workflow`  
> **Status**: Draft  
> **Created**: 2025-12-31  
> **Author**: Development Team  

---

## Context

Le projet Wakeve utilise une architecture FSM (Finite State Machine) basée sur le pattern MVI pour gérer le workflow utilisateur à travers trois state machines principales :

1. **EventManagementStateMachine** - Gestion des événements (création, listing, sélection)
2. **ScenarioManagementStateMachine** - Gestion des scénarios (comparaison, vote)
3. **MeetingServiceStateMachine** - Gestion des réunions virtuelles (création de liens Zoom/Meet/FaceTime)

Maintenant que tout compile, nous devons **vérifier et améliorer les connexions** entre ces state machines pour garantir un workflow utilisateur fluide et cohérent selon les phases du cycle de vie d'un événement.

## Problem Statement

### Issues potentiels identifiés

1. **Transitions entre state machines** : Comment passe-t-on d'EventManagement → ScenarioManagement → MeetingService ?
2. **État partagé** : Comment les state machines communiquent-elles les changements d'état (ex: event confirmé → déverrouiller scenarios) ?
3. **Navigation workflow** : Les SideEffects de navigation sont-ils cohérents avec le cycle de vie de l'événement ?
4. **Gestion des erreurs** : Les erreurs d'une state machine doivent-elles impacter les autres ?
5. **Offline-first** : Comment les state machines gèrent-elles les actions en file d'attente offline ?

### Cycle de vie attendu

Selon `openspec/project.md` et `openspec/specs/event-organization/spec.md`, le workflow attendu est :

```
1. DRAFT        → EventManagement (créer événement, ajouter participants)
2. POLLING      → EventManagement (vote sur créneaux)
3. SCENARIO     → ScenarioManagement (comparer scenarios, voter)
4. CONFIRMED    → EventManagement (confirmer date finale)
5. ORGANIZATION → MeetingService (créer liens réunion, planifier logistique)
6. FINALIZED    → Tous les détails confirmés
```

## Goals

1. **Vérifier la cohérence du workflow** entre les 3 state machines
2. **Identifier les gaps** dans les transitions entre états/phases
3. **Proposer des améliorations** pour connecter les state machines
4. **Documenter le workflow complet** dans une spec unifiée
5. **Créer des tests end-to-end** pour valider les transitions

## Non-Goals

- Refactoriser l'architecture MVI/FSM existante (déjà implémentée)
- Ajouter de nouvelles fonctionnalités (seulement connecter l'existant)
- Modifier le cycle de vie des événements (respecter les specs existantes)

## Proposed Solution

### Phase 1 : Audit du workflow actuel

1. **Mapper les transitions actuelles** :
   - Quels Intents déclenchent des changements de phase ?
   - Quels SideEffects déclenchent des navigations ?
   - Comment les ViewModels communiquent-ils entre eux ?

2. **Identifier les gaps** :
   - Transitions manquantes (ex: POLLING → SCENARIO)
   - États incohérents (ex: scenarios accessibles en DRAFT ?)
   - Données partagées non synchronisées

### Phase 2 : Proposition d'améliorations

1. **Workflow Coordinator** (optionnel) :
   - Un coordinator central qui orchestre les transitions entre state machines
   - Gère les règles métier (ex: "scenarios déverrouillés après CONFIRMED")

2. **Event Status Observability** :
   - Toutes les state machines observent `Event.status`
   - Les transitions s'adaptent automatiquement selon le status

3. **Shared State Repository** :
   - Un repository partagé qui notifie tous les observers des changements d'état
   - Garantit la cohérence entre state machines

4. **Navigation Graph unifiée** :
   - Documenter toutes les transitions de navigation dans un graph complet
   - Valider que chaque SideEffect.NavigateTo a une destination valide

### Phase 3 : Spécifications

Créer une spec unifiée `workflow-coordination` qui définit :

- Les règles de transition entre phases
- Les dépendances entre state machines
- Les invariants à respecter (ex: pas de scenarios en DRAFT)
- Les side effects de navigation attendus

### Phase 4 : Tests end-to-end

Créer des tests qui valident le workflow complet :

```kotlin
@Test
fun `complete event workflow from creation to finalized`() {
    // 1. Create event (DRAFT)
    eventViewModel.dispatch(Intent.CreateEvent(...))
    
    // 2. Start polling (POLLING)
    eventViewModel.dispatch(Intent.StartPoll(...))
    
    // 3. Participants vote
    eventViewModel.dispatch(Intent.Vote(...))
    
    // 4. Confirm date (CONFIRMED)
    eventViewModel.dispatch(Intent.ConfirmDate(...))
    
    // 5. Create scenarios (SCENARIO_COMPARISON)
    scenarioViewModel.dispatch(Intent.CreateScenario(...))
    
    // 6. Vote scenarios
    scenarioViewModel.dispatch(Intent.VoteScenario(...))
    
    // 7. Select scenario (CONFIRMED)
    scenarioViewModel.dispatch(Intent.SelectScenario(...))
    
    // 8. Create meeting (ORGANIZATION)
    meetingViewModel.dispatch(Intent.CreateMeeting(...))
    
    // Assertions:
    // - Chaque phase débloque la suivante
    // - Les données sont cohérentes
    // - Les side effects sont émis correctement
}
```

## Impact

### Benefits

- **Cohérence garantie** : Le workflow utilisateur sera fluide et sans incohérences
- **Moins de bugs** : Les tests end-to-end détectent les régressions
- **Documentation claire** : Les développeurs comprennent le workflow complet
- **Maintenabilité** : Les règles de transition sont explicites et testées

### Risks

- **Complexité** : Introduire un coordinator peut ajouter de la complexité
- **Performance** : Observer l'état partagé peut impacter les performances (mitigation: StateFlow avec replay=1)
- **Breaking changes** : Modifier les transitions peut impacter l'UI existante

## Success Criteria

1. ✅ Toutes les transitions du cycle de vie sont testées end-to-end
2. ✅ Un document `workflow-coordination/spec.md` décrit le workflow complet
3. ✅ Aucun gap dans les transitions entre phases
4. ✅ Les state machines respectent les invariants métier
5. ✅ Les tests passent à 100% (au moins 10 nouveaux tests end-to-end)

## Open Questions

1. **Coordinator pattern** : Faut-il introduire un WorkflowCoordinator ou les state machines communiquent directement via repository ?
2. **Navigation** : Qui gère la navigation ? Les ViewModels ou une NavController centralisée ?
3. **Offline queue** : Comment garantir l'ordre des actions offline (ex: créer event → ajouter participant) ?
4. **Error propagation** : Une erreur dans ScenarioManagement doit-elle bloquer EventManagement ?

## Related Specifications

- `openspec/specs/event-organization/spec.md` - Cycle de vie des événements
- `openspec/specs/scenario-management/spec.md` - Gestion des scénarios
- `openspec/specs/meeting-service/spec.md` - Génération de liens réunion
- `openspec/specs/collaboration-management/spec.md` - Collaboration entre participants

## Next Steps

1. **Review** : Valider cette proposition avec l'équipe
2. **Audit** : Mapper les transitions actuelles (Phase 1)
3. **Spec** : Rédiger `workflow-coordination/spec.md` (Phase 2)
4. **Implementation** : Implémenter les améliorations nécessaires (Phase 3)
5. **Tests** : Créer les tests end-to-end (Phase 4)
6. **Documentation** : Mettre à jour la doc projet (Phase 5)
