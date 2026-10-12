#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/../.." && pwd)"
fixture_root="$(mktemp -d)"
trap 'rm -rf "$fixture_root"' EXIT

# Load the real validator definitions without running unrelated store checks.
sed '/^# Main$/,$d' "$project_root/scripts/lint-store-metadata.sh" > "$fixture_root/validators.sh"
mkdir -p "$fixture_root/project/docs" "$fixture_root/project/scripts" "$fixture_root/bin"
for filename in APP_STORE_OBSERVABILITY_EVIDENCE.md APP_STORE_TESTFLIGHT_EVIDENCE.md APP_STORE_LAUNCH_CHECKLIST.md APP_STORE_FINAL_SIGNOFF.md; do
    cp "$project_root/docs/$filename" "$fixture_root/project/docs/$filename"
done
cp "$project_root/scripts/app-store-submission-audit.sh" "$fixture_root/project/scripts/"
# Artifact availability is the subject of this test; signing is deliberately absent.
printf '#!/usr/bin/env bash\nexit 1\n' > "$fixture_root/bin/codesign"
chmod +x "$fixture_root/bin/codesign"

run_validator() {
    bash -c '
        set -euo pipefail
        source "$1"
        PROJECT_DIR="$2"
        PLATFORM=ios
        export PATH="$3:$PATH"
        validate_app_store_observability_evidence
        echo "VALIDATOR_COMPLETED errors=$ERRORS"
    ' observability-test "$fixture_root/validators.sh" "$fixture_root/project" "$fixture_root/bin" > "$fixture_root/output.log" 2>&1
}

if ! run_validator; then
    cat "$fixture_root/output.log"
    echo "FAIL: absent optional build directory aborted validation" >&2
    exit 1
fi
grep -Fq 'VALIDATOR_COMPLETED errors=0' "$fixture_root/output.log"
grep -Fq 'No signed IPA/archive found in local build scan' "$fixture_root/output.log"
grep -Fq 'Requires observability evidence when TESTFLIGHT_SMOKE_PASSED is true' "$fixture_root/output.log"

mkdir -p "$fixture_root/project/build/release"
run_validator
grep -Fq 'VALIDATOR_COMPLETED errors=0' "$fixture_root/output.log"

for artifact in Signed.ipa Signed.xcarchive; do
    if [[ "$artifact" == *.ipa ]]; then
        touch "$fixture_root/project/build/release/$artifact"
    else
        mkdir "$fixture_root/project/build/release/$artifact"
    fi
    run_validator
    grep -Fq 'Local IPA/archive needs signed-build observability record' "$fixture_root/output.log"
    grep -Fq 'VALIDATOR_COMPLETED errors=1' "$fixture_root/output.log"
    rm -rf "$fixture_root/project/build/release/$artifact"
done
echo 'PASS: missing build directory remains optional; signed artifacts still require evidence'
