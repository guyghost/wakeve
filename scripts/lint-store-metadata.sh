#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────
# Store Metadata Linter — Wakeve
# Validates store listing metadata for both Android and iOS
# Usage: ./scripts/lint-store-metadata.sh [--strict] [--json] [--check-live-urls]
# ──────────────────────────────────────────────────────────────

set -euo pipefail

STRICT_MODE=false
JSON_OUTPUT=false
PLATFORM="all"
CHECK_LIVE_URLS=false
ERRORS=0
WARNINGS=0
RESULTS=()

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse arguments
for arg in "$@"; do
    case $arg in
        --strict) STRICT_MODE=true ;;
        --json) JSON_OUTPUT=true ;;
        --ios-only) PLATFORM="ios" ;;
        --android-only) PLATFORM="android" ;;
        --platform=ios) PLATFORM="ios" ;;
        --platform=android) PLATFORM="android" ;;
        --platform=all) PLATFORM="all" ;;
        --check-live-urls) CHECK_LIVE_URLS=true ;;
        --help|-h) echo "Usage: $0 [--strict] [--json] [--check-live-urls] [--ios-only|--android-only|--platform=ios|--platform=android|--platform=all]"; exit 0 ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
METADATA_DIR="$PROJECT_DIR/composeApp/metadata"

# ──────────────────────────────────────────────────────────────
# Helper functions
# ──────────────────────────────────────────────────────────────

error() {
    ERRORS=$((ERRORS + 1))
    RESULTS+=("ERROR: $1")
    echo -e "  ${RED}❌ ERROR: $1${NC}"
}

warning() {
    WARNINGS=$((WARNINGS + 1))
    RESULTS+=("WARNING: $1")
    echo -e "  ${YELLOW}⚠️  WARNING: $1${NC}"
}

pass() {
    RESULTS+=("PASS: $1")
    echo -e "  ${GREEN}✅ $1${NC}"
}

info() {
    echo -e "  ${BLUE}ℹ️  $1${NC}"
}

# ──────────────────────────────────────────────────────────────
# Validation functions
# ──────────────────────────────────────────────────────────────

validate_text_file() {
    local filepath="$1"
    local label="$2"
    local min_chars="${3:-1}"
    local max_chars="${4:-9999}"

    if [ ! -f "$filepath" ]; then
        error "$label: File not found ($filepath)"
        return 1
    fi

    local content
    content=$(cat "$filepath")
    local char_count=${#content}

    if [ "$char_count" -eq 0 ]; then
        error "$label: Empty file"
        return 1
    fi

    if [ "$char_count" -lt "$min_chars" ]; then
        error "$label: Too short ($char_count chars, min $min_chars)"
        return 1
    fi

    if [ "$char_count" -gt "$max_chars" ]; then
        error "$label: Too long ($char_count chars, max $max_chars)"
        return 1
    fi

    pass "$label: Valid ($char_count chars)"
    return 0
}

validate_url_file() {
    local filepath="$1"
    local label="$2"

    if validate_text_file "$filepath" "$label" 8 500; then
        local url
        url=$(tr -d '\r\n' < "$filepath")
        if [[ "$url" =~ ^https://[^[:space:]]+$ ]]; then
            pass "$label: HTTPS URL"
            validate_live_url "$url" "$label"
        else
            error "$label: Must be an HTTPS URL"
        fi
    fi
}

validate_review_phone_file() {
    local filepath="$1"
    local label="$2"

    if validate_text_file "$filepath" "$label" 4 60; then
        local phone normalized digit_count
        phone=$(tr -d '\r\n' < "$filepath")
        validate_review_phone_value "$phone" "$label"
    fi
}

validate_review_phone_value() {
    local phone="$1"
    local label="$2"
    local normalized digit_count

    normalized=$(printf '%s' "$phone" | tr -d ' .()-')
    digit_count=$(printf '%s' "$normalized" | tr -cd '0-9' | wc -c | tr -d ' ')

    if [[ "$normalized" =~ ^\+?[0-9]+$ ]] && [ "$digit_count" -ge 4 ] && [ "$digit_count" -le 20 ]; then
        pass "$label: Valid phone format"
    else
        error "$label: Must include 4-20 digits, optionally prefixed with +"
    fi
}

validate_live_url() {
    local url="$1"
    local label="$2"

    [ "$CHECK_LIVE_URLS" = true ] || return 0

    if ! command -v curl >/dev/null 2>&1; then
        warning "$label: Cannot check live URL because curl is not available"
        return 0
    fi

    if /usr/bin/curl -fsSI --max-time 10 --retry 1 "$url" >/dev/null 2>&1; then
        pass "$label: Live URL reachable"
        return 0
    fi

    if /usr/bin/curl -fsSL --max-time 10 --retry 1 "$url" -o /dev/null >/dev/null 2>&1; then
        pass "$label: Live URL reachable"
        return 0
    fi

    error "$label: Live URL is not reachable ($url)"
}

validate_live_redirect() {
    local url="$1"
    local expected_location="$2"
    local label="$3"

    [ "$CHECK_LIVE_URLS" = true ] || return 0

    if ! command -v curl >/dev/null 2>&1; then
        warning "$label: Cannot check live redirect because curl is not available"
        return 0
    fi

    local headers_file
    headers_file=$(mktemp)

    if ! /usr/bin/curl -fsSI --max-time 10 --retry 1 "$url" > "$headers_file" 2>/dev/null; then
        rm -f "$headers_file"
        error "$label: Live redirect is not reachable ($url)"
        return 0
    fi

    if grep -Eiq '^location:[[:space:]]*(https://wakeve\.app)?'"$expected_location"'([[:space:]]|$)' "$headers_file"; then
        pass "$label: Redirects to $expected_location"
    else
        error "$label: Expected redirect to $expected_location"
    fi

    rm -f "$headers_file"
}

is_ios_iphone_screenshot_size() {
    local width="$1"
    local height="$2"

    case "${width}x${height}" in
        1260x2736|2736x1260|1290x2796|2796x1290|1320x2868|2868x1320|\
1284x2778|2778x1284|1242x2688|2688x1242|1179x2556|2556x1179|\
1206x2622|2622x1206|1170x2532|2532x1170|1125x2436|2436x1125|\
1080x2340|2340x1080|1242x2208|2208x1242|750x1334|1334x750|\
640x1096|640x1136|1136x600|1136x640|640x920|640x960|960x600|960x640)
            return 0
            ;;
    esac

    return 1
}

is_ios_ipad_screenshot_size() {
    local width="$1"
    local height="$2"

    case "${width}x${height}" in
        2064x2752|2752x2064|2048x2732|2732x2048|1488x2266|2266x1488|\
1668x2420|2420x1668|1668x2388|2388x1668|1640x2360|2360x1640|\
1668x2224|2224x1668|1536x2008|1536x2048|2048x1496|2048x1536|\
768x1004|768x1024|1024x748|1024x768)
            return 0
            ;;
    esac

    return 1
}

validate_ios_screenshot_file() {
    local screenshot="$1"
    local label="$2"
    local width height

    width=$(sips -g pixelWidth "$screenshot" 2>/dev/null | awk '/pixelWidth/ {print $2}')
    height=$(sips -g pixelHeight "$screenshot" 2>/dev/null | awk '/pixelHeight/ {print $2}')

    if [ -z "$width" ] || [ -z "$height" ]; then
        warning "$label: Could not inspect $(basename "$screenshot")"
        return 1
    fi

    if is_ios_iphone_screenshot_size "$width" "$height"; then
        pass "$label: $(basename "$screenshot") is accepted iPhone size (${width}x${height})"
        return 0
    fi

    if is_ios_ipad_screenshot_size "$width" "$height"; then
        pass "$label: $(basename "$screenshot") is accepted iPad size (${width}x${height})"
        return 0
    fi

    error "$label: $(basename "$screenshot") has unsupported App Store screenshot size (${width}x${height})"
    return 1
}

validate_android_locale() {
    local locale_dir="$METADATA_DIR/android/$1"
    local locale="$1"

    echo ""
    echo -e "${BLUE}📱 Android — $locale${NC}"

    # Title (max 30 chars)
    validate_text_file "$locale_dir/title.txt" "Title ($locale)" 1 30

    # Short description (max 80 chars)
    validate_text_file "$locale_dir/short_description.txt" "Short description ($locale)" 1 80

    # Full description (max 4000 chars)
    validate_text_file "$locale_dir/full_description.txt" "Full description ($locale)" 50 4000

    # Changelog (optional, max 500 chars)
    if [ -f "$locale_dir/changelogs/default.txt" ]; then
        validate_text_file "$locale_dir/changelogs/default.txt" "Changelog ($locale)" 1 500
    else
        if [ "$STRICT_MODE" = true ]; then
            warning "Changelog ($locale): Missing (required in strict mode)"
        else
            info "Changelog ($locale): Not present (optional)"
        fi
    fi

    # Screenshots directory
    if [ -d "$locale_dir/images/phoneScreenshots" ]; then
        local screenshot_count
        screenshot_count=$(find "$locale_dir/images/phoneScreenshots" -type f | wc -l | tr -d ' ')
        if [ "$screenshot_count" -lt 2 ]; then
            warning "Screenshots ($locale): Only $screenshot_count (min 2 recommended, max 8)"
        else
            pass "Screenshots ($locale): $screenshot_count images"
        fi
    else
        if [ "$STRICT_MODE" = true ]; then
            warning "Screenshots ($locale): No screenshots directory"
        else
            info "Screenshots ($locale): No screenshots directory yet"
        fi
    fi
}

validate_ios_locale() {
    local locale_dir="$METADATA_DIR/ios/$1"
    local locale="$1"

    echo ""
    echo -e "${BLUE}🍎 iOS — $locale${NC}"

    # Name (max 30 chars)
    validate_text_file "$locale_dir/name.txt" "App name ($locale)" 1 30

    # Subtitle (max 30 chars)
    if [ -f "$locale_dir/subtitle.txt" ]; then
        validate_text_file "$locale_dir/subtitle.txt" "Subtitle ($locale)" 1 30
    else
        if [ "$STRICT_MODE" = true ]; then
            warning "Subtitle ($locale): Missing (recommended)"
        else
            info "Subtitle ($locale): Not present"
        fi
    fi

    # Description (max 4000 chars)
    validate_text_file "$locale_dir/description.txt" "Description ($locale)" 50 4000
    if [ -f "$locale_dir/description.txt" ]; then
        if grep -Eiq "data stays on your device|vos données restent sur votre appareil" "$locale_dir/description.txt"; then
            error "Description ($locale): Avoid claiming all data stays on-device while encrypted sync/privacy labels declare collected data"
        else
            pass "Description ($locale): No conflicting on-device-only privacy claim"
        fi

        if grep -Eiq "Android Material You|iOS Liquid Glass" "$locale_dir/description.txt"; then
            error "Description ($locale): Avoid unnecessary platform trademark/design-system references in App Store metadata"
        else
            pass "Description ($locale): No unnecessary platform trademark/design-system reference"
        fi
    fi

    # Keywords (max 100 chars, comma-separated)
    if [ -f "$locale_dir/keywords.txt" ]; then
        local keywords
        keywords=$(cat "$locale_dir/keywords.txt")
        local keyword_count=${#keywords}
        if [ "$keyword_count" -gt 100 ]; then
            error "Keywords ($locale): Too long ($keyword_count chars, max 100)"
        else
            pass "Keywords ($locale): Valid ($keyword_count chars)"
        fi
    else
        if [ "$STRICT_MODE" = true ]; then
            error "Keywords ($locale): Missing"
        else
            warning "Keywords ($locale): Missing (recommended for ASO)"
        fi
    fi

    # Release notes
    if [ -f "$locale_dir/release_notes.txt" ]; then
        validate_text_file "$locale_dir/release_notes.txt" "Release notes ($locale)" 1 4000
    fi

    # App Store Connect required public URLs
    validate_url_file "$locale_dir/privacy_url.txt" "Privacy URL ($locale)"
    validate_url_file "$locale_dir/support_url.txt" "Support URL ($locale)"

    # Promotional text (optional, max 170 chars)
    if [ -f "$locale_dir/promotional_text.txt" ]; then
        validate_text_file "$locale_dir/promotional_text.txt" "Promotional text ($locale)" 1 170
    fi

    # Screenshots directory
    if [ -d "$locale_dir/screenshots" ]; then
        local screenshot_count
        screenshot_count=$(find "$locale_dir/screenshots" -type f \( -name "*.png" -o -name "*.jpg" -o -name "*.jpeg" \) | wc -l | tr -d ' ')

        if [ "$screenshot_count" -lt 1 ]; then
            warning "iOS screenshots ($locale): No images found"
        else
            pass "iOS screenshots ($locale): $screenshot_count images"
        fi

        local has_iphone=false
        local has_ipad=false
        while IFS= read -r screenshot; do
            local width height
            width=$(sips -g pixelWidth "$screenshot" 2>/dev/null | awk '/pixelWidth/ {print $2}')
            height=$(sips -g pixelHeight "$screenshot" 2>/dev/null | awk '/pixelHeight/ {print $2}')

            if [ -z "$width" ] || [ -z "$height" ]; then
                warning "iOS screenshots ($locale): Could not inspect $(basename "$screenshot")"
                continue
            fi

            validate_ios_screenshot_file "$screenshot" "iOS screenshots ($locale)"

            if is_ios_iphone_screenshot_size "$width" "$height"; then
                has_iphone=true
            fi

            if is_ios_ipad_screenshot_size "$width" "$height"; then
                has_ipad=true
            fi
        done < <(find "$locale_dir/screenshots" -type f \( -name "*.png" -o -name "*.jpg" -o -name "*.jpeg" \))

        if [ "$has_iphone" = true ]; then
            pass "iOS screenshots ($locale): Contains tall iPhone screenshot"
        else
            warning "iOS screenshots ($locale): Missing tall iPhone screenshot"
        fi

        if [ "$has_ipad" = true ]; then
            pass "iOS screenshots ($locale): Contains iPad-sized screenshot"
        else
            warning "iOS screenshots ($locale): Missing iPad-sized screenshot for universal app"
        fi
    else
        if [ "$STRICT_MODE" = true ]; then
            warning "iOS screenshots ($locale): No screenshots directory"
        else
            info "iOS screenshots ($locale): No screenshots directory yet"
        fi
    fi
}

validate_legal() {
    echo ""
    echo -e "${BLUE}⚖️  Legal Pages${NC}"

    local legal_dir="$PROJECT_DIR/docs"

    # Privacy Policy
    if [ -f "$legal_dir/PRIVACY_POLICY.md" ]; then
        local pp_size
        pp_size=$(wc -c < "$legal_dir/PRIVACY_POLICY.md")
        if [ "$pp_size" -lt 1000 ]; then
            warning "Privacy Policy: Very short ($pp_size bytes) — may not cover all requirements"
        else
            pass "Privacy Policy: Present ($pp_size bytes)"
        fi

        # Check for required sections
        for section in "Data Collection" "Information We Collect" "Your Rights" "Contact"; do
            if grep -qi "$section" "$legal_dir/PRIVACY_POLICY.md"; then
                pass "Privacy Policy: Contains '$section' section"
            else
                warning "Privacy Policy: Missing '$section' section"
            fi
        done
    else
        error "Privacy Policy: Not found at docs/PRIVACY_POLICY.md"
    fi

    # Terms of Service
    if [ -f "$legal_dir/TERMS_OF_SERVICE.md" ]; then
        local tos_size
        tos_size=$(wc -c < "$legal_dir/TERMS_OF_SERVICE.md")
        pass "Terms of Service: Present ($tos_size bytes)"
    else
        error "Terms of Service: Not found at docs/TERMS_OF_SERVICE.md"
    fi

    validate_account_deletion_public_claims
}

validate_account_deletion_public_claims() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    local claim_files=(
        "$PROJECT_DIR/docs/PRIVACY_POLICY.md"
        "$PROJECT_DIR/docs/TERMS_OF_SERVICE.md"
        "$PROJECT_DIR/apps/landing/src/routes/privacy/+page.svelte"
        "$PROJECT_DIR/apps/landing/src/routes/support/+page.svelte"
        "$PROJECT_DIR/apps/landing/src/routes/terms/+page.svelte"
    )

    local has_verified_account_deletion=false
    if grep -R "deleteAccount\\|DELETE /api/auth/account\\|delete.*account" "$PROJECT_DIR/iosApp/src" "$PROJECT_DIR/server/src/main/kotlin" >/dev/null 2>&1; then
        has_verified_account_deletion=true
    fi

    local public_claim_pattern="Profile Settings.*Delete Account|Delete Account.*Profile Settings|Profile Settings in the app|in-app settings"
    local matches=""
    local file rel
    for file in "${claim_files[@]}"; do
        [ -f "$file" ] || continue
        if grep -Ein "$public_claim_pattern" "$file" >/dev/null 2>&1; then
            rel="${file#$PROJECT_DIR/}"
            matches+="$rel"$'\n'
        fi
    done

    if [ "$has_verified_account_deletion" = false ] && [ -n "$matches" ]; then
        printf '%s' "$matches" | sed 's/^/    /'
        error "Account deletion public claims: Public legal/support pages claim in-app deletion before the verified iOS/API deletion flow exists"
    else
        pass "Account deletion public claims: No unsupported in-app deletion promise in public legal/support pages"
    fi
}

validate_public_web_legal_routes() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🌍 Public Legal Web Routes${NC}"

    local privacy_route="$PROJECT_DIR/apps/landing/src/routes/privacy/+page.svelte"
    local privacy_route_options="$PROJECT_DIR/apps/landing/src/routes/privacy/+page.ts"
    local support_route="$PROJECT_DIR/apps/landing/src/routes/support/+page.svelte"
    local support_route_options="$PROJECT_DIR/apps/landing/src/routes/support/+page.ts"
    local notices_route="$PROJECT_DIR/apps/landing/src/routes/third-party-notices/+page.svelte"
    local notices_route_options="$PROJECT_DIR/apps/landing/src/routes/third-party-notices/+page.ts"
    local terms_route="$PROJECT_DIR/apps/landing/src/routes/terms/+page.svelte"
    local terms_route_options="$PROJECT_DIR/apps/landing/src/routes/terms/+page.ts"

    local route
    for route in "$privacy_route" "$support_route" "$notices_route" "$terms_route"; do
        if [ -f "$route" ]; then
            pass "Public legal route: ${route#$PROJECT_DIR/} exists"
        else
            error "Public legal route: Missing ${route#$PROJECT_DIR/}"
        fi
    done

    local route_option
    for route_option in "$privacy_route_options" "$support_route_options" "$notices_route_options" "$terms_route_options"; do
        if [ -f "$route_option" ] && grep -Fq "export const ssr = true" "$route_option"; then
            pass "Public legal route SSR: ${route_option#$PROJECT_DIR/} enables server-rendered App Store review content"
        else
            error "Public legal route SSR: ${route_option#$PROJECT_DIR/} must export 'ssr = true' so App Store review pages are visible in initial HTML"
        fi
    done

    local required_route_content=(
        "$privacy_route|Privacy Policy"
        "$privacy_route|Information We Collect"
        "$privacy_route|Your Rights"
        "$privacy_route|privacy@wakeve.app"
        "$privacy_route|does not sell personal information"
        "$support_route|Support"
        "$support_route|support@wakeve.app"
        "$support_route|privacy@wakeve.app"
        "$support_route|/privacy"
        "$support_route|/terms"
        "$support_route|/third-party-notices"
        "$notices_route|Third-Party Notices"
        "$notices_route|Dependencies listed:"
        "$notices_route|Unknown licenses:"
        "$notices_route|Copyleft keywords detected:"
        "$notices_route|Submitted iOS unknown/copyleft risks:"
        "$notices_route|Dependency Notices"
        "$notices_route|Review Status"
        "$terms_route|Terms of Service"
        "$terms_route|User Conduct and Content"
        "$terms_route|Privacy Policy"
        "$terms_route|legal@wakeve.app"
        "$terms_route|request account deletion by email"
    )

    local entry file phrase
    for entry in "${required_route_content[@]}"; do
        file="${entry%%|*}"
        phrase="${entry#*|}"
        if [ -f "$file" ] && grep -Fq "$phrase" "$file"; then
            pass "Public legal route: ${file#$PROJECT_DIR/} covers $phrase"
        else
            error "Public legal route: ${file#$PROJECT_DIR/} missing '$phrase'"
        fi
    done

    local metadata_files=(
        "$PROJECT_DIR/composeApp/metadata/ios/en-US/privacy_url.txt|https://wakeve.app/privacy"
        "$PROJECT_DIR/composeApp/metadata/ios/fr-FR/privacy_url.txt|https://wakeve.app/privacy"
        "$PROJECT_DIR/composeApp/metadata/ios/en-US/support_url.txt|https://wakeve.app/support"
        "$PROJECT_DIR/composeApp/metadata/ios/fr-FR/support_url.txt|https://wakeve.app/support"
    )

    for entry in "${metadata_files[@]}"; do
        file="${entry%%|*}"
        phrase="${entry#*|}"
        if [ -f "$file" ] && grep -Fxq "$phrase" "$file"; then
            pass "Public legal route metadata: ${file#$PROJECT_DIR/} points to $phrase"
        else
            error "Public legal route metadata: ${file#$PROJECT_DIR/} must point to $phrase"
        fi
    done
}

