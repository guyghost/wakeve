from lib import *
import json, pickle, time

sc = Scenario("Scénario 2 — Watch party finale Ligue des champions (Hugo org., Inès, Jules, Léa + Marc en retard, Nora intruse)")
hugo, ines, jules, lea, marc, nora = (User(n) for n in ["Hugo", "Inès", "Jules", "Léa", "Marc", "Nora"])
S1 = {"id": "sam-soir", "start": "2026-10-03T18:30:00Z", "end": "2026-10-03T21:30:00Z", "timezone": "Europe/Paris", "timeOfDay": "EVENING"}
S2 = {"id": "dim-soir", "start": "2026-10-04T18:30:00Z", "end": "2026-10-04T21:30:00Z", "timezone": "Europe/Paris", "timeOfDay": "EVENING"}
deadline_dt = now_utc() + datetime.timedelta(seconds=75)
DEADLINE = iso(deadline_dt)
ev = create_event(sc, hugo, "Watch party – finale Ligue des champions ⚽", "On regarde la finale chez Hugo, écran géant et apéro. Deadline de vote courte !",
                  [S1, S2], DEADLINE, "PARTY", {"expectedParticipants": 6})
E = f"/api/events/{ev}"
for u in (ines, jules, lea):
    sc.step(hugo, "POST", f"{E}/participants", {"eventId": ev, "participantId": u.id}, 201, f"ajout {u.name}")
s, inv = sc.step(hugo, "POST", f"{E}/invite", {"maxUses": 2}, 201, "lien d'invitation maxUses=2", show=lambda j: f"code={j['code']} {j['inviteUrl']}")
code = inv["code"] if s == 201 else "XXXX"
sc.step(ines, "POST", f"{E}/invite", {"maxUses": 5}, 403, "Inès crée un lien → 403")
sc.step(hugo, "POST", f"{E}/invite", {"maxUses": 0}, 400, "maxUses=0 → 400")
status(sc, hugo, ev, "POLLING", 200, "DRAFT → POLLING")
sc.step(hugo, "POST", f"{E}/participants", {"eventId": ev, "participantId": marc.id}, 400, "ajout direct après DRAFT → refus (invitation requise)")

votes = {hugo: ("YES", "MAYBE"), ines: ("YES", "NO"), jules: ("NO", "YES"), lea: ("MAYBE", "YES")}
for u, (a, b) in votes.items():
    sc.step(u, "POST", f"{E}/poll/votes", {"eventId": ev, "participantId": u.id, "slotId": "sam-soir", "vote": a}, 201, f"{u.name} {a} samedi")
    sc.step(u, "POST", f"{E}/poll/votes", {"eventId": ev, "participantId": u.id, "slotId": "dim-soir", "vote": b}, 201, f"{u.name} {b} dimanche")
sc.step(jules, "POST", f"{E}/poll/votes", {"eventId": ev, "participantId": jules.id, "slotId": "sam-soir", "vote": "YES"}, 201, "Jules change d'avis NO → YES samedi")
s, poll = sc.step(jules, "GET", f"{E}/poll", None, 200, "poll après changement", show=lambda j: f"Jules={j['votes'].get(jules.id)}")
sc.note("vote de Jules mis à jour (pas de doublon)", s == 200 and poll["votes"].get(jules.id) == {"sam-soir": "YES", "dim-soir": "YES"}, str(poll["votes"].get(jules.id)))

# late join
sc.step(None, "GET", f"/api/invite/{code}", None, 200, "résolution publique du lien (sans auth)", show=lambda j: f"{j['eventTitle'][:30]} status={j['eventStatus']} n={j['participantCount']}")
s, js = sc.step(marc, "POST", f"/api/invite/{code}/accept", None, 200, "Marc rejoint en retard via le lien", show=lambda j: j.get("message"))
sc.step(marc, "POST", f"/api/invite/{code}/accept", None, 200, "Marc ré-accepte (idempotent)", show=lambda j: j.get("message"))
sc.step(marc, "GET", E, None, 200, "Marc voit l'événement", show=lambda j: f"participants={len(j['participants'])}")
sc.step(marc, "POST", f"{E}/poll/votes", {"eventId": ev, "participantId": marc.id, "slotId": "sam-soir", "vote": "YES"}, 201, "Marc YES samedi")
sc.step(marc, "POST", f"{E}/poll/votes", {"eventId": ev, "participantId": marc.id, "slotId": "dim-soir", "vote": "NO"}, 201, "Marc NO dimanche")
sc.step(None, "POST", f"/api/invite/{code}/accept", None, 401, "accept sans token → 401")
sc.step(nora, "POST", "/api/invite/ZZZZZZZZ/accept", None, 404, "code inconnu → 404")

