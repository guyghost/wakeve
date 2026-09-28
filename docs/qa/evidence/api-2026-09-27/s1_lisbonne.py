from lib import *
import json, pickle

sc = Scenario("Scénario 1 — Road trip à Lisbonne (Alice org., Bruno, Chloé, David + Eve intruse)")
alice, bruno, chloe, david, eve = (User(n) for n in ["Alice", "Bruno", "Chloé", "David", "Eve"])
S1 = {"id": "slot-oct-a", "start": "2026-10-10T07:00:00Z", "end": "2026-10-17T19:00:00Z", "timezone": "Europe/Lisbon", "timeOfDay": "SPECIFIC"}
S2 = {"id": "slot-oct-b", "start": "2026-10-17T07:00:00Z", "end": "2026-10-24T19:00:00Z", "timezone": "Europe/Lisbon", "timeOfDay": "SPECIFIC"}
DEADLINE = "2026-10-05T22:00:00Z"
ev = create_event(sc, alice, "Road trip à Lisbonne 🇵🇹", "Une semaine en octobre entre amis : Alfama, pastéis de nata et surf à Ericeira.",
                  [S1, S2], DEADLINE, "OTHER", {"minParticipants": 3, "maxParticipants": 6, "expectedParticipants": 4})
E = f"/api/events/{ev}"

# --- DRAFT: participants
for u in (bruno, chloe, david):
    sc.step(alice, "POST", f"{E}/participants", {"eventId": ev, "participantId": u.id}, 201, f"ajout {u.name} (DRAFT)")
sc.step(bruno, "POST", f"{E}/participants", {"eventId": ev, "participantId": eve.id}, 403, "Bruno ajoute un participant → 403")
sc.step(alice, "POST", f"{E}/participants", {"eventId": ev, "participantId": bruno.id}, 400, "doublon participant → 400")
sc.step(eve, "GET", E, None, 404, "Eve (intruse) lit l'événement → 404")
sc.step(eve, "GET", f"{E}/participants", None, 403, "Eve liste participants → 403")
sc.step(bruno, "PUT", f"{E}/status", {"eventId": ev, "status": "POLLING"}, 403, "Bruno ouvre le sondage → 403")
status(sc, alice, ev, "POLLING", 200, "DRAFT → POLLING", )

# --- votes (YES=2 MAYBE=1 NO=-1)
plan = {alice: {"slot-oct-a": "YES", "slot-oct-b": "YES"},
        bruno: {"slot-oct-a": "YES", "slot-oct-b": "NO"},
        chloe: {"slot-oct-a": "MAYBE", "slot-oct-b": "YES"},
        david: {"slot-oct-a": "YES", "slot-oct-b": "MAYBE"}}
for u, vs in plan.items():
    for slot, v in vs.items():
        sc.step(u, "POST", f"{E}/poll/votes", {"eventId": ev, "participantId": u.id, "slotId": slot, "vote": v}, 201, f"{u.name} vote {v} sur {slot}")