validate_fastlane() {
    echo ""
    echo -e "${BLUE}🚀 Fastlane Configuration${NC}"

    local fastlane_dir="$PROJECT_DIR/fastlane"
    local expected_fastlane_version="2.228.0"

    if [ -f "$PROJECT_DIR/Gemfile" ]; then
        if grep -Fq "gem \"fastlane\", \"$expected_fastlane_version\"" "$PROJECT_DIR/Gemfile"; then
            pass "Gemfile: Pins fastlane $expected_fastlane_version"
        else
            error "Gemfile: Must pin fastlane $expected_fastlane_version"
        fi
    else
        error "Gemfile: Missing"
    fi

    if [ -f "$PROJECT_DIR/Gemfile.lock" ]; then
        if grep -Fq "fastlane ($expected_fastlane_version)" "$PROJECT_DIR/Gemfile.lock" && grep -Fq "fastlane (= $expected_fastlane_version)" "$PROJECT_DIR/Gemfile.lock"; then
            pass "Gemfile.lock: Locks fastlane $expected_fastlane_version"
        else
            error "Gemfile.lock: Must lock fastlane $expected_fastlane_version"
        fi
    else
        error "Gemfile.lock: Missing"
    fi

    if [ -f "$fastlane_dir/Fastfile" ]; then
        pass "Fastfile: Present"

        # Check for key lanes
        for lane in "upload_internal" "upload_production" "upload_testflight" "upload_appstore" "validate_ipa_entitlements"; do
            if grep -q "$lane" "$fastlane_dir/Fastfile"; then
                pass "Fastfile: Contains '$lane' lane"
            else
                warning "Fastfile: Missing '$lane' lane"
            fi
        done

        if [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ]; then
            local strict_preflight_count
            strict_preflight_count=$(grep -Fc "preflight(strict: true, live_urls: true)" "$fastlane_dir/Fastfile" || true)
            if [ "$strict_preflight_count" -ge 2 ]; then
                pass "Fastfile: App Store upload and submission readiness run strict/live preflight"
            else
                error "Fastfile: App Store upload and submission readiness must run strict/live preflight"
            fi

            local required_patterns=(
                "FINAL_APP_STORE_SIGNOFF_ENV_VARS"
                "APP_STORE_DSA_TRADER_STATUS_CONFIRMED"
                "APP_STORE_PRICING_AVAILABILITY_CONFIRMED"
                "APP_STORE_SDK_PRIVACY_CONFIRMED"
                "APP_STORE_RELEASE_CONTROL_CONFIRMED"
                "APP_STORE_MEDIA_LOCALIZATION_CONFIRMED"
                "APP_STORE_LICENSE_NOTICES_CONFIRMED"
                "APP_STORE_EULA_CONFIRMED"
                "ensure_apple_release_env_values([\"APPLE_ID\", \"ITC_TEAM_ID\", \"TEAM_ID\", \"APPLE_TEAM_ID\"])"
                "ensure_truthy_env_vars(FINAL_APP_STORE_SIGNOFF_ENV_VARS, \"App Store final upload\")"
                "ensure_final_app_store_signoff_record"
                "run_final_app_store_submission_audit"
                "./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready"
                "Final App Store signoff record still contains unresolved TBD/Pending/status/checklist placeholders"
                "upload_appstore"
                "ensure_real_app_review_phone"
                "placeholder_review_phone?"
                "APP_REVIEW_PHONE_NUMBER uses the documented placeholder +15551234567"
                "APPLE_ID uses the documented placeholder release@example.com"
                "ITC_TEAM_ID uses the documented placeholder 123456789"
                "uses the documented placeholder ABCDE12345"
                "ensure_latest_release_build_log_has_no_diagnostics"
                "WEB_PNPM_COMMAND = \"npx --yes pnpm@10\""
                "#{WEB_PNPM_COMMAND} audit --audit-level low"
                "#{WEB_PNPM_COMMAND} check"
                "#{WEB_PNPM_COMMAND} build"
                "run_local_web_route_check"
                "BASE_URL=http://127.0.0.1:3000 APPLE_TEAM_ID=A1B2C3D4E5 ./scripts/app-store-local-web-route-check.sh"
                "validate_built_ipa_entitlements(repo_path(\"build/ios/WakeveApp.ipa\"))"
                "screenshots_path: repo_path(\"composeApp/screenshots/ios\")"
                "app_rating_config_path: repo_path(\"composeApp/metadata/ios/app_rating_config.json\")"
                "app_review_information: app_review_information_from_metadata"
                "skip_screenshots: false"
                "submit_for_review: false"
                "automatic_release: false"
            )

            local pattern
            for pattern in "${required_patterns[@]}"; do
                if grep -Fq "$pattern" "$fastlane_dir/Fastfile"; then
                    pass "Fastfile: Contains required App Store upload guard '$pattern'"
                else
                    error "Fastfile: Missing required App Store upload guard '$pattern'"
                fi
            done

            local upload_testflight_block
            upload_testflight_block=$(awk '
                /lane :upload_testflight do/ { in_lane=1; next }
                in_lane && /^  end$/ { exit }
                in_lane { print }
            ' "$fastlane_dir/Fastfile")

            local testflight_preflight_line
            local testflight_build_line
            testflight_preflight_line=$(printf '%s\n' "$upload_testflight_block" | grep -nF "preflight" | head -n 1 | cut -d: -f1 || true)
            testflight_build_line=$(printf '%s\n' "$upload_testflight_block" | grep -nE '^[[:space:]]+build$' | head -n 1 | cut -d: -f1 || true)
            if [ -n "$testflight_preflight_line" ] && [ -n "$testflight_build_line" ] && [ "$testflight_preflight_line" -lt "$testflight_build_line" ]; then
                pass "Fastfile: TestFlight upload runs local preflight before build"
            else
                error "Fastfile: TestFlight upload must run local preflight before build"
            fi

            if grep -Fq "submit_for_review: true" "$fastlane_dir/Fastfile"; then
                error "Fastfile: App Store upload must not submit for review automatically"
            else
                pass "Fastfile: Does not auto-submit for App Review"
            fi

            local store_readiness_workflow="$PROJECT_DIR/.github/workflows/store-readiness.yml"
            if [ -f "$store_readiness_workflow" ]; then
                pass "Store readiness workflow: Present"

                local workflow_required_patterns=(
                    "version: 10"
                    "pnpm install --frozen-lockfile"
                    "Audit web dependencies"
                    "pnpm audit --audit-level low"
                    "Verify local public web and AASA routes"
                    "CI=true APPLE_TEAM_ID=A1B2C3D4E5 npx --yes pnpm@10 exec vite dev --host 127.0.0.1 --port 3000 --strictPort"
                    "BASE_URL=http://127.0.0.1:3000 APPLE_TEAM_ID=A1B2C3D4E5 ./scripts/app-store-local-web-route-check.sh"
                    "bash -n scripts/app-store-local-web-route-check.sh"
                    "Verify placeholder App Review phone is rejected"
                    "submission_ready unexpectedly accepted the documented App Review phone placeholder"
                    "upload_appstore unexpectedly accepted the documented App Review phone placeholder"
                    "final audit unexpectedly accepted the documented App Review phone placeholder"
                    "final audit did not fail on the documented App Review phone placeholder"
                    "Final App Store lanes reject the documented App Review phone placeholder"
                    "replace it with the real reachable App Review contact before final audit"
                    "Verify Apple release placeholders are rejected"
                    "submission_ready unexpectedly accepted documented Apple release placeholders"
                    "upload_testflight unexpectedly accepted documented Apple release placeholders"
                    "upload_testflight did not reject the documented APPLE_ID placeholder"
                    "upload_testflight ran preflight before rejecting documented Apple release placeholders"
                    "final audit unexpectedly accepted documented Apple release placeholders"
                    "APPLE_ID uses the documented placeholder release@example.com"
                    "ITC_TEAM_ID uses the documented placeholder 123456789"
                    "TEAM_ID uses the documented placeholder ABCDE12345"
                    "APPLE_TEAM_ID uses the documented placeholder ABCDE12345"
                    "Final App Store and TestFlight gates reject documented Apple release placeholders"
                    "Verify upload rejects incomplete final signoff record"
                    "upload_appstore unexpectedly accepted an incomplete final signoff record"
                    "upload_appstore did not reject unresolved final signoff placeholders"
                    "upload_appstore ran the final audit before rejecting incomplete signoff evidence"
                    "App Store upload rejects incomplete final signoff record even when the marker is forced"
                    "Final audit does not document that local preflight is required for a ready result"
                    "Final audit does not block ready-state evaluation when local preflight is skipped"
                    "Verify UGC moderation signoff guards cannot be bypassed"
                    "./scripts/test-app-store-ugc-gates.sh"
                    "Verify forced final signoffs still require final evidence"
                    "Final audit unexpectedly accepted forced final signoffs without DSA/pricing/SDK/release-control/media/licenses/eula/product/account/review/export/app-information/versioning/release-artifact/content-rights/observability/live-url-aasa evidence"
                    "Final audit did not require DSA trader status evidence when final signoffs were forced"
                    "Final audit did not require pricing and availability evidence when final signoffs were forced"
                    "Final audit did not require SDK privacy evidence when final signoffs were forced"
                    "Final audit did not require license notices evidence when final signoffs were forced"
                    "Final audit did not require EULA evidence when final signoffs were forced"
                    "Final audit did not require account access evidence when final signoffs were forced"
                    "Final audit did not require review access evidence when final signoffs were forced"
                    "Final audit did not require export compliance evidence when final signoffs were forced"
                    "Final audit did not require App Information evidence when final signoffs were forced"
                    "Final audit did not require versioning evidence when final signoffs were forced"
                    "Final audit did not require release artifact evidence when final signoffs were forced"
                    "Final audit did not require content rights evidence when final signoffs were forced"
                    "Final audit did not require observability evidence when final signoffs were forced"
                    "Final audit did not require live URL/AASA evidence when final signoffs were forced"
                    "Final audit rejects forced final signoffs without DSA/pricing/SDK/release-control/media/licenses/eula/product/account/review/export/app-information/versioning/release-artifact/content-rights/observability/live-url-aasa evidence"
                    "App Store submission is still blocked until these external/manual release items are complete"
                    "Apple signing, real Apple release env values, App Review phone, Apple Developer capabilities/profiles, and signed IPA entitlement inspection"
                    "Live production legal/support/backend URLs, AASA validation with the production Apple Team ID, AS-14 evidence, and signed final submission-ready gate output"
                    "Privacy/legal approval, Accessibility Nutrition Label evidence, App Store availability evidence, EU DSA trader status evidence, pricing and availability evidence, third-party SDK privacy evidence, App Store release control evidence, App Store media/localization evidence, license inventory/notices evidence, EULA evidence, account access evidence, review access evidence, export compliance evidence, App Store versioning evidence, release artifact evidence, and content rights evidence"
                    "Account deletion readiness, UGC moderation readiness, payment/external purchase compliance, TestFlight smoke evidence, App Store observability evidence, live URL/AASA evidence, and the final signoff record"
                )
                local workflow_pattern
                for workflow_pattern in "${workflow_required_patterns[@]}"; do
                    if grep -Fq "$workflow_pattern" "$store_readiness_workflow"; then
                        pass "Store readiness workflow: Covers $workflow_pattern"
                    else
                        error "Store readiness workflow: Missing '$workflow_pattern'"
                    fi
                done
            else
                error "Store readiness workflow: Missing at .github/workflows/store-readiness.yml"
            fi
        fi
    else
        warning "Fastfile: Not found (optional but recommended)"
    fi

    if [ -f "$fastlane_dir/Appfile" ]; then
        pass "Appfile: Present"

        if grep -Fq 'app_identifier("com.guyghost.wakeve")' "$fastlane_dir/Appfile"; then
            pass "Appfile: iOS Bundle ID is com.guyghost.wakeve"
        else
            error "Appfile: Missing iOS Bundle ID com.guyghost.wakeve"
        fi

        if grep -Fq 'package_name("com.guyghost.wakeve")' "$fastlane_dir/Appfile"; then
            pass "Appfile: Android package name is com.guyghost.wakeve"
        else
            error "Appfile: Missing Android package name com.guyghost.wakeve"
        fi

        local appfile_env_patterns=(
            'apple_id(ENV["APPLE_ID"])'
            'itc_team_id(ENV["ITC_TEAM_ID"])'
            'team_id(ENV["TEAM_ID"])'
        )
        local appfile_pattern
        for appfile_pattern in "${appfile_env_patterns[@]}"; do
            if grep -Fq "$appfile_pattern" "$fastlane_dir/Appfile"; then
                pass "Appfile: Uses $appfile_pattern"
            else
                error "Appfile: Must use $appfile_pattern"
            fi
        done
    else
        error "Appfile: Missing"
    fi

    if [ -f "$METADATA_DIR/ios/copyright.txt" ]; then
        validate_text_file "$METADATA_DIR/ios/copyright.txt" "iOS copyright" 1 500
    elif [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ]; then
        error "iOS copyright: Missing at composeApp/metadata/ios/copyright.txt"
    fi

    if [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ]; then
        if [ -f "$METADATA_DIR/ios/app_rating_config.json" ]; then
            if ruby -rjson -e 'JSON.parse(File.read(ARGV.fetch(0)))' "$METADATA_DIR/ios/app_rating_config.json" >/dev/null 2>&1; then
                pass "iOS app rating config: Valid JSON"
            else
                error "iOS app rating config: Invalid JSON"
            fi

            local rating_schema_errors
            if rating_schema_errors=$(ruby -rjson -e '
                path = ARGV.fetch(0)
                json = JSON.parse(File.read(path))
                rating_keys = %w[
                    alcoholTobaccoOrDrugUseOrReferences
                    contests
                    gamblingSimulated
                    medicalOrTreatmentInformation
                    profanityOrCrudeHumor
                    sexualContentGraphicAndNudity
                    sexualContentOrNudity
                    horrorOrFearThemes
                    matureOrSuggestiveThemes
                    violenceCartoonOrFantasy
                    violenceRealisticProlongedGraphicOrSadistic
                    violenceRealistic
                ]
                boolean_keys = %w[
                    gambling
                    seventeenPlus
                    unrestrictedWebAccess
                ]
                optional_keys = %w[kidsAgeBand]
                allowed_rating_values = %w[
                    NONE
                    INFREQUENT_OR_MILD
                    FREQUENT_OR_INTENSE
                ]
                allowed_kids_age_bands = %w[
                    FIVE_AND_UNDER
                    SIX_TO_EIGHT
                    NINE_TO_ELEVEN
                ]
                required_keys = rating_keys + boolean_keys
                allowed_keys = required_keys + optional_keys
                errors = []

                missing_keys = required_keys - json.keys
                unknown_keys = json.keys - allowed_keys
                errors << "missing required keys: #{missing_keys.join(", ")}" unless missing_keys.empty?
                errors << "unknown keys: #{unknown_keys.join(", ")}" unless unknown_keys.empty?
                errors << "deprecated key present: gamblingAndContests" if json.key?("gamblingAndContests")

                rating_keys.each do |key|
                    next unless json.key?(key)
                    next if allowed_rating_values.include?(json[key])
                    errors << "#{key} must be one of #{allowed_rating_values.join("/")}"
                end

                boolean_keys.each do |key|
                    next unless json.key?(key)
                    next if json[key] == true || json[key] == false
                    errors << "#{key} must be boolean"
                end

                if json.key?("kidsAgeBand") && !allowed_kids_age_bands.include?(json["kidsAgeBand"])
                    errors << "kidsAgeBand must be one of #{allowed_kids_age_bands.join("/")}"
                end

                if errors.any?
                    warn errors.join("\n")
                    exit 1
                end
            ' "$METADATA_DIR/ios/app_rating_config.json" 2>&1); then
                pass "iOS app rating config: Schema matches Fastlane age rating declaration"
            else
                error "iOS app rating config: Invalid schema"
                info "$rating_schema_errors"
            fi
        else
            error "iOS app rating config: Missing at composeApp/metadata/ios/app_rating_config.json"
        fi

        if [ -f "$METADATA_DIR/ios/review_information/notes.txt" ]; then
            local review_notes_file="$METADATA_DIR/ios/review_information/notes.txt"
            validate_text_file "$METADATA_DIR/ios/review_information/notes.txt" "App Review notes" 20 4000
            if [ -f "$METADATA_DIR/ios/review_information/demo_password.txt" ]; then
                error "App Review demo account: Do not store demo_password.txt in repository metadata; enter demo credentials manually in App Store Connect"
            elif [ -f "$METADATA_DIR/ios/review_information/demo_user.txt" ]; then
                warning "App Review demo account: demo_user.txt exists without demo_password.txt; verify App Store Connect has the matching password"
            elif grep -Eiq "guest|invite|continuer en invite|mode invite|demo account|test account" "$review_notes_file"; then
                pass "App Review notes: Explain guest/demo review access"
            else
                error "App Review notes: Must explain guest review access or reference App Store Connect demo credentials"
            fi

            if grep -R "PaymentPotRepository\\|TricountHandoffRepository\\|paymentRoutes\\|tricountGroupUrl\\|providerUrl" \
                "$PROJECT_DIR/server/src/main/kotlin" \
                "$PROJECT_DIR/shared/src/commonMain/kotlin" \
                "$PROJECT_DIR/iosApp/src" >/dev/null 2>&1; then
                local payment_note_phrases=(
                    "real-world shared event expenses"
                    "do not sell or unlock app features"
                    "digital content"
                    "Tricount"
                    "trusted-domain validated"
                )

                local payment_note_phrase
                for payment_note_phrase in "${payment_note_phrases[@]}"; do
                    if grep -Fqi "$payment_note_phrase" "$review_notes_file"; then
                        pass "App Review notes: Payment explanation covers $payment_note_phrase"
                    else
                        error "App Review notes: Missing payment explanation '$payment_note_phrase'"
                    fi
                done
            fi
        else
            error "App Review notes: Missing at composeApp/metadata/ios/review_information/notes.txt"
        fi

        validate_text_file "$METADATA_DIR/ios/review_information/first_name.txt" "App Review contact first name" 1 100
        validate_text_file "$METADATA_DIR/ios/review_information/last_name.txt" "App Review contact last name" 1 100
        validate_text_file "$METADATA_DIR/ios/review_information/email_address.txt" "App Review contact email" 3 320

        if [ -f "$METADATA_DIR/ios/review_information/email_address.txt" ]; then
            local review_email
            review_email=$(cat "$METADATA_DIR/ios/review_information/email_address.txt")
            if [[ "$review_email" =~ ^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$ ]]; then
                pass "App Review contact email: Valid format"
            else
                error "App Review contact email: Invalid format"
            fi
        fi

        if [ -f "$METADATA_DIR/ios/review_information/phone_number.txt" ]; then
            validate_review_phone_file "$METADATA_DIR/ios/review_information/phone_number.txt" "App Review contact phone"
        elif [ -n "${APP_REVIEW_PHONE_NUMBER:-}" ]; then
            validate_review_phone_value "$APP_REVIEW_PHONE_NUMBER" "App Review contact phone (APP_REVIEW_PHONE_NUMBER)"
        else
            warning "App Review contact phone: Missing; set APP_REVIEW_PHONE_NUMBER or add phone_number.txt before submission"
        fi

        local ios_screenshots_dir="$METADATA_DIR/../screenshots/ios"
        if [ -d "$ios_screenshots_dir" ]; then
            for locale in en-US fr-FR; do
                local locale_screenshots_dir="$ios_screenshots_dir/$locale"
                if [ -d "$locale_screenshots_dir" ]; then
                    local screenshot_count
                    screenshot_count=$(find "$locale_screenshots_dir" -type f \( -name "*.png" -o -name "*.jpg" -o -name "*.jpeg" \) | wc -l | tr -d ' ')
                    if [ "$screenshot_count" -gt 0 ]; then
                        pass "Fastlane screenshots ($locale): $screenshot_count images"
                    else
                        error "Fastlane screenshots ($locale): No images found"
                    fi
                    while IFS= read -r screenshot; do
                        validate_ios_screenshot_file "$screenshot" "Fastlane screenshots ($locale)"
                    done < <(find "$locale_screenshots_dir" -type f \( -name "*.png" -o -name "*.jpg" -o -name "*.jpeg" \))
                else
                    error "Fastlane screenshots ($locale): Missing directory"
                fi
            done
        else
            error "Fastlane screenshots: Missing at composeApp/screenshots/ios"
        fi
    fi
}

validate_app_store_final_signoff_record() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🧾 App Store Final Signoff${NC}"

    local signoff_file="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ ! -f "$signoff_file" ]; then
        error "Final App Store signoff: Missing at docs/APP_STORE_FINAL_SIGNOFF.md"
        return 1
    fi

    validate_text_file "$signoff_file" "Final App Store signoff record" 200 20000

    local signoff_baseline_phrases=(
        "## Apple Source Baseline"
        "Apple-source review date: 2026-05-28"
        "review process covers each submitted app version"
        "app versions for each platform are submitted separately"
        "status of an app version on one platform does not influence another platform"
        "one app version submission under review at a time"
        "maximum of two submissions under review"
        "items associated with different platforms cannot be added to the same submission"
        "required metadata must be provided and the build must be chosen"
        "Account Holder, Admin, or App Manager"
        "Add for Review changes the app status to Ready for Review"
        "Submit for Review is clicked"
        "added to an existing draft submission or to a new draft submission"
        "all items submitted together must be accepted to complete the submission"
        "submissions may not be reviewed in the order they are submitted"
        "App Review information on the latest approved app version"
        "final versions with all necessary metadata and fully functional URLs"
        "placeholder text, empty websites, and other temporary content"
        "tested on device for bugs and stability"
        "contact information must be current"
        "active demo account or a fully featured demo mode"
        "backend services must be live and accessible during review"
        "App Store status values distinguish preparation, ready-for-review intent, active review, binary problems, export-compliance waiting, and distribution readiness"
        "Ready for Review means required metadata is entered"
        "Waiting for Review means Apple received the submission"
        "screenshots and app previews cannot be uploaded or edited"
        "In Review means App Review is reviewing the submission"
        "Unresolved Issues"
        "all items approved before the submission is considered approved"
        "cannot have more items added"
        "Developer Rejected"
        "Ready for Distribution requires accepted review state plus agreements in effect"
    )
    local signoff_baseline_phrase
    for signoff_baseline_phrase in "${signoff_baseline_phrases[@]}"; do
        if grep -Fq "$signoff_baseline_phrase" "$signoff_file"; then
            pass "Final App Store signoff Apple baseline: Covers $signoff_baseline_phrase"
        else
            error "Final App Store signoff Apple baseline: Missing $signoff_baseline_phrase"
        fi
    done

    if grep -Fxq "APP_STORE_FINAL_SIGNOFF_COMPLETE=true" "$signoff_file"; then
        pass "Final App Store signoff: Complete marker is true"
    elif grep -Fxq "APP_STORE_FINAL_SIGNOFF_COMPLETE=false" "$signoff_file"; then
        warning "Final App Store signoff: Complete marker is false until final external evidence is recorded"
    else
        error "Final App Store signoff: Missing APP_STORE_FINAL_SIGNOFF_COMPLETE marker"
    fi

    local signoff_vars=(
        "APP_STORE_PRIVACY_SIGNOFF"
        "APP_STORE_ACCESSIBILITY_SIGNOFF"
        "APP_STORE_AVAILABILITY_CONFIRMED"
        "APP_STORE_DSA_TRADER_STATUS_CONFIRMED"
        "APP_STORE_PRICING_AVAILABILITY_CONFIRMED"
        "APP_STORE_SDK_PRIVACY_CONFIRMED"
        "APP_STORE_RELEASE_CONTROL_CONFIRMED"
        "APP_STORE_MEDIA_LOCALIZATION_CONFIRMED"
        "APP_STORE_LICENSE_NOTICES_CONFIRMED"
        "APP_STORE_EULA_CONFIRMED"
        "APP_STORE_ACCOUNT_DELETION_CONFIRMED"
        "APP_STORE_UGC_MODERATION_CONFIRMED"
        "APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED"
        "TESTFLIGHT_SMOKE_PASSED"
        "APP_STORE_CAPABILITIES_CONFIRMED"
    )

    local env_template="$PROJECT_DIR/.env.appstore.example"
    local fastfile="$PROJECT_DIR/fastlane/Fastfile"
    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    local signoff_var
    for signoff_var in "${signoff_vars[@]}"; do
        if grep -Fq "$signoff_var" "$signoff_file"; then
            pass "Final App Store signoff: Documents $signoff_var"
        else
            error "Final App Store signoff: Missing release variable $signoff_var"
        fi

        if [ -f "$env_template" ] && grep -Fq "$signoff_var=false" "$env_template"; then
            pass "App Store env template: Defines $signoff_var=false"
        else
            error "App Store env template: Missing $signoff_var=false"
        fi

        if [ -f "$fastfile" ] && grep -Fq "\"$signoff_var\"" "$fastfile"; then
            pass "Fastfile final signoff vars: Includes $signoff_var"
        else
            error "Fastfile final signoff vars: Missing $signoff_var"
        fi

        if [ -f "$audit_script" ] && grep -Fq "truthy_env \"$signoff_var\"" "$audit_script" && grep -Fq "require_truthy_env \"$signoff_var\"" "$audit_script"; then
            pass "Final audit signoff vars: Requires $signoff_var"
        else
            error "Final audit signoff vars: Missing $signoff_var"
        fi
    done

    if [ -f "$audit_script" ] &&
        grep -Fq "require_complete_evidence_record" "$audit_script" &&
        grep -Fq "unresolved TBD/Pending/status/checklist placeholders" "$audit_script" &&
        grep -Fq "APP_STORE_FINAL_SIGNOFF_COMPLETE=true" "$audit_script"; then
        pass "Final audit: Blocks complete evidence records with unresolved placeholders"
    else
        error "Final audit: Missing unresolved-placeholder guard for complete evidence records"
    fi

    if [ -f "$audit_script" ] &&
        grep -Fq 'The final audit cannot pass unless local preflight, live URL validation, and submission_ready are run.' "$audit_script" &&
        grep -Fq 'Local App Store preflight was skipped; rerun without --skip-preflight before submission' "$audit_script" &&
        grep -Fq '[ "$RUN_PREFLIGHT" = false ] && [ "$BLOCKERS" -eq 0 ]' "$audit_script"; then
        pass "Final audit: Cannot report ready when local preflight is skipped"
    else
        error "Final audit: Missing ready-state guard for skipped local preflight"
    fi

    local conditional_product_guard_patterns=(
        "require_account_deletion_implementation_if_confirmed"
        "require_account_deletion_evidence_if_confirmed"
        "require_ugc_moderation_implementation_if_confirmed"
        "require_ugc_moderation_evidence_if_confirmed"
        "require_openspec_change_valid \"add-in-app-account-deletion\""
        "require_openspec_change_valid \"add-ugc-moderation-controls\""
        "require_openspec_tasks_complete \"add-in-app-account-deletion\""
        "require_openspec_tasks_complete \"add-ugc-moderation-controls\""
        "APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true"
        "APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true"
        "Account deletion backend route or service is present"
        "Account deletion backend tests cover collaborative anonymization"
        "Account deletion stable route is DELETE /api/user/delete"
        "Account deletion response includes local cleanup and provider revocation status"
        "Account deletion collaborative anonymization is present"
        "Account deletion iOS Data Management screen is present"
        "Account deletion iOS authenticated and guest actions are present"
        "Account deletion iOS cleanup waits for backend success"
        "UGC moderation tests cover report/block/unblock/filter behavior"
        "UGC moderation server policy or report/block/unblock endpoints are present"
        "UGC moderation iOS report/block/unblock entry points are present"
        "UGC moderation iOS hidden/pending/rejected states are present"
    )
    local conditional_product_guard_pattern
    for conditional_product_guard_pattern in "${conditional_product_guard_patterns[@]}"; do
        if [ -f "$audit_script" ] && grep -Fq "$conditional_product_guard_pattern" "$audit_script"; then
            pass "Final audit product signoff guard: Covers $conditional_product_guard_pattern"
        else
            error "Final audit product signoff guard: Missing $conditional_product_guard_pattern"
        fi
    done

    local final_audit_placeholder_patterns=(
        "APPLE_ID uses the documented placeholder release@example.com"
        "ITC_TEAM_ID uses the documented placeholder 123456789"
        "TEAM_ID uses the documented placeholder ABCDE12345"
        "APPLE_TEAM_ID uses the documented placeholder ABCDE12345"
        "APP_REVIEW_PHONE_NUMBER uses the documented placeholder +15551234567"
    )
    local final_audit_placeholder_pattern
    for final_audit_placeholder_pattern in "${final_audit_placeholder_patterns[@]}"; do
        if [ -f "$audit_script" ] && grep -Fq "$final_audit_placeholder_pattern" "$audit_script"; then
            pass "Final audit placeholder guard: Covers $final_audit_placeholder_pattern"
        else
            error "Final audit placeholder guard: Missing $final_audit_placeholder_pattern"
        fi
    done

    if grep -Fq "## Blocker Evidence Matrix" "$signoff_file"; then
        pass "Final App Store signoff: Contains blocker evidence matrix"
    else
        error "Final App Store signoff: Missing blocker evidence matrix"
    fi

    local blocker_number blocker_id
    for blocker_number in $(seq 1 22); do
        blocker_id=$(printf "AS-%02d" "$blocker_number")
        if grep -Fq "| $blocker_id |" "$signoff_file"; then
            pass "Final App Store signoff: Evidence matrix covers $blocker_id"
        else
            error "Final App Store signoff: Evidence matrix missing $blocker_id"
        fi
    done

    local required_signoff_evidence=(
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
        "APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md"
        "APP_STORE_UGC_MODERATION_CONFIRMED=true"
        "APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_UGC_MODERATION_EVIDENCE.md"
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
        "APP_STORE_APP_INFORMATION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_APP_INFORMATION_EVIDENCE.md"
        "APP_STORE_VERSIONING_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_VERSIONING_EVIDENCE.md"
        "APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md"
        "APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md"
        "APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md"
        "App Store Connect final state is recorded"
        "platform submission scope is iOS only"
        "draft submission membership is understood"
        "all submitted items are accepted or intentionally excluded"
        "no Unresolved Issues state"
        "removal/cancel/retry decision is documented"
        "App Store Connect submission status"
        "Submitted items"
        "Draft submission ID or screenshot"
        "## Apple References"
        "overview-of-submitting-for-review"
        "submit-an-app"
        "app-and-submission-statuses"
        "manage-a-submission-with-unresolved-issues"
        "remove-a-submission-from-review"
        "./scripts/app-store-submission-audit.sh --check-live-urls"
        "./scripts/app-store-submission-audit.sh --run-submission-ready"
    )

    local evidence_phrase
    for evidence_phrase in "${required_signoff_evidence[@]}"; do
        if grep -Fq "$evidence_phrase" "$signoff_file"; then
            pass "Final App Store signoff: Evidence matrix covers $evidence_phrase"
        else
            error "Final App Store signoff: Evidence matrix missing '$evidence_phrase'"
        fi
    done
}

validate_app_store_privacy_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🔒 App Store Privacy Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_PRIVACY_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store privacy evidence: Missing at docs/APP_STORE_PRIVACY_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store privacy evidence" 1000 22000

    local required_phrases=(
        "APP_STORE_PRIVACY_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-27"
        "App Store privacy details are required to submit new apps and app updates"
        "privacy responses must include the privacy practices of third-party partners"
        "responsible for keeping privacy responses accurate and up to date"
        "defines collection as transmitting data off device"
        "longer than needed to service the request in real time"
        "data processed only on device is not collected"
        "derived data sent off device must be evaluated separately"
        "whether each collected data type is linked to their identity"
        "whether data is used to track them"
        "publicly accessible Privacy Policy URL"
        "## Build And Policy Scope"
        "## Required App Store Connect Answers"
        "## Evidence Commands"
        "## Local Privacy Alignment Scan Result"
        "## Closure Rule"
        "https://wakeve.app/privacy"
        "docs/APP_STORE_PRIVACY_LABELS.md"
        "docs/PRIVACY_POLICY.md"
        "iosApp/src/PrivacyInfo.xcprivacy"
        "NSPrivacyTracking=false"
        "no tracking domains"
        "no IDFA/App Tracking Transparency strings"
        "name, email address, user ID, device ID, other user content, coarse location, and product interaction"
        "Photos or Videos"
        "calendar operations write only"
        "Siri/speech"
        "analytics/crash providers"
        "Legal/privacy owner approval"
        "./scripts/lint-store-metadata.sh --ios-only"
        "./scripts/audit-app-store-privacy-alignment.sh --fail-on-findings"
        "APP_REVIEW_PHONE_NUMBER=<APP_REVIEW_PHONE_NUMBER> ./scripts/lint-store-metadata.sh --ios-only"
        'plutil -p iosApp/src/PrivacyInfo.xcprivacy'
        "/usr/bin/strings build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app/Wakeve"
        "Local scan date: 2026-06-13"
        "docs/app-store-privacy/privacy-alignment-2026-06-13T12-26-10Z.md"
        "PASS for local privacy alignment"
        "0 local findings and 4 external pending confirmations"
        "38dbda46a737beed9c54a65cf089159fbb2712de1c21b8c9cd5de6877acfbfc3"
        "6b8817f3013c36f1ef60b3d1d67d4aa8071aba02d36224cae6aa8e01438cd638"
        "8eb134c37318846c8c3ffbac075ee76d606204f44a17a1618e94b8c6f078b285"
        "Privacy manifest declares \`NSPrivacyTracking=false\` and no tracking domains"
        "iOS/shared source contains no IDFA or App Tracking Transparency API references"
        "External pending confirmations remain"
        "APP_STORE_PRIVACY_SIGNOFF=true"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store privacy evidence: Covers $phrase"
        else
            error "App Store privacy evidence: Missing '$phrase'"
        fi
    done

    local privacy_labels="$PROJECT_DIR/docs/APP_STORE_PRIVACY_LABELS.md"
    if [ -f "$privacy_labels" ] &&
        grep -Fq "docs/APP_STORE_PRIVACY_EVIDENCE.md" "$privacy_labels" &&
        grep -Fq "APP_STORE_PRIVACY_EVIDENCE_COMPLETE=true" "$privacy_labels"; then
        pass "Privacy labels draft: Requires privacy evidence before APP_STORE_PRIVACY_SIGNOFF"
    else
        error "Privacy labels draft: Does not require privacy evidence before APP_STORE_PRIVACY_SIGNOFF"
    fi

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_PRIVACY_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_PRIVACY_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires privacy evidence before APP_STORE_PRIVACY_SIGNOFF"
    else
        error "App Store launch checklist: Does not require privacy evidence before APP_STORE_PRIVACY_SIGNOFF"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_PRIVACY_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_PRIVACY_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires privacy evidence completion"
    else
        error "Final App Store signoff: Does not require privacy evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_PRIVACY_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_privacy_evidence_if_confirmed" "$audit_script"; then
        pass "Final audit: Requires privacy evidence when APP_STORE_PRIVACY_SIGNOFF is true"
    else
        error "Final audit: Does not require privacy evidence when APP_STORE_PRIVACY_SIGNOFF is true"
    fi
}

validate_app_store_env_template() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🔑 App Store Environment Template${NC}"

    local env_template="$PROJECT_DIR/.env.appstore.example"
    if [ ! -f "$env_template" ]; then
        error "App Store env template: Missing at .env.appstore.example"
        return 1
    fi

    validate_text_file "$env_template" "App Store env template" 400 6000

    local required_template_phrases=(
        "Do not commit real secrets"
        "Apple values below are placeholders intentionally rejected by final gates"
        "Replace them with real release secrets before submission"
        "APPLE_ID=release@example.com"
        "ITC_TEAM_ID=123456789"
        "TEAM_ID=ABCDE12345"
        "APPLE_TEAM_ID=ABCDE12345"
        "APP_REVIEW_PHONE_NUMBER=+15551234567"
        "This placeholder is intentionally rejected"
        "use a real reachable secret/env value before submission"
        "Must match TEAM_ID"
        "Prefer a secret/env value for this PII"
        "APP_STORE_FINAL_SIGNOFF_COMPLETE=true"
        "Evidence markers stay in the repository evidence documents"
        "docs/APP_STORE_PRIVACY_EVIDENCE.md with APP_STORE_PRIVACY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md with APP_STORE_ACCESSIBILITY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_AVAILABILITY_EVIDENCE.md with APP_STORE_AVAILABILITY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_DSA_TRADER_STATUS.md with APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md with APP_STORE_PRICING_AVAILABILITY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md with APP_STORE_SDK_PRIVACY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md with APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md with APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md with APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_EULA_EVIDENCE.md with APP_STORE_EULA_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md with APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_UGC_MODERATION_EVIDENCE.md with APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true"
        "openspec/changes/add-in-app-account-deletion/ implemented and tested"
        "openspec/changes/add-ugc-moderation-controls/ implemented and tested"
        "docs/APP_STORE_PAYMENT_EVIDENCE.md with APP_STORE_PAYMENT_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_TESTFLIGHT_EVIDENCE.md with TESTFLIGHT_SMOKE_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_OBSERVABILITY_EVIDENCE.md with APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md with APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_CAPABILITIES_EVIDENCE.md with APP_STORE_CAPABILITIES_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md"
        "APP_STORE_REVIEW_ACCESS_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md"
        "APP_STORE_ACCOUNT_ACCESS_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md"
        "APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_APP_INFORMATION_EVIDENCE.md"
        "APP_STORE_APP_INFORMATION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_VERSIONING_EVIDENCE.md"
        "APP_STORE_VERSIONING_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md"
        "APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md"
        "APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md"
        "APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_EULA_EVIDENCE.md"
        "APP_STORE_EULA_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md"
        "APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_UGC_MODERATION_EVIDENCE.md"
        "APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true"
    )

    local phrase
    for phrase in "${required_template_phrases[@]}"; do
        if grep -Fq "$phrase" "$env_template"; then
            pass "App Store env template: Covers $phrase"
        else
            error "App Store env template: Missing '$phrase'"
        fi
    done

    local required_false_flags=(
        "APP_STORE_PRIVACY_SIGNOFF=false"
        "APP_STORE_ACCESSIBILITY_SIGNOFF=false"
        "APP_STORE_AVAILABILITY_CONFIRMED=false"
        "APP_STORE_DSA_TRADER_STATUS_CONFIRMED=false"
        "APP_STORE_PRICING_AVAILABILITY_CONFIRMED=false"
        "APP_STORE_SDK_PRIVACY_CONFIRMED=false"
        "APP_STORE_RELEASE_CONTROL_CONFIRMED=false"
        "APP_STORE_MEDIA_LOCALIZATION_CONFIRMED=false"
        "APP_STORE_LICENSE_NOTICES_CONFIRMED=false"
        "APP_STORE_EULA_CONFIRMED=false"
        "APP_STORE_ACCOUNT_DELETION_CONFIRMED=false"
        "APP_STORE_UGC_MODERATION_CONFIRMED=false"
        "APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=false"
        "TESTFLIGHT_SMOKE_PASSED=false"
        "APP_STORE_CAPABILITIES_CONFIRMED=false"
    )

    for phrase in "${required_false_flags[@]}"; do
        if grep -Fq "$phrase" "$env_template"; then
            pass "App Store env template: Keeps $phrase"
        else
            error "App Store env template: Missing safe default '$phrase'"
        fi
    done

    local gitignore="$PROJECT_DIR/.gitignore"
    if [ ! -f "$gitignore" ]; then
        error "Git ignore: Missing .gitignore"
        return 0
    fi

    local required_gitignore_rules=(
        ".env"
        ".env.*"
        "!.env.example"
        "!.env.*.example"
    )

    for phrase in "${required_gitignore_rules[@]}"; do
        if grep -Fxq "$phrase" "$gitignore"; then
            pass "Git ignore: Covers $phrase"
        else
            error "Git ignore: Missing rule '$phrase'"
        fi
    done

    if git -C "$PROJECT_DIR" check-ignore -q ".env.appstore" >/dev/null 2>&1; then
        pass "Git ignore: .env.appstore is ignored"
    else
        error "Git ignore: .env.appstore is not ignored"
    fi

    if git -C "$PROJECT_DIR" check-ignore -q ".env.appstore.example" >/dev/null 2>&1; then
        error "Git ignore: .env.appstore.example must stay trackable"
    else
        pass "Git ignore: .env.appstore.example is trackable"
    fi
}

validate_app_store_readiness_report() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}📋 App Store Readiness Report${NC}"

    local readiness_file="$PROJECT_DIR/docs/APP_STORE_READINESS.md"
    if [ ! -f "$readiness_file" ]; then
        error "App Store readiness report: Missing at docs/APP_STORE_READINESS.md"
        return 1
    fi

    validate_text_file "$readiness_file" "App Store readiness report" 2000 140000

    local required_baseline_phrases=(
        "## Apple Source Baseline"
        "Apple-source review date: 2026-05-28"
        "current SDK requirements"
        "Xcode 26 or later"
        "iOS and iPadOS 26 SDK or later"
        "builds must be uploaded and processed"
        "required metadata must be provided and the build must be chosen"
        "Account Holder, Admin, or App Manager"
        "Add for Review changes the app status to Ready for Review"
        "Submit for Review is clicked"
        "final versions with all necessary metadata and fully functional URLs"
        "placeholder text, empty websites, and temporary content"
        "tested on device for crashes, bugs, and stability"
        "active demo account or fully featured demo mode"
        "backend services must be live and accessible during review"
        "filtering, reporting, blocking, timely moderation responses, and published contact information"
        "initiate account deletion from inside the app"
        "App Privacy answers are required"
        "third-party partner data practices"
        "in-app purchase is required for digital content/features"
        "physical goods or services consumed outside the app"
        "app availability must be configured before App Review submission"
        "EU Digital Services Act trader status information"
        "Prepare for Submission, Ready for Review, In Review, Binary Rejected, Waiting for Export Compliance, Pending Developer Release, and Ready for Distribution"
    )

    local baseline_phrase
    for baseline_phrase in "${required_baseline_phrases[@]}"; do
        if grep -Fq "$baseline_phrase" "$readiness_file"; then
            pass "App Store readiness report: Baseline covers $baseline_phrase"
        else
            error "App Store readiness report: Missing baseline phrase '$baseline_phrase'"
        fi
    done

    local required_status_phrases=(
        "Wakeve is not ready for App Store submission yet."
        "A signed archive/upload has not been produced"
        "live URL/AASA evidence"
        "final signoff record"
        "Result: local App Store preflight ran and passed"
        "21 blockers"
        "Result: NOT READY for App Store submission"
        "APP_REVIEW_PHONE_NUMBER='+33123456789' ./scripts/app-store-submission-audit.sh"
    )

    local status_phrase
    for status_phrase in "${required_status_phrases[@]}"; do
        if grep -Fq "$status_phrase" "$readiness_file"; then
            pass "App Store readiness report: Status covers $status_phrase"
        else
            error "App Store readiness report: Missing status phrase '$status_phrase'"
        fi
    done
}

validate_app_store_submission_runbook() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🚀 App Store Submission Runbook${NC}"

    local runbook_file="$PROJECT_DIR/docs/APP_STORE_SUBMISSION_RUNBOOK.md"
    if [ ! -f "$runbook_file" ]; then
        error "App Store submission runbook: Missing at docs/APP_STORE_SUBMISSION_RUNBOOK.md"
        return 1
    fi

    validate_text_file "$runbook_file" "App Store submission runbook" 3000 30000

    local required_sections=(
        "## Apple Source Baseline"
        "## Current Gate"
        "## Required External Values"
        "## Apple Developer Setup"
        "## Production Web/API Setup"
        "## App Store Connect Setup"
        "## Command Sequence"
        "## Expected Failures Until Production Is Ready"
        "## Apple References"
    )

    local section
    for section in "${required_sections[@]}"; do
        if grep -Fq "$section" "$runbook_file"; then
            pass "App Store submission runbook: Contains $section"
        else
            error "App Store submission runbook: Missing $section"
        fi
    done

    local required_baseline_phrases=(
        "Apple-source review date: 2026-05-28"
        "builds can be uploaded after the app is added to the account"
        "Xcode, Swift Playground, altool, or Transporter"
        "build must be processed by Apple before it appears in App Store Connect"
        "bundle ID and version number associate an uploaded build"
        "required metadata must be provided and the build must be chosen"
        "Account Holder, Admin, or App Manager"
        "Build section should be verified before Add for Review"
        "Add for Review changes the app status to Ready for Review"
        "Submit for Review is clicked"
        "app status changes to In Review"
        "submissions may not be reviewed in the order they are submitted"
        "App Review information should provide additional context"
        "app versions for each platform are submitted separately"
        "final versions with all necessary metadata and fully functional URLs"
        "placeholder text, empty websites, and temporary content"
        "tested on device for crashes, bugs, and stability"
        "App Review contact information must be current"
        "active demo account or fully featured demo mode"
        "backend services must be live and accessible during review"
    )

    local baseline_phrase
    for baseline_phrase in "${required_baseline_phrases[@]}"; do
        if grep -Fq "$baseline_phrase" "$runbook_file"; then
            pass "App Store submission runbook: Baseline covers $baseline_phrase"
        else
            error "App Store submission runbook: Missing baseline phrase '$baseline_phrase'"
        fi
    done

    local required_current_gate_items=(
        "Apple account and release environment values"
        "App Store Connect team ID"
        "App Store Connect role/agreement evidence"
        "Apple Developer signing team"
        "App Review contact phone"
        "Apple Developer capabilities/profiles"
        'live `wakeve.app` and `api.wakeve.app` deployment'
        "production Apple Team ID in both AASA endpoints"
        "privacy label/legal approval"
        "accessibility evidence"
        "App Store availability evidence"
        "EU DSA trader status evidence"
        "pricing and availability evidence"
        "third-party SDK privacy evidence"
        "App Store release control evidence"
        "App Store media/localization evidence"
        "license notices evidence"
        "App Store EULA evidence"
        "review access evidence"
        "export compliance evidence"
        "content rights/IP evidence"
        "App Store versioning evidence"
        "signed release artifact evidence"
        "App Store observability evidence"
        "account deletion readiness"
        "user-generated content moderation readiness"
        "docs/APP_STORE_PRODUCT_BLOCKER_APPROVAL.md"
        "Do not set `APP_STORE_ACCOUNT_DELETION_CONFIRMED=true`"
        "Do not set `APP_STORE_UGC_MODERATION_CONFIRMED=true`"
        "payment/external purchase compliance"
        "TestFlight smoke testing"
        "signed final submission-ready gate output"
        "final signoff"
    )

    local item
    for item in "${required_current_gate_items[@]}"; do
        if grep -Fq "$item" "$runbook_file"; then
            pass "App Store submission runbook: Current gate covers $item"
        else
            error "App Store submission runbook: Current gate missing $item"
        fi
    done

    local required_commands=(
        "bundle exec fastlane ios preflight"
        "bundle exec fastlane ios submission_ready"
        "using the same release environment and manual signoff variables as the upload lane"
        "APP_STORE_PRIVACY_SIGNOFF=true \\"
        "APP_STORE_ACCESSIBILITY_SIGNOFF=true \\"
        "APP_STORE_AVAILABILITY_CONFIRMED=true \\"
        "APP_STORE_DSA_TRADER_STATUS_CONFIRMED=true \\"
        "APP_STORE_PRICING_AVAILABILITY_CONFIRMED=true \\"
        "APP_STORE_SDK_PRIVACY_CONFIRMED=true \\"
        "APP_STORE_RELEASE_CONTROL_CONFIRMED=true \\"
        "APP_STORE_MEDIA_LOCALIZATION_CONFIRMED=true \\"
        "APP_STORE_LICENSE_NOTICES_CONFIRMED=true \\"
        "APP_STORE_EULA_CONFIRMED=true \\"
        "APP_STORE_ACCOUNT_DELETION_CONFIRMED=true \\"
        "APP_STORE_UGC_MODERATION_CONFIRMED=true \\"
        "APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=true \\"
        "TESTFLIGHT_SMOKE_PASSED=true \\"
        "APP_STORE_CAPABILITIES_CONFIRMED=true \\"
        'APPLE_ID="$APPLE_ID" \'
        'ITC_TEAM_ID="$ITC_TEAM_ID" \'
        'TEAM_ID="$TEAM_ID" \'
        'APPLE_TEAM_ID="$APPLE_TEAM_ID" \'
        'APP_REVIEW_PHONE_NUMBER="$APP_REVIEW_PHONE_NUMBER" \'
        "./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready"
        "bundle exec fastlane ios validate_ipa_entitlements ipa:build/ios/WakeveApp.ipa"
        "bundle exec fastlane ios upload_testflight"
        "bundle exec fastlane ios upload_appstore"
        "APP_REVIEW_PHONE_NUMBER"
        "APP_STORE_FINAL_SIGNOFF_COMPLETE=true"
        "submit_for_review: false"
        'cannot report a ready result if `--skip-preflight` is used'
        "local preflight plus both final gates"
    )

    for item in "${required_commands[@]}"; do
        if grep -Fq "$item" "$runbook_file"; then
            pass "App Store submission runbook: Command sequence covers $item"
        else
            error "App Store submission runbook: Command sequence missing $item"
        fi
    done
}

