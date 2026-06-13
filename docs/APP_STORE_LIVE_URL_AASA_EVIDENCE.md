# App Store Live URL And AASA Evidence - Wakeve

Date: 2026-06-13

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
| Apple Team ID | `<APPLE_TEAM_ID>` |
| Bundle ID | `com.guyghost.wakeve` |
| DNS provider | TBD |
| Web hosting provider | TBD |
| Backend provider | TBD |
| Rollout owner | TBD |
| Rollback owner | TBD |
| Reviewer/date | TBD |

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
| `https://api.wakeve.app/health` | Public HTTPS response showing production backend health is OK. | Pending |
| TLS and redirects | No TLS error, mixed-content downgrade, or redirect loop for any review URL. | Pending |
| Cache and rollback | AASA/legal/support cache behavior, deployment owner, and rollback path are documented. | Pending |

## Current Live Check Snapshot

Last command:

```bash
./scripts/lint-store-metadata.sh --ios-only --check-live-urls
```

Result on 2026-06-13: live production validation failed with 9 live URL/AASA errors and 1 final-signoff warning. This does not close AS-14. The App Review phone number was provided for the local lint run, so the remaining warning is the intentionally incomplete final App Store signoff marker; the live blocker remains DNS resolution for `wakeve.app` and `api.wakeve.app`.

Generated capture report:

- `docs/app-store-live-url-aasa/live-url-aasa-2026-06-13T12-20-32Z.md`
- Command: `./scripts/capture-app-store-live-url-aasa.sh --allow-failures`
- Result: `FAIL. 9 required live URL/AASA checks failed or could not be validated.`

Direct DNS snapshot on 2026-06-13:

```text
curl -I --max-time 12 https://wakeve.app/privacy
curl: (6) Could not resolve host: wakeve.app

curl -I --max-time 12 https://wakeve.app/support
curl: (6) Could not resolve host: wakeve.app

curl -I --max-time 12 https://wakeve.app/terms
curl: (6) Could not resolve host: wakeve.app

curl -I --max-time 12 https://wakeve.app/third-party-notices
curl: (6) Could not resolve host: wakeve.app

curl -I --max-time 12 https://wakeve.app/.well-known/apple-app-site-association
curl: (6) Could not resolve host: wakeve.app

curl -I --max-time 12 https://wakeve.app/apple-app-site-association
curl: (6) Could not resolve host: wakeve.app

curl -I --max-time 12 https://api.wakeve.app/health
curl: (6) Could not resolve host: api.wakeve.app

dig +short wakeve.app
[no answer]

dig +short api.wakeve.app
[no answer]
```

Observed live blockers:

- `https://wakeve.app/privacy` is not reachable for both `en-US` and `fr-FR` metadata privacy URL checks.
- `https://wakeve.app/support` is not reachable for both `en-US` and `fr-FR` metadata support URL checks.
- `https://wakeve.app/terms` is not reachable.
- `https://wakeve.app/third-party-notices` is not reachable.
- `https://wakeve.app/.well-known/apple-app-site-association` is not reachable.
- `https://wakeve.app/apple-app-site-association` is not reachable.
- `https://api.wakeve.app/health` is not reachable.

The repository has deployable local web routes and AASA route code, but the public production domains currently do not resolve in DNS. They still need DNS, TLS, hosting, environment variables, and backend deployment evidence before App Review submission.

## Local Pre-Deployment Route Evidence

This section is local evidence only. It does not close AS-14 because App Store review requires the production `https://wakeve.app` and `https://api.wakeve.app` endpoints to be reachable from a public network.

| Check | Local Result | Evidence |
| --- | --- | --- |
| Public legal pages render reviewable HTML | Passed on `http://127.0.0.1:4174` for `/privacy`, `/support`, `/terms`, and `/third-party-notices`. | Each route has `+page.ts` with `export const ssr = true`, so the App Store legal/support content is present in the initial HTML even though the app shell disables SSR globally. |
| Local AASA endpoints | Passed on `http://127.0.0.1:4174` for `/.well-known/apple-app-site-association` and `/apple-app-site-association`. | With `APPLE_TEAM_ID=A1B2C3D4E5`, both endpoints returned `application/json`, app ID `A1B2C3D4E5.com.guyghost.wakeve`, and `/event/*`, `/poll/*`, `/meeting/*`, plus `/invite/*` components. |
| Production adapter build | Passed locally on 2026-05-28 with `npx --yes pnpm@10 check` and `npx --yes pnpm@10 build`. | `apps/landing` now uses `@sveltejs/adapter-vercel` with `runtime: 'nodejs22.x'`, so the production build targets Vercel explicitly instead of relying on adapter auto-detection. |

