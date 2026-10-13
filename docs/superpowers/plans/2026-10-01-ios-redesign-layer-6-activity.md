# Refonte iOS — Couche 6 (zone Activité `ActivityView`) — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal :** Sous `iosRedesign2026`, remplacer `InboxView` dans la zone Activité par `ActivityView` : entrées **groupées par événement**, filtres **« À traiter (n) »** (défaut) · **« Tout »**, point rouge sur ce qui attend une action, messages résumés en une ligne (« 3 nouveaux messages ») ; un tap ouvre le hub, l'écran de vote/résultats ou les commentaires de l'événement. Le badge Activité de la barre flottante = nombre d'éléments « À traiter » (actions réelles + notifications non lues).

**Décisions (2026-10-01) :**
- « À traiter » est calculé à partir de **l'état réel** des événements (mêmes règles que l'accueil, `HomeEventFacts`) : vote requis, prêt à confirmer (organisateur), invitation en attente de réponse. Les types de notification ne décident pas qu'une action est requise (une notification de vote reste sinon « à traiter » après avoir voté).
- « Tout » ajoute les notifications (table `notification`, triées par `created_at`) et une ligne « N nouveaux messages » par événement.
- Messages = commentaires de l'événement. Pas d'état de lecture par utilisateur en base : un marqueur local `eventId → dernière consultation` (UserDefaults, via une petite abstraction injectable) + `commentQueries.countRecentActivity(event_id, created_at >)` (requête existante, `Comment.sq:212`). Le marqueur est mis à jour quand l'utilisateur ouvre les commentaires depuis Activité.
- `InboxView`, `InboxViewModel`, `InboxDetailView` restent intacts (chemin legacy et leurs tests de contrat).
- Le filtre du deep link `wakeve://notifications?filter=unread` : `unread` → filtre « Tout », sinon « À traiter » (via une propriété du routeur).

**Spec :** §5.4 — **Proposition :** Swarm DAO #47.

## Architecture

1. **Pur** `iosApp/src/Models/Activity/ActivityFeed.swift` :
   - `ActivityTarget { hub(eventId), vote(eventId), pollResults(eventId), comments(eventId) }`.
   - `ActivityEntry { id, eventId, kind, needsAction, date: Date?, target }` avec `kind ∈ { voteRequired, readyToConfirm, rsvpPending, notification(title, message, isRead), messages(count) }`.
   - `ActivityEventGroup { eventId, title, entries, actionCount, latestDate }`.
   - `enum ActivityFilter { toDo, all }`.
   - `static func build(facts: [ActivityEventFacts], notifications: [ActivityNotification], newMessages: [String: Int], filter:) -> [ActivityEventGroup]` : groupes ayant une action d'abord, puis date la plus récente ; en « À traiter », seules les entrées `needsAction` et les groupes non vides ; « Tout » ajoute notifications (non lues d'abord, puis récentes) et ligne messages (si > 0). Les notifications sans événement vont dans un groupe « Général » (clé `inbox.general_conversation` existante) en fin de liste.
   - `static func badgeCount(...)` = entrées d'action + notifications non lues.
   - `ActivityEventFacts` : réutiliser `HomeEventFacts` (via `EventsHomeViewModel.facts(from:now:)`) + `rsvpPending: Bool`.
2. **Source** `iosApp/src/Services/SharedActivitySource.swift` (protocole `ActivitySource`), hors thread principal (`Task.detached` + annulation, comme `SharedEventsHomeSource`) :
   - événements et faits : réutiliser `SharedEventsHomeSource` (ne pas dupliquer) ; RSVP du spectateur : `getParticipantRecords` + `ParticipantAccessMapper` (comme `SharedEventHubSource`), `rsvpPending` = membre non organisateur dont `rsvp == .pending` ;
   - notifications : `notificationQueries.getNotifications(user_id:, value_: 50)`, `created_at` comme date, `data` parsé en `[String: Any]` (extraire `eventId` en chaîne), titre/message tels quels ;
   - nouveaux messages : `countRecentActivity` depuis le marqueur (absent → depuis 7 jours, borne pour éviter « 200 nouveaux messages » au premier lancement), en excluant ses propres commentaires si la requête le permet (sinon documenter).
