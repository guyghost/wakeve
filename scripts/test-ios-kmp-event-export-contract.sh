#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

EVENT_MODEL="$PROJECT_DIR/shared/src/commonMain/kotlin/com/guyghost/wakeve/models/Event.kt"
SWIFT_ALIASES="$PROJECT_DIR/iosApp/src/Models/SharedModels.swift"
IOS_SOURCE_ROOT="$PROJECT_DIR/iosApp/src"

fail() {
    echo "FAIL: $*" >&2
    exit 1
}

if ! grep -Fq '@ObjCName(swiftName = "WakeveEvent")' "$EVENT_MODEL" \
    || ! grep -Eq '^[[:space:]]*data class Event\b' "$EVENT_MODEL"; then
    fail "The KMP domain Event model must export to Swift as WakeveEvent"
fi

if ! grep -Eq '^[[:space:]]*typealias[[:space:]]+Event[[:space:]]*=[[:space:]]*WakeveEvent[[:space:]]*$' "$SWIFT_ALIASES"; then
    fail "SharedModels.swift must expose the canonical Swift alias: typealias Event = WakeveEvent"
fi

legacy_references="$(rg -n --glob '*.swift' '\bEvent_\b' "$IOS_SOURCE_ROOT" || true)"
if [ -n "$legacy_references" ]; then
    echo "$legacy_references" >&2
    fail "Active iOS Swift sources must consume the KMP domain model through Event, not the obsolete Event_ export"
fi

echo "PASS: iOS Swift sources consume the KMP Event model through the WakeveEvent alias"