validate_app_store_connect_field_map() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🗺️  App Store Connect Field Map${NC}"

    local field_map_file="$PROJECT_DIR/docs/APP_STORE_CONNECT_FIELD_MAP.md"
    if [ ! -f "$field_map_file" ]; then
        error "App Store Connect field map: Missing at docs/APP_STORE_CONNECT_FIELD_MAP.md"
        return 1
    fi

    validate_text_file "$field_map_file" "App Store Connect field map" 1000 16000

    local required_sections=(
        "## Apple Source Baseline"
        "## App Information"
        "## Version Information"
        "## App Review Information"
        "## Privacy And Compliance"
        "## Capabilities And Availability"
        "## Final Review Checks"
    )

    local section
    for section in "${required_sections[@]}"; do
        if grep -Fq "$section" "$field_map_file"; then
            pass "App Store Connect field map: Contains $section"
        else
            error "App Store Connect field map: Missing $section"
        fi
    done

    local required_baseline_phrases=(
        "Apple-source review date: 2026-05-27"
        "App Information properties are shared across platforms"
        "Name, Subtitle, Privacy Policy URL, Bundle ID, SKU, Age Rating, License Agreement, Primary Language, Category, and Content Rights"
        "Name must be at least two characters and no more than 30 characters"
        "Subtitle cannot be longer than 30 characters"
        "Privacy Policy URL is required for iOS and macOS apps"
        "Privacy Policy URL for all apps"
        "Bundle ID must match the Xcode project Bundle ID"
        "cannot be changed after a build is uploaded"
        "SKU is an internal tracking ID"
        "cannot be changed after the app is added to the account"
        "Age Rating is required and is set at the app level"
        "Apple provides a standard EULA"
        "custom license agreement for one or more regions"
        "Primary Language is the default metadata language"
        "primary category should match the category set in Xcode for macOS apps"
        "privacy answers are app-level"
        "include third-party partner practices"
        "screenshots and app previews are managed on the platform section"
        "required metadata must be provided"
        "right build must be selected"
        "Account Holder, Admin, or App Manager"
        "Add for Review moves the app status to Ready for Review"
        "Submit for Review is clicked"
        "App Review information should provide additional information or context"
        "app versions for each platform are submitted separately"
    )

    local baseline_phrase
    for baseline_phrase in "${required_baseline_phrases[@]}"; do
        if grep -Fq "$baseline_phrase" "$field_map_file"; then
            pass "App Store Connect field map: Baseline covers $baseline_phrase"
        else
            error "App Store Connect field map: Missing baseline phrase '$baseline_phrase'"
        fi
    done

    local required_fields=(
        "Name"
        "Subtitle"
        "Bundle ID"
        "SKU"
        "Primary language"
        "Category"
        "Age rating"
        "Copyright"
        "Description"
        "Keywords"
        "Promotional text"
        "What's New"
        "Version and build number"
        "Account access evidence"
        "Release artifact evidence"
        "Content rights evidence"
        "Pricing and availability evidence"
        "Live URL/AASA evidence"
        "Support URL"
        "Privacy Policy URL"
        "Screenshots"
        "Contact first name"
        "Contact last name"
        "Contact email"
        "Contact phone"
        "Demo account"
        "Privacy labels"
        "Privacy manifest"
        "Export compliance"
        "Account deletion"
        "Accessibility Nutrition Labels"
        "EU DSA trader status"
        "Push Notifications"
        "Siri"
        "Sign in with Apple"
        "Associated Domains"
        "Universal Links AASA"
        "Mac Apple silicon availability"
        "Apple Vision Pro availability"
        "EU storefront availability"
    )

    local field
    for field in "${required_fields[@]}"; do
        if grep -Fq "$field" "$field_map_file"; then
            pass "App Store Connect field map: Covers $field"
        else
            error "App Store Connect field map: Missing $field"
        fi
    done

    local required_sources=(
        "composeApp/metadata/ios/<locale>/name.txt"
        "composeApp/metadata/ios/<locale>/subtitle.txt"
        "composeApp/metadata/ios/<locale>/description.txt"
        "composeApp/metadata/ios/<locale>/keywords.txt"
        "composeApp/metadata/ios/<locale>/promotional_text.txt"
        "composeApp/metadata/ios/<locale>/release_notes.txt"
        "composeApp/metadata/ios/<locale>/support_url.txt"
        "composeApp/metadata/ios/<locale>/privacy_url.txt"
        "composeApp/screenshots/ios/<locale>/"
        "composeApp/metadata/ios/review_information/first_name.txt"
        "composeApp/metadata/ios/review_information/last_name.txt"
        "composeApp/metadata/ios/review_information/email_address.txt"
        "APP_REVIEW_PHONE_NUMBER"
        "docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md"
        "docs/APP_STORE_APP_INFORMATION_EVIDENCE.md"
        "docs/APP_STORE_VERSIONING_EVIDENCE.md"
        "docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md"
        "docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md"
        "docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md"
        "APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md"
        "docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md"
        "docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md"
        "docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md"
        "docs/APP_STORE_PRIVACY_LABELS.md"
        "docs/APP_STORE_ACCESSIBILITY_LABELS.md"
        "docs/APP_STORE_DSA_TRADER_STATUS.md"
        "docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md"
        "docs/APP_STORE_FINAL_SIGNOFF.md"
        "iosApp/src/PrivacyInfo.xcprivacy"
        "iosApp/src/Wakeve.entitlements"
        "https://wakeve.app/.well-known/apple-app-site-association"
        "https://wakeve.app/apple-app-site-association"
        "1. Upload to internal TestFlight"
        "2. Complete TestFlight smoke testing"
        "3. Set the release signoff variables"
        "4. Run the final aggregated non-uploading audit"
        "./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready"
        "5. Upload the App Store build"
        "bundle exec fastlane ios upload_appstore"
        "submit_for_review"
    )

    local source
    for source in "${required_sources[@]}"; do
        if grep -Fq "$source" "$field_map_file"; then
            pass "App Store Connect field map: References $source"
        else
            error "App Store Connect field map: Missing reference to $source"
        fi
    done

    local ordered_final_review_steps=(
        "1. Upload to internal TestFlight"
        "2. Complete TestFlight smoke testing"
        "3. Set the release signoff variables"
        "4. Run the final aggregated non-uploading audit"
        "5. Upload the App Store build"
    )

    local previous_line=0
    local order_is_valid=true
    local step line
    for step in "${ordered_final_review_steps[@]}"; do
        line=$(grep -nF "$step" "$field_map_file" | head -n 1 | cut -d: -f1 || true)
        if [ -z "$line" ]; then
            error "App Store Connect field map: Ordered final review step missing '$step'"
            order_is_valid=false
            continue
        fi

        if [ "$line" -le "$previous_line" ]; then
            error "App Store Connect field map: Final review step '$step' is out of order"
            order_is_valid=false
        fi

        previous_line="$line"
    done

    if [ "$order_is_valid" = true ]; then
        pass "App Store Connect field map: Final review sequence is ordered"
    fi
}

validate_app_store_account_access_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🔐 App Store Account Access Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store account access evidence: Missing at docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store account access evidence" 1000 18000

    local required_phrases=(
        "APP_STORE_ACCOUNT_ACCESS_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-27"
        "App Store Connect is used to submit apps for distribution"
        "manage apps, distribute beta versions with TestFlight"
        "accept legal agreements, enter tax and banking information"
        "user roles determine access to App Store Connect and Apple Developer website sections"
        "Account Holder signs legal agreements"
        "only user who can sign legal agreements"
        "two-step verification or two-factor authentication must be enabled"
        "app submission requires required metadata, choosing the build for the version"
        "choosing a build to submit requires Account Holder, Admin, or App Manager access"
        "uploading builds requires Account Holder, Admin, App Manager, or Developer access"
        "associated with the app and version record using the bundle ID and version number"
        "build string uniquely identifies the build"
        "adding or editing users requires Account Holder, Admin, or App Manager access"
        "changing user roles requires Account Holder or Admin access"
        "free apps can be distributed on the App Store under the Apple Developer Program License Agreement"
        "selling apps or offering In-App Purchases requires the Account Holder to sign the Paid Apps Agreement"
        "## Required Apple Account Evidence"
        "## Local Gate Coverage"
        "## Apple References"
        "## Evidence To Attach"
        "## Closure Rule"
        "APPLE_ID"
        "ITC_TEAM_ID"
        "APPLE_TEAM_ID"
        "release@example.com"
        "123456789"
        "ABCDE12345"
        "submission_ready"
        "upload_testflight"
        "upload_appstore"
        "Account Holder, Admin, or App Manager"
        "Account Holder, Admin, App Manager, or Developer"
        "Two-factor authentication"
        "Bundle ID"
        "com.guyghost.wakeve"
        "Agreements"
        "Paid Apps Agreement"
        "App Store Connect team"
        "submit-an-app"
        "role-permissions"
        "add-a-new-app"
        "upload-builds"
        "sign-and-update-agreements"
        "docs/APP_STORE_FINAL_SIGNOFF.md"
        "APP_STORE_ACCOUNT_ACCESS_EVIDENCE_COMPLETE=true"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store account access evidence: Covers $phrase"
        else
            error "App Store account access evidence: Missing '$phrase'"
        fi
    done

    local env_template="$PROJECT_DIR/.env.appstore.example"
    if [ -f "$env_template" ] &&
        grep -Fq "APPLE_ID=release@example.com" "$env_template" &&
        grep -Fq "ITC_TEAM_ID=123456789" "$env_template" &&
        grep -Fq "TEAM_ID=ABCDE12345" "$env_template" &&
        grep -Fq "APPLE_TEAM_ID=ABCDE12345" "$env_template"; then
        pass "App Store account access evidence: Environment template documents rejected Apple placeholders"
    else
        error "App Store account access evidence: Environment template does not document rejected Apple placeholders"
    fi

    local fastfile="$PROJECT_DIR/fastlane/Fastfile"
    if [ -f "$fastfile" ] &&
        grep -Fq 'ensure_env_vars(["APPLE_ID", "ITC_TEAM_ID", "TEAM_ID", "APPLE_TEAM_ID"], "App Store submission readiness")' "$fastfile" &&
        grep -Fq 'ensure_env_vars(["APPLE_ID", "ITC_TEAM_ID", "TEAM_ID", "APPLE_TEAM_ID"], "App Store Connect upload")' "$fastfile" &&
        grep -Fq 'ensure_env_vars(["APPLE_ID", "ITC_TEAM_ID", "TEAM_ID"], "TestFlight upload")' "$fastfile" &&
        grep -Fq "APPLE_ID uses the documented placeholder release@example.com" "$fastfile" &&
        grep -Fq "ITC_TEAM_ID uses the documented placeholder 123456789" "$fastfile" &&
        grep -Fq "#{name} uses the documented placeholder ABCDE12345" "$fastfile"; then
        pass "App Store account access evidence: Fastlane requires Apple release variables and rejects placeholders"
    else
        error "App Store account access evidence: Fastlane Apple release variable or placeholder gates are incomplete"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq "APPLE_ID uses the documented placeholder release@example.com" "$audit_script" &&
        grep -Fq "ITC_TEAM_ID uses the documented placeholder 123456789" "$audit_script" &&
        grep -Fq "TEAM_ID uses the documented placeholder ABCDE12345" "$audit_script" &&
        grep -Fq "APPLE_TEAM_ID uses the documented placeholder ABCDE12345" "$audit_script" &&
        grep -Fq "TEAM_ID and APPLE_TEAM_ID must match" "$audit_script"; then
        pass "App Store account access evidence: Final audit rejects Apple placeholders and Team ID mismatches"
    else
        error "App Store account access evidence: Final audit Apple placeholder or Team ID mismatch gates are incomplete"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_ACCOUNT_ACCESS_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires account access evidence completion"
    else
        error "Final App Store signoff: Does not require account access evidence completion"
    fi

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_ACCOUNT_ACCESS_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires account access evidence before manual submission"
    else
        error "App Store launch checklist: Does not require account access evidence before manual submission"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_account_access_evidence_for_final_release" "$audit_script"; then
        pass "Final audit: Requires account access evidence for final release"
    else
        error "Final audit: Does not require account access evidence for final release"
    fi
}

validate_app_store_app_information_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🧾 App Store App Information Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_APP_INFORMATION_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store app information evidence: Missing at docs/APP_STORE_APP_INFORMATION_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store app information evidence" 1000 16000

    local required_phrases=(
        "APP_STORE_APP_INFORMATION_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-28"
        "app record must be created before uploading a build"
        "latest agreement in the Business section must be signed"
        "required role for adding a new app is Account Holder, App Manager, or Admin"
        "adding a new app record includes app name, primary language, bundle ID, SKU"
        "whether to limit or give full user access"
        "app name must be at least two characters and no more than 30 characters"
        "subtitle cannot be longer than 30 characters"
        "Privacy Policy URL is required for iOS and macOS apps"
        "Bundle ID is a unique identifier for the app"
        "Bundle ID must match the bundle ID set in the Xcode project"
        "cannot be changed after uploading a build"
        "Apple ID is automatically generated for the app"
        "SKU is a unique ID for the app that is not visible on the App Store"
        "SKU can contain letters, numbers, hyphens, periods, and underscores"
        "it cannot start with a hyphen, period, or underscore"
        "it cannot be changed after the app is added to the account"
        "Primary Language is the default language for product-page metadata"
        "displayed when localized metadata is not provided"
        "localized metadata display can vary based on the App Store country or region"
        "primary category set in App Store Connect should match the category set in Xcode"
        "age rating is a required app information property"
        "age rating is determined by answering the age-rating questionnaire"
        "translates age-rating questionnaire answers into an Apple global age rating"
        "additional region-specific ratings when required"
        "Made for Kids age-category selection cannot be changed after App Review approval"
        "Unrated apps cannot be published on the App Store"
        "## Required App Information Values"
        "Bundle ID"
        "com.guyghost.wakeve"
        "App name"
        "SKU"
        "Primary language"
        "User access"
        "Category"
        "Age rating"
        "Privacy Policy URL"
        "Subtitle"
        "docs/APP_STORE_AGE_RATING.md"
        "composeApp/metadata/ios/app_rating_config.json"
        "## Local App Information Scan Result"
        "repository metadata and local bundle identifiers are internally aligned"
        "Fastlane Appfile iOS app identifier: \`com.guyghost.wakeve\`"
        'Xcode project app target uses `PRODUCT_BUNDLE_IDENTIFIER = com.guyghost.wakeve`, `PRODUCT_NAME = Wakeve`, `MARKETING_VERSION = 1.0`, `CURRENT_PROJECT_VERSION = 1`, and `INFOPLIST_KEY_LSApplicationCategoryType = public.app-category.productivity`'
        'Source Info.plist declares `CFBundleDisplayName` as `Wakeve`, `CFBundleIdentifier` as `com.guyghost.wakeve`, `CFBundleShortVersionString` as `1.0`, and `CFBundleVersion` as `1`'
        'iOS metadata locales present: `en-US` and `fr-FR`'
        'Localized app names are 2-30 characters'
        'Localized subtitles are 30 characters or fewer'
        'Localized privacy and support URLs point to `https://wakeve.app/privacy` and `https://wakeve.app/support`'
        '`composeApp/metadata/ios/app_rating_config.json` parses successfully'
        '`bundle exec fastlane ios validate_metadata` passed locally'
        "local pre-submission evidence only"
        "immutable SKU, primary language, user access choice, category, generated age rating, privacy policy URL"
        "## Apple Field References"
        "App Store Connect app record URL or screenshot reference"
        "User access choice"
        "Privacy Policy URL"
        "Generated age rating"
        "APP_STORE_APP_INFORMATION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_FINAL_SIGNOFF.md"
        "add-a-new-app"
        "app-store-localizations"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store app information evidence: Covers $phrase"
        else
            error "App Store app information evidence: Missing '$phrase'"
        fi
    done

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_APP_INFORMATION_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_APP_INFORMATION_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires App Information evidence completion"
    else
        error "Final App Store signoff: Does not require App Information evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_APP_INFORMATION_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_app_information_evidence_for_final_release" "$audit_script"; then
        pass "Final audit: Requires App Information evidence for final release"
    else
        error "Final audit: Does not require App Information evidence for final release"
    fi
}

validate_app_store_versioning_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🔢 App Store Versioning Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_VERSIONING_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store versioning evidence: Missing at docs/APP_STORE_VERSIONING_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store versioning evidence" 1000 18000

    local required_phrases=(
        "APP_STORE_VERSIONING_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-28"
        "required role to upload builds is Account Holder, Admin, App Manager, or Developer"
        "build can be uploaded with Xcode, Swift Playground, altool, or Transporter"
        "build must be processed in Apple's system before it appears in App Store Connect"
        "App Store Connect sends an email after build processing is complete"
        "uses the app bundle's bundle ID and version number to associate the build with the app and version record"
        "build string uniquely identifies the build throughout the system"
        "failed build upload can reuse the same build number"
        "new App Store version requires an incremental App Store version number"
        "build string should be incremented in Xcode before uploading the build to App Store Connect"
        "new build should be added to the latest version before submitting the app to App Review"
        "required role to choose a build for submission is Account Holder, Admin, or App Manager"
        "required metadata must be provided and the build for the version must be chosen"
        "Build section should be checked to verify that the right build was added for the version"
        "only one build can be associated with each app version"
        "selected build can be changed until the version is submitted to App Review"
        "selected build's app icon, version number, build string, and upload date"
        "Missing Compliance status"
        "export compliance questions or encryption documentation must be completed"
        "uploaded builds can be viewed by version number in App Store Connect"
        "build's version, build number, upload status, and creation date"
        "## Current Repository Values"
        "MARKETING_VERSION"
        "CURRENT_PROJECT_VERSION"
        "CFBundleVersion"
        "## Required App Store Connect Comparison"
        "## Local Versioning Scan Result"
        "Latest uploaded build for that version"
        "Candidate build number"
        "Duplicate-build decision"
        "incrementing \`CURRENT_PROJECT_VERSION\` / \`CFBundleVersion\`"
        "Build processing status"
        "Uploaded review build"
        "Missing Compliance status"
        "latest_testflight_build_number"
        "app_store_build_number"
        "com.guyghost.wakeve"
        'Xcode project Release and Debug build settings use `MARKETING_VERSION = 1.0` and `CURRENT_PROJECT_VERSION = 1`'
        'Built local Release app has `CFBundleShortVersionString => "1.0"` and `CFBundleVersion => "1"`'
        'Source Info.plist currently resolves `CFBundleIdentifier => "com.guyghost.wakeve"`, `CFBundleDisplayName => "Wakeve"`, `CFBundleShortVersionString => "1.0"`, and `CFBundleVersion => "1"`'
        "no App Store Connect build lookup or selected review-build proof has been captured"
        "local pre-submission evidence only"
        'latest App Store Connect build for version `1.0`'
        "build processing has completed"
        "## Apple References"
        "upload-builds"
        "choose-a-build-to-submit"
        "view-builds-and-metadata"
        "manage-submissions-to-app-review/submit-an-app"
        "build-upload-statuses"
        "APP_STORE_VERSIONING_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_FINAL_SIGNOFF.md"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store versioning evidence: Covers $phrase"
        else
            error "App Store versioning evidence: Missing '$phrase'"
        fi
    done

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_VERSIONING_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_VERSIONING_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires versioning evidence completion"
    else
        error "Final App Store signoff: Does not require versioning evidence completion"
    fi

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_VERSIONING_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_VERSIONING_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires versioning evidence before manual submission"
    else
        error "App Store launch checklist: Does not require versioning evidence before manual submission"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_VERSIONING_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_versioning_evidence_for_final_release" "$audit_script"; then
        pass "Final audit: Requires versioning evidence for final release"
    else
        error "Final audit: Does not require versioning evidence for final release"
    fi
}

validate_app_store_release_artifact_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}📦 App Store Release Artifact Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store release artifact evidence: Missing at docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store release artifact evidence" 1000 18000

    local required_phrases=(
        "APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-28"
        "app record must be created before a build can be uploaded to App Store Connect"
        "required role to upload builds is Account Holder, Admin, App Manager, or Developer"
        "apps can be uploaded using Xcode, Swift Playground, altool, or Transporter"
        "requires Xcode 14 or later for upload"
        "build delivery progress, warnings, errors, and delivery logs"
        "build must be processed in Apple's system before it appears in App Store Connect"
        "email is sent when build processing is complete"
        "associated with the app and version record using the bundle ID and version number"
        "distributed for testing or submitted for review after they are available in App Store Connect"
        "build string uniquely identifies the build throughout the system"
        "uploaded builds can be viewed by version number"
        "build metadata can be inspected from the TestFlight tab"
        "build metadata includes compressed file size, essential content size, additional in-app content size"
        "build upload status shows version, build number, upload status, and creation date"
        "uploaded builds may go through app thinning"
        "final App Store DRM/recompression can change the final customer-facing size"
        "each Mach-O executable must stay within maximum build file size limits"
        "Xcode archives gather app binaries and dSYM files for distribution"
        "can include symbol files when uploading the app to App Store Connect"
        "dSYM files are needed to view symbolicated logs and crash reports"
        "dSYM files generated by App Store Connect, when available, can be downloaded"
        "dSYM downloads are no longer available for submissions from Xcode 14 or later"
        "requires a team that belongs to the Apple Developer Program"
        "apps should be tested thoroughly before distribution"
        "## Build Under Review"
        "## Required Artifact Record"
        "## Local Unsigned Release Artifact Scan Result"
        "## Evidence Commands"
        "## Cross-Checks"
        "## Closure Rule"
        "Signed IPA"
        "SHA-256"
        "Xcode archive"
        ".xcarchive"
        "dSYM"
        "dwarfdump --uuid"
        "Mach-O size check"
        "Symbol retention"
        "Fastlane logs"
        "submit_for_review: false"
        "automatic_release: false"
        "delivery warnings/errors"
        "Transporter/App Store Connect delivery identifiers"
        "App Store Connect build"
        "build string, upload status, creation date, processing status"
        "Build Metadata screenshot or API/export reference"
        "CODE_SIGNING_ALLOWED=NO"
        "build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app"
        "build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app.dSYM"
        "Local scan date:"
        "App executable SHA-256"
        "Shared framework executable SHA-256"
        'Built `Info.plist` SHA-256'
        'Built `PrivacyInfo.xcprivacy` SHA-256'
        "App executable UUID"
        "dSYM UUID"
        'Built Bundle ID: `com.guyghost.wakeve`'
        'Built marketing version: `1.0`'
        'Built build number: `1`'
        "ITSAppUsesNonExemptEncryption=false"
        "Local signed IPA: none found"
        'Local `.xcarchive`: none found'
        'Repository build scan found no `.ipa`, `.xcarchive`, or `ExportOptions.plist` under `build/` at max depth 4'
        "code object is not signed at all"
        "signed artifact evidence must not reuse the unsigned local \`CODE_SIGNING_ALLOWED=NO\` bundle as a submission artifact"
        'Local artifact hash cross-check is enforced by `./scripts/lint-store-metadata.sh --ios-only`'
        "This does not satisfy App Store release artifact completion"
        "docs/APP_STORE_VERSIONING_EVIDENCE.md"
        "docs/APP_STORE_TESTFLIGHT_EVIDENCE.md"
        "docs/APP_STORE_OBSERVABILITY_EVIDENCE.md"
        "docs/APP_STORE_CAPABILITIES_EVIDENCE.md"
        "docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md"
        "docs/APP_STORE_FINAL_SIGNOFF.md"
        "codesign -dv --verbose=4"
        "ExportOptions.plist"
        "Upload delivery warnings/errors are reviewed"
        "processing is complete before the build is selected"
        "Mach-O executable sizes remain within Apple maximum build file size limits"
        "## Apple References"
        "maximum-build-file-sizes"
        "distributing-your-app-for-beta-testing-and-releases"
        "APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=true"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store release artifact evidence: Covers $phrase"
        else
            error "App Store release artifact evidence: Missing '$phrase'"
        fi
    done

    local release_app="$PROJECT_DIR/build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app"
    local release_dsym="$PROJECT_DIR/build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app.dSYM"
    if [ -d "$release_app" ] && [ -d "$release_dsym" ]; then
        pass "App Store release artifact evidence: Local unsigned Release app and dSYM exist"

        local app_uuid dsym_uuid
        app_uuid=$(dwarfdump --uuid "$release_app/Wakeve" 2>/dev/null | awk '{print $2}' | head -n 1)
        dsym_uuid=$(dwarfdump --uuid "$release_dsym" 2>/dev/null | awk '{print $2}' | head -n 1)
        if [ -n "$app_uuid" ] && [ "$app_uuid" = "$dsym_uuid" ]; then
            pass "App Store release artifact evidence: Local app and dSYM UUIDs match"
        else
            error "App Store release artifact evidence: Local app and dSYM UUIDs do not match"
        fi

        if [ -n "$app_uuid" ] && grep -Fq "$app_uuid" "$evidence_file"; then
            pass "App Store release artifact evidence: Local app UUID is recorded"
        else
            error "App Store release artifact evidence: Local app UUID is not recorded"
        fi

        if command -v shasum >/dev/null 2>&1; then
            local artifact_entries=(
                "App executable SHA-256|$release_app/Wakeve"
                "Shared framework executable SHA-256|$release_app/Frameworks/Shared.framework/Shared"
                'Built `Info.plist` SHA-256|'"$release_app/Info.plist"
                'Built `PrivacyInfo.xcprivacy` SHA-256|'"$release_app/PrivacyInfo.xcprivacy"
            )
            local artifact_entry artifact_label artifact_path recorded_hash actual_hash
            for artifact_entry in "${artifact_entries[@]}"; do
                artifact_label="${artifact_entry%%|*}"
                artifact_path="${artifact_entry#*|}"
                if [ ! -f "$artifact_path" ]; then
                    error "App Store release artifact evidence: Missing local artifact for $artifact_label"
                    continue
                fi

                recorded_hash=$(grep -F "$artifact_label" "$evidence_file" | sed -E 's/.*`([0-9a-f]{64})`.*/\1/' | head -n 1)
                actual_hash=$(shasum -a 256 "$artifact_path" 2>/dev/null | awk '{print $1}')
                if [ -n "$recorded_hash" ] && [ "$recorded_hash" = "$actual_hash" ]; then
                    pass "App Store release artifact evidence: $artifact_label matches local unsigned Release artifact"
                else
                    error "App Store release artifact evidence: $artifact_label hash does not match local unsigned Release artifact"
                fi
            done
        else
            warning "App Store release artifact evidence: Cannot verify local artifact hashes because shasum is unavailable"
        fi
    else
        warning "App Store release artifact evidence: Local unsigned Release app or dSYM not present"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires release artifact evidence completion"
    else
        error "Final App Store signoff: Does not require release artifact evidence completion"
    fi

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires release artifact evidence before manual submission"
    else
        error "App Store launch checklist: Does not require release artifact evidence before manual submission"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_release_artifact_evidence_for_final_release" "$audit_script"; then
        pass "Final audit: Requires release artifact evidence for final release"
    else
        error "Final audit: Does not require release artifact evidence for final release"
    fi

    if [ -f "$final_signoff" ] && grep -Fxq "APP_STORE_FINAL_SIGNOFF_COMPLETE=true" "$final_signoff"; then
        if grep -Fxq "APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=true" "$evidence_file"; then
            pass "App Store release artifact evidence: Complete for final signoff"
        else
            error "App Store release artifact evidence: Final signoff cannot be complete until APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=true"
        fi
    fi
}

validate_app_store_content_rights_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}©️  App Store Content Rights Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store content rights evidence: Missing at docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store content rights evidence" 1000 18000

    local required_phrases=(
        "APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-28"
        "metadata, privacy information, descriptions, screenshots, and previews should accurately reflect the app's core experience"
        "stay up to date with new versions"
        "new features, functionality, and product changes must be described with specificity in the Notes for Review"
        "screenshots should show the app in use"
        "not merely title art, a login page, or a splash screen"
        "app previews may only use video screen captures of the app itself"
        "screenshots and previews may use overlays"
        "responsible for securing the rights to all materials in app icons, screenshots, and previews"
        "should use fictional account information instead of data from a real person"
        "app names and keywords should be unique, accurate"
        "not packed with trademarked terms, popular app names, pricing information, or irrelevant phrases"
        "app names are limited to 30 characters"
        "metadata, icons, screenshots, and previews should be appropriate for all audiences"
        "avoid confusion"
        "metadata should focus on the app itself and its Apple-platform experience"
        "without names, icons, or imagery of other mobile platforms or alternative app marketplaces"
        "apps should include only content the developer created or has a license to use"
        "must not use protected third-party material"
        "trademarks, copyrighted works, or patented ideas, without permission"
        "owns or has licensed the intellectual property and other relevant rights"
        "third-party services must be permitted by the service's terms of use"
        "authorization for third-party service usage must be available on request"
        "must not falsely suggest association, sponsorship, or endorsement"
        "names, icons, screenshots, previews, and metadata should not be copycat or misleading"
        "App Review notes should include supporting information"
        "partnership documentation or authorization should be attached in App Store Connect"
        "## Required Rights Review"
        "## Apple References"
        "## Evidence Commands"
        "## Evidence To Attach"
        "## Closure Rule"
        "App name and brand"
        "App icon assets"
        "Screenshots and previews"
        "App preview videos"
        "Metadata text"
        "Platform references"
        "Provider names and links"
        "Third-party service content"
        "Open-source notices"
        "Apple endorsement"
        "App Review attachments"
        "iosApp/src/Assets.xcassets/AppIcon.appiconset"
        "composeApp/screenshots/ios"
        "composeApp/metadata/ios"
        "Google Play"
        "alternative app marketplace"
        "Tricount"
        "## Local Content Rights Scan Result"
        "Local scan date: 2026-06-01"
        "local App Store-facing assets and metadata have been inventoried"
        '`AppIcon.png`, `AppIconDark.png`, and `AppIconTinted.png`'
        '`1024 x 1024`'
        "local app-icon set hash: \`9692f367663596a6a43793078d3886d064f04011ada5d4f1ea69000e2f1275bd\`"
        "8 PNG files across \`composeApp/metadata/ios\` and \`composeApp/screenshots/ios\`"
        "iPhone screenshots are \`1320 x 2868\`"
        "iPad screenshots are \`2048 x 2732\`"
        "App preview videos: 0"
        "avoid unnecessary platform trademark/design-system references"
        "Local metadata scan found no \`Android\`, \`Google Play\`, \`Play Store\`, or \`alternative app marketplace\` phrases"
        "Sign in with Apple, Google Sign-In, Tricount handoff, FaceTime, Zoom, Google Meet, Microsoft Teams, Webex, Airbnb, Booking.com, Uber, Lyft, and BlaBlaCar"
        "Local screenshot set hash: \`e1d72a791111bc43b561e7b463043167e860a47f3c290443c0a015f64ef3effe\`"
        "local pre-submission evidence only"
        "source/ownership records, screenshot capture provenance"
        "App Review attachment IDs or a no-attachment decision"
        "docs/APP_STORE_FINAL_SIGNOFF.md"
        "APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store content rights evidence: Covers $phrase"
        else
            error "App Store content rights evidence: Missing '$phrase'"
        fi
    done

    if command -v shasum >/dev/null 2>&1; then
        local icon_hash screenshot_hash
        icon_hash=$(cd "$PROJECT_DIR" && find iosApp/src/Assets.xcassets/AppIcon.appiconset -type f -name "*.png" -print | sort | shasum -a 256 | awk '{print $1}')
        screenshot_hash=$(cd "$PROJECT_DIR" && find composeApp/metadata/ios composeApp/screenshots/ios -type f -name "*.png" -print | sort | shasum -a 256 | awk '{print $1}')

        if grep -Fq "local app-icon set hash: \`$icon_hash\`" "$evidence_file"; then
            pass "App Store content rights evidence: App icon inventory hash matches local paths"
        else
            error "App Store content rights evidence: App icon inventory hash does not match local paths"
        fi

        if grep -Fq "Local screenshot set hash: \`$screenshot_hash\`" "$evidence_file"; then
            pass "App Store content rights evidence: Screenshot inventory hash matches local paths"
        else
            error "App Store content rights evidence: Screenshot inventory hash does not match local paths"
        fi
    else
        warning "App Store content rights evidence: Cannot verify local inventory hashes because shasum is unavailable"
    fi

    local preview_count
    preview_count=$(find "$PROJECT_DIR/composeApp/metadata/ios" "$PROJECT_DIR/composeApp/screenshots/ios" -type f \( -name "*.mov" -o -name "*.mp4" -o -name "*.m4v" \) | wc -l | tr -d ' ')
    if [ "$preview_count" = "0" ]; then
        pass "App Store content rights evidence: No local app preview videos found"
    else
        error "App Store content rights evidence: Local app preview videos require rights/provenance review"
    fi

    if rg -n "Android|Google Play|Play Store|alternative app marketplace" "$PROJECT_DIR/composeApp/metadata/ios" >/dev/null 2>&1; then
        error "App Store content rights evidence: iOS metadata contains non-Apple platform or marketplace references"
    else
        pass "App Store content rights evidence: iOS metadata avoids non-Apple platform or marketplace references"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires content rights evidence completion"
    else
        error "Final App Store signoff: Does not require content rights evidence completion"
    fi

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires content rights evidence before manual submission"
    else
        error "App Store launch checklist: Does not require content rights evidence before manual submission"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_content_rights_evidence_for_final_release" "$audit_script"; then
        pass "Final audit: Requires content rights evidence for final release"
    else
        error "Final audit: Does not require content rights evidence for final release"
    fi

    if [ -f "$final_signoff" ] && grep -Fxq "APP_STORE_FINAL_SIGNOFF_COMPLETE=true" "$final_signoff"; then
        if grep -Fxq "APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true" "$evidence_file"; then
            pass "App Store content rights evidence: Complete for final signoff"
        else
            error "App Store content rights evidence: Final signoff cannot be complete until APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true"
        fi
    fi
}

