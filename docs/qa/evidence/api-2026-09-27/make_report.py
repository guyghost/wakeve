import pickle, sys
sys.path.insert(0, "/tmp/wakeve-qa/api")
from lib import *  # noqa

scs = [pickle.load(open(f"/tmp/wakeve-qa/api/s{i}.pkl", "rb")) for i in (1, 2, 3, 4)]
tot_ok = sum(s.summary()[0] for s in scs)
tot_ko = sum(s.summary()[1] for s in scs)

head = f"""# QA API multi-utilisateurs — Wakeve (backend Ktor réel)

**Date** : 2026-09-27 · **Serveur** : `http://localhost:8080` (SQLite `server/wakev_server.db`, non redémarré) · **Méthode** : 4 scénarios joués par 22 comptes invités distincts (JWT réels), via REST + `/api/sync` uniquement. Scripts : `/tmp/wakeve-qa/api/` (`lib.py`, `s1_lisbonne.py` … `s4_fontainebleau.py`, `make_report.py`).

**Bilan : {tot_ok} ✅ / {tot_ko} ❌** ({', '.join(f'S{i+1} {s.summary()[0]}/{s.summary()[1]}' for i, s in enumerate(scs))})

| Scénario | Événement | ✅ | ❌ |
|---|---|---|---|
""" + "\n".join(f"| {s.title.split(' (')[0]} | `{s.ev}` | {s.summary()[0]} | {s.summary()[1]} |" for s in scs) + """

Rappels contrat : scores sondage YES=2 / MAYBE=1 / NO=-1 (calculés depuis `GET /poll`, aucun endpoint serveur ne renvoie le meilleur créneau) ; cycle DRAFT→POLLING→(sync)CONFIRMED→COMPARING (auto à la création d'un scénario)→ORGANIZING→FINALIZED. La confirmation passe **uniquement** par `POST /api/sync` (le `PUT /status CONFIRMED` renvoie 409 par design).

## Non-régressions vérifiées (bugs de la passe « Rio » du 18/09)
- BUG-1 changement de vote avant deadline ✅ (Jules NO→YES, pas de doublon) · BUG-2 confirmation via sync ✅ (y compris après scénarios/artwork) · BUG-3/5 finalisation atteignable par API + blockers listés en 409 ✅ · BUG-4 routes activités/équipement montées ✅ · BUG-6/7 baseline budget au 1er PUT + 400 sur payload incomplet ✅ · BUG-9 ICS `TRIGGER:-P1D` conforme + invités e-mail en ATTENDEE ✅.
- Gardes solides : vote pour autrui 403, vote hors groupe 403, vote après confirmation / après deadline 400, re-confirmation d'un autre créneau 409, confirmation par non-organisateur (sync) rejetée, `finalDate` ≠ début créneau rejetée, RSVP pour autrui 403 / mauvais créneau 400, quota `maxUses` → 410, lien inconnu 404, accept idempotent, scénarios/hébergement/budget/Tricount/réunion/readiness réservés à l'organisateur (403), URL Tricount piégée 422, chambres sur-capacité / non-membre 400, régimes alimentaires protégés (403 pour autrui et intrus), auteur de commentaire lié au JWT (usurpation 403, nom d'affichage serveur).

"""

