#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
VIEW_ROOT="${POLL_CONFIRMATION_VIEW_ROOT:-$PROJECT_DIR/iosApp/src/Views}"

scan_root() {
    local root="$1"
    local failures=0

    record_matches() {
    local description="$1"
    local pattern="$2"
    local matches

    matches="$(rg -n --glob '*.swift' "$pattern" "$root" || true)"
    if [ -n "$matches" ]; then
        printf '%s\n' "$matches" >&2
        echo "FAIL: $description" >&2
        failures=$((failures + 1))
    else
        echo "PASS: $description"
    fi
    }

    record_matches \
        "SwiftUI views must not call direct status or confirmation mutation methods" \
        '\.(updateEventStatus|confirmDate|confirmEventDate|confirmPollDate|confirmTimeSlot|setConfirmedDate|updateConfirmedDate)[[:space:]]*\('

    record_matches \
        "SwiftUI views must not assign CONFIRMED status directly" \
        '\.status[[:space:]]*=[[:space:]]*(EventStatus\.)?([Cc][Oo][Nn][Ff][Ii][Rr][Mm][Ee][Dd]|\.confirmed)'

    [ "$failures" -eq 0 ]
}

self_test() {
    local fixtures
    fixtures="$(mktemp -d)"
    trap "rm -rf '$fixtures'" EXIT
    mkdir -p "$fixtures/positive" "$fixtures/negative"
    printf '%s\n' 'struct Bad { func run() { store.confirmEventDate(eventId: "e") } }' > "$fixtures/positive/Bad.swift"
    printf '%s\n' 'struct AlsoBad { var event: Event; mutating func run() { event.status = .confirmed } }' >> "$fixtures/positive/Bad.swift"
    printf '%s\n' 'struct Good { func run() { workflow.dispatch(.submitConfirmation(operationId: "o")) } }' > "$fixtures/negative/Good.swift"

    if scan_root "$fixtures/positive" >/dev/null 2>&1; then
        echo "FAIL: positive architecture fixture was not rejected" >&2
        exit 1
    fi
    if ! scan_root "$fixtures/negative" >/dev/null 2>&1; then
        echo "FAIL: negative architecture fixture was rejected" >&2
        exit 1
    fi
    echo "PASS: poll-confirmation architecture guard fixtures"
}

if [ "${1:-}" = "--self-test" ]; then
    self_test
    exit 0
fi

if ! scan_root "$VIEW_ROOT"; then
    echo "FAIL: iOS poll-confirmation architecture violations found" >&2
    exit 1
fi

echo "PASS: iOS poll confirmation is owned by the typed shared workflow"