sc.step(eve, "POST", f"{E}/poll/votes", {"eventId": ev, "participantId": eve.id, "slotId": "slot-oct-a", "vote": "YES"}, 403, "Eve vote → 403")
sc.step(bruno, "POST", f"{E}/poll/votes", {"eventId": ev, "participantId": david.id, "slotId": "slot-oct-a", "vote": "NO"}, 403, "Bruno vote à la place de David → 403")
sc.step(bruno, "POST", f"{E}/poll/votes", {"eventId": ev, "participantId": bruno.id, "slotId": "slot-oct-a", "vote": "PEUT-ETRE"}, 400, "vote invalide → 400")
sc.step(bruno, "POST", f"{E}/poll/votes", {"eventId": ev, "participantId": bruno.id, "slotId": "slot-inexistant", "vote": "YES"}, 400, "slot inconnu → 400")
sc.step(bruno, "POST", f"{E}/poll/votes", None, 400, "payload malformé → 400", raw_body='{"vote":')
sc.step(alice, "POST", f"{E}/poll/votes", {"eventId": ev, "participantId": eve.id, "slotId": "slot-oct-a", "vote": "YES"}, 400, "Alice vote pour Eve (non membre) → 400")
s, poll = sc.step(chloe, "GET", f"{E}/poll", None, 200, "Chloé lit le sondage", show=lambda j: json.dumps({k[-6:]: v for k, v in j['votes'].items()}))
sc.step(eve, "GET", f"{E}/poll", None, 403, "Eve lit le sondage → 403")
scores = score(poll["votes"], ["slot-oct-a", "slot-oct-b"]) if s == 200 else {}
sc.note("scores attendus A=7 (2+2+1+2), B=4 (2-1+2+1)", scores == {"slot-oct-a": 7, "slot-oct-b": 4}, str(scores))
sc.note("4 votants × 2 créneaux dans le poll", s == 200 and len(poll["votes"]) == 4 and all(len(v) == 2 for v in poll["votes"].values()), f"{len(poll['votes']) if s == 200 else '-'} votants")

# --- confirmation
status(sc, alice, ev, "CONFIRMED", 409, "PUT CONFIRMED (voie legacy) → 409 attendu", slotId="slot-oct-a")
status(sc, alice, ev, "ORGANIZING", 409, "saut POLLING → ORGANIZING → 409")
sync_confirm(sc, bruno, ev, "slot-oct-a", S1["start"], (200, 409), "Bruno confirme via sync (non org.) → rejet")
sc.note("statut toujours POLLING après tentative Bruno", get_status(alice, ev) == "POLLING", get_status(alice, ev))
sync_confirm(sc, alice, ev, "slot-oct-a", S1["start"], 200, "Alice confirme créneau A via /api/sync", title="Road trip à Lisbonne 🇵🇹", deadline=DEADLINE)
sc.note("statut = CONFIRMED", get_status(alice, ev) == "CONFIRMED", get_status(alice, ev))
sc.step(chloe, "POST", f"{E}/poll/votes", {"eventId": ev, "participantId": chloe.id, "slotId": "slot-oct-a", "vote": "YES"}, 400, "vote après confirmation → 400")
s, js = sc.step(alice, "GET", E, None, 200, "finalDate après confirmation", show=lambda j: f"status={j['status']} finalDate={j.get('finalDate')}")
sc.note("finalDate renseignée = début créneau A", s == 200 and js.get("finalDate") == S1["start"], f"finalDate={js.get('finalDate') if s == 200 else '-'}")

# --- RSVP
for u in (alice, bruno, chloe, david):
    sc.step(u, "POST", f"{E}/participants/{u.id}/rsvp", {"slotId": "slot-oct-a", "attendance": "CONFIRMED"}, 200, f"RSVP {u.name} CONFIRMED",
            show=lambda j: f"{j['rsvpState']}/{j['dateValidationState']}")
sc.step(bruno, "POST", f"{E}/participants/{david.id}/rsvp", {"slotId": "slot-oct-a", "attendance": "DECLINED"}, 403, "Bruno RSVP pour David → 403")
sc.step(chloe, "POST", f"{E}/participants/{chloe.id}/rsvp", {"slotId": "slot-oct-b", "attendance": "CONFIRMED"}, 400, "RSVP mauvais créneau → 400")
sc.step(chloe, "POST", f"{E}/participants/{chloe.id}/rsvp", {"slotId": "slot-oct-a", "attendance": "OUI"}, 400, "RSVP attendance invalide → 400")

