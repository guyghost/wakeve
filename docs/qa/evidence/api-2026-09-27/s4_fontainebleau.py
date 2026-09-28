from lib import *
import json, pickle

sc = Scenario("Scénario 4 — Balade en forêt de Fontainebleau (Théo org., Ugo, Zoé + Yann intrus)")
theo, ugo, zoe, yann = (User(n) for n in ["Théo", "Ugo", "Zoé", "Yann"])
SLOT = {"id": "dim-11", "start": "2026-10-11T07:30:00Z", "end": "2026-10-11T16:00:00Z", "timezone": "Europe/Paris", "timeOfDay": "ALL_DAY"}
DEADLINE = "2026-10-09T20:00:00Z"
ev = create_event(sc, theo, "Balade en forêt de Fontainebleau 🌲", "Rando + bloc au Cuvier, pique-nique. Date déjà fixée : dimanche 11 octobre.", [SLOT], DEADLINE, "SPORTS_EVENT",
                  {"expectedParticipants": 3})
E = f"/api/events/{ev}"
sc.step(theo, "POST", f"{E}/participants", {"eventId": ev, "participantId": ugo.id}, 201, "ajout Ugo")
s, inv = sc.step(theo, "POST", f"{E}/invite", {"maxUses": 1}, 201, "lien pour Zoé (maxUses=1)", show=lambda j: j["code"])
sc.step(zoe, "POST", f"/api/invite/{inv['code']}/accept", None, 200, "Zoé rejoint", show=lambda j: j["message"][:40])

# date déjà connue : peut-on sauter le sondage ?
status(sc, theo, ev, "CONFIRMED", (400, 409), "DRAFT → CONFIRMED direct (PUT) → refus", slotId="dim-11")
sync_confirm(sc, theo, ev, "dim-11", SLOT["start"], 409, "DRAFT → CONFIRMED direct (sync) → refus (POLLING requis)", deadline=DEADLINE)
status(sc, theo, ev, "POLLING", 200, "DRAFT → POLLING (obligatoire)")
sync_confirm(sc, theo, ev, "dim-11", SLOT["start"], 200, "confirmation immédiate sans aucun vote", deadline=DEADLINE)
sc.note("statut CONFIRMED sans vote (sondage « sauté » en 2 appels)", get_status(theo, ev) == "CONFIRMED", get_status(theo, ev))
sc.step(ugo, "POST", f"{E}/poll/votes", {"eventId": ev, "participantId": ugo.id, "slotId": "dim-11", "vote": "YES"}, 400, "vote après confirmation → 400")
for u in (theo, ugo, zoe):
    sc.step(u, "POST", f"{E}/participants/{u.id}/rsvp", {"slotId": "dim-11", "attendance": "CONFIRMED"}, 200, f"RSVP {u.name}", show=lambda j: j["rsvpState"])

# équipement (qui apporte quoi)
sc.step(theo, "GET", f"{E}/equipment", None, 200, "liste équipement (vide)", show=lambda j: f"{len(j)} items")
s, it1 = sc.step(theo, "POST", f"{E}/equipment", {"name": "Trousse de secours", "category": "SAFETY", "quantity": 1, "assignedTo": ugo.id, "status": "ASSIGNED"}, 201, "trousse de secours → Ugo",
                 show=lambda j: f"id={j['id'][:8]} assignedTo=Ugo")
s, it2 = sc.step(zoe, "POST", f"{E}/equipment", {"name": "Thermos de café", "category": "COOKING", "quantity": 2, "assignedTo": zoe.id}, 201, "Zoé apporte 2 thermos",
                 show=lambda j: f"id={j['id'][:8]} status={j['status']}")
