#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────
# Store Metadata Linter — Wakeve
# Validates store listing metadata for both Android and iOS
# Usage: ./scripts/lint-store-metadata.sh [--strict] [--json]
# ──────────────────────────────────────────────────────────────

set -euo pipefail

STRICT_MODE=false
JSON_OUTPUT=false
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
        --help|-h) echo "Usage: $0 [--strict] [--json]"; exit 0 ;;
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
}

validate_fastlane() {
    echo ""
    echo -e "${BLUE}🚀 Fastlane Configuration${NC}"

    local fastlane_dir="$PROJECT_DIR/fastlane"

    if [ -f "$fastlane_dir/Fastfile" ]; then
        pass "Fastfile: Present"

        # Check for key lanes
        for lane in "upload_internal" "upload_production" "upload_testflight" "upload_appstore"; do
            if grep -q "$lane" "$fastlane_dir/Fastfile"; then
                pass "Fastfile: Contains '$lane' lane"
            else
                warning "Fastfile: Missing '$lane' lane"
            fi
        done
    else
        warning "Fastfile: Not found (optional but recommended)"
    fi

    if [ -f "$fastlane_dir/Appfile" ]; then
        pass "Appfile: Present"
    else
        warning "Appfile: Not found"
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
for locale_dir in "$METADATA_DIR"/android/*/; do
    [ -d "$locale_dir" ] || continue
    locale=$(basename "$locale_dir")
    validate_android_locale "$locale"
done

# Validate iOS locales
for locale_dir in "$METADATA_DIR"/ios/*/; do
    [ -d "$locale_dir" ] || continue
    locale=$(basename "$locale_dir")
    validate_ios_locale "$locale"
done

# Validate legal pages
validate_legal

# Validate Fastlane
validate_fastlane

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
