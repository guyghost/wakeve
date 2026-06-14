#!/usr/bin/env bash
# Public smoke checks for the Cloudflare-hosted Wakeve backend.

set -euo pipefail

API_HOST="${API_HOST:-api.wakeve.app}"
API_BASE_URL="${API_BASE_URL:-https://$API_HOST}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-12}"
EXPECT_DNS="${EXPECT_DNS:-true}"
API_RESOLVE_IP="${API_RESOLVE_IP:-}"
DNS_RESOLVER="${DNS_RESOLVER:-1.1.1.1}"

curl_args=(--max-time "$TIMEOUT_SECONDS")
if [ -n "$API_RESOLVE_IP" ]; then
    curl_args+=(--resolve "$API_HOST:443:$API_RESOLVE_IP")
fi

failures=0

record_failure() {
    failures=$((failures + 1))
    echo "FAIL: $*" >&2
}

echo "Checking DNS for $API_HOST"
if [ -n "$DNS_RESOLVER" ]; then
    dns_answer="$(dig "@$DNS_RESOLVER" +short "$API_HOST" || true)"
else
    dns_answer="$(dig +short "$API_HOST" || true)"
fi
printf '%s\n' "$dns_answer"
if [ "$EXPECT_DNS" = true ] && [ -z "$dns_answer" ]; then
    record_failure "$API_HOST does not resolve publicly"
fi

echo
echo "Checking backend health"
health_status="$(curl -sS -o /tmp/wakeve-backend-health.txt -w '%{http_code}' "${curl_args[@]}" "$API_BASE_URL/health" || true)"
cat /tmp/wakeve-backend-health.txt 2>/dev/null || true
echo
echo "health_status=$health_status"
if [ "$health_status" != "200" ]; then
    record_failure "$API_BASE_URL/health returned $health_status"
fi

echo
echo "Checking protected API rejects unauthenticated requests"
api_status="$(curl -sS -o /tmp/wakeve-backend-api.txt -w '%{http_code}' "${curl_args[@]}" "$API_BASE_URL/api/events" || true)"
cat /tmp/wakeve-backend-api.txt 2>/dev/null || true
echo
echo "api_status=$api_status"
if [ "$api_status" != "401" ] && [ "$api_status" != "403" ]; then
    record_failure "$API_BASE_URL/api/events should reject unauthenticated requests, got $api_status"
fi

echo
echo "Checking metrics protection"
metrics_status="$(curl -sS -o /tmp/wakeve-backend-metrics.txt -w '%{http_code}' "${curl_args[@]}" "$API_BASE_URL/metrics" || true)"
cat /tmp/wakeve-backend-metrics.txt 2>/dev/null || true
echo
echo "metrics_status=$metrics_status"
if [ "$metrics_status" != "403" ]; then
    record_failure "$API_BASE_URL/metrics should return 403 without an allowed source, got $metrics_status"
fi

rm -f /tmp/wakeve-backend-health.txt /tmp/wakeve-backend-api.txt /tmp/wakeve-backend-metrics.txt

if [ "$failures" -gt 0 ]; then
    echo
    echo "$failures backend smoke check(s) failed." >&2
    exit 1
fi

echo
echo "Backend smoke checks passed."
