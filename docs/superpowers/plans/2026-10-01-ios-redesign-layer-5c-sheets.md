# Refonte iOS — Couche 5c (Commentaires, Transport, Invités) — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal :** Terminer la couche 5 : (1) rendre les commentaires réellement utilisables (aujourd'hui l'envoi, la réponse, la modification, la suppression et l'épinglage sont sans effet) ; (2) ouvrir **Transport** et **Invités** depuis le hub dans une `WKModuleSheet` de résumé, avec repli plein écran vers les écrans existants pour toute modification.

**Base :** socle 5a/5b — `WKModuleSheet`, `HubModuleSheetData`/`HubModuleSheetRaw`/`primaryAction`, `SharedEventModuleSheetSource` (`readsDatabase(for:)`, `Task.detached`), `HubModuleSheetView`/`HubModuleSheetBody`, `EventHubRouting.sheetModules` + `sheetGuard` + `sheetRoute`, `HubSheetLifecycle` (repli `(eventId, AppView)`), `RedesignBackRoute`. Lire ces fichiers d'abord.

**Spec :** §5.3, §15 « Couche 5a/5b » — **Proposition :** Swarm DAO #47.

## 1. Commentaires (correctif de bug, pas de nouvelle surface)

Constat : `EventCommentsRouteView` (`iosApp/src/Views/Events/EventSecondaryRouteViews.swift:342`) n'injecte aucun callback d'écriture dans `CommentListView` (`iosApp/src/Views/Collaboration/CommentListView.swift`), dont les `onAddComment`/`onReply`/`onEdit`/`onDelete`/`onPin` restent des no-op par défaut. Et `CommentSectionType.meal/.scenario/.poll/.budget` → `sharedValue = nil` (commentaires de ces sections mélangés avec « général » ou perdus — à vérifier).

