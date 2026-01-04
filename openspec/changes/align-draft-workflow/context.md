# Context: Align DRAFT Workflow with UI Interfaces

## Objectif

Aligner et documenter le workflow DRAFT phase pour assurer une cohérence cross-platform entre Android (Jetpack Compose) et iOS (SwiftUI). Ce changement est documentaire et définit la référence unique pour le workflow DRAFT orchestré par la state machine.

## Contraintes

- **Plateforme** : Cross-platform (Android, iOS, JVM, Web)
- **Offline-first** : Oui (auto-save en background)
- **Design System** : Material You (Android) + Liquid Glass (iOS)
- **Architecture** : MVI + Finite State Machine (FSM)

## Décisions Techniques

| Décision | Justification | Agent |
|----------|---------------|-------|
| Workflow DRAFT en 4 étapes (Basic Info → Participants → Locations → Time Slots) | Déjà implémenté sur Android/iOS, UX validée | orchestrator |
| Auto-save à chaque transition d'étape | Prévient la perte de données, déjà implémenté | orchestrator |
| Validation stricte avant navigation | Évite les événements invalides, guide l'utilisateur | orchestrator |
| State Machine orchestre la navigation (Intents + Side Effects) | Centralise la logique, cohérence cross-platform | orchestrator |
| Déprécier EventCreationScreen.kt au profit de DraftEventWizard | Meilleure UX, wizard progressif | orchestrator |
| @Deprecated avec level = WARNING | Avertissement à la compilation, pas d'erreur bloquante | @codegen |
| Timeline: WARNING → next minor log → next major removal | Migration progressive, pas de breaking change immédiat | @codegen |

## Artéfacts Produits

| Fichier | Agent | Status |
|---------|-------|--------|
| openspec/changes/align-draft-workflow/proposal.md | orchestrator | ✅ Créé |
| openspec/changes/align-draft-workflow/tasks.md | orchestrator | ✅ Créé |
| openspec/changes/align-draft-workflow/specs/workflow-coordination/spec.md | orchestrator | ✅ Créé |
| openspec/changes/align-draft-workflow/context.md | orchestrator | ✅ Créé |
| composeApp/src/commonMain/kotlin/com/guyghost/wakeve/EventCreationScreen.kt | @codegen | ✅ Déprécié |
| shared/src/commonTest/kotlin/com/guyghost/wakeve/workflow/DraftWorkflowIntegrationTest.kt | @tests | ✅ Créé |

## Notes Inter-Agents

- [@tests] **DraftWorkflowIntegrationTest créé avec 8 scénarios de test**
  - Emplacement: `shared/src/commonTest/kotlin/com/guyghost/wakeve/workflow/DraftWorkflowIntegrationTest.kt`
  - Tests couverts:
    1. Complete DRAFT Wizard Flow (Step 1-4 → Create Event)
    2. Auto-save at Each Step (persistence verification)
    3. Validation Blocks Invalid Data (empty title, invalid counts)
    4. Skip Optional Fields (minimal event creation)
    5. Full Data Creation (all optional fields filled)
    6. Recovery After Interruption (state preservation)
    7. Add/Remove Locations (location management in Step 3)
    8. Multiple Time Slots with Different TimeOfDay (slot persistence)
  - Architecture: MockEventRepository (in-memory), AAA pattern (Arrange, Act, Assert)
  - Status: Prêt pour exécution (compiles sans erreurs)

- [@codegen → @integrator] **EventCreationScreen est encore utilisé dans le code de production**
  - `WakevNavHost.kt:150` - Route `Screen.EventCreation.route`
  - `App.kt:248` - Écran `Screen.EVENT_CREATION`
  - Ces utilisations devront être migrées vers `DraftEventWizard` dans une PR ultérieure
  - La migration complète est prévue pour la prochaine version majeure
  
- [@codegen → @review] Le commentaire de dépréciation inclut:
  - Exemple de code "OLD vs NEW"
  - Liste des bénéfices de DraftEventWizard
  - Timeline de dépréciation claire
  - Lien vers le guide de migration

## Fichiers Existantants

### Android (Jetpack Compose)
- ✅ `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/DraftEventWizard.kt` - Wizard en 4 étapes
- ⚠️ `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/EventCreationScreen.kt` - **À déprécier**

### iOS (SwiftUI)
- ✅ `iosApp/iosApp/Views/DraftEventWizardView.swift` - Wizard en 4 étapes
- ✅ `iosApp/iosApp/Views/CreateEventView.swift` - Wrapper pour DraftEventWizard

### Shared (Kotlin)
- ✅ `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/EventManagementStateMachine.kt` - Orchestration
- ✅ `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state/EventManagementContract.kt` - Intents, State, SideEffects
- ✅ `shared/src/commonMain/kotlin/com/guyghost/wakeve/models/Event.kt` - Event model enrichi

## Workflow de Migration

1. **Phase 1** : Documentation du workflow DRAFT
2. **Phase 2** : Création des diagrammes (séquence, flux, états)
3. **Phase 3** : Dépréciation de EventCreationScreen.kt
4. **Phase 4** : Tests d'intégration du workflow
5. **Phase 5** : Documentation complémentaire (guides développeurs)
6. **Phase 6** : Review et validation
7. **Phase 7** : Finalisation et archivage

## État Actuel

- **Enhanced DRAFT Phase** : 79/81 tâches (presque complet)
- **EventCreationScreen.kt** : Toujours utilisé, à déprécier
- **DraftEventWizard** : Implémenté sur Android et iOS
- **State Machine** : Orchestre déjà le workflow via Intents