validate_app_store_license_notices_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}📄 App Store License Notices Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store license notices evidence: Missing at docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store license notices evidence" 1000 18000

    local required_phrases=(
        "APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-27"
        "App Review submissions should be final versions with all necessary metadata and fully functional URLs included"
        "placeholder text, empty websites, and other temporary content should be removed before submission"
        "metadata, including privacy information, description, screenshots, and previews, must accurately reflect the app's core experience"
        "apps should include only content the developer created or has a license to use"
        "must not use protected third-party material"
        "trademarks, copyrighted works, or patented ideas, without permission"
        "must not include misleading, false, or copycat representations, names, or metadata"
        "owns or has licensed the intellectual property and other relevant rights"
        "responsible for services provided by the app"
        "review information should include the details needed for App Review"
        "## Required License Review"
        "## Apple References"
        "## Evidence Commands"
        "## Local Strict Inventory Result"
        "## Local Notices Generation Result"
        "## Evidence To Attach"
        "## Closure Rule"
        "Dependency inventory"
        "License compatibility"
        "Notice obligations"
        "App-bundled notices"
        "Generated artifacts"
        "Prohibited or unclear licenses"
        "Review notes"
        "Final build match"
        "Swift Package Manager"
        "CocoaPods"
        "Gradle"
        "npm"
        "Fastlane/runtime"
        "MIT, BSD, Apache-2.0, ISC, CC-BY"
        "GPL/AGPL/LGPL"
        "APP_STORE_LICENSE_NOTICES_CONFIRMED=true"
        "APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md"
        "docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md"
        "docs/APP_STORE_THIRD_PARTY_NOTICES.md"
        "./scripts/app-store-license-inventory.sh --fetch-remote-metadata --output docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md"
        "./scripts/app-store-license-inventory.sh --fetch-remote-metadata --fail-on-unknown"
        "Result: passed locally for the submitted iOS scope in the current repository inventory."
        "Dependencies listed: 316"
        "Unknown licenses: 3"
        "Copyleft keywords detected: 1"
        "Submitted iOS unknown/copyleft risks: 0"
        'docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md`: 348 lines'
        'docs/APP_STORE_THIRD_PARTY_NOTICES.md`: 371 lines'
        'apps/landing/src/routes/third-party-notices/+page.svelte`: 2313 lines'
        "62dbb5f5bb604ecbbe12bd9eb7be254f7027f352db1a043289151ab8d24162e6"
        "d9586fc13c6458680525f6ea7046eae0bb56b663c064946fcede07e9a8624732"
        "977fca4f1ae05c3c8e28cb08e100531d6a4804734c97e88084fc25b2fdf710df"
        "local pre-submission evidence only"
        "matched to the signed App Store review IPA/archive"
        "./scripts/app-store-third-party-notices.sh --markdown-output docs/APP_STORE_THIRD_PARTY_NOTICES.md --web-output apps/landing/src/routes/third-party-notices/+page.svelte"
        "https://wakeve.app/third-party-notices"
        "docs/APP_STORE_FINAL_SIGNOFF.md"
        "review/guidelines"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store license notices evidence: Covers $phrase"
        else
            error "App Store license notices evidence: Missing '$phrase'"
        fi
    done

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires license notices evidence before APP_STORE_LICENSE_NOTICES_CONFIRMED"
    else
        error "App Store launch checklist: Does not require license notices evidence before APP_STORE_LICENSE_NOTICES_CONFIRMED"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires license notices evidence completion"
    else
        error "Final App Store signoff: Does not require license notices evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md"' "$audit_script" &&
        grep -Fq 'require_file "docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md"' "$audit_script" &&
        grep -Fq 'require_file "docs/APP_STORE_THIRD_PARTY_NOTICES.md"' "$audit_script" &&
        grep -Fq 'require_file "apps/landing/src/routes/third-party-notices/+page.svelte"' "$audit_script" &&
        grep -Fq "require_license_notices_evidence_if_confirmed" "$audit_script"; then
        pass "Final audit: Requires license notices evidence, inventory, and notices route when APP_STORE_LICENSE_NOTICES_CONFIRMED is true"
    else
        error "Final audit: Does not require license notices evidence, inventory, and notices route when APP_STORE_LICENSE_NOTICES_CONFIRMED is true"
    fi

    local inventory_script="$PROJECT_DIR/scripts/app-store-license-inventory.sh"
    if [ -x "$inventory_script" ]; then
        pass "App Store license inventory script: Exists and is executable"
    else
        error "App Store license inventory script: Missing or not executable"
    fi

    local notices_script="$PROJECT_DIR/scripts/app-store-third-party-notices.sh"
    if [ -x "$notices_script" ]; then
        pass "App Store third-party notices script: Exists and is executable"
    else
        error "App Store third-party notices script: Missing or not executable"
    fi

    local inventory_file="$PROJECT_DIR/docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md"
    if [ ! -f "$inventory_file" ]; then
        error "App Store license inventory draft: Missing at docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md"
    else
        validate_text_file "$inventory_file" "App Store license inventory draft" 500 120000
        local inventory_phrases=(
            "Status: DRAFT"
            "Dependencies listed:"
            "Unknown licenses:"
            "Copyleft keywords detected:"
            "Submitted iOS unknown/copyleft risks:"
            "App Store Scope"
            "## Dependency Inventory"
            "gradle-version-catalog"
            "npm-web"
            "ruby-gem"
            "Remote Maven POM fetching:"
            "Remote RubyGems fetching:"
            "does not prove App Store readiness"
        )
        local inventory_phrase
        for inventory_phrase in "${inventory_phrases[@]}"; do
            if grep -Fq "$inventory_phrase" "$inventory_file"; then
                pass "App Store license inventory draft: Covers $inventory_phrase"
            else
                error "App Store license inventory draft: Missing '$inventory_phrase'"
            fi
        done
    fi

    local notices_file="$PROJECT_DIR/docs/APP_STORE_THIRD_PARTY_NOTICES.md"
    if [ ! -f "$notices_file" ]; then
        error "App Store third-party notices draft: Missing at docs/APP_STORE_THIRD_PARTY_NOTICES.md"
    else
        validate_text_file "$notices_file" "App Store third-party notices draft" 500 120000
        local notices_phrases=(
            "Status: DRAFT"
            "Dependencies listed:"
            "Unknown licenses:"
            "Copyleft keywords detected:"
            "Submitted iOS unknown/copyleft risks:"
            "App Store Scope"
            "Public notice URL:"
            "## Dependency Notices"
            "does not prove"
            "Final release approval"
        )
        local notices_phrase
        for notices_phrase in "${notices_phrases[@]}"; do
            if grep -Fq "$notices_phrase" "$notices_file"; then
                pass "App Store third-party notices draft: Covers $notices_phrase"
            else
                error "App Store third-party notices draft: Missing '$notices_phrase'"
            fi
        done
    fi

    local notices_route="$PROJECT_DIR/apps/landing/src/routes/third-party-notices/+page.svelte"
    if [ ! -f "$notices_route" ]; then
        error "Public third-party notices route: Missing at apps/landing/src/routes/third-party-notices/+page.svelte"
    else
        local notices_route_phrases=(
            "Third-Party Notices"
            "Dependency Notices"
            "Review Status"
        )
        local notices_route_phrase
        for notices_route_phrase in "${notices_route_phrases[@]}"; do
            if grep -Fq "$notices_route_phrase" "$notices_route"; then
                pass "Public third-party notices route: Covers $notices_route_phrase"
            else
                error "Public third-party notices route: Missing '$notices_route_phrase'"
            fi
        done
    fi

    if command -v shasum >/dev/null 2>&1; then
        local evidence_hash
        for evidence_hash in \
            "$(shasum -a 256 "$inventory_file" 2>/dev/null | awk '{print $1}')" \
            "$(shasum -a 256 "$notices_file" 2>/dev/null | awk '{print $1}')" \
            "$(shasum -a 256 "$notices_route" 2>/dev/null | awk '{print $1}')"; do
            if [ -n "$evidence_hash" ] && grep -Fq "$evidence_hash" "$evidence_file"; then
                pass "App Store license notices evidence: Records generated artifact hash $evidence_hash"
            else
                error "App Store license notices evidence: Missing generated artifact hash $evidence_hash"
            fi
        done
    else
        warning "App Store license notices evidence: Cannot verify generated artifact hashes because shasum is unavailable"
    fi

    if [ -f "$final_signoff" ] && grep -Fxq "APP_STORE_FINAL_SIGNOFF_COMPLETE=true" "$final_signoff"; then
        if grep -Fxq "APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true" "$evidence_file"; then
            pass "App Store license notices evidence: Complete for final signoff"
        else
            error "App Store license notices evidence: Final signoff cannot be complete until APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true"
        fi
    fi
}

validate_app_store_eula_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}📜 App Store EULA Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_EULA_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store EULA evidence: Missing at docs/APP_STORE_EULA_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store EULA evidence" 1000 16000

    local required_phrases=(
        "APP_STORE_EULA_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-28"
        "App Store Connect App Information includes a License Agreement field"
        "Apple's standard EULA applies in all regions when no custom EULA is provided"
        "license agreement link is not shown on the App Store product page"
        "Apple publishes the standard Licensed Application End User License Agreement"
        "https://www.apple.com/legal/internet-services/itunes/dev/stdeula/"
        "custom EULA can supersede the standard Apple EULA for one or more regions"
        "custom EULA text is entered as plain text"
        "HTML tags are stripped and escaped"
        "only line break characters are accepted"
        "localized custom EULA text must be added for each language within the same text field"
        "selected countries or regions must be recorded"
        "custom agreement only to the selected territory scope"
        "## Required EULA Review"
        "## Apple References"
        "## Evidence Commands"
        "## Evidence To Attach"
        "## Closure Rule"
        "License agreement choice"
        "Terms alignment"
        "Standard EULA reference"
        "Custom EULA scope"
        "Custom EULA formatting"
        "Territory scope"
        "Standard EULA scope"
        "Legal owner approval"
        "Product page consistency"
        "Final build match"
        "Apple's standard EULA"
        "custom EULA"
        "plain text"
        "countries/regions"
        "docs/TERMS_OF_SERVICE.md"
        "apps/landing/src/routes/terms/+page.svelte"
        "## Local Terms And EULA Alignment Scan Result"
        "Wakeve has local Terms of Service sources and a deployed-route implementation ready for review"
        "Source Terms document: \`docs/TERMS_OF_SERVICE.md\`, 226 lines, SHA-256 \`acb3e0af841629dbeabf6c868d7474848e3217b625ce0e826e86dcabf2abfd95\`"
        "Public terms route source: \`apps/landing/src/routes/terms/+page.svelte\`, 121 lines, SHA-256 \`b97e8ab88b11dc4c17ac6c9c11fae3076fc64e919554fb61c360d6bfd87d9ca4\`"
        "Public terms route exposes \`Terms of Service - Wakeve\`"
        "Local Terms document covers acceptance, service description, account registration"
        "iOS metadata privacy URLs for \`en-US\` and \`fr-FR\` both point to \`https://wakeve.app/privacy\`"
        "support URLs for both locales point to \`https://wakeve.app/support\`"
        "current local evidence supports using Apple's standard EULA"
        "current local evidence does not prove which App Store Connect License Agreement option is selected"
        "exact plain-text EULA"
        "line breaks only"
        "Country or region scope evidence"
        "App Store Connect License Agreement setting has not been captured"
        "local scan does not close AS-22"
        "APP_STORE_EULA_CONFIRMED=true"
        "APP_STORE_EULA_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_FINAL_SIGNOFF.md"
        "provide-a-custom-license-agreement"
        "app-information"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store EULA evidence: Covers $phrase"
        else
            error "App Store EULA evidence: Missing '$phrase'"
        fi
    done

    local field_map="$PROJECT_DIR/docs/APP_STORE_CONNECT_FIELD_MAP.md"
    if [ -f "$field_map" ] &&
        grep -Fq "docs/APP_STORE_EULA_EVIDENCE.md" "$field_map" &&
        grep -Fq "License Agreement" "$field_map"; then
        pass "App Store Connect field map: Covers EULA evidence"
    else
        error "App Store Connect field map: Does not cover EULA evidence"
    fi

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_EULA_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_EULA_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires EULA evidence before APP_STORE_EULA_CONFIRMED"
    else
        error "App Store launch checklist: Does not require EULA evidence before APP_STORE_EULA_CONFIRMED"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_EULA_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_EULA_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires EULA evidence completion"
    else
        error "Final App Store signoff: Does not require EULA evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_EULA_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_eula_evidence_if_confirmed" "$audit_script"; then
        pass "Final audit: Requires EULA evidence when APP_STORE_EULA_CONFIRMED is true"
    else
        error "Final audit: Does not require EULA evidence when APP_STORE_EULA_CONFIRMED is true"
    fi

    if [ -f "$final_signoff" ] && grep -Fxq "APP_STORE_FINAL_SIGNOFF_COMPLETE=true" "$final_signoff"; then
        if grep -Fxq "APP_STORE_EULA_EVIDENCE_COMPLETE=true" "$evidence_file"; then
            pass "App Store EULA evidence: Complete for final signoff"
        else
            error "App Store EULA evidence: Final signoff cannot be complete until APP_STORE_EULA_EVIDENCE_COMPLETE=true"
        fi
    fi
}

validate_app_store_age_rating() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🔞 App Store Age Rating${NC}"

    local age_rating_doc="$PROJECT_DIR/docs/APP_STORE_AGE_RATING.md"
    if [ ! -f "$age_rating_doc" ]; then
        error "App Store age rating: Missing at docs/APP_STORE_AGE_RATING.md"
        return 1
    fi

    validate_text_file "$age_rating_doc" "App Store age rating" 800 16000

    local required_doc_phrases=(
        "## Apple Source Baseline"
        "Apple-source review date: 2026-05-27"
        "age rating is a required app information property"
        "responding to the App Store Connect age rating questionnaire"
        "content descriptors, in-app controls, and capabilities"
        "Apple global age rating plus region-specific ratings"
        "age ratings may vary based on OS version"
        "iOS 26, iPadOS 26, macOS Tahoe 26, tvOS 26, visionOS 26, and watchOS 26"
        "Unrated app cannot be published on the App Store"
        "January 31, 2026"
        "Unrestricted Web Access means users can navigate to any webpage"
        "4+ rating can still include capabilities such as user-generated content"
        "Made for Kids can only be selected when the calculated rating is 4+ or 9+"
        "cannot be changed after App Review approval"
        "Made for Kids cannot be selected for visionOS apps"
        "override to a higher age rating"
        "override applies in all regions"
        "global and region-specific ratings"
        "Korea region-specific rating overrides"
        "Status: draft, pending App Store Connect verification"
        "composeApp/metadata/ios/app_rating_config.json"
        "updated age-rating system"
        "updated 2026 age-rating questionnaire"
        "every new required answer"
        'Unrestricted web access: `false`'
        'Seventeen plus override: `false`'
        "Kids category age band: not set"
        "not a game, gambling product, medical product, dating product, marketplace, or unrestricted web browser"
        "real-world shared-event expenses"
        "User-generated content exists"
        "user-generated content moderation gate"
        "Location usage is When In Use only"
        "does not expose unrestricted web browsing"
        "new App Store Connect age-rating questionnaire questions"
        "App Store Connect screenshot or export reference"
        "app_rating_config_path: repo_path(\"composeApp/metadata/ios/app_rating_config.json\")"
        "set-an-app-age-rating"
        "reference/age-ratings"
        "upcoming-requirements"
    )

    local phrase
    for phrase in "${required_doc_phrases[@]}"; do
        if grep -Fq "$phrase" "$age_rating_doc"; then
            pass "App Store age rating: Covers $phrase"
        else
            error "App Store age rating: Missing '$phrase'"
        fi
    done

    local rating_config="$PROJECT_DIR/composeApp/metadata/ios/app_rating_config.json"
    if [ ! -f "$rating_config" ]; then
        error "App Store age rating: Missing app_rating_config.json"
        return 0
    fi

    local rating_consistency_errors
    if rating_consistency_errors=$(ruby -rjson -e '
        path = ARGV.fetch(0)
        json = JSON.parse(File.read(path))
        expected_none = %w[
            alcoholTobaccoOrDrugUseOrReferences
            contests
            gamblingSimulated
            medicalOrTreatmentInformation
            profanityOrCrudeHumor
            sexualContentGraphicAndNudity
            sexualContentOrNudity
            horrorOrFearThemes
            matureOrSuggestiveThemes
            violenceCartoonOrFantasy
            violenceRealisticProlongedGraphicOrSadistic
            violenceRealistic
        ]
        expected_false = %w[gambling seventeenPlus unrestrictedWebAccess]
        errors = []
        expected_none.each do |key|
            errors << "#{key} must remain NONE for the current evidence doc" unless json[key] == "NONE"
        end
        expected_false.each do |key|
            errors << "#{key} must remain false for the current evidence doc" unless json[key] == false
        end
        errors << "kidsAgeBand must not be set unless Kids category is selected" if json.key?("kidsAgeBand")
        if errors.any?
            warn errors.join("\n")
            exit 1
        end
    ' "$rating_config" 2>&1); then
        pass "App Store age rating: JSON answers match current no-restricted-content evidence"
    else
        error "App Store age rating: JSON answers diverge from current evidence"
        info "$rating_consistency_errors"
    fi

    local field_map="$PROJECT_DIR/docs/APP_STORE_CONNECT_FIELD_MAP.md"
    if [ -f "$field_map" ] && grep -Fq "docs/APP_STORE_AGE_RATING.md" "$field_map"; then
        pass "App Store age rating: Field map references age rating evidence"
    else
        error "App Store age rating: Field map does not reference docs/APP_STORE_AGE_RATING.md"
    fi
}

validate_app_store_blocker_register() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🚧 App Store Blocker Register${NC}"

    local blocker_file="$PROJECT_DIR/docs/APP_STORE_BLOCKER_REGISTER.md"
    if [ ! -f "$blocker_file" ]; then
        error "App Store blocker register: Missing at docs/APP_STORE_BLOCKER_REGISTER.md"
        return 1
    fi

    validate_text_file "$blocker_file" "App Store blocker register" 1200 24000

    local blocker_number blocker_id
    for blocker_number in $(seq 1 22); do
        blocker_id=$(printf "AS-%02d" "$blocker_number")
        if grep -Fq "| $blocker_id |" "$blocker_file"; then
            pass "App Store blocker register: Covers $blocker_id"
        else
            error "App Store blocker register: Missing blocker $blocker_id"
        fi
    done

    if grep -Fq "| AW-01 |" "$blocker_file"; then
        pass "App Store blocker register: Covers AW-01"
    else
        error "App Store blocker register: Missing warning AW-01"
    fi

	    local required_phrases=(
	        "## Apple Source Baseline"
	        "Apple-source review date: 2026-05-28"
	        "app review process covers each submitted app version"
	        "app versions for each platform are submitted separately"
	        "each platform can typically have one app version submission under review at a time"
	        "items associated with different platforms cannot be added to the same submission"
	        "required metadata must be provided"
	        "correct uploaded build must be selected"
	        "Account Holder, Admin, or App Manager"
	        "Add for Review moves an app to Ready for Review"
	        "Submit for Review is clicked"
	        "app version can be added to an existing draft submission or to a new draft submission"
	        "all items submitted together must be accepted to complete the submission"
	        "final versions with all necessary metadata"
	        "fully functional URLs"
	        "placeholder text, empty websites, and other temporary content"
        "tested on device for bugs and stability"
        "demo account information, or an approved built-in demo mode"
        "filtering, reporting with timely responses, blocking for abusive users, and published contact information"
        "initiate account deletion from within the app"
        "App Privacy answers are required"
        "third-party partner data practices"
        "in-app purchase is required for digital content"
        "physical goods or services consumed outside the app"
        "app availability must be configured before App Review submission"
	        "Digital Services Act trader status information"
	        "current SDK requirements"
	        "iOS and iPadOS 26 SDK or later"
	        "Prepare for Submission, Ready for Review, Waiting for Review, In Review, Unresolved Issues, Developer Rejected, and Waiting for Export Compliance"
	        "Ready for Review means required metadata is entered"
	        "Waiting for Review means Apple received the submission but has not started review"
	        "screenshots and app previews cannot be uploaded or edited"
	        "In Review means App Review is reviewing the submission"
	        "submission status changes to Unresolved Issues"
	        "submission with Unresolved Issues cannot have more items added"
	        "rejected items must be edited and resubmitted or removed"
	        "Developer Rejected and the review process starts over"
	        "submissions may not be reviewed in the order submitted"
	        "Current result on 2026-06-13: 21 blockers, 1 warning"
	        "APP_REVIEW_PHONE_NUMBER='+33123456789' ./scripts/app-store-submission-audit.sh --skip-preflight"
	        'the documented placeholder `+15551234567` is now rejected by the final audit'
	        "Full local preflight baseline"
	        "APP_REVIEW_PHONE_NUMBER='+33123456789' ./scripts/app-store-submission-audit.sh"
	        "Current result on 2026-06-13: 21 blockers, 0 warnings"
	        "local Fastlane App Store preflight passes"
	        "Live deployment baseline"
	        "APP_REVIEW_PHONE_NUMBER='+33123456789' APPLE_TEAM_ID='A1B2C3D4E5' ./scripts/lint-store-metadata.sh --ios-only --check-live-urls"
	        "Current result on 2026-06-13: 9 live URL/AASA errors and 1 final-signoff warning"
	        "Could not resolve host"
	        "/third-party-notices"
	        "https://wakeve.app/third-party-notices"
        "App Store scope"
        "submitted-iOS unknown/copyleft risks"
        "non-iOS scoped exceptions"
        "openspec/changes/add-in-app-account-deletion/"
        "openspec/changes/add-ugc-moderation-controls/"
        "APP_STORE_REVIEW_ACCESS_EVIDENCE_COMPLETE=true"
        "APP_STORE_ACCOUNT_ACCESS_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md"
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
        "APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md"
        "APP_STORE_UGC_MODERATION_CONFIRMED=true"
        "APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_UGC_MODERATION_EVIDENCE.md"
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
        "APP_STORE_APP_INFORMATION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_APP_INFORMATION_EVIDENCE.md"
        "APP_STORE_ACCOUNT_ACCESS_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md"
        "APP_STORE_VERSIONING_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_VERSIONING_EVIDENCE.md"
        "APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md"
        "APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md"
        "APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md"
        "APP_STORE_EULA_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_EULA_EVIDENCE.md"
        "APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md"
        "APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md"
	        "APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true"
	        "docs/APP_STORE_UGC_MODERATION_EVIDENCE.md"
	        "./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready"
	        "APP_STORE_FINAL_SIGNOFF_COMPLETE=true"
	        "platform scope, draft submission membership, submitted items accepted or intentionally excluded, no Unresolved Issues state"
	        "removal, cancellation, or retry decision"
	        "overview-of-submitting-for-review"
	        "submit-an-app"
	        "app-and-submission-statuses"
	        "manage-a-submission-with-unresolved-issues"
	        "remove-a-submission-from-review"
	    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$blocker_file"; then
            pass "App Store blocker register: Covers $phrase"
        else
            error "App Store blocker register: Missing '$phrase'"
        fi
    done

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] && grep -Fq 'require_file "docs/APP_STORE_BLOCKER_REGISTER.md"' "$audit_script"; then
        pass "Final audit: Requires App Store blocker register"
    else
        error "Final audit: Does not require App Store blocker register"
    fi

    if [ -f "$audit_script" ] && grep -Fq "require_final_signoff_evidence_matrix" "$audit_script" && grep -Fq "## Blocker Evidence Matrix" "$audit_script"; then
        pass "Final audit: Requires final signoff blocker evidence matrix"
    else
        error "Final audit: Does not require final signoff blocker evidence matrix"
    fi
}

validate_app_store_product_blocker_approval() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🧾 App Store Product Blocker Approval${NC}"

    local approval_file="$PROJECT_DIR/docs/APP_STORE_PRODUCT_BLOCKER_APPROVAL.md"
    if [ ! -f "$approval_file" ]; then
        error "App Store product blocker approval: Missing at docs/APP_STORE_PRODUCT_BLOCKER_APPROVAL.md"
        return 1
    fi

    validate_text_file "$approval_file" "App Store product blocker approval" 1200 12000

    local required_phrases=(
        "Status: LOCAL IMPLEMENTATION COMPLETE; RELEASE EVIDENCE PENDING"
        "Account deletion and UGC moderation are now locally implemented and verified"
        "AS-09 Account deletion"
        "AS-10 UGC moderation"
        "openspec/changes/add-in-app-account-deletion/"
        "openspec/changes/add-ugc-moderation-controls/"
        "Guideline 5.1.1(v)"
        "Guideline 1.2"
        "Profile -> Data Management -> Delete Account"
        "Profile -> Data Management -> Delete Guest Data"
        "DELETE /api/user/delete"
        "APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=false"
        "APP_STORE_ACCOUNT_DELETION_CONFIRMED=true"
        "deterministic server-side filtering"
        "pending-review quarantine"
        "report comments, chat messages, events, and users"
        "block/unblock users"
        "APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=false"
        "APP_STORE_UGC_MODERATION_CONFIRMED=true"
        "Local implementation, focused tests, iOS discoverability checks, gates, and local final validation present"
        "./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$approval_file"; then
            pass "App Store product blocker approval: Covers $phrase"
        else
            error "App Store product blocker approval: Missing '$phrase'"
        fi
    done

    local blocker_register="$PROJECT_DIR/docs/APP_STORE_BLOCKER_REGISTER.md"
    if [ -f "$blocker_register" ] &&
        grep -Fq "docs/APP_STORE_PRODUCT_BLOCKER_APPROVAL.md" "$blocker_register" &&
        grep -Fq "Product blockers AS-09 and AS-10 required OpenSpec approval" "$blocker_register"; then
        pass "App Store blocker register: References product blocker approval packet"
    else
        error "App Store blocker register: Does not reference product blocker approval packet"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_PRODUCT_BLOCKER_APPROVAL.md"' "$audit_script"; then
        pass "Final audit: Requires product blocker approval packet"
    else
        error "Final audit: Does not require product blocker approval packet"
    fi
}

validate_app_store_review_guideline_audit() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🧭 App Store Review Guideline Audit${NC}"

    local audit_file="$PROJECT_DIR/docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md"
    if [ ! -f "$audit_file" ]; then
        error "App Store Review Guideline audit: Missing at docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md"
        return 1
    fi

    validate_text_file "$audit_file" "App Store Review Guideline audit" 800 24000

    local required_baseline_phrases=(
        "## Apple Source Baseline"
        "Apple-source review date: 2026-05-27"
        "privacy, security, safety, and reliability"
        "filtering for objectionable material"
        "reporting with timely responses"
        "blocking for abusive users"
        "published contact information"
        "final versions with all necessary metadata"
        "fully functional URLs"
        "placeholder text, empty websites, and temporary content removed"
        "tested on device for bugs and stability"
        "demo account information or an approved built-in demo mode"
        "metadata should accurately describe the app"
        "hidden, dormant, or undocumented features"
        "equivalent login option"
        "initiate account deletion from within the app"
        "privacy responses must be accurate"
        "in-app purchase is required for digital content"
        "goods or services consumed outside the app"
        "minimum SDK requirements"
        "iOS and iPadOS 26 SDK or later"
        "Accessibility Nutrition Labels help users understand accessibility support"
        "visible on App Store product pages on OS 26-era platforms"
        "Digital Services Act trader status information"
        "App Review rejections and appeals"
    )

    local baseline_phrase
    for baseline_phrase in "${required_baseline_phrases[@]}"; do
        if grep -Fq "$baseline_phrase" "$audit_file"; then
            pass "App Store Review Guideline audit: Baseline covers $baseline_phrase"
        else
            error "App Store Review Guideline audit: Missing baseline phrase '$baseline_phrase'"
        fi
    done

    local required_topics=(
        "Guideline 1.2"
        "Guideline 2.1"
        "Guideline 2.3"
        "Guideline 3.1.1"
        "Guideline 3.1.3"
        "Guideline 4.8"
        "Guideline 5.1.1"
        "Guideline 5.1.1(v)"
        "Account deletion"
        "Payment compliance"
        "App Store SDK minimum"
        "Accessibility Nutrition Labels"
        "Digital Services Act"
    )

    local topic
    for topic in "${required_topics[@]}"; do
        if grep -Fq "$topic" "$audit_file"; then
            pass "App Store Review Guideline audit: Covers $topic"
        else
            error "App Store Review Guideline audit: Missing $topic"
        fi
    done

    if grep -R "chatRoutes\\|commentRoutes\\|chatWebSocketRoute" "$PROJECT_DIR/server/src/main/kotlin" >/dev/null 2>&1; then
        local ugc_guards=(
            "APP_STORE_UGC_MODERATION_CONFIRMED"
            "Do not submit"
            "filter"
            "report"
            "block"
            "published contact information"
        )

        local guard
        for guard in "${ugc_guards[@]}"; do
            if grep -Fqi "$guard" "$audit_file"; then
                pass "App Store Review Guideline audit: UGC guard covers $guard"
            else
                error "App Store Review Guideline audit: Missing UGC guard '$guard'"
            fi
        done
    fi

    if grep -R "loginWithEmail\\|loginWithOAuth\\|loginAsGuest\\|/auth/apple\\|/auth/google" "$PROJECT_DIR/server/src/main/kotlin" "$PROJECT_DIR/iosApp/src" >/dev/null 2>&1; then
        local account_deletion_guards=(
            "APP_STORE_ACCOUNT_DELETION_CONFIRMED"
            "Profile Settings"
            "entire account record"
            "Sign in with Apple token revocation"
            "Do not submit"
        )

        local account_guard
        for account_guard in "${account_deletion_guards[@]}"; do
            if grep -Fqi "$account_guard" "$audit_file"; then
                pass "App Store Review Guideline audit: Account deletion guard covers $account_guard"
            else
                error "App Store Review Guideline audit: Missing account deletion guard '$account_guard'"
            fi
        done
    fi

    if grep -R "PaymentPotRepository\\|TricountHandoffRepository\\|paymentRoutes\\|tricountGroupUrl\\|providerUrl" \
        "$PROJECT_DIR/server/src/main/kotlin" \
        "$PROJECT_DIR/shared/src/commonMain/kotlin" \
        "$PROJECT_DIR/iosApp/src" >/dev/null 2>&1; then
        local payment_guards=(
            "APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED"
            "real-world event expenses"
            "No external payment flow unlocks app features"
            "App Review notes"
            "trusted-domain validated"
            "Do not submit"
        )

        local payment_guard
        for payment_guard in "${payment_guards[@]}"; do
            if grep -Fqi "$payment_guard" "$audit_file"; then
                pass "App Store Review Guideline audit: Payment guard covers $payment_guard"
            else
                error "App Store Review Guideline audit: Missing payment guard '$payment_guard'"
            fi
        done
    fi
}

validate_app_store_account_deletion_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🧾 App Store Account Deletion Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store account deletion evidence: Missing at docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store account deletion evidence" 1000 20000

    local required_phrases=(
        "APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-28"
        "Apple App Review Guideline 5.1.1(v), last updated 2026-02-06"
        "initiate account deletion from the app"
        "shared user-generated content handling"
        "account deletion option should be easy to find"
        "temporary deactivation or disablement is insufficient"
        "automatically generated or guest-style accounts"
        "manual or delayed deletion processes"
        "all users should be allowed to delete their accounts regardless of location"
        "user-generated content shared with others"
        "## Apple References"
        "offering-account-deletion-in-your-app"
        "guidelines/#data-collection-and-storage"
        "upcoming-requirements/?id=06302022b"
        "## Build Under Review"
        "## Required Account Deletion Review"
        "## Evidence Commands"
        "## Local Implementation Verification Result"
        "## Closure Rule"
        "openspec/changes/add-in-app-account-deletion/"
        "docs/APP_STORE_TESTFLIGHT_EVIDENCE.md"
        "composeApp/metadata/ios/review_information/notes.txt"
        "In-app initiation"
        "Guest data deletion"
        "Backend deletion"
        "Shared UGC handling"
        "Session and token revocation"
        "Sign in with Apple"
        "Local cleanup"
        "Delayed completion"
        "Reviewer path"
        "shared user-generated content deletion, anonymization, or legally required retention behavior"
        "expected completion timeline"
        "openspec validate add-in-app-account-deletion --strict"
        "./gradlew :server:test --tests com.guyghost.wakeve.auth.AccountDeletionServiceTest --tests com.guyghost.wakeve.AuthFlowIntegrationTest"
        "FindingsRegressionTests/testProfileDataManagementExposesAccountDeletionFlow"
        "Change 'add-in-app-account-deletion' is valid"
        "Active tasks are \`13/16\`"
        "local implementation evidence only"
        "APP_STORE_ACCOUNT_DELETION_CONFIRMED=true"
        "APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store account deletion evidence: Covers $phrase"
        else
            error "App Store account deletion evidence: Missing '$phrase'"
        fi
    done

    if command -v openspec >/dev/null 2>&1; then
        if (cd "$PROJECT_DIR" && openspec validate add-in-app-account-deletion --strict >/dev/null 2>&1); then
            pass "OpenSpec account deletion proposal: add-in-app-account-deletion validates strictly"
        else
            error "OpenSpec account deletion proposal: add-in-app-account-deletion does not validate strictly"
        fi
    else
        warning "OpenSpec account deletion proposal: Cannot validate because openspec is unavailable"
    fi

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires account deletion evidence before APP_STORE_ACCOUNT_DELETION_CONFIRMED"
    else
        error "App Store launch checklist: Does not require account deletion evidence before APP_STORE_ACCOUNT_DELETION_CONFIRMED"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires account deletion evidence completion"
    else
        error "Final App Store signoff: Does not require account deletion evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_account_deletion_evidence_if_confirmed" "$audit_script"; then
        pass "Final audit: Requires account deletion evidence when APP_STORE_ACCOUNT_DELETION_CONFIRMED is true"
    else
        error "Final audit: Does not require account deletion evidence when APP_STORE_ACCOUNT_DELETION_CONFIRMED is true"
    fi
}