# --- scénarios destination/logement
base = {"eventId": ev, "dateOrPeriod": "10-17 oct. 2026", "location": "Lisbonne, Portugal", "duration": 7, "estimatedParticipants": 4}
s, a = sc.step(alice, "POST", f"{E}/scenarios", dict(base, name="Airbnb Alfama – vue Tage", estimatedBudgetPerPerson=420.0, description="Appartement 3 ch. dans l'Alfama"), 201, "scénario Airbnb Alfama", show=lambda j: j["id"][:20])
s, b = sc.step(alice, "POST", f"{E}/scenarios", dict(base, name="Airbnb Bairro Alto – rooftop", estimatedBudgetPerPerson=380.0, description="Loft avec rooftop"), 201, "scénario Airbnb Bairro Alto", show=lambda j: j["id"][:20])
sc.step(bruno, "POST", f"{E}/scenarios", dict(base, name="Camping", estimatedBudgetPerPerson=50.0, description="x"), 403, "Bruno crée un scénario → 403")
sc.step(alice, "POST", f"{E}/scenarios", None, 400, "scénario payload incomplet → 400", raw_body=json.dumps({"eventId": ev, "name": "x"}))
A, B = a["id"], b["id"]
sc.note("statut passe auto à COMPARING à la création des scénarios", get_status(alice, ev) == "COMPARING", get_status(alice, ev))
votes = {alice: ("PREFER", "NEUTRAL"), bruno: ("PREFER", "AGAINST"), chloe: ("NEUTRAL", "PREFER"), david: ("PREFER", "NEUTRAL")}
for u, (va, vb) in votes.items():
    sc.step(u, "POST", f"/api/scenarios/{A}/vote", {"participantId": u.id, "vote": va}, 201, f"{u.name} {va} Alfama")
    sc.step(u, "POST", f"/api/scenarios/{B}/vote", {"participantId": u.id, "vote": vb}, 201, f"{u.name} {vb} Bairro Alto")
sc.step(chloe, "POST", f"/api/scenarios/{A}/vote", {"participantId": chloe.id, "vote": "PREFER"}, 201, "Chloé change NEUTRAL→PREFER Alfama")
sc.step(eve, "POST", f"/api/scenarios/{A}/vote", {"participantId": eve.id, "vote": "PREFER"}, 403, "Eve vote scénario → 403")
sc.step(bruno, "POST", f"/api/scenarios/{A}/vote", {"participantId": bruno.id, "vote": "LOVE"}, 400, "vote scénario invalide → 400")
s, ra = sc.step(bruno, "GET", f"/api/scenarios/{A}/votes", None, 200, "résultats Alfama", show=lambda j: f"P{j['preferCount']}/N{j['neutralCount']}/A{j['againstCount']} score={j['score']}")
s2, rb = sc.step(bruno, "GET", f"/api/scenarios/{B}/votes", None, 200, "résultats Bairro Alto", show=lambda j: f"P{j['preferCount']}/N{j['neutralCount']}/A{j['againstCount']} score={j['score']}")
sc.note("Alfama: 4 PREFER (vote de Chloé mis à jour, pas dupliqué)", s == 200 and ra["preferCount"] == 4 and ra["totalVotes"] == 4, f"{ra.get('preferCount')}P total={ra.get('totalVotes')}")
sc.note("Alfama score > Bairro Alto score", s == 200 and s2 == 200 and ra["score"] > rb["score"], f"{ra.get('score')} vs {rb.get('score')}")
sc.step(eve, "GET", f"{E}/scenarios", None, 403, "Eve liste scénarios → 403")
sc.step(bruno, "POST", f"{E}/scenarios/{A}/select-final", None, 403, "Bruno select-final → 403")
sc.step(alice, "POST", f"{E}/scenarios/{A}/select-final", None, 200, "Alice sélectionne Alfama")
s, lst = sc.step(chloe, "GET", f"{E}/scenarios", None, 200, "statuts scénarios", show=lambda j: ",".join(x["status"] for x in j["scenarios"]))
sc.note("Alfama SELECTED, Bairro Alto REJECTED", s == 200 and {x["id"]: x["status"] for x in lst["scenarios"]} == {A: "SELECTED", B: "REJECTED"}, "")

# --- workflow
status(sc, alice, ev, "ORGANIZING", 200, "COMPARING → ORGANIZING")
sc.note("statut = ORGANIZING", get_status(alice, ev) == "ORGANIZING", get_status(alice, ev))