- Lire l'API Kotlin du dépôt de commentaires (`IosFactory.shared.createCommentRepository(database:)`, `Shared.h`) : création, réponse, modification, suppression, épinglage, et ce que l'écran Android correspondant appelle (rechercher dans `composeApp`).
- Brancher les callbacks dans `EventCommentsRouteView` (le `case .comments` legacy ne change pas ; seule la vue de route est modifiée) : chaque écriture appelle le dépôt, puis recharge `comments`. Erreurs Kotlin non déclarées : si les méthodes ne sont pas `@Throws`, les annoter `@Throws(IllegalArgumentException::class)` (ou l'exception réelle) côté Kotlin et entourer de `do/try/catch` côté Swift, avec un message d'erreur localisé.
- Autorisations : modifier/supprimer seulement ses propres commentaires ; épingler réservé à l'organisateur (règle existante de `CommentListView` à respecter, sinon celle du dépôt).
- Sections : corriger le mapping `sharedValue` des sections manquantes **si** le dépôt Kotlin les supporte (énumération partagée) ; sinon documenter.
- Tests : Kotlin (jvmTest, motif des tests de dépôt existants) pour toute annotation/garde ajoutée ; Swift : fonction pure de mapping des sections + test source que la route injecte les 5 callbacks ; vérification simulateur (envoyer, répondre, modifier, supprimer).
- Garder les ancres : `ParityRouteInventoryContractTests` (`.comments` → `EventCommentsRouteView`), `FindingsRegressionTests:259`, `PremiumNavigationContractTests:82`.
- **Commit** `fix(ios): make event comments actually post, reply, edit, delete and pin` (correctif → pas de proposition DAO requise, mais référencer #47 dans le corps car découvert pendant la refonte).

## 2. Transport en sheet de résumé

- Garde : `canAccessTransportPlanning(for:)` (confirmé/organisation/finalisé + accès) — identique au `case .transportPlanning`.
- Données (lecture seule, sans passer par `TransportPlanningViewModel`, possédé par `AuthenticatedView`) : `TransportRepositoryBridge(database:)` — plans de l'événement (`getPlansByEvent`), plan retenu (`getSelectedPlanId`), état « non nécessaire » s'il existe une lecture synchrone ; départs des participants s'ils sont lisibles de façon synchrone (sinon « N participants sans lieu de départ » omis). Lire `TransportPlanningViewModel.swift` pour les appels exacts et ne rien écrire.
- Contenu : carte plan retenu (mode, coût total, durée si dispo) ou cartes des plans proposés ; pastille « Plan choisi » (`.confirmed`) / « À décider » (`.pending`) / « Pas nécessaire » (neutre).
- Phrase manquante : aucun plan → `hub.sheet.transport.no_plan` ; plans sans choix → `hub.sheet.transport.choose`.
- Actions : principale « Organiser le transport » → repli `.transportPlanning` (pour tous ceux qui ont accès ; c'est l'écran legacy qui applique ses propres droits) ; secondaire « Commentaires » (section transport si elle existe) ; pas de « Plein écran » en double.
- Le résumé de la tuile Transport du hub doit utiliser la même fonction pure que la pastille.
- **Commit** `feat(ios): open transport in a summary sheet` — corps `Refs Swarm DAO #47 (layer 5c).`

## 3. Invités en sheet de résumé

- Garde : aucune (tuile jamais verrouillée), comme aujourd'hui.
- Données : `getParticipantRecords(eventId:)` + `ParticipantAccessMapper` (déjà utilisé par `SharedEventHubSource`) ; noms via `userQueries.selectUserById` (cache par chargement).
- Contenu : sections « Confirmés » / « En attente » / « Ont décliné » (cartes avec avatar + nom ; l'organisateur marqué « Organisateur ») ; pastille « N confirmés » (pluriels existants `hub.confirmed_count`).
- Phrase manquante : invités en attente → `hub.sheet.participants.pending_count` (pluriel) ; personne invité → `hub.sheet.participants.empty`.
- Actions : principale « Inviter » (organisateur ∧ non finalisé) → repli vers la route d'ajout de participants existante, **sensible au flag invitations** : réutiliser `EventHubRouting.addParticipantsRoute(...)` (rollout éteint → `.participantManagement` ; allumé → audience via le routeur d'invitations). Non-organisateur : pas d'action principale ; secondaire « Plein écran » vers la même route de consultation.
- **Commit** `feat(ios): open participants in a summary sheet` + même corps.

## 4. Fin de couche

- `EventHubRouting.sheetModules` inclut alors tous les modules à résumé : `meals, equipment, activities, accommodation, photos, budget, payments, meetings, transport, participants` (Date, Lieu, Scénarios et Récap restent des écrans pleins : vote, résultats, comparaison).
- Mettre à jour les tests hub/sheet (`testModuleScreens`, routage, rendu chargé/vide/échec, accessibilité AX5).
- Simulateur (« iPhone 18 Pro » seulement, args QA `-iosRedesign2026 YES -hasCompletedOnboarding YES --wakeve-debug-authenticated --wakeve-qa-seed-invitation-experience --wakeve-qa-open-invitation-route library`) : commentaires (envoyer/répondre/modifier/supprimer), sheets Transport et Invités, replis et retours, flag invitations éteint et allumé pour « Inviter », AX5 et sombre (remettre les réglages). Captures `/tmp/wk-l5c-*.png` regardées. Flag éteint → inchangé.
- Désinstaller l'app, suite complète → seuls les 5 échecs préexistants.
- Spec §15 « Couche 5c » + ligne §9. **Commit** `docs(ios): record layer 5c in redesign spec`.
- Revue de code puis corrections.

## Contraintes

- Worktree `/Users/guy/Developer/dev/wakeve/.claude/worktrees/ios-app-design-e1ae98`. Jamais `git stash`. Jamais indexer `.dao/*`. **Aucune ligne d'attribution.** Français au tutoiement ; `plutil -lint` sur les chaînes modifiées.
- Un seul DerivedData `/tmp/wk-l5c-dd` (supprimé à la fin) ; `df -h /System/Volumes/Data` avant chaque build ; **arrêt et rapport sous 3 Go**. `-parallel-testing-enabled NO` ; tuer son propre `xcodebuild` s'il ne rend pas la main après le résultat.
- Ne pas modifier les `case` legacy, `TransportPlanningView`, `ParticipantManagementView`, `EventAudienceView`, `CommentListView` (sauf si un callback indispensable manque — alors l'ajouter avec une valeur par défaut, sans casser les ancres), ni les tests d'ancrage d'autres fichiers.
