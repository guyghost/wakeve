#!/usr/bin/env bash
# Focused Android Home/Event workspace checks.
#
# The Google Services Gradle task is not configuration-cache friendly in this
# project, so keep these checks on explicit no-cache Gradle invocations.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

run() {
    echo
    echo "+ $*"
    "$@"
}

cd "$PROJECT_DIR"

run ./gradlew --no-configuration-cache :composeApp:testDebugUnitTest \
    --tests com.guyghost.wakeve.ui.event.EventWorkspaceModelsTest

run ./gradlew --no-configuration-cache :composeApp:compileDebugAndroidTestKotlinAndroid

echo
echo "PASS: Android Event workspace focused checks completed"
