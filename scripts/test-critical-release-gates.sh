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

assert_no_android_release_local_backend_defaults() {
    local source_dirs=(
        "$PROJECT_DIR/composeApp/src/androidMain"
        "$PROJECT_DIR/shared/src/androidMain"
    )
    local pattern='http://(localhost|127\.0\.0\.1|10\.0\.2\.2)|localhost:8080|127\.0\.0\.1:8080|10\.0\.2\.2:8080'

    if grep -RInE "$pattern" "${source_dirs[@]}"; then
        echo "FAIL: Android release source contains a local backend default; use BuildConfig/local.properties overrides for development" >&2
        exit 1
    fi

    echo "PASS: Android release source has no localhost, loopback, or emulator backend defaults"
}

assert_android_compose_hygiene() {
    local source_dirs=(
        "$PROJECT_DIR/composeApp/src/androidMain"
        "$PROJECT_DIR/composeApp/src/commonMain"
    )

    local deprecated_compose_pattern='(^|[^A-Za-z0-9_])Divider\(|\.menuAnchor\(\)'
    if grep -RInE "$deprecated_compose_pattern" "${source_dirs[@]}"; then
        echo "FAIL: Android Compose source reintroduced deprecated Divider or parameterless menuAnchor usage" >&2
        exit 1
    fi

    local non_mirrored_directional_icon_pattern='Icons\.(Default|Filled|Outlined)\.(ArrowBack|ArrowForward|ExitToApp|Send|TrendingUp|VolumeUp|Comment)'
    if grep -RInE "$non_mirrored_directional_icon_pattern" "${source_dirs[@]}"; then
        echo "FAIL: Android Compose source uses directional icons without AutoMirrored variants" >&2
        exit 1
    fi

    echo "PASS: Android Compose source avoids deprecated dividers, parameterless menu anchors, and non-mirrored directional icons"
}

assert_android_build_hygiene() {
    local obsolete_agp_flags_pattern='^android\.(defaults\.buildfeatures\.resvalues|sdk\.defaultTargetSdkToCompileSdkIfUnset|enableAppCompileTimeRClass|usesSdkInManifest\.disallowed|r8\.optimizedResourceShrinking)='
    if grep -nE "$obsolete_agp_flags_pattern" "$PROJECT_DIR/gradle.properties"; then
        echo "FAIL: gradle.properties reintroduced obsolete Android Gradle Plugin flags" >&2
        exit 1
    fi

    if grep -RInE '\.statusBarColor\s*=' "$PROJECT_DIR/composeApp/src/androidMain"; then
        echo "FAIL: Android source directly writes deprecated statusBarColor; use edge-to-edge APIs instead" >&2
        exit 1
    fi

    echo "PASS: Android build hygiene avoids obsolete AGP flags and direct statusBarColor writes"
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

assert_wakeve_ai_device_profile_helper() {
    local output_dir
    local report
    output_dir="$(mktemp -d)"

    bash -n "$PROJECT_DIR/scripts/prepare-wakeve-ai-device-profile.sh"
    report="$(OUTPUT_DIR="$output_dir" "$PROJECT_DIR/scripts/prepare-wakeve-ai-device-profile.sh")"

    if [ ! -f "$report" ]; then
        echo "FAIL: WakeveAI device profile helper did not create a report at $report" >&2
        exit 1
    fi

    local required_patterns=(
        'Status: `'
        'TEAM_ID / APPLE_TEAM_ID environment value'
        'Generated report can close OpenSpec task | `no - preparation evidence only`'
        'Apple Intelligence enabled | TODO'
        'Foundation Models availability | TODO: must be `.available`'
        'Foundation Models model assets ready | TODO'
        'Generation latency | TODO'
        'Cancellation latency | TODO'
        'Peak memory during generation | TODO'
        'Production log privacy checked | TODO'
        'Do not mark `6.6` complete'
    )

    for pattern in "${required_patterns[@]}"; do
        if ! grep -Fq "$pattern" "$report"; then
            echo "FAIL: WakeveAI device profile report is missing required field: $pattern" >&2
            exit 1
        fi
    done

    echo "PASS: WakeveAI device profile helper generates the required 6.6 evidence template"
}

assert_weatherkit_device_validation_helper() {
    local output_dir
    local report
    output_dir="$(mktemp -d)"

    bash -n "$PROJECT_DIR/scripts/prepare-weatherkit-device-validation.sh"
    report="$(OUTPUT_DIR="$output_dir" "$PROJECT_DIR/scripts/prepare-weatherkit-device-validation.sh")"

    if [ ! -f "$report" ]; then
        echo "FAIL: WeatherKit device validation helper did not create a report at $report" >&2
        exit 1
    fi

    local required_patterns=(
        'Status: `'
        'Source WeatherKit entitlement | `true`'
        'Generated report can close OpenSpec tasks | `no - preparation evidence only`'
        'Provisioning profile contains WeatherKit entitlement | TODO'
        'Signed app entitlement inspection path | TODO'
        'WeatherKit request result | TODO'
        'Do not mark `1.2` or `6.2` complete'
    )

    for pattern in "${required_patterns[@]}"; do
        if ! grep -Fq "$pattern" "$report"; then
            echo "FAIL: WeatherKit validation report is missing required field: $pattern" >&2
            exit 1
        fi
    done

    echo "PASS: WeatherKit device validation helper generates the required 1.2/6.2 evidence template"
}

cd "$PROJECT_DIR"

run openspec validate --all --strict

run ./scripts/test-app-store-ugc-gates.sh

assert_no_sensitive_server_logs
assert_no_android_release_local_backend_defaults
assert_android_compose_hygiene
assert_android_build_hygiene
assert_ios_profile_legal_notice_links
assert_wakeve_ai_device_profile_helper
assert_weatherkit_device_validation_helper
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