bugs = r"""
## Bugs

> Tous reproduits sur le serveur en cours d'exécution. `$T_ORG`, `$T_MEMBRE`, `$T_INTRUS` = `accessToken` de `POST /api/auth/guest` ; `$EV` = id d'événement.

### P1

**BUG-A — Commentaires lisibles et publiables par n'importe quel utilisateur authentifié (fuite + spam)** — S1 Eve, S2 Nora
Un compte sans lien avec l'événement poste (201) et lit tout le fil (200, 3 commentaires lus).
```
curl -X POST localhost:8080/api/events/$EV/comments -H "Authorization: Bearer $T_INTRUS" -H 'content-type: application/json' \
  -d '{"section":"GENERAL","content":"je m incruste","authorId":"<id intrus>","authorName":"x"}'   # → 201
curl localhost:8080/api/events/$EV/comments -H "Authorization: Bearer $T_INTRUS"                  # → 200 + tous les commentaires
```
Cause : aucun contrôle organisateur/participant dans `server/.../routes/CommentRoutes.kt:50` (POST, seul `bindCommentAuthorToAuthenticatedUser` est vérifié) ni `:134` (GET liste) ; `commentRoutes(...)` ne reçoit même pas la DB participants. (Le « Steve 403 » du rapport Rio venait d'un `authorId` usurpé, pas d'un contrôle d'appartenance.) Corollaire : commentaire sur événement inexistant → 500 (FK) au lieu de 404.

**BUG-B — Équipement (« qui apporte quoi ») : aucune authentification/autorisation** — S4 Yann (intrus)
Yann lit la liste (200), ajoute un item (201), s'attribue le thermos de Zoé (200), change le statut des crash pads d'Ugo (200), les supprime (204). Idem après FINALIZED (modif 200, ajout 201). Événement inexistant → 500.
```
curl localhost:8080/api/events/$EV/equipment -H "Authorization: Bearer $T_INTRUS"                                   # → 200
curl -X PUT localhost:8080/api/events/$EV/equipment/$ITEM/assign -H "Authorization: Bearer $T_INTRUS" -H 'content-type: application/json' -d '{"participantId":"<id intrus>"}'  # → 200
curl -X DELETE localhost:8080/api/events/$EV/equipment/$ITEM -H "Authorization: Bearer $T_INTRUS"                   # → 204
```
Cause : `server/.../routes/EquipmentRoutes.kt:32-422` n'utilise jamais `principal` ni l'appartenance à l'événement, ne vérifie pas que `itemId` appartient à `{eventId}` (IDOR inter-événements) ni le statut FINALIZED ; `Application.kt:606` monte `equipmentRoutes(equipmentRepository)` sans `eventRepository`/`database` (contrairement à `activityRoutes`).

**BUG-C — Les dépenses avancées (`POST /budget/expenses`) sont ignorées par l'équilibrage** — S3
Karim avance 1 900 € (alliances) partagés Sophie+Karim → `201`, ligne présente en table `expense`, mais `GET /budget/settlements` → `{"settlements":[],"count":0}` (attendu : Sophie doit 950 € à Karim).
```
curl -X POST localhost:8080/api/events/$EV/budget/expenses -H "Authorization: Bearer $T_MEMBRE" -H 'content-type: application/json' \
  -d '{"amount":1900,"category":"OTHER","payerId":"<karim>","splitParticipantIds":["<sophie>","<karim>"]}'   # 201
curl localhost:8080/api/events/$EV/budget/settlements -H "Authorization: Bearer $T_ORG"                        # settlements: []
```
Cause : `shared/.../budget/BudgetRepository.kt:485` `getSettlements()` → `BudgetCalculator.calculateSettlements(items)` sur les seuls `budgetItem` ; la table `expense` (écrite par `BudgetRoutes.kt:480`) n'est lue nulle part dans le calcul.

**BUG-D — `GET /budget/items`, `/budget/summary`, `/budget/statistics` → 500 systématique** — S1, S3
Dès qu'un budget existe (avec ou sans items), ces 3 lectures échouent ; `GET /budget`, `/budget/items/{id}`, `/participants/{id}`, `/settlements` fonctionnent.
```
curl localhost:8080/api/events/$EV/budget/items -H "Authorization: Bearer $T_ORG"   # → 500 "Failed to fetch budget items"
```
Cause (lecture de code, seule différence avec les routes qui marchent) : réponse `mapOf(...)` à valeurs hétérogènes (`Map<String, Any>`) non sérialisable par kotlinx — `BudgetRoutes.kt:233` (`items`+`count`), `:744` (`budget`+`summary`+`itemCount`), `:934` (stats) ; les routes OK renvoient un DTO `@Serializable` ou une map homogène. Aucun test serveur ne couvre ces 3 routes.

### P2

**BUG-E — Création d'événement sans validation serveur** — S3
`maxParticipants < minParticipants` → 201 ; `deadline:"demain"` → 201 (puis **aucun vote possible**, `isDeadlinePassed` renvoie `true` si le parse échoue) ; créneau `end < start` → 201 ; `proposedSlots: []` → 201 puis DRAFT→POLLING accepté (sondage sans créneau).
```
curl -X POST localhost:8080/api/events -H "Authorization: Bearer $T_ORG" -H 'content-type: application/json' \
  -d '{"title":"T","description":"d","organizerId":"x","deadline":"demain","proposedSlots":[{"id":"a","start":"2027-01-02T10:00:00Z","end":"2027-01-01T10:00:00Z","timezone":"Europe/Paris"}],"minParticipants":10,"maxParticipants":2}'   # → 201
```
Cause : `EventRoutes.kt:155` (POST) n'appelle jamais `Event.validate()` (`shared/.../models/Event.kt:45`, qui gère min/max) ni de parse ISO ; `EventRoutes.kt:749` autorise DRAFT→POLLING sans vérifier ≥1 créneau (règle AGENTS.md « Step 4 : au moins 1 créneau ») ; `DatabaseEventRepository.kt:1554` traite une deadline illisible comme dépassée.

**BUG-F — Réunion : `startTime` invalide → 500 avec écriture partielle** — S1
`{"platform":"GOOGLE_MEET","title":"Brief bis","startTime":"mardi soir"}` → 500, mais la réunion est **persistée** (visible dans `GET /meetings/persisted`, `startTime='mardi soir'`, 0 rappel en table `meeting_reminder`).
```
curl -X POST localhost:8080/api/events/$EV/meetings/persisted -H "Authorization: Bearer $T_ORG" -H 'content-type: application/json' -d '{"platform":"GOOGLE_MEET","title":"Brief bis","startTime":"mardi soir"}'
```
Cause : `MeetingRoutes.kt:118` `insertMeeting` exécuté avant `Instant.parse(request.startTime)` (`:151`), hors transaction et sans validation ; accessoirement la réponse annonce `PENDING_LINK` (`:116`) alors que `SCHEDULED` est stocké.

**BUG-G — Événement FINALIZED non read-only pour réunions, équipement, activités, invitations** — S1, S2, S4
Après FINALIZED : `POST /meetings/persisted` 201, `POST /equipment` 201 + `PUT /equipment/{id}/status` 200, `POST /activities` 201, et **Nora (inconnue) rejoint l'événement finalisé** via un lien encore valide (200). Budget, transport, statut sont eux correctement bloqués (409).
Cause : pas de garde statut dans `MeetingRoutes.kt:71-165`, `EquipmentRoutes.kt` (tout le fichier), `ActivityRoutes.kt:258-311`, ni dans l'accept `InvitationRoutes.kt:252-300` (seuls quota/expiration sont testés).

**BUG-H — Inscription aux activités au nom de n'importe qui** — S4
Ugo inscrit Théo (201), Yann non-membre (201) et même un id inexistant `"n_importe_quoi"` (201).
```
curl -X POST localhost:8080/api/events/$EV/activities/$ACT/register -H "Authorization: Bearer $T_MEMBRE" -H 'content-type: application/json' -d '{"participantId":"n_importe_quoi"}'   # → 201
```
Cause : `ActivityRoutes.kt:337-370` ne compare pas `request.participantId` à l'appelant, ne vérifie pas qu'il est membre, ni que `activityId` appartient à `{eventId}`.

**BUG-I — Une sortie gratuite / soirée à la maison ne peut pas être finalisée sans inventer un budget et un Tricount** — S2, S4
`POST /readiness/BUDGET_BASELINE/not-needed` et `/readiness/PAYMENT/not-needed` → 400 « Unsupported readiness section ». Blockers restants : `BUDGET_REQUIRED, PAYMENT_POT_REQUIRED, TRICOUNT_HANDOFF_REQUIRED` ; contournement utilisé : budget 15 € + faux lien Tricount. Idem, un scénario « lieu » est obligatoire (FINAL_SCENARIO/DESTINATION) même pour un RDV parking.
Cause : `EventRoutes.kt:618` n'accepte que `MEETINGS`/`LODGING`, alors que `BudgetRepository.kt:418` lit déjà une décision `BUDGET_BASELINE` et `TricountHandoffRepository.kt:115` un `explicitNotNeeded` qu'aucune route n'écrit. Contredit le gate « réduire la charge mentale » d'AGENTS.md.

### P3
- **500 au lieu de 400 sur payloads invalides** (catch générique) : hébergement incomplet `AccommodationRoutes.kt:127` ; repas incomplet `MealRoutes.kt:112` ; régime `"PALEO"` (enum inconnue) `MealRoutes.kt:433+` ; équipement catégorie inconnue `EquipmentRoutes.kt:193` ; activité incomplète `ActivityRoutes.kt:274` (receive) → catch `:307` ; commentaire `content:""` `CommentRoutes.kt:127` ; commentaire/équipement sur événement inexistant (FK) → 500.
- **Baseline budget négative acceptée** : `PUT /budget {"totalEstimated":-5}` → 200 (`BudgetRoutes.kt:123-150`, aucune validation de montants).
- **Rappel manuel non implémenté** : `POST /calendar/reminders/one_day_before` → 501 (`CalendarRoutes.kt:311` TODO). Le rappel J-1 automatique créé avec la réunion fonctionne.
- **Messages d'erreur trompeurs** : vote après deadline / après confirmation → « Failed to save your vote. Please try again. » (`VoteRoutes.kt:146`) — l'utilisateur est invité à réessayer une action définitivement impossible ; titre vide → message de modération « This content cannot be posted ».
- **Produit (non-bugs, à arbitrer)** : pas de rôle co-organisateur (Karim reçoit 403 sur toutes les actions organisateur) ; seul l'organisateur crée les repas (un participant ne peut pas dire « j'apporte les pizzas », 403) alors qu'il peut créer équipement/activités ; aucun endpoint météo (table `eventWeatherSnapshot` sans route, 404) ; une date déjà connue impose 2 appels (POLLING puis confirmation sync sans vote) — fonctionne mais pas de raccourci.

## Non couvert
WebSocket chat, push APNs/FCM, sync offline côté client, UI Android/iOS, génération de plan transport avec provider externe (le provider local déterministe a bien produit un plan covoiturage 3 routes en S4).
"""

md = head + "\n\n".join(s.to_md() for s in scs) + "\n" + bugs
open("/tmp/wakeve-qa/api-multiuser-report.md", "w").write(md)
print(len(md), tot_ok, tot_ko)
