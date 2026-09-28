from lib import *
import json, pickle

sc = Scenario("Scénario 3 — Mariage de Sophie & Karim (Sophie org., Karim « co-org. », Nadia, Olivier, Pauline + Quentin hors quota, Raphaël intrus)")
sophie, karim, nadia, olivier, pauline, quentin, raph = (User(n) for n in ["Sophie", "Karim", "Nadia", "Olivier", "Pauline", "Quentin", "Raphaël"])
slots = [{"id": "mai", "start": "2027-05-15T12:00:00Z", "end": "2027-05-16T02:00:00Z", "timezone": "Europe/Paris", "timeOfDay": "ALL_DAY"},
         {"id": "juin", "start": "2027-06-12T12:00:00Z", "end": "2027-06-13T02:00:00Z", "timezone": "Europe/Paris", "timeOfDay": "ALL_DAY"},
         {"id": "sept", "start": "2027-09-04T12:00:00Z", "end": "2027-09-05T02:00:00Z", "timezone": "Europe/Paris", "timeOfDay": "ALL_DAY"}]
DEADLINE = "2026-12-31T23:00:00Z"

# --- validation à la création (événements jetables)
sc.step(sophie, "POST", "/api/events", {"title": "", "description": "", "organizerId": sophie.id, "deadline": DEADLINE, "proposedSlots": slots[:1]}, 400, "création titre/description vides → 400",
        show=lambda j: f"CRÉÉ id={j['id']}")
sc.step(sophie, "POST", "/api/events", {"title": "Test", "description": "d", "organizerId": sophie.id, "deadline": DEADLINE, "proposedSlots": slots[:1], "minParticipants": 10, "maxParticipants": 2}, 400,
        "création max < min participants → 400", show=lambda j: f"CRÉÉ id={j['id']}")
sc.step(sophie, "POST", "/api/events", {"title": "Test", "description": "d", "organizerId": sophie.id, "deadline": DEADLINE, "proposedSlots": slots[:1], "eventType": "BAPTEME"}, 400, "eventType inconnu → 400")
sc.step(sophie, "POST", "/api/events", {"title": "Test", "description": "d", "organizerId": sophie.id, "deadline": "demain", "proposedSlots": slots[:1]}, 400, "deadline non ISO → 400",
        show=lambda j: f"CRÉÉ id={j['id']} deadline={j['deadline']}")
sc.step(sophie, "POST", "/api/events", {"title": "Test", "description": "d", "organizerId": sophie.id, "deadline": DEADLINE,
                                         "proposedSlots": [{"id": "x", "start": "2027-01-02T10:00:00Z", "end": "2027-01-01T10:00:00Z", "timezone": "Europe/Paris"}]}, 400, "créneau fin < début → 400",
        show=lambda j: f"CRÉÉ id={j['id']}")
s, js = sc.step(sophie, "POST", "/api/events", {"title": "Sans créneau", "description": "d", "organizerId": sophie.id, "deadline": DEADLINE, "proposedSlots": []}, (201, 400), "création sans créneau",
                show=lambda j: f"id={j['id']}")
if s == 201:
    status(sc, sophie, js["id"], "POLLING", (400, 409), "DRAFT → POLLING sans aucun créneau → refus attendu")
sc.step(raph, "POST", "/api/events", {"title": "Usurpation", "description": "d", "organizerId": sophie.id, "deadline": DEADLINE, "proposedSlots": slots[:1]}, 201,
        "organizerId du body ignoré (Raphaël devient org.)", check=lambda j: True if j["organizerId"] == raph.id else f"organizerId={j['organizerId']}", show=lambda j: "organizerId = JWT ✔")

# --- vrai événement
ev = create_event(sc, sophie, "Mariage de Sophie & Karim 💍", "Cérémonie laïque + soirée. Merci de voter pour la date qui vous convient !", slots, DEADLINE, "WEDDING",
                  {"minParticipants": 60, "maxParticipants": 120, "expectedParticipants": 90})
