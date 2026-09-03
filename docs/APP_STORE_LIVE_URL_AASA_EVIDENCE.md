# App Store Live URL And AASA Evidence - Wakeve

Date: 2026-06-20

Status: PENDING

Do not change the marker below until the production web and API domains have been checked from a public network with the real Apple Developer Team ID.

APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-06-01.

- Apple says associated domains require both an associated domain file on the website and a matching Associated Domains entitlement in the app.
- Apple says the apps listed in the `apple-app-site-association` file must match the Associated Domains entitlement.
- Apple says each subdomain requires its own Associated Domains entitlement entry and its own `apple-app-site-association` file.
- Apple says the associated domain file must be named `apple-app-site-association` without an extension.
- Apple says the file should be placed in the site's `.well-known` directory at `https://<fully-qualified-domain>/.well-known/apple-app-site-association`.
- Apple says the file must be hosted with `https://`, a valid certificate, and no redirects.
- Apple says universal links should list app identifiers for the domain in the `applinks` service.
- Apple says Associated Domains entitlement entries use the format `<service>:<fully-qualified-domain>` and must not include path or query components or a trailing slash.
- Apple says iOS 14 and later request `apple-app-site-association` files through an Apple-managed CDN instead of directly from the web server.
- Apple says Apple's CDN requests the file for a domain within 24 hours and devices check for updates approximately once per week after app installation.
- Apple says universal links require a two-way association between the app and website and app handling for `NSUserActivityTypeBrowsingWeb`.
- Apple warns that universal links can be an attack vector, so apps should validate URL parameters, discard malformed URLs, and limit actions that could risk user data.

## Apple References

- Supporting associated domains: https://developer.apple.com/documentation/xcode/supporting-associated-domains
- Associated Domains Entitlement: https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.developer.associated-domains
- Allowing apps and websites to link to your content: https://developer.apple.com/documentation/Xcode/allowing-apps-and-websites-to-link-to-your-content
- Debugging universal links: https://developer.apple.com/documentation/technotes/tn3155-debugging-universal-links

## Scope

This file records the App Store review evidence for AS-14: public legal/support URLs, third-party notices, Apple App Site Association endpoints, and backend health. It complements `docs/APP_STORE_OBSERVABILITY_EVIDENCE.md`, which covers ongoing monitoring after the TestFlight review build is uploaded.

## Build And Deployment Under Review

| Field | Value |
| --- | --- |
| Web deployment commit | TBD |
| API deployment commit | TBD |
| Apple Team ID | Unset; real Team ID still required before closure |
| Bundle ID | `com.guyghost.wakeve` |
| DNS provider | Cloudflare for both `wakeve.app` and `api.wakeve.app` (live as of 2026-09-03) |
| Web hosting provider | Cloudflare Workers (`wakeve-web` + `wakeve-dashboard`); migration prepared in-repo, deploy pending |
| Backend provider | Cloudflare Workers Containers via `infra/cloudflare/backend`; `api.wakeve.app` deployed on 2026-06-14 |
| Rollout owner | TBD; unresolved external ownership blocker before App Review signoff |
| Rollback owner | TBD; unresolved external ownership blocker before App Review signoff |
| Reviewer/date | Local audit refresh on 2026-09-03 |

## Required Live URL And AASA Review

| Check | Required Result | Evidence |
| --- | --- | --- |
| `https://wakeve.app/privacy` | Public HTTPS response with the App Store privacy policy. | Pending |
| `https://wakeve.app/support` | Public HTTPS response with App Review support contact information. | Pending |
| `https://wakeve.app/terms` | Public HTTPS response with the Wakeve terms/EULA-aligned terms. | Pending |
| `https://wakeve.app/third-party-notices` | Public HTTPS response with the generated open-source notices. | Pending |
| `https://wakeve.app/.well-known/apple-app-site-association` | Public HTTPS response, no extension, valid JSON, `application/json`, no placeholder Team ID. | Pending |
| `https://wakeve.app/apple-app-site-association` | Public HTTPS fallback response, no extension, valid JSON, `application/json`, no placeholder Team ID. | Pending |
| AASA app ID | Both AASA responses contain `<APPLE_TEAM_ID>.com.guyghost.wakeve`. | Pending |
| AASA Universal Link paths | Both AASA responses contain `/event/*`, `/poll/*`, `/meeting/*`, and `/invite/*`. | Pending |
| `https://api.wakeve.app/health` | Public HTTPS response showing production backend health is OK. | Passed on 2026-06-14 with GET: `200 OK`, body `OK`. On 2026-06-21 local time DNS resolves and Cloudflare responds; HEAD returns `405`, GET returns `200 OK`; the App Store audit reports API health reachable. |
| `https://wakeve.app/app` | Public dashboard shell route resolves through the `wakeve-dashboard` Cloudflare Worker. | Pending |
| `https://wakeve.app/app/login` | Public dashboard login route resolves through the `wakeve-dashboard` Cloudflare Worker. | Pending |
| `https://wakeve.app/app/dashboard` | Public dashboard home route resolves through the `wakeve-dashboard` Cloudflare Worker. | Pending |
| `https://wakeve.app/app/create` | Public create-event dashboard route resolves through the `wakeve-dashboard` Cloudflare Worker. | Pending |
| `https://wakeve.app/app/events` | Public event-list dashboard route resolves through the `wakeve-dashboard` Cloudflare Worker. | Pending |
| Landing redirects | `/dashboard`, `/login`, `/create`, `/events`, and nested `/events/*` redirect to `/app/*`. | Pending |
| TLS and redirects | No TLS error, mixed-content downgrade, or redirect loop for any review URL. | Pending |
| Cache and rollback | AASA/legal/support cache behavior, deployment owner, and rollback path are documented. | Pending |

