#!/usr/bin/env bash
# Final non-uploading App Store submission audit for Wakeve.
# It aggregates local gates, external configuration checks, and manual signoffs.

set -euo pipefail

RUN_PREFLIGHT=true
CHECK_LIVE_URLS=false
RUN_SUBMISSION_READY=false

BLOCKERS=0
WARNINGS=0

usage() {
    cat <<'EOF'
Usage: ./scripts/app-store-submission-audit.sh [options]

Options:
  --skip-preflight        Do not run bundle exec fastlane ios preflight
  --check-live-urls       Check wakeve.app, AASA, and api.wakeve.app live endpoints
  --run-submission-ready  Run bundle exec fastlane ios submission_ready
  --help, -h              Show this help

This script does not upload to App Store Connect.
It exits non-zero while any submission blocker remains.
The final audit cannot pass unless local preflight, live URL validation, and submission_ready are run.
EOF
}

for arg in "$@"; do
    case "$arg" in
        --skip-preflight) RUN_PREFLIGHT=false ;;
        --check-live-urls) CHECK_LIVE_URLS=true ;;
        --run-submission-ready) RUN_SUBMISSION_READY=true ;;
        --help|-h) usage; exit 0 ;;
        *) echo "Unknown option: $arg" >&2; usage; exit 2 ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

note() {
    echo "INFO: $1"
}

pass() {
    echo "PASS: $1"
}

warn() {
    WARNINGS=$((WARNINGS + 1))
    echo "WARN: $1"
}

blocker() {
    BLOCKERS=$((BLOCKERS + 1))
    echo "BLOCKER: $1"
}

valid_review_phone() {
    local value="$1"
    local normalized digit_count
    normalized=$(printf '%s' "$value" | tr -d ' .()-')
    digit_count=$(printf '%s' "$normalized" | tr -cd '0-9' | wc -c | tr -d ' ')

    [[ "$normalized" =~ ^\+?[0-9]+$ ]] && [ "$digit_count" -ge 4 ] && [ "$digit_count" -le 20 ]
}

placeholder_review_phone() {
    local value="$1"
    local normalized
    normalized=$(printf '%s' "$value" | tr -d ' .()-')

    [ "$normalized" = "+15551234567" ] || [ "$normalized" = "15551234567" ]
}

truthy_env() {
    local name="$1"
    local value="${!name:-}"
    local normalized
    normalized=$(printf '%s' "$value" | tr '[:upper:]' '[:lower:]')

    case "$normalized" in
        1|true|yes|y) return 0 ;;
        *) return 1 ;;
    esac
}

require_file() {
    local path="$1"
    local label="$2"

    if [ -f "$PROJECT_DIR/$path" ]; then
        pass "$label exists"
    else
        blocker "$label is missing at $path"
    fi
}

require_file_contains() {
    local path="$1"
    local phrase="$2"
    local label="$3"

    if [ ! -f "$PROJECT_DIR/$path" ]; then
        blocker "$label cannot be checked because $path is missing"
        return 0
    fi

    if grep -Fq "$phrase" "$PROJECT_DIR/$path"; then
        pass "$label"
    else
        blocker "$label is missing '$phrase'"
    fi
}

require_env() {
    local name="$1"
    local label="$2"

    if [ -n "${!name:-}" ]; then
        pass "$label is set"
    else
        blocker "$label is not set ($name)"
    fi
}

validate_apple_release_env_values() {
    if [ -n "${APPLE_ID:-}" ]; then
        if [[ "$APPLE_ID" =~ ^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$ ]]; then
            pass "APPLE_ID format is plausible"
            if [ "$APPLE_ID" = "release@example.com" ]; then
                blocker "APPLE_ID uses the documented placeholder release@example.com; replace it with the real App Store Connect Apple account before final audit"
            fi
        else
            blocker "APPLE_ID must be an Apple account email address"
        fi
    fi

    if [ -n "${ITC_TEAM_ID:-}" ]; then
        if [[ "$ITC_TEAM_ID" =~ ^[0-9]+$ ]]; then
            pass "ITC_TEAM_ID format is numeric"
            if [ "$ITC_TEAM_ID" = "123456789" ]; then
                blocker "ITC_TEAM_ID uses the documented placeholder 123456789; replace it with the real App Store Connect team ID before final audit"
            fi
        else
            blocker "ITC_TEAM_ID must be numeric"
        fi
    fi

    if [ -n "${TEAM_ID:-}" ]; then
        if [[ "$TEAM_ID" =~ ^[A-Z0-9]{10}$ ]]; then
            pass "TEAM_ID format is a 10-character Apple Developer Team ID"
            if [ "$TEAM_ID" = "ABCDE12345" ]; then
                blocker "TEAM_ID uses the documented placeholder ABCDE12345; replace it with the real Apple Developer Team ID before final audit"
            fi
        else
            blocker "TEAM_ID must be a 10-character uppercase Apple Developer Team ID"
        fi
    fi

    if [ -n "${APPLE_TEAM_ID:-}" ]; then
        if [[ "$APPLE_TEAM_ID" =~ ^[A-Z0-9]{10}$ ]]; then
            pass "APPLE_TEAM_ID format is a 10-character Apple Developer Team ID"
            if [ "$APPLE_TEAM_ID" = "ABCDE12345" ]; then
                blocker "APPLE_TEAM_ID uses the documented placeholder ABCDE12345; replace it with the real Apple Team ID before final audit"
            fi
        else
            blocker "APPLE_TEAM_ID must be a 10-character uppercase Apple Developer Team ID"
        fi
    fi

    if [ -n "${TEAM_ID:-}" ] && [ -n "${APPLE_TEAM_ID:-}" ]; then
        if [ "$TEAM_ID" = "$APPLE_TEAM_ID" ]; then
            pass "TEAM_ID and APPLE_TEAM_ID match for signing and AASA"
        else
            blocker "TEAM_ID and APPLE_TEAM_ID must match for this release so AASA app IDs match the signed app"
        fi
    fi
}

