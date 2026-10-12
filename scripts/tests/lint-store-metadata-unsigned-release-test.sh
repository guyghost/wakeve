#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/../.." && pwd)"
fixture_root="$(mktemp -d)"
trap 'rm -rf "$fixture_root"' EXIT
sed '/^# Main$/,$d' "$project_root/scripts/lint-store-metadata.sh" > "$fixture_root/validators.sh"

# This is integration evidence from a real unsigned Release build, never a mock.
APP_STORE_UNSIGNED_RELEASE_EVIDENCE_FILE="$fixture_root/current.json" ruby "$project_root/scripts/capture-unsigned-release-evidence.rb"

run_validator() {
    local evidence="$1"
    bash -c '
        set -euo pipefail
        source "$1"
        PROJECT_DIR="$2"
        PLATFORM=ios
        if [ "$3" = DEFAULT ]; then
            unset APP_STORE_UNSIGNED_RELEASE_EVIDENCE_FILE
        else
            export APP_STORE_UNSIGNED_RELEASE_EVIDENCE_FILE="$3"
        fi
        validate_app_store_release_artifact_evidence
        echo "VALIDATOR_COMPLETED errors=$ERRORS"
    ' unsigned-evidence-test "$fixture_root/validators.sh" "$project_root" "$evidence" > "$fixture_root/output.log" 2>&1
}

run_validator "$fixture_root/current.json"
if ! grep -Fq 'VALIDATOR_COMPLETED errors=0' "$fixture_root/output.log"; then
    cat "$fixture_root/output.log"
    echo 'FAIL: current unsigned artifact evidence was rejected' >&2
    exit 1
fi
grep -Fq 'APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=false' "$fixture_root/output.log"
grep -Fq 'Final App Store signoff: Requires release artifact evidence completion' "$fixture_root/output.log"

expect_rejected() {
    run_validator "$1"
    if grep -Fq 'VALIDATOR_COMPLETED errors=0' "$fixture_root/output.log"; then
        cat "$fixture_root/output.log"
        echo "FAIL: invalid evidence accepted: $1" >&2
        exit 1
    fi
    grep -Fq 'ERROR: App Store release artifact evidence:' "$fixture_root/output.log"
}

for corruption in hash uuid signed schema schema_type; do
    ruby -rjson -e '
        report = JSON.parse(File.read(ARGV[0]))
        case ARGV[2]
        when "hash" then report.fetch("hashes")["app_binary"] = "0" * 64
        when "uuid" then report.fetch("uuids").fetch("app_binary").first["uuid"] = "00000000-0000-0000-0000-000000000000"
        when "signed" then report["unsigned"] = false
        when "schema" then report["schema_version"] = 99
        when "schema_type" then report["schema_version"] = 1.0
        end
        File.write(ARGV[1], JSON.generate(report))
    ' "$fixture_root/current.json" "$fixture_root/$corruption.json" "$corruption"
    expect_rejected "$fixture_root/$corruption.json"
done
expect_rejected "$fixture_root/missing.json"
expect_rejected ""
printf '{malformed' > "$fixture_root/malformed.json"
expect_rejected "$fixture_root/malformed.json"

run_validator DEFAULT
if grep -Fq 'Current unsigned Release artifact hashes and UUIDs verified' "$fixture_root/output.log"; then
    echo 'FAIL: default validation used CI evidence without explicit configuration' >&2
    exit 1
fi
grep -Fq 'Local app UUID is not recorded' "$fixture_root/output.log"
grep -Fq 'hash does not match local unsigned Release artifact' "$fixture_root/output.log"

# Sign only a scratch copy to exercise real signature rejection. Production
# binaries and the original unsigned build remain untouched.
signed_root="$fixture_root/signed-project"
signed_app="$signed_root/build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app"
actual_app="$project_root/build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app"
mkdir -p "$signed_root/scripts" "$signed_app/Frameworks/Shared.framework"
cp "$project_root/scripts/capture-unsigned-release-evidence.rb" "$signed_root/scripts/"
cp "$actual_app/Wakeve" "$signed_app/Wakeve"
cp "$actual_app/Frameworks/Shared.framework/Shared" "$signed_app/Frameworks/Shared.framework/Shared"
cp "$actual_app/Info.plist" "$signed_app/Info.plist"
cp "$actual_app/PrivacyInfo.xcprivacy" "$signed_app/PrivacyInfo.xcprivacy"
/usr/bin/codesign --force --sign - "$signed_app/Wakeve" >/dev/null 2>&1
if ruby "$signed_root/scripts/capture-unsigned-release-evidence.rb" --validate "$fixture_root/current.json" > "$fixture_root/signed.log" 2>&1; then
    echo 'FAIL: actual signed scratch binary accepted' >&2
    exit 1
fi
grep -Fq 'Expected unsigned app_binary' "$fixture_root/signed.log"

echo 'PASS: actual unsigned evidence accepted; corrupt reports and actual signed scratch binary rejected; historical default preserved'