## Current Live Check Snapshot

Latest capture command:

```bash
./scripts/capture-app-store-live-url-aasa.sh --allow-failures --timeout 8
```

Result on 2026-09-03 local time, capture timestamp 2026-09-03T20-38-08Z: `FAIL. 17 required live URL/AASA checks failed or could not be validated.`

Generated capture report:

- `docs/app-store-live-url-aasa/live-url-aasa-2026-09-03T20-38-08Z.md`
- `dig +short wakeve.app` returned `104.21.48.204` and `172.67.156.46`.
- `dig +short api.wakeve.app` returned `104.21.48.204` and `172.67.156.46`.
- `https://wakeve.app/privacy`, `/support`, `/terms`, and `/third-party-notices` returned HTTP 200 with reviewable HTML.
- Legacy landing redirects `/dashboard`, `/login`, `/create`, and `/events` validate toward `/app/*`.
- `https://api.wakeve.app/health` returned HTTP `405` for HEAD and HTTP `200 OK` for GET.
- The five dashboard shell routes `/app`, `/app/login`, `/app/dashboard`, `/app/create`, and `/app/events` returned HTTP 404 because the Cloudflare `wakeve-dashboard` worker routes are not deployed.
- Both AASA endpoints returned HTTP 503 with `A real APPLE_TEAM_ID or TEAM_ID is required to serve apple-app-site-association`; Apple Team ID was unset, so AASA app ID validation cannot pass.
- Latest full lint live check: `APP_REVIEW_PHONE_NUMBER='+33123456789' APPLE_TEAM_ID='A1B2C3D4E5' ./scripts/lint-store-metadata.sh --ios-only --check-live-urls` resulted in 3119 checks passed, 7 errors, 1 warning; the remaining live blockers are the five dashboard `/app` routes and both AASA endpoints.

Previous capture on 2026-06-21 local time, capture timestamp 2026-06-20T22-48-00Z:

```bash
./scripts/capture-app-store-live-url-aasa.sh --allow-failures --timeout 12
```
Result on 2026-06-21 local time, capture timestamp 2026-06-20T22:48:00Z: `FAIL. 16 required live URL/AASA checks failed or could not be validated.`

Generated capture report:

- `docs/app-store-live-url-aasa/live-url-aasa-2026-06-20T22-48-00Z.md` (plus intermediate captures 22-02-42Z and 21-55-13Z)
- Apple Team ID was unset, so AASA app ID validation cannot pass even after the web host is live.
- `dig +short wakeve.app` returned no answer; `dig +short api.wakeve.app` returned `104.21.48.204` and `172.67.156.46`.
- `https://api.wakeve.app/health` reached Cloudflare with HTTP `405` for HEAD and HTTP `200 OK` for GET.
- All required `wakeve.app` legal pages, dashboard routes, redirects, and AASA URLs failed with `curl: (6) Could not resolve host: wakeve.app`.

Latest full submission audit on 2026-06-20 (`--check-live-urls --run-submission-ready`): `NOT READY`; `Passed: 3084`, `Errors: 22`, `Warnings: 2`, with live URL/AASA and submission_ready failures plus missing `APPLE_ID`, `ITC_TEAM_ID`, `TEAM_ID`, `APPLE_TEAM_ID`.

Previous command:

```bash
./scripts/lint-store-metadata.sh --ios-only --check-live-urls
```

Result on 2026-06-13: live production validation failed with 9 live URL/AASA errors and 1 final-signoff warning (intentionally incomplete final signoff marker). As of 2026-06-14, `api.wakeve.app` was deployed and healthy; `wakeve.app` web/AASA/dashboard/redirect availability remained blocked.