require_truthy_env() {
    local name="$1"
    local label="$2"

    if truthy_env "$name"; then
        pass "$label is confirmed"
    else
        blocker "$label is not confirmed ($name=true)"
    fi
}

require_complete_evidence_record() {
    local path="$1"
    local marker="$2"
    local label="$3"
    local full_path="$PROJECT_DIR/$path"

    if [ ! -f "$full_path" ]; then
        blocker "$label is missing at $path"
        return 0
    fi

    if grep -Fxq "$marker" "$full_path"; then
        pass "$label completion marker is true"
    else
        blocker "$label is not complete ($marker)"
        return 0
    fi

    if grep -Eiq '\b(TBD|Pending)\b|Status: NOT|Status: not confirmed|\[ \]' "$full_path"; then
        blocker "$label still contains unresolved TBD/Pending/status/checklist placeholders"
    else
        pass "$label has no unresolved TBD/Pending/status/checklist placeholders"
    fi
}

require_source_match() {
    local label="$1"
    local pattern="$2"
    shift 2
    local existing_paths=()
    local path

    for path in "$@"; do
        if [ -e "$PROJECT_DIR/$path" ]; then
            existing_paths+=("$PROJECT_DIR/$path")
        fi
    done

    if [ "${#existing_paths[@]}" -eq 0 ]; then
        blocker "$label cannot be checked because source path is missing"
        return 0
    fi

    if command -v rg >/dev/null 2>&1; then
        if rg -n --glob '!**/Shared.framework/**' --glob '!**/build/**' "$pattern" "${existing_paths[@]}" >/dev/null 2>&1; then
            pass "$label"
        else
            blocker "$label is not present"
        fi
        return 0
    fi

    if grep -R -E "$pattern" "${existing_paths[@]}" >/dev/null 2>&1; then
        pass "$label"
    else
        blocker "$label is not present"
    fi
}

require_openspec_tasks_complete() {
    local change_id="$1"
    local label="$2"
    local active_tasks="$PROJECT_DIR/openspec/changes/$change_id/tasks.md"
    local archived_tasks=""

    if [ -f "$active_tasks" ]; then
        if grep -Eq '^- \[ \]' "$active_tasks"; then
            blocker "$label OpenSpec tasks still contain unchecked items (openspec/changes/$change_id/tasks.md)"
        else
            pass "$label OpenSpec tasks have no unchecked items"
        fi
        return 0
    fi

    if [ -d "$PROJECT_DIR/openspec/archive" ]; then
        archived_tasks=$(find "$PROJECT_DIR/openspec/archive" -type f -path "*/tasks.md" | grep -E "/[0-9]{4}-[0-9]{2}-[0-9]{2}-${change_id}/tasks\.md$" | sort | tail -n 1 || true)
    fi

    if [ -z "$archived_tasks" ]; then
        blocker "$label OpenSpec tasks are missing; expected active or archived change $change_id"
        return 0
    fi

    if grep -Eq '^- \[ \]' "$archived_tasks"; then
        blocker "$label archived OpenSpec tasks still contain unchecked items (${archived_tasks#$PROJECT_DIR/})"
    else
        pass "$label archived OpenSpec tasks have no unchecked items"
    fi
}

require_openspec_change_valid() {
    local change_id="$1"
    local label="$2"

    if ! command -v openspec >/dev/null 2>&1; then
        blocker "$label OpenSpec proposal cannot be validated because openspec is unavailable"
        return 0
    fi

    if (cd "$PROJECT_DIR" && openspec validate "$change_id" --strict >/dev/null 2>&1); then
        pass "$label OpenSpec proposal validates strictly"
    else
        blocker "$label OpenSpec proposal does not validate strictly ($change_id)"
    fi
}

