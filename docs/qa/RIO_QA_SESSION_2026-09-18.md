# 🇧🇷 Rapport QA — « Vacances à Rio » (groupe de 3, backend réel)

**Date** : 2026-09-18 · **Serveur** : Ktor local `:8080` (SQLite `server/wakev_server.db`) · **Méthode** : simulation bout-en-bout de 3 acteurs via l'API REST/sync réelle

## Casting
| Acteur | Appareil | Plateforme | Identité |
|---|---|---|---|
| 👩 Alice (organisatrice) | iPhone 15 Pro | iOS (Universal Link) | guest `user_-2030849853…` |
| 👨 Bob | iPhone 13 | iOS | guest `user_1614500898…` |
| 👩🏽 Carla | Pixel 8 | Android (App Link) | guest `user_1941217368…` |
| Steve (intrus, tests négatifs) | téléphone | — | guest jetable |

> Les 2 iOS et l'Android consomment le **même contrat API** ; les différences plateformes (deep link `wakeve://` vs App Link, EventKit vs CalendarContract, APNs vs FCM) sont hors périmètre de cette passe backend — voir « Non couvert ».

## Scénario joué (flux nominal)
Auth guest ×3 → création événement CUSTOM « Vacances à Rio 🇧🇷 » (2 créneaux, tz America/Sao_Paulo, deadline 5 oct) → DRAFT→POLLING → lien d'invitation (maxUses=2) → Bob & Carla rejoignent → votes 6/6 (Semaine 1 = 6 pts 🏆 vs Semaine 2 = 1 pt) → CONFIRMED (semaine 1, 5→12 déc) → RSVP ×3 (`VALIDATED_RETAINED_DATE`) → scénarios Copacabana vs Ipanema + votes (Ipanema score 5) → select-final → ORGANIZING → budget baseline 4 200 € → cagnotte Tricount + lien → hébergement confirmé → réunion briefing + rappel → commentaires ×3 → FINALIZED → ICS.

---

## 🔴 Bloquants (P0)

