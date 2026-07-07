# Cloudflare Backend Deployment

Status: deployed on 2026-06-14.

This runbook deploys the Wakeve Ktor backend to Cloudflare Workers Containers at `https://api.wakeve.app`.

## Architecture

- Cloudflare Worker: public HTTPS entrypoint and request proxy.
- Cloudflare Container: Ktor JVM backend built from the root `Dockerfile`.
- Durable Object binding: required by Cloudflare Containers and used to address the single backend instance.
- Custom domain: `api.wakeve.app`.

The initial configuration uses `max_instances = 1` because the backend currently stores `wakev_server.db` on the container filesystem. Container disk is ephemeral when an instance sleeps, so this deployment is suitable for App Review smoke access until a durable external datastore is selected and implemented.

Current production endpoint:

- Worker: `wakeve-backend`
- Container application: `wakeve-backend-wakevebackendcontainer`
- Container application ID: `a033545d-cdd9-49a2-b1db-95f8138b8ae8`
- Public API: `https://api.wakeve.app`

## Prerequisites

- Cloudflare account has access to Workers Containers.
- The account is on the Workers Paid plan; Cloudflare rejects container image deploys on Free with `Unauthorized: You do not have access to Cloudflare Containers. Deploying containers requires the Workers Paid plan.`
- The Cloudflare zone for `wakeve.app` is active.
- Docker or a compatible local daemon is running.
- Wrangler is authenticated for the target account.
- Non-interactive Wrangler runs have `CLOUDFLARE_API_TOKEN` exported with at least account-scoped `Workers Scripts:Edit`; add `User Memberships:Read` if Wrangler account diagnostics are needed.
- `api.wakeve.app` can be assigned as a Worker custom domain.
- The real Apple Team ID is available for AASA checks.

## Required Secrets

Set at least:

```bash
cd infra/cloudflare/backend
pnpm install
export CLOUDFLARE_API_TOKEN=<token-with-workers-scripts-edit>
pnpm wrangler secret put JWT_SECRET
```

Optional secrets when enabling providers:

```bash
pnpm wrangler secret put GOOGLE_CLIENT_ID
pnpm wrangler secret put GOOGLE_CLIENT_SECRET
pnpm wrangler secret put APPLE_CLIENT_ID
pnpm wrangler secret put APPLE_TEAM_ID
pnpm wrangler secret put APPLE_KEY_ID
pnpm wrangler secret put APPLE_PRIVATE_KEY
pnpm wrangler secret put FCM_SERVER_KEY
pnpm wrangler secret put APNS_KEY_ID
pnpm wrangler secret put APNS_TEAM_ID
pnpm wrangler secret put APNS_AUTH_KEY
```

Non-secret values are declared in `infra/cloudflare/backend/wrangler.jsonc`, including production OAuth callback URLs.

## Deploy

```bash
./scripts/deploy-cloudflare-backend.sh
```

The script runs backend tests, installs the Cloudflare backend package, typechecks the Worker, and calls `wrangler deploy`.

Cloudflare Containers can take several minutes to provision after first deploy.

If `wrangler containers list` returns an unauthorized Workers Paid plan error, upgrade the account before retrying the deploy. The Worker script and secrets may already exist, but `api.wakeve.app` will not be usable until the container image rollout succeeds.

## Smoke Test

```bash
./scripts/smoke-cloudflare-backend.sh
```

The script checks DNS through public resolver `1.1.1.1` by default. If public resolvers already see the hostname but the local resolver cache is stale for curl, pin a Cloudflare edge IP:

```bash
API_RESOLVE_IP=104.21.48.204 TIMEOUT_SECONDS=120 ./scripts/smoke-cloudflare-backend.sh
```

The smoke test verifies:

- `api.wakeve.app` DNS resolution.
- `GET https://api.wakeve.app/health` returns `200`.
- Protected API routes reject unauthenticated requests.
- `/metrics` is not publicly readable without an allowed source.

Last passing production smoke check on 2026-06-14 used `TIMEOUT_SECONDS=120 ./scripts/smoke-cloudflare-backend.sh`:

- `dig @1.1.1.1 api.wakeve.app A` returned `104.21.48.204` and `172.67.156.46`.
- `dig @1.1.1.1 api.wakeve.app AAAA` returned `2606:4700:3030::6815:30cc` and `2606:4700:3036::ac43:9c2e`.
- `GET https://api.wakeve.app/health` returned `200 OK`.
- `GET https://api.wakeve.app/api/events` without credentials returned `401`.
- `GET https://api.wakeve.app/metrics` returned `403`.

## App Store Evidence

After both web and API domains are live:

```bash
APPLE_TEAM_ID=<REAL_TEAM_ID> ./scripts/capture-app-store-live-url-aasa.sh --timeout 12
APP_REVIEW_PHONE_NUMBER='+33123456789' APPLE_TEAM_ID=<REAL_TEAM_ID> ./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready
```

Only update `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md` to `APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true` after every public legal URL, AASA, dashboard route, redirect, and backend health check passes.

## Rollback

Use Wrangler deployments/versions for rollback:

```bash
cd infra/cloudflare/backend
pnpm wrangler deployments list
pnpm wrangler rollback
```

Then rerun:

```bash
./scripts/smoke-cloudflare-backend.sh
```