require_account_deletion_implementation_if_confirmed() {
    if ! truthy_env "APP_STORE_ACCOUNT_DELETION_CONFIRMED"; then
        return 0
    fi

    note "Checking account deletion implementation evidence"
    require_openspec_tasks_complete "add-in-app-account-deletion" "Account deletion"
    require_source_match \
        "Account deletion backend tests cover deletion behavior" \
        "(?i)(account deletion|delete account|deleteAccount|deleteUserAccount|/api/user/delete)" \
        "server/src/test" "shared/src/commonTest"
    require_source_match \
        "Account deletion backend tests cover collaborative anonymization" \
        "(?i)(anonymizes shared collaborative records|Deleted user|deleted_user_|providerRevocationStatus|localCleanupRequired)" \
        "server/src/test" "shared/src/commonTest"
    require_source_match \
        "Account deletion backend route or service is present" \
        "(?i)(deleteAccount|deleteUserAccount|accountDeletion|/api/user/delete|delete-account)" \
        "server/src/main/kotlin" "shared/src/commonMain/kotlin"
    require_source_match \
        "Account deletion stable route is DELETE /api/user/delete" \
        'delete\("/user/delete"\)' \
        "server/src/main/kotlin"
    require_source_match \
        "Account deletion response includes local cleanup and provider revocation status" \
        "(?i)(localCleanupRequired|providerRevocationStatus)" \
        "server/src/main/kotlin" "shared/src/commonMain/kotlin"
    require_source_match \
        "Account deletion collaborative anonymization is present" \
        "(?i)(anonymizeOrganizer|anonymizeParticipantUser|anonymizeCommentsByAuthor|anonymizeMessagesBySender|Deleted user|deleted_user_)" \
        "shared/src/commonMain/kotlin" "shared/src/commonMain/sqldelight"
    require_source_match \
        "Account deletion iOS entry point is present" \
        "(?i)(Delete Account|deleteAccount|Delete Guest Data|Data Management)" \
        "iosApp/src"
    require_source_match \
        "Account deletion iOS Data Management screen is present" \
        "(?i)(DataManagementView|data_management\\.delete_account|data_management\\.delete_guest_data|confirmationDialog)" \
        "iosApp/src"
    require_source_match \
        "Account deletion iOS authenticated and guest actions are present" \
        "(?i)(deleteCurrentAccount|deleteGuestData|Delete Guest Data)" \
        "iosApp/src"
    require_source_match \
        "Account deletion local cleanup is present" \
        "(?i)(clear.*Keychain|Keychain.*clear|clear.*token|remove.*token|guest.*delete|delete.*guest|analytics.*clear|clear.*profile)" \
        "iosApp/src" "shared/src/iosMain/kotlin" "shared/src/commonMain/kotlin"
    require_source_match \
        "Account deletion iOS cleanup waits for backend success" \
        "(?i)(try await authService\\.deleteAccount\\(\\)|await completeLocalAccountDeletion\\(\\)|clearLocalAccountData\\(\\))" \
        "iosApp/src"
}

require_account_deletion_evidence_if_confirmed() {
    if ! truthy_env "APP_STORE_ACCOUNT_DELETION_CONFIRMED"; then
        return 0
    fi

    note "Checking App Store account deletion evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md" \
        "APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true" \
        "App Store account deletion evidence"
}

require_ugc_moderation_implementation_if_confirmed() {
    if ! truthy_env "APP_STORE_UGC_MODERATION_CONFIRMED"; then
        return 0
    fi

    note "Checking UGC moderation implementation evidence"
    require_openspec_tasks_complete "add-ugc-moderation-controls" "UGC moderation"
    require_source_match \
        "UGC moderation tests cover report/block/unblock/filter behavior" \
        "(?i)(ModerationStatus|ReportReason|ContentReport|UserBlock|reportContent|reportOffensive|blockUser|unblockUser|blockedUser|moderation policy)" \
        "server/src/test" "shared/src/commonTest"
    require_source_match \
        "UGC moderation shared/server models are present" \
        "(?i)(ModerationStatus|ReportTarget|ReportReason|UserBlock|ContentReport|ModerationDecision)" \
        "shared/src/commonMain/kotlin" "server/src/main/kotlin"
    require_source_match \
        "UGC moderation server policy or report/block/unblock endpoints are present" \
        "(?i)(moderationPolicy|ModerationService|reportContent|contentReport|blockUser|unblockUser|userBlock|/reports|/blocks)" \
        "server/src/main/kotlin" "shared/src/commonMain/kotlin"
    require_source_match \
        "UGC moderation iOS report/block/unblock entry points are present" \
        "(?i)(Report Abuse|Report Content|Block User|Unblock User|reportUser|blockUser|unblockUser|ReportReason|abuse report|moderation)" \
        "iosApp/src"
    require_source_match \
        "UGC moderation iOS hidden/pending/rejected states are present" \
        "(?i)(ModerationStatusBadge|moderatedCommentNotice|pending_review|rejected_content_notice|hidden_content_notice)" \
        "iosApp/src"
}

require_ugc_moderation_evidence_if_confirmed() {
    if ! truthy_env "APP_STORE_UGC_MODERATION_CONFIRMED"; then
        return 0
    fi

    note "Checking App Store UGC moderation evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_UGC_MODERATION_EVIDENCE.md" \
        "APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true" \
        "App Store UGC moderation evidence"
}

