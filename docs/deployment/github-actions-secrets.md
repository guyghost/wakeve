# GitHub Actions Secret Setup — Landing Deployment

This workflow (`.github/workflows/deploy-landing.yml`) deploys the SvelteKit
landing app to Cloudflare Workers on every merge to `main`.

## Required GitHub Secrets

Configure in: **GitHub repo → Settings → Secrets and variables → Actions → New repository secret**

### `CLOUDFLARE_ACCOUNT_ID`

```
bab940ffcf652079ec6172c267afa11e
```

### `CLOUDFLARE_API_TOKEN`

Create a token at: https://dash.cloudflare.com/profile/api-tokens

Use the template **"Edit Cloudflare Workers"** or create a custom token with:

| Permission | Scope | Value |
|-----------|-------|-------|
| Account → Workers Scripts | Edit | ✅ |
| Zone → Workers Routes | Edit | ✅ |
| Account → Workers Assets | Edit | ✅ |
| User → User Details | Read | ✅ |

Under **Account Resources**, select your account (`Guyghost@gmail.com's Account`).
Under **Zone Resources**, include the specific zone `wakeve.app`.

Copy the generated token (starts with letters/digits, ~40 chars) into the
GitHub secret `CLOUDFLARE_API_TOKEN`.

## Verification

After configuring secrets, trigger a test deploy:

```bash
# Option A: push to main
git push origin main

# Option B: manual trigger from GitHub UI
# Actions tab → "Deploy Landing" → "Run workflow"
```

The workflow will:
1. Install dependencies (pnpm, frozen lockfile)
2. Build SvelteKit with adapter-cloudflare
3. Deploy to the `wakeve-landing` Worker
4. Verify `https://wakeve.app/` returns HTTP 200

## Notes

- Worker secrets (like `APPLE_TEAM_ID`) are **not** managed by this workflow.
  They persist across deploys. Set them once via `wrangler secret put`.
- The custom domain `wakeve.app` is attached at the Worker level and persists
  across deploys — no DNS reconfiguration needed.
- Deploys are serialized per branch (`concurrency.cancel-in-progress: false`).
