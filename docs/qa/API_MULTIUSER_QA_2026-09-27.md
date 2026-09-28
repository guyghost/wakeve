# QA API multi-utilisateurs — Wakeve (backend Ktor réel)

**Date** : 2026-09-27 · **Serveur** : `http://localhost:8080` (SQLite `server/wakev_server.db`, non redémarré) · **Méthode** : 4 scénarios joués par 22 comptes invités distincts (JWT réels), via REST + `/api/sync` uniquement. Scripts : `/tmp/wakeve-qa/api/` (`lib.py`, `s1_lisbonne.py` … `s4_fontainebleau.py`, `make_report.py`).

**Bilan : 346 ✅ / 39 ❌** (S1 116/8, S2 67/6, S3 110/10, S4 53/15)

| Scénario | Événement | ✅ | ❌ |
|---|---|---|---|
| Scénario 1 — Road trip à Lisbonne | `event_1790544700077_3ff8ce04` | 116 | 8 |
| Scénario 2 — Watch party finale Ligue des champions | `event_1790544815396_c434c69c` | 67 | 6 |
| Scénario 3 — Mariage de Sophie & Karim | `event_1790545310000_58dad50e` | 110 | 10 |
| Scénario 4 — Balade en forêt de Fontainebleau | `event_1790545207320_368dfbdf` | 53 | 15 |

