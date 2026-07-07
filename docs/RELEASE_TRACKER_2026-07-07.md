# Release Tracker — Wakeve App Store Submission

Derniere mise a jour: 2026-07-07
Genere par: audit Hermes baseline post-nettoyage

## Etat global

- **Critical release gates**: PASS (`./scripts/test-critical-release-gates.sh`)
- **OpenSpec validation**: 29/29 PASS (`openspec validate --all --strict`)
- **App Store submission audit**: NOT READY — 22 blockers restants
- **Changements OpenSpec actifs**: 4 (2 en cours device-blocked, 2 deferred post-release)
- **Final signoff**: `APP_STORE_FINAL_SIGNOFF_COMPLETE=false`

Tous les blockers restants necessitent un acces externe (Apple Developer account,
App Store Connect, hebergement DNS, device physique) ou une decision owner. **Aucun
blocker n'est resolvable uniquement par du code dans le depot.**

## Les 22 blockers classes par categorie d'action

### A. Infrastructure live (necessite DNS + hebergement)

| Blocker | Etat | Action requise | Script de verification |
|--------|------|----------------|----------------------|
| **AS-14** Live URL/AASA | 🔴 16/16 checks FAIL | Configurer le DNS public `wakeve.app`, deployer la landing/legal/AASA avec le vrai Apple Team ID | `./scripts/capture-app-store-live-url-aasa.sh` |

Detail AS-14 (preuve fraiche 2026-07-07):
- `wakeve.app` n'a toujours aucun enregistrement DNS public (Could not resolve host)
- `api.wakeve.app` fonctionne (Cloudflare `104.21.48.204`/`172.67.156.46`, `GET /health` → 200 OK)
- Pages manquantes: `/privacy`, `/terms`, `/support`, `/third-party-notices`, `/app/*`, redirects legacy, AASA paths

### B. Signature Apple + build (necessite Apple Developer account actif)

| Blocker | Etat | Action requise |
|--------|------|----------------|
| **AS-01** Apple account/ASC access | 🔴 | Compte Apple Developer avec role Account Holder/Admin, 2FA, app record cree |
| **AS-13** Capabilities/profiles | 🔴 | Provisioning profile `com.guyghost.wakeve`, entitlements Push/Siri/SignInWithApple/AssociatedDomains/APNs |
| **AS-02/03** Team/ signing IDs | 🔴 | Variables `APPLE_ID`, `APPLE_TEAM_ID`, `TEAM_ID`, `ITC_TEAM_ID` a definir |
| **AS-15** Signed final gate | 🔴 | `bundle exec fastlane ios submission_ready` avec credentials et signing reels |
| **AS-18** SDK privacy manifests | 🔴 | Inventaire SDK + privacy manifests + signatures sur l'archive signee |
| **AS-12** TestFlight smoke | 🔴 | Uploader build, installer sur device, smoke test, monitoring 24h |

### C. Decisions App Store Connect (necessite owner/produit)

| Blocker | Etat | Decision requise |
|--------|------|------------------|
| **AS-05** Privacy labels | 🔴 | Aligner App Store Connect ↔ `docs/APP_STORE_PRIVACY_LABELS.md` ↔ `PrivacyInfo.xcprivacy`, signoff legal |
| **AS-06** Accessibility labels | 🔴 | Publier ou confirmer non-publie avec evidence device |
| **AS-07** Availability | 🔴 | Confirmer iPhone/iPad only, Mac/Vision Pro desactive (deja en build settings) |
| **AS-08** DSA trader status | 🔴 | Trader verifie / non-trader / EU storefronts disabled — decision owner |
| **AS-11** Payment compliance | 🔴 | Clarifier depenses partagees = biens/services reels, pas d'unlock digital |
| **AS-17** Pricing/storefronts | 🔴 | Prix, storefronts, pre-order, tax category, Paid Apps Agreement |
| **AS-19** Release control | 🔴 | Option de release (manuel/phased), owner, fenetre, criteres stop/pause |
| **AS-20** Media/localization | 🔴 | Ordre screenshots, app preview, metadonnees EN/FR dans les limites |
| **AS-21** License notices | 🔴 | Inventaire approuve, notices publiees sur `wakeve.app/third-party-notices` |
| **AS-22** EULA/terms | 🔴 | Apple standard EULA ou custom, alignement `/terms` |

### D. Preuves uploaded-build (necessitent build signee + TestFlight d'abord)