s, poll = sc.step(hugo, "GET", f"{E}/poll", None, 200, "poll final")
sc_ = score(poll["votes"], ["sam-soir", "dim-soir"]) if s == 200 else {}
sc.note("scores samedi=9 (2+2+2+1+2), dimanche=3 (1-1+2+2-1)", sc_ == {"sam-soir": 9, "dim-soir": 3}, str(sc_))

# deadline
wait = (deadline_dt - now_utc()).total_seconds() + 2
print(f"... attente deadline {wait:.0f}s")
if wait > 0:
    time.sleep(wait)
sc.step(lea, "POST", f"{E}/poll/votes", {"eventId": ev, "participantId": lea.id, "slotId": "sam-soir", "vote": "YES"}, 400, "Léa vote après la deadline → 400")
sync_confirm(sc, hugo, ev, "sam-soir", S1["start"], 200, "Hugo confirme samedi soir (sync)", deadline=DEADLINE)
sc.note("statut = CONFIRMED", get_status(hugo, ev) == "CONFIRMED", get_status(hugo, ev))
sync_confirm(sc, hugo, ev, "dim-soir", S2["start"], 409, "re-confirmation d'un autre créneau → conflit", deadline=DEADLINE)

# RSVP / decline
for u in (hugo, ines, jules, marc):
    sc.step(u, "POST", f"{E}/participants/{u.id}/rsvp", {"slotId": "sam-soir", "attendance": "CONFIRMED"}, 200, f"RSVP {u.name} CONFIRMED", show=lambda j: j["rsvpState"])
sc.step(lea, "POST", f"{E}/participants/{lea.id}/rsvp", {"slotId": "sam-soir", "attendance": "DECLINED"}, 200, "Léa décline", show=lambda j: f"{j['rsvpState']}/{j['dateValidationState']}")
s, parts = sc.step(hugo, "GET", f"{E}/participants", None, 200, "liste participants", show=lambda j: f"{len(j['participants'])} participants")
sc.note("5 participants (Hugo+Inès+Jules+Léa+Marc)", s == 200 and set([hugo.id, ines.id, jules.id, lea.id, marc.id]) <= set(parts["participants"]), f"{len(parts['participants']) if s == 200 else '-'}")

# lieu (scénario unique) → comparaison → organisation
s, scn = sc.step(hugo, "POST", f"{E}/scenarios", {"eventId": ev, "name": "Chez Hugo – écran géant", "dateOrPeriod": "Samedi 3 oct. 20h30", "location": "Paris 11e",
                                               "duration": 1, "estimatedParticipants": 4, "estimatedBudgetPerPerson": 12.0, "description": "Vidéoproj + canapé"}, 201, "lieu unique (scénario)")
sc.step(lea, "GET", f"{E}/scenarios", None, 403, "Léa (déclinée) lit les scénarios → 403")
sc.step(hugo, "POST", f"{E}/scenarios/{scn['id']}/select-final", None, 200, "sélection du lieu")
status(sc, hugo, ev, "ORGANIZING", 200, "COMPARING → ORGANIZING")

# meals / snacks
meal = {"eventId": ev, "type": "SNACK", "name": "Chips & guacamole", "date": "2026-10-03", "time": "20:15", "responsibleParticipantIds": [jules.id], "estimatedCost": 1500, "servings": 5}
s, m1 = sc.step(hugo, "POST", f"{E}/meals", meal, 201, "Snack assigné à Jules", show=lambda j: f"{j.get('name')} resp={len(j.get('responsibleParticipantIds', []))}")
sc.step(hugo, "POST", f"{E}/meals", dict(meal, type="APERITIF", name="Bières & softs", responsibleParticipantIds=[marc.id], estimatedCost=2500), 201, "Apéro assigné à Marc")
sc.step(jules, "POST", f"{E}/meals", dict(meal, name="Pizzas", responsibleParticipantIds=[jules.id]), (201, 403), "Jules propose d'apporter des pizzas (participant)")
sc.step(hugo, "POST", f"{E}/meals", dict(meal, time="25:99"), 400, "heure invalide → 400")
sc.step(hugo, "POST", f"{E}/meals", None, 400, "payload repas invalide → 400", raw_body=json.dumps({"eventId": ev, "name": "x"}))
sc.step(marc, "GET", f"{E}/meals", None, 200, "Marc voit qui apporte quoi", show=lambda j: ", ".join(f"{x['name']}" for x in (j if isinstance(j, list) else j.get('meals', []))))
sc.step(nora, "GET", f"{E}/meals", None, 403, "Nora (intruse) lit les repas → 403")
sc.step(ines, "POST", f"{E}/dietary-restrictions", {"participantId": ines.id, "eventId": ev, "restriction": "VEGETARIAN"}, 201, "Inès végétarienne")