require_final_signoff_evidence_matrix() {
    local signoff_path="docs/APP_STORE_FINAL_SIGNOFF.md"
    local blocker_number blocker_id

    note "Checking final signoff evidence matrix"
    require_file_contains "$signoff_path" "## Blocker Evidence Matrix" "Final signoff blocker evidence matrix exists"

    for blocker_number in $(seq 1 22); do
        blocker_id=$(printf "AS-%02d" "$blocker_number")
        require_file_contains "$signoff_path" "| $blocker_id |" "Final signoff evidence matrix covers $blocker_id"
    done

    local required_evidence_phrases=(
        "APPLE_ID"
        "ITC_TEAM_ID"
        "APP_STORE_ACCOUNT_ACCESS_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md"
        "TEAM_ID"
        "APPLE_TEAM_ID"
        "APP_REVIEW_PHONE_NUMBER"
        "APP_STORE_REVIEW_ACCESS_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md"
        "APP_STORE_PRIVACY_SIGNOFF=true"
        "APP_STORE_PRIVACY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_PRIVACY_EVIDENCE.md"
        "APP_STORE_ACCESSIBILITY_SIGNOFF=true"
        "APP_STORE_ACCESSIBILITY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md"
        "APP_STORE_AVAILABILITY_CONFIRMED=true"
        "APP_STORE_AVAILABILITY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_AVAILABILITY_EVIDENCE.md"
        "APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_DSA_TRADER_STATUS.md"
        "APP_STORE_PRICING_AVAILABILITY_CONFIRMED=true"
        "APP_STORE_PRICING_AVAILABILITY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md"
        "APP_STORE_SDK_PRIVACY_CONFIRMED=true"
        "APP_STORE_SDK_PRIVACY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md"
        "APP_STORE_RELEASE_CONTROL_CONFIRMED=true"
        "APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md"
        "APP_STORE_MEDIA_LOCALIZATION_CONFIRMED=true"
        "APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md"
        "APP_STORE_LICENSE_NOTICES_CONFIRMED=true"
        "APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md"
        "APP_STORE_EULA_CONFIRMED=true"
        "APP_STORE_EULA_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_EULA_EVIDENCE.md"
        "APP_STORE_ACCOUNT_DELETION_CONFIRMED=true"
        "APP_STORE_UGC_MODERATION_CONFIRMED=true"
        "APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=true"
        "APP_STORE_PAYMENT_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_PAYMENT_EVIDENCE.md"
        "TESTFLIGHT_SMOKE_PASSED=true"
        "TESTFLIGHT_SMOKE_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_TESTFLIGHT_EVIDENCE.md"
        "APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_OBSERVABILITY_EVIDENCE.md"
        "APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md"
        "APP_STORE_CAPABILITIES_CONFIRMED=true"
        "APP_STORE_CAPABILITIES_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_CAPABILITIES_EVIDENCE.md"
        "APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md"
        "APP_STORE_APP_INFORMATION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_APP_INFORMATION_EVIDENCE.md"
        "APP_STORE_VERSIONING_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_VERSIONING_EVIDENCE.md"
        "APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md"
        "APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md"
        "./scripts/app-store-submission-audit.sh --check-live-urls"
        "./scripts/app-store-submission-audit.sh --run-submission-ready"
    )

    local phrase
    for phrase in "${required_evidence_phrases[@]}"; do
        require_file_contains "$signoff_path" "$phrase" "Final signoff evidence matrix covers $phrase"
    done
}

require_availability_evidence_if_confirmed() {
    if ! truthy_env "APP_STORE_AVAILABILITY_CONFIRMED"; then
        return 0
    fi

    note "Checking App Store availability evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_AVAILABILITY_EVIDENCE.md" \
        "APP_STORE_AVAILABILITY_EVIDENCE_COMPLETE=true" \
        "App Store availability evidence"
}

require_accessibility_evidence_if_confirmed() {
    if ! truthy_env "APP_STORE_ACCESSIBILITY_SIGNOFF"; then
        return 0
    fi

    note "Checking App Store accessibility evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md" \
        "APP_STORE_ACCESSIBILITY_EVIDENCE_COMPLETE=true" \
        "App Store accessibility evidence"
}

require_privacy_evidence_if_confirmed() {
    if ! truthy_env "APP_STORE_PRIVACY_SIGNOFF"; then
        return 0
    fi

    note "Checking App Store privacy evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_PRIVACY_EVIDENCE.md" \
        "APP_STORE_PRIVACY_EVIDENCE_COMPLETE=true" \
        "App Store privacy evidence"
}

require_dsa_evidence_if_confirmed() {
    if ! truthy_env "APP_STORE_DSA_TRADER_STATUS_CONFIRMED"; then
        return 0
    fi

    note "Checking App Store DSA trader status evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_DSA_TRADER_STATUS.md" \
        "APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE=true" \
        "App Store DSA trader status evidence"
}

