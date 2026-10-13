# Refonte iOS — Couche 5a (socle des sheets + modules simples) — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal :** Sous `iosRedesign2026`, ouvrir les modules depuis le hub dans une sheet (`WKModuleSheet`, detents medium/large) au lieu d'un écran plein. Couche 5a : le socle (conteneur, routage, présentation, repli plein écran) + Repas, Matériel, Activités, Hébergement, Photos.

**Découpage de la couche 5 (décidé le 2026-09-30, ordre modifié vs spec §9) :**
- **5a (ce plan)** : socle + Repas, Matériel, Activités, Hébergement, Photos (petites listes, mêmes dépôts que le hub).
- **5b** : Budget, Cagnotte (+ Tricount), Réunions.
- **5c** : Commentaires (brancher l'envoi, aujourd'hui sans effet), Transport, Invités — résumés avec repli plein écran.
Raison : Transport est l'écran le plus lourd et le plus verrouillé par les tests ; commencer par les modules simples rend la conversion incrémentale et sûre.

**Architecture :**
1. `Components/WK/WKModuleSheet.swift` : conteneur générique (titre + `WKStatusPill` optionnelle, contenu défilant de cartes `.inset`, phrase « ce qui manque », `WKActionBar` en bas, bouton fermer, `presentationDetents([.medium, .large])`, `presentationDragIndicator(.visible)`).
2. Routage : `EventHubRoute.sheet(HubModule)` ; `EventHubRouting.sheetModules: Set<HubModule>` = modules convertis (5a : `meals, equipment, activities, accommodation, photos`) ; un module converti route en `.sheet` **seulement** si l'accès est accordé (même garde que le `case` legacy), sinon comportement actuel.
3. Présentation : `@State private var presentedHubModule: HubModule?` dans `AuthenticatedView`, `.sheet(item:)` sur `eventHubContent(for:)`. `performHubRoute` gère `.sheet` (relit l'événement comme aujourd'hui, puis présente).
4. Contenu : `Views/Hub/Modules/HubModuleSheetContent.swift` — un `EventModuleSheetSource` (protocole, lecture hors thread principal comme les sources existantes) fournit un `HubModuleSheetData` pur (titre, statut, éléments `[HubModuleSheetItem]`, phrase manquante, actions disponibles) ; une vue unique rend n'importe quel module 5a à partir de ces données.
5. Repli : action secondaire « Ouvrir en plein écran » (et « Commentaires ») → fermer la sheet, puis router vers l'écran legacy au tour de boucle suivant (dans `onDismiss` de la sheet, jamais pendant la présentation). Les `case` legacy et leurs vues restent intacts.

**Tech Stack :** SwiftUI (iOS 18.2 min), `import Shared`, XCTest.

**Spec :** §5.3 — **Proposition :** Swarm DAO #47.

---

## Contraintes et commandes

- Worktree `/Users/guy/Developer/dev/wakeve/.claude/worktrees/ios-app-design-e1ae98`. Jamais `git stash`. Ne jamais indexer `.dao/*`. **Aucune ligne d'attribution dans les commits.** Français au tutoiement.
- Tests : `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 18 Pro' -parallel-testing-enabled NO -only-testing:WakeveTests/<Classe>`. Suite complète : baseline = 5 échecs préexistants dans `InvitationExperienceRuntimeSurfaceTests`.
- Simulateur : **toujours** « iPhone 18 Pro » (passer `device` explicitement) ; jamais « Wakeve-QA-iPhone-16-Pro ». Jamais deux `xcodebuild` en parallèle. Ne pas laisser d'événement QA semé modifié (sinon désinstaller l'app avant la suite complète).
- **Ne pas modifier** les `case` de `homeTabContent`, `EventSecondaryRouteViews.swift` (vues legacy), ni les tests d'ancrage d'autres fichiers (`ParityRouteInventoryContractTests`, `OrganizationPhase5/7ContractTests`, etc.). Mettre à jour `EventHubViewTests.testModuleScreens` (et tests hub associés) quand un module passe en sheet.
- `WKModuleSheet` dans `Components/WK` : zéro style en dur (`WKStyleGuardTests`).
- Ne pas envelopper un écran legacy dans un `NavigationStack` supplémentaire.

## Structure des fichiers

| Fichier | Action |
|---|---|
| `iosApp/src/Components/WK/WKModuleSheet.swift` | Créer |
| `iosApp/src/Views/Hub/EventHubRouting.swift` | Modifier — `.sheet`, `sheetModules`, repli |
| `iosApp/src/Models/Hub/HubModuleSheetData.swift` | Créer — données pures + règles |
| `iosApp/src/Services/SharedEventModuleSheetSource.swift` | Créer — lecture des listes |
| `iosApp/src/Views/Hub/Modules/HubModuleSheetView.swift` | Créer — rendu + conteneur (`@StateObject` VM simple ou `.task`) |
| `iosApp/src/Views/App/ContentView.swift` | Modifier — `presentedHubModule`, `.sheet(item:)`, repli après fermeture |
| `iosApp/src/Resources/*.lproj/Localizable.{strings,stringsdict}` | Modifier — clés `hub.sheet.*` |
| `iosApp/WakeveTests/WKModuleSheetTests.swift`, `HubModuleSheetDataTests.swift`, `HubModuleSheetViewTests.swift` | Créer |