E = f"/api/events/{ev}"
sc.step(sophie, "POST", f"{E}/participants", {"eventId": ev, "participantId": karim.id}, 201, "Sophie ajoute Karim")
s, inv = sc.step(sophie, "POST", f"{E}/invite", {"maxUses": 3}, 201, "lien invités maxUses=3", show=lambda j: f"code={j['code']} maxUses={j['maxUses']}")
code = inv["code"]
sc.step(karim, "POST", f"{E}/invite", {"maxUses": 50}, 403, "Karim (co-org.) crée un lien → 403 (pas de rôle co-organisateur)")
for u in (nadia, olivier, pauline):
    sc.step(u, "POST", f"/api/invite/{code}/accept", None, 200, f"{u.name} accepte l'invitation", show=lambda j: j["message"][:50])
sc.step(quentin, "POST", f"/api/invite/{code}/accept", None, 410, "Quentin : quota maxUses atteint → 410")
sc.step(None, "GET", f"/api/invite/{code}", None, 410, "résolution lien épuisé → 410")
sc.step(quentin, "GET", E, None, 404, "Quentin ne voit pas l'événement")
sc.step(karim, "PUT", f"{E}/status", {"eventId": ev, "status": "POLLING"}, 403, "Karim ouvre le sondage → 403")
status(sc, sophie, ev, "POLLING", 200, "DRAFT → POLLING")

pv = {sophie: ("YES", "YES", "MAYBE"), karim: ("YES", "MAYBE", "NO"), nadia: ("NO", "YES", "YES"), olivier: ("MAYBE", "YES", "NO"), pauline: ("YES", "NO", "MAYBE")}
for u, vs in pv.items():
    for sl, v in zip(("mai", "juin", "sept"), vs):
        sc.step(u, "POST", f"{E}/poll/votes", {"eventId": ev, "participantId": u.id, "slotId": sl, "vote": v}, 201, f"{u.name} {v} {sl}")
s, poll = sc.step(sophie, "GET", f"{E}/poll", None, 200, "poll")
scs = score(poll["votes"], ["mai", "juin", "sept"])
# mai 2+2-1+1+2=6 ; juin 2+1+2+2-1=6 ; sept 1-1+2-1+1=2
sc.note("scores mai=6, juin=6, sept=2 (égalité mai/juin)", scs == {"mai": 6, "juin": 6, "sept": 2}, str(scs))
sync_confirm(sc, nadia, ev, "juin", slots[1]["start"], 409, "Nadia (invitée) tente de confirmer → rejet")
sync_confirm(sc, karim, ev, "juin", slots[1]["start"], 409, "Karim (co-org.) tente de confirmer → rejet")
sync_confirm(sc, sophie, ev, "juin", "2027-06-12T13:00:00Z", 409, "confirmation avec finalDate ≠ début créneau → rejet")
sync_confirm(sc, sophie, ev, "juin", slots[1]["start"], 200, "Sophie tranche l'égalité : juin (0 NO éliminatoire côté mariés)")
sc.note("statut CONFIRMED", get_status(sophie, ev) == "CONFIRMED", get_status(sophie, ev))

for u in (sophie, karim, nadia, olivier):
    sc.step(u, "POST", f"{E}/participants/{u.id}/rsvp", {"slotId": "juin", "attendance": "CONFIRMED"}, 200, f"RSVP {u.name} CONFIRMED", show=lambda j: j["rsvpState"])
sc.step(pauline, "POST", f"{E}/participants/{pauline.id}/rsvp", {"slotId": "juin", "attendance": "TENTATIVE"}, 200, "Pauline peut-être", show=lambda j: j["rsvpState"])
sc.step(pauline, "GET", f"{E}/budget", None, 403, "Pauline (TENTATIVE) lit le budget → 403")
sc.step(pauline, "POST", f"{E}/participants/{pauline.id}/rsvp", {"slotId": "juin", "attendance": "CONFIRMED"}, 200, "Pauline confirme finalement", show=lambda j: j["rsvpState"])
sc.step(sophie, "POST", f"{E}/participants/{pauline.id}/rsvp", {"slotId": "juin", "attendance": "CONFIRMED"}, 200, "Sophie (org.) met à jour le RSVP de Pauline")

