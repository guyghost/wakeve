## Context
The Ktor backend already exposes `GET /health` and listens on `0.0.0.0:8080`. The release app uses `https://api.wakeve.app`, but the domain currently has no public DNS record, so live URL checks fail before any backend behavior can be verified.

Cloudflare Containers are invoked from a Worker through a Durable Object binding. Wrangler can build and push the container image from a local Dockerfile during deploy. The public entrypoint should be a Worker route or custom domain for `api.wakeve.app`.

## Goals / Non-Goals
- Goals:
  - Provide a repeatable deployment path for the Ktor backend on Cloudflare Containers.
  - Make `https://api.wakeve.app/health` publicly reachable over HTTPS for App Review.
  - Keep secrets out of the repository.
  - Add enough smoke/evidence automation to close AS-14 when DNS, backend, AASA, and web routes are live.
- Non-Goals:
  - Replace the mobile sync architecture.
  - Migrate all persistence in this change unless a durable backend store is explicitly chosen.
  - Mark App Store AS-14 complete before external DNS and HTTPS checks pass.

## Decisions
- Decision: deploy Ktor behind a Worker-backed Cloudflare Container, not as a standalone VM.
  - Reason: the requested target is Cloudflare Containers and the Worker gives Cloudflare-native routing, HTTPS, observability, and custom-domain integration.
- Decision: keep Ktor bound to port `8080` in the container.
  - Reason: the current server constant and health tests already use `8080`.
- Decision: define `api.wakeve.app` as the production API custom domain/route.
  - Reason: iOS release code, docs, and audit scripts already expect that host.
- Decision: require `JWT_SECRET` and OAuth/provider credentials through Cloudflare secrets.
  - Reason: production startup already fails without `JWT_SECRET`, and credentials must not be committed.
- Decision: treat container-local SQLite as App Review/smoke-only until durable persistence is added.
  - Reason: Cloudflare Container disk is ephemeral when instances sleep; durable user data needs an external database or compatible persistent storage design.

## Risks / Trade-offs
- Risk: cold starts can affect first request latency.
  - Mitigation: keep `/health` lightweight, use a small runtime image, and document expected readiness delay after first deploy.
- Risk: fixed stateless routing across multiple containers can split local SQLite state.
  - Mitigation: use one container instance for App Review smoke, or add durable persistence before scaling beyond one instance.
- Risk: DNS may still block AS-14 even if deployment config exists.
  - Mitigation: make public DNS verification an explicit task and keep evidence incomplete until `dig` and `curl` pass externally.
- Risk: OAuth callback URLs may still point to localhost defaults.
  - Mitigation: set `GOOGLE_REDIRECT_URI` and `APPLE_REDIRECT_URI` to public production URLs in Cloudflare environment variables.

## Migration Plan
1. Add a Ktor container image build using the existing Gradle server module.
2. Add Cloudflare Worker/container configuration under `infra/cloudflare/backend`.
3. Configure `api.wakeve.app` DNS and route/custom domain in Cloudflare.
4. Set required secrets in Cloudflare: `JWT_SECRET`, OAuth credentials, push credentials where enabled.
5. Deploy with Wrangler and wait for container provisioning.
6. Verify `https://api.wakeve.app/health`, authenticated API failure behavior, and metrics protection.
7. Run `./scripts/capture-app-store-live-url-aasa.sh` and `./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready`.
8. Update App Store live URL evidence only after public checks pass.

## Open Questions
- Should this first deployment use one container instance for App Review, or should durable external persistence be added before public review access?
- Which Cloudflare account and zone own `wakeve.app`?
- What is the real production Apple Team ID for AASA validation?
- Which production datastore should replace container-local SQLite for durable backend state?