---

### Task 1 : `WKModuleSheet`

API :
```swift
struct WKModuleSheet<Content: View>: View {
    struct Status { let text: String; let status: WK.Status }
    let title: String
    var status: Status? = nil
    var missing: String? = nil
    let primary: (title: String, action: () -> Void)?
    var secondary: [WKActionBar.Item] = []
    let onClose: () -> Void
    @ViewBuilder let content: () -> Content
}
```
- En-tête : titre `WK.Typo.title`, pastille à droite si `status`, `WKCircleButton(xmark, label "common.close" ou clé existante équivalente — vérifier)` avec identifiant `wk.sheet.close`.
- Corps : `ScrollView` → `VStack(spacing: WK.Space.sm)` { `content()` ; `missing` en `WK.Typo.body` `WK.Colors.textMuted` }.
- Bas : `safeAreaInset(edge: .bottom)` → `WKActionBar` si `primary` (sinon seulement les secondaires dans une rangée d'icônes, ou rien).
- `.presentationDetents([.medium, .large])`, `.presentationDragIndicator(.visible)`, fond `WK.Colors.canvas`.
- [ ] Tests (rendu `sizeThatFits` à AX5 largeur ≤ 375 via taille idéale mesurée à 1000 ; bouton fermer ≥ 44 pt ; pas de style en dur via `WKStyleGuardTests`) → FAIL → implémenter → PASS. Ajouter `WKModuleSheet` à la galerie `WKGallery` (preview). **Commit** `feat(ios): add WKModuleSheet container` + `Refs Swarm DAO #47 (layer 5a).`

### Task 2 : Données pures `HubModuleSheetData`

```swift
struct HubModuleSheetItem: Equatable, Identifiable {
    let id: String
    let title: String
    let detail: String?          // ex. "Samedi soir · 8 personnes"
    let status: WK.Status?       // ex. repas prêt → .confirmed, à assigner → .pending
    let assigneeNames: [String]  // avatars
}

struct HubModuleSheetData: Equatable {
    let module: HubModule
    let items: [HubModuleSheetItem]
    let status: (key: String, status: WK.Status)?   // pastille d'en-tête (Equatable via struct dédiée si besoin)
    let missingKey: String?                          // phrase « ce qui manque »
    let canAdd: Bool                                 // action principale disponible (organisateur, non lecture seule, module qui a un formulaire)
    let pendingSync: Bool
}
```
Règles (fonction pure `HubModuleSheetData.make(module:rawItems:isOrganizer:isReadOnly:pendingSync:)`, entrées = structures Swift simples, pas d'objets Kotlin) :
- **Repas** : item par repas (nom, date/moment, nombre de personnes, statut : `COMPLETED`/prêt → `.confirmed`, sinon `.pending`), assignés = responsables. Pastille : « x/y prêts » (`hub.sheet.meals.progress_format`, `.pending` si incomplet, `.confirmed` sinon). Manquant : repas sans responsable → « N repas sans responsable » (pluriel `hub.sheet.meals.unassigned_count`). `canAdd` = organisateur ∧ ¬lecture seule.
- **Matériel** : item par objet (nom, quantité, statut : apporté/assigné → `.confirmed`, à trouver → `.pending`), assignés. Manquant : « N objets sans personne » (`hub.sheet.equipment.unassigned_count`). `canAdd` = false en 5a (pas de formulaire existant hors écran plein → repli).
- **Activités** : item par activité (nom, date, inscrits), manquant : aucune activité → `hub.sheet.activities.empty`. `canAdd` = false.
- **Hébergement** : item par option (nom, prix/nuit si dispo, statut sélectionné → `.confirmed`), manquant : aucune option retenue → `hub.sheet.accommodation.none_selected`. `canAdd` = false.
- **Photos** : aucun item ; manquant `hub.tile.photos_hint` ; `canAdd` = false.
- Aucune donnée : `missingKey` = clé vide propre au module.
Les champs exacts viennent des modèles Kotlin (lire `Shared.h` et les vues legacy `EventSecondaryRouteViews.swift` qui les affichent déjà) ; la source (Task 3) les convertit en `raw` Swift.
- [ ] Tests par module (items, pastille, manquant, canAdd selon rôle/lecture seule, liste vide) → FAIL → implémenter → PASS. Clés `hub.sheet.*` dans les 5 langues (+ pluriels en stringsdict), test « chaque clé dans chaque langue ». **Commit** `feat(ios): add pure module sheet data rules` + `Refs Swarm DAO #47 (layer 5a).`

### Task 3 : Source `SharedEventModuleSheetSource`

- `protocol EventModuleSheetSource { func load(module: HubModule, eventId: String, viewerId: String) async throws -> HubModuleSheetData }`.
- Lecture dans `Task.detached` + `withTaskCancellationHandler` (même schéma et commentaire que `SharedEventHubSource`). Dépôts : `MealRepository(db:).getMealsByEventId`, `EquipmentRepository(db:).getEquipmentItemsByEventId`, `ActivityRepository(db:).getActivitiesByEventId`, `AccommodationRepository(db:).getAccommodationsByEventId` (voir `EventSecondaryRouteViews.swift` pour l'usage exact et les champs), synchro en attente via le même mécanisme que ces vues (`getWorkflowOutbox`), noms via `userQueries.selectUserById` (cache par chargement), rôle/lecture seule via l'événement (`organizerId`, statut `FINALIZED`).
- Conversion en `raw` Swift puis `HubModuleSheetData.make(...)`.
- [ ] Pas de test unitaire base (vérifié au simulateur), mais tester les fonctions de conversion pures extraites si elles contiennent de la logique. **Commit** `feat(ios): load module sheet data off the main actor` + `Refs Swarm DAO #47 (layer 5a).`

### Task 4 : Vue de sheet et routage

- `HubModuleSheetView(module:eventId:viewerId:source:onClose:onAdd:onOpenFullScreen:onOpenComments:)` : charge via `.task` (état loading/loaded/failed avec réessai), rend `WKModuleSheet` : titre `hub.module.<module>`, pastille, liste de `WKCard(style: .inset)` par item (titre, détail, `WKStatusPill` si statut, `WKAvatarStack` des assignés), phrase manquante, action principale « Ajouter un repas » (`hub.sheet.meals.add`) si `canAdd` (repas seulement), secondaires : « Plein écran » (`arrow.up.left.and.arrow.down.right`, `hub.sheet.open_full`), « Commentaires » (`bubble.left`, `hub.sheet.comments`) pour repas/matériel/activités/hébergement.
- Ajout d'un repas : présenter `MealFormSheet` **depuis la sheet** (réutiliser la vue existante `MealPlanningSheets.swift:5` ; lire sa signature et son comportement : si elle n'écrit pas en base, conserver le comportement actuel et recharger la sheet à la fermeture).
- `EventHubRouting` : `case sheet(HubModule)` ; `static let sheetModules: Set<HubModule> = [.meals, .equipment, .activities, .accommodation, .photos]` ; `route(for:phase:invitationRollout:)` renvoie `.sheet(m)` pour ces modules (les tuiles verrouillées ne routent pas — le hub bloque déjà). Mettre à jour `EventHubViewTests.testModuleScreens` et tests liés.
- `AuthenticatedView` : `@State private var presentedHubModule: HubModule?` + `@State private var pendingHubFallback: AppView?` ; `.sheet(item: $presentedHubModule, onDismiss: { if let v = pendingHubFallback { pendingHubFallback = nil; currentView = v } })` attaché au hub. Repli : `onOpenFullScreen` → `pendingHubFallback = <case legacy du module>` puis `presentedHubModule = nil` ; `onOpenComments` → `selectedCommentSection = <section du module>` + `pendingHubFallback = .comments`. Fermeture simple → rechargement du hub (jeton). `HubModule` doit être `Identifiable` (id = rawValue).
- `performHubRoute` : cas `.sheet(m)` → relire l'événement (ancre `repository.getEvent(id: event.id)` conservée) puis `presentedHubModule = m`.
- [ ] Tests : routage (modules 5a → `.sheet`, autres inchangés), fonction pure de repli `EventHubRouting.fullScreenFallback(for:) -> AppView?`, source ContentView (`.sheet(item: $presentedHubModule`), rendu de la vue avec une source factice (loaded/empty/failed). **Commit** `feat(ios): open simple hub modules in sheets` + `Refs Swarm DAO #47 (layer 5a).`

### Task 5 : Simulateur, suite, spec, revue

- [ ] « iPhone 18 Pro », `-iosRedesign2026 YES -hasCompletedOnboarding YES --wakeve-debug-authenticated` : sur un événement en organisation (passer « Week-end confirmé » en organisation si besoin, puis **désinstaller l'app avant la suite complète**) ouvrir Repas, Matériel, Activités, Hébergement ; sur un finalisé, Photos. Vérifier medium/large, glisser pour fermer, « Plein écran » → écran legacy puis retour au hub, « Commentaires », ajout d'un repas (organisateur), AX5, sombre. Captures `/tmp/wk-l5a-*.png` **regardées** ; remettre taille/apparence par défaut. Flag éteint → inchangé.
- [ ] Suite complète → seuls les 5 échecs préexistants.
- [ ] Spec §15 « Couche 5a » (découpage 5a/5b/5c, ordre modifié, repli plein écran, écarts). Commit `docs(ios): record layer 5a sheets in redesign spec`.
- [ ] Revue de code puis corrections.
