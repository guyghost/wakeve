## 1. OpenSpec
- [x] 1.1 Add public web presence specification delta.
- [x] 1.2 Validate the OpenSpec change in strict mode.

## 2. Web Restructure
- [x] 2.1 Move the existing SvelteKit web app into `apps/dashboard`.
- [x] 2.2 Create `apps/landing` as a separate SvelteKit app.
- [x] 2.3 Move public legal/support and AASA routes to the landing app.
- [x] 2.4 Rebase dashboard routes under `/app`.

## 3. Vercel Microfrontends
- [x] 3.1 Add root pnpm workspace configuration for both apps.
- [x] 3.2 Add `@vercel/microfrontends` and Vite plugin setup to both apps.
- [x] 3.3 Add `microfrontends.json` to the landing app.

## 4. Landing Experience
- [x] 4.1 Implement the public Wakeve landing page.
- [x] 4.2 Add compatibility redirects from old app paths to `/app`.
- [x] 4.3 Update dashboard internal links and auth redirects.

## 5. Verification
- [x] 5.1 Run landing check and build.
- [x] 5.2 Run dashboard check and build.
- [x] 5.3 Smoke test public, dashboard, AASA, and redirect routes locally.
