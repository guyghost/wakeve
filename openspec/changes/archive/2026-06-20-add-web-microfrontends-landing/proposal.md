# Change: Add web microfrontends landing

## Why
Wakeve needs a public landing page that communicates the product clearly without forcing visitors into the authenticated app shell. The current web surface mixes public App Store routes, login, and authenticated product routes in one SvelteKit app, which makes it harder to deploy and evolve the marketing page independently.

## What Changes
- Split the web surface into two SvelteKit apps deployed as Vercel Microfrontends:
  - `apps/landing` as the default public app for `wakeve.app`
  - `apps/dashboard` as the authenticated product app mounted under `/app`
- Add a premium French landing page inspired by the tweet “Your billion dollar app deserves a great landing page”.
- Move public legal/support and AASA routes to the landing app.
- Rebase dashboard routes under `/app` and add compatibility redirects from old authenticated paths.

## Impact
- Affected specs: `public-web-presence`
- Affected code: web app routing, Vercel project configuration, root workspace metadata
- Deployment impact: Vercel must be configured with two projects in the same microfrontends group, with `wakeve-landing` as the default application.