validate_app_store_ugc_moderation_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🛡️  App Store UGC Moderation Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_UGC_MODERATION_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store UGC moderation evidence: Missing at docs/APP_STORE_UGC_MODERATION_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store UGC moderation evidence" 1000 20000

    local required_phrases=(
        "APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-28"
        "Apple App Review Guideline 1.2, last updated 2026-02-06"
        "random or anonymous chat"
        "filtering objectionable material before posting"
        "reporting offensive content with timely responses"
        "blocking abusive users"
        "published contact information"
        "social networking services must include these protections"
        "primarily pornographic, random/anonymous chat, objectification, threat, or bullying services"
        "fully functional URLs"
        "## Apple References"
        "guidelines/#user-generated-content"
        "guidelines/#app-completeness"
        "## Build Under Review"
        "## Required UGC Moderation Review"
        "## Evidence Commands"
        "## OpenSpec Proposal Validation Result"
        "## Closure Rule"
        "openspec/changes/add-ugc-moderation-controls/"
        "docs/APP_STORE_TESTFLIGHT_EVIDENCE.md"
        "composeApp/metadata/ios/review_information/notes.txt"
        "UGC surface inventory"
        "Filtering"
        "Pending review"
        "Reporting"
        "Blocking"
        "Moderator audit"
        "iOS discoverability"
        "support@wakeve.app"
        "Reviewer URLs"
        "Disabled surfaces"
        "Reviewer path"
        "Live moderation/support URLs"
        "disabled UGC surfaces"
        "Support/contact URLs used for moderation are live, final, and non-placeholder"
        "openspec validate add-ugc-moderation-controls --strict"
        "Change 'add-ugc-moderation-controls' is valid"
        "Active tasks are \`21/21\`"
        "Local Implementation Evidence"
        "./scripts/test-app-store-ugc-gates.sh"
        "APP_STORE_UGC_MODERATION_CONFIRMED=true"
        "APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store UGC moderation evidence: Covers $phrase"
        else
            error "App Store UGC moderation evidence: Missing '$phrase'"
        fi
    done

    if command -v openspec >/dev/null 2>&1; then
        if (cd "$PROJECT_DIR" && openspec validate add-ugc-moderation-controls --strict >/dev/null 2>&1); then
            pass "OpenSpec UGC moderation proposal: add-ugc-moderation-controls validates strictly"
        else
            error "OpenSpec UGC moderation proposal: add-ugc-moderation-controls does not validate strictly"
        fi
    else
        warning "OpenSpec UGC moderation proposal: Cannot validate because openspec is unavailable"
    fi

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_UGC_MODERATION_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires UGC moderation evidence before APP_STORE_UGC_MODERATION_CONFIRMED"
    else
        error "App Store launch checklist: Does not require UGC moderation evidence before APP_STORE_UGC_MODERATION_CONFIRMED"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_UGC_MODERATION_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires UGC moderation evidence completion"
    else
        error "Final App Store signoff: Does not require UGC moderation evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_UGC_MODERATION_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_ugc_moderation_evidence_if_confirmed" "$audit_script"; then
        pass "Final audit: Requires UGC moderation evidence when APP_STORE_UGC_MODERATION_CONFIRMED is true"
    else
        error "Final audit: Does not require UGC moderation evidence when APP_STORE_UGC_MODERATION_CONFIRMED is true"
    fi
}

validate_app_store_availability_decisions() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🧭 App Store Availability Decisions${NC}"

    local availability_file="$PROJECT_DIR/docs/APP_STORE_AVAILABILITY_DECISIONS.md"
    if [ ! -f "$availability_file" ]; then
        error "App Store availability decisions: Missing at docs/APP_STORE_AVAILABILITY_DECISIONS.md"
        return 1
    fi

    validate_text_file "$availability_file" "App Store availability decisions" 700 16000

    local required_phrases=(
        "## Apple Source Baseline"
        "Apple-source review date: 2026-05-27"
        "app availability must be set before submitting an app for App Store review"
        "175 countries or regions"
        "Pricing and Availability"
        "Apple Account country or region"
        "All Countries or Regions, Specific Countries or Regions, or Publish as Pre-Order"
        "deselecting a country or region removes the app from the App Store"
        "may require up to 24 hours to be visible to all users"
        "iPhone and iPad apps can be accessed on Macs with Apple silicon"
        "iPhone and iPad apps are available on Apple Vision Pro"
        "same frameworks, resources, and runtime environments as iOS and iPadOS"
        "same frameworks, resources, and runtime environment as iOS and iPadOS"
        "set at the app level and apply to all versions of the app"
        "iPhone and iPad Apps on Apple Silicon Mac section"
        "iPhone and iPad Apps on Apple Vision Pro section"
        "Account Holder, Admin, or App Manager"
        "Mac Apple silicon compatibility can be verified in App Store Connect"
        "not available if a build has never been uploaded"
        "Kids category cannot be made available on visionOS"
        "TARGETED_DEVICE_FAMILY = 1,2"
        "SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD = NO"
        "SUPPORTS_XR_DESIGNED_FOR_IPHONE_IPAD = NO"
        "Submit Wakeve first for iPhone and iPad only"
        "Confirm launch countries or regions"
        "App Store Connect countries or regions availability is configured"
        "Mac Apple silicon"
        "Apple Vision Pro"
        "APP_STORE_AVAILABILITY_CONFIRMED=true"
        "APP_STORE_AVAILABILITY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_AVAILABILITY_EVIDENCE.md"
        "docs/APP_STORE_FINAL_SIGNOFF.md"
        "Accessibility Nutrition Labels"
        "App Store Connect screenshot or export reference"
        "manage-availability-of-iphone-and-ipad-apps-on-apple-vision-pro"
        "manage-availability-of-iphone-and-ipad-apps-on-macs-with-apple-silicon"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$availability_file"; then
            pass "App Store availability decisions: Covers $phrase"
        else
            error "App Store availability decisions: Missing '$phrase'"
        fi
    done
}

validate_app_store_availability_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🧭 App Store Availability Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_AVAILABILITY_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store availability evidence: Missing at docs/APP_STORE_AVAILABILITY_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store availability evidence" 1000 18000

    local required_phrases=(
        "APP_STORE_AVAILABILITY_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-27"
        "iPhone and iPad apps can be accessed on Macs with Apple silicon unless app availability is edited in App Store Connect"
        "iPhone and iPad apps are available on Apple Vision Pro unless availability is edited in App Store Connect"
        "same frameworks, resources, and runtime environments as iOS and iPadOS"
        "runs natively using the same frameworks, resources, and runtime environment as iOS and iPadOS"
        "choose to make iPhone and iPad apps available or not available on Mac Apple silicon and Apple Vision Pro"
        "availability choices are set at the app level and apply to all versions of the app"
        "Pricing and Availability under the iPhone and iPad Apps on Apple Silicon Mac section"
        "Pricing and Availability under the iPhone and iPad Apps on Apple Vision Pro section"
        "required role for editing these availability choices is Account Holder, Admin, or App Manager"
        "Mac Apple silicon compatibility can be verified in App Store Connect"
        "compatibility with Apple Silicon Macs is not available if a build has never been uploaded"
        "apps that have been in the Kids category cannot be made available on visionOS"
        "## Build And Store Scope"
        "## Current Xcode Settings"
        "## Evidence Commands"
        "## Closure Rule"
        "docs/APP_STORE_AVAILABILITY_DECISIONS.md"
        "docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md"
        "docs/APP_STORE_TESTFLIGHT_EVIDENCE.md"
        "TARGETED_DEVICE_FAMILY = 1,2"
        "SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD = NO"
        "SUPPORTS_XR_DESIGNED_FOR_IPHONE_IPAD = NO"
        "iPhone and iPad"
        "Mac Apple silicon"
        "Apple Vision Pro"
        "Accessibility Nutrition Labels"
        "EU storefront availability"
        "xcodebuild -project iosApp/iosApp.xcodeproj"
        "./scripts/lint-store-metadata.sh --ios-only"
        "APP_STORE_AVAILABILITY_CONFIRMED=true"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store availability evidence: Covers $phrase"
        else
            error "App Store availability evidence: Missing '$phrase'"
        fi
    done

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_AVAILABILITY_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_AVAILABILITY_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires availability evidence before APP_STORE_AVAILABILITY_CONFIRMED"
    else
        error "App Store launch checklist: Does not require availability evidence before APP_STORE_AVAILABILITY_CONFIRMED"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_AVAILABILITY_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_AVAILABILITY_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires availability evidence completion"
    else
        error "Final App Store signoff: Does not require availability evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_AVAILABILITY_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_availability_evidence_if_confirmed" "$audit_script"; then
        pass "Final audit: Requires availability evidence when APP_STORE_AVAILABILITY_CONFIRMED is true"
    else
        error "Final audit: Does not require availability evidence when APP_STORE_AVAILABILITY_CONFIRMED is true"
    fi
}

validate_app_store_dsa_trader_status() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🇪🇺 App Store DSA Trader Status${NC}"

    local dsa_file="$PROJECT_DIR/docs/APP_STORE_DSA_TRADER_STATUS.md"
    if [ ! -f "$dsa_file" ]; then
        error "App Store DSA trader status: Missing at docs/APP_STORE_DSA_TRADER_STATUS.md"
        return 1
    fi

    validate_text_file "$dsa_file" "App Store DSA trader status" 700 16000

	    local required_phrases=(
	        "Status: not confirmed"
	        "APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE=false"
	        "## Apple Source Baseline"
	        "Last checked: 2026-06-01"
	        "## Current 2026-06-01 Local Status"
	        "Repository-side checks cannot determine Wakeve's legal trader status"
	        "no App Store Connect screenshot/export or owner decision is recorded"
	        "whether EU countries are enabled or excluded"
	        "record either verified trader details or a non-trader declaration"
	        "excludes EU storefronts"
	        "Articles 30 and 31 of the Digital Services Act require Apple to verify and display trader contact information"
	        "address, phone number, and email address for display on the App Store product page"
	        "published on the App Store product page when the app is distributed in any of the 27 EU territories"
	        "declare a trader status even if they do not distribute apps in the EU"
	        "disclose whether or not they are a trader when submitting a new app"
	        "turn off or specify trader status for each specific app"
	        "developers must assess whether they are a trader for EU law purposes"
	        "Apple cannot determine whether a developer is a trader"
	        "revenue, commercial practices, VAT registration"
	        "professional or business capacity"
	        "not distributing apps on the App Store in the EU is not acting as a trader on the App Store"
	        "non-trader status informs EU consumers"
	        "consumer rights from applicable consumer protection laws will not apply"
	        "organization accounts display the D-U-N-S address"
	        "individual accounts must enter an address or P.O. Box"
	        "provide payment account details"
	        "certify that they only offer products or services that comply with applicable EU law"
	        "required role for account-level EU DSA compliance information is Account Holder or Admin"
	        "app-specific DSA status and optional Labels and Markings URL"
	        "Labels and Markings URL can be shown on App Store product pages"
	        "declare whether they are acting as a trader"
	        "Provide and verify trader status for EU distribution"
	        "Disable EU availability for the first release"
	        "declare non-trader status in App Store Connect"
	        "Labels and Markings URL is not applicable or enter the final URL"
	        "APP_STORE_DSA_TRADER_STATUS_CONFIRMED=true"
	        "APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE=true"
	        "docs/APP_STORE_FINAL_SIGNOFF.md"
	        "trader, non-trader, or EU storefront disabled"
	        'App-specific DSA setting for Bundle ID `com.guyghost.wakeve`'
	        "contact information provided"
	        "organization versus individual trader contact requirements"
	        "payment account details are present"
	        "EU-law certification is accepted"
	        "Labels and Markings URL is either entered or documented as not applicable"
	        "consumer-rights disclosure"
	        "storefront availability excludes EU countries"
	        "docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md"
	        "docs/APP_STORE_AVAILABILITY_EVIDENCE.md"
	        "App Store Connect screenshot or export reference"
	        "manage-european-union-digital-services-act-trader-requirements"
	    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$dsa_file"; then
            pass "App Store DSA trader status: Covers $phrase"
        else
            error "App Store DSA trader status: Missing '$phrase'"
        fi
    done

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_DSA_TRADER_STATUS.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires DSA evidence before APP_STORE_DSA_TRADER_STATUS_CONFIRMED"
    else
        error "App Store launch checklist: Does not require DSA evidence before APP_STORE_DSA_TRADER_STATUS_CONFIRMED"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_DSA_TRADER_STATUS.md" "$final_signoff" &&
        grep -Fq "APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires DSA evidence completion"
    else
        error "Final App Store signoff: Does not require DSA evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_DSA_TRADER_STATUS.md"' "$audit_script" &&
        grep -Fq "require_dsa_evidence_if_confirmed" "$audit_script"; then
        pass "Final audit: Requires DSA evidence when APP_STORE_DSA_TRADER_STATUS_CONFIRMED is true"
    else
        error "Final audit: Does not require DSA evidence when APP_STORE_DSA_TRADER_STATUS_CONFIRMED is true"
    fi
}

validate_app_store_pricing_availability_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}💶 App Store Pricing And Availability Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store pricing and availability evidence: Missing at docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store pricing and availability evidence" 1000 18000

	    local required_phrases=(
	        "APP_STORE_PRICING_AVAILABILITY_EVIDENCE_COMPLETE=false"
	        "## Apple Source Baseline"
	        "Last checked: 2026-05-28"
	        "pricing and availability properties determine where and when an app is available on the App Store and at what price"
	        "must set pricing for an app before submitting it for review"
	        "paid content on the App Store requires the membership Account Holder to accept the Paid Apps Agreement"
	        "if the most recent Paid Apps Agreement has not been accepted, the app can only be offered for free"
	        "required role for setting a price is Account Holder, Admin, or App Manager"
	        "choose from up to 800 price points by default"
	        "periodically updates prices in some regions based on tax and foreign-exchange changes"
	        "base country or region is used to automatically generate prices across other storefronts and currencies"
	        "prices in the base country or region are not adjusted by Apple for taxes or foreign exchange changes"
	        "manually managed storefront pricing makes the developer responsible for staying current with taxes and exchange rates"
	        "price changes can be scheduled with a start date and end date"
	        "pre-orders are available only if the app has never been published on the App Store"
	        "availability is the set of storefronts where the app is available to purchase or download"
	        "availability must be set before App Review"
	        "175 countries or regions"
	        "All Countries or Regions"
	        "Specific Countries or Regions"
	        "future App Store countries or regions"
	        "pre-orders are no longer possible in that same location"
	        "previous customers may continue to receive updates and redownload from purchase history"
	        "availability changes take effect immediately but may require up to 24 hours"
	        "education and business distribution can include Apple Business Manager, Apple School Manager, education discounts, and custom app distribution"
	        "public apps are automatically available for volume purchase"
	        "50% education discount"
	        "private distribution restricts the app to selected businesses or organizations"
	        "unlisted apps require the app's availability to be public"
	        "changing distribution from public to private, or private to public, requires a new app record and binary"
	        "custom app distribution is available only before the app has been approved"
	        "tax category defaults to the App Store software category unless changed"
	        "last compatible version settings control which older app versions existing customers can download from iCloud"
        "## Required Pricing And Availability Review"
        "## Apple References"
        "## Evidence Commands"
        "## Local Free/No-IAP Scan Result"
        "## Evidence To Attach"
        "## Closure Rule"
        "Price"
        "Paid Apps Agreement"
        "Price schedule"
	        "Storefront availability"
	        "Pre-order"
	        "Distribution method"
	        "Education and business distribution"
	        "Tax category"
	        "Last compatible version"
        'StoreKit/IAP source scan returned `NO_MATCHES`'
        "does not currently sell premium app features"
        "does not currently declare StoreKit products"
        "real-world shared event expenses only"
        "free app with no In-App Purchase products"
        "no paid app sale"
        "Paid Apps Agreement should be recorded as not required"
        "c476fd4e818e2d78f7ece9005b8d2fdeb8e1dc3c50481cf12541409572f88ded"
        "c4cf0ba2a78955828e45c1b1d763b245ca94ef66391cd6513bcd29fd7cd276c2"
        "dd457aa7207cd7cb4743ac32aedcdf61d99d2e27a093b7d2ac9d9a38d6efdc1b"
        "APP_STORE_PRICING_AVAILABILITY_CONFIRMED=true"
        "APP_STORE_PRICING_AVAILABILITY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_PAYMENT_EVIDENCE.md"
        "docs/APP_STORE_DSA_TRADER_STATUS.md"
        "docs/APP_STORE_AVAILABILITY_EVIDENCE.md"
        "docs/APP_STORE_FINAL_SIGNOFF.md"
	        "app-pricing-and-availability"
	        "set-a-price"
	        "manage-availability-for-your-app-on-the-app-store"
	        "set-distribution-methods"
	        "submit-an-app"
	        "future-country checkbox state"
	        "Public, Private, or Unlisted distribution method decision"
	        "new-app-record or unlisted-link request implications"
	        "volume purchase"
	        "future-country availability intent"
	        "custom app/private distribution"
	        "unlisted-link"
	    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store pricing and availability evidence: Covers $phrase"
        else
            error "App Store pricing and availability evidence: Missing '$phrase'"
        fi
    done

    if rg -n "StoreKit|SKProduct|Product\\.products|InAppPurchase|purchase\\(" \
        "$PROJECT_DIR/iosApp/src" \
        "$PROJECT_DIR/shared/src/commonMain/kotlin" \
        "$PROJECT_DIR/server/src/main/kotlin" >/dev/null 2>&1; then
        error "App Store pricing and availability evidence: StoreKit/IAP source scan found matches; update pricing evidence"
    else
        pass "App Store pricing and availability evidence: StoreKit/IAP source scan has no matches"
    fi

    local payment_compliance="$PROJECT_DIR/docs/APP_STORE_PAYMENT_COMPLIANCE.md"
    local payment_evidence="$PROJECT_DIR/docs/APP_STORE_PAYMENT_EVIDENCE.md"
    local review_notes="$PROJECT_DIR/composeApp/metadata/ios/review_information/notes.txt"
    if [ -f "$payment_compliance" ] &&
        grep -Fq "does not currently sell premium app features" "$payment_compliance" &&
        grep -Fq "does not currently declare StoreKit products" "$payment_compliance" &&
        [ -f "$payment_evidence" ] &&
        grep -Fq "APP_STORE_PAYMENT_EVIDENCE_COMPLETE=false" "$payment_evidence" &&
        [ -f "$review_notes" ] &&
        grep -Fq "real-world shared event expenses only" "$review_notes" &&
        grep -Fq "They do not sell or unlock app features" "$review_notes"; then
        pass "App Store pricing and availability evidence: Payment policy and review notes support free/no-IAP positioning"
    else
        error "App Store pricing and availability evidence: Payment policy or review notes do not support free/no-IAP positioning"
    fi

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_PRICING_AVAILABILITY_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires pricing and availability evidence before APP_STORE_PRICING_AVAILABILITY_CONFIRMED"
    else
        error "App Store launch checklist: Does not require pricing and availability evidence before APP_STORE_PRICING_AVAILABILITY_CONFIRMED"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_PRICING_AVAILABILITY_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires pricing and availability evidence completion"
    else
        error "Final App Store signoff: Does not require pricing and availability evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_pricing_availability_evidence_if_confirmed" "$audit_script"; then
        pass "Final audit: Requires pricing and availability evidence when APP_STORE_PRICING_AVAILABILITY_CONFIRMED is true"
    else
        error "Final audit: Does not require pricing and availability evidence when APP_STORE_PRICING_AVAILABILITY_CONFIRMED is true"
    fi
}

validate_app_store_sdk_privacy_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}📦 App Store SDK Privacy And Signature Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store SDK privacy evidence: Missing at docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store SDK privacy evidence" 1000 18000

	    local required_phrases=(
	        "APP_STORE_SDK_PRIVACY_EVIDENCE_COMPLETE=false"
	        "## Apple Source Baseline"
	        "Last checked: 2026-05-28"
	        "responsible for all code the SDK includes in the app"
	        "data collection and use practices"
	        "privacy manifest files outline the privacy practices of third-party code"
	        "Xcode combines privacy manifests across all third-party SDKs"
	        "one report to help create accurate Privacy Nutrition Labels"
	        "SDK signatures let Xcode validate"
	        "signed by the same developer"
	        "software supply-chain integrity"
	        "listed SDKs included in new apps or app updates submitted through App Store Connect must include privacy manifests"
	        "Apple requires signatures when those listed SDKs are used as binary dependencies"
	        "any version of a listed SDK"
	        "SDKs that repackage listed SDKs"
	        "valid privacy manifest for commonly used third-party SDKs"
	        "inspect the signed \`.xcarchive\` or IPA"
        "## Required SDK Review"
        "## Apple References"
        "## Evidence Commands"
        "## Evidence To Attach"
        "## Closure Rule"
	        "SDK inventory"
	        "Apple listed SDKs"
	        "Repackaged listed SDKs"
	        "Privacy manifests"
	        "SDK signatures"
        "Xcode privacy report"
        "Required reason APIs"
        "Invalid manifest handling"
	        "Firebase"
	        "GoogleSignIn"
	        "GoogleUtilities"
	        "BoringSSL"
	        "Alamofire"
	        "Kingfisher"
	        "Lottie"
	        "OneSignal"
	        "RealmSwift"
	        "RxSwift"
	        "SnapKit"
	        "Starscream"
	        "PrivacyInfo.xcprivacy"
        "third-party-SDK-requirements"
        "privacy-manifest-files"
        "## Local Unsigned Release Scan Result"
        "Local Release app scanned"
        'Embedded app frameworks found: 1, `Frameworks/Shared.framework`'
        'Bundled privacy manifests found: 1, `Wakeve.app/PrivacyInfo.xcprivacy`'
        "Built privacy manifest hash: \`38dbda46a737beed9c54a65cf089159fbb2712de1c21b8c9cd5de6877acfbfc3\`"
        '`NSPrivacyTracking=false`'
        'file timestamps (`C617.1`) and UserDefaults (`CA92.1`)'
        '`otool -L` for the app binary and `Shared.framework/Shared`'
        "it does not show third-party binary SDK frameworks"
	        "no Firebase, GoogleSignIn, GoogleUtilities, AppAuth, BoringSSL, OpenSSL, Protobuf, nanopb, Alamofire, Realm, SDWebImage, Lottie, OneSignal, Kingfisher, RxSwift, SnapKit, Starscream, or Sentry binary artifacts"
	        "current iOS provider is local-only"
	        '`Shared.framework` is not signed'
	        'signed `.xcarchive` or IPA'
	        "export the Xcode privacy report"
	        "listed or repackaged listed SDK"
	        "evidence that Xcode validates the expected SDK developer"
	        "APP_STORE_SDK_PRIVACY_CONFIRMED=true"
        "APP_STORE_SDK_PRIVACY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_FINAL_SIGNOFF.md"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store SDK privacy evidence: Covers $phrase"
        else
            error "App Store SDK privacy evidence: Missing '$phrase'"
        fi
    done

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_SDK_PRIVACY_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires SDK privacy evidence before APP_STORE_SDK_PRIVACY_CONFIRMED"
    else
        error "App Store launch checklist: Does not require SDK privacy evidence before APP_STORE_SDK_PRIVACY_CONFIRMED"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_SDK_PRIVACY_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires SDK privacy evidence completion"
    else
        error "Final App Store signoff: Does not require SDK privacy evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_sdk_privacy_evidence_if_confirmed" "$audit_script"; then
        pass "Final audit: Requires SDK privacy evidence when APP_STORE_SDK_PRIVACY_CONFIRMED is true"
    else
        error "Final audit: Does not require SDK privacy evidence when APP_STORE_SDK_PRIVACY_CONFIRMED is true"
    fi
}

validate_app_store_release_control_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🚦 App Store Release Control Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store release control evidence: Missing at docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store release control evidence" 1000 16000

    local required_phrases=(
	        "APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=false"
	        "## Apple Source Baseline"
	        "Last checked: 2026-06-01"
	        "## Current 2026-06-01 Local Status"
	        "Recommended first-release setting: manual release in App Store Connect"
	        "do not submit automatically for App Review"
	        "do not request automatic public release from Fastlane"
	        "no phased release because phased release applies to eligible version updates"
	        "Fastlane upload guard remains local-only evidence"
	        "Current local hashes"
	        "8b999cea44fdab22a4d7e6b3bbc644249d71adb056752655fc1da1472a793a30"
	        "fcfca68b5212bab68d1db2e87608957b1774ed265e37c8721d3122f72326af32"
	        "f7a60958b100158fe689132edbdad3e5ddff60c778150955997aaa2902051cd9"
	        "manual release, automatic release after App Review approval, and automatic release no earlier than a specified date"
	        "if a version is released as a pre-order, the release option must be manual release"
	        "required role to choose an App Store version release option is Account Holder, Admin, or App Manager"
        "moves to Pending Developer Release"
        "explicitly choose Release This Version"
        "releasing an app version is platform-specific"
        "must be released separately"
        "email reminder if an app remains in Pending Developer Release for more than 30 days"
        "may take up to 24 hours to appear on the App Store"
        "release request sent through the App Store Connect API cannot be cancelled once sent"
	        "manual release can be cancelled from the version page before the release completes"
	        "releases over a 7-day period"
	        "paused for up to 30 total days"
	        "Release to All Users"
	        "random sample of users with automatic updates on eligible devices"
	        "does not notify those users of their participation"
	        "phased release percentages are 1%, 2%, 5%, 10%, 20%, 50%, and 100%"
	        "anyone can manually download a version update from the App Store during phased release"
	        "removing the app from sale stops phased release"
	        "phased release unavailable for that version again"
	        "Ready for Distribution version has a legal or usability issue"
        "it is not possible to revert to a previous version on the App Store"
        "## Release Decision"
        "## Apple References"
        "## Evidence To Attach"
	        "## Closure Rule"
	        "Release option"
	        "Pre-order interaction"
	        "Phased release"
        "Release owner"
        "Release timing"
        "Post-approval action"
        "Stop/pause criteria"
        "Store propagation"
        "Rollback path"
	        "manual release"
	        "pre-order countries"
	        "automatic release"
        "scheduled release"
        "Pending Developer Release"
        "Release This Version"
        "24 hours"
        "7-day"
        "30 total days"
        "30-day reminder handling"
        "Cancel This Release"
	        "1/2/5/10/20/50/100 percent exposure plan"
	        "automatic-update eligibility note"
	        "remove-from-sale consequence"
	        "Emergency rollback plan"
        "fix update"
        "removal from sale"
        "submit_for_review: false"
        "automatic_release: false"
        "## Local Upload Path Scan Result"
        "repository upload path is configured to keep App Review submission and public release manual"
        "Local scan date: 2026-06-01"
        "Fastlane \`upload_appstore\` requires \`APPLE_ID\`, \`ITC_TEAM_ID\`, \`TEAM_ID\`, \`APPLE_TEAM_ID\`"
        "Fastlane \`upload_appstore\` runs \`run_final_app_store_submission_audit\`, then \`preflight(strict: true, live_urls: true)\`, then \`build\` before \`deliver\`"
        "Fastlane \`deliver\` is configured with \`submit_for_review: false\`"
        "Fastlane \`deliver\` is configured with \`automatic_release: false\`"
        "submit manually from App Store Connect"
        "submission mode remains manual"
        "APP_STORE_RELEASE_CONTROL_CONFIRMED=false"
        "final audit requires \`APP_STORE_RELEASE_CONTROL_CONFIRMED=true\` plus \`APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=true\`"
        "local scan does not close AS-19"
        "APP_STORE_RELEASE_CONTROL_CONFIRMED=true"
	        "APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=true"
	        "Manual release is selected if any pre-order is configured"
	        "docs/APP_STORE_FINAL_SIGNOFF.md"
        "select-an-app-store-version-release-option"
        "release-a-version-update-in-phases"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store release control evidence: Covers $phrase"
        else
            error "App Store release control evidence: Missing '$phrase'"
        fi
    done

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires release control evidence before APP_STORE_RELEASE_CONTROL_CONFIRMED"
    else
        error "App Store launch checklist: Does not require release control evidence before APP_STORE_RELEASE_CONTROL_CONFIRMED"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires release control evidence completion"
    else
        error "Final App Store signoff: Does not require release control evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_release_control_evidence_if_confirmed" "$audit_script"; then
        pass "Final audit: Requires release control evidence when APP_STORE_RELEASE_CONTROL_CONFIRMED is true"
    else
        error "Final audit: Does not require release control evidence when APP_STORE_RELEASE_CONTROL_CONFIRMED is true"
    fi
}

validate_app_store_media_localization_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🖼️  App Store Media And Localization Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store media/localization evidence: Missing at docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store media/localization evidence" 1000 18000

    local required_phrases=(
        "APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-28"
        "minimum of one and a maximum of ten screenshots"
        "screenshots and app previews visually communicate the app's user experience"
        "requires iPad screenshots when the app runs on iPad"
        "Wakeve targets both iPhone and iPad"
        "up to three app previews per supported device size and language"
        "app previews always precede screenshots on iPhone and iPad"
        "iPhone 6.9-inch screenshot sizes include \`1320 x 2868\` portrait"
        "iPad 13-inch screenshot sizes include \`2048 x 2732\` portrait"
        "highest-resolution required screenshots can be provided and scaled down"
        "Media Manager can be used to provide specific screenshots and app previews"
        "15 to 30 seconds"
        "up to 500MB"
        "up to 30 frames per second"
        "accepted H.264 or ProRes 422 formats"
        "accepted extensions \`.mov\`, \`.m4v\`, and \`.mp4\`"
        "poster-frame decision"
        "localized app preview falls back to the next best available language"
        "Prepare for Submission, Ready for Review, Invalid Binary, Rejected, Metadata Rejected, or Developer Rejected"
        "after an app is submitted for review and approved, a new version is required to update screenshots"
        "language or country/region fallback"
        "meaning parity and no misleading claims"
        "## Required Review"
        "## Apple References"
        "## Evidence Commands"
        "## Evidence To Attach"
        "## Closure Rule"
        "Screenshot inventory"
        "Screenshot accuracy"
        "Screenshot ordering"
        "App preview decision"
        "Localization parity"
        "Metadata limits"
        "Device-family coverage"
        "Scaling/Media Manager decision"
        "Editable-status check"
        "Product-page consistency"
        "en-US"
        "fr-FR"
        "iPhone"
        "iPad"
        "one to ten screenshots"
        "app previews are optional"
        "15-30 second"
        "composeApp/screenshots/ios"
        "composeApp/metadata/ios"
        "## Local Media And Metadata Scan Result"
        "Local media scan date: 2026-06-13"
        "docs/app-store-media-localization/media-localization-2026-06-13T12-23-27Z.md"
        "./scripts/audit-app-store-media-localization.sh --fail-on-findings"
        "./scripts/audit-ios-localization-parity.sh --write-report --fail-on-findings"
        "local Fastlane media and localized metadata are structurally ready for upload"
        'Locales present: `en-US` and `fr-FR`'
        "App Store metadata/screenshots remain EN/FR only for the first release"
        "source app strings are audited separately for EN, FR, ES, IT, and PT"
        '`composeApp/screenshots/ios/en-US` and `composeApp/screenshots/ios/fr-FR`'
        '`composeApp/metadata/ios/en-US/screenshots` and `composeApp/metadata/ios/fr-FR/screenshots`'
        "Screenshot inventory: 8 PNG files"
        'every `01-iphone-home.png` is `1320x2868`'
        'every `02-ipad-home.png` is `2048x2732`'
        "each locale has one iPhone screenshot and one iPad screenshot"
        "Screenshot sizes match Apple-accepted iPhone 6.9-inch portrait \`1320 x 2868\` and iPad 13-inch portrait \`2048 x 2732\` sizes"
        "Screenshot set hash: \`e1d72a791111bc43b561e7b463043167e860a47f3c290443c0a015f64ef3effe\`"
        "App preview videos: 0"
        "App preview decision: omit previews for the first release; no preview localization fallback is relied on"
        "Standalone media/localization audit"
        '`bundle exec fastlane ios validate_metadata` passed locally'
        "Local field lengths are within App Store limits"
        '`en-US` name 6 chars, subtitle 24, description 1540, keywords 70, promotional text 85, release notes 179'
        '`fr-FR` name 6 chars, subtitle 29, description 1987, keywords 88, promotional text 109, release notes 196'
        "Both locales use \`https://wakeve.app/privacy\` and \`https://wakeve.app/support\`"
        "iOS app source string parity was refreshed locally on 2026-06-13"
        "docs/a11y/ios-localization-parity-2026-06-13T13-48-19Z.md"
        "EN, FR, ES, IT, and PT \`Localizable.strings\` each 848 keys"
        "0 duplicate keys"
        "0 missing/extra keys versus EN"
        "local scan does not close AS-20"
        "record whether scaled screenshots or Media Manager overrides are used"
        "status still permits media edits or that a new version is being edited"
        "App Store Connect status at the time of media review"
        "Scaling or Media Manager decision for every enabled device family and locale"
        "Screenshot/app-preview edits are made only while the app version status permits edits"
        "Any reliance on scaled screenshots or Media Manager overrides is explicitly recorded"
        "APP_STORE_MEDIA_LOCALIZATION_CONFIRMED=true"
        "APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_FINAL_SIGNOFF.md"
        "screenshot-specifications"
        "app-preview-specifications"
        "localize-app-information"
        "max file size, duration, frame rate, accepted extension, format, poster frame, device capture source, language fallback, and rights"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store media/localization evidence: Covers $phrase"
        else
            error "App Store media/localization evidence: Missing '$phrase'"
        fi
    done

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires media/localization evidence before APP_STORE_MEDIA_LOCALIZATION_CONFIRMED"
    else
        error "App Store launch checklist: Does not require media/localization evidence before APP_STORE_MEDIA_LOCALIZATION_CONFIRMED"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires media/localization evidence completion"
    else
        error "Final App Store signoff: Does not require media/localization evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_media_localization_evidence_if_confirmed" "$audit_script"; then
        pass "Final audit: Requires media/localization evidence when APP_STORE_MEDIA_LOCALIZATION_CONFIRMED is true"
    else
        error "Final audit: Does not require media/localization evidence when APP_STORE_MEDIA_LOCALIZATION_CONFIRMED is true"
    fi
}