s, it3 = sc.step(ugo, "POST", f"{E}/equipment", {"name": "Crash pad", "category": "SPORTS", "quantity": 2}, 201, "Ugo ajoute 2 crash pads (non assignés)", show=lambda j: j["status"])
sc.step(ugo, "PUT", f"{E}/equipment/{it3['id']}/assign", {"participantId": ugo.id}, 200, "Ugo s'assigne les crash pads", show=lambda j: j["status"])
sc.step(ugo, "PUT", f"{E}/equipment/{it1['id']}/status", {"newStatus": "PACKED"}, 200, "Ugo : trousse PACKED", show=lambda j: j.get("status"))
sc.step(theo, "POST", f"{E}/equipment", {"name": "", "category": "OTHER", "quantity": 1}, 400, "équipement sans nom → 400")
sc.step(theo, "POST", f"{E}/equipment", {"name": "Corde", "category": "CORDE", "quantity": 1}, 400, "catégorie inconnue → 400")
sc.step(zoe, "GET", f"{E}/equipment/participant/{zoe.id}", None, 200, "ce que Zoé apporte", show=lambda j: ", ".join(x["name"] for x in j))
sc.step(theo, "GET", f"{E}/equipment/statistics", None, 200, "stats équipement", show=lambda j: json.dumps(j)[:100])
# contrôle d'accès équipement
sc.step(yann, "GET", f"{E}/equipment", None, 403, "Yann (intrus) lit l'équipement → 403", show=lambda j: f"{len(j)} items lus: " + ", ".join(x["name"] for x in j))
sc.step(yann, "POST", f"{E}/equipment", {"name": "Pub casino", "category": "OTHER", "quantity": 1}, 403, "Yann ajoute un item → 403", show=lambda j: f"CRÉÉ {j['id'][:8]}")
sc.step(yann, "PUT", f"{E}/equipment/{it2['id']}/assign", {"participantId": yann.id}, 403, "Yann s'assigne le thermos de Zoé → 403", show=lambda j: f"assignedTo={j.get('assignedTo')}")
sc.step(yann, "PUT", f"{E}/equipment/{it3['id']}/status", {"newStatus": "CONFIRMED"}, 403, "Yann change le statut des crash pads d'Ugo → 403", show=lambda j: f"status={j.get('status')}")
sc.step(yann, "DELETE", f"{E}/equipment/{it3['id']}", None, 403, "Yann supprime les crash pads → 403")
s, lst = sc.step(theo, "GET", f"{E}/equipment", None, 200, "état final équipement vu par Théo", show=lambda j: "; ".join(f"{x['name']}:{x['status']}:{'Yann' if x.get('assignedTo') == yann.id else (x.get('assignedTo') or '-')[-4:]}" for x in j))
sc.step(yann, "POST", "/api/events/event_nexiste_pas/equipment", {"name": "x", "category": "OTHER", "quantity": 1}, 404, "équipement sur événement inexistant → 404")

# activités
act = {"name": "Bloc au Cuvier", "description": "Circuit jaune puis orange", "date": "2026-10-11", "time": "10:00", "durationMinutes": 150, "location": "Bas Cuvier", "maxParticipants": 2, "organizerId": theo.id}
s, a = sc.step(theo, "POST", f"{E}/activities", act, 201, "activité bloc (max 2)", show=lambda j: f"id={j['id'][:8]} max={j.get('maxParticipants')}")
s_, pn = sc.step(zoe, "POST", f"{E}/activities", dict(act, name="Pique-nique aux Gorges d'Apremont", time="13:00", durationMinutes=60, maxParticipants=None, organizerId=zoe.id), 201, "Zoé propose le pique-nique")
sc.step(theo, "POST", f"{E}/activities", dict(act, time="10h"), 400, "heure invalide → 400")
sc.step(theo, "POST", f"{E}/activities", None, 400, "payload activité invalide → 400", raw_body='{"name":"x"}')
aid = a["id"] if s == 201 else "x"
pid = pn["id"] if s_ == 201 else "x"
sc.step(ugo, "POST", f"{E}/activities/{aid}/register", {"participantId": ugo.id}, (200, 201), "Ugo s'inscrit au bloc")
sc.step(zoe, "POST", f"{E}/activities/{aid}/register", {"participantId": zoe.id}, (200, 201), "Zoé s'inscrit au bloc")
sc.step(theo, "POST", f"{E}/activities/{aid}/register", {"participantId": theo.id}, (400, 409), "Théo s'inscrit : complet (max 2) → refus")
sc.step(ugo, "POST", f"{E}/activities/{pid}/register", {"participantId": theo.id}, 403, "Ugo inscrit Théo au pique-nique à sa place → 403")
sc.step(ugo, "POST", f"{E}/activities/{aid}/register", {"participantId": ugo.id}, (400, 409), "Ugo s'inscrit 2× → refus")
sc.step(zoe, "GET", f"{E}/activities/{aid}/participants", None, 200, "inscrits au bloc", show=lambda j: json.dumps(j)[:100])
sc.step(yann, "GET", f"{E}/activities", None, 403, "Yann lit les activités → 403")
sc.step(theo, "GET", f"{E}/activities/schedule", None, 200, "programme de la journée", show=lambda j: json.dumps(j)[:100])

# météo
sc.step(theo, "GET", f"{E}/weather", None, 404, "météo : aucun endpoint exposé (table eventWeatherSnapshot orpheline) — info")

# lieu + covoiturage
s, scn = sc.step(theo, "POST", f"{E}/scenarios", {"eventId": ev, "name": "Parking du Bas Cuvier", "dateOrPeriod": "11 oct.", "location": "Fontainebleau, Bas Cuvier", "duration": 1,
                                                 "estimatedParticipants": 3, "estimatedBudgetPerPerson": 10.0, "description": "RDV 9h30 parking"}, 201, "lieu de RDV (scénario obligatoire)")
