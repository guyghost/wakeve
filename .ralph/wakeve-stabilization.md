# Wakeve Stabilization — Vagues 1, 2, 3

## Vague 1 — Stabilisation (priorité immédiate)
- [x] Fix server build (JVM target mismatch Java 23 vs Kotlin JVM 11) — `e6c213c`
- [x] Trier les 11 issues td en review (approver/rejeter) — All 11 approved
- [x] Fix Xcode environment (xcodebuild -runFirstLaunch) — Run successfully
- [ ] Nettoyer warnings compilation Kotlin (ExperimentalCoroutinesApi, unchecked casts)
- ⚠️ iOS Simulator needs manual install via Xcode > Settings > Components

## Vague 2 — Préparation Production
- [x] Configurer JaCoCo et mesurer couverture réelle — `be6f702`, baseline 25.1%
- [x] CI/CD GitHub Actions — `2c2d3e8`, 4 jobs
- [x] Security audit: valider findings — Score B+, 0 critical, 1 high (cert pinning)
- [x] Fix server unit tests (NPE, missing schema, JWT env vars) — `1f5561d`
- [ ] Ajouter tests pour packages critiques (services, chat, auth)
- [ ] Documentation API (OpenAPI spec)

## Vague 3 — Features Majeures
- [ ] PostgreSQL pour server
- [ ] Beta testing setup

## Tests Added
- [x] 22 ReconnectionManager tests (chat package) — `21ecd66`
