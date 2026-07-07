# Wakeve Cloudflare Backend

This directory deploys the existing Ktor backend as a Cloudflare Workers Container exposed at `https://api.wakeve.app`.

Cloudflare Containers require the Workers Paid plan. On a Free plan, Wrangler can upload the Worker script and secrets but container image rollout fails with an unauthorized Containers error.

## Files

- `wrangler.jsonc`: Worker, Container, Durable Object, observability, and `api.wakeve.app` routing.
- `src/index.ts`: Worker entrypoint that starts one named Ktor container instance and forwards all requests.
- `package.json`: Wrangler, TypeScript, and `@cloudflare/containers` dependencies.
- Root `Dockerfile`: multi-stage Ktor backend image build used by Wrangler.

## Required Secrets

Set secrets in the Cloudflare Worker before deploying:

```bash
cd infra/cloudflare/backend
pnpm install
export CLOUDFLARE_API_TOKEN=<token-with-workers-scripts-edit>
pnpm wrangler secret put JWT_SECRET
```

Optional provider secrets, depending on enabled production features:

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

Non-secret production values live in `wrangler.jsonc` under `vars`.

## Deploy

```bash
./scripts/deploy-cloudflare-backend.sh
```

After deployment, Cloudflare may need several minutes to provision the container. Then verify:

```bash
./scripts/smoke-cloudflare-backend.sh
```

## Persistence Constraint

The current backend opens `wakev_server.db` inside the container. Cloudflare Container disk is ephemeral when instances sleep, so this deployment is suitable for App Review smoke access only until Wakeve adds an external durable datastore or a reviewed persistence design. `max_instances` is intentionally `1` to avoid splitting local SQLite state across container instances.
