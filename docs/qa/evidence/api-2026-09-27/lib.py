"""Tiny multi-user QA harness for the Wakeve Ktor API (http://localhost:8080)."""
import json, time, uuid, urllib.request, urllib.error, datetime

BASE = "http://localhost:8080"
RUN = uuid.uuid4().hex[:6]


def iso(dt):
    return dt.strftime("%Y-%m-%dT%H:%M:%SZ")


def now_utc():
    return datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0)


def raw(method, path, token=None, body=None, raw_body=None, headers=None):
    url = BASE + path
    data = None
    h = {"Accept": "application/json"}
    if headers:
        h.update(headers)
    if raw_body is not None:
        data = raw_body.encode() if isinstance(raw_body, str) else raw_body
        h["Content-Type"] = "application/json"
    elif body is not None:
        data = json.dumps(body).encode()
        h["Content-Type"] = "application/json"
    if token:
        h["Authorization"] = "Bearer " + token
    for attempt in range(12):
        req = urllib.request.Request(url, data=data, method=method, headers=h)
        try:
            with urllib.request.urlopen(req, timeout=30) as r:
                status, txt = r.status, r.read().decode("utf-8", "replace")
        except urllib.error.HTTPError as e:
            status, txt = e.code, e.read().decode("utf-8", "replace")
            if status == 429:
                wait = int(e.headers.get("Retry-After") or 10)
                time.sleep(min(max(wait, 2), 65))
                continue
        try:
            js = json.loads(txt) if txt else None
        except Exception:
            js = txt
        return status, js, txt
    return 429, None, "rate limited"


class User:
    def __init__(self, name):
        self.name = name
        s, js, _ = raw("POST", "/api/auth/guest", body={"deviceId": f"qa-{RUN}-{name}-{uuid.uuid4().hex[:6]}"})
        assert s == 200, (s, js)
        self.token = js["accessToken"]
        self.id = js["user"]["id"]
        raw("PUT", "/api/user/display-name", token=self.token, body={"displayName": name})

    def __repr__(self):
        return self.name


def gist(js, txt, n=110):
    if isinstance(js, dict):
        for k in ("error", "message", "status"):
            if k in js and isinstance(js[k], str):
                return f"{k}={js[k]}"[:n]
    s = txt.replace("\n", " ")
    s = " ".join(s.split())
    return s[:n]


class Scenario:
    def __init__(self, title):
        self.title = title
        self.rows = []

    def step(self, actor, method, path, body=None, expect=(200,), label="", check=None, raw_body=None, show=None):
        tok = actor.token if actor else None
        s, js, txt = raw(method, path, tok, body, raw_body)
        exp = expect if isinstance(expect, (tuple, list)) else (expect,)
        ok = s in exp
        note = ""
        if ok and check is not None:
            try:
                res = check(js)
                if res is not True and res is not None:
                    ok, note = False, f"assert: {res}"
                elif res is None:
                    pass
            except Exception as e:  # noqa
                ok, note = False, f"assert exc: {e!r}"[:120]
        g = show(js) if (show and s < 300) else gist(js, txt)
        self.rows.append(dict(actor=actor.name if actor else "anon", ep=f"{method} {short(path)}",
                              label=label, exp="/".join(map(str, exp)), act=s, ok=ok, gist=(note + " " + g).strip()))
        mark = "OK " if ok else "KO "
        print(f"{mark} [{actor.name if actor else 'anon':7}] {method} {short(path)} -> {s} (exp {exp}) {label} | {(note + ' ' + g).strip()[:160]}")
        return s, js

    def note(self, label, ok, gist_=""):
        self.rows.append(dict(actor="—", ep="(assertion)", label=label, exp="✔", act="✔" if ok else "✘", ok=ok, gist=gist_))
        print(f"{'OK ' if ok else 'KO '} [assert ] {label} | {gist_}")

    def summary(self):
        p = sum(1 for r in self.rows if r["ok"])
        return p, len(self.rows) - p

    def to_md(self):
        p, f = self.summary()
        out = [f"### {self.title} — {p} ✅ / {f} ❌", "",
               "| # | Acteur | Endpoint | Étape | Attendu | Obtenu | | Gist |", "|---|---|---|---|---|---|---|---|"]
        for i, r in enumerate(self.rows, 1):
            g = r["gist"].replace("|", "\\|")[:120]
            out.append(f"| {i} | {r['actor']} | `{r['ep']}` | {r['label']} | {r['exp']} | {r['act']} | {'✅' if r['ok'] else '❌'} | {g} |")
        return "\n".join(out)


def short(path):
    import re
    path = re.sub(r"event_\d+_[0-9a-f]{8}", "{ev}", path)
    path = re.sub(r"scenario_[0-9.]+_[0-9.E-]+", "{sc}", path)
    path = re.sub(r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "{uuid}", path)
    path = re.sub(r"user_[-0-9]+_[-0-9a-f]+", "{user}", path)
    return path


def score(poll_votes, slot_ids):
    w = {"YES": 2, "MAYBE": 1, "NO": -1}
    sc = {s: 0 for s in slot_ids}
    for _, votes in poll_votes.items():
        for s, v in votes.items():
            if s in sc:
                sc[s] += w[v]
    return sc


def create_event(sc, org, title, desc, slots, deadline, etype="OTHER", extra=None, label="création événement"):
    body = {"title": title, "description": desc, "organizerId": org.id, "deadline": deadline,
            "proposedSlots": slots, "eventType": etype}
    if extra:
        body.update(extra)
    s, js = sc.step(org, "POST", "/api/events", body, 201, label, show=lambda j: f"id={j['id']} status={j['status']}")
    return js["id"] if s == 201 else None


def sync_confirm(sc, actor, ev, slot_id, final_date, expect=(200,), label="CONFIRMED via /api/sync", title="t", desc="d", deadline="2030-01-01T00:00:00Z"):
    data = {"id": ev, "title": title, "description": desc, "organizerId": actor.id, "deadline": deadline,
            "timezone": "Europe/Paris", "status": "CONFIRMED", "confirmedSlotId": slot_id, "finalDate": final_date}
    change = {"id": "chg_" + uuid.uuid4().hex[:10], "table": "events", "operation": "UPDATE", "recordId": ev,
              "data": json.dumps(data), "timestamp": iso(now_utc()), "userId": actor.id}
    return sc.step(actor, "POST", "/api/sync", {"changes": [change]}, expect, label,
                   show=lambda j: f"applied={j.get('appliedChanges')} conflicts={len(j.get('conflicts', []))}")


def status(sc, actor, ev, st, expect=(200,), label=None, **kw):
    body = {"eventId": ev, "status": st}
    body.update(kw)
    return sc.step(actor, "PUT", f"/api/events/{ev}/status", body, expect, label or f"→ {st}",
                   show=lambda j: f"status={j.get('status')}")


def get_status(org, ev):
    s, js, _ = raw("GET", f"/api/events/{ev}", org.token)
    return js.get("status") if s == 200 else f"HTTP{s}"
