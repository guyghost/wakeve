#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${OUTPUT_DIR:-$PROJECT_DIR/docs/weatherkit}"
TIMESTAMP="$(date -u +"%Y-%m-%dT%H-%M-%SZ")"
REPORT="$OUTPUT_DIR/weatherkit-device-validation-$TIMESTAMP.md"
BUNDLE_ID="${BUNDLE_ID:-com.guyghost.wakeve}"
TEAM_ID_VALUE="${TEAM_ID:-${APPLE_TEAM_ID:-}}"
ENTITLEMENTS_FILE="$PROJECT_DIR/iosApp/src/Wakeve.entitlements"

mkdir -p "$OUTPUT_DIR"

device_list="$(xcrun devicectl list devices 2>&1 || true)"
xctrace_devices="$(xcrun xctrace list devices 2>&1 || true)"
codesigning_identities="$(security find-identity -v -p codesigning 2>&1 || true)"
valid_signing_identity_count="$(
    printf '%s\n' "$codesigning_identities" \
        | awk '/valid identities found/ { print $1; found = 1 } END { if (!found) print "0" }'
)"
source_weatherkit="$(
    plutil -extract 'com\.apple\.developer\.weatherkit' raw -o - "$ENTITLEMENTS_FILE" 2>/dev/null || true
)"
source_associated_domains="$(
    plutil -extract 'com\.apple\.developer\.associated-domains' xml1 -o - "$ENTITLEMENTS_FILE" 2>/dev/null || true
)"

matching_profiles="$(
    find "$HOME/Library/MobileDevice/Provisioning Profiles" -maxdepth 1 -type f -name '*.mobileprovision' -print 2>/dev/null \
        | while IFS= read -r profile; do
            decoded="$(security cms -D -i "$profile" 2>/dev/null || true)"
            if [ -z "$decoded" ]; then
                continue
            fi
            app_id="$(printf '%s' "$decoded" | plutil -extract Entitlements.application-identifier raw -o - - 2>/dev/null || true)"
            if [[ "$app_id" == *."$BUNDLE_ID" || "$app_id" == "$TEAM_ID_VALUE.$BUNDLE_ID" ]]; then
                name="$(printf '%s' "$decoded" | plutil -extract Name raw -o - - 2>/dev/null || basename "$profile")"
                team="$(printf '%s' "$decoded" | plutil -extract TeamIdentifier.0 raw -o - - 2>/dev/null || true)"
                weatherkit="$(printf '%s' "$decoded" | plutil -extract 'Entitlements.com\.apple\.developer\.weatherkit' raw -o - - 2>/dev/null || true)"
                printf '%s | team=%s | app-id=%s | weatherkit=%s | path=%s\n' "$name" "$team" "$app_id" "${weatherkit:-missing}" "$profile"
            fi
        done
)"
matching_weatherkit_profiles="$(
    printf '%s\n' "$matching_profiles" | grep -E 'weatherkit=true($| )' || true
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

if [ "$source_weatherkit" != "true" ]; then
    status="SOURCE_WEATHERKIT_ENTITLEMENT_MISSING"
elif [ -z "$physical_ios_devices" ]; then
    status="PENDING_PHYSICAL_IOS_DEVICE"
elif [ -z "$trace_ready_ios_devices" ]; then
    status="PHYSICAL_IOS_DEVICE_NOT_TRACE_READY"
elif [ -z "$TEAM_ID_VALUE" ] || [ "$valid_signing_identity_count" = "0" ] || [ -z "$matching_profiles" ]; then
    status="SIGNING_NOT_READY"
elif [ -z "$matching_weatherkit_profiles" ]; then
    status="WEATHERKIT_PROFILE_NOT_CONFIRMED"
else
    status="READY_FOR_WEATHERKIT_DEVICE_VALIDATION"
fi

cat > "$REPORT" <<EOF
# WeatherKit Device Validation Preparation

Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ")

