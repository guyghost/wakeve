# Cloudflare Web Migration Runbook - Wakeve

Date: 2026-09-03
Status: DEPLOYED 2026-09-03 (wakeve-web + wakeve-dashboard live); remaining: production APPLE_TEAM_ID for AASA

## Contexte

Décision owner : Wakeve (web + backend) sera déployé chez Cloudflare. Le backend tourne déjà en Cloudflare Workers Containers (`infra/cloudflare/backend`). Ce runbook couvre la migration des surfaces web depuis Vercel vers Cloudflare Workers, dans le cadre de la proposition Swarm DAO #8 (fermeture AS-14).

## Architecture cible

| Worker | App | Routes | Rôle |
|---|---|---|---|
| `wakeve-web` | `apps/landing` | `wakeve.app/*` | Pages légales, redirects legacy, AASA |
| `wakeve-dashboard` | `apps/dashboard` | `wakeve.app/app`, `wakeve.app/app/*` | Dashboard `/app/*` |
| `wakeve-backend` | `infra/cloudflare/backend` | `api.wakeve.app/*` | API Ktor (déjà déployé) |

Détail important : les routes dashboard (`wakeve.app/app`, `wakeve.app/app/*`) sont plus spécifiques que `wakeve.app/*` et gagnent la priorité Cloudflare, tandis que `/apple-app-site-association` ne matche que le worker landing (attention à ne jamais utiliser un pattern `wakeve.app/app*` qui capturerait aussi `/apple-app-site-association`).

## Changements en repo (déjà faits)

- `apps/dashboard` : `@sveltejs/adapter-vercel` + `@vercel/microfrontends` remplacés par `@sveltejs/adapter-cloudflare` (le landing l'utilisait déjà).
- `apps/landing/wrangler.jsonc` + `apps/dashboard/wrangler.jsonc` : configs Workers avec assets statiques et routes (source de vérité du routage).
- `apps/landing/microfrontends.json` supprimé (routing Vercel obsolète).
- `apps/landing/vite.config.ts` : proxy dev `^/app(/|$)` → `localhost:3001` (parité dev locale ; regex pour ne pas capturer `/apple-app-site-association`).
- `scripts/deploy-cloudflare-web.sh` : build + check + `wrangler deploy` des deux apps.
- `scripts/app-store-local-web-route-check.sh` : la validation routage lit désormais les `wrangler.jsonc` au lieu de `microfrontends.json`.

## Étapes owner (déploiement)

1. **Auth** : `npx wrangler login` (compte Cloudflare propriétaire de la zone `wakeve.app`).
2. **Déployer** :
   ```bash
   ./scripts/deploy-cloudflare-web.sh
   ```
3. **Team ID Apple** (ferme les AASA 503) : renseigner le vrai Team ID à 10 caractères :
   ```bash
   cd apps/landing
   npx wrangler secret put APPLE_TEAM_ID   # ou éditer vars dans wrangler.jsonc + redéployer
   ```
4. **DNS** : la zone est déjà Cloudflare ; s'assurer que `wakeve.app` resolve vers la zone (déjà le cas — pages live).
5. **Retrait Vercel** : décommissionner le projet Vercel après validation Cloudflare (les DNS pointing déjà vers Cloudflare rendent cette étape non bloquante).
6. **Vérifier** :
   ```bash
   BASE_URL=https://wakeve.app APPLE_TEAM_ID=<real> ./scripts/app-store-local-web-route-check.sh
   ./scripts/capture-app-store-live-url-aasa.sh --timeout 12
   APP_REVIEW_PHONE_NUMBER='+33123456789' APPLE_TEAM_ID=<real> ./scripts/lint-store-metadata.sh --ios-only --check-live-urls
   ```

## Critères de succès

- 7 erreurs live URL/AASA restantes → 0 (5 routes `/app/*` + 2 AASA).
- `wakeve.app/app/*` sert le dashboard (200).
- AASA retourne le JSON avec le vrai app ID `<TEAM_ID>.com.guyghost.wakeve`.
- Mise à jour des baselines : re-capture + refresh du registre AS-14 (pattern établi le 2026-09-03).

## Rollback

- `npx wrangler rollback` (ou `deploy` d'une version précédente) par worker.
- Les pages légales actuellement live restent servies jusqu'au basculement des routes Workers ; en cas d'échec du worker landing, réactiver l'hébergement précédent (DNS/zone inchangés).
