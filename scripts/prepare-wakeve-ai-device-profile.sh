#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${OUTPUT_DIR:-$PROJECT_DIR/docs/performance}"
TIMESTAMP="$(date -u +"%Y-%m-%dT%H-%M-%SZ")"
REPORT="$OUTPUT_DIR/wakeve-ai-device-profile-$TIMESTAMP.md"
BUNDLE_ID="${BUNDLE_ID:-com.guyghost.wakeve}"
TEAM_ID_VALUE="${TEAM_ID:-${APPLE_TEAM_ID:-}}"

mkdir -p "$OUTPUT_DIR"

sanitize_report() {
    perl -pi -e 'BEGIN { $home = $ENV{"HOME"} // ""; $home = quotemeta($home); } s/\r//g; s/[ \t]+$//; s/$home/~/g if $home ne "";' "$REPORT"
}

device_list="$(xcrun devicectl list devices 2>&1 || true)"
xctrace_devices="$(xcrun xctrace list devices 2>&1 || true)"
codesigning_identities="$(security find-identity -v -p codesigning 2>&1 || true)"
valid_signing_identity_count="$(
    printf '%s\n' "$codesigning_identities" \
        | awk '/valid identities found/ { print $1; found = 1 } END { if (!found) print "0" }'
)"
matching_profiles="$(
    find "$HOME/Library/MobileDevice/Provisioning Profiles" -maxdepth 1 -type f -name '*.mobileprovision' -print 2>/dev/null \
        | while IFS= read -r profile; do
            app_id="$(security cms -D -i "$profile" 2>/dev/null | plutil -extract Entitlements.application-identifier raw -o - - 2>/dev/null || true)"
            if [[ "$app_id" == *."$BUNDLE_ID" || "$app_id" == "$TEAM_ID_VALUE.$BUNDLE_ID" ]]; then
                name="$(security cms -D -i "$profile" 2>/dev/null | plutil -extract Name raw -o - - 2>/dev/null || basename "$profile")"
                team="$(security cms -D -i "$profile" 2>/dev/null | plutil -extract TeamIdentifier.0 raw -o - - 2>/dev/null || true)"
                printf '%s | team=%s | app-id=%s | path=%s\n' "$name" "$team" "$app_id" "$profile"
            fi
        done
)"
physical_ios_devices="$(
    printf '%s\n' "$device_list" \
        | awk '
            NR > 2 && $0 !~ /simulated/ && $0 ~ /iPhone|iPad/ {
                print
            }
        '
)"
trace_ready_ios_devices="$(
    printf '%s\n' "$xctrace_devices" \
        | awk '
            /^== Devices ==/ { section = "devices"; next }
            /^== Devices Offline ==/ { section = "offline"; next }
            /^== Simulators ==/ { section = "simulators"; next }
            section == "devices" && $0 ~ /iPhone|iPad/ {
                print
            }
        '
)"
trace_offline_ios_devices="$(
    printf '%s\n' "$xctrace_devices" \
        | awk '
            /^== Devices ==/ { section = "devices"; next }
            /^== Devices Offline ==/ { section = "offline"; next }
            /^== Simulators ==/ { section = "simulators"; next }
            section == "offline" && $0 ~ /iPhone|iPad/ {
                print
            }
        '
)"

if [ -z "$physical_ios_devices" ]; then
    status="PENDING_PHYSICAL_IOS_DEVICE"
elif [ -n "$trace_ready_ios_devices" ]; then
    if [ -z "$TEAM_ID_VALUE" ] || [ "$valid_signing_identity_count" = "0" ] || [ -z "$matching_profiles" ]; then
        status="SIGNING_NOT_READY"
    else
        status="READY_FOR_DEVICE_TRACE"
    fi
else
    status="PHYSICAL_IOS_DEVICE_NOT_TRACE_READY"
fi

cat > "$REPORT" <<EOF
# WakeveAI Device Profile Preparation

Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ")

