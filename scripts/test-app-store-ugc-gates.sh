#!/usr/bin/env bash
# Regression tests for App Store Guideline 1.2 UGC moderation submission gates.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
AUDIT_SCRIPT="$PROJECT_DIR/scripts/app-store-submission-audit.sh"

assert_contains() {
    local output="$1"
    local expected="$2"
    local label="$3"

    if grep -Fq "$expected" <<<"$output"; then
        echo "PASS: $label"
    else
        echo "FAIL: $label" >&2
        echo "Expected to find: $expected" >&2
        exit 1
    fi
}

assert_fails_with() {
    local label="$1"
    shift

    set +e
    local output
    output=$("$@" 2>&1)
    local status=$?
    set -e

    echo "$output"
    if [ "$status" -eq 0 ]; then
        echo "FAIL: $label unexpectedly passed" >&2
        exit 1
    fi

    echo "PASS: $label failed as expected"
    TEST_OUTPUT="$output"
}

TEST_OUTPUT=""

assert_fails_with \
    "final audit requires explicit UGC moderation readiness confirmation" \
    env APP_REVIEW_PHONE_NUMBER="+33123456789" "$AUDIT_SCRIPT" --skip-preflight
assert_contains \
    "$TEST_OUTPUT" \
    "User-generated content moderation readiness is not confirmed" \
    "missing APP_STORE_UGC_MODERATION_CONFIRMED remains a blocker"

assert_fails_with \
    "final audit rejects forced UGC confirmation without complete evidence" \
    env APP_REVIEW_PHONE_NUMBER="+33123456789" APP_STORE_UGC_MODERATION_CONFIRMED=true "$AUDIT_SCRIPT" --skip-preflight
assert_contains \
    "$TEST_OUTPUT" \
    "PASS: UGC moderation OpenSpec tasks have no unchecked items" \
    "audit recognizes completed UGC OpenSpec tasks"
assert_contains \
    "$TEST_OUTPUT" \
    "App Store UGC moderation evidence is not complete" \
    "incomplete UGC evidence record remains a blocker"
assert_contains \
    "$TEST_OUTPUT" \
    "PASS: UGC moderation tests cover report/block/unblock/filter behavior" \
    "audit recognizes UGC server test evidence"
assert_contains \
    "$TEST_OUTPUT" \
    "PASS: UGC moderation server policy or report/block/unblock endpoints are present" \
    "audit recognizes UGC server implementation evidence"
assert_contains \
    "$TEST_OUTPUT" \
    "PASS: UGC moderation iOS report/block/unblock entry points are present" \
    "audit recognizes UGC iOS discoverability evidence"
assert_contains \
    "$TEST_OUTPUT" \
    "PASS: UGC moderation iOS hidden/pending/rejected states are present" \
    "audit recognizes UGC iOS state evidence"

echo "PASS: App Store UGC gate regression checks completed"