require_pricing_availability_evidence_if_confirmed() {
    if ! truthy_env "APP_STORE_PRICING_AVAILABILITY_CONFIRMED"; then
        return 0
    fi

    note "Checking App Store pricing and availability evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md" \
        "APP_STORE_PRICING_AVAILABILITY_EVIDENCE_COMPLETE=true" \
        "App Store pricing and availability evidence"
}

require_sdk_privacy_evidence_if_confirmed() {
    if ! truthy_env "APP_STORE_SDK_PRIVACY_CONFIRMED"; then
        return 0
    fi

    note "Checking App Store SDK privacy and signature evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md" \
        "APP_STORE_SDK_PRIVACY_EVIDENCE_COMPLETE=true" \
        "App Store SDK privacy and signature evidence"
}

require_release_control_evidence_if_confirmed() {
    if ! truthy_env "APP_STORE_RELEASE_CONTROL_CONFIRMED"; then
        return 0
    fi

    note "Checking App Store release control evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md" \
        "APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=true" \
        "App Store release control evidence"
}

require_media_localization_evidence_if_confirmed() {
    if ! truthy_env "APP_STORE_MEDIA_LOCALIZATION_CONFIRMED"; then
        return 0
    fi

    note "Checking App Store media and localization evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md" \
        "APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=true" \
        "App Store media and localization evidence"
}

require_license_notices_evidence_if_confirmed() {
    if ! truthy_env "APP_STORE_LICENSE_NOTICES_CONFIRMED"; then
        return 0
    fi

    note "Checking App Store license notices evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md" \
        "APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true" \
        "App Store license notices evidence"
}

require_eula_evidence_if_confirmed() {
    if ! truthy_env "APP_STORE_EULA_CONFIRMED"; then
        return 0
    fi

    note "Checking App Store EULA evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_EULA_EVIDENCE.md" \
        "APP_STORE_EULA_EVIDENCE_COMPLETE=true" \
        "App Store EULA evidence"
}

require_payment_evidence_if_confirmed() {
    if ! truthy_env "APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED"; then
        return 0
    fi

    note "Checking App Store payment evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_PAYMENT_EVIDENCE.md" \
        "APP_STORE_PAYMENT_EVIDENCE_COMPLETE=true" \
        "App Store payment evidence"
}

require_capabilities_evidence_if_confirmed() {
    if ! truthy_env "APP_STORE_CAPABILITIES_CONFIRMED"; then
        return 0
    fi

    note "Checking Apple Developer capabilities evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_CAPABILITIES_EVIDENCE.md" \
        "APP_STORE_CAPABILITIES_EVIDENCE_COMPLETE=true" \
        "Apple Developer capabilities evidence"
}

require_testflight_evidence_if_confirmed() {
    if ! truthy_env "TESTFLIGHT_SMOKE_PASSED"; then
        return 0
    fi

    note "Checking TestFlight smoke evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_TESTFLIGHT_EVIDENCE.md" \
        "TESTFLIGHT_SMOKE_EVIDENCE_COMPLETE=true" \
        "TestFlight smoke evidence"
}

require_observability_evidence_if_testflight_confirmed() {
    if ! truthy_env "TESTFLIGHT_SMOKE_PASSED"; then
        return 0
    fi

    note "Checking App Store observability evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_OBSERVABILITY_EVIDENCE.md" \
        "APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=true" \
        "App Store observability evidence"
}

require_live_url_aasa_evidence_for_final_release() {
    if ! final_signoff_record_complete && ! all_manual_release_signoffs_confirmed; then
        return 0
    fi

    note "Checking App Store live URL and AASA evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md" \
        "APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true" \
        "App Store live URL and AASA evidence"
}

final_signoff_record_complete() {
    local signoff_path="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"

    [ -f "$signoff_path" ] && grep -Fxq "APP_STORE_FINAL_SIGNOFF_COMPLETE=true" "$signoff_path"
}

all_manual_release_signoffs_confirmed() {
    truthy_env "APP_STORE_PRIVACY_SIGNOFF" &&
        truthy_env "APP_STORE_ACCESSIBILITY_SIGNOFF" &&
        truthy_env "APP_STORE_AVAILABILITY_CONFIRMED" &&
        truthy_env "APP_STORE_DSA_TRADER_STATUS_CONFIRMED" &&
        truthy_env "APP_STORE_PRICING_AVAILABILITY_CONFIRMED" &&
        truthy_env "APP_STORE_SDK_PRIVACY_CONFIRMED" &&
        truthy_env "APP_STORE_RELEASE_CONTROL_CONFIRMED" &&
        truthy_env "APP_STORE_MEDIA_LOCALIZATION_CONFIRMED" &&
        truthy_env "APP_STORE_LICENSE_NOTICES_CONFIRMED" &&
        truthy_env "APP_STORE_EULA_CONFIRMED" &&
        truthy_env "APP_STORE_ACCOUNT_DELETION_CONFIRMED" &&
        truthy_env "APP_STORE_UGC_MODERATION_CONFIRMED" &&
        truthy_env "APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED" &&
        truthy_env "TESTFLIGHT_SMOKE_PASSED" &&
        truthy_env "APP_STORE_CAPABILITIES_CONFIRMED"
}

