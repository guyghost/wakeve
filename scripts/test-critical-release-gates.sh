#!/usr/bin/env bash
# Critical local release gates for roadmap P2.1.
#
# This intentionally checks product and App Store blocker regressions instead of
# enforcing a broad coverage percentage that does not map to release risk.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

run() {
    echo
    echo "+ $*"
    "$@"
}

assert_no_sensitive_server_logs() {
    local files=(
        "$PROJECT_DIR/server/src/main/kotlin/com/guyghost/wakeve/auth/OtpManager.kt"
        "$PROJECT_DIR/server/src/main/kotlin/com/guyghost/wakeve/notification/PushNotificationSender.kt"
        "$PROJECT_DIR/server/src/main/kotlin/com/guyghost/wakeve/notification/EventNotificationTrigger.kt"
        "$PROJECT_DIR/server/src/main/kotlin/com/guyghost/wakeve/notification/NotificationScheduler.kt"
    )
    local pattern='logger\.(info|warn|error|debug).*(normalizedEmail|token=|title=|body=|data=|Payload:|responseBody|OTP généré|email: \{\}|pour: \{\}|\$userId|\$participantId|\$eventId|\$displayName|\$authorName|\$voterName|\$jobKey|\$batchKey|\$\{event\.id\}|\$\{request\.userId\})'

    if grep -nE "$pattern" "${files[@]}"; then
        echo "FAIL: Sensitive auth, notification, or push values are logged in server release paths" >&2
        exit 1
    fi

    echo "PASS: Server auth/notification/push logs avoid email, OTP, user IDs, event IDs, tokens, and notification payload values"
}

assert_ios_profile_legal_notice_links() {
    local profile="$PROJECT_DIR/iosApp/src/Views/Profile/ProfileTabView.swift"
    local english="$PROJECT_DIR/iosApp/src/Resources/en.lproj/Localizable.strings"
    local french="$PROJECT_DIR/iosApp/src/Resources/fr.lproj/Localizable.strings"
    local required_profile_patterns=(
        "https://wakeve.app/support"
        "https://wakeve.app/privacy"
        "https://wakeve.app/terms"
        "https://wakeve.app/third-party-notices"
        "profile.third_party_notices"
    )

    for pattern in "${required_profile_patterns[@]}"; do
        if ! grep -Fq "$pattern" "$profile"; then
            echo "FAIL: iOS Profile legal/notice link is missing: $pattern" >&2
            exit 1
        fi
    done

    if ! grep -Fq '"profile.third_party_notices" = "Third-party notices"' "$english"; then
        echo "FAIL: English third-party notices profile label is missing" >&2
        exit 1
    fi

    if ! grep -Fq '"profile.third_party_notices" = "Notices tierces"' "$french"; then
        echo "FAIL: French third-party notices profile label is missing" >&2
        exit 1
    fi

    echo "PASS: iOS Profile exposes support, privacy, terms, and third-party notices links"
}

cd "$PROJECT_DIR"

run openspec validate add-in-app-account-deletion --strict
run openspec validate add-ugc-moderation-controls --strict
run openspec validate add-on-device-wakeve-ai --strict

run ./scripts/test-app-store-ugc-gates.sh

assert_no_sensitive_server_logs
assert_ios_profile_legal_notice_links
run ./scripts/audit-ios-localization-parity.sh --fail-on-findings

run ./gradlew :server:test \
    --tests com.guyghost.wakeve.auth.AccountDeletionServiceTest \
    --tests com.guyghost.wakeve.AuthFlowIntegrationTest \
    --tests com.guyghost.wakeve.routes.UgcModerationRoutesTest

run ./gradlew :shared:jvmTest \
    --tests com.guyghost.wakeve.auth.shell.services.GuestModeServiceTest \
    --tests com.guyghost.wakeve.moderation.ModerationPolicyTest \
    --tests com.guyghost.wakeve.notification.NotificationSchedulerTest \
    --tests com.guyghost.wakeve.workflow.DraftWorkflowIntegrationTest \
    --tests com.guyghost.wakeve.scenario.ScenarioMatrixGenerationServiceTest \
    --tests com.guyghost.wakeve.contacts.ContactParticipantSelectionPolicyTest \
    --tests com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachinePollingConfirmationTest \
    --tests com.guyghost.wakeve.sync.conflict.ConflictResolutionIntegrationTest

if [ "${RUN_IOS_CONTRACTS:-0}" = "1" ]; then
    run xcodebuild test \
        -project iosApp/iosApp.xcodeproj \
        -scheme WakeveApp \
        -destination "platform=iOS Simulator,name=iPhone 17" \
        -only-testing:WakeveTests/FindingsRegressionTests \
        -only-testing:WakeveTests/WakeveAITests
fi

echo
echo "PASS: Critical local release gates completed"
