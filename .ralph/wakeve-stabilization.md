# Wakeve Stabilization — Vagues 1, 2, 3

## Vague 1 — Stabilisation (priorité immédiate)
- [ ] Fix server build (JVM target mismatch Java 23 vs Kotlin JVM 11)
- [ ] Trier les 11 issues td en review (approver/rejeter)
- [ ] Fix Xcode environment (xcodebuild -runFirstLaunch)
- [ ] Nettoyer warnings compilation Kotlin (ExperimentalCoroutinesApi, unchecked casts)

## Vague 2 — Préparation Production
- [ ] Configurer JaCoCo et mesurer couverture réelle
- [ ] Identifier gaps de tests et ajouter tests critiques
- [ ] Security audit: valider findings, fix critical/high
- [ ] Documentation API (OpenAPI spec)

## Vague 3 — Features Majeures
- [ ] CI/CD GitHub Actions
- [ ] PostgreSQL pour server
- [ ] Beta testing setup

**Approche**: Compléter chaque item, commit par commit, en suivant Conventional Commits.
