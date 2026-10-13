# Refonte iOS — Couche 4 (hub d'événement `EventHubView`) — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal :** Sous `iosRedesign2026`, remplacer l'écran de détail d'un événement par un hub : hero (statut, titre, résumé, avatars, teinte de l'événement), grille de tuiles de modules filtrées par statut avec le module de la prochaine action mis en évidence, carte contextuelle, CTA principal en bas. Les tuiles ouvrent les écrans existants (les sheets arrivent en couche 5).

**Architecture (même découpage que la couche 3) :**
1. **Cœur pur** `Models/Hub/EventHubModel.swift` : `EventHubFacts` (faits sur l'événement vus par l'utilisateur) → `EventHubModel` (statut, résumé, modules visibles et leur état, module mis en évidence, CTA, carte contextuelle, disponibilité du cycle de vie). 100 % testable.
2. **Source** `Services/SharedEventHubSource.swift` (protocole `EventHubSource`) : lit une fois, hors du thread principal (`Task.detached`, comme `SharedEventsHomeSource`), les données des modules via les dépôts Kotlin. Aucune lecture en base dans la vue.
3. **ViewModel** `ViewModels/EventHubViewModel.swift` : état (`loading/loaded/failed`), rechargement avec compteur de génération, annulation.
4. **Vue** `Views/Hub/EventHubView.swift` (+ `EventHubContainer`) : composants `WK` uniquement.
5. **Branchement** : dans `case .eventDetail` de `homeTabContent`, `if iosRedesign2026 { EventHubContainer(…) } else { EventDetailView(… legacy intact …) }`.

