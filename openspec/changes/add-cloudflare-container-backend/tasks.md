## 1. Implementation
- [x] 1.1 Add a production Dockerfile for `:server` that builds/runs the Ktor backend on Linux amd64 and exposes port `8080`.
- [x] 1.2 Add Cloudflare Worker container wrapper and Wrangler configuration under `infra/cloudflare/backend`.
- [x] 1.3 Configure `api.wakeve.app` as the public Cloudflare route/custom domain for the backend Worker.
- [x] 1.4 Document required Cloudflare secrets and non-secret environment variables without committing secret values.
- [x] 1.5 Add or update deploy scripts/runbook steps for build, deploy, status, logs, rollback, and smoke tests.

## 2. Verification
- [x] 2.1 Run backend tests with production-like environment variables.
- [x] 2.2 Verify local container startup and `GET /health`.
- [x] 2.3 Deploy to Cloudflare and confirm container status with Wrangler.
- [x] 2.4 Verify public DNS for `api.wakeve.app` from an external resolver.
- [x] 2.5 Verify `https://api.wakeve.app/health` returns `200 OK` over valid HTTPS.
- [x] 2.6 Verify protected API endpoints still reject unauthenticated requests.
- [x] 2.7 Verify `/metrics` is not publicly exposed in production without an allowed source.

## 3. App Store Evidence
- [x] 3.1 Run `./scripts/capture-app-store-live-url-aasa.sh --timeout 12`.
- [x] 3.2 Run `./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready`.
- [x] 3.3 Update `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md` with DNS, HTTPS, backend health, rollout, rollback, and command-output evidence.
- [x] 3.4 Keep `APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=false` until every public DNS, AASA, legal URL, dashboard, and backend health check passes.

## Verification Notes
- 2026-06-14: `openspec validate add-cloudflare-container-backend --strict` passed.
- 2026-06-14: `pnpm --dir infra/cloudflare/backend run check` passed.
- 2026-06-14: `JWT_SECRET=cloudflare-deploy-test-secret-at-least-32-chars JWT_ISSUER=wakev-api JWT_AUDIENCE=wakev-client ./gradlew --no-daemon :server:test` passed.
- 2026-06-14: `JWT_SECRET=cloudflare-deploy-test-secret-at-least-32-chars JWT_ISSUER=wakev-api JWT_AUDIENCE=wakev-client ./gradlew --no-daemon :server:buildFatJar` passed.
- 2026-06-14: `API_BASE_URL=http://127.0.0.1:8080 API_HOST=127.0.0.1 EXPECT_DNS=false ./scripts/smoke-cloudflare-backend.sh` passed against local `:server:runFatJar`.
- 2026-06-14: `pnpm --dir infra/cloudflare/backend exec wrangler deploy --dry-run --containers-rollout=none` passed and validated Worker bundling/bindings without building the container image.
- 2026-06-14: `docker buildx build --platform linux/amd64 -t wakeve-backend-cloudflare:local --load -f Dockerfile .` passed.
- 2026-06-14: `docker run --rm --platform linux/amd64 --name wakeve-backend-cloudflare-local -p 18080:8080 ... wakeve-backend-cloudflare:local` started Ktor successfully and responded on port `8080`.
- 2026-06-14: `API_BASE_URL=http://127.0.0.1:18080 API_HOST=127.0.0.1 EXPECT_DNS=false ./scripts/smoke-cloudflare-backend.sh` passed against the linux/amd64 Docker image.
- 2026-06-14: `pnpm --dir infra/cloudflare/backend exec wrangler deploy --dry-run` passed and built the configured container image via Docker.
- 2026-06-14: Cloudflare API confirms zone `wakeve.app` is active under account `bab940ffcf652079ec6172c267afa11e`, but DNS records are currently empty.
- 2026-06-14: Provided Cloudflare API token is recognized for account `bab940ffcf652079ec6172c267afa11e`, but Wrangler write operations fail with Cloudflare authentication error `10000` for both `workers/scripts/wakeve-backend/secrets` and `workers/services/wakeve-backend`.
- 2026-06-14: Second Cloudflare API token successfully created `JWT_SECRET`, uploaded Worker `wakeve-backend`, and created deployments `41d2e3ac-2ebd-43e6-a997-3554b9a868fa`, `fa10d797-2cfd-468a-b9e1-0c9853d6b8aa`, and `300333ed-9edc-41da-8c4c-9343fa22b7e4`.
- 2026-06-14: Container rollout failed at `POST /accounts/bab940ffcf652079ec6172c267afa11e/containers/me` with `Unauthorized`; `wrangler containers list` reports `You do not have access to Cloudflare Containers. Deploying containers requires the Workers Paid plan.`
- 2026-06-14: Cloudflare DNS records and Worker routes for zone `wakeve.app` remain empty, so no public `api.wakeve.app` endpoint is active yet.
- 2026-06-14: After enabling Workers Paid, `./scripts/deploy-cloudflare-backend.sh --skip-tests` uploaded the Worker, pushed the container image, and created Cloudflare Containers application `wakeve-backend-wakevebackendcontainer` with ID `a033545d-cdd9-49a2-b1db-95f8138b8ae8`.
- 2026-06-14: The second Cloudflare API token could deploy Workers/Containers but failed to create zone Worker routes with Cloudflare authentication error `10000`; the `api.wakeve.app` Worker custom domain was attached via the Cloudflare API instead.
- 2026-06-14: Cloudflare API created Worker custom domain `api.wakeve.app` for service `wakeve-backend` with domain ID `26d5d437d3f1aafdad95894877fce0c38d050396` and certificate ID `d1c51636-7c80-4b80-9e1f-0e2f944bdb5d`.
- 2026-06-14: Cloudflare DNS contains a read-only proxied AAAA record for `api.wakeve.app`; public resolvers return A `104.21.48.204`, `172.67.156.46` and AAAA `2606:4700:3030::6815:30cc`, `2606:4700:3036::ac43:9c2e`.
- 2026-06-14: `curl --resolve api.wakeve.app:443:104.21.48.204 https://api.wakeve.app/health` returned HTTPS `200 OK` with body `OK`.
- 2026-06-14: `TIMEOUT_SECONDS=120 ./scripts/smoke-cloudflare-backend.sh` passed: public DNS via `1.1.1.1`, `/health` `200`, unauthenticated `/api/events` `401`, and `/metrics` `403`.
- 2026-06-14: `pnpm --dir infra/cloudflare/backend exec wrangler containers list` reports application `wakeve-backend-wakevebackendcontainer` active with 1 live instance.
- 2026-06-20: `./scripts/capture-app-store-live-url-aasa.sh --timeout 12` ran and generated `docs/app-store-live-url-aasa/live-url-aasa-2026-06-20T19-28-38Z.md`; result failed because `wakeve.app` has no public DNS answer, AASA cannot be fetched, and `APPLE_TEAM_ID` was unset. `api.wakeve.app` DNS resolves to Cloudflare A records, and HEAD `/health` reached Cloudflare with HTTP `405`.
- 2026-06-20: `./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready` ran to completion. It reported `Passed: 3084`, `Errors: 22`, `Warnings: 2`, then final `Blockers: 23`, `Result: NOT READY for App Store submission`. Blockers are live URL/AASA validation plus missing Fastlane/App Store Connect environment variables: `APPLE_ID`, `ITC_TEAM_ID`, `TEAM_ID`, and `APPLE_TEAM_ID`.