# --- hébergement
acc = {"eventId": ev, "name": "Airbnb Alfama – vue Tage", "type": "AIRBNB", "address": "Rua de São Miguel 12, Lisboa", "capacity": 4,
       "pricePerNight": 16000, "totalNights": 7, "bookingStatus": "CONFIRMED", "bookingUrl": "https://www.airbnb.fr/rooms/123456",
       "checkInDate": "2026-10-10", "checkOutDate": "2026-10-17", "notes": "Code boîte à clés envoyé la veille"}
s, accj = sc.step(alice, "POST", f"{E}/accommodation", acc, 201, "hébergement Airbnb CONFIRMED", show=lambda j: f"totalCost={j.get('totalCost')} status={j.get('bookingStatus')}")
sc.note("totalCost = 7 × 16000 = 112000 cts", s == 201 and accj.get("totalCost") == 112000, f"{accj.get('totalCost') if s == 201 else '-'}")
sc.step(bruno, "POST", f"{E}/accommodation", acc, 403, "Bruno crée hébergement → 403")
sc.step(alice, "POST", f"{E}/accommodation", None, 400, "hébergement payload invalide → 400", raw_body=json.dumps({"eventId": ev, "name": "x"}))
sc.step(alice, "POST", f"{E}/accommodation", dict(acc, capacity=-2, checkOutDate="2026-10-01"), 400, "hébergement capacité<0 / checkout<checkin → 400")
sc.step(david, "GET", f"{E}/accommodation", None, 200, "David (confirmé) lit hébergements", show=lambda j: f"{len(j)} hébergement(s)")

# --- budget
t = iso(now_utc())
budget = {"id": "b-client", "eventId": ev, "totalEstimated": 2400.0, "totalActual": 0.0, "transportEstimated": 600.0, "transportActual": 0.0,
          "accommodationEstimated": 1120.0, "accommodationActual": 0.0, "mealsEstimated": 480.0, "mealsActual": 0.0, "activitiesEstimated": 200.0,
          "activitiesActual": 0.0, "equipmentEstimated": 0.0, "equipmentActual": 0.0, "otherEstimated": 0.0, "otherActual": 0.0, "createdAt": t, "updatedAt": t}
s, bj = sc.step(alice, "PUT", f"{E}/budget", budget, 200, "baseline budget 2400 € (1er PUT)", show=lambda j: f"totalEstimated={j.get('totalEstimated')}")
sc.note("baseline conservée au 1er PUT (régression BUG-6)", s == 200 and bj.get("totalEstimated") == 2400.0, f"{bj.get('totalEstimated') if s == 200 else '-'}")
sc.step(bruno, "PUT", f"{E}/budget", budget, 403, "Bruno modifie la baseline → 403")
sc.step(alice, "PUT", f"{E}/budget", {"eventId": ev, "totalEstimated": 10}, 400, "baseline incomplète → 400 (régression BUG-7)")
items = [(alice, "Essence + péages Paris→Lisbonne", "TRANSPORT", 520.0), (bruno, "Airbnb Alfama 7 nuits", "ACCOMMODATION", 1120.0),
         (chloe, "Courses & pastéis", "MEALS", 300.0), (david, "Cours de surf Ericeira", "ACTIVITIES", 160.0)]
all_ids = [alice.id, bruno.id, chloe.id, david.id]
for u, name, cat, cost in items:
    sc.step(u, "POST", f"{E}/budget/items", {"name": name, "category": cat, "estimatedCost": cost, "sharedBy": all_ids}, 201, f"{u.name} ajoute « {name} »",
            show=lambda j: f"id={str(j.get('id'))[:12]} cost={j.get('estimatedCost')}")