**Décisions (2026-09-30) :**
- Vote rapide Oui/Peut-être/Non : **ouvre `PollVotingView`** (pas de soumission depuis le hub : le journal de bulletins n'accepte que des bulletins complets et porte des gardes de cohérence).
- Transitions de cycle de vie (confirmé → organisation, organisation → finalisé) et invitation à se connecter pour un invité local : **reprises dans le hub** via `EventLifecycleTransitionController` (même API que `EventDetailView.lifecycleCard`).
- Menu « … » (`WKCircleButton`) : Infos de l'événement (→ `.eventInformation`, qui porte quitter/supprimer), Signaler (réutilise `ModerationActionSheet`), Support (mailto existant), Ajouter des participants (organisateur).
- Tuiles verrouillées : si l'accès est refusé par les règles existantes, la tuile reste visible avec l'état « À confirmer d'abord » et n'est pas actionnable (pas d'écran « accès refusé »).
- Événement finalisé avec le flag invitations allumé : le routeur existant mène à l'archive ; inchangé.

**Tech Stack :** SwiftUI (iOS 18.2 min), `import Shared`, XCTest. Dossiers Xcode synchronisés.

**Spec :** `docs/superpowers/specs/2026-09-28-ios-redesign-design.md` §5.2 — **Proposition :** Swarm DAO #47.

---

## Contraintes et commandes

- Worktree : `/Users/guy/Developer/dev/wakeve/.claude/worktrees/ios-app-design-e1ae98`. Jamais `git stash`. Ne jamais indexer `.dao/*`. **Aucune ligne d'attribution dans les commits.** Français **au tutoiement**.
- Tests : `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 18 Pro' -only-testing:WakeveTests/<Classe> 2>&1 | tail -30`. Suite complète avec `-parallel-testing-enabled NO` ; baseline : 5 échecs préexistants dans `InvitationExperienceRuntimeSurfaceTests`.
- Simulateur : **toujours** « iPhone 18 Pro » (passer `device` explicitement à tout outil) ; jamais « Wakeve-QA-iPhone-16-Pro ». Jamais deux `xcodebuild` en parallèle.
- **Ne pas modifier** `struct EventDetailView`, `EventDetailInvitationCanvas.swift`, ni les autres `case` de `homeTabContent`. Dans `case .eventDetail:` … `case .eventAudience:`, le texte legacy doit rester présent : `EventDetailView(`, `artwork:`, `onCanvasAction:`, `InvitationExperienceRouteRequestCanvasAction` ; **ne pas** y écrire `ArtworkNone.shared` ni `invitationQAArtwork`.
- Pont Kotlin : `Event` = `WakeveEvent`, description = `description_`, `EventStatus` avec `default:`, dates ISO `String`. Lire les noms exacts dans `shared/build/xcode-frameworks/Debug/iphonesimulator27.0/Shared.framework/Headers/Shared.h`.
- **Ne pas utiliser `BudgetViewModel.load()`** (crée un budget s'il n'existe pas) : lire `BudgetRepository(db:).getBudgetByEventId`.
- Garde-fou `WKStyleGuardTests` : aucun style en dur dans les nouveaux fichiers.

## Structure des fichiers

| Fichier | Action |
|---|---|
| `iosApp/src/Models/Hub/EventHubModel.swift` | Créer — faits, modules, règles pures |
| `iosApp/src/Services/SharedEventHubSource.swift` | Créer — lecture des faits hors thread principal |
| `iosApp/src/ViewModels/EventHubViewModel.swift` | Créer |
| `iosApp/src/Views/Hub/EventHubView.swift` | Créer — vue + `EventHubContainer` |
| `iosApp/src/Views/App/ContentView.swift` | Modifier — branche flag dans `case .eventDetail` + callbacks |
| `iosApp/src/Resources/*.lproj/Localizable.{strings,stringsdict}` | Modifier — clés `hub.*` |
| `iosApp/WakeveTests/EventHubModelTests.swift`, `EventHubViewModelTests.swift`, `EventHubViewTests.swift` | Créer |

---

### Task 1 : Cœur pur `EventHubModel`

**Files :** Create `iosApp/src/Models/Hub/EventHubModel.swift` ; Test `iosApp/WakeveTests/EventHubModelTests.swift`

```swift
import Foundation

enum HubModule: String, CaseIterable, Equatable {
    case date, location, participants, budget, scenarios
    case transport, accommodation, meals, equipment, activities, meetings
    case recap, photos, payments
}

struct EventHubFacts: Equatable {
    enum Phase: Equatable { case draft, polling, comparing, confirmed, organizing, finalized }

    let id: String
    let title: String
    let phase: Phase
    let isOrganizer: Bool
    let viewerAccepted: Bool          // organisateur ou invitation acceptée
    let hasDetailsAccess: Bool        // règle existante canAccessOrganizationDetails (organisateur → true)
    let isLocalGuest: Bool
    let pollOpen: Bool
    let userBallotComplete: Bool
    let ballotsKnown: Bool
    let votersWithCompleteBallot: Int
    let eligibleVoters: Int
    let otherEligibleVoters: Int
    let otherVotersComplete: Int
    let slotCount: Int
    let leadingSlotStart: Date?       // créneau en tête (PollLogic), nil si aucun vote
    let finalDate: Date?
    let confirmedCount: Int
    let pendingCount: Int
    let participantNames: [String]
    let summaries: [HubModule: String]   // résumés d'une ligne déjà localisés par la source (absent → indice statique)
}

struct EventHubModel: Equatable {
    enum Primary: Equatable { case vote, pollResults, confirmDate, organize, finalize, signInToFinalize, addDates, none }
    struct Tile: Equatable {
        let module: HubModule
        let isLocked: Bool
        let isHighlighted: Bool
        let status: WK.Status?
    }

    let status: WK.Status
    let statusKey: String
    let tiles: [Tile]
    let primary: Primary
    let showsQuickVote: Bool

    init(facts: EventHubFacts) { … }

    static func modules(for phase: EventHubFacts.Phase) -> [HubModule] {
        switch phase {
        case .draft: return [.date, .location, .participants]
        case .polling: return [.date, .location, .participants, .budget]
        case .confirmed, .comparing: return [.date, .scenarios, .participants, .budget]
        case .organizing: return [.transport, .accommodation, .meals, .equipment, .activities, .budget, .meetings]
        case .finalized: return [.recap, .photos, .payments]
        }
    }
}
```

Règles :

| Élément | Règle |
|---|---|
| `status`/`statusKey` | Réutiliser les règles de la couche 3 : brouillon → `.draft` / `home.v2.status.draft` ; sondage : vote requis (`viewerAccepted && pollOpen && ballotsKnown && !userBallotComplete`) → `.actionNeeded` / `home.v2.status.vote_required` ; organisateur prêt (règle `readyToConfirm` de `HomeEventFacts`, y compris après échéance) → `.actionNeeded` / `home.v2.status.ready_to_confirm` ; sinon `.pending` / `home.v2.status.polling` ; comparaison/confirmé/organisation → organisateur `.pending` / `home.v2.status.organizing`, participant `.confirmed` / `home.v2.status.confirmed` ; finalisé → `.confirmed` / `home.v2.status.confirmed`. **Extraire** le calcul `readyToConfirm` partagé dans une fonction statique réutilisée par `HomeEventFacts` et `EventHubFacts` (pas de copie). |
| `primary` | brouillon organisateur : `slotCount == 0` → `.addDates`, sinon `.none` (le lancement du sondage reste dans le flux de création) ; vote requis → `.vote` ; organisateur prêt → `.confirmDate` ; organisateur sondage en cours → `.pollResults` ; participant ayant voté → `.pollResults` ; confirmé, organisateur → `.organize` ; organisation, organisateur → `isLocalGuest ? .signInToFinalize : .finalize` ; sinon `.none`. |
| Tuile verrouillée | `transport` : bloquée hors {confirmé, organisation, finalisé} ou sans `hasDetailsAccess` ; `accommodation/meals/equipment/activities/photos` : sans `hasDetailsAccess` ; `budget/meetings/payments` : sans `hasDetailsAccess` ou hors {organisation, finalisé} **sauf** en sondage/confirmé où `budget` est une estimation consultable par l'organisateur et les acceptés ; `date/location/participants/scenarios/recap` : jamais. |
| Mise en évidence | Une seule tuile : `.date` si `primary ∈ {vote, pollResults, confirmDate, addDates}` ; `.scenarios` en comparaison ; sinon aucune. |
| `status` de tuile | `.date` reçoit `.actionNeeded` si vote requis ou prêt, sinon nil. |
| `showsQuickVote` | vote requis et `slotCount ≥ 1`. |

- [ ] **Step 1 : Tests** (au moins) : modules par phase (5 phases) ; participant accepté sondage ouvert sans vote → `.vote`, date mise en évidence, vote rapide visible ; participant non accepté → pas de vote ; organisateur tous les autres ont voté → `.confirmDate` ; organisateur seul → pas `confirmDate` ; brouillon sans créneau → `.addDates` ; confirmé organisateur → `.organize` ; organisation organisateur → `.finalize`, invité local → `.signInToFinalize` ; participant confirmé sans accès → tuiles d'organisation verrouillées ; finalisé → `recap/photos/payments` ; une seule tuile mise en évidence ; `HomeEventFacts` et le hub donnent le même `readyToConfirm` (test sur la fonction partagée).
- [ ] **Step 2 :** lancer → BUILD FAILED. **Step 3 :** implémenter. **Step 4 :** PASS + `HomeEventSummaryTests` toujours vert (extraction partagée).
- [ ] **Step 5 : Commit** `feat(ios): add pure event hub rules` — corps `Refs Swarm DAO #47 (layer 4).`

### Task 2 : Clés localisées `hub.*`

Ajouter dans les 5 langues (fr tutoiement) et un test « chaque clé existe dans chaque langue » dans `EventHubModelTests` :

| Clé | fr | en | es | it | pt |
|---|---|---|---|---|---|
| hub.module.date | Date | Date | Fecha | Data | Data |
| hub.module.location | Lieu | Place | Lugar | Luogo | Local |
| hub.module.participants | Invités | Guests | Invitados | Invitati | Convidados |
| hub.module.budget | Budget | Budget | Presupuesto | Budget | Orçamento |
| hub.module.scenarios | Scénarios | Scenarios | Escenarios | Scenari | Cenários |
| hub.module.transport | Transport | Transport | Transporte | Trasporti | Transporte |
| hub.module.accommodation | Hébergement | Stay | Alojamiento | Alloggio | Hospedagem |
| hub.module.meals | Repas | Meals | Comidas | Pasti | Refeições |
| hub.module.equipment | Matériel | Gear | Material | Attrezzatura | Equipamento |
| hub.module.activities | Activités | Activities | Actividades | Attività | Atividades |
| hub.module.meetings | Réunions | Meetings | Reuniones | Riunioni | Reuniões |
| hub.module.recap | Récap | Recap | Resumen | Riepilogo | Resumo |
| hub.module.photos | Photos | Photos | Fotos | Foto | Fotos |
| hub.module.payments | Paiements | Payments | Pagos | Pagamenti | Pagamentos |
| hub.tile.locked | À confirmer d'abord | Confirm first | Confirma primero | Conferma prima | Confirme primeiro |
| hub.tile.hint | À préparer | To plan | Por preparar | Da preparare | A preparar |
| hub.summary.format | %1$@ · %2$@ | %1$@ · %2$@ | … | … | … |
| hub.primary.vote | Voter | Vote | Votar | Vota | Votar |
| hub.primary.results | Voir les résultats | See results | Ver resultados | Vedi risultati | Ver resultados |
| hub.primary.confirm_date_format | Confirmer le %@ | Confirm %@ | Confirmar el %@ | Conferma il %@ | Confirmar %@ |
| hub.primary.confirm_date | Choisir la date | Choose the date | Elegir la fecha | Scegli la data | Escolher a data |
| hub.primary.organize | Passer en organisation | Start organizing | Empezar a organizar | Inizia a organizzare | Começar a organizar |
| hub.primary.finalize | Finaliser l'événement | Finalize event | Finalizar evento | Finalizza evento | Finalizar evento |
| hub.primary.sign_in | Connecte-toi pour finaliser | Sign in to finalize | Inicia sesión para finalizar | Accedi per finalizzare | Entre para finalizar |
| hub.primary.add_dates | Ajouter des dates | Add dates | Añadir fechas | Aggiungi date | Adicionar datas |
| hub.quick_vote.title_format | Tu es dispo le %@ ? | Free on %@? | ¿Te va bien el %@? | Sei libero il %@? | Você pode em %@? |
| hub.menu.info | Infos de l'événement | Event info | Información | Informazioni | Informações |
| hub.menu.more | Plus d'options | More options | Más opciones | Altre opzioni | Mais opções |

`Localizable.stringsdict` : `hub.slots_count` (« %d créneau » / « %d créneaux », en « %d option(s) », es « %d opción/opciones », it « %d opzione/opzioni », pt « %d opção/opções ») et `hub.guests_count` (« %d invité(s) », « %d guest(s) », « %d invitado(s) », « %d invitato/invitati », « %d convidado(s) »). `plutil -lint` sur les 10 fichiers. Réutiliser `poll.yes`/`poll.maybe`/`poll.no`, `event.lifecycle.*` (confirmations), `event.detail.menu.add_participants` existants.

- [ ] Test → FAIL → clés → PASS. **Commit** `feat(ios): add localized copy for the event hub` + `Refs Swarm DAO #47 (layer 4).`

### Task 3 : Source et ViewModel

**Files :** Create `iosApp/src/Services/SharedEventHubSource.swift`, `iosApp/src/ViewModels/EventHubViewModel.swift` ; Test `iosApp/WakeveTests/EventHubViewModelTests.swift`

- `protocol EventHubSource { func loadFacts(eventId: String, viewerId: String, isLocalGuest: Bool) async throws -> EventHubFacts }`.
- `EventHubViewModel` (`@MainActor ObservableObject`) : `state` (`loading/loaded/failed`), `facts`, `model` (`EventHubModel?`), `reload()` avec compteur de génération, `CancellationError` ignorée, données conservées en cas d'échec. Tests avec une source factice (chargement, échec sans données → `failed`, échec avec données → garde les données, résultat périmé ignoré).
- `SharedEventHubSource` (non testée unitairement, vérifiée au simulateur), tout dans un `Task.detached` + `withTaskCancellationHandler` (copier le schéma de `SharedEventsHomeSource`, avec le même commentaire sur la sûreté des objets Kotlin) :
  - `repository.getEvent(id:)` ; phase depuis `event.status.name`.
  - Organisateur (`organizerId == viewerId`), invitation acceptée et accès : `getParticipantRecords(eventId:)` + `ParticipantAccessMapper.shared.fromRepositoryRecord(record:)` ; `hasDetailsAccess` = même règle que `canAccessOrganizationDetails` (ContentView, rechercher la fonction ; l'extraire en fonction statique réutilisable **sans** changer son comportement, ou la reproduire à l'identique avec un test comparant les deux sur les mêmes entrées).
  - Bulletins : réutiliser `SharedEventsHomeSource.ballotStats(...)` (fonction pure existante) avec `getPoll(eventId:)?.votes`. Créneau en tête : `PollLogic.shared.getBestSlotWithScore(poll:slots:)` (voir `ViewModels/PollConfirmationViewModel.swift:248`).
  - Comptes invités confirmés/en attente et noms (5 max, cache par chargement, `database.userQueries.selectUserById`).
  - Résumés d'une ligne (localisés dans la source) : date (`hub.slots_count` ou date finale), lieu (`potentialLocationQueries.selectByEventId` : nombre d'options), budget (`BudgetRepository(db:).getBudgetByEventId` : total estimé si > 0), scénarios (`ScenarioRepository(db:).getScenariosByEventId` : nombre), transport (`TransportRepositoryBridge(database:).getPlansByEvent` : nombre de plans / plan choisi), hébergement / activités (listes : nombre), repas (`MealRepository(db:).getMealPlanningSummary` : `mealsCompleted/totalMeals`), matériel (items : nombre), réunions (`meetingQueries.selectByEventId`, hors `CANCELLED`), paiements (logique `paymentPotSummaryValue` de ContentView). Pas de source pour `recap`/`photos` → absent (la vue affiche `hub.tile.hint`). **Réutiliser** les formats existants (`event.detail.slot_option(s)_*`, `event.detail.invited_participant(s)_*`) quand ils conviennent. Toute erreur de lecture d'un module → résumé absent, pas d'échec global.
- [ ] Tests VM → FAIL → implémenter → PASS. **Commit** `feat(ios): load event hub facts off the main actor` + `Refs Swarm DAO #47 (layer 4).`

### Task 4 : `EventHubView`

**Files :** Create `iosApp/src/Views/Hub/EventHubView.swift` ; Test `iosApp/WakeveTests/EventHubViewTests.swift`

Structure :
- Barre haute : `WKCircleButton(chevron.left, retour)` à gauche, `WKCircleButton(ellipsis, hub.menu.more)` à droite dans un `Menu` (Infos, Ajouter des participants si organisateur, Signaler, Support).
- Hero `WKCard(radius: WK.Radius.lg)` avec fond teinté léger (`EventMoodPalette.palette(for: eventTypeName).primary(for: colorScheme)` à faible opacité, superposé au `card`) : `WKStatusPill`, titre (`WK.Typo.title`), résumé `hub.summary.format` (créneaux · invités), `WKAvatarStack`.
- Grille `LazyVGrid` 2 colonnes (1 aux tailles d'accessibilité) de `WKModuleTile(systemImage:title:summary:status:isHighlighted:accessibilityID: "hub.tile.<module>")` ; tuile verrouillée : résumé `hub.tile.locked`, `action` sans effet et `.disabled(true)` + trait non-bouton lu « À confirmer d'abord ».
- Carte contextuelle si `showsQuickVote` : `WKCard(style: .inset)` avec `hub.quick_vote.title_format` (créneau en tête formaté avec `HomeDateText.short`) et trois `WKChip` (`poll.yes/maybe/no`) qui appellent tous `onPrimary(.vote)`.
- CTA en bas (`safeAreaInset(edge: .bottom)`) : `WKPrimaryButton` selon `primary` (`none` → pas de bouton). `confirmDate` avec `leadingSlotStart` → `hub.primary.confirm_date_format` + date courte, sinon `hub.primary.confirm_date`.
- Cycle de vie : `organize`/`finalize` passent par une `confirmationDialog` réutilisant `event.lifecycle.{organizing,finalize}.{title,confirm_message,action}` puis `onLifecycle(target)` ; `signInToFinalize` → `confirmationDialog` avec `event.lifecycle.guest.confirm_message` puis `onRequestSignIn()`. Erreur de transition → texte `WK.Colors.textMuted` sous le CTA (`eventLifecycleError`).
- États : `loading` → `ProgressView` ; `failed` → message `common.error_generic` + `WKChip(common.retry)`.
- `static func primaryTitle(for:facts:locale:)`, `static func columnCount(for:)`, `static func systemImage(for: HubModule)` testables. Icônes : date `calendar`, lieu `mappin.and.ellipse`, invités `person.2`, budget `eurosign.circle`, scénarios `square.stack`, transport `car`, hébergement `bed.double`, repas `fork.knife`, matériel `backpack`, activités `figure.hiking`, réunions `video`, récap `checkmark.seal`, photos `photo.on.rectangle`, paiements `creditcard`.
- `EventHubContainer(eventId:userId:isLocalGuest:reloadToken:callbacks…)` possède le `@StateObject` et recharge sur changement du jeton.

- [ ] Tests (fonctions statiques + rendu `UIHostingController.sizeThatFits` à AX5 sans débordement de largeur sur 375 pt + CTA ≥ 44 pt) → FAIL → implémenter → PASS + `WKStyleGuardTests`. **Commit** `feat(ios): add the event hub view` + `Refs Swarm DAO #47 (layer 4).`

### Task 5 : Branchement dans `AuthenticatedView`

Dans `case .eventDetail:` : `if iosRedesign2026, let event = selectedEvent { EventHubContainer(…) } else { …code legacy inchangé… }`. Callbacks (fonctions privées placées près des helpers d'accueil de la couche 3, **pas** dans la tranche `case .eventDetail`…`case .eventAudience`) :
- `onBack` : même logique que le `onBack` legacy (`invitationLandingEventId = nil; currentView = .eventList`).
- `onOpenModule(HubModule)` : date → `.pollVoting` si sondage et vote requis, sinon `.pollResults` ; lieu/scénarios → `.scenarioList` ; invités → même route que `onManageParticipants` legacy ; budget → `.budgetOverview` ; transport → `.transportPlanning` ; hébergement/repas/matériel/activités/réunions/photos → cases existants ; récap → `.eventInformation` ; paiements → `.paymentPot` (et Tricount accessible depuis cet écran). Les gardes existantes des `case` s'appliquent en plus.
- `onPrimary` : `vote` → `.pollVoting` ; `pollResults`/`confirmDate` → `.pollResults` ; `addDates` → route de modification du brouillon utilisée par l'accueil (`editDraftFromHome`) ; `organize`/`finalize` gérés par la vue via `onLifecycle`.
- `onLifecycle(target)` : `EventLifecycleTransitionController(eventId:userId:repository:).transition(to:)` ; succès → `selectedEvent = repository.getEvent(id:)` + incrément du jeton de rechargement du hub ; échec → message renvoyé à la vue.
- `onRequestSignIn` : `authStateManager.signOut()` (comme legacy).
- Menu : Infos → `.eventInformation` ; Ajouter des participants → route legacy ; Signaler/Support → réutiliser `ModerationActionSheet` et l'URL mailto existantes (lire `canvasMenuActions` ; si elles sont privées à `EventDetailView`, les reproduire dans le hub sans modifier `EventDetailView`).
- [ ] Tests source dans `RedesignShellTests` (branche flag présente, legacy intact dans la tranche) + lancer : `RedesignShellTests`, `EventHub*Tests`, `PremiumEventDetailContractTests`, `OrganizationPhase5ContractTests`, `OrganizationPhase7ContractTests`, `InvitationExperienceSurfaceContractTests`, `InvitationExperienceDesignerFindingsRedTests`, `InvitationExperienceArchitectureReviewRedTests`, `AllDaySlotAndGuestFinalizeTests`, `EventDetailInvitationCanvasContractTests`, `WakeveAIContractTests`, `PremiumNavigationContractTests`, `ParityRouteInventoryContractTests`, `WKStyleGuardTests` → verts **sans modifier ces tests**. **Commit** `feat(ios): open events in the new hub under the redesign flag` + `Refs Swarm DAO #47 (layer 4).`

### Task 6 : Simulateur, suite complète, spec, revue

- [ ] « iPhone 18 Pro », `-iosRedesign2026 YES -hasCompletedOnboarding YES --wakeve-debug-authenticated` : ouvrir un événement de chaque statut disponible (sondage, confirmé, organisation, finalisé, brouillon) ; vérifier hero, tuiles, mise en évidence, CTA, vote rapide → écran de vote, retour → hub rechargé, passage en organisation avec confirmation, tuiles verrouillées, AX5, mode sombre ; captures `/tmp/wk-l4-*.png` **regardées**. Remettre taille/apparence par défaut. Flag éteint → ancien détail inchangé.
- [ ] Suite complète `-parallel-testing-enabled NO` → seuls les 5 échecs préexistants.
- [ ] Spec §15 « Couche 4 » : décisions ci-dessus + écarts constatés. Commit `docs(ios): record layer 4 hub in redesign spec`.
- [ ] Revue de code (conformité puis qualité).