# régime alimentaire
sc.step(olivier, "POST", f"{E}/dietary-restrictions", {"participantId": olivier.id, "eventId": ev, "restriction": "GLUTEN_FREE", "notes": "cœliaque"}, 201, "Olivier sans gluten")
sc.step(nadia, "POST", f"{E}/dietary-restrictions", {"participantId": nadia.id, "eventId": ev, "restriction": "HALAL"}, 201, "Nadia halal")
sc.step(olivier, "POST", f"{E}/dietary-restrictions", {"participantId": nadia.id, "eventId": ev, "restriction": "VEGAN"}, 403, "Olivier déclare pour Nadia → 403")
sc.step(nadia, "POST", f"{E}/dietary-restrictions", {"participantId": nadia.id, "eventId": ev, "restriction": "PALEO"}, 400, "restriction inconnue → 400")
sc.step(sophie, "GET", f"{E}/dietary-restrictions/counts", None, 200, "comptage régimes (traiteur)", show=lambda j: json.dumps(j)[:100])
sc.step(raph, "GET", f"{E}/dietary-restrictions", None, 403, "Raphaël lit les régimes (donnée santé) → 403")

# lieux
base = {"eventId": ev, "dateOrPeriod": "12 juin 2027", "duration": 2, "estimatedParticipants": 90}
s, v1 = sc.step(sophie, "POST", f"{E}/scenarios", dict(base, name="Domaine de la Bergerie", location="Anjou", estimatedBudgetPerPerson=180.0, description="Grange + parc"), 201, "lieu Domaine de la Bergerie")
s, v2 = sc.step(sophie, "POST", f"{E}/scenarios", dict(base, name="Château de Vallery", location="Yonne", estimatedBudgetPerPerson=240.0, description="Château Renaissance"), 201, "lieu Château de Vallery")
sc.step(karim, "POST", f"{E}/scenarios", dict(base, name="Salle des fêtes", location="Paris", estimatedBudgetPerPerson=50.0, description="x"), 403, "Karim ajoute un lieu → 403")
for u, (a, b) in {sophie: ("PREFER", "NEUTRAL"), karim: ("PREFER", "PREFER"), nadia: ("NEUTRAL", "AGAINST"), olivier: ("PREFER", "NEUTRAL"), pauline: ("AGAINST", "PREFER")}.items():
    sc.step(u, "POST", f"/api/scenarios/{v1['id']}/vote", {"participantId": u.id, "vote": a}, 201, f"{u.name} {a} Bergerie")
    sc.step(u, "POST", f"/api/scenarios/{v2['id']}/vote", {"participantId": u.id, "vote": b}, 201, f"{u.name} {b} Vallery")
sc.step(nadia, "PUT", f"/api/scenarios/{v1['id']}", {"eventId": ev, "name": "Hacké", "dateOrPeriod": "x", "location": "x", "duration": 1, "estimatedParticipants": 1, "estimatedBudgetPerPerson": 1.0, "description": "x"},
        403, "Nadia modifie un scénario → 403")
sc.step(nadia, "DELETE", f"/api/scenarios/{v2['id']}", None, 403, "Nadia supprime un scénario → 403")
sc.step(karim, "POST", f"{E}/scenarios/{v1['id']}/select-final", None, 403, "Karim select-final → 403")
sc.step(sophie, "POST", f"{E}/scenarios/{v1['id']}/select-final", None, 200, "Sophie choisit la Bergerie")
status(sc, sophie, ev, "ORGANIZING", 200, "COMPARING → ORGANIZING")

# hébergement + chambres
acc = {"eventId": ev, "name": "Hôtel du Parc – bloc mariage", "type": "HOTEL", "address": "2 av. du Parc, Angers", "capacity": 20, "pricePerNight": 11000, "totalNights": 2,
       "bookingStatus": "CONFIRMED", "bookingUrl": "https://www.booking.com/hotel/fr/parc.html", "checkInDate": "2027-06-12", "checkOutDate": "2027-06-14"}