Rappels contrat : scores sondage YES=2 / MAYBE=1 / NO=-1 (calculés depuis `GET /poll`, aucun endpoint serveur ne renvoie le meilleur créneau) ; cycle DRAFT→POLLING→(sync)CONFIRMED→COMPARING (auto à la création d'un scénario)→ORGANIZING→FINALIZED. La confirmation passe **uniquement** par `POST /api/sync` (le `PUT /status CONFIRMED` renvoie 409 par design).

## Non-régressions vérifiées (bugs de la passe « Rio » du 18/09)
- BUG-1 changement de vote avant deadline ✅ (Jules NO→YES, pas de doublon) · BUG-2 confirmation via sync ✅ (y compris après scénarios/artwork) · BUG-3/5 finalisation atteignable par API + blockers listés en 409 ✅ · BUG-4 routes activités/équipement montées ✅ · BUG-6/7 baseline budget au 1er PUT + 400 sur payload incomplet ✅ · BUG-9 ICS `TRIGGER:-P1D` conforme + invités e-mail en ATTENDEE ✅.
- Gardes solides : vote pour autrui 403, vote hors groupe 403, vote après confirmation / après deadline 400, re-confirmation d'un autre créneau 409, confirmation par non-organisateur (sync) rejetée, `finalDate` ≠ début créneau rejetée, RSVP pour autrui 403 / mauvais créneau 400, quota `maxUses` → 410, lien inconnu 404, accept idempotent, scénarios/hébergement/budget/Tricount/réunion/readiness réservés à l'organisateur (403), URL Tricount piégée 422, chambres sur-capacité / non-membre 400, régimes alimentaires protégés (403 pour autrui et intrus), auteur de commentaire lié au JWT (usurpation 403, nom d'affichage serveur).

### Scénario 1 — Road trip à Lisbonne (Alice org., Bruno, Chloé, David + Eve intruse) — 116 ✅ / 8 ❌

| # | Acteur | Endpoint | Étape | Attendu | Obtenu | | Gist |
|---|---|---|---|---|---|---|---|
| 1 | Alice | `POST /api/events` | création événement | 201 | 201 | ✅ | id=event_1790544700077_3ff8ce04 status=DRAFT |
| 2 | Alice | `POST /api/events/{ev}/participants` | ajout Bruno (DRAFT) | 201 | 201 | ✅ | { "participants": [ "user_2099304043_-161f4ff06162290", "user_-366632177_-6312fe96521e27a" ] } |
| 3 | Alice | `POST /api/events/{ev}/participants` | ajout Chloé (DRAFT) | 201 | 201 | ✅ | { "participants": [ "user_2099304043_-161f4ff06162290", "user_-366632177_-6312fe96521e27a", "user_-1657346297_ |
| 4 | Alice | `POST /api/events/{ev}/participants` | ajout David (DRAFT) | 201 | 201 | ✅ | { "participants": [ "user_2099304043_-161f4ff06162290", "user_-366632177_-6312fe96521e27a", "user_-1657346297_ |
| 5 | Bruno | `POST /api/events/{ev}/participants` | Bruno ajoute un participant → 403 | 403 | 403 | ✅ | error=Only the organizer can add participants |
| 6 | Alice | `POST /api/events/{ev}/participants` | doublon participant → 400 | 400 | 400 | ✅ | error=Failed to add the participant. Please try again. |
| 7 | Eve | `GET /api/events/{ev}` | Eve (intruse) lit l'événement → 404 | 404 | 404 | ✅ | error=Event not found |
| 8 | Eve | `GET /api/events/{ev}/participants` | Eve liste participants → 403 | 403 | 403 | ✅ | error=You do not have access to this event |
| 9 | Bruno | `PUT /api/events/{ev}/status` | Bruno ouvre le sondage → 403 | 403 | 403 | ✅ | error=Only the organizer can update event status |
| 10 | Alice | `PUT /api/events/{ev}/status` | DRAFT → POLLING | 200 | 200 | ✅ | status=POLLING |
| 11 | Alice | `POST /api/events/{ev}/poll/votes` | Alice vote YES sur slot-oct-a | 201 | 201 | ✅ | { "eventId": "event_1790544700077_3ff8ce04", "votes": { "user_2099304043_-161f4ff06162290": { "slot-oct-a": "Y |
| 12 | Alice | `POST /api/events/{ev}/poll/votes` | Alice vote YES sur slot-oct-b | 201 | 201 | ✅ | { "eventId": "event_1790544700077_3ff8ce04", "votes": { "user_2099304043_-161f4ff06162290": { "slot-oct-a": "Y |
| 13 | Bruno | `POST /api/events/{ev}/poll/votes` | Bruno vote YES sur slot-oct-a | 201 | 201 | ✅ | { "eventId": "event_1790544700077_3ff8ce04", "votes": { "user_2099304043_-161f4ff06162290": { "slot-oct-a": "Y |
| 14 | Bruno | `POST /api/events/{ev}/poll/votes` | Bruno vote NO sur slot-oct-b | 201 | 201 | ✅ | { "eventId": "event_1790544700077_3ff8ce04", "votes": { "user_2099304043_-161f4ff06162290": { "slot-oct-a": "Y |
| 15 | Chloé | `POST /api/events/{ev}/poll/votes` | Chloé vote MAYBE sur slot-oct-a | 201 | 201 | ✅ | { "eventId": "event_1790544700077_3ff8ce04", "votes": { "user_2099304043_-161f4ff06162290": { "slot-oct-a": "Y |
| 16 | Chloé | `POST /api/events/{ev}/poll/votes` | Chloé vote YES sur slot-oct-b | 201 | 201 | ✅ | { "eventId": "event_1790544700077_3ff8ce04", "votes": { "user_2099304043_-161f4ff06162290": { "slot-oct-a": "Y |
| 17 | David | `POST /api/events/{ev}/poll/votes` | David vote YES sur slot-oct-a | 201 | 201 | ✅ | { "eventId": "event_1790544700077_3ff8ce04", "votes": { "user_2099304043_-161f4ff06162290": { "slot-oct-a": "Y |
| 18 | David | `POST /api/events/{ev}/poll/votes` | David vote MAYBE sur slot-oct-b | 201 | 201 | ✅ | { "eventId": "event_1790544700077_3ff8ce04", "votes": { "user_2099304043_-161f4ff06162290": { "slot-oct-a": "Y |
| 19 | Eve | `POST /api/events/{ev}/poll/votes` | Eve vote → 403 | 403 | 403 | ✅ | error=You do not have access to vote on this event |
| 20 | Bruno | `POST /api/events/{ev}/poll/votes` | Bruno vote à la place de David → 403 | 403 | 403 | ✅ | error=You do not have access to vote on this event |
| 21 | Bruno | `POST /api/events/{ev}/poll/votes` | vote invalide → 400 | 400 | 400 | ✅ | error=Invalid vote: PEUT-ETRE. Must be YES, MAYBE, or NO |
| 22 | Bruno | `POST /api/events/{ev}/poll/votes` | slot inconnu → 400 | 400 | 400 | ✅ | error=Failed to save your vote. Please try again. |
| 23 | Bruno | `POST /api/events/{ev}/poll/votes` | payload malformé → 400 | 400 | 400 | ✅ | error=Failed to save your vote. Please try again. |
| 24 | Alice | `POST /api/events/{ev}/poll/votes` | Alice vote pour Eve (non membre) → 400 | 400 | 400 | ✅ | error=Failed to save your vote. Please try again. |
| 25 | Chloé | `GET /api/events/{ev}/poll` | Chloé lit le sondage | 200 | 200 | ✅ | {"162290": {"slot-oct-a": "YES", "slot-oct-b": "YES"}, "21e27a": {"slot-oct-a": "YES", "slot-oct-b": "NO"}, "07287c": {" |
| 26 | Eve | `GET /api/events/{ev}/poll` | Eve lit le sondage → 403 | 403 | 403 | ✅ | error=You do not have access to this poll |
| 27 | — | `(assertion)` | scores attendus A=7 (2+2+1+2), B=4 (2-1+2+1) | ✔ | ✔ | ✅ | {'slot-oct-a': 7, 'slot-oct-b': 4} |
| 28 | — | `(assertion)` | 4 votants × 2 créneaux dans le poll | ✔ | ✔ | ✅ | 4 votants |
| 29 | Alice | `PUT /api/events/{ev}/status` | PUT CONFIRMED (voie legacy) → 409 attendu | 409 | 409 | ✅ | error=Poll date confirmation must use the modeled confirmation command |
| 30 | Alice | `PUT /api/events/{ev}/status` | saut POLLING → ORGANIZING → 409 | 409 | 409 | ✅ | error=Invalid workflow transition: POLLING -> ORGANIZING |
| 31 | Bruno | `POST /api/sync` | Bruno confirme via sync (non org.) → rejet | 200/409 | 409 | ✅ | message=1 conflicts detected |
| 32 | — | `(assertion)` | statut toujours POLLING après tentative Bruno | ✔ | ✔ | ✅ | POLLING |
| 33 | Alice | `POST /api/sync` | Alice confirme créneau A via /api/sync | 200 | 200 | ✅ | applied=1 conflicts=0 |
| 34 | — | `(assertion)` | statut = CONFIRMED | ✔ | ✔ | ✅ | CONFIRMED |
| 35 | Chloé | `POST /api/events/{ev}/poll/votes` | vote après confirmation → 400 | 400 | 400 | ✅ | error=Failed to save your vote. Please try again. |
| 36 | Alice | `GET /api/events/{ev}` | finalDate après confirmation | 200 | 200 | ✅ | status=CONFIRMED finalDate=2026-10-10T07:00:00Z |
| 37 | — | `(assertion)` | finalDate renseignée = début créneau A | ✔ | ✔ | ✅ | finalDate=2026-10-10T07:00:00Z |
| 38 | Alice | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP Alice CONFIRMED | 200 | 200 | ✅ | ACCEPTED/VALIDATED_RETAINED_DATE |
| 39 | Bruno | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP Bruno CONFIRMED | 200 | 200 | ✅ | ACCEPTED/VALIDATED_RETAINED_DATE |
| 40 | Chloé | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP Chloé CONFIRMED | 200 | 200 | ✅ | ACCEPTED/VALIDATED_RETAINED_DATE |
| 41 | David | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP David CONFIRMED | 200 | 200 | ✅ | ACCEPTED/VALIDATED_RETAINED_DATE |
| 42 | Bruno | `POST /api/events/{ev}/participants/{user}/rsvp` | Bruno RSVP pour David → 403 | 403 | 403 | ✅ | error=You cannot update this participant RSVP |
| 43 | Chloé | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP mauvais créneau → 400 | 400 | 400 | ✅ | error=RSVP slot must match the retained date |
| 44 | Chloé | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP attendance invalide → 400 | 400 | 400 | ✅ | error=attendance must be CONFIRMED, DECLINED, or TENTATIVE |
| 45 | Alice | `POST /api/events/{ev}/scenarios` | scénario Airbnb Alfama | 201 | 201 | ✅ | scenario_17905447011 |
| 46 | Alice | `POST /api/events/{ev}/scenarios` | scénario Airbnb Bairro Alto | 201 | 201 | ✅ | scenario_17905447011 |
| 47 | Bruno | `POST /api/events/{ev}/scenarios` | Bruno crée un scénario → 403 | 403 | 403 | ✅ | error=Only the organizer can create scenarios |
| 48 | Alice | `POST /api/events/{ev}/scenarios` | scénario payload incomplet → 400 | 400 | 400 | ✅ | error=Failed to create the scenario. Please try again. |
| 49 | — | `(assertion)` | statut passe auto à COMPARING à la création des scénarios | ✔ | ✔ | ✅ | COMPARING |
| 50 | Alice | `POST /api/scenarios/{sc}/vote` | Alice PREFER Alfama | 201 | 201 | ✅ | { "id": "vote_1790544701229_0.004657035611193794", "scenarioId": "scenario_1790544701139_0.42873170775429614", |
| 51 | Alice | `POST /api/scenarios/{sc}/vote` | Alice NEUTRAL Bairro Alto | 201 | 201 | ✅ | { "id": "vote_1790544701254_0.3768239030413667", "scenarioId": "scenario_1790544701158_0.6847557820137313", "p |
| 52 | Bruno | `POST /api/scenarios/{sc}/vote` | Bruno PREFER Alfama | 201 | 201 | ✅ | { "id": "vote_1790544701277_0.4331042880054393", "scenarioId": "scenario_1790544701139_0.42873170775429614", " |
| 53 | Bruno | `POST /api/scenarios/{sc}/vote` | Bruno AGAINST Bairro Alto | 201 | 201 | ✅ | { "id": "vote_1790544701304_0.9872469765079881", "scenarioId": "scenario_1790544701158_0.6847557820137313", "p |
| 54 | Chloé | `POST /api/scenarios/{sc}/vote` | Chloé NEUTRAL Alfama | 201 | 201 | ✅ | { "id": "vote_1790544701327_0.38466138770912994", "scenarioId": "scenario_1790544701139_0.42873170775429614", |
| 55 | Chloé | `POST /api/scenarios/{sc}/vote` | Chloé PREFER Bairro Alto | 201 | 201 | ✅ | { "id": "vote_1790544701347_0.2976076196988431", "scenarioId": "scenario_1790544701158_0.6847557820137313", "p |
| 56 | David | `POST /api/scenarios/{sc}/vote` | David PREFER Alfama | 201 | 201 | ✅ | { "id": "vote_1790544701368_0.02845463904267531", "scenarioId": "scenario_1790544701139_0.42873170775429614", |
| 57 | David | `POST /api/scenarios/{sc}/vote` | David NEUTRAL Bairro Alto | 201 | 201 | ✅ | { "id": "vote_1790544701389_0.24425606603800787", "scenarioId": "scenario_1790544701158_0.6847557820137313", " |
| 58 | Chloé | `POST /api/scenarios/{sc}/vote` | Chloé change NEUTRAL→PREFER Alfama | 201 | 201 | ✅ | { "id": "vote_1790544701411_0.19488721764781536", "scenarioId": "scenario_1790544701139_0.42873170775429614", |
| 59 | Eve | `POST /api/scenarios/{sc}/vote` | Eve vote scénario → 403 | 403 | 403 | ✅ | error=You do not have access to vote on this scenario |
| 60 | Bruno | `POST /api/scenarios/{sc}/vote` | vote scénario invalide → 400 | 400 | 400 | ✅ | error=Invalid vote type: LOVE. Must be PREFER, NEUTRAL, or AGAINST |
| 61 | Bruno | `GET /api/scenarios/{sc}/votes` | résultats Alfama | 200 | 200 | ✅ | P4/N0/A0 score=8 |
| 62 | Bruno | `GET /api/scenarios/{sc}/votes` | résultats Bairro Alto | 200 | 200 | ✅ | P1/N2/A1 score=3 |
| 63 | — | `(assertion)` | Alfama: 4 PREFER (vote de Chloé mis à jour, pas dupliqué) | ✔ | ✔ | ✅ | 4P total=4 |
| 64 | — | `(assertion)` | Alfama score > Bairro Alto score | ✔ | ✔ | ✅ | 8 vs 3 |
| 65 | Eve | `GET /api/events/{ev}/scenarios` | Eve liste scénarios → 403 | 403 | 403 | ✅ | error=You do not have access to scenario details |
| 66 | Bruno | `POST /api/events/{ev}/scenarios/{sc}/select-final` | Bruno select-final → 403 | 403 | 403 | ✅ | error=Only the organizer can select final scenario |
| 67 | Alice | `POST /api/events/{ev}/scenarios/{sc}/select-final` | Alice sélectionne Alfama | 200 | 200 | ✅ | status=selected |
| 68 | Chloé | `GET /api/events/{ev}/scenarios` | statuts scénarios | 200 | 200 | ✅ | REJECTED,SELECTED |
| 69 | — | `(assertion)` | Alfama SELECTED, Bairro Alto REJECTED | ✔ | ✔ | ✅ |  |
| 70 | Alice | `PUT /api/events/{ev}/status` | COMPARING → ORGANIZING | 200 | 200 | ✅ | status=ORGANIZING |
| 71 | — | `(assertion)` | statut = ORGANIZING | ✔ | ✔ | ✅ | ORGANIZING |
| 72 | Alice | `POST /api/events/{ev}/accommodation` | hébergement Airbnb CONFIRMED | 201 | 201 | ✅ | totalCost=112000 status=CONFIRMED |
| 73 | — | `(assertion)` | totalCost = 7 × 16000 = 112000 cts | ✔ | ✔ | ✅ | 112000 |
| 74 | Bruno | `POST /api/events/{ev}/accommodation` | Bruno crée hébergement → 403 | 403 | 403 | ✅ | error=Only the organizer can create accommodations |
| 75 | Alice | `POST /api/events/{ev}/accommodation` | hébergement payload invalide → 400 | 400 | 500 | ❌ | error=Failed to create accommodation. Please try again. |
| 76 | Alice | `POST /api/events/{ev}/accommodation` | hébergement capacité<0 / checkout<checkin → 400 | 400 | 400 | ✅ | error=Invalid accommodation details. Please review the request and try again. |
| 77 | David | `GET /api/events/{ev}/accommodation` | David (confirmé) lit hébergements | 200 | 200 | ✅ | 1 hébergement(s) |
| 78 | Alice | `PUT /api/events/{ev}/budget` | baseline budget 2400 € (1er PUT) | 200 | 200 | ✅ | totalEstimated=2400.0 |
| 79 | — | `(assertion)` | baseline conservée au 1er PUT (régression BUG-6) | ✔ | ✔ | ✅ | 2400.0 |
| 80 | Bruno | `PUT /api/events/{ev}/budget` | Bruno modifie la baseline → 403 | 403 | 403 | ✅ | error=You do not have access to this event |
| 81 | Alice | `PUT /api/events/{ev}/budget` | baseline incomplète → 400 (régression BUG-7) | 400 | 400 | ✅ | error=Invalid budget payload: required fields are missing or malformed |
| 82 | Alice | `POST /api/events/{ev}/budget/items` | Alice ajoute « Essence + péages Paris→Lisbonne » | 201 | 201 | ✅ | id=budget-17905 cost=520.0 |
| 83 | Bruno | `POST /api/events/{ev}/budget/items` | Bruno ajoute « Airbnb Alfama 7 nuits » | 201 | 201 | ✅ | id=budget-17905 cost=1120.0 |
| 84 | Chloé | `POST /api/events/{ev}/budget/items` | Chloé ajoute « Courses & pastéis » | 201 | 201 | ✅ | id=budget-17905 cost=300.0 |
| 85 | David | `POST /api/events/{ev}/budget/items` | David ajoute « Cours de surf Ericeira » | 201 | 201 | ✅ | id=budget-17905 cost=160.0 |
| 86 | Eve | `POST /api/events/{ev}/budget/items` | Eve ajoute item → 403/404 | 403/404 | 403 | ✅ | error=You do not have access to this event |
| 87 | Alice | `POST /api/events/{ev}/budget/items` | item coût négatif → 400 | 400 | 400 | ✅ | error=Estimated cost must be a finite value greater than 0 |
| 88 | Alice | `POST /api/events/{ev}/budget/items` | item catégorie invalide → 400 | 400 | 400 | ✅ | error=Invalid category: BIJOUX |
| 89 | Chloé | `GET /api/events/{ev}/budget/summary` | résumé budget | 200 | 500 | ❌ | error=Failed to fetch the budget summary. Please try again. |
| 90 | Chloé | `GET /api/events/{ev}/budget/statistics` | statistiques budget | 200 | 500 | ❌ | error=Failed to fetch budget statistics. Please try again. |
| 91 | Chloé | `GET /api/events/{ev}/budget/participants/{user}` | part de Chloé | 200 | 200 | ✅ | {"participantId": "user_-1657346297_466e6cb64a07287c", "share": {"participantId": "user_-1657346297_466e6cb64a |
| 92 | Chloé | `GET /api/events/{ev}/budget/settlements` | équilibrage (settlements) | 200 | 200 | ✅ | {"settlements": [], "count": 0} |
| 93 | Bruno | `PUT /api/events/{ev}/transport/departures/{user}` | Bruno départ Paris | 200 | 200 | ✅ | { "eventId": "event_1790544700077_3ff8ce04", "participantId": "user_-366632177_-6312fe96521e27a", "location": |
| 94 | Chloé | `PUT /api/events/{ev}/transport/departures/{user}` | Chloé départ Lyon | 200 | 200 | ✅ | { "eventId": "event_1790544700077_3ff8ce04", "participantId": "user_-1657346297_466e6cb64a07287c", "location": |
| 95 | Bruno | `PUT /api/events/{ev}/transport/departures/{user}` | Bruno modifie départ de Chloé → 403 | 403 | 403 | ✅ | error=You can only update your own confirmed departure |
| 96 | Alice | `PUT /api/events/{ev}/transport/departures/{user}` | départ payload invalide → 400 | 400 | 400 | ✅ |  |
| 97 | Alice | `POST /api/events/{ev}/transport/plans/generate` | génération plan transport (road trip) | 201/409 | 409 | ✅ | { "eventId": "event_1790544700077_3ff8ce04", "destination": { "name": "Lisbonne, Portugal", "address": "Lisbon |
| 98 | Bruno | `POST /api/events/{ev}/transport/not-needed` | Bruno (confirmé) : transport géré à part (not-needed) | 200 | 200 | ✅ | { "transportNotNeeded": true } |
| 99 | Alice | `POST /api/events/{ev}/meetings/persisted` | réunion de préparation (Meet) | 201 | 201 | ✅ | SCHEDULED https://meet.google.com/33950f151 |
| 100 | Bruno | `POST /api/events/{ev}/meetings/persisted` | Bruno crée réunion → 403 | 403 | 403 | ✅ | error=Only the event organizer can create meetings |
| 101 | Alice | `POST /api/events/{ev}/meetings/persisted` | réunion startTime invalide → 400 | 400 | 500 | ❌ |  |
| 102 | David | `GET /api/events/{ev}/meetings/persisted` | David liste les réunions | 200 | 200 | ✅ | 2 réunion(s) |
| 103 | Alice | `POST /api/events/{ev}/calendar/reminders/one_day_before` | rappel J-1 via calendrier | 200/201/202 | 501 | ❌ | Meeting reminders are not yet implemented. Scheduled for Phase 3. |
| 104 | Alice | `POST /api/events/{ev}/payment/tricount/link` | lien Tricount | 201 | 201 | ✅ | { "eventId": "event_1790544700077_3ff8ce04", "provider": "TRICOUNT", "providerId": "lisbonne2026", "providerUr |
| 105 | Alice | `POST /api/events/{ev}/payment/tricount/link` | URL Tricount piégée → 422 | 422 | 422 | ✅ | error=suspicious_provider_url |
| 106 | Bruno | `POST /api/events/{ev}/comments` | commentaire Bruno | 201/202 | 201 | ✅ | author=Bruno |
| 107 | Chloé | `POST /api/events/{ev}/comments` | commentaire Chloé | 201/202 | 201 | ✅ | author=Chloé |
| 108 | Eve | `POST /api/events/{ev}/comments` | Eve commente → 403 | 403 | 201 | ❌ | { "id": "b4a20832-11c1-43e4-835b-f4df78aa90e5", "eventId": "event_1790544700077_3ff8ce04", "section": "GENERAL |
| 109 | Eve | `GET /api/events/{ev}/comments` | Eve lit les commentaires → 403 | 403 | 200 | ❌ | 3 commentaire(s) visibles |
| 110 | Alice | `GET /api/events/{ev}/readiness` | checklist finalisation | 200 | 200 | ✅ | complete=True blockers=[] |
| 111 | Bruno | `GET /api/events/{ev}/readiness` | Bruno lit readiness → 403 | 403 | 403 | ✅ | error=Only the event organizer can view finalization readiness |
| 112 | Bruno | `PUT /api/events/{ev}/status` | Bruno finalise → 403 | 403 | 403 | ✅ | error=Only the organizer can update event status |
| 113 | Alice | `PUT /api/events/{ev}/status` | ORGANIZING → FINALIZED | 200 | 200 | ✅ | status=FINALIZED |
| 114 | — | `(assertion)` | statut = FINALIZED | ✔ | ✔ | ✅ | FINALIZED |
| 115 | Alice | `PUT /api/events/{ev}/status` | réouverture après FINALIZED → refus | 403/409 | 409 | ✅ | error=Invalid workflow transition: FINALIZED -> POLLING |
| 116 | Alice | `POST /api/events/{ev}/budget/items` | item budget après FINALIZED → refus | 403/409 | 409 | ✅ | error=Budget items can only be mutated while event is ORGANIZING |
| 117 | Alice | `POST /api/events/{ev}/meetings/persisted` | réunion après FINALIZED → refus (read-only) | 403/409 | 201 | ❌ | status=SCHEDULED |
| 118 | David | `GET /api/events/{ev}/calendar/ics` | ICS (David) | 200 | 200 | ✅ | BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Wakeve//Wakeve Event// |
| 119 | — | `(assertion)` | ICS: DTSTART = 20261010T070000Z | ✔ | ✔ | ✅ | ['DTSTART:20261010T070000Z'] |
| 120 | — | `(assertion)` | ICS: TRIGGER VALARM conforme RFC5545 | ✔ | ✔ | ✅ | ['TRIGGER:-P1D', 'TRIGGER:-P1W'] |
| 121 | — | `(assertion)` | ICS: SUMMARY contient le titre | ✔ | ✔ | ✅ |  |
| 122 | Eve | `GET /api/events/{ev}/calendar/ics` | Eve télécharge l'ICS → 403 | 403 | 403 | ✅ | error=You do not have access to this event calendar |
| 123 | Alice | `POST /api/events/{ev}/calendar/ics` | ICS avec invités (POST) | 200 | 200 | ✅ | Road_trip_Lisbonne__invitation.ics |
| 124 | — | `(assertion)` | ICS POST: invités e-mail présents en ATTENDEE | ✔ | ✔ | ✅ | ['ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;CN=bruno:mailto:bruno@example.com', 'ATTENDEE;ROLE=REQ-P |

### Scénario 2 — Watch party finale Ligue des champions (Hugo org., Inès, Jules, Léa + Marc en retard, Nora intruse) — 67 ✅ / 6 ❌

| # | Acteur | Endpoint | Étape | Attendu | Obtenu | | Gist |
|---|---|---|---|---|---|---|---|
| 1 | Hugo | `POST /api/events` | création événement | 201 | 201 | ✅ | id=event_1790544815396_c434c69c status=DRAFT |
| 2 | Hugo | `POST /api/events/{ev}/participants` | ajout Inès | 201 | 201 | ✅ | { "participants": [ "user_-512075028_-573212ad4bb32229", "user_-741670073_1a0919618fb99e7f" ] } |
| 3 | Hugo | `POST /api/events/{ev}/participants` | ajout Jules | 201 | 201 | ✅ | { "participants": [ "user_-512075028_-573212ad4bb32229", "user_-741670073_1a0919618fb99e7f", "user_-1422794560 |
| 4 | Hugo | `POST /api/events/{ev}/participants` | ajout Léa | 201 | 201 | ✅ | { "participants": [ "user_-512075028_-573212ad4bb32229", "user_-741670073_1a0919618fb99e7f", "user_-1422794560 |
| 5 | Hugo | `POST /api/events/{ev}/invite` | lien d'invitation maxUses=2 | 201 | 201 | ✅ | code=9ZCCWUKN https://wakeve.app/invite/9ZCCWUKN |
| 6 | Inès | `POST /api/events/{ev}/invite` | Inès crée un lien → 403 | 403 | 403 | ✅ | error=Only the event organizer can create invitation links |
| 7 | Hugo | `POST /api/events/{ev}/invite` | maxUses=0 → 400 | 400 | 400 | ✅ | error=maxUses must be between 1 and 1000 |
| 8 | Hugo | `PUT /api/events/{ev}/status` | DRAFT → POLLING | 200 | 200 | ✅ | status=POLLING |
| 9 | Hugo | `POST /api/events/{ev}/participants` | ajout direct après DRAFT → refus (invitation requise) | 400 | 400 | ✅ | error=Failed to add the participant. Please try again. |
| 10 | Hugo | `POST /api/events/{ev}/poll/votes` | Hugo YES samedi | 201 | 201 | ✅ | { "eventId": "event_1790544815396_c434c69c", "votes": { "user_-512075028_-573212ad4bb32229": { "sam-soir": "YE |
| 11 | Hugo | `POST /api/events/{ev}/poll/votes` | Hugo MAYBE dimanche | 201 | 201 | ✅ | { "eventId": "event_1790544815396_c434c69c", "votes": { "user_-512075028_-573212ad4bb32229": { "sam-soir": "YE |
| 12 | Inès | `POST /api/events/{ev}/poll/votes` | Inès YES samedi | 201 | 201 | ✅ | { "eventId": "event_1790544815396_c434c69c", "votes": { "user_-512075028_-573212ad4bb32229": { "sam-soir": "YE |
| 13 | Inès | `POST /api/events/{ev}/poll/votes` | Inès NO dimanche | 201 | 201 | ✅ | { "eventId": "event_1790544815396_c434c69c", "votes": { "user_-512075028_-573212ad4bb32229": { "sam-soir": "YE |
| 14 | Jules | `POST /api/events/{ev}/poll/votes` | Jules NO samedi | 201 | 201 | ✅ | { "eventId": "event_1790544815396_c434c69c", "votes": { "user_-512075028_-573212ad4bb32229": { "sam-soir": "YE |
| 15 | Jules | `POST /api/events/{ev}/poll/votes` | Jules YES dimanche | 201 | 201 | ✅ | { "eventId": "event_1790544815396_c434c69c", "votes": { "user_-512075028_-573212ad4bb32229": { "sam-soir": "YE |
| 16 | Léa | `POST /api/events/{ev}/poll/votes` | Léa MAYBE samedi | 201 | 201 | ✅ | { "eventId": "event_1790544815396_c434c69c", "votes": { "user_-512075028_-573212ad4bb32229": { "sam-soir": "YE |
| 17 | Léa | `POST /api/events/{ev}/poll/votes` | Léa YES dimanche | 201 | 201 | ✅ | { "eventId": "event_1790544815396_c434c69c", "votes": { "user_-512075028_-573212ad4bb32229": { "sam-soir": "YE |
| 18 | Jules | `POST /api/events/{ev}/poll/votes` | Jules change d'avis NO → YES samedi | 201 | 201 | ✅ | { "eventId": "event_1790544815396_c434c69c", "votes": { "user_-512075028_-573212ad4bb32229": { "sam-soir": "YE |
| 19 | Jules | `GET /api/events/{ev}/poll` | poll après changement | 200 | 200 | ✅ | Jules={'sam-soir': 'YES', 'dim-soir': 'YES'} |
| 20 | — | `(assertion)` | vote de Jules mis à jour (pas de doublon) | ✔ | ✔ | ✅ | {'sam-soir': 'YES', 'dim-soir': 'YES'} |
| 21 | anon | `GET /api/invite/9ZCCWUKN` | résolution publique du lien (sans auth) | 200 | 200 | ✅ | Watch party – finale Ligue des status=POLLING n=4 |
| 22 | Marc | `POST /api/invite/9ZCCWUKN/accept` | Marc rejoint en retard via le lien | 200 | 200 | ✅ | Vous avez rejoint l'événement « Watch party – finale Ligue des champions ⚽ » |
| 23 | Marc | `POST /api/invite/9ZCCWUKN/accept` | Marc ré-accepte (idempotent) | 200 | 200 | ✅ | Vous participez déjà à cet événement |
| 24 | Marc | `GET /api/events/{ev}` | Marc voit l'événement | 200 | 200 | ✅ | participants=5 |
| 25 | Marc | `POST /api/events/{ev}/poll/votes` | Marc YES samedi | 201 | 201 | ✅ | { "eventId": "event_1790544815396_c434c69c", "votes": { "user_-512075028_-573212ad4bb32229": { "sam-soir": "YE |
| 26 | Marc | `POST /api/events/{ev}/poll/votes` | Marc NO dimanche | 201 | 201 | ✅ | { "eventId": "event_1790544815396_c434c69c", "votes": { "user_-512075028_-573212ad4bb32229": { "sam-soir": "YE |
| 27 | anon | `POST /api/invite/9ZCCWUKN/accept` | accept sans token → 401 | 401 | 401 | ✅ |  |
| 28 | Nora | `POST /api/invite/ZZZZZZZZ/accept` | code inconnu → 404 | 404 | 404 | ✅ | error=Invitation not found or invalid |
| 29 | Hugo | `GET /api/events/{ev}/poll` | poll final | 200 | 200 | ✅ | { "eventId": "event_1790544815396_c434c69c", "votes": { "user_-512075028_-573212ad4bb32229": { "sam-soir": "YE |
| 30 | — | `(assertion)` | scores samedi=9 (2+2+2+1+2), dimanche=3 (1-1+2+2-1) | ✔ | ✔ | ✅ | {'sam-soir': 9, 'dim-soir': 3} |
| 31 | Léa | `POST /api/events/{ev}/poll/votes` | Léa vote après la deadline → 400 | 400 | 400 | ✅ | error=Failed to save your vote. Please try again. |
| 32 | Hugo | `POST /api/sync` | Hugo confirme samedi soir (sync) | 200 | 200 | ✅ | applied=1 conflicts=0 |
| 33 | — | `(assertion)` | statut = CONFIRMED | ✔ | ✔ | ✅ | CONFIRMED |
| 34 | Hugo | `POST /api/sync` | re-confirmation d'un autre créneau → conflit | 409 | 409 | ✅ | message=1 conflicts detected |
| 35 | Hugo | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP Hugo CONFIRMED | 200 | 200 | ✅ | ACCEPTED |
| 36 | Inès | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP Inès CONFIRMED | 200 | 200 | ✅ | ACCEPTED |
| 37 | Jules | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP Jules CONFIRMED | 200 | 200 | ✅ | ACCEPTED |
| 38 | Marc | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP Marc CONFIRMED | 200 | 200 | ✅ | ACCEPTED |
| 39 | Léa | `POST /api/events/{ev}/participants/{user}/rsvp` | Léa décline | 200 | 200 | ✅ | DECLINED/NOT_VALIDATED |
| 40 | Hugo | `GET /api/events/{ev}/participants` | liste participants | 200 | 200 | ✅ | 5 participants |
| 41 | — | `(assertion)` | 5 participants (Hugo+Inès+Jules+Léa+Marc) | ✔ | ✔ | ✅ | 5 |
| 42 | Hugo | `POST /api/events/{ev}/scenarios` | lieu unique (scénario) | 201 | 201 | ✅ | status=PROPOSED |
| 43 | Léa | `GET /api/events/{ev}/scenarios` | Léa (déclinée) lit les scénarios → 403 | 403 | 403 | ✅ | error=You do not have access to scenario details |
| 44 | Hugo | `POST /api/events/{ev}/scenarios/{sc}/select-final` | sélection du lieu | 200 | 200 | ✅ | status=selected |
| 45 | Hugo | `PUT /api/events/{ev}/status` | COMPARING → ORGANIZING | 200 | 200 | ✅ | status=ORGANIZING |
| 46 | Hugo | `POST /api/events/{ev}/meals` | Snack assigné à Jules | 201 | 201 | ✅ | Chips & guacamole resp=1 |
| 47 | Hugo | `POST /api/events/{ev}/meals` | Apéro assigné à Marc | 201 | 201 | ✅ | status=PLANNED |
| 48 | Jules | `POST /api/events/{ev}/meals` | Jules propose d'apporter des pizzas (participant) | 201/403 | 403 | ✅ | error=You do not have access to this event meal plan |
| 49 | Hugo | `POST /api/events/{ev}/meals` | heure invalide → 400 | 400 | 400 | ✅ | error=Invalid time format (use HH:MM) |
| 50 | Hugo | `POST /api/events/{ev}/meals` | payload repas invalide → 400 | 400 | 500 | ❌ | error=Failed to create the meal. Please try again. |
| 51 | Marc | `GET /api/events/{ev}/meals` | Marc voit qui apporte quoi | 200 | 200 | ✅ | Chips & guacamole, Bières & softs |
| 52 | Nora | `GET /api/events/{ev}/meals` | Nora (intruse) lit les repas → 403 | 403 | 403 | ✅ | error=You do not have access to this event meal plan |
| 53 | Inès | `POST /api/events/{ev}/dietary-restrictions` | Inès végétarienne | 201 | 201 | ✅ | { "id": "cb70a338-7c16-496b-970a-67ed69b585e4", "participantId": "user_-741670073_1a0919618fb99e7f", "eventId" |
| 54 | Inès | `POST /api/events/{ev}/comments` | commentaire Inès | 201 | 201 | ✅ | author=Inès |
| 55 | Marc | `POST /api/events/{ev}/comments` | commentaire Marc | 201 | 201 | ✅ | author=Marc |
| 56 | Inès | `POST /api/events/{ev}/comments` | Inès poste au nom de Marc → 403 | 403 | 403 | ✅ | error=You are not allowed to create this comment. |
| 57 | Inès | `POST /api/events/{ev}/comments` | commentaire vide → 400 | 400 | 500 | ❌ | error=Failed to create the comment. Please try again. |
| 58 | Nora | `POST /api/events/{ev}/comments` | Nora (intruse) commente → 403 | 403 | 201 | ❌ | { "id": "287439cb-2bb0-465b-901c-2c115df97310", "eventId": "event_1790544815396_c434c69c", "section": "GENERAL |
| 59 | Nora | `GET /api/events/{ev}/comments` | Nora (intruse) lit les commentaires → 403 | 403 | 200 | ❌ | 3 commentaires lus |
| 60 | Nora | `POST /api/events/event_inexistant_123/comments` | commentaire sur événement inexistant → 404 | 403/404 | 500 | ❌ | error=Failed to create the comment. Please try again. |
| 61 | Hugo | `POST /api/events/{ev}/readiness/LODGING/not-needed` | logement non nécessaire | 200 | 200 | ✅ | { "section": "LODGING", "notNeeded": true } |
| 62 | Hugo | `POST /api/events/{ev}/readiness/MEETINGS/not-needed` | réunion non nécessaire | 200 | 200 | ✅ | { "section": "MEETINGS", "notNeeded": true } |
| 63 | Inès | `POST /api/events/{ev}/readiness/MEETINGS/not-needed` | Inès marque not-needed → 403 | 403 | 403 | ✅ | error=Only the event organizer can mark a readiness section as not needed |
| 64 | Hugo | `POST /api/events/{ev}/transport/not-needed` | transport non nécessaire | 200 | 200 | ✅ | { "transportNotNeeded": true } |
| 65 | Hugo | `GET /api/events/{ev}/readiness` | readiness avant budget/paiement | 200 | 200 | ✅ | blockers=['BUDGET_REQUIRED', 'PAYMENT_POT_REQUIRED', 'TRICOUNT_HANDOFF_REQUIRED'] |
| 66 | Hugo | `PUT /api/events/{ev}/status` | finalisation sans budget/Tricount → 409 + blockers | 409 | 409 | ✅ | error=Event cannot be finalized yet |
| 67 | Hugo | `PUT /api/events/{ev}/budget` | budget apéro 60 € | 200 | 200 | ✅ | { "id": "budget-1790544892704-2569", "eventId": "event_1790544815396_c434c69c", "totalEstimated": 60.0, "total |
| 68 | Hugo | `POST /api/events/{ev}/payment/tricount/link` | Tricount | 201 | 201 | ✅ | { "eventId": "event_1790544815396_c434c69c", "provider": "TRICOUNT", "providerId": "ldc-final", "providerUrl": |
| 69 | Hugo | `GET /api/events/{ev}/readiness` | readiness | 200 | 200 | ✅ | complete=True blockers=[] |
| 70 | Hugo | `PUT /api/events/{ev}/status` | ORGANIZING → FINALIZED | 200 | 200 | ✅ | status=FINALIZED |
| 71 | Marc | `POST /api/invite/9ZCCWUKN/accept` | Marc rouvre l'ancien lien après finalisation (idempotent) | 200 | 200 | ✅ | message=Vous participez déjà à cet événement |
| 72 | Nora | `POST /api/invite/9ZCCWUKN/accept` | Nora rejoint un événement FINALIZED via le lien (1/2 utilisé) → refus attendu | 410/409/403 | 200 | ❌ | message=Vous avez rejoint l'événement « Watch party – finale Ligue des champions ⚽ » |
| 73 | Nora | `GET /api/invite/9ZCCWUKN` | résolution du lien après 2/2 utilisations → 410 | 410 | 410 | ✅ | error=Invitation expired or no longer available |

### Scénario 3 — Mariage de Sophie & Karim (Sophie org., Karim « co-org. », Nadia, Olivier, Pauline + Quentin hors quota, Raphaël intrus) — 110 ✅ / 10 ❌

| # | Acteur | Endpoint | Étape | Attendu | Obtenu | | Gist |
|---|---|---|---|---|---|---|---|
| 1 | Sophie | `POST /api/events` | création titre/description vides → 400 | 400 | 400 | ✅ | error=This content cannot be posted. Please revise it and try again. |
| 2 | Sophie | `POST /api/events` | création max < min participants → 400 | 400 | 201 | ❌ | CRÉÉ id=event_1790545309877_58b59f8d |
| 3 | Sophie | `POST /api/events` | eventType inconnu → 400 | 400 | 400 | ✅ | error=Failed to create the event. Please try again. |
| 4 | Sophie | `POST /api/events` | deadline non ISO → 400 | 400 | 201 | ❌ | CRÉÉ id=event_1790545309900_149a1183 deadline=demain |
| 5 | Sophie | `POST /api/events` | créneau fin < début → 400 | 400 | 201 | ❌ | CRÉÉ id=event_1790545309914_c8215d25 |
| 6 | Sophie | `POST /api/events` | création sans créneau | 201/400 | 201 | ✅ | id=event_1790545309930_905349f0 |
| 7 | Sophie | `PUT /api/events/{ev}/status` | DRAFT → POLLING sans aucun créneau → refus attendu | 400/409 | 200 | ❌ | status=POLLING |
| 8 | Raphaël | `POST /api/events` | organizerId du body ignoré (Raphaël devient org.) | 201 | 201 | ✅ | organizerId = JWT ✔ |
| 9 | Sophie | `POST /api/events` | création événement | 201 | 201 | ✅ | id=event_1790545310000_58dad50e status=DRAFT |
| 10 | Sophie | `POST /api/events/{ev}/participants` | Sophie ajoute Karim | 201 | 201 | ✅ | { "participants": [ "user_-1174926092_47f05176423428bd", "user_-591933547_5146df0ed4f47ab" ] } |
| 11 | Sophie | `POST /api/events/{ev}/invite` | lien invités maxUses=3 | 201 | 201 | ✅ | code=RBP4SLZB maxUses=3 |
| 12 | Karim | `POST /api/events/{ev}/invite` | Karim (co-org.) crée un lien → 403 (pas de rôle co-organisateur) | 403 | 403 | ✅ | error=Only the event organizer can create invitation links |
| 13 | Nadia | `POST /api/invite/RBP4SLZB/accept` | Nadia accepte l'invitation | 200 | 200 | ✅ | Vous avez rejoint l'événement « Mariage de Sophie |
| 14 | Olivier | `POST /api/invite/RBP4SLZB/accept` | Olivier accepte l'invitation | 200 | 200 | ✅ | Vous avez rejoint l'événement « Mariage de Sophie |
| 15 | Pauline | `POST /api/invite/RBP4SLZB/accept` | Pauline accepte l'invitation | 200 | 200 | ✅ | Vous avez rejoint l'événement « Mariage de Sophie |
| 16 | Quentin | `POST /api/invite/RBP4SLZB/accept` | Quentin : quota maxUses atteint → 410 | 410 | 410 | ✅ | error=This invitation has expired or reached its maximum uses |
| 17 | anon | `GET /api/invite/RBP4SLZB` | résolution lien épuisé → 410 | 410 | 410 | ✅ | error=Invitation expired or no longer available |
| 18 | Quentin | `GET /api/events/{ev}` | Quentin ne voit pas l'événement | 404 | 404 | ✅ | error=Event not found |
| 19 | Karim | `PUT /api/events/{ev}/status` | Karim ouvre le sondage → 403 | 403 | 403 | ✅ | error=Only the organizer can update event status |
| 20 | Sophie | `PUT /api/events/{ev}/status` | DRAFT → POLLING | 200 | 200 | ✅ | status=POLLING |
| 21 | Sophie | `POST /api/events/{ev}/poll/votes` | Sophie YES mai | 201 | 201 | ✅ | { "eventId": "event_1790545310000_58dad50e", "votes": { "user_-1174926092_47f05176423428bd": { "mai": "YES" } |
| 22 | Sophie | `POST /api/events/{ev}/poll/votes` | Sophie YES juin | 201 | 201 | ✅ | { "eventId": "event_1790545310000_58dad50e", "votes": { "user_-1174926092_47f05176423428bd": { "mai": "YES", " |
| 23 | Sophie | `POST /api/events/{ev}/poll/votes` | Sophie MAYBE sept | 201 | 201 | ✅ | { "eventId": "event_1790545310000_58dad50e", "votes": { "user_-1174926092_47f05176423428bd": { "mai": "YES", " |
| 24 | Karim | `POST /api/events/{ev}/poll/votes` | Karim YES mai | 201 | 201 | ✅ | { "eventId": "event_1790545310000_58dad50e", "votes": { "user_-1174926092_47f05176423428bd": { "mai": "YES", " |
| 25 | Karim | `POST /api/events/{ev}/poll/votes` | Karim MAYBE juin | 201 | 201 | ✅ | { "eventId": "event_1790545310000_58dad50e", "votes": { "user_-1174926092_47f05176423428bd": { "mai": "YES", " |
| 26 | Karim | `POST /api/events/{ev}/poll/votes` | Karim NO sept | 201 | 201 | ✅ | { "eventId": "event_1790545310000_58dad50e", "votes": { "user_-1174926092_47f05176423428bd": { "mai": "YES", " |
| 27 | Nadia | `POST /api/events/{ev}/poll/votes` | Nadia NO mai | 201 | 201 | ✅ | { "eventId": "event_1790545310000_58dad50e", "votes": { "user_-1174926092_47f05176423428bd": { "mai": "YES", " |
| 28 | Nadia | `POST /api/events/{ev}/poll/votes` | Nadia YES juin | 201 | 201 | ✅ | { "eventId": "event_1790545310000_58dad50e", "votes": { "user_-1174926092_47f05176423428bd": { "mai": "YES", " |
| 29 | Nadia | `POST /api/events/{ev}/poll/votes` | Nadia YES sept | 201 | 201 | ✅ | { "eventId": "event_1790545310000_58dad50e", "votes": { "user_-1174926092_47f05176423428bd": { "mai": "YES", " |
| 30 | Olivier | `POST /api/events/{ev}/poll/votes` | Olivier MAYBE mai | 201 | 201 | ✅ | { "eventId": "event_1790545310000_58dad50e", "votes": { "user_-1174926092_47f05176423428bd": { "mai": "YES", " |
| 31 | Olivier | `POST /api/events/{ev}/poll/votes` | Olivier YES juin | 201 | 201 | ✅ | { "eventId": "event_1790545310000_58dad50e", "votes": { "user_-1174926092_47f05176423428bd": { "mai": "YES", " |
| 32 | Olivier | `POST /api/events/{ev}/poll/votes` | Olivier NO sept | 201 | 201 | ✅ | { "eventId": "event_1790545310000_58dad50e", "votes": { "user_-1174926092_47f05176423428bd": { "mai": "YES", " |
| 33 | Pauline | `POST /api/events/{ev}/poll/votes` | Pauline YES mai | 201 | 201 | ✅ | { "eventId": "event_1790545310000_58dad50e", "votes": { "user_-1174926092_47f05176423428bd": { "mai": "YES", " |
| 34 | Pauline | `POST /api/events/{ev}/poll/votes` | Pauline NO juin | 201 | 201 | ✅ | { "eventId": "event_1790545310000_58dad50e", "votes": { "user_-1174926092_47f05176423428bd": { "mai": "YES", " |
| 35 | Pauline | `POST /api/events/{ev}/poll/votes` | Pauline MAYBE sept | 201 | 201 | ✅ | { "eventId": "event_1790545310000_58dad50e", "votes": { "user_-1174926092_47f05176423428bd": { "mai": "YES", " |
| 36 | Sophie | `GET /api/events/{ev}/poll` | poll | 200 | 200 | ✅ | { "eventId": "event_1790545310000_58dad50e", "votes": { "user_-1174926092_47f05176423428bd": { "mai": "YES", " |
| 37 | — | `(assertion)` | scores mai=6, juin=6, sept=2 (égalité mai/juin) | ✔ | ✔ | ✅ | {'mai': 6, 'juin': 6, 'sept': 2} |
| 38 | Nadia | `POST /api/sync` | Nadia (invitée) tente de confirmer → rejet | 409 | 409 | ✅ | message=1 conflicts detected |
| 39 | Karim | `POST /api/sync` | Karim (co-org.) tente de confirmer → rejet | 409 | 409 | ✅ | message=1 conflicts detected |
| 40 | Sophie | `POST /api/sync` | confirmation avec finalDate ≠ début créneau → rejet | 409 | 409 | ✅ | message=1 conflicts detected |
| 41 | Sophie | `POST /api/sync` | Sophie tranche l'égalité : juin (0 NO éliminatoire côté mariés) | 200 | 200 | ✅ | applied=1 conflicts=0 |
| 42 | — | `(assertion)` | statut CONFIRMED | ✔ | ✔ | ✅ | CONFIRMED |
| 43 | Sophie | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP Sophie CONFIRMED | 200 | 200 | ✅ | ACCEPTED |
| 44 | Karim | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP Karim CONFIRMED | 200 | 200 | ✅ | ACCEPTED |
| 45 | Nadia | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP Nadia CONFIRMED | 200 | 200 | ✅ | ACCEPTED |
| 46 | Olivier | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP Olivier CONFIRMED | 200 | 200 | ✅ | ACCEPTED |
| 47 | Pauline | `POST /api/events/{ev}/participants/{user}/rsvp` | Pauline peut-être | 200 | 200 | ✅ | PENDING |
| 48 | Pauline | `GET /api/events/{ev}/budget` | Pauline (TENTATIVE) lit le budget → 403 | 403 | 403 | ✅ | error=You do not have access to this event |
| 49 | Pauline | `POST /api/events/{ev}/participants/{user}/rsvp` | Pauline confirme finalement | 200 | 200 | ✅ | ACCEPTED |
| 50 | Sophie | `POST /api/events/{ev}/participants/{user}/rsvp` | Sophie (org.) met à jour le RSVP de Pauline | 200 | 200 | ✅ | { "eventId": "event_1790545310000_58dad50e", "userId": "user_-2110226651_9820f67a5cd7d3d", "slotId": "juin", " |
| 51 | Olivier | `POST /api/events/{ev}/dietary-restrictions` | Olivier sans gluten | 201 | 201 | ✅ | { "id": "9137e739-e22f-4899-ba78-7af1d23b493b", "participantId": "user_1503158802_689bdce19007d7d4", "eventId" |
| 52 | Nadia | `POST /api/events/{ev}/dietary-restrictions` | Nadia halal | 201 | 201 | ✅ | { "id": "3ca6288e-b737-4b8a-bf5e-caa66e1f0db2", "participantId": "user_-1203957139_5a9cb345fae4d440", "eventId |
| 53 | Olivier | `POST /api/events/{ev}/dietary-restrictions` | Olivier déclare pour Nadia → 403 | 403 | 403 | ✅ | error=You do not have access to this event meal plan |
| 54 | Nadia | `POST /api/events/{ev}/dietary-restrictions` | restriction inconnue → 400 | 400 | 500 | ❌ | error=Failed to add the dietary restriction. Please try again. |
| 55 | Sophie | `GET /api/events/{ev}/dietary-restrictions/counts` | comptage régimes (traiteur) | 200 | 200 | ✅ | {"GLUTEN_FREE": 1, "HALAL": 1} |
| 56 | Raphaël | `GET /api/events/{ev}/dietary-restrictions` | Raphaël lit les régimes (donnée santé) → 403 | 403 | 403 | ✅ | error=You do not have access to this event meal plan |
| 57 | Sophie | `POST /api/events/{ev}/scenarios` | lieu Domaine de la Bergerie | 201 | 201 | ✅ | status=PROPOSED |
| 58 | Sophie | `POST /api/events/{ev}/scenarios` | lieu Château de Vallery | 201 | 201 | ✅ | status=PROPOSED |
| 59 | Karim | `POST /api/events/{ev}/scenarios` | Karim ajoute un lieu → 403 | 403 | 403 | ✅ | error=Only the organizer can create scenarios |
| 60 | Sophie | `POST /api/scenarios/{sc}/vote` | Sophie PREFER Bergerie | 201 | 201 | ✅ | { "id": "vote_1790545311113_0.7636789923648767", "scenarioId": "scenario_1790545311091_0.3410535602564493", "p |
| 61 | Sophie | `POST /api/scenarios/{sc}/vote` | Sophie NEUTRAL Vallery | 201 | 201 | ✅ | { "id": "vote_1790545311126_0.35197454071855405", "scenarioId": "scenario_1790545311101_0.3137693080334004", " |
| 62 | Karim | `POST /api/scenarios/{sc}/vote` | Karim PREFER Bergerie | 201 | 201 | ✅ | { "id": "vote_1790545311142_0.07157572419035096", "scenarioId": "scenario_1790545311091_0.3410535602564493", " |
| 63 | Karim | `POST /api/scenarios/{sc}/vote` | Karim PREFER Vallery | 201 | 201 | ✅ | { "id": "vote_1790545311162_0.37139853899399", "scenarioId": "scenario_1790545311101_0.3137693080334004", "par |
| 64 | Nadia | `POST /api/scenarios/{sc}/vote` | Nadia NEUTRAL Bergerie | 201 | 201 | ✅ | { "id": "vote_1790545311197_0.3095316495608492", "scenarioId": "scenario_1790545311091_0.3410535602564493", "p |
| 65 | Nadia | `POST /api/scenarios/{sc}/vote` | Nadia AGAINST Vallery | 201 | 201 | ✅ | { "id": "vote_1790545311219_0.048611901784960665", "scenarioId": "scenario_1790545311101_0.3137693080334004", |
| 66 | Olivier | `POST /api/scenarios/{sc}/vote` | Olivier PREFER Bergerie | 201 | 201 | ✅ | { "id": "vote_1790545311241_0.7977267247752368", "scenarioId": "scenario_1790545311091_0.3410535602564493", "p |
| 67 | Olivier | `POST /api/scenarios/{sc}/vote` | Olivier NEUTRAL Vallery | 201 | 201 | ✅ | { "id": "vote_1790545311266_0.5115153260178898", "scenarioId": "scenario_1790545311101_0.3137693080334004", "p |
| 68 | Pauline | `POST /api/scenarios/{sc}/vote` | Pauline AGAINST Bergerie | 201 | 201 | ✅ | { "id": "vote_1790545311284_0.4083482480212144", "scenarioId": "scenario_1790545311091_0.3410535602564493", "p |
| 69 | Pauline | `POST /api/scenarios/{sc}/vote` | Pauline PREFER Vallery | 201 | 201 | ✅ | { "id": "vote_1790545311299_0.9314822241932375", "scenarioId": "scenario_1790545311101_0.3137693080334004", "p |
| 70 | Nadia | `PUT /api/scenarios/{sc}` | Nadia modifie un scénario → 403 | 403 | 403 | ✅ | error=Only the organizer can update scenarios |
| 71 | Nadia | `DELETE /api/scenarios/{sc}` | Nadia supprime un scénario → 403 | 403 | 403 | ✅ | error=Only the organizer can delete scenarios |
| 72 | Karim | `POST /api/events/{ev}/scenarios/{sc}/select-final` | Karim select-final → 403 | 403 | 403 | ✅ | error=Only the organizer can select final scenario |
| 73 | Sophie | `POST /api/events/{ev}/scenarios/{sc}/select-final` | Sophie choisit la Bergerie | 200 | 200 | ✅ | status=selected |
| 74 | Sophie | `PUT /api/events/{ev}/status` | COMPARING → ORGANIZING | 200 | 200 | ✅ | status=ORGANIZING |
| 75 | Sophie | `POST /api/events/{ev}/accommodation` | hôtel bloc 20 pers. CONFIRMED | 201 | 201 | ✅ | totalCost=22000 |
| 76 | Karim | `POST /api/events/{ev}/accommodation` | Karim crée hébergement → 403 | 403 | 403 | ✅ | error=Only the organizer can create accommodations |
| 77 | Sophie | `POST /api/events/{ev}/accommodation/{uuid}/rooms` | chambre 101 Nadia+Olivier | 201 | 201 | ✅ | { "id": "b66f5306-bc88-49a1-9976-9b32f274677c", "accommodationId": "255d925e-5eb1-4c2c-bfae-042ca4865b08", "ro |
| 78 | Sophie | `POST /api/events/{ev}/accommodation/{uuid}/rooms` | chambre sur-capacité (2 pers./1 place) → 400 | 400 | 400 | ✅ | error=Too many participants assigned (2 > 1) |
| 79 | Sophie | `POST /api/events/{ev}/accommodation/{uuid}/rooms` | chambre avec non-participant → 400 | 400 | 400 | ✅ | error=Invalid assigned participants. Please select event members and try again. |
| 80 | Nadia | `PUT /api/events/{ev}/accommodation/{uuid}` | Nadia annule l'hôtel → 403 | 403 | 403 | ✅ | error=Only the organizer can update accommodations |
| 81 | Nadia | `GET /api/events/{ev}/accommodation/{uuid}/rooms` | Nadia voit les chambres | 200 | 200 | ✅ | [{"id": "b66f5306-bc88-49a1-9976-9b32f274677c", "accommodationId": "255d925e-5eb1-4c2c-bfa |
| 82 | Karim | `PUT /api/events/{ev}/budget` | Karim fixe la baseline → 403 | 403 | 403 | ✅ | error=You do not have access to this event |
| 83 | Sophie | `PUT /api/events/{ev}/budget` | baseline 25 000 € | 200 | 200 | ✅ | total=25000.0 |
| 84 | Sophie | `PUT /api/events/{ev}/budget` | baseline négative → 400 | 400 | 200 | ❌ | ACCEPTÉ total=-5.0 |
| 85 | Sophie | `PUT /api/events/{ev}/budget` | restauration baseline 25 000 € | 200 | 200 | ✅ | { "id": "budget-1790545320580-1540", "eventId": "event_1790545310000_58dad50e", "totalEstimated": 25000.0, "to |
| 86 | Sophie | `POST /api/events/{ev}/budget/items` | Sophie: Location domaine (6500 €) | 201 | 201 | ✅ | id ok |
| 87 | Sophie | `POST /api/events/{ev}/budget/items` | Sophie: Traiteur (90 couverts) (9900 €) | 201 | 201 | ✅ | id ok |
| 88 | Sophie | `POST /api/events/{ev}/budget/items` | Sophie: Pièce montée (650 €) | 201 | 201 | ✅ | id ok |
| 89 | Sophie | `POST /api/events/{ev}/budget/items` | Sophie: DJ + sono (1400 €) | 201 | 201 | ✅ | id ok |
| 90 | Sophie | `POST /api/events/{ev}/budget/items` | Sophie: Photographe (2200 €) | 201 | 201 | ✅ | id ok |
| 91 | Sophie | `POST /api/events/{ev}/budget/items` | Sophie: Fleuriste (1100 €) | 201 | 201 | ✅ | id ok |
| 92 | Sophie | `POST /api/events/{ev}/budget/items` | Sophie: Navette gare ↔ domaine (780 €) | 201 | 201 | ✅ | id ok |
| 93 | Sophie | `POST /api/events/{ev}/budget/items` | Sophie: Location vaisselle (540 €) | 201 | 201 | ✅ | id ok |
| 94 | Karim | `POST /api/events/{ev}/budget/items` | Karim: Faire-part (320 €) | 201 | 201 | ✅ | id ok |
| 95 | Karim | `POST /api/events/{ev}/budget/items` | Karim: Alliances (1900 €) | 201 | 201 | ✅ | id ok |
| 96 | Nadia | `POST /api/events/{ev}/budget/items` | Nadia: Cadeau commun des témoins (300 €) | 201 | 201 | ✅ | id ok |
| 97 | Olivier | `POST /api/events/{ev}/budget/items` | Olivier: Photobooth (450 €) | 201 | 201 | ✅ | id ok |
| 98 | Nadia | `GET /api/events/{ev}/budget/items` | liste items | 200 | 500 | ❌ | error=Failed to fetch budget items. Please try again. |
| 99 | — | `(assertion)` | 12 items persistés | ✔ | ✘ | ❌ | -1 |
| 100 | Olivier | `GET /api/events/{ev}/budget/items/budget-1790545320693-7197` | lecture item traiteur | 200 | 200 | ✅ | cost=9900.0 |
| 101 | Olivier | `PUT /api/events/{ev}/budget/items/budget-1790545320693-7197` | Olivier modifie l'item de Sophie → 403 | 403 | 403 | ✅ | error=You do not have access to this event |
| 102 | Nadia | `DELETE /api/events/{ev}/budget/items/budget-1790545320693-7197` | Nadia supprime l'item de Sophie → 403 | 403 | 403 | ✅ | error=You do not have access to this event |
| 103 | Karim | `POST /api/events/{ev}/budget/expenses` | Karim avance les alliances (dépense) | 201 | 201 | ✅ | {"id": "expense-1790545321009-2218", "eventId": "event_1790545310000_58dad50e", "budgetId" |
| 104 | Nadia | `POST /api/events/{ev}/budget/expenses` | Nadia déclare une dépense payée par Olivier → 403 | 403 | 403 | ✅ | error=You do not have access to this event |
| 105 | Sophie | `GET /api/events/{ev}/budget/settlements` | équilibrages | 200 | 200 | ✅ | {"settlements": [], "count": 0} |
| 106 | — | `(assertion)` | dépense avancée par Karim (1900 € / Sophie+Karim) → Sophie doit 950 € à Karim | ✔ | ✘ | ❌ | settlements=0 |
| 107 | Sophie | `GET /api/events/{ev}/budget/summary` | résumé budget | 200 | 500 | ❌ | error=Failed to fetch the budget summary. Please try again. |
| 108 | Raphaël | `GET /api/events/{ev}/budget/items` | Raphaël lit le budget → 403 | 403 | 403 | ✅ | error=You do not have access to this event |
| 109 | Sophie | `POST /api/events/{ev}/meetings/persisted` | visio traiteur | 201 | 201 | ✅ | SCHEDULED |
| 110 | Karim | `POST /api/events/{ev}/meetings/persisted` | Karim crée une réunion → 403 | 403 | 403 | ✅ | error=Only the event organizer can create meetings |
| 111 | Sophie | `POST /api/events/{ev}/transport/not-needed` | transport : navette gérée via budget | 200 | 200 | ✅ | { "transportNotNeeded": true } |
| 112 | Sophie | `POST /api/events/{ev}/payment/tricount/link` | Tricount | 201 | 201 | ✅ | { "eventId": "event_1790545310000_58dad50e", "provider": "TRICOUNT", "providerId": "mariage-sk", "providerUrl" |
| 113 | Karim | `POST /api/events/{ev}/payment/tricount/link` | Karim relie Tricount → 403 | 403 | 403 | ✅ | error=You do not have access to this event |
| 114 | Pauline | `POST /api/events/{ev}/comments` | Pauline commente (hébergement) | 201 | 201 | ✅ | { "id": "d66760ac-a143-4351-9cc7-ebe1f6d7a32f", "eventId": "event_1790545310000_58dad50e", "section": "ACCOMMO |
| 115 | Karim | `GET /api/events/{ev}/readiness` | Karim lit la readiness → 403 | 403 | 403 | ✅ | error=Only the event organizer can view finalization readiness |
| 116 | Karim | `PUT /api/events/{ev}/status` | Karim finalise → 403 | 403 | 403 | ✅ | error=Only the organizer can update event status |
| 117 | Sophie | `GET /api/events/{ev}/readiness` | readiness | 200 | 200 | ✅ | complete=True blockers=[] |
| 118 | Sophie | `PUT /api/events/{ev}/status` | ORGANIZING → FINALIZED | 200 | 200 | ✅ | status=FINALIZED |
| 119 | Nadia | `POST /api/events/{ev}/dietary-restrictions` | régime après finalisation (info traiteur) | 201/409 | 201 | ✅ | { "id": "a824f234-fae8-4d7b-b6ba-b2752aa11284", "participantId": "user_-1203957139_5a9cb345fae4d440", "eventId |
| 120 | Nadia | `GET /api/events/{ev}/calendar/ics` | ICS mariage (Nadia) | 200 | 200 | ✅ | ok |

### Scénario 4 — Balade en forêt de Fontainebleau (Théo org., Ugo, Zoé + Yann intrus) — 53 ✅ / 15 ❌

| # | Acteur | Endpoint | Étape | Attendu | Obtenu | | Gist |
|---|---|---|---|---|---|---|---|
| 1 | Théo | `POST /api/events` | création événement | 201 | 201 | ✅ | id=event_1790545207320_368dfbdf status=DRAFT |
| 2 | Théo | `POST /api/events/{ev}/participants` | ajout Ugo | 201 | 201 | ✅ | { "participants": [ "user_-515271790_-566c0683fc4d0a37", "user_744007598_dc3dbf93c6789cc" ] } |
| 3 | Théo | `POST /api/events/{ev}/invite` | lien pour Zoé (maxUses=1) | 201 | 201 | ✅ | XLX7ESH3 |
| 4 | Zoé | `POST /api/invite/XLX7ESH3/accept` | Zoé rejoint | 200 | 200 | ✅ | Vous avez rejoint l'événement « Balade e |
| 5 | Théo | `PUT /api/events/{ev}/status` | DRAFT → CONFIRMED direct (PUT) → refus | 400/409 | 409 | ✅ | error=Poll date confirmation must use the modeled confirmation command |
| 6 | Théo | `POST /api/sync` | DRAFT → CONFIRMED direct (sync) → refus (POLLING requis) | 409 | 409 | ✅ | message=1 conflicts detected |
| 7 | Théo | `PUT /api/events/{ev}/status` | DRAFT → POLLING (obligatoire) | 200 | 200 | ✅ | status=POLLING |
| 8 | Théo | `POST /api/sync` | confirmation immédiate sans aucun vote | 200 | 200 | ✅ | applied=1 conflicts=0 |
| 9 | — | `(assertion)` | statut CONFIRMED sans vote (sondage « sauté » en 2 appels) | ✔ | ✔ | ✅ | CONFIRMED |
| 10 | Ugo | `POST /api/events/{ev}/poll/votes` | vote après confirmation → 400 | 400 | 400 | ✅ | error=Failed to save your vote. Please try again. |
| 11 | Théo | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP Théo | 200 | 200 | ✅ | ACCEPTED |
| 12 | Ugo | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP Ugo | 200 | 200 | ✅ | ACCEPTED |
| 13 | Zoé | `POST /api/events/{ev}/participants/{user}/rsvp` | RSVP Zoé | 200 | 200 | ✅ | ACCEPTED |
| 14 | Théo | `GET /api/events/{ev}/equipment` | liste équipement (vide) | 200 | 200 | ✅ | 0 items |
| 15 | Théo | `POST /api/events/{ev}/equipment` | trousse de secours → Ugo | 201 | 201 | ✅ | id=537f8d97 assignedTo=Ugo |
| 16 | Zoé | `POST /api/events/{ev}/equipment` | Zoé apporte 2 thermos | 201 | 201 | ✅ | id=efdf1fe9 status=NEEDED |
| 17 | Ugo | `POST /api/events/{ev}/equipment` | Ugo ajoute 2 crash pads (non assignés) | 201 | 201 | ✅ | NEEDED |
| 18 | Ugo | `PUT /api/events/{ev}/equipment/{uuid}/assign` | Ugo s'assigne les crash pads | 200 | 200 | ✅ | ASSIGNED |
| 19 | Ugo | `PUT /api/events/{ev}/equipment/{uuid}/status` | Ugo : trousse PACKED | 200 | 200 | ✅ | PACKED |
| 20 | Théo | `POST /api/events/{ev}/equipment` | équipement sans nom → 400 | 400 | 400 | ✅ | error=Equipment name is required |
| 21 | Théo | `POST /api/events/{ev}/equipment` | catégorie inconnue → 400 | 400 | 500 | ❌ | error=Failed to create the equipment item. Please try again. |
| 22 | Zoé | `GET /api/events/{ev}/equipment/participant/{user}` | ce que Zoé apporte | 200 | 200 | ✅ | Thermos de café |
| 23 | Théo | `GET /api/events/{ev}/equipment/statistics` | stats équipement | 200 | 200 | ✅ | {"eventId": "event_1790545207320_368dfbdf", "items": [{"id": "efdf1fe9-d80c-41fb-9119-15b491eabc2c", |
| 24 | Yann | `GET /api/events/{ev}/equipment` | Yann (intrus) lit l'équipement → 403 | 403 | 200 | ❌ | 3 items lus: Thermos de café, Trousse de secours, Crash pad |
| 25 | Yann | `POST /api/events/{ev}/equipment` | Yann ajoute un item → 403 | 403 | 201 | ❌ | CRÉÉ 6b704c94 |
| 26 | Yann | `PUT /api/events/{ev}/equipment/{uuid}/assign` | Yann s'assigne le thermos de Zoé → 403 | 403 | 200 | ❌ | assignedTo=user_-836808875_-4aaf27ee25ec9d9e |
| 27 | Yann | `PUT /api/events/{ev}/equipment/{uuid}/status` | Yann change le statut des crash pads d'Ugo → 403 | 403 | 200 | ❌ | status=CONFIRMED |
| 28 | Yann | `DELETE /api/events/{ev}/equipment/{uuid}` | Yann supprime les crash pads → 403 | 403 | 204 | ❌ |  |
| 29 | Théo | `GET /api/events/{ev}/equipment` | état final équipement vu par Théo | 200 | 200 | ✅ | Thermos de café:ASSIGNED:Yann; Pub casino:NEEDED:-; Trousse de secours:PACKED:89cc |
| 30 | Yann | `POST /api/events/event_nexiste_pas/equipment` | équipement sur événement inexistant → 404 | 404 | 500 | ❌ | error=Failed to create the equipment item. Please try again. |
| 31 | Théo | `POST /api/events/{ev}/activities` | activité bloc (max 2) | 201 | 201 | ✅ | id=d81f37a3 max=2 |
| 32 | Zoé | `POST /api/events/{ev}/activities` | Zoé propose le pique-nique | 201 | 201 | ✅ | { "id": "df6f9018-f04e-426a-9c7b-f6b3d7a8636e", "eventId": "event_1790545207320_368dfbdf", "name": "Pique-niqu |
| 33 | Théo | `POST /api/events/{ev}/activities` | heure invalide → 400 | 400 | 400 | ✅ | error=Invalid time format. Use HH:MM |
| 34 | Théo | `POST /api/events/{ev}/activities` | payload activité invalide → 400 | 400 | 500 | ❌ | error=Failed to create the activity. Please try again. |
| 35 | Ugo | `POST /api/events/{ev}/activities/{uuid}/register` | Ugo s'inscrit au bloc | 200/201 | 201 | ✅ | { "success": true } |
| 36 | Zoé | `POST /api/events/{ev}/activities/{uuid}/register` | Zoé s'inscrit au bloc | 200/201 | 201 | ✅ | { "success": true } |
| 37 | Théo | `POST /api/events/{ev}/activities/{uuid}/register` | Théo s'inscrit : complet (max 2) → refus | 400/409 | 409 | ✅ | error=Activity is full |
| 38 | Ugo | `POST /api/events/{ev}/activities/{uuid}/register` | Ugo inscrit Théo au pique-nique à sa place → 403 | 403 | 201 | ❌ | { "success": true } |
| 39 | Ugo | `POST /api/events/{ev}/activities/{uuid}/register` | Ugo s'inscrit 2× → refus | 400/409 | 409 | ✅ | error=Activity is full |
| 40 | Zoé | `GET /api/events/{ev}/activities/{uuid}/participants` | inscrits au bloc | 200 | 200 | ✅ | [{"id": "4721bfa9-fca2-466a-b796-595c7726f890", "activityId": "d81f37a3-5243-4608-bf6d-4474cd6d9b29" |
| 41 | Yann | `GET /api/events/{ev}/activities` | Yann lit les activités → 403 | 403 | 403 | ✅ | error=You do not have access to this event |
| 42 | Théo | `GET /api/events/{ev}/activities/schedule` | programme de la journée | 200 | 200 | ✅ | [{"date": "2026-10-11", "activities": [{"id": "d81f37a3-5243-4608-bf6d-4474cd6d9b29", "eventId": "ev |
| 43 | Théo | `GET /api/events/{ev}/weather` | météo : aucun endpoint exposé (table eventWeatherSnapshot orpheline) — info | 404 | 404 | ✅ |  |
| 44 | Théo | `POST /api/events/{ev}/scenarios` | lieu de RDV (scénario obligatoire) | 201 | 201 | ✅ | status=PROPOSED |
| 45 | Théo | `POST /api/events/{ev}/scenarios/{sc}/select-final` | sélection du lieu | 200 | 200 | ✅ | status=selected |
| 46 | Théo | `PUT /api/events/{ev}/status` | COMPARING → ORGANIZING | 200 | 200 | ✅ | status=ORGANIZING |
| 47 | Théo | `PUT /api/events/{ev}/transport/departures/{user}` | Théo part de Paris 12e | 200 | 200 | ✅ | { "eventId": "event_1790545207320_368dfbdf", "participantId": "user_-515271790_-566c0683fc4d0a37", "location": |
| 48 | Ugo | `PUT /api/events/{ev}/transport/departures/{user}` | Ugo part de Paris 14e | 200 | 200 | ✅ | { "eventId": "event_1790545207320_368dfbdf", "participantId": "user_744007598_dc3dbf93c6789cc", "location": { |
| 49 | Zoé | `PUT /api/events/{ev}/transport/departures/{user}` | Zoé part de Melun | 200 | 200 | ✅ | { "eventId": "event_1790545207320_368dfbdf", "participantId": "user_1631125883_4682e2bbc37df476", "location": |
| 50 | Zoé | `GET /api/events/{ev}/transport/readiness` | readiness transport | 200 | 200 | ✅ | canGenerate=True missing=[] |
| 51 | Théo | `POST /api/events/{ev}/transport/plans/generate` | génération covoiturage (coût min.) | 201 | 201 | ✅ | plan=transpor total=400.0 routes=3 |
| 52 | Théo | `POST /api/events/{ev}/transport/plans/transport_plan_{ev}_COST_MINIMIZE_3573877004/select` | sélection du plan | 200 | 200 | ✅ | {"eventId": "event_1790545207320_368dfbdf", "planId": "transport_plan_event_1790545207320_368dfbdf_C |
| 53 | Ugo | `POST /api/events/{ev}/transport/plans/transport_plan_{ev}_COST_MINIMIZE_3573877004/select` | Ugo sélectionne un plan → 403 | 403 | 403 | ✅ | error=Only the organizer can select transport plans |
| 54 | Yann | `GET /api/events/{ev}/transport/plans` | Yann lit les plans transport → 403 | 403/404 | 403 | ✅ | error=You do not have access to transport plans |
| 55 | Théo | `POST /api/events/{ev}/readiness/LODGING/not-needed` | pas de logement | 200 | 200 | ✅ | { "section": "LODGING", "notNeeded": true } |
| 56 | Théo | `POST /api/events/{ev}/readiness/MEETINGS/not-needed` | pas de réunion | 200 | 200 | ✅ | { "section": "MEETINGS", "notNeeded": true } |
| 57 | Théo | `POST /api/events/{ev}/readiness/BUDGET_BASELINE/not-needed` | budget non nécessaire (sortie gratuite) | 200 | 400 | ❌ | error=Unsupported readiness section: BUDGET_BASELINE |
| 58 | Théo | `POST /api/events/{ev}/readiness/PAYMENT/not-needed` | paiement/Tricount non nécessaire | 200 | 400 | ❌ | error=Unsupported readiness section: PAYMENT |
| 59 | Théo | `GET /api/events/{ev}/readiness` | readiness sortie gratuite | 200 | 200 | ✅ | blockers=['BUDGET_REQUIRED', 'PAYMENT_POT_REQUIRED', 'TRICOUNT_HANDOFF_REQUIRED'] |
| 60 | — | `(assertion)` | une balade gratuite peut être finalisée sans budget ni Tricount | ✔ | ✘ | ❌ | blockers=['BUDGET_REQUIRED', 'PAYMENT_POT_REQUIRED', 'TRICOUNT_HANDOFF_REQUIRED'] |
| 61 | Théo | `PUT /api/events/{ev}/budget` | contournement : budget essence 15 € | 200 | 200 | ✅ | { "id": "budget-1790545259519-3276", "eventId": "event_1790545207320_368dfbdf", "totalEstimated": 15.0, "total |
| 62 | Théo | `POST /api/events/{ev}/payment/tricount/link` | contournement : Tricount créé pour 15 € | 201 | 201 | ✅ | { "eventId": "event_1790545207320_368dfbdf", "provider": "TRICOUNT", "providerId": "fontainebleau", "providerU |
| 63 | Théo | `GET /api/events/{ev}/readiness` | readiness | 200 | 200 | ✅ | complete=True blockers=[] |
| 64 | Théo | `PUT /api/events/{ev}/status` | ORGANIZING → FINALIZED | 200 | 200 | ✅ | status=FINALIZED |
| 65 | Zoé | `PUT /api/events/{ev}/equipment/{uuid}/status` | équipement modifié après FINALIZED → 409 | 409 | 200 | ❌ | MODIFIÉ status=CONFIRMED |
| 66 | Ugo | `POST /api/events/{ev}/equipment` | équipement ajouté après FINALIZED → 409 | 409 | 201 | ❌ | CRÉÉ |
| 67 | Zoé | `POST /api/events/{ev}/activities` | activité créée après FINALIZED → 409 | 409 | 201 | ❌ | CRÉÉE |
| 68 | Zoé | `GET /api/events/{ev}/calendar/ics` | ICS balade | 200 | 200 | ✅ | ok |

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