# comments
for u, t in ((ines, "Je ramène le drapeau 🇫🇷"), (marc, "Allez les Bleus ! (ou pas, c'est la LDC)")):
    sc.step(u, "POST", f"{E}/comments", {"section": "GENERAL", "content": t, "authorId": u.id, "authorName": "x"}, 201, f"commentaire {u.name}", show=lambda j: f"author={j.get('authorName')}")
sc.step(ines, "POST", f"{E}/comments", {"section": "GENERAL", "content": "usurpation", "authorId": marc.id, "authorName": "Marc"}, 403, "Inès poste au nom de Marc → 403")
sc.step(ines, "POST", f"{E}/comments", {"section": "GENERAL", "content": "", "authorId": ines.id, "authorName": "x"}, 400, "commentaire vide → 400")
sc.step(nora, "POST", f"{E}/comments", {"section": "GENERAL", "content": "je m'incruste", "authorId": nora.id, "authorName": "Nora"}, 403, "Nora (intruse) commente → 403")
s, cl = sc.step(nora, "GET", f"{E}/comments", None, 403, "Nora (intruse) lit les commentaires → 403", show=lambda j: f"{len(j)} commentaires lus")
sc.step(nora, "POST", "/api/events/event_inexistant_123/comments", {"section": "GENERAL", "content": "fantôme", "authorId": nora.id, "authorName": "N"}, (403, 404), "commentaire sur événement inexistant → 404")

# finalisation d'une soirée à la maison
sc.step(hugo, "POST", f"{E}/readiness/LODGING/not-needed", None, 200, "logement non nécessaire")
sc.step(hugo, "POST", f"{E}/readiness/MEETINGS/not-needed", None, 200, "réunion non nécessaire")
sc.step(ines, "POST", f"{E}/readiness/MEETINGS/not-needed", None, 403, "Inès marque not-needed → 403")
sc.step(hugo, "POST", f"{E}/transport/not-needed", None, 200, "transport non nécessaire")
s, rd = sc.step(hugo, "GET", f"{E}/readiness", None, 200, "readiness avant budget/paiement", show=lambda j: f"blockers={j['blockers']}")
status(sc, hugo, ev, "FINALIZED", 409, "finalisation sans budget/Tricount → 409 + blockers")
t = iso(now_utc())
budget = {"id": "x", "eventId": ev, "totalEstimated": 60.0, "totalActual": 0.0, "transportEstimated": 0.0, "transportActual": 0.0, "accommodationEstimated": 0.0, "accommodationActual": 0.0,
          "mealsEstimated": 60.0, "mealsActual": 0.0, "activitiesEstimated": 0.0, "activitiesActual": 0.0, "equipmentEstimated": 0.0, "equipmentActual": 0.0, "otherEstimated": 0.0, "otherActual": 0.0, "createdAt": t, "updatedAt": t}
sc.step(hugo, "PUT", f"{E}/budget", budget, 200, "budget apéro 60 €")
sc.step(hugo, "POST", f"{E}/payment/tricount/link", {"provider": "TRICOUNT", "providerId": "ldc-final", "providerUrl": "https://tricount.com/ldc-final", "syncStatus": "LINKED"}, 201, "Tricount")
s, rd = sc.step(hugo, "GET", f"{E}/readiness", None, 200, "readiness", show=lambda j: f"complete={j['complete']} blockers={j['blockers']}")
status(sc, hugo, ev, "FINALIZED", 200, "ORGANIZING → FINALIZED")
sc.step(marc, "POST", f"/api/invite/{code}/accept", None, 200, "Marc rouvre l'ancien lien après finalisation (idempotent)")
sc.step(nora, "POST", f"/api/invite/{code}/accept", None, (410, 409, 403), "Nora rejoint un événement FINALIZED via le lien (1/2 utilisé) → refus attendu")
sc.step(nora, "GET", f"/api/invite/{code}", None, 410, "résolution du lien après 2/2 utilisations → 410")

sc.users = {u.name: (u.id, u.token) for u in (hugo, ines, jules, lea, marc, nora)}; sc.ev = ev; sc.code = code
pickle.dump(sc, open("/tmp/wakeve-qa/api/s2.pkl", "wb"))
print("EVENT", ev, sc.summary())
