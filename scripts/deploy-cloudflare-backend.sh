#!/usr/bin/env bash
# Deploys the Wakeve Ktor backend to Cloudflare Workers Containers.

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="$PROJECT_DIR/infra/cloudflare/backend"

usage() {
    cat <<'USAGE'
Usage: ./scripts/deploy-cloudflare-backend.sh [--skip-tests]

Required before first deploy:
  1. Cloudflare account owns the wakeve.app zone.
  2. Docker or a Docker-compatible daemon is running.
  3. Wrangler is authenticated.
  4. Cloudflare Worker secrets are set, at minimum JWT_SECRET.

Environment:
  PNPM        pnpm executable, defaults to pnpm.

Options:
  --skip-tests  Skip Gradle backend tests before deploy.
USAGE
}

SKIP_TESTS=false
PNPM_BIN="${PNPM:-pnpm}"

while [ "$#" -gt 0 ]; do
    case "$1" in
        --skip-tests)
            SKIP_TESTS=true
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

if ! command -v docker >/dev/null 2>&1; then
    echo "docker is required for Cloudflare container image builds." >&2
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "docker is installed but the daemon is not reachable." >&2
    exit 1
fi

if ! command -v "$PNPM_BIN" >/dev/null 2>&1; then
    echo "$PNPM_BIN is required. Install pnpm or set PNPM to another executable." >&2
    exit 1
fi

if [ "$SKIP_TESTS" != true ]; then
    JWT_SECRET="${JWT_SECRET:-cloudflare-deploy-test-secret-at-least-32-chars}" \
    JWT_ISSUER="${JWT_ISSUER:-wakev-api}" \
    JWT_AUDIENCE="${JWT_AUDIENCE:-wakev-client}" \
        "$PROJECT_DIR/gradlew" --no-daemon -p "$PROJECT_DIR" :server:test
fi

"$PNPM_BIN" --dir "$BACKEND_DIR" install
"$PNPM_BIN" --dir "$BACKEND_DIR" run check
"$PNPM_BIN" --dir "$BACKEND_DIR" exec wrangler deploy

echo
echo "Deployment requested. Wait for container provisioning, then run:"
echo "  ./scripts/smoke-cloudflare-backend.sh"
echo "  ./scripts/capture-app-store-live-url-aasa.sh --timeout 12"