sc.step(eve, "POST", f"{E}/budget/items", {"name": "x", "category": "OTHER", "estimatedCost": 1.0}, (403, 404), "Eve ajoute item → 403/404")
sc.step(alice, "POST", f"{E}/budget/items", {"name": "neg", "category": "OTHER", "estimatedCost": -50.0, "sharedBy": all_ids}, 400, "item coût négatif → 400")
sc.step(alice, "POST", f"{E}/budget/items", {"name": "cat", "category": "BIJOUX", "estimatedCost": 5.0}, 400, "item catégorie invalide → 400")
s, summ = sc.step(chloe, "GET", f"{E}/budget/summary", None, 200, "résumé budget", show=lambda j: json.dumps(j)[:110])
sc.step(chloe, "GET", f"{E}/budget/statistics", None, 200, "statistiques budget", show=lambda j: json.dumps(j)[:110])
sc.step(chloe, "GET", f"{E}/budget/participants/{chloe.id}", None, 200, "part de Chloé", show=lambda j: json.dumps(j)[:110])
s, st = sc.step(chloe, "GET", f"{E}/budget/settlements", None, 200, "équilibrage (settlements)", show=lambda j: json.dumps(j)[:110])

# --- transport
for u, city in ((bruno, "Paris"), (chloe, "Lyon")):
    sc.step(u, "PUT", f"{E}/transport/departures/{u.id}", {"location": {"name": city}}, 200, f"{u.name} départ {city}")
sc.step(bruno, "PUT", f"{E}/transport/departures/{chloe.id}", {"location": {"name": "Nice"}}, 403, "Bruno modifie départ de Chloé → 403")
sc.step(alice, "PUT", f"{E}/transport/departures/{david.id}", None, 400, "départ payload invalide → 400", raw_body='{"location":{}}')
s, plan_ = sc.step(alice, "POST", f"{E}/transport/plans/generate", {"optimizationType": "BALANCED"}, (201, 409), "génération plan transport (road trip)",
                   show=lambda j: f"plan={j.get('id')} cost={j.get('totalGroupCost')}")
if s == 201:
    sc.step(alice, "POST", f"{E}/transport/plans/{plan_['id']}/select", None, 200, "sélection plan transport", show=lambda j: json.dumps(j)[:90])
else:
    sc.step(bruno, "POST", f"{E}/transport/not-needed", None, 200, "Bruno (confirmé) : transport géré à part (not-needed)")

# --- réunion + rappel
s, m = sc.step(alice, "POST", f"{E}/meetings/persisted", {"platform": "GOOGLE_MEET", "title": "Brief road trip", "startTime": "2026-10-06T18:00:00Z", "duration": "30m"}, 201,
               "réunion de préparation (Meet)", show=lambda j: f"{j['status']} {j['meetingLink']}")
sc.step(bruno, "POST", f"{E}/meetings/persisted", {"platform": "ZOOM", "title": "x", "startTime": "2026-10-06T18:00:00Z"}, 403, "Bruno crée réunion → 403")
sc.step(alice, "POST", f"{E}/meetings/persisted", {"platform": "GOOGLE_MEET", "title": "Brief bis", "startTime": "mardi soir"}, 400, "réunion startTime invalide → 400")
sc.step(david, "GET", f"{E}/meetings/persisted", None, 200, "David liste les réunions", show=lambda j: f"{len(j['meetings'])} réunion(s)")
sc.step(alice, "POST", f"{E}/calendar/reminders/one_day_before", None, (200, 201, 202), "rappel J-1 via calendrier")

# --- tricount
sc.step(alice, "POST", f"{E}/payment/tricount/link", {"provider": "TRICOUNT", "providerId": "lisbonne2026", "providerUrl": "https://tricount.com/fr/lisbonne2026", "syncStatus": "LINKED"}, 201, "lien Tricount")
sc.step(alice, "POST", f"{E}/payment/tricount/link", {"provider": "TRICOUNT", "providerId": "x", "providerUrl": "https://tricount.com.evil.io/x", "syncStatus": "LINKED"}, 422, "URL Tricount piégée → 422")