validate_app_store_payment_compliance() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}💳 App Store Payment Compliance${NC}"

    local payment_file="$PROJECT_DIR/docs/APP_STORE_PAYMENT_COMPLIANCE.md"
    if [ ! -f "$payment_file" ]; then
        error "App Store payment compliance: Missing at docs/APP_STORE_PAYMENT_COMPLIANCE.md"
        return 1
    fi

    validate_text_file "$payment_file" "App Store payment compliance" 800 12000

    local required_phrases=(
        "APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED"
        "APP_STORE_PAYMENT_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_PAYMENT_EVIDENCE.md"
        "Apple Source Baseline"
        "App Review Guideline 3.1.1"
        "unlock features, functionality, subscriptions"
        "license keys, QR codes, cryptocurrencies"
        "App Review Guideline 3.1.1(a)"
        "StoreKit External Purchase Link entitlement"
        "United States storefront apps do not require those entitlements"
        "limited to the iOS or iPadOS App Store in specific storefronts"
        "App Review Guideline 3.1.3"
        "alternative purchase method"
        "App Review Guideline 3.1.3(b)"
        "also available as in-app purchases"
        "App Review Guideline 3.1.3(d)"
        "real-time person-to-person services"
        "App Review Guideline 3.1.3(e)"
        "physical goods or services consumed outside the app"
        "external purchase APIs and entitlements are for qualifying apps only"
        "App Store Connect App Review information"
        "App Store Connect in-app purchase information"
        "real-world event expenses"
        "external payment handoff as Tricount-only"
        "arbitrary external payment providers are rejected"
        "No external payment flow unlocks app features"
        "digital goods"
        "App Review notes"
        "Tricount/provider URLs are HTTPS-only"
        "trusted-domain validated"
        "Payment-pot links are restricted to trusted Tricount hosts"
        "Storefront-specific external-purchase-link behavior is not used"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fqi "$phrase" "$payment_file"; then
            pass "App Store payment compliance: Covers $phrase"
        else
            error "App Store payment compliance: Missing $phrase"
        fi
    done
}

validate_app_store_payment_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}💳 App Store Payment Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_PAYMENT_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store payment evidence: Missing at docs/APP_STORE_PAYMENT_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store payment evidence" 1000 20000

    local required_phrases=(
        "APP_STORE_PAYMENT_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-28"
        "may not use their own mechanisms to unlock content or functionality"
        "apply for entitlements to link to their own website to purchase digital content or services"
        "not required for United States storefront apps"
        "StoreKit External Purchase Link Entitlements are limited to specific iOS and iPadOS App Store storefronts"
        "non-US storefront apps and metadata may not include buttons, external links, or other calls to action"
        "may allow access to content, subscriptions, or features acquired elsewhere only if those items are also available as in-app purchases within the app"
        "real-time person-to-person services between two individuals may use purchase methods other than in-app purchase"
        "one-to-few and one-to-many real-time services must use in-app purchase"
        "physical goods or services consumed outside the app must use purchase methods other than in-app purchase"
        "free stand-alone companion apps to paid web-based tools do not need in-app purchase"
        "no purchasing inside the app or calls to action for purchase outside the app"
        "advertising management apps may use purchase methods other than in-app purchase"
        "digital purchases for content experienced or consumed in the app must use in-app purchase"
        "if any configured in-app purchase items cannot be found or reviewed in the app"
        "## Build Under Review"
        "## Review Scope"
        "## Evidence Commands"
        "## Local Payment Scan Result"
        "## Closure Rule"
        "docs/APP_STORE_TESTFLIGHT_EVIDENCE.md"
        "docs/APP_STORE_PAYMENT_COMPLIANCE.md"
        "composeApp/metadata/ios/review_information/notes.txt"
        "Payment pots"
        "Settlement suggestions"
        "Tricount handoff"
        "Provider scope"
        "Storefront-specific external purchase links"
        "StoreKit/IAP"
        "real-world shared-event expenses"
        "No external payment flow unlocks app features"
        "Tricount/provider URLs are HTTPS-only"
        "trusted-domain validated"
        "App Review notes"
        "rg -n \"StoreKit|SKProduct|SKProductsRequest|SKPayment|Product\\\\.products|InAppPurchase|purchase\\\\(|paywall\""
        "rg -n \"PaymentPotRepository|TricountHandoffRepository|PaymentRoutes|paymentRoutes|tricountGroupUrl|providerUrl|isTrustedPaymentLink|isTrustedProviderUrl\""
        "./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.organization.EventOrganizationPhase5ReadinessTest'"
        'StoreKit/IAP scan: `NO_MATCHES`'
        "SKProductsRequest"
        "SKPayment"
        "paywall"
        "Payment surface scan: payment surfaces are present"
        "Trusted link scan:"
        "require HTTPS Tricount hosts"
        "reject non-Tricount providers when a payment-pot URL is present"
        "reject template markers"
        "Regression test:"
        "non-Tricount external provider links"
        "App Review notes/policy scan:"
        "local pre-submission evidence only"
        "uploaded TestFlight/App Review build is inspected"
        "Payment-pot URLs remain restricted to trusted Tricount hosts"
        "non-Tricount external-provider rejection"
        "APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=true"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store payment evidence: Covers $phrase"
        else
            error "App Store payment evidence: Missing '$phrase'"
        fi
    done

    local payment_repo="$PROJECT_DIR/shared/src/commonMain/kotlin/com/guyghost/wakeve/payment/PaymentPotRepository.kt"
    if [ -f "$payment_repo" ] &&
        grep -Fq 'if (!provider.equals("TRICOUNT", ignoreCase = true)) return false' "$payment_repo" &&
        grep -Fq 'host == "tricount.com" || host == "www.tricount.com" || host.endsWith(".tricount.com")' "$payment_repo"; then
        pass "App Store payment evidence: PaymentPotRepository restricts payment-pot URLs to trusted Tricount hosts"
    else
        error "App Store payment evidence: PaymentPotRepository does not restrict payment-pot URLs to trusted Tricount hosts"
    fi

    local payment_test="$PROJECT_DIR/shared/src/jvmTest/kotlin/com/guyghost/wakeve/organization/EventOrganizationPhase5ReadinessTest.kt"
    if [ -f "$payment_test" ] &&
        grep -Fq "Phase5 payment pot rejects non Tricount external provider links" "$payment_test" &&
        grep -Fq "assertFailsWith<IllegalArgumentException>" "$payment_test"; then
        pass "App Store payment evidence: Regression test rejects non-Tricount payment-pot provider links"
    else
        error "App Store payment evidence: Missing regression test for non-Tricount payment-pot provider links"
    fi

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_PAYMENT_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_PAYMENT_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires payment evidence before APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED"
    else
        error "App Store launch checklist: Does not require payment evidence before APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_PAYMENT_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_PAYMENT_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires payment evidence completion"
    else
        error "Final App Store signoff: Does not require payment evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_PAYMENT_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_payment_evidence_if_confirmed" "$audit_script"; then
        pass "Final audit: Requires payment evidence when APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED is true"
    else
        error "Final audit: Does not require payment evidence when APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED is true"
    fi
}

validate_app_store_accessibility_labels() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}♿ App Store Accessibility Labels${NC}"

    local labels_file="$PROJECT_DIR/docs/APP_STORE_ACCESSIBILITY_LABELS.md"
    if [ ! -f "$labels_file" ]; then
        error "Accessibility labels draft: Missing at docs/APP_STORE_ACCESSIBILITY_LABELS.md"
        return 1
    fi

    validate_text_file "$labels_file" "Accessibility labels draft" 500 20000

    local required_sections=(
        "## Apple Source Baseline"
        "## Device Scope"
        "## Recommended App Store Connect Answers"
        "### iPhone"
        "### iPad"
        "### Mac with Apple Silicon"
        "### Apple Vision Pro"
        "## Before Publishing Labels"
    )

    local section
    for section in "${required_sections[@]}"; do
        if grep -Fq "$section" "$labels_file"; then
            pass "Accessibility labels draft: Contains $section"
        else
            error "Accessibility labels draft: Missing $section"
        fi
    done

    local required_baseline_phrases=(
        "Apple-source review date: 2026-05-27"
        "Accessibility Nutrition Labels help users learn whether an app will be accessible"
        "specific to the device type used to view the page"
        "iOS 26, iPadOS 26, macOS 26, tvOS 26, visionOS 26, and watchOS 26"
        "voluntary to start"
        "required to share accessibility support details to submit new apps and app updates"
        "support has not yet been indicated"
        "review the evaluation criteria before indicating support"
        "testing matrix for each device"
        "common tasks include primary app functionality plus first launch, login, purchase, and settings"
        "complete all common tasks using an accessibility feature"
        "not available on some devices"
        "saved as a draft until published"
        "cannot be unpublished"
        "can be updated at any time"
        "optional accessibility URL"
        "intentionally misleading or harmful Accessibility Nutrition Labels"
        "keeping accessibility responses accurate and up to date"
        "available on Apple Vision Pro unless availability is edited"
        "available on Macs with Apple silicon unless availability is edited"
    )

    local baseline_phrase
    for baseline_phrase in "${required_baseline_phrases[@]}"; do
        if grep -Fq "$baseline_phrase" "$labels_file"; then
            pass "Accessibility labels draft: Covers $baseline_phrase"
        else
            error "Accessibility labels draft: Missing baseline phrase '$baseline_phrase'"
        fi
    done

    local required_features=(
        "Dark Interface"
        "Larger Text / Adjustable Text Size"
        "VoiceOver"
        "Voice Control"
        "Sufficient Contrast"
        "Reduced Motion"
        "Differentiate without Color Alone"
        "Captions"
        "Audio Descriptions"
    )

    local feature
    for feature in "${required_features[@]}"; do
        if grep -Fq "$feature" "$labels_file"; then
            pass "Accessibility labels draft: Covers $feature"
        else
            error "Accessibility labels draft: Missing feature $feature"
        fi
    done

    local conservative_claims=(
        "Do not publish these labels until the TestFlight smoke tests"
        "docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md"
        "APP_STORE_ACCESSIBILITY_EVIDENCE_COMPLETE=true"
        "APP_STORE_ACCESSIBILITY_SIGNOFF=true"
        "Larger Text / Adjustable Text Size | Do not claim yet"
        "VoiceOver | Do not claim yet"
        "Voice Control | Do not claim yet"
        "Sufficient Contrast | Do not claim yet"
        "Reduced Motion | Do not claim yet"
        "Differentiate without Color Alone | Do not claim yet"
        "Do not publish iPad accessibility labels until"
        "Do not claim accessibility support for Mac with Apple silicon in the first release"
        "Do not claim accessibility support for Apple Vision Pro in the first release"
    )

    local claim
    for claim in "${conservative_claims[@]}"; do
        if grep -Fq "$claim" "$labels_file"; then
            pass "Accessibility labels draft: Conservative guard present"
        else
            error "Accessibility labels draft: Missing conservative guard '$claim'"
        fi
    done
}

validate_app_store_accessibility_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}♿ App Store Accessibility Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store accessibility evidence: Missing at docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store accessibility evidence" 1000 22000

    local required_phrases=(
        "APP_STORE_ACCESSIBILITY_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-27"
        "Accessibility Nutrition Labels help users learn whether an app will be accessible before download"
        "Accessibility Nutrition Labels appear on the App Store product page"
        "specific to the device type used to view the product page"
        "voluntary to start"
        "required to share accessibility support details to submit new apps and app updates"
        "support has not yet been indicated"
        "audit the app before providing responses"
        "assess each supported device separately"
        "complete all common tasks of the app using an accessibility feature"
        "common tasks include primary app functionality plus first launch, login, purchase, and settings"
        "App Review can contact developers to update Accessibility Nutrition Labels"
        "responsible for keeping accessibility responses accurate and up to date"
        "optional accessibility URL"
        "iPhone and iPad labels are shown on Apple Vision Pro and Mac App Store"
        "## Build And Label Scope"
        "## Device Matrix"
        "## Feature Evidence"
        "## Local Debug Simulator Evidence"
        "## Evidence Commands And Checks"
        "## Closure Rule"
        "docs/APP_STORE_ACCESSIBILITY_LABELS.md"
        "docs/APP_STORE_TESTFLIGHT_EVIDENCE.md"
        "docs/ACCESSIBILITY_AUDIT.md"
        "docs/a11y/ACCESSIBILITY_AUDIT_iOS.md"
        "Accessibility Nutrition Labels"
        "leave labels unpublished"
        "iPhone"
        "iPad"
        "Mac with Apple silicon"
        "Apple Vision Pro"
        "Dark Interface"
        "Larger Text / Adjustable Text Size"
        "VoiceOver"
        "Voice Control"
        "Sufficient Contrast"
        "Reduced Motion"
        "Differentiate without Color Alone"
        "Captions"
        "Audio Descriptions"
        "Partial local evidence"
        "TestFlight iPhone and iPad evidence"
        "Dynamic Type at large accessibility sizes"
        "VoiceOver traversal"
        "./scripts/lint-store-metadata.sh --ios-only"
        "APP_REVIEW_PHONE_NUMBER=<APP_REVIEW_PHONE_NUMBER> ./scripts/lint-store-metadata.sh --ios-only"
        "APP_STORE_ACCESSIBILITY_SIGNOFF=true"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store accessibility evidence: Covers $phrase"
        else
            error "App Store accessibility evidence: Missing '$phrase'"
        fi
    done

    local required_evidence_files=(
        "docs/app-store-evidence/xcodebuildmcp-iphone-onboarding-events-2026-05-27.jpg"
        "docs/app-store-evidence/xcodebuildmcp-iphone-onboarding-collaboration-2026-05-27.jpg"
        "docs/app-store-evidence/xcodebuildmcp-iphone-login-2026-05-27.jpg"
        "docs/app-store-evidence/xcodebuildmcp-iphone-post-login-home-2026-05-27.jpg"
        "docs/app-store-evidence/xcodebuildmcp-iphone-create-event-2026-05-27.jpg"
        "docs/app-store-evidence/xcodebuildmcp-ipad-login-2026-05-27.jpg"
    )

    local evidence_path
    for evidence_path in "${required_evidence_files[@]}"; do
        if grep -Fq "$evidence_path" "$evidence_file" && [ -s "$PROJECT_DIR/$evidence_path" ]; then
            pass "App Store accessibility evidence: References existing local evidence $evidence_path"
        else
            error "App Store accessibility evidence: Missing or empty local evidence reference $evidence_path"
        fi
    done

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_ACCESSIBILITY_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires accessibility evidence before APP_STORE_ACCESSIBILITY_SIGNOFF"
    else
        error "App Store launch checklist: Does not require accessibility evidence before APP_STORE_ACCESSIBILITY_SIGNOFF"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_ACCESSIBILITY_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires accessibility evidence completion"
    else
        error "Final App Store signoff: Does not require accessibility evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_accessibility_evidence_if_confirmed" "$audit_script"; then
        pass "Final audit: Requires accessibility evidence when APP_STORE_ACCESSIBILITY_SIGNOFF is true"
    else
        error "Final audit: Does not require accessibility evidence when APP_STORE_ACCESSIBILITY_SIGNOFF is true"
    fi
}

validate_app_store_launch_checklist() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🧪 App Store Launch Checklist${NC}"

    local checklist_file="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ ! -f "$checklist_file" ]; then
        error "App Store launch checklist: Missing at docs/APP_STORE_LAUNCH_CHECKLIST.md"
        return 1
    fi

    validate_text_file "$checklist_file" "App Store launch checklist" 1000 24000

    local required_sections=(
        "## Apple Source Baseline"
        "## Entry Criteria"
        "## TestFlight Internal Smoke Test"
        "## Monitoring During TestFlight"
        "## App Review Submission"
        "## Rollback Plan"
        "## Exit Criteria"
    )

    local section
    for section in "${required_sections[@]}"; do
        if grep -Fq "$section" "$checklist_file"; then
            pass "App Store launch checklist: Contains $section"
        else
            error "App Store launch checklist: Missing $section"
        fi
    done

    local required_baseline_phrases=(
        "Apple-source review date: 2026-05-28"
        "TestFlight builds can be tested for up to 90 days"
        "application identifiers within provisioning profiles"
        "up to 100 App Store Connect users"
        "up to 10,000 people"
        "may require App Review"
        "first build added to an external tester group is sent to App Review"
        "App Review Guidelines compliance"
        "install the TestFlight app"
        "send feedback, and get updates"
        "build status and metrics include sessions and crashes"
        "screenshots, crash-related comments, and general comments"
        "crash reports from TestFlight feedback are available for download for 120 days"
        "crash report may be missing if the app becomes unresponsive"
        "build can be expired to stop testing"
        "expired builds no longer allow internal or external testers to install it"
        "intended for public distribution and comply with App Review Guidelines"
        "final versions with all necessary metadata and fully functional URLs"
        "tested on device for crashes, bugs, and stability"
        "App Review contact information should be current"
        "full review access should be provided"
        "backend services should be live during review"
    )

    local baseline_phrase
    for baseline_phrase in "${required_baseline_phrases[@]}"; do
        if grep -Fq "$baseline_phrase" "$checklist_file"; then
            pass "App Store launch checklist: Baseline covers $baseline_phrase"
        else
            error "App Store launch checklist: Missing baseline phrase '$baseline_phrase'"
        fi
    done

    local required_entry_criteria=(
        "bundle exec fastlane ios preflight"
        "bundle exec fastlane ios preflight strict:true live_urls:true"
        "bundle exec fastlane ios submission_ready"
        "bundle exec fastlane ios validate_ipa_entitlements ipa:build/ios/WakeveApp.ipa"
        "APP_REVIEW_PHONE_NUMBER"
        "https://wakeve.app/privacy"
        "/terms"
        "/support"
        "/.well-known/apple-app-site-association"
        "/apple-app-site-association"
        "https://api.wakeve.app/health"
        "docs/APP_STORE_PRIVACY_LABELS.md"
        "docs/APP_STORE_AVAILABILITY_DECISIONS.md"
        "docs/APP_STORE_DSA_TRADER_STATUS.md"
        "openspec/changes/add-in-app-account-deletion/"
        "openspec/changes/add-ugc-moderation-controls/"
        "docs/APP_STORE_ACCESSIBILITY_LABELS.md"
        "APP_STORE_FINAL_SIGNOFF_COMPLETE=false"
    )

    local item
    for item in "${required_entry_criteria[@]}"; do
        if grep -Fq "$item" "$checklist_file"; then
            pass "App Store launch checklist: Entry criteria cover $item"
        else
            error "App Store launch checklist: Entry criteria missing $item"
        fi
    done

    local required_smoke_items=(
        "recent iPhone"
        "supported iPad"
        "Install from TestFlight"
        "docs/APP_STORE_TESTFLIGHT_EVIDENCE.md"
        "TESTFLIGHT_SMOKE_EVIDENCE_COMPLETE=true"
        "Guest login"
        "Privacy Policy and Terms"
        "Create a draft event offline"
        "start a poll"
        "Vote YES/MAYBE/NO"
        "Confirm a date"
        "Add to calendar"
        "Notification permission"
        "Universal Link smoke test"
        "wakeve://event/<id>"
        "Delete Account"
        "comments/chat"
        "filtering/report/block"
        "Airplane mode"
        "Force quit and relaunch"
        "Dark mode"
        "Dynamic Type"
        "VoiceOver"
    )

    for item in "${required_smoke_items[@]}"; do
        if grep -Fqi "$item" "$checklist_file"; then
            pass "App Store launch checklist: Smoke test covers $item"
        else
            error "App Store launch checklist: Smoke test missing $item"
        fi
    done

    local required_monitoring_items=(
        "at least 24 hours"
        'Backend `/health` availability'
        "API 4xx/5xx rate"
        "Authentication failure rate"
        "Event creation success rate"
        "Poll vote success rate"
        "Calendar integration failures"
        "Push token registration failures"
        "App crashes"
        "dSYM"
        "symbolicated"
        "support@wakeve.app"
        "Universal Links fail"
        "Privacy/support URLs or AASA become unreachable"
    )

    for item in "${required_monitoring_items[@]}"; do
        if grep -Fqi "$item" "$checklist_file"; then
            pass "App Store launch checklist: Monitoring covers $item"
        else
            error "App Store launch checklist: Monitoring missing $item"
        fi
    done

    local required_submission_items=(
        "APP_STORE_PRIVACY_SIGNOFF=true"
        "APP_STORE_PRIVACY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_PRIVACY_EVIDENCE.md"
        "APP_STORE_ACCESSIBILITY_SIGNOFF=true"
        "APP_STORE_ACCESSIBILITY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md"
        "APP_STORE_AVAILABILITY_CONFIRMED=true"
        "APP_STORE_AVAILABILITY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_AVAILABILITY_EVIDENCE.md"
        "APP_STORE_DSA_TRADER_STATUS_CONFIRMED=true"
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
        "APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md"
        "APP_STORE_UGC_MODERATION_CONFIRMED=true"
        "APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_UGC_MODERATION_EVIDENCE.md"
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
        "APP_STORE_ACCOUNT_ACCESS_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md"
        "APP_STORE_APP_INFORMATION_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_APP_INFORMATION_EVIDENCE.md"
        "APP_STORE_VERSIONING_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_VERSIONING_EVIDENCE.md"
        "APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md"
        "APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md"
        "APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md"
        "APP_STORE_REVIEW_ACCESS_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md"
        'APP_REVIEW_PHONE_NUMBER="$APP_REVIEW_PHONE_NUMBER"'
        "bundle exec fastlane ios upload_appstore"
        "submit_for_review: false"
        "Submit manually"
        "account deletion flow is reachable"
        "comments/chat moderation evidence"
        "export compliance"
        "Push Notifications, Siri, Sign in with Apple, and Associated Domains"
    )

    for item in "${required_submission_items[@]}"; do
        if grep -Fq "$item" "$checklist_file"; then
            pass "App Store launch checklist: Submission covers $item"
        else
            error "App Store launch checklist: Submission missing $item"
        fi
    done

    local required_rollback_items=(
        "Stop external distribution"
        'increment `CFBundleVersion`'
        "manual release selected"
        "Reject the release"
        "Disable affected backend behavior server-side"
        '`wakeve.app` legal/support/AASA endpoints online'
        "expedited fix"
    )

    for item in "${required_rollback_items[@]}"; do
        if grep -Fqi "$item" "$checklist_file"; then
            pass "App Store launch checklist: Rollback covers $item"
        else
            error "App Store launch checklist: Rollback missing $item"
        fi
    done
}

validate_app_store_testflight_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🧪 App Store TestFlight Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_TESTFLIGHT_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store TestFlight evidence: Missing at docs/APP_STORE_TESTFLIGHT_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store TestFlight evidence" 1000 20000

    local required_phrases=(
        "TESTFLIGHT_SMOKE_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-06-01"
        "TestFlight lets developers distribute beta builds, manage beta testers, and collect feedback"
        "continue distributing builds until all issues are resolved before submitting the app for review"
        "TestFlight builds can be tested for up to 90 days"
        "TestFlight builds must include application identifiers within the provisioning profiles"
        "up to 10,000 external testers"
        "up to 100 App Store Connect users with access to the app"
        "external testers may require TestFlight App Review"
        "first build added to a group is sent to App Review"
        "external testing requires TestFlight test information"
        "beta app description and feedback email"
        "required role for providing TestFlight test information is Account Holder, Admin, App Manager, Developer, or Marketing"
        "TestFlight Internal Only can only be added to internal tester groups"
        "cannot be submitted for external testing or to customers"
        "feedback from TestFlight 2.3 or later appears in App Store Connect"
        "crash reports for TestFlight-distributed apps can be opened in Xcode"
        "tester metrics are only available after testers install the app"
        "can take up to 24 hours to appear in App Store Connect"
        "TestFlight feedback can include screenshots, crash-related comments, and general comments"
        "TestFlight crash reports are available for download for 120 days"
        "a crash report may be missing when the app becomes unresponsive"
        "## Current 2026-06-01 Local Status"
        "No uploaded TestFlight build is recorded"
        'No signed IPA, signed `.xcarchive`, App Store Connect build number, or TestFlight install evidence'
        "local unsigned Release build and dSYM evidence do not replace TestFlight installation"
        "live DNS/AASA checks currently fail"
        "AS-09 account deletion and AS-10 UGC moderation are locally implemented, but they remain review-build evidence blockers"
        "validated Apple source links were refreshed on 2026-06-01"
        "## Build Under Test"
        "## Device Matrix"
        "## Smoke Checklist Evidence"
        "## Monitoring Window"
        "## Closure Rule"
        "App Store Connect version"
        "Build number"
        "Release commit"
        "bundle exec fastlane ios validate_ipa_entitlements ipa:build/ios/WakeveApp.ipa"
        "bundle exec fastlane ios submission_ready"
        "Install from TestFlight, not Xcode"
        "iPhone"
        "iPad"
        "Guest login"
        "Privacy Policy and Terms"
        "Create a draft event offline"
        "vote YES/MAYBE/NO"
        "Universal Links"
        "Delete Account"
        "UGC moderation"
        "Dynamic Type"
        "VoiceOver"
        "at least 24 hours"
        'Backend `/health` availability'
        "API 4xx/5xx rate by endpoint"
        "App crashes"
        "dSYM"
        "symbolicated"
        "support@wakeve.app"
        "APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=true"
        "docs/APP_STORE_OBSERVABILITY_EVIDENCE.md"
        "TESTFLIGHT_SMOKE_PASSED=true"
        "## Apple References"
        "testflight-overview"
        "view-tester-feedback"
        "acquiring-crash-reports-and-diagnostic-logs"
        "building-your-app-to-include-debugging-information"
        "view-builds-and-metadata"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store TestFlight evidence: Covers $phrase"
        else
            error "App Store TestFlight evidence: Missing '$phrase'"
        fi
    done

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_TESTFLIGHT_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "TESTFLIGHT_SMOKE_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires TestFlight evidence record before TESTFLIGHT_SMOKE_PASSED"
    else
        error "App Store launch checklist: Does not require TestFlight evidence record before TESTFLIGHT_SMOKE_PASSED"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_TESTFLIGHT_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "TESTFLIGHT_SMOKE_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires TestFlight evidence completion"
    else
        error "Final App Store signoff: Does not require TestFlight evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_TESTFLIGHT_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_testflight_evidence_if_confirmed" "$audit_script"; then
        pass "Final audit: Requires TestFlight evidence when TESTFLIGHT_SMOKE_PASSED is true"
    else
        error "Final audit: Does not require TestFlight evidence when TESTFLIGHT_SMOKE_PASSED is true"
    fi
}

validate_app_store_observability_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}📈 App Store Observability Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_OBSERVABILITY_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store observability evidence: Missing at docs/APP_STORE_OBSERVABILITY_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store observability evidence" 1000 18000

    local required_phrases=(
        "APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-06-01"
        "crash reports and diagnostic logs can be gathered from the App Store, TestFlight, and devices"
        "Crashes organizer shows crash reports for apps distributed with TestFlight or through the App Store"
        "TestFlight users automatically share crash logs with developers"
        "App Store crash reports require users to agree to share crash and usage data"
        "refresh crash reports for a selected app version and build"
        "top crash reports from the past two weeks"
        "possible delay of up to one day after first distribution"
        "uses the app, version number, and build string"
        "fully or partially symbolicated crash reports are needed"
        "unsymbolicated crash reports are rarely useful"
        "third-party crash reports may omit necessary information"
        "dSYM files can be used to symbolicate crash reports"
        "build UUIDs should be verified with \`dwarfdump\`"
        "symbols are included when uploading to App Store Connect"
        "automatically symbolicates logs"
        "correct dSYM files are available locally"
        "must retain the Xcode archive for each distributed build"
        "dSYM downloads are no longer available for submissions from Xcode 14 or later"
        "TestFlight feedback submitted by testers running TestFlight 2.3 or later appears in App Store Connect"
        "required role to view tester feedback is Account Holder, Admin, App Manager, Developer, or Marketing"
        "crash feedback can be filtered by platform, app version, build group, build, OS version, or device"
        "crash feedback downloads include the crash report and associated comments"
        "crash reports are available for download for 120 days"
        "crash report may not be included in feedback if the app becomes unresponsive"
        "uploaded build metadata can be inspected in App Store Connect"
        "Complete uploaded builds can still contain warnings"
        "Processing builds remaining over 24 hours"
        "## Required Evidence"
        "## Local Unsigned dSYM Scan Result"
        "## Evidence Commands"
        "## Closure Rule"
        "App Store Connect/TestFlight crashes"
        "dSYM"
        "symbolicated"
        "Xcode archive retention"
        "Build processing health"
        "TestFlight feedback"
        "Wakeve.app.dSYM"
        "1A770D94-184E-3238-B52E-9B272592D1AD"
        'local `CODE_SIGNING_ALLOWED=NO` Release build'
        "not an uploaded App Store Connect build"
        'Repository build scan found no `.ipa` or `.xcarchive` under `build/` at max depth 4'
        "code object is not signed at all"
        "https://api.wakeve.app/health"
        "API 4xx/5xx rate by endpoint"
        "Event creation"
        "poll voting"
        "calendar integration"
        "push token registration"
        "Universal Links"
        "support@wakeve.app"
        "docs/APP_STORE_PRIVACY_LABELS.md"
        "docs/APP_STORE_PRIVACY_EVIDENCE.md"
        "docs/APP_STORE_TESTFLIGHT_EVIDENCE.md"
        "Crash feedback download path and retention date before the 120-day availability window expires"
        "Build metadata screenshot or API/export showing Complete upload status"
        "Apple crash reports are checked before relying on third-party crash tooling"
        "The exact Xcode archive and dSYM/symbol evidence for the uploaded build are retained"
        "TestFlight feedback, including crash feedback, is reviewed or exported"
        "App Store Connect build upload status is Complete and delivery warnings/errors are reviewed"
        "## Apple References"
        "acquiring-crash-reports-and-diagnostic-logs"
        "building-your-app-to-include-debugging-information"
        "view-tester-feedback"
        "view-builds-and-metadata"
        "APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=true"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store observability evidence: Covers $phrase"
        else
            error "App Store observability evidence: Missing '$phrase'"
        fi
    done

    local release_app="$PROJECT_DIR/build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app"
    local release_dsym="$PROJECT_DIR/build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app.dSYM"
    if [ -d "$release_app" ] && [ -d "$release_dsym" ]; then
        pass "App Store observability evidence: Local unsigned Release app and dSYM exist"
        local app_uuid dsym_uuid
        app_uuid=$(dwarfdump --uuid "$release_app/Wakeve" 2>/dev/null | awk '{print $2}' | head -1)
        dsym_uuid=$(dwarfdump --uuid "$release_dsym" 2>/dev/null | awk '{print $2}' | head -1)
        if [ -n "$app_uuid" ] && [ "$app_uuid" = "$dsym_uuid" ]; then
            pass "App Store observability evidence: Local app and dSYM UUIDs match"
        else
            error "App Store observability evidence: Local app and dSYM UUIDs do not match"
        fi
    else
        warning "App Store observability evidence: Local unsigned Release app or dSYM not present"
    fi

    local local_release_artifact_count
    local_release_artifact_count=$(find "$PROJECT_DIR/build" -maxdepth 4 \( -name "*.ipa" -o -name "*.xcarchive" \) -print 2>/dev/null | wc -l | tr -d ' ')
    if [ "$local_release_artifact_count" = "0" ]; then
        pass "App Store observability evidence: No signed IPA/archive found in local build scan"
    else
        error "App Store observability evidence: Local IPA/archive needs signed-build observability record"
    fi

    if codesign -dv --verbose=2 "$release_app" >/tmp/wakeve-observability-codesign.log 2>&1; then
        error "App Store observability evidence: Local Release app is signed but evidence still describes unsigned scan"
    elif grep -Fq "code object is not signed at all" /tmp/wakeve-observability-codesign.log; then
        pass "App Store observability evidence: Local Release app unsigned limitation is current"
    else
        warning "App Store observability evidence: Could not confirm local Release app signing state"
    fi

    local testflight_file="$PROJECT_DIR/docs/APP_STORE_TESTFLIGHT_EVIDENCE.md"
    if [ -f "$testflight_file" ] &&
        grep -Fq "docs/APP_STORE_OBSERVABILITY_EVIDENCE.md" "$testflight_file" &&
        grep -Fq "APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=true" "$testflight_file"; then
        pass "App Store TestFlight evidence: Requires observability evidence completion"
    else
        error "App Store TestFlight evidence: Does not require observability evidence completion"
    fi

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_OBSERVABILITY_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires observability evidence before final submission"
    else
        error "App Store launch checklist: Does not require observability evidence before final submission"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_OBSERVABILITY_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires observability evidence completion"
    else
        error "Final App Store signoff: Does not require observability evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_OBSERVABILITY_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_observability_evidence_if_testflight_confirmed" "$audit_script"; then
        pass "Final audit: Requires observability evidence when TESTFLIGHT_SMOKE_PASSED is true"
    else
        error "Final audit: Does not require observability evidence when TESTFLIGHT_SMOKE_PASSED is true"
    fi
}

