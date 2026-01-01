#!/bin/bash

# ML Model Accuracy Validation Test Quick Start
# Usage: ./ML_TEST_QUICK_START.sh [option]
#
# Options:
#   all      - Run all tests with detailed output
#   test1    - Run Test 1: Overall Model Accuracy
#   test2    - Run Test 2: Weekend Preference Prediction
#   test3    - Run Test 3: Afternoon Preference Prediction
#   test4    - Run Test 4: Event Type Matching
#   test5    - Run Test 5: Seasonality Prediction
#   test6    - Run Test 6: Confidence Score Distribution
#   test7    - Run Test 7: Fallback Heuristic Accuracy
#   help     - Show this help message

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$SCRIPT_DIR"

echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║     ML Model Accuracy Validation Test Suite - Quick Start          ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo ""

show_help() {
    echo "ML Model Accuracy Validation Test - Quick Start Guide"
    echo ""
    echo "Test Suite: MLModelAccuracyValidationTest"
    echo "Location: shared/src/commonTest/kotlin/com/guyghost/wakeve/ml/"
    echo ""
    echo "Available Options:"
    echo "  all      Run all 7 accuracy tests with verbose output"
    echo "  test1    Test 1: Overall Model Accuracy (>=70%)"
    echo "  test2    Test 2: Weekend Preference Prediction"
    echo "  test3    Test 3: Afternoon Preference Prediction"
    echo "  test4    Test 4: Event Type Matching"
    echo "  test5    Test 5: Seasonality Prediction"
    echo "  test6    Test 6: Confidence Score Distribution"
    echo "  test7    Test 7: Fallback Heuristic Accuracy"
    echo "  help     Show this help message"
    echo ""
    echo "Examples:"
    echo "  ./ML_TEST_QUICK_START.sh all      # Run all tests"
    echo "  ./ML_TEST_QUICK_START.sh test1    # Run accuracy test"
    echo ""
}

run_all_tests() {
    echo "📊 Running ALL ML Accuracy Validation Tests"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "📈 Datasets:"
    echo "  • Training Data: 1000 simulated events"
    echo "  • Validation Data: 200 scenarios"
    echo "  • Random Seed: 42 (reproducible)"
    echo ""
    cd "$PROJECT_DIR"
    ./gradlew shared:jvmTest --tests "MLModelAccuracyValidationTest" -i
}

run_test_1() {
    echo "📊 Running TEST 1: Overall Model Accuracy"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Requirement: Verify > 70% accuracy on validation set"
    echo ""
    cd "$PROJECT_DIR"
    ./gradlew shared:jvmTest --tests "*MLModelAccuracyValidationTest*given_validation_set*" -i
}

run_test_2() {
    echo "📊 Running TEST 2: Weekend Preference Prediction"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Requirement: Verify weekend preference learning"
    echo ""
    cd "$PROJECT_DIR"
    ./gradlew shared:jvmTest --tests "*MLModelAccuracyValidationTest*given_user_prefers_weekend*" -i
}

run_test_3() {
    echo "📊 Running TEST 3: Afternoon Preference Prediction"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Requirement: Verify time-of-day preference learning"
    echo ""
    cd "$PROJECT_DIR"
    ./gradlew shared:jvmTest --tests "*MLModelAccuracyValidationTest*given_user_prefers_afternoon*" -i
}

run_test_4() {
    echo "📊 Running TEST 4: Event Type Matching"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Requirement: Verify event type recommendation matching"
    echo ""
    cd "$PROJECT_DIR"
    ./gradlew shared:jvmTest --tests "*MLModelAccuracyValidationTest*given_cultural_event*" -i
}

run_test_5() {
    echo "📊 Running TEST 5: Seasonality Prediction"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Requirement: Verify seasonal preference learning"
    echo ""
    cd "$PROJECT_DIR"
    ./gradlew shared:jvmTest --tests "*MLModelAccuracyValidationTest*given_summer*" -i
}

run_test_6() {
    echo "📊 Running TEST 6: Confidence Score Distribution"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Requirement: Verify confidence score reliability"
    echo ""
    cd "$PROJECT_DIR"
    ./gradlew shared:jvmTest --tests "*MLModelAccuracyValidationTest*Confidence*" -i
}

run_test_7() {
    echo "📊 Running TEST 7: Fallback Heuristic Accuracy"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Requirement: Verify fallback heuristic effectiveness"
    echo ""
    cd "$PROJECT_DIR"
    ./gradlew shared:jvmTest --tests "*MLModelAccuracyValidationTest*Fallback*" -i
}

# Main execution
case "${1:-help}" in
    all)
        run_all_tests
        ;;
    test1)
        run_test_1
        ;;
    test2)
        run_test_2
        ;;
    test3)
        run_test_3
        ;;
    test4)
        run_test_4
        ;;
    test5)
        run_test_5
        ;;
    test6)
        run_test_6
        ;;
    test7)
        run_test_7
        ;;
    help)
        show_help
        ;;
    *)
        echo "❌ Unknown option: $1"
        echo ""
        show_help
        exit 1
        ;;
esac

echo ""
echo "✅ Test execution completed!"