s, h = sc.step(sophie, "POST", f"{E}/accommodation", acc, 201, "hôtel bloc 20 pers. CONFIRMED", show=lambda j: f"totalCost={j['totalCost']}")
sc.step(karim, "POST", f"{E}/accommodation", acc, 403, "Karim crée hébergement → 403")
if s == 201:
    sc.step(sophie, "POST", f"{E}/accommodation/{h['id']}/rooms", {"accommodationId": h["id"], "roomNumber": "101", "capacity": 2, "assignedParticipants": [nadia.id, olivier.id]}, 201, "chambre 101 Nadia+Olivier")
    sc.step(sophie, "POST", f"{E}/accommodation/{h['id']}/rooms", {"accommodationId": h["id"], "roomNumber": "102", "capacity": 1, "assignedParticipants": [pauline.id, karim.id]}, 400, "chambre sur-capacité (2 pers./1 place) → 400")
    sc.step(sophie, "POST", f"{E}/accommodation/{h['id']}/rooms", {"accommodationId": h["id"], "roomNumber": "103", "capacity": 2, "assignedParticipants": [raph.id]}, 400, "chambre avec non-participant → 400")
    sc.step(nadia, "PUT", f"{E}/accommodation/{h['id']}", dict(acc, bookingStatus="CANCELLED"), 403, "Nadia annule l'hôtel → 403")
    sc.step(nadia, "GET", f"{E}/accommodation/{h['id']}/rooms", None, 200, "Nadia voit les chambres", show=lambda j: json.dumps(j)[:90])

# budget
t = iso(now_utc())
budget = {"id": "x", "eventId": ev, "totalEstimated": 25000.0, "totalActual": 0.0, "transportEstimated": 800.0, "transportActual": 0.0, "accommodationEstimated": 4400.0,
          "accommodationActual": 0.0, "mealsEstimated": 12000.0, "mealsActual": 0.0, "activitiesEstimated": 3000.0, "activitiesActual": 0.0, "equipmentEstimated": 1800.0,
          "equipmentActual": 0.0, "otherEstimated": 3000.0, "otherActual": 0.0, "createdAt": t, "updatedAt": t}
sc.step(karim, "PUT", f"{E}/budget", budget, 403, "Karim fixe la baseline → 403")
sc.step(sophie, "PUT", f"{E}/budget", budget, 200, "baseline 25 000 €", show=lambda j: f"total={j['totalEstimated']}")
sc.step(sophie, "PUT", f"{E}/budget", dict(budget, totalEstimated=-5.0), 400, "baseline négative → 400", show=lambda j: f"ACCEPTÉ total={j['totalEstimated']}")
sc.step(sophie, "PUT", f"{E}/budget", budget, 200, "restauration baseline 25 000 €")
everyone = [sophie.id, karim.id, nadia.id, olivier.id, pauline.id]
items = [(sophie, "Location domaine", "ACCOMMODATION", 6500.0), (sophie, "Traiteur (90 couverts)", "MEALS", 9900.0), (sophie, "Pièce montée", "MEALS", 650.0),
         (sophie, "DJ + sono", "ACTIVITIES", 1400.0), (sophie, "Photographe", "OTHER", 2200.0), (sophie, "Fleuriste", "OTHER", 1100.0),
         (sophie, "Navette gare ↔ domaine", "TRANSPORT", 780.0), (sophie, "Location vaisselle", "EQUIPMENT", 540.0), (karim, "Faire-part", "OTHER", 320.0),
         (karim, "Alliances", "OTHER", 1900.0), (nadia, "Cadeau commun des témoins", "OTHER", 300.0), (olivier, "Photobooth", "ACTIVITIES", 450.0)]
item_ids = {}
for u, n, c, cost in items:
    s, j = sc.step(u, "POST", f"{E}/budget/items", {"name": n, "category": c, "estimatedCost": cost, "sharedBy": everyone}, 201, f"{u.name}: {n} ({cost:.0f} €)", show=lambda j: f"id ok")
    if s == 201:
        item_ids[n] = j["id"]
s, lst = sc.step(nadia, "GET", f"{E}/budget/items", None, 200, "liste items", show=lambda j: f"{len(j) if isinstance(j, list) else len(j.get('items', []))} items")
n_items = len(lst) if isinstance(lst, list) else len(lst.get("items", [])) if s == 200 else -1
sc.note("12 items persistés", n_items == 12, str(n_items))
tid = item_ids.get("Traiteur (90 couverts)")
if tid:
    s, it = sc.step(olivier, "GET", f"{E}/budget/items/{tid}", None, 200, "lecture item traiteur", show=lambda j: f"cost={j.get('estimatedCost')}")
    sc.step(olivier, "PUT", f"{E}/budget/items/{tid}", dict(it, estimatedCost=1.0), 403, "Olivier modifie l'item de Sophie → 403")
    sc.step(nadia, "DELETE", f"{E}/budget/items/{tid}", None, 403, "Nadia supprime l'item de Sophie → 403")