validate_app_store_live_url_aasa_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🌐 App Store Live URL And AASA Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store live URL and AASA evidence: Missing at docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store live URL and AASA evidence" 1000 18000

    local required_phrases=(
        "APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-06-01"
        "associated domains require both an associated domain file on the website and a matching Associated Domains entitlement in the app"
        "apps listed in the \`apple-app-site-association\` file must match the Associated Domains entitlement"
        "each subdomain requires its own Associated Domains entitlement entry and its own \`apple-app-site-association\` file"
        "associated domain file must be named \`apple-app-site-association\` without an extension"
        "placed in the site's \`.well-known\` directory"
        "hosted with \`https://\`, a valid certificate, and no redirects"
        "universal links should list app identifiers for the domain in the \`applinks\` service"
        "Associated Domains entitlement entries use the format \`<service>:<fully-qualified-domain>\`"
        "must not include path or query components or a trailing slash"
        "iOS 14 and later request \`apple-app-site-association\` files through an Apple-managed CDN"
        "CDN requests the file for a domain within 24 hours"
        "devices check for updates approximately once per week after app installation"
        "universal links require a two-way association between the app and website"
        "NSUserActivityTypeBrowsingWeb"
        "universal links can be an attack vector"
        "validate URL parameters, discard malformed URLs"
        "## Apple References"
        "supporting-associated-domains"
        "com.apple.developer.associated-domains"
        "allowing-apps-and-websites-to-link-to-your-content"
        "tn3155-debugging-universal-links"
        "## Build And Deployment Under Review"
        "## Required Live URL And AASA Review"
        "## Current Live Check Snapshot"
        "## Local Pre-Deployment Route Evidence"
        "## Evidence Commands"
        "## Production Deployment Fix Checklist"
        "## Closure Rule"
        "https://wakeve.app/privacy"
        "https://wakeve.app/support"
        "https://wakeve.app/terms"
        "https://wakeve.app/third-party-notices"
        "https://wakeve.app/app"
        "https://wakeve.app/app/login"
        "https://wakeve.app/app/dashboard"
        "https://wakeve.app/app/create"
        "https://wakeve.app/app/events"
        "https://wakeve.app/.well-known/apple-app-site-association"
        "https://wakeve.app/apple-app-site-association"
        "https://api.wakeve.app/health"
        "APPLE_TEAM_ID=<APPLE_TEAM_ID>"
        "<APPLE_TEAM_ID>.com.guyghost.wakeve"
        "/event/*"
        "/poll/*"
        "/meeting/*"
        "/invite/*"
        "application/json"
        "Public legal pages render reviewable HTML"
        "Local dashboard routing"
        "microfrontends.json"
        "wakeve-dashboard"
        "/app/:path*"
        "export const ssr = true"
        "scripts/app-store-local-web-route-check.sh"
        "A1B2C3D4E5.com.guyghost.wakeve"
        "live production validation failed with 9 live URL/AASA errors and 1 final-signoff warning"
        "docs/app-store-live-url-aasa/live-url-aasa-2026-06-13T12-20-32Z.md"
        "./scripts/capture-app-store-live-url-aasa.sh --allow-failures"
        "docs/app-store-live-url-aasa/live-url-aasa-2026-06-13T13-02-14Z.md"
        "./scripts/capture-app-store-live-url-aasa.sh --allow-failures --timeout 5"
        "18 required live URL/AASA checks failed or could not be validated"
        "Direct DNS snapshot on 2026-06-13"
        "Could not resolve host: wakeve.app"
        "Could not resolve host: api.wakeve.app"
        "Observed live blockers"
        "The repository has deployable local web routes and AASA route code"
        "public production domains currently do not resolve in DNS"
        "./scripts/capture-app-store-live-url-aasa.sh"
        "Deploy the web app serving"
        "Keep Vercel Microfrontends routing \`wakeve-dashboard\` for \`/app\` and \`/app/:path*\`"
        "Configure production \`APPLE_TEAM_ID\` or \`TEAM_ID\`"
        "AASA responses use \`application/json\`, no redirects, valid TLS"
        "Deploy the backend health endpoint"
        "Record DNS provider, hosting provider, backend provider, deployment IDs, cache headers, rollout owner, rollback owner"
        "Re-run \`APP_REVIEW_PHONE_NUMBER='+33123456789' APPLE_TEAM_ID=<APPLE_TEAM_ID> ./scripts/lint-store-metadata.sh --ios-only --check-live-urls\`"
        "APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store live URL and AASA evidence: Covers $phrase"
        else
            error "App Store live URL and AASA evidence: Missing '$phrase'"
        fi
    done

    local local_route_check="$PROJECT_DIR/scripts/app-store-local-web-route-check.sh"
    if [ -x "$local_route_check" ]; then
        pass "App Store local web route check: Script exists and is executable"
    else
        error "App Store local web route check: Missing executable scripts/app-store-local-web-route-check.sh"
    fi

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires live URL and AASA evidence before final submission"
    else
        error "App Store launch checklist: Does not require live URL and AASA evidence before final submission"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires live URL and AASA evidence completion"
    else
        error "Final App Store signoff: Does not require live URL and AASA evidence completion"
    fi

    local blocker_register="$PROJECT_DIR/docs/APP_STORE_BLOCKER_REGISTER.md"
    if [ -f "$blocker_register" ] &&
        grep -Fq "docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md" "$blocker_register" &&
        grep -Fq "APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true" "$blocker_register"; then
        pass "App Store blocker register: Requires live URL and AASA evidence completion"
    else
        error "App Store blocker register: Does not require live URL and AASA evidence completion"
    fi

    local runbook="$PROJECT_DIR/docs/APP_STORE_SUBMISSION_RUNBOOK.md"
    if [ -f "$runbook" ] &&
        grep -Fq "docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md" "$runbook" &&
        grep -Fq "APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true" "$runbook"; then
        pass "App Store submission runbook: Requires live URL and AASA evidence completion"
    else
        error "App Store submission runbook: Does not require live URL and AASA evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_live_url_aasa_evidence_for_final_release" "$audit_script"; then
        pass "Final audit: Requires live URL and AASA evidence for final release"
    else
        error "Final audit: Does not require live URL and AASA evidence for final release"
    fi
}

validate_app_store_capabilities_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🔐 App Store Capabilities Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_CAPABILITIES_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store capabilities evidence: Missing at docs/APP_STORE_CAPABILITIES_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store capabilities evidence" 1000 20000

    local required_phrases=(
        "APP_STORE_CAPABILITIES_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-28"
        "App ID identifies the app in a provisioning profile"
        "explicit for one app or wildcard for a set of apps"
        "app capabilities enabled for an App ID serve as an allow list"
        "configuring capabilities that an app uses also requires adding them to a target in the Xcode project"
        "Xcode edits the needed entitlements and Information Property List files"
        "capabilities can also be manually configured for apps and websites within the Identifiers section"
        "enabling app capabilities requires the Account Holder or Admin role"
        "provisioning profiles that contain a modified App ID become invalid and must be regenerated"
        "enabling a capability can affect provisioning profiles for all eligible platforms"
        "Sign in with Apple, App groups, Apple Pay, Data protection, iCloud, and push notifications require additional steps"
        "uploading to App Store Connect requires an app record registered with an explicit App ID"
        "App Store Connect provisioning profile uses the explicit App ID that matches the bundle ID"
        "App Store Connect provisioning profile is created for the App ID that matches the bundle ID"
        "contains a single distribution certificate"
        "automatically managed signing can let Xcode manage distribution provisioning profiles during upload"
        "provisioning profiles authorize the app to use certain app services"
        "managed capabilities require approval from Apple before they can be used"
        "approved managed capabilities are automatically included in eligible provisioning profiles"
        "Sign in with Apple starts by enabling the app's App ID"
        "new Sign in with Apple App IDs should be enabled as primary"
        "related apps can be grouped with an existing primary App ID"
        "managed capabilities can be enabled only after Apple assigns the entitlement"
        "## App Identifier"
        "## Required Capabilities"
        "## Local Unsigned Capability Scan Result"
        "## Required Commands"
        "## Evidence To Attach"
        "## Closure Rule"
        "com.guyghost.wakeve"
        "Push Notifications"
        "Siri"
        "Sign in with Apple"
        "Associated Domains"
        'aps-environment` is `$(APS_ENVIRONMENT)'
        'APS_ENVIRONMENT = production'
        'CODE_SIGN_ENTITLEMENTS = src/Wakeve.entitlements'
        'CODE_SIGN_STYLE = Automatic'
        'DEVELOPMENT_TEAM = ${TEAM_ID}'
        'PROVISIONING_PROFILE_REQUIRED = YES'
        'code object is not signed at all'
        'aps-environment=production'
        'com.apple.developer.siri=true'
        'com.apple.developer.applesignin'
        'applinks:wakeve.app'
        "it cannot prove"
        "TEAM_ID=<APPLE_TEAM_ID> bundle exec fastlane ios validate_ipa_entitlements ipa:build/ios/WakeveApp.ipa"
        "application-identifier"
        "com.apple.developer.team-identifier"
        "Release provisioning profile"
        "Sign in with Apple configuration screenshot"
        "Distribution certificate identifier"
        "exactly one distribution certificate"
        "App Store Connect uploaded build number"
        "APP_STORE_CAPABILITIES_CONFIRMED=true"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store capabilities evidence: Covers $phrase"
        else
            error "App Store capabilities evidence: Missing '$phrase'"
        fi
    done

    local entitlements="$PROJECT_DIR/iosApp/src/Wakeve.entitlements"
    local project_file="$PROJECT_DIR/iosApp/iosApp.xcodeproj/project.pbxproj"
    if [ -f "$entitlements" ] &&
        plutil -extract aps-environment raw -o - "$entitlements" 2>/dev/null | grep -qx "\$(APS_ENVIRONMENT)" &&
        plutil -extract 'com\.apple\.developer\.siri' raw -o - "$entitlements" 2>/dev/null | grep -qx "true" &&
        plutil -extract 'com\.apple\.developer\.applesignin' xml1 -o - "$entitlements" 2>/dev/null | grep -Fq "<string>Default</string>" &&
        plutil -extract 'com\.apple\.developer\.associated-domains' xml1 -o - "$entitlements" 2>/dev/null | grep -Fq "<string>applinks:wakeve.app</string>"; then
        pass "App Store capabilities evidence: Source entitlements match documented capabilities"
    else
        error "App Store capabilities evidence: Source entitlements do not match documented capabilities"
    fi

    if [ -f "$project_file" ] &&
        grep -Fq "APS_ENVIRONMENT = production;" "$project_file" &&
        grep -Fq "CODE_SIGN_ENTITLEMENTS = src/Wakeve.entitlements;" "$project_file" &&
        grep -Fq 'DEVELOPMENT_TEAM = "${TEAM_ID}";' "$project_file" &&
        grep -Fq "PRODUCT_BUNDLE_IDENTIFIER = com.guyghost.wakeve;" "$project_file"; then
        pass "App Store capabilities evidence: Xcode project has Release entitlement and Team ID wiring"
    else
        error "App Store capabilities evidence: Xcode project Release entitlement or Team ID wiring is incomplete"
    fi

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_CAPABILITIES_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_CAPABILITIES_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires capabilities evidence before APP_STORE_CAPABILITIES_CONFIRMED"
    else
        error "App Store launch checklist: Does not require capabilities evidence before APP_STORE_CAPABILITIES_CONFIRMED"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_CAPABILITIES_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_CAPABILITIES_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires capabilities evidence completion"
    else
        error "Final App Store signoff: Does not require capabilities evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_CAPABILITIES_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "require_capabilities_evidence_if_confirmed" "$audit_script"; then
        pass "Final audit: Requires capabilities evidence when APP_STORE_CAPABILITIES_CONFIRMED is true"
    else
        error "Final audit: Does not require capabilities evidence when APP_STORE_CAPABILITIES_CONFIRMED is true"
    fi
}

validate_app_store_export_compliance_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🌐 App Store Export Compliance Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store export compliance evidence: Missing at docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store export compliance evidence" 1000 18000

    local required_phrases=(
        "APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-28"
        "use, access, contain, implement, or incorporate encryption"
        "determine export compliance requirements in App Store Connect before upload, testing, and distribution"
        "encryption includes crypto functionality within Apple's operating system"
        "crypto functionality from proprietary or non-Apple sources"
        "developer is responsible for reviewing export regulations"
        "App Store Connect asks encryption questions for each new version"
        "HTTPS connections using \`NSURLSession\`"
        "using encryption limited to that within the Apple operating system do not require documentation in App Store Connect"
        "industry-standard algorithm not provided by the Apple operating system require a French encryption declaration"
        "proprietary encryption algorithms not accepted by international standard bodies require a US CCATS"
        "French encryption declaration form is only required if distributing the app on the App Store in France"
        "adding \`ITSAppUsesNonExemptEncryption\` to the app Info.plist declares whether the app uses encryption"
        "if \`ITSAppUsesNonExemptEncryption\` is absent, App Store Connect walks the developer through an export compliance questionnaire"
        "a value of \`false\` for \`ITSAppUsesNonExemptEncryption\` indicates the app doesn't use encryption or only uses exempt encryption"
        "\`ITSAppUsesNonExemptEncryption=false\` claim covers the app and third-party libraries linked into the app"
        "if non-exempt encryption is used and documentation is reviewed successfully, Apple provides a code"
        "app encryption declarations may be required for builds with \`usesNonExemptEncryption=true\`"
        "## Build Under Review"
        "## Current Repository Evidence"
        "## App Store Connect Evidence"
        "## Evidence Commands"
        "## Local Export Compliance Scan Result"
        "## Apple References"
        "## Closure Rule"
        "iosApp/src/Info.plist"
        "ITSAppUsesNonExemptEncryption=false"
        "ITSEncryptionExportComplianceCode"
        "iOS network transport"
        "iOS secure storage"
        "Custom crypto scan"
        "URLSession"
        "Keychain"
        "CryptoKit"
        "CommonCrypto"
        "CCCrypt"
        "SecKey"
        'Source Info.plist: `"ITSAppUsesNonExemptEncryption" => false`'
        'Built Release Info.plist: `"ITSAppUsesNonExemptEncryption" => false`'
        '`ITSEncryptionExportComplianceCode`: no match'
        "iOS app networking uses \`URLSession\`"
        "iOS token storage uses Apple Keychain/Security framework APIs"
        "No iOS app source match was found for \`CryptoKit\`, \`CommonCrypto\`, \`CCCrypt\`, \`SecKey\`, AES, RSA, ChaCha, or \`ITSEncryptionExportComplianceCode\`"
        "server-side Apple Sign in token signing and is not bundled as iOS app encryption evidence"
        "one-way hashing/encoding/parsing helpers, not custom reversible encryption"
        "local pre-submission evidence only"
        "uploaded App Store Connect build is reviewed"
        "no non-exempt encryption"
        "export compliance documentation"
        "linked third-party libraries and app code"
        "docs/APP_STORE_FINAL_SIGNOFF.md"
        "./scripts/lint-store-metadata.sh --ios-only"
        "overview-of-export-compliance"
        "export-compliance-documentation-for-encryption"
        "complying_with_encryption_export_regulations"
        "itsappusesnonexemptencryption"
        "itsencryptionexportcompliancecode"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store export compliance evidence: Covers $phrase"
        else
            error "App Store export compliance evidence: Missing '$phrase'"
        fi
    done

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires export compliance evidence before final signoff"
    else
        error "App Store launch checklist: Does not require export compliance evidence before final signoff"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires export compliance evidence completion"
    else
        error "Final App Store signoff: Does not require export compliance evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "all_manual_release_signoffs_confirmed" "$audit_script" &&
        grep -Fq "require_export_compliance_evidence_for_final_release" "$audit_script"; then
        pass "Final audit: Requires export compliance evidence when final signoff is complete"
    else
        error "Final audit: Does not require export compliance evidence when final signoff is complete"
    fi

    if [ -f "$final_signoff" ] && grep -Fxq "APP_STORE_FINAL_SIGNOFF_COMPLETE=true" "$final_signoff"; then
        if grep -Fxq "APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true" "$evidence_file"; then
            pass "App Store export compliance evidence: Complete for final signoff"
        else
            error "App Store export compliance evidence: Final signoff cannot be complete until APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true"
        fi
    fi
}

validate_app_store_review_access_evidence() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🧑‍⚖️ App Store Review Access Evidence${NC}"

    local evidence_file="$PROJECT_DIR/docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md"
    if [ ! -f "$evidence_file" ]; then
        error "App Store review access evidence: Missing at docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md"
        return 1
    fi

    validate_text_file "$evidence_file" "App Store review access evidence" 1000 18000

    local required_phrases=(
        "APP_STORE_REVIEW_ACCESS_EVIDENCE_COMPLETE=false"
        "## Apple Source Baseline"
        "Last checked: 2026-05-28"
        "App Review Information in App Store Connect should include all details needed for review"
        "if some features require signing in, the submission should provide a valid demo account username and password"
        "apps with account-based features should include either an active demo account or a fully featured demo mode"
        "any other resources needed to review the app"
        "App Store review details include contact information, demo account information, notes, and review attachments"
        "required App Store review details include contact first and last name, contact phone number, contact email address"
        "whether testing requires a demo account"
        "demo account name/password if the app uses single sign-on"
        "App Review contact information includes name, email, and phone number"
        "demo account used during App Review must not expire"
        "App Review notes can provide additional information that helps reviewers understand the app"
        "app-specific settings, test registration, account details"
        "Notes field can contain up to 4000 bytes"
        "can be written in any language"
        "single sign-on service"
        "details for additional accounts should be included in the Notes field"
        "required metadata must be provided and the right build must be added before submitting"
        "required role to submit an app is Account Holder, Admin, or App Manager"
        "Add for Review changes the app status to Ready for Review"
        "Submit for Review is clicked"
        "replies may include attachments such as screenshots and supporting documents"
        "every app version and its content are reviewed"
        "submission is not sent to App Review until the app is explicitly submitted for review"
        "metadata and review information must accurately describe app behavior and review-relevant access"
        "partnership documentation or authorization should be attached in App Store Connect"
        "## Build Under Review"
        "## Review Access Path"
        "## Local Reviewer Access Scan Result"
        "## Evidence Commands"
        "## Closure Rule"
        "## Apple References"
        "composeApp/metadata/ios/review_information/notes.txt"
        "docs/APP_STORE_TESTFLIGHT_EVIDENCE.md"
        "docs/APP_STORE_FINAL_SIGNOFF.md"
        "Guest access"
        "Demo credentials"
        "Review details API fields"
        "App Review communication"
        "Do not commit passwords"
        "account deletion"
        "UGC moderation"
        "payment"
        "Tricount"
        "APP_REVIEW_PHONE_NUMBER"
        "first_name=Wakeve"
        "last_name=Support"
        "email_address=support@wakeve.app"
        "Review notes byte length: \`1548\`"
        'No `demo_password.txt` is committed'
        'No `demo_user.txt` is committed'
        'No `phone_number.txt` is committed'
        "Local credential scan found no review-information file matching"
        "xcodebuildmcp-iphone-login-2026-05-27.jpg"
        "xcodebuildmcp-iphone-login-guest-2026-05-27.jpg"
        "Continue as guest"
        "Continuer en invité"
        "guestAccessButton"
        "auth.continue_as_guest"
        "continueAsGuest()"
        "wakeve_guest_user_id"
        "Development mode: Skip authentication"
        "`#if DEBUG`"
        "signed TestFlight/App Store Connect review build"
        "App Store Connect demo account"
        "signInRequired"
        "review detail output or screenshot"
        "owner and response path for App Review messages"
        "Review notes remain within Apple's 4000-byte limit"
        "Required contact name, email, and phone are present in App Store Connect"
        "respond to App Review messages and attach supporting files"
        "app-store-review-details"
        "platform-version-information"
        "submit-an-app"
        "reply-to-app-review-messages"
        "./scripts/lint-store-metadata.sh --ios-only"
    )

    local phrase
    for phrase in "${required_phrases[@]}"; do
        if grep -Fq "$phrase" "$evidence_file"; then
            pass "App Store review access evidence: Covers $phrase"
        else
            error "App Store review access evidence: Missing '$phrase'"
        fi
    done

    local login_view="$PROJECT_DIR/iosApp/src/Views/Auth/LoginView.swift"
    if [ -f "$login_view" ] &&
        awk '/guestAccessButton/ { if (!guest) guest=NR } /#if DEBUG/ { if (!debug) debug=NR } END { exit !(guest && debug && guest < debug) }' "$login_view"; then
        pass "iOS reviewer guest access: Guest button is rendered before DEBUG-only development skip"
    else
        error "iOS reviewer guest access: LoginView must render a release-visible guest button before DEBUG-only development controls"
    fi

    local auth_state_manager="$PROJECT_DIR/iosApp/src/Services/AuthStateManager.swift"
    if [ -f "$auth_state_manager" ] &&
        grep -Fq "func continueAsGuest()" "$auth_state_manager" &&
        grep -Fq "wakeve_guest_user_id" "$auth_state_manager" &&
        grep -Fq "guard !isCurrentSessionGuest else { return }" "$auth_state_manager"; then
        pass "iOS reviewer guest access: AuthStateManager creates and restores local-only guest sessions"
    else
        error "iOS reviewer guest access: AuthStateManager must create/restore local-only guest sessions and skip token refresh for guests"
    fi

    local review_notes="$PROJECT_DIR/composeApp/metadata/ios/review_information/notes.txt"
    if [ -f "$review_notes" ] &&
        grep -Fq "Continue as guest" "$review_notes" &&
        grep -Fq "local-only guest session" "$review_notes"; then
        pass "App Review notes: Match release-visible local-only guest access"
    else
        error "App Review notes: Must match release-visible local-only guest access"
    fi

    if [ -f "$review_notes" ]; then
        local review_notes_bytes
        review_notes_bytes=$(wc -c < "$review_notes" | tr -d ' ')
        if [ "$review_notes_bytes" -le 4000 ]; then
            pass "App Review notes: Within 4000-byte Apple limit"
        else
            error "App Review notes: Exceed 4000-byte Apple limit"
        fi

        if grep -Fq "Review notes byte length: \`$review_notes_bytes\`" "$evidence_file"; then
            pass "App Store review access evidence: Recorded review notes byte length matches local notes"
        else
            error "App Store review access evidence: Recorded review notes byte length does not match local notes"
        fi
    fi

    local review_info_dir="$PROJECT_DIR/composeApp/metadata/ios/review_information"
    if find "$review_info_dir" -maxdepth 1 -type f \( -name "*password*" -o -name "*user*" -o -name "*phone*" \) -print | grep -q .; then
        error "App Review metadata: Must not commit demo credentials or review phone files"
    else
        pass "App Review metadata: No committed demo credential or phone files"
    fi

    local launch_checklist="$PROJECT_DIR/docs/APP_STORE_LAUNCH_CHECKLIST.md"
    if [ -f "$launch_checklist" ] &&
        grep -Fq "docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md" "$launch_checklist" &&
        grep -Fq "APP_STORE_REVIEW_ACCESS_EVIDENCE_COMPLETE=true" "$launch_checklist"; then
        pass "App Store launch checklist: Requires review access evidence before final signoff"
    else
        error "App Store launch checklist: Does not require review access evidence before final signoff"
    fi

    local final_signoff="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"
    if [ -f "$final_signoff" ] &&
        grep -Fq "docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md" "$final_signoff" &&
        grep -Fq "APP_STORE_REVIEW_ACCESS_EVIDENCE_COMPLETE=true" "$final_signoff"; then
        pass "Final App Store signoff: Requires review access evidence completion"
    else
        error "Final App Store signoff: Does not require review access evidence completion"
    fi

    local audit_script="$PROJECT_DIR/scripts/app-store-submission-audit.sh"
    if [ -f "$audit_script" ] &&
        grep -Fq 'require_file "docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md"' "$audit_script" &&
        grep -Fq "all_manual_release_signoffs_confirmed" "$audit_script" &&
        grep -Fq "require_review_access_evidence_for_final_release" "$audit_script"; then
        pass "Final audit: Requires review access evidence when final signoff is complete"
    else
        error "Final audit: Does not require review access evidence when final signoff is complete"
    fi

    if [ -f "$final_signoff" ] && grep -Fxq "APP_STORE_FINAL_SIGNOFF_COMPLETE=true" "$final_signoff"; then
        if grep -Fxq "APP_STORE_REVIEW_ACCESS_EVIDENCE_COMPLETE=true" "$evidence_file"; then
            pass "App Store review access evidence: Complete for final signoff"
        else
            error "App Store review access evidence: Final signoff cannot be complete until APP_STORE_REVIEW_ACCESS_EVIDENCE_COMPLETE=true"
        fi
    fi
}

validate_ios_plist() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}📄 iOS Info.plist${NC}"

    local plist="$PROJECT_DIR/iosApp/src/Info.plist"
    if [ ! -f "$plist" ]; then
        error "Info.plist: Missing at iosApp/src/Info.plist"
        return 1
    fi

    if plutil -lint "$plist" >/dev/null 2>&1; then
        pass "Info.plist: Valid XML plist"
    else
        error "Info.plist: Invalid plist"
        return 1
    fi

    if plutil -extract ITSAppUsesNonExemptEncryption raw -o - "$plist" 2>/dev/null | grep -qx "false"; then
        pass "Info.plist: ITSAppUsesNonExemptEncryption=false"
    else
        warning "Info.plist: ITSAppUsesNonExemptEncryption is not false"
    fi

    if plutil -extract NSLocationAlwaysAndWhenInUseUsageDescription raw -o - "$plist" >/dev/null 2>&1; then
        warning "Info.plist: Declares Always location permission; verify App Review justification"
    else
        pass "Info.plist: Does not declare Always location permission"
    fi

    if plutil -extract UIBackgroundModes raw -o - "$plist" 2>/dev/null | grep -q "fetch"; then
        warning "Info.plist: Declares background fetch; verify active implementation"
    else
        pass "Info.plist: Does not declare background fetch"
    fi

    if plutil -extract UIRequiredDeviceCapabilities raw -o - "$plist" 2>/dev/null | grep -q "armv7"; then
        warning "Info.plist: Declares legacy armv7 device capability"
    else
        pass "Info.plist: Does not declare legacy armv7 device capability"
    fi

    if plutil -extract CFBundleURLTypes xml1 -o - "$plist" 2>/dev/null | grep -q "<string>wakeve</string>"; then
        pass "Info.plist: Registers wakeve URL scheme"
    else
        error "Info.plist: Missing wakeve URL scheme registration"
    fi

    if grep -R "INPreferences.requestSiriAuthorization" "$PROJECT_DIR/iosApp/src" >/dev/null 2>&1 || plutil -extract INIntentsSupported raw -o - "$plist" >/dev/null 2>&1; then
        if plutil -extract NSSiriUsageDescription raw -o - "$plist" >/dev/null 2>&1; then
            pass "Info.plist: Declares NSSiriUsageDescription for Siri authorization"
        else
            error "Info.plist: Missing NSSiriUsageDescription while Siri authorization/intents are present"
        fi
    fi

    if grep -R "SFSpeechRecognizer" "$PROJECT_DIR/iosApp/src" >/dev/null 2>&1; then
        if plutil -extract NSSpeechRecognitionUsageDescription raw -o - "$plist" >/dev/null 2>&1; then
            pass "Info.plist: Declares NSSpeechRecognitionUsageDescription for speech recognition"
        else
            error "Info.plist: Missing NSSpeechRecognitionUsageDescription while speech recognition is present"
        fi
    fi

    if grep -RE "AVAudioEngine|AVAudioSession|requestRecordPermission" "$PROJECT_DIR/iosApp/src" "$PROJECT_DIR/shared/src/iosMain" >/dev/null 2>&1; then
        if plutil -extract NSMicrophoneUsageDescription raw -o - "$plist" >/dev/null 2>&1; then
            pass "Info.plist: Declares NSMicrophoneUsageDescription for microphone capture"
        else
            error "Info.plist: Missing NSMicrophoneUsageDescription while microphone capture APIs are present"
        fi
    fi

    if grep -R "CLLocationManager" "$PROJECT_DIR/iosApp/src" >/dev/null 2>&1; then
        if plutil -extract NSLocationWhenInUseUsageDescription raw -o - "$plist" >/dev/null 2>&1; then
            pass "Info.plist: Declares NSLocationWhenInUseUsageDescription for location access"
        else
            error "Info.plist: Missing NSLocationWhenInUseUsageDescription while CLLocationManager is present"
        fi
    fi

    if grep -RE "EventKit|EKEventStore|EKEntityType" "$PROJECT_DIR/iosApp/src" "$PROJECT_DIR/shared/src/iosMain" >/dev/null 2>&1; then
        if plutil -extract NSCalendarsUsageDescription raw -o - "$plist" >/dev/null 2>&1; then
            pass "Info.plist: Declares NSCalendarsUsageDescription for calendar access"
        else
            error "Info.plist: Missing NSCalendarsUsageDescription while EventKit is present"
        fi
    fi

    if grep -RE "PhotosPicker|PHPickerViewController|UIImagePickerController" "$PROJECT_DIR/iosApp/src" "$PROJECT_DIR/shared/src/iosMain" >/dev/null 2>&1; then
        if plutil -extract NSPhotoLibraryUsageDescription raw -o - "$plist" >/dev/null 2>&1; then
            pass "Info.plist: Declares NSPhotoLibraryUsageDescription for photo selection"
        else
            error "Info.plist: Missing NSPhotoLibraryUsageDescription while photo selection is present"
        fi
    fi

    if grep -RE "PHPhotoLibrary|UIImageWriteToSavedPhotosAlbum" "$PROJECT_DIR/iosApp/src" "$PROJECT_DIR/shared/src/iosMain" >/dev/null 2>&1; then
        if plutil -extract NSPhotoLibraryAddUsageDescription raw -o - "$plist" >/dev/null 2>&1; then
            pass "Info.plist: Declares NSPhotoLibraryAddUsageDescription for photo library write access"
        else
            error "Info.plist: Missing NSPhotoLibraryAddUsageDescription while photo library write APIs are present"
        fi
    fi

    if grep -RE "sourceType[[:space:]]*=[[:space:]]*\\.camera|UIImagePickerController\\.SourceType\\.camera" "$PROJECT_DIR/iosApp/src" >/dev/null 2>&1; then
        if plutil -extract NSCameraUsageDescription raw -o - "$plist" >/dev/null 2>&1; then
            pass "Info.plist: Declares NSCameraUsageDescription for camera capture"
        else
            error "Info.plist: Missing NSCameraUsageDescription while camera capture is present"
        fi
    fi

    if grep -RE "registerForRemoteNotifications|UNUserNotificationCenter" "$PROJECT_DIR/iosApp/src" "$PROJECT_DIR/shared/src/iosMain" >/dev/null 2>&1; then
        if plutil -extract UIBackgroundModes xml1 -o - "$plist" 2>/dev/null | grep -q "<string>remote-notification</string>"; then
            pass "Info.plist: Declares remote-notification background mode for push handling"
        else
            warning "Info.plist: Push notification code is present without remote-notification background mode"
        fi
    fi
}

validate_ios_source_network_configuration() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🌐 iOS Source Network Configuration${NC}"

    local source_files=()
    while IFS= read -r file; do
        source_files+=("$file")
    done < <(find "$PROJECT_DIR/iosApp/src" "$PROJECT_DIR/shared/src/iosMain" -type f \( -name "*.swift" -o -name "*.kt" \) ! -path "*/Shared.framework/*" 2>/dev/null)

    if grep -R "https://api.wakeve.app" "$PROJECT_DIR/iosApp/src" "$PROJECT_DIR/shared/src/iosMain" >/dev/null 2>&1; then
        pass "iOS source: Production API endpoint is configured"
    else
        error "iOS source: Missing production API endpoint https://api.wakeve.app"
    fi

    local local_url_files=()
    local file
    for file in "${source_files[@]}"; do
        if grep -Eq "\"http://(localhost|127\\.0\\.0\\.1|10\\.0\\.2\\.2)" "$file"; then
            local_url_files+=("$file")
        fi
    done

    if [ "${#local_url_files[@]}" -eq 0 ]; then
        pass "iOS source: No local development API URLs found"
        return 0
    fi

    for file in "${local_url_files[@]}"; do
        local rel="${file#$PROJECT_DIR/}"
        if grep -q "#if DEBUG" "$file" && grep -q "#else" "$file" && grep -q "https://api.wakeve.app" "$file"; then
            pass "iOS source: $rel confines local API URL to DEBUG with production fallback"
        else
            error "iOS source: $rel contains a local API URL without DEBUG-only production fallback"
        fi
    done
}

validate_ios_launch_source_hygiene() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🧹 iOS Launch Source Hygiene${NC}"

    local matches=""
    local file rel
    while IFS= read -r file; do
        if grep -nE "TODO|FIXME" "$file" >/dev/null 2>&1; then
            rel="${file#$PROJECT_DIR/}"
            matches+="$rel"$'\n'
        fi
    done < <(find "$PROJECT_DIR/iosApp/src" "$PROJECT_DIR/shared/src/iosMain" -type f \( -name "*.swift" -o -name "*.kt" \) ! -path "*/Shared.framework/*" 2>/dev/null)

    if [ -n "$matches" ]; then
        printf '%s' "$matches" | sed 's/^/    /'
        error "iOS source hygiene: TODO/FIXME markers remain in launch source"
    else
        pass "iOS source hygiene: No TODO/FIXME markers in active iOS launch source"
    fi
}

validate_ios_release_debug_logging() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    local debug_logger="$PROJECT_DIR/iosApp/src/Services/DebugLogging.swift"

    if [ -f "$debug_logger" ] &&
        grep -q "#if DEBUG" "$debug_logger" &&
        grep -q "print(message())" "$debug_logger"; then
        pass "iOS source hygiene: debugLog is compiled only for DEBUG logging"
    else
        error "iOS source hygiene: Missing DEBUG-only debugLog helper"
    fi

    local matches=""
    local file rel
    while IFS= read -r file; do
        [ "$file" = "$debug_logger" ] && continue
        if grep -nE "\b(print|debugPrint|NSLog)[[:space:]]*\(" "$file" >/dev/null 2>&1; then
            rel="${file#$PROJECT_DIR/}"
            matches+="$rel"$'\n'
        fi
    done < <(find "$PROJECT_DIR/iosApp/src" "$PROJECT_DIR/shared/src/iosMain" -type f \( -name "*.swift" -o -name "*.kt" \) ! -path "*/Shared.framework/*" 2>/dev/null)

    if [ -n "$matches" ]; then
        printf '%s' "$matches" | sed 's/^/    /'
        error "iOS source hygiene: raw print/debug logging remains in launch source"
    else
        pass "iOS source hygiene: Raw print/debug logging is absent from active launch source"
    fi
}