require_review_access_evidence_for_final_release() {
    if ! final_signoff_record_complete && ! all_manual_release_signoffs_confirmed; then
        return 0
    fi

    note "Checking App Store review access evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md" \
        "APP_STORE_REVIEW_ACCESS_EVIDENCE_COMPLETE=true" \
        "App Store review access evidence"
}

require_export_compliance_evidence_for_final_release() {
    if ! final_signoff_record_complete && ! all_manual_release_signoffs_confirmed; then
        return 0
    fi

    note "Checking App Store export compliance evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md" \
        "APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true" \
        "App Store export compliance evidence"
}

require_app_information_evidence_for_final_release() {
    if ! final_signoff_record_complete && ! all_manual_release_signoffs_confirmed; then
        return 0
    fi

    note "Checking App Store app information evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_APP_INFORMATION_EVIDENCE.md" \
        "APP_STORE_APP_INFORMATION_EVIDENCE_COMPLETE=true" \
        "App Store app information evidence"
}

require_versioning_evidence_for_final_release() {
    if ! final_signoff_record_complete && ! all_manual_release_signoffs_confirmed; then
        return 0
    fi

    note "Checking App Store versioning evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_VERSIONING_EVIDENCE.md" \
        "APP_STORE_VERSIONING_EVIDENCE_COMPLETE=true" \
        "App Store versioning evidence"
}

require_account_access_evidence_for_final_release() {
    if ! final_signoff_record_complete && ! all_manual_release_signoffs_confirmed; then
        return 0
    fi

    note "Checking App Store account access evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md" \
        "APP_STORE_ACCOUNT_ACCESS_EVIDENCE_COMPLETE=true" \
        "App Store account access evidence"
}

require_release_artifact_evidence_for_final_release() {
    if ! final_signoff_record_complete && ! all_manual_release_signoffs_confirmed; then
        return 0
    fi

    note "Checking App Store release artifact evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md" \
        "APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=true" \
        "App Store release artifact evidence"
}

require_content_rights_evidence_for_final_release() {
    if ! final_signoff_record_complete && ! all_manual_release_signoffs_confirmed; then
        return 0
    fi

    note "Checking App Store content rights evidence"
    require_complete_evidence_record \
        "docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md" \
        "APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true" \
        "App Store content rights evidence"
}

require_final_signoff_record_if_envs_confirmed() {
    local signoff_path="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"

    if ! truthy_env "APP_STORE_PRIVACY_SIGNOFF" ||
        ! truthy_env "APP_STORE_ACCESSIBILITY_SIGNOFF" ||
        ! truthy_env "APP_STORE_AVAILABILITY_CONFIRMED" ||
        ! truthy_env "APP_STORE_DSA_TRADER_STATUS_CONFIRMED" ||
        ! truthy_env "APP_STORE_PRICING_AVAILABILITY_CONFIRMED" ||
        ! truthy_env "APP_STORE_SDK_PRIVACY_CONFIRMED" ||
        ! truthy_env "APP_STORE_RELEASE_CONTROL_CONFIRMED" ||
        ! truthy_env "APP_STORE_MEDIA_LOCALIZATION_CONFIRMED" ||
        ! truthy_env "APP_STORE_LICENSE_NOTICES_CONFIRMED" ||
        ! truthy_env "APP_STORE_EULA_CONFIRMED" ||
        ! truthy_env "APP_STORE_ACCOUNT_DELETION_CONFIRMED" ||
        ! truthy_env "APP_STORE_UGC_MODERATION_CONFIRMED" ||
        ! truthy_env "APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED" ||
        ! truthy_env "TESTFLIGHT_SMOKE_PASSED" ||
        ! truthy_env "APP_STORE_CAPABILITIES_CONFIRMED"; then
        return 0
    fi

    note "Checking final signoff record"
    require_complete_evidence_record \
        "docs/APP_STORE_FINAL_SIGNOFF.md" \
        "APP_STORE_FINAL_SIGNOFF_COMPLETE=true" \
        "Final App Store signoff record"
}

run_gate() {
    local label="$1"
    shift

    note "Running: $label"
    if "$@"; then
        pass "$label"
    else
        blocker "$label failed"
    fi
}

echo "Wakeve App Store Submission Audit"
echo "Project: $PROJECT_DIR"
echo ""

cd "$PROJECT_DIR"

