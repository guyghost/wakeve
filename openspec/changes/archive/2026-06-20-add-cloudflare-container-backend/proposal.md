# Change: Deploy Ktor backend on Cloudflare Containers

## Why
Wakeve App Store readiness is blocked by AS-14 because `wakeve.app` and `api.wakeve.app` do not resolve publicly, and `https://api.wakeve.app/health` cannot be validated from an external network. The backend needs a public HTTPS deployment path that can stay reachable during App Review.

## What Changes
- Add a Cloudflare Workers Container deployment for the existing Ktor backend.
- Route `api.wakeve.app` to the Worker-backed container with public DNS and Cloudflare-managed HTTPS.
- Containerize the Ktor server with an App Review-safe `/health` response on port `8080`.
- Configure production secrets and non-secret environment values through Wrangler/Cloudflare, without committing credentials.
- Add deployment, smoke-test, rollback, and evidence steps for `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md`.
- Document the persistence constraint: Cloudflare Container disk is ephemeral, so persistent production data must use an external durable store before this is treated as a durable production backend.

## Impact
- Affected specs: `production-deployment`
- Affected code: `server/`, `infra/cloudflare/`, deployment scripts, App Store evidence docs
- External dependencies: Cloudflare Workers Containers, Wrangler, Docker-compatible local builder, Cloudflare DNS for `wakeve.app`, production secret values