### BUG-1 — Impossible de modifier son vote avant la deadline
`POST /api/events/{id}/poll/votes` renvoie 400 au 2ᵉ vote d'un même participant/slot.
**Root cause** : `DatabaseEventRepository.addVote` (L370) appelle toujours `insertVote` (INSERT brut) alors que `vote` a `UNIQUE(timeslotId, participantId)` ; la requête `updateVote` existe dans `Vote.sq` mais n'est jamais utilisée.
**Impact** : cœur du produit (on change d'avis en groupe).
**Fix** : select-then-update ou upsert.

### BUG-2 — Confirmation de date impossible via sync (`EVENT_AGGREGATE_WRITER_FENCED`)
`PUT /api/events/{id}/status` (CONFIRMED) renvoie volontairement 409 « use the modeled confirmation command » → la voie normale est `POST /api/sync` (`applyConfirmedEventDecision`). Or celui-ci écrit `updateEventStatus` **sans insérer le permis** `aggregate_write_authorization` requis par le trigger `fence_protected_event_update` → `RAISE(ABORT)` → rollback silencieux → conflit `SERVER_WINS` **permanent** dès que `aggregateRevision > 1` (tout événement ayant changé de statut une fois) + ligne `event_artwork` (créée pour tout événement).
**Fix** : `SyncService.applyConfirmedEventDecision` doit suivre le protocole du repo (`authorizeAggregateWrite` → update → `clearAggregateWriteAuthorization`), comme dans `DatabaseEventRepository` L882-1096.

### BUG-3 — Finalisation impossible via l'API (checklist insatisfiable + spirale de deadlock)
`ORGANIZING → FINALIZED` exige la checklist `EventOrganizationReadinessRepository` mais :
- **MEETING_REQUIRED** : aucune route REST ni handler sync pour créer une réunion (`insertMeeting` n'est appelé que côté client, DB locale) → insatisfiable serveur ;
- **NOTIFICATION_REMINDERS_REQUIRED** : rappels créés uniquement côté client → idem ;
- **catch-22 hébergement** : en ORGANIZING l'ajout d'hébergement est refusé (409, réservé CONFIRMED/COMPARING) alors que la checklist exige un hébergement CONFIRMED pour finaliser ;
- **spirale QA-15b** : chaque tentative FINALIZED échouée insère une ligne `syncMetadata synced=0` qui fait échouer **toutes** les tentatives suivantes, même après correction des vrais blocages (`updateEventStatus` insère `sync_status_*_FINALIZED` puis la readiness la voit en pending).
**Workaround QA** : 3 écritures DB manuelles (réunion, hébergement, syncMetadata).

### BUG-4 — Activités et équipements inutilisables (routes non montées)
`activityRoutes` et `equipmentRoutes` **ne sont pas enregistrés** dans `Application.kt` → 404 systématique sur `/api/events/{id}/activities` et équipements. La phase ORGANIZING perd sa fonctionnalité phare.

---

## 🟠 Majeurs (P1)

### BUG-5 — Le message de finalisation ne dit pas ce qui manque
La failure renvoie « Failed to update event status. Please try again. » au lieu de lister les blockers (`MEETING_REQUIRED`, `LODGING_REQUIRED`, `TRICOUNT_HANDOFF_REQUIRED`, `CRITICAL_SYNC_PENDING`…). L'organisateur ne peut pas agir. **Fix** : exposer `GET …/readiness` agrégé + renvoyer les blockers dans la 400.

### BUG-6 — Baseline budget perdue silencieusement (1ʳᵉ sauvegarde)
`PUT /api/events/{id}/budget` : si aucun budget n'existe, la branche `createBudget(eventId)` **ignore les valeurs reçues** et crée un budget à zéro (réponse 200 OK). Le 2ᵉ PUT fonctionne. **Fix** : upsert avec les valeurs reçues.

### BUG-7 — Payload budget incomplet → 500 au lieu de 400
`createdAt`/`updatedAt` requis dans le DTO → `MissingFieldException` → catch générique 500.

### BUG-8 — Commentaires : DTO divergent + 500 générique
Le vrai DTO (local à `CommentRoutes.kt:834`) exige `authorId`/`authorName`/`section` ; celui de `models/CommentDTOs.kt` (`participantId`/`parentId` + `toComment()`) est du **code mort masqué**. Payload naïf → 500 au lieu de 400/403. Steve (hors groupe) est bien 403 ensuite ✅.

### BUG-9 — ICS non conforme RFC 5545 (VALARM)
`TRIGGER:-P1DT090000` est une durée invalide (désignateur `H` manquant : `-P1DT9H` ou datetime). De plus, les `invitees` passés en requête sont **ignorés** : ORGANIZER/ATTENDEE pointent vers les adresses guest internes (`CN=Invité`), pas vers les e-mails réels.

---

## 🟡 Mineurs / produit (P2-P3)

- **10 (prod)** : seuls les organisateurs peuvent créer des items de budget (403 pour participants) et déclarer « transport not needed » — à valider vs vision Tricount collaborative.
- **11 (UX)** : `authorName` fourni non conservé → fil affiche `guest_82383fd4…` au lieu des prénoms.
- **13 (produit)** : guests sans refresh token (voulu) mais session 1 h : une planification guest est coupée sans reconnect.
- **15 (design sync)** : toute mutation serveur crée des `syncMetadata synced=0` qui bloquent la readiness (`CRITICAL_SYNC_PENDING`) — deadlock auto-infligé dès qu'un client ne « tire » pas la sync.
- Cosmétique : IDs d'événements `event_<ms>_<float>` ; `updatedAt` en nanosecondes (`…575971001Z`) ; erreurs commentaires ICS.

---

## ✅ Ce qui fonctionne bien (vérifié)
- Auth guest multi-devices, JWT, permissions EVENT_*/VOTE_*.
- Invitations : résolution publique, code 8 car., deep link, **quota maxUses → 410** pour Steve.
- Guards d'autorisation systématiques : vote hors groupe 403, vote invalide 400, RSVP mauvais créneau 400, select-final par non-organisateur 403, commentaires par intrus 403, réouverture d'événement finalisé 403, endpoint budget/transport 404 pour intrus.
- Machine à statuts : transitions DRAFT→POLLING, CONFIRMED→ORGANIZING, garde `isAllowedEventStatusTransition`, FINALIZED irréversible.
- Scénarios : création 201, votes PREFER/NEUTRAL/AGAINST, scoring cohérent (2/1/0/-1), sélection finale, vote invalide 400.
- Budget : baseline (2ᵉ PUT), items, catégories, cagnotte Tricount + lien trusted (regex host `.tricount.com`).
- ICS : structure VCALENDAR/VEVENT/VALARM, UID stable, échappements texte corrects.
- Modération, rate limiting, audit logs budgétaires en place.

## Non couvert (prochaine passe)
- UI réelle : wizard DRAFT Android (Material You) vs iOS (Liquid Glass), screens Poll/Scenario/Chat.
- WebSocket chat temps réel, APNs/FCM (delivery worker), offline sync côté client (SQLDelight local-first), EventKit/CalendarContract natifs, génération de plans transport, auto-génération de repas, gamification dashboard.

## Environnement de test
Serveur démarré avec `JWT_SECRET` requis (sinon crash au boot — à documenter en README dev).
DB de test : `server/wakev_server.db` (peut être supprimée pour repartir de zéro).