## Evidence Commands

Run with the real production Apple Team ID before final signoff:

```bash
./scripts/capture-app-store-live-url-aasa.sh
APP_REVIEW_PHONE_NUMBER='+33123456789' APPLE_TEAM_ID=<APPLE_TEAM_ID> ./scripts/lint-store-metadata.sh --ios-only --check-live-urls
curl -I --max-time 10 https://wakeve.app/privacy
curl -I --max-time 10 https://wakeve.app/support
curl -I --max-time 10 https://wakeve.app/terms
curl -I --max-time 10 https://wakeve.app/third-party-notices
curl -i --max-time 10 https://wakeve.app/.well-known/apple-app-site-association
curl -i --max-time 10 https://wakeve.app/apple-app-site-association
curl -i --max-time 10 https://api.wakeve.app/health
```

Attach or paste the generated `docs/app-store-live-url-aasa/live-url-aasa-*.md` report, deployment IDs, DNS check output, cache headers, and reviewer/date before changing the marker.

Local pre-deployment validation command used before live deployment:

```bash
APPLE_TEAM_ID=A1B2C3D4E5 ./node_modules/.bin/vite dev --host 127.0.0.1 --port 4174
BASE_URL=http://127.0.0.1:4174 APPLE_TEAM_ID=A1B2C3D4E5 ./scripts/app-store-local-web-route-check.sh
```

`scripts/app-store-local-web-route-check.sh` validated that the four public legal pages return status 200 with the expected App Store review phrases in initial HTML, and that both AASA endpoints return JSON with the expected test Team ID, Bundle ID, and Universal Link paths.

## Production Deployment Fix Checklist

Before retrying the live App Store gate:

- Deploy the web app serving `/privacy`, `/support`, `/terms`, `/third-party-notices`, `/.well-known/apple-app-site-association`, and `/apple-app-site-association` on `https://wakeve.app`.
- Keep the Vercel project root set to `apps/landing/`; the SvelteKit production build now uses `@sveltejs/adapter-vercel` and should be built with the checked-in `pnpm-lock.yaml`.
- Configure production `APPLE_TEAM_ID` or `TEAM_ID` to the real 10-character Apple Developer Team ID so AASA app IDs are not placeholders.
- Confirm the AASA responses use `application/json`, no redirects, valid TLS, app ID `<APPLE_TEAM_ID>.com.guyghost.wakeve`, and paths `/event/*`, `/poll/*`, `/meeting/*`, plus `/invite/*`.
- Deploy the backend health endpoint at `https://api.wakeve.app/health` with an App Review-safe response that proves the production backend is available.
- Run `./scripts/capture-app-store-live-url-aasa.sh` without `--allow-failures`; keep the generated report as the public-network evidence artifact.
- Record DNS provider, hosting provider, backend provider, deployment IDs, cache headers, rollout owner, rollback owner, command output, reviewer, and date in this file.
- Re-run `APP_REVIEW_PHONE_NUMBER='+33123456789' APPLE_TEAM_ID=<APPLE_TEAM_ID> ./scripts/lint-store-metadata.sh --ios-only --check-live-urls` from a public network and attach the successful output.

## Closure Rule

Set `APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true` only after:

- The production `wakeve.app` deployment serves `/privacy`, `/support`, `/terms`, `/third-party-notices`, `/.well-known/apple-app-site-association`, and `/apple-app-site-association` over HTTPS.
- Both AASA endpoints return valid JSON with `application/json`, no file extension, the real `<APPLE_TEAM_ID>.com.guyghost.wakeve` app ID, and `/event/*`, `/poll/*`, `/meeting/*`, plus `/invite/*` link groups.
- `https://api.wakeve.app/health` is reachable from a public network and returns the expected production health response.
- The linter command above exits 0 with the real `APPLE_TEAM_ID`.
- Rollout owner, rollback owner, deployment IDs, DNS state, cache behavior, command output, reviewer, and date are recorded in this file.