source_line_is_in_debug_block() {
    local file="$1"
    local target_line="$2"

    awk -v target="$target_line" '
        /#if[[:space:]]+DEBUG/ { debug_depth++ }
        /#endif/ && debug_depth > 0 { pending_endif = 1 }
        NR == target {
            if (debug_depth > 0) {
                exit 0
            }
            exit 1
        }
        pending_endif == 1 {
            debug_depth--
            pending_endif = 0
        }
    ' "$file"
}

validate_ios_debug_auth_isolation() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    local pattern='wakeve-debug-authenticated|WAKEVE_DEBUG_AUTHENTICATED|authenticateForDevelopmentLaunchIfRequested|shouldUseDevelopmentLaunchAuthentication|setAuthStateForDevelopment|dev-token-mock|dev@example\.com|Dev User'
    local violations=""
    local file line rel line_no

    while IFS=: read -r file line_no line; do
        [ -n "$file" ] || continue
        if ! source_line_is_in_debug_block "$file" "$line_no"; then
            rel="${file#$PROJECT_DIR/}"
            violations+="$rel:$line_no:$line"$'\n'
        fi
    done < <(grep -RInE "$pattern" "$PROJECT_DIR/iosApp/src" --include="*.swift" ! -path "*/Shared.framework/*" 2>/dev/null || true)

    if [ -n "$violations" ]; then
        printf '%s' "$violations" | sed 's/^/    /'
        error "iOS source hygiene: development auth hooks must remain inside DEBUG-only source blocks"
    else
        pass "iOS source hygiene: Development auth hooks are DEBUG-only in launch source"
    fi
}

validate_ios_background_task_configuration() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    local plist="$PROJECT_DIR/iosApp/src/Info.plist"
    local files=()
    local file rel all_debug_only=true

    while IFS= read -r file; do
        files+=("$file")
    done < <(grep -RIlE "BackgroundTasks|BGTaskScheduler|BGProcessingTask|BGProcessingTaskRequest" "$PROJECT_DIR/iosApp/src" "$PROJECT_DIR/shared/src/iosMain" --include="*.swift" --include="*.kt" 2>/dev/null || true)

    if [ "${#files[@]}" -eq 0 ]; then
        pass "iOS source hygiene: No BackgroundTasks API usage found"
        return 0
    fi

    if plutil -extract BGTaskSchedulerPermittedIdentifiers raw -o - "$plist" >/dev/null 2>&1 &&
        plutil -extract UIBackgroundModes raw -o - "$plist" 2>/dev/null | grep -q "processing"; then
        pass "iOS source hygiene: BackgroundTasks APIs are backed by plist processing configuration"
        return 0
    fi

    for file in "${files[@]}"; do
        if ! grep -q "#if DEBUG" "$file" || ! grep -q "#endif" "$file"; then
            all_debug_only=false
            rel="${file#$PROJECT_DIR/}"
            printf '    %s\n' "$rel"
        fi
    done

    if [ "$all_debug_only" = true ]; then
        pass "iOS source hygiene: BackgroundTasks APIs are DEBUG-only without App Store background processing configuration"
    else
        error "iOS source hygiene: BackgroundTasks APIs require BGTaskSchedulerPermittedIdentifiers and UIBackgroundModes=processing, or DEBUG-only isolation"
    fi
}

validate_ios_xcode_release_settings() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🏗️  iOS Xcode Release Settings${NC}"

    if ! command -v xcodebuild >/dev/null 2>&1; then
        warning "Xcode Release settings: xcodebuild is not available"
        return 0
    fi

    local settings_file
    settings_file=$(mktemp)
    if ! xcodebuild \
        -project "$PROJECT_DIR/iosApp/iosApp.xcodeproj" \
        -scheme WakeveApp \
        -configuration Release \
        -destination "generic/platform=iOS" \
        -showBuildSettings \
        -json >"$settings_file" 2>/dev/null; then
        rm -f "$settings_file"
        error "Xcode Release settings: Could not read build settings"
        return 0
    fi

    build_setting() {
        ruby -rjson -e 'data = JSON.parse(File.read(ARGV.fetch(0))); puts data.fetch(0).fetch("buildSettings")[ARGV.fetch(1)].to_s' "$settings_file" "$1"
    }

    local bundle_id product_name info_plist entitlements aps_environment marketing_version build_version sdkroot deployment_target device_family app_icon mac_support xr_support
    bundle_id=$(build_setting PRODUCT_BUNDLE_IDENTIFIER)
    product_name=$(build_setting PRODUCT_NAME)
    info_plist=$(build_setting INFOPLIST_FILE)
    entitlements=$(build_setting CODE_SIGN_ENTITLEMENTS)
    aps_environment=$(build_setting APS_ENVIRONMENT)
    marketing_version=$(build_setting MARKETING_VERSION)
    build_version=$(build_setting CURRENT_PROJECT_VERSION)
    sdkroot=$(build_setting SDKROOT)
    deployment_target=$(build_setting IPHONEOS_DEPLOYMENT_TARGET)
    device_family=$(build_setting TARGETED_DEVICE_FAMILY)
    app_icon=$(build_setting ASSETCATALOG_COMPILER_APPICON_NAME)
    mac_support=$(build_setting SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD)
    xr_support=$(build_setting SUPPORTS_XR_DESIGNED_FOR_IPHONE_IPAD)

    if [ "$bundle_id" = "com.guyghost.wakeve" ]; then
        pass "Xcode Release: Bundle ID is com.guyghost.wakeve"
    else
        error "Xcode Release: Unexpected Bundle ID ($bundle_id)"
    fi

    if [ "$product_name" = "Wakeve" ]; then
        pass "Xcode Release: Product name is Wakeve"
    else
        error "Xcode Release: Unexpected product name ($product_name)"
    fi

    if [ "$info_plist" = "src/Info.plist" ]; then
        pass "Xcode Release: Uses source Info.plist"
    else
        error "Xcode Release: Unexpected Info.plist path ($info_plist)"
    fi

    if [ "$entitlements" = "src/Wakeve.entitlements" ]; then
        pass "Xcode Release: Uses Wakeve entitlements"
    else
        error "Xcode Release: Unexpected entitlements path ($entitlements)"
    fi

    if [ "$aps_environment" = "production" ]; then
        pass "Xcode Release: APNs environment is production"
    else
        error "Xcode Release: APNs environment must be production"
    fi

    if [[ "$marketing_version" =~ ^[0-9]+(\.[0-9]+){1,2}$ ]]; then
        pass "Xcode Release: Marketing version is $marketing_version"
    else
        error "Xcode Release: Invalid marketing version ($marketing_version)"
    fi

    if [[ "$build_version" =~ ^[0-9]+$ ]]; then
        pass "Xcode Release: Build version is $build_version"
    else
        error "Xcode Release: Invalid build version ($build_version)"
    fi

    local sdkroot_major
    sdkroot_major=$(printf '%s' "$sdkroot" | sed -nE 's/.*iPhoneOS([0-9]+).*/\1/p')
    if [[ "$sdkroot_major" =~ ^[0-9]+$ ]] && [ "$sdkroot_major" -ge 26 ]; then
        pass "Xcode Release: SDKROOT uses iPhoneOS $sdkroot_major"
    else
        error "Xcode Release: SDKROOT must use iPhoneOS 26 or later"
    fi

    local deployment_major="${deployment_target%%.*}"
    if [[ "$deployment_major" =~ ^[0-9]+$ ]] && [ "$deployment_major" -ge 16 ]; then
        pass "Xcode Release: iOS deployment target is $deployment_target"
    else
        warning "Xcode Release: iOS deployment target is below 16.0 ($deployment_target)"
    fi

    if [ "$device_family" = "1,2" ]; then
        pass "Xcode Release: Targets iPhone and iPad"
    else
        error "Xcode Release: TARGETED_DEVICE_FAMILY must include iPhone and iPad"
    fi

    if [ "$app_icon" = "AppIcon" ]; then
        pass "Xcode Release: Uses AppIcon asset catalog"
    else
        error "Xcode Release: Unexpected app icon asset name ($app_icon)"
    fi

    if [ "$mac_support" = "YES" ] && grep -q "SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD" "$PROJECT_DIR/docs/APP_STORE_AVAILABILITY_DECISIONS.md"; then
        pass "Xcode Release: Mac Designed for iPhone/iPad availability is documented"
    elif [ "$mac_support" = "YES" ]; then
        warning "Xcode Release: Mac Designed for iPhone/iPad is enabled but not documented"
    else
        pass "Xcode Release: Mac Designed for iPhone/iPad is disabled"
    fi

    if [ "$xr_support" = "YES" ] && grep -q "SUPPORTS_XR_DESIGNED_FOR_IPHONE_IPAD" "$PROJECT_DIR/docs/APP_STORE_AVAILABILITY_DECISIONS.md"; then
        pass "Xcode Release: Apple Vision Pro availability is documented"
    elif [ "$xr_support" = "YES" ]; then
        warning "Xcode Release: Apple Vision Pro compatibility is enabled but not documented"
    else
        pass "Xcode Release: Apple Vision Pro compatibility is disabled"
    fi

    rm -f "$settings_file"
}

validate_ios_entitlements() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🔏 iOS Entitlements${NC}"

    local entitlements="$PROJECT_DIR/iosApp/src/Wakeve.entitlements"
    if [ ! -f "$entitlements" ]; then
        error "Entitlements: Missing at iosApp/src/Wakeve.entitlements"
        return 1
    fi

    if plutil -lint "$entitlements" >/dev/null 2>&1; then
        pass "Entitlements: Valid XML plist"
    else
        error "Entitlements: Invalid plist"
        return 1
    fi

    if plutil -extract aps-environment raw -o - "$entitlements" 2>/dev/null | grep -qx "\$(APS_ENVIRONMENT)"; then
        pass "Entitlements: APNs environment uses APS_ENVIRONMENT build setting"
    else
        error "Entitlements: aps-environment must use \$(APS_ENVIRONMENT)"
    fi

    if grep -R "INPreferences.requestSiriAuthorization" "$PROJECT_DIR/iosApp/src" >/dev/null 2>&1 || plutil -extract INIntentsSupported raw -o - "$PROJECT_DIR/iosApp/src/Info.plist" >/dev/null 2>&1; then
        if grep -A1 "<key>com.apple.developer.siri</key>" "$entitlements" | grep -q "<true/>"; then
            pass "Entitlements: Siri capability declared"
        else
            error "Entitlements: Missing com.apple.developer.siri while Siri authorization/intents are present"
        fi
    fi

    if grep -R "loginWithGoogle\\|Google Sign-In\\|SignInWithGoogle" "$PROJECT_DIR/iosApp/src" "$PROJECT_DIR/shared/src" >/dev/null 2>&1; then
        if grep -R "SignInWithAppleButton\\|ASAuthorizationAppleIDCredential" "$PROJECT_DIR/iosApp/src" >/dev/null 2>&1; then
            pass "iOS login: Sign in with Apple is present alongside third-party login"
        else
            error "iOS login: Third-party login exists without Sign in with Apple"
        fi

        if plutil -extract 'com\.apple\.developer\.applesignin' xml1 -o - "$entitlements" 2>/dev/null | grep -q "<string>Default</string>"; then
            pass "Entitlements: Sign in with Apple capability declared"
        else
            error "Entitlements: Missing Sign in with Apple capability while third-party login exists"
        fi
    fi

    if grep -R "https://wakeve.app" "$PROJECT_DIR/iosApp/src/Services/DeepLinkService.swift" >/dev/null 2>&1; then
        if grep -A4 "<key>com.apple.developer.associated-domains</key>" "$entitlements" | grep -q "<string>applinks:wakeve.app</string>"; then
            pass "Entitlements: Associated Domains declares applinks:wakeve.app"
        else
            error "Entitlements: Missing applinks:wakeve.app while Universal Links are parsed"
        fi
    fi
}

validate_ios_universal_links() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🔗 iOS Universal Links${NC}"

    local aasa_source="$PROJECT_DIR/apps/landing/src/lib/server/apple-app-site-association.ts"
    local aasa_well_known="$PROJECT_DIR/apps/landing/src/routes/.well-known/apple-app-site-association/+server.ts"
    local aasa_root="$PROJECT_DIR/apps/landing/src/routes/apple-app-site-association/+server.ts"

    if [ -f "$aasa_source" ]; then
        pass "AASA source: Present"
    else
        error "AASA source: Missing apps/landing/src/lib/server/apple-app-site-association.ts"
        return 0
    fi

    if [ -f "$aasa_well_known" ]; then
        pass "AASA route: /.well-known/apple-app-site-association is defined"
    else
        error "AASA route: Missing /.well-known/apple-app-site-association endpoint"
    fi

    if [ -f "$aasa_root" ]; then
        pass "AASA route: /apple-app-site-association is defined"
    else
        error "AASA route: Missing /apple-app-site-association fallback endpoint"
    fi

    if grep -q "APPLE_TEAM_ID" "$aasa_source" && grep -q "TEAM_ID" "$aasa_source"; then
        pass "AASA source: Uses Apple Team ID environment variable"
    else
        error "AASA source: Must derive appIDs from APPLE_TEAM_ID or TEAM_ID"
    fi

    if grep -q "APPLE_TEAM_ID_PATTERN" "$aasa_source" && grep -q "PLACEHOLDER_APPLE_TEAM_IDS" "$aasa_source" && grep -q "ABCDE12345" "$aasa_source"; then
        pass "AASA source: Rejects invalid or placeholder Apple Team IDs"
    else
        error "AASA source: Must reject invalid or placeholder Apple Team IDs before serving AASA"
    fi

    if grep -q "com.guyghost.wakeve" "$aasa_source"; then
        pass "AASA source: Uses iOS bundle ID com.guyghost.wakeve"
    else
        error "AASA source: Missing iOS bundle ID com.guyghost.wakeve"
    fi

    for path in "/event/*" "/poll/*" "/meeting/*" "/invite/*"; do
        if grep -Fq "$path" "$aasa_source"; then
            pass "AASA source: Declares $path"
        else
            error "AASA source: Missing $path"
        fi
    done
}

validate_ios_privacy_manifest() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🔐 iOS Privacy Manifest${NC}"

    local manifest="$PROJECT_DIR/iosApp/src/PrivacyInfo.xcprivacy"
    if [ ! -f "$manifest" ]; then
        error "Privacy manifest: Missing at iosApp/src/PrivacyInfo.xcprivacy"
        return 1
    fi

    if plutil -lint "$manifest" >/dev/null 2>&1; then
        pass "Privacy manifest: Valid XML plist"
    else
        error "Privacy manifest: Invalid plist"
        return 1
    fi

    if plutil -extract NSPrivacyTracking raw -o - "$manifest" 2>/dev/null | grep -qx "false"; then
        pass "Privacy manifest: Tracking disabled"
    else
        error "Privacy manifest: NSPrivacyTracking must be false unless tracking domains are declared"
    fi

    if plutil -extract NSPrivacyTrackingDomains xml1 -o - "$manifest" 2>/dev/null | grep -q "<string>"; then
        error "Privacy manifest: Tracking domains declared while privacy labels say no tracking"
    else
        pass "Privacy manifest: No tracking domains declared"
    fi

    local required_data_types=(
        "NSPrivacyCollectedDataTypeName"
        "NSPrivacyCollectedDataTypeEmailAddress"
        "NSPrivacyCollectedDataTypeUserID"
        "NSPrivacyCollectedDataTypeDeviceID"
        "NSPrivacyCollectedDataTypeOtherUserContent"
        "NSPrivacyCollectedDataTypeCoarseLocation"
        "NSPrivacyCollectedDataTypeProductInteraction"
    )

    for data_type in "${required_data_types[@]}"; do
        if grep -q "$data_type" "$manifest"; then
            pass "Privacy manifest: Declares $data_type"
        else
            error "Privacy manifest: Missing $data_type"
        fi
    done

    local privacy_labels_doc="$PROJECT_DIR/docs/APP_STORE_PRIVACY_LABELS.md"
    if [ -f "$privacy_labels_doc" ]; then
        pass "Privacy labels draft: Present"

        local privacy_label_required_phrases=(
            "## Apple Source Baseline"
            "Apple-source review date: 2026-05-27"
            "App Store privacy details are required to submit new apps and app updates"
            "privacy responses must include the practices of third-party partners"
            "responsible for keeping privacy responses accurate and up to date"
            "identify all data they or their third-party partners collect"
            "meets every optional-disclosure criterion"
            "transmitting data off device"
            "longer than needed to service the request in real time"
            "collected only for app functionality still needs to be declared"
            "linked to the user's identity"
            "de-identified or anonymized before collection"
            "linking app-collected user or device data with third-party data"
            "sharing app-collected user or device data with a data broker"
            "linked only on device and not sent off device"
            "publicly accessible Privacy Policy URL"
            "app-level and should be comprehensive"
            "completing setup for each new data type before publishing"
            "APP_STORE_PRIVACY_EVIDENCE_COMPLETE=true"
        )

        local privacy_label_phrase
        for privacy_label_phrase in "${privacy_label_required_phrases[@]}"; do
            if grep -Fq "$privacy_label_phrase" "$privacy_labels_doc"; then
                pass "Privacy labels draft: Covers $privacy_label_phrase"
            else
                error "Privacy labels draft: Missing $privacy_label_phrase"
            fi
        done

        if grep -Fq "Does this app use data for tracking? **No**" "$privacy_labels_doc"; then
            pass "Privacy labels draft: Declares no tracking"
        else
            error "Privacy labels draft: Must declare no tracking to match PrivacyInfo.xcprivacy"
        fi

        for data_type in "${required_data_types[@]}"; do
            if grep -Fq "$data_type" "$privacy_labels_doc"; then
                pass "Privacy labels draft: Mirrors $data_type"
            else
                error "Privacy labels draft: Missing manifest data type $data_type"
            fi
        done
    else
        error "Privacy labels draft: Missing at docs/APP_STORE_PRIVACY_LABELS.md"
    fi

    if grep -q "NSPrivacyCollectedDataTypeTracking</key>[[:space:]]*<true/>" "$manifest"; then
        error "Privacy manifest: Collected data type is marked as tracking"
    else
        pass "Privacy manifest: Collected data types are not marked as tracking"
    fi

    local required_reason_source_paths=(
        "$PROJECT_DIR/iosApp/src"
        "$PROJECT_DIR/shared/src/iosMain"
        "$PROJECT_DIR/shared/src/commonMain"
    )

    if rg -n "UserDefaults|NSUserDefaults|@AppStorage|@SceneStorage" "${required_reason_source_paths[@]}" >/dev/null 2>&1; then
        if grep -q "NSPrivacyAccessedAPICategoryUserDefaults" "$manifest" && grep -q "CA92.1" "$manifest"; then
            pass "Privacy manifest: Declares UserDefaults required reason API usage"
        else
            error "Privacy manifest: Missing UserDefaults required reason API declaration with CA92.1"
        fi
    fi

    local file_timestamp_api_pattern="attributesOfItem|creationDateKey|contentModificationDateKey|fileModificationDate|creationDate|modificationDate|getattrlist|fgetattrlist|fstat|fstatat|lstat|stat\\("
    if rg -n "$file_timestamp_api_pattern" "${required_reason_source_paths[@]}" >/dev/null 2>&1; then
        if grep -q "NSPrivacyAccessedAPICategoryFileTimestamp" "$manifest" && grep -q "C617.1" "$manifest"; then
            pass "Privacy manifest: Declares file timestamp required reason API usage"
        else
            error "Privacy manifest: Missing file timestamp required reason API declaration with C617.1"
        fi
    elif grep -q "NSPrivacyAccessedAPICategoryFileTimestamp" "$manifest"; then
        pass "Privacy manifest: Declares file timestamp required reason API usage for bundled code or SDKs"
    fi

    if plutil -extract NSUserTrackingUsageDescription raw -o - "$PROJECT_DIR/iosApp/src/Info.plist" >/dev/null 2>&1; then
        error "Info.plist: Declares NSUserTrackingUsageDescription while privacy labels say no tracking"
    else
        pass "Info.plist: Does not declare App Tracking Transparency usage"
    fi

    local tracking_api_pattern="AdSupport|ASIdentifierManager|advertisingIdentifier|AppTrackingTransparency|ATTrackingManager"
    if rg -n "$tracking_api_pattern" "$PROJECT_DIR/iosApp/src" "$PROJECT_DIR/shared/src" "$PROJECT_DIR/composeApp/src" >/dev/null 2>&1; then
        error "iOS source: Uses IDFA/App Tracking Transparency APIs while privacy labels say no tracking"
    else
        pass "iOS source: No IDFA or App Tracking Transparency API usage found"
    fi
}

validate_ios_sdk_requirements() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}🧰 iOS SDK Requirements${NC}"

    local xcode_version
    xcode_version=$(xcodebuild -version 2>/dev/null | head -n 1 || true)
    if [ -n "$xcode_version" ]; then
        pass "Xcode: $xcode_version"
    else
        warning "Xcode: Could not determine installed Xcode version"
    fi

    local sdk_version
    sdk_version=$(xcodebuild -version -sdk iphoneos SDKVersion 2>/dev/null || true)
    if [ -z "$sdk_version" ]; then
        warning "iOS SDK: Could not determine iphoneos SDK version"
        return 0
    fi

    local sdk_major="${sdk_version%%.*}"
    if [[ "$sdk_major" =~ ^[0-9]+$ ]] && [ "$sdk_major" -ge 26 ]; then
        pass "iOS SDK: $sdk_version meets App Store upload minimum"
    else
        error "iOS SDK: $sdk_version is below the App Store upload minimum of iOS 26"
    fi
}

validate_ios_live_endpoints() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0
    [ "$CHECK_LIVE_URLS" = true ] || return 0

    echo ""
    echo -e "${BLUE}🌐 iOS Live Endpoints${NC}"

    validate_live_url "https://wakeve.app/terms" "Terms URL"
    validate_live_url "https://wakeve.app/third-party-notices" "Third-party notices URL"
    validate_live_url "https://wakeve.app/app" "Dashboard app shell URL"
    validate_live_url "https://wakeve.app/app/login" "Dashboard login URL"
    validate_live_url "https://wakeve.app/app/dashboard" "Dashboard home URL"
    validate_live_url "https://wakeve.app/app/create" "Dashboard create URL"
    validate_live_url "https://wakeve.app/app/events" "Dashboard events URL"
    validate_live_redirect "https://wakeve.app/dashboard" "/app/dashboard" "Legacy dashboard redirect"
    validate_live_redirect "https://wakeve.app/login" "/app/login" "Legacy login redirect"
    validate_live_redirect "https://wakeve.app/create" "/app/create" "Legacy create redirect"
    validate_live_redirect "https://wakeve.app/events" "/app/events" "Legacy events redirect"
    validate_live_aasa "https://wakeve.app/.well-known/apple-app-site-association" "Apple App Site Association well-known URL"
    validate_live_aasa "https://wakeve.app/apple-app-site-association" "Apple App Site Association root URL"
    validate_live_url "https://api.wakeve.app/health" "API health URL"
}

validate_live_aasa() {
    local url="$1"
    local label="$2"

    if ! command -v curl >/dev/null 2>&1; then
        warning "$label: Cannot check live AASA because curl is not available"
        return 0
    fi

    local response_file headers_file
    response_file=$(mktemp)
    headers_file=$(mktemp)

    if ! /usr/bin/curl -fsSL --max-time 10 --retry 1 -D "$headers_file" "$url" -o "$response_file" >/dev/null 2>&1; then
        rm -f "$response_file" "$headers_file"
        error "$label: Live URL is not reachable ($url)"
        return 0
    fi

    pass "$label: Live URL reachable"

    if grep -qi '^content-type:[[:space:]]*application/json' "$headers_file"; then
        pass "$label: Content-Type is application/json"
    else
        warning "$label: Content-Type is not application/json"
    fi

    if ruby -rjson -e 'JSON.parse(File.read(ARGV.fetch(0)))' "$response_file" >/dev/null 2>&1; then
        pass "$label: Valid JSON"
    else
        error "$label: Response is not valid JSON"
    fi

    if grep -q "com.guyghost.wakeve" "$response_file"; then
        pass "$label: Contains bundle ID com.guyghost.wakeve"
    else
        error "$label: Missing bundle ID com.guyghost.wakeve"
    fi

    if [ -n "${APPLE_TEAM_ID:-}" ]; then
        if grep -q "${APPLE_TEAM_ID}.com.guyghost.wakeve" "$response_file"; then
            pass "$label: Contains APPLE_TEAM_ID app ID"
        else
            error "$label: Missing ${APPLE_TEAM_ID}.com.guyghost.wakeve"
        fi
    else
        warning "$label: APPLE_TEAM_ID is not set, cannot verify full app ID"
    fi

    for path in "/event/" "/poll/" "/meeting/" "/invite/"; do
        if grep -Fq "$path" "$response_file"; then
            pass "$label: Declares $path links"
        else
            error "$label: Missing $path links"
        fi
    done

    rm -f "$response_file" "$headers_file"
}

validate_ios_release_binary() {
    [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ] || return 0

    echo ""
    echo -e "${BLUE}📦 iOS Release Binary${NC}"

    local app_dir="$PROJECT_DIR/build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app"
    if [ ! -d "$app_dir" ]; then
        warning "Release binary: Missing built app at build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app"
        return 0
    fi

    local manifest="$app_dir/PrivacyInfo.xcprivacy"
    if [ -f "$manifest" ]; then
        pass "Release binary: PrivacyInfo.xcprivacy is bundled"
    else
        error "Release binary: PrivacyInfo.xcprivacy is missing from the app bundle"
    fi

    local built_plist="$app_dir/Info.plist"
    if [ -f "$built_plist" ]; then
        local bundle_id display_name marketing_version build_version
        bundle_id=$(plutil -extract CFBundleIdentifier raw -o - "$built_plist" 2>/dev/null || true)
        display_name=$(plutil -extract CFBundleDisplayName raw -o - "$built_plist" 2>/dev/null || true)
        marketing_version=$(plutil -extract CFBundleShortVersionString raw -o - "$built_plist" 2>/dev/null || true)
        build_version=$(plutil -extract CFBundleVersion raw -o - "$built_plist" 2>/dev/null || true)

        if [ "$bundle_id" = "com.guyghost.wakeve" ]; then
            pass "Release binary: Bundle ID is com.guyghost.wakeve"
        else
            error "Release binary: Unexpected Bundle ID ($bundle_id)"
        fi

        if [ "$display_name" = "Wakeve" ]; then
            pass "Release binary: Display name is Wakeve"
        else
            error "Release binary: Unexpected display name ($display_name)"
        fi

        if [[ "$marketing_version" =~ ^[0-9]+(\.[0-9]+){1,2}$ ]]; then
            pass "Release binary: Marketing version is $marketing_version"
        else
            error "Release binary: Invalid marketing version ($marketing_version)"
        fi

        if [[ "$build_version" =~ ^[0-9]+$ ]]; then
            pass "Release binary: Build version is $build_version"
        else
            error "Release binary: Invalid build version ($build_version)"
        fi

        if plutil -extract CFBundleURLTypes xml1 -o - "$built_plist" 2>/dev/null | grep -q "<string>wakeve</string>"; then
            pass "Release binary: Registers wakeve URL scheme"
        else
            error "Release binary: Missing wakeve URL scheme registration"
        fi
    else
        error "Release binary: Info.plist is missing from the app bundle"
    fi

    if /usr/bin/strings "$app_dir/Wakeve" "$app_dir/Frameworks/Shared.framework/Shared" 2>/dev/null | grep -E "localhost|127\\.0\\.0\\.1|http://"; then
        error "Release binary: Contains localhost, loopback, or cleartext HTTP strings"
    else
        pass "Release binary: No localhost, loopback, or cleartext HTTP strings found"
    fi

    if /usr/bin/strings "$app_dir/Wakeve" "$app_dir/Frameworks/Shared.framework/Shared" 2>/dev/null | grep -E "dev-refresh-token|dev-token-mock|dev@example\\.com|Dev User|wakeve-debug-authenticated|WAKEVE_DEBUG_AUTHENTICATED"; then
        error "Release binary: Contains development auth credentials, mock user strings, or debug auth launch hooks"
    else
        pass "Release binary: No development auth credentials or debug auth launch hooks found"
    fi

    if /usr/bin/strings "$app_dir/Wakeve" "$app_dir/Frameworks/Shared.framework/Shared" 2>/dev/null | grep -E "AdSupport|ASIdentifierManager|advertisingIdentifier|AppTrackingTransparency|ATTrackingManager|NSUserTrackingUsageDescription"; then
        error "Release binary: Contains IDFA/App Tracking Transparency strings while privacy labels say no tracking"
    else
        pass "Release binary: No IDFA or App Tracking Transparency strings found"
    fi

    if ! plutil -extract BGTaskSchedulerPermittedIdentifiers raw -o - "$PROJECT_DIR/iosApp/src/Info.plist" >/dev/null 2>&1 &&
        /usr/bin/strings "$app_dir/Wakeve" "$app_dir/Frameworks/Shared.framework/Shared" 2>/dev/null | grep -E "BGTaskScheduler|BGProcessingTask|BGProcessingTaskRequest|BackgroundTasks"; then
        error "Release binary: Contains BackgroundTasks symbols without App Store background processing plist configuration"
    else
        pass "Release binary: No unconfigured BackgroundTasks symbols found"
    fi
}

# ──────────────────────────────────────────────────────────────
# Main
# ──────────────────────────────────────────────────────────────

echo "╔══════════════════════════════════════════════╗"
echo "║   Wakeve Store Metadata Linter               ║"
echo "║   Validating store listing readiness...       ║"
echo "╚══════════════════════════════════════════════╝"
echo ""

if [ ! -d "$METADATA_DIR" ]; then
    error "Metadata directory not found: $METADATA_DIR"
    echo ""
    echo -e "${RED}❌ Cannot continue — metadata directory missing${NC}"
    exit 1
fi

# Validate Android locales
if [ "$PLATFORM" = "all" ] || [ "$PLATFORM" = "android" ]; then
    for locale_dir in "$METADATA_DIR"/android/*/; do
        [ -d "$locale_dir" ] || continue
        locale=$(basename "$locale_dir")
        validate_android_locale "$locale"
    done
fi

# Validate iOS locales
if [ "$PLATFORM" = "all" ] || [ "$PLATFORM" = "ios" ]; then
    for locale_dir in "$METADATA_DIR"/ios/*/; do
        [ -d "$locale_dir" ] || continue
        locale=$(basename "$locale_dir")
        [ "$locale" = "review_information" ] && continue
        validate_ios_locale "$locale"
    done
fi

# Validate legal pages
validate_legal
validate_public_web_legal_routes

# Validate Fastlane
validate_fastlane

# Validate App Store release environment template safety
validate_app_store_env_template

# Validate top-level App Store readiness report
validate_app_store_readiness_report

# Validate final App Store signoff record
validate_app_store_final_signoff_record
validate_app_store_privacy_evidence

# Validate App Store Connect field map
validate_app_store_submission_runbook
validate_app_store_connect_field_map
validate_app_store_account_access_evidence
validate_app_store_app_information_evidence
validate_app_store_versioning_evidence
validate_app_store_release_artifact_evidence
validate_app_store_content_rights_evidence
validate_app_store_license_notices_evidence
validate_app_store_eula_evidence

# Validate App Store age rating evidence
validate_app_store_age_rating

# Validate active App Store blocker register
validate_app_store_blocker_register
validate_app_store_product_blocker_approval

# Validate App Store Review Guideline audit
validate_app_store_review_guideline_audit
validate_app_store_account_deletion_evidence
validate_app_store_ugc_moderation_evidence

# Validate App Store availability and DSA decisions
validate_app_store_availability_decisions
validate_app_store_availability_evidence
validate_app_store_dsa_trader_status
validate_app_store_pricing_availability_evidence
validate_app_store_sdk_privacy_evidence
validate_app_store_release_control_evidence
validate_app_store_media_localization_evidence

# Validate App Store payment and external purchase compliance
validate_app_store_payment_compliance
validate_app_store_payment_evidence

# Validate App Store Accessibility Nutrition Labels draft
validate_app_store_accessibility_labels
validate_app_store_accessibility_evidence

# Validate TestFlight and launch checklist
validate_app_store_launch_checklist
validate_app_store_testflight_evidence
validate_app_store_observability_evidence
validate_app_store_live_url_aasa_evidence
validate_app_store_capabilities_evidence
validate_app_store_export_compliance_evidence
validate_app_store_review_access_evidence

# Validate iOS plist submission settings
validate_ios_plist

# Validate iOS source network settings
validate_ios_source_network_configuration

# Validate iOS launch source hygiene
validate_ios_launch_source_hygiene
validate_ios_release_debug_logging
validate_ios_debug_auth_isolation
validate_ios_background_task_configuration

# Validate Xcode Release build settings used for App Store builds
validate_ios_xcode_release_settings

# Validate iOS entitlements
validate_ios_entitlements

# Validate Universal Links app/web configuration
validate_ios_universal_links

# Validate iOS privacy manifest source
validate_ios_privacy_manifest

# Validate Apple upload SDK minimums
validate_ios_sdk_requirements

# Validate public URLs that are not directly represented in store metadata
validate_ios_live_endpoints

# Validate built iOS app submission-sensitive bundle contents
validate_ios_release_binary

# ──────────────────────────────────────────────────────────────
# Summary
# ──────────────────────────────────────────────────────────────

echo ""
echo "══════════════════════════════════════════════"
echo -e "  ${GREEN}Passed: $(( ${#RESULTS[@]} - ERRORS - WARNINGS ))${NC}"
echo -e "  ${RED}Errors: $ERRORS${NC}"
echo -e "  ${YELLOW}Warnings: $WARNINGS${NC}"
echo "══════════════════════════════════════════════"

# JSON output
if [ "$JSON_OUTPUT" = true ]; then
    echo ""
    echo '{"errors":'$ERRORS',"warnings":'$WARNINGS',"results":['
    first=true
    for result in "${RESULTS[@]}"; do
        if [ "$first" = true ]; then first=false; else echo ","; fi
        echo -n "\"$result\""
    done
    echo ']}'
fi

if [ "$ERRORS" -gt 0 ]; then
    echo ""
    echo -e "${RED}❌ METADATA VALIDATION FAILED — fix errors before submission${NC}"
    exit 1
elif [ "$WARNINGS" -gt 0 ] && [ "$STRICT_MODE" = true ]; then
    echo ""
    echo -e "${YELLOW}⚠️  Warnings found in strict mode — review recommended${NC}"
    exit 1
else
    echo ""
    echo -e "${GREEN}✅ All metadata validations passed${NC}"
    exit 0
fi