Status: \`$status\`

This report supports OpenSpec tasks \`add-event-weather-forecast\` / \`1.2\` and \`6.2\`.
It is not completion evidence until a signed build on a real iOS device proves WeatherKit entitlement availability for bundle \`$BUNDLE_ID\`.

## Source Entitlement Check

| Field | Value |
| --- | --- |
| Entitlements file | \`iosApp/src/Wakeve.entitlements\` |
| Source WeatherKit entitlement | \`${source_weatherkit:-missing}\` |
| Bundle ID | \`$BUNDLE_ID\` |
| TEAM_ID / APPLE_TEAM_ID environment value | \`${TEAM_ID_VALUE:-missing}\` |
| Valid code signing identities | \`$valid_signing_identity_count\` |
| Matching provisioning profiles | \`$(if [ -n "$matching_profiles" ]; then printf '%s' "present"; else printf '%s' "missing"; fi)\` |
| Matching profiles with WeatherKit entitlement | \`$(if [ -n "$matching_weatherkit_profiles" ]; then printf '%s' "present"; else printf '%s' "missing"; fi)\` |

### Associated Domains Source Entitlement

\`\`\`xml
$(if [ -n "$source_associated_domains" ]; then printf '%s\n' "$source_associated_domains"; else printf 'missing\n'; fi)
\`\`\`

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
\`Settings -> Privacy & Security -> Developer Mode\`.

## Signing Readiness

### Code Signing Identities

\`\`\`text
$codesigning_identities
\`\`\`

### Matching Provisioning Profiles

\`\`\`text
$(if [ -n "$matching_profiles" ]; then printf '%s\n' "$matching_profiles"; else printf 'No provisioning profile matched %s.\n' "$BUNDLE_ID"; fi)
\`\`\`

The profile used for final validation must include both:

- \`Entitlements.application-identifier = <TEAM_ID>.$BUNDLE_ID\`
- \`Entitlements.com.apple.developer.weatherkit = true\`

## Required Closure Evidence

Record all fields below before checking off OpenSpec tasks \`1.2\` and \`6.2\`:

| Field | Value |
| --- | --- |
| Apple Developer Team ID | TODO |
| App ID / Identifier screenshot or API evidence | TODO |
| WeatherKit capability enabled on App ID | TODO |
| Provisioning profile name and UUID | TODO |
| Provisioning profile contains WeatherKit entitlement | TODO |
| Device model | TODO |
| Device OS build | TODO |
| Wakeve build configuration | TODO |
| Wakeve commit / build number | TODO |
| Signed app entitlement inspection path | TODO |
| WeatherKit request fixture | TODO: non-personal location/date |
| WeatherKit request result | TODO: success or mapped entitlement/provider state |
| iOS event weather UI validation result | TODO |
| Reviewer/date | TODO |

## Recommended Device Flow

1. Enable WeatherKit for the explicit App ID \`$BUNDLE_ID\` in the Apple Developer portal.
2. Regenerate and install a development or distribution provisioning profile that includes WeatherKit.
3. Set \`TEAM_ID\` or \`APPLE_TEAM_ID\` to the real 10-character Apple Developer Team ID.
4. Build and install Wakeve on a physical iPhone using the regenerated profile.
5. Inspect the signed app entitlements and confirm \`com.apple.developer.weatherkit = true\`.
6. Open an event or scenario with a non-personal location/date fixture inside WeatherKit's supported forecast window.
7. Verify the UI reaches the available weather state or records the exact mapped provider/entitlement error.
8. Save the device log, signed entitlement inspection, and screenshot/result bundle path.
9. Update \`docs/implementation/WEATHERKIT_DEVICE_VALIDATION.md\` with final values.

## Non-Closure Conditions

Do not mark \`1.2\` or \`6.2\` complete if any of these are true:

- Only source entitlement or simulator evidence is available.
- The Apple Developer App ID capability has not been confirmed.
- The provisioning profile does not include \`com.apple.developer.weatherkit = true\`.
- The signed app entitlements were not inspected.
- The physical device is unavailable, offline in Instruments, or cannot install the signed build.
- WeatherKit was not exercised on a real device or TestFlight-equivalent signed build.
- The report uses personal location, participant, vote, address, price, or chat data that cannot be safely committed.
EOF

echo "$REPORT"
