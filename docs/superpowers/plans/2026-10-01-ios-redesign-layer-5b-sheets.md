# Refonte iOS — Couche 5b (Budget, Cagnotte, Réunions en sheets) — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal :** Sous `iosRedesign2026`, ouvrir Budget, Paiements (cagnotte + Tricount) et Réunions depuis le hub dans une `WKModuleSheet` avec un résumé natif ; toutes les écritures restent dans les écrans existants via « Plein écran » (ou une action dédiée).

**Base :** socle de la couche 5a — `WKModuleSheet`, `HubModuleSheetData`/`HubModuleSheetItem` (avec `statusText`, `accessibilityLabel`), `SharedEventModuleSheetSource` (`Task.detached`), `HubModuleSheetView`, `EventHubRouting.sheetModules` + `sheetRoute(for:accessGranted:)`, `HubSheetLifecycle` (repli `(eventId, AppView)` appliqué après fermeture). Lire ces fichiers avant tout.

**Spec :** §5.3, §15 « Couche 5a » — **Proposition :** Swarm DAO #47.

## Règles par module

| Module | Garde d'accès (identique au `case` legacy) | Contenu | Pastille | Phrase « ce qui manque » | Actions |
|---|---|---|---|---|---|
| **Budget** (`.budget`) | `canAccessOrganizationDashboard(for:)` | Totaux estimé / réel et une carte par catégorie avec montant > 0 (`BudgetRepository(db:).getBudgetByEventId` → `Budget_` : `totalEstimated`, `totalActual`, champs par catégorie — lire `Shared.h`). **Ne jamais appeler `BudgetViewModel.load()`** (crée un budget). | « Réel x € / estimé y € » ; `.pending` si réel > estimé (dépassement), `.confirmed` sinon | Pas de budget → `hub.sheet.budget.empty` ; dépassement → `hub.sheet.budget.over_format` | Principal « Voir les dépenses » → repli plein écran `.budgetOverview` ; secondaire Plein écran |
| **Paiements** (`.payments`) | même garde que `case .paymentPot:` (`canAccessPhase5Organization` ou équivalent — lire le `case`) | Carte cagnotte (`PaymentPotRepository(db:).getActivePotForEvent` : objectif, devise, statut) ; carte Tricount (`TricountHandoffRepository(db:).getPaymentReadiness` — reprendre la logique `tricountSummaryValue` de ContentView sans la modifier) | statut de la cagnotte (`hub.sheet.status.*`) | Pas de cagnotte → `hub.sheet.payments.no_pot` | Principal « Gérer la cagnotte » → repli `.paymentPot` ; secondaire « Tricount » → repli `.tricount` ; Plein écran |
| **Réunions** (`.meetings`) | `canAccessOrganizationDashboard(for:)` | Une carte par réunion non annulée (`database.meetingQueries.selectByEventId`, statut ≠ `CANCELLED`) triée par date : titre, date/heure courte, plateforme, statut (« Lien prêt » si lien généré / « Sans lien ») | « N à venir » | Aucune réunion → `hub.sheet.meetings.empty` ; réunions sans lien → `hub.sheet.meetings.without_link_count` (pluriel) | Principal « Planifier une réunion » (organisateur ∧ organisation, règle legacy `canCreateMeetings`) → repli `.meetingList` ; secondaire Plein écran |