require_file "Gemfile.lock" "Pinned Fastlane toolchain"
require_file ".env.appstore.example" "App Store environment template"
require_file "fastlane/Fastfile" "Fastlane configuration"
require_file "iosApp/src/Info.plist" "iOS Info.plist"
require_file "iosApp/src/PrivacyInfo.xcprivacy" "iOS privacy manifest"
require_file "iosApp/src/Wakeve.entitlements" "iOS entitlements"
require_file "docs/APP_STORE_READINESS.md" "App Store readiness report"
require_file "docs/APP_STORE_BLOCKER_REGISTER.md" "App Store blocker register"
require_file "docs/APP_STORE_PRODUCT_BLOCKER_APPROVAL.md" "App Store product blocker approval packet"
require_file "docs/APP_STORE_SUBMISSION_RUNBOOK.md" "App Store submission runbook"
require_file "docs/APP_STORE_LAUNCH_CHECKLIST.md" "Launch checklist"
require_file "docs/APP_STORE_CONNECT_FIELD_MAP.md" "App Store Connect field map"
require_file "docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md" "App Store account access evidence record"
require_file "docs/APP_STORE_APP_INFORMATION_EVIDENCE.md" "App Store app information evidence record"
require_file "docs/APP_STORE_VERSIONING_EVIDENCE.md" "App Store versioning evidence record"
require_file "docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md" "App Store release artifact evidence record"
require_file "docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md" "App Store content rights evidence record"
require_file "docs/APP_STORE_FINAL_SIGNOFF.md" "Final App Store signoff record"
require_file "docs/APP_STORE_PRIVACY_LABELS.md" "Privacy labels draft"
require_file "docs/APP_STORE_PRIVACY_EVIDENCE.md" "App Store privacy evidence record"
require_file "docs/APP_STORE_ACCESSIBILITY_LABELS.md" "Accessibility labels draft"
require_file "docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md" "App Store accessibility evidence record"
require_file "docs/APP_STORE_AVAILABILITY_DECISIONS.md" "Availability decisions"
require_file "docs/APP_STORE_AVAILABILITY_EVIDENCE.md" "App Store availability evidence record"
require_file "docs/APP_STORE_DSA_TRADER_STATUS.md" "DSA trader status decision"
require_file "docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md" "App Store pricing and availability evidence record"
require_file "docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md" "App Store SDK privacy and signature evidence record"
require_file "docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md" "App Store release control evidence record"
require_file "docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md" "App Store media and localization evidence record"
require_file "docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md" "App Store license notices evidence record"
require_file "docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md" "App Store license inventory draft"
require_file "docs/APP_STORE_THIRD_PARTY_NOTICES.md" "App Store third-party notices draft"
require_file "apps/landing/src/routes/third-party-notices/+page.svelte" "Public third-party notices route"
require_file "docs/APP_STORE_EULA_EVIDENCE.md" "App Store EULA evidence record"
require_file "docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md" "App Store Review Guideline audit"
require_file "docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md" "App Store account deletion evidence record"
require_file "docs/APP_STORE_UGC_MODERATION_EVIDENCE.md" "App Store UGC moderation evidence record"
require_file "docs/APP_STORE_PAYMENT_COMPLIANCE.md" "App Store payment compliance record"
require_file "docs/APP_STORE_PAYMENT_EVIDENCE.md" "App Store payment evidence record"
require_file "docs/APP_STORE_TESTFLIGHT_EVIDENCE.md" "App Store TestFlight evidence record"
require_file "docs/APP_STORE_OBSERVABILITY_EVIDENCE.md" "App Store observability evidence record"
require_file "docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md" "App Store live URL and AASA evidence record"
require_file "docs/APP_STORE_CAPABILITIES_EVIDENCE.md" "Apple Developer capabilities evidence record"
require_file "docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md" "App Store export compliance evidence record"
require_file "docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md" "App Store review access evidence record"

echo ""
note "Checking active App Store OpenSpec proposals"
require_openspec_change_valid "add-in-app-account-deletion" "Account deletion"
require_openspec_change_valid "add-ugc-moderation-controls" "UGC moderation"

echo ""
note "Checking App Review contact"
phone_path="$PROJECT_DIR/composeApp/metadata/ios/review_information/phone_number.txt"
if [ -n "${APP_REVIEW_PHONE_NUMBER:-}" ]; then
    if valid_review_phone "$APP_REVIEW_PHONE_NUMBER"; then
        if placeholder_review_phone "$APP_REVIEW_PHONE_NUMBER"; then
            blocker "APP_REVIEW_PHONE_NUMBER uses the documented placeholder +15551234567; replace it with the real reachable App Review contact before final audit"
        else
            pass "App Review phone number is present in APP_REVIEW_PHONE_NUMBER and plausible"
        fi
    else
        blocker "APP_REVIEW_PHONE_NUMBER has invalid format"
    fi
elif [ -f "$phone_path" ]; then
    phone_number=$(tr -d '\r\n' < "$phone_path")
    if valid_review_phone "$phone_number"; then
        if placeholder_review_phone "$phone_number"; then
            blocker "App Review phone number file uses the documented placeholder +15551234567; replace it with the real reachable App Review contact before final audit"
        else
            pass "App Review phone number is present and plausible"
        fi
    else
        blocker "App Review phone number has invalid format"
    fi
