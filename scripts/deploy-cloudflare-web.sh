#!/usr/bin/env bash
# Deploys the Wakeve web surfaces (landing + dashboard) to Cloudflare Workers.
#
# Architecture:
#   - wakeve-web        (landing)  -> route wakeve.app/*
#   - wakeve-dashboard  (dashboard) -> routes wakeve.app/app and wakeve.app/app/*
# The more specific dashboard routes win over wakeve.app/*, and
# /apple-app-site-association keeps matching only the landing worker.
#
# Required before first deploy:
#   1. Cloudflare account owns the wakeve.app zone.
#   2. Wrangler is authenticated (npx wrangler login).
#   3. The real 10-character Apple Team ID is set as APPLE_TEAM_ID
#      (vars in apps/landing/wrangler.jsonc, or
#       npx wrangler secret/var from the apps/landing directory).
#      While empty, the AASA endpoints return 503 exactly like the
#      current production state.

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PNPM_BIN="${PNPM:-pnpm}"

usage() {
    cat <<'USAGE'
Usage: ./scripts/deploy-cloudflare-web.sh [--skip-check]

Options:
  --skip-check  Skip svelte-check type validation before deploy.
USAGE
}

SKIP_CHECK=false
while [ "$#" -gt 0 ]; do
    case "$1" in
        --skip-check)
            SKIP_CHECK=true
            shift
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

if ! command -v "$PNPM_BIN" >/dev/null 2>&1; then
    echo "$PNPM_BIN is required. Install pnpm or set PNPM to another executable." >&2
    exit 1
fi

for app in landing dashboard; do
    APP_DIR="$PROJECT_DIR/apps/$app"
    echo "==> Building apps/$app"
    "$PNPM_BIN" --dir "$APP_DIR" install
    if [ "$SKIP_CHECK" != true ]; then
        "$PNPM_BIN" --dir "$APP_DIR" run check
    fi
    "$PNPM_BIN" --dir "$APP_DIR" run build
    "$PNPM_BIN" --dir "$APP_DIR" exec wrangler deploy
done

echo
echo "Web deployment requested. Wait for route propagation, then run:"
echo "  BASE_URL=https://wakeve.app APPLE_TEAM_ID=<real-team-id> ./scripts/app-store-local-web-route-check.sh"
echo "  ./scripts/capture-app-store-live-url-aasa.sh --timeout 12"