- Lecture seule (finalisé) : pas d'action principale d'écriture (« Voir les dépenses » reste disponible, c'est une consultation).
- Toutes les nouvelles clés `hub.sheet.budget.*`, `hub.sheet.payments.*`, `hub.sheet.meetings.*` en en/fr/es/it/pt (fr au tutoiement), pluriels en stringsdict ; montants via le formatage monétaire déjà utilisé par le hub (`HubSummaryText.currency`).
- Les résumés de tuiles du hub doivent rester cohérents avec les pastilles des sheets (même source de calcul ; extraire une fonction pure partagée si nécessaire, comme `HubSummaryText.mealProgress`).

## Contraintes

- Worktree `/Users/guy/Developer/dev/wakeve/.claude/worktrees/ios-app-design-e1ae98`. Jamais `git stash`. Jamais indexer `.dao/*`. **Aucune ligne d'attribution.** Français au tutoiement. `plutil -lint` sur les fichiers de chaînes modifiés.
- `xcodebuild … -parallel-testing-enabled NO -derivedDataPath /tmp/wk-l5b-dd` (un seul dossier, **supprimé à la fin**). Si `xcodebuild test` ne rend pas la main après le résultat, tuer son propre processus.
- Simulateur : « iPhone 18 Pro » seulement (passer `device` explicitement) ; jamais « Wakeve-QA-iPhone-16-Pro ». Données QA : lancer avec `-iosRedesign2026 YES -hasCompletedOnboarding YES --wakeve-debug-authenticated --wakeve-qa-seed-invitation-experience --wakeve-qa-open-invitation-route library`. Attendre l'apparition complète des boîtes de dialogue avant de confirmer. Désinstaller l'app avant la suite complète si des données ont été modifiées.
- Ne pas modifier les `case` legacy, `BudgetOverviewView`/`MeetingListView` (hors ce qui existe déjà pour le retour), `PaymentPotView`/`TricountHandoffView` (privées dans ContentView, ancrées par les tests), ni les tests d'ancrage d'autres fichiers. Mettre à jour seulement les tests hub/sheet.

## Tâches

### Task 1 : Données pures
Étendre `HubModuleSheetRaw` / `HubModuleSheetData.make` pour `.budget`, `.payments`, `.meetings` selon le tableau. Tests par module : contenu, pastille (dont dépassement budgétaire), phrase manquante, actions selon rôle/lecture seule/phase, liste vide, pluriels, montants. Clés dans 5 langues + test de présence. **Commit** `feat(ios): add budget, payment and meeting sheet data rules` — corps `Refs Swarm DAO #47 (layer 5b).`

### Task 2 : Source
Lectures dans `SharedEventModuleSheetSource` (même schéma détaché/annulable) ; conversion en `raw` Swift ; garde : ne lire que si le module est routé en sheet. Fonctions de conversion pures testées si elles portent de la logique. **Commit** `feat(ios): load budget, payment and meeting sheets off the main actor` + même corps.

### Task 3 : Routage et vue
- `EventHubRouting.sheetModules` += `.budget, .payments, .meetings` ; `sheetRoute(for:accessGranted:)` avec la garde de chaque module (dans `performHubRoute`, utiliser les fonctions de garde existantes de ContentView).
- `HubModuleSheetView` : actions principales/secondaires de ces modules ; replis via `HubSheetLifecycle` (`.budgetOverview`, `.paymentPot`, `.tricount`, `.meetingList`). Les écrans legacy gardent leurs retours existants (`RedesignBackRoute`) qui ramènent au hub.
- Mettre à jour `EventHubViewTests.testModuleScreens` et tests de routage ; tests de rendu (chargé/vide/échec) avec source factice ; tests de repli (mapping module/action → `AppView`).
**Commit** `feat(ios): open budget, payments and meetings in sheets` + même corps.

### Task 4 : Simulateur, suite, spec
- Événement en organisation (passer « Week-end confirmé » en organisation via le hub) et événement finalisé : ouvrir Budget, Paiements, Réunions ; vérifier medium/large, contenu, pastilles cohérentes avec les tuiles, « Voir les dépenses » → écran budget → retour hub, « Gérer la cagnotte », « Tricount », « Planifier une réunion » → écrans legacy et retour ; tuiles verrouillées hors organisation ; AX5 et sombre (remettre `large` / `light`). Captures `/tmp/wk-l5b-*.png` regardées. Flag éteint → inchangé.
- Désinstaller l'app, suite complète → seuls les 5 échecs préexistants (`InvitationExperienceRuntimeSurfaceTests`).
- Spec §15 « Couche 5b » (avant « ### Décisions »). **Commit** `docs(ios): record layer 5b sheets in redesign spec`.
- Supprimer `/tmp/wk-l5b-dd`.

### Task 5 : Revue de code puis corrections.