Generated capture reports:

- `docs/app-store-live-url-aasa/live-url-aasa-2026-06-13T12-20-32Z.md` via `./scripts/capture-app-store-live-url-aasa.sh --allow-failures`: `FAIL. 9 required live URL/AASA checks failed or could not be validated.`
- `docs/app-store-live-url-aasa/live-url-aasa-2026-06-13T13-02-14Z.md` via `./scripts/capture-app-store-live-url-aasa.sh --allow-failures --timeout 5`: `FAIL. 18 required live URL/AASA checks failed or could not be validated.` This refreshed capture adds the five `/app` dashboard routes and legacy landing redirect checks to the public smoke scope.

Direct DNS snapshot on 2026-06-13: `wakeve.app` returned no DNS answer with `Could not resolve host: wakeve.app`; `api.wakeve.app` failed with `Could not resolve host: api.wakeve.app` before the backend deployment.

Observed live blockers (2026-06-13 baseline): `https://wakeve.app/privacy` and `/support` (per `en-US`/`fr-FR` metadata URL checks), `/terms`, `/third-party-notices`, both AASA paths, all five `/app` dashboard routes, and the `/dashboard`, `/login`, `/create`, `/events` legacy redirects were unreachable; `https://api.wakeve.app/health` was unreachable on 2026-06-13 and backend deployment evidence below supersedes this for the API domain only.

The repository has deployable local web routes and AASA route code. In the 2026-06-13/06-21 baselines the production API domain was deployed but public production domains currently do not resolve in DNS for the `wakeve.app` web/AASA scope; the 2026-09-03 baseline above records that DNS/HTTPS and the legal pages are now live, leaving only the dashboard mount and AASA Team ID.

## Cloudflare Backend Live Evidence

Backend deployment completed on 2026-06-14 after enabling the Workers Paid plan: Cloudflare Worker `wakeve-backend`, container application `wakeve-backend-wakevebackendcontainer` (ID `a033545d-cdd9-49a2-b1db-95f8138b8ae8`, custom domain ID `26d5d437d3f1aafdad95894877fce0c38d050396`, certificate ID `d1c51636-7c80-4b80-9e1f-0e2f944bdb5d`), custom domain `api.wakeve.app` resolving to `104.21.48.204`/`172.67.156.46`.
Passing backend smoke: `./scripts/smoke-cloudflare-backend.sh` → `health_status=200`, `api_status=401`, `metrics_status=403`; container `active, 1 live instance`.

## Cloudflare Backend Deployment Plan

The approved `add-cloudflare-container-backend` change adds a deployable backend path for `api.wakeve.app`: root `Dockerfile` (Ktor `:server` fat jar on `8080`), `infra/cloudflare/backend/wrangler.jsonc` (Worker, Cloudflare Container, Durable Object, `api.wakeve.app` route), `infra/cloudflare/backend/src/index.ts` (named backend container instance), `scripts/deploy-cloudflare-backend.sh` (tests + typecheck + `wrangler deploy`), and `scripts/smoke-cloudflare-backend.sh` (DNS, `/health`, API rejection, `/metrics` protection).

This deployed backend does not close AS-14 by itself. `APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=false` remains required until `wakeve.app`, AASA, dashboard routes, redirects, and the full live URL audit pass public checks with the real Apple Team ID.

Persistence note: the current backend uses container-local SQLite (`wakev_server.db`). Cloudflare Container disk is ephemeral when the instance sleeps, so this deployment is App Review/smoke-ready but not durable production data storage until an external datastore or approved persistence design is implemented.

## Local Pre-Deployment Route Evidence

This section is local evidence only. It does not close AS-14 because App Store review requires public production endpoints.

- Public legal pages render reviewable HTML: passed locally for `/privacy`, `/support`, `/terms`, and `/third-party-notices`; each route has `+page.ts` with `export const ssr = true`.
- Local AASA endpoints: passed locally for `/.well-known/apple-app-site-association` and `/apple-app-site-association` with `application/json`, `A1B2C3D4E5.com.guyghost.wakeve`, `/event/*`, `/poll/*`, `/meeting/*`, and `/invite/*`.
- Local dashboard routing: `scripts/app-store-local-web-route-check.sh` verified `apps/dashboard/wrangler.jsonc` routes `wakeve.app/app`(+`/*`) to `wakeve-dashboard` plus the legacy redirects to `/app/*`.
- Production adapter build: passed locally on 2026-05-28 with `npx --yes pnpm@10 check` and `npx --yes pnpm@10 build`.

## Evidence Commands

Run with the real production Apple Team ID before final signoff:

