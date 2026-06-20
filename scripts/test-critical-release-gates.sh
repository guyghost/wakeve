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

assert_generated_report_is_commit_safe() {
    local report="$1"
    local label="$2"

    if LC_ALL=C grep -q $'\r' "$report"; then
        echo "FAIL: $label report contains carriage returns" >&2
        exit 1
    fi

    if grep -nE '[[:blank:]]+$' "$report"; then
        echo "FAIL: $label report contains trailing whitespace" >&2
        exit 1
    fi

    if [ -n "${HOME:-}" ] && grep -Fq "$HOME" "$report"; then
        echo "FAIL: $label report contains an absolute local HOME path; redact before committing evidence" >&2
        exit 1
    fi
}

assert_report_sanitizer_is_shared() {
    local sanitizer="$PROJECT_DIR/scripts/lib/report-sanitization.sh"

    bash -n "$sanitizer"

    if ! grep -Fq 'sanitize_report_file()' "$sanitizer" \
        || ! grep -Fq 's/\r//g; s/[ \t]+$//' "$sanitizer" \
        || ! grep -Fq 's/$home/~/g if $home ne ""' "$sanitizer"; then
        echo "FAIL: shared report sanitizer must strip CR, trailing whitespace, and local HOME paths" >&2
        exit 1
    fi

    local duplicate_sanitizers
    duplicate_sanitizers="$(
        find "$PROJECT_DIR/scripts" -type f -name '*.sh' \
            ! -path "$sanitizer" \
            ! -path "$PROJECT_DIR/scripts/test-critical-release-gates.sh" \
            -print0 \
            | xargs -0 grep -nE 'perl -pi -e.*s/\\r//g|perl -pi -e.*s/\[ \\t\]\+\$//' || true
    )"

    if [ -n "$duplicate_sanitizers" ]; then
        echo "$duplicate_sanitizers" >&2
        echo "FAIL: report sanitization must use scripts/lib/report-sanitization.sh instead of inline Perl" >&2
        exit 1
    fi

    echo "PASS: release evidence report sanitization is centralized"
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

assert_android_home_workspace_user_copy() {
    local source_dirs=(
        "$PROJECT_DIR/composeApp/src/androidMain"
        "$PROJECT_DIR/composeApp/src/commonMain"
        "$PROJECT_DIR/composeApp/src/androidUnitTest"
        "$PROJECT_DIR/composeApp/src/androidInstrumentedTest"
    )
    local model="$PROJECT_DIR/composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/EventWorkspaceModels.kt"
    local internal_product_pattern='Boucle de croissance|Signal (emotionnel|émotionnel)|Position (strategique|stratégique)|Roadmap 6 mois|Pourquoi inviter|Pourquoi installer|Pourquoi revenir|Score émotionnel|Scores :|Honnête|Face aux concurrents|OS social|Problème critique|Fonction à créer|Capacité manquante|Interet utilisateur|Widget prioritaire|Widget voyage|Widget compte|Widget tache'

    if grep -RInE "$internal_product_pattern" "${source_dirs[@]}"; then
        echo "FAIL: Android Home workspace reintroduced internal product-audit copy; keep user-facing coordination labels" >&2
        exit 1
    fi

    local required_labels=(
        'title = "Invitations et retours"'
        'title = "Ambiance du groupe"'
        'title = "Prochaine décision"'
        'title = "Plan d'\''action"'
    )

    for label in "${required_labels[@]}"; do
        if ! grep -Fq "$label" "$model"; then
            echo "FAIL: Android Home workspace is missing expected user-facing coordination label: $label" >&2
            exit 1
        fi
    done

    echo "PASS: Android Home workspace uses user-facing coordination copy"
}