sc.step(karim, "POST", f"{E}/budget/expenses", {"amount": 1900.0, "category": "OTHER", "payerId": karim.id, "splitParticipantIds": [sophie.id, karim.id]}, 201, "Karim avance les alliances (dépense)",
        show=lambda j: json.dumps(j)[:90])
sc.step(nadia, "POST", f"{E}/budget/expenses", {"amount": 300.0, "category": "OTHER", "payerId": olivier.id, "splitParticipantIds": everyone}, 403, "Nadia déclare une dépense payée par Olivier → 403")
s, stl = sc.step(sophie, "GET", f"{E}/budget/settlements", None, 200, "équilibrages", show=lambda j: json.dumps(j)[:110])
sc.note("dépense avancée par Karim (1900 € / Sophie+Karim) → Sophie doit 950 € à Karim", s == 200 and any(x.get("fromParticipantId") == sophie.id and x.get("toParticipantId") == karim.id for x in stl.get("settlements", [])),
        f"settlements={stl.get('count') if s == 200 else '-'}")
sc.step(sophie, "GET", f"{E}/budget/summary", None, 200, "résumé budget")
sc.step(raph, "GET", f"{E}/budget/items", None, 403, "Raphaël lit le budget → 403")

# réunion traiteur, Tricount, commentaires
sc.step(sophie, "POST", f"{E}/meetings/persisted", {"platform": "FACETIME", "title": "Dégustation traiteur (visio)", "startTime": "2027-03-10T18:00:00Z", "duration": "1h"}, 201, "visio traiteur",
        show=lambda j: f"{j['status']}")
sc.step(karim, "POST", f"{E}/meetings/persisted", {"platform": "FACETIME", "title": "x", "startTime": "2027-03-10T18:00:00Z"}, 403, "Karim crée une réunion → 403")
sc.step(sophie, "POST", f"{E}/transport/not-needed", None, 200, "transport : navette gérée via budget")
sc.step(sophie, "POST", f"{E}/payment/tricount/link", {"provider": "TRICOUNT", "providerId": "mariage-sk", "providerUrl": "https://tricount.com/mariage-sk", "syncStatus": "SYNCED"}, 201, "Tricount")
sc.step(karim, "POST", f"{E}/payment/tricount/link", {"provider": "TRICOUNT", "providerId": "k", "providerUrl": "https://tricount.com/k", "syncStatus": "SYNCED"}, 403, "Karim relie Tricount → 403")
sc.step(pauline, "POST", f"{E}/comments", {"section": "ACCOMMODATION", "content": "Je partage la chambre avec qui ?", "authorId": pauline.id, "authorName": "x"}, 201, "Pauline commente (hébergement)")
sc.step(karim, "GET", f"{E}/readiness", None, 403, "Karim lit la readiness → 403")
sc.step(karim, "PUT", f"{E}/status", {"eventId": ev, "status": "FINALIZED"}, 403, "Karim finalise → 403")
s, rd = sc.step(sophie, "GET", f"{E}/readiness", None, 200, "readiness", show=lambda j: f"complete={j['complete']} blockers={j['blockers']}")
status(sc, sophie, ev, "FINALIZED", 200, "ORGANIZING → FINALIZED")
sc.step(nadia, "POST", f"{E}/dietary-restrictions", {"participantId": nadia.id, "eventId": ev, "restriction": "VEGAN"}, (201, 409), "régime après finalisation (info traiteur)")
sc.step(nadia, "GET", f"{E}/calendar/ics", None, 200, "ICS mariage (Nadia)", show=lambda j: "ok")

sc.users = {u.name: (u.id, u.token) for u in (sophie, karim, nadia, olivier, pauline, quentin, raph)}; sc.ev = ev
pickle.dump(sc, open("/tmp/wakeve-qa/api/s3.pkl", "wb"))
print("EVENT", ev, sc.summary())