Status: \`$status\`

This report supports OpenSpec task \`add-on-device-wakeve-ai\` / \`6.6\`.
It is not completion evidence until a real supported iOS device is profiled with Foundation Models available.

## Detected Devices

### CoreDevice

\`\`\`text
$device_list
\`\`\`

### Xcode Instruments

\`\`\`text
$xctrace_devices
\`\`\`

If a physical iPhone appears in CoreDevice but under \`Devices Offline\` for
Instruments, unlock the device, trust this Mac, and enable Developer Mode in
\`Settings -> Privacy & Security -> Developer Mode\` before profiling.

## Signing Readiness

| Field | Value |
| --- | --- |
| Bundle ID | \`$BUNDLE_ID\` |
| TEAM_ID / APPLE_TEAM_ID environment value | \`${TEAM_ID_VALUE:-missing}\` |
| Valid code signing identities | \`$valid_signing_identity_count\` |
| Matching provisioning profiles | \`$(if [ -n "$matching_profiles" ]; then printf '%s' "present"; else printf '%s' "missing"; fi)\` |
| Generated report can close OpenSpec task | \`no - preparation evidence only\` |

### Code Signing Identities

\`\`\`text
$codesigning_identities
\`\`\`

### Matching Provisioning Profiles

\`\`\`text
$(if [ -n "$matching_profiles" ]; then printf '%s\n' "$matching_profiles"; else printf 'No provisioning profile matched %s.\n' "$BUNDLE_ID"; fi)
\`\`\`

If signing is not ready, configure a concrete \`TEAM_ID\`, install an Apple
Development signing certificate in the login keychain, and ensure the Apple
Developer account has a provisioning profile for \`$BUNDLE_ID\`.

## Required Closure Evidence

Generated reports from this helper are never sufficient on their own. They only
become closure evidence after the table below is filled from a supported
physical device with Foundation Models available and reviewed.

Record all fields below before checking off OpenSpec task \`6.6\`:

| Field | Value |
| --- | --- |
| Device model | TODO |
| Device OS build | TODO |
| Wakeve build configuration | TODO |
| Wakeve build number / commit | TODO |
| Apple Intelligence enabled | TODO |
| Foundation Models availability | TODO: must be \`.available\` |
| Foundation Models model assets ready | TODO |
| Smart Event Draft prompt | TODO: describe fixture without personal data |
| Generation latency | TODO |
| Cancellation latency | TODO |
| Memory before generation | TODO |
| Peak memory during generation | TODO |
| Memory after idle | TODO |
| Production log privacy checked | TODO |
| Instruments trace path | TODO |
| WakeveAI metrics snapshot path or screenshot | TODO |
| Reviewer/date | TODO |

## Recommended Device Flow

1. Install a signed Debug, Release, or TestFlight build on a supported physical iPhone with Apple Intelligence and Foundation Models available.
2. Open Wakeve and navigate to event creation.
3. Confirm Apple Intelligence is enabled and the model availability state is \`.available\` through the WakeveAI availability path or debug instrumentation.
4. Start an Instruments run using Allocations or Leaks plus Time Profiler.
5. Generate a Smart Event Draft from a non-personal fixture, for example: \`Week-end à Lisbonne avec 8 amis en septembre\`.
6. Record generation duration from \`WakeveAIMetricsRecorder\` or the debug metrics UI/log snapshot.
7. Start a second generation and tap cancel while generation is in progress; record cancellation latency.
8. Record memory before generation, peak during generation, and memory after 30 seconds idle.
9. Inspect production-style logs and confirm prompt text, generated text, participant names, votes, addresses, and prices are absent.
10. Save the Instruments trace and update \`docs/implementation/WAKEVE_AI_DEVICE_VERIFICATION.md\` with the final values.

## Non-Closure Conditions

Do not mark \`6.6\` complete if any of these are true:

- Only simulator evidence is available.
- The physical device is connected but not trace-ready in Instruments.
- Xcode signing is missing a development team, valid Apple Development identity, or provisioning profile for the Wakeve bundle ID.
- Apple Intelligence is disabled for the device or account.
- The physical device reports Foundation Models as disabled, not ready, unsupported, or unknown.
- The run uses personal prompt content that cannot be committed or summarized safely.
- Latency is measured without cancellation and memory evidence.
- Logs contain personal prompt text, generated content, participant names, votes, addresses, or prices.
EOF

sanitize_report

echo "$REPORT"
