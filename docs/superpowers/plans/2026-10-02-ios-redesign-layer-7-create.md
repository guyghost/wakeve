# Refonte iOS — Couche 7 (création `CreateEventFlow`) — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal :** Sous `iosRedesign2026`, ＋ ouvre un flux de création en 4 questions — **Quoi ? · Qui ? · Où ? · Quand ?** — une question par écran, brouillon enregistré à chaque étape, validation par étape (règles DRAFT d'AGENTS.md), dernière étape « Lancer le sondage » (`StartPoll`) puis ouverture du hub.

**Branche :** `claude/ios-redesign-layer-7` (empilée sur `claude/ios-app-design-e1ae98`, PR #46).

**Décisions (2026-10-02) :**
- ~~＋ ouvre toujours le flux 4 questions~~ — **révisé le 2026-10-02 (décision produit)** : sous la refonte, ＋ ouvre le flux 4 questions **seulement** quand `iosInvitationExperienceV1` est éteint ; allumé, le studio reste le point d'entrée (`CreateFlowEntry.newEventRoute(redesign:invitationRollout:)`), car le flux ne synchronise pas encore les événements avec le serveur (lacune du code partagé, Swarm DAO #48). Les brouillons créés dans le studio continuent de s'y rouvrir (`editDraftFromHome` inchangé pour eux).
- Les brouillons créés par le flux se rouvrent dans le flux (étape de la première validation en échec).
- Persistance (contraintes du code partagé, cf. exploration) : création par `CreateEvent` à la sortie de l'étape 1 ; ensuite `UpdateEvent` avec l'événement **relu** depuis le dépôt (révision d'agrégat à jour) pour titre/description/créneaux (chemin `saveEvent` qui synchronise les créneaux) ; `UpdateDraftEvent` pour type et effectifs (et `UpdateEvent` pour remettre une valeur à nil) ; lieux persistés en SQL comme `persistCreationContext` (l'intent `AddPotentialLocation` ne persiste pas) ; lancement par `StartPoll(eventId, userId)` (réutiliser `EventPollStartController`).
- « Qui ? » : effectifs min / attendu / max (optionnels) + mention « Tu inviteras ton groupe juste après le lancement » ; pas de saisie d'e-mails dans cette couche (le partage du lien reste le chemin principal).
- Modèles ex-Explorer : puces `WKChip` sur `EventScenario.allScenarios` (sélection = pré-remplit titre/description/type + checklist via `EventCreationContext`).
- `CreateEventSheet`, `CreateEventViewModel`, `EventCreationStudioView` et les `case` legacy restent intacts.

**Spec :** §5.5 — **Proposition :** Swarm DAO #47.

## Architecture

1. **Pur** `iosApp/src/Models/Create/CreateEventFlowModel.swift` :
   - `enum CreateEventFlowStep: Int, CaseIterable { what, who, where, when }` (identifiants Swift valides : `place`, `time` pour `where`/`when`).
   - `struct CreateEventForm` (titre, description, `eventTypeName: String`, `eventTypeCustom`, `minParticipants`, `expectedParticipants`, `maxParticipants: Int?`, `locations: [String]`, `slots: [EventTimeSlotInput]`, `scenarioId: String?`).
   - `func errors(for step) -> [Field: String]` (clés de message) : titre et description non vides après trim ; CUSTOM → libellé requis ; effectifs ≥ 1 ; `max ≥ min` si les deux ; lieux : noms non vides, sans doublon (insensible à la casse) ; créneaux : au moins 1, un `SPECIFIC` exige début < fin.
   - `firstInvalidStep`, `progress` (segments), `apply(scenario:)`.
2. **Contrôleur** `iosApp/src/ViewModels/EventDraftFlowController.swift` (`@MainActor ObservableObject`), une machine `IosFactory.shared.createEventStateMachine(database:eventRepository:)` (schéma `EventDraftDatesController` : règlement sur `ShowToast` puis relecture du dépôt, reconstruction de `WakeveEvent` champ par champ, `dispose` en `deinit`) :
   - `save(step:form:) async -> Result` ; `eventId` créé à la première sauvegarde (construction reprise de `CreateEventViewModel.createEvent`) ; publie `lastSavedAt`, `isSaving`, `errorMessage`.
   - `hydrate(eventId:)` pour reprendre un brouillon (événement + `potentialLocationQueries.selectByEventId`).
   - `launch() async` → `StartPoll` ; succès quand le statut devient `POLLING` ; mappe `POLL_REQUIRES_TIME_SLOT_MESSAGE`.
   - Erreurs Kotlin non déclarées : n'appeler que des chemins via la machine (qui convertit les erreurs en `ShowToast`) ou des requêtes SQL simples.
3. **Vues** `iosApp/src/Views/Create/` : `CreateEventFlow(userId:draftEventId:initialScenario:onClose:onLaunched:)` — en-tête (`WKCircleButton` fermer, progression segmentée, « Brouillon enregistré » discret), une question par écran (`WK.Typo.title`), bas `WKPrimaryButton` « Continuer » / « Lancer le sondage » + retour ; erreurs par champ sous le champ (texte + icône, annoncées à VoiceOver) ; identifiants d'accessibilité stables.
   - **Quoi ?** titre, description, puces de modèles, type (réutiliser `EventTypePickerSheet` public ou une liste de `WKChip` des types courants + « Autre » avec libellé).
   - **Qui ?** trois `Stepper` optionnels (activer/désactiver chaque valeur), texte d'aide.
   - **Où ?** liste de lieux (ajout via `LocationSelectionSheet` public, ou saisie simple d'un nom), suppression ; optionnel (bouton « Passer »).
   - **Quand ?** liste de créneaux ; ajout via un éditeur réutilisant `EventSlotInputBuilder` / `DateTimePickerPopup` (publics) avec choix du moment (`Journée`, `Matin`, `Après-midi`, `Soir`, `Heure précise`) ; suppression.
   - Fermer : brouillon conservé (déjà enregistré) ; si rien n'a été saisi, aucun brouillon créé.
4. **Branchement** `AuthenticatedView` : `@State showCreateEventFlow`, `@State createFlowDraftId: String?` ; un **nouveau** `.fullScreenCover(isPresented: $showCreateEventFlow)` placé **après** `.sheet(isPresented: $showNotificationPreferencesSheet)` (tranches de test intactes). `beginRedesignEventCreation()` : sous `iosRedesign2026` → flux si le flag invitations est éteint, studio sinon (révisé le 2026-10-02, #48). Ne pas modifier la branche deep link `.eventCreate` ni `case .eventCreation`. `editDraftFromHome` (sous flag) : brouillon sans reçu d'invitation (créé hors studio) → flux avec `draftEventId` ; sinon chemin actuel. `onLaunched(event)` → fermer, `selectedEvent = event`, `currentView = .eventDetail`, rechargement accueil/activité ; `persistCreationContext` pour la checklist du modèle.

## Clés (préfixe `create_flow.*`, 5 langues, fr au tutoiement)

Questions (« C'est quoi, ton événement ? », « Vous serez combien ? », « Où ça pourrait se passer ? », « Quand est-ce que ça pourrait se passer ? »), sous-titres, champs, « Brouillon enregistré », « Continuer », « Passer », « Ajouter un lieu », « Ajouter un créneau », moments de la journée, erreurs (`max_less_than_min`, `description_required`, `custom_type_required`, `slot_end_before_start`, `location_duplicate`), aide invitations. Réutiliser `create_event.validation.title_required`, `.slot_required`, `participants.start_poll.action`, `participants.start_poll.requires_slot`, `events.all_day`, `common.*`.

## Contraintes

- Worktree `/Users/guy/Developer/dev/wakeve/.claude/worktrees/ios-app-design-e1ae98`, branche `claude/ios-redesign-layer-7`. Jamais `git stash`. Jamais indexer `.dao/*`. **Aucune ligne d'attribution.** `plutil -lint` sur les chaînes.
- Un seul DerivedData `/tmp/wk-l7-dd` (supprimé à la fin) ; `df -h /System/Volumes/Data` avant chaque build ; arrêt et rapport sous 3 Go. `-parallel-testing-enabled NO` ; tuer son propre `xcodebuild` bloqué.
- Simulateur « iPhone 18 Pro » seulement (`device` explicite), jamais « Wakeve-QA-iPhone-16-Pro » ; désinstaller l'app avant la suite complète.
- Ne pas modifier `CreateEventSheet.swift`, `CreateEventViewModel.swift`, `EventCreationStudioView`, `DraftDatesSheet`, les `case` legacy, ni les tests d'ancrage d'autres fichiers (`OrganizationPhase7`, `WakeveAIContractTests`, `FindingsRegressionTests`, `InvitationExperienceArchitectureReviewRedTests`, `ParityRouteInventoryContractTests`, `PremiumCreateEventContractTests`, `QABlockersRegressionTests`, `AllDaySlotAndGuestFinalizeTests`). Composants `WK` uniquement (cliquet `WKStyleGuardTests`).

## Tâches (TDD, un commit chacune, corps `Refs Swarm DAO #47 (layer 7).`)

1. `feat(ios): add pure create flow model` — modèle + tests (validation par étape selon le tableau DRAFT, `firstInvalidStep`, modèles, doublons de lieux, créneaux).
2. `feat(ios): add localized copy for the create flow` — clés + test de présence.
3. `feat(ios): persist create flow drafts step by step` — contrôleur + tests avec une machine/dépôt réels sur base en mémoire si possible (sinon protocole injectable + faux) : création à l'étape 1, mise à jour avec révision relue, effectifs, lieux, créneaux, reprise d'un brouillon, lancement (refus sans créneau, succès → POLLING).
4. `feat(ios): add the four-question create flow` — vues + tests (rendu par étape, erreurs visibles et annoncées, AX5 sans débordement, boutons ≥ 44 pt).
5. `feat(ios): open the create flow from the redesign` — branchement + tests source/pure ; tests d'ancrage listés verts.
6. Simulateur (créer un événement de bout en bout avec un modèle, fermer au milieu puis reprendre depuis l'accueil, erreurs de validation, lancer → hub en sondage ; flag invitations allumé et éteint ; AX5 ; sombre ; captures `/tmp/wk-l7-*.png` regardées ; flag refonte éteint → ancienne feuille inchangée), suite complète (5 échecs préexistants), spec §15 « Couche 7 » + ligne §9. Commit `docs(ios): record layer 7 create flow in redesign spec`.
7. Revue de code puis corrections.