sc.step(theo, "POST", f"{E}/scenarios/{scn['id']}/select-final", None, 200, "sélection du lieu")
status(sc, theo, ev, "ORGANIZING", 200, "COMPARING → ORGANIZING")
for u, city in ((theo, "Paris 12e"), (ugo, "Paris 14e"), (zoe, "Melun")):
    sc.step(u, "PUT", f"{E}/transport/departures/{u.id}", {"location": {"name": city}}, 200, f"{u.name} part de {city}")
sc.step(zoe, "GET", f"{E}/transport/readiness", None, 200, "readiness transport", show=lambda j: f"canGenerate={j.get('canGeneratePlan')} missing={j.get('missingDepartureParticipantIds')}")
s, plan = sc.step(theo, "POST", f"{E}/transport/plans/generate", {"optimizationType": "COST_MINIMIZE"}, 201, "génération covoiturage (coût min.)",
                  show=lambda j: f"plan={str(j.get('id'))[:8]} total={j.get('totalGroupCost')} routes={len(j.get('participantRoutes', {}))}")
if s == 201:
    sc.step(theo, "POST", f"{E}/transport/plans/{plan['id']}/select", None, 200, "sélection du plan", show=lambda j: json.dumps(j)[:100])
    sc.step(ugo, "POST", f"{E}/transport/plans/{plan['id']}/select", None, 403, "Ugo sélectionne un plan → 403")
else:
    sc.step(zoe, "POST", f"{E}/transport/not-needed", None, 200, "fallback : covoiturage géré hors app")
sc.step(yann, "GET", f"{E}/transport/plans", None, (403, 404), "Yann lit les plans transport → 403")

# finalisation d'une sortie gratuite
sc.step(theo, "POST", f"{E}/readiness/LODGING/not-needed", None, 200, "pas de logement")
sc.step(theo, "POST", f"{E}/readiness/MEETINGS/not-needed", None, 200, "pas de réunion")
sc.step(theo, "POST", f"{E}/readiness/BUDGET_BASELINE/not-needed", None, 200, "budget non nécessaire (sortie gratuite)")
sc.step(theo, "POST", f"{E}/readiness/PAYMENT/not-needed", None, 200, "paiement/Tricount non nécessaire")
s, rd = sc.step(theo, "GET", f"{E}/readiness", None, 200, "readiness sortie gratuite", show=lambda j: f"blockers={j['blockers']}")
sc.note("une balade gratuite peut être finalisée sans budget ni Tricount", s == 200 and rd["complete"], f"blockers={rd.get('blockers') if s == 200 else '-'}")
t = iso(now_utc())
budget = {"id": "x", "eventId": ev, "totalEstimated": 15.0, "totalActual": 0.0, "transportEstimated": 15.0, "transportActual": 0.0, "accommodationEstimated": 0.0, "accommodationActual": 0.0,
          "mealsEstimated": 0.0, "mealsActual": 0.0, "activitiesEstimated": 0.0, "activitiesActual": 0.0, "equipmentEstimated": 0.0, "equipmentActual": 0.0, "otherEstimated": 0.0, "otherActual": 0.0, "createdAt": t, "updatedAt": t}
sc.step(theo, "PUT", f"{E}/budget", budget, 200, "contournement : budget essence 15 €")
sc.step(theo, "POST", f"{E}/payment/tricount/link", {"provider": "TRICOUNT", "providerId": "fontainebleau", "providerUrl": "https://tricount.com/fontainebleau", "syncStatus": "LINKED"}, 201,
        "contournement : Tricount créé pour 15 €")
s, rd = sc.step(theo, "GET", f"{E}/readiness", None, 200, "readiness", show=lambda j: f"complete={j['complete']} blockers={j['blockers']}")
status(sc, theo, ev, "FINALIZED", 200, "ORGANIZING → FINALIZED")
sc.step(zoe, "PUT", f"{E}/equipment/{it2['id']}/status", {"newStatus": "CONFIRMED"}, 409, "équipement modifié après FINALIZED → 409", show=lambda j: f"MODIFIÉ status={j.get('status')}")
sc.step(ugo, "POST", f"{E}/equipment", {"name": "Après coup", "category": "OTHER", "quantity": 1}, 409, "équipement ajouté après FINALIZED → 409", show=lambda j: "CRÉÉ")
sc.step(zoe, "POST", f"{E}/activities", dict(act, name="Post-final", organizerId=zoe.id), 409, "activité créée après FINALIZED → 409", show=lambda j: "CRÉÉE")
sc.step(zoe, "GET", f"{E}/calendar/ics", None, 200, "ICS balade", show=lambda j: "ok")

sc.users = {u.name: (u.id, u.token) for u in (theo, ugo, zoe, yann)}; sc.ev = ev
pickle.dump(sc, open("/tmp/wakeve-qa/api/s4.pkl", "wb"))
print("EVENT", ev, sc.summary())