3. **ViewModel** `iosApp/src/ViewModels/ActivityViewModel.swift` : `state`, `filter` (défaut `.toDo`), `groups`, `toDoCount`, `reload()` avec compteur de génération, annulation ignorée, données conservées en cas d'échec ; `markSeen(eventId:)` met à jour le marqueur et recharge.
4. **Vue** `iosApp/src/Views/Activity/ActivityView.swift` : titre « Activité » (`wk.nav.activity`), contrôle segmenté natif (`Picker` `.segmented`) « À traiter (n) » / « Tout », liste de `WKCard` par événement (titre de l'événement, lignes : point `WK.Status.actionNeeded.color` si action, libellé, date relative courte), ligne messages, états vide (« Rien à traiter pour l'instant » / « Aucune activité »), chargement, échec + réessayer, `.refreshable`. Mêmes paramètres que `InboxView` pour le shell : `reloadToken: Int = 0`, `onRootStateChange: ((Bool) -> Void)? = nil` (toujours racine → appeler `true`), plus `actionCount: Binding<Int>`, `initialFilter`, `onOpen: (ActivityTarget) -> Void`.
5. **Branchement** `AuthenticatedView.redesignChrome` : remplacer la closure `activity:` par `ActivityView` ; badge = nouveau `@State activityToDoCount` (le badge legacy garde `unreadInboxCount`) ; `openActivityTarget(_:)` : passer `redesignRouter.zone = .events` **puis** naviguer au tour de boucle suivant (`Task { @MainActor in … }`), car le changement de zone ferme les sheets du hub : `hub` → `openEventFromHome`, `vote`/`pollResults` → `handleHomeNextStep` équivalent, `comments` → `selectedCommentSection = .general` + route `.comments` existante (garde incluse) + `markSeen`.

## Clés (préfixe `activity.feed.*`, 5 langues, fr au tutoiement)

| Clé | fr | en |
|---|---|---|
| activity.feed.filter.todo_format | À traiter (%d) | To do (%d) |
| activity.feed.filter.all | Tout | All |
| activity.feed.vote_required | Vote : il manque ton vote | Vote: yours is missing |
| activity.feed.ready_to_confirm | Tout le monde a voté, choisis la date | Everyone voted, pick the date |
| activity.feed.rsvp_pending | Réponds à l'invitation | Reply to the invitation |
| activity.feed.messages_count (stringsdict) | %d nouveau message / %d nouveaux messages | %d new message(s) |
| activity.feed.empty.todo | Rien à traiter pour l'instant | Nothing to do right now |
| activity.feed.empty.all | Aucune activité pour l'instant | No activity yet |

es/it/pt : traductions équivalentes. Réutiliser `inbox.general_conversation`, `notifications.time.*` si adaptés, `common.retry`, `common.error_generic`.

## Contraintes

- Worktree `/Users/guy/Developer/dev/wakeve/.claude/worktrees/ios-app-design-e1ae98`. Jamais `git stash`. Jamais indexer `.dao/*`. **Aucune ligne d'attribution.** Français au tutoiement ; `plutil -lint` sur les chaînes modifiées.
- Un seul DerivedData `/tmp/wk-l6-dd` (supprimé à la fin) ; `df -h /System/Volumes/Data` avant chaque build ; arrêt et rapport sous 3 Go. `-parallel-testing-enabled NO` ; tuer son propre `xcodebuild` bloqué après le résultat.
- Simulateur « iPhone 18 Pro » seulement (`device` explicite), jamais « Wakeve-QA-iPhone-16-Pro » ; args QA `-iosRedesign2026 YES -hasCompletedOnboarding YES --wakeve-debug-authenticated --wakeve-qa-seed-invitation-experience --wakeve-qa-open-invitation-route library` (relancer si le chargement bloque) ; désinstaller l'app avant la suite complète.
- Ne pas modifier `InboxView`/`InboxViewModel`/`InboxDetailView`, les `case` legacy, ni les tests d'ancrage d'autres fichiers — **sauf** les assertions de `RedesignShellTests` qui visent la closure `activity:` / `InboxView.swift` dans le shell (les rediriger vers `ActivityView`).

## Tâches (TDD, un commit chacune, corps `Refs Swarm DAO #47 (layer 6).`)

1. `feat(ios): add pure activity feed rules` — `ActivityFeed` + tests (regroupement, tri, filtres, groupe Général, badge, cibles, ligne messages, action RSVP vs vote, notifications non lues d'abord).
2. `feat(ios): add localized copy for the activity feed` — clés + test de présence 5 langues (+ stringsdict).
3. `feat(ios): load the activity feed off the main actor` — `ActivitySource`, marqueur de consultation injectable (testé), `SharedActivitySource`, `ActivityViewModel` + tests (états, génération, `markSeen`).
4. `feat(ios): add the activity view` — vue + tests (rendu chargé/vide/échec, AX5 sans débordement, segment, accessibilité des lignes : point rouge non porteur seul — le libellé dit « À traiter »).
5. `feat(ios): show the activity feed in the Activity zone` — branchement, badge, `openActivityTarget`, filtre du deep link ; tests source/pure + `RedesignShellTests` redirigés ; `AppRouterTests`, `ParityDeepLinkContractTests`, `PremiumMessagesContractTests`, `InboxViewModelTests`, `WKRenderingTests` verts.
6. Simulateur (Activité : À traiter/Tout, tap vote → écran de vote, tap messages → commentaires puis retour et ligne disparue, invitation en attente si disponible, badge, deep link `wakeve://notifications`, AX5, sombre ; captures `/tmp/wk-l6-*.png` regardées ; flag éteint → Inbox legacy inchangée), suite complète (5 échecs préexistants seulement), spec §15 « Couche 6 » + ligne §9. Commit `docs(ios): record layer 6 activity in redesign spec`.
7. Revue de code puis corrections.
