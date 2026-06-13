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

cd "$PROJECT_DIR"

run openspec validate add-in-app-account-deletion --strict
run openspec validate add-ugc-moderation-controls --strict
run openspec validate add-on-device-wakeve-ai --strict

run ./scripts/test-app-store-ugc-gates.sh

assert_no_sensitive_server_logs

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