assert_android_event_workspace_harness() {
    local harness="$PROJECT_DIR/scripts/test-android-event-workspace.sh"

    bash -n "$harness"

    local required_patterns=(
        '--no-configuration-cache :composeApp:testDebugUnitTest'
        'com.guyghost.wakeve.ui.event.EventWorkspaceModelsTest'
        '--no-configuration-cache :composeApp:compileDebugAndroidTestKotlinAndroid'
        'PASS: Android Event workspace focused checks completed'
    )

    for pattern in "${required_patterns[@]}"; do
        if ! grep -Fq -- "$pattern" "$harness"; then
            echo "FAIL: Android Event workspace harness is missing expected no-configuration-cache check: $pattern" >&2
            exit 1
        fi
    done

    echo "PASS: Android Event workspace harness keeps focused Gradle checks configuration-cache safe"
}

assert_android_event_workspace_device_audit_helper() {
    local output_dir
    local report
    mkdir -p "$PROJECT_DIR/build/tmp"
    output_dir="$(mktemp -d "$PROJECT_DIR/build/tmp/android-event-workspace-audit.XXXXXX")"

    bash -n "$PROJECT_DIR/scripts/prepare-android-event-workspace-device-audit.sh"
    report="$(OUTPUT_DIR="$output_dir" "$PROJECT_DIR/scripts/prepare-android-event-workspace-device-audit.sh")"

    if [ ! -f "$report" ]; then
        echo "FAIL: Android Event workspace device audit helper did not create a report at $report" >&2
        exit 1
    fi
    assert_generated_report_is_commit_safe "$report" "Android Event workspace device audit"

    local required_patterns=(
        'Status: `'
        'creation -> invitation -> vote -> date confirmed -> day J path'
        'Generated report can close roadmap item | `no - preparation evidence only`'
        'Selected adb serial | `'
        'Wakeve package id | `com.guyghost.wakeve`'
        'Evidence asset directory | `'
        'Android device/emulator model | TODO'
        'Wakeve installed package summary reviewed | TODO'
        'Focused source harness result | TODO: run ./scripts/test-android-event-workspace.sh'
        'Day J coordination step result | TODO'
        'shell screencap -p "/sdcard/wakeve-<step>.png"'
        'ANDROID_SERIAL="'
        'ANDROID_APP_ID="com.guyghost.wakeve"'
        'Do not mark roadmap P2.1 complete'
    )

    for pattern in "${required_patterns[@]}"; do
        if ! grep -Fq "$pattern" "$report"; then
            echo "FAIL: Android Event workspace device audit report is missing required field: $pattern" >&2
            exit 1
        fi
    done

    echo "PASS: Android Event workspace device audit helper generates the required P2.1 evidence template"
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

assert_android_resource_defaults() {
    local base_strings="$PROJECT_DIR/composeApp/src/androidMain/res/values/strings.xml"
    local required_keys=(
        "badges_count_label"
        "duration_label"
        "points_value_label"
        "try_again_later"
        "loading_more"
        "notification_event_created"
        "notification_poll_opened"
        "notification_date_confirmed"
        "entity_event"
        "entity_scenario"
        "entity_meeting"
    )

    for key in "${required_keys[@]}"; do
        if ! grep -Eq "<string name=\"$key\">" "$base_strings"; then
            echo "FAIL: Android default strings are missing release resource key: $key" >&2
            exit 1
        fi
    done

    echo "PASS: Android release string resources have required default values"
}

assert_release_performance_harness() {
    local harness="$PROJECT_DIR/scripts/profile-release-performance.sh"
    local runbook="$PROJECT_DIR/docs/performance/release-profiling-runbook.md"
    local output_dir
    local report

    if ! grep -Fq 'OUTPUT_DIR="${OUTPUT_DIR:-$PROJECT_DIR/docs/performance}"' "$harness"; then
        echo "FAIL: release performance harness must respect OUTPUT_DIR overrides for temporary captures" >&2
        exit 1
    fi

    if ! grep -Fq -- '--no-configuration-cache :composeApp:assembleRelease' "$harness"; then
        echo "FAIL: release performance harness must build Android release without configuration cache" >&2
        exit 1
    fi

    if ! grep -Fq 'Status: BUILT_LOCAL_RELEASE_ARTIFACT' "$harness"; then
        echo "FAIL: release performance harness must record local release build artifacts" >&2
        exit 1
    fi

    if ! grep -Fq 'append_release_build_result "iOS"' "$harness"; then
        echo "FAIL: release performance harness must record iOS Release simulator artifacts when --build-ios is used" >&2
        exit 1
    fi

    mkdir -p "$PROJECT_DIR/build/tmp"
    output_dir="$(mktemp -d "$PROJECT_DIR/build/tmp/release-performance.XXXXXX")"
    report="$(PATH="/usr/bin:/bin" OUTPUT_DIR="$output_dir" "$harness" --android-only --runs 1)"
    if [ ! -f "$report" ]; then
        echo "FAIL: release performance harness did not create a report at $report" >&2
        exit 1
    fi
    assert_generated_report_is_commit_safe "$report" "release performance"

    if ! grep -Fq 'Status: LOCAL EVIDENCE' "$report"; then
        echo "FAIL: release performance report must keep local evidence status explicit" >&2
        exit 1
    fi

    if ! grep -Fq 'PENDING_DEVICE_TRACE' "$report"; then
        echo "FAIL: release performance report must keep device trace closure status explicit" >&2
        exit 1
    fi

    if ! grep -Fq './scripts/prepare-android-event-workspace-device-audit.sh' "$runbook"; then
        echo "FAIL: release profiling runbook must reference the Android event-workspace device audit helper" >&2
        exit 1
    fi

    if ! grep -Fq 'creation -> invitation -> vote -> date confirmed -> day J' "$runbook"; then
        echo "FAIL: release profiling runbook must name the Android event-workspace audit flow" >&2
        exit 1
    fi

    if ! grep -Fq 'docs/product/android-event-workspace-device-audit-2026-06-20T22-46-22Z.md' "$runbook" \
        || ! grep -Fq 'PENDING_ANDROID_DEVICE_OR_EMULATOR' "$runbook"; then
        echo "FAIL: release profiling runbook must keep the latest Android device-audit preparation status explicit" >&2
        exit 1
    fi

    echo "PASS: release performance harness records local Release artifacts safely"
}

assert_payment_compliance_audit_helper() {
    local output_dir
    local report
    mkdir -p "$PROJECT_DIR/build/tmp"
    output_dir="$(mktemp -d "$PROJECT_DIR/build/tmp/payment-compliance.XXXXXX")"

    bash -n "$PROJECT_DIR/scripts/audit-app-store-payment-compliance.sh"
    report="$(OUTPUT_DIR="$output_dir" "$PROJECT_DIR/scripts/audit-app-store-payment-compliance.sh")"

    if [ ! -f "$report" ]; then
        echo "FAIL: payment compliance audit helper did not create a report at $report" >&2
        exit 1
    fi
    assert_generated_report_is_commit_safe "$report" "payment compliance audit"

    local required_patterns=(
        'Status: `LOCAL_SOURCE_SCAN_READY`'
        'IAP/paywall source matches | `none`'
        'Payment/Tricount surface matches | `present`'
        'Review-note/policy matches | `present`'
        'Generated report can close AS-11 | `no - local scan only`'
        'Payment surfaces inspected in uploaded build | TODO'
        'Do not mark AS-11 complete'
    )

    for pattern in "${required_patterns[@]}"; do
        if ! grep -Fq "$pattern" "$report"; then
            echo "FAIL: payment compliance audit report is missing required field: $pattern" >&2
            exit 1
        fi
    done

    echo "PASS: payment compliance audit helper generates the required AS-11 local evidence template"
}

assert_dsa_trader_status_evidence_helper() {
    local output_dir
    local report
    mkdir -p "$PROJECT_DIR/build/tmp"
    output_dir="$(mktemp -d "$PROJECT_DIR/build/tmp/dsa-trader-status.XXXXXX")"

    bash -n "$PROJECT_DIR/scripts/prepare-app-store-dsa-trader-status-evidence.sh"
    report="$(OUTPUT_DIR="$output_dir" "$PROJECT_DIR/scripts/prepare-app-store-dsa-trader-status-evidence.sh")"

    if [ ! -f "$report" ]; then
        echo "FAIL: DSA trader status evidence helper did not create a report at $report" >&2
        exit 1
    fi
    assert_generated_report_is_commit_safe "$report" "DSA trader status evidence"

    local required_patterns=(
        'Status: `PENDING_APP_STORE_CONNECT_DSA_DECISION`'
        'Generated report can close AS-08 | `no - preparation evidence only`'
        'Trader for EU distribution.'
        'Non-trader for EU distribution.'
        'EU storefronts disabled for the first release.'
        'Selected DSA path | TODO: trader / non-trader / EU storefronts disabled'
        'App-specific DSA status reviewed | TODO'
        'Pricing and availability evidence cross-check | TODO'
        'App availability evidence cross-check | TODO'
        'Product/legal owner approval | TODO'
        'Do not mark AS-08 complete'
    )

    for pattern in "${required_patterns[@]}"; do
        if ! grep -Fq "$pattern" "$report"; then
            echo "FAIL: DSA trader status evidence report is missing required field: $pattern" >&2
            exit 1
        fi
    done

    echo "PASS: DSA trader status helper generates the required AS-08 preparation template"
}

assert_notification_review_contract() {
    local model="$PROJECT_DIR/shared/src/commonMain/kotlin/com/guyghost/wakeve/notification/NotificationCategory.kt"
    local service="$PROJECT_DIR/shared/src/commonMain/kotlin/com/guyghost/wakeve/notification/RichNotificationService.kt"
    local docs="$PROJECT_DIR/docs/deep-linking.md"
    local required_identifiers=(
        'EVENT_INVITE("event_invite")'
        'POLL_REMINDER("poll_reminder")'
        'MEETING_STARTING("meeting_starting")'
        'SCENARIO_VOTE("scenario_vote")'
        'GENERAL("general")'
    )

    for identifier in "${required_identifiers[@]}"; do
        if ! grep -Fq "$identifier" "$model"; then
            echo "FAIL: notification category contract changed without release-review documentation: $identifier" >&2
            exit 1
        fi
    done

    local in_app_action_policy='NotificationCategory.EVENT_INVITE,
        NotificationCategory.POLL_REMINDER,
        NotificationCategory.SCENARIO_VOTE,
        NotificationCategory.GENERAL -> emptyList()'
    if ! grep -Fq "$in_app_action_policy" "$service"; then
        echo "FAIL: rich notification review policy must keep invite, poll, scenario, and general direct actions disabled" >&2
        exit 1
    fi

    local required_doc_rows=(
        '| Event invite | `event_invite` | none |'
        '| Poll reminder | `poll_reminder` | none |'
        '| Meeting starting | `meeting_starting` | `join` |'
        '| Scenario vote | `scenario_vote` | none |'
        '| General | `general` | none |'
    )
    for row in "${required_doc_rows[@]}"; do
        if ! grep -Fq "$row" "$docs"; then
            echo "FAIL: deep-linking docs no longer match notification review contract: $row" >&2
            exit 1
        fi
    done

    echo "PASS: notification review contract keeps direct-write actions out of first-release categories"
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
    assert_generated_report_is_commit_safe "$report" "WakeveAI device profile"

    local required_patterns=(
        'Status: `'
        'TEAM_ID / APPLE_TEAM_ID environment value'
        'Generated report can close OpenSpec task | `no - preparation evidence only`'
        '## Toolchain Readiness'
        'Xcode version | `'
        'iPhoneOS SDK version | `'
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
    assert_generated_report_is_commit_safe "$report" "WeatherKit device validation"

    local required_patterns=(
        'Status: `'
        'Source WeatherKit entitlement | `true`'
        'Generated report can close OpenSpec tasks | `no - preparation evidence only`'
        '## Toolchain Readiness'
        'Xcode version | `'
        'iPhoneOS SDK version | `'
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

assert_ios_accessibility_source_audit() {
    local output_dir
    local report
    mkdir -p "$PROJECT_DIR/build/tmp"
    output_dir="$(mktemp -d "$PROJECT_DIR/build/tmp/ios-accessibility-source.XXXXXX")"

    bash -n "$PROJECT_DIR/scripts/audit-ios-accessibility-source.sh"
    report="$("$PROJECT_DIR/scripts/audit-ios-accessibility-source.sh" --fail-on-findings --output "$output_dir")"

    if [ ! -f "$report" ]; then
        echo "FAIL: iOS accessibility source audit did not create a report at $report" >&2
        exit 1
    fi
    assert_generated_report_is_commit_safe "$report" "iOS accessibility source audit"

    if ! grep -Fq '| Total | 0 |' "$report"; then
        echo "FAIL: iOS accessibility source audit report must have zero findings" >&2
        exit 1
    fi

    echo "PASS: iOS accessibility source audit has zero release-source findings"
}

assert_ios_release_screen_evidence_integrity() {
    local output_dir
    local report
    mkdir -p "$PROJECT_DIR/build/tmp"
    output_dir="$(mktemp -d "$PROJECT_DIR/build/tmp/ios-release-screen-evidence.XXXXXX")"

    bash -n "$PROJECT_DIR/scripts/audit-ios-release-screen-evidence.sh"
    report="$("$PROJECT_DIR/scripts/audit-ios-release-screen-evidence.sh" --output-dir "$output_dir")"

    if [ ! -f "$report" ]; then
        echo "FAIL: iOS release screen evidence audit did not create a report at $report" >&2
        exit 1
    fi
    assert_generated_report_is_commit_safe "$report" "iOS release screen evidence"

    local required_patterns=(
        'Local coverage: 3 / 5 required screens'
        'Missing required screens: 2'
        'Integrity checked: 13 indexed screenshots'
        'Integrity failures: 0'
        '| Event detail | MISSING | 0 |'
        '| Organization | MISSING | 0 |'
    )

    for pattern in "${required_patterns[@]}"; do
        if ! grep -Fq "$pattern" "$report"; then
            echo "FAIL: iOS release screen evidence audit no longer matches expected local evidence state: $pattern" >&2
            exit 1
        fi
    done

    echo "PASS: iOS release screen evidence audit verifies indexed screenshots and keeps missing release screens explicit"
}

assert_app_store_media_localization_audit() {
    local output_dir
    local report
    mkdir -p "$PROJECT_DIR/build/tmp"
    output_dir="$(mktemp -d "$PROJECT_DIR/build/tmp/app-store-media-localization.XXXXXX")"

    bash -n "$PROJECT_DIR/scripts/audit-app-store-media-localization.sh"
    report="$("$PROJECT_DIR/scripts/audit-app-store-media-localization.sh" --fail-on-findings --output-dir "$output_dir")"

    if [ ! -f "$report" ]; then
        echo "FAIL: App Store media/localization audit did not create a report at $report" >&2
        exit 1
    fi
    assert_generated_report_is_commit_safe "$report" "App Store media/localization audit"

    local required_patterns=(
        'Status: LOCAL EVIDENCE'
        '| App preview videos | 0 |'
        '| Findings | 0 |'
        'APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=false'
    )

    for pattern in "${required_patterns[@]}"; do
        if ! grep -Fq "$pattern" "$report"; then
            echo "FAIL: App Store media/localization audit report is missing required field: $pattern" >&2
            exit 1
        fi
    done

    echo "PASS: App Store media/localization audit remains clean and local-only"
}

assert_app_store_privacy_alignment_audit() {
    local output_dir
    local report
    mkdir -p "$PROJECT_DIR/build/tmp"
    output_dir="$(mktemp -d "$PROJECT_DIR/build/tmp/app-store-privacy.XXXXXX")"

    bash -n "$PROJECT_DIR/scripts/audit-app-store-privacy-alignment.sh"
    report="$("$PROJECT_DIR/scripts/audit-app-store-privacy-alignment.sh" --fail-on-findings --output-dir "$output_dir")"

    if [ ! -f "$report" ]; then
        echo "FAIL: App Store privacy alignment audit did not create a report at $report" >&2
        exit 1
    fi
    assert_generated_report_is_commit_safe "$report" "App Store privacy alignment audit"

    local required_patterns=(
        'Status: LOCAL EVIDENCE'
        '| Local findings | 0 |'
        'Result: PASS for local privacy alignment.'
        'AS-05 remains open'
    )

    for pattern in "${required_patterns[@]}"; do
        if ! grep -Fq "$pattern" "$report"; then
            echo "FAIL: App Store privacy alignment audit report is missing required field: $pattern" >&2
            exit 1
        fi
    done

    echo "PASS: App Store privacy alignment audit remains clean and local-only"
}

assert_ios_localization_parity_report() {
    local output_dir
    local output
    local report
    mkdir -p "$PROJECT_DIR/build/tmp"
    output_dir="$(mktemp -d "$PROJECT_DIR/build/tmp/ios-localization-parity.XXXXXX")"

    bash -n "$PROJECT_DIR/scripts/audit-ios-localization-parity.sh"
    output="$("$PROJECT_DIR/scripts/audit-ios-localization-parity.sh" --write-report --fail-on-findings --output "$output_dir")"
    report="$(printf '%s\n' "$output" | tail -n 1)"

    if [ ! -f "$report" ]; then
        echo "FAIL: iOS localization parity audit did not create a report at $report" >&2
        exit 1
    fi
    assert_generated_report_is_commit_safe "$report" "iOS localization parity audit"

    if ! grep -Fq '| Total findings |  |  |  |  | 0 |' "$report"; then
        echo "FAIL: iOS localization parity report must have zero findings" >&2
        exit 1
    fi

    echo "PASS: iOS localization parity report has zero findings and commit-safe evidence"
}

assert_live_url_aasa_blocker_evidence() {
    local evidence="$PROJECT_DIR/docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md"
    local capture="$PROJECT_DIR/docs/app-store-live-url-aasa/live-url-aasa-2026-06-20T22-48-00Z.md"
    local blocker_register="$PROJECT_DIR/docs/APP_STORE_BLOCKER_REGISTER.md"
    local capture_script="$PROJECT_DIR/scripts/capture-app-store-live-url-aasa.sh"
    local sanitizer="$PROJECT_DIR/scripts/lib/report-sanitization.sh"

    if ! grep -Fxq 'APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=false' "$evidence"; then
        echo "FAIL: live URL/AASA evidence marker must stay false until public wakeve.app and AASA checks pass" >&2
        exit 1
    fi

    if ! grep -Fq 'sanitize_report_file "$report"' "$capture_script" \
        || ! grep -Fq 's/\r//g; s/[ \t]+$//' "$sanitizer"; then
        echo "FAIL: live URL/AASA capture script must normalize trailing whitespace before reports are committed" >&2
        exit 1
    fi

    if ! grep -Fq 'docs/app-store-live-url-aasa/live-url-aasa-2026-06-20T22-48-00Z.md' "$evidence"; then
        echo "FAIL: live URL/AASA evidence must reference the latest AS-14 capture report" >&2
        exit 1
    fi

    if [ ! -f "$capture" ]; then
        echo "FAIL: latest live URL/AASA capture report is missing: $capture" >&2
        exit 1
    fi

    local required_patterns=(
        'Result: FAIL. 16 required live URL/AASA checks failed or could not be validated.'
        'curl: (6) Could not resolve host: wakeve.app'
        'HTTP/2 405'
        '104.21.48.204'
        '172.67.156.46'
    )

    for pattern in "${required_patterns[@]}"; do
        if ! grep -Fq "$pattern" "$capture"; then
            echo "FAIL: latest live URL/AASA capture report no longer documents expected AS-14 blocker evidence: $pattern" >&2
            exit 1
        fi
    done

    local blocker_register_patterns=(
        'Date: 2026-06-21'
        'Current result on 2026-06-21 local time: 17 live URL/AASA errors and 1 final-signoff warning remain expected'
        'api.wakeve.app/health` is reachable, but `wakeve.app` DNS/live web/AASA routes remain unreachable'
        'docs/app-store-live-url-aasa/live-url-aasa-2026-06-20T22-48-00Z.md'
    )

    for pattern in "${blocker_register_patterns[@]}"; do
        if ! grep -Fq "$pattern" "$blocker_register"; then
            echo "FAIL: App Store blocker register no longer documents current AS-14 public-check status: $pattern" >&2
            exit 1
        fi
    done

    echo "PASS: live URL/AASA blocker evidence remains explicit and incomplete"
}

assert_store_metadata_lint_evidence_count() {
    local roadmap="$PROJECT_DIR/ROADMAP.md"
    local readiness="$PROJECT_DIR/docs/APP_STORE_READINESS.md"
    local lint="$PROJECT_DIR/scripts/lint-store-metadata.sh"

    if grep -nE '3049 checks|3088 checks' "$roadmap"; then
        echo "FAIL: ROADMAP.md still references stale App Store metadata lint counts" >&2
        exit 1
    fi

    local roadmap_count
    roadmap_count="$(grep -Fc "3111 checks, 0 erreur, 1 warning" "$roadmap")"
    if [ "$roadmap_count" -lt 2 ]; then
        echo "FAIL: ROADMAP.md must document the current 3111-check App Store metadata lint result in both release evidence sections" >&2
        exit 1
    fi

    local required_patterns=(
        "Result: 3111 checks passed, 0 errors, 1 warning"
        "3111 checks passed, 0 errors, 1 warning"
    )

    if ! grep -Fq "${required_patterns[0]}" "$lint"; then
        echo "FAIL: lint-store-metadata.sh no longer publishes the expected 3111-check readiness result" >&2
        exit 1
    fi

    if ! grep -Fq "${required_patterns[1]}" "$readiness"; then
        echo "FAIL: APP_STORE_READINESS.md no longer documents the expected 3111-check lint result" >&2
        exit 1
    fi

    echo "PASS: App Store metadata lint evidence count is synchronized across roadmap, readiness, and lint gate"
}

cd "$PROJECT_DIR"

run openspec validate --all --strict

run ./scripts/test-app-store-ugc-gates.sh

assert_no_sensitive_server_logs
assert_no_android_release_local_backend_defaults
assert_android_compose_hygiene
assert_android_home_workspace_user_copy
assert_android_event_workspace_harness
assert_report_sanitizer_is_shared
assert_android_event_workspace_device_audit_helper
assert_android_build_hygiene
assert_android_resource_defaults
assert_release_performance_harness
assert_payment_compliance_audit_helper
assert_dsa_trader_status_evidence_helper
assert_notification_review_contract
assert_ios_profile_legal_notice_links
assert_wakeve_ai_device_profile_helper
assert_weatherkit_device_validation_helper
assert_ios_accessibility_source_audit
assert_ios_release_screen_evidence_integrity
assert_app_store_media_localization_audit
assert_app_store_privacy_alignment_audit
assert_ios_localization_parity_report
assert_store_metadata_lint_evidence_count
assert_live_url_aasa_blocker_evidence

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