| Blocker | Etat | Action requise |
|--------|------|----------------|
| **AS-09** Account deletion | 🟡 Code + tests faits | Verifier le flux Profile → Delete Account sur build TestFlight, collecter evidence |
| **AS-10** UGC moderation | 🟡 Code + tests faits | Verifier report/block/filter sur build TestFlight + contact support live |
| **AS-04** Phone format | 🟡 | `APP_REVIEW_PHONE_NUMBER` au format international valide |

## Ordre d'execution recommande (chemin critique)

```
Phase 1: DNS + Landing          → ferme AS-14
    │
Phase 2: Apple Developer setup  → ferme AS-01, AS-02, AS-03, AS-13
    │
Phase 3: Build signee           → ferme AS-15, AS-18
    │                              (debloque aussi AS-09, AS-10 evidence)
Phase 4: TestFlight             → ferme AS-12, AS-09, AS-10
    │
Phase 5: Decisions ASC          → ferme AS-05, AS-06, AS-07, AS-08,
    │                              AS-11, AS-17, AS-19, AS-20, AS-21, AS-22
    │
Phase 6: Final signoff          → APP_STORE_FINAL_SIGNOFF_COMPLETE=true
```

**Le chemin critique est sequentiel**: on ne peut pas collecter les preuves
TestFlight (Phase 4) sans build signee (Phase 3), qui necessite l'Apple Developer
account (Phase 2), qui necessite le DNS live (Phase 1) pour l'AASA.

## Changements OpenSpec en cours

| Changement | Taches | Bloqueur | Scope |
|-----------|--------|----------|-------|
| `add-event-weather-forecast` | 20/22 | WeatherKit entitlement + validation device physique | P1 iOS |
| `add-on-device-wakeve-ai` | 40/41 | Profiling latence/memoire sur device compatible | P1 iOS |

Restant:
- `add-event-weather-forecast` tache 1.2: confirmer WeatherKit entitlement (Apple Developer)
- `add-event-weather-forecast` tache 6.2: tests device physique WeatherKit
- `add-on-device-wakeve-ai` tache 6.6: profiling device reel Foundation Models

Ces 3 taches sont bloquées par le meme acces Apple Developer / device physique que les blockers App Store. Elles se feront naturellement pendant les Phases 2-4.

## Changements OpenSpec deferred (post-release)

| Changement | Raison |
|-----------|--------|
| `align-ios-android-feature-parity` (0/37) | Trop large pour la premiere release iOS |
| `migrate-android-google-auth-credential-manager` (0/8) | Warnings non bloquants |

## Ce qui est dejas pret et verifie localement

- ✅ Tests unitaires shared/server: PASS (71+ tests)
- ✅ Critical release gates: PASS (Compose hygiene, sensitive logs, Android release defaults, notification contracts, evidence sanitization)
- ✅ OpenSpec strict validation: 29/29 PASS
- ✅ Account deletion: code + tests backend + iOS + cleanup local
- ✅ UGC moderation: code + tests filtering/report/block + iOS discoverability
- ✅ Privacy: `PrivacyInfo.xcprivacy` coherent, 0 IDFA/ATT, 0 log sensible serveur
- ✅ Accessibility: audits source SwiftUI 0 finding, parite loc EN/FR/ES/IT/PT (1831 cles x5)
- ✅ Media localization: 0 finding local, screenshots inventories
- ✅ License inventory: 316 dependances, 0 risque unknown/copyleft en submitted-iOS
- ✅ Payment compliance: rapport local StoreKit/IAP genere
- ✅ Performance: cold start iOS simulateur ~250ms (device reste a faire)
- ✅ Android release build: assembleRelease passe, defaults production

## Commandes de verification

```bash
# Baseline engineering
./scripts/test-critical-release-gates.sh
openspec validate --all --strict
./gradlew :shared:jvmTest

# Audit App Store local (sans preflight)
APP_REVIEW_PHONE_NUMBER='+331****6789' ./scripts/app-store-submission-audit.sh --skip-preflight

# Check live URL/AASA
./scripts/capture-app-store-live-url-aasa.sh --allow-failures --timeout 12

# Audit complet avec live URLs + submission ready
APPLE_ID='...' APPLE_TEAM_ID='...' TEAM_ID='...' ITC_TEAM_ID='...' \
  APP_REVIEW_PHONE_NUMBER='+331xxxxxxxx' \
  ./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready
```