# --- comments
for u, txt in ((bruno, "Je m'occupe de la playlist 🎶"), (chloe, "Qui a une glacière ?")):
    sc.step(u, "POST", f"{E}/comments", {"section": "GENERAL", "content": txt, "authorId": u.id, "authorName": "X"}, (201, 202), f"commentaire {u.name}",
            show=lambda j: f"author={j.get('authorName')}")
sc.step(eve, "POST", f"{E}/comments", {"section": "GENERAL", "content": "spam", "authorId": eve.id, "authorName": "Eve"}, 403, "Eve commente → 403")
sc.step(eve, "GET", f"{E}/comments", None, 403, "Eve lit les commentaires → 403", show=lambda j: f"{len(j) if isinstance(j, list) else j} commentaire(s) visibles")

# --- readiness + finalize
s, rd = sc.step(alice, "GET", f"{E}/readiness", None, 200, "checklist finalisation", show=lambda j: f"complete={j['complete']} blockers={j['blockers']}")
sc.step(bruno, "GET", f"{E}/readiness", None, 403, "Bruno lit readiness → 403")
sc.step(bruno, "PUT", f"{E}/status", {"eventId": ev, "status": "FINALIZED"}, 403, "Bruno finalise → 403")
status(sc, alice, ev, "FINALIZED", 200, "ORGANIZING → FINALIZED")
sc.note("statut = FINALIZED", get_status(alice, ev) == "FINALIZED", get_status(alice, ev))
status(sc, alice, ev, "POLLING", (403, 409), "réouverture après FINALIZED → refus")
sc.step(alice, "POST", f"{E}/budget/items", {"name": "après coup", "category": "OTHER", "estimatedCost": 5.0, "sharedBy": all_ids}, (403, 409), "item budget après FINALIZED → refus")
sc.step(alice, "POST", f"{E}/meetings/persisted", {"platform": "FACETIME", "title": "Débrief", "startTime": "2026-10-20T18:00:00Z"}, (403, 409), "réunion après FINALIZED → refus (read-only)")

# --- ICS
s, ics = sc.step(david, "GET", f"{E}/calendar/ics", None, 200, "ICS (David)", show=lambda j: str(j)[:60].replace("\r\n", " "))
if s == 200:
    t_ = ics if isinstance(ics, str) else json.dumps(ics)
    sc.note("ICS: DTSTART = 20261010T070000Z", "20261010T070000Z" in t_ or "DTSTART;TZID=Europe/Lisbon:20261010T080000" in t_, [l for l in t_.splitlines() if l.startswith("DTSTART")].__str__())
    sc.note("ICS: TRIGGER VALARM conforme RFC5545", all(("TRIGGER" not in l) or __import__("re").match(r"^TRIGGER(;[^:]*)?:-?P(\d+W|(\d+D)?(T(\d+H)?(\d+M)?(\d+S)?)?)$", l) for l in t_.splitlines()),
            str([l for l in t_.splitlines() if "TRIGGER" in l]))
    sc.note("ICS: SUMMARY contient le titre", "Lisbonne" in t_, "")
    open("/tmp/wakeve-qa/api/s1.ics", "w").write(t_)
sc.step(eve, "GET", f"{E}/calendar/ics", None, 403, "Eve télécharge l'ICS → 403")
s, js = sc.step(alice, "POST", f"{E}/calendar/ics", {"invitees": ["bruno@example.com", "chloe@example.com"]}, 200, "ICS avec invités (POST)", show=lambda j: j.get("filename"))
if s == 200:
    sc.note("ICS POST: invités e-mail présents en ATTENDEE", "bruno@example.com" in js["content"], str([l for l in js["content"].splitlines() if "ATTENDEE" in l])[:110])

sc.users = {u.name: (u.id, u.token) for u in (alice, bruno, chloe, david, eve)}; sc.ev = ev
pickle.dump(sc, open("/tmp/wakeve-qa/api/s1.pkl", "wb"))
print("EVENT", ev, sc.summary())
