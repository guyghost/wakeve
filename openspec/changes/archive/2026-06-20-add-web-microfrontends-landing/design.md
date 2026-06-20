## Context
Wakeve currently deploys one SvelteKit app from `webApp/`. That app owns authenticated product routes, login, legal/support pages, and Apple App Site Association endpoints. The new deployment model separates public marketing concerns from authenticated product concerns while keeping a single public domain through Vercel Microfrontends.

## Goals
- Serve a polished public landing page at `/`.
- Serve the authenticated product app under `/app`.
- Keep App Store public routes and AASA responses on the root domain.
- Allow landing and dashboard to build and deploy independently as separate Vercel projects.

## Non-Goals
- No backend API deployment changes.
- No authentication protocol changes.
- No new UI framework; both apps remain SvelteKit.

## Decisions
- Use `apps/landing` as the default Vercel Microfrontends application because it owns `/` and App Store public routes.
- Use `apps/dashboard` for all authenticated UI under `/app`.
- Keep the existing dashboard route names inside the SvelteKit route tree by nesting them under `src/routes/app`.
- Implement old app paths as landing-app server redirects so existing links continue to resolve.
- Use native HTML/CSS/Svelte for the landing product mockup to avoid shipping static UI screenshots.