```bash
./scripts/capture-app-store-live-url-aasa.sh
APP_REVIEW_PHONE_NUMBER='+33123456789' APPLE_TEAM_ID=<APPLE_TEAM_ID> ./scripts/lint-store-metadata.sh --ios-only --check-live-urls
curl -I --max-time 10 https://wakeve.app/privacy https://wakeve.app/support https://wakeve.app/terms https://wakeve.app/third-party-notices
curl -I --max-time 10 https://wakeve.app/app https://wakeve.app/app/login https://wakeve.app/app/dashboard https://wakeve.app/app/create https://wakeve.app/app/events
curl -I --max-time 10 https://wakeve.app/dashboard https://wakeve.app/login https://wakeve.app/create https://wakeve.app/events
curl -i --max-time 10 https://wakeve.app/.well-known/apple-app-site-association https://wakeve.app/apple-app-site-association https://api.wakeve.app/health
```

Attach or paste the generated `docs/app-store-live-url-aasa/live-url-aasa-*.md` report, deployment IDs, DNS check output, cache headers, and reviewer/date before changing the marker.

Local pre-deployment validation command used before live deployment:

```bash
APPLE_TEAM_ID=A1B2C3D4E5 pnpm --dir apps/landing exec vite dev --host 127.0.0.1
BASE_URL=http://127.0.0.1:3000 APPLE_TEAM_ID=A1B2C3D4E5 ./scripts/app-store-local-web-route-check.sh
```

`scripts/app-store-local-web-route-check.sh` validated that the four public legal pages return status 200 with the expected App Store review phrases in initial HTML, that landing redirects point to `/app/*`, that `apps/dashboard/wrangler.jsonc` assigns `wakeve.app/app` and `wakeve.app/app/*` to `wakeve-dashboard`, and that both AASA endpoints return JSON with the expected test Team ID, Bundle ID, and Universal Link paths.

## Production Deployment Fix Checklist

Before retrying the live App Store gate:

- Deploy the web app serving `/privacy`, `/support`, `/terms`, `/third-party-notices`, `/app`, `/app/login`, `/app/dashboard`, `/app/create`, `/app/events`, `/.well-known/apple-app-site-association`, and `/apple-app-site-association` on `https://wakeve.app`.
- Deploy both web Workers with `./scripts/deploy-cloudflare-web.sh`; both SvelteKit builds use `@sveltejs/adapter-cloudflare` and the checked-in `pnpm-lock.yaml`.
- Keep Cloudflare Workers routing `wakeve-dashboard` for `wakeve.app/app` and `wakeve.app/app/*`, and keep landing redirects from `/dashboard`, `/login`, `/create`, and `/events` to their `/app/*` equivalents.
- Configure production `APPLE_TEAM_ID` or `TEAM_ID` to the real 10-character Apple Developer Team ID so AASA app IDs are not placeholders.
- Confirm the AASA responses use `application/json`, no redirects, valid TLS, app ID `<APPLE_TEAM_ID>.com.guyghost.wakeve`, and paths `/event/*`, `/poll/*`, `/meeting/*`, plus `/invite/*`.
- Deploy the backend health endpoint at `https://api.wakeve.app/health` returning an App Review-safe `200 OK` response that proves the production backend is available.
- Run `./scripts/capture-app-store-live-url-aasa.sh` without `--allow-failures`; keep the generated report as the public-network evidence artifact.
- Record DNS provider, hosting provider, backend provider, deployment IDs, cache headers, rollout owner, rollback owner, command output, reviewer, and date in this file.
- Re-run `APP_REVIEW_PHONE_NUMBER='+33123456789' APPLE_TEAM_ID=<APPLE_TEAM_ID> ./scripts/lint-store-metadata.sh --ios-only --check-live-urls` from a public network and attach the successful output.

## Closure Rule

Set `APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true` only after:

- The production `wakeve.app` deployment serves `/privacy`, `/support`, `/terms`, `/third-party-notices`, `/app`, `/app/login`, `/app/dashboard`, `/app/create`, `/app/events`, `/.well-known/apple-app-site-association`, and `/apple-app-site-association` over HTTPS.
- The production landing redirects `/dashboard`, `/login`, `/create`, and `/events` to `/app/*` without redirect loops.
- Both AASA endpoints return valid JSON with `application/json`, no file extension, the real `<APPLE_TEAM_ID>.com.guyghost.wakeve` app ID, and `/event/*`, `/poll/*`, `/meeting/*`, plus `/invite/*` link groups.
- `https://api.wakeve.app/health` is reachable from a public network and returns the expected production health response.
- The linter command above exits 0 with the real `APPLE_TEAM_ID`.
- Rollout owner, rollback owner, deployment IDs, DNS state, cache behavior, command output, reviewer, and date are recorded in this file.