else
    blocker "App Review phone number is missing; set APP_REVIEW_PHONE_NUMBER or add composeApp/metadata/ios/review_information/phone_number.txt"
fi

echo ""
note "Checking required Apple release environment"
require_env "APPLE_ID" "Apple account"
require_env "ITC_TEAM_ID" "App Store Connect team ID"
require_env "TEAM_ID" "Apple Developer Team ID for signing"
require_env "APPLE_TEAM_ID" "Apple Team ID for AASA"
validate_apple_release_env_values

echo ""
note "Checking required manual release signoffs"
require_truthy_env "APP_STORE_PRIVACY_SIGNOFF" "Privacy labels/legal approval"
require_truthy_env "APP_STORE_ACCESSIBILITY_SIGNOFF" "Accessibility label decision"
require_truthy_env "APP_STORE_AVAILABILITY_CONFIRMED" "Mac/Vision Pro availability decision"
require_truthy_env "APP_STORE_DSA_TRADER_STATUS_CONFIRMED" "EU DSA trader status decision"
require_truthy_env "APP_STORE_PRICING_AVAILABILITY_CONFIRMED" "App Store pricing and storefront availability decision"
require_truthy_env "APP_STORE_SDK_PRIVACY_CONFIRMED" "Third-party SDK privacy manifests/signatures"
require_truthy_env "APP_STORE_RELEASE_CONTROL_CONFIRMED" "App Store release option and rollout control"
require_truthy_env "APP_STORE_MEDIA_LOCALIZATION_CONFIRMED" "App Store screenshots, previews, and localized metadata"
require_truthy_env "APP_STORE_LICENSE_NOTICES_CONFIRMED" "Third-party license notices and attributions"
require_truthy_env "APP_STORE_EULA_CONFIRMED" "App Store EULA and terms alignment"
require_truthy_env "APP_STORE_ACCOUNT_DELETION_CONFIRMED" "Account deletion readiness"
require_truthy_env "APP_STORE_UGC_MODERATION_CONFIRMED" "User-generated content moderation readiness"
require_truthy_env "APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED" "Payment and external purchase compliance"
require_truthy_env "TESTFLIGHT_SMOKE_PASSED" "TestFlight smoke test"
require_truthy_env "APP_STORE_CAPABILITIES_CONFIRMED" "Apple Developer capabilities/profiles"
require_final_signoff_evidence_matrix
require_account_deletion_implementation_if_confirmed
require_account_deletion_evidence_if_confirmed
require_ugc_moderation_implementation_if_confirmed
require_ugc_moderation_evidence_if_confirmed
require_availability_evidence_if_confirmed
require_accessibility_evidence_if_confirmed
require_privacy_evidence_if_confirmed
require_dsa_evidence_if_confirmed
require_pricing_availability_evidence_if_confirmed
require_sdk_privacy_evidence_if_confirmed
require_release_control_evidence_if_confirmed
require_media_localization_evidence_if_confirmed
require_license_notices_evidence_if_confirmed
require_eula_evidence_if_confirmed
require_payment_evidence_if_confirmed
require_capabilities_evidence_if_confirmed
require_testflight_evidence_if_confirmed
require_observability_evidence_if_testflight_confirmed
require_live_url_aasa_evidence_for_final_release
require_review_access_evidence_for_final_release
require_export_compliance_evidence_for_final_release
require_account_access_evidence_for_final_release
require_app_information_evidence_for_final_release
require_versioning_evidence_for_final_release
require_release_artifact_evidence_for_final_release
require_content_rights_evidence_for_final_release
require_final_signoff_record_if_envs_confirmed

echo ""
if [ "$RUN_PREFLIGHT" = true ]; then
    run_gate "local App Store preflight" bundle exec fastlane ios preflight
else
    warn "Local App Store preflight skipped"
fi

if [ "$CHECK_LIVE_URLS" = true ]; then
    run_gate "live URL and AASA validation" ./scripts/lint-store-metadata.sh --ios-only --check-live-urls
else
    blocker "Live URL and AASA validation was not run; pass --check-live-urls before submission"
fi

if [ "$RUN_SUBMISSION_READY" = true ]; then
    run_gate "Fastlane submission_ready" bundle exec fastlane ios submission_ready
else
    blocker "Signed final gate was not run; pass --run-submission-ready after Apple signing is configured"
fi

if [ "$RUN_PREFLIGHT" = false ] && [ "$BLOCKERS" -eq 0 ]; then
    blocker "Local App Store preflight was skipped; rerun without --skip-preflight before submission"
fi

echo ""
echo "Summary"
echo "Warnings: $WARNINGS"
echo "Blockers: $BLOCKERS"

if [ "$BLOCKERS" -gt 0 ]; then
    echo "Result: NOT READY for App Store submission"
    exit 1
fi

echo "Result: READY for App Store submission gate. Upload still requires an explicit Fastlane/App Store Connect command."
exit 0
